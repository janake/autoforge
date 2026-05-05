#!/bin/sh
set -eu

cat >/usr/share/nginx/html/config.js <<EOF
window.__AUTOFORGE_CONFIG__ = {
  apiBaseUrl: "${API_BASE_URL:-/api}",
  keycloak: {
    url: "${KEYCLOAK_URL:-https://kc.prodet.org}",
    realm: "${KEYCLOAK_REALM:-autoforge}",
    clientId: "${KEYCLOAK_CLIENT_ID:-autoforge-web}"
  }
};
EOF
