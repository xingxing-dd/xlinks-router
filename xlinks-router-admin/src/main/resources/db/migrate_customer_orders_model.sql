-- customer_orders model migration
-- Date: 2026-04-08

-- 0) rename table
RENAME TABLE `third_party_pay_orders` TO `customer_orders`;

-- 1) ensure common audit fields exist (compatible with older source schemas)
SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'customer_orders'
              AND COLUMN_NAME = 'created_at'
        ),
        'SELECT 1',
        'ALTER TABLE `customer_orders` ADD COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'customer_orders'
              AND COLUMN_NAME = 'updated_at'
        ),
        'SELECT 1',
        'ALTER TABLE `customer_orders` ADD COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'customer_orders'
              AND COLUMN_NAME = 'create_by'
        ),
        'SELECT 1',
        'ALTER TABLE `customer_orders` ADD COLUMN `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'customer_orders'
              AND COLUMN_NAME = 'update_by'
        ),
        'SELECT 1',
        'ALTER TABLE `customer_orders` ADD COLUMN `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'customer_orders'
              AND COLUMN_NAME = 'expired_at'
        ),
        'SELECT 1',
        'ALTER TABLE `customer_orders` ADD COLUMN `expired_at` datetime DEFAULT NULL COMMENT ''支付过期时间'''
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) add/rename new fields
ALTER TABLE `customer_orders`
    CHANGE COLUMN `third_party_order_no` `ref_no` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '支付渠道关联订单号',
    ADD COLUMN `order_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'subscription_purchase' AFTER `account_id`,
    ADD COLUMN `order_info` json DEFAULT NULL AFTER `order_title`,
    ADD COLUMN `payment_channel` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'third-party' AFTER `order_info`,
    ADD COLUMN `complete_at` datetime DEFAULT NULL AFTER `status`;

-- 3) migrate old values
UPDATE `customer_orders`
SET `order_type` = CASE
                       WHEN `target_type` = 'plan' THEN 'subscription_purchase'
                       ELSE COALESCE(NULLIF(`target_type`, ''), 'subscription_purchase')
    END;

UPDATE `customer_orders`
SET `payment_channel` = COALESCE(NULLIF(`payment_method_code`, ''), 'third-party');

UPDATE `customer_orders`
SET `complete_at` = `pay_time`
WHERE `complete_at` IS NULL;

UPDATE `customer_orders`
SET `expired_at` = DATE_ADD(`created_at`, INTERVAL 30 MINUTE)
WHERE `status` = 0
  AND `expired_at` IS NULL;

-- 4) drop old columns
ALTER TABLE `customer_orders`
    DROP COLUMN `target_id`,
    DROP COLUMN `target_type`,
    DROP COLUMN `payment_method_code`,
    DROP COLUMN `payment_method_type`,
    DROP COLUMN `trade_status`,
    DROP COLUMN `pay_url`,
    DROP COLUMN `pay_time`;

-- 5) rebuild indexes
ALTER TABLE `customer_orders`
    DROP INDEX `uk_third_party_order_no`,
    DROP INDEX `idx_trade_status`,
    DROP INDEX `idx_pay_time`,
    ADD UNIQUE KEY `uk_ref_no` (`ref_no`),
    ADD KEY `idx_order_type` (`order_type`),
    ADD KEY `idx_payment_channel` (`payment_channel`),
    ADD KEY `idx_complete_at` (`complete_at`),
    ADD KEY `idx_expired_at` (`expired_at`);
