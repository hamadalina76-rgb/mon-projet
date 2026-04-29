# SpeedLine - Robot de Tests Automatisés

> Projet de Fin d'Études — ISET Nabeul / DevWise

Robot de tests automatisés couvrant l'intégralité de la pyramide de tests pour les projets **Speed Line** et **Delivery App**.

---

## Quick Start (One Command)

**Prerequisites:** Docker Desktop (running) + Node.js 18+

```bash
# macOS / Linux
./tests/run-demo.sh

# Windows
tests\run-demo.bat
```

This single command will:
1. Install dependencies
2. Build & start 12 microservices + API Gateway in Docker
3. Seed test database
4. Run 104 API tests through the gateway
5. Run performance tests
6. Open the Allure report in your browser

---

## Architecture

```
                    ┌─────────────────────────────────────────┐
                    │           ALLURE REPORT (:9090)          │
                    └─────────────────┬───────────────────────┘
                                      │ results
                    ┌─────────────────┴───────────────────────┐
                    │         PLAYWRIGHT TEST RUNNER            │
                    │  104 API │ 66 E2E │ 16 Security │ 8 WS   │
                    └─────────────────┬───────────────────────┘
                                      │ HTTP requests
                    ┌─────────────────┴───────────────────────┐
                    │          API GATEWAY (:8080)              │
                    │       Routes → StripPrefix → Services    │
                    └─────────────────┬───────────────────────┘
                                      │
        ┌──────────┬──────────┬───────┴───────┬──────────┬──────────┐
        │          │          │               │          │          │
   ┌────┴───┐ ┌───┴────┐ ┌───┴────┐   ┌─────┴──┐ ┌────┴───┐ ┌───┴────┐
   │  Auth  │ │  User  │ │Partner │   │ Order  │ │Delivery│ │Payment │
   │Service │ │Service │ │Service │   │Service │ │Service │ │Service │
   └────┬───┘ └───┬────┘ └───┬────┘   └────┬───┘ └────┬───┘ └───┬────┘
        │         │          │              │          │          │
        └─────────┴──────────┴──────┬───────┴──────────┴──────────┘
                                    │
                    ┌───────────────┬┴──────────────┐
                    │  PostgreSQL   │    Redis       │
                    │  (PostGIS)    │   (Cache)      │
                    └───────────────┴───────────────┘
```

### Testing Pyramid

```
          ┌───────────┐
          │  Perf (3) │  ← k6 load/stress/spike
         ─┤           ├─
        ┌─┴───────────┴─┐
        │   E2E (66+30) │  ← Playwright UI + Flutter Patrol
       ─┤               ├─
      ┌─┴───────────────┴─┐
      │   API/Integ (104)  │  ← Playwright API through Gateway
     ─┤                    ├─
    ┌─┴────────────────────┴─┐
    │    Unit Tests (450+)    │  ← JUnit 5 + Mockito + Jasmine
    └────────────────────────┘
```

---

## Project Structure

