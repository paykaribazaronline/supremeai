# 📜 Feature: Global Scraping Policies (`AdminRules.tsx`)

> **Status:** 🟢 Updated for v5 Architecture


> **[CLASSIFICATION: WEB SCRAPING SECURITY]**  
> **DEVELOPMENT STATUS: FULLY FUNCTIONAL**  

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)
*   **কম্পোনেন্ট:** `dashboard/src/pages/AdminRules.tsx`
*   **ভিজুয়াল উইজেট:**
    *   **Allowed Domain List:** যে সাইটগুলো স্ক্র্যাপ করার অনুমতি আছে তার নামের তালিকা।
    *   **Blocked URL Input Box:** নিষিদ্ধ সাইট টাইপ করার সিকিউর প্যানেল।
    *   **Domain Priority Slider:** প্রায়োরিটি বা রুল সেট আপ স্লাইডার।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)
*   **কন্ট্রোলার:** `AdminSystemWorkRuleController.java`
*   **প্রধান API রাউট:** `POST /api/browser/urls/allowed` এবং `POST /api/browser/urls/denied`
*   **ডেটা ফরম্যাট:**
    ```json
    {
      "domain": "facebook.com",
      "policy": "DENY",
      "timestamp": "ISO Date"
    }
    ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)
1. ইউজার একটি সিকিউরিটি পলিসি রুল টাইপ করে `SAVE RULE` বাটনে চাপ দেন।
2. ব্যাকএন্ডের সিকিউরিটি রুল ডাটাবেজে পলিসি জেসনটি আপডেট হয়ে যায়।
3. ব্রাউজার কুয়েরি করার আগে রুল চেক করে এবং ব্লক সাইট হলে ডিনাইড রেসপন্স পাঠায়।
