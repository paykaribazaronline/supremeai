# SupremeAI Project Browser / Autonomous Browser - Audit Report

> **Status:** 🟢 Updated for v5 Architecture


এই ডকুমেন্টে **Project Browser** বা **Autonomous Browser** এর সমস্ত ফিচার এবং API গুলোর বর্তমান অবস্থা (Implementation Status) বিস্তারিতভাবে তুলে ধরা হলো। ফ্রন্টএন্ডে "Cinematic Autonomous Browser" এবং "Auto Browser" UI তৈরি করা থাকলেও, ব্যাকএন্ডের বেশিরভাগ লজিক এখনও অসম্পূর্ণ বা Stubbed অবস্থায় আছে।

## ১. কোর ব্রাউজার ইঞ্জিন এবং স্ক্র্যাপিং (Core Engine)

### Java Backend (`BrowserService.java`)
*   **Basic Jsoup Scraping:** ✅ ইমপ্লিমেন্টেড। `searchAndScrape()` মেথডে Jsoup ব্যবহার করে ডেটা নিয়ে আসার কাজ করা হয়েছে। 
*   **Playwright Fallback:** ✅ ইমপ্লিমেন্টেড। Jsoup যদি ফেইল করে বা JS/Bot Protection (যেমন Cloudflare) ডিটেক্ট করে, তবে স্বয়ংক্রিয়ভাবে Playwright (Headless Chromium) রান করে ডেটা স্ক্র্যাপ করার মেকানিজম আছে।

### Node.js Backend (`scrapeEngine.ts`)
*   **Sidecar Integration:** ✅ ইমপ্লিমেন্টেড। `callPlaywright()` ফাংশনের মাধ্যমে একটি আলাদা Playwright সার্ভার (`localhost:3001`) এর সাথে যোগাযোগ করে URL নেভিগেট এবং টেক্সট এক্সট্রাক্ট করার সিস্টেম আছে।

---

## ২. লাইভ ইন্টারেক্টিভ ব্রাউজার (Admin Browser / Viewport)
এই API গুলো ড্যাশবোর্ডের `AdminBrowser.tsx` থেকে কল করা হয় যাতে ব্যবহারকারী রিমোট ব্রাউজার দেখতে এবং ক্লিক করতে পারেন। **সবগুলো API অসম্পূর্ণ (Stubbed)।**

*   `GET /api/browser/surf/screenshot` - ❌ (রিটার্ন করে ফাঁকা টেক্সট)
*   `POST /api/browser/surf/navigate` - ❌ (ফাঁকা `Mono.empty()`)
*   `POST /api/browser/surf/click` - ❌ (ফাঁকা `Mono.empty()`)
*   `POST /api/browser/surf/fill` - ❌ (ফাঁকা `Mono.empty()`)
*   `POST /api/browser/surf/click-at` - ❌ (ফাঁকা `Mono.empty()`)
*   `POST /api/browser/surf/type-key` - ❌ (ফাঁকা `Mono.empty()`)
*   `GET /api/browser/surf/accessibility` - ❌ (রিটার্ন করে ফাঁকা Map)

---

## ৩. অটোনোমাস ব্রাউজার টাস্ক (Auto Browser Tasks)
AI নিজে থেকে ব্রাউজার চালিয়ে টাস্ক কমপ্লিট করবে, এই সংক্রান্ত ফিচারগুলো। **সবগুলো API অসম্পূর্ণ (Stubbed)।**

*   `GET /api/browser/tasks` - ❌ (অ্যাকটিভ টাস্কের লিস্ট দেয় না, `Flux.empty()`)
*   `POST /api/browser/tasks` - ❌ (টাস্ক ক্রিয়েট করার লজিক নেই, ডামি অবজেক্ট রিটার্ন করে)
*   `DELETE /api/browser/tasks/{id}` - ❌ (ডিলিট লজিক নেই)
*   `POST /api/browser/tasks/{id}/step` - ❌ (অটো স্টেপ রান করার লজিক নেই)
*   `GET /api/browser/tasks/{id}/findings` - ❌ (ফাইন্ডিং বা রেজাল্ট সেভ/রিট্রিভ করার লজিক নেই)
*   `POST /api/browser/findings` - ❌ (ডাটাবেসে ফাইন্ডিং অ্যাড করার লজিক নেই)

