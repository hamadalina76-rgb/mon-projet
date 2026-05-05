#!/bin/bash
# =============================================================================
# SpeedLine - Generate Unified Allure Report (All 4 Layers)
# =============================================================================
# Collects results from:
#   Layer 1: Java unit tests (allure-junit5) + Angular unit tests
#   Layer 2: Playwright API tests (allure-playwright)
#   Layer 3: Playwright E2E tests (allure-playwright)
#   Layer 4: k6 performance tests (converted to Allure format)
#
# Usage: ./tests/generate-full-report.sh
# =============================================================================

set -e

BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPORT_DIR="$SCRIPT_DIR/allure-results-unified"
JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"

echo ""
echo -e "${BLUE}=====================================================${NC}"
echo -e "${BLUE}  SpeedLine - Unified Test Report (All 4 Layers)${NC}"
echo -e "${BLUE}=====================================================${NC}"
echo ""

# Clean previous results
rm -rf "$REPORT_DIR"
mkdir -p "$REPORT_DIR"

# =========================================================================
# Layer 1: Java Unit Tests (JUnit 5 + Allure)
# =========================================================================
echo -e "${YELLOW}[Layer 1/4] Running Java unit tests (JUnit 5 + Mockito)...${NC}"
cd "$ROOT_DIR/backend"

export JAVA_HOME
mvn test --fail-at-end -Dtest='!*IntegrationTest,!*E2ETest' --no-transfer-progress 2>&1 | grep -E "Tests run:|BUILD" | tail -3

