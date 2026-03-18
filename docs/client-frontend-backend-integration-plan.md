## 客户端前后端对接计划

### 当前结论

- 后端代理服务 `xlinks-router-client` 已运行在 `127.0.0.1:8082`，适合作为前端联调入口。
- `docs/client-api.md` 中列出的客户端接口，大部分已经在 `xlinks-router-client` 中提供了 mock controller。
- 当前前端代码里，已明确发现仍在本地直接使用 mock 数据的页面是 `xlinks-router-web/xlinks-router-web-vue/src/views/tokens/index.vue`。
- 该页面所需的接口在后端已经具备：
  - `GET /api/v1/customer-tokens`
  - `POST /api/v1/customer-tokens`
  - `PUT /api/v1/customer-tokens/{id}`
  - `DELETE /api/v1/customer-tokens/{id}`
  - `POST /api/v1/customer-tokens/{id}/refresh`

### 对接目标

1. 先完成 Token 管理页前后端联调，验证前端 -> `127.0.0.1:8082` -> client-api mock 的连通性。
2. 再按页面逐步替换其他前端页面中的静态数据或占位数据。
3. 若某页面对应接口缺失或字段不匹配，优先在 `xlinks-router-client` 中补 mock，保证前端可持续联调。

### 推荐实施顺序

#### 第 1 阶段：打通基础请求链路

- 在前端新增统一请求封装，例如 `src/utils/request.js`。
- 约定开发环境接口基地址指向 `http://127.0.0.1:8082`。
- 在 `vite.config.js` 中增加 `/api` 与 `/auth` 的代理，避免本地开发跨域。
- 优先不引入额外依赖，使用浏览器原生 `fetch` 即可。

#### 第 2 阶段：优先完成注册/登录闭环

- 优先替换 `xlinks-router-web/xlinks-router-web-vue/src/views/login/index.vue` 与 `xlinks-router-web/xlinks-router-web-vue/src/views/register/index.vue` 中的本地模拟逻辑。
- 注册页先接入：
  - `POST /auth/verify-code`
  - `POST /auth/register`
- 登录页接入：
  - `POST /auth/rsa-public-key`
  - `POST /auth/login`
- 登录成功后，将 `accessToken` 持久化到前端（建议 `localStorage`），为后续访问 `/api/v1/**` 接口做准备。
- 若暂时不实现真实 RSA 加密，可先完成接口联通与 token 存储，后续再补前端加密逻辑。

#### 第 3 阶段：完成 Token 页面替换

- 将 `tokens/index.vue` 中的 `mockTokens` 替换为真实接口加载。
- 页面初始化调用 `GET /api/v1/customer-tokens`。
- “创建 Token” 调用 `POST /api/v1/customer-tokens`。
- “删除” 调用 `DELETE /api/v1/customer-tokens/{id}`。
- “刷新” 调用 `POST /api/v1/customer-tokens/{id}/refresh`。
- 前端需要做字段映射：
  - `tokenName -> name`
  - `tokenValue -> key`
  - `createdAt -> created`
  - `lastUsedAt -> lastUsed`
  - `totalRequests -> requests`
  - `status: 1/0 -> active/inactive`

#### 第 4 阶段：联调验证

- 启动 `xlinks-router-client` 服务，确认 `8082` 可访问。
- 启动前端 Vite 服务。
- 优先验证注册、发送验证码、登录、token 落库（浏览器存储）是否正常。
- 在浏览器中验证 Token 列表加载、创建、删除、刷新是否正常。
- 使用浏览器网络面板或控制台确认请求确实发往 `127.0.0.1:8082`。

#### 第 5 阶段：扩展到其他页面

- 继续检查 `dashboard`、`models`、`plans`、`promotion`、`contact`、`login`、`register` 页面。
- 如果页面仍是静态渲染，则逐页替换为真实请求。
- 若后端字段与前端展示结构不一致，先做前端适配；若接口缺失，再补 `xlinks-router-client` mock。

### 风险与注意点

- 当前环境中的 `xlinks-router-client` 配置了真实 MySQL/Redis 连接；虽然现有 controller 主要返回 mock，但启动时仍可能受外部依赖影响，需要实际验证。
- `CustomerTokenController` 当前返回的分页记录总数直接取固定列表长度，暂时足够支持前端联调，但不适合后续正式分页。
- 前端当前未见统一状态管理和请求层，若直接在页面中散落 `fetch`，后续维护成本会升高，因此建议先补一个轻量请求工具。

### 建议的下一步

建议先落地最小闭环：

1. 增加前端代理与请求封装。
2. 优先改造 `login` / `register` 页面，拿到并保存 `accessToken`。
3. 再改造 `tokens/index.vue`，使用登录态测试受保护接口。
4. 测通后，再逐页推进其余模块。