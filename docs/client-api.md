# 客户端 API 接口文档

本文档定义 xlinks-router-web-vue 前端界面所需的 API 接口。前端基于 Vue 3 + Tailwind CSS，后端基于 Spring Boot。

---

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

用于登录/注册前加密密码。

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

### 2.2 发送邮箱验证码

用于注册时发送验证码到用户邮箱。

**请求地址**: `POST /auth/sms-code`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | String | 是 | 邮箱地址 |
| scene | String | 是 | 场景：`register`(注册)、`resetpwd`(重置密码) |

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| message | String | 提示信息 |
| mockCode | String | 模拟模式下的验证码（开发环境） |
| expireSeconds | Integer | 验证码有效期（秒） |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "message": "验证码发送成功",
    "mockCode": "123456",
    "expireSeconds": 300
  }
}
```

---

### 2.3 用户注册

**请求地址**: `POST /auth/register`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | String | 是 | 邮箱地址 |
| password | String | 是 | 密码（RSA 加密） |
| code | String | 是 | 邮箱验证码 |
| inviteCode | String | 否 | 邀请码 |

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| message | String | 注册结果信息 |

**响应示例**:
```json
{
  "code": 0,
  "message": "注册成功",
  "data": null
}
```

---

### 2.4 用户登录

**请求地址**: `POST /auth/login`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | String | 是 | 邮箱地址 |
| password | String | 是 | 密码（RSA 加密） |

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| accessToken | String | 访问令牌 |
| expiresIn | Long | 过期秒数 |
| user | Object | 用户信息 |
| user.id | Long | 用户 ID |
| user.email | String | 邮箱 |
| user.status | Integer | 状态：1-启用 |

**响应示例**:
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

### 2.5 获取当前用户信息

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

供前端「模型」页面展示所有可用模型的详细信息。

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
| name | String | 模型名称（如 claude-3-7-sonnet） |
| provider | String | 提供商名称（如 Anthropic） |
| description | String | 模型描述 |
| inputPrice | String | 输入价格（如 $3.00/M） |
| outputPrice | String | 输出价格（如 $15.00/M） |
| contextWindow | String | 上下文窗口大小（如 200K） |
| status | String | 状态：available/limited/unavailable |

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

## 6. 套餐与充值模块

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
| concurrency | Integer | 并发限制 |
| features | Array | 特性列表 |
| isRecommended | Boolean | 是否推荐 |

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
      "concurrency": 8,
      "features": [
        "有效期 30 天",
        "仅可用 Codex",
        "月度可用 $900 额度",
        "每日可用 $30 + 昨日未用完额度"
      ],
      "isRecommended": false
    }
  ]
}
```

---

### 6.2 获取充值选项

**请求地址**: `GET /api/v1/recharge-options`

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| usd | BigDecimal | 美元金额 |
| cny | BigDecimal | 人民币金额 |
| bonus | BigDecimal | 赠送金额（如有） |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "usd": 100, "cny": 20, "bonus": 0 },
    { "usd": 200, "cny": 40, "bonus": 0 },
    { "usd": 500, "cny": 100, "bonus": 10 },
    { "usd": 1000, "cny": 200, "bonus": 30 },
    { "usd": 2000, "cny": 400, "bonus": 80 },
    { "usd": 5000, "cny": 1000, "bonus": 250 }
  ]
}
```

---

### 6.3 创建订单

**请求地址**: `POST /api/v1/orders`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | String | 是 | 订单类型：plan/recharge |
| planId | String | 是（type=plan） | 套餐 ID |
| amount | BigDecimal | 是（type=recharge） | 充值金额(人民币) |
| paymentMethod | String | 是 | 支付方式：alipay/wechat |

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
    "payUrl": "https://pay.example.com/alipay?order_id=...",
    "expireTime": "2026-03-17 15:30:00"
  }
}
```

---

### 6.4 获取用户当前套餐状态

