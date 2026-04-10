-- add model_provider to models
-- Date: 2026-04-10

SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'models'
              AND COLUMN_NAME = 'model_provider'
        ),
        'SELECT 1',
        'ALTER TABLE `models` ADD COLUMN `model_provider` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT ''UNKNOWN'' COMMENT ''Display vendor label, e.g. OPENAI / ANTHROPIC'' AFTER `model_code`'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `models`
SET `model_provider` = CASE
    WHEN LOWER(`model_code`) LIKE 'gpt%' OR LOWER(`model_code`) LIKE 'o1%' OR LOWER(`model_code`) LIKE 'o3%' THEN 'OPENAI'
    WHEN LOWER(`model_code`) LIKE 'claude%' THEN 'ANTHROPIC'
    WHEN LOWER(`model_code`) LIKE 'gemini%' THEN 'GOOGLE'
    WHEN LOWER(`model_code`) LIKE 'deepseek%' THEN 'DEEPSEEK'
    WHEN LOWER(`model_code`) LIKE 'mistral%' THEN 'MISTRAL'
    WHEN LOWER(`model_code`) LIKE 'llama%' THEN 'META'
    WHEN LOWER(`model_code`) LIKE 'qwen%' THEN 'QWEN'
    WHEN LOWER(`model_code`) LIKE 'grok%' THEN 'XAI'
    ELSE COALESCE(NULLIF(`model_provider`, ''), 'UNKNOWN')
END
WHERE `model_provider` IS NULL OR `model_provider` = '' OR `model_provider` = 'UNKNOWN';

SET @idx_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'models'
              AND INDEX_NAME = 'idx_model_provider'
        ),
        'SELECT 1',
        'ALTER TABLE `models` ADD INDEX `idx_model_provider` (`model_provider`)'
    )
);
PREPARE idx_stmt FROM @idx_sql;
EXECUTE idx_stmt;
DEALLOCATE PREPARE idx_stmt;

