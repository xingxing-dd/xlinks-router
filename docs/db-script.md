# xlinks-router Schema Notes

This document focuses on the routing-related schema only. The full bootstrap script is:

- `xlinks-router-admin/src/main/resources/db/init.sql`

## 1. Design Goal

The schema serves a multi-provider aggregation router:

- `models` represents platform-facing standard models
- `providers` represents upstream platforms
- `provider_models` maps standard models to upstream models
- routing is based on protocol support and provider priority

## 2. Core Relationships

```text
model_endpoints 1 ---- n models
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
  `provider_type` VARCHAR(20) NOT NULL DEFAULT 'openai-compatible',
  `supported_protocols` VARCHAR(255) DEFAULT NULL,
  `priority` INT NOT NULL DEFAULT 0,
  `base_url` VARCHAR(255) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_code` (`provider_code`)
);
```

## 4. model_endpoints

```sql
CREATE TABLE `model_endpoints` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `endpoint_code` VARCHAR(100) NOT NULL,
  `endpoint_name` VARCHAR(100) NOT NULL,
  `endpoint_url` VARCHAR(255) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_endpoint_code` (`endpoint_code`),
  UNIQUE KEY `uk_endpoint_url` (`endpoint_url`)
);
```

## 5. models

`models` no longer stores `provider_id`.

```sql
CREATE TABLE `models` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `model_name` VARCHAR(100) NOT NULL,
  `model_code` VARCHAR(100) NOT NULL,
  `endpoint_id` BIGINT NOT NULL,
  `model_desc` VARCHAR(500) DEFAULT NULL,
  `input_price` DECIMAL(12, 2) DEFAULT NULL,
  `output_price` DECIMAL(12, 2) DEFAULT NULL,
  `context_size` INT DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_endpoint_model_code` (`endpoint_id`, `model_code`)
);
```

## 6. provider_models

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
  UNIQUE KEY `uk_provider_model` (`provider_id`, `model_id`),
  UNIQUE KEY `uk_provider_model_code` (`provider_id`, `provider_model_code`)
);
```

## 7. Main Changes from the Old Design

Old design issues:

- `models.provider_id` coupled standard models with upstream providers
- one standard model could not map to multiple providers
- protocol filtering and priority routing were not explicit
- standard model codes and upstream model codes were mixed

Current design changes:

- remove `models.provider_id`
- add `provider_models`
- add `providers.supported_protocols`
- add `providers.priority`
- use `model_endpoints.endpoint_code` as the protocol capability key

## 8. Routing Query Order

```sql
-- 1) resolve endpoint by protocol
SELECT * FROM model_endpoints WHERE endpoint_code = 'chat/completions' AND status = 1;

-- 2) resolve standard model
SELECT * FROM models WHERE endpoint_id = ? AND model_code = ? AND status = 1;

-- 3) load provider mappings
SELECT * FROM provider_models WHERE model_id = ? AND status = 1;

-- 4) filter by provider protocol support and sort by priority
```
