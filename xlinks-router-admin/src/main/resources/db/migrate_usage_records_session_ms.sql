-- rename latency_ms to session_ms in usage_records
-- Date: 2026-04-12

SET @schema_name = DATABASE();

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

SET @sql = (
    SELECT CASE
        WHEN @has_session_ms > 0 THEN 'SELECT 1'
        WHEN @has_latency_ms > 0 THEN
            'ALTER TABLE `usage_records` CHANGE COLUMN `latency_ms` `session_ms` int(11) DEFAULT ''0'' COMMENT ''Session duration in milliseconds'''
        ELSE
            'ALTER TABLE `usage_records` ADD COLUMN `session_ms` int(11) DEFAULT ''0'' COMMENT ''Session duration in milliseconds'' AFTER `response_ms`'
    END
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- If legacy column still exists (for any custom schema drift), backfill then keep only session_ms.
SET @has_session_ms_after = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'usage_records'
      AND COLUMN_NAME = 'session_ms'
);

SET @has_latency_ms_after = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'usage_records'
      AND COLUMN_NAME = 'latency_ms'
);

SET @sql_backfill = (
    SELECT CASE
        WHEN @has_session_ms_after > 0 AND @has_latency_ms_after > 0 THEN
            'UPDATE `usage_records` SET `session_ms` = COALESCE(`session_ms`, `latency_ms`, 0)'
        ELSE 'SELECT 1'
    END
);
PREPARE stmt_backfill FROM @sql_backfill;
EXECUTE stmt_backfill;
DEALLOCATE PREPARE stmt_backfill;

SET @sql_drop_legacy = (
    SELECT CASE
        WHEN @has_session_ms_after > 0 AND @has_latency_ms_after > 0 THEN
            'ALTER TABLE `usage_records` DROP COLUMN `latency_ms`'
        ELSE 'SELECT 1'
    END
);
PREPARE stmt_drop_legacy FROM @sql_drop_legacy;
EXECUTE stmt_drop_legacy;
DEALLOCATE PREPARE stmt_drop_legacy;
