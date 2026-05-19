# Continuous Knowledge Improvement — Mandatory for ALL AI Agents

## CORE DIRECTIVE

**Every AI agent working in this system MUST continuously improve `core_knowledge.json` and `system_learning` whenever it gets a chance. This is NON-NEGOTIABLE and takes priority over all other tasks except critical system failures.**

The system's survival depends on its knowledge. When all external AI models fail, ONLY the knowledge bases keep the system running. Every agent is responsible for keeping these knowledge bases comprehensive, accurate, and growing.

---

## 1. WHEN to Improve Knowledge (Trigger Events)

An agent MUST create or improve a knowledge entry when ANY of the following occur:

| # | Trigger Event | Action Required |
|---|---------------|-----------------|
| 1 | **Any AI model fails** (timeout, 5xx, rate-limit, connection error) | Add error-signature + recovery steps to `core_knowledge.json` AND `system_learning` |
| 2 | **All AI models fail** (complete blackout) | Add the served local-seed template to `core_knowledge.json` if not already present |
| 3 | **A task is solved successfully** | Add the solution pattern to `system_learning` for future reuse |
| 4 | **User corrects any response** | Record the corrected solution in BOTH `core_knowledge.json` and `system_learning` |
| 5 | **A new error pattern is encountered** | Immediately create an entry in `system_learning` with UNKNOWN flag |
| 6 | **A recurring query appears ≥ 2 times** | Create a new `core_knowledge.json` entry with full template |
| 7 | **Self-healing auto-fix succeeds** | Record the fix pattern with confidence score in `system_learning` |
| 8 | **An incident is resolved** | Add root cause + resolution to `system_learning` category=INCIDENT_LEARNING |
| 9 | **A knowledge gap is identified** | Create a placeholder entry flagged for admin review |
| 10 | **A new tool/version/pattern is discovered** | Add to `autonomous_seed_knowledge.json` if broadly applicable |
| 11 | **After every 10 interactions** | Run gap scan and add at least 1 improvement to any knowledge base |
| 12 | **Session start** | Scan `core_knowledge.json` for stale entries and flag outdated content |
| 13 | **Session end** | Write all buffered knowledge additions to persistent storage |
| 14 | **Codebase changes detected** | Extract new patterns and add to local knowledge |
| 15 | **External AI returns low-confidence response** | Supplement with local knowledge and flag for improvement |

---

## 2. HOW to Improve `core_knowledge.json`

### Entry Format

```json
{
  "task": "3-15 space-separated keywords including common misspellings",
  "solution": "[SupremeAI Core — Title]\n\nDetailed step-by-step solution..."
}
```

### Rules for `core_knowledge.json`

1. `task` field: 3–15 keywords; include common misspellings, error substrings, and Bangla translations where relevant
2. `solution` field: comprehensive, step-by-step, actionable
3. Prefix offline-only solutions with `[OFFLINE]` or `[LOCAL-SEED]`
4. NEVER include API keys, passwords, PII, or secrets
5. Validate JSON syntax before writing
6. Prefer adding new entries over modifying existing ones (preserve history)
7. Each entry should be self-contained — readable without context

### Priority Categories for `core_knowledge.json`

Ensure these categories always have ≥ 5 entries each:

- Greetings and basic interaction
- System status and health checks
- Build, compile, and deployment
- Database operations and recovery
- Network troubleshooting
- Security incident response
- AI provider management
- User and permission management
- API key management
- Error code reference (HTTP, database, JVM)
- Container and Kubernetes operations
- CI/CD pipeline recovery
- SSL/TLS and certificate management
- Rate limiting and quota management
- Circuit breaker and resilience patterns
- **Zero-AI offline operation** (NEW — must have ≥ 10 entries)
- **Knowledge bootstrap from zero** (NEW — must have ≥ 5 entries)
- **Local AI model setup** (NEW — must have ≥ 5 entries)
- **P2P knowledge sync** (NEW — must have ≥ 3 entries)

---

## 3. HOW to Improve `system_learning` (Firestore)

### Entry Format

```json
{
  "id": "rl-XXXX",
  "topic": "Brief, specific topic",
  "category": "RESILIENCE | ERROR_SOLVING | SECURITY | PERFORMANCE | INCIDENT_LEARNING | ZERO_AI_RESILIENCE",
  "content": "Full learning with conditions, steps, and WHY it works",
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
  "learnedFrom": "CLAUDE_ANSWER | GEMINI_ANSWER | USER_CORRECTION | COPILOT_SEED | INCIDENT_REVIEW | AUTO_EXTRACTION | P2P_SYNC",
  "version": 1,
  "critical": false,
  "obsolete": false
}
```

