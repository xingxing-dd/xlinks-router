# xlinks-router Admin API

本文档定义 `D:\project\xlinks-router\xlinks-router-admin` 后端与 `D:\project\xlinks-router\xlinks-router-web\xlinks-router-admin` 前端之间的后台运营接口约定。

## 1. 能力范围

当前 admin 端后端能力包括：

1. 管理员认证
2. Dashboard 运营概览
3. 服务商管理
4. 服务商 Token 管理
5. 标准端点管理
6. 标准模型管理
7. 服务商模型映射管理
8. 客户 Token 管理
9. 套餐管理
10. 套餐激活码管理
11. 套餐订阅记录
12. 支付链接管理

## 2. 通用约定

- 基础路径：`/admin`
- 鉴权方式：`Authorization: Bearer <accessToken>`
- 通用状态：`1 = 启用`，`0 = 停用`
- 分页参数：`page`、`pageSize`
- 列表响应：`Result<PageResult<T>>`
- 普通响应：`Result<T>`

### 2.1 统一响应

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 2.2 分页响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [],
    "total": 0,
    "page": 1,
    "pageSize": 10
  }
}
```

### 2.3 常用错误码

- `4001`：参数错误或业务校验失败
- `4010`：未登录或管理员 Token 无效
- `4014`：管理员账号已停用
- `5000`：系统异常

## 3. 管理员认证

管理员账号保存在 `admin_accounts` 表，不复用商户或客户账号体系。

### 3.1 登录

- `POST /admin/auth/login`

```json
{
  "username": "admin",
  "password": "admin123"
}
```

### 3.2 当前登录信息

- `GET /admin/auth/me`

### 3.3 退出登录

- `POST /admin/auth/logout`

## 4. Dashboard

### 4.1 运营概览

- `GET /admin/dashboard/overview`

返回字段包括：

- `providerCount`
- `activeProviderCount`
- `endpointCount`
- `modelCount`
- `providerModelCount`
- `providerTokenCount`
- `customerTokenCount`
- `expiringTokenCount`

## 5. 服务商管理

资源：`providers`

- `POST /admin/providers`
- `GET /admin/providers?page=1&pageSize=10`
- `GET /admin/providers/{id}`
- `PUT /admin/providers/{id}`
- `PATCH /admin/providers/{id}/status?status=0`
- `DELETE /admin/providers/{id}`

查询参数：`providerCode`、`providerName`、`status`

## 6. 服务商 Token 管理

资源：`provider_tokens`

- `POST /admin/provider-tokens`
- `GET /admin/provider-tokens?page=1&pageSize=10`
- `GET /admin/provider-tokens/{id}`
- `PUT /admin/provider-tokens/{id}`
- `PATCH /admin/provider-tokens/{id}/status?status=0`
- `DELETE /admin/provider-tokens/{id}`

查询参数：`providerId`、`tokenStatus`

## 7. 标准端点管理

资源：`model_endpoints`

- `POST /admin/model-endpoints`
- `GET /admin/model-endpoints?page=1&pageSize=10`
- `GET /admin/model-endpoints/{id}`
- `PUT /admin/model-endpoints/{id}`
- `PATCH /admin/model-endpoints/{id}/status?status=0`
- `DELETE /admin/model-endpoints/{id}`

查询参数：`endpointName`、`status`

## 8. 模型管理

当前模型管理包含标准模型与服务商模型映射两部分。

### 8.1 标准模型

资源：`models`

- `POST /admin/models`
- `GET /admin/models?page=1&pageSize=10`
- `GET /admin/models/{id}`
- `PUT /admin/models/{id}`
- `PATCH /admin/models/{id}/status?status=0`
- `DELETE /admin/models/{id}`

查询参数：`endpointId`、`modelCode`、`status`

### 8.2 服务商模型映射

资源：`provider_models`

- `POST /admin/provider-models`
- `GET /admin/provider-models?page=1&pageSize=10`
- `GET /admin/provider-models/{id}`
- `PUT /admin/provider-models/{id}`
- `PATCH /admin/provider-models/{id}/status?status=0`
- `DELETE /admin/provider-models/{id}`

查询参数：`providerId`、`modelId`、`providerModelCode`、`status`

## 9. 客户 Token 管理

资源：`customer_tokens`

说明：

- 创建成功后只返回一次明文 `tokenValue`
- 后续列表和详情不再返回明文 Token
- `customerName` 支持传用户名、手机号或邮箱，后端会自动解析为 `accountId`

- `POST /admin/customer-tokens`
- `GET /admin/customer-tokens?page=1&pageSize=10`
- `GET /admin/customer-tokens/{id}`
- `PUT /admin/customer-tokens/{id}`
- `PATCH /admin/customer-tokens/{id}/status?status=0`
- `DELETE /admin/customer-tokens/{id}`

查询参数：`customerName`、`status`

## 10. 套餐管理

资源：`plans`

说明：

- `allowedModels` 使用 JSON 字符串数组，例如 `["gpt-5.2","gpt-5.4"]`
- 后端会校验 `allowedModels` 中引用的模型编码是否存在
- 套餐支持绑定单独的第三方支付链接，保存在 `third_party_pay_links` 表中

### 10.1 新增套餐

- `POST /admin/plans`

```json
{
  "planName": "标准套餐",
  "price": 59.9,
  "durationDays": 30,
  "dailyQuota": 30,
  "totalQuota": 900,
  "allowedModels": "[\"gpt-5.2\",\"gpt-5.3\",\"deepseek-chat\"]",
  "status": 1,
  "visible": 1,
  "payUrl": "https://dwz.cn/0Nj6pooi",
  "payLinkStatus": 1,
  "remark": "适合日常开发与测试"
}
```

### 10.2 套餐列表

- `GET /admin/plans?page=1&pageSize=10`

查询参数：

- `planName`
- `status`
- `visible`

### 10.3 套餐详情

- `GET /admin/plans/{id}`

### 10.4 更新套餐

- `PUT /admin/plans/{id}`

说明：

- `payUrl = ""` 表示删除已绑定的支付链接
- 只传需要更新的字段即可

### 10.5 套餐状态切换

- `PATCH /admin/plans/{id}/status?status=0`

### 10.6 套餐展示状态切换

- `PATCH /admin/plans/{id}/visible?visible=0`

### 10.7 删除套餐

- `DELETE /admin/plans/{id}`

约束：

- 若套餐下已有激活码，不允许直接删除
- 若套餐已产生客户订阅记录，不允许直接删除

### 10.8 套餐返回字段

```json
{
  "id": 10002,
  "planName": "标准套餐",
  "price": 59.9,
  "durationDays": 30,
  "dailyQuota": 30,
  "totalQuota": 900,
  "allowedModels": "[\"gpt-5.2\",\"gpt-5.3\",\"deepseek-chat\"]",
  "status": 1,
  "visible": 1,
  "payUrl": "https://dwz.cn/0Nj6pooi",
  "payLinkStatus": 1,
  "remark": "适合日常开发与测试",
  "createdAt": "2026-04-02T12:00:00",
  "updatedAt": "2026-04-02T12:00:00"
}
```

## 11. 套餐激活码管理

资源：`activation_code_stocks`

状态定义：

- `1`：可用
- `0`：禁用
- `2`：已使用

### 11.1 批量生成激活码

- `POST /admin/activation-codes/generate`

```json
{
  "planId": 10002,
  "quantity": 5,
  "codeLength": 12,
  "prefix": "STD",
  "remark": "4 月活动批次"
}
```

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "planId": 10002,
    "planName": "标准套餐",
    "generatedCount": 5,
    "codes": [
      "STD-8YQ2J6MX4KPW",
      "STD-2Q9L7MNR5XCY"
    ]
  }
}
```

