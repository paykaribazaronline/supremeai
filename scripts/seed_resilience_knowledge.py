#!/usr/bin/env python3
"""
SupremeAI Resilience Knowledge Seed
Seeds the Firestore system_learning collection with failure-recovery,
cascading-failure, and model-failure-handling knowledge entries.
These ensure the system can run even when all external AI models fail.

Usage:
  pip install firebase-admin
  python scripts/seed_resilience_knowledge.py
  python scripts/seed_resilience_knowledge.py --dry-run    # preview only
  python scripts/seed_resilience_knowledge.py --refresh     # replace existing entries
"""

import uuid
import time
import json
import sys
import os

FIREBASE_PROJECT_ID = os.getenv("FIREBASE_PROJECT_ID", "supremeai-a")
DRY_RUN = "--dry-run" in sys.argv
REFRESH = "--refresh" in sys.argv

def _ts():
    return int(time.time() * 1000)

def _uid():
    return str(uuid.uuid4())[:8]


def entry(id_, topic, category, content, learning_type="RESILIENCE",
          confidence=0.95, severity="HIGH", permanent=True,
          tags=None, related_provider=None, resolution=None,
          solutions=None, context=None, error_count=0, times_applied=0,
          success=None, quality_score=0.9):
    """Build a SystemLearning document matching SystemLearning.java model."""
    return {
        "id": id_,
        "topic": topic,
        "category": category,
        "content": content,
        "learningType": learning_type,
        "confidenceScore": confidence,
        "severity": severity,
        "permanent": permanent,
        "tags": tags or [],
        "relatedProvider": related_provider,
        "resolution": resolution or (solutions[0] if solutions else ""),
        "solutions": solutions or [],
        "context": context or {},
        "errorCount": error_count,
        "timesApplied": times_applied,
        "success": success if success is not None else True,
        "qualityScore": quality_score,
        "learnedAt": time.strftime("%Y-%m-%dT%H:%M:%S"),
        "learnedFrom": "RESILIENCE_SEED_V2",
        "timestamp": _ts(),
        "version": 2,
        "obsolete": False,
        "critical": True,
    }


# ══════════════════════════════════════════════════════════════════════════════
#  SYSTEM-LEARNING ENTRIES — Failure Recovery & Resilience
# ══════════════════════════════════════════════════════════════════════════════

