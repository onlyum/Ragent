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

package com.nageoffer.ai.ragent.framework.trace;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * RAG Trace 上下文
 * 使用 TTL 在异步线程池中透传 traceId 与节点栈
 *
 * 这里的“TTL”是 TransmittableThreadLocal 的概念：
 * 当任务提交到线程池时，TTL 会捕获当前线程的上下文快照；
 * 在子线程执行前，再把这份上下文恢复到 worker 线程。
 * 这样异步/线程池场景下也能继续沿用同一次 trace。
 */
public final class RagTraceContext {

    /** 当前 traceId，代表一次完整的链路运行 */
    private static final TransmittableThreadLocal<String> TRACE_ID = new TransmittableThreadLocal<>();
    /** 当前 taskId，主要用于 SSE / 流式会话中的任务级关联 */
    private static final TransmittableThreadLocal<String> TASK_ID = new TransmittableThreadLocal<>();
    /** 当前节点栈，栈顶是当前正在执行的父节点 ID */
    private static final TransmittableThreadLocal<Deque<String>> NODE_STACK = new TransmittableThreadLocal<>();

    private RagTraceContext() {
    }

    public static String getTraceId() {
        return TRACE_ID.get();
    }

    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
    }

    public static String getTaskId() {
        return TASK_ID.get();
    }

    public static void setTaskId(String taskId) {
        TASK_ID.set(taskId);
    }

    /**
     * 当前逻辑调用深度，即节点栈大小。
     * 入口节点 depth=0，嵌套调用后依次 +1。
     */
    public static int depth() {
        Deque<String> stack = NODE_STACK.get();
        return stack == null ? 0 : stack.size();
    }

    /**
     * 获取当前正在执行的节点 ID，用于新节点计算 parentNodeId。
     */
    public static String currentNodeId() {
        Deque<String> stack = NODE_STACK.get();
        return stack == null ? null : stack.peek();
    }

    /**
     * 进入一个节点时，将 nodeId 推入栈顶。
     */
    public static void pushNode(String nodeId) {
        Deque<String> stack = NODE_STACK.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            NODE_STACK.set(stack);
        }
        stack.push(nodeId);
    }

    /**
     * 退出当前节点时弹栈，栈空后清除 ThreadLocal 避免线程池线程污染。
     */
    public static void popNode() {
        Deque<String> stack = NODE_STACK.get();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.pop();
        if (stack.isEmpty()) {
            NODE_STACK.remove();
        }
    }

    /**
     * 清除当前线程中的 trace / task / 节点栈上下文。
     */
    public static void clear() {
        TRACE_ID.remove();
        TASK_ID.remove();
        NODE_STACK.remove();
    }
}
