# xlinks-router Admin API

本文档定义 `D:\project\xlinks-router\xlinks-router-admin` 后端与 `D:\project\xlinks-router\xlinks-router-web\xlinks-router-admin` 前端之间的后台运营接口约定。

## 1. 能力范围

当前 admin 端已覆盖以下能力：

1. 管理员认证（基于 `admin_accounts`）
2. Dashboard 运营总览
3. 商户管理
4. 服务商管理
5. 服务商 Token 管理
6. 标准端点管理
7. 标准模型管理
8. 服务商模型映射管理
9. 客户 Token 管理
10. 套餐管理
11. 套餐订阅记录管理
12. 激活码库存管理
13. 激活码使用记录查询
14. 支付方式管理
15. 支付链接管理

## 2. 通用约定

- 基础路径：`/api`
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

管理员账号存储于 `admin_accounts`，不复用商户 / 客户账号表。

### 3.1 登录

- `POST /api/auth/login`

```json
{
  "username": "admin",
  "password": "admin123"
}
```

### 3.2 当前登录信息

- `GET /api/auth/me`

### 3.3 退出登录

- `POST /api/auth/logout`

## 4. Dashboard

### 4.1 总览数据

- `GET /api/dashboard/overview`

返回字段包括：

- `merchantCount`
- `activeMerchantCount`
- `providerCount`
- `activeProviderCount`
- `endpointCount`
- `modelCount`
- `providerModelCount`
- `providerTokenCount`
- `customerTokenCount`
- `expiringTokenCount`

## 5. 商户管理

资源：`merchants`

- `GET /api/merchants?page=1&pageSize=10`
- `GET /api/merchants/{id}`
- `PUT /api/merchants/{id}`
- `PATCH /api/merchants/{id}/status?status=0`

查询参数：

- `keyword`：支持用户名 / 手机号 / 邮箱搜索
- `status`

更新说明：

- 当前开放运营备注维护字段：`remark`
- 商户启停仅支持 `0 / 1`

## 6. 服务商管理

资源：`providers`

- `POST /api/providers`
- `GET /api/providers?page=1&pageSize=10`
- `GET /api/providers/{id}`
- `PUT /api/providers/{id}`
- `PATCH /api/providers/{id}/status?status=0`
- `DELETE /api/providers/{id}`

查询参数：`providerCode`、`providerName`、`status`

## 7. 服务商 Token 管理

资源：`provider_tokens`

- `POST /api/provider-tokens`
- `GET /api/provider-tokens?page=1&pageSize=10`
- `GET /api/provider-tokens/{id}`
- `PUT /api/provider-tokens/{id}`
- `PATCH /api/provider-tokens/{id}/status?status=0`
- `DELETE /api/provider-tokens/{id}`

查询参数：`providerId`、`tokenStatus`

## 8. 模型管理

模型管理包含标准端点、标准模型、服务商模型映射三部分。

### 8.1 标准端点

资源：`model_endpoints`

- `POST /api/model-endpoints`
- `GET /api/model-endpoints?page=1&pageSize=10`
- `GET /api/model-endpoints/{id}`
- `PUT /api/model-endpoints/{id}`
- `PATCH /api/model-endpoints/{id}/status?status=0`
- `DELETE /api/model-endpoints/{id}`

查询参数：`endpointName`、`status`

### 8.2 标准模型

资源：`models`

- `POST /api/models`
- `GET /api/models?page=1&pageSize=10`
- `GET /api/models/{id}`
- `PUT /api/models/{id}`
- `PATCH /api/models/{id}/status?status=0`
- `DELETE /api/models/{id}`

查询参数：`endpointId`、`modelCode`、`status`

### 8.3 服务商模型映射

资源：`provider_models`

- `POST /api/provider-models`
- `GET /api/provider-models?page=1&pageSize=10`
- `GET /api/provider-models/{id}`
- `PUT /api/provider-models/{id}`
- `PATCH /api/provider-models/{id}/status?status=0`
- `DELETE /api/provider-models/{id}`

查询参数：`providerId`、`modelId`、`providerModelCode`、`status`

## 9. 客户 Token 管理

资源：`customer_tokens`

说明：

- 创建成功后仅返回一次明文 `tokenValue`
- 后续列表和详情不再返回明文 Token
- `customerName` 支持传用户名、手机号或邮箱，后端自动解析为 `accountId`

