-- xlinks-router database bootstrap script
-- schema version: 2026-04-02
-- encoding: UTF-8

CREATE DATABASE IF NOT EXISTS `xlinks_router`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `xlinks_router`;

-- ============================================
-- Schema
-- ============================================

-- 1. Customer accounts
CREATE TABLE IF NOT EXISTS `customer_accounts` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `invite_code` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `invited_by` bigint(20) DEFAULT NULL,
  `password` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_invite_code` (`invite_code`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_invited_by` (`invited_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Promotion records
CREATE TABLE IF NOT EXISTS `promotion_records` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `inviter_user_id` bigint(20) NOT NULL,
  `invitee_user_id` bigint(20) NOT NULL,
  `invite_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reward_type` tinyint(4) NOT NULL,
  `reward_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `reward_rate` decimal(5,2) NOT NULL DEFAULT '0.00',
  `status` tinyint(4) NOT NULL DEFAULT '0',
  `source_order_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_inviter_user_id` (`inviter_user_id`),
  KEY `idx_invitee_user_id` (`invitee_user_id`),
  KEY `idx_invite_code` (`invite_code`),
  KEY `idx_reward_type` (`reward_type`),
  KEY `idx_status` (`status`),
  KEY `idx_source_order_no` (`source_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Promotion rules
CREATE TABLE IF NOT EXISTS `promotion_rules` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `rule_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rule_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reward_type` tinyint(4) DEFAULT NULL,
  `reward_amount` decimal(12,2) DEFAULT NULL,
  `reward_rate` decimal(5,2) DEFAULT NULL,
  `settlement_day` tinyint(4) DEFAULT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `icon_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int(11) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`),
  KEY `idx_reward_type` (`reward_type`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Admin accounts
CREATE TABLE IF NOT EXISTS `admin_accounts` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL,
  `display_name` varchar(100) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `password` varchar(128) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `last_login_at` datetime DEFAULT NULL,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) DEFAULT NULL,
  `update_by` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_username` (`username`),
  UNIQUE KEY `uk_admin_email` (`email`),
  UNIQUE KEY `uk_admin_phone` (`phone`),
  KEY `idx_admin_status` (`status`),
  KEY `idx_admin_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Providers
CREATE TABLE IF NOT EXISTS `providers` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `provider_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `provider_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `provider_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'openai-compatible',
  `supported_protocols` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `priority` int(11) NOT NULL DEFAULT '0',
  `base_url` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `provider_logo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `provider_website` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_code` (`provider_code`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Model endpoints
CREATE TABLE IF NOT EXISTS `model_endpoints` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `endpoint_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `endpoint_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `endpoint_url` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_endpoint_code` (`endpoint_code`),
  UNIQUE KEY `uk_endpoint_url` (`endpoint_url`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Standard models
CREATE TABLE IF NOT EXISTS `models` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `model_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `model_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `endpoint_id` bigint(20) NOT NULL,
  `model_desc` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `input_price` decimal(12,2) DEFAULT NULL,
  `output_price` decimal(12,2) DEFAULT NULL,
  `context_size` int(11) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_endpoint_model_code` (`endpoint_id`,`model_code`),
  KEY `idx_endpoint_id` (`endpoint_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Provider model mappings
CREATE TABLE IF NOT EXISTS `provider_models` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `provider_id` bigint(20) NOT NULL,
  `model_id` bigint(20) NOT NULL,
  `provider_model_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `provider_model_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_model` (`provider_id`,`model_id`),
  KEY `idx_provider_model_code` (`provider_id`,`provider_model_code`),
  KEY `idx_model_id` (`model_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Provider tokens
CREATE TABLE IF NOT EXISTS `provider_tokens` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `provider_id` bigint(20) NOT NULL,
  `token_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token_value` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token_status` tinyint(4) NOT NULL DEFAULT '1',
  `quota_total` bigint(20) DEFAULT NULL,
  `quota_used` bigint(20) DEFAULT '0',
  `expire_time` datetime DEFAULT NULL,
  `last_used_at` datetime DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_provider_id` (`provider_id`),
  KEY `idx_token_status` (`token_status`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Customer tokens
CREATE TABLE IF NOT EXISTS `customer_tokens` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) NOT NULL,
  `customer_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `token_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token_value` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `expire_time` datetime DEFAULT NULL,
  `allowed_models` json DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token_value` (`token_value`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_status` (`status`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Subscription plans
CREATE TABLE IF NOT EXISTS `plans` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `plan_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `price` decimal(12,2) NOT NULL,
  `duration_days` int(11) NOT NULL,
  `daily_quota` decimal(12,2) NOT NULL,
  `total_quota` decimal(12,2) NOT NULL,
  `allowed_models` json DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `visible` tinyint(4) NOT NULL DEFAULT '1',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_visible` (`visible`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Customer subscriptions
CREATE TABLE IF NOT EXISTS `customer_plans` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) NOT NULL,
  `plan_id` bigint(20) NOT NULL,
  `plan_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `price` decimal(12,2) NOT NULL,
  `duration_days` int(11) NOT NULL,
  `daily_quota` decimal(12,2) NOT NULL,
  `total_quota` decimal(12,2) NOT NULL,
  `used_quota` decimal(12,2) NOT NULL DEFAULT '0.00',
  `total_used_quota` decimal(12,2) NOT NULL DEFAULT '0.00',
  `quota_refresh_time` datetime DEFAULT NULL,
  `plan_expire_time` datetime DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `source` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_status` (`status`),
  KEY `idx_plan_expire_time` (`plan_expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. Activation code stocks
CREATE TABLE IF NOT EXISTS `activation_code_stocks` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `activation_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `plan_id` bigint(20) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `used_at` datetime DEFAULT NULL,
  `used_by` bigint(20) DEFAULT NULL,
  `subscription_id` bigint(20) DEFAULT NULL,
  `order_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activation_code` (`activation_code`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. Third-party pay links
CREATE TABLE IF NOT EXISTS `third_party_pay_links` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `target_id` bigint(20) NOT NULL,
  `target_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `pay_url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`,`target_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. Usage records
CREATE TABLE IF NOT EXISTS `usage_records` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `request_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `customer_token` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `provider_token` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `usage_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `usage_from` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `provider_id` bigint(20) NOT NULL,
  `provider_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `provider_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `endpoint_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `model_id` bigint(20) NOT NULL,
  `model_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `model_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `response_status` int(11) NOT NULL,
  `prompt_tokens` int(11) DEFAULT '0',
  `completion_tokens` int(11) DEFAULT '0',
  `total_tokens` int(11) DEFAULT '0',
  `prompt_cost` decimal(12,6) DEFAULT '0.000000',
  `completion_cost` decimal(12,6) DEFAULT '0.000000',
  `total_cost` decimal(12,6) DEFAULT '0.000000',
  `latency_ms` int(11) DEFAULT '0',
  `error_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `error_message` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_request_id` (`request_id`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_provider_id` (`provider_id`),
  KEY `idx_model_id` (`model_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_model_code` (`model_code`),
  KEY `idx_endpoint_code` (`endpoint_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Seed data
-- ============================================

INSERT IGNORE INTO `model_endpoints`
  (`id`, `endpoint_code`, `endpoint_name`, `endpoint_url`, `status`, `create_by`, `update_by`)
VALUES
  (1, 'chat/completions', 'Chat Completions', '/v1/chat/completions', 1, 'system', 'system'),
  (2, 'responses', 'Responses', '/v1/responses', 1, 'system', 'system');

INSERT IGNORE INTO `providers`
  (`id`, `provider_code`, `provider_name`, `provider_type`, `supported_protocols`, `priority`, `base_url`, `provider_logo`, `provider_website`, `status`, `remark`, `create_by`, `update_by`)
VALUES
  (1, 'deepseek', 'DeepSeek', 'openai-compatible', 'chat/completions,responses', 100, 'https://api.deepseek.com/v1', 'https://deepseek.com/logo.png', 'https://deepseek.com', 1, 'default provider', 'system', 'system'),
  (2, 'right-codex', 'Right Codex', 'openai-compatible', 'chat/completions', 50, 'https://right.codes/codex/v1', NULL, 'https://right.codes', 1, 'test provider', 'system', 'system');

INSERT IGNORE INTO `models`
  (`id`, `model_name`, `model_code`, `endpoint_id`, `model_desc`, `input_price`, `output_price`, `context_size`, `status`, `remark`, `create_by`, `update_by`)
VALUES
  (1, 'DeepSeek V3', 'deepseek-v3', 1, 'DeepSeek V3 chat model', 0.27, 1.10, 64000, 1, 'default chat model', 'system', 'system'),
  (2, 'DeepSeek Chat', 'deepseek-chat', 1, 'DeepSeek Chat model', 0.14, 0.28, 32000, 1, 'chat model', 'system', 'system'),
  (3, 'GPT-5.2', 'gpt-5.2', 1, 'GPT-5.2 chat model', 2.00, 8.00, 128000, 1, 'aggregated model', 'system', 'system'),
  (4, 'GPT-5.3', 'gpt-5.3', 1, 'GPT-5.3 chat model', 3.00, 15.00, 128000, 1, 'aggregated model', 'system', 'system'),
  (5, 'GPT-5.4', 'gpt-5.4', 1, 'GPT-5.4 chat model', 5.00, 20.00, 200000, 1, 'aggregated model', 'system', 'system');

INSERT IGNORE INTO `provider_models`
  (`provider_id`, `model_id`, `provider_model_code`, `provider_model_name`, `status`, `remark`, `create_by`, `update_by`)
VALUES
  (1, 1, 'deepseek-v3', 'DeepSeek V3', 1, 'DeepSeek mapping', 'system', 'system'),
  (1, 2, 'deepseek-chat', 'DeepSeek Chat', 1, 'DeepSeek mapping', 'system', 'system'),
  (2, 3, 'gpt-5.2', 'GPT-5.2', 1, 'Right Codex mapping', 'system', 'system'),
  (2, 4, 'gpt-5.3', 'GPT-5.3', 1, 'Right Codex mapping', 'system', 'system'),
  (2, 5, 'gpt-5.4', 'GPT-5.4', 1, 'Right Codex mapping', 'system', 'system');

INSERT IGNORE INTO `provider_tokens`
  (`provider_id`, `token_name`, `token_value`, `token_status`, `quota_total`, `quota_used`, `remark`, `create_by`, `update_by`)
VALUES
  (2, 'right-codex-default', 'sk-16dc1a6df0c04c7c851ec8c326f5f79b', 1, NULL, 0, 'default provider token', 'system', 'system');

INSERT IGNORE INTO `promotion_rules`
  (`rule_code`, `rule_name`, `reward_type`, `reward_amount`, `reward_rate`, `settlement_day`, `description`, `icon_type`, `sort_order`, `status`, `remark`, `create_by`, `update_by`)
VALUES
  ('invite_register', '邀请注册奖励', 1, 10.00, NULL, NULL, '好友通过您的链接注册，您将获得 10 元奖励', 'blue', 1, 1, '默认推广规则', 'system', 'system'),
  ('first_recharge', '首次充值奖励', 2, NULL, 10.00, NULL, '好友首次充值，您将获得充值金额 10% 的奖励', 'green', 2, 1, '默认推广规则', 'system', 'system'),
  ('consumption_rebate', '持续返佣', 3, NULL, 5.00, NULL, '好友每次消费，您将获得消费金额 5% 的返佣', 'purple', 3, 1, '默认推广规则', 'system', 'system'),
  ('settlement_cycle', '结算周期', NULL, NULL, NULL, 1, '每月 1 日自动结算上月收益，直接转入账户余额', 'orange', 4, 1, '默认推广规则', 'system', 'system');

INSERT IGNORE INTO `plans`
  (`id`, `plan_name`, `price`, `duration_days`, `daily_quota`, `total_quota`, `allowed_models`, `status`, `visible`, `remark`, `create_by`, `update_by`)
VALUES
  (10001, '体验套餐', 19.90, 30, 10.00, 300.00, JSON_ARRAY('gpt-5.2', 'deepseek-chat'), 1, 1, '适合轻量体验', 'system', 'system'),
  (10002, '标准套餐', 59.90, 30, 30.00, 900.00, JSON_ARRAY('gpt-5.2', 'gpt-5.3', 'deepseek-chat'), 1, 1, '适合日常开发与测试', 'system', 'system'),
  (10003, '旗舰套餐', 129.90, 30, 80.00, 2400.00, JSON_ARRAY('gpt-5.2', 'gpt-5.3', 'gpt-5.4', 'deepseek-v3'), 1, 1, '适合高频使用场景', 'system', 'system');

INSERT IGNORE INTO `third_party_pay_links`
  (`target_id`, `target_type`, `pay_url`, `status`, `remark`, `create_by`, `update_by`)
VALUES
  (10001, 'plan', 'https://dwz.cn/KT1tozGu', 1, '体验套餐支付链接', 'system', 'system'),
  (10002, 'plan', 'https://dwz.cn/0Nj6pooi', 1, '标准套餐支付链接', 'system', 'system'),
  (10003, 'plan', 'https://dwz.cn/qCXw5k5E', 1, '旗舰套餐支付链接', 'system', 'system');

INSERT IGNORE INTO `customer_tokens`
  (`account_id`, `customer_name`, `token_name`, `token_value`, `status`, `allowed_models`, `remark`, `create_by`, `update_by`)
VALUES
  (
    1,
    'right-codex-test-customer',
    'right-codex-customer-token',
    '4f7d9c2a1b6e43d8a5c0f1e2b3d4a5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a',
    1,
    JSON_ARRAY('gpt-5.2', 'gpt-5.3', 'gpt-5.4'),
    'default customer token',
    'system',
    'system'
  );
