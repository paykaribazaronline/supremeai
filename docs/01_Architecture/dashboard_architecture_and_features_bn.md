# 🖥️ Dashboard Architecture & Tab Features

> **Status:** 🟢 Updated for v6.1 Architecture (Post-Audit Fixes)

> **ডকুমেন্ট আপডেট তারিখ:** 2026-06-05
> **প্রজেক্ট:** SupremeAI
> **হোস্টিং:** Firebase Hosting (`supremeai-a.web.app`)

SupremeAI-এর ড্যাশবোর্ডটি React 18, TypeScript এবং Vite ব্যবহার করে তৈরি একটি Dual-Dashboard সিস্টেম। অ্যাডমিন এবং সাধারণ ইউজারের জন্য আলাদা আলাদা লেআউট ও রাউট রয়েছে।

---

## 🏗️ ১. ফ্রন্টএন্ড আর্কিটেকচার ওভারভিউ

### টেক স্ট্যাক
| প্রযুক্তি | বিবরণ |
|---|---|
| **React 18** | UI রেন্ডারিং |
| **TypeScript** | টাইপ-সেফ কোড |
| **Vite 5** | বিল্ড টুল ও ডেভ সার্ভার |
| **Ant Design** | UI কম্পোনেন্ট লাইব্রেরি |
| **React Router DOM** | ক্লায়েন্ট-সাইড রাউটিং |
| **Firebase Auth** | অথেনটিকেশন |
| **Firestore** | ইউজার রোল/টিয়ার ডেটাবেস |
| **Three.js / R3F** | 3D ভিজুয়ালাইজেশন |
| **Framer Motion** | অ্যানিমেশন |

### কোর আর্কিটেকচার
- **State Management:** React Hooks (`useState`, `useEffect`) + `RoleContext` (গ্লোবাল রোল ম্যানেজমেন্ট)
- **Routing:** React Router DOM দিয়ে `/admin/*` ও `/user/*` আলাদা লেআউটে ন্যাভিগেশন কন্ট্রোল
- **Authentication:** Firebase Auth → Firestore থেকে `tier`/`role` ফেচ → `authUtils` এ টোকেন ও ইউজার ডেটা সেভ
- **Lazy Loading:** `lazyWithRetry()` র‍্যাপার দিয়ে সকল পেজ কোড-স্প্লিট করা হয়, ক্যাশ স্টেল হলে অটো-রিকভারি করে
- **Error Boundary:** চাংক লোড ফেইল হলে `ErrorBoundary` অটোমেটিকভাবে পেজ রিফ্রেশ করে

---

## 🔐 ২. Dual-Dashboard অথেনটিকেশন ফ্লো

### লগইন প্রসেস
```
ইউজার লগইন → Firebase Auth ভেরিফিকেশন → Firestore `users` কালেকশনে email দিয়ে সার্চ
    → `tier: "ADMIN"` পেলে → /admin/dashboard এ রিডাইরেক্ট
    → `tier: "user"` বা অন্য কিছু পেলে → /user/dashboard এ রিডাইরেক্ট
```

### রাউট গার্ডস
| লেআউট | ফাইল | গার্ড লজিক |
|---|---|---|
| **AdminRouteLayout** | `AdminRouteLayout.tsx` | `isAuthenticated` + `isAdmin` চেক। ফেইল হলে `/user/dashboard`-এ কিক |
| **UserRouteLayout** | `UserRouteLayout.tsx` | শুধু `isAuthenticated` চেক। ফেইল হলে `/login`-এ কিক |

### RoleContext (গ্লোবাল স্টেট)
- `RoleProvider` কম্পোনেন্ট অ্যাপ লোড হওয়ার সাথে সাথে `localStorage` থেকে সিঙ্ক্রোনাসভাবে রোল রিড করে (Race Condition প্রিভেনশন)
- ক্রস-ট্যাব সিঙ্ক: `storage` ইভেন্ট লিসেনার দিয়ে এক ট্যাবে লগআউট হলে সব ট্যাব অটো-লগআউট হয়
- লগইন থাকা অবস্থায় `/login` পেজে গেলে ড্যাশবোর্ডে অটো-রিডাইরেক্ট (ফ্ল্যাশ প্রিভেনশন)

---

## 🗂️ ৩. সাইডবার মেন্যু কনফিগারেশন

সাইডবারের সকল মেন্যু আইটেম `DashboardConfigs.tsx` ফাইলে সেন্ট্রালি কনফিগার করা আছে। প্রতিটি আইটেমে `roles` অ্যারে দিয়ে রোল-বেসড ফিল্টারিং হয়।

---

## 📑 ৪. ৯টি সাইডবার গ্রুপ ও তাদের ট্যাবসমূহ

### 🥇 1. Dashboard (কমান্ড সেন্টার)
| ফাইল | রোল | বিবরণ |
|---|---|---|
| `DashboardHome.tsx` | guest, user, admin | মূল ড্যাশবোর্ড। রিয়েল-টাইম সিস্টেম স্ট্যাটাস, আপটাইম, নিউরাল অ্যাক্টিভিটি, মডেল হেলথ ও গ্রাফিক্যাল মেট্রিক্স দেখায়। |

