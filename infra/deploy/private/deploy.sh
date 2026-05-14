#!/usr/bin/env bash
set -euo pipefail

: "${APP_DIR:?APP_DIR is required}"

cd "$APP_DIR"

ENV_FILE="$APP_DIR/.env"
BACKEND_IMAGE_ARCHIVE="$APP_DIR/autoforge-backend-image.tar.gz"
OPENCODE_IMAGE_ARCHIVE="$APP_DIR/autoforge-opencode-image.tar.gz"
OCI_CLI_IMAGE_ARCHIVE="$APP_DIR/autoforge-oci-cli-image.tar.gz"
PRIVATE_IMAGES_PRELOADED="${PRIVATE_IMAGES_PRELOADED:-false}"
WALLET_DIR="$APP_DIR/wallet"
OCI_CLI_IMAGE="${OCI_CLI_IMAGE:-ghcr.io/oracle/oci-cli:latest}"
OCI_CMD=(oci)

if [ -d "$HOME/.local/bin" ]; then
  PATH="$HOME/.local/bin:$PATH"
fi

export PATH

if [ -f .deploy.env ]; then
  set -a
  . ./.deploy.env
  set +a
fi

: "${ENV_FILE:?ENV_FILE is required}"

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

ensure_oci_command() {
  if command -v oci >/dev/null 2>&1; then
    OCI_CMD=(oci)
    return 0
  fi

  if ! command -v docker >/dev/null 2>&1; then
    echo "Docker is required to run the OCI CLI container on the private host." >&2
    return 1
  fi

  if ! docker image inspect "$OCI_CLI_IMAGE" >/dev/null 2>&1; then
    docker pull "$OCI_CLI_IMAGE" >/dev/null
  fi
  OCI_CMD=(docker run --rm --network host "$OCI_CLI_IMAGE")
}

get_vault_secret() {
  local secret_ocid="$1"

  "${OCI_CMD[@]}" secrets secret-bundle get \
    --auth instance_principal \
    --secret-id "$secret_ocid" \
    --query 'data."secret-bundle-content".content' \
    --raw-output | base64 --decode
}

find_vault_secret_id_by_name() {
  local name="$1"

  if [[ "$name" == *"'"* ]]; then
    echo "Vault secret names with single quotes are not supported: $name" >&2
    return 1
  fi

  "${OCI_CMD[@]}" search resource free-text-search \
    --auth instance_principal \
    --text "$name" \
    --query "data.items[?\"resource-type\"=='VaultSecret' && \"display-name\"=='$name' && \"lifecycle-state\"=='ACTIVE'] | [0].identifier" \
    --raw-output
}

get_vault_secret_by_name() {
  local name="$1"
  local secret_ocid

  secret_ocid="$(find_vault_secret_id_by_name "$name")"
  if [ -z "$secret_ocid" ] || [ "$secret_ocid" = "null" ]; then
    echo "Expected one ACTIVE OCI Vault secret named $name, found none." >&2
    return 1
  fi

  get_vault_secret "$secret_ocid"
}

get_optional_vault_secret_by_name() {
  local name="$1"
  local secret_ocid

  secret_ocid="$(find_vault_secret_id_by_name "$name")"
  if [ -z "$secret_ocid" ] || [ "$secret_ocid" = "null" ]; then
    return 1
  fi

  get_vault_secret "$secret_ocid"
}

