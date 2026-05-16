# 🏗️ SupremeAI — Codebase & Documentation Organization Guide
**তৈরি:** 2026-05-15 | **লক্ষ্য:** Clean, Maintainable, Scalable Architecture

---

## 📊 বর্তমান অবস্থা (Current State)

| বিভাগ | ফাইল সংখ্যা | অবস্থা |
|-------|------------|--------|
| Java Backend Packages | 42 packages, ~585 files | ⚠️ অসংগঠিত |
| Controller | 88 files | ⚠️ অনেক বড়, বিভক্ত করা দরকার |
| Service | 132 files | ❌ সবচেয়ে বড় — বিভক্ত করা জরুরি |
| Model | 62 files | ✅ মোটামুটি ঠিক |
| Repository | 49 files | ✅ মোটামুটি ঠিক |
| Config | 34 files | ⚠️ ডুপ্লিকেট আছে |
| Dashboard (React) | 191 tsx/ts files | ⚠️ আংশিক সংগঠিত |
| Docs | ~100 files | ❌ অত্যন্ত অসংগঠিত |
| Root-level Scripts | 33 scripts | ❌ সব root-এ, সরাতে হবে |

---

## 🎯 PART 1 — Java Backend Package Structure

### ✅ প্রস্তাবিত Package Layout (Feature-First)

```
com.supremeai/
│
├── 📦 core/                          [নতুন — shared utilities]
│   ├── config/                       [সব @Configuration classes]
│   ├── exception/                    [Global exception handlers]
│   ├── filter/                       [HTTP Filters]
│   ├── interceptor/                  [Request Interceptors]
│   ├── response/                     [ApiResponse wrappers]
│   └── util/                         [Utility classes]
│
├── 📦 auth/                          [নতুন — auth সব এক জায়গায়]
│   ├── controller/
│   ├── service/
│   ├── filter/                       [JwtAuthFilter]
│   └── model/
│
├── 📦 ai/                            [AI Provider সব এক জায়গায়]
│   ├── provider/                     [AIProvider interface + implementations]
│   ├── fallback/                     [AIFallbackOrchestrator]
│   ├── factory/                      [AIProviderFactory]
│   ├── model/                        [APIProvider, ProviderVote etc]
│   ├── repository/
│   └── service/                      [AIProviderService, Discovery etc]
│
├── 📦 chat/                          [Chat সব এক জায়গায়]
│   ├── controller/
│   ├── service/
│   ├── model/                        [ChatMessage, ChatSession etc]
│   └── repository/
│
├── 📦 learning/                      [Learning & Knowledge]
│   ├── controller/
│   ├── service/
│   ├── model/
│   ├── repository/
│   └── router/                       [SelfLearningRouter — একটিই]
│
├── 📦 knowledge/                     [Knowledge Base]
│   ├── controller/
│   ├── service/
│   ├── model/
│   └── repository/
│
├── 📦 orchestration/                 [Agent Orchestration]
│   ├── controller/
│   ├── service/
│   └── model/
│
├── 📦 healing/                       [Self-Healing]
│   ├── controller/
│   ├── service/
│   └── model/                        [HealingEvent — @Document যোগ করুন]
│
├── 📦 simulator/                     [Simulator & Reverse Engineering]
│   ├── controller/
│   ├── service/
│   ├── model/
│   └── repository/
│
├── 📦 browser/                       [Browser Automation]
│   ├── controller/
│   ├── service/
│   ├── model/
│   └── repository/
│
├── 📦 admin/                         [Admin-only features]
│   ├── controller/
│   ├── service/
│   └── model/
│
├── 📦 security/                      [Security & Rate Limiting]
│   ├── config/                       [SecurityConfig]
│   ├── filter/                       [RateLimitingFilter — একটিই]
│   ├── ratelimit/
│   └── service/
│
└── 📦 websocket/                     [WebSocket — একটি config]
    ├── config/                       [WebSocketConfig একটিই]
    └── handler/
```

