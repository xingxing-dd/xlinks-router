# Repository Guidelines

## 项目结构与模块组织

本仓库是一个基于 Maven 的多模块大模型中转服务系统。

- `xlinks-router-common`：公共实体、DTO、工具类与基础能力
- `xlinks-router-admin`：管理后台后端
- `xlinks-router-api`：对外模型中转 API 与路由逻辑
- `xlinks-router-api-distributed`：面向分布式部署的 API 重构模块，用于承接 `xlinks-router-api` 的分布式化演进
- `xlinks-router-client`：用户侧后端服务
- `xlinks-router-web/xlinks-router-admin`：管理端前端，基于 Vue 3 + Vite
- `xlinks-router-web/xlinks-router-client`：客户端前端，基于 Vue 3 + Vite
- `docs/`：接口、数据库、架构与设计文档

Java 源码位于 `src/main/java`，测试代码位于 `src/test/java`，数据库初始化与迁移脚本位于 `xlinks-router-admin/src/main/resources/db`。

## 构建、测试与开发命令

- `mvn clean install -DskipTests`：构建全部后端模块并安装到本地仓库
- `mvn test`：运行后端全部测试
- `./start-backend.sh all`：启动全部后端模块
- `./start-backend.sh api`：仅启动 API 模块
- `cd xlinks-router-web/xlinks-router-admin && npm run dev`：启动管理端前端
- `cd xlinks-router-web/xlinks-router-client && npm run build`：构建客户端前端

完整初始化流程请参考根目录 `README.md`，其中包含 MySQL、Redis 和启动说明。若涉及 API 分布式改造，请优先检查 `xlinks-router-api-distributed` 是否已纳入当前构建与启动链路。

## 代码风格与命名规范

Java 使用 4 空格缩进，Vue、JavaScript、CSS 使用 2 空格缩进。后端类名沿用现有 Spring 风格：`*Controller`、`*Service`、`*Mapper`、`*DTO`。包名统一使用小写，位于 `site.xlinks.ai.router` 下。

方法命名应直接表达业务含义，避免无意义缩写。配置项按领域归类写入 `application.yml`。前端页面按功能拆分在 `src/views/<feature>/index.vue`。

## 测试规范

后端测试基于 Spring Boot Test 与 JUnit 5，测试类命名使用 `*Test`，例如 `ProviderRouteResolverTest`。运行全部测试使用 `mvn test`，按模块运行可使用 `mvn -pl xlinks-router-api test`。

涉及路由、鉴权、额度、控制器变更时，应补充对应测试。当前前端未配置独立测试框架，前端修改至少执行一次 `npm run build` 做静态校验。

## 提交与合并请求规范

仓库历史中常见提交风格为简短祈使句，例如 `Retry upstream routing on provider failures`、`Improve activation code list masking`，也存在简洁中文提交，如 `系统优化`。建议保持这一风格，标题聚焦单一变更点。

提交 PR 时应包含：

- 变更摘要
- 影响的模块、接口或数据库脚本
- 配置项或环境变量变更说明
- 前端改动截图
- 构建或测试结果，如 `mvn test`、`npm run build`

## 安全与配置提示

不要提交真实密钥或生产环境配置。当前各模块 `application.yml` 中包含环境相关参数，提交前应确认是否需要替换为本地、测试或占位配置。
