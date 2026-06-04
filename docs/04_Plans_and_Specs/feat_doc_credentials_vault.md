# 🔒 Feature: Credentials Vault Security (`AdminSecurity.tsx`)

> **Status:** 🟢 Updated for v5 Architecture


> **[CLASSIFICATION: KEY VAULT SECURITY]**  
> **DEVELOPMENT STATUS: FULLY FUNCTIONAL**  

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)
*   **কম্পোনেন্ট:** `dashboard/src/pages/AdminSecurity.tsx`
*   **ভিজুয়াল উইজেট:**
    *   **Encrypted Secrets Ledger:** সিস্টেমের পাসওয়ার্ড এনক্রিপশন স্ট্যাটাস দেখার গেটওয়ে।
    *   **Lock Secrets Button:** পাসওয়ার্ড টাইপ করে ভল্ট সিকিউর করার উইজেট।
    *   **AES Encryption Status Indicator:** ভল্ট সচল থাকার গ্রিন ডট সিগন্যাল।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)
*   **কন্ট্রোলার:** `AuthenticationController.java` / `APIKeyController.java`
*   **প্রধান API রাউট:** `POST /api/browser/credentials`
*   **ডেটা ফরম্যাট:**
    ```json
    {
      "targetSite": "github.com",
      "vaultKey": "ENC_3e89a...",
      "encrypted": true
    }
    ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)
1. ইউজার কোনো এপিআই কী বা পাসওয়ার্ড টাইপ করে `LOCK IN VAULT` বাটনে চাপ দেন।
2. ব্যাকএন্ড **AES-256 এনক্রিপশন** প্রয়োগ করে ডাটাবেজে সেভ করে।
3. ব্রাউজার পেজ রেন্ডার হওয়ার সময় ভল্ট থেকে পাসওয়ার্ড রিড করে অটোমেটিক ফর্ম ফিলআপ করে দেয়।
