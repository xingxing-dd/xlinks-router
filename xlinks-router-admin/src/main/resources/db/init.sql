-- xlinks-router 数据库初始化脚本
-- 数据库版本: 2026-03-19
-- 数据模型: Provider + Model Endpoint + Model 三级结构

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `xlinks_router`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `xlinks_router`;

-- ============================================
-- 表结构定义
-- ============================================

-- 1. 客户账户表
CREATE TABLE IF NOT EXISTS `customer_accounts` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `invite_code` VARCHAR(32) DEFAULT NULL COMMENT '邀请码',
  `invited_by` BIGINT DEFAULT NULL COMMENT '邀请人账户ID，关联 customer_accounts.id',
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
  UNIQUE KEY `uk_invite_code` (`invite_code`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_invited_by` (`invited_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户账户表';

-- 推广记录表
CREATE TABLE IF NOT EXISTS `promotion_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `inviter_user_id` BIGINT NOT NULL COMMENT '邀请人账户ID',
  `invitee_user_id` BIGINT NOT NULL COMMENT '被邀请人账户ID',
  `invite_code` VARCHAR(32) NOT NULL COMMENT '邀请码',
  `reward_type` TINYINT NOT NULL COMMENT '奖励类型：1-邀请注册，2-首次充值，3-持续返佣',
  `reward_amount` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '奖励金额',
  `reward_rate` DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '奖励比例',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待结算，1-已生效，2-已失效',
  `source_order_no` VARCHAR(64) DEFAULT NULL COMMENT '来源订单号',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_inviter_user_id` (`inviter_user_id`),
  KEY `idx_invitee_user_id` (`invitee_user_id`),
  KEY `idx_invite_code` (`invite_code`),
  KEY `idx_reward_type` (`reward_type`),
  KEY `idx_status` (`status`),
  KEY `idx_source_order_no` (`source_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推广记录表';

-- 推广规则配置表
CREATE TABLE IF NOT EXISTS `promotion_rules` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rule_code` VARCHAR(64) NOT NULL COMMENT '规则编码',
  `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
  `reward_type` TINYINT DEFAULT NULL COMMENT '奖励类型：1-邀请注册，2-首次充值，3-持续返佣，空表示非奖励类规则',
  `reward_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '固定奖励金额',
  `reward_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '奖励比例，按百分比存储',
  `settlement_day` TINYINT DEFAULT NULL COMMENT '结算日',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '规则描述',
  `icon_type` VARCHAR(32) DEFAULT NULL COMMENT '前端图标类型',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`),
  KEY `idx_reward_type` (`reward_type`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推广规则配置表';

-- 2. 提供商表
CREATE TABLE IF NOT EXISTS `providers` (
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

-- 3. 模型端点表
CREATE TABLE IF NOT EXISTS `model_endpoints` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `endpoint_name` VARCHAR(100) NOT NULL COMMENT '端点名称，如 chat/completions',
  `endpoint_desc` VARCHAR(500) DEFAULT NULL COMMENT '端点描述',
  `endpoint_url` VARCHAR(255) NOT NULL COMMENT '端点 URL，如 /v1/chat/completions',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
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

-- 4. 统一模型表
CREATE TABLE IF NOT EXISTS `models` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
  `model_code` VARCHAR(100) NOT NULL COMMENT '模型编码',
  `endpoint_id` BIGINT NOT NULL COMMENT '模型端点 ID',
  `provider_id` BIGINT NOT NULL COMMENT '服务商 ID',
  `model_desc` VARCHAR(500) DEFAULT NULL COMMENT '模型描述',
  `input_price` DECIMAL(12, 2) DEFAULT NULL COMMENT '输入价格，单位：每百万 token',
  `output_price` DECIMAL(12, 2) DEFAULT NULL COMMENT '输出价格，单位：每百万 token',
  `context_size` INT DEFAULT NULL COMMENT '上下文大小，单位：K',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
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

-- 5. Provider Token 表
CREATE TABLE IF NOT EXISTS `provider_tokens` (
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

-- 6. 客户 Token 表
CREATE TABLE IF NOT EXISTS `customer_tokens` (
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

-- 7. 使用记录表
CREATE TABLE IF NOT EXISTS `usage_records` (
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

-- ============================================
-- 初始化数据
-- ============================================

-- 插入 Model Endpoint
INSERT INTO `model_endpoints`
  (`endpoint_name`, `endpoint_desc`, `endpoint_url`, `status`, `create_by`, `update_by`)
VALUES
  ('chat/completions', '聊天补全端点', '/v1/chat/completions', 1, 'system', 'system');

-- 插入 Provider
INSERT INTO `providers`
  (`provider_code`, `provider_name`, `provider_type`, `base_url`, `provider_logo`, `provider_website`, `status`, `remark`, `create_by`, `update_by`)
VALUES
  ('deepseek', 'DeepSeek', 'openai-compatible', 'https://api.deepseek.com/v1', 'https://deepseek.com/logo.png', 'https://deepseek.com', 1, '默认初始化 Provider', 'system', 'system'),
  ('right-codex', 'Right Codex', 'openai-compatible', 'https://right.codes/codex/v1', NULL, 'https://right.codes', 1, '测试 Provider', 'system', 'system');

-- 插入 Model
INSERT INTO `models`
  (`model_name`, `model_code`, `endpoint_id`, `provider_id`, `model_desc`, `input_price`, `output_price`, `context_size`, `status`, `remark`, `create_by`, `update_by`)
VALUES
  ('DeepSeek V3', 'deepseek-v3', 1, 1, 'DeepSeek V3 聊天模型', 0.27, 1.10, 64000, 1, '默认聊天模型', 'system', 'system'),
  ('DeepSeek Chat', 'deepseek-chat', 1, 1, 'DeepSeek Chat 聊天模型', 0.14, 0.28, 32000, 1, 'DeepSeek Chat 模型', 'system', 'system'),
  ('GPT-5.2', 'gpt-5.2', 1, 2, 'GPT-5.2 聊天模型', 2.00, 8.00, 128000, 1, '测试 Provider 模型', 'system', 'system'),
  ('GPT-5.3', 'gpt-5.3', 1, 2, 'GPT-5.3 聊天模型', 3.00, 15.00, 128000, 1, '测试 Provider 模型', 'system', 'system'),
  ('GPT-5.4', 'gpt-5.4', 1, 2, 'GPT-5.4 聊天模型', 5.00, 20.00, 200000, 1, '测试 Provider 模型', 'system', 'system');

-- 插入 Provider Token
INSERT INTO `provider_tokens`
  (`provider_id`, `token_name`, `token_value`, `token_status`, `quota_total`, `quota_used`, `remark`, `create_by`, `update_by`)
VALUES
  (2, 'right-codex-default', 'sk-16dc1a6df0c04c7c851ec8c326f5f79b', 1, NULL, 0, '测试 Provider Token', 'system', 'system');

-- 插入推广规则
INSERT INTO `promotion_rules`
  (`rule_code`, `rule_name`, `reward_type`, `reward_amount`, `reward_rate`, `settlement_day`, `description`, `icon_type`, `sort_order`, `status`, `remark`, `create_by`, `update_by`)
VALUES
  ('invite_register', '邀请注册奖励', 1, 10.00, NULL, NULL, '好友通过您的链接注册，您将获得 ￥10.00 奖励', 'blue', 1, 1, '默认推广规则', 'system', 'system'),
  ('first_recharge', '首次充值奖励', 2, NULL, 10.00, NULL, '好友首次充值，您将获得充值金额 10% 的奖励', 'green', 2, 1, '默认推广规则', 'system', 'system'),
  ('consumption_rebate', '持续返佣', 3, NULL, 5.00, NULL, '好友每次消费，您将获得消费金额 5% 的返佣', 'purple', 3, 1, '默认推广规则', 'system', 'system'),
  ('settlement_cycle', '结算周期', NULL, NULL, NULL, 1, '每月 1 日自动结算上月收益，直接转入账户余额', 'orange', 4, 1, '默认推广规则', 'system', 'system');

-- 插入 Customer Token
INSERT INTO `customer_tokens`
  (`account_id`, `customer_name`, `token_name`, `token_value`, `status`, `allowed_models`, `remark`, `create_by`, `update_by`)
VALUES
  (
    1,
    'right-codex-test-customer',
    'right-codex-customer-token',
    '4f7d9c2a1b6e43d8a5c0f1e2b3d4a5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a',
    1,
    JSON_ARRAY('gpt-5.2', 'gpt-5.3', 'gpt-5.4'),
    '测试客户 Token',
    'system',
    'system'
  );
