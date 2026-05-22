---
id: research-2026-05-22-ai-communication-alternatives
type: research
date: 2026-05-22
---

# Research: AI Communication Alternatives

**Backend:** opencode-subagents

**Scope:** Current OpenCode/OpenRouter/provider-proxy path and lower-resource alternatives.

## Summary

OpenCode is currently used as a REST chat facade between the backend and the provider proxy. The smallest lower-resource replacement is for the backend to call the existing provider proxy directly, keeping OpenRouter credentials isolated while removing the OpenCode container from the private host.

## Key Files

| File | Purpose |
|---|---|
| `infra/compose/docker-compose.private.yml` | Private backend, OpenCode, provider-proxy compose wiring |
| `services/backend/src/main/java/org/autoforge/backend/service/HttpOpenCodeAIPatchGenerator.java` | Backend OpenCode REST client |
| `services/provider-proxy/src/server.mjs` | OpenRouter proxy and guardrails |
| `infra/deploy/private/opencode-smoke.sh` | Current deploy smoke check for OpenCode |
| `docs/ai-runtime-openrouter.md` | Current runtime decision and constraints |

## Findings

- Backend is configured to call OpenCode through `OPENCODE_SERVER_URL` in `infra/compose/docker-compose.private.yml:13`.
- OpenCode and provider-proxy are separate private compose services in `infra/compose/docker-compose.private.yml:31` and `infra/compose/docker-compose.private.yml:57`.
- The backend creates an OpenCode session and sends a message in `services/backend/src/main/java/org/autoforge/backend/service/HttpOpenCodeAIPatchGenerator.java:47` and `services/backend/src/main/java/org/autoforge/backend/service/HttpOpenCodeAIPatchGenerator.java:64`.
- The provider proxy already forwards OpenAI-compatible chat completions upstream in `services/provider-proxy/src/server.mjs:121`.
- The private host constraint is about 1 GB RAM in `docs/ai-runtime-openrouter.md:66`.
- The provider proxy must remain private-only according to `services/provider-proxy/README.md:27`.

## Recommendations

- Prefer backend direct calls to the provider proxy for the next implementation.
- Remove OpenCode from the always-on private runtime path.
- Keep remote worker or queue-based execution for a later phase if AI jobs outgrow synchronous backend calls.
