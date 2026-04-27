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

package com.nageoffer.ai.ragent.knowledge.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 文档业务类型枚举
 */
@Getter
@RequiredArgsConstructor
public enum DocumentType {

    /**
     * 通用文档
     */
    GENERAL("general"),

    /**
     * 学术论文
     */
    ACADEMIC_PAPER("academic_paper"),

    /**
     * 项目报告
     */
    PROJECT_REPORT("project_report");

    private final String value;

    public static DocumentType fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        if ("general".equals(normalized) || "default".equals(normalized)) {
            return GENERAL;
        }
        if ("academic_paper".equals(normalized) || "academic-paper".equals(normalized) || "paper".equals(normalized)) {
            return ACADEMIC_PAPER;
        }
        if ("project_report".equals(normalized) || "project-report".equals(normalized) || "report".equals(normalized)) {
            return PROJECT_REPORT;
        }
        return null;
    }

    /**
     * 解析文档类型，空值默认回落到通用文档
     */
    public static DocumentType normalize(String value) {
        if (StrUtil.isBlank(value)) {
            return GENERAL;
        }
        DocumentType result = fromValue(value);
        if (result == null) {
            throw new IllegalArgumentException("不支持的文档类型: " + value);
        }
        return result;
    }
}
