# 🏆 MASTER TODO — SupremeAI Final Phase Roadmap
> অডিট তারিখ: 2026-05-24 | সংস্করণ: 6.0
> নীতি: Zero Hardcode AI Model · Solo Mode First · Zero/Low Maintenance · Flawless Code

---

## ✅ Phase 0 — পূর্বে সম্পন্ন (Completed)

- [x] P0-01: Firebase emulator config non-fatal (null return instead of throw)
- [x] P0-02: AutonomousVotingService — hardcoded provider list → Firestore query
- [x] P0-03: ProvidersSuggestionController — COMMON_PROVIDERS → ProviderMetadataService live data
- [x] P0-04: AIFallbackOrchestrator — airllm-sidecar default → ProviderRepository dynamic resolve
- [x] P0-05: AIProviderFactory.resolveModel() — "default" fallback removed → explicit IllegalStateException
- [x] P0-06: ContextualAIRankingService — hardcoded scores → DB-sourced metadata
- [x] P0-07: VisionService — "default-vision-model" fallback removed → null skips call
- [x] P0-08: AIFallbackOrchestrator — soloMode flag via @PostConstruct
- [x] P0-09: SelfHealingService RCA catch block → recordFailedCorrection() called
- [x] P0-10: SoloModeHealthController → real state from AIFallbackOrchestrator.getSoloMode()
- [x] P0-11: AIProviderFactory.injectMetadataService() → setter injection
- [x] P0-12: javax.annotation → jakarta.annotation migration (7 files)
- [x] P0-13: Docker-compose.yml — real credentials replaced with CHANGE_ME placeholders
- [x] P0-14: Docker healthcheck directives — all services covered
- [x] P0-15: Solo-mode regression tests added
- [x] P0-16: KnowledgeSeedDataProvider.java — duplicate makeLearning/makeErrorSolution fixed (B-01)
- [x] P0-17: DynamicInstructionService.java — getApplicableTaskTypes()/getContent() added (B-02)
- [x] P0-18: ApiResponse.java — @Builder added (B-03)
- [x] P0-19: InfiniteAutoHealer.java — Mono<String> type fixed (B-04)
- [x] P0-20: SelfHealingService.java — APIHealthReport methods added (B-05)
- [x] P0-21: SelfHealingService.java — RootCauseAnalysis .block() type fixed (B-06)
- [x] P0-22: SelfHealingService.java — AIProviderType + Schedulers imports fixed (B-07, B-08)
- [x] P0-23: BrowserService.java — BrowserTask Lombok @Data added (B-09)
- [x] P0-24: AgentOrchestrationHub.java — Map type fixed (B-10)
- [x] P0-25: SelfHealingController.java — detectAndFix() + missing stubs fixed (B-11)
- [x] P0-26: BrowserService.getCredentialContext() — password redacted (S-01)
- [x] P0-27: AI-01→AI-06: Context compression, streaming, weighted consensus, search engine, vector memory, MAD enhancement
- [x] P0-28: BV-01→BV-05: Benchmark & Validation Suite (code complete)
- [x] P0-29: ZM-01→ZM-05: Zero Maintenance Architecture (code complete, partial stubs)
- [x] P0-30: SL-01→SL-04: Solo Mode Intelligence (code complete, simulation stage)
- [x] P0-31: CD-03→CD-05: ProjectDNA, Cost Transparency, One-Click Deploy
- [x] P0-32: PC-01: Selective re-query for disagreeing providers ✅ (implemented in EnhancedMultiAIConsensusService)

---

## 🔴 PHASE 1 — Market Testing Ready (সর্বোচ্চ অগ্রাধিকার)
> লক্ষ্য: স্থিতিশীল, নিরাপদ, শূন্য-hardcode, solo-mode সক্ষম সিস্টেম যা বাজারে পরীক্ষার উপযোগী

---

### 🔴 BLOCKER-A — নতুন Build ত্রুটি (Date → LocalDateTime মাইগ্রেশন)
> **সূত্র:** বর্তমান বিল্ড output — 52টি স্থানে `Date cannot be converted to LocalDateTime`

