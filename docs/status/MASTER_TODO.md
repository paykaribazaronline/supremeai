# ✅ SupremeAI — Master TODO List
**সোর্স:** CONFLICT_AND_DUPLICATE_ANALYSIS.md + CODEBASE_ORGANIZATION_GUIDE.md + DATABASE_LINKAGE_MAP.md + TODO_LIST.md  
**তৈরি:** 2026-05-15 | **নিয়ম:** কাজ শেষ হলে `[ ]` → `[x]` করুন এবং তারিখ লিখুন

> ⚠️ **এই ফাইলটি Living Document** — নতুন সমস্যা পেলে এখানে যোগ করুন। কোনো agent কাজ করলে এটি আগে পড়বে।

---

## 🔴 P1 — CRITICAL (এখনই করুন — Build/Security Break / Core UX)

| # | কাজ | সোর্স | অবস্থা | সমাধানকারী | তারিখ |
|---|-----|-------|--------|-----------|-------|
| C-01 | `service-account.json` → `.gitignore` এ যোগ করুন (credential leak!) | ORG | `[x]` | — | 2026-05-15 |
| C-02 | `ai/provider/AIProvider.java` → মুছুন (duplicate of `provider/AIProvider.java`) | CONFLICT | `[x]` | — | 2026-05-15 |
| C-03 | `ai/provider/OpenAIProvider.java` → মুছুন (duplicate of `provider/OpenAIProvider.java`) | CONFLICT | `[x]` | — | 2026-05-15 |
| C-04 | `security/RateLimitingFilter.java` → মুছুন (`filter/` version রাখুন) | CONFLICT | `[x]` | — | 2026-05-15 |
| C-05 | `config/RateLimiterConfiguration.java` → মুছুন (empty class) | CONFLICT | `[x]` | — | 2026-05-15 |
| C-06 | WebSocket: `AdminWebSocketConfig` + `SimulatorWebSocketConfig` → `WebSocketConfig.java` তে merge | CONFLICT | `[x]` | — | 2026-05-15 |
| C-07 | `HikariCPConfig` vs `DatabaseConfig` → `matchIfMissing=false` দিয়ে condition fix | CONFLICT | `[x]` | — | 2026-05-15 |
| C-08 | `app.jar` → root থেকে মুছুন (build artifact, `.gitignore` is already set) | ORG | `[x]` | — | 2026-05-15 |
| C-09 | `temp_build/` → মুছুন | ORG | `[x]` | — | 2026-05-15 |
| C-10 | `src/main/java/com/supremeai/scratch/` → মুছুন | ORG | `[x]` | — | 2026-05-15 |
| C-11 | **Firebase Service Account Hardcoded Secret Elimination**: `src/main/resources/firebase-service-account.json` এর রিয়েল কী সরিয়ে প্লেসহোল্ডার বসানো | SECURITY | `[x]` | Antigravity AI | 2026-05-19 |
| C-12 | **TypeScript Errors (dashboard/)**: `AdminDashboardUnified.tsx`, `AdminLogs.tsx`, `LauncherPage.tsx` ফিক্স | TODO_LIST | `[ ]` | — | — |
| C-13 | **GCP Key GUI Setup Wizard**: `/setup` রাউটে ড্র্যাগ-অ্যান্ড-ড্রপ কী আপলোড ইন্টারফেস তৈরি | TODO_LIST | `[x]` | Antigravity AI | 2026-05-19 |
| C-14 | **Demo/Sandbox Mode**: GCP ক্রেডেনশিয়াল ছাড়া প্রজেক্ট রান করার সুবিধা যোগ করা | TODO_LIST | `[x]` | Antigravity AI | 2026-05-19 |
| C-15 | **Security Vulnerability - Insecure JWT Storage**: `dashboard/src/lib/authUtils.ts` এ HttpOnly cookie বা secure storage ইমপ্লিমেন্ট করা | SECURITY | `[x]` | Antigravity AI | 2026-05-19 |
| C-16 | **Input Validation**: `src/main/java/com/supremeai/controller/` এর সব কন্ট্রোলারে `@Valid` ও DTO ভ্যালিডেশন যোগ | SECURITY | `[ ]` | — | — |
| C-17 | **N+1 Query & Performance**: `AdminDashboardController`, `SystemLearningController` এর N+1 কোয়েরি ফিক্স করা | PERF | `[ ]` | — | — |
| C-18 | **Blocking Calls in Reactive Chains**: `ChatProcessingService` সহ অন্যান্য রিয়েক্টিভ চেইনে `.subscribe()` বা blocking কল রিমুভ করা | PERF | `[ ]` | — | — |
| C-19 | **No Pagination**: সমস্ত রিপোজিটরিতে `Pageable` ইন্টারফেস ব্যবহার করে পেজিনেশন ইমপ্লিমেন্ট করা | PERF | `[ ]` | — | — |
| C-20 | **Firebase SDK Mismatch**: Login পেজে Compat SDK এবং React এ Modular SDK-এর অমিল দূর করা | UX/BOOTSTRAP | `[ ]` | — | — |
| C-21 | **Missing Firebase Environment Variables**: ড্যাশবোর্ডের জন্য প্রয়োজনীয় ফায়ারবেস এনভায়রনমেন্ট ভেরিয়েবল কনফিগার করা | CONFIG | `[ ]` | — | — |
| C-22 | **Hardcoded Secrets**: `build.gradle.kts`, `config/*.java`, `functions/index.js` থেকে হার্ডকোডেড সিক্রেট দূর করা | SECURITY | `[ ]` | — | — |
| C-23 | **Vision API Fallback**: `VisionService.java` এ Gemini ব্যর্থ হলে হার্ডকোডেড "mock" এর বদলে রিয়েল এরর ফেলা বা লোকাল এমুলেটর হ্যান্ডলিং | AI_FALLBACK | `[ ]` | — | — |
| C-24 | **Embedding Fallback**: `VectorSearchService.java` এ এমবেডিং ব্যর্থ হলে dummy zero-array এর বদলে সঠিক এরর প্রোপাগেশন বা গ্রেসফুল ফলব্যাক | AI_FALLBACK | `[ ]` | — | — |


