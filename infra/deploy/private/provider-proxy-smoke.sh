#!/usr/bin/env bash
set -euo pipefail

: "${AI_PROVIDER_MODEL:?AI_PROVIDER_MODEL is required}"

NETWORK_NAME="${AI_PROVIDER_NETWORK_NAME:-autoforge-private}"
PROVIDER_PROXY_PORT="${PROVIDER_PROXY_PORT:-8080}"
PROVIDER_PROXY_BASE_URL="${AI_PROVIDER_PROXY_BASE_URL:-http://openrouter-proxy:${PROVIDER_PROXY_PORT}/v1}"
PROVIDER_PROXY_BASE_URL="${PROVIDER_PROXY_BASE_URL%/}"
PROVIDER_PROXY_ROOT_URL="${PROVIDER_PROXY_BASE_URL%/v1}"
HTTP_CLIENT_IMAGE="${HTTP_CLIENT_IMAGE:-curlimages/curl:8.13.0}"

docker_curl() {
  docker run --rm --network "$NETWORK_NAME" "$HTTP_CLIENT_IMAGE" "$@"
}

health_response="$(docker_curl -fsS "$PROVIDER_PROXY_ROOT_URL/health")"
if [[ "$health_response" != *'"healthy":true'* ]] || [[ "$health_response" != *'"apiKeyConfigured":true'* ]]; then
  echo "Provider proxy health check did not report healthy configured state: $health_response" >&2
  exit 1
fi

models_response="$(docker_curl -fsS "$PROVIDER_PROXY_BASE_URL/models")"
if [[ "$models_response" != *"$AI_PROVIDER_MODEL"* ]]; then
  echo "Provider proxy model list does not include $AI_PROVIDER_MODEL: $models_response" >&2
  exit 1
fi

chat_payload="$(printf '{"model":"%s","messages":[{"role":"user","content":"Reply with the exact token smoke-ok and nothing else."}]}' "$AI_PROVIDER_MODEL")"
chat_result="$(docker_curl -sS -w '\n%{http_code}' -H 'Content-Type: application/json' -X POST -d "$chat_payload" "$PROVIDER_PROXY_BASE_URL/chat/completions")"
chat_status="${chat_result##*$'\n'}"
chat_response="${chat_result%$'\n'*}"
if [[ "$chat_status" != 2* ]]; then
  if [[ "$chat_status" == "429" ]]; then
    echo "Provider proxy smoke prompt was rate-limited by the upstream provider after health/model checks passed: $chat_response" >&2
    exit 0
  fi
  echo "Provider proxy smoke prompt returned HTTP $chat_status: $chat_response" >&2
  exit 1
fi

if [[ "$chat_response" != *smoke-ok* ]]; then
  echo "Provider proxy smoke prompt did not complete successfully: $chat_response" >&2
  exit 1
fi

echo "Provider proxy smoke test passed for model $AI_PROVIDER_MODEL"
