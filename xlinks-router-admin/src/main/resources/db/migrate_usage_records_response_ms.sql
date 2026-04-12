-- add/rename response_ms in usage_records
-- Date: 2026-04-11

SET @schema_name = DATABASE();

SET @has_response_ms = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'usage_records'
      AND COLUMN_NAME = 'response_ms'
);

SET @has_first_response_ms = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'usage_records'
      AND COLUMN_NAME = 'first_response_ms'
);

SET @sql = (
    SELECT CASE
        WHEN @has_response_ms > 0 THEN 'SELECT 1'
        WHEN @has_first_response_ms > 0 THEN
            'ALTER TABLE `usage_records` CHANGE COLUMN `first_response_ms` `response_ms` int(11) DEFAULT NULL COMMENT ''Time to first response data in milliseconds'''
        ELSE
            'ALTER TABLE `usage_records` ADD COLUMN `response_ms` int(11) DEFAULT NULL COMMENT ''Time to first response data in milliseconds'' AFTER `total_cost`'
    END
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_session_ms = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'usage_records'
      AND COLUMN_NAME = 'session_ms'
);

SET @has_latency_ms = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'usage_records'
      AND COLUMN_NAME = 'latency_ms'
);

-- Backfill existing rows using available duration column as approximation.
SET @sql_backfill = (
    SELECT CASE
        WHEN @has_session_ms > 0 THEN
            'UPDATE `usage_records` SET `response_ms` = COALESCE(`response_ms`, `session_ms`, 0) WHERE `response_ms` IS NULL'
        WHEN @has_latency_ms > 0 THEN
            'UPDATE `usage_records` SET `response_ms` = COALESCE(`response_ms`, `latency_ms`, 0) WHERE `response_ms` IS NULL'
        ELSE
            'SELECT 1'
    END
);
PREPARE stmt_backfill FROM @sql_backfill;
EXECUTE stmt_backfill;
DEALLOCATE PREPARE stmt_backfill;
