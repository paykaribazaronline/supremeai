# Load Testing Guide / লোড টেস্টিং গাইড

## Quick Start / দ্রুত শুরু

### Install k6 / k6 ইনস্টল করুন

```bash
# Windows (Chocolatey)
choco install k6

# Or download from: https://k6.io/docs/get-started/installation/

# Verify installation
k6 version
```

### Run Load Test / লোড টেস্ট চালান

```bash
# Start your application first
./gradlew bootRun

# In another terminal - Basic test
k6 run load-test.js

# Custom parameters
k6 run --vus 100 --duration 2m load-test.js

# With environment variables
BASE_URL=https://supremeai-lhlwyikwlq-uc.a.run.app k6 run load-test.js

# With authentication token
AUTH_TOKEN=your-jwt-token k6 run load-test.js
```

## Test Scenarios / টেস্ট দৃশ্য

### 1. Health Check & Metrics

- Checks `/actuator/health`
- Monitors memory usage via `/actuator/metrics/jvm.memory.used`
- Detects memory leaks over time

### 2. Authentication Load

- Tests Firebase login endpoint
- Measures auth response time
- Checks for auth bottlenecks

### 3. AI Orchestration

- Sends 5 different natural prompts
- Randomly selects AI provider (groq, openai, anthropic, ollama)
- Tracks latency for each provider

### 4. Dashboard Performance

- Loads performance dashboard
- Checks HTML size and load time
- Validates content

### 5. API Key Management

- Tests admin API key endpoints
- Monitors for unauthorized access patterns

## What to Watch For / কী দেখবেন

### Memory Leaks / মেমরি লিক

```
✅ Normal: Memory stabilizes after 2-3 minutes
❌ Leak: Memory continuously grows without dropping
```

Check: `memory_usage_check` counter in results

### Latency Issues / লেটেন্সি সমস্যা

```
✅ Good: p95 < 2000ms
⚠️ Warning: p95 2000-5000ms
❌ Bad: p95 > 5000ms
```

Check: `latency_trend` metric

### Error Rate / ত্রটির হার

```
✅ Acceptable: < 5% errors
⚠️ Warning: 5-10% errors
❌ Critical: > 10% errors
```

Check: `error_rate` metric

## Results Interpretation / ফলাফল ব্যাখ্যা

### JSON Report / JSON রিপোর্ট

```json
{
  "metrics": {
    "http_req_duration": {
      "avg": 1234.56,
      "p95": 2345.67,
      "max": 5678.90
    },
    "error_rate": {
      "rate": 0.03
    }
  }
}
```

### CSV Export / CSV এক্সপোর্ট

Opens `load-test-metrics.csv` after test:

```
metric,value
total_requests,1234
failed_requests,37
p95_response_time,2345.67
avg_response_time,1234.56
error_rate,0.03
```

## Continuous Monitoring / কন্টিনিউয়াস মনিটরিং

### Add to CI/CD

```yaml
# .github/workflows/load-test.yml
- name: Run Load Test
  run: |
    k6 run --vus 50 --duration 1m load-test.js \
      --out json=results.json
```

### Performance Budget

Add to `build.gradle.kts`:

```kotlin
tasks.register("loadTest") {
    doLast {
        exec {
            commandLine("k6", "run", "load-test.js", 
                "--vus=50", "--duration=1m")
        }
    }
}
```

## Troubleshooting / সমস্যা সমাধান

### Issue: Connection Refused

```bash
# Check if app is running
curl http://localhost:8080/actuator/health

# Check port
netstat -an | findstr 8080
```

### Issue: High Memory Usage

```bash
# Check JVM memory
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# If > 3GB, check for leaks:
# 1. Database connection leaks
# 2. Cache not evicting
# 3. Thread pools not shrinking
```

### Issue: High Error Rate

```bash
# Check logs
tail -f logs/supremeai.log

# Common causes:
# 1. AI provider API limits
# 2. Database timeouts
# 3. Redis connection issues
```

## Bengali Quick Reference / বাংলা দ্রুত রেফারেন্স

**লোড টেস্টিং কেন দরকার?**

- সিস্টেম কতটা লোড নিতে পারে তা জানার জন্য
- মেমরি লিক বা পারফরম্যান্স সমস্যা খুঁজে বের করতে

**কখন টেস্ট করবেন?**

- নতুন ফিচার যোগ করার পর
- প্রোডাকশনে ডিপ্লয় করার আগে
- প্রতি সপ্তাহে রুটিন চেক হিসেবে

**কী কী মেপে দেখবেন?**

- ✅ Response time < 2s (95% requests)
- ✅ Error rate < 5%
- ✅ Memory stable (no continuous growth)
- ✅ Concurrent users: 50+ without issues

---

**Test Duration:** ~7 minutes  
**Virtual Users:** Up to 100  
**Request Types:** Health, Auth, AI Generation, Dashboard, API Keys  
**Output:** JSON + CSV + Console Summary
