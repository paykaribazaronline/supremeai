# 🏗️ Part 2 — Current Plan Implementation

> **Goal:** Execute the agreed technical roadmap — fix all known issues, stabilize the codebase, and bring all modules to a production-ready state.  
> This assumes Part 1 (Instant Release) is complete.

**Timeframe:** Week 2–4 (May 13 – June 2, 2026)  
**Prerequisites:** All IR-* tasks from Part 1 checked off

---

## 🔧 Backend Stabilization (Week 2)

### CP-1. Fix N+1 / Blocking Calls in Reactive Services
**Files:** `ChatProcessingService.java`, `UserAccountService.java`, `APIKeyController.java`, `EnhancedLearningService.java`, `SystemLearningService.java`  
**Why:** `.block()` in a reactive context blocks the event loop thread — causes performance degradation and scalability issues under load.

- [ ] Audit all services for `.block()` usage: `grep -rn "\.block()" src/main/java/`
- [ ] Replace `.block()` with `flatMap()` / `flatMapMany()` / `zipWith()` in `ChatProcessingService`
- [ ] Replace `.block()` in `UserAccountService` with reactive chain
- [ ] Replace `.block()` in `EnhancedLearningService` with `collectList()` for batch operations
- [ ] Replace sequential Firestore loops in `SystemLearningService` with `Flux.fromIterable().flatMap()`
- [ ] Run load test after: `k6 run load-test.js` — confirm no thread blocking errors

### CP-2. Standardize API Response Format
**Files:** All controllers in `src/main/java/com/supremeai/controller/`  
**Why:** 61 controllers return different shapes — frontend code is fragile and inconsistent.

- [ ] Create `src/main/java/com/supremeai/response/ApiResponse.java`:
  ```java
  public record ApiResponse<T>(boolean success, T data, String error, long timestamp) {}
  ```
- [ ] Create `ApiResponse.ok(T data)` and `ApiResponse.error(String msg)` factory methods
- [ ] Update all controllers to return `ResponseEntity<ApiResponse<T>>`
- [ ] Update dashboard service layer to expect the new wrapper format
- [ ] Add Swagger/OpenAPI docs: `@Operation` annotations on all endpoints

### CP-3. Input Validation — DTOs and `@Valid`
**Files:** All controllers, new `dto/` package  
**Why:** Raw `Map<String,Object>` request bodies have no validation — security and reliability risk.

- [ ] Audit all endpoints that accept `Map<String, Object>` as request body
- [ ] Create DTO classes for top-10 highest-traffic endpoints (auth, chat, app generation, providers)
- [ ] Add `@NotBlank`, `@Size`, `@Email`, `@Min`, `@Max` to DTO fields
- [ ] Add `@Valid` annotation to all `@RequestBody` parameters
- [ ] Write one integration test per new DTO verifying validation errors return 400

### CP-4. Test Coverage — JaCoCo 10%+ Enforcement
**Files:** `build.gradle.kts`, new test files  
**Why:** Minimum coverage rule exists but is not enforced in CI yet.

- [ ] Fix all 17 remaining CodeAnalyzerTest failures (from IR-1, verify done)
- [ ] Run `./gradlew jacocoTestReport` — check current line coverage %
- [ ] Add JaCoCo `violationRules` block to `build.gradle.kts` with `minimum = 0.10`
- [ ] Write unit tests for `Gitingest` Spring Boot integration points
- [ ] Write unit tests for `GitReverse` reverse proxy in Cloud Functions
- [ ] Target: 314+ tests, 0 failing, coverage ≥ 10%

---

## 🖥️ Dashboard Quality (Week 2–3)

### CP-5. TypeScript Strict Mode & Remove `any` Types
**Files:** `dashboard/src/lib/firebase.ts`, `dashboard/src/services/`, `dashboard/src/components/`

- [ ] Enable `"strict": true` in `dashboard/tsconfig.json`
- [ ] Fix all TypeScript errors that surface (run `npm run type-check`)
- [ ] Create `dashboard/src/types/index.ts` with:
  - `AuthUser` interface
  - `ApiResponse<T>` generic
  - `Provider`, `Project`, `User`, `ApiKey` interfaces
- [ ] Replace all `any` in `firebase.ts` with proper return types
- [ ] Replace all `any` in component props

### CP-6. Proper Loading & Error States in All Pages
**Files:** `dashboard/src/pages/*.tsx`

- [ ] Create reusable `<LoadingSpinner />` and `<ErrorCard message={} />` components
- [ ] Wrap all `useEffect` data fetches with try/catch → set error state
- [ ] Show `<LoadingSpinner />` while data is fetching (replace hardcoded data)
- [ ] Show `<ErrorCard />` on fetch failures
- [ ] Pages to update: `AdminDashboardUnified`, `AdminUsers`, `AdminProjects`, `AdminProviders`, `AdminMonitoring`, `AdminLogs`

### CP-7. Firebase Emulator Config for Local Dev
**Files:** `dashboard/src/lib/firebase.ts`

