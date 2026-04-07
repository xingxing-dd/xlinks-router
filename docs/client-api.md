# 客户端 API 接口文档

本文档仅保留前后端对接所需的接口定义与字段信息。

---

## 缓存命中计费补充（2026-04-07）

Usage Record 明细建议补充两个字段（后端 usage_records 已落库）：

- `cacheHitTokens`：缓存命中输入 Token 数
- `cacheHitCost`：缓存命中输入 Token 费用

计费公式：

`(inputTokens - cacheHitTokens) * inputPrice + cacheHitTokens * cacheHitPrice + outputTokens * outputPrice`

## 1. 通用说明

### 1.1 基础信息

| 项目 | 说明 |
|------|------|
| 基础 URL | `http://localhost:8080` (开发环境) |
| 数据格式 | JSON |
| 编码 | UTF-8 |
| 认证方式 | Bearer Token (登录后获取) |

### 1.2 统一响应格式

**成功响应**
```json
{
  "code": 0,
  "message": "success",
  "data": { ... }
}
```

**分页响应**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [...],
    "total": 100,
    "page": 1,
    "pageSize": 10
  }
}
```

**错误响应**
```json
{
  "code": 400,
  "message": "错误信息",
  "data": null
}
```

### 1.3 通用状态码

| code | 说明 |
|------|------|
| 0 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权/登录失效 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 2. 用户认证模块

### 2.1 获取 RSA 公钥

**请求地址**: `POST /auth/rsa-public-key`

**请求参数**: 无

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| algorithm | String | RSA 算法，如 `RSA/ECB/PKCS1Padding` |
| publicKey | String | RSA 公钥（Base64 编码） |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "algorithm": "RSA/ECB/PKCS1Padding",
    "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA..."
  }
}
```

---

### 2.2 ?????

**????**: `POST /auth/verify-code`

**????**:

| ?? | ?? | ?? | ?? |
|------|------|------|------|
| codeType | String | ? | ??????`email`(??)?`phone`(???) |
| target | String | ? | ????????`codeType=email` ???????`codeType=phone` ????? |
| scene | String | ? | ???`register`(??)?`resetpwd`(????) |

**????**:

| ?? | ?? | ?? |
|------|------|------|
| message | String | ??????????????????? |
| token | String | ????????????? |
| expireSeconds | Integer | ????????? |

**?????????**:
```json
{
  "codeType": "email",
  "target": "user@example.com",
  "scene": "register"
}
```

**??????????**:
```json
{
  "codeType": "phone",
  "target": "13800138000",
  "scene": "resetpwd"
}
```

**????**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "message": "?????????",
    "token": "xlinks:resetpwd:email:user@example.com",
    "expireSeconds": 300
  }
}
```

---

### 2.3 ????

**????**: `POST /auth/register`

**????**:

| ?? | ?? | ?? | ?? |
|------|------|------|------|
| target | String | ? | ?????`targetType=email` ???????`targetType=phone` ????? |
| targetType | String | ? | ?????`email`(??)?`phone`(???) |
| password | String | ? | ???RSA ??? |
| code | String | ? | ??? |
| inviteCode | String | ? | ??? |

**????**:

| ?? | ?? | ?? |
|------|------|------|
| message | String | ?????? |

**????????**:
```json
{
  "target": "user@example.com",
  "targetType": "email",
  "password": "encrypted-password",
  "code": "123456",
  "inviteCode": "INVITE2026ABC"
}
```

**?????????**:
```json
{
  "target": "13800138000",
  "targetType": "phone",
  "password": "encrypted-password",
  "code": "123456"
}
```

**????**:
```json
{
  "code": 0,
  "message": "????",
  "data": null
}
```

---

### 2.4 ????

**????**: `POST /auth/reset-password`

**????**:

| ?? | ?? | ?? | ?? |
|------|------|------|------|
| target | String | ? | ?????????? |
| targetType | String | ? | ???????`email`(??)?`phone`(???) |
| password | String | ? | ????RSA ??? |
| code | String | ? | ??? |

**????**:

| ?? | ?? | ?? |
|------|------|------|
| message | String | ?????? |

**??????????**:
```json
{
  "target": "user@example.com",
  "targetType": "email",
  "password": "encrypted-new-password",
  "code": "123456"
}
```

**???????????**:
```json
{
  "target": "13800138000",
  "targetType": "phone",
  "password": "encrypted-new-password",
  "code": "123456"
}
```

**????**:
```json
{
  "code": 0,
  "message": "??????",
  "data": null
}
```

---

### 2.5 ????

**????**: `POST /auth/login`

**????**:

| ?? | ?? | ?? | ?? |
|------|------|------|------|
| username | String | ? | ??? |
| password | String | ? | ???RSA ??? |

**????**:

| ?? | ?? | ?? |
|------|------|------|
| accessToken | String | ???? |
| expiresIn | Long | ???? |
| user | Object | ???? |
| user.id | Long | ?? ID |
| user.email | String | ?? |
| user.status | Integer | ???1-?? |

**????**:
```json
{
  "username": "user@example.com",
  "password": "encrypted-password"
}
```

**????**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 7200,
    "user": {
      "id": 1,
      "email": "user@example.com",
      "status": 1
    }
  }
}
```

