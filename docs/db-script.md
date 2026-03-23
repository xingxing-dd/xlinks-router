# xlinks-router 数据模型与脚本补充

本文档用于承载从原 `docs/tech-design.md` 拆分出的数据库脚本、DDL 与初始化说明。
当前模型设计已调整为"Provider + Model Endpoint + Model"三级结构，不再区分 `customer_model`、`provider_model`，也不再保留 `model_mapping`。

## 1. 数据库初始化

```sql
CREATE DATABASE IF NOT EXISTS xlinks_router DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE xlinks_router;
```

## 2. 逻辑删除规范

Provider、Model Endpoint、Model 三类核心模型统一采用逻辑删除字段：

- 字段名：`deleted`
- 类型：`TINYINT`
- 默认值：`0`
- 语义：`0-未删除`，`1-已删除`

约束建议：

- 所有查询默认追加 `deleted = 0`
- 逻辑删除后不做物理删除
- 唯一键冲突校验、列表展示、关联查询均应基于 `deleted = 0` 数据集
- OpenAPI 侧模型解析时，也只允许命中 `deleted = 0` 且 `status = 1` 的记录

## 3. Customer Account

客户账户表，用于管理前端客户账号。

```sql
CREATE TABLE `customer_accounts` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `password` VARCHAR(128) NOT NULL COMMENT '密码（BCrypt 加密存储）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户账户表';
```

## 4. Provider

新增 `provider_logo`、`provider_website`，用于品牌展示与跳转。