---

## 📋 PART 2 — বর্তমান Package → প্রস্তাবিত Package Mapping

### 🔴 সরাতে হবে (Delete/Merge)

| বর্তমান | করণীয় | কারণ |
|---------|--------|------|
| `ai/provider/AIProvider.java` | ❌ মুছুন | `provider/AIProvider.java` আছে |
| `ai/provider/OpenAIProvider.java` | ❌ মুছুন | `provider/OpenAIProvider.java` আছে |
| `security/RateLimitingFilter.java` | ❌ মুছুন | `filter/RateLimitingFilter.java` আছে |
| `config/RateLimiterConfiguration.java` | ❌ মুছুন | Empty class |
| `controller/UserChatController.java` | ❌ মুছুন | Legacy `/api/chat-legacy` |
| `scratch/` package | ❌ মুছুন | Production code-এ থাকা উচিত নয় |

### 🟡 একত্রিত করতে হবে (Merge)

| বর্তমান ১ | বর্তমান ২ | → যাবে |
|-----------|-----------|--------|
| `agentorchestration/` | `agent/` | → `orchestration/` |
| `healing/` | `selfhealing/` | → `healing/` |
| `websocket/AdminWebSocketConfig` + `SimulatorWebSocketConfig` | `config/WebSocketConfig` | → `websocket/config/WebSocketConfig` (একটিই) |
| `learning/SelfLearningRouter` | `learning/EnhancedSelfLearningRouter` | → একটি রাখুন |
| `controller/AdminRuleController` | `controller/SystemAdminRuleController` | → একটি URL |

### 🟢 ঠিক আছে — শুধু সঠিক Package-এ রাখুন

| বর্তমান | প্রস্তাবিত |
|---------|-----------|
| `model/` | Feature package-এর ভেতরে |
| `repository/` | Feature package-এর ভেতরে |
| `dto/` | `core/dto/` অথবা feature-এর ভেতরে |
| `exception/` | `core/exception/` |
| `util/` | `core/util/` |

---

## 🗂️ PART 3 — Root Directory Organization

### ❌ বর্তমান সমস্যা
Root directory-তে ৩৩+ script, কোনো organization নেই।

### ✅ প্রস্তাবিত Root Layout

```
supremeai/                           [Root]
│
├── 📄 README.md                     ✅ আছে
├── 📄 AGENTS.md                     ✅ আছে
├── 📄 LICENSE                       ✅ আছে
├── 📄 .gitignore                    ✅ আছে
├── 📄 .env.example                  ✅ আছে
│
├── 🔧 build.gradle.kts              ✅ (Backend build)
├── 🔧 settings.gradle.kts          ✅
├── 🔧 gradlew / gradlew.bat        ✅
│
├── 🐳 Dockerfile                    ✅
├── 🐳 docker-compose.yml           ✅
├── ☁️ cloudbuild.yaml              ✅
├── ☁️ firebase.json                ✅
├── ☁️ firestore.rules              ✅
│
├── 📁 scripts/                      [নতুন — সব script এখানে]
│   ├── deploy/
│   │   ├── deploy.sh               [deploy_gcp_firebase.sh থেকে move]
│   │   └── deploy.bat
│   ├── setup/
│   │   ├── setup-admin-user.js
│   │   └── set-admin-claims.js
│   ├── test/
│   │   ├── test-auth.js
│   │   ├── validate_all.py
│   │   └── load-test.js
│   └── seed/
│       └── seed-firebase-knowledge.js
│
├── 📁 src/                          ✅ Backend Java
├── 📁 dashboard/                    ✅ React Frontend
├── 📁 supremeai/                    ✅ Flutter Admin App
├── 📁 supremeai-vscode-extension/   ✅ VS Code Extension
├── 📁 supremeai-intellij-plugin/    ✅ IntelliJ Plugin
├── 📁 command-hub/                  ✅ CLI
├── 📁 functions/                    ✅ Firebase Functions
├── 📁 docs/                         📝 Documentation
├── 📁 infrastructure/               ✅ GCP setup scripts
├── 📁 monitoring/                   ✅ Prometheus/Grafana
│
└── 🗑️ সরাতে হবে (Root থেকে):
    ├── *.py files → scripts/seed/ বা scripts/test/
    ├── *.js files → scripts/ এর ভেতরে
    ├── *.sh / *.bat → scripts/ এর ভেতরে
    ├── PIPELINE_CHECK_SUMMARY.md → docs/status/
    ├── project_todo_list.md → docs/status/
    ├── TestCloudRun.java → src/test/ এর ভেতরে
    ├── service-account.json → ❌ .gitignore করুন (security risk!)
    ├── app.jar → ❌ মুছুন (build artifact)
    └── temp_build/ → ❌ মুছুন
```

