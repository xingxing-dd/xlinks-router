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

### usageType 定义

`usage_type` 用于限制当前 Provider Model 在什么计费/权益模式下可用：

| 值 | 含义 | 说明 |
|----|------|------|
| 0 | 不限制 | 套餐模式、余额模式都可参与路由 |
| 1 | 仅套餐可用 | 仅当用户当前请求按套餐模式结算时可用 |
| 2 | 仅余额可用 | 仅当用户当前请求按余额模式结算时可用 |

### 表结构 (DDL)

```sql
CREATE TABLE `provider_models` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `provider_id` BIGINT NOT NULL COMMENT '关联 Provider ID',
  `provider_model_code` VARCHAR(50) NOT NULL COMMENT 'Provider Model 的稳定业务编码',
  `provider_model_name` VARCHAR(100) NOT NULL COMMENT '底层 Provider 的实际模型名',
  `model_type` VARCHAR(20) NOT NULL DEFAULT 'chat' COMMENT '模型类型：chat、embedding、image 等',
  `usage_type` TINYINT NOT NULL DEFAULT 0 COMMENT '使用类型：0-不限制，1-仅套餐可用，2-仅余额可用',
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
| model_type | VARCHAR(20) | 是 | 模型类型：chat、embedding、image 等 |
| usage_type | TINYINT | 是 | 使用类型：0-不限制，1-仅套餐可用，2-仅余额可用 |
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

## 4.4.1 路由宽表（推荐新增）

为提高 `/chat/completions` 的实时路由性能，建议增加一张面向查询的“模型路由宽表”，以 `customer_model` 为核心维度，提前把逻辑模型到 Provider 模型的路由信息摊平，减少运行时多表 Join。

### 设计目标

1. 将 `customer_model` -> `provider_model` -> `provider` 的映射预计算后落表
2. 将 `usage_type` 和 `model_type` 一并冗余到宽表，支持按权益模式和模型类别快速筛选
3. 对 `/chat/completions` 只保留一次按条件排序查询，降低响应延迟

### 推荐表结构 (DDL)

```sql
CREATE TABLE `model_route_cache` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `customer_model` VARCHAR(50) NOT NULL COMMENT '客户侧逻辑模型编码，对应 customer_models.logic_model_code',
  `provider_id` BIGINT NOT NULL COMMENT 'Provider ID',
  `provider_model` VARCHAR(100) NOT NULL COMMENT '底层 Provider 模型名',
  `priority` INT NOT NULL DEFAULT 100 COMMENT '优先级，值越小优先级越高',
  `usage_type` TINYINT NOT NULL DEFAULT 0 COMMENT '使用类型：0-不限制，1-仅套餐可用，2-仅余额可用',
  `model_type` VARCHAR(20) NOT NULL DEFAULT 'chat' COMMENT '模型类型：chat、embedding、image 等',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_customer_model_usage_priority` (`customer_model`, `usage_type`, `priority`),
  KEY `idx_provider_id` (`provider_id`),
  KEY `idx_model_type` (`model_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型路由宽表';
```

### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| customer_model | VARCHAR(50) | 是 | 客户传入模型名 |
| provider_id | BIGINT | 是 | 目标 Provider ID |
| provider_model | VARCHAR(100) | 是 | 最终调用的底层模型名 |
| priority | INT | 是 | 优先级，值越小优先级越高 |
| usage_type | TINYINT | 是 | 适用权益模式：0/1/2 |
| model_type | VARCHAR(20) | 是 | 模型类型，便于后续扩展到 embedding/image |
| status | TINYINT | 是 | 路由状态 |

### 刷新策略

宽表本质上是 `customer_models + model_mapping + provider_models + providers` 的冗余投影，其中 `model_type` 建议直接来自 `provider_models.model_type`，避免宽表字段无法回溯来源。推荐两种刷新方式：

1. **管理后台写后刷新**：在 Customer Model、Provider Model、Mapping 发生新增/修改/启停时同步刷新对应路由记录
2. **定时全量重建**：通过定时任务按分钟或小时全量重算，作为兜底一致性策略

MVP 阶段建议优先实现“管理后台写后刷新 + 手动全量重建接口”。

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
