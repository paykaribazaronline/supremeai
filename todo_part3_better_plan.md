# ⚡ Part 3 — Better Plan Implementation

> **Goal:** Don't just fix what's broken — redesign the weak points for scalability, developer experience, and real user value. This is the "if we were doing this right" plan.  
> Assumes Part 1 and Part 2 are complete.

**Timeframe:** Week 5–8 (June 3 – July 1, 2026)  
**Prerequisites:** Part 1 ✅ + Part 2 ✅

---

## 🏛️ Architecture Improvements

### BP-1. Replace Raw Firestore Calls with Repository Pattern
**Why better:** Current code mixes Firestore SDK calls directly in services. A proper repository layer makes testing, mocking, and swapping databases easier.

- [ ] Create `BaseFirestoreRepository<T>` generic abstract class with:
  - `findById(String id): Mono<T>`
  - `save(T entity): Mono<T>`
  - `delete(String id): Mono<Void>`
  - `findAll(): Flux<T>`
  - `query(FirestoreQuery): Flux<T>`
- [ ] Migrate top-5 most-used repositories to extend `BaseFirestoreRepository`
- [ ] Replace all direct `firestore.collection("...").document("...").get()` calls in services with repository method calls
- [ ] Add mock repository implementations for all unit tests

### BP-2. Event-Driven Architecture for AI Operations
**Why better:** Long-running AI operations (app generation, code analysis) currently block HTTP threads. Events decouple the request from the work.

- [ ] Add `ApplicationEventPublisher` integration in `AppGenerationController`
- [ ] Create `AppGenerationRequestedEvent`, `AppGenerationCompletedEvent`
- [ ] Move generation logic to `@EventListener` in `AppGenerationService`
- [ ] Return `202 Accepted` immediately with a job ID
- [ ] Add `GET /api/generation/{jobId}/status` polling endpoint
- [ ] Update dashboard to poll for status instead of waiting on a long request

### BP-3. Circuit Breaker & Resilience for AI Providers
**Why better:** If one AI provider is down, the whole request chain fails. Resilience4j is already in the stack — use it properly.

- [ ] Annotate all AI provider calls with `@CircuitBreaker(name = "ai-provider", fallbackMethod = "fallbackResponse")`
- [ ] Add `@Retry(name = "ai-provider")` with 3 attempts and exponential backoff
- [ ] Add `@Bulkhead(name = "ai-provider")` to limit concurrent calls per provider
- [ ] Configure in `application.yml`: timeouts, thresholds, wait durations
- [ ] Add provider health status to `HealthController` response
- [ ] Test by shutting down a provider → confirm fallback kicks in

### BP-4. Caching Layer — Redis for Hot Data
**Why better:** Repeated Firestore reads for config, providers list, knowledge base cost money and add latency.

- [ ] Add Redis dependency to `build.gradle.kts`
- [ ] Configure `spring.data.redis` in `application.yml`
- [ ] Add `@Cacheable("providers")` to `ProvidersController.list()`
- [ ] Add `@Cacheable("knowledge")` to `KnowledgeBaseController.search()`
- [ ] Add `@CacheEvict` on all write operations
- [ ] Add Redis health check to `HealthController`
- [ ] Fallback: if Redis unavailable, bypass cache gracefully

---

## 🔐 Security Improvements

### BP-5. Proper JWT Implementation with Refresh Tokens
**Why better:** Current JWT fix was partial — full implementation needs refresh token rotation and blacklisting.

- [ ] Create `RefreshToken` Firestore collection (token hash, userId, expiry, isRevoked)
- [ ] Issue opaque refresh tokens (not JWTs) — store in Firestore
- [ ] Implement `POST /api/auth/refresh` that validates refresh token → issues new access JWT
- [ ] Implement refresh token rotation (new token issued on each refresh, old one revoked)
- [ ] Implement logout: revoke refresh token in Firestore
- [ ] Access JWT TTL: 15 minutes | Refresh token TTL: 30 days
- [ ] Add `HttpOnly; SameSite=Strict` cookie for refresh token storage

### BP-6. Role-Based Access Control (RBAC) in Spring Security
**Why better:** Current auth is binary (authenticated/not). We need admin, user, and viewer roles.

