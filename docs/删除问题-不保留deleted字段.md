# 知识库当前为deleted字段可恢复删除数据，如下为不保留

## 1. 问题概述

在 `http://localhost:5173/admin/knowledge` 的知识库管理页面中，手动删除所有知识库及其文档后，数据库表 `t_knowledge_base`、`t_knowledge_document`、`t_knowledge_chunk` 中仍残留已删除数据。随后再次创建相同 `collection_name` 的知识库时，会触发数据库唯一约束 `uk_collection_name` 冲突，导致创建失败。

## 2. 影响范围

- 删除知识库后，数据库仍保留逻辑删除记录，无法真正释放 `collection_name`
- 删除文档后，数据库仍保留文档和分块残留记录
- 在 Milvus 模式下，知识库对应的 collection 不会被删除
- 在 S3/RestFS 模式下，知识库存储桶不会被删除
- 在 PostgreSQL pgvector 模式下，`t_knowledge_vector` 中可能残留同 `collection_name` 的历史向量数据

## 3. 根因溯源

### 3.1 直接原因

`KnowledgeBaseDO`、`KnowledgeDocumentDO`、`KnowledgeChunkDO` 三个实体都使用了 `@TableLogic`。因此：

- `knowledgeBaseMapper.deleteById(...)`
- `documentMapper.deleteById(...)`
- `chunkMapper.delete(...)`

执行的都是逻辑删除，而不是物理删除。

### 3.2 为什么会导致本次报错

逻辑删除只会把 `deleted` 字段更新为 `1`，数据库记录仍然存在；而数据库唯一索引 `uk_collection_name` 并不会因为逻辑删除自动失效。因此旧知识库记录虽然在业务页面不可见，但在数据库层面仍占用原有 `collection_name`，新建同名知识库时必然触发唯一键冲突。

### 3.3 隐含风险

排查删除链路时还发现，知识库创建过程不仅会写数据库，还会创建外部资源：

- S3/RestFS 存储桶
- 向量空间（Milvus collection 或 pgvector 对应数据）

而原删除流程没有做对应清理。即使只修复数据库删除，后续仍可能因为外部资源残留而再次阻塞重建。

## 4. 修复方案

### 4.1 数据库删除改为物理删除

新增 Mapper 物理删除方法：

- `KnowledgeBaseMapper.deletePhysicallyById`
- `KnowledgeDocumentMapper.deletePhysicallyById`
- `KnowledgeChunkMapper.deletePhysicallyByDocId`

通过显式 `DELETE FROM ...` SQL 绕过 `@TableLogic`，确保真正清除残留记录。

### 4.2 调整删除链路

#### 文档删除

`KnowledgeDocumentServiceImpl.delete`

- 先删除文档下所有 chunk
- 删除调度记录和分块日志
- 物理删除 `t_knowledge_document`
- 删除该文档对应向量
- 删除文档文件

#### 分块删除

`KnowledgeChunkServiceImpl.deleteByDocId`

- 改为直接物理删除 `t_knowledge_chunk` 中指定文档的全部分块记录

#### 知识库删除

`KnowledgeBaseServiceImpl.delete`

- 先校验知识库下已无文档
- 物理删除 `t_knowledge_base`
- 删除知识库存储桶
- 删除对应向量空间

### 4.3 外部资源清理补齐

新增 `VectorStoreAdmin.deleteVectorSpace(VectorSpaceId)`：

- `MilvusVectorStoreAdmin`：实际执行 `dropCollection`
- `PgVectorStoreAdmin`：清理 `t_knowledge_vector` 中指定 `collection_name` 的残留向量

同时在知识库删除时补充 S3 bucket 删除，避免数据库清了但对象存储和向量空间还占着原名字。

## 5. 修复文件

- `bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/dao/mapper/KnowledgeBaseMapper.java`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/dao/mapper/KnowledgeDocumentMapper.java`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/dao/mapper/KnowledgeChunkMapper.java`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/service/impl/KnowledgeBaseServiceImpl.java`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/service/impl/KnowledgeDocumentServiceImpl.java`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/service/impl/KnowledgeChunkServiceImpl.java`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/vector/VectorStoreAdmin.java`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/vector/MilvusVectorStoreAdmin.java`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/vector/PgVectorStoreAdmin.java`
- `bootstrap/src/test/java/com/nageoffer/ai/ragent/knowledge/service/impl/KnowledgeDeletionCleanupTest.java`

## 6. 修复后的测试过程

### 6.1 单元测试设计

新增 `KnowledgeDeletionCleanupTest`，覆盖以下关键场景：

1. 删除知识库时，确认执行物理删除，并同步清理 S3 bucket 与向量空间
2. 知识库下仍有文档时，删除应被拒绝，避免误删
3. 删除文档时，确认执行物理删除，并同步删除 chunk、向量、文件和相关日志
4. 按文档删除 chunk 时，确认走物理删除 SQL

### 6.2 预期验证点

- `deletePhysicallyById` / `deletePhysicallyByDocId` 被实际调用
- 不再调用逻辑删除路径
- 删除链路能覆盖数据库、向量层、对象存储层三类资源
- 重新创建同 `collection_name` 的知识库时，不会再被历史残留记录阻塞

### 6.3 建议人工回归步骤

1. 启动后端与前端服务
2. 在管理后台新建一个知识库，例如 `collection_name=productdocs`
3. 上传 1 个或多个文档并完成分块
4. 在页面中依次删除该知识库下全部文档
5. 删除知识库
6. 检查数据库：
   - `t_knowledge_base` 不应存在该知识库记录
   - `t_knowledge_document` 不应存在该知识库文档记录
   - `t_knowledge_chunk` 不应存在对应 chunk 记录
7. 如使用 pgvector，检查 `t_knowledge_vector` 中对应 `collection_name` 数据已被清理
8. 如使用 Milvus，检查对应 collection 已被删除
9. 如使用 S3/RestFS，检查对应 bucket 已被删除
10. 使用同一个 `collection_name` 再次创建知识库，确认创建成功

## 7. 风险说明

- 本次修复将“知识库/文档/分块删除”从逻辑删除切换为物理删除，意味着这些数据删除后无法再通过 `deleted` 字段恢复
- 该行为与当前业务诉求一致，因为 `collection_name`、向量空间和存储桶都要求删除后真实释放资源
- 若后续产品需要“回收站”能力，应重新设计唯一键策略和资源回收策略，而不是继续复用当前删除语义

## 8. 结论

本次问题的本质是“业务语义要求真实删除，但代码实现采用了逻辑删除”，导致数据库唯一键和外部资源均未真正释放。修复后，删除链路已覆盖：

- 业务主表数据
- 文档与分块子表数据
- 向量存储残留数据
- 对象存储桶资源

这样可以从根本上解决“删除后无法用同一 `collection_name` 重建知识库”的问题。
