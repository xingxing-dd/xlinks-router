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
  `settle_at` datetime DEFAULT NULL,
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
  `supported_protocols` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `priority` int(11) NOT NULL DEFAULT '0',
  `base_url` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `provider_logo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `provider_website` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `concurrency_limit_enabled` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否启用 provider token 并发限制',
  `max_concurrent_per_token` int(11) NOT NULL DEFAULT '0' COMMENT '每个 provider token 最大并发会话数',
  `acquire_timeout_ms` int(11) NOT NULL DEFAULT '0' COMMENT '获取 permit 等待时间',
  `request_timeout_ms` int(11) NOT NULL DEFAULT '20000' COMMENT '非流式请求超时',
  `stream_first_response_timeout_ms` int(11) NOT NULL DEFAULT '20000' COMMENT '流式首包超时',
  `stream_idle_timeout_ms` int(11) NOT NULL DEFAULT '20000' COMMENT '流式空闲超时',
  `session_lease_ms` int(11) NOT NULL DEFAULT '30000' COMMENT 'permit 租约时长',
  `session_renew_interval_ms` int(11) NOT NULL DEFAULT '10000' COMMENT 'permit 续租周期',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_code` (`provider_code`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Standard models
CREATE TABLE IF NOT EXISTS `models` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `model_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `model_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `model_provider` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Display vendor label, e.g. OPENAI / ANTHROPIC',
  `model_desc` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `input_price` decimal(12,2) DEFAULT NULL,
  `output_price` decimal(12,2) DEFAULT NULL,
  `cache_hit_price` decimal(12,2) DEFAULT NULL,
  `context_size` int(11) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_code` (`model_code`),
  KEY `idx_model_provider` (`model_provider`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Provider model mappings
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

-- 8. Provider tokens
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

-- 9. Customer tokens
CREATE TABLE IF NOT EXISTS `customer_tokens` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) NOT NULL,
  `customer_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `token_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token_value` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `expire_time` datetime DEFAULT NULL,
  `allowed_models` json DEFAULT NULL,
  `daily_quota` decimal(12,6) DEFAULT NULL,
  `used_quota` decimal(12,6) DEFAULT NULL,
  `total_quota` decimal(12,6) DEFAULT NULL,
  `total_used_quota` decimal(12,6) DEFAULT NULL,
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

-- 10. Subscription plans
CREATE TABLE IF NOT EXISTS `plans` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `plan_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `price` decimal(12,6) NOT NULL,
  `duration_days` int(11) NOT NULL,
  `daily_quota` decimal(12,6) NOT NULL,
  `total_quota` decimal(12,6) NOT NULL,
  `multiplier` decimal(10,4) NOT NULL DEFAULT '1.0000' COMMENT 'Cache-hit billing multiplier',
  `max_purchase_count` int(11) DEFAULT NULL COMMENT 'Maximum purchase count per account, NULL means unlimited',
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

-- 11. Customer subscriptions
CREATE TABLE IF NOT EXISTS `customer_plans` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) NOT NULL,
  `plan_id` bigint(20) NOT NULL,
  `plan_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `price` decimal(12,6) NOT NULL,
  `duration_days` int(11) NOT NULL,
  `daily_quota` decimal(12,6) NOT NULL,
  `total_quota` decimal(12,6) NOT NULL,
  `multiplier` decimal(10,4) NOT NULL DEFAULT '1.0000' COMMENT 'Cache-hit billing multiplier snapshot',
  `used_quota` decimal(12,6) NOT NULL DEFAULT '0.000000',
  `total_used_quota` decimal(12,6) NOT NULL DEFAULT '0.000000',
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
  KEY `idx_account_plan_source` (`account_id`,`plan_id`,`source`),
  KEY `idx_status` (`status`),
  KEY `idx_plan_expire_time` (`plan_expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `merchant_provider_routes` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) NOT NULL,
  `model_id` bigint(20) NOT NULL,
  `provider_id` bigint(20) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_model` (`account_id`,`model_id`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_provider_id` (`provider_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Activation code stocks
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

-- 13. Payment methods
CREATE TABLE IF NOT EXISTS `payment_methods` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `method_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `method_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `method_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `icon_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort` int(11) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `config_json` longtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_method_code` (`method_code`),
  KEY `idx_method_type` (`method_type`),
  KEY `idx_status` (`status`),
  KEY `idx_sort` (`sort`)
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

-- 15. Customer orders
CREATE TABLE IF NOT EXISTS `customer_orders` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台订单号',
  `ref_no` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '支付渠道关联订单号',
  `account_id` bigint(20) DEFAULT NULL COMMENT '下单用户 ID',
  `order_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单类型（充值/提现/购买订阅等）',
  `order_title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单标题',
  `order_info` json DEFAULT NULL COMMENT '订单详情快照(JSON)',
  `payment_channel` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '支付通道',
  `total_amount` decimal(12,2) NOT NULL COMMENT '订单金额',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态：0待支付，1支付成功，2支付失败，3已关闭，4已退款',
  `complete_at` datetime DEFAULT NULL COMMENT '支付完成时间',
  `expired_at` datetime DEFAULT NULL COMMENT '支付过期时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_ref_no` (`ref_no`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_order_type` (`order_type`),
  KEY `idx_payment_channel` (`payment_channel`),
  KEY `idx_status` (`status`),
  KEY `idx_complete_at` (`complete_at`),
  KEY `idx_expired_at` (`expired_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. Customer main wallets
CREATE TABLE IF NOT EXISTS `customer_main_wallets` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) NOT NULL,
  `wallet_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_balance` decimal(18,6) NOT NULL DEFAULT '0.000000',
  `available_balance` decimal(18,6) NOT NULL DEFAULT '0.000000',
  `allow_in` tinyint(4) NOT NULL DEFAULT '1',
  `allow_out` tinyint(4) NOT NULL DEFAULT '1',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wallet_account_id` (`account_id`),
  UNIQUE KEY `uk_wallet_no` (`wallet_no`),
  KEY `idx_wallet_status` (`status`),
  KEY `idx_wallet_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. Customer sub wallets
CREATE TABLE IF NOT EXISTS `customer_sub_wallets` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `main_wallet_id` bigint(20) NOT NULL,
  `wallet_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `wallet_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `balance` decimal(18,6) NOT NULL DEFAULT '0.000000',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sub_wallet_no` (`wallet_no`),
  UNIQUE KEY `uk_main_wallet_type` (`main_wallet_id`,`wallet_type`),
  KEY `idx_sub_wallet_status` (`status`),
  KEY `idx_sub_wallet_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 18. Customer main wallet flows
CREATE TABLE IF NOT EXISTS `customer_main_wallet_flows` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `main_wallet_id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `order_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `biz_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `direction` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `change_amount` decimal(18,6) NOT NULL DEFAULT '0.000000',
  `total_balance_before` decimal(18,6) NOT NULL DEFAULT '0.000000',
  `total_balance_after` decimal(18,6) NOT NULL DEFAULT '0.000000',
  `available_balance_before` decimal(18,6) NOT NULL DEFAULT '0.000000',
  `available_balance_after` decimal(18,6) NOT NULL DEFAULT '0.000000',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wallet_order_biz` (`main_wallet_id`,`order_no`,`biz_type`),
  KEY `idx_wallet_flow_account_id` (`account_id`),
  KEY `idx_wallet_flow_created_at` (`created_at`),
  KEY `idx_wallet_flow_biz_type` (`biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 19. Customer sub wallet flows
CREATE TABLE IF NOT EXISTS `customer_sub_wallet_flows` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sub_wallet_id` bigint(20) NOT NULL,
  `main_wallet_id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `order_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wallet_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `biz_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `direction` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `change_amount` decimal(18,6) NOT NULL DEFAULT '0.000000',
  `balance_before` decimal(18,6) NOT NULL DEFAULT '0.000000',
  `balance_after` decimal(18,6) NOT NULL DEFAULT '0.000000',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sub_wallet_order_biz` (`sub_wallet_id`,`order_no`,`biz_type`),
  KEY `idx_sub_wallet_flow_account_id` (`account_id`),
  KEY `idx_sub_wallet_flow_created_at` (`created_at`),
  KEY `idx_sub_wallet_flow_biz_type` (`biz_type`),
  KEY `idx_sub_wallet_flow_type` (`wallet_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 20. Usage records
CREATE TABLE IF NOT EXISTS `usage_records` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `request_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `customer_token` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `provider_token` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `provider_token_id` bigint(20) DEFAULT NULL COMMENT 'Provider Token ID',
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
  `cache_hit_tokens` int(11) DEFAULT '0',
  `prompt_cost` decimal(12,6) DEFAULT '0.000000',
  `cache_hit_cost` decimal(12,6) DEFAULT '0.000000',
  `completion_cost` decimal(12,6) DEFAULT '0.000000',
  `total_cost` decimal(12,6) DEFAULT '0.000000',
  `response_ms` int(11) DEFAULT NULL COMMENT 'Time to first response data in milliseconds',
  `session_ms` int(11) DEFAULT '0' COMMENT 'Session duration in milliseconds',
  `error_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `error_message` text COLLATE utf8mb4_unicode_ci,
  `finish_reason` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结束原因',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_request_id` (`request_id`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_provider_id` (`provider_id`),
  KEY `idx_provider_token_id` (`provider_token_id`),
  KEY `idx_model_id` (`model_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_model_code` (`model_code`),
  KEY `idx_endpoint_code` (`endpoint_code`),
  KEY `idx_finish_reason` (`finish_reason`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. Contact messages 联系我们问题记录表
CREATE TABLE IF NOT EXISTS `contact_messages` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `subject` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `message` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_email` (`email`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 18. Contact message records 联系问题沟通记录表
CREATE TABLE IF NOT EXISTS `contact_message_records` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `contact_message_id` bigint(20) NOT NULL,
  `sender_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sender_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_contact_message_id` (`contact_message_id`),
  KEY `idx_sender_type` (`sender_type`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 18.1 Contact faqs 联系页常见问题表
CREATE TABLE IF NOT EXISTS `contact_faqs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `question` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `answer` varchar(4000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int(11) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `contact_faqs` (`question`, `answer`, `sort_order`, `status`, `remark`, `create_by`, `update_by`)
SELECT '如何获取 API Key?', '您可以登录平台后进入“令牌管理”页面，点击“创建 Token”生成专属 API Key。创建成功后请妥善保存，避免泄露。', 10, 1, '联系页常见问题', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `contact_faqs` WHERE `question` = '如何获取 API Key?'
);

INSERT INTO `contact_faqs` (`question`, `answer`, `sort_order`, `status`, `remark`, `create_by`, `update_by`)
SELECT '套餐可以随时更改吗?', '可以。您可以根据业务需求选择新的套餐方案，新的配置会按平台规则生效；如涉及已生效套餐，请以当前订阅说明和结算规则为准。', 20, 1, '联系页常见问题', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `contact_faqs` WHERE `question` = '套餐可以随时更改吗?'
);

INSERT INTO `contact_faqs` (`question`, `answer`, `sort_order`, `status`, `remark`, `create_by`, `update_by`)
SELECT '如何查看使用统计?', '登录后进入“数据看板”或相关使用记录页面，即可查看请求量、Token 消耗、费用统计以及模型调用分布等信息。', 30, 1, '联系页常见问题', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `contact_faqs` WHERE `question` = '如何查看使用统计?'
);

INSERT INTO `contact_faqs` (`question`, `answer`, `sort_order`, `status`, `remark`, `create_by`, `update_by`)
SELECT '支持哪些支付方式?', '当前平台支持支付宝、微信支付以及第三方支付链接，具体可用方式请以购买页面展示的支付渠道为准。', 40, 1, '联系页常见问题', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `contact_faqs` WHERE `question` = '支持哪些支付方式?'
);

INSERT INTO `contact_faqs` (`question`, `answer`, `sort_order`, `status`, `remark`, `create_by`, `update_by`)
SELECT '推广奖励如何结算?', '推广奖励会根据平台推广规则按结算周期自动统计并发放，您可以在推广页面查看奖励明细、待结算金额和历史结算记录。', 50, 1, '联系页常见问题', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `contact_faqs` WHERE `question` = '推广奖励如何结算?'
);


-- 17.2 Contact channel configs
CREATE TABLE IF NOT EXISTS `contact_channel_configs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `channel_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_value` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `action_link` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `action_label` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int(11) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_channel_type` (`channel_type`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


INSERT INTO `contact_channel_configs` (`channel_type`, `title`, `description`, `contact_value`, `action_link`, `action_label`, `sort_order`, `status`, `remark`)
SELECT 'email', '邮箱支持', '我们会在 24 小时内回复您的邮件', 'support@token-hub.com', 'mailto:support@token-hub.com', NULL, 10, 1, '默认邮箱支持'
WHERE NOT EXISTS (
  SELECT 1 FROM `contact_channel_configs` WHERE `channel_type` = 'email' AND `contact_value` = 'support@token-hub.com'
);

INSERT INTO `contact_channel_configs` (`channel_type`, `title`, `description`, `contact_value`, `action_link`, `action_label`, `sort_order`, `status`, `remark`)
SELECT 'online', '在线客服', '工作日 9:00 - 18:00 在线服务', '启动在线对话', '#', '启动在线对话', 20, 1, '默认在线客服入口'
WHERE NOT EXISTS (
  SELECT 1 FROM `contact_channel_configs` WHERE `channel_type` = 'online' AND `title` = '在线客服'
);

INSERT INTO `contact_channel_configs` (`channel_type`, `title`, `description`, `contact_value`, `action_link`, `action_label`, `sort_order`, `status`, `remark`)
SELECT 'phone', '电话支持', '企业版用户专享电话支持', '400-123-4567', 'tel:+864001234567', NULL, 30, 1, '默认电话支持'
WHERE NOT EXISTS (
  SELECT 1 FROM `contact_channel_configs` WHERE `channel_type` = 'phone' AND `contact_value` = '400-123-4567'
);


-- ============================================
-- Seed data
-- ============================================

INSERT IGNORE INTO `providers`
  (`id`, `provider_code`, `provider_name`, `supported_protocols`, `priority`, `base_url`, `provider_logo`, `provider_website`, `status`, `remark`, `create_by`, `update_by`)
VALUES
  (1, 'deepseek', 'DeepSeek', 'chat/completions,responses', 100, 'https://api.deepseek.com/v1', 'https://deepseek.com/logo.png', 'https://deepseek.com', 1, 'default provider', 'system', 'system'),
  (2, 'right-codex', 'Right Codex', 'chat/completions', 50, 'https://right.codes/codex/v1', NULL, 'https://right.codes', 1, 'test provider', 'system', 'system');

INSERT IGNORE INTO `models`
  (`id`, `model_name`, `model_code`, `model_provider`, `model_desc`, `input_price`, `output_price`, `cache_hit_price`, `context_size`, `status`, `remark`, `create_by`, `update_by`)
VALUES
  (1, 'DeepSeek V3', 'deepseek-v3', 'DEEPSEEK', 'DeepSeek V3 chat model', 0.27, 1.10, 0.27, 64000, 1, 'default chat model', 'system', 'system'),
  (2, 'DeepSeek Chat', 'deepseek-chat', 'DEEPSEEK', 'DeepSeek Chat model', 0.14, 0.28, 0.14, 32000, 1, 'chat model', 'system', 'system'),
  (3, 'GPT-5.2', 'gpt-5.2', 'OPENAI', 'GPT-5.2 chat model', 2.00, 8.00, 0.50, 128000, 1, 'aggregated model', 'system', 'system'),
  (4, 'GPT-5.3', 'gpt-5.3', 'OPENAI', 'GPT-5.3 chat model', 3.00, 15.00, 0.75, 128000, 1, 'aggregated model', 'system', 'system'),
  (5, 'GPT-5.4', 'gpt-5.4', 'OPENAI', 'GPT-5.4 chat model', 5.00, 20.00, 1.25, 200000, 1, 'aggregated model', 'system', 'system');

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
  ('invite_register', '邀请注册奖励', 1, 10.00, NULL, NULL, '好友通过您的链接注册，您将获得 ￥10.00 奖励', 'blue', 1, 1, '默认推广规则', 'system', 'system'),
  ('first_recharge', '首次充值奖励', 2, NULL, 10.00, NULL, '好友首次充值，您将获得充值金额 10% 的奖励', 'green', 2, 1, '默认推广规则', 'system', 'system'),
  ('consumption_rebate', '持续返佣', 3, NULL, 5.00, NULL, '好友每次消费，您将获得消费金额 5% 的返佣', 'purple', 3, 1, '默认推广规则', 'system', 'system'),
  ('settlement_cycle', '结算周期', NULL, NULL, NULL, 1, '每月 1 日自动结算上月收益，直接转入账户余额', 'orange', 4, 1, '默认推广规则', 'system', 'system');

INSERT IGNORE INTO `plans`
  (`id`, `plan_name`, `price`, `duration_days`, `daily_quota`, `total_quota`, `allowed_models`, `status`, `visible`, `remark`, `create_by`, `update_by`)
VALUES
  (10001, 'Codex小包套餐', 19.90, 30, 10.00, 300.00, JSON_ARRAY('gpt-5.2', 'deepseek-chat'), 1, 1, '仅可用基础模型', 'system', 'system'),
  (10002, 'Codex中包套餐', 59.90, 30, 30.00, 900.00, JSON_ARRAY('gpt-5.2', 'gpt-5.3', 'deepseek-chat'), 1, 1, '支持进阶模型使用', 'system', 'system'),
  (10003, 'Codex大包套餐', 129.90, 30, 80.00, 2400.00, JSON_ARRAY('gpt-5.2', 'gpt-5.3', 'gpt-5.4', 'deepseek-v3'), 1, 1, '全量模型可用', 'system', 'system');

INSERT IGNORE INTO `payment_methods`
  (`method_code`, `method_name`, `method_type`, `icon_url`, `sort`, `status`, `config_json`, `remark`, `create_by`, `update_by`)
VALUES
  ('alipay_official', '支付宝官方收款', 'alipay', NULL, 10, 1, '{"appId":"demo-alipay-app","merchantId":"2088100000000000","notifyUrl":"https://example.com/pay/notify/alipay"}', '默认支付宝配置', 'system', 'system'),
  ('wechat_native', '微信支付', 'wechat', NULL, 20, 1, '{"appId":"wx-demo-app","merchantId":"1900000109","apiV3Key":"demo-key","notifyUrl":"https://example.com/pay/notify/wechat"}', '默认微信支付配置', 'system', 'system'),
  ('local_gateway', '本地网关支付', 'local', NULL, 30, 0, '{"gatewayUrl":"https://pay.example.local/submit","merchantNo":"LOCAL10001","signKey":"demo-sign-key"}', '本地支付网关占位配置', 'system', 'system');

INSERT IGNORE INTO `third_party_pay_links`
  (`target_id`, `target_type`, `pay_url`, `status`, `remark`, `create_by`, `update_by`)
VALUES
  (10001, 'plan', 'https://dwz.cn/KT1tozGu', 1, '小包套餐支付链接', 'system', 'system'),
  (10002, 'plan', 'https://dwz.cn/0Nj6pooi', 1, '中包套餐支付链接', 'system', 'system'),
  (10003, 'plan', 'https://dwz.cn/qCXw5k5E', 1, '大包套餐支付链接', 'system', 'system');

INSERT IGNORE INTO `customer_tokens`
  (`account_id`, `customer_name`, `token_name`, `token_value`, `status`, `allowed_models`, `daily_quota`, `used_quota`, `total_quota`, `total_used_quota`, `remark`, `create_by`, `update_by`)
VALUES
  (
    1,
    'right-codex-test-customer',
    'right-codex-customer-token',
    '4f7d9c2a1b6e43d8a5c0f1e2b3d4a5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a',
    1,
    JSON_ARRAY('gpt-5.2', 'gpt-5.3', 'gpt-5.4'),
    NULL,
    0.00,
    NULL,
    0.00,
    'default customer token',
    'system',
    'system'
  );
