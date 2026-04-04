-- Incremental migration for existing databases:
-- Remove providers.provider_type and use supported_protocols for routing capability.
-- Compatible with MySQL 5.7.

USE `xlinks_router`;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'providers'
    AND COLUMN_NAME = 'provider_type'
);
SET @sql := IF(
  @col_exists > 0,
  'ALTER TABLE `providers` DROP COLUMN `provider_type`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
