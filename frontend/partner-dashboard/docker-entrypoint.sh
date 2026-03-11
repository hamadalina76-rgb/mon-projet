#!/bin/sh
set -eu

cat >/usr/share/caddy/assets/config/config.json <<EOF
{
  "env": "${ENV:-dev}",
  "apiBaseUrl": "${API_BASE_URL:-https://api-gateway-392205979525.europe-west1.run.app}",
  "wsUrl": "${WS_URL:-wss://api-gateway-392205979525.europe-west1.run.app}",
  "appName": "${APP_NAME:-SpeedLine Partner Dashboard}",
  "defaultLanguage": "${DEFAULT_LANGUAGE:-fr}",
  "supportedLanguages": ["fr", "en", "ar"]
}
EOF

exec caddy run --config /etc/caddy/Caddyfile --adapter caddyfile
