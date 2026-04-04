# xlinks-router OpenAPI

This document describes the public gateway behavior and protocol-based provider selection.

## 1. Public Endpoints

The gateway exposes:

- `POST /v1/chat/completions`
- `POST /v1/responses`
- `GET /v1/models`

Authentication:

- `Authorization: Bearer {customer_token}`

## 2. Standard Model Rules

- request field `model` maps to `models.model_code`
- standard models do not directly bind to providers
- the real upstream model code comes from `provider_models.provider_model_code`

## 3. Chat Completions

### 3.1 Request

- `POST /v1/chat/completions`

```json
{
  "model": "gpt-5.4",
  "messages": [
    { "role": "system", "content": "You are a helpful assistant." },
    { "role": "user", "content": "Hello" }
  ],
  "temperature": 0.7,
  "stream": false
}
```

### 3.2 Routing Logic

For `chat/completions`:

1. resolve the standard model by `model_code`
2. load candidate `provider_models`
3. keep only providers that support `chat/completions`
4. sort by `providers.priority DESC`
5. choose the highest-priority available provider
6. rewrite request field `model` to `provider_model_code`
7. invoke the selected upstream provider

## 4. Responses

### 4.1 Request

- `POST /v1/responses`

```json
{
  "model": "gpt-5.4",
  "input": "Summarize this content"
}
```

### 4.2 Routing Logic

For `responses`:

1. resolve the standard model by `model_code`
2. follow the same provider-mapping flow
3. only providers supporting `responses` are eligible

## 5. Models List

- `GET /v1/models`

The response contains enabled standard models that the current customer token can access.

Example:

```json
{
  "object": "list",
  "data": [
    {
      "id": "gpt-5.4",
      "object": "model",
      "created": 1710000000,
      "owned_by": "xlinks-router"
    }
  ]
}
```

## 6. Provider Selection Fields

Provider selection depends on:

- `supported_protocols`: protocol filtering
- `priority`: candidate ordering, larger value first
- request protocol: adapter selection

Examples for `supported_protocols`:

- `chat/completions,responses`
- `chat/completions`
- empty value: no protocol restriction

## 7. Upstream Model Rewriting

Inbound request:

```json
{ "model": "gpt-5.4" }
```

If the selected mapping is:

- `provider_id = 2`
- `provider_model_code = gpt-5.4-mini-router`

Then the outbound request body becomes:

```json
{ "model": "gpt-5.4-mini-router" }
```

This separation is required for an aggregation router, because the platform-facing standard model name is not necessarily the same as the upstream provider model name.

## 8. Common Errors

- standard model not found: `400`
- standard model disabled: `400`
- no provider available for the current protocol: route error
- no provider token available: route error
- customer token invalid or expired: `401`
