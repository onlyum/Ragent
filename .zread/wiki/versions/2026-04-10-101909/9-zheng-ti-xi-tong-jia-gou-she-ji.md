Ragent 是一个基于 Spring Boot 的企业级 RAG（检索增强生成）智能系统，采用模块化分层架构设计，集成了向量数据库、多模型路由、文档处理流水线等核心组件，为用户提供智能问答、知识库管理、会话记忆等功能。

## 系统总体架构

### 架构概览

Ragent 系统采用分层架构设计，从上至下分为客户端层、应用编排层、AI 基础设施层和存储层：

```mermaid
graph TB
    subgraph "客户端层"
        A[浏览器/应用端] --> B[前端界面 + SSE]
    end
    
    subgraph "应用编排层"
        B --> C[统一入口: /api/ragent]
        C --> D[接口层]
        D --> E[RAG 主链路]
        D --> F[文档入库流水线]
        D --> G[可观测与运营]
    end
    
    subgraph "AI 基础设施层"
        E --> H[模型路由与容错]
        E --> I[多通道检索引擎]
        F --> J[文档处理引擎]
        G --> K[链路追踪系统]
    end
    
    subgraph "存储层"
        H --> L[向量数据库]
        H --> M[关系数据库]
        H --> N[缓存系统]
        H --> O[对象存储]
        I --> P[MCP 工具服务]
    end
```

### 核心特性

| 特性 | 描述 | 技术实现 |
|------|------|----------|
| **多通道检索** | 支持向量检索、意图定向检索等多种检索策略 | 可扩展的 SearchChannel 接口设计 |
| **模型路由** | 多模型智能路由与故障切换机制 | Provider 优先级探活机制 |
| **文档流水线** | 完整的文档摄取、处理、索引流程 | 节点式流水线引擎 |
| **会话记忆** | 智能会话管理与上下文维护 | 对话摘要+历史消息机制 |
| **工具集成** | MCP 工具服务支持 | MCP 协议实现 |

## 模块架构

### 模块划分

Ragent 系统采用多模块 Maven 项目结构，主要分为以下核心模块：

```mermaid
graph LR
    subgraph "项目根目录"
        A[bootstrap] --> B[应用启动层]
        C[framework] --> D[通用框架层]
        E[infra-ai] --> F[AI 基础设施层]
        G[mcp-server] --> H[工具服务层]
        I[frontend] --> J[前端界面层]
    end
    
    B --> K[Spring Boot 应用]
    D --> L[统一异常处理]
    D --> M[分布式ID]
    D --> N[幂等性控制]
    F --> O[模型客户端]
    F --> P[向量计算]
    H --> Q[工具执行引擎]
    J --> R[React + TypeScript]
```

#### 1. Bootstrap 模块（应用启动层）

作为应用的启动入口，负责：
- Spring Boot 应用的启动与配置
- 核心业务模块的组织与协调
- 外部依赖的初始化

主要组件：
- `RagentApplication.java` - 主启动类
- `rag/` - RAG 核心业务逻辑
- `ingestion/` - 文档摄取流水线
- `knowledge/` - 知识库管理
- `admin/` - 管理后台功能

#### 2. Framework 模块（通用框架层）

提供系统通用的基础设施组件：

```mermaid
graph TB
    subgraph "Framework 模块"
        A[convention] --> B[统一返回格式]
        C[exception] --> D[全局异常处理]
        E[trace] --> F[链路追踪]
        G[idempotent] --> H[幂等性控制]
        I[distributedid] --> J[分布式ID生成]
        K[cache] --> L[缓存抽象]
        M[mq] --> N[消息队列抽象]
        O[web] --> P[Web层增强]
    end
```

#### 3. Infra-AI 模块（AI 基础设施层）

封装 AI 相关的基础能力：

```mermaid
graph LR
    subgraph "Infra-AI 模块"
        A[chat] --> B[对话模型客户端]
        C[embedding] --> D[向量模型客户端]
        E[rerank] --> F[重排模型客户端]
        G[model] --> H[模型抽象层]
        I[http] --> J[HTTP 客户端封装]
        K[token] --> L[Token 计算工具]
        M[util] --> N[AI 工具类]
    end
```

#### 4. MCP-Server 模块（工具服务层）

实现 MCP (Model Context Protocol) 工具服务：

```mermaid
graph TB
    subgraph "MCP-Server 模块"
        A[core] --> B[工具注册中心]
        C[endpoint] --> D[API 端点管理]
        E[executor] --> F[工具执行引擎]
        G[protocol] --> H[MCP 协议实现]
    end
```

### 数据架构

#### 数据库设计

系统采用多数据库架构设计：

