# Safe Infinite Healing Loop Implementation Guide

## Overview

This implementation provides a **production-grade self-healing GitHub Actions system** that:

✅ Detects workflow failures automatically  
✅ Analyzes error logs  
✅ Generates code fixes using AI  
✅ Validates fixes before applying  
✅ Applies fixes and reruns workflows  
✅ **Prevents infinite loops** with circuit breakers and watchdog  
✅ Escalates to humans when needed  
✅ Learns from patterns  

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│ GitHub Actions Workflow Fails                           │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ 1. WebhookListener captures workflow_run event          │
│    - New handler in processEvent()                      │
│    - Routes to handleWorkflowCompletion()               │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ 2. SafeInfiniteHealingLoop.onWorkflowFailure()          │
│    - Event driven                                       │
│    - Starts healing chain                               │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ 3. HealingCircuitBreaker.shouldAttemptFix()             │
│    - Check: Too many retries?                           │
│    - Check: Same error repeating?                       │
│    - DECISION: Continue or escalate?                    │
└─────────────────────────────────────────────────────────┘
                          ↓ YES
┌─────────────────────────────────────────────────────────┐
│ 4. GitHubAPIService.getWorkflowLogs()                   │
│    - Fetch logs (respects rate limit)                   │
│    - Parse with GitHubActionsErrorParser                │
│    - Extract error type & message                       │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ 5. HealingStateManager.isRepeatedFailure()              │
│    - Check Firestore: Seen this error before?           │
│    - If yes: escalate (loop prevention)                 │
│    - If no: proceed with fix generation                 │
└─────────────────────────────────────────────────────────┘
                          ↓YES
┌─────────────────────────────────────────────────────────┐
│ 6. AutoCodeRepairAgent.generateFix()                    │
│    - Try: Known pattern (database lookup)               │
│    - Try: AI Consensus (10 providers vote)              │
│    - Try: Template fix                                  │
│    - Result: Code fix or null                           │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ 7. FixValidationPipeline.validate()                     │
│    - Stage 1: Static Analysis (syntax, security)        │
│    - Stage 2: Unit Tests (did we pass tests?)           │
│    - Stage 3: Security Scan (Y-Reviewer checks)         │
│    - Stage 4: Code Diff (is change reasonable?)         │
│    - All must pass with >85% confidence                 │
└─────────────────────────────────────────────────────────┘
                          ↓ PASS
