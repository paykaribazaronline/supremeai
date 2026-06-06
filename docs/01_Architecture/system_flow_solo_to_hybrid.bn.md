# 🏗️ SupremeAI: সম্পূর্ণ সিস্টেম ফ্লো — সোলো থেকে হাইব্রিড

> **Status:** 🟢 Updated for v5 Architecture

**ভাষা:** বাংলা | **তারিখ:** ৩১ মে ২০২৬

---

## ১. সম্পূর্ণ রিকোয়েস্ট লাইফসাইকেল

যখন একজন ব্যবহারকারী কিছু জিজ্ঞেস করেন, তখন SupremeAI কীভাবে সিদ্ধান্ত নেয়:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
               ব্যবহারকারীর মেসেজ
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        ↓
         ChatController.sendMessage()
                        ↓
         AutonomousQuestioningEngine
         (প্রশ্নটি বোঝার চেষ্টা করে)
                        ↓
     ┌──────────────────────────────────┐
     │  Intent কী?                      │
     ├──────────────┬───────────────────┤
     │  GREETING    │  DIRECT_ANSWER    │  অন্যান্য
     ↓              ↓                   ↓
  সরাসরি      NeuralChatService    MultiAIVotingService
  বাংলা              ↓                   ↓
  উত্তর       [Tier 1-3 pipeline]   [0 মডেল?]
  (100%           ↓                   ↓
  offline)   উত্তর দেওয়া হয়     executeSoloFallback()
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## ২. NeuralChatService এর বিস্তারিত ফ্লো

```
NeuralChatService.generateIntelligentResponse(message)
│
├── Tier 1: findCoreKnowledge(message)
│   ├── learningOrchestrator.findCoreKnowledgeSolution()
│   │   (core_knowledge.json থেকে খোঁজে)
│   │
│   └── "What is X?" টাইপ হলে → সরাসরি রিটার্ন (scraping বাদ)
│
├── Tier 2: ActiveInternetScraper.scrapeKnowledge()
│   ├── টাইমআউট: ১২ সেকেন্ড
│   ├── ব্যর্থ হলে → খালি list → পরের স্তরে
│   └── Wikipedia, StackOverflow, MDN, GitHub, arXiv
│
├── Merge: mergeKnowledgeTiers()
│   ├── Core + Web উভয়? → MERGED (confidence 0.85-0.98)
│   ├── শুধু Core? → CORE_ONLY (confidence 0.85)
│   ├── শুধু Web? → WEB_ONLY
│   │
│   └── কিছুই না? → Tier 2.5: DuckDuckGo Instant Answer API
│       ├── সফল? → WEB_INSTANT (confidence 0.75)
│       │
│       └── ব্যর্থ? → Tier 3: StubLocalProvider.generate()
│           (25+ টপিকের rule-based উত্তর)
│           └── STUB_FALLBACK (confidence 0.5)
```

---

## ৩. MultiAIVotingService এর সোলো ফ্লো

```
MultiAIVotingService.executeEnsembleVoting(prompt)
│
├── Tier 1: learningOrchestrator.findCoreKnowledgeSolution()
│   └── পেলে → সরাসরি রিটার্ন (confidence 0.99)
│
├── decideRoutingAndKeywords(prompt)
│   └── TinyLlama/local model দিয়ে keywords ও domain বের করে
│
├── ActiveInternetScraper.scrapeKnowledge(domain, keywords)
│   └── Issues list তৈরি
│
└── executeMainFlow(prompt, issues, ...)
    │
    ├── isComplexConversation(prompt)?
    │   ├── না → executeDirectInternetCommunication()
    │   │   ├── cloud_helper চালু? → queryCloudHelper()
    │   │   └── না → executeDirectSoloSynthesis() ✅ অফলাইন
    │   │
    │   └── হ্যাঁ (complex) → activeModels.size() চেক
    │       ├── 0 মডেল → executeSoloFallback() ✅ সোলো মোড
    │       ├── 1 মডেল → executeSingleModelResilientFlow()
    │       └── 2+ মডেল → executeMultiModelVotingFlow()
    │
    └── executeSoloFallback()
        ├── playwrightResearch() [ইন্টারনেট থাকলে]
        ├── soloModeManagerService.triggerLocalModelFallback()
        └── synthesizeSoloResponse() [template-based] ✅ সবসময় কাজ করে
```

---

## ৪. AIFallbackOrchestrator এর ফ্লো

```
executeWithSupremeIntelligence(taskCategory, errorSignature, prompt)
│
├── Tier 2: knowledgeBase.findKnownSolution(errorSignature)
│   └── Firestore-এ জানা সমাধান খোঁজে
│
├── Tier 3: tryBrowserScraping(taskCategory, prompt)
│   └── ActiveInternetScraper দিয়ে ওয়েব স্ক্র্যাপিং
│
└── Tier 4: tryHelperAIProviders(taskCategory, errorSignature, prompt)
    ├── active cloud providers আছে?
    │   ├── হ্যাঁ → cloud provider কল
    │   └── না → tryPrivateCloudFailover()
    │
    └── tryPrivateCloudFailover()
        ├── Firestore-এ "airllm-sidecar" খোঁজে
        ├── পেলে → local sidecar কল
        └── না পেলে → StubLocalProvider ✅ সবসময় পাওয়া যায়
```

---

## ৫. কোন মোডে কী পাওয়া যায়?