- [ ] **BA-01: [CRITICAL]** `EnhancedLearningService.java` (5 স্থান) — `new java.util.Date()` → `LocalDateTime.now()` এবং `.before(date)` → `.isBefore(LocalDateTime)` রিপ্লেস
- [ ] **BA-02: [CRITICAL]** `AuditLoggingAspect.java` (1 স্থান) — `Date.from(Instant.now())` → `LocalDateTime.now()`
- [ ] **BA-03: [CRITICAL]** `UserCodeLearningService.java` (4 স্থান) — `new java.util.Date()` → `LocalDateTime.now()`
- [ ] **BA-04: [CRITICAL]** `BrowserService.java` (1 স্থান) — `new Date()` → `LocalDateTime.now()`
- [ ] **BA-05: [CRITICAL]** অন্যান্য ফাইল: `LearningAdminController`, `TaskAssignmentController`, `AutomaticTaskAssigner`, `LearningArchiveService`, `MultiAIConsensusService`, `MultiAIVotingService`, `ReverseEngineeringIntegrationService` — একই pattern fix
- [ ] **BA-06: [CRITICAL]** বিল্ড verify: `./gradlew build -x test` — **0 compilation errors**

---

### 🔴 BLOCKER-B — কোড কাঠামো ত্রুটি (Code Structure)

- [ ] **CS-01: [CRITICAL]** `FaithfulnessTrendWidget.tsx` Java service ডিরেক্টরিতে আছে → `dashboard/src/components/` তে সরানো
- [ ] **B-13:** `./gradlew test` — **0 test failures** (target ≥ 40% coverage)

---

### 🔴 Security — Must Fix Before Any Public User

- [x] S-01: **[DONE]** `BrowserService.getCredentialContext()` — password redacted
- [ ] **S-02:** Audit all `@RestController` DTOs (50 DTO files আছে, validation coverage ~88%) — add `@Valid` + `@NotBlank`/`@NotNull` যেখানে নেই
- [ ] **S-03:** Verify GCP Secret Manager is the **only** credential source in production; env var fallback audit
- [ ] **S-04:** Add GCP Billing Alerts at $10 / $50 / $100 thresholds (Cloud Console + Terraform/CLI)

---

### 🔴 Zero Hardcode Validation — বাকি

- [ ] **ZH-01:** Full codebase grep — remaining hardcoded model names audit (completed for known list)
- [ ] **ZH-A:** `ContextualAIRankingService.java:163` — `model.contains("gpt-4")` → Firestore-backed tier config
- [ ] **ZH-B:** `CostTransparencyReportService.java:25` — `"gpt-4"` hardcoded test data → move to test class or config
- [ ] **ZH-C:** `OpenAIProvider.java:17,78` — `"gpt-4"` list + `"gpt-3.5"` check → load from Firestore `provider_models` collection
- [ ] **ZH-D:** `BenchmarkController.java:55` — `"gpt-4", 0.892` hardcoded score → load from Firestore `benchmark_results`
- [ ] **ZH-E:** `SoloModeManagerService.java:41` — `"Phi-3-mini"` model name → `@Value("${solo.fallback.model:phi-3-mini}")` config
- [ ] **ZH-02:** `EnhancedMultiAIConsensusService.triggerDebate()` — judge selection ✅ (already dynamic via `aiRankingService`)
- [ ] **ZH-03:** `KnowledgeService.processLearningJob()` — max step limit configurable via `@Value`
- [ ] **ZH-04:** `buildDiscussionContext()` — null guard before `.substring()` ✅ (already fixed)

---

### 🟡 Solo Mode — Core Independence

- [ ] **SM-01:** Wire Playwright into `soloModeAnswerAndLearn()` — replace HTTP-only scraping with real browser navigation
- [ ] **SM-02:** Add step limit (max 15 steps) + timeout (5 min) guard in `executeAutonomousStep()` via `SystemWorkRuleService`
- [ ] **SM-03:** `recoverFailedProviders()` — currently empty stub → implement actual provider health recheck + reactivation logic
- [ ] **SM-04:** Add graceful degradation in `executeAutonomousStep()` when VisionService is unavailable (fallback to DOM text only)
- [ ] **SM-05:** Verify Solo Mode boots and answers basic queries with **zero external AI providers active** (end-to-end test)

