#!/bin/sh
set -eu

cat >/usr/share/caddy/assets/config/config.json <<EOF
{
  "env": "${ENV:-dev}",
  "apiUrl": "${API_URL:-https://api-gateway-392205979525.europe-west1.run.app/api/v1}",
  "apiBaseUrl": "${API_BASE_URL:-https://api-gateway-392205979525.europe-west1.run.app}",
  "uploadsBaseUrl": "${UPLOADS_BASE_URL:-https://api-gateway-392205979525.europe-west1.run.app}",
  "notificationsApiUrl": "${NOTIFICATIONS_API_URL:-https://api-gateway-392205979525.europe-west1.run.app/api}",
  "wsUrl": "${WS_URL:-wss://api-gateway-392205979525.europe-west1.run.app}",
  "appName": "${APP_NAME:-SpeedLine Admin Panel}",
  "defaultLanguage": "${DEFAULT_LANGUAGE:-fr}",
  "supportedLanguages": ["fr", "en", "ar"]
}
EOF

exec caddy run --config /etc/caddy/Caddyfile --adapter caddyfile
