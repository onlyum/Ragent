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

package com.nageoffer.ai.ragent.infra.chat;

import com.nageoffer.ai.ragent.framework.convention.ChatRequest;
import com.nageoffer.ai.ragent.infra.model.ModelTarget;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class LLMJsonLogger {

    private static final String UNKNOWN_STEP = "unknown-step";

    public static void logRequest(ChatRequest request, ModelTarget target, String provider, boolean stream, String json) {
        log.info("[LLM_JSON][step={}][phase=request][mode={}][provider={}][modelId={}] {}",
                resolveStep(request),
                stream ? "stream" : "sync",
                provider,
                target == null ? "unknown-model" : target.id(),
                json);
    }

    public static void logResponse(ChatRequest request, ModelTarget target, String provider, boolean stream, String json) {
        log.info("[LLM_JSON][step={}][phase=response][mode={}][provider={}][modelId={}] {}",
                resolveStep(request),
                stream ? "stream" : "sync",
                provider,
                target == null ? "unknown-model" : target.id(),
                json);
    }

    public static void logStreamChunk(ChatRequest request, ModelTarget target, String provider, int chunkIndex, String json) {
        log.info("[LLM_JSON][step={}][phase=response-chunk][mode=stream][provider={}][modelId={}][chunk={}] {}",
                resolveStep(request),
                provider,
                target == null ? "unknown-model" : target.id(),
                chunkIndex,
                json);
    }

    private static String resolveStep(ChatRequest request) {
        if (request == null || request.getStep() == null || request.getStep().isBlank()) {
            return UNKNOWN_STEP;
        }
        return request.getStep().trim();
    }
}
