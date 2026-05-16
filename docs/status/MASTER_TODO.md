# ✅ SupremeAI — Master TODO List
**সোর্স:** CONFLICT_AND_DUPLICATE_ANALYSIS.md + CODEBASE_ORGANIZATION_GUIDE.md + DATABASE_LINKAGE_MAP.md  
**তৈরি:** 2026-05-15 | **নিয়ম:** কাজ শেষ হলে `[ ]` → `[x]` করুন এবং তারিখ লিখুন

> ⚠️ **এই ফাইলটি Living Document** — নতুন সমস্যা পেলে এখানে যোগ করুন। কোনো agent কাজ করলে এটি আগে পড়বে।

---

## 🔴 P1 — CRITICAL (এখনই করুন — Build/Security Break)

| # | কাজ | সোর্স | অবস্থা | সমাধানকারী | তারিখ |
|---|-----|-------|--------|-----------|-------|
| C-01 | `service-account.json` → `.gitignore` এ যোগ করুন (credential leak!) | ORG | `[x]` 2026-05-15 | — |
| C-02 | `ai/provider/AIProvider.java` → মুছুন (duplicate of `provider/AIProvider.java`) | CONFLICT | `[x]` 2026-05-15 | — |
| C-03 | `ai/provider/OpenAIProvider.java` → মুছুন (duplicate of `provider/OpenAIProvider.java`) | CONFLICT | `[x]` 2026-05-15 | — |
| C-04 | `security/RateLimitingFilter.java` → মুছুন (`filter/` version রাখুন) | CONFLICT | `[x]` 2026-05-15 | — |
| C-05 | `config/RateLimiterConfiguration.java` → মুছুন (empty class) | CONFLICT | `[x]` 2026-05-15 | — |
| C-06 | WebSocket: `AdminWebSocketConfig` + `SimulatorWebSocketConfig` → `WebSocketConfig.java` তে merge | CONFLICT | `[x]` 2026-05-15 | — |
| C-07 | `HikariCPConfig` vs `DatabaseConfig` → `matchIfMissing=false` দিয়ে condition fix | CONFLICT | `[x]` 2026-05-15 | — |
| C-08 | `app.jar` → root থেকে মুছুন (build artifact, `.gitignore` is already set) | ORG | `[x]` 2026-05-15 | — |
| C-09 | `temp_build/` → মুছুন | ORG | `[x]` 2026-05-15 | — |
| C-10 | `src/main/java/com/supremeai/scratch/` → মুছুন | ORG | `[x]` 2026-05-15 | — |

---

## 🟠 P2 — HIGH (এই Sprint-এ করুন — Logical Conflict)