ensure_metadata_block() {
  local metadata_ip="169.254.169.254"
  local iptables_cmd=(iptables)

  if ! command -v iptables >/dev/null 2>&1; then
    echo "iptables is required to block container access to the metadata endpoint." >&2
    exit 1
  fi

  if [ "$EUID" -ne 0 ]; then
    if command -v sudo >/dev/null 2>&1 && sudo -n true 2>/dev/null; then
      iptables_cmd=(sudo -n iptables)
    else
      echo "Passwordless sudo is required to block container access to the metadata endpoint with iptables." >&2
      exit 1
    fi
  fi

  if ! "${iptables_cmd[@]}" -L DOCKER-USER -n >/dev/null 2>&1; then
    echo "Docker DOCKER-USER iptables chain is required to block container metadata access." >&2
    exit 1
  fi

  if ! "${iptables_cmd[@]}" -C DOCKER-USER -d "$metadata_ip" -j REJECT 2>/dev/null; then
    "${iptables_cmd[@]}" -I DOCKER-USER 1 -d "$metadata_ip" -j REJECT
  fi
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

ensure_wallet_dir() {
  rm -rf "$WALLET_DIR"
  mkdir -p "$WALLET_DIR"
  chmod 700 "$WALLET_DIR"
}

download_wallet() {
  local wallet_url="$1"
  local wallet_password="$2"

  if ! command -v curl >/dev/null 2>&1; then
    echo "curl is required to download the ADB wallet zip." >&2
    exit 1
  fi

  if ! command -v unzip >/dev/null 2>&1; then
    echo "unzip is required to extract the ADB wallet zip." >&2
    exit 1
  fi

  curl -fsSL "$wallet_url" -o "$APP_DIR/autoforge-adb-wallet.zip"
  unzip -o -P "$wallet_password" "$APP_DIR/autoforge-adb-wallet.zip" -d "$WALLET_DIR" >/dev/null
  rm -f "$APP_DIR/autoforge-adb-wallet.zip"
}

OPENCODE_SERVER_PASSWORD_SECRET_OCID="$(get_env_value OPENCODE_SERVER_PASSWORD_SECRET_OCID || true)"
OPENAI_API_KEY_SECRET_OCID="$(get_env_value OPENAI_API_KEY_SECRET_OCID || true)"
GEMINI_API_KEY_SECRET_OCID="$(get_env_value GEMINI_API_KEY_SECRET_OCID || true)"
DB_URL="$(get_env_value AUTOFORGE_DB_URL || true)"
DB_URL_SECRET_NAME="$(get_env_value AUTOFORGE_DB_URL_SECRET_NAME || true)"
DB_WALLET_URL="$(get_env_value AUTOFORGE_DB_WALLET_URL || true)"
DB_WALLET_URL_SECRET_NAME="$(get_env_value AUTOFORGE_DB_WALLET_URL_SECRET_NAME || true)"
DB_WALLET_PASSWORD_SECRET_OCID="$(get_env_value AUTOFORGE_DB_WALLET_PASSWORD_SECRET_OCID || true)"
DB_WALLET_PASSWORD_SECRET_NAME="$(get_env_value AUTOFORGE_DB_WALLET_PASSWORD_SECRET_NAME || true)"
DB_PASSWORD_SECRET_OCID="$(get_env_value AUTOFORGE_DB_PASSWORD_SECRET_OCID || true)"
DB_PASSWORD_SECRET_NAME="$(get_env_value AUTOFORGE_DB_PASSWORD_SECRET_NAME || true)"
DB_PASSWORD="$(get_env_value AUTOFORGE_DB_PASSWORD || true)"
DB_USERNAME="$(get_env_value AUTOFORGE_DB_USERNAME || true)"
DB_USERNAME_SECRET_NAME="$(get_env_value AUTOFORGE_DB_USERNAME_SECRET_NAME || true)"
DB_SERVICE_ALIAS="$(get_env_value AUTOFORGE_DB_SERVICE_ALIAS || true)"
DB_SERVICE_ALIAS_SECRET_NAME="$(get_env_value AUTOFORGE_DB_SERVICE_ALIAS_SECRET_NAME || true)"

RUNTIME_ENV="$(mktemp "$APP_DIR/.runtime.env.XXXXXX")"
trap 'rm -f "$APP_DIR/.deploy.env" "${RUNTIME_ENV:-}"' EXIT
chmod 600 "$RUNTIME_ENV"
cp "$ENV_FILE" "$RUNTIME_ENV"
printf '\n' >> "$RUNTIME_ENV"

COMPOSE_ARGS=(--env-file "$RUNTIME_ENV" -f docker-compose.private.yml)

if [ -f "$OCI_CLI_IMAGE_ARCHIVE" ]; then
  docker load --input "$OCI_CLI_IMAGE_ARCHIVE"
  rm -f "$OCI_CLI_IMAGE_ARCHIVE"
fi

: "${DB_URL_SECRET_NAME:=autoforge-db-url}"
: "${DB_WALLET_URL_SECRET_NAME:=autoforge-db-wallet-url}"
: "${DB_WALLET_PASSWORD_SECRET_NAME:=db-wallet-pwd}"
: "${DB_PASSWORD_SECRET_NAME:=autoforge-db-password}"
: "${DB_USERNAME_SECRET_NAME:=autoforge-db-username}"
: "${DB_SERVICE_ALIAS_SECRET_NAME:=autoforge-db-service-alias}"

NEEDS_OCI=false

if [ -n "$OPENCODE_SERVER_PASSWORD_SECRET_OCID" ] \
  || [ -n "$OPENAI_API_KEY_SECRET_OCID" ] \
  || [ -n "$GEMINI_API_KEY_SECRET_OCID" ] \
  || [ -n "$DB_WALLET_PASSWORD_SECRET_OCID" ] \
  || [ -n "$DB_PASSWORD_SECRET_OCID" ]; then
  NEEDS_OCI=true
elif [ -z "$DB_URL" ] && [ -z "$DB_WALLET_URL" ]; then
  NEEDS_OCI=true
elif [ -n "$DB_URL" ] && [ -z "$DB_PASSWORD" ]; then
  NEEDS_OCI=true
elif [ -n "$DB_WALLET_URL" ] && [ -z "$DB_WALLET_PASSWORD_SECRET_OCID" ]; then
  NEEDS_OCI=true
fi

if [ "$NEEDS_OCI" = "true" ]; then
  if ! ensure_oci_command; then
    echo "OCI CLI access is required on the private host to read configured secrets from OCI Vault." >&2
    exit 1
  fi
fi

if [ -n "$OPENCODE_SERVER_PASSWORD_SECRET_OCID" ]; then
  OPENCODE_SERVER_PASSWORD_VALUE="$(get_vault_secret "$OPENCODE_SERVER_PASSWORD_SECRET_OCID")"
  append_secret_env "OPENCODE_SERVER_PASSWORD" "$OPENCODE_SERVER_PASSWORD_VALUE"
  COMPOSE_ARGS+=(--profile opencode)
elif ! get_env_value OPENCODE_SERVER_PASSWORD >/dev/null 2>&1; then
  append_secret_env "OPENCODE_SERVER_PASSWORD" "disabled-until-vault-secrets-are-configured"
fi

if [ -n "$OPENAI_API_KEY_SECRET_OCID" ]; then
  OPENAI_API_KEY_VALUE="$(get_vault_secret "$OPENAI_API_KEY_SECRET_OCID")"
  append_secret_env "OPENAI_API_KEY" "$OPENAI_API_KEY_VALUE"
fi

if [ -n "$GEMINI_API_KEY_SECRET_OCID" ]; then
  GEMINI_API_KEY_VALUE="$(get_vault_secret "$GEMINI_API_KEY_SECRET_OCID")"
  append_secret_env "GEMINI_API_KEY" "$GEMINI_API_KEY_VALUE"
fi

if [ -z "$DB_URL" ] && [ -n "$DB_URL_SECRET_NAME" ]; then
  DB_URL="$(get_optional_vault_secret_by_name "$DB_URL_SECRET_NAME" || true)"
fi

if [ -z "$DB_WALLET_URL" ] && [ -n "$DB_WALLET_URL_SECRET_NAME" ]; then
  DB_WALLET_URL="$(get_optional_vault_secret_by_name "$DB_WALLET_URL_SECRET_NAME" || true)"
fi

if [ -z "$DB_USERNAME" ] && [ -n "$DB_USERNAME_SECRET_NAME" ]; then
  DB_USERNAME="$(get_optional_vault_secret_by_name "$DB_USERNAME_SECRET_NAME" || true)"
fi

if [ -z "$DB_SERVICE_ALIAS" ] && [ -n "$DB_SERVICE_ALIAS_SECRET_NAME" ]; then
  DB_SERVICE_ALIAS="$(get_optional_vault_secret_by_name "$DB_SERVICE_ALIAS_SECRET_NAME" || true)"
fi

if [ -z "$DB_PASSWORD" ] && [ -z "$DB_PASSWORD_SECRET_OCID" ] && [ -n "$DB_PASSWORD_SECRET_NAME" ]; then
  DB_PASSWORD="$(get_optional_vault_secret_by_name "$DB_PASSWORD_SECRET_NAME" || true)"
fi

if [ -n "$DB_URL" ]; then
  : "${DB_USERNAME:=ADMIN}"
  if [ -z "$DB_PASSWORD" ] && [ -z "$DB_PASSWORD_SECRET_OCID" ]; then
    echo "AUTOFORGE_DB_PASSWORD or AUTOFORGE_DB_PASSWORD_SECRET_OCID is required when AUTOFORGE_DB_URL is set." >&2
    exit 1
  fi
  append_secret_env "AUTOFORGE_DB_URL" "$DB_URL"
  append_secret_env "AUTOFORGE_DB_USERNAME" "$DB_USERNAME"
  if [ -n "$DB_PASSWORD_SECRET_OCID" ]; then
    DB_PASSWORD_VALUE="$(get_vault_secret "$DB_PASSWORD_SECRET_OCID")"
    append_secret_env "AUTOFORGE_DB_PASSWORD" "$DB_PASSWORD_VALUE"
  elif [ -n "$DB_PASSWORD" ]; then
    append_secret_env "AUTOFORGE_DB_PASSWORD" "$DB_PASSWORD"
  fi
elif [ -n "$DB_WALLET_URL" ]; then
  if [ -z "$DB_WALLET_PASSWORD_SECRET_OCID" ] && [ -z "$DB_WALLET_PASSWORD_SECRET_NAME" ]; then
    echo "AUTOFORGE_DB_WALLET_PASSWORD_SECRET_OCID or AUTOFORGE_DB_WALLET_PASSWORD_SECRET_NAME is required when AUTOFORGE_DB_WALLET_URL is set." >&2
    exit 1
  fi
  : "${DB_USERNAME:=ADMIN}"
  : "${DB_SERVICE_ALIAS:=autoforge_high}"

  if [ -n "$DB_WALLET_PASSWORD_SECRET_OCID" ]; then
    DB_WALLET_PASSWORD_VALUE="$(get_vault_secret "$DB_WALLET_PASSWORD_SECRET_OCID")"
  else
    DB_WALLET_PASSWORD_VALUE="$(get_vault_secret_by_name "$DB_WALLET_PASSWORD_SECRET_NAME")"
  fi
  ensure_wallet_dir
  download_wallet "$DB_WALLET_URL" "$DB_WALLET_PASSWORD_VALUE"

  append_secret_env "TNS_ADMIN" "$WALLET_DIR"
  append_secret_env "AUTOFORGE_DB_URL" "jdbc:oracle:thin:@${DB_SERVICE_ALIAS}?TNS_ADMIN=${WALLET_DIR}"
  append_secret_env "AUTOFORGE_DB_USERNAME" "$DB_USERNAME"

  if [ -n "$DB_PASSWORD_SECRET_OCID" ]; then
    DB_PASSWORD_VALUE="$(get_vault_secret "$DB_PASSWORD_SECRET_OCID")"
    append_secret_env "AUTOFORGE_DB_PASSWORD" "$DB_PASSWORD_VALUE"
  elif [ -n "$DB_PASSWORD" ]; then
    append_secret_env "AUTOFORGE_DB_PASSWORD" "$DB_PASSWORD"
  fi
else
  echo "AUTOFORGE_DB_URL or AUTOFORGE_DB_WALLET_URL is required for private backend deploy." >&2
  exit 1
fi

ensure_metadata_block

if [ -f "$BACKEND_IMAGE_ARCHIVE" ]; then
  docker load --input "$BACKEND_IMAGE_ARCHIVE"
  rm -f "$BACKEND_IMAGE_ARCHIVE"
fi

if [ -f "$OPENCODE_IMAGE_ARCHIVE" ]; then
  docker load --input "$OPENCODE_IMAGE_ARCHIVE"
  rm -f "$OPENCODE_IMAGE_ARCHIVE"
fi

if [ "$PRIVATE_IMAGES_PRELOADED" != "true" ]; then
  : "${GHCR_USERNAME:?GHCR_USERNAME is required}"
  : "${GHCR_TOKEN:?GHCR_TOKEN is required}"
  echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin
  "${COMPOSE_CMD[@]}" "${COMPOSE_ARGS[@]}" pull
fi

"${COMPOSE_CMD[@]}" "${COMPOSE_ARGS[@]}" up -d --remove-orphans
docker image prune -f