**请求地址**: `GET /api/v1/user/subscription`

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| planId | String | 当前套餐 ID |
| planName | String | 套餐名称 |
| dailyLimit | BigDecimal | 每日额度 |
| dailyUsed | BigDecimal | 今日已用 |
| monthlyQuota | BigDecimal | 月度额度 |
| monthlyUsed | BigDecimal | 月度已用 |
| concurrency | Integer | 并发限制 |
| currentConcurrency | Integer | 当前并发数 |
| expireTime | DateTime | 套餐过期时间 |
| status | String | 套餐状态：active/expired/frozen |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "planId": "medium",
    "planName": "Codex中包套餐",
    "dailyLimit": 60,
    "dailyUsed": 25.5,
    "monthlyQuota": 1800,
    "monthlyUsed": 450,
    "concurrency": 12,
    "currentConcurrency": 3,
    "expireTime": "2026-04-15 00:00:00",
    "status": "active"
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
| model | String | 否 | 模型筛选 |
| providerId | Long | 否 | Provider 筛选 |
| startDate | DateTime | 否 | 开始时间 |
| endDate | DateTime | 否 | 结束时间 |

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 记录 ID |
| requestId | String | 请求 ID |
| model | String | 请求模型 |
| providerName | String | Provider 名称 |
| promptTokens | Integer | 提示词 Token |
| completionTokens | Integer | 补全 Token |
| totalTokens | Integer | 总 Token |
| latencyMs | Integer | 延迟(毫秒) |
| responseStatus | Integer | 响应状态码 |
| errorCode | String | 错误码 |
| cost | BigDecimal | 费用(元) |
| createdAt | DateTime | 请求时间 |

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
| providerStats | Array | 按 Provider 统计 |

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
      { "model": "GPT-4", "requests": 5000, "tokens": 800000, "cost": 800.00 }
    ],
    "providerStats": [
      { "provider": "OpenAI", "requests": 6000, "tokens": 900000, "cost": 900.00 }
    ]
  }
}
```

---

## 10. 文件上传（待定义）

如需支持用户上传头像、付款凭证等文件，需要定义以下接口：

- `POST /api/v1/upload` - 上传文件
- `DELETE /api/v1/files/{id}` - 删除文件

---

## 附录：待新增数据表

根据前端页面需求，建议新增以下数据表：

### A.1 套餐表 (plans)

```sql
CREATE TABLE `plans` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_code` VARCHAR(50) NOT NULL COMMENT '套餐编码，如 small、medium、large',
  `plan_name` VARCHAR(100) NOT NULL COMMENT '套餐名称',
  `price` DECIMAL(10,2) NOT NULL COMMENT '价格(元)',
  `daily_limit` DECIMAL(10,2) NOT NULL COMMENT '每日额度(美元)',
  `monthly_quota` DECIMAL(10,2) NOT NULL COMMENT '月度额度(美元)',
  `concurrency` INT NOT NULL DEFAULT 1 COMMENT '并发限制',
  `features` JSON DEFAULT NULL COMMENT '特性列表',
  `is_recommended` TINYINT NOT NULL DEFAULT 0 COMMENT '是否推荐：1-是，0-否',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_code` (`plan_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐表';
```

### A.2 用户订阅表 (user_subscriptions)

```sql
CREATE TABLE `user_subscriptions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `plan_id` BIGINT NOT NULL COMMENT '套餐 ID',
  `daily_used` DECIMAL(10,2) DEFAULT 0 COMMENT '今日已用额度(美元)',
  `monthly_used` DECIMAL(10,2) DEFAULT 0 COMMENT '月度已用额度(美元)',
  `current_concurrency` INT DEFAULT 0 COMMENT '当前并发数',
  `expire_time` DATETIME NOT NULL COMMENT '过期时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-过期',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户订阅表';
```

### A.3 推广表 (referrals)

```sql
CREATE TABLE `referrals` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '推广人 ID',
  `referral_code` VARCHAR(50) NOT NULL COMMENT '邀请码',
  `referred_user_id` BIGINT NOT NULL COMMENT '被邀请人 ID',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待激活，1-已激活',
  `register_reward` DECIMAL(10,2) DEFAULT 0 COMMENT '注册奖励',
  `first_recharge_reward` DECIMAL(10,2) DEFAULT 0 COMMENT '首充奖励',
  `total_earnings` DECIMAL(10,2) DEFAULT 0 COMMENT '累计收益',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `activated_at` DATETIME DEFAULT NULL COMMENT '激活时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_referral_code` (`referral_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推广记录表';
```

### A.4 充值订单表 (recharge_orders)

```sql
CREATE TABLE `recharge_orders` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `type` VARCHAR(20) NOT NULL COMMENT '类型：plan/recharge',
  `plan_id` BIGINT DEFAULT NULL COMMENT '套餐 ID',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额(人民币)',
  `usd_amount` DECIMAL(10,2) NOT NULL COMMENT '美元金额',
  `bonus_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '赠送金额',
  `payment_method` VARCHAR(20) NOT NULL COMMENT '支付方式：alipay/wechat',
  `payment_status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '支付状态：pending/paid/failed',
  `trade_no` VARCHAR(64) DEFAULT NULL COMMENT '第三方交易号',
  `paid_at` DATETIME DEFAULT NULL COMMENT '支付时间',
  `expire_time` DATETIME NOT NULL COMMENT '订单过期时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值订单表';
```

### A.5 联系表单表 (contact_messages)

```sql
CREATE TABLE `contact_messages` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户 ID（未登录可为空）',
  `name` VARCHAR(50) NOT NULL COMMENT '姓名',
  `email` VARCHAR(100) NOT NULL COMMENT '邮箱',
  `subject` VARCHAR(20) NOT NULL COMMENT '主题：technical/billing/feature/bug/other',
  `message` TEXT NOT NULL COMMENT '消息内容',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待处理，1-已回复',
  `reply` TEXT DEFAULT NULL COMMENT '回复内容',
  `replied_at` DATETIME DEFAULT NULL COMMENT '回复时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='联系表单表';
```
