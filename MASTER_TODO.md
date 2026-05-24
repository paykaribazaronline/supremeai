# 🏆 MASTER TODO — SupremeAI Final Phase Roadmap
> অডিট তারিখ: 2026-05-24 | সংস্করণ: 5.0
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

---

## 🔴 PHASE 1 — Market Testing Ready (সর্বোচ্চ অগ্রাধিকার)
> লক্ষ্য: স্থিতিশীল, নিরাপদ, শূন্য-hardcode, solo-mode সক্ষম সিস্টেম যা বাজারে পরীক্ষার উপযোগী

### 🔴 BLOCKER — Build Must Pass First (48h deadline)

- [ ] B-01: **[CRITICAL]** Fix `KnowledgeSeedDataProvider.java` — duplicate method definitions (`makeLearning` + `makeErrorSolution`) → rename/merge duplicates
- [ ] B-02: **[CRITICAL]** Fix `DynamicInstructionService.java` — `SystemInstruction.getApplicableTaskTypes()` + `getContent()` missing → add methods to model or fix service
- [ ] B-03: **[CRITICAL]** Fix `ApiResponse.java` — missing `@Builder` on class → add Lombok `@Builder` or manual builder
- [ ] B-04: **[CRITICAL]** Fix `InfiniteAutoHealer.java:28` — `Mono<String>` returned where `String` expected → subscribe/block correctly or change return type
- [ ] B-05: **[CRITICAL]** Fix `SelfHealingService.java` — `APIHealthReport` missing `incrementTotal/Active/Inactive/Error()` → add methods to model
- [ ] B-06: **[CRITICAL]** Fix `SelfHealingService.java:201` — `.block()` called on `RootCauseAnalysis` (not a Mono) → fix type mismatch
- [ ] B-07: **[CRITICAL]** Fix `SelfHealingService.java:383` — `AIProviderType` import missing → add correct import
- [ ] B-08: **[CRITICAL]** Fix `SelfHealingService.java:398` — `Schedulers` import missing → add `reactor.core.scheduler.Schedulers` import
- [ ] B-09: **[CRITICAL]** Fix `BrowserService.java` — `BrowserTask` model missing setters/getters (setId, setGoal, setStatus, getLastUrl, etc.) → add `@Data` Lombok or manual accessors
- [ ] B-10: **[CRITICAL]** Fix `AgentOrchestrationHub.java:69` — `Map<String,Object>` cannot convert to `Map<String,String>` → fix type or cast
- [ ] B-11: **[CRITICAL]** Fix `SelfHealingController.java` — `detectAndFix()` returns `Mono<ResponseEntity>` not `Map` → unwrap correctly; `getRootCauseAnalysisStats()` + `getRecentCorrections()` missing from service → add stubs
- [ ] B-12: Run `./gradlew build -x test` — confirm **0 compilation errors**
- [ ] B-13: Run `./gradlew test` — confirm **0 test failures** (target ≥ 40% coverage)

### 🔴 Security — Must Fix Before Any Public User

- [ ] S-01: **[CRITICAL]** `BrowserService.getCredentialContext()` — decrypted password/token appended to AI prompt → replace with `[REDACTED]` marker; inject via Playwright directly
- [ ] S-02: Audit all `@RestController` DTOs — add `@Valid` + `@NotBlank`/`@NotNull` annotations where missing
- [ ] S-03: Verify GCP Secret Manager is the **only** credential source in production (no env var fallback with real values)
- [ ] S-04: Add GCP Billing Alerts at $10 / $50 / $100 thresholds

### 🟡 Solo Mode — Core Independence

- [ ] SM-01: Wire Playwright into `soloModeAnswerAndLearn()` — replace HTTP-only scraping with real browser navigation
- [ ] SM-02: Add step limit (max 15 steps) + timeout (5 min) guard in `executeAutonomousStep()` via `SystemWorkRuleService`
- [ ] SM-03: `recoverFailedProviders()` — currently empty stub → implement actual provider health recheck + reactivation
- [ ] SM-04: Add graceful degradation in `executeAutonomousStep()` when VisionService is unavailable (fallback to DOM text only)
- [ ] SM-05: Verify Solo Mode boots and answers basic queries with **zero external AI providers active**

### 🟡 Zero Hardcode Validation

