# 📈 Feature: System Activity Summary (`AdminActivitySummary.tsx`)

> **[CLASSIFICATION: PLATFORM METRICS]**  
> **DEVELOPMENT STATUS: PARTIALLY COMPLETED**  

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)
*   **কম্পোনেন্ট:** `dashboard/src/pages/AdminActivitySummary.tsx`
*   **ভিজুয়াল উইজেট:**
    *   **Daily Action Counter:** গত ২৪ ঘণ্টায় সিস্টেমে ঘটা মোট কাজের মেট্রিক।
    *   **Success vs Failure Rates:** ব্রাউজার ক্রলিং বা এরর হিলিং সফলতার গ্রাফ।
    *   **Activity Export Button:** কাজের রিপোর্ট এক্সপোর্ট করার কন্ট্রোল।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)
*   **কন্ট্রোলার:** `SystemMetricsController.java` / `MonitoringController.java`
*   **প্রধান API রাউট:** `GET /api/system/activity/summary`
*   **ডেটা ফরম্যাট:**
    ```json
    {
      "totalActions": 234,
      "successPercent": 94.5,
      "failedActions": 12
    }
    ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)
1. পেজ লোড হওয়ার পর ফ্রন্টএন্ড সামারি এপিআই হিট করে।
2. ব্যাকএন্ড ডাটাবেজে রেকর্ড থাকা দৈনিক কাজের লগ রিড করে হিসাব কষে।
3. চূড়ান্ত ফলাফল গ্রাফ ও ড্যাশবোর্ডে পার্সেন্টেজ প্রোগ্রেস বার আকারে রেন্ডার হয়।
