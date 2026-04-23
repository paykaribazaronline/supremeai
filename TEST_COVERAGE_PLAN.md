# SupremeAI Test Coverage Improvement Plan

**POTENTIALLY OUTDATED - Created March 2024**

এই ডকুমেন্টটি SupremeAI প্রোজেক্টের টেস্ট কভারেজ ১০% থেকে বাড়িয়ে লক্ষ্যমাত্রায় (৮০%+) পৌঁছানোর জন্য একটি রোডম্যাপ হিসেবে কাজ করবে।

**Note:** This document is from March 2024. For current testing status and coverage, refer to:

- PROJECT_ANALYSIS_REPORT.md (April 2026 analysis shows many empty test files)
- Recent code changes and implemented tests

The project has evolved significantly since this plan was created. Current testing infrastructure may differ.

## ১. বর্তমান অবস্থা (Analysis)

- **মোট প্যাকেজ সংখ্যা:** ২০+ (Backend)
- **বর্তমান কভারেজ:** < ১০% (সম্ভাব্য)
- **প্রধান গ্যাপ:** ইউনিট টেস্টের অভাব, বেশিরভাগই ইন্টিগ্রেশন টেস্ট।

## ২. অগ্রাধিকার তালিকা (Priority List)

### ধাপ ১: কোর লজিক (Service & ML Layer) - উচ্চ অগ্রাধিকার

- `com.supremeai.service.*`: বিজনেস লজিক যাচাই করা।
- `com.supremeai.ml.*`: এআই মডেলের ইনপুট/আউটপুট প্রসেসিং যাচাই করা।
- `com.supremeai.agent.*`: এজেন্টদের কাজ সঠিকভাবে হচ্ছে কি না তা চেক করা।

### ধাপ ২: সিকিউরিটি (Security Layer)

- `SecurityConfig` এর জন্য মক টেস্ট।
- Firebase Authentication এবং Role-based অথরাইজেশন লজিক চেক করা।

### ধাপ ৩: এপিআই এন্ডপয়েন্ট (Controller Layer)

- `MockMvc` ব্যবহার করে সব কন্ট্রোলারের এন্ডপয়েন্ট টেস্ট করা।
- সঠিক HTTP স্ট্যাটাস কোড এবং JSON রেসপন্স নিশ্চিত করা।

### ধাপ ৪: ফ্রন্টেন্ড (Flutter)

- **Widget Testing:** প্রধান ইউআই এলিমেন্টগুলো চেক করা।
- **Unit Testing:** রিডাক্স/ব্লক বা প্রোভাইডার লজিক টেস্ট করা।

## ৩. প্রযুক্তিগত টুলস (Tech Stack for Testing)

- **JUnit 5:** প্রধান টেস্টিং ফ্রেমওয়ার্ক।
- **Mockito:** মকিং করার জন্য।
- **Jacoco:** কভারেজ রিপোর্ট জেনারেট করার জন্য।
- **Flutter Test:** ফ্ল্যাটার অ্যাপের জন্য।

## ৪. লক্ষ্যমাত্রা (Milestones)

- **মাইলস্টোন ১:** ২৫% কভারেজ (কোর সার্ভিসগুলো কভার করা)।
- **মাইলস্টোন ২:** ৫০% কভারেজ (কন্ট্রোলার এবং সিকিউরিটি কভার করা)।
- **মাইলস্টোন ৩:** ৮০%+ কভারেজ (পুরো প্রোজেক্ট)।

## ৫. অগ্রগতি (Progress Tracking)

### [✅] সম্পন্ন কাজগুলো

- **Backend:**
  - `QuotaServiceTest.java` - ইউনিট টেস্ট যোগ করা হয়েছে (Quota Logic)।
  - `ExpertAgentRouterTest.java` - স্ট্রাকচার এবং বেসিক মক টেস্ট যোগ করা হয়েছে।
  - `AuthenticationControllerTest.java` - Firebase Login and User Creation টেস্ট যোগ করা হয়েছে।
  - `IsolationForestTest.java` - Anomaly detection logic validated (100% coverage).
  - `RandomForestFailurePredictorTest.java` - Failure prediction logic validated (100% coverage).
  - `RequirementAnalyzerAITest.java` - AI parsing and fallback scenarios verified (Dynamic orchestration validated).
- **Frontend (Flutter):**
  - `widget_test.dart` - বেসিক অ্যাপ স্টার্ট এবং স্ক্রিন ভেরিফিকেশন টেস্ট আপডেট করা হয়েছে।
  - `auth_provider_test.dart` - AuthProvider এর স্টেট ম্যানেজমেন্ট এবং SharedPreferences টেস্ট যোগ করা হয়েছে।
  - `orchestration_provider_test.dart` - Added tests for requirement submission and project generation flow.
  - **Synchronization:** All primary screens (VPN, Resilience, Git Ops, Dashboard) are implemented and integrated into the main navigation flow.
- **IntelliJ Plugin:**
  - Enhanced `SupremeAIToolWindowFactory.kt` with a structured `JBTable` for AI orchestration decisions, moving away from raw text representation.

### [ ] পরবর্তী লক্ষ্য

- `com.supremeai.ml` প্যাকেজের কোর অ্যালগরিদমগুলোর জন্য টেস্ট লেখা।
- `com.supremeai.controller` লেভেলের এন্ডপয়েন্ট টেস্ট শুরু করা।

## ৬. বিস্তারিত TODO লিস্ট (Upcoming Tasks)

### Backend (Java)

- [x] **ML Logic:** `IsolationForest.java` এবং `RandomForestFailurePredictor.java` এর জন্য ইউনিট টেস্ট (Logic Implemented).
- [x] **Orchestration:** `AdaptiveAgentOrchestrator` এর AI-driven প্রশ্ন জেনারেট করার লজিক ভেরিফাই করা (`RequirementAnalyzerAITest`).
- [ ] **Security:** `JwtAuthenticationFilter` এর জন্য মক টেস্ট।
- [ ] **Agent Orchestration:** `AdaptiveAgentOrchestrator.java` এবং `ExpertAgentRouter.java` এর পূর্ণাঙ্গ ইমপ্লিমেন্টেশন টেস্ট।
- [ ] **API Endpoints:** `AgentOrchestrationController` এবং অন্যান্য কন্ট্রোলারের জন্য `MockMvc` টেস্ট।
- [ ] **Services:** `QuotaService` এর রিমেইনিং মেথডগুলোর এজ-কেস টেস্ট।

### Frontend (Flutter)

- [ ] **Auth Flow:** `LoginScreen` এর জন্য ডিটেইলড উইজেট টেস্ট (Success/Failure scenarios)।
- [ ] **State Management:** `AuthProvider` এর জন্য ইউনিট টেস্ট।
- [ ] **Navigation:** এক স্ক্রিন থেকে অন্য স্ক্রিনে যাওয়ার লজিক চেক করা।

## ৭. কিভাবে শুরু করবেন?

১. প্রতিটি নতুন ফিচারের সাথে অবশ্যই একটি ইউনিট টেস্ট ফাইল যোগ করুন।
২. পুরানো কোড রিফ্যাক্টর করার সময় টেস্ট যোগ করুন।
৩. `./gradlew jacocoTestReport` রান করে প্রতি সপ্তাহে প্রগ্রেস চেক করুন।

---
*Created on: March 2024*
