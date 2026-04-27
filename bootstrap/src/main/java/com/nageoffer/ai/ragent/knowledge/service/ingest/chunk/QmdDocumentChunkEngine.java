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

package com.nageoffer.ai.ragent.knowledge.service.ingest.chunk;

import com.nageoffer.ai.ragent.core.chunk.ChunkingMode;
import com.nageoffer.ai.ragent.core.chunk.ChunkingOptions;
import com.nageoffer.ai.ragent.core.chunk.ChunkingStrategyFactory;
import com.nageoffer.ai.ragent.core.chunk.QmdSmartOptions;
import com.nageoffer.ai.ragent.core.chunk.TextBoundaryOptions;
import com.nageoffer.ai.ragent.core.chunk.VectorChunk;
import com.nageoffer.ai.ragent.knowledge.dao.entity.KnowledgeDocumentDO;
import com.nageoffer.ai.ragent.knowledge.service.ingest.QmdProcessClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * QMD 智能切块引擎，失败时回退到结构感知切块
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QmdDocumentChunkEngine implements DocumentChunkEngine {

    private final QmdProcessClient qmdProcessClient;
    private final ChunkingStrategyFactory chunkingStrategyFactory;

    @Override
    public boolean supports(ChunkingMode mode) {
        return mode == ChunkingMode.QMD_SMART;
    }

    @Override
    public ChunkEngineResult chunk(KnowledgeDocumentDO documentDO, ChunkingMode mode, ChunkingOptions options, String text) {
        if (!(options instanceof QmdSmartOptions qmdOptions)) {
            throw new IllegalArgumentException("QMD 切块需要 QmdSmartOptions 配置");
        }
        try {
            List<VectorChunk> chunks = qmdProcessClient.chunk(text, documentDO, qmdOptions);
            return new ChunkEngineResult(chunks, "qmd", false);
        } catch (Exception e) {
            log.warn("QMD 智能切分失败，回退为 structure_aware，docId={}, docName={}",
                    documentDO == null ? null : documentDO.getId(),
                    documentDO == null ? null : documentDO.getDocName(),
                    e);
            TextBoundaryOptions fallbackOptions = toStructureAwareFallback(qmdOptions);
            List<VectorChunk> fallbackChunks = chunkingStrategyFactory
                    .requireStrategy(ChunkingMode.STRUCTURE_AWARE)
                    .chunk(text, fallbackOptions);
            return new ChunkEngineResult(fallbackChunks, ChunkingMode.STRUCTURE_AWARE.getValue(), true);
        }
    }

    private TextBoundaryOptions toStructureAwareFallback(QmdSmartOptions options) {
        int maxChars = Math.max(1, options.maxChars());
        int overlapChars = Math.max(0, options.overlapChars());
        int windowChars = Math.max(0, options.windowChars());
        int targetChars = Math.max(1, maxChars - Math.max(200, Math.min(windowChars, Math.max(200, maxChars / 2))));
        int minChars = Math.max(100, targetChars / 2);
        return new TextBoundaryOptions(targetChars, overlapChars, maxChars, minChars);
    }
}
