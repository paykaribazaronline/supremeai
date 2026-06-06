# ⏱️ Feature: API Route Performance Metrics (`AdminPerformance.tsx`)

> **Status:** 🟢 Updated for v5 Architecture

> **[CLASSIFICATION: METRIC AUDITING]**  
> **DEVELOPMENT STATUS: PARTIALLY COMPLETED**

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)

- **কম্পোনেন্ট:** `dashboard/src/pages/AdminPerformance.tsx`
- **ভিজুয়াল উইজেট:**
  - **API Latency Graph:** ব্যাকএন্ড এপিআই কলের গড় রেসপন্স টাইমের লাইন গ্রাফ।
  - **Route Traffic Counter:** সর্বোচ্চ ব্যবহার হওয়া এপিআই রাউটগুলোর বার চার্ট।
  - **Thread Block Indicator:** কোনো থ্রেড প্রসেসিং জ্যামে আটকে আছে কি না তা দেখানোর স্পিডোমিটার গেজ।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)

- **কন্ট্রোলার:** `SystemMetricsController.java`
- **প্রধান API রাউট:** `GET /api/system/performance`
- **ডেটা ফরম্যাট:**
  ```json
  {
    "pingMs": 24,
    "averageLatencyMs": 145.2,
    "databaseResponseMs": 8.1
  }
  ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)

1. পারফরম্যান্স পেজ লোড হলে ফ্রন্টএন্ড ল্যাটেন্সি মেট্রিক এপিআই রিকোয়েস্ট পাঠায়।
2. ব্যাকএন্ড প্রতি এপিআই কলের শুরু থেকে শেষ সময় ট্র্যাক করে গড় ল্যাটেন্সি রেসপন্স ক্যালকুলেট করে।
3. তথ্যগুলো চার্ট লাইব্রেরির মাধ্যমে রিয়েল-টাইমে রিফ্রেশ হয়ে ড্যাশবোর্ডে শো করে।
