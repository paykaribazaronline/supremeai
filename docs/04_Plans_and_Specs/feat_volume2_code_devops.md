# 📚 SupremeAI Feature Encyclopedia - Volume 2: Code, DevOps & Deployment

> **Status:** 🟢 Updated for v5 Architecture

> **[VOLUME STATUS: 100% OPERATIONAL]**  
> **[LANG: BENGALI]**

এই ভলিউমে সুপ্রিম এআই (SupremeAI) প্ল্যাটফর্মের **Code, DevOps এবং Deployment** বিষয়ক ৭টি ফিচারের পূর্ণাঙ্গ ফ্রন্টএন্ড ও ব্যাকএন্ড আর্কিটেকচার এবং কার্যপ্রণালী দেওয়া হলো।

---

## 🛠️ ১. Self-Healing Code Architecture (`AdminSelfHealing.tsx`)

- **স্থিতি (Status):** **Partially Completed**
- **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
  - **কম্পোনেন্ট:** `dashboard/src/pages/AdminSelfHealing.tsx`
  - **ভিজুয়াল উইজেট:** সক্রিয় কম্পাইলার বা বিল্ড এরর ডিসপ্লে, এআই হিলিং প্রোগ্রেস লুপ ট্র্যাকার এবং সেফ গিট রোলব্যাক বাটন।
- **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
  - **কন্ট্রোলার:** `AppGenerationController.java`
  - **প্রধান API রাউট:** `POST /api/generation/heal`
- **কীভাবে কাজ করে (How it Works - Data Flow):**
  1. কোনো ফাইলে এরর থাকলে ফ্রন্টএন্ড এরর লগ ব্যাকএন্ড হিলিং কন্ট্রোলারে পাঠায়।
  2. ব্যাকএন্ড লোকাল এআই (AirLLM) দিয়ে কোড ফিক্স জেনারেট করে সোর্স ফাইলে লেখে।
  3. ব্যাকএন্ড `./gradlew compileJava` বা এনপিএম বিল্ড রান করে কম্পাইলেশন চেক করে। বিল্ড পাস হলে কমিট করে, ফেইল হলে রোলব্যাক করে।

---

## 💻 ২. Interactive Codebase Explorer (`AdminCodeAnalysis.tsx`)

- **স্থিতি (Status):** **Partially Completed**
- **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
  - **কম্পোনেন্ট:** `dashboard/src/pages/AdminCodeAnalysis.tsx`
  - **ভিজুয়াল উইজেট:** প্রজেক্ট ডিরেক্টরির ফাইল স্ট্রাকচার ট্রি, সোর্স কোড রিডার ভিউপোর্ট এবং সিনট্যাক্স হাইলাইটার ইন্টারফেস।
- **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
  - **কন্ট্রোলার:** `analysis/AnalysisController.java` / `IdeAssistantController.java`
  - **প্রধান API রাউট:** `GET /api/analysis/files`
- **কীভাবে কাজ করে (How it Works - Data Flow):**
  1. ইউজার ফাইল স্ট্রাকচার ট্রি থেকে যেকোনো ফাইলে ক্লিক করেন।
  2. ফ্রন্টএন্ড এপিআই-এর মাধ্যমে ফাইল পাথ পাঠিয়ে ব্যাকএন্ড থেকে ফাইলের র টেক্সট ডাটা রিড করে নিয়ে আসে।
  3. ড্যাশবোর্ডে ফাইলটির কোড এবং এর পসিবল এরর বা বাগ ডাটা এনালাইসিস উইজেটে ভেসে ওঠে।

---

## 🔍 ৩. Reverse Engineer & DOM Topology Parser (`AdminReverseEngineer.tsx`)

- **স্থিতি (Status):** **Partially Completed**
- **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
  - **কম্পোনেন্ট:** `dashboard/src/pages/AdminReverseEngineer.tsx`
  - **ভিজুয়াল উইজেট:** যেকোনো রান হওয়া অ্যাপের ইউআই টপোলজি রিডার, বাটন ও ইনপুট এলিমেন্টের পজিশনিং পিক্সেল ম্যাপ এবং অ্যাক্সেসিবিলিটি নোড ভিউ।
- **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
  - **কন্ট্রোলার:** `analysis/AnalysisController.java`
  - **প্রধান API রাউট:** `POST /api/analysis/reverse-dom`
- **কীভাবে কাজ করে (How it Works - Data Flow):**
  1. ব্রাউজার কোনো পৃষ্ঠা লোড করলে ফ্রন্টএন্ড ডম রিভার্স ইঞ্জিনিয়ারিং ট্রিগার করে।
  2. ব্যাকএন্ড সম্পূর্ণ পৃষ্ঠার DOM এলিমেন্টগুলো ফিল্টার করে সেগুলোর ইন্টারেক্টিভ কঙ্কাল বা টপোলজি ম্যাপ রি-কনস্ট্রাক্ট করে।
  3. ড্যাশবোর্ডে অ্যাপটির বাটন ও ইনপুটের গ্রাফিক্যাল নোড স্ট্রাকচার রেন্ডার হয়।

---

## 🧪 ৪. API Endpoint Testing Suite (`AdminTesting.tsx`)