---

### 2.6 获取当前用户信息

**请求地址**: `GET /api/v1/user/info`

**请求头**:

| 字段 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 用户 ID |
| email | String | 邮箱 |
| nickname | String | 昵称 |
| avatar | String | 头像 URL |
| balance | BigDecimal | 账户余额 |
| status | Integer | 状态：1-启用 |
| createdAt | DateTime | 注册时间 |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "nickname": "张三",
    "avatar": "https://example.com/avatar.jpg",
    "balance": 1258.00,
    "status": 1,
    "createdAt": "2026-01-15 10:30:00"
  }
}
```

---

## 3. 仪表盘模块

### 3.1 获取仪表盘统计数据

**请求地址**: `GET /api/v1/dashboard/stats`

**请求头**: `Authorization: Bearer {accessToken}`

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| todayRequests | Integer | 今日请求数 |
| todayRequestsChange | Double | 今日请求数环比变化(%) |
| todayTokens | Integer | 今日 Token 消耗 |
| todayTokensChange | Double | 今日 Token 环比变化(%) |
| todayCost | BigDecimal | 今日费用(元) |
| todayCostChange | Double | 今日费用环比变化(%) |
| balance | BigDecimal | 账户余额 |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "todayRequests": 3245,
    "todayRequestsChange": 12.5,
    "todayTokens": 28500,
    "todayTokensChange": 8.2,
    "todayCost": 56.80,
    "todayCostChange": -3.1,
    "balance": 1258.00
  }
}
```

---

### 3.2 获取 Token 使用趋势

**请求地址**: `GET /api/v1/dashboard/usage-trend`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| days | Integer | 否 | 查询天数，默认 7 |

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| date | String | 日期，格式：MM-DD |
| tokens | Integer | Token 数量 |
| cost | BigDecimal | 费用(元) |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "date": "03-03", "tokens": 12000, "cost": 240.00 },
    { "date": "03-04", "tokens": 15000, "cost": 300.00 },
    { "date": "03-05", "tokens": 18000, "cost": 360.00 },
    { "date": "03-06", "tokens": 14000, "cost": 280.00 },
    { "date": "03-07", "tokens": 22000, "cost": 440.00 },
    { "date": "03-08", "tokens": 25000, "cost": 500.00 },
    { "date": "03-09", "tokens": 28000, "cost": 560.00 }
  ]
}
```

---

### 3.3 获取模型使用分布

**请求地址**: `GET /api/v1/dashboard/model-usage`

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| model | String | 模型名称 |
| requests | Integer | 请求次数 |
| tokens | Integer | Token 数量 |
| cost | BigDecimal | 费用(元) |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "model": "GPT-4", "requests": 850, "tokens": 125000, "cost": 1250.00 },
    { "model": "GPT-3.5", "requests": 1200, "tokens": 180000, "cost": 360.00 },
    { "model": "Claude-3", "requests": 650, "tokens": 95000, "cost": 1425.00 },
    { "model": "Gemini", "requests": 420, "tokens": 63000, "cost": 126.00 }
  ]
}
```

---

### 3.4 获取最近活动

