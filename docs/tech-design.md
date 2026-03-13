# xlinks-router 技术设计文档（增强版）

## 1. 设计目标

基于 PRD，xlinks-router 的技术设计目标是：

1. 建立统一的大模型网关基础架构
2. 完成 Provider、Model、Token Pool、Customer Token 四类核心对象建模
3. 定义标准化 API 入口与基础管理 API
4. 明确技术栈、工程结构、接口规范和开发约定
5. 保证项目可以按阶段有序落地，而不是一次性做大做重
6. 确保系统的安全性、可扩展性、高可用性和可维护性

---

## 2. 技术栈

## 2.1 后端技术栈

| 类别 | 技术选型 | 版本 | 选型理由 |
|------|----------|------|----------|
| 编程语言 | Java | 11 (LTS) | 稳定性高、生态成熟、企业级支持 |
| 开发框架 | Spring Boot | 3.2.x | 生态丰富、自动配置、简化开发 |
| Web 框架 | Spring Web (Spring MVC) | - | RESTful API 首选 |
| ORM 框架 | MyBatis-Plus | 3.5.x | 轻量级、高性能、支持增量更新 |
| 构建工具 | Maven | 3.9.x | 依赖管理成熟、插件生态丰富 |
| 接口文档 | SpringDoc OpenAPI | 2.x | 自动生成 OpenAPI 3.0 文档 |
| 参数校验 | Hibernate Validator | - | Bean Validation 规范实现 |
| HTTP 客户端 | OkHttp | 4.x | 高性能连接池、支持 HTTP/2 |
| JSON 处理 | Jackson | - | Spring Boot 默认集成 |
| 日志框架 | SLF4J + Logback | - | Spring Boot 默认集成 |

## 2.2 基础设施

| 组件 | 版本 | 说明 |
|------|------|------|
| MySQL | 8.0+ | 主数据库，支持 JSON、窗口函数 |
| Redis | 7.0+ | 缓存、Session、分布式锁、限流 |

## 2.3 当前环境配置

### MySQL
- Host: `123.60.29.123`
- Port: `3306`
- Username: `root`
- Password: `132311aA.`
- Database: `xlinks_router` (需创建)

### Redis
- Host: `123.60.29.123`
- Port: `6379`
- Password: `132311aA.`

---

## 3. 系统定位

xlinks-router 是一个统一的大模型网关平台，分为两类能力：

### 3.1 对外能力
- 向客户提供统一标准 API
- 使用 Customer Token 完成平台级授权
- 屏蔽底层 Provider 差异

### 3.2 对内能力
- 管理 Provider
- 管理 Model
- 管理 Token Pool
- 管理 Customer Token
- 根据路由策略完成底层服务调用

---

## 4. 核心领域模型

## 4.1 Provider

表示底层模型服务商。

### 表结构 (DDL)