---

## 🟠 P2 — HIGH (এই Sprint-এ করুন — Logical Conflict & Core Features)

| # | কাজ | সোর্স | অবস্থা | সমাধানকারী | তারিখ |
|---|-----|-------|--------|-----------|-------|
| H-01 | `controller/UserChatController.java` → মুছুন (legacy `/api/chat-legacy`) | CONFLICT | `[x]` | — | 2026-05-15 |
| H-02 | `learning/SelfLearningRouter` + `EnhancedSelfLearningRouter` → Enhanced রাখুন, সমস্ত caller আপডেট | CONFLICT | `[x]` | — | 2026-05-15 |
| H-03 | `AdminRuleController (/api/admin/rules)` + `SystemAdminRuleController (/api/v1/admin/rules)` → delete | CONFLICT | `[x]` | — | 2026-05-15 |
| H-04 | `ConfigService` + `ConfigServiceLocal` → `ConfigService` এ `@Profile("!local")` দিয়ে disambiguate | CONFLICT | `[x]` | — | 2026-05-15 |
| H-05 | `AdminDashboardController` → Phase 4 comment নোট; ছোট endpoint remain | CONFLICT | `[x]` | — | 2026-05-15 |
| H-06 | `CacheConfig` + `PerformanceConfig` → no conflict: different @Primary types | CONFLICT | `[x]` | — | 2026-05-15 |
| H-07 | Root-এর ৩৩+ script → `scripts/deploy/`, `scripts/setup/`, `scripts/test/`, `scripts/seed/` তে সরান | ORG | `[x]` | — | 2026-05-15 |
| H-08 | `reverse_engineer/` + `reverse-engineer-service/` + `reverse-engineering/` → `microservices/reverse-engineering/` | ORG | `[x]` | — | 2026-05-15 |
| H-09 | `problem_and_solution/` → `docs/troubleshooting/` তে merge | ORG | `[x]` | — | 2026-05-15 |
| H-10 | `docs/summaries/` → `docs/reports/` তে merge করুন | ORG | `[x]` | — | 2026-05-15 |
| H-11 | `docs/final_document/` → `docs/plans/` এ reorganize করুন | ORG | `[x]` | — | 2026-05-15 |
| H-12 | Root-এ stray docs → `docs/status/`, `docs/troubleshooting/`, `docs/architecture/` তে সরান | ORG | `[x]` | — | 2026-05-15 |
| H-13 | `DebugController` + `SecurityTestController` → `@PreAuthorize("hasRole('ADMIN')")` secure করা | CONFLICT | `[x]` | — | 2026-05-15 |
| H-14 | `ProjectGenerator.tsx` → `/api/orchestrate/generate` কল করুন (real generation) | UX | `[ ]` | — | — |
| H-15 | `AppGenerationController` → সিমুলেটরের জন্য ডাইনামিক HTML/React প্রিভিউ তৈরি করুন | UX | `[ ]` | — | — |
| H-16 | `CodeGenerationService` → AI-চালিত ডাইনামিক এনটিটি জেনারেশন (Linked to AI) | UX | `[ ]` | — | — |
| H-17 | `SimulatorPreview.tsx` → "Generated File Browser" ট্যাব যোগ করুন | UX | `[ ]` | — | — |
| H-18 | **Admin Dashboard Stabilization** (AD-01 to AD-05) | STABILITY | `[x]` | Antigravity AI | 2026-05-16 |
| H-19 | **End-to-End Pipeline** (EP-01 to EP-04) | PIPELINE | `[x]` | Antigravity AI | 2026-05-16 |
| H-20 | `AdminUsers.tsx` API Sync (Update paths to `/api/admin/users/*`) | UX | `[x]` | Antigravity AI | 2026-05-16 |
| H-21 | `AdminProviders.tsx` Features (Patch, Validate, Cleanup) | UX | `[ ]` | — | — |
| H-22 | **Security Services**: `SecurityMonitoringService`, `ThreatIntelligenceService`, `IncidentResponseService`, `ClientSecurityService` ইমপ্লিমেন্ট | SECURITY | `[ ]` | — | — |
| H-23 | **Security API**: `/api/admin/security/events`, `/api/admin/security/policies` এন্ডপয়েন্ট তৈরি | SECURITY | `[ ]` | — | — |
| H-24 | **Security UI**: ড্যাশবোর্ডে সিকিউরিটি মনিটরিং এবং পলিসি ম্যানেজমেন্ট প্যানেল যোগ করা | SECURITY | `[ ]` | — | — |
| H-25 | **Client Safety Features**: DDoS প্রোটেকশন ইন্টিগ্রেশন, WAF as a Service ও স্ক্যানিং এপিআই | SECURITY | `[ ]` | — | — |
| H-26 | **Client Management**: ক্লায়েন্ট রেজিস্ট্রেশন ফ্লো, SLA কনফিগারেশন এবং ইনসিডেন্ট রিপোর্টিং | SECURITY | `[ ]` | — | — |
| H-27 | **No Security Headers**: `SecurityConfig.java` তে CSP, X-Frame-Options ইত্যাদি যোগ করা | SECURITY | `[ ]` | — | — |
| H-28 | **SQL Injection Risk**: `CodeGenerationService` এবং `CodeGenerationServiceEnhanced` এ প্যারামিটারাইজড কোয়েরি ব্যবহার নিশ্চিত করা | SECURITY | `[ ]` | — | — |
| H-29 | **Synchronized Methods**: `ChatProcessingService.confirmItem()` এ ডিস্ট্রিবিউটেড লক (Redis) ব্যবহার করা | PERF | `[ ]` | — | — |
| H-30 | **Missing Cache TTL**: `AIProviderFactory` তে ConcurrentHashMap এর বদলে Redis বা Caffeine ক্যাшением ব্যবহার করা | PERF | `[ ]` | — | — |
| H-31 | **Code Duplication in AI Providers**: `OpenAIProvider`, `AnthropicProvider` ইত্যাদি প্রোভাইডারগুলোর জন্য কমন `BaseHttpProvider` এক্সট্রাক্ট করা | CODE | `[ ]` | — | — |
| H-32 | **No Rate Limiting on Auth Endpoints**: brute force/DDoS সুরক্ষার জন্য রেট লিমিটিং যোগ করা | SECURITY | `[ ]` | — | — |
| H-33 | **Circular Dependencies**: `EnhancedLearningService.java` ↔ `SystemLearningService.java` এর মধ্যকার সার্কুলার ডিপেন্ডেন্সি দূর করা | ARCH | `[ ]` | — | — |
| H-34 | **Business Logic in Controllers**: কন্ট্রোলার থেকে বিজনেস লজিক সার্ভিস লেয়ারে স্থানান্তরিত করা | ARCH | `[ ]` | — | — |
| H-35 | **Too Many Dependencies in Constructors**: `AdminDashboardController.java` (১৩টি প্যারামিটার) এর ডিপেন্ডেন্সি অপ্টিমাইজ বা Facade প্যাটার্ন ব্যবহার | ARCH | `[ ]` | — | — |
| H-36 | **AI Model Registry**: `AIProviderFactory` ও `ProviderTypeConfig` থেকে হার্ডকোডেড মডেলের নাম Firestore এ স্থানান্তর করা | ARCH | `[ ]` | — | — |
| H-37 | **Self-Learning Loop**: `SelfImprovementService` এর একটিভ স্ক্র্যাপিং ইন্টিগ্রেশনের সফল ভ্যালিডেশন নিশ্চিত করা | PIPELINE | `[ ]` | — | — |
| H-38 | Auto-Learning Notifications: লার্নিং অর্কেস্ট্রেটর পাইপলাইন ট্রিগার হলে ড্যাশবোর্ডে রিয়েল নোটিফিকেশন পাঠানো | UX | `[ ]` | — | — |
| H-39 | **Plan 24: MCP Tools Endpoints**: `MCPServerController` এ `tools/list` ও `tools/call` এন্ডপয়েন্ট ইমপ্লিমেন্ট করা | PLAN_24 | `[ ]` | — | — |
| H-40 | **Plan 24: HNSW Vector Search**: `SelfLearningRouter` এর জন্য HNSW বা কাস্টম হাইপার-ডাইমেনশনাল ভেক্টর সার্চ ইমপ্লিমেন্টেশন | PLAN_24 | `[ ]` | — | — |
| H-41 | **Plan 23: Dynamic JS Scraping**: `reverse_engineer` এ dynamic JS রেন্ডারিংয়ের জন্য Playwright বা অনুরূপ মডিউল ইন্টিগ্রেশন | PLAN_23 | `[ ]` | — | — |
| H-42 | **Plan 23: Anti-Detection Capabilities**: Scraper এ stealth মোড ও প্রক্সি এভয়েডেন্স মেকানিজম যুক্ত করা | PLAN_23 | `[ ]` | — | — |

