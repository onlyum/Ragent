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

package com.nageoffer.ai.ragent.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * QMD 智能切分配置
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "rag.knowledge.qmd")
public class KnowledgeQmdProperties {

    /**
     * 是否启用 QMD 智能切分
     */
    private Boolean enabled = true;

    /**
     * 调用脚本所使用的命令
     */
    private String command = "node";

    /**
     * QMD 适配脚本路径（相对于工作目录，或绝对路径）
     */
    private String scriptPath = "scripts/qmd/chunker.mjs";

    /**
     * QMD 包安装目录，可为空。为空时由脚本自行在本地 node_modules 中解析。
     */
    private String packageDir;

    /**
     * QMD 切分策略。当前阶段默认使用 regex，避免引入额外 AST 依赖。
     */
    private String chunkStrategy = "regex";

    /**
     * 外部脚本执行超时时间（毫秒）
     */
    private Long timeoutMs = 120000L;
}
