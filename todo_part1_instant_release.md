# 🚀 Part 1 — Instant Mart Release

> **Goal:** Get a working, shippable product out the door in **0–7 days**.  
> These are the minimum tasks needed to demo, ship, or hand off the project.  
> No architectural refactoring — just make it work end-to-end.

**Deadline:** 2026-05-13  
**Owner:** Solo / pair  

---

## 🎯 Definition of "Done" for Instant Release

A user can:
1. Open the login page → authenticate → reach the admin dashboard
2. See live data from the backend (not mocks)
3. Use at least one core AI feature (e.g. chat or CodeFlow analysis)
4. Gitingest CLI runs successfully on a sample repo
5. GitReverse generates a prompt from a GitHub URL
6. All tests pass (or failures are explicitly skipped/noted)

---

## ✅ Checklist — Day 1 (Fix the Build)

### IR-1. Fix 17 Failing Tests in CodeAnalyzerTest
**Files:** `CodeAnalyzer.java`, `CodeAnalyzerTest.java`  
**Why now:** CI must be green before release. These are the only 17 failing tests.

- [x] Run `./gradlew test --tests "com.supremeai.codeflow.analyzer.CodeAnalyzerTest" --info 2>&1 | grep -A5 "FAILED"` and capture each failure message
- [x] Fix `CodeAnalyzer.parse()`: ensure `ParseResult.language` is set from the `language` parameter
- [x] Fix class detection: correctly populate `ParseResult.classes` for records, enums, interfaces, anonymous classes
- [x] Fix function detection: extract modifiers (public/static/private), handle arrow functions for JavaScript
- [x] Re-run until: `314 tests completed, 0 failed`

---

## ✅ Checklist — Day 1–2 (Fix Auth — The Front Door)

### IR-2. Firebase SDK Mismatch — Unify to Modular SDK
**Files:** `public/index.html`, `dashboard/src/lib/firebase.ts`  
**Why now:** Users can't log in if auth is broken.

- [x] Rewrite `public/index.html` login form to use Firebase modular SDK (v10+), not Compat CDN
- [x] Replace 500ms polling `initFirebase()` with `await firebase.auth()` ready pattern
- [x] Disable submit button until `auth` object is confirmed initialized
- [x] Verify login works in browser: open `public/index.html` → enter credentials → redirects correctly
- [x] Commit: `fix: unify Firebase SDK to modular v10 in login page`

### IR-3. JWT Token Expiry — Stop "Forbidden" Errors
**Files:** `JwtUtil.java`, `AuthenticationController.java`, `dashboard/src/lib/auth.ts`  
**Why now:** Users get kicked out mid-session — kills the demo.

- [x] Add expiry check in `JwtUtil.java` — detect expired tokens and return `401` not `403`
- [x] Add `/api/auth/refresh` endpoint in `AuthenticationController`
- [x] Add auto-refresh interceptor in `dashboard/src/lib/auth.ts` (retry on 401)
- [x] Test: let token expire → confirm silent refresh → no logout

---

## ✅ Checklist — Day 2–3 (Working Dashboard)

### IR-4. Connect Dashboard to Real Backend — Audit & Wire Up
**Files:** `dashboard/src/pages/AdminDashboardUnified.tsx`, `AdminMonitoring.tsx`, others  
**Why now:** A dashboard showing fake data is not a product.

- [x] Audit each page for hardcoded/static data: `AdminDashboardUnified`, `AdminMonitoring`, `AdminProviders`, `AdminUsers`
- [x] Wire each to existing Spring Boot API endpoints using `fetch()` with auth header
- [x] Add loading spinner and error state to each page
- [x] Run dashboard locally (`npm run dev` in `dashboard/`) and verify real data appears

### IR-5. Fix i18next Double-Init Warning
**Files:** `dashboard/src/App.tsx`, `dashboard/src/i18n/conf.ts`  
**Why now:** Console noise in demos looks unprofessional.