| AD-01 | `AdminQuotas.tsx` implementation | UX | `[x]` | Antigravity AI | 2026-05-16 |
| AD-02 | `AdminLogs.tsx` implementation | UX | `[x]` | Antigravity AI | 2026-05-16 |
| AD-03 | `AdminMonitoring.tsx` integration | UX | `[x]` | Antigravity AI | 2026-05-16 |
| EP-01 | Replace mock timeouts in `AdminProjects.tsx` with WebSocket events | PIPELINE | `[ ]` | — | — |
| EP-02 | Fully workable app delivery flow (Build -> Preview -> Export) | PIPELINE | `[x]` | Antigravity AI | 2026-05-16 |
| EP-03 | **Infrastructure Concierge** implementation | PIPELINE | `[x]` | Antigravity AI | 2026-05-16 |

---

## 🟡 P3 — MEDIUM (পরবর্তী Sprint)

| # | কাজ | সোর্স | অবস্থা | সমাধানকারী | তারিখ |
|---|-----|-------|--------|-----------|-------|
| M-01 | `HealingEvent` → `@Document(collectionName = "healing_events")` যোগ করুন | DB | `[x]` | — | 2026-05-15 |
| M-02 | `UserTier` → `@Document(collectionName = "user_tiers")` যোগ করুন | DB | `[x]` | — | 2026-05-15 |
| M-03 | `AIBehaviorProfile` → `@Document(collectionName = "ai_behavior_profiles")` + Repository আছে | DB | `[x]` | — | 2026-05-15 |
| M-04 | `KnowledgeEntry` → `@Document(collectionName = "knowledge_entries")` + Repository তৈরি করুন | DB | `[x]` | — | 2026-05-15 |
| M-05 | `ReasoningLog` → `@Document(collectionName = "reasoning_logs")` + Repository তৈরি করুন | DB | `[x]` | — | 2026-05-15 |
| M-06 | `ConsensusResult` → in-memory থেকে Firestore persistence এ নিন | DB | `[x]` | Antigravity AI | 2026-05-15 |
| M-07 | `ProviderTaskPerformanceRepository` → নতুন interface তৈরি করুন | DB | `[x]` | — | 2026-05-15 |
| M-08 | `ChatProcessingService.java:262` → `// TODO: implement provider persistence` → implement করুন | CONFLICT | `[ ]` | — | — |
| M-09 | `ReverseEngineeringJob` → `startedAt` field যোগ করুন | CONFLICT | `[x]` | Antigravity AI | 2026-05-15 |
| M-10 | `service/` package (132 files) → feature-based sub-packages এ ভাগ করুন | ORG | `[ ]` | — | — |
| M-11 | `controller/` package (88 files) → feature-based sub-packages এ ভাগ করুন | ORG | `[ ]` | — | — |
| M-12 | `TestCloudRun.java` (root) → `src/test/` এ সরান | ORG | `[x]` | — | 2026-05-15 |
| M-13 | `dashboard/src/dataconnect-generated/` → `.gitignore` করুন (auto-generated) | ORG | `[x]` | — | 2026-05-15 |
| M-14 | `docs/DASHBOARD_COMMAND_CENTER_PLAN.md` → `docs/plans/features/` তে সরান | ORG | `[x]` | — | 2026-05-15 |
| M-15 | `docs/plugin_failure_analysis_bn.md` → `docs/troubleshooting/` তে সরান | ORG | `[x]` | — | 2026-05-15 |
| M-16 | `smart_chat_system/` root folder → `legacy/` তে সরান বা মুছুন | ORG | `[x]` | — | 2026-05-15 |
| M-17 | Teldrive (Telegram Drive) connectivity & startup fix | AGENTS | `[x]` | Antigravity AI | 2026-05-15 |
| M-18 | **AI Response Validation**: AI আউটপুট ভ্যালিডেশন, কনফিডেন্স স্কোর ও ফিডব্যাক লুপ তৈরি | TODO_LIST | `[ ]` | — | — |
| M-19 | **Performance Optimizations**: React.memo/useMemo ইমপ্লিমেন্টেশন এবং কোড স্প্লিটিং | TODO_LIST | `[ ]` | — | — |
| M-20 | **Usability Compliance**: এরর বাউন্ডারি, অ্যাক্সেসিবিলিটি (WCAG 2.1) ও লোডিং স্টেট যোগ | TODO_LIST | `[ ]` | — | — |
| M-21 | **Test Coverage Goals (20% Target)**: জাভা ব্যাকএন্ডের সিকিউরিটি ও কোর সার্ভিসের জন্য ইউনিট টেস্ট তৈরি করা (বর্তমান প্রকৃত কভারেজ ~8-10%) | TEST | `[ ]` | — | — |
| M-22 | **Documentation Indexing**: ক্রস-রেফারেন্স এবং একটি মাস্টার ডকুমেন্টেশন ইনডেক্স তৈরি করা | DOCS | `[ ]` | — | — |
| M-23 | **Repeated CRUD Patterns**: একাধিক সার্ভিসে CRUD প্যাটার্ন স্ট্যান্ডার্ডাইজ করা (`BaseService<T>` ব্যবহার করে) | CODE | `[ ]` | — | — |
| M-24 | **Manual DTO Mapping**: ডেকোরেটিভ DTO ম্যাপিং লাইব্রেরি (যেমন MapStruct) ইন্টিগ্রেটে করা | CODE | `[ ]` | — | — |
| M-25 | **401 errors on `/api/ext/*` endpoints**: ব্রাউজার এক্সটেনশন এন্ডপয়েন্টে 401 অথরাইজেশন এরর হ্যান্ডলিং ঠিক করা | BUG | `[ ]` | — | — |
| M-26 | **Fix `content.js` undefined error**: ব্রাউজার এক্সটেনশনের `content.js` আনডিফাইন্ড এরর ফিক্স করা | BUG | `[ ]` | — | — |
| M-27 | **Implement proper error logging (Sentry)**: ড্যাশবোর্ড ও ব্যাকএন্ডের জন্য সেন্ট্রি বা অন্য সেন্ট্রালাইজড এরর লগিং সেটআপ করা | QUALITY | `[ ]` | — | — |
| M-28 | **Add end-to-end authentication tests**: সাইন-ইন, সেশন রিনিউ এবং সাইন-আউটের জন্য কমপ্লিট ই-টু-ই টেস্ট স্ক্রিপ্ট তৈরি | TEST | `[ ]` | — | — |

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
| L-15 | **CLI OAuth flow**: `supremeai login` চালুর মাধ্যমে স্বয়ংক্রিয় ক্রেডেনশিয়াল লোড | TODO_LIST | `[ ]` | — | — |
| L-16 | **Desktop Wrapper**: ডাবল-ক্লিক সুবিধার জন্য Electron.js বা Tauri দিয়ে মোড়ানো | TODO_LIST | `[ ]` | — | — |
| L-17 | **Billing Alerts**: GCP-তে $10, $50, $100 থ্রেশহোল্ড সেট করার জন্য বিলিং অ্যালার্ট কনফিগার করা | DEPLOY/COST | `[ ]` | — | — |
| L-18 | **Resource Rightsizing**: Cloud Run সার্ভিসগুলোর জন্য CPU/Memory ব্যবহার মনিটর করে তা সঠিকভাবে অপ্টিমাইজ করা | DEPLOY/COST | `[ ]` | — | — |

