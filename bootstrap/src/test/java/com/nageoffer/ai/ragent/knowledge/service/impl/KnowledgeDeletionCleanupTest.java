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

package com.nageoffer.ai.ragent.knowledge.service.impl;

import com.nageoffer.ai.ragent.core.chunk.ChunkEmbeddingService;
import com.nageoffer.ai.ragent.core.chunk.ChunkingStrategyFactory;
import com.nageoffer.ai.ragent.core.parser.DocumentParserSelector;
import com.nageoffer.ai.ragent.framework.exception.ClientException;
import com.nageoffer.ai.ragent.framework.mq.producer.MessageQueueProducer;
import com.nageoffer.ai.ragent.infra.embedding.EmbeddingService;
import com.nageoffer.ai.ragent.infra.token.TokenCounterService;
import com.nageoffer.ai.ragent.knowledge.config.KnowledgeScheduleProperties;
import com.nageoffer.ai.ragent.knowledge.dao.entity.KnowledgeBaseDO;
import com.nageoffer.ai.ragent.knowledge.dao.entity.KnowledgeDocumentDO;
import com.nageoffer.ai.ragent.knowledge.dao.mapper.KnowledgeBaseMapper;
import com.nageoffer.ai.ragent.knowledge.dao.mapper.KnowledgeChunkMapper;
import com.nageoffer.ai.ragent.knowledge.dao.mapper.KnowledgeDocumentChunkLogMapper;
import com.nageoffer.ai.ragent.knowledge.dao.mapper.KnowledgeDocumentMapper;
import com.nageoffer.ai.ragent.knowledge.enums.DocumentStatus;
import com.nageoffer.ai.ragent.knowledge.handler.RemoteFileFetcher;
import com.nageoffer.ai.ragent.knowledge.service.KnowledgeChunkService;
import com.nageoffer.ai.ragent.knowledge.service.KnowledgeDocumentScheduleService;
import com.nageoffer.ai.ragent.knowledge.service.ingest.KnowledgeDocumentIngestionOrchestrator;
import com.nageoffer.ai.ragent.rag.core.vector.VectorStoreAdmin;
import com.nageoffer.ai.ragent.rag.core.vector.VectorStoreService;
import com.nageoffer.ai.ragent.rag.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;
import software.amazon.awssdk.services.s3.S3Client;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeDeletionCleanupTest {

    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;
    @Mock
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Mock
    private VectorStoreAdmin vectorStoreAdmin;
    @Mock
    private S3Client s3Client;

    @Mock
    private DocumentParserSelector parserSelector;
    @Mock
    private ChunkingStrategyFactory chunkingStrategyFactory;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private VectorStoreService vectorStoreService;
    @Mock
    private KnowledgeChunkService knowledgeChunkService;
    @Mock
    private KnowledgeDocumentScheduleService scheduleService;
    @Mock
    private ChunkEmbeddingService chunkEmbeddingService;
    @Mock
    private KnowledgeDocumentChunkLogMapper chunkLogMapper;
    @Mock
    private TransactionOperations transactionOperations;
    @Mock
    private MessageQueueProducer messageQueueProducer;
    @Mock
    private KnowledgeScheduleProperties scheduleProperties;
    @Mock
    private RemoteFileFetcher remoteFileFetcher;
    @Mock
    private KnowledgeDocumentIngestionOrchestrator ingestionOrchestrator;

    @Mock
    private KnowledgeChunkMapper chunkMapper;
    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private TokenCounterService tokenCounterService;

    @Test
    void shouldPhysicallyDeleteKnowledgeBaseAndCleanupResources() {
        KnowledgeBaseServiceImpl service = new KnowledgeBaseServiceImpl(
                knowledgeBaseMapper,
                knowledgeDocumentMapper,
                vectorStoreAdmin,
                s3Client
        );
        KnowledgeBaseDO kb = KnowledgeBaseDO.builder()
                .id("kb-1")
                .collectionName("productdocs")
                .deleted(0)
                .build();
        when(knowledgeBaseMapper.selectById("kb-1")).thenReturn(kb);
        when(knowledgeDocumentMapper.selectCount(any())).thenReturn(0L);

        service.delete("kb-1");

        verify(knowledgeBaseMapper).deleteById(kb);
    }

    @Test
    void shouldRefuseDeleteKnowledgeBaseWhenDocumentsRemain() {
        KnowledgeBaseServiceImpl service = new KnowledgeBaseServiceImpl(
                knowledgeBaseMapper,
                knowledgeDocumentMapper,
                vectorStoreAdmin,
                s3Client
        );
        KnowledgeBaseDO kb = KnowledgeBaseDO.builder()
                .id("kb-1")
                .collectionName("productdocs")
                .deleted(0)
                .build();
        when(knowledgeBaseMapper.selectById("kb-1")).thenReturn(kb);
        when(knowledgeDocumentMapper.selectCount(any())).thenReturn(1L);

        assertThrows(ClientException.class, () -> service.delete("kb-1"));

        verify(knowledgeBaseMapper, never()).deleteById(any(KnowledgeBaseDO.class));
    }

    @Test
    void shouldPhysicallyDeleteDocumentAndCleanupVectorsAndFile() {
        KnowledgeDocumentServiceImpl service = new KnowledgeDocumentServiceImpl(
                knowledgeBaseMapper,
                knowledgeDocumentMapper,
                fileStorageService,
                vectorStoreService,
                knowledgeChunkService,
                new ObjectMapper(),
                scheduleService,
                chunkEmbeddingService,
                chunkLogMapper,
                transactionOperations,
                messageQueueProducer,
                scheduleProperties,
                remoteFileFetcher,
                ingestionOrchestrator
        );
        KnowledgeDocumentDO document = KnowledgeDocumentDO.builder()
                .id("doc-1")
                .kbId("kb-1")
                .status(DocumentStatus.SUCCESS.getCode())
                .fileUrl("s3://productdocs/manual.pdf")
                .build();
        KnowledgeBaseDO kb = KnowledgeBaseDO.builder()
                .id("kb-1")
                .collectionName("productdocs")
                .build();
        when(knowledgeDocumentMapper.selectById("doc-1")).thenReturn(document);
        when(knowledgeBaseMapper.selectById("kb-1")).thenReturn(kb);

        service.delete("doc-1");

        verify(knowledgeChunkService).deleteByDocId("doc-1");
        verify(scheduleService).deleteByDocId("doc-1");
        verify(chunkLogMapper).delete(any());
        verify(knowledgeDocumentMapper).deleteById(document);
        verify(vectorStoreService).deleteDocumentVectors("productdocs", "doc-1");
        verify(fileStorageService).deleteByUrl("s3://productdocs/manual.pdf");
    }

    @Test
    void shouldPhysicallyDeleteChunksByDocId() {
        KnowledgeChunkServiceImpl service = new KnowledgeChunkServiceImpl(
                chunkMapper,
                knowledgeDocumentMapper,
                knowledgeBaseMapper,
                embeddingService,
                tokenCounterService,
                vectorStoreService,
                transactionOperations
        );

        service.deleteByDocId("doc-1");

        verify(chunkMapper).delete(any());
    }
}
