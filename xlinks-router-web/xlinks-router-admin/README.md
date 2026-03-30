# Xlinks Router Admin Web

运营端前端（静态页面与路由骨架）。

## 本地运行

```bash
cd xlinks-router-web/xlinks-router-admin
npm install
npm run dev
```

默认端口为 `5174`，可在 [`vite.config.js`](xlinks-router-web/xlinks-router-admin/vite.config.js:1) 中调整。

## 页面

- 登录：`/login`
- 概览：`/dashboard`
- 商户管理：`/merchants`
- 服务商管理：`/providers`
- Token 管理：`/tokens`
- 模型管理：`/models`
- 套餐管理：`/plans`
- 交易管理：`/trades`

## 说明

- 当前为静态数据与 UI 骨架，后续可接入后端接口。
- 认证状态使用本地缓存（Pinia + localStorage），仅用于 UI 演示。
