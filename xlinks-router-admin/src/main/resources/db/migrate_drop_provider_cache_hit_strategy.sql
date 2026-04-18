-- Incremental migration for existing databases:
-- Drop providers.cache_hit_strategy because usage extraction strategy is derived from models.model_provider.
-- Compatible with MySQL 5.7.

USE `xlinks_router`;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'providers'
    AND COLUMN_NAME = 'cache_hit_strategy'
);
SET @sql := IF(
  @col_exists > 0,
  'ALTER TABLE `providers` DROP COLUMN `cache_hit_strategy`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

