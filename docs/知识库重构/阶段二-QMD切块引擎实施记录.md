# 阶段二：QMD 切块引擎实施记录

## 1. 阶段目标

阶段二的目标是先解决“切块不智能、标题和正文容易被切断”的问题，同时保持第一阶段已经打通的 `docType -> orchestrator -> metadata` 主链路不倒退。

本阶段的落地目标：

1. 在现有编排器里引入 `qmd_smart`，作为新的默认切块策略。
2. 用本地脚本适配 `@tobilu/qmd`，避免 Java 直接耦合 QMD 的内部 API 或假定某个 CLI 子命令永久稳定。
3. 为 QMD 增加超时、异常、空结果兜底，并自动回退到现有 `structure_aware`。
4. 把前端上传/编辑页面切换到 `qmd_smart` 默认值，并补齐 QMD 参数面板。

---

## 2. 本阶段实际改造内容

## 2.1 新增 `qmd_smart` 策略与类型安全配置

新增后端配置类型：

- `QmdSmartOptions`
- `ChunkingMode.QMD_SMART`

默认参数为：

- `maxChars = 3600`
- `overlapChars = 540`
- `windowChars = 800`

这样做的目的不是替换掉已有 `fixed_size / structure_aware`，而是把三种策略都保留下来，方便灰度、回退和排障。

## 2.2 Java 侧通过 `ProcessBuilder` 调用 QMD 适配脚本

阶段二首轮先沿用第一阶段的轻量编排器接入 QMD，100% 补齐时已把切块能力抽成 `DocumentChunkEngine`，让 Java 主链路不再直接写死 QMD 分支。

新增：

- `KnowledgeQmdProperties`
- `QmdProcessClient`
- `scripts/qmd/chunker.mjs`
- `scripts/qmd/package.json`

当前实际调用链路：

```text
KnowledgeDocumentIngestionOrchestrator
  -> DocumentChunkEngine
  -> QmdDocumentChunkEngine
  -> QmdProcessClient
  -> node scripts/qmd/chunker.mjs
  -> @tobilu/qmd
```

选择这种方式的原因：

1. Java 与 QMD 的耦合点被限制在 JSON 输入输出。
2. 即使 QMD 后续调整内部导出路径，主要只需要改脚本，不需要再改 Java 主链路。
3. 当前环境已经具备 Node 22，直接用 Node 适配脚本比额外引入 Bun 运行时更容易落地和验证。

## 2.3 `chunker.mjs` 做了三层解析定位

适配脚本会按下面顺序寻找 QMD 包：

1. 环境变量 `QMD_PACKAGE_DIR`
2. `scripts/qmd/node_modules/@tobilu/qmd`
3. 当前 Node 模块解析结果 `require.resolve("@tobilu/qmd")`

脚本读取 stdin JSON，请求字段包括：

- `text`
- `fileName`
- `maxChars`
- `overlapChars`
- `windowChars`
- `chunkStrategy`

脚本输出统一 JSON：

```json
{
  "engine": "qmd",
  "chunks": [
    {
      "index": 0,
      "text": "chunk content",
      "position": 0
    }
  ]
}
```

这让 Java 端始终只需要关心协议，不需要感知 QMD 的内部对象结构。

## 2.4 新增独立 `DocumentChunkEngine` 边界

阶段二 100% 补齐时，把首轮落地中写在 `KnowledgeDocumentIngestionOrchestrator` 里的切块分支拆成了独立引擎边界：

- `DocumentChunkEngine`
- `ChunkEngineResult`
- `DefaultDocumentChunkEngine`
- `QmdDocumentChunkEngine`

现在 orchestrator 只负责：

- 文档类型路由
- 解析器选择
- 调用切块引擎
- 统一补齐 chunk id / index
- 注入 metadata
- 调用 embedding

具体切块职责下沉到引擎：

- `DefaultDocumentChunkEngine`
  - 兼容 `fixed_size / structure_aware`
- `QmdDocumentChunkEngine`
  - 负责 `qmd_smart`
  - 负责 QMD 失败后的 `structure_aware` 回退

这样第三阶段继续接入更强解析器时，不需要再把 QMD 细节塞回编排器。

## 2.5 编排器保持“QMD 优先，结构感知回退”

`KnowledgeDocumentIngestionOrchestrator` 现在的切块逻辑变为：

1. 如果策略是 `qmd_smart`，先调用 `QmdProcessClient`
2. 如果 QMD 返回成功，直接进入 embedding
3. 如果 QMD 报错、超时、返回空结果，自动回退为 `structure_aware`

回退时会根据 `QmdSmartOptions` 推导一份 `TextBoundaryOptions`，保证：

- 用户配置的 `maxChars / overlapChars` 不会完全丢失
- 回退策略仍然尽量接近 QMD 的目标块尺度

## 2.5.1 运行期修正：避免 QMD 子进程超时误判

在第二阶段首轮联调后，发现少量文档会出现下面这种现象：

- Java 侧等待 30 秒后判定超时
- 但脚本本身并不一定真的卡在 QMD 算法上