---

## 📚 PART 4 — Documentation Structure

### ❌ বর্তমান সমস্যা

| সমস্যা | বিস্তারিত |
|--------|----------|
| ডুপ্লিকেট ফোল্ডার | `problem_and_solution/` ও `problems_and_solutions/` দুটোই আছে |
| Root-এ stray ফাইল | `DASHBOARD_COMMAND_CENTER_PLAN.md`, `DATABASE_LINKAGE_MAP.md`, `plugin_failure_analysis_bn.md` docs root-এ |
| `final_document/` অগোছালো | ৫০+ ফাইল main plan/ ফোল্ডারে |
| `summaries/` আর `reports/` overlap | একই ধরনের content |

### ✅ প্রস্তাবিত Docs Layout

```
docs/
│
├── 📄 README.md                         ✅ Index of all docs
│
├── 📁 architecture/                     ✅ আছে
│   ├── ARCHITECTURE.md                  ✅
│   ├── ANALYSIS_SYSTEM_DESIGN.md        ✅
│   └── DATABASE_LINKAGE_MAP.md          ⬅️ docs root থেকে move করুন
│
├── 📁 guides/                           ✅ আছে
│   ├── CONTRIBUTING.md                  ✅
│   ├── CODE_OF_CONDUCT.md              ✅
│   ├── HANDOVER_GUIDE.md               ✅
│   └── CODEBASE_ORGANIZATION_GUIDE.md  ✅ (এই ফাইল)
│
├── 📁 deployment/                       ✅ আছে
│   ├── DEPLOYMENT_GUIDE.md             ✅
│   └── DEPLOY_GCP_FIREBASE.md          ✅
│
├── 📁 reports/                          ✅ আছে
│   ├── ANALYTICS_REPORT_2026_05_15.md  ✅
│   ├── CODE_QUALITY_REPORT.md          ✅
│   ├── CONFLICT_AND_DUPLICATE_ANALYSIS.md ✅
│   └── COMPLETION_REPORT.md            ✅
│
├── 📁 status/                           ✅ আছে
│   ├── IMPLEMENTATION_STATUS.md        ✅
│   ├── TODO_LIST.md                    ✅
│   └── PIPELINE_CHECK_SUMMARY.md       ✅
│
├── 📁 technical/                        ✅ আছে
│   ├── CHAT_AUTH_WORKFLOW.md           ✅
│   ├── hybrid_storage_strategy.md      ✅
│   └── CODEFLOW_MODULE_README.md       ✅
│
├── 📁 troubleshooting/                  ✅ আছে (একটিই রাখুন)
│   ├── ERRORS_AND_SOLUTIONS.md         ✅
│   └── authentication/REMEDY.md        ✅
│
├── 📁 plans/                            [নতুন — সব Plan এক জায়গায়]
│   ├── yearly/
│   │   └── 2026_yearly_plan.md
│   ├── sprints/
│   │   └── sprint_planning_template.md
│   ├── features/                        [final_document/main plan/ থেকে]
│   │   ├── Plan_01_Dynamic_AI_Agent_System.md
│   │   ├── Plan_02_API_Key_Rotation_System.md
│   │   └── ... (Plan_03 to Plan_24)
│   └── phases/                          [final_document/phases/ থেকে]
│       ├── phase1_foundation.md
│       └── ...
│
└── 🗑️ সরাতে হবে / মার্জ করতে হবে:
    ├── final_document/ → plans/ এ reorganize
    ├── summaries/ → reports/ এ merge
    ├── problem_and_solution/ + problems_and_solutions/ → troubleshooting/ একটিতে
    └── DASHBOARD_COMMAND_CENTER_PLAN.md → plans/features/ তে move
```

