# সুপ্রেম SupremeAI সিস্টেমের জীবনচক্র ও কার্যক্রম
## একটি সম্পূর্ণ বিবরণী দস্তাবেজ

---

## ১. সিস্টেমের সাধারণ ওভারভিউ (Overview)

**সুপ্রেম SupremeAI** একটি এন্টারপ্রাইজ-গ্রেড মাল্টি-এজেন্ট প্ল্যাটফর্ম। এটি ন্যাচারাল ভাষায় রিকোয়েস্ট থেকে অ্যান্ড্রয়েড অ্যাপ তৈরি করা, কোড বিশ্লেষণ, এবং ক্লাউড অর্কেস্ট্রেশনের জন্য ডিজাইন করা।

### ক্লাউড-নেটিভ (Cloud Native)
- **GKE-তে হোস্টেড**: Google Kubernetes Engine-এ রান
- **Firebase ইন্টিগ্রেশন**: Firestore, Authentication, Hosting
- **Artifact Registry**: ডকার ইমেজ স্টোরেজ

---

## ২. সিস্টেমের জীবনচক্র (Lifecycle Process)

### ২.১ ইনপুট গ্রহণ (Input Reception)
```
User Request (CLI/Web/Dashboard)
         ↓
   API Gateway
         ↓
  Authentication Layer (JWT/Firebase Auth)
         ↓
  Request Router → Task Classification
```

### ২.২ টাস্ক ক্লাসিফিকেশন
- **টাস্ক টাইপ**: CODE_GENERATION, ERROR_FIXING, QUESTION_ANSWERING, BUILD_DEPLOY
- **প্রম্পট প্রক্রিয়াকরণ**: ইন্টেনশনালাইজেশন, কন্টেক্স্ট বাইপাস

### ২.৩ AI সর্বজনীন অর্কেস্ট্রেশন (AI Orchestration) - Solo-First Approach

```
executeWithSupremeIntelligence()
         ↓
┌─────────────────────────────────────────────────────┐
│ 1. Knowledge Base Search (errorSignature)           │
│    → core_knowledge.json + Firestore system_learning│
└─────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────┐
│ 2. Solo Mode Default - Local First                  │
│    → ALWAYS try local fallback first                 │
│    → Solo-First: local model + knowledge base       │
└─────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────┐
│ 3. Opportunistic Cloud Help (if needed)             │
│    → Check for active cloud providers                │
│    → If available: aiProfiler.getBestAIForTask()     │
│    → Circuit Breaker + Quota Check                   │
│    → 8-second timeout for quick cloud response       │
└─────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────┐
│ 4. Fallback Chain                                   │
│    → Cloud fails → Local fallback                    │
│    → All cloud fails → Solo Mode activation          │
└─────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────┐
│ 5. Co-Reasoning & Immunity                            │
│    → Local AirLLM sidecar polishes cloud response   │
│    → CodeInfectionCheck filters malicious output    │
└─────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────┐
│ 6. Knowledge Recording                              │
│    → recordSuccessWithPermission()                    │
│    → enhancedLearningService.learnFromAPIUsage()      │
└─────────────────────────────────────────────────────┘
```

---

## ৩. মূল উপাদানসমূহ (Core Components)

### ৩.১ AIFallbackOrchestrator.java
**ফাইল লোকেশন**: `src/main/java/com/supremeai/fallback/AIFallbackOrchestrator.java`

**কার্যক্রম**:
- **Circuit Breaker**: Resilience4j এর মাধ্যমে সক্রিয় ক্লাউড প্রোভাইডারদের ত্রুটি সহনশীলতা নিশ্চিত করা
- **Solo Mode**: ডিফল্টরূপে স্থানীয় জ্ঞান বেস ও মডেল ব্যবহার (Solo-First)
- **Hedging**: একই সময়ে একাধিক প্রোভাইডারের সাথে সমান্তরাল রিকোয়েস্ট (Latency কমানোর জন্য)
- **Co-Reasoning**: স্থানীয় AirLLM সাইডকার দ্বারা ক্লাউড রেসপন্স রিয়েল-টাইমে অপ্টিমাইজ করা