```
tests/
├── run-demo.sh              # One-command demo (macOS/Linux)
├── run-demo.bat              # One-command demo (Windows)
├── docker-compose.test.yml   # Full Docker environment (12 services + gateway)
├── Dockerfile.service        # Universal service Dockerfile
├── Dockerfile.gateway        # Gateway Dockerfile
│
├── playwright/               # Playwright tests (API + E2E + Security)
│   ├── playwright.config.ts  # Test configuration
│   ├── .env                  # Environment variables
│   ├── package.json          # Dependencies
│   │
│   ├── api/                  # API tests (104 tests)
│   │   ├── auth.api.spec.ts           # Auth: login, profile, refresh, logout
│   │   ├── orders.api.spec.ts         # Orders: CRUD, pagination, filters
│   │   ├── partners.api.spec.ts       # Partners: list, detail, menu, nearby
│   │   ├── delivery.api.spec.ts       # Delivery: list, tracking, schedule
│   │   ├── payments.api.spec.ts       # Payments: list, detail, wallets
│   │   ├── users.api.spec.ts          # Users: customers, couriers, admins
│   │   ├── promotions.api.spec.ts     # Promotions: validate, dashboard
│   │   ├── notifications.api.spec.ts  # Notifications: list, read, push
│   │   ├── reviews.api.spec.ts        # Reviews: create, ratings
│   │   ├── support.api.spec.ts        # Support: tickets CRUD
│   │   ├── analytics.api.spec.ts      # Analytics: metrics, reports
│   │   ├── location.api.spec.ts       # Location: zones, distance
│   │   ├── security.api.spec.ts       # Security: SQLi, XSS, auth bypass
│   │   ├── contracts.api.spec.ts      # Contract: response schema validation
│   │   ├── smoke.api.spec.ts          # Smoke: quick health checks
│   │   ├── websocket.api.spec.ts      # WebSocket: tracking, chat, dispatch
│   │   └── e2e-order-flow.api.spec.ts # E2E: full order lifecycle
│   │
│   ├── e2e/                  # UI E2E tests (66 tests)
│   │   ├── admin-panel/               # Admin panel tests
│   │   │   ├── auth.spec.ts           # Login/logout
│   │   │   ├── dashboard.spec.ts      # Navigation
│   │   │   ├── orders.spec.ts         # Order management
│   │   │   ├── partners.spec.ts       # Partner management
│   │   │   ├── users.spec.ts          # User management
│   │   │   ├── visual-regression.spec.ts  # Screenshot comparison
│   │   │   └── accessibility.spec.ts  # WCAG 2.1 compliance
│   │   └── partner-dashboard/         # Partner dashboard tests
│   │       ├── auth.spec.ts
│   │       ├── dashboard.spec.ts
│   │       ├── orders.spec.ts
│   │       ├── menu.spec.ts
│   │       ├── visual-regression.spec.ts
│   │       └── accessibility.spec.ts
│   │
│   ├── pages/                # Page Object Models
│   │   ├── admin/
│   │   └── partner/
│   │
│   ├── helpers/              # Shared utilities
│   │   ├── api-client.ts     # Authenticated HTTP client
│   │   └── test-data.ts      # Test accounts & fixtures
│   │
│   └── fixtures/             # Auth setup
│       ├── admin-auth.setup.ts
│       └── partner-auth.setup.ts
│
├── performance/              # k6 performance tests
│   ├── k6-load-test.js       # Load test (10→50 users, 5 min)
│   ├── k6-stress-test.js     # Stress test (up to 300 users)
│   └── k6-spike-test.js      # Spike test (sudden 200 users)
│
├── seed/                     # Database seeder
│   ├── seed-test-data.js     # Seeds 4 test accounts with bcrypt
│   └── package.json
│
├── ci/                       # CI/CD pipeline
│   └── .gitlab-ci-tests.yml  # GitLab CI integration
│
├── mobile/                   # Mobile test documentation
│   └── patrol-setup-guide.md
│
└── docs/                     # Documentation (PFE deliverables)
    ├── TEST-PLAN.md          # Formal test plan (160 scenarios)
    └── MANUAL-TEST-SCENARIOS.md  # 109 manual QA scenarios
```

---

## Running Tests

### Full Demo (recommended)

```bash
./tests/run-demo.sh          # macOS/Linux
tests\run-demo.bat            # Windows
```

### Individual Test Suites

```bash
cd tests/playwright

# All API tests
npm test

# Specific suites
npm run test:api              # API tests only
npm run test:smoke            # Quick health check
npm run test:security         # Security tests
npm run test:contracts        # API contract validation
npm run test:admin            # Admin panel E2E
npm run test:partner          # Partner dashboard E2E
npm run test:visual           # Visual regression
npm run test:a11y             # Accessibility (WCAG)

# Debug mode (opens browser)
npm run test:debug

# Generate Allure report
npm run report:allure
```

### Unit Tests (Java backend)

```bash
cd backend
export JAVA_HOME=/path/to/jdk17

# All services
mvn test --fail-at-end

# Specific service
mvn test -pl services/auth-service
mvn test -pl services/order-service
```

### Unit Tests (Angular frontend)

```bash
# Admin panel (26 tests)
cd frontend/admin-panel
npx ng test --watch=false --browsers=ChromeHeadless

# Partner dashboard (8 tests)
cd frontend/partner-dashboard
npx ng test --watch=false --browsers=ChromeHeadless
```

### Performance Tests

```bash
# Load test
k6 run tests/performance/k6-load-test.js

# Stress test
k6 run tests/performance/k6-stress-test.js

# Spike test
k6 run tests/performance/k6-spike-test.js
```

### Mobile Tests (Flutter)

```bash
# Customer app
cd frontend/customer_app
flutter pub get
flutter test integration_test/app_test.dart

# Courier app
cd frontend/courier_app
flutter pub get
flutter test integration_test/app_test.dart

# With Patrol (native dialog handling)
patrol test --target integration_test/patrol_test.dart
```

---

## Docker Environment

### Start

```bash
cd tests
docker compose -f docker-compose.test.yml up -d --build
```

### Check Status

```bash
docker ps --format "table {{.Names}}\t{{.Status}}" | grep sl-test
```

### View Logs

```bash
docker logs sl-test-auth       # Auth service
docker logs sl-test-gateway    # API Gateway
docker logs sl-test-order      # Order service
```

### Stop & Clean

```bash
docker compose -f docker-compose.test.yml down -v
```

### Services & Ports