**请求地址**: `GET /api/v1/dashboard/recent-activities`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| limit | Integer | 否 | 返回条数，默认 5 |

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| time | String | 发生时间（相对时间） |
| event | String | 事件描述 |
| tokens | String | Token 数量（如有） |
| status | String | 状态：success/error/info |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "time": "2 分钟前", "event": "GPT-4 API 调用成功", "tokens": "1,245", "status": "success" },
    { "time": "15 分钟前", "event": "Claude-3 API 调用成功", "tokens": "856", "status": "success" },
    { "time": "1 小时前", "event": "账户充值 ¥500", "tokens": "", "status": "info" },
    { "time": "2 小时前", "event": "GPT-3.5 API 调用失败", "tokens": "重试中", "status": "error" },
    { "time": "3 小时前", "event": "新增 API Key", "tokens": "sk-***abc", "status": "info" }
  ]
}
```

---

## 4. 模型管理模块

### 4.1 获取客户模型列表

**请求地址**: `GET /api/v1/customer-models`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页数量，默认 10 |
| status | Integer | 否 | 状态筛选：1-启用，0-禁用 |
| keyword | String | 否 | 关键词搜索 |

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 模型 ID |
| logicModelCode | String | 模型编码 |
| logicModelName | String | 模型名称 |
| modelType | String | 模型类型：chat/embedding/image |
| status | Integer | 状态：1-启用，0-禁用 |
| isDefault | Integer | 是否默认：1-是，0-否 |
| remark | String | 备注 |
| createdAt | DateTime | 创建时间 |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "logicModelCode": "claude-sonnet",
        "logicModelName": "Claude Sonnet",
        "modelType": "chat",
        "status": 1,
        "isDefault": 1,
        "remark": "主力模型",
        "createdAt": "2026-01-15 10:30:00"
      }
    ],
    "total": 10,
    "page": 1,
    "pageSize": 10
  }
}
```

---

### 4.2 获取可用模型列表（展示用）

**请求地址**: `GET /api/v1/models/available`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | String | 否 | 模型类型：chat/embedding/image |
| keyword | String | 否 | 关键词搜索 |

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 模型 ID |
| name | String | 模型名称（展示名，如 claude-3-7-sonnet） |
| provider | String | 提供商名称 |
| description | String | 模型描述 |
| inputPrice | String | 输入价格（展示字段） |
| outputPrice | String | 输出价格（展示字段） |
| contextWindow | String | 上下文窗口大小（展示字段） |
| status | String | 状态：available/limited/unavailable |

说明：
- 该接口主要服务 `models/index.vue` 的展示卡片，字段保持轻量化即可。
- 若后续接真实模型表，对客展示字段建议优先使用 `customer_models.logic_model_name`；底层供应商模型名可在路由明细等内部场景中再使用。

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "claude-3-7-sonnet",
      "provider": "Anthropic",
      "description": "高性能对话模型，适合复杂推理任务",
      "inputPrice": "$3.00/M",
      "outputPrice": "$15.00/M",
      "contextWindow": "200K",
      "status": "available"
    }
  ]
}
```

---

### 4.3 获取模型详情及路由信息

**请求地址**: `GET /api/v1/models/{id}`

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 模型 ID |
| name | String | 模型名称 |
| provider | String | 提供商 |
| description | String | 描述 |
| inputPrice | String | 输入价格 |
| outputPrice | String | 输出价格 |
| contextWindow | String | 上下文窗口 |
| routes | Array | 路由信息列表 |
| routes[].providerId | Long | Provider ID |
| routes[].providerName | String | Provider 名称 |
| routes[].modelName | String | 底层模型名 |
| routes[].priority | Integer | 优先级 |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "name": "claude-3-7-sonnet",
    "provider": "Anthropic",
    "description": "高性能对话模型",
    "inputPrice": "$3.00/M",
    "outputPrice": "$15.00/M",
    "contextWindow": "200K",
    "routes": [
      {
        "providerId": 1,
        "providerName": "OpenAI",
        "modelName": "claude-3-7-sonnet-20250219",
        "priority": 1
      }
    ]
  }
}
```

---

## 5. 令牌管理模块

### 5.1 获取客户 Token 列表