### ৩.২ GlobalKnowledgeBase
- **core_knowledge.json**: স্থানীয় সিদ্ধান্ত গ্রহণের জন্য টেমপ্লেট
- **system_learning Firestore**: রিয়েল-টাইম শিখনের কলেকশন
- **Error Signature Hashing**: একই এররের জন্য ক্যাশে করা সমাধান

### ৩.৩ AIProviderFactory
- **প্রোভাইডার টাইপ**: Gemini, OpenAI, Claude, Vertex AI, Ollama, airllm-sidecar
- **কনফিগারেশন**: Firestore-এ সঞ্চালিত api_providers কালেকশন

### ৩.৪ CodeImmunitySystem
- **টক্সিসিটি ও ক্ষতিকর কোড চিহ্নিতকরণ**: জেনারেটেড কোডে ম্যালওয়্যার বা কোনো নিরাপত্তাহীন স্ক্রিপ্ট আছে কিনা চেক
- **ফিল্টারিং**: ক্ষতিকারক বা ত্রুটিযুক্ত কোড সরাসরি ব্লক করা

---

## ৪. সফটওয়্যার আর্কিটেকচার (Software Architecture)

### ৪.১ Backend (Spring Boot 3)
- **জাভা সংখ্যা**: 799+ ক্লাস
- **রিয়েস্ট কন্ট্রোলার**: 104+ এন্ডপয়েন্ট
- **প্রধান প্যাকেজ**:
  - `com.supremeai.fallback`: সহায়তা কর্মক্রম
  - `com.supremeai.service`: বিশ্লেষণ ও স্বয়ংসম্পূর্ণীকরণ
  - `com.supremeai.provider`: AI প্রোভাইডার ব্যবস্থাপনা
  - `com.supremeai.controller`: REST API ইন্টারফেস

### ৪.২ Frontend (React 18/TypeScript)
- **ড্যাশবোর্ড**: ২৬ পেজ
- **আইকন**: ১৯টি ডিজাইন মকআপ
- **এডমিন ইউআরএল**: `http://localhost:3000/admin`

### ৪.৩ Mobile (Flutter)
- **ক্রস-প্ল্যাটফর্ম**: iOS ও Android
- **বাংলা সাপোর্ট**: পুরো সিস্টেমে বাংলা ভাষা

---

## ৫. ক্ষমতা ও সীমাবদ্ধতা (Capabilities & Limitations)

### ৫.১ ক্ষমতা (Capabilities)

| ক্ষমতা | বিবরণ |
|--------|-------|
| **অ্যাপ জেনারেশন** | ন্যাচারাল ভাষায় রিকোয়েস্ট থেকে অ্যান্ড্রয়েড অ্যাপ তৈরি |
| **কোড অপ্টিমাইজেশন** | ৯৫% সিকিউরিটি ডিটেকশন, ১০০-পয়েন্ট হেল্থ স্কোরিং |
| **মাল্টি-ইউজার কোলাবোরেশন** | রিয়েল-টাইম উপস্থিতি ট্র্যাকিং |
| **স্বয়ংসম্পূর্ণীকরণ** | Self-healing, Root Cause Analysis |
| **জ্ঞান শিখন** | সিস্টেম স্বয়ংক্রিয়ভাবে নতুন সমাধান শেখে |
| **বাংলা সমর্থন** | পুরো সিস্টেমে সম্পূর্ণ বাংলা ভাষা সমর্থন |

### ৫.২ সীমাবদ্ধতা (Limitations)

| সীমাবদ্ধতা | বিবরণ |
|-----------|-------|
| **ফিচার সীমিত** | কিছু প্রিমিয়াম ড্যাশবোর্ড ফিচার ডেভেলপমেন্টাধীন |
| **টেস্টিং গ্যাপ** | ১৮/১৬০৯ টেস্ট ফেইল (Firebase এমুলেটর কানেক্টিভিটি টাইমআউট) |
| **স্বয়ংসম্পূর্ণীকরণ লুপ** | SH→RCA→GKB লার্নিং লুপ ইন্টিগ্রেশন চলমান |
| **ব্রাউজার স্ক্রেপিং** | প্লে-রাইট বেসিক ইন্টিগ্রেশন; সম্পূর্ণ ক্রলিং ফিচার বাকি |

---

## ৬. রিসাইলিয়েন্স ও সচেতনতা (Resilience & Monitoring)

