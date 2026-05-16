# SupremeAI Hybrid Storage Strategy (Hot & Cold)

## Overview
SupremeAI-তে ডাটা ম্যানেজমেন্ট এবং কস্ট অপ্টিমাইজেশনের জন্য একটি হাইব্রিড স্টোরেজ মডেল ব্যবহার করা হচ্ছে। এই মডেলটি ফায়ারবেসের গতি (Speed) এবং টেলিগ্রামের আনলিমিটেড স্টোরেজ (Scale) ক্ষমতাকে একত্রিত করে।

## 1. Data Classification

### A. Hot Storage (Firebase/Firestore)
- **উদ্দেশ্য:** রিয়েল-টাইম এক্সেস এবং লো ল্যাটেন্সি।
- **ডাটা টাইপ:** 
    - সাম্প্রতিক চ্যাট হিস্ট্রি (সর্বশেষ ২৪ ঘণ্টা অথবা ৫০টি মেসেজ)।
    - ইউজার প্রোফাইল এবং সক্রিয় সেশন ডাটা।
    - সিস্টেম কনফিগারেশন।
- **সুবিধা:** অত্যন্ত দ্রুত লোড হয় এবং সার্চিং সহজ।

### B. Cold Storage (Telegram/Teldrive)
- **উদ্দেশ্য:** দীর্ঘমেয়াদী আর্কাইভ এবং বড় ফাইল স্টোরেজ।
- **ডাটা টাইপ:**
    - পুরানো চ্যাট হিস্ট্রি (Archive)।
    - ইউজার আপলোডেড মিডিয়া (Images, Videos, Documents)।
    - সিস্টেম লার্নিং র-ডাটা (Raw Logs, Training Artifacts)।
    - **Codebase Backups:** সোর্স কোডের জিপ আর্কাইভ।
- **সুবিধা:** ১০০% ফ্রি স্টোরেজ, আনলিমিটেড ক্যাপাসিটি।

## 2. Centralized Configuration (Firestore)
সিস্টেমের সমস্ত স্টোরেজ এবং ব্যাকআপ সেটিংস এখন `system_configs/global_settings` ডকুমেন্টে সেন্ট্রালাইজড করা হয়েছে।

- **Telegram Config:** `apiId`, `apiHash`, `botToken`, `teldriveUrl`, `channelId` (এর মাধ্যমে Teldrive-এর সাথে সংযোগ স্থাপন হয়)।
- **Supabase Config:** `dbUrl`, `password` (মেটাডাটা এবং সেশন স্টোরেজের জন্য বিকল্প ডাটাবেস)।
- **Real-time Monitoring:** ড্যাশবোর্ডে বটের লাইভ স্ট্যাটাস (Connected/Disconnected) এবং স্টোরেজ ইউজেশ দেখা যায়।

## 3. Chat Archiving Workflow (The Implementation Plan)

### Phase 1: Real-time Ingestion
ইউজার যখন মেসেজ পাঠাবে, তা সরাসরি **Hot Storage (Firestore)**-এ সেভ হবে।

### Phase 2: Automatic Offloading
একটি ব্যাকগ্রাউন্ড শিডিউলার (Cron Job) নির্দিষ্ট সময় পর পর চলবে:
1. ইউজার প্রতি মেসেজ কাউন্ট চেক করবে।
2. যদি মেসেজ সংখ্যা ৫০ অতিক্রম করে অথবা ২৪ ঘণ্টার বেশি পুরানো হয়:
   - সেই মেসেজগুলোকে একটি JSON ফাইলে রূপান্তর করবে।
   - মিডিয়া ফাইল থাকলে সেগুলোকে Teldrive-এ আপলোড করবে।
   - JSON ফাইলটি Teldrive-এর নির্দিষ্ট ফোল্ডারে (`/archives/chats/{userId}/`) স্টোর করবে।
   - ফায়ারবেস থেকে সেই পুরানো ডাটাগুলো ডিলিট করে দিবে।

### Phase 3: Retrieval
ইউজার যখন "View Older Messages" এ ক্লিক করবে:
1. সিস্টেম Teldrive এপিআই কল করে ওই ইউজারের আর্কাইভ লিস্ট দেখাবে।
2. ইউজার নির্দিষ্ট আর্কাইভ সিলেক্ট করলে তা ডাউনলোড হয়ে লোড হবে।

## 4. Codebase Backup Strategy
সিস্টেমের পূর্ণ নিরাপত্তা নিশ্চিত করতে পুরো সোর্স কোড এবং গুরুত্বপূর্ণ কনফিগারেশন ফাইলগুলো নিয়মিত ব্যাকআপ রাখা হবে।

- **ব্যাকআপ মেকানিজম:** 
    - সম্পূর্ণ মনোরেপোকে (src, dashboard, supremeai-vscode-extension, etc.) একটি `.tar.gz` বা `.zip` ফাইলে রূপান্তর করা হবে।
    - এই ফাইলটি প্রতি ২৪ ঘণ্টা পর পর (অথবা বড় কোনো পরিবর্তনের পর) স্বয়ংক্রিয়ভাবে **Cold Storage (Telegram)**-এ আপলোড হবে।
- **অটোমেশন:** `BackupService.java` (To be finalized) এর মাধ্যমে প্রতিদিন রাতে এই প্রসেসটি চলবে।
- **স্টোরেজ পাথ:** `/supremeai/backups/codebase/{timestamp}/`
- **সুবিধা:** সার্ভারে কোনো সমস্যা হলে বা কোডবেস নষ্ট হয়ে গেলে আমরা যেকোনো সময় টেলিগ্রাম থেকে সর্বশেষ ভার্সনটি রিস্টোর করতে পারব।

## 5. System Learning & Metadata
- লার্নিং ডাটাগুলো প্রথমে লোকাল বাফারে থাকবে।
- নির্দিষ্ট ব্যাচ পূর্ণ হলে তা সরাসরি Cold Storage-এ পাঠিয়ে দেয়া হবে।
- শুধুমাত্র লার্নিং সামারি বা ইন্ডেক্স ফায়ারবেসে রাখা হবে যাতে সার্চিং ফাস্ট হয়।

## 6. Cost Comparison

| Metric | Pure Firebase | Hybrid (Firebase + Telegram) |
| :--- | :--- | :--- |
| **Storage Cost** | High (Exponential) | **Zero (Flat)** |
| **Read/Write Cost**| High (Every message) | **Minimal (Batch Sync)** |
| **Media Handling** | Expensive | **Unlimited & Free** |
| **Retention** | Limited by budget | **Forever** |