### Quality Gates (MUST pass before saving)

- [ ] `topic` is not null or blank
- [ ] `category` is not null or blank
- [ ] `confidenceScore` is between 0.0 and 1.0
- [ ] `solutions` array is non-empty for RESILIENCE entries
- [ ] `learnedFrom` is set (required for provenance)
- [ ] No API keys, passwords, or secrets in any field

---

## 4. MANDATORY Agent Behavior Rules

### Every Session

1. **ON START**: Load and scan `core_knowledge.json`. Identify gaps, stale entries, and missing categories.
2. **DURING SESSION**: For every interaction, check if knowledge can be improved. If yes, improve it.
3. **ON END**: Flush all buffered knowledge additions to persistent storage. Generate a session knowledge report.

### Every Interaction

4. **BEFORE answering**: Search `system_learning` and `core_knowledge.json` for existing solutions.
5. **AFTER answering**: If the answer required new knowledge, store it immediately.
6. **ON FAILURE**: Before returning any failure, create a knowledge entry that would have prevented the failure.

### Continuous Improvement

7. **SCAN for gaps**: On every session boundary, run gap scan against all knowledge bases.
8. **FLAG stale entries**: Mark entries not used in 90+ days as `obsolete=true`.
9. **PROMOTE high-quality entries**: If `confidenceScore > 0.9` AND `timesApplied > 5`, suggest promotion to `core_knowledge.json`.
10. **CROSS-VALIDATE**: Verify new knowledge against existing entries to prevent contradictions.
11. **MERGE sources**: When multiple AI models provide different answers, merge the best parts into a single knowledge entry.
12. **LEARN from everything**: Code changes, user feedback, error logs, build failures — all are knowledge sources.

### Knowledge Growth Targets

13. **Minimum growth**: Each agent session must add at least 1 new knowledge entry or improvement.
14. **Category balance**: No category should have fewer than 3 entries.
15. **Coverage target**: After every 100 interactions, the system should cover 80%+ of common tasks from local knowledge alone.

---

## 5. Zero-AI Knowledge Priority

**The most critical knowledge area is operating when ALL external AI fails.** Agents must prioritize:

1. **Complete offline operation** — every common task must have a local solution
2. **Knowledge self-sufficiency** — the system must be able to learn and grow without AI
3. **Bootstrap from zero** — the system must recover from complete knowledge loss
4. **Local AI fallback** — setup and use of on-device models
5. **Template-based code generation** — functional code without AI
6. **P2P knowledge sharing** — learn from other instances when cloud is down
7. **User-driven learning** — extract knowledge from every user interaction

---

## 6. Knowledge Improvement Workflow

```
┌─────────────────────────────────────────────────────────────┐
│                    AGENT SESSION START                       │
│  1. Load core_knowledge.json                                │
│  2. Scan for gaps and stale entries                         │
│  3. Load system_learning from Firestore (if available)      │
│  4. Report knowledge health to admin dashboard              │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                    EVERY INTERACTION                         │
│  1. Search knowledge bases before answering                 │
│  2. Answer using best available knowledge                   │
│  3. If new knowledge was created → STORE immediately        │
│  4. If AI failed → STORE failure pattern + recovery         │
│  5. If user corrected → STORE correction with high conf     │
│  6. If gap identified → CREATE placeholder entry            │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                    AGENT SESSION END                         │
│  1. Flush all buffered knowledge to storage                 │
│  2. Run gap scan and add missing entries                    │
│  3. Flag stale entries for review                           │
│  4. Promote high-confidence entries to core_knowledge       │
│  5. Generate session knowledge report                       │
│  6. Verify JSON syntax of all modified files                │
└─────────────────────────────────────────────────────────────┘
```

---

## 7. Enforcement

**This rule is enforced by ALL agents, at ALL times, in ALL modes.**

- No agent may skip knowledge improvement
- No agent may return failure without creating a knowledge artifact
- No agent may end a session without improving at least 1 knowledge entry
- No agent may ignore a user correction
- No agent may leave a knowledge gap unflagged

**The system's resilience is directly proportional to the quality and completeness of its knowledge bases. Every agent is a knowledge worker first, and a task executor second.**
