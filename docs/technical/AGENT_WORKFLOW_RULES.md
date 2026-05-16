# Repository Guidelines & Agent Workflow Rules

## Project Structure & Module Organization

This is a monorepo featuring a multi-agent system for automated app generation.

- **Backend**: Spring Boot 3 application located in `src/main/java/com/supremeai/`.
- **Dashboards**:
  - React/TypeScript 3D dashboard in `dashboard/`.
  - Admin dashboard at `public/` (static files).
- **Mobile**: Flutter-based admin application in `supremeai/`.
- **IDE Extensions**:
  - VS Code extension in `supremeai-vscode-extension/`.
  - IntelliJ plugin in `supremeai-intellij-plugin/`.
- **CLI**: Core command-hub in `command-hub/`.
- **Serverless**: Firebase Cloud Functions in `functions/`.

## Build, Test, and Development Commands

- **Environment**: Java 21 (Required for Virtual Threads).
- **Backend (Gradle)**:
  - Run: `./gradlew bootRun`
  - Build (skip tests): `./gradlew clean build -x test`
  - Test: `./gradlew test`
  - Coverage: `./gradlew jacocoTestReport`
- **Dashboard (Vite/React)**:
  - Dev: `npm run dev` (inside `dashboard/`)
  - Build: `npm run build`
  - Type-check: `npm run type-check`
- **VS Code Extension**:
  - Compile: `npm run compile` (inside `supremeai-vscode-extension/`)
  - Lint: `npm run lint`

## Coding Style & Naming Conventions

- **Spring Boot 3-Layer Flow**: Controller (validation) -> Service (business logic) -> Repository/Data (Firestore/Storage).
- **Security**: Always keep permission and security checks in the service layer, not just controllers.
- **Feature Parity**: Ensure any user-facing capability in the React dashboard is mirrored in the Flutter admin app.
- **Cloud-First**: Prioritize cloud backend and providers over local fallbacks.
- **Solo-Capable**: Features must remain functional with meaningful output even if external AI providers are unavailable.
- **No Hardcoded Secrets**: Read API keys and tokens from environment variables or dynamic config APIs.

## Testing Guidelines

- **Framework**: Use JUnit 5 and Mockito for backend tests.
- **Coverage**: Minimum 10% line coverage is enforced via JaCoCo.
- **Execution**: Tests run in parallel by default. Use `./gradlew test` to verify changes.

## Commit & Pull Request Guidelines

- **Commit Prefixes**: Use `feat:`, `fix:`, `docs:`, or `Cleanup:` prefixes for commit messages.
- **PRs**: Follow the template in `.github/pull_request_template.md`. Ensure tests pass before submitting.

## বাংলা ভাষা সমর্থ্যা (Bengali Localization Support)

### আন্তর্জালিকরণ (Internationalization)

- **ফাইলগুলো**: `dashboard/src/i18n/bn.json`, `supremeai/assets/i18n/bn.json`
- **VS Code এক্সটেনশন**: `supremeai-vscode-extension/package.nls.bn.json`
- **ভাষা কীগুলো**: `en` (ইংরেজি), `bn` (বাংলা)

### বাংলা যোগ করার নিয়মাবলী

1. **ইউজার ইন্টারফেস**: যেকোনো ইউজার-ফেসিং টেক্স্টের জন্য `bn.json` এ বাংলা যোগ করুন
2. **এরর মেসেজ**: সব এরর মেসেজের বাংলা অনুবাদ যোগ করুন
3. **ডকুমেন্টেশন**: নয়, শুধুমানে ইংরেজিতেই রাখুন
4. **কমмент**: বাংলায় কমмент করা যায় (বিশেশ করে Java/Spring Boot ফাইলে)
5. **যোগাযোগ**: ব্যবহারকারীর সাথে সবসময় বাংলা ভাষায় কথা বলতে হবে।

### ভাষা সনাক্তকরণ

- ব্যবহারকারীর ব্রাউজার/সিস্টেম ভাষা অনুযায়ী স্বয়ংক্রিয়ভাবে বাংলা দেখানো হবে
- `UserLanguagePreferenceService` ভাষা পছন্দ সংরক্ষণ করে
- API রেসপন্সেও ভাষা অনুযায়ী বাংলা উপযোগী হবে

## AI Model Registry & Landscape (Comprehensive All-in-One)

SupremeAI utilizes a dynamic registry of primary GCP models and a secondary "All-in-One" suite of free-tier models (HuggingFace/Render) to ensure 100% uptime and cost-efficiency.

### 1. Primary Orchestrators (GCP/OpenAI)
- **Gemini 1.5 Flash**: Primary Orchestrator & Multimodal Specialist (1M Context).
- **GPT-4o-mini**: Structured Data & Logic Verification backup.
- **DeepSeek-V4Pro**: Professional Coding & Technical Architect.

### 2. Fallback & Development Suite (HuggingFace Serverless)
- **CodeLlama-34b**: Primary fallback for code generation.
- **Mistral-7B-v0.3**: Major chat and conversational instruction tuning.
- **Llama-3-8B**: Specialized for Bengali language and conversational nuance.
- **Phi-3-Vision**: Specialized endpoint for lightweight image analysis.
- **Multilingual-E5**: High-performance embeddings for RAG tasks.

