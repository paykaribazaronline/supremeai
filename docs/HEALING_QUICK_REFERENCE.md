# Safe Healing Loop - Quick Reference

## 🎯 The Problem (Solved)

**Infinite Loop Risk:**

- Build fails → AI fixes → Push → Build fails again → AI fixes → ...
- No max retries → 1000s of commits
- GitHub rate limits hit
- Costs $$$
- System breaks

## ✅ The Solution (Implemented)

### Circuit Breaker Pattern

```java
// Max 3 retries per workflow
// After 3 failures → Open circuit (30-min cooldown)
// Any success → Reset

if (!circuitBreaker.shouldAttemptFix(workflowId, errorHash)) {
    escalateToHuman(); // Too many failures
    return;
}
```

### 4-Stage Validation Pipeline

```
Fix Generated
    ↓
1. Static Analysis (syntax, security patterns) 
    ↓
2. Unit Tests (pass? regression?)
    ↓
3. Security Scan (vulnerabilities?)
    ↓
4. Code Diff (>50 lines = require review)
    ↓
All pass + ≥85% confidence → Deploy
```

### GitHub Rate Limiter

```java
// Token bucket: 4,500 requests/hour
// Refill: Linear over 1 hour
// Excess: Queue for later

rateLimiter.executeWithRateLimit("api_call", () -> {
    githubAPI.createCommit(...);
});
```

### External Watchdog (THE KEY)

```java
// SEPARATE from healing system
// Runs every 60 seconds
// Monitor: Recent attempts
// If >90% failures in 10 min → DISABLE HEALING

if (successRate < 0.1 && attemptCount > 5) {
    watchdog.disableAutoHealing();
    alertAdmin("HEALING SYSTEM FAILURE");
}
```

### Admin Control Always Available

```bash
# Emergency stop
POST /api/healing/disable

# Check status
GET /api/healing/diagnostics

# Manual retry
POST /api/healing/retry/{workflowId}

# Re-enable
POST /api/healing/enable
```

---

## 📁 Files Created

### Core Healing Engine

- `HealingCircuitBreaker.java` - Retry limiter
- `FixValidationPipeline.java` - 4-stage validation
- `GitHubRateLimiter.java` - Token bucket
- `SafeInfiniteHealingLoop.java` - Orchestrator
- `HealingStateManager.java` - Persistence
- `SupremeAIHealingWatchdog.java` - External monitor ⭐

### Support Services

- `AutoCodeRepairAgent.java` - Code generation
- `AdminEscalationService.java` - Alerts (PagerDuty, Slack)
- `HealingSystemController.java` - REST API

### Domain Models

- `HealingAttempt.java` - Healing history
- `ValidationResult.java` - Validation results
- `ValidationStage.java` - Individual stage results

### Updated Files

- `WebhookListener.java` - Added workflow_run handler

---

## 🔄 Flow (The Happy Path)

```
1. GitHub Action fails
2. Webhook → WebhookListener.handleWorkflowCompletion()
3. SafeInfiniteHealingLoop.onWorkflowFailure()
4. CircuitBreaker check: "Should retry?"
   - Not too many failures? ✓
   - Error not repeating? ✓
5. Fetch logs + parse error
6. StateManager check: "Seen this before?"
   - Not a known pattern? ✓
7. AutoCodeRepairAgent: Generate fix
8. FixValidationPipeline: 4-stage validation
   - All pass + ≥85% confidence? ✓
9. RateLimiter: Rate limited API call
10. Git commit + push fix
11. Retrigger workflow
12. Poll result (5 min later)
13. SUCCESS → Mark resolved, learn from success
    FAILURE → Retry (up to 3 times)
```

## 🚫 The Unhappy Paths (Handled)

### Path 1: Too Many Retries

```
Attempt #1 fails
Attempt #2 fails
Attempt #3 fails
→ Circuit breaker OPENS
→ Human escalation (PagerDuty, Slack)
```

### Path 2: Same Error Repeating

```
Error hash matches previous error
Previous attempt FAILED
→ This is a loop → Escalate
```

### Path 3: Validation Fails

```
Generated code has syntax errors
+ security vulnerabilities
+ breaks tests
→ Retry with different strategy
→ Or escalate if all strategies fail
```

