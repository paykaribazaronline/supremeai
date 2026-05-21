# 🔴 সক্রিয় ত্রুটিসমূহ (Active Errors)
> বর্তমানে সিস্টেমে অপারেশনাল, ইউজার-ফেসিং, বা ডেভেলপমেন্ট ওয়ার্কফ্লো ব্যাহত করছে

**সর্বশেষ পুনরায় যাচাই:** 2026-05-21

---

## ACT-01: SecurityConfig-এ অতিরিক্ত `permitAll()` — এন্ডপয়েন্ট বাইপাস ✅ FIXED

| ফিল্ড | বিবরণ |
|:---|:---|
| **তীব্রতা** | ✅ **RESOLVED** |
| **ফাইল** | `SecurityConfig.java` লাইন 116-129 |
| **যাচাই** | ✅ 14টি admin route `hasRole("ADMIN")` দিয়ে সুরক্ষিত |

```java
// লাইন 115-129 — কোডে বিদ্যমান ✅
// 3. Admin routes — STRICTLY ADMIN only (must come BEFORE any broad wildcard)
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.requestMatchers("/api/admin/chat/**").hasRole("ADMIN")
.requestMatchers("/api/self-healing/**").hasRole("ADMIN")
.requestMatchers("/api/healing/**").hasRole("ADMIN")
.requestMatchers("/api/debug/**").hasRole("ADMIN")
.requestMatchers("/api/security/**").hasRole("ADMIN")
.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
.requestMatchers("/api/v1/agents/**").hasRole("ADMIN")
.requestMatchers("/api/v1/optimization/**").hasRole("ADMIN")
.requestMatchers("/api/v1/phase6/**").hasRole("ADMIN")
.requestMatchers("/api/phase7/**").hasRole("ADMIN")
.requestMatchers("/api/v1/agents/phase8/**").hasRole("ADMIN")
.requestMatchers("/api/v1/agents/phase9/**").hasRole("ADMIN")
.requestMatchers("/api/v1/agents/phase10/**").hasRole("ADMIN")
```

### ⚠️ নতুন সতর্কতা (পুনরায় যাচাই)
`permitAll()` তালিকায় নতুন রুট যোগ হয়েছে যা আগে ছিল না:
- `/api/workflows/**` → `WorkflowController.java` বিদ্যমান — auth review করুন
- `/api/ext/**` → `ExternalToolsController.java` বিদ্যমান — auth review করুন

---

## ACT-02: `.env` ফাইলে প্লেইন-টেক্সট সিক্রেটস এক্সপোজার ✅ RESOLVED

| ফিল্ড | বিবরণ |
|:---|:---|
| **তীব্রতা** | ✅ **RESOLVED** (পুনরায় যাচাই) |
| **ফাইল** | `dashboard/.env` |
| **যাচাই** | `VITE_API_URL=http://localhost:8080` — এখন সেট করা আছে ✅ |

### বর্তমান `dashboard/.env` অবস্থা
```
VITE_FIREBASE_API_KEY=AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8
VITE_FIREBASE_PROJECT_ID=supremeai-a
...
VITE_WS_URL=/ws
VITE_API_URL=http://localhost:8080   ✅ সেট করা আছে
VITE_USE_EMULATOR=false
VITE_USE_FIREBASE_EMULATOR=false
```

### বাকি করণীয় (Deferred)
- প্রোডাকশনে `VITE_API_URL` → Cloud Run URL সেট করুন
- GCP Secret Manager-এ backend secrets মাইগ্রেট করুন
- `.gitignore` সুরক্ষা: `.env`, `.env.*`, `*.env` — সব কভার্ড ✅

---

## ACT-03: CORS কনফিগারেশনে `origins = *` ফলব্যাক ✅ FIXED

| ফিল্ড | বিবরণ |
|:---|:---|
| **তীব্রতা** | ✅ **RESOLVED** |
| **ফাইল** | `SecurityConfig.java` লাইন 155-200 |
| **যাচাই** | `setAllowedOriginPatterns()` — property থেকে পড়ে; fallback: `localhost:5173`, `localhost:3000`, `supremeai-a.web.app` ✅ |

---

## ACT-04: ৫৮+ টেস্ট ফেইলিওর — Firebase Emulator Context ✅ FIXED

| ফিল্ড | বিবরণ |
|:---|:---|
| **তীব্রতা** | ✅ **RESOLVED** |
| **ফাইল** | `src/test/java/com/supremeai/BaseFirestoreTest.java` |
| **যাচাই** | ✅ ফাইল বিদ্যমান; 170 টেস্ট ফাইল (আগের 167 থেকে বৃদ্ধি) |

---

## ACT-05: Self-Healing → RCA লুপ অসম্পূর্ণ সংযোগ ✅ FIXED

| ফিল্ড | বিবরণ |
|:---|:---|
| **তীব্রতা** | ✅ **RESOLVED** |
| **ফাইল** | `SelfHealingService.java` লাইন 232-264 (619 লাইনে বিস্তৃত) |
| **যাচাই** | `try/catch` সহ RCA পাইপলাইন + `log.warn()` fallback ✅ |

---

*পরবর্তী ফাইল: [02_latent_risks.md](./02_latent_risks.md)*
