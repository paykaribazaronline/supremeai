# 📚 SupremeAI Feature Encyclopedia - Volume 4: Telemetry & Monitoring

> **[VOLUME STATUS: 100% OPERATIONAL]**  
> **[LANG: BENGALI]**  

এই ভলিউমে সুপ্রিম এআই (SupremeAI) প্ল্যাটফর্মের **Telemetry এবং Infrastructure Monitoring** বিষয়ক ৭টি ফিচারের পূর্ণাঙ্গ ফ্রন্টএন্ড ও ব্যাকএন্ড আর্কিটেকচার এবং কার্যপ্রণালী দেওয়া হলো।

---

## 📊 ১. Telemetry Analytics Dashboard (`AdminAnalytics.tsx`)
*   **স্থিতি (Status):** **Partially Completed**
*   **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
    *   **কম্পোনেন্ট:** `dashboard/src/pages/AdminAnalytics.tsx`
    *   **ভিজুয়াল উইজেট:** সিপিইউ, র‍্যাম এবং ব্রাউজার ক্রলিংয়ের পারফরম্যান্স দেখানোর জন্য সুন্দর লাইন এবং এরিয়া চার্ট (Ant Design Charts)।
*   **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
    *   **কন্ট্রোলার:** `SystemMetricsController.java`
    *   **প্রধান API রাউট:** `GET /api/system/metrics`
*   **কীভাবে কাজ করে (How it Works - Data Flow):**
    1. প্রতি ২ সেকেন্ড পর পর ফ্রন্টএন্ড সিপিইউ ও মেমোরি ডেটা পোল করে।
    2. ব্যাকএন্ডের `SystemMetricsController` ওএস শেল বা জেভিএম (JVM) রিডিং নিয়ে মেট্রিক্স হিস্টোরি ডেটা রিড করে।
    3. মেট্রিক্স জেসনটি ফ্রন্টএন্ডে যাওয়ার পর চার্টগুলো ডাইনামিক অ্যানিমেশনের মাধ্যমে লাইভ রেন্ডার হয়।

---

## 🖥️ ২. JVM & Server Infrastructure Indicator (`AdminInfrastructure.tsx`)
*   **স্থিতি (Status):** **Partially Completed**
*   **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
    *   **কম্পোনেন্ট:** `dashboard/src/pages/AdminInfrastructure.tsx`
    *   **ভিজুয়াল উইজেট:** সার্ভার ডিরেক্টরি স্পেস মিটার, সক্রিয় থ্রেড কাউন্ট প্রোগ্রেস বার এবং JVM মেমোরি কনসাম্পশন গেজ (Gauge Chart)।
*   **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
    *   **কন্ট্রোলার:** `AdminInfrastructureController.java`
    *   **প্রধান API রাউট:** `GET /api/infrastructure/health`
*   **কীভাবে কাজ করে (How it Works - Data Flow):**
    1. ড্যাশবোর্ড লোড হওয়ার সময় ইনফ্রাস্ট্রাকচার মেট্রিক্সের জন্য এপিআই রিকোয়েস্ট ফায়ার হয়।
    2. ব্যাকএন্ড `Runtime.getRuntime()` ব্যবহার করে মেমোরি পুল ও মেমোরি কালেকশন হিস্টোরি স্ক্যান করে।
    3. তথ্যগুলো প্রোগ্রেস বার ও গেজ চার্ট আকারে ড্যাশবোর্ডে ভেসে ওঠে।

---

## ⏱️ ৩. API Route & Engine Performance (`AdminPerformance.tsx`)
*   **স্থিতি (Status):** **Partially Completed**
*   **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
    *   **কম্পোনেন্ট:** `dashboard/src/pages/AdminPerformance.tsx`
    *   **ভিজুয়াল উইজেট:** প্রতিটি এক্সপ্রেস রাউট ও জাভা কন্ট্রোলারের ল্যাটেন্সি পিং গ্রাফ, ব্যাকগ্রাউন্ড থ্রেড পারফরম্যান্স এবং ডেটা ডাটাবেজ রেসপন্স স্পিড বার।
*   **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
    *   **কন্ট্রোলার:** `SystemMetricsController.java`
    *   **প্রধান API রাউট:** `GET /api/system/performance`
*   **কীভাবে কাজ করে (How it Works - Data Flow):**
    1. ফ্রন্টএন্ড ল্যাটেন্সি ডাটা পোল করার জন্য এপিআই রিকোয়েস্ট পাঠায়।
    2. ব্যাকএন্ড থ্রেড ইন্টারসেপ্টর প্রতি এপিআই কলের প্রসেসিং স্পিড ও ল্যাটেন্সি মিলি-সেকেন্ড ট্র্যাক করে।
    3. তথ্যগুলো ল্যাটেন্সি পিং গ্রাফে আপডেট হয়ে যায়।

---

