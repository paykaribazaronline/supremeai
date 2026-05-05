# Load Testing with k6

## Prerequisites

Install k6: https://k6.io/docs/getting-started/installation/

## Run Load Test

```bash
k6 run load-test.js
```

## Test Configuration

- Ramps up to 100 concurrent users over 30 seconds
- Maintains 100 users for 1 minute
- Ramps down over 30 seconds
- Tests health, metrics, and performance dashboard endpoints
- Thresholds: 95% requests under 500ms, error rate <10%