### Path 4: Healing System Itself is Broken

```
Watchdog notices: >90% failures in last 10 min
→ **IMMEDIATELY DISABLES HEALING**
→ Alerts admin (PagerDuty: CRITICAL)
→ Prevents infinite damage
```

---

## 📊 Safety Guarantees

| Scenario | Protection | Method |
|----------|-----------|--------|
| Infinite retries | Circuit breaker (max 3) | `HealingCircuitBreaker` |
| Bad code deployed | 4-stage validation + 85% min | `FixValidationPipeline` |
| GitHub rate limit | Token bucket algorithm | `GitHubRateLimiter` |
| System breaks itself | External watchdog | `SupremeAIHealingWatchdog` |
| Loop detection | Error fingerprint comparison | `HealingStateManager` |
| Human visibility | REST API + Firestore logs | `HealingSystemController` |
| Emergency stop | Admin endpoint | `POST /api/healing/disable` |

---

## 🚀 Quick Start

### 1. Build & Deploy

```bash
mvn clean package
docker build -t supremeai .
docker run -e GITHUB_TOKEN=... -e PAGERDUTY_API_KEY=... supremeai:latest
```

### 2. Configure GitHub Webhook

- Settings → Webhooks → Add webhook
- URL: `https://your-domain.com/webhook/github`
- Events: **Workflow runs**
- Secret: Set `GITHUB_WEBHOOK_SECRET`

### 3. Monitor

```bash
curl http://localhost:8080/api/healing/status
```

### 4. Emergency Stop (if needed)

```bash
curl -X POST http://localhost:8080/api/healing/disable
```

---

## 🎓 Key Concepts

### Circuit Breaker

- **CLOSED:** Normal operation
- **OPEN:** Too many failures, wait 30 min
- **HALF_OPEN:** Try once, then decide

### Token Bucket

- **Rate:** 4,500 tokens/hour
- **Refill:** Linear (constant rate)
- **Spillover:** Queue excess requests

### Error Fingerprint

- **Purpose:** Detect loops (same error repeating)
- **Calculation:** Hash of normalized error message
- **Match:** Same fingerprint + failed before = loop

### Validation Score

- **Range:** 0.0 to 1.0
- **Threshold:** ≥0.85 required to deploy
- **Calculation:** Average of 4 stage scores

---

## 📞 Support

| Issue | Endpoint | Action |
|-------|----------|--------|
| "Why did healing disable?" | `GET /api/healing/watchdog` | Check failure rate |
| "Is rate limit OK?" | `GET /api/healing/rate-limit` | Check utilization |
| "What happened to my workflow?" | `GET /api/healing/attempts/{id}` | See attempt history |
| "Emergency: Stop everything!" | `POST /api/healing/disable` | Disable immediately |
| "Reset and retry" | `POST /api/healing/retry/{id}` | Reset circuit breaker |
| "Full diagnostics" | `GET /api/healing/diagnostics` | See everything |

---

## ⚡ Performance

- **Detection:** <1 minute (webhook)
- **Fix generation:** 5-15 seconds
- **Validation:** 10-30 seconds
- **Deploy:** <10 seconds
- **Total loop:** 5-20 minutes (includes workflow runtime)
- **Rate limit:** 4,500 req/hour = ~1.25 req/sec

---

## 💡 Best Practices

1. **Monitor watchdog health:** Check every hour
2. **Set PagerDuty alerts:** For critical escalations
3. **Review Firestore collection:** Understand patterns
4. **Tune retry count:** Start at 3, adjust based on data
5. **Watch rate limit:** Keep <80% utilization
6. **Manual testing first:** Verify with fake failures
7. **Have runbook:** What to do if healing fails
8. **Gradual rollout:** Start with low-impact repos

---

## 🎯 Success Metrics

After 1 week of deployment:

- [ ] >70% of failures auto-fixed
- [ ] <5 escalations per day
- [ ] 0 GitHub rate limit hits
- [ ] <50% false positive fixes (validation catches issues)
- [ ] Watchdog never triggers (healthy system)

After 1 month:

- [ ] >85% of failures auto-fixed
- [ ] 1-2 escalations per day
- [ ] CI/CD time reduced 30%
- [ ] Team spending 50% less time debugging
