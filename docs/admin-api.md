# xlinks-router Admin API

本文档定义 `D:\project\xlinks-router\xlinks-router-admin` 与 `D:\project\xlinks-router\xlinks-router-web\xlinks-router-admin` 之间的后台运营接口约定。

## 能力范围

当前 admin 端覆盖以下能力：

1. 管理员认证（`admin_accounts`）
2. Dashboard 运营总览
3. 商户管理
4. 服务商 / 服务商 Token 管理
5. 标准端点 / 标准模型 / 服务商模型映射管理
6. 客户 Token 管理
7. 套餐管理
8. 套餐订阅记录管理
9. 激活码库存与使用记录管理
10. 支付方式管理
11. 支付链接管理

## 通用约定

- 基础路径：`/admin`
- 鉴权方式：`Authorization: Bearer <accessToken>`
- 通用状态：`1 = 启用`，`0 = 停用`
- 分页参数：`page`、`pageSize`
- 列表响应：`Result<PageResult<T>>`
- 普通响应：`Result<T>`

统一响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

分页响应示例：

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

常用错误码：

- `4001`：参数错误或业务校验失败
- `4010`：未登录或管理员 Token 无效
- `4014`：管理员账号已停用
- `5000`：系统异常

## 管理员认证

- `POST /admin/auth/login`
- `GET /admin/auth/me`
- `POST /admin/auth/logout`

登录请求示例：

```json
{
  "username": "admin",
  "password": "admin123"
}
```

## Dashboard

- `GET /admin/dashboard/overview`

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

## 商户管理

资源：`merchants`

- `GET /admin/merchants?page=1&pageSize=10`
- `GET /admin/merchants/{id}`
- `PUT /admin/merchants/{id}`
- `PATCH /admin/merchants/{id}/status?status=0`

查询参数：

- `keyword`：支持用户名 / 手机号 / 邮箱搜索
- `status`

说明：

- 当前开放运营备注维护：`remark`
- 商户启停仅支持 `0 / 1`

## 服务商与 Token

服务商资源：`providers`

- `POST /admin/providers`
- `GET /admin/providers`
- `GET /admin/providers/{id}`
- `PUT /admin/providers/{id}`
- `PATCH /admin/providers/{id}/status`
- `DELETE /admin/providers/{id}`

服务商 Token 资源：`provider_tokens`

- `POST /admin/provider-tokens`
- `GET /admin/provider-tokens`
- `GET /admin/provider-tokens/{id}`
- `PUT /admin/provider-tokens/{id}`
- `PATCH /admin/provider-tokens/{id}/status`
- `DELETE /admin/provider-tokens/{id}`

## 模型管理

### 标准端点

资源：`model_endpoints`

- `POST /admin/model-endpoints`
- `GET /admin/model-endpoints`
- `GET /admin/model-endpoints/{id}`
- `PUT /admin/model-endpoints/{id}`
- `PATCH /admin/model-endpoints/{id}/status`
- `DELETE /admin/model-endpoints/{id}`

### 标准模型

资源：`models`

- `POST /admin/models`
- `GET /admin/models`
- `GET /admin/models/{id}`
- `PUT /admin/models/{id}`
- `PATCH /admin/models/{id}/status`
- `DELETE /admin/models/{id}`

查询参数：`endpointId`、`modelCode`、`status`

### 服务商模型映射

资源：`provider_models`

- `POST /admin/provider-models`
- `GET /admin/provider-models`
- `GET /admin/provider-models/{id}`
- `PUT /admin/provider-models/{id}`
- `PATCH /admin/provider-models/{id}/status`
- `DELETE /admin/provider-models/{id}`

## 客户 Token 管理

资源：`customer_tokens`

- `POST /admin/customer-tokens`
- `GET /admin/customer-tokens`
- `GET /admin/customer-tokens/{id}`
- `PUT /admin/customer-tokens/{id}`
- `PATCH /admin/customer-tokens/{id}/status`
- `DELETE /admin/customer-tokens/{id}`

说明：

- 创建成功后仅返回一次明文 `tokenValue`
- 后续列表和详情不再返回明文 Token
- `customerName` 支持用户名、手机号、邮箱自动解析为 `accountId`

## 套餐运营

### 套餐管理

资源：`plans`

- `POST /admin/plans`
- `GET /admin/plans`
- `GET /admin/plans/{id}`
- `PUT /admin/plans/{id}`
- `PATCH /admin/plans/{id}/status`
- `PATCH /admin/plans/{id}/visible`
- `DELETE /admin/plans/{id}`

说明：

- `allowedModels` 使用 JSON 字符串数组
- `payUrl` 仍通过 `third_party_pay_links` 为套餐绑定专属支付链接

### 套餐订阅记录

资源：`customer_plans`

- `GET /admin/subscriptions`
- `GET /admin/subscriptions/{id}`

### 激活码库存

资源：`activation_code_stocks`

- `POST /admin/activation-codes/generate`
- `GET /admin/activation-codes`
- `GET /admin/activation-codes/{id}`
- `PUT /admin/activation-codes/{id}`
- `PATCH /admin/activation-codes/{id}/status`
- `DELETE /admin/activation-codes/{id}`

### 激活码使用记录

- 与 `activation_code_stocks` 列表/详情联动返回
- 前端运营页路由：`/activation-usage`

## 支付管理

### 支付方式管理

资源：`payment_methods`

- `POST /admin/payment-methods`
- `GET /admin/payment-methods`
- `GET /admin/payment-methods/{id}`
- `PUT /admin/payment-methods/{id}`
- `PATCH /admin/payment-methods/{id}/status`
- `DELETE /admin/payment-methods/{id}`

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

### 支付链接管理

资源：`third_party_pay_links`

- `POST /admin/pay-links`
- `GET /admin/pay-links`
- `GET /admin/pay-links/{id}`
- `PUT /admin/pay-links/{id}`
- `PATCH /admin/pay-links/{id}/status`
- `DELETE /admin/pay-links/{id}`

说明：

- 当前用于维护套餐维度的支付链接
- 前端菜单归属：`支付管理 -> 支付链接管理`

## 前端对接建议

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

- 前端默认请求相对路径 `/admin/**`
- Vite 开发环境代理到 `http://localhost:8080`
- 若前后端分离部署，可通过 `VITE_ADMIN_API_BASE_URL` 指定后端地址
