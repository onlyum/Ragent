/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nageoffer.ai.ragent.knowledge.service.ingest;

import cn.hutool.core.util.IdUtil;
import com.nageoffer.ai.ragent.core.chunk.ChunkEmbeddingService;
import com.nageoffer.ai.ragent.core.chunk.ChunkingMode;
import com.nageoffer.ai.ragent.core.chunk.ChunkingOptions;
import com.nageoffer.ai.ragent.core.chunk.VectorChunk;
import com.nageoffer.ai.ragent.core.parser.DocumentParser;
import com.nageoffer.ai.ragent.core.parser.DocumentParserSelector;
import com.nageoffer.ai.ragent.core.parser.ParserType;
import com.nageoffer.ai.ragent.knowledge.dao.entity.KnowledgeDocumentDO;
import com.nageoffer.ai.ragent.knowledge.enums.DocumentType;
import com.nageoffer.ai.ragent.knowledge.service.ingest.chunk.ChunkEngineResult;
import com.nageoffer.ai.ragent.knowledge.service.ingest.chunk.DocumentChunkEngine;
import com.nageoffer.ai.ragent.rag.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档摄取编排器
 * <p>
 * 第一阶段负责把主链路中的解析、切块、metadata 写入与向量化从 Service 中抽离出来，
 * 为第二阶段 QMD 和第三阶段多解析引擎路由预留稳定入口。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentIngestionOrchestrator {

    private final DocumentParserSelector parserSelector;
    private final List<DocumentChunkEngine> chunkEngines;
    private final ChunkEmbeddingService chunkEmbeddingService;
    private final FileStorageService fileStorageService;

    public IngestionProcessResult process(KnowledgeDocumentDO documentDO, ChunkingMode chunkingMode,
                                          ChunkingOptions chunkingOptions, String embeddingModel) {
        DocumentIngestionRoute route = resolveRoute(documentDO);
        DocumentParser parser = requireParser(route.parserType());

        long extractStart = System.currentTimeMillis();
        try (InputStream inputStream = fileStorageService.openStream(documentDO.getFileUrl())) {
            String text = parser.extractText(inputStream, documentDO.getDocName());
            long extractDuration = System.currentTimeMillis() - extractStart;

            long chunkStart = System.currentTimeMillis();
            ChunkExecutionResult chunkExecutionResult = executeChunking(documentDO, chunkingMode, chunkingOptions, text);
            List<VectorChunk> chunks = chunkExecutionResult.chunks();
            normalizeChunkIdentity(chunks);
            long chunkDuration = System.currentTimeMillis() - chunkStart;

            applyMetadata(documentDO, chunks, route, chunkExecutionResult);

            long embedStart = System.currentTimeMillis();
            chunkEmbeddingService.embed(chunks, embeddingModel);
            long embedDuration = System.currentTimeMillis() - embedStart;

            return new IngestionProcessResult(
                    chunks,
                    route,
                    extractDuration,
                    chunkDuration,
                    embedDuration,
                    chunkExecutionResult.chunkEngine(),
                    chunkExecutionResult.fallback()
            );
        } catch (Exception e) {
            throw new RuntimeException("文档内容提取或分块失败", e);
        }
    }

    public void enrichChunks(KnowledgeDocumentDO documentDO, List<VectorChunk> chunks) {
        applyMetadata(documentDO, chunks, resolveRoute(documentDO), inferChunkExecution(documentDO));
    }

    DocumentIngestionRoute resolveRoute(KnowledgeDocumentDO documentDO) {
        DocumentType documentType = DocumentType.normalize(documentDO == null ? null : documentDO.getDocType());
        String parserType = switch (documentType) {
            case GENERAL -> parserSelector.selectByMimeType(documentDO.getFileType()).getParserType();
            case PROJECT_REPORT, ACADEMIC_PAPER -> ParserType.TIKA.getType();
        };
        return new DocumentIngestionRoute(documentType, parserType);
    }

    private DocumentParser requireParser(String parserType) {
        DocumentParser parser = parserSelector.select(parserType);
        if (parser == null) {
            throw new IllegalStateException("未找到文档解析器: " + parserType);
        }
        return parser;
    }

    private ChunkExecutionResult executeChunking(KnowledgeDocumentDO documentDO, ChunkingMode chunkingMode,
                                                 ChunkingOptions chunkingOptions, String text) {
        DocumentChunkEngine chunkEngine = chunkEngines.stream()
                .filter(engine -> engine.supports(chunkingMode))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到文档切块引擎: " + chunkingMode));
        ChunkEngineResult result = chunkEngine.chunk(documentDO, chunkingMode, chunkingOptions, text);
        return new ChunkExecutionResult(result.chunks(), result.chunkEngine(), result.fallback());
    }

    private ChunkExecutionResult inferChunkExecution(KnowledgeDocumentDO documentDO) {
        ChunkingMode requestedMode = ChunkingMode.fromValue(documentDO == null ? null : documentDO.getChunkStrategy());
        if (requestedMode == ChunkingMode.QMD_SMART) {
            return new ChunkExecutionResult(List.of(), "qmd", false);
        }
        String chunkEngine = requestedMode == null ? ChunkingMode.STRUCTURE_AWARE.getValue() : requestedMode.getValue();
        return new ChunkExecutionResult(List.of(), chunkEngine, false);
    }

    private void applyMetadata(KnowledgeDocumentDO documentDO, List<VectorChunk> chunks, DocumentIngestionRoute route,
                               ChunkExecutionResult chunkExecutionResult) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        for (VectorChunk chunk : chunks) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            if (chunk.getMetadata() != null) {
                metadata.putAll(chunk.getMetadata());
            }
            metadata.put("kb_id", documentDO.getKbId());
            metadata.put("doc_id", documentDO.getId());
            metadata.put("doc_name", documentDO.getDocName());
            metadata.put("doc_type", route.documentType().getValue());
            metadata.put("source_type", route.documentType().getValue());
            metadata.put("ingest_source_type", documentDO.getSourceType());
            metadata.put("file_type", documentDO.getFileType());
            metadata.put("chunk_strategy", documentDO.getChunkStrategy());
            metadata.put("chunk_engine", chunkExecutionResult.chunkEngine());
            metadata.put("chunk_fallback", chunkExecutionResult.fallback());
            metadata.put("parse_engine", route.parserType());
            metadata.put("parser_type", route.parserType());
            chunk.setMetadata(metadata);
        }
    }

    private void normalizeChunkIdentity(List<VectorChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        for (int i = 0; i < chunks.size(); i++) {
            VectorChunk chunk = chunks.get(i);
            if (chunk == null) {
                continue;
            }
            if (chunk.getIndex() == null) {
                chunk.setIndex(i);
            }
            if (chunk.getChunkId() == null || chunk.getChunkId().isBlank()) {
                chunk.setChunkId(IdUtil.getSnowflakeNextIdStr());
            }
        }
    }

    private record ChunkExecutionResult(List<VectorChunk> chunks, String chunkEngine, boolean fallback) {
    }

    public record IngestionProcessResult(List<VectorChunk> chunks, DocumentIngestionRoute route, long extractDuration,
                                         long chunkDuration, long embedDuration, String chunkEngine,
                                         boolean chunkFallback) {
    }
}
