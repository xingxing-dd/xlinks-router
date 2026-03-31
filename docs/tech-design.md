# xlinks-router 技术设计总览

## 1. 项目说明

xlinks-router 是一个统一的大模型网关平台，目标是对外提供兼容 OpenAI 风格的标准调用入口，对内提供 Provider、模型、路由、Token 与客户接入能力的统一管理。

当前仓库主要包含三个后端模块：

- `xlinks-router-admin`：管理后台 API，负责 Provider、Model Endpoint、Model、Token 等管理能力
- `xlinks-router-api`：对外 OpenAPI 网关，负责客户鉴权、模型路由、Provider 调用与 usage 记录
- `xlinks-router-common`：公共实体、枚举、统一返回结构与共享基础组件

## 2. 技术栈

### 2.1 后端技术栈

| 类别 | 技术选型 | 版本 | 说明 |
|------|----------|------|------|
| 编程语言 | Java | 11 (LTS) | 稳定、生态成熟 |
| 开发框架 | Spring Boot | 3.2.x | 主应用框架 |
| Web 框架 | Spring Web | - | REST API 开发 |
| ORM | MyBatis-Plus | 3.5.x | 数据访问与 CRUD |
| 构建工具 | Maven | 3.9.x | 多模块构建 |
| 接口文档 | SpringDoc OpenAPI | 2.x | OpenAPI 文档生成 |
| 参数校验 | Hibernate Validator | - | Bean Validation |
| HTTP 客户端 | OkHttp | 4.x | 下游 Provider 调用 |
| JSON | Jackson | - | JSON 序列化/反序列化 |
| 日志 | SLF4J + Logback | - | 应用日志 |

### 2.2 基础设施

| 组件 | 版本 | 说明 |
|------|------|------|
| MySQL | 8.0+ | 主数据库 |
| Redis | 7.0+ | 缓存、限流、会话等扩展能力 |

## 3. 系统定位

### 3.1 对外能力

- 提供统一标准 API
- 使用 Customer Token 完成平台级授权
- 屏蔽底层 Provider 差异
- 提供模型路由与标准化响应

### 3.2 对内能力

- 管理 Provider
- 管理 Model Endpoint
- 管理 Model
- 管理 Provider Token / Customer Token
- 支撑路由、鉴权、记录与运营分析

## 4. 核心模型设计

当前核心模型统一调整为三级结构：

- `providers`：服务商
- `model_endpoints`：模型端点/能力分组
- `models`：统一模型表

设计原则：

- 不再区分 `customer_model`、`provider_model`
- 不再保留 `model_mapping`
- OpenAPI 请求中的 `model` 字段直接对应 `models.model_code`
- 一个 Model 必须归属于一个 Provider 和一个 Model Endpoint
- 同一个 `endpointId` 下，`modelCode` 唯一

## 5. 逻辑删除规范

Provider、Model Endpoint、Model 三类核心模型统一采用逻辑删除机制。

### 5.1 字段规范

- 逻辑删除字段统一命名为：`deleted`
- 字段类型：`TINYINT`
- 默认值：`0`
- 字段语义：
  - `0`：未删除
  - `1`：已删除

### 5.2 适用范围

以下表必须包含逻辑删除字段：

- `providers`
- `model_endpoints`
- `models`

### 5.3 查询规范

- 列表查询默认追加 `deleted = 0`
- 详情查询默认仅允许读取 `deleted = 0`
- 下拉选项、联表查询、路由解析默认仅使用 `deleted = 0`
- OpenAPI 模型解析必须同时满足：
  - `deleted = 0`
  - `status = 1`

### 5.4 删除规范

- 管理后台删除操作统一执行逻辑删除，不做物理删除
- 删除接口语义为将 `deleted` 更新为 `1`
- 已逻辑删除数据默认对前端不可见、对路由不可用

### 5.5 唯一性与约束规范

- 唯一键冲突校验默认基于 `deleted = 0` 的可见数据集
- 模型唯一性规则保持为：同一个 `endpointId` 下，`modelCode` 唯一
- 业务层新增、编辑、恢复时，都要考虑逻辑删除记录带来的唯一性冲突问题

## 6. 工程结构

```text
xlinks-router/
├── pom.xml
├── docs/
│   ├── tech-design.md
│   ├── admin-api.md
│   ├── openapi.md
│   ├── model.md
│   ├── db-script.md
│   └── client-api.md
├── xlinks-router-admin/
├── xlinks-router-api/
├── xlinks-router-client/
├── xlinks-router-common/
└── xlinks-router-web/
```

## 7. 文档索引

### 7.1 总览与设计
- `docs/tech-design.md`：项目说明、技术栈、系统定位、核心模型设计与逻辑删除规范

### 7.2 接口文档
- `docs/admin-api.md`：管理后台 API 定义
- `docs/openapi.md`：对外 OpenAPI / 网关 API 定义
- `docs/client-api.md`：前端 client 侧接口定义

### 7.3 数据模型与脚本
- `docs/model.md`：核心数据模型、字段定义、关系说明
- `docs/db-script.md`：数据库初始化脚本、推荐 DDL 与相关脚本说明

## 8. 建议阅读顺序

1. 先阅读 `docs/tech-design.md` 了解项目边界、模型设计与逻辑删除规范
2. 阅读 `docs/model.md` 理解核心实体与字段定义
3. 按角色选择：
   - 管理后台开发看 `docs/admin-api.md`
   - 网关/OpenAPI 开发看 `docs/openapi.md`
   - 前端联调看 `docs/client-api.md`
4. 涉及数据库初始化和结构落表时，再阅读 `docs/db-script.md`

## Provider protocol & priority routing update

- Provider now includes `supported_protocols` and `priority`.
- `supported_protocols` is a comma-separated protocol list such as `chat/completions,responses`; empty means all protocols.
- `priority` is an integer; higher value means higher priority during routing.
- The same `model_code` can be configured on multiple providers under the same endpoint. Recommended uniqueness rule: `UNIQUE KEY (endpoint_id, model_code, provider_id)`.
- API routing should filter providers by request protocol first, then sort by provider priority.

