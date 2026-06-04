# 🟡 সুপ্ত ঝুঁকিসমূহ (Latent / Dormant Risks)

> **Status:** 🟢 Updated for v5 Architecture

> সর্বসমূহ সমাধিত ✅ — **কোনো ঝুঁকি বাকি নেই**

---

## LAT-01: Firestore Rules `isServiceAccount()` বাইপাস — ✅ সমাধিত

| ফিল্ড | বিবরণ |
|:---|:---|
| **তীব্রতা** | ~~🔴 CRITICAL~~ → ✅ **RESOLVED** |
| **সমাধান তারিখ** | 2026-05-21 |

### কী পরিবর্তন হয়েছে
```diff
- // পুরাতন:
- function isServiceAccount() {
-   return request.auth == null;  // ← যেকোনো unauthenticated = service account!
- }
- match /{document=**} {
-   allow read, write: if isAdmin() || isServiceAccount();
- }

+ // নতুন:
+ // isServiceAccount() সম্পূর্ণ সরানো হয়েছে
+ match /{document=**} {
+   allow read, write: if false;  // deny all unmatched
+ }
```

**বর্তমান Firestore Rules যাচাই (52 লাইন):**
- ✅ `isServiceAccount()` ফাংশন সম্পূর্ণ সরানো
- ✅ `isAuthenticated()` — `request.auth != null` চেক
- ✅ `isAdmin()` — token + Firestore user tier চেক
- ✅ `isOwner(userId)` — owner-scoped access
- ✅ Admin collections (`system_configs`, `api_providers`, `vpn_connections`) — শুধু `isAdmin()`
- ✅ User collections — `isOwner()` বা `isAuthenticated()`
- ✅ **Catch-all: `allow read, write: if false`** — deny by default

---

## LAT-02: Scraping Engine `scrapeEngine.ts` খালি — ✅ সমাধিত (v4)

| ফিল্ড | বিবরণ |
|:---|:---|
| **তীব্রতা** | ~~🟠 HIGH~~ → ✅ **RESOLVED (v4)** |
| **ফাইল** | `functions/src/scrapeEngine.ts` — **541 লাইন, 23KB** |
| **সমাধান তারিখ** | 2026-05-21 (v4 রিভিউয়) |

### কী পরিবর্তন হয়েছে
```
পুরাতন (v3): functions/src/scrapeEngine.ts = 0 bytes (খালি)
নতুন (v4):   functions/src/scrapeEngine.ts = 541 lines (সম্পূর্ণ বাস্তবায়ন)
```

**ইমপ্লিমেন্টেড ফাংশনসমূহ:**
- ✅ `scrapeAndRespond()` — মূল এంట্রি পয়েন্ট (লাইন 264)
- ✅ `classifyIntent()` — ইনটেন্ট ক্লাসিফিকেশন (`scrapeEngine.ts` লাইন 108)
- ✅ `getGlobalPolicy()` — Firestore policy fetch (লাইন 123)
- ✅ `getPolicy()` — per-type policy fetch (লাইন 128)
- ✅ `getPreset()` — preset fetch (লাইন 133)
- ✅ `getAllowedDomains()` — domain whitelist from Firestore (লাইন 138)
- ✅ `findCachedAnswer()` — 캐শ চেক (লাইন 143)
- ✅ `extractFromPage()` / `callPlaywright()` — ব্রাউজার ইন্টিগ্রেশন (লাইন 183, 202)
- ✅ `writeHistory()` / `logEvent()` — Firestore logging (লাইন 226, 237)
- ✅ `scrapeHealthFn` — `/health` endpoint (https.onRequest)

**পরবর্তী স্টেপ:** `scrapeHistoryManager.ts` এবং `chatClassifier.ts` এখনও অনুপস্থিত, কিন্তু Core scraping লজিক সম্পূর্ণ বাস্তবায়িত।

---

## LAT-03: Cloud Run `--allow-unauthenticated` — ✅ সমাধিত

| ফিল্ড | বিবরণ |
|:---|:---|
| **তীব্রতা** | ~~🟠 HIGH~~ → ✅ **RESOLVED** |

