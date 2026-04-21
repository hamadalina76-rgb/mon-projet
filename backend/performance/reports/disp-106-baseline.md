# DISP-106 Baseline Report

Status: `PENDING_EXECUTION`

## Test scope

- Flow: `ORDER_CREATED -> dispatch cycle -> DISPATCH_ASSIGNED`
- Load: `500 order creations / 1 minute`
- SLA: `all orders dispatched in < 5 minutes`

## Percentiles (dispatch time)

- p50: `TBD`
- p95: `TBD`
- p99: `TBD`

## Capacity outcome

- Throughput: `TBD orders/min`
- Success rate: `TBD%`
- Orders dispatched < 5 min: `TBD%`

## Resilience outcome

- Redis outage: `TBD`
- Pub/Sub outage: `TBD`
- Solver outage/fallback: `TBD`

## Notes

Fill this report after running:

```bash
mvn -pl performance/gatling gatling:test
python backend/performance/scripts/summarize_gatling.py ...
```