| Container | Service | Internal Port | External Port |
|-----------|---------|---------------|---------------|
| sl-test-gateway | API Gateway | 8080 | **8080** |
| sl-test-postgres | PostgreSQL | 5432 | 5433 |
| sl-test-redis | Redis | 6379 | - |
| sl-test-auth | Auth Service | 8080 | - |
| sl-test-user | User Service | 8080 | - |
| sl-test-partner | Partner Service | 8080 | - |
| sl-test-order | Order Service | 8080 | - |
| sl-test-delivery | Delivery Service | 8080 | - |
| sl-test-payment | Payment Service | 8080 | - |
| sl-test-location | Location Service | 8080 | - |
| sl-test-promotion | Promotion Service | 8080 | - |
| sl-test-support | Support Service | 8080 | - |
| sl-test-analytics | Analytics Service | 8080 | - |
| sl-test-pubsub | Pub/Sub Emulator | 8085 | - |

All tests hit `localhost:8080` (gateway) which routes to the correct service.

---

## Test Accounts

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@speedline-test.com | TestAdmin123! |
| Partner | partner@speedline-test.com | TestPartner123! |
| Customer | customer@speedline-test.com | TestCustomer123! |
| Courier | courier@speedline-test.com | TestCourier123! |

Created by: `cd tests/seed && node seed-test-data.js`

---

## Test Coverage Summary

| Layer | Tests | Tool | Status |
|-------|-------|------|--------|
| Unit (Backend) | 450+ | JUnit 5 + Mockito | All 12 services |
| Unit (Frontend) | 34 | Jasmine + Karma | Both Angular apps |
| API | 104 | Playwright | All endpoints |
| E2E Web | 66 | Playwright | Admin + Partner |
| E2E Mobile | ~30 | Flutter + Patrol | Customer + Courier |
| Performance | 3 | k6 | Load, Stress, Spike |
| Security | 16 | Playwright | SQLi, XSS, Auth |
| Accessibility | 13 | axe-core | WCAG 2.1 |
| Visual | 16 | Playwright | Screenshot diff |
| Contracts | 9 | Playwright | Schema validation |
| WebSocket | 8 | ws + Playwright | Tracking, Chat |
| Smoke | 8 | Playwright | Health checks |
| **Total** | **~750+** | | |

---

## CI/CD Integration

The test pipeline integrates into GitLab CI. Add to your `.gitlab-ci.yml`:

```yaml
include:
  - local: 'tests/ci/.gitlab-ci-tests.yml'

stages:
  - build
  - test        # ← test robot runs here
  - deploy
  - report      # ← Allure report generated here
```

Pipeline stages:
1. **Backend unit tests** + JaCoCo coverage
2. **Frontend unit tests** + Istanbul coverage
3. **API tests** via Playwright
4. **Web E2E tests** via Playwright
5. **Mobile tests** via Flutter
6. **Performance tests** via k6 (staging only)
7. **Allure report** generation

---

## Reporting

### Allure (recommended)

```bash
# Install
brew install allure          # macOS
choco install allure         # Windows

# Generate & open
cd tests/playwright
allure generate allure-results --clean -o allure-report
allure open allure-report
```

Features: interactive dashboard, test history, categories, timeline, trend charts.

### Playwright HTML

```bash
cd tests/playwright
npx playwright show-report
```

---

## Troubleshooting

### Docker services not starting

```bash
# Check logs
docker logs sl-test-auth

# Common fix: restart everything
docker compose -f docker-compose.test.yml down -v
docker compose -f docker-compose.test.yml up -d --build
```

### Port 8080 already in use

```bash
# Find what's using it
lsof -i :8080    # macOS/Linux
netstat -ano | findstr :8080  # Windows

# Kill it or change the port in docker-compose.test.yml
```

### Tests failing with "Login failed"

```bash
# Re-seed the database
cd tests/seed
DB_HOST=localhost DB_PORT=5433 DB_USER=postgres DB_PASSWORD=postgres123 \
    AUTH_DB_NAME=speedline_auth node seed-test-data.js
```

### Java 25 installed but need Java 17

```bash
# macOS
brew install openjdk@17
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

# Windows
# Download JDK 17 from https://adoptium.net/
```

---

## Technologies

| Category | Tool | Version |
|----------|------|---------|
| API Testing | Playwright | 1.49 |
| Unit Testing (Java) | JUnit 5 + Mockito | 5.x |
| Unit Testing (Angular) | Jasmine + Karma | 4.x |
| Mobile Testing | Flutter integration_test + Patrol | 3.13 |
| Performance | k6 | 1.7 |
| Reporting | Allure | 2.27 |
| Accessibility | axe-core | 4.11 |
| Code Coverage (Java) | JaCoCo | 0.8.12 |
| Code Coverage (JS) | Istanbul | built-in |
| Containers | Docker Compose | v2 |
| CI/CD | GitLab CI | - |

---

## Authors

- **[Nom Stagiaire 1]** — ISET Nabeul
- **[Nom Stagiaire 2]** — ISET Nabeul

**Encadrant entreprise:** M. Mahmoud Slama — DevWise, Nabeul

**Année universitaire:** 2025–2026
