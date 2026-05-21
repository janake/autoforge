#!/usr/bin/env bash
set -euo pipefail

: "${OPENCODE_SERVER_USERNAME:?OPENCODE_SERVER_USERNAME is required}"
: "${OPENCODE_SERVER_PASSWORD:?OPENCODE_SERVER_PASSWORD is required}"
: "${OPENCODE_MODEL:?OPENCODE_MODEL is required}"

NETWORK_NAME="${OPENCODE_NETWORK_NAME:-autoforge-private}"
OPENCODE_PORT="${OPENCODE_SERVER_PORT:-4096}"
OPENCODE_BASE_URL="${OPENCODE_BASE_URL:-http://opencode:${OPENCODE_PORT}}"
PROVIDER_PROXY_PORT="${PROVIDER_PROXY_PORT:-8080}"
OPENROUTER_PROXY_BASE_URL="${OPENROUTER_PROXY_BASE_URL:-http://openrouter-proxy:${PROVIDER_PROXY_PORT}}"
OPENROUTER_DEFAULT_MODEL="${OPENROUTER_DEFAULT_MODEL:-deepseek/deepseek-chat}"
HTTP_CLIENT_IMAGE="${HTTP_CLIENT_IMAGE:-curlimages/curl:8.13.0}"

docker_curl() {
  docker run --rm --network "$NETWORK_NAME" "$HTTP_CLIENT_IMAGE" "$@"
}

basic_auth_args=(--user "$OPENCODE_SERVER_USERNAME:$OPENCODE_SERVER_PASSWORD")

if [[ "$OPENCODE_MODEL" != */* ]]; then
  echo "OPENCODE_MODEL must use provider/model format: $OPENCODE_MODEL" >&2
  exit 1
fi
opencode_provider_id="${OPENCODE_MODEL%%/*}"
opencode_model_id="${OPENCODE_MODEL#*/}"
if [ -z "$opencode_provider_id" ] || [ -z "$opencode_model_id" ]; then
  echo "OPENCODE_MODEL must include both provider and model id: $OPENCODE_MODEL" >&2
  exit 1
fi

proxy_health_response="$(docker_curl -fsS "$OPENROUTER_PROXY_BASE_URL/health")"
if [[ "$proxy_health_response" != *'"healthy":true'* ]] || [[ "$proxy_health_response" != *'"apiKeyConfigured":true'* ]]; then
  echo "OpenRouter proxy health check did not report healthy configured state: $proxy_health_response" >&2
  exit 1
fi

proxy_models_response="$(docker_curl -fsS "$OPENROUTER_PROXY_BASE_URL/v1/models")"
if [[ "$proxy_models_response" != *"$OPENROUTER_DEFAULT_MODEL"* ]]; then
  echo "OpenRouter proxy model list does not include $OPENROUTER_DEFAULT_MODEL: $proxy_models_response" >&2
  exit 1
fi

health_response="$(docker_curl -fsS "${basic_auth_args[@]}" "$OPENCODE_BASE_URL/global/health")"
if [[ "$health_response" != *'"healthy":true'* ]]; then
  echo "OpenCode health check did not report healthy: $health_response" >&2
  exit 1
fi

session_response="$(docker_curl -fsS "${basic_auth_args[@]}" -H 'Content-Type: application/json' -X POST -d '{"title":"AUTO-322 opencode smoke"}' "$OPENCODE_BASE_URL/session")"
if [[ ! "$session_response" =~ \"id\":\"([^\"]+)\" ]]; then
  echo "OpenCode session response missing id: $session_response" >&2
  exit 1
fi
session_id="${BASH_REMATCH[1]}"

message_payload="$(printf '{"model":{"providerID":"%s","modelID":"%s"},"parts":[{"type":"text","text":"Reply with the exact token smoke-ok and nothing else."}]}' "$opencode_provider_id" "$opencode_model_id")"
message_result="$(docker_curl -sS -w '\n%{http_code}' "${basic_auth_args[@]}" -H 'Content-Type: application/json' -X POST -d "$message_payload" "$OPENCODE_BASE_URL/session/$session_id/message")"
message_status="${message_result##*$'\n'}"
message_response="${message_result%$'\n'*}"
if [[ "$message_status" != 2* ]]; then
  echo "OpenCode smoke prompt returned HTTP $message_status: $message_response" >&2
  exit 1
fi

if [[ "$message_response" != *smoke-ok* ]]; then
  echo "OpenCode smoke prompt did not complete successfully: $message_response" >&2
  exit 1
fi

history_response="$(docker_curl -fsS "${basic_auth_args[@]}" "$OPENCODE_BASE_URL/session/$session_id/message?limit=1")"
if [[ "$history_response" != *smoke-ok* ]]; then
  echo "OpenCode message history did not include the smoke response: $history_response" >&2
  exit 1
fi

echo "OpenCode REST smoke test passed for session $session_id"
