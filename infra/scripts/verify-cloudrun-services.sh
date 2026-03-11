#!/usr/bin/env bash
set -u

PROJECT_NUMBER="${PROJECT_NUMBER:-392205979525}"
REGION="${REGION:-europe-west1}"
BASE_DOMAIN="${BASE_DOMAIN:-${PROJECT_NUMBER}.${REGION}.run.app}"

CONFIG_SERVER_URL="${CONFIG_SERVER_URL:-https://config-server-${BASE_DOMAIN}}"
API_GATEWAY_URL="${API_GATEWAY_URL:-https://api-gateway-${BASE_DOMAIN}}"
CONFIG_APP_NAME="${CONFIG_APP_NAME:-api-gateway}"
CONFIG_PROFILE="${CONFIG_PROFILE:-dev}"

SERVICES=(
  "config-server"
  "api-gateway"
  "eureka-server"
  "auth-service"
  "user-service"
  "partner-service"
  "notification-service"
  "location-service"
)

FRONTENDS=(
  "partner-dashboard"
  "admin-panel"
  "courier-app"
  "customer-app"
)

PASS_COUNT=0
FAIL_COUNT=0

print_header() {
  echo "=============================================="
  echo "Speedline Cloud Run Verification"
  echo "Project Number : ${PROJECT_NUMBER}"
  echo "Region         : ${REGION}"
  echo "Base Domain    : ${BASE_DOMAIN}"
  echo "=============================================="
}

record_result() {
  local ok="$1"
  local label="$2"
  local details="$3"

  if [[ "$ok" == "true" ]]; then
    PASS_COUNT=$((PASS_COUNT + 1))
    echo "[PASS] ${label} - ${details}"
  else
    FAIL_COUNT=$((FAIL_COUNT + 1))
    echo "[FAIL] ${label} - ${details}"
  fi
}

check_http_endpoint() {
  local label="$1"
  local url="$2"

  local status
  status=$(curl -s -o /tmp/verify_body.$$ -w "%{http_code}" "$url")

  if [[ "$status" =~ ^2 ]]; then
    record_result "true" "$label" "HTTP ${status}"
  else
    local body
    body=$(cat /tmp/verify_body.$$ 2>/dev/null || true)
    record_result "false" "$label" "HTTP ${status} - ${body}"
  fi

  rm -f /tmp/verify_body.$$
}

check_config_server_health() {
  local url="${CONFIG_SERVER_URL}/actuator/health"
  local body
  body=$(curl -s "$url")

  if echo "$body" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'; then
    record_result "true" "Config Server health" "$url"
  else
    record_result "false" "Config Server health" "$url => $body"
  fi
}

check_config_retrieval() {
  local url="${CONFIG_SERVER_URL}/${CONFIG_APP_NAME}/${CONFIG_PROFILE}"
  local body
  local status

  body=$(curl -s -o /tmp/config_payload.$$ -w "%{http_code}" "$url")
  status="$body"

  if [[ "$status" =~ ^2 ]] && grep -q '"name"[[:space:]]*:[[:space:]]*"' /tmp/config_payload.$$; then
    record_result "true" "Config retrieval" "$url"
  else
    local payload
    payload=$(cat /tmp/config_payload.$$ 2>/dev/null || true)
    record_result "false" "Config retrieval" "$url => HTTP ${status} - ${payload}"
  fi

  rm -f /tmp/config_payload.$$
}

check_microservices_connectivity() {
  for service in "${SERVICES[@]}"; do
    local service_url="https://${service}-${BASE_DOMAIN}/actuator/health"
    check_http_endpoint "Service health (${service})" "$service_url"
  done
}

check_frontend_connectivity() {
  for frontend in "${FRONTENDS[@]}"; do
    local frontend_url="https://${frontend}-${BASE_DOMAIN}"
    check_http_endpoint "Frontend index (${frontend})" "$frontend_url"
  done
}

check_api_gateway_smoke() {
  local url="${API_GATEWAY_URL}/actuator/health"
  check_http_endpoint "API Gateway health" "$url"
}

print_summary() {
  echo "=============================================="
  echo "Verification summary: ${PASS_COUNT} passed / ${FAIL_COUNT} failed"
  echo "=============================================="

  if [[ "$FAIL_COUNT" -gt 0 ]]; then
    return 1
  fi

  return 0
}

main() {
  print_header
  check_config_server_health
  check_config_retrieval
  check_api_gateway_smoke
  check_microservices_connectivity
  check_frontend_connectivity
  print_summary
}

main