### 3. Emergency & Fast Response (Render Free Tier)
- **Phi-2 / Phi-3**: Fast response models running on Docker (Render).
- **TinyLlama-1.1B**: Always-on emergency fallback for basic connectivity tests.

## Zero-Hardcode Orchestration Policy

- **Dynamic Discovery**: All AI models are fetched from the `AIProviderDiscoveryService`.
- **Admin Control**: The dashboard must allow administrators to:
  - Enable/Disable providers.
  - Assign roles (Communication, Execution, Voting) dynamically.
  - Update API endpoints and keys without code changes.
- **Fallback Logic**: If a high-priority model fails, the system automatically rotates to the next available provider based on the dynamic config.

## Development & Execution Principles

- **Task Completeness**: Every time a new feature is added or a bug is solved, all related and dependent tasks (e.g., UI updates, repository methods, API documentation) must be completed to ensure the feature is fully functional.
- **Zero-Redundancy Policy**: Always identify and remove old, deprecated, or duplicate content, whether it's in the code or documentation. Maintain a lean and clean codebase.
- **Robust Documentation**: Maintain comprehensive documentation in the `docs/` directory using appropriate sub-folders (`architecture/`, `deployment/`, `reports/`, `guides/`, `summaries/`, `status/`, `technical/`). Always update the documentation with new changes.
- **Workflow-First Design**: For every new feature, a detailed workflow must be added to the `docs/technical/` or relevant sub-folder. This workflow should describe how the feature functions and identify other parts of the system that need optimization or modification to make the integration robust.
- **Analysis-Driven Development**: Before code implementation, perform an impact analysis stored in `docs/reports/analysis/`.
- **Agent Governance**: All agent-based interactions must adhere to the `docs/technical/AGENT_WORKFLOW_RULES.md` protocol to prevent loop-drift and ensure accountability.

---

## 🤖 Agent Mandatory Workflow (সকল Agent অবশ্যই অনুসরণ করবে)

### ⚠️ কাজ শুরু করার আগে — MANDATORY FIRST STEPS

যেকোনো কাজ শুরু করার **আগে** এই ৪টি ফাইল পড়তে হবে:

```
1. docs/status/MASTER_TODO.md                       ← সব pending কাজের তালিকা
2. docs/reports/CONFLICT_AND_DUPLICATE_ANALYSIS.md  ← known conflicts & duplicates
3. docs/DATABASE_LINKAGE_MAP.md                     ← DB schema & missing connections
4. docs/CODEBASE_ORGANIZATION_GUIDE.md              ← কোথায় কী রাখতে হবে
```

### 📋 Agent কাজের ধাপ (Step-by-Step)

**ধাপ ১ — Read Before Act:**
- `MASTER_TODO.md` পড়ুন → কাজটি ইতিমধ্যে listed আছে কিনা দেখুন
- `CONFLICT_AND_DUPLICATE_ANALYSIS.md` পড়ুন → সংশ্লিষ্ট conflict আছে কিনা দেখুন
- DB-সংক্রান্ত হলে → `DATABASE_LINKAGE_MAP.md` পড়ুন
- নতুন file/package তৈরি করতে হলে → `CODEBASE_ORGANIZATION_GUIDE.md` পড়ুন

**ধাপ ২ — Conflict Check (কাজের আগে):**
```bash
# Duplicate class name check:
find src/main/java/com/supremeai -name "ClassName.java" | sort

# Duplicate @RequestMapping check:
grep -rn "@RequestMapping(\"/api/path\")" src/main/java/com/supremeai/controller/
```

**ধাপ ৩ — After Completion (MANDATORY):**
- কাজ শেষ হলে `MASTER_TODO.md` এ `[ ]` → `[x]` করুন এবং তারিখ লিখুন
- নতুন সমস্যা পেলে `MASTER_TODO.md` এর "নতুন সমস্যা লগ" table-এ যোগ করুন
- DB পরিবর্তন হলে → `DATABASE_LINKAGE_MAP.md` আপডেট করুন
- Conflict/duplicate পেলে → `CONFLICT_AND_DUPLICATE_ANALYSIS.md` এ যোগ করুন

### 🚫 Agent নিষেধাজ্ঞা

```
❌ একই নামে নতুন class তৈরি করবেন না (duplicate check না করে)
❌ Root directory তে নতুন .py/.js/.sh script রাখবেন না → scripts/ এ রাখুন
❌ service-account.json বা .env কখনো commit করবেন না
❌ নতুন @EnableWebSocket class তৈরি করবেন না (WebSocketConfig.java তে merge করুন)
❌ docs/ এর বাইরে documentation ফাইল রাখবেন না
❌ কাজ শেষে MASTER_TODO.md আপডেট না করে task complete বলবেন না
❌ @ConditionalOnProperty ছাড়া DataSource bean তৈরি করবেন না
```

### ✅ নতুন Feature Pattern

```
1. MASTER_TODO.md চেক → CODEBASE_ORGANIZATION_GUIDE.md দেখে package নির্ধারণ
2. DATABASE_LINKAGE_MAP.md → model/collection লাগবে কিনা দেখুন
3. Feature-based package এ কোড লিখুন
4. @Document annotation + Repository যোগ করুন (DB লাগলে)
5. JUnit 5 + Mockito দিয়ে test লিখুন
6. MASTER_TODO.md → [x] mark করুন
7. DATABASE_LINKAGE_MAP.md → নতুন entry যোগ করুন
```
