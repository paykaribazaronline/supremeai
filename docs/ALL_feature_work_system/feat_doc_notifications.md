# 🔔 Feature: System Notifications Manager (`AdminNotifications.tsx`)

> **[CLASSIFICATION: ALARM INTERACTION]**  
> **DEVELOPMENT STATUS: FULLY FUNCTIONAL**  

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)
*   **কম্পোনেন্ট:** `dashboard/src/pages/AdminNotifications.tsx`
*   **ভিজুয়াল উইজেট:**
    *   **Notification Ledger:** সিস্টেমে ঘটা সাম্প্রতিক অ্যালার্টের রঙিন বাবল লিস্ট।
    *   **Dismiss All Button:** সব নোটিফিকেশন এক ক্লিকে মুছে ফেলার কন্ট্রোল।
    *   **System Alert Badge:** ড্যাশবোর্ড হেডার নোটিফিকেশন কাউন্ট বেল আইকন।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)
*   **কন্ট্রোলার:** `PubSubWebhookController.java` / `functions/api-router.js`
*   **প্রধান API রাউট:** `GET /api/notifications` এবং `POST /api/notifications/dismiss`
*   **ডেটা ফরম্যাট:**
    ```json
    {
      "notifications": [
        { "id": "NOT_101", "type": "ALERT", "title": "Database is 90% full" }
      ]
    }
    ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)
1. ব্যাকএন্ডে কোনো জটিল অ্যালার্ট ফায়ার হলে ফায়ারবেসে একটি নোটিফিকেশন এন্ট্রি পুশ হয়।
2. ফ্রন্টএন্ড পুশ রিডার দিয়ে তা রিড করে এবং হেডার বেল আইকনে লাল ব্যাজ শো করে।
3. ইউজার নোটিফিকেশন প্যানেলে ক্লিক করে সেটি ডিসমিস করতে পারেন।
