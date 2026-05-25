# 📚 SupremeAI Feature Encyclopedia - Volume 3: Security, VPN & Database

> **[VOLUME STATUS: 100% OPERATIONAL]**  
> **[LANG: BENGALI]**  

এই ভলিউমে সুপ্রিম এআই (SupremeAI) প্ল্যাটফর্মের **Security, VPN এবং Database** বিষয়ক ৫টি ফিচারের পূর্ণাঙ্গ ফ্রন্টএন্ড ও ব্যাকএন্ড আর্কিটেকচার এবং কার্যপ্রণালী দেওয়া হলো।

---

## 🔒 ১. Credentials Security Vault (`AdminSecurity.tsx`)
*   **স্থিতি (Status):** **Fully Functional**
*   **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
    *   **কম্পোনেন্ট:** `dashboard/src/pages/AdminSecurity.tsx`
    *   **ভিজুয়াল উইজেট:** এনক্রিপ্টেড সিক্রেট স্টোরেজ কার্ড, পাসওয়ার্ড এনক্রিপশন স্ট্যাটাস নোটিফিকেশন এবং ডিলিট/অ্যাড সিক্রেট বাটন।
*   **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
    *   **কন্ট্রোলার:** `AuthenticationController.java` / `APIKeyController.java`
    *   **প্রধান API রাউট:** `POST /api/browser/credentials`
*   **কীভাবে কাজ করে (How it Works - Data Flow):**
    1. ইউজার কোনো সাইটের পাসওয়ার্ড সেভ করতে টাইপ করে `LOCK IN VAULT` বাটনে চাপ দেন।
    2. ব্যাকএন্ডে **AES-256 এনক্রিপশন** অ্যালগরিদমের মাধ্যমে পাসওয়ার্ডটি সম্পূর্ণ এনক্রিপ্ট হয়ে ফায়ারবেসে স্টোর হয়।
    3. ব্রাউজার লগইনের সময় ব্যাকএন্ড স্বয়ংক্রিয়ভাবে পাসওয়ার্ডটি ডিক্রিপ্ট করে ফর্মের ভেতর বসিয়ে দেয়।

---

## 📜 ২. Global Scraping & Security Policies (`AdminRules.tsx`)
*   **স্থিতি (Status):** **Fully Functional**
*   **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
    *   **কম্পোনেন্ট:** `dashboard/src/pages/AdminRules.tsx`
    *   **ভিজুয়াল উইজেট:** ডোমেইন এলাও/ডিনাই পলিসি উইন্ডো, নতুন সিকিউরিটি রুল অ্যাড করার টেক্সট এরিয়া এবং অ্যাক্টিভ সিকিউরিটি গার্ড ইন্ডিকেটর।
*   **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
    *   **কন্ট্রোলার:** `AdminSystemWorkRuleController.java` / `SystemAdminRuleController.java`
    *   **প্রধান API রাউট:** `POST /api/browser/urls/allowed` এবং `POST /api/browser/urls/denied`
*   **কীভাবে কাজ করে (How it Works - Data Flow):**
    1. ইউজার একটি সিকিউরিটি পলিসি অ্যাড করে (যেমন: `Deny facebook.com`)।
    2. ব্যাকএন্ড ফায়ারবেস বা লোকাল স্টোরেজে পলিসি তালিকা আপডেট করে।
    3. ব্রাউজার কোনো সাইট লোড করার আগে পলিসি চেক রান করে এবং নিষিদ্ধ সাইট হলে `blocked` এরর মেসেজ দিয়ে স্ক্র্যাপ স্টপ করে।

---

## 🛡️ ৩. VPN Proxy IP Tunnel Controller (`AdminVPN.tsx`)
*   **স্থিতি (Status):** **Partially Completed**
*   **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
    *   **কম্পোনেন্ট:** `dashboard/src/pages/AdminVPN.tsx`
    *   **ভিজুয়াল উইজেট:** সক্রিয় আইপি প্রক্সি এড্রেস, ভিপিএন নেটওয়ার্ক কানেকশন স্ট্যাটাস ম্যাপ এবং ডাইনামিক আইপি রোটেশন (IP Rotation) বাটন।
