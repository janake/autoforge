#!/usr/bin/env bash
set -euo pipefail

: "${APP_DIR:?APP_DIR is required}"

cd "$APP_DIR"

if [ -f .deploy.env ]; then
  set -a
  . ./.deploy.env
  set +a
fi

: "${GHCR_USERNAME:?GHCR_USERNAME is required}"
: "${GHCR_TOKEN:?GHCR_TOKEN is required}"

if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose)
else
  echo "Docker Compose is not installed on the target host." >&2
  exit 1
fi

trap 'rm -f "$APP_DIR/.deploy.env"' EXIT

echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin
"${COMPOSE_CMD[@]}" --env-file .env -f docker-compose.public.yml pull
"${COMPOSE_CMD[@]}" --env-file .env -f docker-compose.public.yml up -d --remove-orphans

if [ -n "${CLOUDFLARE_API_TOKEN:-}" ] && [ -n "${CLOUDFLARE_ZONE_ID:-}" ] && [ -n "${CLOUDFLARE_ORIGIN_HOST:-}" ]; then
  if command -v python3 >/dev/null 2>&1; then
    CLOUDFLARE_RECORDS="${CLOUDFLARE_RECORDS:-$GATEWAY_DOMAIN}" \
    CLOUDFLARE_PROXIED="${CLOUDFLARE_PROXIED:-true}" \
      python3 "$APP_DIR/cloudflare_sync.py"
  else
    echo "python3 is required to sync Cloudflare DNS records." >&2
    exit 1
  fi
else
  echo "Cloudflare sync skipped: missing CLOUDFLARE_API_TOKEN, CLOUDFLARE_ZONE_ID, or CLOUDFLARE_ORIGIN_HOST."
fi

docker image prune -f
