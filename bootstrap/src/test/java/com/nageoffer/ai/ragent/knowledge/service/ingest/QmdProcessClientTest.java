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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nageoffer.ai.ragent.core.chunk.QmdSmartOptions;
import com.nageoffer.ai.ragent.core.chunk.VectorChunk;
import com.nageoffer.ai.ragent.knowledge.config.KnowledgeQmdProperties;
import com.nageoffer.ai.ragent.knowledge.dao.entity.KnowledgeDocumentDO;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QmdProcessClientTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldBuildProcessBuilderWithConfiguredScriptAndPackageDir() {
        KnowledgeQmdProperties properties = new KnowledgeQmdProperties();
        properties.setCommand("node");
        properties.setScriptPath("scripts/qmd/chunker.mjs");
        properties.setPackageDir("C:/tools/qmd");

        QmdProcessClient client = new QmdProcessClient(new ObjectMapper(), properties);
        ProcessBuilder processBuilder = client.createProcessBuilder();

        assertEquals("node", processBuilder.command().get(0));
        assertTrue(processBuilder.command().get(1).replace("\\", "/").endsWith("scripts/qmd/chunker.mjs"));
        assertEquals("C:/tools/qmd", processBuilder.environment().get("QMD_PACKAGE_DIR"));
    }

    @Test
    void shouldMapAdapterResponseIntoVectorChunks() throws Exception {
        Assumptions.assumeTrue(isNodeAvailable(), "node command is not available");

        Path script = tempDir.resolve("adapter.mjs");
        Files.writeString(script, """
                const chunks = [];
                for await (const chunk of process.stdin) {
                  chunks.push(typeof chunk === "string" ? Buffer.from(chunk) : chunk);
                }
                const request = JSON.parse(Buffer.concat(chunks).toString("utf8"));
                process.stdout.write(JSON.stringify({
                  engine: "qmd",
                  chunks: [
                    { index: 2, text: `chunk:${request.fileName}`, position: 17 }
                  ]
                }));
                """);

        KnowledgeQmdProperties properties = new KnowledgeQmdProperties();
        properties.setCommand("node");
        properties.setScriptPath(script.toString());
        properties.setTimeoutMs(5000L);

        QmdProcessClient client = new QmdProcessClient(new ObjectMapper(), properties);
        KnowledgeDocumentDO documentDO = KnowledgeDocumentDO.builder()
                .docName("paper.md")
                .build();

        List<VectorChunk> chunks = client.chunk("sample text", documentDO, new QmdSmartOptions(3600, 540, 800));

        assertEquals(1, chunks.size());
        assertEquals(2, chunks.get(0).getIndex());
        assertEquals("chunk:paper.md", chunks.get(0).getContent());
        assertEquals(17, chunks.get(0).getMetadata().get("chunk_pos"));
        assertTrue(chunks.get(0).getChunkId() != null && !chunks.get(0).getChunkId().isBlank());
    }

    @Test
    void shouldDrainLargeStderrWithoutBlockingProcess() throws Exception {
        Assumptions.assumeTrue(isNodeAvailable(), "node command is not available");

        Path script = tempDir.resolve("adapter-large-stderr.mjs");
        Files.writeString(script, """
                const noisy = "x".repeat(1024 * 512);
                process.stderr.write(noisy);
                process.stdout.write(JSON.stringify({
                  engine: "qmd",
                  chunks: [
                    { index: 0, text: "ok", position: 0 }
                  ]
                }));
                """);

        KnowledgeQmdProperties properties = new KnowledgeQmdProperties();
        properties.setCommand("node");
        properties.setScriptPath(script.toString());
        properties.setTimeoutMs(5000L);

        QmdProcessClient client = new QmdProcessClient(new ObjectMapper(), properties);
        List<VectorChunk> chunks = client.chunk("sample text", KnowledgeDocumentDO.builder().docName("noise.md").build(),
                new QmdSmartOptions(3600, 540, 800));

        assertEquals(1, chunks.size());
        assertEquals("ok", chunks.get(0).getContent());
    }

    @Test
    void shouldRejectUnexpectedAdapterEngine() throws Exception {
        Assumptions.assumeTrue(isNodeAvailable(), "node command is not available");

        Path script = tempDir.resolve("adapter-wrong-engine.mjs");
        Files.writeString(script, """
                process.stdout.write(JSON.stringify({
                  engine: "legacy",
                  chunks: [
                    { index: 0, text: "ok", position: 0 }
                  ]
                }));
                """);

        KnowledgeQmdProperties properties = new KnowledgeQmdProperties();
        properties.setCommand("node");
        properties.setScriptPath(script.toString());
        properties.setTimeoutMs(5000L);

        QmdProcessClient client = new QmdProcessClient(new ObjectMapper(), properties);
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> client.chunk(
                "sample text",
                KnowledgeDocumentDO.builder().docName("wrong.md").build(),
                new QmdSmartOptions(3600, 540, 800)
        ));

        assertTrue(exception.getMessage().contains("引擎标识异常"));
    }

    private boolean isNodeAvailable() {
        try {
            Process process = new ProcessBuilder("node", "-v").start();
            boolean finished = process.waitFor(Duration.ofSeconds(3).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
