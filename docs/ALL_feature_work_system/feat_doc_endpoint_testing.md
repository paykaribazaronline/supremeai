# 🧪 Feature: API Endpoint Test Suite (`AdminTesting.tsx`)

> **[CLASSIFICATION: AUTOMATION TESTING]**  
> **DEVELOPMENT STATUS: PARTIALLY COMPLETED**  

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)
*   **কম্পোনেন্ট:** `dashboard/src/pages/AdminTesting.tsx`
*   **ভিজুয়াল উইজেট:**
    *   **API Test Request Maker:** GET/POST এবং হেডার ও পেলোড ইনপুট ইন্টারফেস।
    *   **Response Timing Chart:** এপিআই-এর রেসপন্স টাইমের গ্রাফিক্যাল চার্ট।
    *   **Test Case Results Grid:** পাস/ফেইল স্ট্যাটাস ও হিলিং অপশন।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)
*   **কন্ট্রোলার:** `ApiTestingController.java`
*   **প্রধান API রাউট:** `POST /api/testing/run-case`
*   **ডেটা ফরম্যাট:**
    ```json
    {
      "caseId": "TEST_01",
      "statusCode": 200,
      "passed": true,
      "responseTimeMs": 14
    }
    ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)
1. ইউজার পেলোড ও হেডার সেট করে `RUN TEST CASE` বোতামে চাপ দেন।
2. ব্যাকএন্ড ভার্চুয়াল টেস্ট মডিউল সক্রিয় করে টার্গেট এপিআই রিকোয়েস্ট পাঠায়।
3. রেসপন্স হেডার ও রেসপন্স স্পিড রেকর্ড করে ফ্রন্টএন্ডে রেন্ডার করা হয়।
