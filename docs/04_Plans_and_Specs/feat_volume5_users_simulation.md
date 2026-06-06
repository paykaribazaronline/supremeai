# 📚 SupremeAI Feature Encyclopedia - Volume 5: Users, Simulation & Utility

> **Status:** 🟢 Updated for v5 Architecture

> **[VOLUME STATUS: 100% OPERATIONAL]**  
> **[LANG: BENGALI]**

এই ভলিউমে সুপ্রিম এআই (SupremeAI) প্ল্যাটফর্মের **Users, Simulation এবং Utility** বিষয়ক ৫টি ফিচারের পূর্ণাঙ্গ ফ্রন্টএন্ড ও ব্যাকএন্ড আর্কিটেকচার এবং কার্যপ্রণালী দেওয়া হলো।

---

## 👥 ১. Active User Accounts Database (`AdminUsers.tsx`)

- **স্থিতি (Status):** **Fully Functional**
- **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
  - **কম্পোনেন্ট:** `dashboard/src/pages/AdminUsers.tsx`
  - **ভিজুয়াল উইজেট:** সিস্টেমে সক্রিয় রেজিস্টার্ড ইউজারদের তালিকা টেবিল (Ant Design Table), ইউজারের স্ট্যাটাস টগল (Active/Banned) এবং প্রোফাইল ডিটেইলস পপআপ।
- **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
  - **কন্ট্রোলার:** `UserAccountController.java`
  - **প্রধান API রাউট:** `GET /api/admin/users`
- **কীভাবে কাজ করে (How it Works - Data Flow):**
  1. ইউজার ডেটা ভিউ পেজ লোড হওয়ার সাথে সাথে এপিআই রিকোয়েস্ট ব্যাকএন্ডে যায়।
  2. ব্যাকএন্ডের `UserAccountController` ফায়ারস্টোর ডাটাবেজ থেকে সব ব্যবহারকারীর মেম্বারশিপ এবং অ্যাকাউন্ট ইনফো নিয়ে জেসন রিটার্ন করে।
  3. ফ্রন্টএন্ডে এটি ফিল্টারিং এবং সার্চ ফিল্ডসহ একটি ডাইনামিক টেবিলে রেন্ডার হয়।

---

## 🔐 ২. Role Assignments & Access Controls (`AdminUserManagement.tsx`)

- **স্থিতি (Status):** **Fully Functional**
- **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
  - **কম্পোনেন্ট:** `dashboard/src/pages/AdminUserManagement.tsx`
  - **ভিজুয়াল উইজেট:** ইউজারের রোল (Admin, Developer, Guest) সিলেক্ট করার উইজেট, এক্সেস কন্ট্রোল চেকবক্স ম্যাট্রিক্স এবং সেভ বাটন।
- **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
  - **কন্ট্রোলার:** `UserAccountController.java`
  - **প্রধান API রাউট:** `POST /api/admin/users/roles`
- **কীভাবে কাজ করে (How it Works - Data Flow):**
  1. অ্যাডমিন কোনো ইউজারের রোল বা পারমিশন পরিবর্তন করে `Apply Role` বাটনে চাপ দেন।
  2. ব্যাকএন্ড ডাটাবেজে ইউজারের ফায়ারবেস প্রোফাইলের কাস্টম পারমিশন অবজেক্ট আপডেট করে।
  3. ইউজারের পরবর্তী এপিআই রিকোয়েস্টগুলোতে নতুন রোল অনুযায়ী এক্সেস প্রিভিলেজ অ্যাপ্লাই হয়।

---

## 📱 ৩. Visual App Simulator Runtime Preview (`AdminSimulator.tsx`)

- **স্থিতি (Status):** **Partially Completed**
- **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
  - **কম্পোনেন্ট:** `dashboard/src/pages/AdminSimulator.tsx` / `SimulatorPreview.tsx`
  - **ভিজুয়াল উইজেট:** তৈরি হওয়া অ্যাপ বা প্রোটোটাইপের লাইভ মোবাইল/ট্যাবলেট ভিউপোর্ট ফ্রেম, রিয়াল-টাইম ইন্টারেক্টিভ রিফ্রেশ বাটন এবং `Take Simulator Screenshot` বাটন।