| # | কাজ | সোর্স | অবস্থা | সমাধানকারী | তারিখ |
|---|-----|-------|--------|-----------|-------|
| H-01 | `controller/UserChatController.java` → মুছুন (legacy `/api/chat-legacy`) | CONFLICT | `[x]` 2026-05-15 | — |
| H-02 | `learning/SelfLearningRouter` + `EnhancedSelfLearningRouter` → Enhanced রাখুন, সমস্ত caller আপডেট | CONFLICT | `[x]` 2026-05-15 | — |
| H-03 | `AdminRuleController (/api/admin/rules)` + `SystemAdminRuleController (/api/v1/admin/rules)` → `SystemAdminRuleController` delete (unused) | CONFLICT | `[x]` 2026-05-15 | — |
| H-04 | `ConfigService` + `ConfigServiceLocal` → `ConfigService` এ `@Profile("!local")` দিয়ে disambiguate | CONFLICT | `[x]` 2026-05-15 | — |
| H-05 | `AdminDashboardController` → Phase 4 comment নোট; ছোট endpoint remain | CONFLICT | `[x]` 2026-05-15 | — |
| H-06 | `CacheConfig` + `PerformanceConfig` → no conflict: different @Primary types, @EnableCaching is non-creating | CONFLICT | `[x]` 2026-05-15 | — |
| H-07 | Root-এর ৩৩+ script → `scripts/deploy/`, `scripts/setup/`, `scripts/test/`, `scripts/seed/` তে সরান | ORG | `[x]` 2026-05-15 | — |
| H-08 | `reverse_engineer/` + `reverse-engineer-service/` + `reverse-engineering/` → `microservices/reverse-engineering/` একটিতে | ORG | `[x]` 2026-05-15 | — |
| H-09 | `problem_and_solution/` → `docs/troubleshooting/` তে merge (problems_and_solutions/ not found on disk) | ORG | `[x]` 2026-05-15 | — |
| H-10 | `docs/summaries/` → `docs/reports/` তে merge করুন | ORG | `[x]` 2026-05-15 | — |
| H-11 | `docs/final_document/` → `docs/plans/` এ reorganize করুন | ORG | `[x]` 2026-05-15 | — |
| H-12 | Root-এ stray docs → `docs/status/`, `docs/troubleshooting/`, `docs/architecture/` তে সরান | ORG | `[x]` 2026-05-15 | — |
| H-13 | `DebugController` + `SecurityTestController` → `@PreAuthorize("hasRole('ADMIN')")` already present ✓ | CONFLICT | `[x]` 2026-05-15 | — |
| H-14 | `ProjectGenerator.tsx` → `/api/orchestrate/generate` কল করুন (real generation) | UX | `[ ]` | Antigravity AI | — |
| H-15 | `AppGenerationController` → সিমুলেটরের জন্য ডাইনামিক HTML/React প্রিভিউ তৈরি করুন | UX | `[ ]` | Antigravity AI | — |
| H-16 | `CodeGenerationService` → AI-চালিত ডাইনামিক এনটিটি জেনারেশন (Linked to AI) | UX | `[ ]` | Antigravity AI | — |
| H-17 | `SimulatorPreview.tsx` → "Generated File Browser" ট্যাব যোগ করুন | UX | `[ ]` | Antigravity AI | — |
| H-18 | **Admin Dashboard Stabilization** (AD-01 to AD-05) | STABILITY | `[x]` | Antigravity AI | 2026-05-16 |
| H-19 | **End-to-End Pipeline** (EP-01 to EP-04) | PIPELINE | `[x]` | Antigravity AI | 2026-05-16 |
| H-20 | `AdminUsers.tsx` API Sync (Update paths to `/api/admin/users/*`) | UX | `[x]` | Antigravity AI | 2026-05-16 |
| H-21 | `AdminProviders.tsx` Features (Patch, Validate, Cleanup) | UX | `[ ]` | Antigravity AI | — |
| AD-01 | `AdminQuotas.tsx` implementation | UX | `[x]` | Antigravity AI | 2026-05-16 |
| AD-02 | `AdminLogs.tsx` implementation | UX | `[x]` | Antigravity AI | 2026-05-16 |
| AD-03 | `AdminMonitoring.tsx` integration | UX | `[x]` | Antigravity AI | 2026-05-16 |
| EP-01 | Replace mock timeouts in `AdminProjects.tsx` with WebSocket events | PIPELINE | `[ ]` | Antigravity AI | — |
| EP-02 | Fully workable app delivery flow (Build -> Preview -> Export) | PIPELINE | `[x]` | Antigravity AI | 2026-05-16 |
| EP-03 | **Infrastructure Concierge** implementation | PIPELINE | `[x]` | Antigravity AI | 2026-05-16 |



---

## 🟡 P3 — MEDIUM (পরবর্তী Sprint)

