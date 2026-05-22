# OpenRouter Provider Proxy Runtime

Jira: AUTO-323, AUTO-480

## Current Decision

Use a lightweight private provider proxy for backend AI calls:

- The Spring backend calls an internal OpenAI-compatible provider proxy at `/v1/chat/completions`.
- OpenRouter is the provider integration target.
- Gemma 4 26B A4B IT Free is the default OpenRouter-hosted model because it supports text, image, and video inputs and is free in the OpenRouter catalog.
- The provider proxy holds the OpenRouter credential boundary and enforces model/request guardrails.
- The Spring backend must not receive OpenRouter or other provider API keys as environment variables, config values, prompts, generated context, or logs.
- Firecracker is deferred to a later sandboxing decision for AI-driven code execution, not required for provider credential storage.

## Runtime Shape

```text
Spring backend
  -> internal OpenAI-compatible provider proxy on the private Docker network
    -> OpenRouter API
```

The backend uses these runtime properties:

- `AI_PROVIDER_PROXY_BASE_URL`, for example `http://openrouter-proxy:8080/v1`.
- `AI_PROVIDER_MODEL`, default `google/gemma-4-26b-a4b-it:free`.

The provider proxy injects the OpenRouter authorization header after enforcing local policy. The backend only sees the private proxy URL and model ID.

## Secret Boundary

Allowed to know the OpenRouter API key:

- OCI Vault.
- Private-host deploy/runtime secret resolver.
- Provider proxy process memory.

Not allowed to know the OpenRouter API key:

- Spring backend environment, prompts, or job records.
- AI-visible MCP tools.
- Git repository files.
- Logs, request traces, or generated content records.

## MCP Role

Do not build an MCP tool that returns secret values.

MCP may be useful as a control-plane or diagnostic layer for:

- Checking whether an allowed secret reference exists.
- Checking provider proxy health.
- Checking configured model allowlists.
- Reporting non-secret cost/usage metadata.

MCP must not expose `getSecretValue`, raw API keys, bearer tokens, or provider credentials to the model.

## Docker vs Firecracker

Use Docker for this runtime because:

- The private host target is constrained to about 1 GB RAM.
- Backend, database connectivity, and provider proxy already fit the Docker deployment model.
- The immediate risk is provider credential exposure, which the proxy boundary addresses directly.
- Firecracker adds operational and memory overhead that should be justified by stronger code-execution isolation needs.

Defer Firecracker until the product starts running AI-generated or AI-selected commands in a less trusted workspace. At that point, evaluate Firecracker for the execution sandbox, not for provider credential storage.

## Runtime Validation

The private deploy should run a provider-proxy smoke test after the stack comes up:

- `GET /health` must report `healthy: true` and `apiKeyConfigured: true`.
- `GET /v1/models` must include the configured `AI_PROVIDER_MODEL`.
- `POST /v1/chat/completions` must complete through the configured model.

## Acceptance Notes

- No direct provider API key should be added to the backend service environment.
- Any config checked into git must use non-secret placeholders only.
- Provider model IDs should be allowlisted, with Gemma 4 26B A4B IT Free as the default multimodal model.
- The provider proxy caps completion tokens and request body bytes with `OPENROUTER_MAX_COMPLETION_TOKENS` and `OPENROUTER_MAX_REQUEST_BYTES`.
- Logs must prove which model/provider path was used without recording the API key, bearer token, full prompt payload, or provider response body by default.
