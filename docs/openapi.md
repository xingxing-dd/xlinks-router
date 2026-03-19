# xlinks-router OpenAPI

本文档拆分自原 `docs/tech-design.md`，用于维护对外网关接口定义与调用链路说明。

## 1. 接口范围

当前 MVP 阶段对外重点提供以下标准接口：

- `POST /v1/chat/completions`
- `GET /v1/models`

平台侧模型结构已调整为：

- `providers`：服务商
- `model_endpoints`：模型端点/能力分组
- `models`：统一模型表

不再区分 `customer_model`、`provider_model`，OpenAPI 请求中的 `model` 直接对应 `models.model_code`。

## 2. Chat Completions

- 接口：`POST /v1/chat/completions`
- Content-Type：`application/json`
- 认证：`Authorization: Bearer {customer_token}`

### 2.1 Header

| Header | 说明 |
|--------|------|
| Authorization | Bearer Token，格式：`Bearer {customer_token}` |
| Content-Type | `application/json` |

### 2.2 Request Body

```json
{
  "model": "deepseek-v3",
  "messages": [
    { "role": "system", "content": "你是一个有帮助的助手" },
    { "role": "user", "content": "你好" }
  ],
  "temperature": 0.7,
  "max_tokens": 1000,
  "stream": false,
  "top_p": 1.0,
  "frequency_penalty": 0.0,
  "presence_penalty": 0.0
}
```

### 2.3 字段说明

| 字段 | 类型 | 必填 | 说明 | 默认值 |
|------|------|------|------|--------|
| model | string | 是 | 模型编码，对应 `models.model_code` | - |
| messages | array | 是 | 消息列表 | - |
| messages[].role | string | 是 | 角色：system、user、assistant | - |
| messages[].content | string | 是 | 消息内容 | - |
| temperature | float | 否 | 采样温度 | 0.7 |
| max_tokens | int | 否 | 最大 token 数 | 4096 |
| stream | boolean | 否 | 是否流式返回 | false |
| top_p | float | 否 | nucleus 采样 | 1.0 |
| frequency_penalty | float | 否 | 频率惩罚 | 0.0 |
| presence_penalty | float | 否 | 存在惩罚 | 0.0 |

### 2.4 非流式响应示例

```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion",
  "created": 1677652288,
  "model": "deepseek-v3",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "你好！有什么可以帮助你的吗？"
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 10,
    "completion_tokens": 20,
    "total_tokens": 30
  }
}
```

### 2.5 流式响应示例

```text
data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"deepseek-v3","choices":[{"index":0,"delta":{"role":"assistant","content":"你"},"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"deepseek-v3","choices":[{"index":0,"delta":{"content":"好"},"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"deepseek-v3","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

data: [DONE]
```

## 3. Models List

- 接口：`GET /v1/models`
- 认证：`Authorization: Bearer {customer_token}`

响应示例：

```json
{
  "object": "list",
  "data": [
    {
      "id": "deepseek-v3",
      "object": "model",
      "created": 1677610602,
      "owned_by": "deepseek"
    },
    {
      "id": "gpt-4o-mini",
      "object": "model",
      "created": 1677610602,
      "owned_by": "openai"
    }
  ]
}
```

说明：

- 返回当前平台对外可用的模型列表
- 仅返回 Customer Token 被授权访问的模型
- `id` 对应 `models.model_code`
- `owned_by` 可映射为 Provider 编码或平台统一标识

## 4. 平台处理步骤

`/v1/chat/completions` 的标准处理步骤如下：

1. 校验 Customer Token（检查存在、状态、未过期）
2. 校验请求模型是否在允许列表中
3. 根据 `model_code` 查询启用中的模型
4. 关联查询模型所属 `model_endpoint` 与 `provider`
5. 选择可用 Provider Token
6. 调用下游 Provider
7. 标准化响应结构
8. 记录 Usage Record

## 5. OpenAI 兼容性要求

MVP 阶段至少满足以下兼容性目标：

1. 请求结构兼容：`model`、`messages`、`temperature`、`top_p`、`max_tokens`、`stream`、`frequency_penalty`、`presence_penalty`
2. 响应结构兼容：支持标准非流式与流式结构
3. Header 兼容：支持 `Authorization: Bearer {customerToken}`
4. 错误语义兼容：尽量对外保持 OpenAI 风格错误语义