### 11.2 激活码列表

- `GET /admin/activation-codes?page=1&pageSize=10`

查询参数：

- `planId`
- `status`
- `activationCode`
- `usedAccount`：支持用户名 / 手机号 / 邮箱精确匹配
- `subscriptionId`
- `orderId`

### 11.3 激活码详情

- `GET /admin/activation-codes/{id}`

### 11.4 更新激活码

- `PUT /admin/activation-codes/{id}`

```json
{
  "planId": 10003,
  "orderId": "APR20260402001",
  "remark": "升级到旗舰套餐"
}
```

说明：

- 已使用的激活码不允许修改套餐、订单号，也不允许删除
- 已使用的激活码仍允许补充备注

### 11.5 激活码状态切换

- `PATCH /admin/activation-codes/{id}/status?status=0`

说明：

- 仅允许在 `可用` 与 `禁用` 间切换
- 已使用激活码不可变更状态

### 11.6 删除激活码

- `DELETE /admin/activation-codes/{id}`

### 11.7 激活码返回字段

```json
{
  "id": 1,
  "activationCode": "STD-8YQ2J6MX4KPW",
  "planId": 10002,
  "planName": "标准套餐",
  "status": 1,
  "usedAt": null,
  "usedBy": null,
  "usedAccount": null,
  "subscriptionId": null,
  "orderId": null,
  "remark": "4 月活动批次",
  "createdAt": "2026-04-02T12:30:00",
  "updatedAt": "2026-04-02T12:30:00"
}
```