```sql
CREATE TABLE `providers` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `provider_code` VARCHAR(50) NOT NULL COMMENT '提供商编码，唯一标识，如 openai、deepseek',
  `provider_name` VARCHAR(100) NOT NULL COMMENT '提供商名称',
  `provider_type` VARCHAR(20) NOT NULL DEFAULT 'openai-compatible' COMMENT '协议类型：openai-compatible、anthropic、azure 等',
  `base_url` VARCHAR(255) NOT NULL COMMENT '基础请求 URL',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_code` (`provider_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型服务商表';
```

### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | 是 | 主键，自增 |
| provider_code | VARCHAR(50) | 是 | 唯一编码，例如 `openai`、`deepseek` |
| provider_name | VARCHAR(100) | 是 | 提供商名称 |
| provider_type | VARCHAR(20) | 是 | 协议类型，默认 `openai-compatible` |
| base_url | VARCHAR(255) | 是 | 基础请求 URL |
| status | TINYINT | 是 | 1-启用，0-禁用 |
| remark | VARCHAR(500) | 否 | 备注信息 |
| created_at | DATETIME | 是 | 创建时间 |
| updated_at | DATETIME | 是 | 更新时间 |
| create_by | VARCHAR(50) | 否 | 创建人 |
| update_by | VARCHAR(50) | 否 | 更新人 |

---

## 4.2 Customer Model

表示平台对外暴露的逻辑模型定义。

### 表结构 (DDL)

```sql
CREATE TABLE `customer_models` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `logic_model_code` VARCHAR(50) NOT NULL COMMENT '平台逻辑模型编码，对外暴露',
  `logic_model_name` VARCHAR(100) NOT NULL COMMENT '逻辑模型名称',
  `model_type` VARCHAR(20) NOT NULL DEFAULT 'chat' COMMENT '模型类型：chat、embedding、image 等',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认模型：1-是，0-否',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_logic_model_code` (`logic_model_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户逻辑模型表';
```

### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | 是 | 主键，自增 |
| logic_model_code | VARCHAR(50) | 是 | 平台逻辑模型编码，对外暴露 |
| logic_model_name | VARCHAR(100) | 是 | 逻辑模型名称 |
| model_type | VARCHAR(20) | 是 | 模型类型：chat、embedding、image 等 |
| status | TINYINT | 是 | 1-启用，0-禁用 |
| is_default | TINYINT | 是 | 是否默认模型 |
| remark | VARCHAR(500) | 否 | 备注信息 |
| created_at | DATETIME | 是 | 创建时间 |
| updated_at | DATETIME | 是 | 更新时间 |
| create_by | VARCHAR(50) | 否 | 创建人 |
| update_by | VARCHAR(50) | 否 | 更新人 |

### 说明

- 一个 Customer Model 可关联多个 Provider Model

---

## 4.3 Provider Model

表示底层 Provider 的实际模型定义。

### 表结构 (DDL)

```sql
CREATE TABLE `provider_models` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `provider_id` BIGINT NOT NULL COMMENT '关联 Provider ID',
  `provider_model_code` VARCHAR(50) NOT NULL COMMENT 'Provider Model 的稳定业务编码',
  `provider_model_name` VARCHAR(100) NOT NULL COMMENT '底层 Provider 的实际模型名',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_model` (`provider_id`, `provider_model_code`),
  KEY `idx_provider_id` (`provider_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Provider 模型表';
```

### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | 是 | 主键，自增 |
| provider_id | BIGINT | 是 | 关联 Provider ID |
| provider_model_code | VARCHAR(50) | 是 | Provider Model 的稳定业务编码 |
| provider_model_name | VARCHAR(100) | 是 | 底层 Provider 的实际模型名 |
| status | TINYINT | 是 | 1-启用，0-禁用 |
| remark | VARCHAR(500) | 否 | 备注信息 |
| created_at | DATETIME | 是 | 创建时间 |
| updated_at | DATETIME | 是 | 更新时间 |
| create_by | VARCHAR(50) | 否 | 创建人 |
| update_by | VARCHAR(50) | 否 | 更新人 |

### 说明

- 一个 Provider 可以配置多个 Provider Model

---

## 4.4 Model Mapping

表示 Customer Model 与 Provider Model 的关联映射（一对多）。

### 表结构 (DDL)

```sql
CREATE TABLE `model_mapping` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `customer_model_id` BIGINT NOT NULL COMMENT '客户模型 ID',
  `provider_model_id` BIGINT NOT NULL COMMENT 'Provider 模型 ID',
  `priority` INT NOT NULL DEFAULT 100 COMMENT '优先级，值越小优先级越高，用于路由/兜底顺序控制',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_customer_model_id` (`customer_model_id`),
  KEY `idx_provider_model_id` (`provider_model_id`),
  KEY `idx_priority` (`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户模型与 Provider 模型关联表';
```

### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | 是 | 主键，自增 |
| customer_model_id | BIGINT | 是 | 客户模型 ID |
| provider_model_id | BIGINT | 是 | Provider 模型 ID |
| priority | INT | 是 | 优先级，值越小优先级越高 |
| status | TINYINT | 是 | 1-启用，0-禁用 |
| remark | VARCHAR(500) | 否 | 备注信息 |
| created_at | DATETIME | 是 | 创建时间 |
| updated_at | DATETIME | 是 | 更新时间 |
| create_by | VARCHAR(50) | 否 | 创建人 |
| update_by | VARCHAR(50) | 否 | 更新人 |

### 说明

- 一个 Customer Model 可对应多个 Provider Model
- `priority` 用于路由/兜底顺序控制，值越小优先级越高

---

## 4.5 Provider Token（Token Pool Item）

表示平台维护的底层 Provider token。

### 表结构 (DDL)

```sql
CREATE TABLE `provider_tokens` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `provider_id` BIGINT NOT NULL COMMENT '关联 Provider ID',
  `token_name` VARCHAR(100) NOT NULL COMMENT 'Token 名称/别名',
  `token_value` VARCHAR(500) NOT NULL COMMENT 'Token 值，加密存储',
  `token_status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Token 状态：1-正常，0-禁用',
  `quota_total` BIGINT DEFAULT NULL COMMENT '配额总量（可选）',
  `quota_used` BIGINT DEFAULT 0 COMMENT '已使用配额',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  `last_used_at` DATETIME DEFAULT NULL COMMENT '最后使用时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_provider_id` (`provider_id`),
  KEY `idx_token_status` (`token_status`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Provider Token 表';
```

### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | 是 | 主键，自增 |
| provider_id | BIGINT | 是 | 关联 Provider ID |
| token_name | VARCHAR(100) | 是 | Token 名称/别名 |
| token_value | VARCHAR(500) | 是 | Token 值，需加密存储 |
| token_status | TINYINT | 是 | 1-正常，0-禁用 |
| quota_total | BIGINT | 否 | 配额总量 |
| quota_used | BIGINT | 否 | 已使用配额 |
| expire_time | DATETIME | 否 | 过期时间 |
| last_used_at | DATETIME | 否 | 最后使用时间 |
| remark | VARCHAR(500) | 否 | 备注信息 |
| created_at | DATETIME | 是 | 创建时间 |
| updated_at | DATETIME | 是 | 更新时间 |
| create_by | VARCHAR(50) | 否 | 创建人 |
| update_by | VARCHAR(50) | 否 | 更新人 |

### 安全说明

- `token_value` 字段必须加密存储，建议使用 AES-256 加密
- 密钥管理参考第 12 节安全规范

---

## 4.6 Customer Token

表示平台发给客户的访问凭证。

### 表结构 (DDL)

```sql
CREATE TABLE `customer_tokens` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `customer_name` VARCHAR(100) NOT NULL COMMENT '客户名称',
  `token_name` VARCHAR(100) NOT NULL COMMENT 'Token 名称/别名',
  `token_value` VARCHAR(64) NOT NULL COMMENT 'Token 值，SHA256 哈希存储',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  `allowed_models` JSON DEFAULT NULL COMMENT '允许访问的模型列表',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token_value` (`token_value`),
  KEY `idx_status` (`status`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户 Token 表';
```

### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | 是 | 主键，自增 |
| customer_name | VARCHAR(100) | 是 | 客户名称 |
| token_name | VARCHAR(100) | 是 | Token 名称/别名 |
| token_value | VARCHAR(64) | 是 | Token 值，SHA256 哈希存储 |
| status | TINYINT | 是 | 1-启用，0-禁用 |
| expire_time | DATETIME | 否 | 过期时间 |
| allowed_models | JSON | 否 | 允许访问的模型列表 |
| remark | VARCHAR(500) | 否 | 备注信息 |
| created_at | DATETIME | 是 | 创建时间 |
| updated_at | DATETIME | 是 | 更新时间 |
| create_by | VARCHAR(50) | 否 | 创建人 |
| update_by | VARCHAR(50) | 否 | 更新人 |

### 安全说明

- `token_value` 存储 SHA256 哈希值，不存储明文
- 客户调用时使用 Bearer Token，服务器验证哈希值

---

## 4.7 Usage Record

表示一次请求调用记录。

### 表结构 (DDL)

```sql
CREATE TABLE `usage_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `request_id` VARCHAR(64) NOT NULL COMMENT '请求 ID，全局唯一',
  `customer_token_id` BIGINT NOT NULL COMMENT '客户 Token ID',
  `provider_id` BIGINT NOT NULL COMMENT 'Provider ID',
  `model_id` BIGINT NOT NULL COMMENT '模型 ID',
  `provider_token_id` BIGINT NOT NULL COMMENT 'Provider Token ID',
  `request_model` VARCHAR(50) NOT NULL COMMENT '请求的模型名称',
  `response_status` INT NOT NULL COMMENT '响应状态码',
  `prompt_tokens` INT DEFAULT 0 COMMENT '提示词 token 数',
  `completion_tokens` INT DEFAULT 0 COMMENT '补全 token 数',
  `total_tokens` INT DEFAULT 0 COMMENT '总 token 数',
  `latency_ms` INT DEFAULT 0 COMMENT '延迟（毫秒）',
  `error_code` VARCHAR(50) DEFAULT NULL COMMENT '错误码',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_request_id` (`request_id`),
  KEY `idx_customer_token_id` (`customer_token_id`),
  KEY `idx_provider_id` (`provider_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_request_model` (`request_model`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='使用记录表';
```

### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | 是 | 主键，自增 |
| request_id | VARCHAR(64) | 是 | 请求 ID，全局唯一 |
| customer_token_id | BIGINT | 是 | 客户 Token ID |
| provider_id | BIGINT | 是 | Provider ID |
| model_id | BIGINT | 是 | 模型 ID |
| provider_token_id | BIGINT | 是 | Provider Token ID |
| request_model | VARCHAR(50) | 是 | 请求的模型名称 |
| response_status | INT | 是 | 响应状态码 |
| prompt_tokens | INT | 否 | 提示词 token 数 |
| completion_tokens | INT | 否 | 补全 token 数 |
| total_tokens | INT | 否 | 总 token 数 |
| latency_ms | INT | 否 | 延迟（毫秒） |
| error_code | VARCHAR(50) | 否 | 错误码 |
| error_message | TEXT | 否 | 错误信息 |
| created_at | DATETIME | 是 | 创建时间 |

### 说明

- MVP 初期可先保留结构，不一定立即全部实现
- 为后续使用情况统计和 token 使用情况维护做准备

---

## 5. 核心关系

关系如下：

1. 一个 Provider 可以有多个 Model
2. 一个 Provider 可以有多个 Provider Token
3. 一个 Customer Token 可以访问一个或多个逻辑模型
4. 一次请求会落到一个 Provider、一个 Model、一个 Provider Token
5. 一次请求可记录为一条 Usage Record

### ER 图简述

```
Provider (1) ──────< ProviderModel
Provider (1) ──────< ProviderToken
CustomerModel (1) ──────< CustomerModelMapping >---- (1) ProviderModel
CustomerToken (1) ──────< UsageRecord
```

---

## 6. 项目结构

## 6.1 Maven 模块结构

```
xlinks-router/
├── pom.xml                      # 父 POM
├── xlinks-router-admin/          # 管理后台 API 模块
│   ├── src/main/java/
│   │   └── site/xlinks/ai/
│   │       └── router/
│   │           ├── controller/      # Controller 层
│   │           ├── service/         # Service 层
│   │           ├── mapper/          # MyBatis Mapper
│   │           ├── entity/          # 实体类
│   │           ├── dto/              # 数据传输对象
│   │           ├── vo/               # 视图对象
│   │           ├── config/          # 配置类
│   │           ├── common/          # 公共组件
│   │           │   ├── constant/     # 常量定义
│   │           │   ├── exception/    # 异常定义
│   │           │   ├── enums/        # 枚举类
│   │           │   └── result/       # 统一响应
│   │           └── AdminApplication.java
│   └── src/main/resources/
│       ├── application.yml          # 应用配置
│       ├── mapper/                   # MyBatis XML 映射文件
│       └── db/                       # 数据库脚本
├── xlinks-router-api/           # 对外 API 模块
│   ├── src/main/java/
│   │   └── site/xlinks/ai/
│   │       └── router/
│   │           ├── controller/      # Controller 层
│   │           ├── service/         # Service 层
│   │           ├── mapper/          # MyBatis Mapper
│   │           ├── entity/          # 实体类
│   │           ├── dto/             # 数据传输对象
│   │           ├── vo/              # 视图对象
│   │           ├── client/          # HTTP 客户端（调用 Provider）
│   │           ├── handler/         # 请求处理器
│   │           ├── router/          # 路由逻辑
│   │           ├── converter/       # 请求/响应转换器
│   │           ├── auth/            # 认证授权
│   │           ├── config/          # 配置类
│   │           ├── common/          # 公共组件
│   │           └── ApiApplication.java
│   └── src/main/resources/
│       ├── application.yml          # 应用配置
│       └── mapper/                   # MyBatis XML 映射文件
└── xlinks-router-common/       # 公共模块
    ├── src/main/java/
    │   └── site/xlinks/ai/
    │       └── router/
    │           ├── entity/           # 共享实体类
    │           ├── dto/              # 共享 DTO
    │           ├── vo/               # 共享 VO
    │           ├── constant/         # 常量定义
    │           ├── exception/        # 异常定义
    │           ├── enums/            # 枚举类
    │           └── result/           # 统一响应
    └── src/main/resources/
```

## 6.2 包命名规范

基础包名：`site.xlinks.ai.router`

| 包名 | 说明 |
|------|------|
| site.xlinks.ai.router.controller | REST API 控制器 |
| site.xlinks.ai.router.service | 业务逻辑层 |
| site.xlinks.ai.router.mapper | 数据访问层 |
| site.xlinks.ai.router.entity | 实体类 |
| site.xlinks.ai.router.dto | 数据传输对象（入参） |
| site.xlinks.ai.router.vo | 视图对象（出参） |
| site.xlinks.ai.router.config | 配置类 |
| site.xlinks.ai.router.client | HTTP 客户端 |
| site.xlinks.ai.router.handler | 请求处理器 |
| site.xlinks.ai.router.router | 路由逻辑 |
| site.xlinks.ai.router.converter | 请求/响应转换器 |
## 6.3 Maven POM 配置

### 父 POM (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>site.xlinks.ai</groupId>
    <artifactId>xlinks-router</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>xlinks-router</name>
    <description>统一大模型网关平台</description>

    <modules>
        <module>xlinks-router-common</module>
        <module>xlinks-router-admin</module>
        <module>xlinks-router-api</module>
    </modules>

    <properties>
        <java.version>11</java.version>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring-boot.version>3.2.0</spring-boot.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <okhttp.version>4.12.0</okhttp.version>
        <springdoc.version>2.3.0</springdoc.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-boot-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            <dependency>
                <groupId>com.squareup.okhttp3</groupId>
                <artifactId>okhttp</artifactId>
                <version>${okhttp.version}</version>
            </dependency>
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                <version>${springdoc.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 子模块 POM (xlinks-router-admin/pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>site.xlinks.ai</groupId>
        <artifactId>xlinks-router</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>xlinks-router-admin</artifactId>
    <packaging>jar</packaging>

    <name>xlinks-router-admin</name>
    <description>管理后台 API 模块</description>

    <dependencies>
        <dependency>
            <groupId>site.xlinks.ai</groupId>
            <artifactId>xlinks-router-common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 7. API 设计

API 分为两类：

### 7.1 管理类 API
用于管理平台核心资源。

### 7.2 对外标准 API
用于客户调用平台模型能力。

---

## 8. 管理类 API 定义（MVP）

## 8.1 Provider API

### 新增 Provider

- **接口**: `POST /admin/providers`
- **Content-Type**: `application/json`

**Request Body**:

```json
{
  "providerCode": "deepseek",
  "providerName": "DeepSeek",
  "providerType": "openai-compatible",
  "baseUrl": "https://api.deepseek.com/v1",
  "status": 1,
  "remark": "DeepSeek 模型提供商"
}
```

**Response (成功)**:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "providerCode": "deepseek",
    "providerName": "DeepSeek",
    "providerType": "openai-compatible",
    "baseUrl": "https://api.deepseek.com/v1",
    "status": 1,
    "remark": "DeepSeek 模型提供商",
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
}
```

### Provider 列表

- **接口**: `GET /admin/providers`
- **Query Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 10 |
| providerCode | string | 否 | 提供商编码（精确匹配） |
| providerName | string | 否 | 提供商名称（模糊匹配） |
| status | int | 否 | 状态：1-启用，0-禁用 |

**Response (成功)**:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "providerCode": "deepseek",
        "providerName": "DeepSeek",
        "providerType": "openai-compatible",
        "baseUrl": "https://api.deepseek.com/v1",
        "status": 1,
        "remark": "DeepSeek 模型提供商",
        "createdAt": "2024-01-01T00:00:00Z",
        "updatedAt": "2024-01-01T00:00:00Z"
      }
    ],
    "total": 1,
    "page": 1,
    "pageSize": 10
  }
}
```

### Provider 详情

- **接口**: `GET /admin/providers/{id}`

**Response (成功)**:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "providerCode": "deepseek",
    "providerName": "DeepSeek",
    "providerType": "openai-compatible",
    "baseUrl": "https://api.deepseek.com/v1",
    "status": 1,
    "remark": "DeepSeek 模型提供商",
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
}
```

### 更新 Provider

- **接口**: `PUT /admin/providers/{id}`
- **Content-Type**: `application/json`

**Request Body**:

```json
{
  "providerName": "DeepSeek AI",
  "baseUrl": "https://api.deepseek.com/v1",
  "remark": "更新备注"
}
```

### 启用/禁用 Provider

- **接口**: `PATCH /admin/providers/{id}/status`
- **Content-Type**: `application/json`

**Request Body**:

```json
{
  "status": 0
}
```

---

## 8.2 Customer Model API

### 新增 Customer Model

- **接口**: `POST /admin/customer-models`
- **Content-Type**: `application/json`

**Request Body**:

```json
{
  "logicModelCode": "deepseek-v3",
  "logicModelName": "DeepSeek V3",
  "modelType": "chat",
  "status": 1,
  "isDefault": 0,
  "remark": "DeepSeek V3 聊天模型"
}
```

### Customer Model 列表

- **接口**: `GET /admin/customer-models`
- **Query Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 10 |
| logicModelCode | string | 否 | 模型编码（精确匹配） |
| logicModelName | string | 否 | 模型名称（模糊匹配） |
| modelType | string | 否 | 模型类型 |
| status | int | 否 | 状态：1-启用，0-禁用 |

### Customer Model 详情

- **接口**: `GET /admin/customer-models/{id}`

### 更新 Customer Model

- **接口**: `PUT /admin/customer-models/{id}`

### 启用/禁用 Customer Model

- **接口**: `PATCH /admin/customer-models/{id}/status`

---

## 8.3 Provider Model API

### 新增 Provider Model

- **接口**: `POST /admin/provider-models`
- **Content-Type**: `application/json`

**Request Body**:

```json
{
  "providerId": 1,
  "providerModelCode": "deepseek-chat",
  "providerModelName": "deepseek-chat",
  "status": 1,
  "remark": "DeepSeek 聊天模型"
}
```

### Provider Model 列表

- **接口**: `GET /admin/provider-models`
- **Query Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 10 |
| providerId | long | 否 | Provider ID |
| providerModelCode | string | 否 | 模型编码 |
| status | int | 否 | 状态 |

### Provider Model 详情

- **接口**: `GET /admin/provider-models/{id}`

### 更新 Provider Model

- **接口**: `PUT /admin/provider-models/{id}`

### 启用/禁用 Provider Model

- **接口**: `PATCH /admin/provider-models/{id}/status`

---

## 8.4 Customer Model Mapping API

### 新增关联

- **接口**: `POST /admin/customer-model-mappings`
- **Content-Type**: `application/json`

**Request Body**:

```json
{
  "customerModelId": 1,
  "providerModelId": 1,
  "priority": 1,
  "status": 1,
  "remark": "DeepSeek V3 优先使用 deepseek-chat"
}
```

### 关联列表

- **接口**: `GET /admin/customer-model-mappings`
- **Query Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 10 |
| customerModelId | long | 否 | Customer Model ID |
| providerModelId | long | 否 | Provider Model ID |
| status | int | 否 | 状态 |

### 更新关联

- **接口**: `PUT /admin/customer-model-mappings/{id}`

### 启用/禁用关联

- **接口**: `PATCH /admin/customer-model-mappings/{id}/status`

---

## 8.5 Provider Token API

### 新增 Provider Token

- **接口**: `POST /admin/provider-tokens`
- **Content-Type**: `application/json`

**Request Body**:

```json
{
  "providerId": 1,
  "tokenName": "DeepSeek API Key",
  "tokenValue": "sk-xxxxxxxxxxxxxxxx",
  "tokenStatus": 1,
  "quotaTotal": 1000000,
  "expireTime": "2025-12-31T23:59:59Z",
  "remark": "DeepSeek 主账号 Token"
}
```

### Token 列表

- **接口**: `GET /admin/provider-tokens`
- **Query Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 10 |
| providerId | long | 否 | Provider ID |
| tokenStatus | int | 否 | Token 状态 |

**Response (成功)**:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "providerId": 1,
        "tokenName": "DeepSeek API Key",
        "tokenStatus": 1,
        "quotaTotal": 1000000,
        "quotaUsed": 10000,
        "expireTime": "2025-12-31T23:59:59Z",
        "lastUsedAt": "2024-01-01T00:00:00Z",
        "remark": "DeepSeek 主账号 Token",
        "createdAt": "2024-01-01T00:00:00Z",
        "updatedAt": "2024-01-01T00:00:00Z"
      }
    ],
    "total": 1,
    "page": 1,
    "pageSize": 10
  }
}
```

**注意**: `tokenValue` 不应在列表中返回

### Token 详情

- **接口**: `GET /admin/provider-tokens/{id}`

### 更新 Token

- **接口**: `PUT /admin/provider-tokens/{id}`

### 启用/禁用 Token

- **接口**: `PATCH /admin/provider-tokens/{id}/status`

---

## 8.6 Customer Token API

### 新增 Customer Token

- **接口**: `POST /admin/customer-tokens`
- **Content-Type**: `application/json`

**Request Body**:

```json
{
  "customerName": "示例客户",
  "tokenName": "客户主账号",
  "status": 1,
  "expireTime": "2025-12-31T23:59:59Z",
  "allowedModels": ["deepseek-v3", "gpt-4"],
  "remark": "示例客户 Token"
}
```

**Response (成功)**:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "customerName": "示例客户",
    "tokenName": "客户主账号",
    "tokenValue": "xlr_ct_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    "status": 1,
    "expireTime": "2025-12-31T23:59:59Z",
    "allowedModels": ["deepseek-v3", "gpt-4"],
    "remark": "示例客户 Token",
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
}
```

**重要**: `tokenValue` 只在创建时返回一次，之后不再显示，请妥善保管

### Token 列表

- **接口**: `GET /admin/customer-tokens`
- **Query Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 10 |
| customerName | string | 否 | 客户名称（模糊匹配） |
| status | int | 否 | 状态 |

### Token 详情

- **接口**: `GET /admin/customer-tokens/{id}`

### 更新 Token

- **接口**: `PUT /admin/customer-tokens/{id}`

### 启用/禁用 Token

- **接口**: `PATCH /admin/customer-tokens/{id}/status`

---

## 9. 对外标准 API 定义（MVP）

## 9.1 Chat Completions

- **接口**: `POST /v1/chat/completions`
- **Content-Type**: `application/json`
- **认证**: `Authorization: Bearer {customer_token}`

### Header

| Header | 说明 |
|--------|------|
| Authorization | Bearer Token，格式: `Bearer {customer_token}` |
| Content-Type | `application/json` |

### Request Body

```json
{
  "model": "deepseek-v3",
  "messages": [
    {"role": "system", "content": "你是一个有帮助的助手"},
    {"role": "user", "content": "你好"}
  ],
  "temperature": 0.7,
  "max_tokens": 1000,
  "stream": false,
  "top_p": 1.0,
  "frequency_penalty": 0.0,
  "presence_penalty": 0.0
}
```

### Request Body 字段说明

| 字段 | 类型 | 必填 | 说明 | 默认值 |
|------|------|------|------|--------|
| model | string | 是 | 模型名称 | - |
| messages | array | 是 | 消息列表 | - |
| messages[].role | string | 是 | 角色：system、user、assistant | - |
| messages[].content | string | 是 | 消息内容 | - |
| temperature | float | 否 | 采样温度 | 0.7 |
| max_tokens | int | 否 | 最大 token 数 | 4096 |
| stream | boolean | 否 | 是否流式返回 | false |
| top_p | float | 否 | nucleus 采样 | 1.0 |
| frequency_penalty | float | 否 | 频率惩罚 | 0.0 |
| presence_penalty | float | 否 | 存在惩罚 | 0.0 |

### Response (非流式)

```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion",
  "created": 1677652288,
  "model": "deepseek-v3",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "你好！有什么可以帮助你的吗？"
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 10,
    "completion_tokens": 20,
    "total_tokens": 30
  }
}
```

### Response (流式)

```
data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"deepseek-v3","choices":[{"index":0,"delta":{"role":"assistant","content":"你"},"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"deepseek-v3","choices":[{"index":0,"delta":{"content":"好"},"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"deepseek-v3","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

data: [DONE]
```

### 平台处理步骤

1. 校验 Customer Token（检查存在、状态、未过期）
2. 校验请求模型（检查是否在 allowedModels 中）
3. 路由到目标 Provider / Model（根据 priority 选择）
4. 选择可用 Provider Token（状态正常、未过期、配额未用完）
5. 调用底层 Provider（转换请求格式）
6. 标准化返回结果
7. 记录 Usage Record

---

## 9.2 Models List

- **接口**: `GET /v1/models`
- **认证**: `Authorization: Bearer {customer_token}`

### Response

```json
{
  "object": "list",
  "data": [
    {
      "id": "deepseek-v3",
      "object": "model",
      "created": 1677610602,
      "owned_by": "xlinks-router"
    },
    {
      "id": "gpt-4",
      "object": "model",
      "created": 1677610602,
      "owned_by": "xlinks-router"
    }
  ]
}
```

### 说明

- 向客户返回当前平台对外可用的逻辑模型列表
- 只返回客户 Token 允许访问的模型

---

## 10. 路由约定（MVP）

MVP 阶段先采用简单路由规则：

1. 先根据 `model` 找到逻辑模型
2. 根据逻辑模型找到对应 Provider 和底层模型（按 priority 排序）
3. 在该 Provider 下选择一个状态正常且未过期的 Provider Token
4. 发起底层调用

当前阶段不引入复杂权重、健康打分和自动故障切换。

### 路由策略扩展（未来规划）

- [ ] 支持权重路由
- [ ] 支持地理位置路由
- [ ] 支持成本优化路由
- [ ] 支持健康检查与故障切换
- [ ] 支持自定义路由规则

---

## 11. 接口规范

## 11.1 统一响应结构

管理类 API 统一返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 分页响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "page": 1,
    "pageSize": 10
  }
}
```

## 11.2 错误码约定（MVP）

| 错误码 | 说明 | HTTP Status |
|--------|------|-------------|
| 0 | 成功 | 200 |
| 4001 | 参数错误 | 400 |
| 4002 | Customer Token 无效 | 401 |
| 4003 | 模型不可用 | 400 |
| 4004 | Provider 不可用 | 502 |
| 4005 | Provider Token 不可用 | 502 |
| 4006 | 模型不在允许列表中 | 403 |
| 4007 | Token 已过期 | 401 |
| 5000 | 系统异常 | 500 |
| 5001 | 外部服务调用失败 | 502 |

### 错误响应示例

```json
{
  "code": 4002,
  "message": "Customer Token 无效",
  "data": null
}
```

## 11.3 时间字段规范

- 数据库存储：`datetime`
- 接口返回：ISO 8601 格式字符串（如 `2024-01-01T00:00:00Z`）

## 11.4 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 表名 | 下划线风格复数 | `providers`, `customer_tokens` |
| 字段名 | 下划线风格 | `provider_code`, `created_at` |
| Java 类名 | 大驼峰 | `ProviderController`, `CustomerToken` |
| Java 字段 | 小驼峰 | `providerCode`, `createdAt` |
| API 路径 | 小写复数风格 | `/admin/providers`, `/v1/chat/completions` |
| JSON 字段 | 小驼峰 | `providerCode`, `createdAt` |

---

## 12. 安全规范

## 12.1 认证与授权

### Customer Token 认证

1. 客户在请求 Header 中携带 `Authorization: Bearer {token}`
2. 服务器对 token 进行 SHA256 哈希
3. 查询数据库匹配哈希值
4. 检查 token 状态（启用、未过期）

### 敏感数据加密

- Provider Token 的 `token_value` 字段必须加密存储
- 推荐使用 AES-256-GCM 加密
- 加密密钥通过环境变量或配置中心管理，不要硬编码

### 请求签名（未来规划）

- [ ] 支持 API Key + Secret 签名验证
- [ ] 支持 IP 白名单
- [ ] 支持请求频率限制

## 12.2 API 安全

- 所有对外 API 必须使用 HTTPS
- 管理类 API 应部署在内部网络或使用 VPN
- 敏感操作应记录审计日志

## 12.3 输入校验

- 所有用户输入必须进行校验
- 使用 Hibernate Validator 进行参数校验
- 特殊字符进行转义处理，防止注入攻击

---

## 13. 配置管理

## 13.1 配置文件结构

### application.yml 示例

```yaml
server:
  port: 8080

spring:
  application:
    name: xlinks-router-admin
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://123.60.29.123:3306/xlinks_router?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 132311aA.
  data:
    redis:
      host: 123.60.29.123
      port: 6379
      password: 132311aA.
      database: 0

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.xlinks.router.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

# 应用配置
app:
  security:
    token-encrypt-key: ${TOKEN_ENCRYPT_KEY:}  # Token 加密密钥
  api:
    timeout: 30000  # 外部 API 调用超时（毫秒）
    retry-count: 3  # 重试次数

# 日志配置
logging:
  level:
    com.xlinks.router: DEBUG
    org.springframework: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

## 13.2 环境变量

| 变量名 | 说明 | 必填 |
|--------|------|------|
| TOKEN_ENCRYPT_KEY | Token 加密密钥，AES-256 长度 | 是 |
| MYSQL_HOST | MySQL 主机 | 是 |
| MYSQL_PORT | MySQL 端口 | 是 |
| MYSQL_USERNAME | MySQL 用户名 | 是 |
| MYSQL_PASSWORD | MySQL 密码 | 是 |
| REDIS_HOST | Redis 主机 | 是 |
| REDIS_PORT | Redis 端口 | 是 |
| REDIS_PASSWORD | Redis 密码 | 否 |

---

## 14. 日志规范

## 14.1 日志级别

| 级别 | 使用场景 |
|------|----------|
| DEBUG | 调试信息，详细业务流程 |
| INFO | 正常业务流程记录 |
| WARN | 警告信息，可容忍的异常 |
| ERROR | 错误信息，需要关注的异常 |

## 14.2 日志内容规范

- 记录关键业务操作（如创建、更新、删除）
- 记录敏感操作（如 Token 操作）
- 记录请求响应关键信息（脱敏处理）
- 记录异常堆栈信息

### 日志示例

```java
// 请求入口日志
log.info("Received chat completion request, model: {}, customerTokenId: {}", 
         request.getModel(), customerTokenId);

// 路由日志
log.info("Routing to provider: {}, model: {}, token: {}", 
         provider.getProviderName(), providerModel.getProviderModelName(), tokenName);

// 错误日志
log.error("Provider API call failed, provider: {}, error: {}", 
          provider.getProviderCode(), e.getMessage(), e);
```

## 14.3 日志脱敏

- 禁止在日志中记录 Token 明文
- 禁止在日志中记录密码、密钥
- 敏感信息使用脱敏处理

---

## 15. 异常处理

## 15.1 异常类定义

```java
// 业务异常
public class BusinessException extends RuntimeException {
    private final int code;
    
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}

// 参数异常
public class ValidationException extends BusinessException {
    public ValidationException(String message) {
        super(4001, message);
    }
}

// Token 异常
public class TokenException extends BusinessException {
    public TokenException(String message) {
        super(4002, message);
    }
}
```

## 15.2 全局异常处理器

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return Result.error(4001, message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("System error", e);
        return Result.error(5000, "系统异常");
    }
}
```

---

## 16. 监控与可观测性

## 16.1 指标监控

- [ ] 请求 QPS、延迟、错误率
- [ ] Token 使用量统计
- [ ] Provider 调用统计
- [ ] 业务指标（活跃客户数、调用模型分布）

## 16.2 日志收集

- [ ] 日志集中收集（ELK/Loki）
- [ ] 链路追踪（SkyWalking/Jaeger）

## 16.3 告警规则

- [ ] 错误率超过阈值告警
- [ ] 响应延迟超过阈值告警
- [ ] Provider 可用性异常告警

---

## 17. 测试策略

## 17.1 单元测试

- Service 层必须编写单元测试
- 使用 JUnit 5 + Mockito
- 覆盖率目标：核心业务 80% 以上

## 17.2 集成测试

- Controller 层编写集成测试
- 使用 Spring Boot Test
- 配合 H2 内存数据库

## 17.3 压力测试

- [ ] 使用 JMeter 进行压力测试
- [ ] 验证系统在高并发下的表现

---

## 18. 部署规范

## 18.1 Docker 部署

### Dockerfile 示例

```dockerfile
FROM eclipse-temurin:11-jre-alpine

WORKDIR /app

COPY target/xlinks-router-admin.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms512m -Xmx1024m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

## 18.2 环境要求

| 环境 | 最低配置 |
|------|----------|
| 开发 | 2C4G |
| 测试 | 4C8G |
| 生产 | 8C16G |

---

## 19. 开发约定

## 19.1 Git 提交规范

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type 类型

| 类型 | 说明 |
|------|------|
| feat | 新功能 |
| fix | Bug 修复 |
| docs | 文档更新 |
| style | 代码格式 |
| refactor | 重构 |
| test | 测试相关 |
| chore | 构建/工具 |

### 示例

```
feat(provider): 添加 Provider 列表查询接口

支持分页查询和状态过滤

Closes #123
```

## 19.2 代码审查要点

- [ ] 代码是否符合命名规范
- [ ] 是否有安全风险（SQL 注入、XSS 等）
- [ ] 异常处理是否完善
- [ ] 是否有性能问题
- [ ] 单元测试是否通过

---

## 20. 附录

## 20.1 数据库初始化脚本

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS xlinks_router DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE xlinks_router;

-- 执行各表 DDL（见上文第 4 节）
```

## 20.2 OpenAPI 文档

启动应用后访问：
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## 20.3 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | 2024-01-01 | MVP 版本 |