为降低误判，本次补了两项修正：

1. `QmdProcessClient` 改为并发消费子进程的 `stdout / stderr`
2. 默认 `timeoutMs` 从 `30000` 提升到 `120000`

这样可以同时覆盖两类风险：

- 子进程输出较多时的管道阻塞
- 首次加载依赖或较大 Markdown 文档时的慢启动

## 2.5.2 运行期修正：补齐外部切块结果的 `chunkId`

在第二阶段真实联调时又发现一条仅出现在 QMD 路径的问题：

- `QmdProcessClient` 返回的 `VectorChunk` 带有 `content / index / metadata`
- 但没有补 `chunkId`
- PostgreSQL 向量表 `t_knowledge_vector.id` 是非空字段，因此会直接报：
  - `null value in column "id" of relation "t_knowledge_vector"`

本次修正做成了两层保险：

1. `QmdProcessClient` 在映射 QMD 结果时直接生成 `chunkId`
2. `KnowledgeDocumentIngestionOrchestrator` 在进入 embedding / 持久化前统一兜底，凡是缺失的 `chunkId / index` 都会补齐

这样后面即使接入新的外部切块器，只要它漏掉块标识，也不会再把 `null id` 带进向量库。

## 2.5.3 协议修正：校验 QMD 引擎标识

阶段二 100% 补齐时，`QmdProcessClient` 增加了协议校验：

- 适配脚本必须返回 `engine = "qmd"`
- chunks 不能为空
- 每个有效 chunk 必须有非空文本

这样可以避免错误脚本、旧脚本或调试输出被 Java 侧误认为是 QMD 结果。

## 2.6 新增 `chunk_engine / chunk_fallback` metadata

第二阶段除了继续保留第一阶段的：

- `doc_type`
- `source_type`
- `ingest_source_type`
- `chunk_strategy`
- `parser_type`

还额外写入了：

- `chunk_engine`
- `chunk_fallback`

含义如下：

- `chunk_strategy`
  - 表示用户请求的策略，例如 `qmd_smart`
- `chunk_engine`
  - 表示实际执行的切块器，例如 `qmd` 或 `structure_aware`
- `chunk_fallback`
  - 表示本次是否发生了回退

这样以后排查“用户明明选了 QMD，为什么效果像旧切块器”时，就能直接从 metadata 看出真实执行结果。

## 2.7 上传与编辑页面已切到 `qmd_smart` 默认值

前端页面同步完成了三类改造：

1. 文档列表支持显示 `QMD 智能切分`
2. 上传弹窗默认策略从 `fixed_size` 改为 `qmd_smart`
3. 上传/编辑弹窗的参数面板改成三分支：
   - `fixed_size`
   - `qmd_smart`
   - `structure_aware`

QMD 分支展示的配置项为：

- `maxChars`
- `overlapChars`
- `windowChars`

同时修复了一个旧问题：

- 编辑弹窗保存参数时，手动输入 `0` 不会再被错误回退成默认值

## 2.8 默认策略已切换

后端 `KnowledgeDocumentServiceImpl.resolveChunkProcessConfig()` 的默认策略现在是：

- `qmd_smart`

也就是说，截至 **2026-04-26**，新上传文档如果前端未显式传入策略，将默认走第二阶段的 QMD 路径。

---

## 3. 涉及文件

## 3.1 后端

- `bootstrap/src/main/java/com/nageoffer/ai/ragent/core/chunk/ChunkingMode.java`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/core/chunk/ChunkingOptions.java`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/core/chunk/QmdSmartOptions.java`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/config/KnowledgeQmdProperties.java`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/service/impl/KnowledgeDocumentServiceImpl.java`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/service/ingest/KnowledgeDocumentIngestionOrchestrator.java`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/service/ingest/QmdProcessClient.java`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/service/ingest/chunk/DocumentChunkEngine.java`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/service/ingest/chunk/ChunkEngineResult.java`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/service/ingest/chunk/DefaultDocumentChunkEngine.java`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/service/ingest/chunk/QmdDocumentChunkEngine.java`
- `bootstrap/src/main/resources/application.yaml`

## 3.2 前端

- `frontend/src/pages/admin/knowledge/KnowledgeDocumentsPage.tsx`

## 3.3 脚本

- `scripts/qmd/package.json`
- `scripts/qmd/package-lock.json`
- `scripts/qmd/chunker.mjs`
- `scripts/qmd/smoke-request.json`

## 3.4 测试

- `bootstrap/src/test/java/com/nageoffer/ai/ragent/knowledge/service/ingest/KnowledgeDocumentIngestionOrchestratorTest.java`
- `bootstrap/src/test/java/com/nageoffer/ai/ragent/knowledge/service/ingest/QmdProcessClientTest.java`
- `bootstrap/src/test/java/com/nageoffer/ai/ragent/knowledge/service/impl/KnowledgeDeletionCleanupTest.java`
- `bootstrap/src/test/java/com/nageoffer/ai/ragent/rag/core/retrieve/PgRetrieverServiceTest.java`
- `bootstrap/src/test/java/com/nageoffer/ai/ragent/rag/core/retrieve/MilvusRetrieverServiceTest.java`

