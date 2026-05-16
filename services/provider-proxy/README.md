# provider-proxy

Small OpenAI-compatible proxy used to keep the OpenRouter API key outside the `opencode` runtime.

The proxy accepts `POST /v1/chat/completions` from the private Docker network, enforces an allowlisted model, and forwards the request to OpenRouter with the real `OPENROUTER_API_KEY` injected inside this process only.

Required runtime env:

- `OPENROUTER_API_KEY`: real OpenRouter key, resolved from Vault on the private host.

Optional runtime env:

- `PROVIDER_PROXY_PORT`: listen port, default `8080`.
- `OPENROUTER_BASE_URL`: upstream base URL, default `https://openrouter.ai/api/v1`.
- `OPENROUTER_DEFAULT_MODEL`: default model when the client omits one, default `deepseek/deepseek-chat`.
- `OPENROUTER_ALLOWED_MODELS`: comma-separated allowlist, default to the default model.
- `OPENROUTER_SITE_URL`: optional OpenRouter attribution header.
- `OPENROUTER_APP_NAME`: optional OpenRouter attribution header, default `Autoforge`.

Do not expose this service outside the private Docker network.
