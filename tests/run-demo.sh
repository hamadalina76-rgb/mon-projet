#!/bin/bash
# =============================================================================
# SpeedLine Test Robot - One-Command Demo
# =============================================================================
# Prerequisites: Docker Desktop running, Node.js 18+, Git
#
# Usage:
#   chmod +x tests/run-demo.sh
#   ./tests/run-demo.sh
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo -e "${BLUE}=====================================================${NC}"
echo -e "${BLUE}  SpeedLine - Robot de Tests Automatisés${NC}"
echo -e "${BLUE}  Projet PFE - ISET Nabeul / DevWise${NC}"
echo -e "${BLUE}=====================================================${NC}"
echo ""

# Get the root directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$SCRIPT_DIR"

# =============================================================================
# Step 1: Check prerequisites
# =============================================================================
echo -e "${YELLOW}[1/7] Checking prerequisites...${NC}"

if ! command -v docker &>/dev/null; then
    echo -e "${RED}ERROR: Docker is not installed. Install Docker Desktop first.${NC}"
    exit 1
fi

if ! docker info &>/dev/null; then
    echo -e "${RED}ERROR: Docker is not running. Start Docker Desktop first.${NC}"
    exit 1
fi

if ! command -v node &>/dev/null; then
    echo -e "${RED}ERROR: Node.js is not installed. Install Node.js 18+ first.${NC}"
    exit 1
fi

echo -e "${GREEN}  Docker: $(docker --version | cut -d' ' -f3)${NC}"
echo -e "${GREEN}  Node.js: $(node -v)${NC}"
echo ""

# =============================================================================
# Step 2: Install npm dependencies
# =============================================================================
echo -e "${YELLOW}[2/7] Installing test dependencies...${NC}"
cd "$SCRIPT_DIR/playwright"
npm ci --silent 2>/dev/null || npm install --silent
npx playwright install chromium --with-deps 2>/dev/null || npx playwright install chromium
cd "$SCRIPT_DIR/seed"
npm ci --silent 2>/dev/null || npm install --silent
cd "$SCRIPT_DIR"
echo -e "${GREEN}  Dependencies installed.${NC}"
echo ""

# =============================================================================
# Step 3: Start Docker environment
# =============================================================================
echo -e "${YELLOW}[3/7] Starting Docker environment (infra + 12 services + gateway)...${NC}"
echo -e "  This takes 2-5 minutes on first run (building Java services)."
echo ""

docker compose -f docker-compose.test.yml down -v --remove-orphans 2>/dev/null || true
docker compose -f docker-compose.test.yml up -d --build 2>&1 | grep -E "Started|Built|Created|Error" | head -20

echo ""
echo -e "${YELLOW}  Waiting for services to start...${NC}"

# Wait for Postgres
echo -n "  Postgres: "
until docker inspect sl-test-postgres --format='{{.State.Health.Status}}' 2>/dev/null | grep -q healthy; do
    echo -n "."
    sleep 3
done
echo -e " ${GREEN}UP${NC}"

# Wait for auth-service (critical path)
echo -n "  Auth Service: "
until docker inspect sl-test-auth --format='{{.State.Health.Status}}' 2>/dev/null | grep -q healthy; do
    echo -n "."
    sleep 5
done
echo -e " ${GREEN}UP${NC}"

# Wait for gateway
echo -n "  API Gateway: "
until curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; do
    echo -n "."
    sleep 5
done
echo -e " ${GREEN}UP${NC}"

# Wait a bit more for other services
echo -n "  Other services: "
sleep 15
echo -n "."
sleep 15
echo -e " ${GREEN}ready${NC}"

# Show running services
echo ""
echo -e "${GREEN}  Running containers:${NC}"
docker ps --format "    {{.Names}}: {{.Status}}" | grep sl-test | sort
echo ""

# =============================================================================
# Step 4: Seed test data
# =============================================================================
echo -e "${YELLOW}[4/7] Seeding test database...${NC}"
cd "$SCRIPT_DIR/seed"
DB_HOST=localhost DB_PORT=5433 DB_USER=postgres DB_PASSWORD=postgres123 \
    AUTH_DB_NAME=speedline_auth \
    node seed-test-data.js 2>&1 | grep -E "Seeded|Error|complete"
cd "$SCRIPT_DIR"
echo ""

# =============================================================================
# Step 5: Run Playwright API tests
# =============================================================================
echo -e "${YELLOW}[5/7] Running API tests (Playwright)...${NC}"
echo ""
cd "$SCRIPT_DIR/playwright"

# Clear old results
rm -rf allure-results/* test-results/* playwright-report/*

# Run tests
npx playwright test --project=api-tests --reporter=list,allure-playwright 2>&1 | tail -5

echo ""

# =============================================================================
# Step 6: Run k6 smoke test (if installed)
# =============================================================================
echo -e "${YELLOW}[6/7] Running performance smoke test...${NC}"
if command -v k6 &>/dev/null; then
    k6 run --quiet --duration 10s --vus 5 "$SCRIPT_DIR/performance/k6-load-test.js" 2>&1 | tail -10
else
    echo -e "  ${YELLOW}k6 not installed. Skipping performance tests.${NC}"
    echo "  Install with: brew install k6 (macOS) or choco install k6 (Windows)"
fi
echo ""

# =============================================================================
# Step 7: Generate Allure report
# =============================================================================
echo -e "${YELLOW}[7/7] Generating Allure report...${NC}"
if command -v allure &>/dev/null; then
    allure generate allure-results --clean -o allure-report 2>/dev/null
    echo -e "${GREEN}  Report generated in playwright/allure-report/${NC}"
    echo ""
    echo -e "${BLUE}=====================================================${NC}"
    echo -e "${BLUE}  DEMO COMPLETE${NC}"
    echo -e "${BLUE}=====================================================${NC}"
    echo ""
    echo -e "  ${GREEN}Opening Allure report in browser...${NC}"
    echo ""
    allure open allure-report --port 9090 &
    echo -e "  Report URL: ${GREEN}http://localhost:9090${NC}"
else
    echo -e "  ${YELLOW}Allure CLI not installed. Using Playwright HTML report.${NC}"
    npx playwright show-report &
    echo ""
    echo -e "${BLUE}=====================================================${NC}"
    echo -e "${BLUE}  DEMO COMPLETE${NC}"
    echo -e "${BLUE}=====================================================${NC}"
fi

echo ""
echo -e "  To stop everything:  ${YELLOW}cd tests && docker compose -f docker-compose.test.yml down${NC}"
echo -e "  To re-run tests:     ${YELLOW}cd tests/playwright && npm test${NC}"
echo ""
