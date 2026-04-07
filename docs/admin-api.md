# xlinks-router Admin API

鏈枃妗ｅ畾涔?`D:\project\xlinks-router\xlinks-router-admin` 涓?`D:\project\xlinks-router\xlinks-router-web\xlinks-router-admin` 涔嬮棿鐨勫悗鍙拌繍钀ユ帴鍙ｇ害瀹氥€?
## 鑳藉姏鑼冨洿

褰撳墠 admin 绔鐩栦互涓嬭兘鍔涳細

1. 绠＄悊鍛樿璇侊紙`admin_accounts`锛?2. Dashboard 杩愯惀鎬昏
3. 鍟嗘埛绠＄悊
4. 鏈嶅姟鍟?/ 鏈嶅姟鍟?Token 绠＄悊
5. 鏍囧噯绔偣 / 鏍囧噯妯″瀷 / 鏈嶅姟鍟嗘ā鍨嬫槧灏勭鐞?6. 瀹㈡埛 Token 绠＄悊
7. 濂楅绠＄悊
8. 濂楅璁㈤槄璁板綍绠＄悊
9. 婵€娲荤爜搴撳瓨涓庝娇鐢ㄨ褰曠鐞?10. 鏀粯鏂瑰紡绠＄悊
11. 鏀粯閾炬帴绠＄悊

## 閫氱敤绾﹀畾

- 鍩虹璺緞锛歚/admin`
- 閴存潈鏂瑰紡锛歚Authorization: Bearer <accessToken>`
- 閫氱敤鐘舵€侊細`1 = 鍚敤`锛宍0 = 鍋滅敤`
- 鍒嗛〉鍙傛暟锛歚page`銆乣pageSize`
- 鍒楄〃鍝嶅簲锛歚Result<PageResult<T>>`
- 鏅€氬搷搴旓細`Result<T>`

缁熶竴鍝嶅簲绀轰緥锛?
```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

鍒嗛〉鍝嶅簲绀轰緥锛?
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

甯哥敤閿欒鐮侊細

- `4001`锛氬弬鏁伴敊璇垨涓氬姟鏍￠獙澶辫触
- `4010`锛氭湭鐧诲綍鎴栫鐞嗗憳 Token 鏃犳晥
- `4014`锛氱鐞嗗憳璐﹀彿宸插仠鐢?- `5000`锛氱郴缁熷紓甯?
## 绠＄悊鍛樿璇?
- `POST /admin/auth/login`
- `GET /admin/auth/me`
- `POST /admin/auth/logout`

鐧诲綍璇锋眰绀轰緥锛?
```json
{
  "username": "admin",
  "password": "admin123"
}
```

## Dashboard

- `GET /admin/dashboard/overview`

杩斿洖瀛楁鍖呮嫭锛?
- `merchantCount`
- `activeMerchantCount`
- `providerCount`
- `activeProviderCount`
- `modelCount`
- `providerModelCount`
- `providerTokenCount`
- `customerTokenCount`
- `expiringTokenCount`

## 鍟嗘埛绠＄悊

璧勬簮锛歚merchants`

- `GET /admin/merchants?page=1&pageSize=10`
- `GET /admin/merchants/{id}`
- `PUT /admin/merchants/{id}`
- `PATCH /admin/merchants/{id}/status?status=0`

鏌ヨ鍙傛暟锛?
- `keyword`锛氭敮鎸佺敤鎴峰悕 / 鎵嬫満鍙?/ 閭鎼滅储
- `status`

璇存槑锛?
- 褰撳墠寮€鏀捐繍钀ュ娉ㄧ淮鎶わ細`remark`
- 鍟嗘埛鍚仠浠呮敮鎸?`0 / 1`

## 鏈嶅姟鍟嗕笌 Token

鏈嶅姟鍟嗚祫婧愶細`providers`

- `POST /admin/providers`
- `GET /admin/providers`
- `GET /admin/providers/{id}`
- `PUT /admin/providers/{id}`
- `PATCH /admin/providers/{id}/status`
- `DELETE /admin/providers/{id}`

鏈嶅姟鍟?Token 璧勬簮锛歚provider_tokens`

- `POST /admin/provider-tokens`
- `GET /admin/provider-tokens`
- `GET /admin/provider-tokens/{id}`
- `PUT /admin/provider-tokens/{id}`
- `PATCH /admin/provider-tokens/{id}/status`
- `DELETE /admin/provider-tokens/{id}`

## 妯″瀷绠＄悊

### 鏍囧噯妯″瀷

璧勬簮锛歚models`

- `POST /admin/models`
- `GET /admin/models`
- `GET /admin/models/{id}`
- `PUT /admin/models/{id}`
- `PATCH /admin/models/{id}/status`
- `DELETE /admin/models/{id}`

鏌ヨ鍙傛暟锛歚modelCode`銆乣status`

### 鏈嶅姟鍟嗘ā鍨嬫槧灏?
璧勬簮锛歚provider_models`

- `POST /admin/provider-models`
- `GET /admin/provider-models`
- `GET /admin/provider-models/{id}`
- `PUT /admin/provider-models/{id}`
- `PATCH /admin/provider-models/{id}/status`
- `DELETE /admin/provider-models/{id}`

## 瀹㈡埛 Token 绠＄悊

璧勬簮锛歚customer_tokens`

- `POST /admin/customer-tokens`
- `GET /admin/customer-tokens`
- `GET /admin/customer-tokens/{id}`
- `PUT /admin/customer-tokens/{id}`
- `PATCH /admin/customer-tokens/{id}/status`
- `DELETE /admin/customer-tokens/{id}`