- [ ] Add `role` field to Firebase Auth custom claims (already partially done)
- [ ] Create `@PreAuthorize("hasRole('ADMIN')")` on all admin endpoints
- [ ] Create `@PreAuthorize("hasRole('USER')")` on user endpoints
- [ ] Add role management UI to `AdminUsers.tsx` dashboard page
- [ ] Test: non-admin token cannot access `/api/admin/**`

### BP-7. API Rate Limiting per User
**Why better:** Current rate limiting is global or absent. Per-user limits prevent abuse.

- [ ] Add `Bucket4j` or custom Redis-backed rate limiter
- [ ] Apply rate limit per `userId` extracted from JWT
- [ ] Limits: 100 req/min for regular users, 1000 req/min for admin
- [ ] Return `429 Too Many Requests` with `Retry-After` header
- [ ] Add rate limit status to dashboard API keys page

---

## 🎨 Frontend & UX Improvements

### BP-8. Real-Time Updates via WebSocket
**Why better:** Users currently have to manually refresh to see updates. WebSocket push eliminates polling.

- [ ] Extend `WebSocketController.java` — add topics for:
  - `/topic/generation/{jobId}` — real-time app generation progress
  - `/topic/provider-health` — live provider status updates
  - `/topic/admin-activity` — live admin activity feed
- [ ] Add `useWebSocket` custom hook in dashboard
- [ ] Subscribe to topics in `AdminDashboardUnified.tsx` — show live updates
- [ ] Add visual indicator (pulsing dot) when WebSocket is connected

### BP-9. CodeFlow Dashboard Page
**Why better:** CodeFlow is the most production-ready feature but has no dedicated UI in the React dashboard.

- [ ] Create `dashboard/src/pages/AdminCodeFlow.tsx`
- [ ] Features:
  - [ ] GitHub URL input → trigger CodeFlow analysis
  - [ ] Show animated progress during analysis
  - [ ] Display: file tree, security findings, function complexity graph
  - [ ] Show `ParseResult` details: classes, functions, imports
  - [ ] Export analysis as JSON or PDF
- [ ] Add `/admin/codeflow` route in `App.tsx`
- [ ] Add "CodeFlow" nav item in sidebar

### BP-10. Gitingest & GitReverse Dashboard Integration
**Why better:** These tools exist as standalone services but aren't visible in the main product UI.

- [ ] Create `dashboard/src/pages/AdminGitingest.tsx`
  - URL input → trigger Gitingest → display tree + content
  - Download digest as `.txt` file
- [ ] Create `dashboard/src/pages/AdminGitReverse.tsx`
  - URL input → trigger GitReverse LLM → display generated prompt
  - Copy prompt to clipboard button
- [ ] Add Spring Boot proxy endpoints:
  - `GET /api/tools/gitingest?url=...` → proxies to Gitingest FastAPI
  - `POST /api/tools/gitreverse` → proxies to GitReverse Next.js
- [ ] Add both to sidebar nav under "Tools"

---

## 🧪 Testing & Quality Improvements

### BP-11. Integration Test Suite
**Why better:** Only unit tests exist. Integration tests catch real bugs at the API level.

- [ ] Add `@SpringBootTest` + `@AutoConfigureMockMvc` test base class
- [ ] Write integration tests for critical flows:
  - [ ] `POST /api/auth/login` → valid credentials → JWT returned
  - [ ] `POST /api/auth/login` → invalid credentials → 401
  - [ ] `GET /api/admin/dashboard` → no token → 403
  - [ ] `POST /api/generation/app` → valid payload → 202 + jobId
  - [ ] `GET /api/codeflow/analyze?url=...` → result returned
- [ ] Use Testcontainers for Firebase emulator in tests
- [ ] Target: 30+ integration tests

### BP-12. End-to-End Tests with Playwright
**Why better:** No automated browser tests — regressions go undetected.

- [ ] Set up Playwright in `dashboard/` (or root):
  ```bash
  npm init playwright@latest
  ```