```sql
CREATE TABLE `providers` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `provider_code` VARCHAR(50) NOT NULL COMMENT '提供商编码，唯一标识，如 openai、deepseek',
  `provider_name` VARCHAR(100) NOT NULL COMMENT '提供商名称',
  `provider_type` VARCHAR(20) NOT NULL DEFAULT 'openai-compatible' COMMENT '协议类型：openai-compatible、anthropic、azure 等',
  `base_url` VARCHAR(255) NOT NULL COMMENT '基础请求 URL',
  `provider_logo` VARCHAR(255) DEFAULT NULL COMMENT '服务商 Logo URL',
  `provider_website` VARCHAR(255) DEFAULT NULL COMMENT '服务商官网 URL',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_code` (`provider_code`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型服务商表';
```

## 5. Model Endpoint

模型端点用于对模型能力分组，例如聊天、Embedding、图像生成等。

```sql
CREATE TABLE `model_endpoints` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `endpoint_code` VARCHAR(100) NOT NULL COMMENT '端点编码，如 chat/completions',
  `endpoint_name` VARCHAR(500) DEFAULT NULL COMMENT '端点名称',
  `endpoint_url` VARCHAR(255) NOT NULL COMMENT '端点 URL，如 /v1/chat/completions',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_endpoint_name` (`endpoint_name`),
  UNIQUE KEY `uk_endpoint_url` (`endpoint_url`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型端点表';
```

## 6. Model

统一模型表，直接归属服务商与模型端点。

### 6.1 字段说明

| 字段 | 说明 |
|------|------|
| model_name | 模型名称 |
| model_code | 模型编码 |
| endpoint_id | 模型端点 ID |
| provider_id | 服务商 ID |
| model_desc | 模型描述 |
| input_price | 输入价格，单位：每百万 token |
| output_price | 输出价格，单位：每百万 token |
| context_size | 上下文大小，单位：K |
| status | 状态：1-启用，0-禁用 |
| deleted | 逻辑删除：0-未删除，1-已删除 |

### 6.2 唯一性约束

同一个模型端点下，模型编码唯一：

```sql
UNIQUE KEY `uk_endpoint_model_code` (`endpoint_id`, `model_code`)
```

说明：业务查询需要额外保证只在 `deleted = 0` 的数据集中校验唯一性和可见性。

### 6.3 建表语句

```sql
CREATE TABLE `models` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
  `model_code` VARCHAR(100) NOT NULL COMMENT '模型编码',
  `endpoint_code` BIGINT NOT NULL COMMENT '模型端点code',
  `provider_id` BIGINT NOT NULL COMMENT '服务商 ID',
  `model_desc` VARCHAR(500) DEFAULT NULL COMMENT '模型描述',
  `input_price` DECIMAL(12, 2) DEFAULT NULL COMMENT '输入价格，单位：每百万 token',
  `output_price` DECIMAL(12, 2) DEFAULT NULL COMMENT '输出价格，单位：每百万 token',
  `context_size` INT DEFAULT NULL COMMENT '上下文大小，单位：K',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_endpoint_model_code` (`endpoint_id`, `model_code`),
  KEY `idx_provider_id` (`provider_id`),
  KEY `idx_endpoint_id` (`endpoint_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一模型表';
```

### 6.4 初始化模型数据

```sql
INSERT INTO `models` (`model_name`, `model_code`, `endpoint_id`, `provider_id`, `model_desc`, `input_price`, `output_price`, `status`, `deleted`) VALUES
('gpt-5', 'gpt-5', 1, 2, '通用旗舰模型', 1.25, 10.00, 1, 0),
('gpt-5-codex', 'gpt-5-codex', 1, 2, '旗舰编码增强模型', 1.25, 10.00, 1, 0),
('gpt-5-codex-mini', 'gpt-5-codex-mini', 1, 2, '轻量编码模型', 0.25, 2.00, 1, 0),
('gpt-5.1', 'gpt-5.1', 1, 2, '高稳定通用模型', 1.25, 10.00, 1, 0),
('gpt-5.1-codex', 'gpt-5.1-codex', 1, 2, '高稳定编码模型', 1.25, 10.00, 1, 0),
('gpt-5.1-codex-max', 'gpt-5.1-codex-max', 1, 2, '高配编码模型', 1.25, 10.00, 1, 0),
('gpt-5.1-codex-mini', 'gpt-5.1-codex-mini', 1, 2, '轻量稳定编码模型', 0.25, 2.00, 1, 0),
('gpt-5.2', 'gpt-5.2', 1, 2, '高性能通用模型', 1.75, 14.00, 1, 0),
('gpt-5.2-codex', 'gpt-5.2-codex', 1, 2, '高性能编码模型', 1.75, 14.00, 1, 0),
('gpt-5.2-high', 'gpt-5.2-high', 1, 2, '高性能高质量模型', 1.75, 14.00, 1, 0),
('gpt-5.2-low', 'gpt-5.2-low', 1, 2, '高性能低延迟模型', 1.75, 14.00, 1, 0),
('gpt-5.2-medium', 'gpt-5.2-medium', 1, 2, '高性能均衡模型', 1.75, 14.00, 1, 0),
('gpt-5.2-xhigh', 'gpt-5.2-xhigh', 1, 2, '高性能极致质量模型', 1.75, 14.00, 1, 0),
('gpt-5.3-codex', 'gpt-5.3-codex', 1, 2, '新一代编码模型', 1.75, 14.00, 1, 0),
('gpt-5.3-codex-high', 'gpt-5.3-codex-high', 1, 2, '新一代高质量编码模型', 1.75, 14.00, 1, 0),
('gpt-5.3-codex-low', 'gpt-5.3-codex-low', 1, 2, '新一代低延迟编码模型', 1.75, 14.00, 1, 0),
('gpt-5.3-codex-medium', 'gpt-5.3-codex-medium', 1, 2, '新一代均衡编码模型', 1.75, 14.00, 1, 0),
('gpt-5.3-codex-xhigh', 'gpt-5.3-codex-xhigh', 1, 2, '新一代极致质量编码模型', 1.75, 14.00, 1, 0),
('gpt-5.4', 'gpt-5.4', 1, 2, '最新旗舰通用模型', 2.50, 15.00, 1, 0),
('gpt-5.4-high', 'gpt-5.4-high', 1, 2, '最新高质量模型', 2.50, 15.00, 1, 0),
('gpt-5.4-medium', 'gpt-5.4-medium', 1, 2, '最新均衡模型', 2.50, 15.00, 1, 0),
('gpt-5.4-mini', 'gpt-5.4-mini', 1, 2, '最新轻量模型', 0.75, 4.50, 1, 0),
('gpt-5.4-xhigh', 'gpt-5.4-xhigh', 1, 2, '最新极致质量模型', 2.50, 15.00, 1, 0);
```

## 7. Provider Token

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

## 8. Customer Token

```sql
CREATE TABLE `customer_tokens` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `account_id` BIGINT NOT NULL COMMENT '账户 ID',
  `customer_name` VARCHAR(100) NOT NULL COMMENT '客户名称',
  `token_name` VARCHAR(100) NOT NULL COMMENT 'Token 名称/别名',
  `token_value` VARCHAR(128) NOT NULL COMMENT 'Token 值，SHA256 哈希存储',
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
  KEY `idx_account_id` (`account_id`),
  KEY `idx_status` (`status`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户 Token 表';
```

## 9. Usage Record

```sql
CREATE TABLE `usage_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `request_id` VARCHAR(64) NOT NULL COMMENT '请求 ID，全局唯一',
  `account_id` BIGINT NOT NULL COMMENT '账户 ID',
  `customer_token` VARCHAR(128) DEFAULT NULL COMMENT '客户 Token（明文）',
  `provider_token` VARCHAR(128) DEFAULT NULL COMMENT 'Provider Token（明文）',
  `usage_type` VARCHAR(20) DEFAULT NULL COMMENT '使用类型：balance/plan',
  `usage_from` VARCHAR(64) DEFAULT NULL COMMENT '使用来源（套餐 ID）',
  `provider_id` BIGINT NOT NULL COMMENT 'Provider ID',
  `provider_code` VARCHAR(50) NOT NULL COMMENT 'Provider 编码',
  `provider_name` VARCHAR(100) NOT NULL COMMENT 'Provider 名称',
  `endpoint_code` VARCHAR(100) NOT NULL COMMENT '端点编码',
  `model_id` BIGINT NOT NULL COMMENT '模型 ID',
  `model_code` VARCHAR(100) NOT NULL COMMENT '模型编码',
  `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
  `response_status` INT NOT NULL COMMENT '响应状态码',
  `prompt_tokens` INT DEFAULT 0 COMMENT '提示词 token 数',
  `completion_tokens` INT DEFAULT 0 COMMENT '补全 token 数',
  `total_tokens` INT DEFAULT 0 COMMENT '总 token 数',
  `prompt_cost` DECIMAL(12, 6) DEFAULT 0 COMMENT '输入 token 费用',
  `completion_cost` DECIMAL(12, 6) DEFAULT 0 COMMENT '输出 token 费用',
  `total_cost` DECIMAL(12, 6) DEFAULT 0 COMMENT '总费用',
  `latency_ms` INT DEFAULT 0 COMMENT '延迟（毫秒）',
  `error_code` VARCHAR(50) DEFAULT NULL COMMENT '错误码',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_request_id` (`request_id`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_provider_id` (`provider_id`),
  KEY `idx_model_id` (`model_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_model_code` (`model_code`),
  KEY `idx_endpoint_code` (`endpoint_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='使用记录表';
```

## 10. Plan（订阅套餐）

用于定义订阅计划（可见/隐藏），用于前端展示与订阅。

```sql
CREATE TABLE `plans` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `plan_name` VARCHAR(100) NOT NULL COMMENT '套餐名称',
  `price` DECIMAL(12, 2) NOT NULL COMMENT '套餐价格（元）',
  `duration_days` INT NOT NULL COMMENT '有效期天数',
  `daily_quota` DECIMAL(12, 6) NOT NULL COMMENT '每日额度（美元）',
  `total_quota` DECIMAL(12, 6) NOT NULL COMMENT '总额度（美元）',
  `allowed_models` JSON DEFAULT NULL COMMENT '允许访问的模型列表',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `visible` TINYINT NOT NULL DEFAULT 1 COMMENT '是否可见：1-可见，0-隐藏',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_visible` (`visible`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订阅套餐表';

INSERT INTO `plans` (`id`, `plan_name`, `price`, `duration_days`, `daily_quota`, `total_quota`, `allowed_models`, `status`, `visible`, `remark`, `create_by`, `update_by`)
VALUES
(10001, 'Codex小包套餐', 45.00, 30, 30, 900, '["codex"]', 1, 1, '仅可用 Codex', 'system', 'system'),
(10002, 'Codex中包套餐', 60.00, 30, 60, 1800, '["codex"]', 1, 1, '仅可用 Codex', 'system', 'system'),
(10003, 'Codex大包套餐', 75.00, 30, 90, 2700, '["codex"]', 1, 1, '仅可用 Codex', 'system', 'system');
```

## 11. Customer Subscription（客户订阅）

用于维护客户订阅关系与生效周期，可支持后台发放的隐藏套餐。

```sql
CREATE TABLE `customer_plans` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `account_id` BIGINT NOT NULL COMMENT '客户账户 ID',
  `plan_id` BIGINT NOT NULL COMMENT '订阅套餐 ID',
  `plan_name` VARCHAR(100) NOT NULL COMMENT '套餐名称',
  `price` DECIMAL(12, 2) NOT NULL COMMENT '订阅价格（元）',
  `duration_days` INT NOT NULL COMMENT '订阅周期天数',
  `daily_quota` DECIMAL(12, 6) NOT NULL COMMENT '每日额度（美元）',
  `total_quota` DECIMAL(12, 6) NOT NULL COMMENT '总额度（美元）',
  `used_quota` DECIMAL(12, 6) NOT NULL DEFAULT 0 COMMENT '已使用额度（美元）',
  `total_used_quota` DECIMAL(12, 6) NOT NULL DEFAULT 0 COMMENT '总共已使用额度（美元）',
  `quota_refresh_time` DATETIME NOT NULL COMMENT '额度刷新时间',
  `plan_expire_time` DATETIME NOT NULL COMMENT '订阅过期时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-生效，0-失效',
  `source` VARCHAR(50) DEFAULT NULL COMMENT '订阅来源：purchase/grant/admin 等',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_status` (`status`),
  KEY `idx_end_time` (`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户订阅表';
```

## 12. Payment Method（支付方式）

用于维护支付方式及其配置（JSON）。

```sql
CREATE TABLE `payment_methods` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `method_code` VARCHAR(50) NOT NULL COMMENT '支付方式编码',
  `method_name` VARCHAR(100) NOT NULL COMMENT '支付方式名称',
  `config` VARCHAR(2000) DEFAULT NULL COMMENT '支付方式配置（JSON字符串）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_method_code` (`method_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付方式表';
```

## 13. Third Party Pay Link（第三方支付链接）

用于配置第三方支付跳转链接，可按目标类型与目标ID区分。

```sql
CREATE TABLE `third_party_pay_links` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `target_id` BIGINT NOT NULL COMMENT '目标ID（如 plan_id）',
  `target_type` VARCHAR(50) NOT NULL COMMENT '目标类型（如 plan）',
  `pay_url` VARCHAR(500) NOT NULL COMMENT '第三方支付跳转链接',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='第三方支付链接表';
```

## 14. Activation Code Stock（激活码库存）

用于第三方支付后发放激活码，绑定套餐并控制可用状态。

```sql
CREATE TABLE `activation_code_stocks` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `activation_code` VARCHAR(100) NOT NULL COMMENT '激活码',
  `plan_id` BIGINT NOT NULL COMMENT '绑定套餐 ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-可用，0-禁用，2-已使用',
  `used_at` DATETIME DEFAULT NULL COMMENT '使用时间',
  `used_by` BIGINT DEFAULT NULL COMMENT '使用者账号 ID',
  `subscription_id` BIGINT DEFAULT NULL COMMENT '对应客户订阅记录 ID',
  `order_id` VARCHAR(64) DEFAULT NULL COMMENT '支付订单号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activation_code` (`activation_code`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='激活码库存表';
```

## 15. 脚本与执行建议

- 核心模型关系已调整为 `providers -> model_endpoints -> models`
- OpenAPI 请求中的 `model` 字段应直接对应 `models.model_code`
- `allowed_models` 建议存储 `model_code` 列表，便于授权校验
- Provider、Model Endpoint、Model 默认按 `deleted = 0` 查询
- 建表脚本建议统一沉淀到模块内的 `resources/db/` 目录
- 后续如引入 Flyway / Liquibase，可将本文件内容进一步拆分为版本化迁移脚本

