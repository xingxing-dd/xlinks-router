ALTER TABLE `providers`
  ADD COLUMN `concurrency_limit_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用 provider token 并发限制' AFTER `remark`,
  ADD COLUMN `max_concurrent_per_token` INT NOT NULL DEFAULT 0 COMMENT '每个 provider token 最大并发会话数' AFTER `concurrency_limit_enabled`,
  ADD COLUMN `acquire_timeout_ms` INT NOT NULL DEFAULT 0 COMMENT '获取 permit 等待时间' AFTER `max_concurrent_per_token`,
  ADD COLUMN `request_timeout_ms` INT NOT NULL DEFAULT 20000 COMMENT '非流式请求超时' AFTER `acquire_timeout_ms`,
  ADD COLUMN `stream_first_response_timeout_ms` INT NOT NULL DEFAULT 20000 COMMENT '流式首包超时' AFTER `request_timeout_ms`,
  ADD COLUMN `stream_idle_timeout_ms` INT NOT NULL DEFAULT 20000 COMMENT '流式空闲超时' AFTER `stream_first_response_timeout_ms`,
  ADD COLUMN `session_lease_ms` INT NOT NULL DEFAULT 30000 COMMENT 'permit 租约时长' AFTER `stream_idle_timeout_ms`,
  ADD COLUMN `session_renew_interval_ms` INT NOT NULL DEFAULT 10000 COMMENT 'permit 续租周期' AFTER `session_lease_ms`;

ALTER TABLE `usage_records`
  ADD COLUMN `provider_token_id` BIGINT DEFAULT NULL COMMENT 'Provider Token ID' AFTER `provider_token`,
  ADD COLUMN `finish_reason` VARCHAR(64) DEFAULT NULL COMMENT '结束原因' AFTER `error_message`;
