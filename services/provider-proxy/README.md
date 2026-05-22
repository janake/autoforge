# provider-proxy

Small OpenAI-compatible proxy used to keep the OpenRouter API key outside the `opencode` runtime.

The proxy accepts `POST /v1/chat/completions` from the private Docker network, enforces allowlisted models plus request limits, and forwards the request to OpenRouter with the real `OPENROUTER_API_KEY` injected inside this process only. The default model supports text, image, and video inputs.

Required runtime env:

- `OPENROUTER_API_KEY`: real OpenRouter key, resolved from Vault on the private host.

Optional runtime env:

- `PROVIDER_PROXY_PORT`: listen port, default `8080`.
- `OPENROUTER_BASE_URL`: upstream base URL, default `https://openrouter.ai/api/v1`.
- `OPENROUTER_DEFAULT_MODEL`: default model when the client omits one, default `qwen/qwen3.6-plus`.
- `OPENROUTER_ALLOWED_MODELS`: comma-separated allowlist, default to the default model.
- `OPENROUTER_MAX_COMPLETION_TOKENS`: maximum `max_tokens` accepted and the default applied when omitted, default `65536`.
- `OPENROUTER_MAX_REQUEST_BYTES`: maximum JSON request body size, default `10485760`.
- `OPENROUTER_SITE_URL`: optional OpenRouter attribution header.
- `OPENROUTER_APP_NAME`: optional OpenRouter attribution header, default `Autoforge`.

Health and logs:

- `GET /health` returns non-secret provider, model allowlist, API-key configured state, and request limit metadata.
- Request logs include provider, model, status, and duration only. They must not include prompts, bearer tokens, API keys, or response bodies.

Do not expose this service outside the private Docker network.
