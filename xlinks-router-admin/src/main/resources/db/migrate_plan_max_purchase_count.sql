-- add max_purchase_count to plans
-- Date: 2026-04-10

SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'plans'
              AND COLUMN_NAME = 'max_purchase_count'
        ),
        'SELECT 1',
        'ALTER TABLE `plans` ADD COLUMN `max_purchase_count` int(11) DEFAULT NULL COMMENT ''Maximum purchase count per account, NULL means unlimited'' AFTER `total_quota`'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'customer_plans'
              AND INDEX_NAME = 'idx_account_plan_source'
        ),
        'SELECT 1',
        'ALTER TABLE `customer_plans` ADD INDEX `idx_account_plan_source` (`account_id`,`plan_id`,`source`)'
    )
);
PREPARE idx_stmt FROM @idx_sql;
EXECUTE idx_stmt;
DEALLOCATE PREPARE idx_stmt;