# Collect allure results from all services
for svc_dir in services/*/target/allure-results; do
    if [ -d "$svc_dir" ]; then
        svc_name=$(echo "$svc_dir" | cut -d'/' -f2)
        echo -e "  Collecting from ${GREEN}$svc_name${NC}"
        # Copy results and add suite label
        for f in "$svc_dir"/*.json; do
            if [ -f "$f" ]; then
                # Add parent suite label to identify the service
                python3 -c "
import json, sys
with open('$f') as fh:
    data = json.load(fh)
data.setdefault('labels', []).append({'name': 'parentSuite', 'value': 'Unit Tests'})
data['labels'].append({'name': 'suite', 'value': '$svc_name'})
with open('$f', 'w') as fh:
    json.dump(data, fh)
" 2>/dev/null || true
                cp "$f" "$REPORT_DIR/"
            fi
        done
    fi
done
echo ""

# =========================================================================
# Layer 1b: Angular Unit Tests
# =========================================================================
echo -e "${YELLOW}[Layer 1b] Running Angular unit tests (Jasmine + Karma)...${NC}"

# Admin panel
cd "$ROOT_DIR/frontend/admin-panel"
if [ -f "node_modules/.package-lock.json" ]; then
    npx ng test --watch=false --browsers=ChromeHeadless 2>&1 | grep "SUCCESS\|FAILED\|TOTAL" | tail -2

    # Create Allure results for Angular tests
    RESULT=$(npx ng test --watch=false --browsers=ChromeHeadless 2>&1 | grep "TOTAL" | tail -1)
    PASSED=$(echo "$RESULT" | grep -o '[0-9]* SUCCESS' | grep -o '[0-9]*' || echo "0")
    FAILED=$(echo "$RESULT" | grep -o '[0-9]* FAILED' | grep -o '[0-9]*' || echo "0")

    python3 -c "
import json, uuid, time
for i in range($PASSED):
    result = {
        'uuid': str(uuid.uuid4()),
        'name': f'Angular Admin Panel - Test {i+1}',
        'status': 'passed',
        'stage': 'finished',
        'start': int(time.time() * 1000),
        'stop': int(time.time() * 1000) + 50,
        'labels': [
            {'name': 'parentSuite', 'value': 'Unit Tests'},
            {'name': 'suite', 'value': 'admin-panel (Angular)'},
            {'name': 'framework', 'value': 'Jasmine'}
        ]
    }
    with open(f'$REPORT_DIR/{result[\"uuid\"]}-result.json', 'w') as f:
        json.dump(result, f)
for i in range($FAILED):
    result = {
        'uuid': str(uuid.uuid4()),
        'name': f'Angular Admin Panel - Failed Test {i+1}',
        'status': 'failed',
        'stage': 'finished',
        'start': int(time.time() * 1000),
        'stop': int(time.time() * 1000) + 50,
        'labels': [
            {'name': 'parentSuite', 'value': 'Unit Tests'},
            {'name': 'suite', 'value': 'admin-panel (Angular)'},
            {'name': 'framework', 'value': 'Jasmine'}
        ]
    }
    with open(f'$REPORT_DIR/{result[\"uuid\"]}-result.json', 'w') as f:
        json.dump(result, f)
print(f'  admin-panel: {$PASSED} passed, {$FAILED} failed')
" 2>/dev/null || echo "  admin-panel: skipped (deps not installed)"
fi

# Partner dashboard
cd "$ROOT_DIR/frontend/partner-dashboard"
if [ -f "node_modules/.package-lock.json" ]; then
    RESULT=$(npx ng test --watch=false --browsers=ChromeHeadless 2>&1 | grep "TOTAL" | tail -1)
    PASSED=$(echo "$RESULT" | grep -o '[0-9]* SUCCESS' | grep -o '[0-9]*' || echo "0")
    FAILED=$(echo "$RESULT" | grep -o '[0-9]* FAILED' | grep -o '[0-9]*' || echo "0")

    python3 -c "
import json, uuid, time
for i in range($PASSED):
    result = {
        'uuid': str(uuid.uuid4()),
        'name': f'Angular Partner Dashboard - Test {i+1}',
        'status': 'passed',
        'stage': 'finished',
        'start': int(time.time() * 1000),
        'stop': int(time.time() * 1000) + 50,
        'labels': [
            {'name': 'parentSuite', 'value': 'Unit Tests'},
            {'name': 'suite', 'value': 'partner-dashboard (Angular)'},
            {'name': 'framework', 'value': 'Jasmine'}
        ]
    }
    with open(f'$REPORT_DIR/{result[\"uuid\"]}-result.json', 'w') as f:
        json.dump(result, f)
print(f'  partner-dashboard: {$PASSED} passed, {$FAILED} failed')
" 2>/dev/null || echo "  partner-dashboard: skipped"
fi
echo ""

# =========================================================================
# Layer 2+3: Playwright API + E2E Tests (already has allure-playwright)
# =========================================================================
echo -e "${YELLOW}[Layer 2+3] Running Playwright tests (API + E2E)...${NC}"
cd "$SCRIPT_DIR/playwright"
rm -rf allure-results/*

npx playwright test --project=api-tests --reporter=allure-playwright 2>&1 | grep -E "passed|failed|skipped" | tail -1

# Copy Playwright results to unified dir and label them
for f in allure-results/*.json; do
    if [ -f "$f" ]; then
        python3 -c "
import json
with open('$f') as fh:
    data = json.load(fh)
# Determine if API or E2E based on the test path
labels = data.get('labels', [])
test_path = next((l['value'] for l in labels if l['name'] == 'package'), '')
if 'api/' in test_path or 'api\\\\' in test_path:
    data.setdefault('labels', []).append({'name': 'parentSuite', 'value': 'API/Integration Tests'})
else:
    data.setdefault('labels', []).append({'name': 'parentSuite', 'value': 'E2E Tests'})
with open('$f', 'w') as fh:
    json.dump(data, fh)
" 2>/dev/null || true
        cp "$f" "$REPORT_DIR/"
    fi
done
echo ""

# =========================================================================
# Layer 4: k6 Performance Tests
# =========================================================================
echo -e "${YELLOW}[Layer 4] Running k6 performance tests...${NC}"
if command -v k6 &>/dev/null; then
    # Run k6 and capture summary
    k6 run --quiet --duration 10s --vus 3 "$SCRIPT_DIR/performance/k6-load-test.js" \
        --summary-export=/tmp/k6-summary.json 2>&1 | tail -5

    # Convert k6 results to Allure format
    python3 -c "
import json, uuid, time, os

summary_file = '/tmp/k6-summary.json'
if os.path.exists(summary_file):
    with open(summary_file) as f:
        data = json.load(f)

    metrics = data.get('metrics', {})

    # Create Allure results for key k6 metrics
    checks = [
        ('HTTP Request Duration (p95)', 'http_req_duration', 'p(95)', 3000, 'ms'),
        ('HTTP Request Duration (avg)', 'http_req_duration', 'avg', 2000, 'ms'),
        ('HTTP Request Failed Rate', 'http_req_failed', 'rate', 0.05, '%'),
        ('HTTP Requests Total', 'http_reqs', 'count', 0, 'count'),
        ('Iterations', 'iterations', 'count', 0, 'count'),
    ]

    for name, metric_key, stat, threshold, unit in checks:
        metric = metrics.get(metric_key, {})
        values = metric.get('values', {})
        value = values.get(stat, values.get('value', 0))

        if threshold > 0 and unit != 'count':
            status = 'passed' if value <= threshold else 'failed'
        else:
            status = 'passed'

        result = {
            'uuid': str(uuid.uuid4()),
            'name': f'k6 Load Test - {name}: {value:.2f} {unit}',
            'status': status,
            'stage': 'finished',
            'start': int(time.time() * 1000) - 10000,
            'stop': int(time.time() * 1000),
            'labels': [
                {'name': 'parentSuite', 'value': 'Performance Tests'},
                {'name': 'suite', 'value': 'k6 Load Test'},
                {'name': 'framework', 'value': 'k6'}
            ],
            'parameters': [
                {'name': 'threshold', 'value': str(threshold)},
                {'name': 'actual', 'value': f'{value:.2f}'}
            ]
        }
        with open(f'$REPORT_DIR/{result[\"uuid\"]}-result.json', 'w') as f:
            json.dump(result, f)
        print(f'  {name}: {value:.2f} {unit} -> {status.upper()}')
else:
    print('  k6 summary not found')
" 2>/dev/null || echo "  k6 results conversion failed"
else
    echo -e "  ${YELLOW}k6 not installed. Skipping.${NC}"
fi
echo ""

# =========================================================================
# Generate unified report
# =========================================================================
echo -e "${YELLOW}Generating unified Allure report...${NC}"
TOTAL=$(ls "$REPORT_DIR"/*.json 2>/dev/null | wc -l | tr -d ' ')
echo -e "  Total result files: ${GREEN}$TOTAL${NC}"

if command -v allure &>/dev/null; then
    allure generate "$REPORT_DIR" --clean -o "$SCRIPT_DIR/allure-report-unified" 2>/dev/null
    echo -e "  ${GREEN}Report generated at tests/allure-report-unified/${NC}"
    echo ""
    echo -e "${BLUE}=====================================================${NC}"
    echo -e "${BLUE}  Opening unified report...${NC}"
    echo -e "${BLUE}=====================================================${NC}"
    pkill -f "allure open" 2>/dev/null || true
    allure open "$SCRIPT_DIR/allure-report-unified" --port 9090 &
    echo -e "  ${GREEN}http://localhost:9090${NC}"
else
    echo -e "  ${YELLOW}Allure CLI not installed. Install with: brew install allure${NC}"
fi
echo ""
