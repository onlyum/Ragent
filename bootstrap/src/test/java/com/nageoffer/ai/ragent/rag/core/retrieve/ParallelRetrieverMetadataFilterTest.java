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

import com.nageoffer.ai.ragent.framework.convention.RetrievedChunk;
import com.nageoffer.ai.ragent.rag.core.intent.IntentNode;
import com.nageoffer.ai.ragent.rag.core.intent.NodeScore;
import com.nageoffer.ai.ragent.rag.core.retrieve.channel.strategy.CollectionParallelRetriever;
import com.nageoffer.ai.ragent.rag.core.retrieve.channel.strategy.IntentParallelRetriever;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParallelRetrieverMetadataFilterTest {

    @Test
    void shouldPassMetadataFiltersToCollectionRetrieval() {
        RetrieverService retrieverService = mock(RetrieverService.class);
        when(retrieverService.retrieve(org.mockito.ArgumentMatchers.any(RetrieveRequest.class)))
                .thenReturn(List.of(RetrievedChunk.builder().id("1").text("hit").score(1F).build()));
        CollectionParallelRetriever retriever = new CollectionParallelRetriever(retrieverService, Runnable::run);
        Map<String, Object> filters = Map.of("doc_type", "project_report");

        retriever.executeParallelRetrieval("question", List.of("collection-a"), 3, filters);

        ArgumentCaptor<RetrieveRequest> captor = ArgumentCaptor.forClass(RetrieveRequest.class);
        verify(retrieverService).retrieve(captor.capture());
        assertEquals("collection-a", captor.getValue().getCollectionName());
        assertEquals(filters, captor.getValue().getMetadataFilters());
    }

    @Test
    void shouldPassMetadataFiltersToIntentRetrieval() {
        RetrieverService retrieverService = mock(RetrieverService.class);
        when(retrieverService.retrieve(org.mockito.ArgumentMatchers.any(RetrieveRequest.class)))
                .thenReturn(List.of(RetrievedChunk.builder().id("1").text("hit").score(1F).build()));
        IntentParallelRetriever retriever = new IntentParallelRetriever(retrieverService, Runnable::run);
        IntentNode node = IntentNode.builder()
                .id("intent-a")
                .collectionName("collection-a")
                .topK(5)
                .build();
        NodeScore nodeScore = NodeScore.builder().node(node).score(0.9).build();
        Map<String, Object> filters = Map.of("doc_type", "academic_paper");

        retriever.executeParallelRetrieval("question", List.of(nodeScore), 3, 2, filters);

        ArgumentCaptor<RetrieveRequest> captor = ArgumentCaptor.forClass(RetrieveRequest.class);
        verify(retrieverService).retrieve(captor.capture());
        assertEquals("collection-a", captor.getValue().getCollectionName());
        assertEquals(10, captor.getValue().getTopK());
        assertEquals(filters, captor.getValue().getMetadataFilters());
    }
}
