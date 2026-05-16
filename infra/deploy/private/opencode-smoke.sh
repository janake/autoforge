#!/usr/bin/env bash
set -euo pipefail

: "${OPENCODE_SERVER_USERNAME:?OPENCODE_SERVER_USERNAME is required}"
: "${OPENCODE_SERVER_PASSWORD:?OPENCODE_SERVER_PASSWORD is required}"
: "${OPENCODE_MODEL:?OPENCODE_MODEL is required}"

NETWORK_NAME="${OPENCODE_NETWORK_NAME:-autoforge-private}"
OPENCODE_PORT="${OPENCODE_SERVER_PORT:-4096}"
OPENCODE_BASE_URL="${OPENCODE_BASE_URL:-http://opencode:${OPENCODE_PORT}}"
HTTP_CLIENT_IMAGE="${HTTP_CLIENT_IMAGE:-curlimages/curl:8.13.0}"

docker_curl() {
  docker run --rm --network "$NETWORK_NAME" "$HTTP_CLIENT_IMAGE" "$@"
}

basic_auth_args=(--user "$OPENCODE_SERVER_USERNAME:$OPENCODE_SERVER_PASSWORD")

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

message_payload="$(printf '%s' "{\"model\":\"$OPENCODE_MODEL\",\"parts\":[{\"type\":\"text\",\"text\":\"Reply with the exact token smoke-ok and nothing else.\"}]}" )"
message_response="$(docker_curl -fsS "${basic_auth_args[@]}" -H 'Content-Type: application/json' -X POST -d "$message_payload" "$OPENCODE_BASE_URL/session/$session_id/message")"

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
