# DISP-106 Performance & E2E Artifacts

This folder contains load-testing assets for dispatch.

## Gatling module

- Module: `backend/performance/gatling`
- Main simulation: `com.speedline.performance.DispatchLoadSimulation`

Run:

```bash
cd backend
mvn -pl performance/gatling gatling:test \
  -DbaseUrl=http://localhost:8080 \
  -Dorders=500 \
  -DwindowSeconds=60 \
  -DtrackingTimeoutSeconds=300
```

Optional auth:

```bash
-DauthToken=<jwt> -DuserId=10001
```

## Report generation

Generate machine-readable summary:

```bash
python backend/performance/scripts/summarize_gatling.py \
  backend/performance/gatling/target/gatling \
  backend/performance/reports/disp-106-summary.json \
  backend/performance/reports/disp-106-summary.md
```

## Deliverables

- E2E tests in `delivery-service` (DISP-106)
- Gatling HTML report in `backend/performance/gatling/target/gatling/*`
- Summary files:
  - `backend/performance/reports/disp-106-summary.json`
  - `backend/performance/reports/disp-106-summary.md`
