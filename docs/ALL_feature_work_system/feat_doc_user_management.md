# 🔐 Feature: Role Assignments & Access Controls (`AdminUserManagement.tsx`)

> **[CLASSIFICATION: ACCESS CONTROLS]**  
> **DEVELOPMENT STATUS: FULLY FUNCTIONAL**  

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)
*   **কম্পোনেন্ট:** `dashboard/src/pages/AdminUserManagement.tsx`
*   **ভিজুয়াল উইজেট:**
    *   **User Role Selector:** অ্যাডমিন, ডেভেলপার বা গেস্ট রোলের ড্রপডাউন মেনু।
    *   **Permission Checkbox Matrix:** ইউজারকে কোন কোন ফিচারের পারমিশন দেওয়া হবে তার গ্রিড।
    *   **Save Perms Button:** রোল পারমিশন সেভ করার বাটন।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)
*   **কন্ট্রোলার:** `UserAccountController.java`
*   **প্রধান API রাউট:** `POST /api/admin/users/roles`
*   **ডেটা ফরম্যাট:**
    ```json
    {
      "userId": "USR_8992",
      "newRole": "ADMIN",
      "permissions": ["ACCESS_BROWSER", "ACCESS_DEVELOPMENT"]
    }
    ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)
1. অ্যাডমিন কোনো ইউজারের রোল ও পারমিশন চেঞ্জ করে `Apply Role` বোতামে ক্লিক করেন।
2. ব্যাকএন্ড ফায়ারবেস বা ডাটাবেজে ইউজারের প্রোফাইল রোল ম্যাপটি আপডেট করে।
3. ইউজার রিফ্রেশ করলে নতুন এক্সেস পারমিশন অনুযায়ী তার ইন্টারফেস লোড হয়।