| 数据库 | 用途 | 技术选型 |
|--------|------|----------|
| **关系数据库** | 用户、会话、消息等结构化数据 | PostgreSQL + pgvector |
| **向量数据库** | 向量存储与相似性检索 | Milvus / pgvector |
| **缓存系统** | 会话状态、临时数据 | Redis |
| **对象存储** | 文件存储 | MinIO / S3 兼容 |

#### 核心数据表

```mermaid
erDiagram
    t_user ||--o{ t_conversation : creates
    t_user ||--o{ t_message : sends
    t_conversation ||--o{ t_message : contains
    t_conversation ||--o{ t_conversation_summary : has
    t_message ||--o{ t_message_feedback : has
    t_knowledge_base ||--o{ t_knowledge_document : contains
    t_knowledge_document ||--o{ t_knowledge_chunk : chunks
    
    t_user {
        VARCHAR id PK
        VARCHAR username
        VARCHAR password
        VARCHAR role
        TIMESTAMP create_time
    }
    
    t_conversation {
        VARCHAR id PK
        VARCHAR conversation_id
        VARCHAR user_id FK
        VARCHAR title
        TIMESTAMP create_time
    }
    
    t_message {
        VARCHAR id PK
        VARCHAR conversation_id FK
        VARCHAR user_id FK
        VARCHAR role
        TEXT content
        TIMESTAMP create_time
    }
    
    t_knowledge_base {
        VARCHAR id PK
        VARCHAR name
        VARCHAR description
        VARCHAR user_id FK
    }
    
    t_knowledge_document {
        VARCHAR id PK
        VARCHAR knowledge_base_id FK
        VARCHAR title
        VARCHAR source_url
        VARCHAR file_path
        VARCHAR status
    }
    
    t_knowledge_chunk {
        VARCHAR id PK
        VARCHAR document_id FK
        VECTOR embedding
        TEXT content
        INT chunk_index
    }
```

## 核心业务流程

### RAG 主链路流程

```mermaid
sequenceDiagram
    participant U as 用户
    participant C as RAGChatController
    participant S as RAGChatService
    participant M as 会话记忆管理
    participant R as 问题重写器
    participant I as 意图识别器
    participant E as 多通道检索引擎
    participant P as 后置处理器
    participant L as LLM 模型
    participant DB as 数据存储
    
    U->>C: 发送问题
    C->>S: chat(question, conversationId)
    S->>M: 加载会话记忆
    M->>DB: 查询历史消息
    M->>S: 返回记忆上下文
    S->>R: 重写当前问题
    R->>S: 返回改写后问题
    S->>I: 意图识别
    I->>S: 返回意图结果
    S->>E: 多通道检索
    E->>E: 并行执行检索通道
    E->>P: 结果聚合
    P->>P: 后置处理（去重、重排）
    P->>S: 返回检索结果
    S->>L: 组装提示词
    L->>S: 流式返回答案
    S->>C: SSE 流式输出
    C->>U: 实时显示答案
```

#### 处理步骤详解

1. **会话记忆加载**
   - 从数据库加载最近 N 条对话历史
   - 生成对话摘要（超过指定轮次时）
   - 将上下文信息补入当前问题

2. **问题重写与优化**
   - 基于历史对话重写用户问题
   - 消除歧义，明确意图
   - 优化查询表达

3. **意图识别与引导**
   - 判断用户问题的真实意图
   - 提供可能的选项引导用户
   - 确定检索策略

4. **多通道并行检索**
   - 向量全局检索：在所有知识库中搜索相关内容
   - 意图定向检索：基于意图在特定知识库中搜索
   - 工具检索：调用 MCP 工具获取信息

5. **结果后处理**
   - 去重处理：移除重复的检索结果
   - 质量过滤：基于阈值过滤低质量结果
   - 重排序：使用 Rerank 模型优化结果排序

6. **答案生成**
   - 组装检索结果为提示词上下文
   - 路由到合适的对话模型
   - 流式生成答案

### 文档入库流水线

```mermaid
graph TD
    A[文档来源] --> B[文件/URL/飞书/对象存储]
    B --> C[抓取节点 FetcherNode]
    C --> D[解析节点 ParserNode]
    D --> E[分块节点 ChunkerNode]
    E --> F[增强节点 EnhancerNode]
    F --> G[丰富节点 EnricherNode]
    G --> H[索引节点 IndexerNode]
    H --> I[向量数据库]
    H --> J[关系数据库]
    
    A --> K[处理条件评估]
    K --> C
```

#### 流水线节点

