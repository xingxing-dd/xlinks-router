# Xlinks Router 项目初始化说明

这是一个大模型中转服务系统，仓库包含后端聚合工程、管理端前端和客户端前端。

## 项目说明

- `xlinks-router-api`：当前单节点可运行的 API 转发模块
- `xlinks-router-api-distributed`：面向分布式部署的 API 重构模块

`xlinks-router-api-distributed` 的建设大纲、目标与演进方向已单独整理在 [xlinks-router-api-distributed/readme.md](/Users/mingxing/Desktop/project/xlinks-router/xlinks-router-api-distributed/readme.md:1)。

## 目录结构

- `xlinks-router-common`：公共模块
- `xlinks-router-admin`：管理后台后端，默认端口 `8083`
- `xlinks-router-api`：模型中转 API，默认端口 `8081`
- `xlinks-router-api-distributed`：分布式部署版 API 重构模块，用于承接 `xlinks-router-api` 的分布式化改造
- `xlinks-router-client`：用户侧后端，默认端口 `8082`
- `xlinks-router-web/xlinks-router-admin`：管理端前端
- `xlinks-router-web/xlinks-router-client`：客户端前端
- `docs/`：接口、数据库和设计文档

## 环境要求

- JDK `17+`
- Maven `3.9+`
- Node.js `20+`
- npm `10+`
- MySQL `8.x`
- Redis `6.x` 或 `7.x`

当前工程的 Maven 配置以 `Java 17` 为目标版本，建议优先使用 JDK 17。

## 第一步：初始化数据库

1. 创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS `xlinks_router`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

2. 执行初始化脚本：

```bash
mysql -uroot -p xlinks_router < xlinks-router-admin/src/main/resources/db/init.sql
```

3. 如需补齐后续结构变更，再按需执行 `xlinks-router-admin/src/main/resources/db/` 下的迁移脚本。

## 第二步：检查并修改配置

后端默认配置文件：

- `xlinks-router-admin/src/main/resources/application.yml`
- `xlinks-router-api/src/main/resources/application.yml`
- `xlinks-router-api-distributed/src/main/resources/application.yml`
- `xlinks-router-client/src/main/resources/application.yml`

这些文件里默认包含：

- MySQL 连接
- Redis 连接
- 管理员初始化账号
- JWT / 内部调用配置
- 短信、邮件、支付宝等集成参数

初始化本地环境时，先按你的机器或测试环境修改这些配置。

## 第三步：安装依赖

后端依赖安装与编译：

```bash
mvn clean install -DskipTests
```

前端依赖安装：

```bash
cd xlinks-router-web/xlinks-router-admin
npm install

cd ../xlinks-router-client
npm install
```

## 第四步：启动项目

### 启动全部后端模块

```bash
./start-backend.sh all
```

### 启动单个后端模块

```bash
./start-backend.sh admin
./start-backend.sh api
./start-backend.sh distributed
./start-backend.sh client
```

### 启动前端

```bash
./start-frontend.sh admin
./start-frontend.sh client
```

如果直接进入前端目录，也可以使用：

```bash
npm run dev
```

## 常用命令

后端整体打包：

```bash
mvn clean package -DskipTests
```

单模块启动示例：

```bash
cd xlinks-router-api
mvn spring-boot:run

cd ../xlinks-router-api-distributed
mvn spring-boot:run
```

前端构建示例：

```bash
cd xlinks-router-web/xlinks-router-admin
npm run build
```

## 默认端口

- `8081`：`xlinks-router-api`
- `8084`：`xlinks-router-api-distributed`
- `8082`：`xlinks-router-client`
- `8083`：`xlinks-router-admin`
- Vite 前端端口以各自 `vite.config.js` 为准

## 建议初始化顺序

1. 准备 MySQL 和 Redis
2. 执行 `init.sql`
3. 修改三个后端模块的 `application.yml`
4. 执行 `mvn clean install -DskipTests`
5. 安装两个前端模块依赖
6. 启动 `admin`、`api`、`client`
7. 启动前端并联调

## 补充说明

- 根目录 `pom.xml` 当前聚合了 5 个后端模块：`common`、`admin`、`api`、`api-distributed`、`client`
- `xlinks-router-api-distributed` 当前用于分布式 API 能力重构，已接入根聚合构建列表
- 仓库内已经提供 `start-backend.sh`、`start-frontend.sh` 作为统一启动入口
