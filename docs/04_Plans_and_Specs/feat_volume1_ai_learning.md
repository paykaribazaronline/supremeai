# 📚 SupremeAI Feature Encyclopedia - Volume 1: AI, Browser & Learning

> **Status:** 🟢 Updated for v5 Architecture

> **[VOLUME STATUS: 100% OPERATIONAL]**  
> **[LANG: BENGALI]**

এই ভলিউমে সুপ্রিম এআই (SupremeAI) প্ল্যাটফর্মের **AI, Browser এবং Autodidact Learning** বিষয়ক ৫টি কোর ফিচারের পূর্ণাঙ্গ ফ্রন্টএন্ড ও ব্যাকএন্ড আর্কিটেকচার এবং কার্যপ্রণালী দেওয়া হলো।

---

## 🤖 ১. AI Providers Management (`AdminProviders.tsx`)

- **স্থিতি (Status):** **Fully Functional**
- **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
  - **কম্পোনেন্ট:** `dashboard/src/pages/AdminProviders.tsx`
  - **ভিজুয়াল উইজেট:** এটি কনফিগার করা সমস্ত প্রোভাইডার (Firebase, Vertex AI, Local AirLLM) গ্রিড আকারে দেখায়। এর সাথে প্রতিটি প্রোভাইডারের পিং স্পিড ও মডেল সিলেকশন ড্রপডাউন ইন্টারফেস রয়েছে।
- **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
  - **কন্ট্রোলার:** `RouterController.java` / `api-router.js`
  - **প্রধান API রাউট:** `GET /api/admin/providers/configured`
- **কীভাবে কাজ করে (How it Works - Data Flow):**
  1. ইউজার প্রোভাইডারের পেজ খুললে ফ্রন্টএন্ড এপিআই রিকোয়েস্ট পাঠায়।
  2. ব্যাকএন্ড ডাটাবেজ (Firestore) থেকে কনফিগারড মডেলের ডাটা পড়ে ফ্রন্টএন্ডে পাঠায়।
  3. কোনো কাস্টম এআই মডেল সিলেক্ট করা হলে তা সরাসরি `/api/chat/send` রাউটে ডিফল্ট গেটওয়ে হিসেবে সেট হয়ে যায়।

---

## 🧠 ২. AI Multi-Agent Orchestration (`AdminAIOrchestration.tsx`)

- **স্থিতি (Status):** **Partially Completed**
- **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
  - **কম্পোনেন্ট:** `dashboard/src/pages/AdminAIOrchestration.tsx`
  - **ভিজুয়াল উইজেট:** একাধিক এআই এজেন্ট (যেমন: Llama, GPT, Gemini) একই কাজের জন্য কীভাবে "Consensus" বা ঐক্যমতে পৌঁছায়, তার চাক্ষুষ পাই-চার্ট এবং লাইভ এজেন্ট অ্যাক্টিভিটি স্ট্রিম।
- **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
  - **কন্ট্রোলার:** `MultiAIConsensusController.java` / `AIAgentsController.java`
  - **প্রধান API রাউট:** `POST /api/orchestration/consensus`
- **কীভাবে কাজ করে (How it Works - Data Flow):**
  1. এআই এজেন্টের কাছে কোনো কোড বা কাজ আসলে ফ্রন্টএন্ড অর্কেস্ট্রেশন এপিআই হিট করে।
  2. ব্যাকএন্ডের `MultiAIConsensusController` সমান্তরালভাবে ৩টি এআই মডেলে রিকোয়েস্ট ফায়ার করে।
  3. প্রাপ্ত ৩টি উত্তরের কনফিডেন্স স্কোর ক্যালকুলেট করে সর্বোচ্চ ভোটের ভিত্তিতে সঠিক উত্তরটি চ্যাটে রেন্ডার করে।

---

## 🌐 ৩. Autonomous Surfing & Web Browser (`AdminBrowser.tsx` / `AutoBrowser.tsx`)

