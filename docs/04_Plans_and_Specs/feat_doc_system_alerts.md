# 🔔 Feature: System Health Alerts (`AdminSystemAlerts.tsx`)

> **Status:** 🟢 Updated for v5 Architecture


> **[CLASSIFICATION: SYSTEM MONITORING]**  
> **DEVELOPMENT STATUS: FULLY FUNCTIONAL**  

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)
*   **কম্পোনেন্ট:** `dashboard/src/pages/AdminSystemAlerts.tsx`
*   **ভিজুয়াল উইজেট:**
    *   **System Alert Banner List:** এরর বা ডেঞ্জার মেসেজের রঙিন প্রিমিয়াম লিস্ট।
    *   **Alert Acknowledge Button:** অ্যালার্টটি রিড করে ক্লিয়ার করার কন্ট্রোল।
    *   **Urgent System Alerts Counter:** ড্যাশবোর্ড সাইডবার নোটিফিকেশন ব্যাজ।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)
*   **কন্ট্রোলার:** `MonitoringController.java`
*   **প্রধান API রাউট:** `GET /api/system/alerts`
*   **ডেটা ফরম্যাট:**
    ```json
    {
      "alerts": [
        { "id": "AL_404", "level": "WARNING", "message": "Playwright sidecar offline" }
      ]
    }
    ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)
1. ব্যাকএন্ডের কোনো প্রসেস এরর ফেস করলে এটি ফায়ারবেসে ইভেন্ট স্টোর করে।
2. ফ্রন্টএন্ড লাইভ পোলিং দিয়ে অ্যালার্ট কুয়েরি করে।
3. ড্যাশবোর্ডে লাল বা হলুদ রঙের ডাইনামিক অ্যালার্ট বাটন রেন্ডার হয়।