---

## 4. 配置与使用说明

## 4.1 应用配置

`application.yaml` 已新增：

```yaml
rag:
  knowledge:
    qmd:
      enabled: true
      command: node
      script-path: scripts/qmd/chunker.mjs
      chunk-strategy: regex
      timeout-ms: 120000
```

如需指定 QMD 安装目录，可补充：

```yaml
rag:
  knowledge:
    qmd:
      package-dir: D:/tools/qmd/node_modules/@tobilu/qmd
```

## 4.2 安装脚本依赖

仓库内脚本目录只包含适配层，不默认提交 `node_modules`。

如果要在本仓库本地直接运行脚本，执行：

```bash
cd scripts/qmd
npm install
```

如果不希望在仓库内安装依赖，也可以像本次测试一样：

1. 在临时目录安装 `@tobilu/qmd`
2. 通过 `QMD_PACKAGE_DIR` 指向安装目录

---

## 5. 测试记录

## 5.1 前端构建验证

执行时间：

- 2026-04-26

执行命令：

```bash
cd frontend
npm run build
```

结果：

- `vite build` 成功
- 无新增 TypeScript / JSX 构建错误
- 仍只有既有的大包体积 warning

## 5.2 后端定向测试

### 测试前说明

继续沿用仓库既有规避方式：

- `pom.xml` 中 surefire 的 `@{argLine}` 占位符在 fork 模式下仍会导致失败
- 本次继续使用 `-DforkCount=0`

### 执行命令

```powershell
.\mvnw.cmd -pl bootstrap -am "-Dtest=KnowledgeDeletionCleanupTest,KnowledgeDocumentIngestionOrchestratorTest,QmdProcessClientTest,PgRetrieverServiceTest,MilvusRetrieverServiceTest,ParallelRetrieverMetadataFilterTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dspotless.apply.skip=true" "-Dspotless.check.skip=true" "-DforkCount=0" test
```

### 结果

- `BUILD SUCCESS`
- 测试总数：`16`
- 失败：`0`
- 错误：`0`
- 跳过：`0`

覆盖的测试点：

1. `KnowledgeDocumentIngestionOrchestratorTest`
   - 验证 `general` 文档仍可走 MIME 路由
   - 验证 `qmd_smart` 正常命中 QMD 分支
   - 验证 QMD 异常时自动回退 `structure_aware`
   - 验证 `chunk_engine / chunk_fallback` metadata 写入
2. `QmdProcessClientTest`
   - 验证进程命令、脚本路径、环境变量拼装正确
   - 验证 stdin/stdout JSON 协议可以被正确映射为 `VectorChunk`
   - 验证大量 `stderr` 输出不会阻塞 Java 侧等待逻辑
   - 验证适配脚本返回非 `qmd` 引擎标识时会被拒绝
3. `KnowledgeDeletionCleanupTest`
   - 验证新增 QMD 相关改造后，既有删除清理逻辑未受影响
4. `PgRetrieverServiceTest / MilvusRetrieverServiceTest`
   - 回归验证第一阶段 metadata 过滤能力未回退
5. `ParallelRetrieverMetadataFilterTest`
   - 回归验证上层检索 metadataFilters 透传能力未回退

## 5.3 QMD 适配脚本冒烟验证

执行时间：

- 2026-04-26

验证方式：

1. 在系统临时目录安装官方 `@tobilu/qmd@2.1.0`
2. 通过 `QMD_PACKAGE_DIR` 指向该安装目录
3. 直接运行仓库内 `scripts/qmd/chunker.mjs`
4. 新增固定冒烟命令：

```bash
cd scripts/qmd
npm run smoke
```

结果：

- 脚本成功返回 JSON
- 输出包含 `engine = "qmd"`
- 返回的 `chunks[0].text` 与输入文档内容一致

结论：

- 第二阶段不是只在 mock 环境中成立，当前仓库脚本已经能真实驱动官方 QMD 包

---

## 6. 当前结论

第二阶段已经达到 100% 可验收状态，本轮在这里暂停，等待人工确认后再继续第三阶段：

1. 默认切块策略已升级为 `qmd_smart`
2. Java 主链路已经具备独立 `DocumentChunkEngine` 边界
3. QMD 已具备“外部脚本切块 + 协议校验 + 自动回退”的稳定入口
4. QMD 的真实执行结果已经能通过 metadata 和文档表/日志字段回传
5. 前端、后端、脚本、测试与实施文档已经同步

---

## 7. 暂停点与下一阶段输入

按当前协作节奏，阶段二完成后暂停。用户手动查看效果并确认后，再开始第三阶段双路专轨。

第三阶段建议直接在现有 orchestrator 上继续演进：

1. `academic_paper` 接入 `MinerU`
2. `project_report` 接入 `Docling`
3. `general` 保持 `Tika / Markdown` 兜底
4. 在解析结果中补齐页码、标题路径、表格和公式相关 metadata