┌─────────────────────────────────────────────────────────┐
│ 8. GitService.commitChanges() + push                    │
│    - Rate limited (max 100 API calls/hour)              │
│    - Respects admin mode (AUTO/WAIT/FORCE_STOP)         │
│    - Returns commit hash                                │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ 9. GitHubAPIService.triggerWorkflow()                   │
│    - Retrigger the workflow with fix                    │
│    - Workflow_dispatch event                            │
│    - Rate limited                                       │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ 10. Poll workflow result (5 min later)                  │
│     - Is workflow passing now?                          │
│     - SUCCESS: Mark attempt as SUCCESS                  │
│     - FAILURE: Loop back (retry #2)                     │
└─────────────────────────────────────────────────────────┘
                 ↓ FAIL (max 3 retries)
┌─────────────────────────────────────────────────────────┐
│ 11. AdminEscalationService.escalate()                   │
│     - PagerDuty incident                                │
│     - Slack notification                                │
│     - GitHub issue                                      │
│     - Email admin                                       │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ 12. SupremeAIHealingWatchdog monitors all this          │
│     - Every minute: check failure rates                 │
│     - If >90% failures: KILL the healing system         │
│     - Prevent infinite damage                           │
└─────────────────────────────────────────────────────────┘
```

---

## Core Components

### 1. **HealingAttempt** (Domain Model)  

**File:** `src/main/java/org/example/selfhealing/domain/HealingAttempt.java`

Persists all healing history:

- Attempt ID, workflow ID, error fingerprint
- Fix strategy used
- Validation results
- Status (ATTEMPTED, SUCCESS, FAILED, ESCALATED)
- Commit hashes

### 2. **HealingCircuitBreaker** (State Machine)  

**File:** `src/main/java/org/example/selfhealing/healing/HealingCircuitBreaker.java`

Prevents infinite loops:

- Tracks consecutive failures per workflow
- Max 3 retries, then opens circuit (30-min cooldown)
- Detects repeated errors (same fingerprint = loop)
- Resets on success

### 3. **FixValidationPipeline** (4-Stage)  

**File:** `src/main/java/org/example/selfhealing/healing/FixValidationPipeline.java`

Validates generated fixes:

1. **Static Analysis** - Syntax, imports, security patterns
2. **Unit Tests** - Do tests pass? No regression?
3. **Security Scan** - Y-Reviewer checks for vulnerabilities
4. **Code Diff** - Changes <50 lines? Otherwise require human review

Min confidence: 85%

### 4. **GitHubRateLimiter** (Token Bucket)  

**File:** `src/main/java/org/example/selfhealing/healing/GitHubRateLimiter.java`

Prevents GitHub API rate limit:

- Limit: 4,500 requests/hour (80% of GitHub's 5,000)
- Token refill rate: linear over hour
- Queues excess requests
- Async processor drains queue

### 5. **SafeInfiniteHealingLoop** (Orchestrator)  

**File:** `src/main/java/org/example/selfhealing/healing/SafeInfiniteHealingLoop.java`

Main healing orchestrator:

- Event driven: `@EventListener(condition = "#event.eventType == 'workflow_run'")`
- Recursive retry with max iterations (3)
- Integrates all components
- Delegates to repair agent
- Schedules monitoring with backoff

### 6. **HealingStateManager** (Persistence)  

**File:** `src/main/java/org/example/selfhealing/healing/HealingStateManager.java`

Stores healing history in Firestore:

- In-memory cache (fast queries)
- Firestore backup (persistence)
- Analytics: common errors, best strategies
- Loop detection: repeated failures

### 7. **SupremeAIHealingWatchdog** (External)  

**File:** `src/main/java/org/example/selfhealing/healing/SupremeAIHealingWatchdog.java`

SEPARATE external service that monitors the healing system:

- Runs every minute: `@Scheduled(fixedRate = 60000)`
- Checks: >90% failure rate in last 10 min?
- If yes: DISABLES auto-healing immediately
- Alerts admin via PagerDuty
- Prevents infinite damage

### 8. **AutoCodeRepairAgent** (Code Generator)  

**File:** `src/main/java/org/example/selfhealing/repair/AutoCodeRepairAgent.java`

Generates code fixes using 3 strategies:

1. **Pattern Matching** - Known solutions from database
2. **AI Consensus** - Ask 10 AIs, vote on best
3. **Template** - Common fix templates

### 9. **AdminEscalationService** (Multi-Channel)  

**File:** `src/main/java/org/example/selfhealing/healing/AdminEscalationService.java`

Escalates to admins via:

- 🔴 PagerDuty (critical incidents)
- 💬 Slack (all escalations)
- 📧 Email (detailed report)
- 📋 GitHub Issue (for tracking)

---

## Integration Points

### 1. **WebhookListener** (Already Done)

Added workflow_run event handler:

```java
case "workflow_run" -> {
    // GitHub Actions workflow completion
    handleWorkflowCompletion(event);
}
```

Method `handleWorkflowCompletion()` extracts:

- Conclusion (success/failure/cancelled)
- Workflow name
- Run URL

### 2. **Autowiring in Spring Config**

Add to your Spring configuration or let auto-wiring handle it:

```java
@Bean
public HealingCircuitBreaker healingCircuitBreaker() {
    return new HealingCircuitBreaker();
}

@Bean
public FixValidationPipeline fixValidationPipeline() {
    return new FixValidationPipeline();
}

@Bean
public HealingStateManager healingStateManager() {
    return new HealingStateManager();
}

// ... etc for all services
```

### 3. **Firestore Schema**

Create this collection in Firestore:

```
healing_attempts/
  ├── attempt_<id>
  │   ├── attemptId: string
  │   ├── workflowId: string
  │   ├── errorFingerprint: string
  │   ├── status: string (ATTEMPTED/SUCCESS/FAILED/ESCALATED)
  │   ├── retryCount: number
  │   ├── createdAt: timestamp
  │   ├── resolvedAt: timestamp?
  │   ├── commitHashes: array<string>
  │   ├── confidenceScore: number (0.0-1.0)
  │   └── notes: string?
```

### 4. **GitHub Webhook Setup**

GitHub needs to send `workflow_run` events:

1. Go to: **Settings → Webhooks**
2. Add webhook: `https://your-domain.com/webhook/github`
3. Content type: `application/json`
4. Events: Select **Workflow runs**
5. Secret: Set to `GITHUB_WEBHOOK_SECRET` env var

---

## Configuration

### Environment Variables

```bash
# GitHub
GITHUB_TOKEN=ghp_xxxxxxxxxxxx (for API calls)
GITHUB_WEBHOOK_SECRET=your-secret (for signature verification)

# PagerDuty (for escalation)
PAGERDUTY_API_KEY=u+xxxxxxxxxxxxxxxx
PAGERDUTY_ROUTING_KEY=Pxxxxxxxxxxxxxxxx

# Slack (for notifications)
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXX

# Admin
ADMIN_EMAIL=admin@company.com
```

### Application Properties

```properties
# Healing system
supremeai.healing.max-retries=3
supremeai.healing.retry-backoff-minutes=1
supremeai.healing.workflow-check-delay-ms=300000

# Circuit breaker
supremeai.circuit-breaker.max-failures=3
supremeai.circuit-breaker.cooldown-minutes=30

# Rate limiter
supremeai.rate-limiter.max-requests-per-hour=4500

# Watchdog
supremeai.watchdog.failure-threshold=5
supremeai.watchdog.check-interval-minutes=1
```

---

## REST API Endpoints

### System Status

```bash
# Get overall status
GET /api/healing/status

# Get watchdog health
GET /api/healing/watchdog

# Get rate limit status
GET /api/healing/rate-limit

# Get attempt history for workflow
GET /api/healing/attempts/{workflowId}?limit=10

# Full diagnostics
GET /api/healing/diagnostics

# Health check (for K8s/monitoring)
GET /api/healing/health
```

### Admin Actions

```bash
# Disable healing (emergency stop)
POST /api/healing/disable

# Re-enable healing
POST /api/healing/enable

# Manually reset circuit breaker for retry
POST /api/healing/retry/{workflowId}

# Check circuit breaker status
GET /api/healing/circuit-breaker/{workflowId}
```

---

## How to Use

### 1. **First-Time Setup**

```bash
# 1. Deploy the code
mvn clean package
docker build -t supremeai:latest .

# 2. Configure environment variables (see above)
export GITHUB_TOKEN=...
export PAGERDUTY_API_KEY=...
export SLACK_WEBHOOK_URL=...

# 3. Start application
java -jar supremeai.jar

# 4. Verify: Check health endpoint
curl http://localhost:8080/api/healing/health
```

### 2. **Monitor the System**

```bash
# Dashboard: Real-time stats
curl http://localhost:8080/api/healing/status

# Rate limiter check
curl http://localhost:8080/api/healing/rate-limit

# Watchdog health
curl http://localhost:8080/api/healing/watchdog
```

### 3. **Emergency Stop**

If healing system is malfunctioning:

```bash
# Disable immediately
curl -X POST http://localhost:8080/api/healing/disable

# Check status
curl http://localhost:8080/api/healing/status

# Re-enable after fixing
curl -X POST http://localhost:8080/api/healing/enable
```

### 4. **Manual Retry**

If a workflow needs manual retry:

```bash
# Reset circuit breaker
curl -X POST http://localhost:8080/api/healing/retry/{workflowId}

# This allows one more attempt
```

---

## Safety Features

### ✅ Circuit Breaker

- Max 3 consecutive failures
- Opens circuit, enters 30-min cooldown
- Resets on any success
- Detection of repeated errors (same fingerprint)

### ✅ Validation Pipeline

- 4-stage validation (static, tests, security, diff)
- Requires 85% confidence score
- Detects security vulnerabilities
- Flags large diffs (>50 lines) for human review

### ✅ Rate Limiting

- Respects GitHub's 5,000 req/hour limit
- Conservative: 4,500 limit to stay safe
- Token bucket refill algorithm
- Queues excess requests

### ✅ Watchdog (EXTERNAL)

- Runs separately from healing system
- Monitors every minute
- If >90% failures: DISABLES healing immediately
- Prevents infinite damage

### ✅ Escalation

- After max retries: human review required
- Multi-channel alerts (PagerDuty, Slack, email, GitHub)
- All decisions logged in Firestore
- Audit trail for compliance

### ✅ State Persistence

- All attempts stored in Firestore
- Firestore rules protect data
- Enables learning and analytics
- Can detect patterns across time

---

## Testing

Run the test suite:

```bash
mvn test
```

Key test files to verify:

- `HealingCircuitBreakerTest.java` - Retry logic
- `FixValidationPipelineTest.java` - Validation stages
- `GitHubRateLimiterTest.java` - Rate limiting
- `SafeInfiniteHealingLoopTest.java` - End-to-end flow

---

## Troubleshooting

### Problem: "Circuit breaker OPEN"

**Cause:** Max retries exceeded or repeated errors

**Solution:**

1. Check circuit breaker status: `GET /api/healing/circuit-breaker/{workflowId}`
2. Fix underlying issue in your code
3. Manual retry: `POST /api/healing/retry/{workflowId}`

### Problem: "Validation failed"

**Cause:** Generated fix didn't pass one of 4 stages

**Check:** `/api/healing/diagnostics` → Look at recent attempts

**Common fixes:**

- Fix has syntax errors (fail static analysis)
- Tests still failing after fix
- Security vulnerabilities introduced
- Too many lines changed

### Problem: "Rate limit exceeded"

**Cause:** Too many GitHub API calls

**Solution:**

1. Check rate limit: `GET /api/healing/rate-limit`
2. Wait for refill (linear over 1 hour)
3. Disable healing temporarily: `POST /api/healing/disable`
4. Investigate GitHub API usage

### Problem: "Watchdog triggered FAILURE"

**Most Dangerous:** Healing system itself is broken

**Actions:**

1. Check watchdog status: `GET /api/healing/watchdog`
2. **Immediately disable:** `POST /api/healing/disable`
3. Investigate logs
4. Fix healing system or code
5. Re-enable: `POST /api/healing/enable`

---

## Performance Characteristics

| Metric | Value |
|--------|-------|
| Detection latency | <1 min (webhook) |
| Fix generation | 5-15 seconds (AI consensus) |
| Validation | 10-30 seconds (4 stages) |
| Commit & push | 5-10 seconds |
| Workflow retrigger | <5 seconds |
| Result checking | 5+ minutes (workflow runtime) |
| **Total loop time** | **5-20 minutes/iteration** |
| **Rate limit** | **4,500 requests/hour** |

---

## Next Steps

1. Deploy to Cloud Run / your K8s cluster
2. Configure all environment variables
3. Set up GitHub webhook (workflow_runs event)
4. Test with a manual GitHub Actions failure
5. Monitor via `/api/healing/status`
6. Set up alerting in your monitoring system (Datadog, New Relic, etc.)

---

## Support

Questions or issues? Check:

- Logs: `docker logs supremeai`
- Diagnostics: `GET /api/healing/diagnostics`
- Firestore: `healing_attempts` collection
- GitHub webhook delivery: Settings → Webhooks → your webhook → Deliveries
