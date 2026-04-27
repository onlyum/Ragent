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
import com.nageoffer.ai.ragent.core.chunk.ChunkingStrategy;
import com.nageoffer.ai.ragent.core.chunk.ChunkingStrategyFactory;
import com.nageoffer.ai.ragent.core.chunk.VectorChunk;
import com.nageoffer.ai.ragent.knowledge.dao.entity.KnowledgeDocumentDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 兼容现有固定大小与结构感知切块策略的默认引擎
 */
@Component
@RequiredArgsConstructor
public class DefaultDocumentChunkEngine implements DocumentChunkEngine {

    private final ChunkingStrategyFactory chunkingStrategyFactory;

    @Override
    public boolean supports(ChunkingMode mode) {
        return mode != null && mode != ChunkingMode.QMD_SMART;
    }

    @Override
    public ChunkEngineResult chunk(KnowledgeDocumentDO documentDO, ChunkingMode mode, ChunkingOptions options, String text) {
        ChunkingStrategy chunkingStrategy = chunkingStrategyFactory.requireStrategy(mode);
        List<VectorChunk> chunks = chunkingStrategy.chunk(text, options);
        return new ChunkEngineResult(chunks, mode.getValue(), false);
    }
}
