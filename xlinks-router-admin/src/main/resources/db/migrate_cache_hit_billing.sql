-- Cache-hit billing migration
-- Date: 2026-04-07

-- 1) provider strategy
ALTER TABLE `providers`
    ADD COLUMN `cache_hit_strategy` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'none' AFTER `priority`;

-- 2) model price
ALTER TABLE `models`
    ADD COLUMN `cache_hit_price` decimal(12,2) DEFAULT NULL AFTER `output_price`;

-- Keep backward compatibility for existing rows when cache-hit price is not configured yet.
UPDATE `models`
SET `cache_hit_price` = `input_price`
WHERE `cache_hit_price` IS NULL;

-- 3) usage record fields
ALTER TABLE `usage_records`
    ADD COLUMN `cache_hit_tokens` int(11) DEFAULT '0' AFTER `total_tokens`,
    ADD COLUMN `cache_hit_cost` decimal(12,6) DEFAULT '0.000000' AFTER `prompt_cost`;
