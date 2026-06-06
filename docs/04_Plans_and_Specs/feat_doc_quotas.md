# ⏳ Feature: Token Rate Limits & Quotas Ledger (`AdminQuotas.tsx`)

> **Status:** 🟢 Updated for v5 Architecture

> **[CLASSIFICATION: USAGE COMPLIANCE]**  
> **DEVELOPMENT STATUS: PARTIALLY COMPLETED**

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)

- **কম্পোনেন্ট:** `dashboard/src/pages/AdminQuotas.tsx`
- **ভিজুয়াল উইজেট:**
  - **API Token Consumed Meter:** ইউজারের কোটা শেষ হতে কত বাকি তা দেখানোর থার্মোমিটার প্রোগ্রেস বার।
  - **Rate Limit Controller:** রিকোয়েস্ট পার মিনিট (RPM) লিমিট করার স্লাইডার প্যানেল।
  - **Membership Tier Badges:** প্রিমিয়াম বা গেস্ট লেভেলের ইউজার ক্যাটাগরি ভিউ।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)

- **কন্ট্রোলার:** `AdminQuotaController.java` / `UsageOptimizationController.java`
- **প্রধান API রাউট:** `GET /api/admin/quotas`
- **ডেটা ফরম্যাট:**
  ```json
  {
    "tokenLimit": 50000,
    "tokensUsed": 12890,
    "remainingQuota": 37110
  }
  ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)

1. ইউজার চ্যাট শুরু করলে বা এপিআই রান করলে ফ্রন্টএন্ড তার লাইভ কোটা চেক কুয়েরি করে।
2. ব্যাকএন্ড ফায়ারস্টোর থেকে ইউজারের মেম্বারশিপ ট্রিগার ও টোকেন কাউন্ট রিড করে।
3. কোটা লিমিট ওভার হলে ফ্রন্টএন্ডে স্বয়ংক্রিয়ভাবে `Token Quota Expired` লাল ব্যানার দেখায়।
