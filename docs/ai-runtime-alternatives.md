# AI Runtime Alternatives Spike

Task: `AUTO-478`

Version: `0.1.81`

## Problem

The private host cannot sustain the full OpenCode runtime. We need a smaller way for Autoforge to ask an AI model for prompt clarification and patch-generation output.

## Current Path

Current flow:

```text
Browser -> public gateway -> private backend -> OpenCode REST -> provider-proxy -> OpenRouter
```

Evidence:

- `infra/compose/docker-compose.private.yml:13` points the backend at `http://opencode:4096`.
- `infra/compose/docker-compose.private.yml:31` defines the `opencode` service.
- `infra/compose/docker-compose.private.yml:57` defines the `openrouter-proxy` service.
- `docs/ai-runtime-openrouter.md:20` documents OpenCode on the private Docker network before the provider proxy.
- `services/backend/src/main/java/org/autoforge/backend/service/HttpOpenCodeAIPatchGenerator.java:47` creates an OpenCode session.
- `services/backend/src/main/java/org/autoforge/backend/service/HttpOpenCodeAIPatchGenerator.java:64` sends the prompt to the OpenCode session.
- `services/provider-proxy/src/server.mjs:121` forwards OpenAI-compatible chat completions to the upstream provider.

## What OpenCode Currently Does

OpenCode is acting as a REST chat facade, not as the repository executor.

- The backend expects a JSON response containing `patch`, `summary`, and `changedFiles` from OpenCode output in `services/backend/src/main/java/org/autoforge/backend/service/HttpOpenCodeAIPatchGenerator.java:83`.
- Git execution happens through backend-side `ProcessBuilder`, shown in `services/backend/src/main/java/org/autoforge/backend/git/GitProcessRunner.java:13`.
- The deploy smoke test validates OpenCode session creation and message completion in `infra/deploy/private/opencode-smoke.sh:51` and `infra/deploy/private/opencode-smoke.sh:59`.

## Constraints

- The private host target is about 1 GB RAM, documented in `docs/ai-runtime-openrouter.md:66`.
- The provider key should stay outside OpenCode/backend prompts and logs; `docs/ai-runtime-openrouter.md:12` and `docs/ai-runtime-openrouter.md:13` define that boundary.
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

- Replace `HttpOpenCodeAIPatchGenerator` with a provider-proxy client that calls `/v1/chat/completions`.
- Remove `opencode` from `docker-compose.private.yml` and private deploy env generation.
- Replace `opencode-smoke.sh` with provider-proxy and backend AI smoke checks.
- Update `docs/ai-runtime-openrouter.md`, `docs/deployment.md`, and architecture docs once the implementation direction is confirmed.
- Keep queue/remote-worker design as a later phase if patch generation becomes too heavy for synchronous backend calls.
