ALTER TABLE `customer_tokens`
    MODIFY COLUMN `daily_quota` decimal(12,6) DEFAULT NULL,
    MODIFY COLUMN `used_quota` decimal(12,6) DEFAULT NULL,
    MODIFY COLUMN `total_quota` decimal(12,6) DEFAULT NULL,
    MODIFY COLUMN `total_used_quota` decimal(12,6) DEFAULT NULL;

ALTER TABLE `plans`
    MODIFY COLUMN `price` decimal(12,6) NOT NULL,
    MODIFY COLUMN `daily_quota` decimal(12,6) NOT NULL,
    MODIFY COLUMN `total_quota` decimal(12,6) NOT NULL;

ALTER TABLE `customer_plans`
    MODIFY COLUMN `price` decimal(12,6) NOT NULL,
    MODIFY COLUMN `daily_quota` decimal(12,6) NOT NULL,
    MODIFY COLUMN `total_quota` decimal(12,6) NOT NULL,
    MODIFY COLUMN `used_quota` decimal(12,6) NOT NULL DEFAULT '0.000000',
    MODIFY COLUMN `total_used_quota` decimal(12,6) NOT NULL DEFAULT '0.000000';
