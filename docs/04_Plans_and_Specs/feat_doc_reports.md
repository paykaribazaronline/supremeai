# 📄 Feature: Cost Auditing & AI Accuracy Reports (`AdminReports.tsx`)

> **Status:** 🟢 Updated for v5 Architecture


> **[CLASSIFICATION: COMMERCIAL AUDITING]**  
> **DEVELOPMENT STATUS: PARTIALLY COMPLETED**  

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)
*   **কম্পোনেন্ট:** `dashboard/src/pages/AdminReports.tsx`
*   **ভিজুয়াল উইজেট:**
    *   **Monthly Spent Chart:** বিভিন্ন এআই প্রোভাইডারের মাসিক বিল ও টোকেন কস্টের এরিয়া চার্ট।
    *   **Accuracy Index Badge:** এআই উত্তরের নির্ভুলতা বা ফেইথফুলনেস পার্সেন্টেজ স্কোর গেজ।
    *   **Export PDF Button:** মাসিক ডেটা রিপোর্ট এক্সপোর্ট করার কমান্ড।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)
*   **কন্ট্রোলার:** `CostTransparencyController.java` / `AIBehaviorProfileController.java`
*   **প্রধান API রাউট:** `GET /api/reports/cost-summary`
*   **ডেটা ফরম্যাট:**
    ```json
    {
      "monthlyCostUSD": 23.40,
      "tokenCounts": 450000,
      "avgAccuracyScore": 0.92
    }
    ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)
1. ইউজার `Generate Report` বাটনে ক্লিক করলে রিমোট এপিআই কল করা হয়।
2. ব্যাকএন্ডের ডাটা প্রসেসর লোকাল লগে জমা থাকা মোট এপিআই রিকোয়েস্ট ও মডেল কস্ট প্রসেস করে রিপোর্ট ফাইল তৈরি করে।
3. চার্ট ও কস্ট ট্র্যাকিং সামারি ফ্রন্টএন্ড স্ক্রিনে ভেসে ওঠে।
