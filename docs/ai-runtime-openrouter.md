# OpenRouter AI Runtime Decision

Jira: AUTO-323

## Decision

Use a Docker-only private runtime for the first AI integration sprint:

- `opencode` runs in `serve` mode and is controlled through its HTTP REST API.
- OpenRouter is the only provider integration target for this sprint.
- Qwen3.6 Plus is the default OpenRouter-hosted model because it supports text, image, and video inputs.
- A dedicated provider proxy holds the OpenRouter credential boundary.
- `opencode` must not receive OpenRouter or other provider API keys as environment variables, config values, mounted files, prompts, MCP tool results, or generated context.
- Firecracker is deferred to a later sandboxing decision for AI-driven code execution, not required for the first provider integration.

## Runtime Shape

```text
Spring backend
  -> opencode REST server on private Docker network
    -> internal OpenAI-compatible provider proxy
      -> OpenRouter API
```

The backend talks to `opencode` over the private Docker network using `OPENCODE_SERVER_URL`, `OPENCODE_SERVER_USERNAME`, and `OPENCODE_SERVER_PASSWORD`.

The `opencode` runtime talks to the provider proxy as if it were a model provider endpoint. The proxy injects the OpenRouter authorization header after enforcing local policy.

## Secret Boundary

Allowed to know the OpenRouter API key:

- OCI Vault.
- Private-host deploy/runtime secret resolver.
- Provider proxy process memory.

Not allowed to know the OpenRouter API key:

- `opencode` container environment.
- `opencode.json`.
- `opencode` auth store.
- Spring backend prompts or job records.
- AI-visible MCP tools.
- Git repository files.
- Logs, request traces, or OpenCode sessions.

The current private compose still contains direct provider key examples for OpenAI/Gemini. The OpenRouter implementation must remove that pattern from the active AI runtime path instead of adding another direct provider key to `opencode`.

## MCP Role

Do not build an MCP tool that returns secret values.

MCP may be useful as a control-plane or diagnostic layer for:

- Checking whether an allowed secret reference exists.
- Checking provider proxy health.
- Checking configured model allowlists.
- Reporting non-secret cost/usage metadata.

MCP must not expose `getSecretValue`, raw API keys, bearer tokens, or provider credentials to the model.

## Docker vs Firecracker

Use Docker for this sprint because:

- The private host target is constrained to about 1 GB RAM.
- `opencode`, backend, database connectivity, and proxy already fit the Docker deployment model.
- The immediate risk is provider credential exposure, which a proxy boundary addresses more directly than a microVM.
- Firecracker adds operational and memory overhead that should be justified by stronger code-execution isolation needs.

Defer Firecracker until the product starts running AI-generated or AI-selected commands in a less trusted workspace. At that point, evaluate Firecracker for the execution sandbox, not for provider credential storage.

## OpenCode REST Validation

OpenCode documents `opencode serve` as a headless HTTP server. It exposes:

- `GET /global/health` for health/version.
- `GET /doc` for the OpenAPI 3.1 spec.
- `POST /session` to create sessions.
- `POST /session/:id/message` to send a message and wait for response.
- `POST /session/:id/prompt_async` for async submission.
- `GET /session/:id/message` to list messages.
- HTTP Basic Auth via `OPENCODE_SERVER_USERNAME` and `OPENCODE_SERVER_PASSWORD`.

This is enough to proceed with `AUTO-322`, but that story must still smoke-test the exact container image and endpoint shapes before backend integration work depends on them.

## Follow-Up Stories

- `AUTO-324`: implement the OpenRouter provider proxy with model allowlisting and secret isolation.
- `AUTO-322`: run `opencode serve` against the proxy and smoke-test REST session/message flow.
- `AUTO-321`: wire backend approved-job execution to the opencode REST API.
- `AUTO-325`: provision Vault-backed OpenRouter runtime secret references.
- `AUTO-326`: add cost, model, health, and logging guardrails.

## Acceptance Notes

- No new direct provider API key should be added to the `opencode` service environment.
- Any config checked into git must use non-secret placeholders only.
- Provider model IDs should be allowlisted, with Qwen3.6 Plus as the default multimodal model.
- The provider proxy caps completion tokens and request body bytes with `OPENROUTER_MAX_COMPLETION_TOKENS` and `OPENROUTER_MAX_REQUEST_BYTES` so the OpenCode path has a predictable default cost envelope while allowing image-bearing requests through the private proxy.
- Logs must prove which model/provider path was used without recording the API key, bearer token, full prompt payload, or provider response body by default.
- The private deploy should run proxy health/model checks and an `opencode` REST smoke test after the stack comes up, using the documented health/session/message endpoints.