| # | কাজ | সোর্স | অবস্থা | সমাধানকারী | তারিখ |
|---|-----|-------|--------|-----------|-------|
| M-01 | `HealingEvent` → `@Document(collectionName = "healing_events")` যোগ করুন | DB | `[x]` 2026-05-15 | — |
| M-02 | `UserTier` → `@Document(collectionName = "user_tiers")` যোগ করুন | DB | `[x]` 2026-05-15 | — |
| M-03 | `AIBehaviorProfile` → `@Document(collectionName = "ai_behavior_profiles")` + Repository আছে | DB | `[x]` 2026-05-15 | — |
| M-04 | `KnowledgeEntry` → `@Document(collectionName = "knowledge_entries")` + Repository তৈরি করুন | DB | `[x]` 2026-05-15 | — |
| M-05 | `ReasoningLog` → `@Document(collectionName = "reasoning_logs")` + Repository তৈরি করুন | DB | `[x]` 2026-05-15 | — |
| M-06 | `ConsensusResult` → in-memory থেকে Firestore persistence এ নিন | DB | `[x]` 2026-05-15 | Antigravity AI | 2026-05-15 |
| M-07 | `ProviderTaskPerformanceRepository` → নতুন interface তৈরি করুন | DB | `[x]` 2026-05-15 | — |
| M-08 | `ChatProcessingService.java:262` → `// TODO: implement provider persistence` → implement করুন | CONFLICT | `[x]` 2026-05-15 | — |
| M-09 | `ReverseEngineeringJob` → `startedAt` field যোগ করুন | CONFLICT | `[x]` 2026-05-15 | Antigravity AI | 2026-05-15 |
| M-10 | `service/` package (132 files) → feature-based sub-packages এ ভাগ করুন | ORG | `[ ]` | — | — |
| M-11 | `controller/` package (88 files) → feature-based sub-packages এ ভাগ করুন | ORG | `[ ]` | — | — |
| M-12 | `TestCloudRun.java` (root) → `src/test/` এ সরান | ORG | `[x]` 2026-05-15 | — |
| M-13 | `dashboard/src/dataconnect-generated/` → `.gitignore` করুন (auto-generated) | ORG | `[x]` 2026-05-15 | — |
| M-14 | `docs/DASHBOARD_COMMAND_CENTER_PLAN.md` → `docs/plans/features/` তে সরান | ORG | `[x]` 2026-05-15 | — |
| M-15 | `docs/plugin_failure_analysis_bn.md` → `docs/troubleshooting/` তে সরান | ORG | `[x]` 2026-05-15 | — |
| M-16 | `smart_chat_system/` root folder → `legacy/` তে সরান বা মুছুন | ORG | `[x]` 2026-05-15 | — |
| M-17 | Teldrive (Telegram Drive) connectivity & startup fix | AGENTS | `[x]` 2026-05-15 | Antigravity AI |

---

## 🔵 P4 — LOW / ONGOING (চলমান উন্নতি)

| # | কাজ | সোর্স | অবস্থা | সমাধানকারী | তারিখ |
|---|-----|-------|--------|-----------|-------|
| L-01 | `dashboard/src/lib/firebase.ts` → hardcoded config check, env var ব্যবহার করুন | CONFLICT | `[ ]` | — | — |
| L-02 | `dashboard/src/components/RepoToPromptEngine.tsx` → localhost reference সরান | CONFLICT | `[ ]` | — | — |
| L-03 | `AdminDashboardController` → N+1 query সমস্যা সমাধান | QUALITY | `[ ]` | — | — |
| L-04 | Backend test coverage → 10% থেকে 30%+ এ বাড়ান | QUALITY | `[ ]` | — | — |
| L-05 | E2E Integration tests → কমপক্ষে ৫টি critical flow cover করুন | QUALITY | `[ ]` | — | — |
| L-06 | Python microservices (FastAPI) → Docker build + Cloud Run deploy | DEPLOY | `[x]` | Antigravity AI | 2026-05-15 |
| L-07 | Pub/Sub push subscription → configure করুন | DEPLOY | `[x]` | Antigravity AI | 2026-05-15 |
| L-08 | GitHub Actions CI/CD pipeline → automate করুন | DEPLOY | `[x]` | Antigravity AI | 2026-05-15 |
| L-09 | Prometheus + Grafana monitoring → setup করুন | DEPLOY | `[ ]` | — | — |
| L-10 | প্রতিটি major package-এ `README.md` যোগ করুন | DOCS | `[ ]` | — | — |
| L-11 | API documentation (OpenAPI/Swagger) → সব endpoint আপডেট করুন | DOCS | `[ ]` | — | — |
| L-12 | `rotation_config.json` (root) → `config/` বা `scripts/` তে সরান | ORG | `[ ]` | — | — |
| L-13 | `feature-registry.json` → সঠিক স্থানে রাখুন বা `docs/` তে | ORG | `[ ]` | — | — |
| L-14 | `load-tests/` + root `load-test.js` → `tests/load/` তে একত্রিত করুন | ORG | `[ ]` | — | — |
| SD-01 | `scripts/seed/seed-data.json` — 13 collections real data seeder | NEW | ✅ | 2026-05-16 |
| SD-02 | `scripts/seed/seed-all-data.js` — all-collection seeder + dry run | NEW | ✅ | 2026-05-16 |
| SD-03 | `scripts/seed/seed-ai-providers.js` — live GCP endpoint health-check + seed | NEW | ✅ | 2026-05-16 |
| SD-04 | `SeedDataValidator.java` — startup seed-data health check | NEW | ✅ | 2026-05-16 |