RESILIENCE_LEARNINGS = {

    # ─── CASCADING FAILURE HANDLING ───────────────────────────────────────────
    "rl-0001": entry(
        id_="rl-0001",
        topic="Cascading Failure Pattern Recognition & Recovery",
        category="RESILIENCE",
        confidence=0.97,
        content=(
            "[RESILIENCE — Cascading Failure Recovery]\n\n"
            "Pattern: Multiple AI models fail simultaneously due to shared dependency or provider outage.\n\n"
            "RECOGNITION:\n"
            "  FAILED last 60s: A ≥ 3 providers → 500/503 errors → no successful responses\n"
            "  Queue depth > 2x normal for < threshold window → suppression of retry storms\n\n"
            "IMMEDIATE MITIGATION:\n"
            "  Step 1. Open circuit breakers for all failing providers (shake them off the mesh)\n"
            "  Step 2. Throttle remaining healthy providers to 50% request rate (Bulkhead pattern)\n"
            "  Step 3. Upgrade service tier from FULL → DEGRADED → EMERGENCY via config flag\n"
            "  Step 4. Notify admin dashboard: cascade.detected=<true>, cascade.level=<depth>\n\n"
            "REPAIR:\n"
            "  1. Isolate shared dependency (DB, Redis, DNS) — check ONLY that first\n"
            "  2. Confirm non-failing provider is still accepting traffic\n"
            "  3. Send probe: async ping to each quarantined provider, 30 s apart\n"
            "  4. Auto-close circuit breaker if 1 probe succeeds (gradual warm-up)\n"
            "  5. Restore: 10% → 30% → 60% → 100% traffic incrementally (no sudden jumps)\n\n"
            "LEARNING: capture cascade depth + provider list → system_learning category=RESILIENCE"
        ),
        tags=["cascading-failure", "cascade", "provider-failure", "circuit-breaker"],
        severity="CRITICAL",
        solutions=[
            "Step 1: Open all circuit breakers for failing providers immediately.",
            "Step 2: Throttle remaining healthy providers to 50% capacity.",
            "Step 3: Upgrade to DEGRADED or EMERGENCY mode; fall back to local seed.",
            "Step 4: Diagnose root cause — shared dependency, network, provider outage, or code regression.",
            "Step 5: Send probes every 30s to quarantined providers; auto-close circuit on 1 success.",
            "Step 6: Gradual traffic ramp: 10% → 30% → 60% → 100% over recovery window.",
        ],
        error_count=0,
    ),

    # ─── COMPLETE AI BLACKOUT ────────────────────────────────────────────────
    "rl-0002": entry(
        id_="rl-0002",
        topic="Complete AI Provider Blackout — Thunder Mode Activation",
        category="RESILIENCE",
        confidence=0.99,
        content=(
            "[RESILIENCE — Complete AI Blackout / Thunder Mode]\n\n"
            "Definition: ALL helper AI providers are non-responsive (circuit breakers OPEN ≥ threshold), "
            "AND Firestore memory unavailable, AND no provider returning any response.\n\n"
            "WHAT IS STILL RUNNING:\n"
            "  • Spring Boot JVM (SupremeAI Core process)\n"
            "  • HTTP endpoints: actuator/health, /admin, /api/health\n"
            "  • local core_knowledge.json (offline templates)\n"
            "  • In-memory cache accumulated BEFORE blackout\n\n"
            "THUNDER MODE RESPONSE TEMPLATE:\n"
            '  "[LOCAL-SEED] <template answer from core_knowledge.json>\\n\\n  '
            '  NOTE: Full AI capabilities are offline. '
            '  Operating on local knowledge seed only. '
            '  Contact admin to restore AI providers."\n\n'
            "RESTART CHECKLIST (try in order):\n"
            "  1. ./kill-switch diagnose                          # identify failing tier\n"
            "  2. ps aux | grep java                               # confirm process exists\n"
            "  3. killall -9 java                                  # force stop\n"
            "  4. ./gradlew clean                                  # clear old build\n"
            "  5. ./gradlew bootRun --stacktrace                   # rebuild & start\n"
            "  6. curl http://localhost:8080/actuator/health        # verify\n"
            "  7. Admin panel: /admin > AI Providers > Re-activate  # restore providers\n\n"
            "NO data loss: All operations are safe. Only AI-generation is downgraded to local seed."
        ),
        tags=["thunder-mode", "blackout", "complete-failure", "all-ai-down", "emergency"],
        severity="CRITICAL",
        solutions=[
            "1. Verify SupremeAI Core JVM alive: ps aux | grep java",
            "2. Kill and restart JVM: killall -9 java; ./gradlew clean bootRun",
            "3. Verify health endpoint: curl http://localhost:8080/actuator/health",
            "4. Re-activate at least 1 provider from admin panel",
            "5. Confirm transition: local-seed → degraded → full (watch circuit breaker states)",
        ],
        error_count=0,
    ),

    # ─── SELF-HEALING MEMORY ──────────────────────────────────────────────────
    "rl-0003": entry(
        id_="rl-0003",
        topic="Self-Healing Memory Pattern — Learn from failures without external AI",
        category="RESILIENCE",
        confidence=0.96,
        content=(
            "[RESILIENCE — Self-Healing Memory]\n\n"
            "Goal: System must recover and learn from failures WHILE operating in offline mode, "
            "without waiting for external AI to be restored.\n\n"
            "SELF-HEALING TRIGGERS (automatic):\n"
            "  • 5x consecutive 5xx errors within 60s\n"
            "  • Build loop failure > 3 consecutive attempts\n"
            "  • Firestore memory unavailable > 120 s\n"
            "  • All circuit breakers OPEN for > 30s\n"
            "  • Unknown error signature not in local knowledge seed\n\n"
            "SELF-REPAIR STEPS:\n"
            "  1. Snapshot: capture last 200 log lines + error signature hash\n"
            "  2. Search: local solution memory for exact or similar signature\n"
            "  3. Match > 0.85 confidence: auto-apply highest-scoring verified fix\n"
            "  4. Match 0.5–0.85: suggest fix to admin, wait for confirmation (30 s timeout)\n"
            "  5. Match < 0.5: create new system_learning entry with UNKNOWN flag\n"
            "  6. Log to activity_log with isSelfHealed=true and result applied fix SHA\n"
            "  7. Notify admin via dashboard widget self-healing-updated=true\n\n"
            "RULE: Self-Repair applies only non-destructive verified fixes. "
            "Never truncate, delete, or overwrite data."
        ),
        tags=["self-healing", "auto-recovery", "offline-recovery", "knowledge-apply"],
        severity="HIGH",
        solutions=[
            "Enable self-healing trigger flags in production config.",
            "Verify local solution memory is pre-warmed at startup (loadMemories on boot).",
            "Set isSelfHealed=true on all auto-applied fixes in the activity_log.",
        ],
        error_count=0,
    ),

    # ─── PROVIDER HEALTH-QUARANTINE ───────────────────────────────────────────
    "rl-0004": entry(
        id_="rl-0004",
        topic="Provider Health Check & Auto-Quarantine — Isolate bad providers before cascade",
        category="RESILIENCE",
        confidence=0.95,
        content=(
            "[RESILIENCE — Provider Health Check & Auto-Quarantine]\n\n"
            "WHY: A single bad provider can trigger cascading failure if not isolated early.\n\n"
            "HEALTH CHECK INTERVAL: Every 30s per provider\n"
            "  GET /health  →  expect 200 + { status: \"ok\", latencyMs: < 500 }\n"
            "  3 consecutive failures → MARK UNHEALTHY → enter QUARANTINE\n\n"
            "QUARANTINE BEHAVIOR:\n"
            "  1. All active routes skip quarantined provider (auto re-route)\n"
            "  2. Admin alerted via dashboard widget: provider_quarantined=true\n"
            "  3. Log entry: provider=<id>, reason=<error_sig>, quarantinedAt=<ts>\n"
            "  4. Probe every 10 min (gradual warm-up): 1 test request → success → RELEASE\n\n"
            "MANUAL OVERRIDE:\n"
            "  - Admin: /admin > AI Providers > Quarantine / Release\n"
            "  - API: POST /api/admin/providers/{id}/quarantine  (force-quarantine any provider)\n"
            "  - API: POST /api/admin/providers/{id}/release      (force-release from quarantine)\n\n"
            "CRITICAL RULE: Never quarantine ALL providers. Always keep ≥ 1 active to serve."
        ),
        tags=["provider-health", "quarantine", "auto-isolation", "health-check"],
        severity="HIGH",
        solutions=[
            "Configure health-check ping URL in provider registration (field: healthCheckUrl).",
            "Set circuit breaker failure threshold to 3 rapid failures per provider.",
            "Alert admin dashboard widget on provider quarantine: widget=true, providerId=<id>.",
        ],
        error_count=0,
    ),

    # ─── GRACEFUL DEGRADATION TIERS ──────────────────────────────────────────
    "rl-0005": entry(
        id_="rl-0005",
        topic="Graceful Degradation Tiers — Progressive quality reduction instead of total failure",
        category="RESILIENCE",
        confidence=0.94,
        content=(
            "[RESILIENCE — Graceful Degradation Tiers]\n\n"
            "Instead of failing all at once when AI providers degrade, "
            "progressively reduce capability while keeping system operational.\n\n"
            "TIER 1 — FULL (all providers ≥ 80% availability):\n"
            "  All AI features enabled: generation, routing, memory, multi-model voting\n"
            "  Timeout per AI call: 30s\n\n"
            "TIER 2 — DEGRADED (≥ 1 provider < 80%):\n"
            "  Disable: creative gen, long-form response, multi-model voting\n"
            "  Keep: code gen templates, error fix lookup, admin routing\n"
            "  Timeout: 15s | Banner: \"Some AI features reduced\"\n\n"
            "TIER 3 — EMERGENCY (≥ 2 providers < 50% + Firestore slow):\n"
            "  Disable: all external AI calls\n"
            "  Serve: core_knowledge.json + in-memory cache only\n"
            "  Timeout: 5s | Banner: \"[OFFLINE] Cloud features unavailable\"\n\n"
            "TIER 4 — ADMIN-ONLY (total blackout confirmed):\n"
            "  /api/* → 503 Service Unavailable + Retry-After header\n"
            "  /admin, /actuator/health → still accessible\n"
            "  Local file/journal operations → still allowed\n"
            "  Timeout: 2s\n\n"
            "CONFIG KEY: system.degradation-tier = FULL | DEGRADED | EMERGENCY | ADMIN_ONLY"
        ),
        tags=["graceful-degradation", "tiers", "progressive-reduction", "availability"],
        severity="HIGH",
        solutions=[
            "Set system.degradation-tier config in application.yml per environment.",
            "Monitor provider availability every 30s and auto-upgrade/downgrade tier.",
            "Display user-facing banners clearly so users understand reduced capability.",
        ],
        error_count=0,
    ),

    # ─── CONFIDENCE-WEIGHTED MODEL VOTING ────────────────────────────────────
    "rl-0006": entry(
        id_="rl-0006",
        topic="Confidence-Weighted Model Voting — Combine answers from multiple providers intelligently",
        category="RESILIENCE",
        confidence=0.93,
        content=(
            "[RESILIENCE — Confidence-Weighted Model Voting]\n\n"
            "Use when multiple providers answer the same task and answers differ.\n\n"
            "ALGORITHM:\n"
            "  1. Each provider returns (response, confidenceScore: 0.0–1.0)\n"
            "  2. Weighted_sum = Σ (response_confidence × confidenceScore)\n"
            "  3. Final answer = response with highest combined (confidence + weight) score\n"
            "  4. Quality gate: if max_confidence < 0.55 → FALLBACK to core_knowledge.json\n\n"
            "SINGLE MODEL FALLBACK:\n"
            "  If only 1 provider active and confidence < 0.3 → return but flag LOW_CONFIDENCE\n"
            "  Display response with disclaimer: \"[LOW_CONFIDENCE]\" prefix\n\n"
            "CONFIGURATION:\n"
            "  - voting.confidence_gate = 0.55        # minimum for any model to be accepted\n"
            "  - voting.single_model_min = 0.30       # below this, response is flagged\n"
            "  - voting.tiebreak = HIGHEST_CONFIDENCE  # tie resolution strategy\n\n"
            "EXAMPLE:\n"
            "  Claude: 0.82 → \"Use JPA save()\"\n"
            "  Gemini: 0.68 → \"Use JPA repository\"\n"
            "  GPT:    0.55 → \"Use JDBC\"\n"
            "  → Winner: Claude (highest combined confidence)"
        ),
        tags=["voting", "confidence-weighted", "multi-model", "quality-gate", "fallback"],
        severity="MEDIUM",
        solutions=[
            "Always collect confidence scores from provider responses.",
            "Reject and fall back to local seed if all providers return confidence below 0.55.",
            "Log weighted voting decision to activity_log for model quality analysis.",
        ],
        error_count=0,
    ),

    # ─── PROVIDER MIGRATION & KNOWLEDGE TRANSFER ─────────────────────────────
    "rl-0007": entry(
        id_="rl-0007",
        topic="Provider Migration & Knowledge Transfer — Swap providers without losing learned knowledge",
        category="RESILIENCE",
        confidence=0.92,
        content=(
            "[RESILIENCE — Provider Migration & Knowledge Transfer]\n\n"
            "When retiring or replacing a provider (e.g. old API → new API), "
            "migrate its knowledge losslessly to prevent capability regression.\n\n"
            "STEP 1 — SCHEDULE BLACKOUT (do NOT delete old provider yet):\n"
            "  a. Set isActive=false in Firestore (preserves data)\n"
            "  b. Open circuit breaker, throttle traffic to 0%\n"
            "  c. Notify admin: provider=MIGRATING, new_provider=<name>\n\n"
            "STEP 2 — COPY KNOWLEDGE:\n"
            "  a. Fetch all system_learning entries where relatedProvider=<old-provider>\n"
            "  b. Write copies with relatedProvider=<new-provider> + version=+1\n"
            "  c. Validate: cross-sample 10 responses old vs new for > 90% match\n\n"
            "STEP 3 — GRADUAL RAMP (warm up new provider):\n"
            "  1. Set new provider priority=1, old provider priority=999\n"
            "  2. Traffic split: new 10% → 30% → 60% → 100% over 30 min\n"
            "  3. Monitor: success rate, latency, error count at each step\n\n"
            "STEP 4 — ARCHIVE (after 7 days, success rate > 95%):\n"
            "  a. Mark old provider ARCHIVED\n"
            "  b. Move orguidated entries to system_learning_archive collection\n"
            "  c. Update core_knowledge.json: replace old provider references everywhere\n\n"
            "EVENT LOG: Write every migration step to activity_log with isMigration=true."
        ),
        tags=["provider-migration", "knowledge-transfer", "provider-replace", "knowledge-migration"],
        severity="MEDIUM",
        solutions=[
            "NEVER delete a provider before knowledge is transferred to replacement.",
            "Always run at least 24 h of parallel traffic (both old + new running).",
            "Update core_knowledge.json provider references immediately after archive.",
        ],
        error_count=0,
    ),

    # ─── KNOWLEDGE SEED REBUILD ───────────────────────────────────────────────
    "rl-0008": entry(
        id_="rl-0008",
        topic="Knowledge Seed Rebuild — Reconstruct knowledge from Firestore backup when local seed is lost",
        category="RESILIENCE",
        confidence=0.96,
        content=(
            "[RESILIENCE — Knowledge Seed Rebuild]\n\n"
            "The KNOWLEDGE SEED is the authoritative local copy (in priority order):\n"
            "  Layer 1: Firestore system_learning collection (HIGHEST priority — live AI knowledge)\n"
            "  Layer 2: autonomous_seed_knowledge.json (STATIC — committed to repo)\n"
            "  Layer 3: core_knowledge.json (EMERGENCY — bare minimum templates)\n\n"
            "REBUILD COMMAND:\n"
            "  python scripts/seed_massive_knowledge.py --rebuild-seed\n"
            "  output: target/knowledge_seed_rebuilt.json\n\n"
            "REBUILD PROCESS:\n"
            "  1. FETCH: all system_learning docs where permanent=true from Firestore\n"
            "  2. MERGE:  with autonomous_seed_knowledge.json entries\n"
            "  3. FILTER: apply confidence threshold — keep if conf ≥ 0.7; flag 0.3–0.7 for review\n"
            "  4. DEDUPLICATE: sha256(content) — if duplicate, keep highest confidence entry\n"
            "  5. SCORE: calculate reliabilityScore per entry:\n"
            "     reliabilityScore = (successCount / max(1, timesApplied)) × confidenceScore\n"
            "     if reliabilityScore < 0.3 after 5+ applications → FLAG FOR REVIEW\n"
            "  6. PROMOTE: entries with conf > 0.9 eligible for auto-promotion to core_knowledge.json\n"
            "  7. WRITE: merged, scored, deduplicated knowledge to target/knowledge_seed_rebuilt.json\n\n"
            "When Firestore unavailable: use autonomous_seed_knowledge.json as rebuild source.\n"
            "SCHEDULE: Daily at 03:00 via cron: 0 3 * * * python scripts/seed_massive_knowledge.py --rebuild-seed"
        ),
        tags=["seed-rebuild", "seed-regeneration", "knowledge-reconstruct", "cron"],
        severity="MEDIUM",
        solutions=[
            "Run: python scripts/seed_massive_knowledge.py --rebuild-seed",
            "Check logs: target/knowledge_seed_rebuilt.log (inspect deduplicate/remove/promote summary).",
            "If Firestore unavailable during rebuild: system falls back to autonomous_seed_knowledge.json.",
        ],
        error_count=0,
    ),

    # ─── MODEL HEALTH MONITORING ─────────────────────────────────────────────
    "rl-0009": entry(
        id_="rl-0009",
        topic="Model Health Monitoring — Detect degrading AI models BEFORE they cause failures",
        category="RESILIENCE",
        confidence=0.93,
        content=(
            "[RESILIENCE — Model Health Monitoring]\n\n"
            "Proactive health tracking for each AI model to detect degradation before cascade.\n\n"
            "METRICS TRACKED PER MODEL (rolling 30 min windows):\n"
            "  • success_rate  = successful_responses / total_responses\n"
            "  • error_rate    = error_responses / total_responses\n"
            "  • latency_p99   = 99th percentile response time\n"
            "  • quality_score = avg(response_confidence) over all successful responses\n"
            "  • circuit_state = CLOSED | OPEN | HALF_OPEN (from Resilience4j)\n\n"
            "DEGRADATION DETECTION (trigger → action):\n"
            "  • success_rate < 0.7 AND error_rate > 0.3 for 10 min\n"
            "    → FLAG: model_degrading=true, increase circuit breaker sensitivity\n"
            "  • quality_score < 0.5 for 20+ responses\n"
            "    → FLAG: response_quality_low, log low-quality samples to system_learning\n"
            "  • latency_p99 > 3× provider SLA for 15 min\n"
            "    → FLAG: latency_degrading, check provider status page\n"
            "  • circuit_state == OPEN for > 60s\n"
            "    → ACTION: auto-quarantine provider, switch to next in priority chain\n\n"
            "HEALTH DASHBOARD ENDPOINTS:\n"
            "  GET /api/admin/providers/health          # all providers health summary\n"
            "  GET /api/admin/providers/{id}/health      # single provider detailed health\n"
            "  GET /api/admin/system/degradation-tier    # current system tier"
        ),
        tags=["health-monitoring", "model-health", "degradation-detection", "proactive"],
        severity="HIGH",
        solutions=[
            "Enable model health scoring in AIProviderDiscoveryService (track rolling windows).",
            "Expose health summary via GET /api/admin/providers/health for dashboard.",
            "Set alert threshold: notify admin when any provider health drops below 0.7.",
        ],
        error_count=0,
    ),

    # ─── ERROR SIGNATURE LEARNING ─────────────────────────────────────────────
    "rl-0010": entry(
        id_="rl-0010",
        topic="Error Signature Learning — Learn fix patterns from recurring errors even when models fail",
        category="RESILIENCE",
        confidence=0.94,
        content=(
            "[RESILIENCE — Error Signature Learning]\n\n"
            "Even when external AI fails, the local system can learn fix patterns from error logs:\n\n"
            "STEP 1 — HASH THE ERROR:\n"
            "  Generate a stable error_signature from the stack trace:\n"
            "  1. Extract exception class name (e.g. HikariCP$ConnectionTimeoutException)\n"
            "  2. Extract method name + line number from stack trace top frame\n"
            "  3. Apply: sha256(exception_class + method_name + line_number) → error_signature\n"
            "  4. Normalize: remove Thread-IDs, timestamps, non-deterministic values\n\n"
            "STEP 2 — LOOKUP LOCAL MEMORY:\n"
            "  Query local_solution_memory (pre-warmed at boot) for error_signature\n"
            "  HIT: apply verified fix immediately\n"
            "  MISS: create TEMP entry in system_learning with error_signature + UNKNOWN flag\n\n"
            "STEP 3 — CONFIRM OR REJECT:\n"
            "  If attempt auto-fix succeeds → update system_learning entry to confidence += 0.1\n"
            "  If attempt fails → increment failureCount, if failureCount > 3 → flag INCORRECT_FIX\n"
            "  If user applies manual fix → record user_correction with details\n\n"
            "STEP 4 — PERSIST:\n"
            "  Promoted entries (confidence ≥ 0.8) → write to Firestore system_learning\n"
            "  Confirm core_knowledge.json updated if pattern is emergency-relevant\n\n"
            "This loop runs WITHOUT external AI — purely local computation."
        ),
        tags=["error-signature", "hash-matching", "local-learning", "fix-pattern"],
        severity="HIGH",
        solutions=[
            "Implement error_signature hashing (sha256 of exception + method + line).",
            "Pre-warm local_solution_memory at JVM boot from Firestore system_learning.",
            "Log all UNKNOWN errors to system_learning with error_signature for later analysis.",
        ],
        error_count=0,
    ),

    # ─── RETRY BUDGET & BACKOFF ───────────────────────────────────────────────
    "rl-0011": entry(
        id_="rl-0011",
        topic="Retry Budget & Exponential Backoff — Prevent retry storms when models are failing",
        category="RESILIENCE",
        confidence=0.96,
        content=(
            "[RESILIENCE — Retry Budget & Backoff]\n\n"
            "When models are failing, naive retries amplify load on already-unhealthy providers.\n\n"
            "RETRY BUDGET (per-provider, per-time-window):\n"
            "  - Maximum retries: 3 per time_window (e.g. 60 s)\n"
            "  - Once budget exhausted → DO NOT retry until budget window resets\n"
            "  - Reset: every 60 s (config: retry.budget.window_seconds=60)\n"
            "  - Track in-memory: Map<providerId, {count, windowStartTs}>\n\n"
            "EXPONENTIAL BACKOFF SCHEDULE:\n"
            "  Attempt 1: delay=0ms (immediate)\n"
            "  Attempt 2: delay=1000ms\n"
            "  Attempt 3: delay=4000ms\n"
            "  Attempt 4: CANCELLED — budget exhausted → next provider or local seed\n\n"
            "BULKHEAD ISOLATION:\n"
            "  - 1 Scheduler/ThreadPool per provider (independent pools, no bleed-over)\n"
            "  - Pool size = provider.max_parallel_connections (default: 5)\n"
            "  - One slow/failing provider CANNOT starve other providers of threads\n\n"
            "FAIL-FAST TIMEOUTS:\n"
            "  - Provider call timeout = max(2 × p99_latency, 10000ms) (minimum 10 s)\n"
            "  - Circuit-breaker open delay = 60 s default\n"
            "  - If timeout hit 3x in window → open circuit immediately"
        ),
        tags=["retry-budget", "backoff", "bulkhead", "fail-fast", "storm-prevention"],
        severity="HIGH",
        solutions=[
            "Configure retry budget: retry.max_retries=3, retry.window_seconds=60 in application.yml.",
            "Enable Bulkhead pattern: separate thread pool per provider (Resilience4j threading).",
            "Set circuit-breaker failure threshold: circuitBreaker.failure-rate-threshold=50%.",
            "Log all budget-exhaust events to system_learning for runbook generation.",
        ],
        error_count=0,
    ),

    # ─── SMART ROUTER RECOVERY ────────────────────────────────────────────────
    "rl-0012": entry(
        id_="rl-0012",
        topic="Smart Router Self-Healing — Router automatically selects working model when others fail",
        category="RESILIENCE",
        confidence=0.92,
        content=(
            "[RESILIENCE — Smart Router Self-Healing]\n\n"
            "The SupremeLearningOrchestrator routes every request to the best available model. "
            "When a model fails, the router must recover WITHOUT admin intervention.\n\n"
            "Q-LEARNING ROUTER (existing EnhancedSelfLearningRouter):\n"
            "  - Maintains Q(s, a) table: state = task type; action = provider selected\n"
            "  - Boltzmann exploration: prob(choice) = softmax(Q(s, provider) / temperature)\n"
            "  - On success: Q(s, provider) += alpha × (reward − Q(s, provider))\n"
            "  - On failure: Q(s, provider) -= alpha × penalty\n\n"
            "SELF-HEALING ROUTER RECOVERY:\n"
            "  Step 1. Detect: Q-table shows state has 0 viable actions (all penalized to -inf)\n"
            "  Step 2. Reset exploration penalty: re-initialize Q values for that state\n"
            "  Step 3. Assign random exploration priority to each provider (normalized)\n"
            "  Step 4. Apply Boltzmann: first-choice provider with non-zero probability\n"
            "  Step 5. Monitor first 3 responses: if all failures → escalate to core_knowledge.json\n"
            "  Step 6. Log reset event: state=<task_type>, reason=routing_exhaustion\n\n"
            "REINITIALIZATION TRIGGER: Only when ALL providers for a state fail (not 1 or 2).\n"
            "EVENT LOG: router_reset=true in activity_log."
        ),
        tags=["smart-router", "q-learning", "self-healing-router", "exploration"],
        severity="MEDIUM",
        solutions=[
            "Monitor Q-table health: alert when any state shows 0 viable actions.",
            "Implement safe Q-table reset (not full wipe — only exhausted states).",
            "Log router_reset events to system_learning with routing statistics.",
        ],
        error_count=0,
    ),

    # ─── FALLBACK CHAIN COMPLETENESS ──────────────────────────────────────────
    "rl-0013": entry(
        id_="rl-0013",
        topic="Fallback Chain Completeness — Every task must have ≥ 2 fallback levels before reaching local seed",
        category="RESILIENCE",
        confidence=0.96,
        content=(
            "[RESILIENCE — Fallback Chain Completeness]\n\n"
            "Every task in core_knowledge.json MUST have a complete fallback chain "
            "of at least 2 levels before reaching the emergency static response.\n\n"
            "MANDATORY 4-LEVEL CHAIN:\n"
            "  Level 1 — External AI (Admin-configured helpers, highest priority)\n"
            "  Level 2 — Firestore /memory (SolutionMemoryRepository — cached AI responses)\n"
            "  Level 3 — Local Seed (core_knowledge.json — pre-programmed templates)\n"
            "  Level 4 — Emergency (static built-in response — last resort only)\n\n"
            "VALIDATION RULE: No task schema is accepted unless Level 3 has a defined response.\n"
            "  Task schema in core_knowledge.json MUST have: { task: <keywords>, solution: <non-empty> }\n"
            "  Any entry with empty solution → REJECTED at validation → flagged to admin\n\n"
            "AUDIT:\n"
            "  Run: python scripts/audit_fallback_chain.py\n"
            "  Checks: every task has ≥ 3 keywords, every task has non-empty solution,\n"
            "          no duplicate task keywords, at least 20 fallback tasks covering ERROR_* patterns\n"
            "  Exit code 0 = PASS, 1 = FAIL with report written to audit_report.json\n\n"
            "AUDIT SCHEDULE: Every deployment via CI pipeline step: audit-fallback-chain"
        ),
        tags=["fallback-chain", "validation", "audit", "chain-completeness"],
        severity="CRITICAL",
        solutions=[
            "Run: python scripts/audit_fallback_chain.py before every deploy.",
            "Ensure every task keyword in core_knowledge.json is ≥ 3 chars and unique.",
            "Add at least 20 ERROR_* pattern tasks to core_knowledge.json for offline diagnostics.",
        ],
        error_count=0,
    ),

    # ─── INCIDENT LEARNING FEEDBACK LOOP ─────────────────────────────────────
    "rl-0014": entry(
        id_="rl-0014",
        topic="Incident Learning Feedback Loop — Systemic learning from every outage or model failure event",
        category="RESILIENCE",
        confidence=0.94,
        content=(
            "[RESILIENCE — Incident Learning Feedback Loop]\n\n"
            "EVERY failure event (model down, provider timeout, DB error, self-heal run) must be "
            "captured, analysed, and fed back into the learning system — no exceptions.\n\n"
            "POST-INCIDENT CAPTURE (automatic):\n"
            "  1. EVENT LOGGED: activity_log with type=INCIDENT, status=RESOLVED|UNRESOLVED\n"
            "  2. SIGNATURE CAPTURED: error_signature hash + first 3 stack frames\n"
            "  3. TIMELINE LOGGED: detected_at, escalated_at, resolved_at, resolution_duration_ms\n"
            "  4. PROVIDERS LISTED: which providers were failing, their circuit states during event\n"
            "  5. ACTIONS TAKEN: every self-heal, restart, or manual action with timestamp\n\n"
            "ANALYSIS (runs post-resolution within 5 min):\n"
            "  1. Root cause classification: NETWORK | CODE | PROVIDER | CONFIG | DATABASE\n"
            "  2. Was this a known pattern? Search system_learning for matching signature\n"
            "  3. Known → auto-update: timesApplied += 1, lastUsed = now, add timeline to metadata\n"
            "  4. Unknown → create new system_learning entry with root_cause from analysis\n"
            "  5. Seasonality check: has this same signature fired 3+ times in 30 days? → flag TRENDING\n"
            "  6. Recurring (> 5 occurrences in 30 days) → escalate to admin → mark for code fix\n\n"
            "REPORT: Weekly incident summary sent to admin dashboard covering:\n"
            "  top_recurring_errors, avg_recovery_time, self_heal_success_rate, trend_flags"
        ),
        tags=["incident-learning", "post-mortem", "feedback-loop", "trending"],
        severity="HIGH",
        solutions=[
            "Implement IncidentLearningService to orchestrate post-resolution analysis.",
            "Auto-create system_learning entries for all UNKNOWN error signatures.",
            "Send weekly incident digest to admin dashboard every Monday at 09:00.",
        ],
        error_count=0,
    ),

    # ─── SEED GENERATION & KNOWLEDGE BASE STORES ─────────────────────────────
    "rl-0015": entry(
        id_="rl-0015",
        topic="Seed Generation & Knowledge Base Stores — Multi-source knowledge merging with priority ordering",
        category="RESILIENCE",
        confidence=0.95,
        content=(
            "[RESILIENCE — Seed Generation & Knowledge Base Stores]\n\n"
            "SupremeAI maintains THREE layers of knowledge — ordered by fallback priority:\n\n"
            "LAYER 1 — Firestore system_learning (highest authority):\n"
            "  • Dynamically learned from live session corrections, error fixes, and incident analysis\n"
            "  • Source is external AI responses that passed the quality gate\n"
            "  • Treated as ground truth when available\n\n"
            "LAYER 2 — Autonomous Seed (autonomous_seed_knowledge.json committed to repo):\n"
            "  • Static knowledge committed to source control\n"
            "  • 53+ seed items across 10 categories (APP_CREATION, ERROR_SOLVING, ARCHITECTURE, etc.)\n"
            "  • Survives repo redeploys even if Firestore is wiped\n\n"
            "LAYER 3 — Core Knowledge Seed (core_knowledge.json — emergency templates):\n"
            "  • Small, hand-picked set of critical offline responses\n"
            "  • Used only when Layers 1+2 are unavailable\n"
            "  • Tags: [OFFLINE], [LOCAL-SEED] on all responses\n\n"
            "SEED MERGE ORDER: Layer 1 (idempotent) ⊕ Layer 2 ⊕ Layer 3\n"
            "Conflict resolution: higher-authority layer wins; within same layer → highest confidenceScore wins\n"
            "Cold start: pre-warm SeedStore from Layer 1 at JVM boot (loadMemories())"
        ),
        tags=["seed-generation", "knowledge-layers", "merge", "cold-start", "priority"],
        severity="HIGH",
        solutions=[
            "Load Layer 1 at JVM startup to reduce cold-start latency (loadMemories in SupremAIApplication).",
            "Implement separation of concerns: SeedStore manager class handles layer loading & merging.",
            "Record seed version hash in application startup log for audit trail.",
        ],
        error_count=0,
    ),
}