**请求地址**: `GET /api/v1/customer-tokens`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页数量，默认 10 |
| keyword | String | 否 | 关键词搜索（名称） |
| status | Integer | 否 | 状态筛选 |

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | Token ID |
| customerName | String | 客户名称 |
| tokenName | String | Token 名称 |
| tokenValue | String | Token 值（脱敏显示，如 sk-abc***xyz） |
| status | Integer | 状态：1-启用，0-禁用 |
| expireTime | DateTime | 过期时间 |
| allowedModels | JSON | 允许访问的模型列表 |
| totalRequests | Integer | 累计请求次数 |
| lastUsedAt | DateTime | 最后使用时间 |
| createdAt | DateTime | 创建时间 |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "customerName": "张三",
        "tokenName": "生产环境主Key",
        "tokenValue": "sk-abc123***pqr678",
        "status": 1,
        "expireTime": "2027-01-01 00:00:00",
        "allowedModels": ["claude-sonnet", "gpt-4"],
        "totalRequests": 12453,
        "lastUsedAt": "2026-03-17 14:30:00",
        "createdAt": "2026-02-15 10:30:00"
      }
    ],
    "total": 10,
    "page": 1,
    "pageSize": 10
  }
}
```

---

### 5.2 创建客户 Token

**请求地址**: `POST /api/v1/customer-tokens`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| tokenName | String | 是 | Token 名称 |
| allowedModels | Array | 否 | 允许访问的模型列表 |
| expireDays | Integer | 否 | 过期天数（不填则永久） |

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | Token ID |
| tokenName | String | Token 名称 |
| tokenValue | String | 生成的 Token 值（只返回一次） |
| expireTime | DateTime | 过期时间 |
| createdAt | DateTime | 创建时间 |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 4,
    "tokenName": "新 Token",
    "tokenValue": "sk-newabc123def456ghi789jkl012mno345pqr",
    "expireTime": "2027-03-17 00:00:00",
    "createdAt": "2026-03-17 15:00:00"
  }
}
```

---

### 5.3 更新客户 Token

**请求地址**: `PUT /api/v1/customer-tokens/{id}`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| tokenName | String | 否 | Token 名称 |
| allowedModels | Array | 否 | 允许访问的模型列表 |
| status | Integer | 否 | 状态：1-启用，0-禁用 |
| expireTime | DateTime | 否 | 过期时间 |

**响应参数**: 空

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

### 5.4 删除客户 Token

**请求地址**: `DELETE /api/v1/customer-tokens/{id}`

**响应参数**: 空

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

### 5.5 刷新客户 Token

**请求地址**: `POST /api/v1/customer-tokens/{id}/refresh`

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| tokenValue | String | 新 Token 值 |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "tokenValue": "sk-refreshedabc123def456ghi789jkl012mno"
  }
}
```

---

## 6. 套餐模块（plans/index.vue）

### 6.1 获取套餐列表

**请求地址**: `GET /api/v1/plans`

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 套餐 ID |
| name | String | 套餐名称 |
| price | BigDecimal | 价格(元) |
| dailyLimit | BigDecimal | 每日额度(美元) |
| monthlyQuota | BigDecimal | 月度额度(美元) |
| durationDays | Integer | 有效期天数 |
| allowedModels | Array | 允许访问的模型列表 |
| carryOverDailyQuota | Boolean | 是否支持昨日未用额度结转 |
| stackQuotaOnly | Boolean | 多买是否只叠加额度不叠加时长 |
| isRecommended | Boolean | 是否推荐（用于高亮） |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": "small",
      "name": "Codex小包套餐",
      "price": 45.00,
      "dailyLimit": 30,
      "monthlyQuota": 900,
      "durationDays": 30,
      "allowedModels": ["codex"],
      "carryOverDailyQuota": true,
      "stackQuotaOnly": true,
      "isRecommended": false
    }
  ]
}
```

---

### 6.2 获取当前生效订阅列表（我的订阅）

**请求地址**: `GET /api/v1/subscriptions/active`

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 订阅记录 ID |
| planId | String | 套餐 ID |
| planName | String | 套餐名称 |
| daysRemaining | Integer | 剩余天数 |
| purchaseDate | DateTime | 购买时间 |
| expiryDate | DateTime | 到期时间 |
| dailyReset | Boolean | 今日额度是否已重置 |
| remainingQuota | BigDecimal | 剩余额度（美元） |
| totalQuota | BigDecimal | 总额度（美元） |
| usedPercentage | Integer | 已使用比例（0-100） |

说明：
- 返回列表用于右上角下拉切换与当前订阅卡片展示。
- 建议按 `expiryDate` 升序排序，默认取第一条展示。

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": "sub-1",
      "planId": "medium",
      "planName": "Codex中包套餐",
      "daysRemaining": 24,
      "purchaseDate": "2026-03-11 16:15",
      "expiryDate": "2026-04-10 16:15",
      "dailyReset": true,
      "remainingQuota": 58.049198,
      "totalQuota": 60,
      "usedPercentage": 3
    }
  ]
}
```

---

### 6.3 获取历史订阅列表

**请求地址**: `GET /api/v1/subscriptions/history`

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 订阅记录 ID |
| planId | String | 套餐 ID |
| planName | String | 套餐名称 |
| purchaseDate | DateTime | 购买时间 |
| expiryDate | DateTime | 到期时间 |
| totalQuota | BigDecimal | 总额度（美元） |
| usedQuota | BigDecimal | 已使用额度（美元） |
| usedPercentage | Integer | 已使用比例（0-100） |
| status | String | 状态：expired/cancelled |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": "hist-1",
      "planId": "small",
      "planName": "Codex小包套餐",
      "purchaseDate": "2026-01-15 10:30",
      "expiryDate": "2026-02-14 10:30",
      "totalQuota": 30,
      "usedQuota": 29.5,
      "usedPercentage": 98,
      "status": "expired"
    }
  ]
}
```