---

## ⚛️ PART 5 — React Dashboard Structure

### বর্তমান অবস্থা (মোটামুটি ঠিক)

```
dashboard/src/
├── components/           ✅ Feature-based organization
├── pages/               ✅
├── services/            ✅ API calls
├── hooks/               ✅
├── contexts/            ✅
├── types/               ✅
├── utils/               ✅
├── constants/           ✅
├── i18n/                ✅ bn.json + en.json
└── lib/                 ⚠️ firebase.ts — config hardcode যাচাই করুন
```

### 🔧 Dashboard-এ করণীয়

| সমস্যা | সমাধান |
|--------|--------|
| `lib/firebase.ts` hardcoded config | Environment variable ব্যবহার করুন |
| `RepoToPromptEngine.tsx` localhost ref | `VITE_API_BASE_URL` env var ব্যবহার করুন |
| `dataconnect-generated/` auto-generated | `.gitignore` করুন |
| Test files in `src/test/` | `src/__tests__/` নামে organize করুন |

---

## 🗃️ PART 6 — বিভিন্ন Module-এর বর্তমান স্থান

| Module | বর্তমান Location | প্রস্তাবিত Location | অবস্থা |
|--------|-----------------|---------------------|--------|
| Flutter Admin App | `supremeai/` | `supremeai/` | ✅ ঠিক আছে |
| VS Code Extension | `supremeai-vscode-extension/` | `supremeai-vscode-extension/` | ✅ ঠিক আছে |
| IntelliJ Plugin | `supremeai-intellij-plugin/` | `supremeai-intellij-plugin/` | ✅ ঠিক আছে |
| Python Microservices | `reverse-engineering/`, `simulator-runtime/`, `reverse_engineer/`, `reverse-engineer-service/` | `microservices/reverse-engineering/`, `microservices/simulator/` | ❌ ৩টি ডুপ্লিকেট ফোল্ডার |
| Smart Chat System | `smart_chat_system/` root | `legacy/smart_chat_system/` | ⚠️ legacy? |
| Firebase Functions | `functions/` | `functions/` | ✅ ঠিক আছে |
| Load Tests | `load-tests/` + `load-test.js` root | `tests/load/` | ⚠️ বিভক্ত |

---

## ⚠️ PART 7 — Security Issues (জরুরি)

| ফাইল | সমস্যা | করণীয় |
|------|--------|--------|
| `service-account.json` | Root-এ আছে — Git-এ commit হলে credential leak | `.gitignore` করুন, env var ব্যবহার করুন |
| `.env` | Root-এ — secrets থাকতে পারে | `.gitignore` যাচাই করুন |
| `auth-token.txt.example` | Example file fine কিন্তু actual file? | `git status` চেক করুন |
| `rotation_config.json` | API key rotation config root-এ | `config/` ফোল্ডারে নিন |

---

## 📋 PART 8 — সম্পূর্ণ করণীয় তালিকা (Priority অনুযায়ী)

### 🔴 এখনই করুন

```
[ ] 1. service-account.json → .gitignore এ যোগ করুন (security!)
[ ] 2. ai/provider/ package → মুছুন (AIProvider, OpenAIProvider duplicate)
[ ] 3. security/RateLimitingFilter.java → মুছুন (filter/ version রাখুন)
[ ] 4. config/RateLimiterConfiguration.java → মুছুন (empty class)
[ ] 5. WebSocket configs → WebSocketConfig.java তে merge করুন
[ ] 6. scratch/ package → src থেকে সরান
[ ] 7. app.jar → .gitignore, মুছুন (build artifact)
[ ] 8. temp_build/ → মুছুন
```

