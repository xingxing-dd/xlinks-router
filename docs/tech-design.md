# xlinks-router Technical Design

## 1. Product Positioning

xlinks-router is a multi-provider, multi-model aggregation router.

It exposes a unified OpenAI-style API externally and manages these routing resources internally:

- Providers
- Standard models
- Provider model mappings
- Provider tokens and customer tokens

The platform exposes **standard models** to customers and routes requests to the best available upstream provider.

## 2. Main Modules

- `xlinks-router-admin`: admin APIs
- `xlinks-router-api`: public OpenAPI gateway
- `xlinks-router-client`: console/client APIs
- `xlinks-router-common`: shared entities, mappers, base components

## 3. Core Data Model

```text
providers
   |
   +-- provider_tokens
   |
   +-- provider_models ---- models
```

### providers

Upstream platform definition.

Key fields:

- `provider_code`
- `supported_protocols`
- `priority`
- `cache_hit_strategy`
- `base_url`

Notes:

- `supported_protocols`: comma-separated protocol list; empty means all protocols
- `priority`: larger value means higher routing priority
- `cache_hit_strategy`: provider-specific usage parsing strategy (`none`, `openai_cached_tokens`, `anthropic_cache_read_input_tokens`)

### models

Platform-facing standard model definition.

Design rules:

- `models` has no endpoint dimension
- `models` is **not** directly related to `providers`
- request field `model` maps to `models.model_code`

Current platform-level fields:

- `input_price`
- `output_price`
- `cache_hit_price`
- `context_size`

Cost formula:

```text
(input_tokens - cache_hit_tokens) * input_price
+ cache_hit_tokens * cache_hit_price
+ output_tokens * output_price
```

### provider_models

Mapping from standard model to upstream provider model.

This table answers:

- which provider can serve a standard model
- what upstream model code should be sent
- which providers are candidates for the same standard model

## 4. Routing Flow

For `POST /v1/chat/completions` (the same pattern applies to `responses`):

1. Resolve standard model by `model_code`.
2. Load candidate `provider_models` by `model_id`.
3. Filter by mapping status, provider status, and `supported_protocols`.
4. Sort by `providers.priority DESC`.
5. Try candidates in order and select the first provider with an available provider token.
6. Rewrite outbound `model` to `provider_model_code`.
7. Invoke adapter by request protocol.

## 5. Key Constraints

Recommended uniqueness rules:

- `providers.provider_code` unique
- `models(model_code)` unique
- `provider_models(provider_id, model_id)` unique

Routing-related tables use logical delete:

- `providers`
- `models`
- `provider_models`

## 6. Public API Convention

Gateway paths:

- `POST /v1/chat/completions`
- `POST /v1/responses`
- `GET /v1/models`

The endpoint is inferred from the request protocol and filtered by provider capability.
The endpoint is inferred from the request protocol and filtered by provider capability.

## 7. Document Index

- `docs/tech-design.md`: architecture and model design
- `docs/admin-api.md`: admin APIs for routing resources
- `docs/openapi.md`: public gateway behavior
- `docs/db-script.md`: core schema notes
- `xlinks-router-admin/src/main/resources/db/init.sql`: bootstrap SQL
- `docs/client-email.md`: client mail verification delivery and Mailtrap configuration

## 8. Payment Order Model

The payment module uses `customer_orders` as the unified order domain table.

Core fields:

- `order_no`: platform order id
- `ref_no`: payment channel reference id (for example Alipay trade_no)
- `account_id`: customer account id
- `order_type`: business order type (`recharge` / `withdraw` / `subscription_purchase` ...)
- `order_title`: order title
- `order_info`: JSON snapshot of business context at order creation
- `payment_channel`: channel code (`alipay` / `wechat` / `third-party` ...)
- `total_amount`: order amount
- `status`: `0 pending`, `1 paid`, `2 failed`, `3 closed`, `4 refunded`
- `complete_at`: completed timestamp for paid/refunded lifecycle states
- `expired_at`: payment expiration time, defaults to 30 minutes after order creation
- `remark`: extended notes
- `created_at`: audit create timestamp (common base field)
- `updated_at`: audit update timestamp (common base field)
- `create_by`: audit creator (common base field)
- `update_by`: audit updater (common base field)

Lifecycle rule:

- pending orders (`status = 0`) are automatically closed (`status = 3`) after `expired_at`.
- paid callback will trigger fulfillment by `order_type`; `subscription_purchase` creates `customer_plans` from `order_info` snapshot.