---

## ৪. অ্যাক্টিভিটি ট্র্যাকিং এবং ব্রাউজিং স্ট্যাটাস
ব্রাউজার বর্তমানে কী করছে বা রিসেন্ট অ্যাক্টিভিটি। **সবগুলো API অসম্পূর্ণ (Stubbed)।**

*   `GET /api/browser/surf/status` - ❌ (সবসময় `inactive` দেখায়)
*   `POST /api/browser/surf/start` - ❌ (ব্রাউজার স্টার্ট হয় না)
*   `POST /api/browser/surf/stop` - ❌ (ব্রাউজার স্টপ হয় না)
*   `GET /api/browser/activity/recent` - ❌ (ফাঁকা লিস্ট রিটার্ন করে)
*   `POST /api/browser/simulate-activity` - ❌ (ডাটাবেসে রেকর্ড সেভ হয় না)

---

## ৫. ক্রেডেনশিয়াল এবং সেফটি (Credentials & Manual Auth)
ব্রাউজার যদি লগইন পেজে আটকে যায় এবং ইউজারের ক্রেডেনশিয়াল বা ম্যানুয়াল অ্যাকশন দরকার হয়। **সবগুলো API অসম্পূর্ণ (Stubbed)।**

*   `GET /api/browser/credentials` - ❌ (ফাঁকা লিস্ট)
*   `POST /api/browser/credentials` - ❌ (পাসওয়ার্ড সেভ হয় না)
*   `DELETE /api/browser/credentials/{id}` - ❌
*   `POST /api/browser/surf/pause-manual` - ❌ (ম্যানুয়াল ইন্টারভেনশনের লজিক নেই)
*   `POST /api/browser/surf/resume` - ❌ 
*   `POST /api/browser/surf/skip-auth` - ❌ 
*   `GET /api/browser/surf/paused-state` - ❌ 

---

## ৬. URL পারমিশন এবং রুলস (Allowed/Denied URLs)
ব্রাউজার কোন সাইটে যেতে পারবে আর কোথায় পারবে না, তার কন্ট্রোল। **সবগুলো API অসম্পূর্ণ (Stubbed)।**

*   `GET /api/browser/urls/allowed` - ❌
*   `GET /api/browser/urls/denied` - ❌
*   `POST /api/browser/urls/allowed` ও `denied` - ❌ (ডাটাবেসে সেভ হয় না)
*   `POST /api/browser/urls/allowAll` - ❌
*   `PUT /api/browser/urls/{id}` - ❌
*   `DELETE /api/browser/urls/{id}` - ❌
*   `GET /api/browser/urls/requests` - ❌ (ব্রাউজারের পারমিশন রিকোয়েস্ট আসে না)
*   `POST /api/browser/urls/requests/{id}/decision` - ❌ 

---

## ৭. সিস্টেম লার্নিং টগল
*   `GET /api/browser/system-learning` - ❌ (সবসময় Hardcoded `enabled: true` রিটার্ন করে)
*   `POST /api/browser/system-learning/toggle` - ❌ (টগল সেভ হয় না)

---

## সারসংক্ষেপ (Conclusion)
*   **ড্যাশবোর্ড UI:** সম্পূর্ণ তৈরি (React components for BrowserViewport, Toolbar, Tasks, etc. are present)।
*   **বেসিক ওয়েব স্ক্র্যাপিং (Background):** কাজ করছে। (Jsoup + Playwright fallback in backend services)।
*   **UI থেকে কন্ট্রোল (API Controller):** কাজ করছে না। Controller রাউটগুলো তৈরি করা থাকলেও, Service লেয়ারে `BrowserService.java`-তে সমস্ত মেথড ফাঁকা (Stubbed) অবস্থায় আছে (যেমন `return Mono.empty()`)। 

কাজেই, Project Browser এর ভিশন বাস্তবায়িত করতে হলে `BrowserService.java` এর ভেতরের প্রতিটি ফাঁকা মেথডে প্রকৃত ডেটাবেস অপারেশন এবং Playwright ইন্টিগ্রেশন (অথবা Remote Browser Protocol) এর লজিকগুলো লিখতে হবে।