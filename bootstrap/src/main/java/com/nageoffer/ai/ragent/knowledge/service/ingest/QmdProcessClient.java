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

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nageoffer.ai.ragent.core.chunk.QmdSmartOptions;
import com.nageoffer.ai.ragent.core.chunk.VectorChunk;
import com.nageoffer.ai.ragent.knowledge.config.KnowledgeQmdProperties;
import com.nageoffer.ai.ragent.knowledge.dao.entity.KnowledgeDocumentDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 通过本地 Node 适配脚本调用 QMD 的进程客户端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QmdProcessClient {

    private final ObjectMapper objectMapper;
    private final KnowledgeQmdProperties qmdProperties;

    public List<VectorChunk> chunk(String text, KnowledgeDocumentDO documentDO, QmdSmartOptions options) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        if (!Boolean.TRUE.equals(qmdProperties.getEnabled())) {
            throw new IllegalStateException("QMD 智能切分未启用");
        }

        ProcessBuilder processBuilder = createProcessBuilder();
        String requestBody = writeRequest(text, documentDO, options);
        long startTime = System.currentTimeMillis();
        long timeoutMs = resolveTimeoutMs();
        try {
            Process process = processBuilder.start();
            CompletableFuture<String> stdoutFuture = readAsync(process.getInputStream());
            CompletableFuture<String> stderrFuture = readAsync(process.getErrorStream());
            try (OutputStream outputStream = process.getOutputStream()) {
                outputStream.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                String stderr = readFutureSafely(stderrFuture);
                String stdout = readFutureSafely(stdoutFuture);
                throw new IllegalStateException("QMD 切分超时，timeoutMs=" + timeoutMs
                        + ", textLength=" + text.length()
                        + ", stderr=" + abbreviate(stderr)
                        + ", stdout=" + abbreviate(stdout));
            }

            String stdout = readFuture(stdoutFuture);
            String stderr = readFuture(stderrFuture);
            if (process.exitValue() != 0) {
                throw new IllegalStateException("QMD 进程执行失败: " + abbreviate(stderr));
            }

            QmdChunkResponse response = objectMapper.readValue(stdout, QmdChunkResponse.class);
            if (response == null || !"qmd".equalsIgnoreCase(response.engine())) {
                throw new IllegalStateException("QMD 返回的引擎标识异常: " + (response == null ? null : response.engine()));
            }
            if (response.chunks() == null || response.chunks().isEmpty()) {
                throw new IllegalStateException("QMD 未返回有效分块结果");
            }
            List<VectorChunk> chunks = new ArrayList<>(response.chunks().size());
            for (int i = 0; i < response.chunks().size(); i++) {
                QmdChunkItem item = response.chunks().get(i);
                if (!StringUtils.hasText(item.text())) {
                    continue;
                }
                chunks.add(VectorChunk.builder()
                        .chunkId(IdUtil.getSnowflakeNextIdStr())
                        .index(item.index() != null ? item.index() : i)
                        .content(item.text())
                        .metadata(Map.of("chunk_pos", item.position() == null ? 0 : item.position()))
                        .build());
            }
            if (chunks.isEmpty()) {
                throw new IllegalStateException("QMD 返回的分块内容为空");
            }
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > 5000) {
                log.info("QMD 切分完成较慢，docId={}, docName={}, textLength={}, chunkCount={}, elapsedMs={}",
                        documentDO == null ? null : documentDO.getId(),
                        documentDO == null ? null : documentDO.getDocName(),
                        text.length(),
                        chunks.size(),
                        elapsed);
            }
            return chunks;
        } catch (IOException e) {
            throw new IllegalStateException("QMD 进程调用失败: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("QMD 切分被中断", e);
        }
    }

    ProcessBuilder createProcessBuilder() {
        String command = StringUtils.hasText(qmdProperties.getCommand()) ? qmdProperties.getCommand().trim() : "node";
        String scriptPath = resolveScriptPath();
        ProcessBuilder processBuilder = new ProcessBuilder(command, scriptPath);
        processBuilder.directory(Path.of(System.getProperty("user.dir")).toFile());
        if (StringUtils.hasText(qmdProperties.getPackageDir())) {
            processBuilder.environment().put("QMD_PACKAGE_DIR", qmdProperties.getPackageDir().trim());
        }
        return processBuilder;
    }

    private String resolveScriptPath() {
        Path script = Path.of(qmdProperties.getScriptPath());
        if (script.isAbsolute()) {
            return script.toString();
        }
        return Path.of(System.getProperty("user.dir")).resolve(script).normalize().toString();
    }

    private String writeRequest(String text, KnowledgeDocumentDO documentDO, QmdSmartOptions options) {
        try {
            QmdChunkRequest request = new QmdChunkRequest(
                    text,
                    documentDO == null ? null : documentDO.getDocName(),
                    options.maxChars(),
                    options.overlapChars(),
                    options.windowChars(),
                    qmdProperties.getChunkStrategy()
            );
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new IllegalStateException("QMD 请求序列化失败", e);
        }
    }

    private String readFully(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private CompletableFuture<String> readAsync(InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return readFully(inputStream);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private String readFuture(CompletableFuture<String> future) {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("QMD 进程输出读取被中断", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof UncheckedIOException uncheckedIOException) {
                throw new IllegalStateException("QMD 进程输出读取失败", uncheckedIOException.getCause());
            }
            throw new IllegalStateException("QMD 进程输出读取失败", cause);
        } catch (TimeoutException e) {
            throw new IllegalStateException("QMD 进程输出读取超时", e);
        }
    }

    private String readFutureSafely(CompletableFuture<String> future) {
        try {
            return future.get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "";
        }
    }

    private long resolveTimeoutMs() {
        Long configured = qmdProperties.getTimeoutMs();
        return configured == null || configured <= 0 ? 120000L : configured;
    }

    private String abbreviate(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replace("\r", " ").replace("\n", " ").trim();
        if (normalized.length() <= 300) {
            return normalized;
        }
        return normalized.substring(0, 300) + "...";
    }

    private record QmdChunkRequest(String text, String fileName, int maxChars, int overlapChars, int windowChars,
                                   String chunkStrategy) {
    }

    private record QmdChunkResponse(String engine, List<QmdChunkItem> chunks) {
    }

    private record QmdChunkItem(Integer index, String text, Integer position) {
    }
}
