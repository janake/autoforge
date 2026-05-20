# Bug Report: Prompt Draft Shows Generic 503

Date: 2026-05-21

Task: AUTO-472

## Symptom

Prompt flow draft creation showed `Request failed with 503` after the backend returned `503 Service Unavailable` for unavailable prompt-draft AI.

## Root Cause

The backend correctly returned an `ApiErrorResponse` body with an actionable `message`, but `apps/web/src/auth/keycloak.ts` discarded non-2xx response bodies and threw only `Request failed with <status>`.

## Execution Path

- `PromptDraftPanel.createDraft` posts to `/v1/prompt-drafts`.
- `postAuthedJson` delegates to `authedFetch`.
- Backend `PromptDraftService.createDraft` calls `PromptDraftClarifier`.
- `HttpPromptDraftClarifier` throws `PromptDraftAiUnavailableException` when OpenCode config/runtime is unavailable.
- `GlobalExceptionHandler` maps that exception to `503` with a JSON `message`.
- `authedFetch` hid the message and surfaced only the status code.

## Fix

`authedFetch` now parses the backend JSON error body and throws `body.message` when present, falling back to the prior status-only message for empty or non-JSON errors.

## Verification

- `npm ci` - ok
- `npm run typecheck:web` - ok
- `npm run test:backend` - ok
- `npm run build:web` - ok

## Failure Count

0 countable hypothesis failures.
