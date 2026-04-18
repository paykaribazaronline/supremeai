# Server Status & Health Monitoring

## Overview

SupremeAI now includes comprehensive server health monitoring with detailed metrics on uptime, resource usage, and performance.

## Quick Test

```bash
# Quick status (all 3 endpoints below work)
curl http://localhost:8080/
curl http://localhost:8080/api/status/summary
curl http://localhost:8080/actuator/health
```

## Available Endpoints

### 1. **Comprehensive Health Check** (Detailed Metrics)

```
GET /api/status/health
```

**Response Example:**

```json
{
  "status": "UP",
  "version": "3.5",
  "timestamp": 1712079600000,
  "serviceTime": "2026-04-02T10:30:00",
  "uptime": "5d 12h 34m 21s",
  "uptimeMs": 475461000,
  "totalRequests": 15420,
  "totalErrors": 23,
  "errorRate": 0.15,
  "activeConnections": 12,
  "heap": {
    "usedMB": 356,
    "maxMB": 1024,
    "usagePercent": 35
  },
  "processCpuUsage": "8.42%",
  "systemCpuUsage": "12.56%",
  "availableProcessors": 8,
  "javaVersion": "17.0.1",
  "javaVendor": "Eclipse Adoptium",
  "osName": "Linux",
  "osVersion": "5.15.0-1234-gcp"
}
```

### 2. **Status Summary** (Lightweight)

```
GET /api/status/summary
```

**Response Example:**

```json
{
  "status": "UP",
  "version": "3.5",
  "message": "🚀 SupremeAI Cloud Server is Running!",
  "timestamp": 1712079600000
}
```

### 3. **Performance Metrics Only**

```
GET /api/status/performance
```

**Response Example:**

```json
{
  "status": "UP",
  "uptime": "5d 12h 34m 21s",
  "heap": {
    "usedMB": 356,
    "maxMB": 1024,
    "usagePercent": 35
  },
  "cpu": {
    "processCpuUsage": "8.42%",
    "systemCpuUsage": "12.56%"
  },
  "requests": {
    "total": 15420,
    "errors": 23,
    "errorRate": 0.15
  }
}
```

### 4. **Home Endpoint** (Links to All Status Endpoints)

```
GET /
```

**Response Example:**

```json
{
  "message": "🚀 SupremeAI Cloud Server is Running!",
  "version": "3.5",
  "status": "UP",
  "timestamp": 1712079600000,
  "healthCheck": "/api/status/health",
  "statusSummary": "/api/status/summary",
  "performance": "/api/status/performance"
}
```

## Monitoring Strategy

| Use Case | Endpoint | Best For |
|----------|----------|----------|
| **Cloud Health Check** | `/api/status/summary` | Fast, lightweight checks |
| **Monitoring Dashboard** | `/api/status/performance` | Performance tracking |
| **Debug/Deep Inspection** | `/api/status/health` | Full diagnostics |
| **Simple Confirmation** | `/` | Is server alive? |

## Key Metrics Explained

### Uptime

- Format: `5d 12h 34m 21s` (days, hours, minutes, seconds)
- Useful for: SLA tracking, maintenance windows, stability assessment

### Memory Usage (Heap)

- **usedMB**: Current heap memory in use
- **maxMB**: Maximum heap allocated to JVM
- **usagePercent**: Percentage of max heap in use
- Alert threshold: >90%

### CPU Usage

- **processCpuUsage**: CPU used by SupremeAI process
- **systemCpuUsage**: Overall system CPU
- Alert threshold: >80%

### Request Metrics

- **totalRequests**: All HTTP requests handled since startup
- **totalErrors**: Failed requests (5xx, exceptions, etc.)
- **errorRate**: Percentage of errors
- Alert threshold: errorRate > 1%

## Monitoring with External Tools

### Example: Prometheus Monitoring

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'supremeai'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/api/status/health'
    scrape_interval: 30s
```

### Example: Health Check Script (Bash)

```bash
#!/bin/bash

ENDPOINT="http://localhost:8080/api/status/performance"
RESPONSE=$(curl -s $ENDPOINT)
STATUS=$(echo $RESPONSE | jq -r '.status')

if [ "$STATUS" = "UP" ]; then
  echo "✅ Server is healthy"
  echo $RESPONSE | jq .
else
  echo "❌ Server is DOWN"
  exit 1
fi
```

### Example: Docker Health Check

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/api/status/summary || exit 1
```

## Benefits of This Approach

✅ **Live Monitoring** - Know server state anytime  
✅ **Performance Tracking** - Memory, CPU, request metrics  
✅ **Uptime Verification** - Confirm server hasn't restarted unexpectedly  
✅ **Error Detection** - Spot problems early via error rates  
✅ **SLA Compliance** - Track availability for reporting  
✅ **Load Balancing** - Health checks for auto-scaling  
✅ **Debugging** - Full JVM and OS information  

## Integration with Cloud Platforms

### Google Cloud Run (Current)

Cloud Run uses `/api/status/summary` for health checks

### AWS Lambda

Can use `/api/status/performance` for custom metrics

### Kubernetes

```yaml
livenessProbe:
  httpGet:
    path: /api/status/health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
```

---

**Version:** 3.5  
**Last Updated:** April 2, 2026
