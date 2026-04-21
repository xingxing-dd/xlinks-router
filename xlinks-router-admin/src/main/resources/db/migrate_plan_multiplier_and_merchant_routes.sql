SET @schema_name = DATABASE();

SELECT COUNT(1)
INTO @plan_multiplier_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @schema_name
  AND TABLE_NAME = 'plans'
  AND COLUMN_NAME = 'multiplier';

SET @plan_multiplier_sql = IF(
  @plan_multiplier_exists = 0,
  'ALTER TABLE `plans` ADD COLUMN `multiplier` decimal(10,4) NOT NULL DEFAULT ''1.0000'' COMMENT ''Cache-hit billing multiplier'' AFTER `total_quota`',
  'SELECT 1'
);
PREPARE stmt FROM @plan_multiplier_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(1)
INTO @customer_plan_multiplier_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @schema_name
  AND TABLE_NAME = 'customer_plans'
  AND COLUMN_NAME = 'multiplier';

SET @customer_plan_multiplier_sql = IF(
  @customer_plan_multiplier_exists = 0,
  'ALTER TABLE `customer_plans` ADD COLUMN `multiplier` decimal(10,4) NOT NULL DEFAULT ''1.0000'' COMMENT ''Cache-hit billing multiplier snapshot'' AFTER `total_quota`',
  'SELECT 1'
);
PREPARE stmt FROM @customer_plan_multiplier_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `plans`
SET `multiplier` = '1.0000'
WHERE `multiplier` IS NULL OR `multiplier` <= 0;

UPDATE `customer_plans`
SET `multiplier` = '1.0000'
WHERE `multiplier` IS NULL OR `multiplier` <= 0;

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
