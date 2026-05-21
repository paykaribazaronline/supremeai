# 📋 অগ্রাধিকার-ভিত্তিক সমাধান পরিকল্পনা ও রোডম্যাপ
> টার্গেট: MASTER_TODO.md ও রোডম্যাপের সাথে সামঞ্জস্যপূর্ণ সমাধান কৌশল

**সর্বশেষ পুনরায় যাচাই:** 2026-05-21

---

## 🚨 ফেজ ১: জরুরি সিকিউরিটি ফিক্স ✅ সম্পন্ন

| আইটেম | কাজ | স্ট্যাটাস |
|:---|:---|:---:|
| ১.১ SecurityConfig `permitAll()` বাইপাস | 14 admin routes `hasRole("ADMIN")` | ✅ DONE |
| ১.২ Firestore Rules `isServiceAccount()` | Catch-all → deny by default | ✅ DONE |
| ১.৩ CORS safe defaults | property-based whitelist | ✅ DONE |
| ১.৪ Cloud Run IAM | `--no-allow-unauthenticated` internal services | ✅ DONE |

---

## 🔧 ফেজ ২: টেস্ট ইনফ্রাস্ট্রাকচার ✅ সম্পন্ন

| আইটেম | কাজ | স্ট্যাটাস |
|:---|:---|:---:|
| ২.১ `BaseFirestoreTest.java` | JUnit 5 Extension | ✅ DONE |
| ২.২ টেস্ট ফাইল বৃদ্ধি | 167 → 170 টেস্ট ফাইল | ✅ |

---

## 🔗 ফেজ ৩: Self-Healing Loop ✅ সম্পন্ন

| আইটেম | কাজ | স্ট্যাটাস |
|:---|:---|:---:|
| ৩.১ RCA `detectAndFix()` catch | warn + fallback + GKB record | ✅ DONE |
| ৩.২ nested `.block()` | `developUntilPerfection()` fix | ✅ DONE |
| ৩.৩ `getHealingHistory()` | Firestore real-time Flux | ✅ DONE |
| ৩.৪ `reindexModels()` | `component` field backfill | ✅ DONE |
| ৩.৫ `testAllProviders()` | 10-concurrent validation | ✅ DONE |
| ৩.৬ `isCodePerfect()` | 4-layer check (braces, class, lines ≥ 8) | ✅ UPGRADED |
| ৩.৭ `improveCode()` | `applyHeuristicImprovements()` multi-pass | ✅ UPGRADED |

---

## 🌐 ফেজ ৪: Chat Commands ✅ সম্পন্ন

| আইটেম | কাজ | স্ট্যাটাস |
|:---|:---|:---:|
| ৪.১ `ADD_API` | JSON parse → `APIProvider` save | ✅ DONE |
| ৪.২ `TEST_API` | `testAllProviders()` real validation | ✅ DONE |
| ৪.৩ `RUN_AUDIT` | অডিট পাথ প্রদান | 🟡 Soft Stub |
| ৪.৪ `LEARN_WEBSITE` | scrapeEngine না থাকায় soft stub | 🟡 Soft Stub |

---

## 🔜 ফেজ ৫: পরবর্তী অগ্রাধিকার (OPEN)

### 🔴 P1 — তাৎক্ষণিক (এখনই করুন)

| আইটেম | কাজ | অনুমানিত সময় |
|:---|:---|:---|
| **NEW-01a** | `WorkflowController` endpoint auth review করুন | 30 মিনিট |
| **NEW-01b** | `ExternalToolsController` endpoint auth review করুন | 30 মিনিট |
| **ACT-02-PROD** | প্রোডাকশনে `VITE_API_URL` → Cloud Run URL সেট | 5 মিনিট |

### 🟠 P2 — উচ্চ অগ্রাধিকার (এই সপ্তাহে)

| আইটেম | কাজ | অনুমানিত সময় |
|:---|:---|:---|
| **LAT-02** | `functions/src/scrapeEngine.ts` তৈরি করুন | 2-3 দিন |
| **STB-07** | `QualityScoringService` 6 test methods — real logic | 1 দিন |
| **STB-03** | `BrowserService.createBrowserTask()` সম্পূর্ণ | 1 দিন |
| **STB-04** | `AgentOrchestrationHub` code generation | 2-3 দিন |