## 12. 套餐订阅记录

资源：`customer_plans`

### 12.1 订阅记录列表

- `GET /admin/subscriptions?page=1&pageSize=10`

查询参数：

- `accountKeyword`：支持用户名 / 手机号 / 邮箱模糊匹配
- `planId`
- `status`
- `source`：如 `activation_code` / `purchase` / `grant` / `admin`

### 12.2 订阅记录详情

- `GET /admin/subscriptions/{id}`

### 12.3 订阅记录返回字段

```json
{
  "id": 1,
  "accountId": 10001,
  "accountName": "demo_user",
  "accountPhone": "13800138000",
  "accountEmail": "demo@example.com",
  "accountStatus": 1,
  "planId": 10002,
  "planName": "标准套餐",
  "price": 59.90,
  "durationDays": 30,
  "dailyQuota": 30,
  "totalQuota": 900,
  "usedQuota": 5.20,
  "totalUsedQuota": 120.50,
  "dailyRemainingQuota": 24.80,
  "totalRemainingQuota": 779.50,
  "quotaRefreshTime": "2026-04-02T00:00:00",
  "planExpireTime": "2026-05-01T23:59:59",
  "status": 1,
  "source": "activation_code",
  "remark": "4 月活动兑换",
  "createdAt": "2026-04-02T12:30:00",
  "updatedAt": "2026-04-02T13:00:00"
}
```

## 13. 支付链接管理

资源：`third_party_pay_links`

说明：

- 当前仅开放 `plan` 类型支付链接管理

### 13.1 支付链接列表

- `GET /admin/pay-links?page=1&pageSize=10`

查询参数：

- `targetId`：套餐 ID
- `planName`：套餐名称模糊匹配
- `status`

### 13.2 新增支付链接

- `POST /admin/pay-links`

```json
{
  "targetId": 10002,
  "payUrl": "https://dwz.cn/0Nj6pooi",
  "status": 1,
  "remark": "4 月站外投放链接"
}
```

### 13.3 支付链接详情

- `GET /admin/pay-links/{id}`

### 13.4 更新支付链接

- `PUT /admin/pay-links/{id}`

```json
{
  "payUrl": "https://dwz.cn/new-pay-link",
  "status": 1,
  "remark": "更新活动落地页"
}
```

### 13.5 支付链接状态切换

- `PATCH /admin/pay-links/{id}/status?status=0`

### 13.6 删除支付链接

- `DELETE /admin/pay-links/{id}`

### 13.7 支付链接返回字段

```json
{
  "id": 1,
  "targetId": 10002,
  "targetType": "plan",
  "planName": "标准套餐",
  "planStatus": 1,
  "planVisible": 1,
  "payUrl": "https://dwz.cn/0Nj6pooi",
  "status": 1,
  "remark": "4 月站外投放链接",
  "createdAt": "2026-04-02T12:30:00",
  "updatedAt": "2026-04-02T13:00:00"
}
```

## 14. 前端对接建议

建议后台前端路由补充：

- `/login`
- `/dashboard`
- `/providers`
- `/provider-tokens`
- `/customer-tokens`
- `/models`
- `/plans`
- `/subscriptions`
- `/activation-codes`
- `/activation-usage`
- `/pay-links`

开发联调约定：

- 前端默认请求相对路径 `/admin/**`
- Vite 开发环境代理到 `http://localhost:8080`
- 若前后端分离部署，可通过 `VITE_ADMIN_API_BASE_URL` 指定后端地址
