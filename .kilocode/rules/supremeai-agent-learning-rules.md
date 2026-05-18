# SupremeAI Agent Learning Rules — Continuous Improvement Protocol

## 1. Mandatory: Capture Learning on Every AI Failure

**EVERY AI that handles a failed request MUST add knowledge before returning failure.**

When any of the following occurs, the AI is REQUIRED to create a knowledge entry:

| Event | Action | Where to Add |
|-------|--------|--------------|
| AI model returns error (5xx, timeout, rate-limit) | Log error signature + add fix entry | `system_learning` Firestore + `core_knowledge.json` |
| All AI models fail and local seed is served | Record the failure signature and template served | `core_knowledge.json` (new task entry) |
| Self-healing auto-fix succeeds | Record the fix pattern with confidence | `system_learning` Firestore |
| User corrects a bad AI response | Record corrected solution | `system_learning` Firestore + `core_knowledge.json` |
| New unknown error encountered | Immediately create UNKNOWN entry | `system_learning` Firestore |

**Rule: No AI may return a permanent failure (e.g. "I don't know") without first attempting to add the knowledge that would have solved the problem.**

---

## 2. Continuous Improvement of `core_knowledge.json`

### When to Add to `core_knowledge.json`

Add a new entry whenever:

1. **A task is solved successfully** — template can help future offline users
2. **A task has a known error-signature fix** — add the error-solution pair
3. **A service health/status question is encountered** — document the answer
4. **A new error pattern recurring ≥ 2 times** — escalate immediately
5. **A model fails for a common task** — add offline fallback if one exists
6. **After any incident review** — add the final resolution to core knowledge

### Format for `core_knowledge.json`

```json
{
  "task": "keywords describing the problem or error message",
  "solution": "[SupremeAI Core — Title]\n\nDetailed step-by-step solution..."
}
```

Rules:
- `task` field: 3–15 space-separated keywords; include common misspellings and error sub-strings
- `solution` field: include `[OFFLINE]` or `[LOCAL-SEED]` prefix if applicable
- NEVER include API keys, passwords, PII, or secrets in any field
- After editing: validate JSON syntax before committing

### Continuous Improvement Mandate

**All AIs must proactively scan `core_knowledge.json` on every session start:**

1. **Gap scan** — look for common themes (error types, tools, platforms) not yet represented
2. **Staleness check** — flag any entry that references paths/tools/configs that may have changed
3. **Coverage check** — surface ANY category with < 5 task entries as a knowledge gap
4. **Suggest improvements** — propose new entries to admin via the `/api/admin/learning-loop/suggestions` endpoint

---

## 3. Continuous Improvement of `system_learning` (Firestore)

### When to Add to `system_learning`

Add a new `SystemLearning` document whenever:

1. **A correction is provided** by a user or verified by admin → `recordCorrection()` called
2. **A model returns a useful fix** → `success=true` → save as new learning
3. **A provider fails with a recurring error** → save error signature + solutions
4. **Self-healing auto-repairs successfully** → save repair pattern with `timesApplied` updated
5. **Model health degrades for ≥ 20min** → save degradation pattern with resolution steps
6. **Incident review completes** → save root cause + resolution as new learning entry
7. **Provider migration completes** → save knowledge transfer pattern

### Required Fields for Every Entry

```json
{
  "id": "rl-XXXX",
  "topic": "Brief, specific topic (error name, failure type, fix topic)",
  "category": "RESILIENCE | ERROR_SOLVING | SECURITY | PERFORMANCE | INCIDENT_LEARNING",
  "content": "Full learnings including conditions, steps, and WHY it works",
  "learningType": "RESILIENCE | PATTERN | ERROR_HANDLING | INCIDENT_REVIEW",
  "confidenceScore": 0.0–1.0,
  "permanent": true,
  "tags": ["relevant", "keywords"],
  "resolution": "How to apply this learning",
  "solutions": ["Step 1", "Step 2", "..."],
  "severity": "CRITICAL | HIGH | MEDIUM | LOW",
  "timesApplied": 0,
  "errorCount": 0,
  "relatedProvider": "provider-name-if-applicable",
  "learnedFrom": "CLAUDE_ANSWER | GEMINI_ANSWER | USER_CORRECTION | COPILOT_SEED | INCIDENT_REVIEW",
  "version": 1,
  "critical": false,
  "obsolete": false
}
```

