# 🤖 SupremeAI অ্যাডমিন প্যানেল — সম্পূর্ণ বাংলা ইউজার গাইড

> **সংস্করণ:** 1.0 | **তারিখ:** এপ্রিল ২০২৬  
> এই গাইডটি যেকোনো নতুন অ্যাডমিনের জন্য তৈরি — ধাপে ধাপে সব কাজ শিখুন।

---

## 📋 সূচিপত্র

1. [প্রথমবার লগইন](#১-প্রথমবার-লগইন)
2. [ড্যাশবোর্ড (Dashboard)](#২-ড্যাশবোর্ড)
3. [API কী ম্যানেজার](#৩-api-কী-ম্যানেজার)
4. [Provider Coverage](#৪-provider-coverage)
5. [অ্যাপ্রুভাল (Approvals)](#৫-অ্যাপ্রুভাল)
6. [কমান্ড কনসোল (Commands)](#৬-কমান্ড-কনসোল)
7. [AI এজেন্ট অ্যাসাইনমেন্ট](#৭-ai-এজেন্ট-অ্যাসাইনমেন্ট)
8. [প্রজেক্ট ম্যানেজমেন্ট (Projects)](#৮-প্রজেক্ট-ম্যানেজমেন্ট)
9. [Git প্রজেক্ট (Auto AI Development)](#৯-git-প্রজেক্ট)
10. [ব্যবহারকারী ব্যবস্থাপনা (User Management)](#১০-ব্যবহারকারী-ব্যবস্থাপনা)
11. [টিয়ার ও কোটা (Tiers & Quotas)](#১১-টিয়ার-ও-কোটা)
12. [ডিপ্লয়মেন্ট (Deployment)](#১২-ডিপ্লয়মেন্ট)
13. [সিস্টেম লার্নিং (Auto-Learning)](#১৩-সিস্টেম-লার্নিং)
14. [বিদ্যমান অ্যাপ উন্নত করুন (Improve Existing App)](#১৪-বিদ্যমান-অ্যাপ-উন্নত-করুন)
15. [AI Orchestration (Kimi-K2)](#১৫-ai-orchestration)
16. [SupremeAI-এর সাথে চ্যাট](#১৬-supremeai-এর-সাথে-চ্যাট)
17. [মেট্রিক্স ও অ্যালার্ট](#১৭-মেট্রিক্স-ও-অ্যালার্ট)
18. [ডিসিশন টাইমলাইন](#১৮-ডিসিশন-টাইমলাইন)
19. [নোটিফিকেশন](#১৯-নোটিফিকেশন)
20. [অডিট লগ](#২০-অডিট-লগ)
21. [সেটিংস](#২১-সেটিংস)
22. [VPN ম্যানেজমেন্ট](#২২-vpn-ম্যানেজমেন্ট)
23. [সাধারণ সমস্যা ও সমাধান](#২৩-সাধারণ-সমস্যা-ও-সমাধান)

---

## ১. প্রথমবার লগইন

### কীভাবে লগইন করবেন

1. ব্রাউজারে `https://আপনার-সার্ভার/admin.html` খুলুন।
2. Firebase Authentication ব্যবহার করে লগইন করুন (ইমেইল + পাসওয়ার্ড)।
3. সফলভাবে লগইন হলে অ্যাডমিন ড্যাশবোর্ড দেখাবে।

> ⚠️ **গুরুত্বপূর্ণ:** শুধুমাত্র অ্যাডমিন রোলে থাকা অ্যাকাউন্ট এই প্যানেলে ঢুকতে পারবে।

### সাইডবার নেভিগেশন

বাম পাশে সাইডবার থেকে যেকোনো সেকশনে ক্লিক করুন। মোবাইলে হ্যামবার্গার মেনু (☰) ক্লিক করলে সাইডবার খুলবে।

---

## ২. ড্যাশবোর্ড

**সাইডবার → Dashboard (📊)**

ড্যাশবোর্ড হলো সিস্টেমের সার্বিক অবস্থার একটি চিত্র। এখানে দেখতে পাবেন:

| কার্ড | মানে |
|-------|------|
| **Pending Approvals** | অপেক্ষমান অনুমোদন সংখ্যা |
| **Active Agents** | চলমান AI এজেন্ট সংখ্যা |
| **System Load** | সার্ভারের উপর চাপ |
| **Active Projects** | চলমান প্রজেক্ট |
| **Completed Projects** | সম্পন্ন প্রজেক্ট |
| **Success Rate** | সফলতার হার |

### System Health (সিস্টেম স্বাস্থ্য)

- 🟢 **HEALTHY** — সব ঠিক আছে
- 🟡 **WARNING** — কিছু সমস্যা আছে, নজর রাখুন
- 🔴 **CRITICAL** — জরুরি পদক্ষেপ নিন

### Operational Techniques Panel

- **Refresh Summary** বাটন চাপলে AI-এর শেখা নিয়মগুলো দেখাবে।
- ক্যাটাগরি ফিল্টার দিয়ে নির্দিষ্ট ধরনের নিয়ম দেখুন।

### Provider Level Coverage (ড্যাশবোর্ডে)

- **Refresh Coverage** বাটন চাপলে প্রতিটি AI Provider-এর L1-L4 কভারেজ টেবিল দেখাবে।

---

## ৩. API কী ম্যানেজার

**সাইডবার → 🔑 API Key Manager**

এটি সবচেয়ে গুরুত্বপূর্ণ সেকশন। এখানে AI Provider-এর API Key যোগ করুন। **যতটি Provider-এর Key যোগ করবেন, সিস্টেম শুধু সেগুলোই ব্যবহার করবে।**

### নতুন API Key যোগ করা

1. **"Add New API Key"** বাটনে ক্লিক করুন।
2. ফর্মে পূরণ করুন:
   - **Provider/Model Name** — যেমন: `groq`, `openai`, `anthropic`
   - **API Key** — Provider-এর ওয়েবসাইট থেকে সংগ্রহ করা key
   - **Base URL / Endpoint** — API-এর ঠিকানা (সাধারণত স্বয়ংক্রিয়ভাবে পূরণ হয়)
   - **Base Model** — ব্যবহার করতে চাওয়া মডেল (যেমন: `llama-3.3-70b-versatile`)
   - **Rate Limit** — প্রতি মিনিটে সর্বোচ্চ কতটি request
3. **"Save"** বাটন চাপুন।

### API Key টেস্ট করা

1. সেভ করার পর টেবিলে Provider দেখাবে।
2. **"Test"** বাটন চাপুন।
3. সবুজ ✅ আসলে Key সঠিক, লাল ❌ আসলে Key ভুল।

### API Key এডিট বা মুছে ফেলা

- **Edit** — Key বা সেটিং পরিবর্তন করুন।
- **Delete** — Provider সরিয়ে দিন।
- **Rotate** — পুরনো key বাতিল করে নতুন key দিন।

### স্ট্যাটাস কী মানে

| স্ট্যাটাস | মানে |
|-----------|------|
| 🟢 Active | Key কাজ করছে |
| 🟡 Warning | Rate limit-এর কাছাকাছি |
| 🔴 Failed | Key কাজ করছে না |

### AI Fallback Chain (ব্যাকআপ Provider ক্রম)

যদি প্রধান Provider ব্যর্থ হয়, সিস্টেম স্বয়ংক্রিয়ভাবে পরের Provider ব্যবহার করবে।

1. **Fallback Chain** বক্সে Provider ID গুলো কমা দিয়ে লিখুন।  
   উদাহরণ: `groq, deepseek, anthropic-claude`
2. **"Save Fallback Chain"** বাটন চাপুন।
3. সব Active Provider ব্যবহার করতে চাইলে **"Clear"** চাপুন।

> 💡 **টিপস:** সবচেয়ে সস্তা এবং দ্রুত Provider প্রথমে রাখুন।

---

## ৪. Provider Coverage

**সাইডবার → 📡 Provider Coverage**

এই সেকশনে দেখুন কোন AI Provider কতটা প্রস্তুত।

### Coverage লোড করা

1. **"Load Coverage"** বাটন চাপুন।
2. টেবিলে প্রতিটি Provider দেখাবে।

### কলামগুলোর মানে

| কলাম | মানে |
|------|------|
| **Provider** | AI Provider-এর নাম |
| **Canonical** | সিস্টেমের ভেতরে ব্যবহৃত কোড-নাম |
| **Connector** | Native = সরাসরি সংযোগ আছে |
| **Configured** | ✅ = API Key দেওয়া আছে, ⚠️ Missing Key = Key নেই |
| **L1 Ready** | জ্ঞান-ভিত্তি (Knowledge Seed) আছে কিনা |
| **L2 Ready** | AI কতটা pattern শিখেছে |
| **L3 Ready** | Reasoning Chain কপি করা আছে কিনা |
| **L4 Ready** | সরাসরি সংযোগ চালু আছে কিনা |

> ⚠️ **"Missing Key"** মানে ওই Provider-এর জন্য API Key যোগ করা হয়নি।  
> **সমাধান:** API Key Manager-এ গিয়ে ওই Provider-এর Key যোগ করুন।

> ℹ️ **নোট:** শুধুমাত্র API Key Manager-এ যোগ করা Provider-ই এখানে দেখাবে।

---

## ৫. অ্যাপ্রুভাল

**সাইডবার → ✅ Approvals**

AI যখন কোনো কাজ করার আগে অনুমতি চায়, তখন এখানে request আসে।

### অ্যাপ্রুভাল দেওয়া

1. টেবিলে pending request গুলো দেখুন।
2. বিবরণ পড়ুন।
3. **"Approve"** বা **"Reject"** বাটন চাপুন।

> 💡 **কখন Approve দেবেন?** কাজটি নিরাপদ এবং আপনার পরিকল্পনা অনুযায়ী হলে।

---

## ৬. কমান্ড কনসোল

**সাইডবার → 💻 Commands**

সরাসরি সিস্টেমকে কমান্ড দিন।

### Quick Command বাটনগুলো

| বাটন | কাজ |
|------|-----|
| **System Status** | সার্বিক অবস্থা দেখুন |
| **Git Status** | Git repository-র অবস্থা দেখুন |
| **Quota Summary** | API কোটা কতটা ব্যবহার হয়েছে দেখুন |
| **Work History** | AI এজেন্ট কী কী করেছে দেখুন |
| **Metrics** | পারফরম্যান্স মেট্রিক্স দেখুন |
| **Clear** | কনসোল পরিষ্কার করুন |
| **🚨 Emergency Stop** | সব AI কার্যক্রম তাৎক্ষণিক বন্ধ করুন |

### কাস্টম কমান্ড দেওয়া

1. নিচের ইনপুট বক্সে কমান্ড টাইপ করুন।
2. Enter চাপুন বা **"Run"** বাটন চাপুন।

> ⚠️ **Emergency Stop** শুধুমাত্র জরুরি অবস্থায় ব্যবহার করুন।

---

## ৭. AI এজেন্ট অ্যাসাইনমেন্ট

**সাইডবার → 🤖 AI Agent Assignment**

কোন AI এজেন্ট কোন কাজ করছে তা দেখুন এবং নতুন কাজ বরাদ্দ করুন।

### নতুন এজেন্ট অ্যাসাইন করা

1. **"Assign New AI Agent"** বাটনে ক্লিক করুন।
2. ফর্মে পূরণ করুন:
   - **Agent Name** — এজেন্টের নাম দিন
   - **Task Type** — কাজের ধরন (Code Generation, Bug Fix, ইত্যাদি)
   - **Priority** — HIGH, MEDIUM, LOW
3. **"Save"** চাপুন।

### অ্যাসাইনমেন্ট টেবিলের কলাম

| কলাম | মানে |
|------|------|
| **Agent** | এজেন্টের নাম |
| **Task Type** | কাজের ধরন |
| **Priority** | অগ্রাধিকার মাত্রা |
| **Status** | Running/Idle/Failed |
| **Progress** | কাজ কতটুকু শেষ হয়েছে |

---

## ৮. প্রজেক্ট ম্যানেজমেন্ট

**সাইডবার → 📁 Projects**

AI দিয়ে নতুন অ্যাপ তৈরি করুন।

### নতুন প্রজেক্ট তৈরি

1. **"Create New Project"** বাটনে ক্লিক করুন।
2. ফর্মে পূরণ করুন:

| ফিল্ড | কী দিতে হবে |
|-------|-------------|
| **Project ID** | একটি ইউনিক আইডি (ছোট হাতে, কোনো স্পেস নেই) |
| **Template Type** | React, Spring Boot, Flutter, Full Stack, REST API |
| **Description** | অ্যাপটি কী করবে তা বাংলায় বা ইংরেজিতে লিখুন |
| **Features** | কী কী ফিচার চান (একটি করে লাইনে) |
| **GitHub Repo URL** | আপনার GitHub repository-র লিংক |
| **Branch** | branch-এর নাম (সাধারণত `main`) |
| **GitHub Token** | Private repo হলে Personal Access Token দিন |

3. **"Create Project"** বাটন চাপুন।

### প্রজেক্ট স্ট্যাটাস

| স্ট্যাটাস | মানে |
|-----------|------|
| 🔵 **Pending** | অপেক্ষায় আছে |
| 🟡 **In Progress** | AI কাজ করছে |
| 🟢 **Completed** | সম্পন্ন |
| 🔴 **Failed** | ব্যর্থ হয়েছে |

### প্রজেক্টের উপর কাজ

- **View** — প্রজেক্টের বিস্তারিত দেখুন
- **Edit** — তথ্য পরিবর্তন করুন
- **Delete** — প্রজেক্ট মুছে দিন
- **Deploy** — প্রজেক্ট Deploy করুন

---

## ৯. Git প্রজেক্ট

**সাইডবার → ⚙️ Git Projects**

AI স্বয়ংক্রিয়ভাবে Git repository-তে কোড লিখে push করে।

### মূল বাটনগুলো

| বাটন | কাজ |
|------|-----|
| **New Git Project** | নতুন Auto-Development প্রজেক্ট শুরু করুন |
| **Git Status** | বর্তমান repository অবস্থা দেখুন |
| **Recent Commits** | সর্বশেষ কমিটগুলো দেখুন |

### Recent Commits টেবিল

| কলাম | মানে |
|------|------|
| **Hash** | কমিটের ইউনিক কোড |
| **Message** | কমিটের বার্তা |
| **Author** | কে করেছে |
| **Date** | কখন করেছে |

---

## ১০. ব্যবহারকারী ব্যবস্থাপনা

**সাইডবার → 👥 User Management**

নতুন ব্যবহারকারী যোগ করুন এবং বিদ্যমান ব্যবহারকারীদের পরিচালনা করুন।

### নতুন ব্যবহারকারী তৈরি

1. **"Create New User"** বাটনে ক্লিক করুন।
2. পূরণ করুন:
   - **Username** — ব্যবহারকারীর নাম
   - **Email** — ইমেইল ঠিকানা
   - **Password** — পাসওয়ার্ড (কমপক্ষে ৮ অক্ষর)
   - **Role** — Super Admin, Admin, বা Viewer
3. **"Create"** চাপুন।

### রোল ব্যাখ্যা

| রোল | অনুমতি |
|-----|---------|
| **Super Admin** | সব কিছু করতে পারে |
| **Admin** | বেশিরভাগ কাজ করতে পারে |
| **Viewer** | শুধু দেখতে পারে, পরিবর্তন করতে পারে না |

### ব্যবহারকারী পরিচালনা

- **Edit** — তথ্য আপডেট করুন
- **Reset Password** — পাসওয়ার্ড রিসেট করুন
- **Disable** — অ্যাকাউন্ট সাময়িক বন্ধ করুন
- **Delete** — স্থায়ীভাবে মুছুন

### নিজের পাসওয়ার্ড পরিবর্তন

1. **"Change Password"** সেকশনে যান।
2. বর্তমান পাসওয়ার্ড দিন।
3. নতুন পাসওয়ার্ড দিন (কমপক্ষে ৮ অক্ষর)।
4. **"Change Password"** বাটন চাপুন।

---

## ১১. টিয়ার ও কোটা

**সাইডবার → 🏷️ Tiers & Quotas**

ব্যবহারকারীদের প্ল্যান এবং ব্যবহারের সীমা পরিচালনা করুন।

### টিয়ার (Plan) সমূহ

| টিয়ার | মূল্য | সীমা |
|--------|-------|------|
| **FREE** | বিনামূল্যে | সীমিত |
| **STARTER** | $৯/মাস | মাঝারি |
| **PROFESSIONAL** | $৯৯/মাস | বেশি |
| **ENTERPRISE** | কাস্টম | সর্বোচ্চ |
| **SUPERADMIN** | — | সীমাহীন |

### ব্যবহারকারীর টিয়ার পরিবর্তন

1. **"Set User Tier"** ফর্মে User ID দিন।
2. টিয়ার dropdown থেকে বেছে নিন।
3. **"Apply Tier"** বাটন চাপুন।

### কোটা রিসেট

- **Reset Daily Quotas** — আজকের ব্যবহার শূন্য করুন
- **Reset Monthly Quotas** — মাসিক ব্যবহার শূন্য করুন

> ⚠️ রিসেট করলে পুরনো ডেটা মুছে যাবে।

### Provider Quotas দেখা

**"Provider Quotas"** বাটন চাপুন। প্রতিটি AI Provider-এর:
- আজকে কতটুকু ব্যবহার হয়েছে
- দৈনিক সীমা কতটুকু
- বাকি কত আছে

---

## ১২. ডিপ্লয়মেন্ট

**সাইডবার → 🚀 Deployment**

তৈরি করা অ্যাপ বিভিন্ন platform-এ deploy করুন।

### মূল বাটনগুলো

| বাটন | কাজ |
|------|-----|
| **Docker Build 🐳** | Docker image তৈরি করুন |
| **K8s Deploy ⎈** | Kubernetes-এ deploy করুন |
| **Pipeline History 📜** | আগের deployment গুলো দেখুন |

### Atomic Multi-Target Deployment

একসাথে একাধিক platform-এ deploy করুন।

#### নতুন Deployment Target যোগ করা

1. **"Add Deployment Target"** সেকশনে পূরণ করুন:
   - **Target ID** — একটি ইউনিক নাম (যেমন: `production-cloudrun`)
   - **Type** — Cloud Run, Flutter, K8s, Play Store, App Store, Custom
   - **Description** — এই target-এর উদ্দেশ্য
   - **Validation Command** — Deploy-এর আগে চালানো কমান্ড
   - **Deploy Command** — Deploy করার কমান্ড
   - **Rollback Command** — ব্যর্থ হলে পূর্বে ফেরার কমান্ড

2. Target যোগ হলে **"Start Atomic Deployment"** বাটন চাপুন।

#### Deployment চলার সময়

- Live Status panel-এ progress দেখুন।
- সবুজ ✅ = সফল, লাল ❌ = ব্যর্থ।
- ব্যর্থ হলে স্বয়ংক্রিয়ভাবে Rollback হবে।

---

## ১৩. সিস্টেম লার্নিং

**সাইডবার → 🧠 System Learning**

AI নিজে নিজে নতুন জ্ঞান অর্জন করে। এখান থেকে সেই প্রক্রিয়া নিয়ন্ত্রণ করুন।

### Auto-Learning চালু/বন্ধ করা

- **✅ Enable Learning** — চালু করুন
- **🛑 Disable Learning** — বন্ধ করুন
- **🔄 Refresh Status** — বর্তমান অবস্থা দেখুন

### Auto-Learning কীভাবে কাজ করে

- সিস্টেম ১ ঘণ্টা নিষ্ক্রিয় থাকলে স্বয়ংক্রিয়ভাবে শিখতে শুরু করে।
- বিষয়গুলো: Architecture, Security, Performance, DevOps, AI/ML
- দুর্বল বিষয়টি আগে শেখে।
- Firebase-এর বিনামূল্যে কোটা ব্যবহার করে।

### Firebase কোটা মনিটর

| সীমা | মানে |
|------|------|
| **Daily Writes: 18,000** | প্রতিদিন সর্বোচ্চ ১৮,০০০টি তথ্য সংরক্ষণ |
| **Daily Reads: 50,000** | প্রতিদিন সর্বোচ্চ ৫০,০০০টি তথ্য পড়া |

> ⚠️ ৮০% পৌঁছলে Auto-Learning স্বয়ংক্রিয়ভাবে থামবে।

### এখনই Research শুরু করা

**"⚡ Trigger Research Now"** বাটন চাপুন।

### Knowledge Reseed করা

**"🌱 Reseed Knowledge"** বাটন চাপুন। এটি ভিত্তিগত জ্ঞান পুনরায় লোড করবে।

---

## ১৪. বিদ্যমান অ্যাপ উন্নত করুন

**সাইডবার → 🔧 Improve Existing App**

আপনার GitHub-এ থাকা যেকোনো অ্যাপ AI দিয়ে উন্নত করুন।

### কীভাবে কাজ করে

1. GitHub URL নিবন্ধন করুন।
2. SupremeAI-এর সাথে চ্যাট করে পরিকল্পনা তৈরি করুন।
3. **"⚡ Improve Now"** বা Continuous mode চালু করুন।
4. AI কোড পরিবর্তন করে push করবে।

### নতুন প্রজেক্ট নিবন্ধন

1. **"Register New Project"** ফর্মে পূরণ করুন:

| ফিল্ড | কী দিতে হবে |
|-------|-------------|
| **Project Name** | প্রজেক্টের নাম (ঐচ্ছিক) |
| **Branch** | branch (সাধারণত `main`) |
| **GitHub Repository URL** | অবশ্যই দিতে হবে |
| **Improvement Goal** | কী উন্নতি চান তা বিস্তারিত লিখুন |
| **GitHub Token** | Private repo-র জন্য প্রয়োজন |

2. **"📎 Register Project"** বাটন চাপুন।

### প্রজেক্টের উপর কাজ

প্রজেক্ট কার্ডে ক্লিক করলে Detail Panel খুলবে:

- **⚡ Improve Now** — এখনই উন্নতি শুরু করুন
- **⏸ Disable Continuous** — প্রতি ৩০ মিনিটে স্বয়ংক্রিয় পরীক্ষা বন্ধ করুন
- **▶ Enable Continuous** — স্বয়ংক্রিয় মোড চালু করুন
- **🗑️ Remove** — প্রজেক্ট মুছুন

### AI-এর সাথে পরিকল্পনা আলোচনা

1. **"💬 Discuss Improvement Plan"** সেকশনে message লিখুন।
2. Enter চাপুন বা **"Send"** বাটন চাপুন।
3. AI সম্ভাব্য পরিবর্তনের পরিকল্পনা জানাবে।
4. একমত হলে **"⚡ Improve Now"** চাপুন।

### Improvement History

- **"🕰️ Improvement History"** সেকশনে আগের সব পরিবর্তনের তালিকা দেখুন।

---

## ১৫. AI Orchestration

**সাইডবার → 🔮 AI Learning Orchestration**

স্বয়ংক্রিয়ভাবে সেরা AI এজেন্ট বেছে কাজ করায়।

### Task জমা দেওয়া

1. **"Submit Task"** ফর্মে পূরণ করুন:
   - **Goal/Task Description** — কী করতে চান বিস্তারিত লিখুন
   - **Task Type** — Code Generation, Bug Fix, Code Review, Architecture, Testing, Documentation, Deployment
   - **Top K** — কতজন এজেন্ট কাজ করবে (১ থেকে ১০)
2. **"⚡ Submit Task"** বাটন চাপুন।

### ফলাফল দেখা

- কোন AI এজেন্ট বেছে নেওয়া হয়েছে দেখাবে।
- কেন বেছে নেওয়া হয়েছে (Reasoning) দেখাবে।
- সমাধান দেখাবে।

### Agent Leaderboard

**"🏆 Leaderboard"** বাটন চাপুন — কোন এজেন্ট সবচেয়ে ভালো কাজ করছে দেখুন।

| কলাম | মানে |
|------|------|
| **Agent** | এজেন্টের নাম |
| **Score** | পারফরম্যান্স স্কোর |
| **Tasks** | কতটি কাজ করেছে |
| **Status** | Available/Busy |

---

## ১৬. SupremeAI-এর সাথে চ্যাট

**সাইডবার → 💬 Chat with SupremeAI**

তিন ধরনের চ্যাট মোড আছে।

### A) Session Chat (দীর্ঘমেয়াদি কথোপকথন)

প্রতিটি চ্যাট session সংরক্ষিত থাকে।

1. **"✚ New Chat"** বাটনে ক্লিক করুন।
2. বাম পাশে session list থেকে যেকোনো আলোচনায় ফিরে যান।
3. **Task Type** বেছে নিন: General, Code Generation, Bug Fix, Architecture, Planning, Document Analysis।
4. বার্তা লিখুন এবং **"➤ Send"** চাপুন বা Enter দিন।

### B) Project Chat (প্রজেক্ট-নির্দিষ্ট)

1. **Project ID** দিন।
2. **Session Title** দিন।
3. **"📁 Open Project Chat"** চাপুন।
4. AI প্রজেক্টের context জেনে উত্তর দেবে।

> ⚠️ Project শেষ হলে এই chat স্বয়ংক্রিয়ভাবে মুছে যাবে।

### C) Admin Rules (সিস্টেম নিয়ম)

AI-এর আচরণ নিয়ন্ত্রণ করুন।

#### নতুন নিয়ম যোগ করা

1. **"Add New Rule"** ফর্মে পূরণ করুন:
   - **Title** — নিয়মের নাম
   - **Category** — General, Behaviour, Safety, Formatting, Coding Style
   - **Rule Text** — নিয়মটি বিস্তারিত লিখুন
2. **"💾 Save Rule"** চাপুন।

#### সিস্টেম Prompt দেখা

**"👁️ Preview System Prompt"** বাটন চাপলে AI-কে প্রতিটি কলে কী বলা হচ্ছে দেখাবে।

#### উদাহরণ নিয়ম

```
শিরোনাম: বাংলায় উত্তর দাও
ক্যাটাগরি: Behaviour
নিয়ম: ব্যবহারকারী বাংলায় প্রশ্ন করলে সর্বদা বাংলায় উত্তর দাও।
```

---

## ১৭. মেট্রিক্স ও অ্যালার্ট

**সাইডবার → 📊 Metrics & Alerts**

সিস্টেমের কার্যক্ষমতা পর্যবেক্ষণ করুন।

### System Metrics

| মেট্রিক | মানে |
|---------|------|
| **CPU Usage %** | প্রসেসরের ব্যবহার |
| **Memory Usage %** | RAM-এর ব্যবহার |
| **API Latency (ms)** | AI API উত্তর দিতে কত সময় লাগছে |
| **Success Rate %** | সফলতার হার |
| **Error Rate %** | ত্রুটির হার |
| **Uptime** | সিস্টেম কতক্ষণ চলছে |

### AI Ops Summary

| মেট্রিক | মানে |
|---------|------|
| **Total Requests** | মোট AI request |
| **Cache Hits** | Cache থেকে সরাসরি উত্তর দেওয়া |
| **Retries** | পুনরায় চেষ্টার সংখ্যা |
| **Rate Limit Errors** | API সীমা ছাড়িয়ে যাওয়ার ঘটনা |
| **Queue Depth** | অপেক্ষায় থাকা request-এর সংখ্যা |

### Provider Cost Estimate

প্রতিটি AI Provider-এ কত টাকা খরচ হচ্ছে দেখুন।

---

## ১৮. ডিসিশন টাইমলাইন

**সাইডবার → 📅 Decision Timeline**

কোনো প্রজেক্টে AI কী কী সিদ্ধান্ত নিয়েছে তার ইতিহাস।

### ব্যবহার পদ্ধতি

1. **Project ID** বক্সে project-এর আইডি লিখুন।
2. **"🔍 Load Timeline"** বাটন চাপুন।

### Timeline রঙের অর্থ

| রঙ | মানে |
|----|------|
| 🟢 সবুজ | SUCCESS — সফল |
| 🔴 লাল | FAILED — ব্যর্থ |
| 🟡 হলুদ | PARTIAL — আংশিক সফল |

### Statistics বাটন

- **"📊 Aggregate Stats"** — সামগ্রিক পরিসংখ্যান দেখুন
- **"🌐 3D Viz Stats"** — ত্রি-মাত্রিক চিত্রে দেখুন

---

## ১৯. নোটিফিকেশন

**সাইডবার → 📬 Notifications**

Email, Slack, Discord বা SMS-এ বার্তা পাঠান।

### নোটিফিকেশন পাঠানো

1. **"Send Notification"** ফর্মে পূরণ করুন:
   - **Channel** — 📧 Email, 💬 Slack, 🎮 Discord, 📱 SMS
   - **Recipient** — ইমেইল ঠিকানা বা channel নাম
   - **Title/Subject** — বিষয়
   - **Message** — বার্তার বিষয়বস্তু
   - **Severity** — INFO (তথ্য), WARNING (সতর্কতা), CRITICAL (জরুরি)
2. **"📤 Send Notification"** চাপুন।

### Notification History

**"📜 Notification History"** বাটন চাপুন — আগের সব নোটিফিকেশনের তালিকা দেখুন।

| কলাম | মানে |
|------|------|
| **Channel** | কোথায় পাঠানো হয়েছে |
| **Status** | Sent/Failed/Queued |
| **Timestamp** | কখন পাঠানো হয়েছে |

---

## ২০. অডিট লগ

**সাইডবার → 📝 Audit Logs**

কে কখন কী করেছে তার সম্পূর্ণ ইতিহাস।

### লগ টেবিলের কলাম

| কলাম | মানে |
|------|------|
| **Timestamp** | তারিখ ও সময় |
| **Action** | কী করা হয়েছে |
| **Admin** | কোন অ্যাডমিন করেছে |
| **Details** | বিস্তারিত বিবরণ |
| **Status** | Success/Failed/Partial |

> 💡 **টিপস:** কোনো সমস্যা হলে Audit Log দেখে কারণ খুঁজুন।

---

## ২১. সেটিংস

**সাইডবার → ⚙️ Settings**

### Account Settings (অ্যাকাউন্ট তথ্য)

- **Email** — আপনার ইমেইল (পরিবর্তন করা যায় না)
- **Role** — আপনার ভূমিকা (পরিবর্তন করা যায় না)

### System Settings (সিস্টেম সেটিং)

- **Enable Email Notifications** — ইমেইল বিজ্ঞপ্তি চালু/বন্ধ
- **Enable SMS Alerts** — SMS বিজ্ঞপ্তি চালু/বন্ধ

### Save করা

**"💾 Save Settings"** বাটন চাপুন।

### লগআউট করা

**"🚨 Logout"** বাটন চাপুন।

---

## ২২. VPN ম্যানেজমেন্ট

**সাইডবার → 🔒 VPN Management**

সুরক্ষিত নেটওয়ার্ক সংযোগ পরিচালনা করুন।

### নতুন VPN যোগ করা

1. **"Add VPN"** বাটনে ক্লিক করুন।
2. পূরণ করুন:
   - **VPN Name** — একটি নাম দিন
   - **Protocol** — WireGuard, OpenVPN, বা IPSec
   - **Host** — সার্ভারের ঠিকানা
   - **Port** — পোর্ট নম্বর
   - **Encryption** — এনক্রিপশন ধরন
   - **Credentials** — লগইন তথ্য
3. **"Save"** চাপুন।

### VPN সংযোগ

- **Connect** — VPN চালু করুন
- **Disconnect** — VPN বন্ধ করুন

---

## ২৩. সাধারণ সমস্যা ও সমাধান

### সমস্যা ১: Provider Coverage-এ "Missing Key" দেখাচ্ছে

**কারণ:** ওই Provider-এর API Key যোগ করা হয়নি।

**সমাধান:**
1. **API Key Manager**-এ যান।
2. Provider-টির API Key যোগ করুন।
3. **Provider Coverage**-এ ফিরে **"Load Coverage"** চাপুন।

---

### সমস্যা ২: AI কাজ করছে না

**সমাধান:**
1. **Commands → System Status** চেক করুন।
2. **Metrics → Error Rate** দেখুন।
3. **API Key Manager** থেকে Provider-এর Key **"Test"** করুন।
4. সব ব্যর্থ হলে **Commands → Emergency Stop** চেপে সিস্টেম রিস্টার্ট করুন।

---

### সমস্যা ৩: Firebase Quota ৮০%-এ পৌঁছেছে

**কারণ:** Auto-Learning অনেক বেশি তথ্য সংরক্ষণ করেছে।

**সমাধান:**
1. **System Learning → Disable Learning** চাপুন।
2. পরের দিন সকালে quota রিসেট হলে আবার **Enable** করুন।

---

### সমস্যা ৪: Deployment ব্যর্থ হয়েছে

**সমাধান:**
1. **Deployment → Pipeline History** থেকে error message পড়ুন।
2. **Validation Command** ঠিক আছে কিনা দেখুন।
3. GitHub Token সঠিক কিনা যাচাই করুন।
4. Rollback Command কাজ করেছে কিনা দেখুন।

---

### সমস্যা ৫: "Improve Existing App" কাজ করছে না

**সমাধান:**
1. GitHub Token সঠিক কিনা যাচাই করুন (private repo-র জন্য)।
2. **Improvement Goal** যথেষ্ট বিস্তারিত কিনা দেখুন।
3. **💬 Discuss Improvement Plan**-এ AI-এর সাথে আলোচনা করুন।
4. কমপক্ষে একটি Active API Key আছে কিনা নিশ্চিত করুন।

---

## 🎯 দ্রুত শুরুর গাইড (Quick Start)

নতুন অ্যাডমিন হিসেবে এই ক্রমে কাজ শুরু করুন:

```
1️⃣  API Key Manager → অন্তত একটি Provider-এর Key যোগ করুন
2️⃣  Provider Coverage → "Load Coverage" চেপে নিশ্চিত করুন "Configured" দেখাচ্ছে  
3️⃣  Admin Rules → AI-এর আচরণের নিয়ম সেট করুন
4️⃣  System Learning → Enable করুন (বিনামূল্যে শেখে)
5️⃣  Projects → নতুন অ্যাপ তৈরি করুন বা Improve Existing App ব্যবহার করুন
6️⃣  Dashboard → নিয়মিত পর্যবেক্ষণ করুন
```

---

## 📞 সাহায্য দরকার?

সমস্যা হলে:
1. **Audit Logs** দেখুন — কী ঘটেছে তা জানুন।
2. **Commands → System Status** চালান — সিস্টেম অবস্থা জানুন।
3. **💬 Chat with SupremeAI** — AI-কে সরাসরি জিজ্ঞেস করুন।

---

*এই গাইডটি SupremeAI Admin Panel v3.1 এর জন্য তৈরি।*