---

### 🟡 Knowledge System

- [ ] **K-01:** `core_knowledge.json` — expand to ≥ 5 entries per category for: AI provider management, User/permission management, Zero-AI offline operation (currently 0-2 entries each)
- [ ] **K-02:** Add Knowledge Bootstrap entries (≥ 5) for local AI model setup + P2P sync scenarios
- [ ] **K-03:** Verify `system_learning` Firestore collection is populated and `KnowledgeService` reads from it correctly at startup

---

### 🟡 Performance & Cost

- [ ] **PC-02:** Circuit Breaker auto-cooldown — verify quarantine 5 min after 3 failures (check `SelfHealingService.isProviderQuarantined()`)
- [ ] **PC-03:** Verify Caffeine response cache 5 min TTL active for identical questions (check `ResponseCacheService.java`)
- [ ] **PC-04:** Verify request hedging — top 2 providers parallel, first valid wins (check `RequestHedgingService.java`)

---

### 🟡 Dashboard & UX

- [ ] **D-01:** Cost dashboard endpoint (`GET /api/cost/realtime`) + minimal UI card in `ModernAdminDashboard.tsx`
- [ ] **D-02:** Bengali i18n coverage audit — ensure ≥ 95% of UI strings have bn.json translation
- [ ] **D-03:** WebSocket pipeline progress — end-to-end test from job submission to UI update
- [ ] **D-04:** Fix `tsc_errors.log` issues in dashboard (currently 0 TS errors — verify after any changes)

---

### 🟢 Infrastructure

- [ ] **I-01:** Docker resource limits — add `mem_limit` + `cpus` to all compose services (supremeai, dashboard, redis, airllm, teldrive, reverse-engineering)
- [ ] **I-02:** Delete duplicate `legacy/browser-automation-tool/` directory
- [ ] **I-03:** Clean root-level junk files: `BrowserException.java`, `TestCloudRun.java`, `compile_errors.txt`, `build_errors.txt`, পুরনো `GEMINI_AUDIT_REPORT*.md` ফাইলগুলো
- [ ] **I-04:** `.env.example` — verify all keys documented; `.env` — verify no real secrets committed to git
- [ ] **I-05:** `deploy.yml` — health check URL fix (নতুন revision URL ব্যবহার করা), Actions version upgrade (`@v4`), auto-rollback if error rate >1%

---

## 🏆 PHASE 2 — Beat Other AI / Top Rank Validation
> লক্ষ্য: Cursor, Replit, GitHub Copilot-কে ছাড়িয়ে AI validation benchmarks-এ শীর্ষে যাওয়া

---

### 🔴 Mock → Real Conversion (সর্বোচ্চ গুরুত্ব)

- [ ] **R-01: [CRITICAL]** `AutoProviderDiscoveryService.java` — `fetchMockProviders()` stub → real OpenRouter API (`GET https://openrouter.ai/api/v1/models`) + HuggingFace Hub API call; quality threshold filter (benchmark score > 0.7)
- [ ] **R-02: [CRITICAL]** `SoloModeManagerService.java` — `triggerLocalModelFallback()` simulated delay → real AirLLM sidecar API call (`POST /v1/chat/completions`) using `AIRLLM_ENDPOINT` env var
- [ ] **R-03:** `DatabaseSchemaMigrationService.java` — single migration check → real batch Firestore document migration with rollback support

---

### 🟡 Intelligence Upgrade — Phase 2 Verification

- [x] AI-01: Context Window Compression ✅
- [x] AI-02: Streaming Responses (SSE) ✅
- [x] AI-03: Weighted Consensus by Task Type ✅
- [x] AI-04: Search Intelligence Engine ✅
- [x] AI-05: Cross-Agent Vector Memory ✅
- [x] AI-06: MAD Enhancement — judge dynamic + confidence skip ✅

---

### 🟡 Benchmark & Validation Suite — Verification

- [x] BV-01: AI Validation Harness ✅
- [x] BV-02: SWE-bench style test set (50+ tasks) ✅
- [x] BV-03: Self-Ranking Dashboard ✅
- [x] BV-04: Provider Tournament Mode ✅
- [x] BV-05: Public benchmark endpoint ✅
- [ ] **BV-06:** Run actual benchmark tests → verify results store in Firestore `benchmark_results` collection → confirm `GET /api/benchmarks/public` returns live data