- **স্থিতি (Status):** **Fully Functional**
- **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
  - **কম্পোনেন্ট:** `dashboard/src/pages/AdminBrowser.tsx` এবং `AutoBrowser.tsx`
  - **ভিজুয়াল উইজেট:** প্লে-রাইট হেডলেস ব্রাউজার থেকে জেনারেট হওয়া লাইভ Base64 ইমেজ প্রিভিউ প্যানেল, মাউস কোঅর্ডিনেট ট্র্যাকার এবং ডিরেক্ট টাইপিং ইনপুট বক্স।
- **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
  - **কন্ট্রোলার:** `BrowserController.java` / `functions/src/scrapeEngine.ts`
  - **প্রধান API রাউট:**
    - `GET /api/browser/surf/screenshot`
    - `POST /api/browser/surf/click-at`
- **কীভাবে কাজ করে (How it Works - Data Flow):**
  1. প্রতি ১.৫ সেকেন্ডে ফ্রন্টএন্ড স্ক্রিনশট রিকোয়েস্ট পাঠায়।
  2. ব্যাকএন্ড প্লে-রাইটের মাধ্যমে পেজ রেন্ডার করে স্ক্রিনশটের Base64 রেসপন্স দেয়।
  3. প্রিভিউতে ক্লিক করা হলে তা `1280x720` রেশিওতে কনভার্ট হয়ে হেডলেস ব্রাউজারে মাউস ক্লিক ট্রিগার করে।

---

## 🎓 ৪. System Autodidact Learning (`AdminLearning.tsx`)

- **স্থিতি (Status):** **Partially Completed**
- **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
  - **কম্পোনেন্ট:** `dashboard/src/pages/AdminLearning.tsx`
  - **ভিজুয়াল উইজেট:** লার্নিং মোড সক্রিয় করার টগল বাটন, ব্রাউজার দ্বারা সংগৃহীত মোট প্যাটার্ন কাউন্টের মেট্রিক বার এবং ডাইনামিক স্ক্রিপ্ট অটোমেশন কনসোল।
- **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
  - **কন্ট্রোলার:** `EnhancedLearningController.java` / `src/main/resources/core_knowledge_learned.json`
  - **প্রধান API রাউট:** `GET /api/browser/system-learning`
- **কীভাবে কাজ করে (How it Works - Data Flow):**
  1. ব্রাউজার কোনো নতুন সাইট স্ক্র্যাপ করলে সংগৃহীত ডেটা `EnhancedLearningController` এ পাঠানো হয়।
  2. ব্যাকএন্ড সেই ডেটা থেকে গুরুত্বপূর্ণ লজিক ও সোর্স লিঙ্ক নিষ্কাশন করে `core_knowledge_learned.json` ফাইলে সংরক্ষণ করে।
  3. ফ্রন্টএন্ড এই ফাইল থেকে মোট শেখা বিষয়গুলোর ড্যাশবোর্ড আপডেট দেখায়।

---

## 🗳️ ৫. Action Approvals Gateway (`AdminApprovals.tsx`)

- **স্থিতি (Status):** **Partially Completed**
- **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
  - **কম্পোনেন্ট:** `dashboard/src/pages/AdminApprovals.tsx`
  - **ভিজুয়াল উইজেট:** এআই এজেন্ট কোনো ফাইলে মারাত্মক কোড চেঞ্জ বা গিট পুশ করতে চাইলে তা ইউজারের অনুমতির অপেক্ষায় রাখার এপ্রুভাল গেটওয়ে কার্ড।
- **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
  - **কন্ট্রোলার:** `VotingController.java`
  - **প্রধান API রাউট:** `POST /api/approvals/vote`
- **কীভাবে কাজ করে (How it Works - Data Flow):**
  1. এআই কোনো গুরুত্বপূর্ণ অ্যাকশন নিলে ব্যাকএন্ড সেটিকে `Pending` স্টেটে ফায়ারবেসে রেকর্ড করে এবং ফ্লো হোল্ড করে।
  2. ফ্রন্টএন্ড এপ্রুভাল পেজ রিফ্রেশ করে সেই পেন্ডিং কার্ডটি ইউজারকে দেখায়।
  3. ইউজার `Approve` বা `Reject` বাটনে চাপ দিলে ব্যাকএন্ড থ্রেডটি রিলিজ হয় এবং কোড এক্সিকিউশন সামনে এগিয়ে যায়।
