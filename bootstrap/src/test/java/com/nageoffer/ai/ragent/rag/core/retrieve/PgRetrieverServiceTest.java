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
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class PgRetrieverServiceTest {

    @Test
    void shouldBuildMetadataAwareQueryPlan() {
        PgRetrieverService service = new PgRetrieverService(mock(JdbcTemplate.class), mock(EmbeddingService.class));
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("doc_type", "academic_paper");
        filters.put("source_type", "academic_paper");
        filters.put("ignored", null);

        RetrieveRequest request = RetrieveRequest.builder()
                .collectionName("kb_docs")
                .topK(8)
                .metadataFilters(filters)
                .build();

        PgRetrieverService.QueryPlan plan = service.buildQueryPlan(request, "[0.1,0.2]");

        assertEquals(
                "SELECT id, content, 1 - (embedding <=> ?::vector) AS score FROM t_knowledge_vector " +
                        "WHERE metadata->>'collection_name' = ? AND metadata->>? = ? AND metadata->>? = ? " +
                        "ORDER BY embedding <=> ?::vector LIMIT ?",
                plan.sql()
        );
        assertArrayEquals(
                new Object[]{"[0.1,0.2]", "kb_docs", "doc_type", "academic_paper", "source_type", "academic_paper", "[0.1,0.2]", 8},
                plan.args()
        );
    }
}