- [ ] Write E2E tests:
  - [ ] Login flow: fill form → submit → reach `/admin`
  - [ ] Dashboard load: `/admin` → all sections visible, no errors
  - [ ] CodeFlow: paste URL → analyze → results displayed
  - [ ] Gitingest: paste URL → ingest → tree shown
- [ ] Add Playwright to CI pipeline
- [ ] Target: 10+ E2E tests passing

### BP-13. Continuous Integration Pipeline
**Why better:** Tests only run manually. Automation prevents regressions.

- [ ] Create `.github/workflows/ci.yml`:
  ```yaml
  on: [push, pull_request]
  jobs:
    backend-test:
      runs-on: ubuntu-latest
      steps:
        - ./gradlew test jacocoTestReport
    frontend-build:
      - npm run type-check && npm run build
    gitingest-test:
      - pip install -e ".[all]" && pytest tests/
    gitreverse-build:
      - npm install && npm run build
  ```
- [ ] Add branch protection: PRs require CI pass
- [ ] Add coverage badge to `README.md`

---

## 📦 CodeFlow Module Enhancement

### BP-14. Expand Language Support (Python, Go, Dart, JS)
**Files:** `CodeAnalyzer.java`, `CodeAnalyzerTest.java`

- [ ] Add Python parser: detect `def`, `class`, `import` using regex + AST concepts
- [ ] Add Go parser: detect `func`, `type`, `import`, `struct`
- [ ] Add Dart parser: detect `class`, `void`, `Future<>`, `@override`
- [ ] JavaScript already partially done — complete arrow function + class detection
- [ ] Add test cases for each new language in `CodeAnalyzerTest`
- [ ] Update CodeFlow dashboard to show language badge

### BP-15. CodeFlow GitHub PR Integration
**Why better:** Auto-review PRs using CodeFlow — killer feature for developer adoption.

- [ ] Add GitHub Octokit dependency to `build.gradle.kts`
- [ ] Create `GitHubPRAnalysisService.java`:
  - Fetch PR diff from GitHub API
  - Run CodeFlow on changed files
  - Post review comments via GitHub API
- [ ] Create `POST /api/codeflow/analyze-pr` endpoint
- [ ] Add "Analyze PR" button to CodeFlow dashboard page
- [ ] Test with sample PR from a test repository

---

## 📊 Progress Tracking

| Task | Status | Target Date |
|------|--------|-------------|
| BP-1 Repository Pattern | ⬜ Not started | Jun 5 |
| BP-2 Event-Driven AI Ops | ⬜ Not started | Jun 7 |
| BP-3 Circuit Breaker | ⬜ Not started | Jun 9 |
| BP-4 Redis Cache | ⬜ Not started | Jun 11 |
| BP-5 JWT Refresh Rotation | ⬜ Not started | Jun 13 |
| BP-6 RBAC | ⬜ Not started | Jun 15 |
| BP-7 Rate Limiting | ⬜ Not started | Jun 17 |
| BP-8 WebSocket Live Updates | ⬜ Not started | Jun 19 |
| BP-9 CodeFlow Dashboard | ⬜ Not started | Jun 21 |
| BP-10 Tool Integration | ⬜ Not started | Jun 23 |
| BP-11 Integration Tests | ⬜ Not started | Jun 25 |
| BP-12 Playwright E2E | ⬜ Not started | Jun 27 |
| BP-13 CI Pipeline | ⬜ Not started | Jun 29 |
| BP-14 Multi-Language | ⬜ Not started | Jul 1 |
| BP-15 GitHub PR Integration | ⬜ Not started | Jul 1 |

---

## 🎯 Exit Criteria for Part 3

- [ ] All AI operations are async (no blocking HTTP threads)
- [ ] Circuit breakers protect all AI provider calls
- [ ] Redis cache reduces Firestore reads by ≥ 50% (measure with metrics)
- [ ] JWT uses refresh token rotation with revocation
- [ ] Role-based access enforced on all endpoints
- [ ] CodeFlow supports 5 languages (Java, JS, Python, Go, Dart)
- [ ] Playwright E2E: 10+ tests green
- [ ] CI runs automatically on every PR
- [ ] JaCoCo coverage ≥ 25%
