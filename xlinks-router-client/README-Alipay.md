# 支付宝集成说明

## 概述
本项目已集成支付宝支付功能，支持电脑网站支付、订单查询、退款等操作。

## 功能特性
- ✅ 电脑网站支付
- ✅ 支付同步回调处理
- ✅ 支付异步通知处理
- ✅ 订单状态查询
- ✅ 订单退款
- ✅ RSA2 签名验证
- ✅ 沙箱/正式环境切换

## 配置说明

### 1. 环境变量配置
在启动应用前，需要设置以下环境变量：

```bash
# 沙箱环境
export ALIPAY_SANDBOX_APP_ID=your-sandbox-app-id
export ALIPAY_SANDBOX_PRIVATE_KEY=your-private-key
export ALIPAY_SANDBOX_PUBLIC_KEY=your-public-key
export ALIPAY_SANDBOX_ALIPAY_PUBLIC_KEY=your-alipay-public-key

# 正式环境
export ALIPAY_APP_ID=your-app-id
export ALIPAY_PRIVATE_KEY=your-private-key
export ALIPAY_PUBLIC_KEY=your-public-key
export ALIPAY_ALIPAY_PUBLIC_KEY=your-alipay-public-key
```

### 2. application.yml 配置
也可以直接在 `application.yml` 中配置：

```yaml
alipay:
  sandbox:
    enabled: true
    app-id: ${ALIPAY_SANDBOX_APP_ID:your-sandbox-app-id}
    private-key: ${ALIPAY_SANDBOX_PRIVATE_KEY:your-private-key}
    public-key: ${ALIPAY_SANDBOX_PUBLIC_KEY:your-public-key}
    alipay-public-key: ${ALIPAY_SANDBOX_ALIPAY_PUBLIC_KEY:your-alipay-public-key}
    gateway-url: https://openapi.alipaydev.com/gateway.do
    charset: UTF-8
    sign-type: RSA2
    format: json
    return-url: http://localhost:8082/alipay/return
    notify-url: http://localhost:8082/alipay/notify
```

## API 接口说明

### 1. 电脑网站支付
```http
POST /alipay/pay
Content-Type: application/json

{
  "outTradeNo": "202404060001",
  "totalAmount": 0.01,
  "subject": "测试商品",
  "body": "这是一个测试商品"
}
```

### 2. 订单查询
```http
POST /alipay/query
Content-Type: application/json

{
  "outTradeNo": "202404060001",
  "tradeNo": "2024040622001234567890123456"
}
```

### 3. 订单退款
```http
POST /alipay/refund
Content-Type: application/json

{
  "outTradeNo": "202404060001",
  "refundAmount": 0.01,
  "outRequestNo": "202404060001001",
  "refundReason": "商品质量问题"
}
```

## 回调地址

### 同步回调
- 地址：`GET /alipay/return`
- 说明：用户支付完成后跳转的页面

### 异步通知
- 地址：`POST /alipay/notify`
- 说明：支付宝服务器异步通知支付结果

## 测试流程

### 1. 获取沙箱账号
1. 登录支付宝开放平台：https://open.alipay.com/
2. 进入沙箱环境
3. 获取沙箱应用ID和测试账号

### 2. 生成密钥对
1. 下载支付宝密钥生成工具
2. 生成RSA2密钥对
3. 上传商户公钥到支付宝开放平台
4. 获取支付宝公钥

### 3. 配置参数
1. 设置环境变量或修改 application.yml
2. 启动应用

### 4. 测试支付
1. 调用 `/alipay/pay` 接口创建支付订单
2. 使用沙箱账号完成支付
3. 验证回调处理是否正常

## 注意事项

1. **密钥安全**：私钥请妥善保管，不要提交到代码仓库
2. **回调地址**：确保回调地址在公网可以访问（测试时可以使用内网穿透工具）
3. **签名验证**：所有回调都必须验证签名，防止伪造请求
4. **幂等性**：处理异步通知时要注意幂等性，避免重复处理
5. **日志记录**：建议记录所有支付相关的日志，便于问题排查

## 常见问题

### Q: 如何获取支付宝公钥？
A: 在支付宝开放平台上传商户公钥后，平台会生成对应的支付宝公钥。

### Q: 回调地址无法访问怎么办？
A: 可以使用内网穿透工具（如 ngrok）将本地服务映射到公网。

### Q: 签名验证失败怎么办？
A: 检查密钥配置是否正确，确保使用的是RSA2算法。

## 依赖说明
- 支付宝SDK：`com.alipay.sdk:alipay-sdk-java:4.35.79.ALL`
- Servlet API：`javax.servlet:javax.servlet-api:4.0.1`

## 更多文档
- 支付宝开放平台文档：https://open.alipay.com/docs/doc.htm?spm=a219a.7629140.0.0.4b5e4c83VnG4tF
- 支付宝沙箱环境：https://open.alipay.com/develop/sandbox