璇存槑锛?
- 鍒涘缓鎴愬姛鍚庝粎杩斿洖涓€娆℃槑鏂?`tokenValue`
- 鍚庣画鍒楄〃鍜岃鎯呬笉鍐嶈繑鍥炴槑鏂?Token
- `customerName` 鏀寔鐢ㄦ埛鍚嶃€佹墜鏈哄彿銆侀偖绠辫嚜鍔ㄨВ鏋愪负 `accountId`

## 濂楅杩愯惀

### 濂楅绠＄悊

璧勬簮锛歚plans`

- `POST /admin/plans`
- `GET /admin/plans`
- `GET /admin/plans/{id}`
- `PUT /admin/plans/{id}`
- `PATCH /admin/plans/{id}/status`
- `PATCH /admin/plans/{id}/visible`
- `DELETE /admin/plans/{id}`

璇存槑锛?
- `allowedModels` 浣跨敤 JSON 瀛楃涓叉暟缁?- `payUrl` 浠嶉€氳繃 `third_party_pay_links` 涓哄椁愮粦瀹氫笓灞炴敮浠橀摼鎺?
### 濂楅璁㈤槄璁板綍

璧勬簮锛歚customer_plans`

- `GET /admin/subscriptions`
- `GET /admin/subscriptions/{id}`

### 婵€娲荤爜搴撳瓨

璧勬簮锛歚activation_code_stocks`

- `POST /admin/activation-codes/generate`
- `GET /admin/activation-codes`
- `GET /admin/activation-codes/{id}`
- `PUT /admin/activation-codes/{id}`
- `PATCH /admin/activation-codes/{id}/status`
- `DELETE /admin/activation-codes/{id}`

### 婵€娲荤爜浣跨敤璁板綍

- 涓?`activation_code_stocks` 鍒楄〃/璇︽儏鑱斿姩杩斿洖
- 鍓嶇杩愯惀椤佃矾鐢憋細`/activation-usage`

## 鏀粯绠＄悊

### 鏀粯鏂瑰紡绠＄悊

璧勬簮锛歚payment_methods`

- `POST /admin/payment-methods`
- `GET /admin/payment-methods`
- `GET /admin/payment-methods/{id}`
- `PUT /admin/payment-methods/{id}`
- `PATCH /admin/payment-methods/{id}/status`
- `DELETE /admin/payment-methods/{id}`

瀛楁璇存槑锛?
- `methodCode`锛氭敮浠樻柟寮忕紪鐮侊紝鍞竴
- `methodName`锛氭敮浠樻柟寮忓悕绉?- `methodType`锛氭笭閬撶被鍨嬶紝濡?`alipay` / `wechat` / `local`
- `iconUrl`锛氬浘鏍囧湴鍧€锛屽彲閫?- `sort`锛氭帓搴忓€?- `status`锛氬惎鍋滅姸鎬?- `configJson`锛氭敮浠樺弬鏁?JSON 瀛楃涓?- `remark`锛氳繍钀ュ娉?
鍒涘缓绀轰緥锛?
```json
{
  "methodCode": "alipay_official",
  "methodName": "鏀粯瀹濆畼鏂规敹娆?,
  "methodType": "alipay",
  "sort": 10,
  "status": 1,
  "configJson": "{\"appId\":\"demo-app\",\"merchantId\":\"2088xxxx\",\"notifyUrl\":\"https://example.com/pay/notify/alipay\"}",
  "remark": "榛樿鏀粯瀹濋厤缃?
}
```

### 鏀粯閾炬帴绠＄悊

璧勬簮锛歚third_party_pay_links`

- `POST /admin/pay-links`
- `GET /admin/pay-links`
- `GET /admin/pay-links/{id}`
- `PUT /admin/pay-links/{id}`
- `PATCH /admin/pay-links/{id}/status`
- `DELETE /admin/pay-links/{id}`

璇存槑锛?
- 褰撳墠鐢ㄤ簬缁存姢濂楅缁村害鐨勬敮浠橀摼鎺?- 鍓嶇鑿滃崟褰掑睘锛歚鏀粯绠＄悊 -> 鏀粯閾炬帴绠＄悊`

## 鍓嶇瀵规帴寤鸿

寤鸿鍚庡彴鍓嶇缁存姢浠ヤ笅璺敱锛?
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

鑱旇皟绾﹀畾锛?
- 鍓嶇榛樿璇锋眰鐩稿璺緞 `/admin/**`
- Vite 寮€鍙戠幆澧冧唬鐞嗗埌 `http://localhost:8080`
- 鑻ュ墠鍚庣鍒嗙閮ㄧ讲锛屽彲閫氳繃 `VITE_ADMIN_API_BASE_URL` 鎸囧畾鍚庣鍦板潃

## 2026-04-07 缓存命中计费字段变更

### Provider

`/admin/providers` 新增字段：

- `cacheHitStrategy`：缓存命中策略（`none` / `openai_cached_tokens` / `anthropic_cache_read_input_tokens`）

### Model

`/admin/models` 新增字段：

- `cacheHitPrice`：缓存命中输入 Token 的单价（按每百万 Token）

### Usage Record

`usage_records` 新增落库字段：

- `cache_hit_tokens`
- `cache_hit_cost`

计费公式更新为：

`(inputTokens - cacheHitTokens) * inputPrice + cacheHitTokens * cacheHitPrice + outputTokens * outputPrice`


