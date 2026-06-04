# 👥 Feature: Active Members Directory (`AdminUsers.tsx`)

> **Status:** 🟢 Updated for v5 Architecture


> **[CLASSIFICATION: ACCOUNTS DATABASE]**  
> **DEVELOPMENT STATUS: FULLY FUNCTIONAL**  

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)
*   **কম্পোনেন্ট:** `dashboard/src/pages/AdminUsers.tsx`
*   **ভিজুয়াল উইজেট:**
    *   **Active Users List Table:** ইউজারের নাম, ইমেইল ও অ্যাকাউন্ট টাইপের ইন্টারেক্টিভ টেবিল।
    *   **Search and Filter Panel:** নির্দিষ্ট ইউজার সার্চ করার প্যানেল।
    *   **Ban/Activate Toggle Switch:** অ্যাকাউন্ট ব্লক বা আনব্লক করার বাটন।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)
*   **কন্ট্রোলার:** `UserAccountController.java`
*   **প্রধান API রাউট:** `GET /api/admin/users`
*   **ডেটা ফরম্যাট:**
    ```json
    {
      "users": [
        { "id": "USR_01", "name": "Nazifa", "email": "nazifa@supreme.ai", "active": true }
      ]
    }
    ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)
1. মেম্বার ডিরেক্টরি পেজ ওপেন করার সাথে সাথে এপিআই রিকোয়েস্ট ব্যাকএন্ডে ফায়ার হয়।
2. ব্যাকএন্ড ফায়ারস্টোর থেকে সব ইউজারের অ্যাকাউন্ট জেসন অবজেক্ট রিটার্ন করে।
3. ফ্রন্টএন্ডে ডেটা এসে ফিল্টারিং ও রোল টেবিল ভিউ আকারে সাজিয়ে দেয়।
