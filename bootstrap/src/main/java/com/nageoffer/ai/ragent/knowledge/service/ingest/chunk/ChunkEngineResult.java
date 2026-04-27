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

import com.nageoffer.ai.ragent.core.chunk.VectorChunk;

import java.util.List;

/**
 * 文档切块引擎执行结果
 *
 * @param chunks      切块结果
 * @param chunkEngine 实际使用的切块引擎
 * @param fallback    是否发生回退
 */
public record ChunkEngineResult(List<VectorChunk> chunks, String chunkEngine, boolean fallback) {
}