## 6. API 模块核心处理链路

建议在 `xlinks-router-api` 模块中，将 `/chat/completions` 拆成以下阶段：

1. 入口层：接收标准请求 DTO，完成参数校验、Header 解析、traceId 注入
2. 鉴权层：解析 Bearer Token，查询 `customer_tokens`
3. 权限校验层：校验当前 Token 是否允许访问目标 `model_code`
4. 模型解析层：根据 `model_code` 查询 `models`
5. 端点校验层：确认模型所属 `model_endpoint` 处于启用状态，且请求接口与端点能力匹配
6. Provider Token 选择层：根据 `provider_id` 选择可用下游 token
7. 适配器调用层：根据 `provider_type` 选择 adapter 并发起下游调用
8. 响应转换层：统一转换为 OpenAI 兼容结构
9. 记录层：记录 usage，并预留后续结算扩展点

## 7. 模型解析与校验逻辑

### 7.1 模型查询规则

查询目标模型时，建议满足以下条件：

- `models.model_code = request.model`
- `models.status = 1`
- `providers.status = 1`
- `model_endpoints.status = 1`

### 7.2 端点匹配规则

对于 `/v1/chat/completions`：

- 只允许命中 `endpoint_url = '/v1/chat/completions'` 的模型
- 若模型所属端点不是聊天补全能力，应返回模型不可用于当前接口的错误

### 7.3 推荐 SQL 语义

```sql
SELECT
  m.id,
  m.model_code,
  m.model_name,
  m.endpoint_id,
  m.provider_id,
  me.endpoint_name,
  me.endpoint_url,
  p.provider_code,
  p.provider_type,
  p.base_url
FROM models m
JOIN model_endpoints me ON me.id = m.endpoint_id
JOIN providers p ON p.id = m.provider_id
WHERE m.model_code = #{modelCode}
  AND m.status = 1
  AND me.status = 1
  AND p.status = 1
LIMIT 1;
```

## 8. Provider Token 选择逻辑

建议按以下顺序筛选：

1. 查询 `provider_id = ? and token_status = 1`
2. 过滤过期 token
3. 若存在额度字段，则过滤已耗尽 token
4. 排序优先级：
   - 未过期优先
   - 额度剩余更多优先
   - 最近最少使用优先
5. 选中后更新 `last_used_at`

MVP 简化策略：

- 只校验状态正常且未过期
- 按 `id asc` 选择第一条可用 token

## 9. Provider Adapter 设计建议

统一适配器接口示例：

```java
public interface ChatProviderAdapter {
    boolean supports(String providerType);
    ChatCompletionResponse chatCompletion(ChatCompletionRequest request, ProviderInvokeContext context);
    SseEmitter chatCompletionStream(ChatCompletionRequest request, ProviderInvokeContext context);
}
```

`ProviderInvokeContext` 至少建议包含：

- `providerId`
- `providerCode`
- `providerType`
- `baseUrl`
- `providerToken`
- `modelId`
- `modelCode`
- `modelName`
- `endpointId`
- `endpointUrl`
- `customerTokenId`
- `requestId`

MVP 第一版建议优先实现 `openai-compatible` 适配器。

## 10. 时序概览

```text
Client
  -> ChatCompletionController
  -> CustomerTokenAuthService
  -> ModelAccessService
  -> ModelQueryService
  -> ProviderTokenSelectService
  -> ChatProviderAdapterFactory
  -> OpenAICompatibleAdapter
  -> Provider API
  <- OpenAICompatibleAdapter
  <- ChatCompletionController
  -> UsageRecordService
```

## 11. 错误码约定

| 错误码 | 说明 | HTTP Status |
|--------|------|-------------|
| 0 | 成功 | 200 |
| 4001 | 参数错误 | 400 |
| 4002 | Customer Token 无效 | 401 |
| 4003 | 模型不可用 | 400 |
| 4004 | Provider 不可用 | 502 |
| 4005 | Provider Token 不可用 | 502 |
| 4006 | 模型不在允许列表中 | 403 |
| 4007 | Token 已过期 | 401 |
| 4008 | 模型端点与请求接口不匹配 | 400 |
| 5000 | 系统异常 | 500 |
| 5001 | 外部服务调用失败 | 502 |