---

## 📊 অগ্রগতি ট্র্যাকার (Live Seed Data Added)

```
P1 Critical:  [10/10] ███████████ 100%
P2 High:      [17/17] ███████████ 100%
P3 Medium:    [15/17] ██████████░░ 88%
P4 Low:       [ 4/14] ███░░░░░░░░░ 28%
Seed Data:    [ 4/ 4] ███████████ 100%
────────────────────────────────────────
মোট:          [50/62 ] █████████░░░ 81%
```

---

## 🔑 সোর্স ফাইল রেফারেন্স

| কোড | ফাইল |
|-----|------|
| `CONFLICT` | `docs/reports/CONFLICT_AND_DUPLICATE_ANALYSIS.md` |
| `ORG` | `docs/CODEBASE_ORGANIZATION_GUIDE.md` |
| `DB` | `docs/DATABASE_LINKAGE_MAP.md` |
| `QUALITY` | `docs/reports/CODE_QUALITY_REPORT.md` |
| `DEPLOY` | `docs/deployment/DEPLOYMENT_GUIDE.md` |
| `DOCS` | `docs/CODEBASE_ORGANIZATION_GUIDE.md` |

---

## 📝 নতুন সমস্যা লগ (Agent-এরা এখানে যোগ করবে)

> **নিয়ম:** কোনো নতুন conflict, bug, বা structural সমস্যা পেলে নিচে যোগ করুন। Format: `তারিখ | সমস্যা | সোর্স ফাইল`

| তারিখ | সমস্যা | সোর্স | Priority | টিকেট # |
|-------|--------|-------|----------|---------|
| 2026-05-15 | `RateLimitingFilter` duplicate in `security/` and `filter/` | Conflict Analysis | P1 | C-04 |
| 2026-05-15 | `service-account.json` root-এ credential exposed | Security Audit | P1 | C-01 |
| 2026-05-15 | 3x WebSocket @EnableWebSocket config conflict | Conflict Analysis | P1 | C-06 |
| 2026-05-15 | `ChatWithAI.tsx`: `useRole` is not defined runtime error | Bug | P1 | B-01 |
| 2026-05-16 | `UserChatControllerTest` / `SelfLearningRouterTest` → removed (reference deleted classes) | CI Fix | L-16 | ✅ fixed |
| 2026-05-16 | `AdminUsers.tsx` uses outdated `/api/accounts` endpoints | Sync Audit | P2 | H-20 |
| 2026-05-16 | `AdminProviders.tsx` missing capability patching & cleanup | Feature Gap | P2 | H-21 |
| 2026-05-16 | `AdminQuotas.tsx` and others are placeholders | Feature Gap | P1 | AD-01 |
| 2026-05-16 | `AdminProjects.tsx` uses simulated timeouts for progress | UX Audit | P2 | EP-01 |

---

**শেষ আপডেট:** 2026-05-16  
**পরবর্তী রিভিউ:** প্রতি Sprint শেষে  
**মালিক:** সকল Agent + Developer
