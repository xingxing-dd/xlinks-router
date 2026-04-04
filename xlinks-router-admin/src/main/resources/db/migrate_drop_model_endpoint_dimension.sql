-- Incremental migration for existing databases:
-- Remove model endpoint dimension from models table.
-- Final routing key: models.model_code
-- Compatible with MySQL 5.7.

USE `xlinks_router`;

-- 1) Drop old unique/index that include endpoint columns when present
SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'models'
    AND INDEX_NAME = 'uk_endpoint_model_code'
);
SET @sql := IF(
  @idx_exists > 0,
  'ALTER TABLE `models` DROP INDEX `uk_endpoint_model_code`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'models'
    AND INDEX_NAME = 'idx_endpoint_code'
);
SET @sql := IF(
  @idx_exists > 0,
  'ALTER TABLE `models` DROP INDEX `idx_endpoint_code`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'models'
    AND INDEX_NAME = 'idx_endpoint_id'
);
SET @sql := IF(
  @idx_exists > 0,
  'ALTER TABLE `models` DROP INDEX `idx_endpoint_id`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) Ensure model_code unique
SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'models'
    AND INDEX_NAME = 'uk_model_code'
);
SET @sql := IF(
  @idx_exists = 0,
  'ALTER TABLE `models` ADD UNIQUE KEY `uk_model_code` (`model_code`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) Drop endpoint_code from models when present
SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'models'
    AND COLUMN_NAME = 'endpoint_code'
);
SET @sql := IF(
  @col_exists > 0,
  'ALTER TABLE `models` DROP COLUMN `endpoint_code`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) Drop endpoint_id from models when present
SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'models'
    AND COLUMN_NAME = 'endpoint_id'
);
SET @sql := IF(
  @col_exists > 0,
  'ALTER TABLE `models` DROP COLUMN `endpoint_id`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Optional cleanup:
-- DROP TABLE `model_endpoints`;
