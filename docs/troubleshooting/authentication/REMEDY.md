# SupremeAI — Permanent Fixes Register

> **Purpose:** Document every root cause that has caused recurring auth/routing bugs so they are NEVER re-introduced.

---

## Root Cause 1 — Spring Boot serves `No static resource` error for SPA routes

**Symptom:** `{"error":"An unexpected error occurred: No static resource admin."}` on paths like `/admin/` or `/admin/users`.

**Root Cause:** Spring Boot 3 uses `PathPatternParser` which does NOT auto-match trailing slashes. When `/admin/` missed the `ViewController`, Spring's `ResourceHttpRequestHandler` intercepted it and threw `NoResourceFoundException`, which the `GlobalExceptionHandler` surfaces as a 500 JSON error.

**Permanent Fix (ViewController.java):**
```java
@GetMapping({
    "/admin", "/admin/",
    "/admin/{p1:[^\\.]*}",
    "/admin/{p1:[^\\.]*}/{p2:[^\\.]*}",
    "/admin/{p1:[^\\.]*}/{p2:[^\\.]*}/{p3:[^\\.]*}",
    "/admin/{p1:[^\\.]*}/{p2:[^\\.]*}/{p3:[^\\.]*}/{p4:[^\\.]*}"
})
public String admin() {
    return "forward:/admin/index.html";
}
```

**Permanent Fix (application.properties):**
```properties
spring.mvc.throw-exception-if-no-handler-found=true
spring.web.resources.add-mappings=false
```

> ⚠️ **NEVER** remove `spring.web.resources.add-mappings=false`. Doing so re-enables the default static resource handler which races with the `ViewController` for SPA routes.

---

## Root Cause 2 — Multiple token storage keys cause silent auth failures

**Symptom:** Some dashboard components (AuditLog, APIKeysManager, VPNManagement, etc.) send requests with no `Authorization` header and receive 401/403 silently.

**Root Cause:** The canonical token key is `supremeai_token` (set in `firebase.ts` and `authUtils.ts`), but ~20 components used the legacy key `authToken` via `localStorage.getItem('authToken')` directly. Since `authToken` was never set by the login flow, these components always had a `null` token.

**Permanent Fix (authUtils.ts):**
- `getToken()` now auto-migrates from `authToken` → `supremeai_token` on first read.
- `clearAuth()` and `clearToken()` both wipe both keys.

> ⚠️ **NEVER** call `localStorage.getItem('authToken')` directly in any component. **Always** use `authUtils.getToken()` or `authUtils.fetchWithAuth()`. This is the single source of truth.

---

## Root Cause 3 — Firebase Emulator connection leaks into production

**Symptom:** `Authentication failed` at `localhost:8080`. Backend rejects all tokens. Frontend logs `VITE_USE_FIREBASE_EMULATOR` connected.

**Root Cause:** The original `firebase.ts` connected to the local Firebase Auth Emulator on `localhost:9099` unconditionally when running on `localhost`. The emulator issues tokens signed with a test key. The production backend validates against the real Firebase project — so emulator tokens were always rejected.

**Permanent Fix (firebase.ts):**
```typescript
// Only connect to emulators when EXPLICITLY requested
if (import.meta.env.VITE_USE_FIREBASE_EMULATOR === 'true') {
  connectAuthEmulator(firebaseAuth, 'http://localhost:9099', { disableWarnings: true });
  ...
}
```

> ⚠️ **NEVER** make emulator connection conditional on `window.location.hostname === 'localhost'`. That is guaranteed to break production-like local testing.

---

## Root Cause 4 — Duplicate authentication filters

**Symptom:** Unexpected 401 errors where valid tokens are rejected, or valid tokens are accepted by one filter but then re-rejected by the next.

**Root Cause:** Two filters are registered — `AuthenticationFilter` (Firebase ID tokens) and `JwtAuthFilter` (internal JWTs). The order in `SecurityConfig.java` is:
```java
.addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class)
.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
```

Both correctly check `SecurityContextHolder` before running, but both run `addFilterBefore` the same class, meaning their relative order is JVM-implementation-dependent.

**Permanent Fix:** Login flow is: Firebase ID token → POST `/api/auth/firebase-login` → backend issues internal JWT. ALL subsequent requests use the internal JWT only. The `AuthenticationFilter` (Firebase) is only needed at the login endpoint itself — it is redundant on all other endpoints because the login exchange already happened.

> ✅ **Rule:** Frontend always stores and sends the **backend-issued JWT** (`supremeai_token`), NOT the raw Firebase ID token, to all API endpoints.

---

## Quick Reference: Auth Flow

```
User Login
  ↓
Firebase signInWithEmailAndPassword()
  ↓ (gets Firebase ID token, checks admin role claim)
POST /api/auth/firebase-login  { idToken: "<firebase-id-token>" }
  ↓ (AuthenticationFilter verifies the Firebase ID token)
  ↓ (AuthenticationService exchanges it for a backend JWT)
Response: { token: "<backend-jwt>", refreshToken: "..." }
  ↓
localStorage.setItem('supremeai_token', token)
  ↓
All subsequent API calls use:
  Authorization: Bearer <backend-jwt>
  ↓ (JwtAuthFilter validates the backend JWT)
```

---

*Last updated: 2026-05-09 by Antigravity — permanent root cause resolution*


## Problem: `PERMISSION_DENIED` in Firestore / Backend Login Failure

If the dashboard shows "AUTHENTICATION FAILED" and the backend logs contain `io.grpc.StatusRuntimeException: PERMISSION_DENIED`, follow these steps:

### 1. Check Service Account Credentials
Ensure `src/main/resources/firebase-service-account.json` exists and is valid.
The backend needs this to identify itself as an "Admin" to Firestore (both Cloud and Emulator).

### 2. Environment Variables
Always run the backend with the `GOOGLE_APPLICATION_CREDENTIALS` environment variable pointing to your service account JSON.
```bash
export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/src/main/resources/firebase-service-account.json
./gradlew bootRun
```

### 3. Avoid Manual Firestore ManagedChannel Configuration
Do not override the `ManagedChannel` or `FirestoreTemplate` beans manually unless you are explicitly attaching the `CredentialsProvider`. Overriding them without credentials makes the connection "anonymous," which is blocked by `firestore.rules`.

**Solution:** Use Spring Boot auto-configuration and set properties in `application.properties`:
```properties
spring.cloud.gcp.firestore.emulator.enabled=true
spring.cloud.gcp.firestore.host=localhost:8081
spring.cloud.gcp.credentials.location=classpath:firebase-service-account.json
```

### 4. Firestore Rules
Ensure `firestore.rules` allow the intended operations. For the Admin SDK, it should bypass rules automatically if correctly authenticated.
If using the Emulator, ensure the Emulator is started with the same rules that are in the project.

### 5. Dashboard Sync
If you make changes to the backend or security config, always rebuild and sync the dashboard:
```bash
bash scripts/supremeai-control.sh -> Module 2 -> Option 4
```

---
*Created by Antigravity on 2026-05-08*
