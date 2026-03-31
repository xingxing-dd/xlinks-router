# xlinks-router Admin API

本文档拆分自原 `docs/tech-design.md`，用于单独维护管理后台接口定义。

## 1. 统一约定

### 1.1 基础路径

管理类 API 统一以 `/admin` 作为前缀。

### 1.2 统一响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 1.3 分页响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "page": 1,
    "pageSize": 10
  }
}
```

### 1.4 逻辑删除约定

Provider、Model Endpoint、Model 三类核心模型统一采用逻辑删除，不执行物理删除。

- 逻辑删除字段：`deleted`
- 字段语义：`0-未删除`，`1-已删除`
- 列表、详情、下拉选项、关联查询默认仅返回 `deleted = 0` 的数据
- 删除接口语义为“逻辑删除”
- 唯一键冲突校验默认基于 `deleted = 0` 的可见数据集

## 2. Provider API

### 2.1 新增 Provider

- 接口：`POST /admin/providers`
- Content-Type：`application/json`

```json
{
  "providerCode": "deepseek",
  "providerName": "DeepSeek",
  "providerType": "openai-compatible",
  "baseUrl": "https://api.deepseek.com/v1",
  "providerLogo": "https://static.example.com/provider/deepseek.svg",
  "providerWebsite": "https://www.deepseek.com",
  "status": 1,
  "remark": "DeepSeek 模型提供商"
}
```

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "providerCode": "deepseek",
    "providerName": "DeepSeek",
    "providerType": "openai-compatible",
    "baseUrl": "https://api.deepseek.com/v1",
    "providerLogo": "https://static.example.com/provider/deepseek.svg",
    "providerWebsite": "https://www.deepseek.com",
    "status": 1,
    "deleted": 0,
    "remark": "DeepSeek 模型提供商",
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
}
```

### 2.2 Provider 列表

- 接口：`GET /admin/providers`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 10 |
| providerCode | string | 否 | 提供商编码（精确匹配） |
| providerName | string | 否 | 提供商名称（模糊匹配） |
| status | int | 否 | 状态：1-启用，0-禁用 |

返回项建议包含：`providerLogo`、`providerWebsite`，便于前端直接展示品牌信息。默认仅返回未逻辑删除数据。

### 2.3 Provider 详情

- 接口：`GET /admin/providers/{id}`

默认仅允许查询 `deleted = 0` 的记录。

### 2.4 更新 Provider

- 接口：`PUT /admin/providers/{id}`

```json
{
  "providerName": "DeepSeek AI",
  "baseUrl": "https://api.deepseek.com/v1",
  "providerLogo": "https://static.example.com/provider/deepseek.svg",
  "providerWebsite": "https://www.deepseek.com",
  "remark": "更新备注"
}
```

### 2.5 启用/禁用 Provider

- 接口：`PATCH /admin/providers/{id}/status`

```json
{
  "status": 0
}
```

### 2.6 删除 Provider

- 接口：`DELETE /admin/providers/{id}`

说明：执行逻辑删除，将 `deleted` 更新为 `1`。

## 3. Model Endpoint API

模型端点用于对模型能力分组，例如聊天、Embedding、图像生成等。一个模型必须归属于一个模型端点；同一个模型端点下，`modelCode` 唯一。

### 3.1 新增模型端点

- 接口：`POST /admin/model-endpoints`

```json
{
  "endpointName": "chat/completions",
  "endpointDesc": "聊天补全能力端点",
  "endpointUrl": "/v1/chat/completions",
  "status": 1
}
```

### 3.2 模型端点列表

- 接口：`GET /admin/model-endpoints`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 10 |
| endpointName | string | 否 | 端点名称（模糊匹配） |
| status | int | 否 | 状态：1-启用，0-禁用 |

默认仅返回未逻辑删除数据。

### 3.3 模型端点详情

- 接口：`GET /admin/model-endpoints/{id}`

默认仅允许查询 `deleted = 0` 的记录。

### 3.4 更新模型端点

- 接口：`PUT /admin/model-endpoints/{id}`

```json
{
  "endpointName": "chat/completions",
  "endpointDesc": "标准聊天补全端点",
  "endpointUrl": "/v1/chat/completions",
  "status": 1
}
```

### 3.5 启用/禁用模型端点

- 接口：`PATCH /admin/model-endpoints/{id}/status`

### 3.6 删除模型端点

- 接口：`DELETE /admin/model-endpoints/{id}`

说明：执行逻辑删除，将 `deleted` 更新为 `1`。

## 4. Model API

不再区分 `customer_model` 与 `provider_model`，统一收敛为单一模型表。模型直接绑定服务商与模型端点，满足展示、调用与管理需要。

### 4.1 新增模型

- 接口：`POST /admin/models`

```json
{
  "modelName": "DeepSeek V3",
  "modelCode": "deepseek-v3",
  "endpointId": 1,
  "providerId": 1,
  "modelDesc": "DeepSeek 对话模型",
  "inputPrice": 2.00,
  "outputPrice": 8.00,
  "contextSize": 128,
  "status": 1
}
```