---

### 🧠 2. AI & Neural Hub
| ট্যাব | ফাইল | রোল | বিবরণ |
|---|---|---|---|
| Neural Chat | `ChatWithAI.tsx` | guest, user, admin | AI-এর সাথে সরাসরি চ্যাট। Intent Classifier কি-ওয়ার্ড চেক করে উপযুক্ত মডেলে কুয়েরি পাঠায়। |
| AI Providers | `AdminProviders.tsx` | admin | AI প্রোভাইডার (OpenAI, Anthropic, Groq ইত্যাদি) কনফিগারেশন, API-Key ম্যানেজমেন্ট ও হেলথ চেক। |
| Learning | `AdminLearning.tsx` | admin | AI-এর অফলাইন নলেজ ও ব্যাকগ্রাউন্ড লার্নিং মনিটরিং। নলেজ সিডিং ও লার্নিং হিস্ট্রি ভিউ। |
| Code Intelligence | `AdminCodeAnalysis.tsx` | admin | কোড অ্যানালাইসিস, কোড কোয়ালিটি রিপোর্ট ও AI-চালিত কোড রিভিউ। |
| Reverse Engineering | `AdminReverseEngineer.tsx` | admin | APK/ওয়েবসাইট রিভার্স ইঞ্জিনিয়ারিং ও স্ট্রাকচার অ্যানালাইসিস। |
| SupremeAI Offline | `AdminSuperFly.tsx` | guest, user, admin | এজ AI — অফলাইনেও কাজ করতে পারে এমন AI মডেল কন্ট্রোল। |

---

### 📊 3. System Observability
| ট্যাব | ফাইল | রোল | বিবরণ |
|---|---|---|---|
| Monitoring | `AdminMonitoring.tsx` | admin | সার্ভার হেলথ, CPU/RAM ব্যবহার, API রেসপন্স টাইম মনিটরিং। |
| Analytics & Perf. | `AdminAnalytics.tsx` | admin | সিস্টেম পারফরম্যান্স এনালাইটিক্স ও ট্রেন্ড চার্ট। |
| System Logs | `AdminLogs.tsx` | admin | রিয়েল-টাইম সিস্টেম লগ ভিউয়ার। |
| Reports | `AdminReports.tsx` | admin | অটো-জেনারেটেড সিস্টেম রিপোর্ট ও PDF এক্সপোর্ট। |
| Notifications | `AdminNotifications.tsx` | admin | সিস্টেম অ্যালার্ট ও নোটিফিকেশন ম্যানেজমেন্ট। |

---

### ☁️ 4. Infrastructure Hub
| ট্যাব | ফাইল | রোল | বিবরণ |
|---|---|---|---|
| Infra & Advice | `AdminInfrastructure.tsx` | admin | GCP ক্লাউড ইনফ্রাস্ট্রাকচার ম্যানেজমেন্ট ও কস্ট অপ্টিমাইজেশন পরামর্শ। |
| Cloud DB & Backup | `AdminCloudDbHub.tsx` | admin | ডেটাবেস ম্যানেজমেন্ট, ব্যাকআপ শিডিউলিং ও রিস্টোর। |
| VPN Connection | `AdminVPN.tsx` | admin | VPN কানেকশন কনফিগারেশন ও স্ট্যাটাস। |

---

### 🛡️ 5. Security & Policies
| ট্যাব | ফাইল | রোল | বিবরণ |
|---|---|---|---|
| Security | `AdminSecurity.tsx` | admin | সিকিউরিটি অডিট, ভালনারেবিলিটি স্ক্যান ও থ্রেট মনিটরিং। |
| Approvals | `AdminApprovals.tsx` | admin | পেন্ডিং অনুমোদন ও অ্যাপ্রুভাল ওয়ার্কফ্লো। |
| System & Work Rules | `AdminRules.tsx` | admin | সিস্টেম বিহেভিয়ার রুলস (AI কনফিডেন্স, ফেইলওভার পলিসি ইত্যাদি) কনফিগারেশন। |

---

### 👥 6. Users & Access
| ট্যাব | ফাইল | রোল | বিবরণ |
|---|---|---|---|
| User Management | `AdminUsers.tsx` | admin | ইউজার তালিকা, রোল অ্যাসাইন, টিয়ার পরিবর্তন ও ইউজার ব্লক/আনব্লক। |
| Quotas | `AdminQuotas.tsx` | admin | ইউজার ও সিস্টেম কোটা ম্যানেজমেন্ট (API কল লিমিট, স্টোরেজ ইত্যাদি)। |

---