- **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
  - **কন্ট্রোলার:** `SimulatorController.java` / `SimulatorRuntimeController.java`
  - **প্রধান API রাউট:** `GET /api/simulator/preview/{appId}/screenshot`
- **কীভাবে কাজ করে (How it Works - Data Flow):**
  1. এআই দ্বারা তৈরি করা অ্যাপটির লাইভ প্রিভিউ দেখতে ফ্রন্টএন্ড সিমুলেটর রিকোয়েস্ট পাঠায়।
  2. ব্যাকএন্ড সিমুলেটর রানটাইমে অ্যাপটি লোড করে এবং তার একটি রেন্ডার হওয়া ভিউপোর্ট জেনারেট করে।
  3. সিমুলেটরের লাইভ স্ক্রিন প্রিভিউটি মোবাইল ফ্রেমে রেন্ডার হয়, যেখানে ইউজার সরাসরি বাটন ও ইনপুট ট্রিগার করতে পারে।

---

## ⏳ ৪. Active Session Tracing & Live Activity (`AdminLiveActivity.tsx`)

- **স্থিতি (Status):** **Partially Completed**
- **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
  - **কম্পোনেন্ট:** `dashboard/src/pages/AdminLiveActivity.tsx`
  - **ভিজুয়াল উইজেট:** বর্তমানে প্ল্যাটফর্মে কতজন ইউজার বা এআই এজেন্ট সক্রিয় সেশনে কাজ করছে তার রিয়েল-টাইম পাই চার্ট, সেশন টাইমলাইন স্ট্রিম এবং অ্যাক্টিভিটি ডিটেইলস প্যানেল।
- **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
  - **কন্ট্রোলার:** `com.supremeai.controller.ChatController.java`
  - **প্রধান API রাউট:** `GET /api/admin/chat/actions/pending`
- **কীভাবে কাজ করে (How it Works - Data Flow):**
  1. ফ্রন্টএন্ড সেশন ট্র্যাকিং প্যানেল প্রতি ৩ সেকেন্ডে পোল করে ব্যাকএন্ডে সেশন ডাটা চায়।
  2. ব্যাকএন্ডের সেশন ইন্টারসেপ্টর সক্রিয় ফায়ারবেস সেশন টোকেন ও চ্যাট অ্যাক্টিভিটি ট্র্যাক করে।
  3. লাইভ সেশন ম্যাপ এবং সক্রিয় এজেন্টগুলোর কাউন্ট ড্যাশবোর্ডে ভেসে ওঠে।

---

## 🔔 ৫. System Alerts & Notifications (`AdminSystemAlerts.tsx`)

- **স্থিতি (Status):** **Fully Functional**
- **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
  - **কম্পোনেন্ট:** `dashboard/src/pages/AdminSystemAlerts.tsx`
  - **ভিজুয়াল উইজেট:** সিস্টেমের কোনো সার্ভিস ডাউন হলে বা রেট লিমিট ক্রস করলে লাল/হলুদ অ্যালার্ট নোটিফিকেশন ব্যানার, সার্ভিস রিসেট বাটন এবং অ্যালার্ট হিস্টোরি প্যানেল।
- **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
  - **কন্ট্রোলার:** `MonitoringController.java` / `PubSubWebhookController.java`
  - **প্রধান API রাউট:** `GET /api/system/alerts`
- **কীভাবে কাজ করে (How it Works - Data Flow):**
  1. ব্যাকএন্ডে কোনো এরর বা সার্ভিস ফেইলিওর ঘটলে এটি সাথে সাথে অ্যালার্ট ইভেন্ট জেনারেট করে ফায়ারবেসে লেখে।
  2. ফ্রন্টএন্ড পুশ নোটিফিকেশন লুপের মাধ্যমে ডাটাবেজ থেকে অ্যালার্ট লিস্ট পোল করে।
  3. ড্যাশবোর্ডে রিয়েল-টাইমে লাল বা হলুদ অ্যালার্ট পপআপ ব্যানার রেন্ডার হয়, যা দেখে অ্যাডমিন প্রয়োজনীয় সার্ভিস রিসেট অ্যাকশন নিতে পারে।