## 📻 ৪. Live Action & Logging Streams (`AdminMonitoring.tsx`)
*   **স্থিতি (Status):** **Partially Completed**
*   **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
    *   **কম্পোনেন্ট:** `dashboard/src/pages/AdminMonitoring.tsx`
    *   **ভিজুয়াল উইজেট:** এআই এজেন্টগুলোর "Thought Stream" কনসোল, সক্রিয় এক্সেপশন ও রানটাইম অ্যালার্ট ডিসপ্লে।
*   **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
    *   **কন্ট্রোলার:** `MonitoringController.java`
    *   **প্রধান API রাউট:** `GET /api/monitoring/events`
*   **কীভাবে কাজ করে (How it Works - Data Flow):**
    1. ফ্রন্টএন্ড পেজটি ব্যাকএন্ডের ইভেন্ট লগের এপিআই রিকোয়েস্ট পাঠায়।
    2. ব্যাকএন্ডের `MonitoringController` লাইভ রানটাইম ইভেন্ট এবং এক্সেপশন লগের তালিকা জেসন আকারে রিটার্ন করে।
    3. ড্যাশবোর্ডের টার্মিনাল এরিয়াতে ইভেন্টগুলো রিয়েল-টাইমে রেন্ডার হয়।

---

## 📂 ৫. API Router Logs Archive (`AdminLogs.tsx`)
*   **স্থিতি (Status):** **Partially Completed**
*   **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
    *   **কম্পোনেন্ট:** `dashboard/src/pages/AdminLogs.tsx`
    *   **ভিজুয়াল উইজেট:** এক্সপ্রেস গেটওয়ে সার্ভারের এপিআই রাউটিং লগ এবং ক্লাউড ফাংশনের এরর লক টার্মিনাল উইন্ডো।
*   **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
    *   **কন্ট্রোলার:** `functions/api-router.js` (Express stub fallback)
    *   **প্রধান API রাউট:** `GET /api/admin/logs`
*   **কীভাবে কাজ করে (How it Works - Data Flow):**
    1. ইউজার লগ ফাইল ডক দেখতে চাপ দিলে ফ্রন্টএন্ড এক্সপ্রেস রাউটারে রিকোয়েস্ট পাঠায়।
    2. ব্যাকএন্ড `api-router.js` সার্ভারের ফাইল সিস্টেম থেকে রাউটিং এরর ফাইল রিড করে।
    3. সম্পূর্ণ এরর লকটি ড্যাশবোর্ডের টেক্সট ভিউয়ারে চলে আসে।

---

## ⏳ ৬. User Quotas & Rate Limits Ledger (`AdminQuotas.tsx`)
*   **স্থিতি (Status):** **Partially Completed**
*   **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
    *   **কম্পোনেন্ট:** `dashboard/src/pages/AdminQuotas.tsx`
    *   **ভিজুয়াল উইজেট:** ইউজারের মোট টোকেন ব্যবহার ট্র্যাকিং মিটার, রেট লিমিট প্রোগ্রেস গেজ এবং কাস্টম কোটা কনফিগারেশন ড্রপডাউন।
*   **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
    *   **কন্ট্রোলার:** `AdminQuotaController.java` / `UsageOptimizationController.java`
    *   **প্রধান API রাউট:** `GET /api/admin/quotas`
*   **কীভাবে কাজ করে (How it Works - Data Flow):**
    1. ড্যাশবোর্ডে ইউজারের মেম্বারশিপ টোকেন ডাটা পোলিং চলে।
    2. ব্যাকএন্ড ইউজারের মোট চ্যাট কোটা ব্যবহার ও লিমিট ক্যালকুলেট করে।
    3. তথ্যগুলো ইউজারের চ্যাট উইন্ডো ও কোটা মেট্রিক মিটারে রেন্ডার হয়।

---

## 📄 ৭. Cost Auditing & AI Reports (`AdminReports.tsx`)
*   **স্থিতি (Status):** **Partially Completed**
*   **ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI):**
    *   **কম্পোনেন্ট:** `dashboard/src/pages/AdminReports.tsx`
    *   **ভিজুয়াল উইজেট:** এআই এপিআই ব্যবহারের কস্ট চার্ট, মডেল এক্যুরেসি ও ফেইথফুলনেস ইনডেক্স কার্ড এবং ডাউনলোডেবল PDF রিপোর্ট জেনারেটর বাটন।
*   **ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API):**
    *   **কন্ট্রোলার:** `CostTransparencyController.java` / `AIBehaviorProfileController.java`
    *   **প্রধান API রাউট:** `GET /api/reports/cost-summary`
*   **কীভাবে কাজ করে (How it Works - Data Flow):**
    1. ইউজার `Generate Report` বাটনে ক্লিক করলে এপিআই রিকোয়েস্ট যায়।
    2. ব্যাকএন্ড এপিআই ট্র্যাকার এবং মডেল ব্যবহারের কস্ট হিস্টোরি ডেটা প্রসেস করে।
    3. কস্ট চার্ট ও এক্যুরেসি পিডিএফ রিপোর্ট জেনারেট হয়ে ড্যাশবোর্ডে ভেসে ওঠে।