### ৬.১ সোলো মোড (Solo Mode) - Solo-First Design
**ঘোষণা**: `AIFallbackOrchestrator.java:55`

**গুরুত্বপূর্ণ**: সিস্টেমটি ডিফল্টভাবে Solo-First মোডে চালু হয়। মানে হলো:
- **স্থানীয় থেকে শুরু**: প্রথমে core_knowledge.json এবং স্থানীয় জ্ঞান বেস ব্যবহার করা হয়
- **ঐচ্ছিক ক্লাউড সাহায্য**: যদি ক্লাউড প্রোভাইডার সচল থাকে এবং প্রয়োজন হয়, তবে সুযোগসন্ধানী (opportunistic) ক্লাউড সহায়তা নেওয়া হয়
- **সম্পূর্ণ অফলাইন**: ক্লাউড প্রোভাইডার ডাউন বা অনুপস্থিত থাকলে সম্পূর্ণ লোকাল আইসোলেটেড মোডে নিরবচ্ছিন্ন কাজ করা হয়

### ৬.২ Circuit Breaker কনফিগারেশন
```yaml
failureRateThreshold: 50%
waitDurationInOpenState: 30 সেকেন্ড
slidingWindowSize: 10
permittedNumberOfCallsInHalfOpenState: 3
```

### ৬.৩ P2P জ্ঞান সিঙ্ক
- **LAN সিঙ্ক**: ফায়ারস্টোর ডাউন হলে
- **UDP মাল্টিকাস্ট**: `239.255.0.1:8082`
- **সংগ্রাম**: confidenceScore + timesApplied

---

## ৭. ডিপ্লয়মেন্ট প্রক্রিয়া (Deployment Process)

### ৭.১ লোকাল ডেভেলপমেন্ট
```bash
# Backend
./gradlew bootRun

# Dashboard
cd dashboard && npm run dev
# URL: http://localhost:5173
```

### ৭.২ প্রোডাকশন
```bash
# Firebase Deploy
firebase deploy --project supremeai-a --region us-central1
```

### ৭.৩ ডকার সাইন্স
```yaml
services:
  supremeai:
    build: .
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
```

---

## ৮. জ্ঞান বেস (Knowledge Base)

### ৮.১ core_knowledge.json
- **এন্ট্রি সংখ্যা**: ৬০+ (বিভিন্ন ক্যাটাগরির)
- **ক্ভারেজ টার্গেট**: প্রতিটি ক্যাটাগরিতে কমপক্ষে ৫টি এন্ট্রি

### ৮.২ autonomous_seed_knowledge.json
- **আইটেম সংখ্যা**: ৬৫
- **ক্যাটাগরি**: APP_CREATION, ERROR_SOLVING, ARCHITECTURE, SECURITY, CI_CD, PERFORMANCE, QUOTA_POLICY, INCIDENT_LEARNING, OPERATIONS, BACKEND_SERVICES, ZERO_AI_RESILIENCE

### ৮.৩ বাধ্যতামূলক ক্যাটাগরি (Mandatory Categories)
| ক্যাটাগরি | মিনিমাম এন্ট্রি |
|----------|----------------|
| Greetings | ≥ ৫ |
| System Status | ≥ ৫ |
| Build/Compile | ≥ ৫ |
| Database | ≥ ৫ |
| Network | ≥ ৫ |
| Security | ≥ ৫ |
| AI Provider | ≥ ৫ |
| User Management | ≥ ৫ |
| API Key | ≥ ৫ |
| Error Codes | ≥ ৫ |
| Container | ≥ ৫ |
| CI/CD | ≥ ৫ |
| SSL/TLS | ≥ ৫ |
| Rate Limit | ≥ ৫ |
| Circuit Breaker | ≥ ৫ |
| Zero-AI | ≥ ১০ |
| Knowledge Bootstrap | ≥ ৫ |
| Local AI Setup | ≥ ৫ |
| P2P Sync | ≥ ৩ |

---

## ৯. সুবিধা ও অসুবিধা (Pros & Cons)

### ৯.১ সুবিধা (Advantages)