### 4.2 模型列表

- 接口：`GET /admin/models`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 10 |
| modelCode | string | 否 | 模型编码（精确匹配） |
| modelName | string | 否 | 模型名称（模糊匹配） |
| endpointId | long | 否 | 模型端点 ID |
| providerId | long | 否 | 服务商 ID |
| status | int | 否 | 状态：1-启用，0-禁用 |

返回项建议包含：

- `endpointName`
- `providerName`
- `providerLogo`
- `providerWebsite`

默认仅返回未逻辑删除数据。

### 4.3 模型详情

- 接口：`GET /admin/models/{id}`

默认仅允许查询 `deleted = 0` 的记录。

### 4.4 更新模型

- 接口：`PUT /admin/models/{id}`

```json
{
  "modelName": "DeepSeek V3",
  "endpointId": 1,
  "providerId": 1,
  "modelDesc": "DeepSeek 官方对话模型",
  "inputPrice": 2.00,
  "outputPrice": 8.00,
  "contextSize": 128,
  "status": 1
}
```

### 4.5 启用/禁用模型

- 接口：`PATCH /admin/models/{id}/status`

### 4.6 删除模型

- 接口：`DELETE /admin/models/{id}`

说明：执行逻辑删除，将 `deleted` 更新为 `1`。

### 4.7 唯一性约束

- 同一个 `endpointId` 下，`modelCode` 必须唯一
- 推荐约束：`UNIQUE KEY uk_endpoint_model_code (endpoint_id, model_code)`
- 业务规则：新增与编辑校验时，仅以 `deleted = 0` 的模型作为唯一性冲突判断范围

## 5. Provider Token API

### 5.1 新增 Provider Token

- 接口：`POST /admin/provider-tokens`

```json
{
  "providerId": 1,
  "tokenName": "DeepSeek API Key",
  "tokenValue": "sk-xxxxxxxxxxxxxxxx",
  "tokenStatus": 1,
  "quotaTotal": 1000000,
  "expireTime": "2025-12-31T23:59:59Z",
  "remark": "DeepSeek 主账号 Token"
}
```

### 5.2 Token 列表

- 接口：`GET /admin/provider-tokens`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 10 |
| providerId | long | 否 | Provider ID |
| tokenStatus | int | 否 | Token 状态 |

注意：`tokenValue` 不应在列表中返回。

### 5.3 Token 详情

- 接口：`GET /admin/provider-tokens/{id}`

### 5.4 更新 Token

- 接口：`PUT /admin/provider-tokens/{id}`

### 5.5 启用/禁用 Token

- 接口：`PATCH /admin/provider-tokens/{id}/status`

## 6. Customer Token API

### 6.1 新增 Customer Token

- 接口：`POST /admin/customer-tokens`

```json
{
  "customerName": "示例客户",
  "tokenName": "客户主账号",
  "status": 1,
  "expireTime": "2025-12-31T23:59:59Z",
  "allowedModels": ["deepseek-v3", "gpt-4"],
  "remark": "示例客户 Token"
}
```

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "customerName": "示例客户",
    "tokenName": "客户主账号",
    "tokenValue": "xlr_ct_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    "status": 1,
    "expireTime": "2025-12-31T23:59:59Z",
    "allowedModels": ["deepseek-v3", "gpt-4"],
    "remark": "示例客户 Token",
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
}
```

重要：`tokenValue` 只在创建时返回一次，之后不再显示。

### 6.2 Token 列表

- 接口：`GET /admin/customer-tokens`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 10 |
| customerName | string | 否 | 客户名称（模糊匹配） |
| status | int | 否 | 状态 |

### 6.3 Token 详情

- 接口：`GET /admin/customer-tokens/{id}`

### 6.4 更新 Token

- 接口：`PUT /admin/customer-tokens/{id}`

### 6.5 启用/禁用 Token

- 接口：`PATCH /admin/customer-tokens/{id}/status`

## 7. 错误码约定

| 错误码 | 说明 | HTTP Status |
|--------|------|-------------|
| 0 | 成功 | 200 |
| 4001 | 参数错误 | 400 |
| 5000 | 系统异常 | 500 |
| 5001 | 外部服务调用失败 | 502 |

## Provider protocol & priority fields

| Field | Type | Required | Description |
|------|------|----------|-------------|
| supportedProtocols | string | No | Comma-separated supported protocols, e.g. `chat/completions,responses`; empty means all protocols |
| priority | int | No | Route priority, higher value means higher priority |

Create example:

```json
{
  "providerCode": "deepseek",
  "providerName": "DeepSeek",
  "providerType": "openai-compatible",
  "supportedProtocols": "chat/completions,responses",
  "priority": 100,
  "baseUrl": "https://api.deepseek.com/v1",
  "status": 1
}
```

