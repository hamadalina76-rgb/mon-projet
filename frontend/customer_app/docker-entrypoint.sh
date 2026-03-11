#!/bin/sh
set -eu

mkdir -p /usr/share/caddy/assets/assets/config
cat >/usr/share/caddy/assets/assets/config/config.json <<EOF
{
  "env": "${ENV:-dev}",
  "apiBaseUrl": "${API_BASE_URL:-https://api-gateway-392205979525.europe-west1.run.app}",
  "wsUrl": "${WS_URL:-wss://api-gateway-392205979525.europe-west1.run.app}",
  "apiTimeoutMs": ${API_TIMEOUT_MS:-30000}
}
EOF

exec caddy run --config /etc/caddy/Caddyfile --adapter caddyfile