| 节点名称 | 功能描述 | 输入 | 输出 |
|----------|----------|------|------|
| **FetcherNode** | 文档抓取与下载 | 来源URL/文件 | 原始文档内容 |
| **ParserNode** | 文档解析与清理 | 原始文档 | 结构化文本 |
| **ChunkerNode** | 文档分块处理 | 结构化文本 | 文档块集合 |
| **EnhancerNode** | 内容增强与优化 | 文档块 | 增强后内容 |
| **EnricherNode** | 元数据丰富 | 文档块 + 增强内容 | 完整块数据 |
| **IndexerNode** | 索引构建与存储 | 完整块数据 | 向量索引 |

## 技术架构

### 技术栈选型

| 层级 | 组件 | 技术选型 | 用途 |
|------|------|----------|------|
| **应用层** | Spring Boot | 3.5.7 | 应用框架 |
| **Web层** | Spring MVC | - | RESTful API |
| **数据层** | MyBatis Plus | 3.5.14 | ORM 框架 |
| **数据库** | PostgreSQL | - | 主数据库 |
| **向量存储** | pgvector/Milvus | 2.6.6 | 向量检索 |
| **缓存** | Redis | - | 缓存存储 |
| **消息队列** | RocketMQ | 2.3.5 | 异步处理 |
| **前端** | React + TypeScript | 18+ | 前端框架 |
| **构建工具** | Maven | - | 项目构建 |

### 核心依赖

```yaml
# 关键技术依赖
spring-boot: 3.5.7
mybatis-plus: 3.5.14
milvus-sdk: 2.6.6
tika: 3.2.3
hutool: 5.8.37
sa-token: 1.43.0
redisson: 4.0.0
rocketmq: 2.3.5
okhttp: 4.12.0
```

### 配置架构

系统采用分层配置管理：

```mermaid
graph TB
    subgraph "配置层次"
        A[application.yaml] --> B[基础配置]
        C[rag/config/] --> D[业务配置]
        E[resources/] --> F[静态配置]
        G[环境变量] --> H[动态配置]
    end
    
    B --> I[数据库连接]
    B --> J[服务端口]
    D --> K[RAG 参数]
    D --> L[模型配置]
    D --> M[检索配置]
    F --> N[提示词模板]
    H --> O[API密钥]
```

## 可观测性与运维

### 链路追踪系统

系统实现了完整的链路追踪机制：

```mermaid
graph LR
    A[请求入口] --> B[生成TraceID]
    B --> C[各节点传递]
    C --> D[记录关键指标]
    D --> E[聚合分析]
    E --> F[可视化展示]
    
    D --> G[响应时间]
    D --> H[错误率]
    D --> I[调用链]
    D --> J[资源使用]
```

### 监控指标

| 指标类型 | 监控内容 | 告警规则 |
|----------|----------|----------|
| **性能指标** | 响应时间、QPS | > 2s 告警 |
| **错误指标** | 错误率、异常数 | > 5% 告警 |
| **资源指标** | CPU、内存使用 | > 80% 告警 |
| **业务指标** | 检索准确率、用户满意度 | < 90% 告警 |

## 扩展设计

### 插件化扩展机制

系统支持多种扩展方式：

1. **检索通道扩展**
   - 实现 `SearchChannel` 接口
   - 注册为 Spring Bean
   - 配置启用条件

2. **后置处理器扩展**
   - 实现 `SearchResultPostProcessor` 接口
   - 定义处理优先级
   - 配置处理条件

3. **文档解析扩展**
   - 实现 `DocumentParser` 接口
   - 注册解析器类型
   - 配置 MIME 类型映射

4. **AI 模型扩展**
   - 实现 `ModelClient` 接口
   - 配置模型参数
   - 设置路由策略

### 部署架构

系统支持多种部署模式：

```mermaid
graph TB
    subgraph "单体部署"
        A[应用服务器] --> B[PostgreSQL]
        A --> C[Redis]
        A --> D[Milvus]
        A --> E[MinIO]
    end
    
    subgraph "微服务部署"
        F[RAG 服务] --> G[PostgreSQL]
        F --> H[Redis]
        I[入库服务] --> G
        I --> J[消息队列]
        K[MCP 服务] --> L[工具存储]
    end
    
    subgraph "容器化部署"
        M[Docker Compose] --> N[应用容器]
        M --> O[数据库容器]
        M --> P[缓存容器]
        M --> Q[向量库容器]
    end
```

## 总结

Ragent 系统通过模块化的分层架构设计，实现了高性能、可扩展的企业级 RAG 解决方案。其核心优势包括：

1. **灵活的多通道检索架构**，支持不同场景的检索需求
2. **智能的模型路由机制**，确保服务可用性和性能
3. **完整的文档处理流水线**，支持多种文档格式和来源
4. **强大的可观测性能力**，便于运维和问题定位
5. **开放的扩展机制**，支持定制化功能开发

系统设计充分考虑了企业级应用的需求，在性能、可靠性、可维护性等方面进行了充分优化，能够满足复杂的业务场景需求。