- [ ] Add `VITE_USE_EMULATOR=true` env variable support
- [ ] When enabled: connect Auth, Firestore, and Functions emulators
- [ ] Update `.env.example` with emulator config vars
- [ ] Document local dev setup in `CONTRIBUTING.md`

---

## 📱 Flutter & Extensions (Week 3–4)

### CP-8. Flutter Admin App — Feature Parity with React Dashboard
**Location:** `supremeai/lib/`

- [ ] Audit current Flutter screens vs React dashboard pages (17 pages)
- [ ] List missing screens and implement priority ones:
  - [ ] Users management screen
  - [ ] Providers screen  
  - [ ] Projects screen
  - [ ] Logs screen
- [ ] Add Firebase Auth sign-in flow in Flutter (using `firebase_auth` package)
- [ ] Implement shared API service layer in Flutter (matching backend endpoints)
- [ ] Test on Android: `flutter build apk` — no errors
- [ ] Test on web: `flutter build web` — runs in browser

### CP-9. VS Code Extension — Decision & Completion
**Location:** `supremeai-vscode-extension/`

- [ ] Decision: **Complete** or **Remove** (decide now)
- [ ] If **Complete**:
  - [ ] Run `npm run compile` — fix all TypeScript errors
  - [ ] Implement core commands: "Analyze File", "Chat with AI", "Generate Code"
  - [ ] Connect to Spring Boot backend via HTTP
  - [ ] Test in VS Code Extension Development Host
  - [ ] Package: `vsce package`
- [ ] If **Remove**:
  - [ ] Delete `supremeai-vscode-extension/` directory
  - [ ] Update `README.md` and `AGENTS.md` to remove extension references

### CP-10. Firebase Functions Hardening
**Location:** `functions/index.js`

- [ ] Add retry logic (3 attempts with exponential backoff) for all backend API calls
- [ ] Replace hardcoded URL with `process.env.BACKEND_URL || functions.config().backend.url`
- [ ] Add `minInstances: 1` to all critical functions (prevent cold starts)
- [ ] Add request field validation at function entry point
- [ ] Add 30-second timeout handling with `AbortController`
- [ ] Test each function individually with Firebase Emulator

---

## 🛠️ Infrastructure & Code Quality (Week 4)

### CP-11. Logging — Standardize & Add Correlation IDs
**Files:** All Java service files

- [ ] Audit: `grep -rn "System.out.print" src/main/java/` — list offenders
- [ ] Replace all `System.out.println` with `log.info()` / `log.error()`
- [ ] Add `MDC.put("requestId", UUID.randomUUID().toString())` in request filter
- [ ] Update log format in `application.yml` to include `%X{requestId}` 
- [ ] Enable JSON logging in production profile

### CP-12. Repo Cleanup — Remove Artifacts from Git
**Location:** Root repo

- [ ] Check: `git ls-files node_modules/ | wc -l` — if > 0, files are tracked
- [ ] If tracked: `git filter-repo --path node_modules/ --invert-paths` (backup first!)
- [ ] Also clean: `git filter-repo --path build/ --invert-paths`
- [ ] Force push: `git push origin main --force-with-lease`
- [ ] Verify: `git count-objects -vH` — packSize should be < 200MB

### CP-13. Hardcoded TODO/FIXME — Top 10
**Location:** Java source files

- [ ] Run: `grep -rn "TODO\|FIXME" src/main/java/ | sort` — get current list
- [ ] Pick top 10 most impactful (MCPClientManager, PluginManager, KnowledgeBaseController)
- [ ] Implement or remove each TODO
- [ ] Create GitHub Issues for remaining TODOs

---

## 📊 Progress Tracking

| Task | Status | Target Date |
|------|--------|-------------|
| CP-1 N+1 Fix | ⬜ Not started | May 15 |
| CP-2 API Response | ⬜ Not started | May 16 |
| CP-3 Input Validation | ⬜ Not started | May 17 |
| CP-4 Test Coverage | ⬜ Not started | May 18 |
| CP-5 TypeScript Strict | ⬜ Not started | May 20 |
| CP-6 Loading/Error States | ⬜ Not started | May 21 |
| CP-7 Firebase Emulator | ⬜ Not started | May 22 |
| CP-8 Flutter Parity | ⬜ Not started | May 25 |
| CP-9 VS Code Extension | ⬜ Not started | May 26 |
| CP-10 Functions Hardening | ⬜ Not started | May 28 |
| CP-11 Logging | ⬜ Not started | May 30 |
| CP-12 Repo Cleanup | ⬜ Not started | Jun 1 |
| CP-13 TODO Cleanup | ⬜ Not started | Jun 2 |

---

## 🎯 Exit Criteria for Part 2

- [ ] `./gradlew test` → **0 failing tests**
- [ ] `npm run build` in `dashboard/` → **no errors, no warnings**
- [ ] JaCoCo coverage report → **≥ 10% line coverage**
- [ ] Firebase auth works in both `public/` and `dashboard/`
- [ ] Flutter builds for Android/web without errors
- [ ] No `.block()` calls in any reactive service
- [ ] All 61 controllers return `ApiResponse<T>` format
