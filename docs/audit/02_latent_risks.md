# 🟡 সুপ্ত ঝুঁকিসমূহ (Latent / Dormant Risks)
> বর্তমানে দৃশ্যমান প্রভাব নেই, কিন্তু নির্দিষ্ট পরিস্থিতিতে ক্রিটিক্যাল ফেইলিওর ট্রিগার করবে

**সর্বশেষ পুনরায় যাচাই:** 2026-05-21

---

## LAT-01: Firestore Rules — `isServiceAccount()` Bypass ✅ RESOLVED

| ফিল্ড | বিবরণ |
|:---|:---|
| **তীব্রতা** | ✅ **RESOLVED** |
| **যাচাই** | সম্পূর্ণ `firestore.rules` পড়া হয়েছে — `isServiceAccount` শব্দটি সম্পূর্ণ অনুপস্থিত ✅ |
| **Catch-all** | `match /{document=**} { allow read, write: if false; }` ✅ |

---

## LAT-02: Scraping Engine সম্পূর্ণ অনুপস্থিত 🟠 OPEN

| ফিল্ড | বিবরণ |
|:---|:---|
| **তীব্রতা** | 🟠 **HIGH — এখনও উন্মুক্ত** |
| **ডিরেক্টরি** | `functions/src/` |
| **যাচাই** | শুধুমাত্র `dataconnect-admin-generated/` সাবডিরেক্টরি; `.ts` স্ক্র্যাপিং ফাইল শূন্য ❌ |

### বর্তমান `functions/src/` অবস্থা
```
functions/src/
└── dataconnect-admin-generated/
    ├── index.d.ts
    ├── package.json
    ├── esm/package.json
    ├── esm/index.esm.js
    └── index.cjs.js
```
**প্রভাব:** লাইভ ওয়েব রিসার্চ শূন্য; `LEARN_WEBSITE` চ্যাট কমান্ড soft stub থাকবে।

---

## LAT-03: Cloud Run IAM ✅ RESOLVED

| ফিল্ড | বিবরণ |
|:---|:---|
| **তীব্রতা** | ✅ **RESOLVED** |
| **যাচাই** | `deploy.sh` — `reverse-engineering` ও `simulator-runtime` → `--no-allow-unauthenticated` ✅ |
| **backend** | intentionally `--allow-unauthenticated` (public API gateway; Spring Security লেয়ার নিশ্চিত করে) ✅ |

---

## LAT-04: ChatProcessingService-এ Stub Action Handlers ✅ RESOLVED

| ফিল্ড | বিবরণ |
|:---|:---|
| **তীব্রতা** | ✅ **RESOLVED** |
| **ফাইল** | `ChatProcessingService.java` লাইন 291-344 |
| **যাচাই** | `ADD_API` (লাইন 293), `TEST_API` (লাইন 336), `RUN_AUDIT` (লাইন 343) সব বিদ্যমান ✅ |

---

## LAT-05: CSRF Token ✅ RESOLVED

| ফিল্ড | বিবরণ |
|:---|:---|
| **তীব্রতা** | ✅ **RESOLVED** |
| **যাচাই** | `CookieCsrfTokenRepository.withHttpOnlyFalse()` — exempt: `/api/auth/**`, `/ws/**` only (লাইন 44-47) ✅ |

---

## LAT-06: `developUntilPerfection()` — `.block()` ✅ RESOLVED

| ফিল্ড | বিবরণ |
|:---|:---|
| **তীব্রতা** | ✅ **RESOLVED** |
| **ফাইল** | `SelfHealingService.java` লাইন 292-308 |
| **যাচাই** | `Mono.fromCallable(() -> votingService.conductApprovalVote(...)).subscribeOn(boundedElastic()).block()` — nested `.block()` নেই ✅ |

---

## LAT-07: Dashboard Build Pipeline ✅ RESOLVED

| ফিল্ড | বিবরণ |
|:---|:---|
| **তীব্রতা** | ✅ **RESOLVED** |
| **ফাইল** | `deploy.sh` |
| **যাচাই** | npm build → dist copy → gradlew clean build (সঠিক ক্রম) ✅ |

---

## ⚠️ NEW-01: `/api/workflows/**` ও `/api/ext/**` — `permitAll()` তে 🟡 নতুন

| ফিল্ড | বিবরণ |
|:---|:---|
| **তীব্রতা** | 🟡 **MEDIUM — নতুন চিহ্নিত** |
| **ফাইল** | `SecurityConfig.java` লাইন 109-110 |
| **যাচাই** | উভয় রুট `permitAll()` তালিকায় ✅ কিন্তু controller গুলো বিদ্যমান |

### বিবরণ
```java
// লাইন 109-110 (permitAll তালিকায়)
"/api/ext/**",        // ExternalToolsController.java — @RequestMapping("/api/ext")
"/api/workflows/**",  // WorkflowController.java — @RequestMapping("/api/workflows")
```

### সুপারিশ
- `WorkflowController` ও `ExternalToolsController` এর প্রতিটি endpoint review করুন
- যদি sensitive তথ্য বা operation থাকে → `SecurityConfig` থেকে `permitAll()` থেকে সরিয়ে `hasRole("ADMIN")` বা `.authenticated()` এ যোগ করুন
- অথবা controller-এ `@PreAuthorize("isAuthenticated()")` দিন

---
*পরবর্তী ফাইল: [03_security_audit.md](./03_security_audit.md)*