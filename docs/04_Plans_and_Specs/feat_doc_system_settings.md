# ⚙️ Feature: System Preferences Settings (`AdminSettings.tsx`)

> **Status:** 🟢 Updated for v5 Architecture


> **[CLASSIFICATION: PLATFORM PREFERENCES]**  
> **DEVELOPMENT STATUS: FULLY FUNCTIONAL**  

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)
*   **কম্পোনেন্ট:** `dashboard/src/pages/AdminSettings.tsx`
*   **ভিজুয়াল উইজেট:**
    *   **Operational Mode Selector:** সিস্টেম অনলাইন বনাম সোলো মোড সুইচ।
    *   **Language preference dropdown:** বাংলা/ইংরেজি ল্যাঙ্গুয়েজ সিলেক্টর।
    *   **Dark Mode Toggle:** ডার্ক এবং লাইট থিম মোড বাটন।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)
*   **কন্ট্রোলার:** `AdminConfigController.java` / `UserLanguagePreferenceController.java`
*   **প্রধান API রাউট:** `POST /api/settings/update`
*   **ডেটা ফরম্যাট:**
    ```json
    {
      "mode": "SOLO",
      "language": "bn",
      "darkMode": true
    }
    ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)
1. ইউজার মোড বা থিম চেঞ্জ করে `Save Settings` বাটনে প্রেস করেন।
2. ব্যাকএন্ড ইউজারের পছন্দটি ডাটাবেজ বা লোকাল কনফিগারেশন ফাইলে আপডেট করে।
3. ড্যাশবোর্ড নতুন প্রেফারেন্স অনুযায়ী সাকসেসফুলি রিস্টার্ট হয়ে সম্পূর্ণ ইন্টারফেস চেঞ্জ করে।