| সুবিধা | বিবরণ |
|-------|-------|
| **অফলাইন রিজিলিয়েন্স** | বন্ধ ইন্টারনেটেও কাজ করে |
| **মাল্টি-প্রোভাইডার** | একাধিক এআই মডেল সাপোর্ট |
| **স্বয়ংসম্পূর্ণীকরণ** | ত্রুটি স্বয়ংক্রিয়াভাবে ফিক্স |
| **ট্রেসেবিলিটি** | রিকোয়েস্টের পুরো ট্রেস আছে |
| **বাংলা সমর্থন** | পুরো সিস্টেমে বাংলায় ব্যবহারযোগ্য |

### ৯.২ অসুবিধা (Disadvantages)

| অসুবিধা | বিবরণ |
|--------|-------|
| **জটিল আর্কিটেকচার** | শিখতে সময় লাগে |
| **কনফিগারেশন জটিল** | প্রয়োজনীয়তা অনুযায়ী সেটআপ |
| **রিসোর্স রিকোয়ার্ড** | এআই প্রোভাইডারের জন্য |
| **ডিপ্লয়মেন্ট চ্যালেঞ্জ** | Firebase/GKE সেটআপ আবশ্যক |

---

## ১০. গুরুত্বপূর্ণ API এন্ডপয়েন্টস (Key API Endpoints)

| এন্ডপয়েন্ট | ফাংশন |
|------------|--------|
| `/api/admin/providers/health` | প্রোভাইডার হেল্থ চেক |
| `/api/admin/knowledge/health` | জ্ঞান বেস হেল্থ |
| `/api/auth/firebase-login` | ফায়ারবেস অথেন্টিকেশন |
| `/api/actuator/health` | সিস্টেম হেল্থ চেক |
| `/api/admin/providers/rankings` | প্রোভাইডার র্যাঙ্কিং |

---

## ১১. সেশন সম্পাদন ও শিখন (Session & Learning)

### ১১.১ শিখনের ধরনগুলো

1. **User Correction**: ব্যবহারকারীর সংশোধনা
2. **Error Pattern**: ত্রুটির প্যাটার্ন
3. **Successful Solution**: সফল সমাধান
4. **P2P Sync**: অন্য সিস্টেম von থেকে শিখন

### ১১.২ শিখনের ফ্লো

```
User Request
     ↓
AI Provider Response
     ↓
Co-Reasoning Optimization
     ↓
Code Immunity Check
     ↓
recordSuccessWithPermission()
     ↓
Knowledge Base Update
     ↓
System Learning Firestore
```

---

## ১২. নিরাপত্তা ব্যবস্থা (Security Framework)

### ১২.১ জেডব্লিউটি (JWT Token)
- **সম্পূর্ণ টোকেন ম্যানেজমেন্ট**
- **রোল-বেসড অ্যাক্সেস কন্ট্রোল**
- **অ্যাডমিন/এডিটর/ভিউয়ার ভূমিকা**

### ১২.২ API কি ম্যানেজমেন্ট
- **স্থানীয় রোটেশন**
- **কোটা ম্যানেজমেন্ট**
- **সেক্রেট ম্যানেজার**

---

## ১৩. সহকারী সম্পর্কে (About SupremeAI Assistant)

এই ডকুমেন্টেশনটি **SupremeAI**-এর উন্নত স্বয়ংক্রিয় এআই ইঞ্জিন দ্বারা তৈরি ও পরিশোধিত। এটি সহজেই কোড বিশ্লেষণ, নিখুঁত সমস্যা সমাধান এবং অফলাইন নলেজ উপস্থাপন করতে সক্ষম।

---

## ১৪. উপসংহার (Conclusion)

সুপ্রেম SupremeAI একটি অত্যাধুনিক এআই-চালিত সফ্টওয়্যার ইঞ্জিনিয়ারিং প্ল্যাটফর্ম। এটি ন্যাচারাল ল্যাঙ্গুয়েজে কোড লিখতে, স্বয়ংসম্পূর্ণীকরণ করতে এবং সম্পূর্ণ অফলাইনে কাজ করতে সক্ষম। Solo-First ডিজাইন নিশ্চিত করে যে সিস্টেম কখনোই অচল হয় না - ডিফল্টভাবে স্থানীয় জ্ঞান বেস ব্যবহার করে এবং প্রগতিশীলভাবে ক্লাউড সহায়তা গ্রহণ করে।

**ইউআই ব্যবহার করুন**: `http://localhost:3000/admin`