| SD-01 | `scripts/seed/seed-data.json` — 13 collections real data seeder | NEW | `[x]` | — | 2026-05-16 |
| SD-02 | `scripts/seed/seed-all-data.js` — all-collection seeder + dry run | NEW | `[x]` | — | 2026-05-16 |
| SD-03 | `scripts/seed/seed-all-providers.js` — live GCP endpoint health-check + seed | NEW | `[x]` | — | 2026-05-16 |
| SD-04 | `SeedDataValidator.java` — startup seed-data health check | NEW | `[x]` | — | 2026-05-16 |

---

## 📊 অগ্রগতি ট্র্যাকার

```
P1 Critical:  [13/24] ███████░░░░░ 54%
P2 High:      [21/48] █████░░░░░░░ 44%
P3 Medium:    [14/30] ██████░░░░░░ 46%
P4 Low:       [ 7/22] ████░░░░░░░░ 31%
Seed Data:    [ 4/ 4] ███████████ 100%
────────────────────────────────────────
মোট:          [59/128] ███████░░░░░ 46%

```

---

## 📝 নতুন সমস্যা লগ

| তারিখ | সমস্যা | সোর্স | Priority | টিকেট # |
|-------|--------|-------|----------|---------|
| 2026-05-15 | `RateLimitingFilter` duplicate in `security/` and `filter/` | Conflict Analysis | P1 | C-04 |
| 2026-05-15 | `service-account.json` root-এ credential exposed | Security Audit | P1 | C-01 |
| 2026-05-15 | 3x WebSocket @EnableWebSocket config conflict | Conflict Analysis | P1 | C-06 |
| 2026-05-15 | `ChatWithAI.tsx`: `useRole` is not defined runtime error | Bug | P1 | B-01 |
| 2026-05-16 | `UserChatControllerTest` / `SelfLearningRouterTest` → removed | CI Fix | L-16 | ✅ fixed |
| 2026-05-16 | `AdminUsers.tsx` uses outdated `/api/accounts` endpoints | Sync Audit | P2 | H-20 |
| 2026-05-16 | `AdminProviders.tsx` missing capability patching & cleanup | Feature Gap | P2 | H-21 |
| 2026-05-16 | `AdminQuotas.tsx` and others are placeholders | Feature Gap | P1 | AD-01 |
| 2026-05-16 | `AdminProjects.tsx` uses simulated timeouts for progress | UX Audit | P2 | EP-01 |
| 2026-05-19 | `firebase-service-account.json` contains real private key | Security Audit | P1 | C-11 | ✅ fixed |

---
**শেষ আপডেট:** 2026-05-19  
**মালিক:** সকল Agent + Developer