- [ ] ZH-01: Grep full codebase for any remaining hardcoded model names (e.g., `"gpt-4"`, `"claude"`, `"gemini"`) → replace all with Firestore-backed config
- [ ] ZH-02: `EnhancedMultiAIConsensusService.triggerDebate()` line 390 — judge `allProviders.get(0)` → `aiRankingService.getTopProvider()` dynamic selection
- [ ] ZH-03: `KnowledgeService.processLearningJob()` — add max step limit (configurable via `@Value`) + timeout guard
- [ ] ZH-04: `buildDiscussionContext()` — add null guard before `.substring()` call (prevents NPE)

### 🟡 Knowledge System

- [ ] K-01: `core_knowledge.json` — expand to ≥ 5 entries per category for: AI provider management, User/permission management, Zero-AI offline operation (currently 0-2 entries each)
- [ ] K-02: Add Knowledge Bootstrap entries (≥ 5) for local AI model setup + P2P sync scenarios
- [ ] K-03: Verify `system_learning` Firestore collection is populated and `KnowledgeService` reads from it correctly at startup

### 🟡 Performance & Cost

- [ ] PC-01: Consensus voting — selective re-query: Round 2+ queries only **disagreeing providers** (reduces API calls ~60%)
- [ ] PC-02: Add Circuit Breaker auto-cooldown — quarantine failed provider for 5 min after 3 failures
- [ ] PC-03: Implement Caffeine response cache (5 min TTL) for identical questions — reduces redundant API calls ~25%
- [ ] PC-04: Add request hedging — top 2 providers queried in parallel; first valid response wins (eliminates tail latency)

### 🟡 Dashboard & UX

- [ ] D-01: Cost dashboard endpoint (`GET /api/cost/realtime`) + minimal UI card in `ModernAdminDashboard.tsx`
- [ ] D-02: Bengali i18n coverage audit — ensure ≥ 95% of UI strings have bn.json translation
- [ ] D-03: WebSocket pipeline progress — end-to-end test from job submission to UI update
- [ ] D-04: Fix `tsc_errors.log` issues in dashboard (currently 0 TS errors — verify after any changes)

### 🟢 Infrastructure

- [ ] I-01: Docker resource limits — add `mem_limit` + `cpus` to all compose services
- [ ] I-02: Delete duplicate `legacy/browser-automation-tool/` directory
- [ ] I-03: Clean root-level junk files: `BrowserException.java`, `TestCloudRun.java`, `fix_chat*.js`, `compile_errors.txt`, `build_errors.txt`
- [ ] I-04: `.env.example` — verify all keys documented; `.env` — verify no real secrets committed

---

## 🏆 PHASE 2 — Beat Other AI / Top Rank Validation
> লক্ষ্য: Cursor, Replit, GitHub Copilot-কে ছাড়িয়ে AI validation benchmarks-এ শীর্ষে যাওয়া

### 🔴 Intelligence Upgrade

- [ ] AI-01: **Context Window Compression** — summarize conversation history when >4,000 tokens using fastest available model (Gemini Flash or equivalent). Target: 60% cost reduction on long conversations
- [ ] AI-02: **Streaming Responses** — return partial consensus results as each provider responds (Server-Sent Events); don't wait for all providers to finish
- [ ] AI-03: **Weighted Consensus by Task Type** — code tasks → weight GPT-class models higher; creative → Claude-class; math → reasoning-specialist models. Config stored in Firestore, zero hardcode
- [ ] AI-04: **Search Intelligence Engine** — `QueryClassifier` + `SearchEngineRegistry` with multi-source routing: StackOverflow, GitHub Issues, MDN, arXiv, Wikipedia. Solo Mode uses this before any AI API call
- [ ] AI-05: **Cross-Agent Vector Memory** — shared context store between Code Agent, Security Agent, Deploy Agent without re-prompting. Eliminate redundant context rebuilding
- [ ] AI-06: **Multi-Agent Debate Enhancement** — MAD judge selection fully dynamic; add confidence threshold: if both sides >0.85 confidence, skip debate to save cost

### 🟡 Benchmark & Validation Suite

- [ ] BV-01: Build **AI Validation Harness** — automated weekly test suite running standard prompts against all active providers + SupremeAI consensus; store results in Firestore `benchmark_results`
- [ ] BV-02: Implement **SWE-bench style test set** — 50+ real coding tasks with known correct outputs; measure SupremeAI pass rate vs single-model baselines
- [ ] BV-03: **Self-Ranking Dashboard** — display SupremeAI accuracy vs each provider on last 100 real queries; update in real-time
- [ ] BV-04: **Provider Tournament Mode** — monthly automated head-to-head provider comparison; auto-demote consistently underperforming providers
- [ ] BV-05: Publish benchmark results endpoint (`GET /api/benchmarks/public`) for transparent AI quality reporting

