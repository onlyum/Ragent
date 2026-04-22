本文档详细介绍 Ragent 系统的数据库初始化配置过程，为初学者提供完整的数据库搭建指导。

## 系统概览

Ragent 系统采用 PostgreSQL 作为主数据库，集成向量存储能力，支持完整的 RAG（检索增强生成）功能。

### 核心组件

| 组件 | 功能 | 配置文件 |
|------|------|----------|
| PostgreSQL | 主数据库，存储用户、对话、知识库等数据 | `resources/database/schema_pg.sql` |
| Vector Storage | pgvector 扩展，支持向量检索 | `t_knowledge_vector` 表 |
| 连接池 | HikariCP，提供高性能数据库连接 | `application.yaml` |

## 数据库环境准备

### 系统要求

- **操作系统**: Windows/Linux/macOS
- **数据库**: PostgreSQL 13+
- **内存**: 建议 2GB+
- **存储**: 建议 10GB+

### PostgreSQL 安装

#### Windows 安装

1. 下载 PostgreSQL 安装包：[PostgreSQL 官方下载](https://www.postgresql.org/download/windows/)
2. 运行安装程序，记住设置密码（默认：postgres）
3. 确保端口 5432 开启

#### Linux 安装

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install postgresql postgresql-contrib

# CentOS/RHEL
sudo yum install postgresql-server postgresql-contrib
sudo postgresql-setup initdb
sudo systemctl start postgresql
```

#### macOS 安装

```bash
# 使用 Homebrew
brew install postgresql
brew services start postgresql
```

### 数据库创建

```sql
-- 连接到 PostgreSQL
psql -U postgres

-- 创建数据库
CREATE DATABASE ragent;
CREATE USER ragent_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE ragent TO ragent_user;
```

## 数据库初始化脚本

### 1. 数据库架构初始化

执行 PostgreSQL 架构脚本创建所有必要的表结构：

```bash
# 连接到数据库并执行脚本
psql -U postgres -d ragent -f resources/database/schema_pg.sql
```

**架构包含的主要表：**

| 表名 | 功能说明 | 索引 |
|------|----------|------|
| `t_user` | 用户管理 | 用户名唯一索引 |
| `t_conversation` | 会话管理 | 用户+时间索引 |
| `t_message` | 消息记录 | 会话+用户索引 |
| `t_knowledge_base` | 知识库定义 | 名称索引 |
| `t_knowledge_document` | 知识库文档 | 知识库ID索引 |
| `t_knowledge_chunk` | 文档分块 | 文档ID索引 |
| `t_knowledge_vector` | 向量存储 | 元数据GIN索引，向量HNSW索引 |
| `t_rag_trace_run` | 链路追踪 | 任务ID索引 |
| `t_ingestion_pipeline` | 摄取流水线 | 名称唯一索引 |

### 2. 向量扩展启用

PostgreSQL 需要启用 pgvector 扩展以支持向量存储：

```sql
-- 在数据库中启用向量扩展
CREATE EXTENSION IF NOT EXISTS vector;
```

### 3. 初始数据导入

执行初始数据脚本创建默认管理员账户：

```bash
psql -U postgres -d ragent -f resources/database/init_data_pg.sql
```

**默认管理员账户：**

| 字段 | 值 |
|------|-----|
| 用户名 | admin |
| 密码 | admin |
| 角色 | admin |
| 头像 | 系统默认头像 |

## 连接池配置

### HikariCP 配置详解

系统使用 HikariCP 作为连接池，配置如下：

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    type: com.zaxxer.hikari.HikariDataSource
    username: postgres
    password: postgres
    url: jdbc:postgresql://127.0.0.1:5432/ragent?client_encoding=UTF8
    hikari:
      connection-timeout: 5000        # 连接超时时间（毫秒）
      idle-timeout: 600000           # 空闲连接超时（毫秒）
      max-lifetime: 1800000          # 连接最大生命周期（毫秒）
      maximum-pool-size: 10          # 最大连接数
      minimum-idle: 5                # 最小空闲连接数
      pool-name: RagentHikariPool     # 连接池名称
```

**连接池参数说明：**

| 参数 | 说明 | 推荐值 |
|------|------|--------|
| `connection-timeout` | 获取连接超时时间 | 5000ms |
| `idle-timeout` | 空闲连接超时时间 | 600000ms (10分钟) |
| `max-lifetime` | 连接最大生命周期 | 1800000ms (30分钟) |
| `maximum-pool-size` | 最大连接数 | 根据应用负载调整 |
| `minimum-idle` | 最小空闲连接数 | 保持一定数量连接 |

## 数据库架构演进

### 版本升级

系统提供了数据库升级脚本支持版本演进：

```bash
# 执行 v1.0 到 v1.1 的升级
psql -U postgres -d ragent -f resources/database/upgrade_v1.0_to_v1.1.sql
```

**升级内容：**
- 重命名 `embedding_duration` 为 `embed_duration`
- 新增 `persist_duration` 字段用于记录持久化耗时

### 备份与恢复

#### 数据库备份

```bash
# 完整备份
pg_dump -U postgres -d ragent > ragent_backup_$(date +%Y%m%d).sql

# 自定义格式备份（推荐）
pg_dump -U postgres -d ragent -Fc -f ragent_backup_$(date +%Y%m%d).dump
```

#### 数据库恢复

```bash
# 从 SQL 文件恢复
psql -U postgres -d ragent < ragent_backup_20241201.sql

# 从自定义格式文件恢复
pg_restore -U postgres -d ragent -v ragent_backup_20241201.dump
```

## 常见问题排查

### 连接失败

**问题现象：** 应用无法连接到数据库

**排查步骤：**
1. 检查 PostgreSQL 服务是否启动
2. 验证数据库用户名和密码
3. 确认数据库端口 5432 是否开放
4. 检查防火墙设置

```bash
# 检查 PostgreSQL 服务状态
systemctl status postgresql

# 测试数据库连接
psql -U postgres -d ragent
```

### 权限问题

**问题现象：** 表操作权限不足

**解决方案：**
```sql
-- 授予用户必要权限
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ragent_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ragent_user;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO ragent_user;
```

### 向量扩展未安装

**问题现象：** 向量存储相关操作报错

**解决方案：**
```sql
-- 确保向量扩展已安装
SELECT * FROM pg_extension WHERE extname = 'vector';

-- 如未安装，则执行
CREATE EXTENSION IF NOT EXISTS vector;
```

## 部署建议

### 开发环境配置

对于开发环境，建议配置：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ragent?client_encoding=UTF8
    username: postgres
    password: postgres
    hikari:
      maximum-pool-size: 5
      minimum-idle: 2
```

### 生产环境配置

对于生产环境，建议配置：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://db-prod.example.com:5432/ragent?client_encoding=UTF8&sslmode=require
    username: ragent_prod
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      max-lifetime: 1800000
      leak-detection-threshold: 15000
```

## 性能优化

### 索引优化

系统已包含必要的索引，但可根据实际查询需求添加额外索引：

```sql
-- 添加复合索引以提高查询性能
CREATE INDEX idx_conversation_user_status ON t_conversation (user_id, deleted);
CREATE INDEX idx_message_conversation_time ON t_message (conversation_id, create_time);
```

### 连接池调优

根据应用负载调整连接池大小：

```yaml
hikari:
  maximum-pool-size: ${DB_POOL_SIZE:20}  # 根据并发量调整
  minimum-idle: ${DB_MIN_IDLE:5}        # 保持一定数量空闲连接
```

## 验证安装

### 完整性检查

执行以下 SQL 验证数据库安装是否成功：

```sql
-- 检查表数量
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';

-- 检查向量扩展
SELECT * FROM pg_extension WHERE extname = 'vector';

-- 检查管理员用户
SELECT * FROM t_user WHERE role = 'admin';
```

### 功能测试

启动应用后访问 API 接口验证数据库连接：

```bash
# 检查应用健康状态
curl http://localhost:9090/api/ragent/actuator/health

# 测试用户登录
curl -X POST http://localhost:9090/api/ragent/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

_sources: [resources/database/schema_pg.sql](resources/database/schema_pg.sql), [resources/database/init_data_pg.sql](resources/database/init_data_pg.sql), [resources/database/upgrade_v1.0_to_v1.1.sql](resources/database/upgrade_v1.0_to_v1.1.sql), [bootstrap/src/main/resources/application.yaml](bootstrap/src/main/resources/application.yaml)_