---

### 🟡 Solo Mode — Full Intelligence Verification

- [x] SL-01: Local AI Model Auto-Download (code ✅, real call needed via R-02)
- [x] SL-02: P2P Knowledge Sync ✅
- [x] SL-03: Offline Knowledge Distillation ✅
- [x] SL-04: Emergency Code Generation ✅
- [ ] **SL-05:** End-to-end Solo Mode integration test — shutdown all external APIs → verify system responds via SL-01/SL-04 fallback

---

### 🟡 Competitive Differentiators

- [ ] **CD-01:** VS Code Extension (`supremeai-vscode-extension/`) — complete REST API integration, streaming autocomplete, provider status sidebar
- [ ] **CD-02:** Flutter Mobile App (`supremeai/`) — consensus chat UI, provider status, knowledge explorer, push notifications
- [x] CD-03: ProjectDNA Harvester Enhancement ✅
- [x] CD-04: Cost Transparency Report ✅
- [x] CD-05: One-Click Deploy ✅

---

### 🟢 Zero Maintenance Architecture — Verification

- [x] ZM-01: Auto-Provider Discovery (code ✅, real API needed via R-01)
- [x] ZM-02: Self-Updating Knowledge ✅
- [x] ZM-03: Dependency CVE Auto-Scan ✅ (cve-scan.yml active)
- [x] ZM-04: Database Schema Auto-Migration (code ✅, real batch needed via R-03)
- [x] ZM-05: Zero-Downtime Deploy ✅ (deploy.yml active, fix needed via I-05)
- [ ] **ZM-06:** Verify all ZM workflows run successfully in GitHub Actions (dry-run with `workflow_dispatch`)

---

## 📊 Current Status Scorecard (V6)

| Dimension | Current Score | Phase 1 Target | Phase 2 Target |
|-----------|:---:|:---:|:---:|
| Zero Hardcode AI Model | 7.5/10 | 9.5/10 | 10/10 |
| Solo Mode Self-Sufficiency | 5/10 | 8/10 | 10/10 |
| Zero/Low Maintenance Cost | 6/10 | 8/10 | 9.5/10 |
| Code Structure & Quality | 5.5/10 🔴 | 8.5/10 | 9.5/10 |
| Security Posture | 6.5/10 | 9/10 | 9.5/10 |
| Knowledge System | 7/10 | 8.5/10 | 10/10 |
| Competitive Intelligence | 7/10 | 8/10 | 10/10 |
| **BUILD STATUS** | **🔴 BROKEN** | **✅ Green** | **✅ Green** |
| **Overall** | **6.1/10** | **8.5/10** | **9.7/10** |

---

## ⚠️ PHASE 1 CRITICAL PATH (এই ক্রমে করতে হবে)

```
BA-01 → BA-06 (Date→LocalDateTime Build Fix)
  ↓
CS-01 (TSX ফাইল সঠিক ডিরেক্টরিতে)
  ↓
BA-06 (Compile Clean ✅)
  ↓
ZH-A → ZH-E (Zero Hardcode Fix)
  ↓
S-02, S-04 (Security)
  ↓
SM-01 → SM-05 (Solo Mode)
  ↓
I-01, I-05 (Infrastructure)
  ↓
K-01 → K-03 (Knowledge)
  ↓
D-01 → D-04 (Dashboard)
  ↓
✅ MARKET TESTING READY
```

## ⚠️ PHASE 2 CRITICAL PATH

```
R-01 → R-03 (Mock → Real APIs)
  ↓
B-13 (Test Coverage ≥ 40%)
  ↓
CD-01 (VS Code Extension)
  ↓
CD-02 (Flutter App)
  ↓
BV-06, SL-05, ZM-06 (Verification)
  ↓
🏆 BEAT ALL AI — TOP RANK READY
```

---

*শেষ আপডেট: 2026-05-24 | রিপোর্ট ভার্সন: 6.0 | অডিট ফাইল: SUPREMEAI_PHASE_AUDIT_V6.md*