---

### 6.4 创建套餐订单

**请求地址**: `POST /api/v1/orders`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| planId | String | 是 | 套餐 ID |
| paymentMethod | String | 是 | 支付方式：third-party（当前仅支持） |

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| orderId | String | 订单 ID |
| payUrl | String | 支付跳转链接 |
| expireTime | DateTime | 订单过期时间 |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "orderId": "ORDER202603171500001",
    "payUrl": "https://pay.example.com/third-party?order_id=...",
    "expireTime": "2026-03-17 15:30:00"
  }
}
```

---

### 6.5 使用激活码兑换套餐

**请求地址**: `POST /api/v1/activation-codes/consume`

**请求头**: `Authorization: Bearer {accessToken}`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| code | String | 是 | 激活码 |

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| message | String | 兑换结果提示 |
| activatedPlanId | String | 兑换的套餐 ID |
| activatedPlanName | String | 兑换的套餐名称 |
| expireTime | DateTime | 兑换套餐到期时间 |
| subscriptionId | String | 新生成的用户套餐记录 ID |

**状态流转**:
- `activation_code_stocks.status = 1`（可用） -> 兑换成功后置为 `2`（已使用）
- `activation_code_stocks.status = 0`（禁用） -> 返回错误码 `400`（参数/状态错误）
- `activation_code_stocks.status = 2`（已使用）
  - 若 `used_by` 为当前用户：返回成功（幂等）
  - 若 `used_by` 为其他用户：返回错误码 `403`

**幂等说明**:
- 相同用户重复兑换同一激活码，返回同一次兑换的套餐信息，不重复创建订阅记录。

**错误码**:
- `400` 激活码不存在或状态不可用
- `403` 激活码已被其他用户使用

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "message": "激活成功",
    "activatedPlanId": "medium",
    "activatedPlanName": "Codex中包套餐",
    "expireTime": "2026-04-15 00:00:00",
    "subscriptionId": "SUB202603201530001"
  }
}
```

---

## 7. 推广模块

### 7.1 获取推广信息

**请求地址**: `GET /api/v1/promotion/info`

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| referralCode | String | 邀请码 |
| referralLink | String | 邀请链接 |
| totalReferrals | Integer | 累计邀请人数 |
| activeReferrals | Integer | 活跃用户数 |
| totalEarnings | BigDecimal | 累计收益(元) |
| pendingEarnings | BigDecimal | 待结算收益(元) |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "referralCode": "INVITE2026ABC",
    "referralLink": "https://token-hub.com/register?ref=INVITE2026ABC",
    "totalReferrals": 23,
    "activeReferrals": 18,
    "totalEarnings": 1580.00,
    "pendingEarnings": 320.00
  }
}
```

---

### 7.2 获取推广记录

**请求地址**: `GET /api/v1/promotion/records`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码 |
| pageSize | Integer | 否 | 每页数量 |
| status | String | 否 | 状态筛选：active/pending/inactive |

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 记录 ID |
| userName | String | 被邀请人姓名 |
| email | String | 被邀请人邮箱（脱敏） |
| joinDate | DateTime | 注册时间 |
| status | String | 状态：active/pending/inactive |
| earnings | BigDecimal | 累计奖励(元) |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [
      {
        "id": "1",
        "userName": "张三",
        "email": "zhang***@example.com",
        "joinDate": "2026-03-01",
        "status": "active",
        "earnings": 150.00
      }
    ],
    "total": 23,
    "page": 1,
    "pageSize": 10
  }
}
```

---

### 7.3 获取推广规则

