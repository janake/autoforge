#!/usr/bin/env bash
set -euo pipefail

: "${APP_DIR:?APP_DIR is required}"

cd "$APP_DIR"

ENV_FILE="$APP_DIR/.env"

if [ -f .deploy.env ]; then
  set -a
  . ./.deploy.env
  set +a
fi

: "${ENV_FILE:?ENV_FILE is required}"
: "${GHCR_USERNAME:?GHCR_USERNAME is required}"
: "${GHCR_TOKEN:?GHCR_TOKEN is required}"

if [ ! -f "$ENV_FILE" ]; then
  echo "Runtime environment file is missing: $ENV_FILE" >&2
  exit 1
fi

if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose)
else
  echo "Docker Compose is not installed on the target host." >&2
  exit 1
fi

get_env_value() {
  local key="$1"
  local line

  while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in
      "$key="*)
        printf '%s' "${line#*=}"
        return 0
        ;;
    esac
  done < "$ENV_FILE"

  return 1
}

get_vault_secret() {
  local secret_ocid="$1"

  oci secrets secret-bundle get \
    --auth instance_principal \
    --secret-id "$secret_ocid" \
    --query 'data."secret-bundle-content".content' \
    --raw-output | base64 --decode
}

append_secret_env() {
  local key="$1"
  local value="$2"

  if [[ "$value" == *$'\n'* ]]; then
    echo "$key must not contain newline characters." >&2
    exit 1
  fi

  printf '%s=%s\n' "$key" "$value" >> "$RUNTIME_ENV"
}

OPENCODE_SERVER_PASSWORD_SECRET_OCID="$(get_env_value OPENCODE_SERVER_PASSWORD_SECRET_OCID || true)"
OPENAI_API_KEY_SECRET_OCID="$(get_env_value OPENAI_API_KEY_SECRET_OCID || true)"

RUNTIME_ENV="$(mktemp "$APP_DIR/.runtime.env.XXXXXX")"
trap 'rm -f "$APP_DIR/.deploy.env" "${RUNTIME_ENV:-}"' EXIT
chmod 600 "$RUNTIME_ENV"
cp "$ENV_FILE" "$RUNTIME_ENV"
printf '\n' >> "$RUNTIME_ENV"

COMPOSE_ARGS=(--env-file "$RUNTIME_ENV" -f docker-compose.private.yml)

if [ -n "$OPENCODE_SERVER_PASSWORD_SECRET_OCID" ] && [ -n "$OPENAI_API_KEY_SECRET_OCID" ]; then
  if ! command -v oci >/dev/null 2>&1; then
    echo "OCI CLI is required on the private host to read OpenCode secrets from OCI Vault." >&2
    exit 1
  fi

  if ! OPENCODE_SERVER_PASSWORD_VALUE="$(get_vault_secret "$OPENCODE_SERVER_PASSWORD_SECRET_OCID")"; then
    echo "Failed to read OPENCODE_SERVER_PASSWORD from OCI Vault." >&2
    exit 1
  fi

  if ! OPENAI_API_KEY_VALUE="$(get_vault_secret "$OPENAI_API_KEY_SECRET_OCID")"; then
    echo "Failed to read OPENAI_API_KEY from OCI Vault." >&2
    exit 1
  fi

  : "${OPENCODE_SERVER_PASSWORD_VALUE:?OPENCODE_SERVER_PASSWORD secret value is empty}"
  : "${OPENAI_API_KEY_VALUE:?OPENAI_API_KEY secret value is empty}"

  append_secret_env "OPENCODE_SERVER_PASSWORD" "$OPENCODE_SERVER_PASSWORD_VALUE"
  append_secret_env "OPENAI_API_KEY" "$OPENAI_API_KEY_VALUE"
  COMPOSE_ARGS+=(--profile opencode)
else
  echo "OpenCode secrets are not configured; deploying backend without the opencode profile." >&2
  append_secret_env "OPENCODE_SERVER_PASSWORD" "disabled-until-vault-secrets-are-configured"
fi

echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin
"${COMPOSE_CMD[@]}" "${COMPOSE_ARGS[@]}" pull
"${COMPOSE_CMD[@]}" "${COMPOSE_ARGS[@]}" up -d --remove-orphans
docker image prune -f
