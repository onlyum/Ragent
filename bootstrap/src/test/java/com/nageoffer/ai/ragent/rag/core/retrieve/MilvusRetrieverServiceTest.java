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

package com.nageoffer.ai.ragent.rag.core.retrieve;

import com.nageoffer.ai.ragent.infra.embedding.EmbeddingService;
import com.nageoffer.ai.ragent.rag.config.RAGDefaultProperties;
import io.milvus.v2.client.MilvusClientV2;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class MilvusRetrieverServiceTest {

    @Test
    void shouldBuildMilvusMetadataExpression() {
        MilvusRetrieverService service = new MilvusRetrieverService(
                mock(EmbeddingService.class),
                mock(MilvusClientV2.class),
                mock(RAGDefaultProperties.class)
        );
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("doc_type", "academic_paper");
        filters.put("chunk_index", 3);
        filters.put("quoted", "a\"b");

        String expression = service.buildMetadataFilterExpression(filters);

        assertEquals(
                "metadata[\"doc_type\"] == \"academic_paper\" && metadata[\"chunk_index\"] == 3 && metadata[\"quoted\"] == \"a\\\"b\"",
                expression
        );
    }
}