**请求地址**: `GET /api/v1/promotion/rules`

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| registerReward | BigDecimal | 注册奖励(元) |
| firstRechargeRate | BigDecimal | 首次充值奖励比例(%) |
| consumptionRate | BigDecimal | 消费返佣比例(%) |
| settlementDay | Integer | 结算日（每月几号） |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "registerReward": 10.00,
    "firstRechargeRate": 10,
    "consumptionRate": 5,
    "settlementDay": 1
  }
}
```

---

## 8. 联系我们模块

### 8.1 提交联系表单

**请求地址**: `POST /api/v1/contact`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 姓名 |
| email | String | 是 | 邮箱 |
| subject | String | 是 | 主题：technical/billing/feature/bug/other |
| message | String | 是 | 消息内容 |

**响应参数**: 空

**响应示例**:
```json
{
  "code": 0,
  "message": "提交成功，我们会在24小时内回复您",
  "data": null
}
```

---

## 9. 使用记录模块

### 9.1 获取使用记录列表

**请求地址**: `GET /api/v1/usage-records`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码 |
| pageSize | Integer | 否 | 每页数量 |
| model | String | 否 | 模型筛选；建议与 `usage_records.request_model` 保持一致 |
| providerId | Long | 否 | Provider 筛选 |
| startDate | DateTime | 否 | 开始时间 |
| endDate | DateTime | 否 | 结束时间 |

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 记录 ID |
| requestId | String | 请求 ID，对应 `usage_records.request_id` |
| model | String | 请求模型，对应 `usage_records.request_model` |
| providerName | String | Provider 名称 |
| promptTokens | Integer | 提示词 Token |
| completionTokens | Integer | 补全 Token |
| totalTokens | Integer | 总 Token |
| latencyMs | Integer | 延迟(毫秒) |
| responseStatus | Integer | 响应状态码 |
| errorCode | String | 错误码 |
| cost | BigDecimal | 费用(元) |
| createdAt | DateTime | 请求时间 |

说明：
- 当前后端 `UsageRecordController` 的 `GET /api/v1/usage-records` 仍返回空 `List<Object>`，与本文档定义的分页结构不一致，需补齐 DTO 与分页返回。
- 当前前端 `src/views` 里尚未接入使用记录页，但该接口文档可继续保留，便于后续扩展控制台明细页。

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "requestId": "req_abc123",
        "model": "claude-sonnet",
        "providerName": "OpenAI",
        "promptTokens": 500,
        "completionTokens": 745,
        "totalTokens": 1245,
        "latencyMs": 1500,
        "responseStatus": 200,
        "errorCode": null,
        "cost": 0.125,
        "createdAt": "2026-03-17 14:30:00"
      }
    ],
    "total": 100,
    "page": 1,
    "pageSize": 10
  }
}
```

---

### 9.2 获取使用统计汇总

**请求地址**: `GET /api/v1/usage-records/summary`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| startDate | DateTime | 否 | 开始时间 |
| endDate | DateTime | 否 | 结束时间 |

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| totalRequests | Integer | 总请求数 |
| totalTokens | Integer | 总 Token 数 |
| totalCost | BigDecimal | 总费用(元) |
| avgLatencyMs | Integer | 平均延迟(毫秒) |
| successRate | Double | 成功率(%) |
| modelStats | Array | 按模型统计 |
| modelStats[].name | String | 统计名称；当前后端 DTO 使用通用字段 `name` |
| modelStats[].requests | Integer | 请求数 |
| modelStats[].tokens | Integer | Token 数 |
| modelStats[].cost | BigDecimal | 费用 |
| providerStats | Array | 按 Provider 统计 |
| providerStats[].name | String | Provider 名称；当前后端 DTO 使用通用字段 `name` |
| providerStats[].requests | Integer | 请求数 |
| providerStats[].tokens | Integer | Token 数 |
| providerStats[].cost | BigDecimal | 费用 |

说明：
- 当前文档示例写法使用 `model` / `provider` 作为子项字段名，但后端 DTO `UsageSummaryStatItem` 实际为统一字段 `name`。
- 为避免前后端歧义，建议以前端/接口统一的 `name` 为准；若需要更强语义，可后续拆分为 `modelName` 与 `providerName` 两类 VO。

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "totalRequests": 10000,
    "totalTokens": 1500000,
    "totalCost": 1500.00,
    "avgLatencyMs": 1200,
    "successRate": 99.5,
    "modelStats": [
      { "name": "GPT-4", "requests": 5000, "tokens": 800000, "cost": 800.00 }
    ],
    "providerStats": [
      { "name": "OpenAI", "requests": 6000, "tokens": 900000, "cost": 900.00 }
    ]
  }
}
```

---
