# Feature 09: Quota & Tier Management
> **অবস্থা:** ✅ বিদ্যমান (সম্পূর্ণ)
> **Priority:** HIGH
> **ফাইলসমূহ:** `QuotaService.java` (9K), `QuotaController.java` (6K), `QuotaPredictionService.java` (5K), `QuotaManager.java`, `UserTier.java`, `ConfigService.java`

---

## 🎯 ফিচারটি কী করে?

ব্যবহারকারীদের API ব্যবহারের সীমা (quota) নিয়ন্ত্রণ করে। Tier অনুযায়ী মাসিক request limit নির্ধারণ, ব্যবহার ট্র্যাক, এবং AI-powered prediction দিয়ে quota শেষ হওয়ার সতর্কতা দেয়।

---

## 🔄 সম্পূর্ণ ফ্লো

```mermaid
flowchart TD
    A([👤 API Request]) --> B[QuotaService.hasQuotaRemaining]
    B --> C{Quota আছে?}
    C -->|"হ্যাঁ"| D[QuotaService.incrementUsage]
    C -->|"না"| E([🛑 429 Quota Exceeded])

    D --> F[Firestore-এ count +1]
    F --> G[QuotaPredictionService.recordUsage]
    G --> H[7-day Moving Average হিসাব]

    H --> I{daysRemaining <= 3?}
    I -->|"হ্যাঁ"| J[⚠️ Warning Generate]
    I -->|"না"| K[✅ Normal]

    L([🔧 Admin]) --> M[GET /api/quota/warnings]
    M --> N[সব users-এর warning list]

    L --> O[POST /api/quota/{userId}/reset]
    O --> P[Usage reset to 0]
    P --> Q[ActivityLog তে record]

    R([⏰ Cron: প্রতি মাসে ১ তারিখ]) --> S[resetMonthlyUsage]
    S --> T[সব API keys-এর count = 0]
```

---

## 📋 বর্তমান Implementation

| কম্পোনেন্ট | বিবরণ | অবস্থা |
|------------|-------|--------|
| Quota Check | hasQuotaRemaining (reactive) | ✅ |
| Usage Increment | Atomic increment with optimistic locking | ✅ |
| Guest Mode | GUEST_MODE auto-create, 50 req limit | ✅ |
| Monthly Reset | Scheduled cron (1st of month) | ✅ |
| Admin Reset | Manual user/API quota reset | ✅ |
| Usage Stats | currentUsage, percentage, remaining | ✅ |
| Prediction Service | 7-day moving average | ✅ |
| Warning System | <= 3 days remaining alert | ✅ |
| Daily Prediction Update | Scheduled midnight cron | ✅ |
| Tier-based Limits | ConfigService dynamic quotas | ✅ |
| Authorization | Users own quota, admins all | ✅ |
| Audit Logging | Reset actions logged | ✅ |

---

## ❌ কী মিসিং?

| মিসিং অংশ | প্রভাব | জরুরিতা |
|-----------|--------|---------|
| **Real-time quota UI** — dashboard-এ live usage | no live view | 🟡 High |
| **Overage billing** — quota শেষে paid overage | hard cutoff only | 🟡 High |
| **Per-feature quotas** — feature-wise limit | global count only | 🟡 High |
| **Quota upgrade flow** — self-service tier upgrade | admin-only | 🟠 Medium |
| **Email notifications** — quota warning email | no notifications | 🟠 Medium |
| **Usage analytics** — historical trends chart | no visualization | 🟠 Medium |
| **Rate limiting per minute** — burst protection | monthly only | 🟡 High |

---

## 🆚 প্রতিযোগী তুলনা

| ফিচার | SupremeAI | OpenAI | Claude | Gemini |
|-------|-----------|--------|--------|--------|
| Monthly Quota | ✅ | ✅ | ✅ | ✅ |
| Tier System | ✅ | ✅ | ✅ | ✅ |
| Prediction/Warning | ✅ | ❌ | ❌ | ❌ |
| Overage Billing | ❌ | ✅ | ✅ | ✅ |
| Real-time Dashboard | ❌ | ✅ | ✅ | ✅ |
| Per-minute Rate Limit | ❌ | ✅ | ✅ | ✅ |
| Self-service Upgrade | ❌ | ✅ | ✅ | ✅ |

---

## 📊 API Endpoints

| Endpoint | Method | কাজ | অবস্থা |
|----------|--------|-----|--------|
| `/api/quota/{userId}` | GET | Usage stats | ✅ |
| `/api/quota/{userId}/reset` | POST | Admin reset | ✅ |
| `/api/quota/{userId}/prediction` | GET | Prediction | ✅ |
| `/api/quota/warnings` | GET | All warnings | ✅ |
| `/api/quota/upgrade` | POST | Tier upgrade | ❌ মিসিং |
| `/api/quota/history/{userId}` | GET | Usage history | ❌ মিসিং |

---

*বিশ্লেষণ তারিখ: ২০২৬-০৫-১৪*