### 🟡 P3 — মধ্যম অগ্রাধিকার (এই মাসে)

| আইটেম | কাজ | অনুমানিত সময় |
|:---|:---|:---|
| **STB-06** | `KnowledgeSeederServiceEnhanced` core seeders | 2 দিন |
| **STB-08** | `SimulatorController` Admin Operations | 1 দিন |
| **STB-09** | `CodeValidationService` full implementation | 1 দিন |
| **Dashboard Features** | 7 planned features | 1-2 সপ্তাহ |
| **Voting** | Double-Pass, Learning Loop, Browser Voting | 1 সপ্তাহ |
| **SEC-04** | `Permissions-Policy` ও `Referrer-Policy` হেডার | 1 ঘণ্টা |
| **GCP Secret Manager** | Backend secrets মাইগ্রেট | 2 ঘণ্টা |

---

## 📊 সামগ্রিক অগ্রগতি (পুনরায় যাচাই)

| মেট্রিক | 2026-05-20 | 2026-05-21 (পুনরায়) | পার্থক্য |
|:---|:---:|:---:|:---:|
| **রিডিনেস স্কোর** | 66/100 | **76/100** | **+10** ✅ |
| **সিকিউরিটি স্কোর** | 32/80 | **70/90** (78%) | **+38** ✅ |
| **সমাধান করা সমস্যা** | 0 | **19** | +19 ✅ |
| **বাকি সমস্যা** | 26 | **7** | -19 ✅ |
| **ব্যাকএন্ড ফাইল** | 632 | 631 | -1 |
| **টেস্ট ফাইল** | 167 | 170 | **+3** ✅ |
| **SelfHealingService** | ~529 লাইন | **619 লাইন** | +90 ✅ |

---

## ✅ সম্পূর্ণ সমাধান তালিকা (19/26)

- ✅ ACT-01 SecurityConfig 14 admin routes secured
- ✅ ACT-02 `VITE_API_URL=http://localhost:8080` — সেট করা আছে
- ✅ ACT-03 CORS wildcard → specific origin whitelist
- ✅ ACT-04 BaseFirestoreTest JUnit5 extension (170 test files)
- ✅ ACT-05 Self-Healing RCA catch → warn + fallback
- ✅ LAT-01 Firestore `isServiceAccount` → removed; deny-by-default
- ✅ LAT-03 Cloud Run IAM `--no-allow-unauthenticated` internal
- ✅ LAT-04 `ADD_API` + `TEST_API` fully implemented
- ✅ LAT-05 CSRF exempt only `/api/auth/**` and `/ws/**`
- ✅ LAT-06 `developUntilPerfection` nested `.block()` removed
- ✅ LAT-07 Dashboard build pipeline (npm build before gradlew)
- ✅ STB-A `getHealingHistory()` Firestore real-time Flux
- ✅ STB-B `reindexModels()` Firestore backfill
- ✅ STB-C `isCodePerfect()` 4-layer check (brace, class, lines ≥ 8)
- ✅ STB-D `improveCode()` → `applyHeuristicImprovements()` multi-pass

---

## 🔴 এখনও উন্মুক্ত সমস্যা (7টি)

| আইডি | বিষয় | অগ্রাধিকার |
|:---|:---|:---:|
| **LAT-02** | Scraping Engine — `functions/src/` empty | 🟠 High |
| **NEW-01** | `/api/workflows/**` ও `/api/ext/**` → `permitAll()` auth review | 🟡 Medium |
| **STB-03** | `BrowserService.createBrowserTask()` — stub | 🟠 Hard |
| **STB-04** | `AgentOrchestrationHub` code generation — stub | 🟠 Hard |
| **STB-06** | `KnowledgeSeederServiceEnhanced` core seeders — stub | 🟠 Hard |
| **STB-07** | `QualityScoringService` 6 test methods → always `return true` | 🟠 Hard |
| **STB-08** | `SimulatorController` Admin Operations — stubbed | 🟡 Medium |
