CREATE DATABASE IF NOT EXISTS `xlinks_router`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `xlinks_router`;

CREATE TABLE IF NOT EXISTS `providers` (
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

CREATE TABLE IF NOT EXISTS `customer_models` (
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

CREATE TABLE IF NOT EXISTS `provider_models` (
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

CREATE TABLE IF NOT EXISTS `model_mapping` (
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

CREATE TABLE IF NOT EXISTS `customer_tokens` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
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
  KEY `idx_status` (`status`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户 Token 表';

CREATE TABLE IF NOT EXISTS `usage_records` (
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
  UNIQUE KEY `uk_request_id` (`request_id`),
  KEY `idx_customer_token_id` (`customer_token_id`),
  KEY `idx_provider_id` (`provider_id`),
  KEY `idx_provider_token_id` (`provider_token_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_request_model` (`request_model`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='使用记录表';

INSERT INTO `providers`
  (`provider_code`, `provider_name`, `provider_type`, `base_url`, `status`, `remark`, `create_by`, `update_by`)
VALUES
  ('deepseek', 'DeepSeek', 'openai-compatible', 'https://api.deepseek.com/v1', 1, '默认初始化 Provider', 'system', 'system');

INSERT INTO `customer_models`
  (`logic_model_code`, `logic_model_name`, `model_type`, `status`, `is_default`, `remark`, `create_by`, `update_by`)
VALUES
  ('deepseek-v3', 'DeepSeek V3', 'chat', 1, 1, '默认逻辑聊天模型', 'system', 'system');

INSERT INTO `provider_models`
  (`provider_id`, `provider_model_code`, `provider_model_name`, `status`, `remark`, `create_by`, `update_by`)
VALUES
  (1, 'deepseek-chat', 'deepseek-chat', 1, '默认底层聊天模型', 'system', 'system');

INSERT INTO `model_mapping`
  (`customer_model_id`, `provider_model_id`, `priority`, `status`, `remark`, `create_by`, `update_by`)
VALUES
  (1, 1, 1, 1, '默认模型映射', 'system', 'system');

INSERT INTO `providers`
  (`provider_code`, `provider_name`, `provider_type`, `base_url`, `status`, `remark`, `create_by`, `update_by`)
VALUES
  ('right-codex', 'Right Codex', 'openai-compatible', 'https://right.codes/codex/v1', 1, '测试 Provider', 'system', 'system');

INSERT INTO `provider_models`
  (`provider_id`, `provider_model_code`, `provider_model_name`, `status`, `remark`, `create_by`, `update_by`)
VALUES
  (2, 'gpt-5.2', 'gpt-5.2', 1, '测试 Provider 模型', 'system', 'system'),
  (2, 'gpt-5.3', 'gpt-5.3', 1, '测试 Provider 模型', 'system', 'system'),
  (2, 'gpt-5.4', 'gpt-5.4', 1, '测试 Provider 模型', 'system', 'system');

INSERT INTO `provider_tokens`
  (`provider_id`, `token_name`, `token_value`, `token_status`, `quota_total`, `quota_used`, `remark`, `create_by`, `update_by`)
VALUES
  (2, 'right-codex-default', 'sk-16dc1a6df0c04c7c851ec8c326f5f79b', 1, NULL, 0, '测试 Provider Token', 'system', 'system');

INSERT INTO `customer_models`
  (`logic_model_code`, `logic_model_name`, `model_type`, `status`, `is_default`, `remark`, `create_by`, `update_by`)
VALUES
  ('gpt-5.2', 'gpt-5.2', 'chat', 1, 0, '测试逻辑模型', 'system', 'system'),
  ('gpt-5.3', 'gpt-5.3', 'chat', 1, 0, '测试逻辑模型', 'system', 'system'),
  ('gpt-5.4', 'gpt-5.4', 'chat', 1, 0, '测试逻辑模型', 'system', 'system');

INSERT INTO `model_mapping`
  (`customer_model_id`, `provider_model_id`, `priority`, `status`, `remark`, `create_by`, `update_by`)
VALUES
  (2, 2, 1, 1, 'gpt-5.2 一对一映射', 'system', 'system'),
  (3, 3, 1, 1, 'gpt-5.3 一对一映射', 'system', 'system'),
  (4, 4, 1, 1, 'gpt-5.4 一对一映射', 'system', 'system');

INSERT INTO `customer_tokens`
  (`customer_name`, `token_name`, `token_value`, `status`, `allowed_models`, `remark`, `create_by`, `update_by`)
VALUES
  (
    'right-codex-test-customer',
    'right-codex-customer-token',
    'sk-4f7d9c2a1b6e43d8a5c0f1e2b3d4a5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2',
    1,
    JSON_ARRAY('gpt-5.2', 'gpt-5.3', 'gpt-5.4'),
    '测试客户 Token',
    'system',
    'system'
  );