| পরিস্থিতি                    | গ্রিটিং | সহজ প্রশ্ন   | কোডিং প্রশ্ন    | জটিল প্রশ্ন     |
| ---------------------------- | ------- | ------------ | --------------- | --------------- |
| **সম্পূর্ণ অফলাইন**          | ✅      | ✅ Core      | ✅ Stub         | 🟡 Template     |
| **ইন্টারনেট আছে, AI নেই**    | ✅      | ✅ Web+Core  | ✅ Web+Stub     | 🟡 Web+Template |
| **১টি AI আছে**               | ✅      | ✅ AI        | ✅ AI+Web       | ✅ AI Compared  |
| **হাইব্রিড (SuperFly চালু)** | ✅      | ✅ On-device | ✅ ChickenBrain | ✅ Pocket Lab   |
| **সম্পূর্ণ ক্লাউড**          | ✅      | ✅           | ✅              | ✅ Multi-vote   |

---

## ৬. সোলো মোডে স্বয়ংক্রিয় শিক্ষা (Auto-Learning)

```
ব্যবহারকারীর প্রশ্ন → সোলো উত্তর
        ↓
enhancedLearningService.learnFromNLPInteraction()
        ↓
Firestore "system_learning" collection-এ সেভ
        ↓
পরবর্তীবার → knowledgeBase.findKnownSolution() এ পাওয়া যাবে
        ↓
[আস্তে আস্তে সিস্টেম স্মার্ট হয়]
```

---

## ৭. Self-Healing সোলো মোডে

```
কম্পোনেন্ট ব্যর্থ হলে:
│
├── প্রথমবার → Smart Retry (ভিন্ন CSS selector)
├── দ্বিতীয়বার → পুনরায় retry
├── তৃতীয়বার → Domain Quarantine (৫ মিনিট)
│   └── Fallback: Jsoup scraper বা cached data
│
└── Circuit Breaker OPEN হলে:
    ├── Provider Skip → পরের provider-এ
    └── সব provider fail → StubLocalProvider
```

---

## ৮. হাইব্রিড মোডে সোলো মোডের ভূমিকা

নতুন হাইব্রিড আর্কিটেকচারে সোলো মোড আর শুধু "শেষ অবলম্বন" নয়, এটি **প্রথম পছন্দ**:

```
হাইব্রিড পাইপলাইন (docs/latest_plan/hybrid_nano_cloud_architecture.bn.md):

Level 0: SuperFly 94M (on-device) → গ্রিটিং, সহজ প্রশ্ন [অফলাইন]
    ↓ (ব্যর্থ হলে)
Level 1: Core Knowledge + RAG [অফলাইন]
    ↓ (similarity < 0.70)
Level 2+3: ChickenBrain (Cloud VM) + Browser [ইন্টারনেট]
    ↓ (ব্যর্থ হলে)
Level 4: Pocket Lab Edge Node [প্রিমিয়াম]
    ↓ (ব্যর্থ হলে)
Level 4 Alt: Multi-AI Voting Council [ক্লাউড]
    ↓ (সবকিছু ব্যর্থ হলে)
StubLocalProvider → সবসময় উত্তর দেয় [চূড়ান্ত অফলাইন]
```

---

## ৯. ডেটা ফ্লো ডায়াগ্রাম (Data Flow)

```
ব্যবহারকারীর ইনপুট
        ↓
[ChatController] ← Security Filter (JWT Auth)
        ↓
[AutonomousQuestioningEngine] — Intent, Clarity Score
        ↓
    ┌───────────────────────────────────────────┐
    │           জ্ঞান উৎস (Knowledge Sources)   │
    ├──────────┬────────────┬───────────────────┤
    │ Local    │ Internet   │ AI Models         │
    │ core_    │ Browser    │ Groq, Gemini      │
    │ knowledge│ Playwright │ Local Sidecar     │
    │ .json    │ DuckDuckGo │ SuperFly (future) │
    └──────────┴────────────┴───────────────────┘
        ↓
[EnhancedLearningService] — শিখে Firestore-এ সেভ
        ↓
    উত্তর ব্যবহারকারীকে দেওয়া হয়
```

---

## ১০. সিস্টেম স্বাস্থ্য পর্যবেক্ষণ (Health Monitoring)

```
GET /api/chat/health → সিস্টেম স্ট্যাটাস

{
  "status": "UP",
  "soloMode": false,          // AIFallbackOrchestrator থেকে
  "activeProviders": 3,       // Firestore থেকে
  "coreKnowledgeLoaded": true, // core_knowledge.json
  "browserService": "UP",     // Playwright sidecar
  "autonomous_questioning": "ACTIVE",
  "voting_system": "ACTIVE"
}
```

---

## উপসংহার

SupremeAI-এর আর্কিটেকচার **Defense-in-Depth** নীতিতে তৈরি:

1. **প্রথম লাইন:** On-device (SuperFly) — ভবিষ্যৎ
2. **দ্বিতীয় লাইন:** Local Knowledge + Rules — বর্তমানে কার্যকর
3. **তৃতীয় লাইন:** Browser + Internet — কার্যকর
4. **চতুর্থ লাইন:** Cloud AI + Voting — কার্যকর
5. **চূড়ান্ত লাইন:** StubLocalProvider — সবসময় কার্যকর

> **সত্য:** সোলো মোডে সিস্টেম কখনো "সম্পূর্ণ ব্যর্থ" হয় না — সবচেয়ে খারাপ অবস্থায়ও StubLocalProvider থেকে একটি সহায়ক উত্তর পাওয়া যাবে।