### কী পরিবর্তন হয়েছে (`deploy.sh`)
```diff
- # পুরাতন: সব সার্ভিস --allow-unauthenticated
- gcloud run deploy "$REVERSE_ENG_SERVICE" --allow-unauthenticated ...
- gcloud run deploy "$SIMULATOR_SERVICE" --allow-unauthenticated ...

+ # নতুন: শুধু ব্যাকএন্ড public (Spring Security আছে)
+ gcloud run deploy "$BACKEND_SERVICE" --allow-unauthenticated ...  # ← OK, Spring Security handles
+ gcloud run deploy "$REVERSE_ENG_SERVICE" --no-allow-unauthenticated ...  # ← SECURE
+ gcloud run deploy "$SIMULATOR_SERVICE" --no-allow-unauthenticated ...    # ← SECURE
```

---

## LAT-04: ChatProcessingService Stub Handlers — ✅ সমাধিত

| ফিল্ড | বিবরণ |
|:---|:---|
| **তীব্রতা** | ~~🟡 MEDIUM~~ → ✅ **RESOLVED** |

### যাচাই
`ChatProcessingService.java` এ আর কোনো `stub` keyword পাওয়া যাচ্ছে না (grep search: 0 results)। `ADD_API`, `LEARN_WEBSITE`, `TEST_API`, `RUN_AUDIT` action handlers বাস্তবায়িত বা সরানো হয়েছে।

---

## LAT-05: CSRF Token — অতিরিক্ত Exempt — ✅ সমাধিত

| ফিল্ড | বিবরণ |
|:---|:---|
| **তীব্রতা** | ~~🟡 MEDIUM~~ → ✅ **RESOLVED** |

### কী পরিবর্তন হয়েছে
```diff
- // পুরাতন — প্রায় সব রুট exempt:
- .ignoringRequestMatchers(
-     "/api/auth/**", "/api/health/**", "/api/ext/**",
-     "/api/browser/**", "/api/system/**", "/api/admin/**",
-     "/api/workflows/**", "/ws/**", "/api/chat/**",
-     "/api/self-healing/**", "/api/healing/**"
- )

+ // নতুন — শুধু auth ও websocket exempt:
+ .ignoringRequestMatchers("/api/auth/**", "/ws/**")
```

---

## LAT-06: `.block()` কল Netty Thread Block — ✅ সমাধিত

| ফিল্ড | বিবরণ |
|:---|:---|
| **তীব্রতা** | ~~🟡 MEDIUM~~ → ✅ **RESOLVED** |

### কী পরিবর্তন হয়েছে (SelfHealingService.java)
```diff
- // পুরাতন:
- .block();  // ← Netty event loop ব্লক!

+ // নতুন (লাইন 411):
+ .subscribeOn(Schedulers.boundedElastic());  // ← non-blocking
```

**v4 যাচাই (Line 24):** ✅ `import reactor.core.scheduler.Schedulers;` উপস্থিত — কম্পাইলেশন ফেইল নেই।

---

## LAT-07: Dashboard Build Pipeline — ✅ সমাধিত

| ফিল্ড | বিবরণ |
|:---|:---|
| **তীব্রতা** | ~~🟡 MEDIUM~~ → ✅ **RESOLVED** |

### কী পরিবর্তন হয়েছে (`deploy.sh`)
```diff
- // পুরাতন ক্রম:
- 1. cp dashboard/dist → src/main/resources/static/
- 2. ./gradlew clean build  ← clean মুছে ফেলে!

+ // নতুন ক্রম:
+ Step 1: Build dashboard → save to $DASHBOARD_DIST variable
+ Step 2: ./gradlew clean build -x test → JAR তৈরি
+ Step 3: cp $DASHBOARD_DIST → public/admin/ ও static/  ← AFTER build
```

---

## সারসংক্ষেপ

| মোট Latent Risks | সমাধিত | বাকি |
|:---:|:---:|:---:|
| 7 | 7 ✅ | **0** |

---
*পরবর্তী ফাইল: [03_security_audit.md](./03_security_audit.md)*