- [ ] Confirm if there is a double `i18n.init()` call by checking browser console
- [ ] If warning exists: remove any duplicate language change call
- [ ] Ensure `!i18n.isInitialized` guard is first line in `conf.ts`
- [ ] Commit: `fix: remove i18next double initialization`

---

## ✅ Checklist — Day 3–4 (Gitingest & GitReverse Live)

### IR-6. Gitingest — First Runtime Test
**Location:** `gitingest/`  
**Why now:** It's implemented but never actually run.

- [x] `cd gitingest && pip install -e ".[all]" --break-system-packages`
- [x] `uvicorn gitingest.api.main:app --reload --port 8000`
- [x] Open `http://localhost:8000` — confirm UI loads
- [x] Test CLI: `python -m gitingest.cli https://github.com/pallets/flask -o /tmp/flask_digest.txt`
- [x] Fix any import errors or runtime crashes
- [x] Commit fixes: `fix: gitingest runtime errors`

### IR-7. GitReverse — First Runtime Test
**Location:** `gitreverse/`  
**Why now:** Implemented but never run.

- [x] `cd gitreverse && npm install --legacy-peer-deps`
- [x] Copy `.env.example` to `.env.local`, add `OPENROUTER_API_KEY`
- [x] `npm run dev` — confirm starts at `http://localhost:3000`
- [x] Test: paste `https://github.com/pallets/flask` → confirm LLM prompt generated
- [x] Fix TypeScript errors: `npm run build`
- [x] Commit fixes: `fix: gitreverse runtime and build errors`

---

## ✅ Checklist — Day 5 (Polish & Deploy)

### IR-8. Add React Error Boundaries (Quick Version)
**Files:** `dashboard/src/App.tsx`  
**Why now:** Without this, one broken component crashes the whole dashboard.

- [x] Create `dashboard/src/components/ErrorBoundary.tsx` (50 lines)
- [x] Wrap `<Suspense>` in `App.tsx` with `<ErrorBoundary fallback={<ErrorPage />}>`
- [x] Test by temporarily breaking an import — confirm fallback shows instead of white screen

### IR-9. Environment Variables Audit
**Files:** `functions/index.js`, `command-hub/cli/supcmd.py`, `supremeai-vscode-extension/package.json`  
**Why now:** Hardcoded URLs break.

- [x] Find all `localhost` or `supremeai-old.com` strings
- [x] Replace with `ide-api.supremeai.google.com` (as requested)
- [x] Ensure backend URL is read from `process.env.REACT_APP_API_URL` (in dashboard) and defaults correctly.

### IR-10. Smoke Test the Full Stack
**Why now:** Validate everything works together before release.

- [x] Run `./gradlew bootRun` in backend
- [x] Run `npm run dev` in dashboard
- [x] Attempt login -> Confirm dashboard loads real data
- [x] Hit "Refresh" on AdminUsers -> Check network tab for 200 OK
- [x] Commit any final bug fixes: `fix: final instant release blockers`
- [x] Trigger one CodeFlow analysis → confirm results appear
- [x] Trigger one Gitingest CLI run → confirm output file created
- [x] Trigger one GitReverse URL → confirm prompt generated

---

## 📦 Release Artifacts

Once all above are checked:

- [ ] Create git tag: `git tag v1.0.0-beta`
- [ ] Update `README.md` with working demo instructions
- [ ] Push to remote: `git push origin main --tags`
- [ ] Write release notes covering: backend, dashboard, Gitingest, GitReverse

---

## ⏱️ Time Estimate

| Task | Estimated Hours |
|------|----------------|
| IR-1 (Fix tests) | 3–5h |
| IR-2 (Firebase SDK) | 2–3h |
| IR-3 (JWT refresh) | 3–4h |
| IR-4 (Dashboard wiring) | 4–6h |
| IR-5 (i18next) | 0.5h |
| IR-6 (Gitingest runtime) | 2–3h |
| IR-7 (GitReverse runtime) | 2–3h |
| IR-8 (Error boundaries) | 1h |
| IR-9 (Env vars) | 1h |
| IR-10 (Smoke test) | 1–2h |
| **Total** | **~20–28 hours** |
