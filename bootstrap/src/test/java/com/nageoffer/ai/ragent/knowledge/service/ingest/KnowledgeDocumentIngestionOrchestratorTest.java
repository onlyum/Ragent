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

import com.nageoffer.ai.ragent.core.chunk.ChunkEmbeddingService;
import com.nageoffer.ai.ragent.core.chunk.ChunkingMode;
import com.nageoffer.ai.ragent.core.chunk.ChunkingOptions;
import com.nageoffer.ai.ragent.core.chunk.ChunkingStrategy;
import com.nageoffer.ai.ragent.core.chunk.ChunkingStrategyFactory;
import com.nageoffer.ai.ragent.core.chunk.QmdSmartOptions;
import com.nageoffer.ai.ragent.core.chunk.TextBoundaryOptions;
import com.nageoffer.ai.ragent.core.chunk.VectorChunk;
import com.nageoffer.ai.ragent.core.parser.DocumentParser;
import com.nageoffer.ai.ragent.core.parser.DocumentParserSelector;
import com.nageoffer.ai.ragent.core.parser.ParserType;
import com.nageoffer.ai.ragent.knowledge.dao.entity.KnowledgeDocumentDO;
import com.nageoffer.ai.ragent.knowledge.enums.DocumentType;
import com.nageoffer.ai.ragent.knowledge.service.ingest.chunk.DefaultDocumentChunkEngine;
import com.nageoffer.ai.ragent.knowledge.service.ingest.chunk.QmdDocumentChunkEngine;
import com.nageoffer.ai.ragent.rag.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeDocumentIngestionOrchestratorTest {

    @Mock
    private DocumentParserSelector parserSelector;
    @Mock
    private ChunkingStrategyFactory chunkingStrategyFactory;
    @Mock
    private ChunkEmbeddingService chunkEmbeddingService;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private QmdProcessClient qmdProcessClient;
    @Mock
    private DocumentParser markdownParser;
    @Mock
    private DocumentParser tikaParser;
    @Mock
    private ChunkingStrategy chunkingStrategy;

    @Test
    void shouldUseMimeParserForGeneralDocumentsAndWriteMetadata() {
        KnowledgeDocumentIngestionOrchestrator orchestrator = newOrchestrator();
        KnowledgeDocumentDO documentDO = KnowledgeDocumentDO.builder()
                .id("doc-1")
                .kbId("kb-1")
                .docName("readme.md")
                .docType("general")
                .sourceType("file")
                .fileType("text/plain")
                .fileUrl("s3://bucket/readme.md")
                .chunkStrategy("structure_aware")
                .build();
        ChunkingOptions options = ChunkingMode.STRUCTURE_AWARE.createOptions(Map.of());
        List<VectorChunk> chunks = List.of(VectorChunk.builder()
                .chunkId("chunk-1")
                .index(0)
                .content("hello")
                .build());

        when(parserSelector.selectByMimeType("text/plain")).thenReturn(markdownParser);
        when(markdownParser.getParserType()).thenReturn(ParserType.MARKDOWN.getType());
        when(parserSelector.select(ParserType.MARKDOWN.getType())).thenReturn(markdownParser);
        when(fileStorageService.openStream("s3://bucket/readme.md")).thenReturn(streamOf("ignored"));
        when(markdownParser.extractText(any(InputStream.class), eq("readme.md"))).thenReturn("# title\nhello");
        when(chunkingStrategyFactory.requireStrategy(ChunkingMode.STRUCTURE_AWARE)).thenReturn(chunkingStrategy);
        when(chunkingStrategy.chunk(eq("# title\nhello"), eq(options))).thenReturn(chunks);

        KnowledgeDocumentIngestionOrchestrator.IngestionProcessResult result = orchestrator.process(
                documentDO,
                ChunkingMode.STRUCTURE_AWARE,
                options,
                "bge-m3"
        );

        assertEquals(DocumentType.GENERAL, result.route().documentType());
        assertEquals(ParserType.MARKDOWN.getType(), result.route().parserType());
        assertSame(chunks, result.chunks());
        assertEquals("general", chunks.get(0).getMetadata().get("doc_type"));
        assertEquals("general", chunks.get(0).getMetadata().get("source_type"));
        assertEquals("file", chunks.get(0).getMetadata().get("ingest_source_type"));
        assertEquals("text/plain", chunks.get(0).getMetadata().get("file_type"));
        assertEquals("structure_aware", chunks.get(0).getMetadata().get("chunk_strategy"));
        assertEquals("structure_aware", chunks.get(0).getMetadata().get("chunk_engine"));
        assertEquals(false, chunks.get(0).getMetadata().get("chunk_fallback"));
        assertEquals(ParserType.MARKDOWN.getType(), chunks.get(0).getMetadata().get("parser_type"));
        verify(parserSelector).selectByMimeType("text/plain");
        verify(chunkEmbeddingService).embed(chunks, "bge-m3");
    }

    @Test
    void shouldFallbackAcademicPaperToTikaRoute() {
        KnowledgeDocumentIngestionOrchestrator orchestrator = newOrchestrator();
        KnowledgeDocumentDO documentDO = KnowledgeDocumentDO.builder()
                .id("doc-2")
                .kbId("kb-2")
                .docName("paper.pdf")
                .docType("academic_paper")
                .sourceType("url")
                .fileType("application/pdf")
                .fileUrl("s3://bucket/paper.pdf")
                .chunkStrategy("fixed_size")
                .build();
        ChunkingOptions options = ChunkingMode.FIXED_SIZE.createOptions(Map.of());
        List<VectorChunk> chunks = List.of(VectorChunk.builder()
                .chunkId("chunk-2")
                .index(0)
                .content("paper body")
                .build());

        when(parserSelector.select(ParserType.TIKA.getType())).thenReturn(tikaParser);
        when(fileStorageService.openStream("s3://bucket/paper.pdf")).thenReturn(streamOf("ignored"));
        when(tikaParser.extractText(any(InputStream.class), eq("paper.pdf"))).thenReturn("paper body");
        when(chunkingStrategyFactory.requireStrategy(ChunkingMode.FIXED_SIZE)).thenReturn(chunkingStrategy);
        when(chunkingStrategy.chunk(eq("paper body"), eq(options))).thenReturn(chunks);

        KnowledgeDocumentIngestionOrchestrator.IngestionProcessResult result = orchestrator.process(
                documentDO,
                ChunkingMode.FIXED_SIZE,
                options,
                "bge-m3"
        );

        assertEquals(DocumentType.ACADEMIC_PAPER, result.route().documentType());
        assertEquals(ParserType.TIKA.getType(), result.route().parserType());
        assertEquals("academic_paper", chunks.get(0).getMetadata().get("doc_type"));
        assertEquals("academic_paper", chunks.get(0).getMetadata().get("source_type"));
        assertEquals("url", chunks.get(0).getMetadata().get("ingest_source_type"));
        assertEquals("fixed_size", chunks.get(0).getMetadata().get("chunk_engine"));
        assertEquals(false, chunks.get(0).getMetadata().get("chunk_fallback"));
        assertTrue(result.extractDuration() >= 0);
        verify(parserSelector, never()).selectByMimeType("application/pdf");
        verify(parserSelector).select(ParserType.TIKA.getType());
    }

    @Test
    void shouldUseQmdChunkerWhenQmdStrategyIsRequested() {
        KnowledgeDocumentIngestionOrchestrator orchestrator = newOrchestrator();
        KnowledgeDocumentDO documentDO = KnowledgeDocumentDO.builder()
                .id("doc-3")
                .kbId("kb-3")
                .docName("notes.md")
                .docType("general")
                .sourceType("file")
                .fileType("text/markdown")
                .fileUrl("s3://bucket/notes.md")
                .chunkStrategy("qmd_smart")
                .build();
        ChunkingOptions options = new QmdSmartOptions(3600, 540, 800);
        List<VectorChunk> chunks = List.of(VectorChunk.builder()
                .index(0)
                .content("qmd chunk")
                .build());

        when(parserSelector.selectByMimeType("text/markdown")).thenReturn(markdownParser);
        when(markdownParser.getParserType()).thenReturn(ParserType.MARKDOWN.getType());
        when(parserSelector.select(ParserType.MARKDOWN.getType())).thenReturn(markdownParser);
        when(fileStorageService.openStream("s3://bucket/notes.md")).thenReturn(streamOf("ignored"));
        when(markdownParser.extractText(any(InputStream.class), eq("notes.md"))).thenReturn("# intro\nqmd body");
        when(qmdProcessClient.chunk("# intro\nqmd body", documentDO, (QmdSmartOptions) options)).thenReturn(chunks);

        KnowledgeDocumentIngestionOrchestrator.IngestionProcessResult result = orchestrator.process(
                documentDO,
                ChunkingMode.QMD_SMART,
                options,
                "bge-m3"
        );

        assertEquals("qmd", result.chunkEngine());
        assertEquals(false, result.chunkFallback());
        assertTrue(chunks.get(0).getChunkId() != null && !chunks.get(0).getChunkId().isBlank());
        assertEquals("qmd", chunks.get(0).getMetadata().get("chunk_engine"));
        assertEquals(false, chunks.get(0).getMetadata().get("chunk_fallback"));
        verify(qmdProcessClient).chunk("# intro\nqmd body", documentDO, (QmdSmartOptions) options);
        verify(chunkingStrategyFactory, never()).requireStrategy(ChunkingMode.QMD_SMART);
    }

    @Test
    void shouldFallbackToStructureAwareWhenQmdFails() {
        KnowledgeDocumentIngestionOrchestrator orchestrator = newOrchestrator();
        KnowledgeDocumentDO documentDO = KnowledgeDocumentDO.builder()
                .id("doc-4")
                .kbId("kb-4")
                .docName("report.txt")
                .docType("project_report")
                .sourceType("url")
                .fileType("text/plain")
                .fileUrl("s3://bucket/report.txt")
                .chunkStrategy("qmd_smart")
                .build();
        QmdSmartOptions options = new QmdSmartOptions(3000, 300, 600);
        List<VectorChunk> chunks = List.of(VectorChunk.builder()
                .chunkId("chunk-4")
                .index(0)
                .content("fallback chunk")
                .build());

        when(parserSelector.select(ParserType.TIKA.getType())).thenReturn(tikaParser);
        when(fileStorageService.openStream("s3://bucket/report.txt")).thenReturn(streamOf("ignored"));
        when(tikaParser.extractText(any(InputStream.class), eq("report.txt"))).thenReturn("report body");
        when(qmdProcessClient.chunk("report body", documentDO, options))
                .thenThrow(new IllegalStateException("qmd unavailable"));
        when(chunkingStrategyFactory.requireStrategy(ChunkingMode.STRUCTURE_AWARE)).thenReturn(chunkingStrategy);
        when(chunkingStrategy.chunk(eq("report body"), any(TextBoundaryOptions.class))).thenReturn(chunks);

        KnowledgeDocumentIngestionOrchestrator.IngestionProcessResult result = orchestrator.process(
                documentDO,
                ChunkingMode.QMD_SMART,
                options,
                "bge-m3"
        );

        ArgumentCaptor<TextBoundaryOptions> fallbackCaptor = ArgumentCaptor.forClass(TextBoundaryOptions.class);
        verify(chunkingStrategy).chunk(eq("report body"), fallbackCaptor.capture());
        assertEquals("structure_aware", result.chunkEngine());
        assertEquals(true, result.chunkFallback());
        assertEquals("structure_aware", chunks.get(0).getMetadata().get("chunk_engine"));
        assertEquals(true, chunks.get(0).getMetadata().get("chunk_fallback"));
        assertTrue(fallbackCaptor.getValue().maxChars() >= options.maxChars());
        assertTrue(fallbackCaptor.getValue().targetChars() > 0);
    }

    private InputStream streamOf(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    private KnowledgeDocumentIngestionOrchestrator newOrchestrator() {
        return new KnowledgeDocumentIngestionOrchestrator(
                parserSelector,
                List.of(
                        new QmdDocumentChunkEngine(qmdProcessClient, chunkingStrategyFactory),
                        new DefaultDocumentChunkEngine(chunkingStrategyFactory)
                ),
                chunkEmbeddingService,
                fileStorageService
        );
    }
}
