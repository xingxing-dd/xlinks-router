# xlinks-router Technical Design

## 1. Product Positioning

xlinks-router is a multi-provider, multi-model aggregation router.

It exposes a unified OpenAI-style API externally and manages these routing resources internally:

- Providers
- Model endpoints
- Standard models
- Provider model mappings
- Provider tokens and customer tokens

The platform goal is not to expose one provider directly. It is to expose **standard models** and route them to the best available upstream provider.

## 2. Main Modules

- `xlinks-router-admin`: admin APIs
- `xlinks-router-api`: public OpenAPI gateway
- `xlinks-router-client`: console / client APIs
- `xlinks-router-common`: shared entities, mappers, base components

## 3. Problems in the Old Model Design

If `models` is directly bound to `providers`, the design has several issues:

1. A standard model can only belong to one provider.
2. The router cannot express multi-provider failover or priority routing.
3. Standard model codes and upstream model codes are mixed together.
4. Protocol support belongs to providers, not to standard models.

## 4. Revised Core Model Design

### 4.1 Relationships

```text
providers
   |
   +-- provider_tokens
   |
   +-- provider_models ---- models ---- model_endpoints
```

### 4.2 Resource Responsibilities

#### providers
Upstream platform definition.

Key fields:
- `provider_code`
- `provider_type`
- `supported_protocols`
- `priority`
- `base_url`

Notes:
- `supported_protocols`: comma-separated protocol list; empty means all protocols
- `priority`: larger value means higher routing priority

#### model_endpoints
Standard protocol capability definition.

Key fields:
- `endpoint_code` such as `chat/completions` and `responses`
- `endpoint_name`
- `endpoint_url`

#### models
Platform-facing standard model definition.

Design rules:
- `models` is related only to `model_endpoints`
- `models` is **not** directly related to `providers`
- request field `model` maps to `models.model_code`

Current platform-level fields kept on `models`:
- `input_price`
- `output_price`
- `context_size`

These fields currently mean platform-level configuration. If provider-specific price or context overrides are needed later, those differences can be moved or extended on `provider_models`.

#### provider_models
Mapping table from a standard model to an upstream provider model.

This table answers:
- which provider can serve a standard model
- what upstream model code should be sent
- which providers are candidates for the same standard model

## 5. Routing Flow

For a `POST /v1/chat/completions` request:

1. Derive `endpoint_code = chat/completions` from the request protocol
2. Resolve the standard model by `endpoint_id + model_code`
3. Load candidate `provider_models` by `model_id`
4. Filter out disabled mappings, disabled providers, and providers that do not support the current protocol
5. Sort by `providers.priority DESC`
6. Pick the highest-priority provider
7. Rewrite the outbound `model` field to `provider_model_code`
8. Select an available provider token
9. Invoke the adapter based on `provider_type`

## 6. Key Constraints

Recommended uniqueness rules:

- `providers.provider_code` unique
- `model_endpoints.endpoint_code` unique
- `model_endpoints.endpoint_url` unique
- `models(endpoint_id, model_code)` unique
- `provider_models(provider_id, model_id)` unique
- `provider_models(provider_id, provider_model_code)` unique

Core routing tables using logical delete:

- `providers`
- `model_endpoints`
- `models`
- `provider_models`

## 7. Public API Convention

The gateway exposes standard paths directly:

- `POST /v1/chat/completions`
- `POST /v1/responses`
- `GET /v1/models`

Routing no longer depends on an extra path segment. The endpoint is inferred from the request protocol itself.

## 8. Document Index

- `docs/tech-design.md`: overall architecture and model design
- `docs/admin-api.md`: admin APIs for routing resources
- `docs/openapi.md`: public OpenAPI gateway behavior
- `docs/db-script.md`: core schema notes
- `xlinks-router-admin/src/main/resources/db/init.sql`: bootstrap SQL