- **স্থিতি (Status):** **Partially Completed**
- **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
  - **কম্পোনেন্ট:** `dashboard/src/pages/AdminTesting.tsx`
  - **ভিজুয়াল উইজেট:** কাস্টম API রিকোয়েস্ট মেকার (GET/POST/PUT/DELETE), হেডার কনফিগারেটর এবং জেডিকে/স্প্রিং বুটের রানটাইম এরর ও রেসপন্স টাইমিং চার্ট।
- **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
  - **কন্ট্রোলার:** `ApiTestingController.java`
  - **প্রধান API রাউট:** `POST /api/testing/run-case`
- **কীভাবে কাজ করে (How it Works - Data Flow):**
  1. ইউজার কোনো এপিআই এর ডিটেইলস লিখে `RUN TEST` বাটনে চাপ দেন।
  2. ব্যাকএন্ডের টেস্ট রানার এপিআই থ্রেড থেকে টার্গেট এন্ডপয়েন্টে ভার্চুয়াল রিকোয়েস্ট পাঠায়।
  3. রেসপন্স হেডার, স্ট্যাটাস কোড (যেমন: 200 OK বা 500 Error) এবং রেসপন্স স্পিড সংগ্রহ করে ফ্রন্টএন্ডে রেন্ডার করে।

---

## 🚀 ৫. Automated Cloud Deployment (`AdminDeployment.tsx`)

- **স্থিতি (Status):** **Partially Completed**
- **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
  - **কম্পোনেন্ট:** `dashboard/src/pages/AdminDeployment.tsx`
  - **ভিজুয়াল উইজেট:** সক্রিয় ডিপ্লয়মেন্ট প্রজেক্টের লিস্ট, গুগল ক্লাউড বা ফায়ারবেস হোস্টিং ড্যাশবোর্ড এবং রিয়েল-টাইম ক্লাউড ডেপ্লয়মেন্ট প্রোগ্রেস বার।
- **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
  - **কন্ট্রোলার:** `DeploymentController.java`
  - **প্রধান API রাউট:** `POST /api/deployment/deploy`
- **কীভাবে কাজ করে (How it Works - Data Flow):**
  1. ইউজার `Deploy to Cloud` বাটনে চাপ দিলে ব্যাকএন্ডে ডিপ্লয়মেন্ট রিকোয়েস্ট ফায়ার হয়।
  2. ব্যাকএন্ড ওএস শেলে `gcloud` বা `firebase deploy` রান করে ক্লাউডে সোর্স কোড পাঠায়।
  3. সিডিএন লিঙ্ক বা ডেপ্লয়মেন্ট ইউআরএল জেনারেট হয়ে ড্যাশবোর্ডে ভেসে ওঠে।

---

## 📂 6. Multi-Project Repository Manager (`AdminProjects.tsx`)

- **স্থিতি (Status):** **Partially Completed**
- **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
  - **কম্পোনেন্ট:** `dashboard/src/pages/AdminProjects.tsx`
  - **ভিজুয়াল উইজেট:** বর্তমানে আমাদের সিস্টেমে থাকা সমস্ত প্রোজেক্টের লিস্ট, প্রোজেক্টের ডিরেক্টরি লোকেশন এবং ডিলিট/ক্রিয়েট অ্যাকশন গেটওয়ে।
- **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
  - **কন্ট্রোলার:** `RouterController.java` / `api-router.js`
  - **প্রধান API রাউট:** `GET /api/projects`
- **কীভাবে কাজ করে (How it Works - Data Flow):**
  1. ফ্রন্টএন্ড থেকে সব প্রোজেক্ট লিস্ট করার জন্য রিকোয়েস্ট যায়।
  2. ব্যাকএন্ডের ফায়ারবেস বা লোকাল স্টোরেজ থেকে সব প্রোজেক্টের ডিটেইলস রিড করে পাঠানো হয়।
  3. ড্যাশবোর্ডে প্রতিটি প্রোজেক্টের সাইজ, ব্রাঞ্চ এবং লাস্ট আপডেট টাইমস্ট্যাম্প দেখা যায়।

---

## 🐙 ৭. Git Multi-Origin Rotation Center (`AdminGitProjects.tsx`)

- **স্থিতি (Status):** **Partially Completed**
- **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
  - **কম্পোনেন্ট:** `dashboard/src/pages/AdminGitProjects.tsx`
  - **ভিজুয়াল উইজেট:** গিট অরিজিন চেঞ্জার কার্ড, ব্রাঞ্চ রোটেশন কিউ এবং রিমোট গিট কমিট ভিউয়ার।
- **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
  - **কন্ট্রোলার:** `CommandController.java`
  - **প্রধান API রাউট:** `POST /api/git/rotate`
- **কীভাবে কাজ করে (How it Works - Data Flow):**
  1. ইউজার যখন রিমোট গিট অরিজিন চেঞ্জ করেন, তখন ফ্রন্টএন্ড এপিআই কল করে।
  2. ব্যাকএন্ড শেলে `git remote set-url origin` রান করে রিমোট রিপোজিটরির সংযোগ পরিবর্তন করে।
  3. সিঙ্ক সাকসেস হলে নতুন ব্রাঞ্চের স্ট্যাটাস ড্যাশবোর্ডে আপডেট হয়ে যায়।