def main():
    # Preview
    print("=" * 65)
    print("  SUPREMEAI RESILIENCE KNOWLEDGE SEED — system_learning")
    print("=" * 65)
    print(f"\nEntries to seed: {len(RESILIENCE_LEARNINGS)}")
    for entry_id, doc in RESILIENCE_LEARNINGS.items():
        print(f"  [{entry_id}] {doc['topic'][:60]}")

    if DRY_RUN:
        print("\n[dry-run] No changes written. Run without --dry-run to seed Firestore.")
        return

    import firebase_admin
    from firebase_admin import credentials, firestore

    cred_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS", "")
    if cred_path and os.path.exists(cred_path):
        cred = credentials.Certificate(cred_path)
    else:
        cred = credentials.ApplicationDefault()

    if not firebase_admin._apps:
        firebase_admin.initialize_app(cred, {"projectId": FIREBASE_PROJECT_ID})

    db = firestore.client()

    batch = db.batch()
    items_written = 0

    # If --refresh, delete existing resilience entries first
    if REFRESH:
        print("\n[refresh] Removing existing rl-* entries from system_learning ...")
        docs = db.collection("system_learning").stream()
        to_delete = []
        for doc in docs:
            if doc.id.startswith("rl-"):
                to_delete.append(db.collection("system_learning").document(doc.id))
        if to_delete:
            del_batch = db.batch()
            for ref in to_delete:
                del_batch.delete(ref)
            del_batch.commit()
            print(f"  Deleted {len(to_delete)} old resilience entries.")

    for entry_id, doc in RESILIENCE_LEARNINGS.items():
        ref = db.collection("system_learning").document(entry_id)
        batch.set(ref, doc, merge=True)
        items_written += 1

    batch.commit()
    print(f"\n[commit] Written {items_written} resilience learning entries to system_learning.")
    print("Done ✓")


if __name__ == "__main__":
    main()
