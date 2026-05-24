# Sprint 0: Critical Fixes for Phase 1 Market Readiness

**Goal:** Eliminate all critical security and stability gaps before any market testing.

**Duration:** Week 1, Days 1-2 (Estimated: 12 hours total)

---

## Critical Issues (Must Fix Before Market Testing)

| Priority | Issue | Severity | Effort | Owner |
|----------|-------|----------|--------|-------|
| 🔴 CRITICAL | G-03: Password leak in BrowserService logs | Security | 30 min | Backend |
| 🔴 CRITICAL | G-04: No Circuit Breaker auto-cooldown | Cost/Reliability | 4 hours | Backend |
| 🔴 CRITICAL | G-05: Hardcoded judge in triggerDebate() | Accuracy | 1 hour | Backend |
| 🔴 CRITICAL | G-01: Solo Mode browser not wired | Solo Mode | 3 days | Full-stack |
| 🔴 CRITICAL | G-02: core_knowledge.json empty categories | Offline Mode | 2 days | Knowledge |

---

## Task Breakdown

### Task S0-1: Fix Password Leak (G-03)
**File:** `src/main/java/com/supremeai/service/BrowserService.java`
**Effort:** 30 minutes

**Action:**
- Find `getCredentialContext()` method
- Replace decrypted password logging with `[REDACTED]`
- Search for any other credential logging patterns

```java
// BEFORE (vulnerable):
log.info("Credential context: {}", decryptedPassword);

// AFTER (secure):
log.info("Credential context: [REDACTED]");
```

---

### Task S0-2: Add Circuit Breaker Auto-Cooldown (G-04)
**Files:** `src/main/java/com/supremeai/service/EnhancedMultiAIConsensusService.java`, `application.yml`
**Effort:** 4 hours

**Action:**
- Add Resilience4j CircuitBreaker configuration in `application.yml`
- Configure auto-cooldown: 3 failures in 60s → 5 min cooldown
- Add circuit breaker to all provider API calls
- Test: simulate provider failure → verify quarantine

```yaml
resilience4j.circuitbreaker:
  instances:
    ai-provider:
      failure-rate-threshold: 50
      slow-call-rate-threshold: 50
      wait-duration-in-open-state: 300000 # 5 minutes
      permitted-number-of-calls-in-half-open-state: 1
      sliding-window-size: 10
      minimum-number-of-calls: 3
```

---

### Task S0-3: Fix Judge Selection (G-05)
**File:** `src/main/java/com/supremeai/service/EnhancedMultiAIConsensusService.java`
**Effort:** 1 hour

**Action:**
- Find `triggerDebate()` method (around line 406)
- Replace `allProviders.get(0)` with `aiRankingService.getTopProvider()`
- Ensure fallback to first provider if ranking unavailable

```java
// BEFORE:
String judge = allProviders.get(0);

// AFTER:
String judge = aiRankingService.getTopProvider()
    .orElse(allProviders.get(0));
```

---

### Task S0-4: Add Docker Healthchecks
**File:** `docker-compose.yml`
**Effort:** 2 hours

**Action:**
- Add `healthcheck` directive to each service
- Configure appropriate test commands and intervals

```yaml
services:
  supremeai:
    # ... existing config
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
```

---

### Task S0-5: Add Null Guard
**File:** `src/main/java/com/supremeai/service/EnhancedMultiAIConsensusService.java`
**Effort:** 30 minutes

**Action:**
- Find `buildDiscussionContext()` method
- Add null check before `.substring()` call

```java
// BEFORE:
String context = response.substring(0, Math.min(200, response.length()));

// AFTER:
String context = (response != null && !response.isEmpty())
    ? response.substring(0, Math.min(200, response.length()))
    : "";
```

---

### Task S0-6: Solo Mode Browser Wiring (G-01)
**Files:** Multiple
**Effort:** 3 days

**Action:**
- See detailed plan in `docs/plans/browser-intelligence-improvement-plan.md`

---

### Task S0-7: Expand core_knowledge.json (G-02)
**File:** `core_knowledge.json`
**Effort:** 2 days

**Action:**
- Add entries for:
  - AI provider management
  - User and permission management
  - Knowledge bootstrap from zero
  - Local AI model setup
  - P2P knowledge sync

---

## Progress Tracking

| Task | Status | Notes |
|------|--------|-------|
| S0-1: Password Leak | ⏳ Pending | - |
| S0-2: Circuit Breaker | ⏳ Pending | - |
| S0-3: Judge Selection | ⏳ Pending | - |
| S0-4: Docker Healthchecks | ⏳ Pending | - |
| S0-5: Null Guard | ⏳ Pending | - |
| S0-6: Solo Mode Browser | ⏳ Pending | See browser-intelligence plan |
| S0-7: Knowledge Expansion | ⏳ Pending | - |

---

## Definition of Done

- [ ] All critical security issues fixed
- [ ] All circuit breakers configured and tested
- [ ] Solo Mode fully functional with Playwright
- [ ] Docker healthchecks passing
- [ ] All code compiles and passes tests
- [ ] Manual testing of all fixes completed
- [ ] Sprint 1 ready to begin

---

**Created:** 2026-05-24
**Updated:** -