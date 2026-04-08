# xlinks-router Schema Notes

This document focuses on routing-related schema only. The full bootstrap script is:

- `xlinks-router-admin/src/main/resources/db/init.sql`

## 1. Design Goal

The schema serves a multi-provider aggregation router:

- `models` represents platform-facing standard models
- `providers` represents upstream platforms
- `provider_models` maps standard models to upstream models
- routing is based on protocol support and provider priority

## 2. Core Relationships

```text
models          1 ---- n provider_models
providers       1 ---- n provider_models
providers       1 ---- n provider_tokens
```

## 3. providers

```sql
CREATE TABLE `providers` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `provider_code` VARCHAR(50) NOT NULL,
  `provider_name` VARCHAR(100) NOT NULL,
  `supported_protocols` VARCHAR(255) DEFAULT NULL,
  `priority` INT NOT NULL DEFAULT 0,
  `cache_hit_strategy` VARCHAR(64) NOT NULL DEFAULT 'none',
  `base_url` VARCHAR(255) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_code` (`provider_code`)
);
```

## 4. models

`models` does not store `provider_id` or endpoint dimensions. Standard model identity is `model_code`.

```sql
CREATE TABLE `models` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `model_name` VARCHAR(100) NOT NULL,
  `model_code` VARCHAR(100) NOT NULL,
  `model_desc` VARCHAR(500) DEFAULT NULL,
  `input_price` DECIMAL(12, 2) DEFAULT NULL,
  `output_price` DECIMAL(12, 2) DEFAULT NULL,
  `cache_hit_price` DECIMAL(12, 2) DEFAULT NULL,
  `context_size` INT DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_code` (`model_code`)
);
```

Pricing formula (per request):

```text
(input_tokens - cache_hit_tokens) * input_price
+ cache_hit_tokens * cache_hit_price
+ output_tokens * output_price
```

## 5. provider_models

```sql
CREATE TABLE `provider_models` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `provider_id` BIGINT NOT NULL,
  `model_id` BIGINT NOT NULL,
  `provider_model_code` VARCHAR(100) NOT NULL,
  `provider_model_name` VARCHAR(100) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_model` (`provider_id`, `model_id`)
);
```

## 6. usage_records (cost fields)

```sql
CREATE TABLE `usage_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `request_id` VARCHAR(64) NOT NULL,
  `provider_id` BIGINT NOT NULL,
  `model_id` BIGINT NOT NULL,
  `prompt_tokens` INT DEFAULT 0,
  `completion_tokens` INT DEFAULT 0,
  `total_tokens` INT DEFAULT 0,
  `cache_hit_tokens` INT DEFAULT 0,
  `prompt_cost` DECIMAL(12, 6) DEFAULT 0.000000,
  `cache_hit_cost` DECIMAL(12, 6) DEFAULT 0.000000,
  `completion_cost` DECIMAL(12, 6) DEFAULT 0.000000,
  `total_cost` DECIMAL(12, 6) DEFAULT 0.000000,
  PRIMARY KEY (`id`)
);
```

## 7. Main Changes

- remove `models.provider_id`
- keep `provider_models` for multi-provider mapping
- keep `providers.supported_protocols` + `providers.priority` for filtering and sorting
- add `providers.cache_hit_strategy` to support provider-specific cache-hit extraction
- add `models.cache_hit_price` for cache-hit token billing
- remove `model_endpoints` table and remove endpoint dimension from models

## 8. Routing Query Order

```sql
-- 1) resolve standard model by model code
SELECT * FROM models
WHERE model_code = ? AND status = 1;

-- 2) load provider mappings
SELECT * FROM provider_models WHERE model_id = ? AND status = 1;

-- 3) filter providers by supported_protocols and sort by priority

-- 4) select available provider_token under selected provider
```

## 9. customer_orders (order domain)

Order table stores unified business orders (recharge/withdraw/subscription purchase) and channel references.

```sql
CREATE TABLE `customer_orders` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(64) NOT NULL,
  `ref_no` VARCHAR(128) DEFAULT NULL,
  `account_id` BIGINT DEFAULT NULL,
  `order_type` VARCHAR(50) NOT NULL,
  `order_title` VARCHAR(200) NOT NULL,
  `order_info` JSON DEFAULT NULL,
  `payment_channel` VARCHAR(50) NOT NULL,
  `total_amount` DECIMAL(12, 2) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 0,
  `complete_at` DATETIME DEFAULT NULL,
  `expired_at` DATETIME DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_ref_no` (`ref_no`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_order_type` (`order_type`),
  KEY `idx_payment_channel` (`payment_channel`),
  KEY `idx_status` (`status`),
  KEY `idx_complete_at` (`complete_at`),
  KEY `idx_expired_at` (`expired_at`)
);
```

Status definition:

- `0`: pending
- `1`: paid
- `2`: failed
- `3`: closed
- `4`: refunded

Expire rule:

- `expired_at` defaults to 30 minutes after order creation in application logic.
- if `status = 0` and current time is greater than `expired_at`, order will be auto-closed (`status = 3`).