### 🛠️ 7. Web & Tools
| ট্যাব | ফাইল | রোল | বিবরণ |
|---|---|---|---|
| Browser | `AdminBrowser.tsx` | guest, user, admin | ইন-অ্যাপ ব্রাউজার। Playwright-চালিত রিয়েল ব্রাউজার সেশন। |
| Auto Browser | `AutoBrowser.tsx` | guest, user, admin | AI-চালিত অটোমেটেড ওয়েব স্ক্র্যাপিং ও ব্রাউজিং টাস্ক। |
| OCR Tool | `AdminOCR.tsx` | admin | ইমেজ/PDF থেকে টেক্সট এক্সট্রাকশন (Optical Character Recognition)। |

---

### 🚀 8. Workspaces
| ট্যাব | ফাইল | রোল | বিবরণ |
|---|---|---|---|
| Deployments | `AdminProjects.tsx` | user, admin | প্রজেক্ট ডিপ্লয়মেন্ট ম্যানেজমেন্ট। GCP Cloud Run-এ কন্টেইনার ডেপ্লয়। |
| Simulator | `AdminSimulator.tsx` | guest, user, admin | AI-জেনারেটেড কোডের লাইভ প্রিভিউ ও সিমুলেশন। |

---

### ⚙️ 9. Config (কনফিগ)
| ফাইল | রোল | বিবরণ |
|---|---|---|
| `AdminSettings.tsx` | guest, user, admin | ড্যাশবোর্ড সেটিংস — ডার্ক/লাইট মোড, চ্যাট ফন্ট, ল্যাঙ্গুয়েজ ইত্যাদি। |

---

## 🔧 ৫. Firebase Hosting কনফিগারেশন

### ক্যাশ হেডার স্ট্র্যাটেজি
| রিসোর্স | Cache-Control | কারণ |
|---|---|---|
| `/index.html` | `no-cache, no-store, must-revalidate` | প্রতিটি ভিজিটে সর্বশেষ HTML ফেচ করে, যেন নতুন ডেপ্লয়ের পর পুরোনো চাংক রেফারেন্স না থাকে |
| `/assets/**` | `public, max-age=31536000, immutable` | Vite হ্যাশড ফাইলগুলো কন্টেন্ট বদলালে নাম বদলায়, তাই চিরকাল ক্যাশ করা নিরাপদ |

### রাউটিং রিরাইটস
- `/api/**` → Cloud Run (Spring Boot ব্যাকএন্ড)
- `/telemetry/**` → Cloud Run
- `/ws/**` → Cloud Run (WebSocket)
- `/admin/**`, `/user/**`, `/login`, `/mobile/**`, `/visualizer` → `index.html` (SPA রাউটিং)
- ~~`** → /index.html`~~ **সরানো হয়েছে** — আগে এই catch-all rewrite পুরোনো JS ফাইল ফেচ করলে `index.html` সার্ভ করত, যার ফলে "MIME type text/html" এরর হতো। এখন অনুপস্থিত অ্যাসেট ফাইল সঠিকভাবে ৪০৪ রিটার্ন করে এবং `lazyWithRetry()` অটো-রিলোড করে।

---

## 🛡️ ৬. সিকিউরিটি ফিচারস

| ফিচার | বিবরণ |
|---|---|
| **Firestore Role Fetch** | লগইনের সময় ইমেইল দিয়ে Firestore `users` কালেকশন থেকে `tier`/`role` ফেচ |
| **Synchronous Init** | RoleContext সিঙ্ক্রোনাসভাবে localStorage থেকে রোল রিড করে (Race Condition প্রিভেনশন) |
| **Cross-Tab Sync** | `storage` ইভেন্ট লিসেনার — এক ট্যাবে লগআউট হলে সব ট্যাব অটো-লগআউট |
| **Login Guard** | লগইন থাকা অবস্থায় `/login` পেজ রেন্ডার হয় না, সরাসরি ড্যাশবোর্ডে রিডাইরেক্ট |
| **Chunk Auto-Recovery** | `lazyWithRetry()` ও `ErrorBoundary` — ক্যাশ স্টেল হলে সাইলেন্টলি অটো-রিলোড |
| **Token Fallback Fix** | `getToken()` টোকেন না পেলে `null` রিটার্ন করে (`"GUEST_MODE"` নয়)। এতে হার্ড রিফ্রেশে অটো-লগআউট বন্ধ হয়েছে |
| **Token Obfuscation** | `authUtils`-এ XOR + Base64 দিয়ে টোকেন ও ইউজার ডেটা অবফাস্কেট করে localStorage-এ সেভ |
| **No Catch-All Rewrite** | `/assets/**` পাথে মিসিং ফাইল সঠিকভাবে ৪০৪ দেয়, HTML সার্ভ করে না |

---

**সারসংক্ষেপ:**
SupremeAI v6 ড্যাশবোর্ড একটি Dual-Dashboard আর্কিটেকচার (Admin + User) যা Firestore-ভিত্তিক রোল ম্যানেজমেন্ট, অটো-রিকভারি লেজি লোডিং, ক্রস-ট্যাব সেশন সিঙ্ক এবং CDN-অপ্টিমাইজড ক্যাশিং সহ একটি এন্টারপ্রাইজ-গ্রেড কমান্ড সেন্টার।