- `POST /api/customer-tokens`
- `GET /api/customer-tokens?page=1&pageSize=10`
- `GET /api/customer-tokens/{id}`
- `PUT /api/customer-tokens/{id}`
- `PATCH /api/customer-tokens/{id}/status?status=0`
- `DELETE /api/customer-tokens/{id}`

查询参数：`customerName`、`status`

## 10. 套餐管理

资源：`plans`

说明：

- `allowedModels` 使用 JSON 字符串数组，例如 `["gpt-5.2","gpt-5.4"]`
- 后端会校验 `allowedModels` 中引用的模型编码是否存在
- 套餐支持绑定独立的第三方支付链接，数据存放于 `third_party_pay_links`

### 10.1 套餐接口

- `POST /api/plans`
- `GET /api/plans?page=1&pageSize=10`
- `GET /api/plans/{id}`
- `PUT /api/plans/{id}`
- `PATCH /api/plans/{id}/status?status=0`
- `PATCH /api/plans/{id}/visible?visible=0`
- `DELETE /api/plans/{id}`

查询参数：`planName`、`status`、`visible`

## 11. 套餐订阅记录

资源：`customer_plans`

- `GET /api/subscriptions?page=1&pageSize=10`
- `GET /api/subscriptions/{id}`

查询参数：

- `accountKeyword`
- `planId`
- `status`
- `source`

## 12. 激活码库存

资源：`activation_code_stocks`

- `POST /api/activation-codes/generate`
- `GET /api/activation-codes?page=1&pageSize=10`
- `GET /api/activation-codes/{id}`
- `PUT /api/activation-codes/{id}`
- `PATCH /api/activation-codes/{id}/status?status=0`
- `DELETE /api/activation-codes/{id}`

查询参数：

- `planId`
- `status`
- `activationCode`
- `usedAccount`
- `subscriptionId`
- `orderId`

## 13. 激活码使用记录

- 当前通过激活码列表与详情页联动展示使用信息
- 前端路由：`/activation-usage`

## 14. 支付方式管理

资源：`payment_methods`

- `POST /api/payment-methods`
- `GET /api/payment-methods?page=1&pageSize=10`
- `GET /api/payment-methods/{id}`
- `PUT /api/payment-methods/{id}`
- `PATCH /api/payment-methods/{id}/status?status=0`
- `DELETE /api/payment-methods/{id}`

查询参数：

- `keyword`：支持支付方式编码 / 名称
- `methodType`
- `status`

字段说明：

- `methodCode`：支付方式编码，唯一
- `methodName`：支付方式名称
- `methodType`：渠道类型，如 `alipay` / `wechat` / `local`
- `iconUrl`：图标地址，可选
- `sort`：排序值
- `status`：启停状态
- `configJson`：支付参数 JSON 字符串
- `remark`：运营备注

创建示例：

```json
{
  "methodCode": "alipay_official",
  "methodName": "支付宝官方收款",
  "methodType": "alipay",
  "sort": 10,
  "status": 1,
  "configJson": "{\"appId\":\"demo-app\",\"merchantId\":\"2088xxxx\",\"notifyUrl\":\"https://example.com/pay/notify/alipay\"}",
  "remark": "默认支付宝配置"
}
```

## 15. 支付链接管理

资源：`third_party_pay_links`

- `POST /api/pay-links`
- `GET /api/pay-links?page=1&pageSize=10`
- `GET /api/pay-links/{id}`
- `PUT /api/pay-links/{id}`
- `PATCH /api/pay-links/{id}/status?status=0`
- `DELETE /api/pay-links/{id}`

说明：

- 当前用于维护套餐维度的支付链接
- 前端菜单归属：`支付管理 -> 支付链接管理`

## 16. 前端对接建议

建议后台前端维护以下路由：

- `/login`
- `/dashboard`
- `/merchants`
- `/providers`
- `/provider-tokens`
- `/customer-tokens`
- `/models`
- `/plans`
- `/subscriptions`
- `/activation-codes`
- `/activation-usage`
- `/payment-methods`
- `/pay-links`

联调约定：

- 前端默认请求相对路径 `/api/**`
- Vite 开发环境代理到 `http://localhost:8080`
- 若前后端分离部署，可通过 `VITE_ADMIN_API_BASE_URL` 指定后端地址

## 17. 兼容性说明

- 本次已将 admin 后端与 admin 前端的接口前缀从 `/admin/**` 统一调整为 `/api/**`
- 若存在历史脚本、测试用例或联调记录仍引用旧路径，需要同步切换到新前缀