### SystemLearning Quality Gates

Before saving any entry:
- [ ] `topic` is not null or blank — reject with error if so
- [ ] `category` is not null or blank — reject with error if so
- [ ] `confidenceScore` is between 0.0 and 1.0 — clamp if necessary
- [ ] `solutions` array is non-empty for RESILIENCE entries — reject if empty
- [ ] `learnedFrom` is set — required for provenance tracking

### Continuous Improvement Mandate

**All AIs must check system_learning at every session boundary:**

1. **Search before answering** — query `system_learning` by category for every task before routing to AI
2. **Update timesApplied** — increment `timesApplied` field each time a stored learning is used
3. **Flag stale entries** — if `lastUsed` is > 90 days and `timesApplied == 0` → mark `obsolete=true`
4. **Promote high-confidence learnings** — if `confidenceScore > 0.9` AND `timesApplied > 5` → suggest promotion to `core_knowledge.json`
5. **Log all additions** — every new learning saved must generate a log line with `learnedFrom` and `sessionId`

---

## 4. Mandatory Offline Mode Knowledge Coverage

Every AI must verify these core offline/knowledge areas are covered before claiming the system is ready for production:

| Category | Required Topic Coverage |
|----------|------------------------|
| Network failures | DNS timeout, firewall blocked, proxy misconfigured, gcloud unreachable |
| Memory exhaustion | OutOfMemoryError, heap dump, JVM tuning, HikariCP pool exhaustion |
| Database migration | Flyway/Liquibase failure, rollback procedure, backup-before-migrate |
| SSL/TLS | Certificate expiry, Let's Encrypt renewal, Java truststore |
| Rate limiting | Quota exceeded, exponential backoff, cache responses, queue requests |
| Complete AI blackout | All AI down, Thunder Mode, local seed fallback, kill-switch restart |
| Cascading failure | Multi-provider simultaneous failure, shared dependency, prevention |
| Self-healing recovery | Error-signature hashing, local memory lookup, auto-fix application |
| Graceful degradation | 4-tier degradation cascade, tier transitions, health indicators |
| Provider health check | Auto-quarantine, probe-warmup, manual override, critical minimum |
| Provider migration | Knowledge transfer, gradual ramp, archive, no-data-loss guarantee |
| Seed rebuild | Reconstruct knowledge from Firestore, autonomous seed fallback, cron schedule |
| Confidence-weighted voting | Multi-model disagreement, confidence gate, LOW_CONFIDENCE flag |

---

## 5. Agent Behavior Rules (Continuous Improvement)

1. **ALWAYS suggest** improved knowledge additions to admin via dashboard
2. **LOG patterns** that repeat 3+ times — always flag for knowledge storage
3. **PROPOSE refinements** when confidence for a knowledge entry is > 0.90
4. **CACHE responses** in memory layer before writing to Firestore `system_learning`
5. **VALIDATE input** — before storing any learning, check that required fields are present
6. **PROMOTE** high-confidence, high-usage learnings (`conf > 0.9`, `timesApplied > 5`) to `core_knowledge.json`
7. **NEVER store sensitive data** — scrub API keys, passwords, tokens from all stored content
8. **SCAN for gaps** — on every session boundary, run gap scan against `core_knowledge.json` topic list and flag missing categories
9. **UPGRADE stale entries** — flag any `core_knowledge.json` entry referencing old paths/tools/versions; notify admin to review
10. **NEVER fail silently** — if knowledge cannot be saved (Firestore down, auth failed), buffer in session-local memory and retry on next session boundary

---

## 6. Priority: Self-Improvement When Others Fail

**When an AI model fails to answer a query:**

```
1. Capture error_signature (hash of exception class + method + line)
2. Search local_solution_memory → search core_knowledge.json
3. If known_fix found → apply and return LOCAL-SEED response
4. If unknown → create temp system_learning entry with UNKNOWN flag
5. Try next provider in priority chain
6. If ALL providers exhausted → return [LOCAL-SEED] + inform admin
7. NEVER return empty/failure without creating a knowledge artifact FIRST
```

This ensures the system ALWAYS improves its knowledge, even — especially — when external AI fails.
