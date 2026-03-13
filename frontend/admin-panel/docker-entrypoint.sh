#!/bin/sh
set -eu

API_BASE_URL="${API_BASE_URL:-https://api-gateway-392205979525.europe-west1.run.app}"
API_URL="${API_URL:-${API_BASE_URL}/api/v1}"
UPLOADS_BASE_URL="${UPLOADS_BASE_URL:-${API_BASE_URL}}"
NOTIFICATIONS_API_URL="${NOTIFICATIONS_API_URL:-${API_BASE_URL}/api}"
WS_URL="${WS_URL:-wss://api-gateway-392205979525.europe-west1.run.app}"

mkdir -p /usr/share/caddy/assets/config

cat >/usr/share/caddy/assets/config/config.json <<EOF
{
  "env": "${ENV:-dev}",
  "apiUrl": "${API_URL}",
  "apiBaseUrl": "${API_BASE_URL}",
  "uploadsBaseUrl": "${UPLOADS_BASE_URL}",
  "notificationsApiUrl": "${NOTIFICATIONS_API_URL}",
  "wsUrl": "${WS_URL}",
  "appName": "${APP_NAME:-SpeedLine Admin Panel}",
  "defaultLanguage": "${DEFAULT_LANGUAGE:-fr}",
  "supportedLanguages": ["fr", "en", "ar"]
}
EOF

exec caddy run --config /etc/caddy/Caddyfile --adapter caddyfile
