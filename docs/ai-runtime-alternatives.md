# AI Runtime Alternatives Spike

Task: `AUTO-478`

Version: `0.1.81`

## Problem

The private host cannot sustain the full OpenCode runtime. We need a smaller way for Autoforge to ask an AI model for prompt clarification and patch-generation output.

## Previous Path

At the time of the spike, the live flow was:

```text
Browser -> public gateway -> private backend -> OpenCode REST -> provider-proxy -> OpenRouter
```

AUTO-480 replaces that path with:

```text
Browser -> public gateway -> private backend -> provider-proxy -> OpenRouter
```

Current implementation:

- `infra/compose/docker-compose.private.yml` wires the backend with `AI_PROVIDER_PROXY_BASE_URL` and `AI_PROVIDER_MODEL`.
- `infra/compose/docker-compose.private.yml` defines `openrouter-proxy` under the `ai` profile.
- `services/backend/src/main/java/org/autoforge/backend/service/ProviderProxyChatClient.java` calls the OpenAI-compatible provider proxy.
- `services/provider-proxy/src/server.mjs` forwards OpenAI-compatible chat completions to the upstream provider.

Historical implementation details from the old path:

- The private compose stack pointed the backend at `http://opencode:4096`.
- The private compose stack defined both `opencode` and `openrouter-proxy` services.
- The backend created an OpenCode session and sent prompts to that session.
- The provider proxy forwarded OpenAI-compatible chat completions to the upstream provider.

## What OpenCode Did

OpenCode acted as a REST chat facade, not as the repository executor.

- The old backend integration expected a JSON response containing `patch`, `summary`, and `changedFiles` from OpenCode output.
- Git execution happens through backend-side `ProcessBuilder`, shown in `services/backend/src/main/java/org/autoforge/backend/git/GitProcessRunner.java:13`.
- The old deploy smoke test validated OpenCode session creation and message completion.

## Constraints

- The private host target is about 1 GB RAM.
- The provider key should stay outside backend prompts, backend environment, generated records, and logs; `docs/ai-runtime-openrouter.md` defines that boundary.
- The provider proxy already enforces model allowlisting and request limits in `services/provider-proxy/src/server.mjs:15`, `services/provider-proxy/src/server.mjs:27`, and `services/provider-proxy/src/server.mjs:28`.
- The provider proxy is explicitly private-only in `services/provider-proxy/README.md:27`.

## Options

| Option | Summary | Pros | Cons |
|---|---|---|---|
| Backend -> OpenRouter direct | Backend calls OpenRouter itself. | Lowest container count and simplest flow. | Backend now holds provider API key and must own all guardrails. |
| Backend -> provider-proxy -> OpenRouter | Backend calls the existing lightweight proxy. | Removes OpenCode, keeps key isolation and guardrails. | Backend needs an OpenAI-compatible client and response parser. |
| Backend -> queue -> remote worker | Private host enqueues AI work; larger worker runs elsewhere. | Moves heavy work off the 1 GB host. | Requires queue, auth, retries, observability. |
| Serverless AI worker | Per-job external function talks to providers. | No long-running AI container. | More vendor/runtime coupling and cold-start behavior. |
| Keep OpenCode off by default | Start OpenCode only manually or for rare jobs. | Minimal change. | Still keeps a heavy runtime path and fragile deploy/smoke logic. |

## Recommendation

Use `backend -> provider-proxy -> OpenRouter` as the next implementation direction.

Reasons:

- It removes the heavy OpenCode container from the private host.
- It preserves the current provider-key isolation boundary.
- It reuses the existing provider proxy model allowlist and token/body limits.
- It matches current usage because OpenCode is only a REST chat/session facade in the production path.

## Follow-up Tasks

- DONE in `AUTO-480`: replace `HttpOpenCodeAIPatchGenerator` with a provider-proxy client that calls `/v1/chat/completions`.
- DONE in `AUTO-480`: remove `opencode` from `docker-compose.private.yml` and private deploy env generation.
- DONE in `AUTO-480`: replace `opencode-smoke.sh` with provider-proxy smoke checks.
- DONE in `AUTO-480`: update `docs/ai-runtime-openrouter.md`, `docs/deployment.md`, and architecture docs.
- Keep queue/remote-worker design as a later phase if patch generation becomes too heavy for synchronous backend calls.