*   **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
    *   **কন্ট্রোলার:** `FailoverController.java`
    *   **প্রধান API রাউট:** `POST /api/vpn/rotate-ip`
*   **কীভাবে কাজ করে (How it Works - Data Flow):**
    1. স্ক্র্যাপিং ব্লক বা রেট লিমিট এড়াতে ইউজার `Rotate IP` বাটনে ক্লিক করেন।
    2. ব্যাকএন্ডের `FailoverController` প্রক্সি টানেল সার্ভারে রিকোয়েস্ট পাঠায়।
    3. প্রক্সি আইপি ঘূর্ণন সম্পন্ন হয়ে নতুন সুরক্ষিত আইপি এড্রেস ও কান্ট্রি লোকেশন ড্যাশবোর্ডে ভেসে ওঠে।

---

## 💾 ৪. Database Emulator Backup Snapshots (`AdminBackup.tsx`)
*   **স্থিতি (Status):** **Partially Completed**
*   **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
    *   **কম্পোনেন্ট:** `dashboard/src/pages/AdminBackup.tsx`
    *   **ভিজুয়াল উইজেট:** ডাটাবেজ ব্যাকআপ হিস্টোরি গ্রিড, ব্যাকআপ ফাইল আপলোডার উইজেট এবং `Trigger Backup` ও `Restore` অ্যাকশন বাটন।
*   **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
    *   **কন্ট্রোলার:** `FirebaseEmulatorController.java` / `DataController.java`
    *   **প্রধান API রাউট:** `POST /api/backup/export` এবং `POST /api/backup/restore`
*   **কীভাবে কাজ করে (How it Works - Data Flow):**
    1. ইউজার `Trigger Backup` বাটনে চাপ দিলে ব্যাকএন্ডে রিকোয়েস্ট যায়।
    2. ব্যাকএন্ড ওএস শেলে `firebase emulators:export` রান করে সম্পূর্ণ ফায়ারবেস বা ফায়ারস্টোর ডাটাবেজের জিপ ফাইল তৈরি করে।
    3. জিপ ফাইলটির ডাউনলোডেবল লিংক এবং স্টোরেজ পাথ ড্যাশবোর্ডে রেন্ডার হয়।

---

## ⚙️ ৫. System Config & Preferences (`AdminSettings.tsx`)
*   **স্থিতি (Status):** **Fully Functional**
*   **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
    *   **কম্পোনেন্ট:** `dashboard/src/pages/AdminSettings.tsx`
    *   **ভিজুয়াল উইজেট:** সিস্টেম মোড সিলেকশন (অনলাইন বনাম সোলো মোড), প্রজেক্ট থিম ও ল্যাঙ্গুয়েজ প্রেফারেন্স সিলেকশন ড্রপডাউন এবং ডার্ক মোড টগল বাটন।
*   **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
    *   **কন্ট্রোলার:** `AdminConfigController.java` / `UserLanguagePreferenceController.java`
    *   **প্রধান API রাউট:** `POST /api/settings/update`
*   **কীভাবে কাজ করে (How it Works - Data Flow):**
    1. ইউজার তার মোড বা সেটিংস পরিবর্তন করে `Save Settings` বাটনে চাপ দেন।
    2. ব্যাকএন্ডের লোকাল কনফিগ বা ডাটাবেজে ইউজারের প্রিফারেন্স ডেটা আপডেট হয়ে যায়।
    3. ফ্রন্টএন্ড উইন্ডো স্বয়ংক্রিয়ভাবে রিফ্রেশ হয়ে নতুন কনফিগারেশন অনুযায়ী পুরো ইন্টারফেস লোড করে।