### 🟡 Solo Mode — Full Intelligence (No External AI Required)

- [ ] SL-01: **Local AI Model Auto-Download** — if all external providers fail, auto-pull smallest viable GGUF model (e.g., Phi-3-mini) via AirLLM sidecar
- [ ] SL-02: **P2P Knowledge Sync** — SupremeAI instances can share learned knowledge entries via signed Firestore writes (no central coordination server needed)
- [ ] SL-03: **Offline Knowledge Distillation** — periodic job compresses Firestore `system_learning` into updated `core_knowledge.json` offline snapshot
- [ ] SL-04: **Emergency Code Generation** — template-based code scaffolding that works with zero AI (pattern matching against `autonomous_seed_knowledge.json`)

### 🟡 Competitive Differentiators

- [ ] CD-01: **IDE Extensions** — VS Code extension (`supremeai-vscode-extension/`) + IntelliJ plugin (`supremeai-intellij-plugin/`) — complete REST API integration, streaming autocomplete
- [ ] CD-02: **Mobile App** — Flutter app (`supremeai/`) — consensus chat, provider status, knowledge explorer, push notifications
- [ ] CD-03: **ProjectDNA Harvester Enhancement** — expand beyond `build.gradle.kts` to scan all config files, detect frameworks, auto-tag project context in every prompt
- [ ] CD-04: **Cost Transparency Report** — per-user, per-session cost breakdown; export as CSV/PDF; zero manual effort
- [ ] CD-05: **One-Click Deploy** — from admin dashboard, deploy any generated project to Cloud Run / Firebase Hosting with a single button

### 🟢 Zero Maintenance Architecture

- [ ] ZM-01: **Auto-Provider Discovery** — background job scans OpenRouter/HuggingFace for new free/cheap models weekly; auto-registers in Firestore if passing quality threshold
- [ ] ZM-02: **Self-Updating Knowledge** — `SelfHealingService` failures automatically generate new `core_knowledge.json` entries; no manual curation needed
- [ ] ZM-03: **Dependency CVE Auto-Scan** — weekly GitHub Action running `./gradlew dependencyCheckAnalyze`; auto-creates PR if critical CVE found
- [ ] ZM-04: **Database Schema Auto-Migration** — Firestore collection schema changes detected and applied automatically via `@PostConstruct` migration service
- [ ] ZM-05: **Zero-Downtime Deploy** — Cloud Run traffic splitting: 10% → new revision → health check → 100%. Rollback auto-trigger if error rate >1%

---

## 📊 Current Status Scorecard

| Dimension | Current Score | Phase 1 Target | Phase 2 Target |
|-----------|:---:|:---:|:---:|
| Zero Hardcode AI Model | 8/10 | 9.5/10 | 10/10 |
| Solo Mode Self-Sufficiency | 5/10 | 8/10 | 10/10 |
| Zero/Low Maintenance Cost | 5/10 | 7/10 | 9/10 |
| Code Structure & Quality | 4/10 🔴 | 8/10 | 9/10 |
| Security Posture | 6/10 | 9/10 | 9.5/10 |
| Knowledge System | 7/10 | 8.5/10 | 10/10 |
| Competitive Intelligence | 6/10 | 7/10 | 10/10 |
| **BUILD STATUS** | **🔴 BROKEN** | **✅ Green** | **✅ Green** |
| **Overall** | **5.9/10** | **8.1/10** | **9.5/10** |

---

## ⚠️ PHASE 1 CRITICAL PATH (এই ক্রমে করতে হবে)

```
B-01 → B-11 (Build Fix)
  ↓
B-12 (Compile Clean)
  ↓
S-01 (Credential Leak Fix)
  ↓
SM-01 → SM-05 (Solo Mode)
  ↓
ZH-01 → ZH-04 (Zero Hardcode)
  ↓
PC-01 → PC-04 (Performance)
  ↓
D-01 → D-04 (Dashboard)
  ↓
✅ MARKET TESTING READY
```

---

*শেষ আপডেট: 2026-05-24 | রিপোর্ট ভার্সন: 5.0*