### 🟠 এই Sprint-এ করুন

```
[ ] 9. Root scripts (33টি) → scripts/ ফোল্ডারে organize করুন
[ ] 10. reverse_engineer/ + reverse-engineer-service/ + reverse-engineering/ → একটি করুন
[ ] 11. problem_and_solution/ + problems_and_solutions/ → troubleshooting/ তে merge
[ ] 12. final_document/ → plans/ তে reorganize
[ ] 13. UserChatController (legacy) → মুছুন
[ ] 14. summaries/ → reports/ তে merge
[ ] 15. PIPELINE_CHECK_SUMMARY.md (root) → docs/status/ তে move
[ ] 16. project_todo_list.md (root) → docs/status/TODO_LIST.md তে merge
```

### 🟡 পরবর্তী Sprint-এ করুন

```
[ ] 17. Service package (132 files) → Feature-based sub-packages তে ভাগ করুন
[ ] 18. Controller package (88 files) → Feature-based sub-packages তে ভাগ করুন
[ ] 19. Model/Repository → Feature package-এর ভেতরে নিন
[ ] 20. HealingEvent, UserTier → @Document annotation যোগ করুন
[ ] 21. ProviderTaskPerformance → Repository তৈরি করুন
[ ] 22. Dashboard hardcoded URLs → env variable তে নিন
[ ] 23. TestCloudRun.java (root) → src/test/ এ নিন
[ ] 24. DataConnect generated code → .gitignore করুন
```

---

## 📏 PART 9 — Naming Conventions (মান অনুসরণ করুন)

### Java

| Type | Convention | উদাহরণ |
|------|-----------|--------|
| Class | PascalCase | `ChatController` |
| Method | camelCase | `getUserById()` |
| Package | lowercase | `com.supremeai.chat.service` |
| Constant | UPPER_SNAKE | `MAX_RETRY_COUNT` |
| Interface | PascalCase (no I prefix) | `AIProvider` ✅, `IAIProvider` ❌ |

### React/TypeScript

| Type | Convention | উদাহরণ |
|------|-----------|--------|
| Component | PascalCase | `ChatComponent.tsx` |
| Hook | camelCase + use | `useAuth.ts` |
| Service | camelCase + Service | `authService.ts` |
| Type/Interface | PascalCase | `ChatMessage` |
| Constant | UPPER_SNAKE | `API_BASE_URL` |

### Files & Docs

| Type | Convention |
|------|-----------|
| Docs | `UPPER_SNAKE_CASE.md` |
| Scripts | `kebab-case.sh` |
| Config | `camelCase.json` বা `kebab-case.yml` |

---

## 🔄 PART 10 — Package Refactoring Migration Plan

### ধাপ ১ (২ দিন): Cleanup
1. Duplicate file মুছুন
2. Empty class মুছুন
3. Security issue fix করুন

### ধাপ ২ (৩ দিন): Root Organization
1. Scripts সরান
2. Legacy ফোল্ডার organize করুন
3. Docs merge করুন

### ধাপ ৩ (১ সপ্তাহ): Package Restructure
1. Feature-based packages তৈরি করুন
2. Service → Feature packages তে ভাগ করুন
3. Controller → Feature packages তে ভাগ করুন

### ধাপ ৪ (চলমান): Documentation
1. প্রতিটি feature-এর জন্য README.md
2. API documentation update
3. Architecture diagram update

---

*এই গাইড অনুসরণ করলে codebase maintenance সহজ হবে এবং নতুন developer দ্রুত onboard হতে পারবে।*

**শেষ আপডেট:** 2026-05-15 | **পরবর্তী রিভিউ:** পরবর্তী Sprint শেষে
