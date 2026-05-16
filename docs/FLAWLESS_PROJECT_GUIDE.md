# 🏆 SupremeAI — Flawless Project Guide
**তৈরি:** 2026-05-15 | **উদ্দেশ্য:** প্রজেক্টকে ১০০% error-free ও maintainable করা

> এই ডকুমেন্টটি সবচেয়ে গুরুত্বপূর্ণ। সব agent এবং developer এটি অনুসরণ করবে।

---

## 🚨 PART 1 — বর্তমান BUILD ERROR (এখনই ঠিক করুন)

### ❌ BUILD FAILED — `AIVotingSystem.java`

**Error:**
```
/supremeai/src/main/java/com/supremeai/ai/AIVotingSystem.java:3:
error: package com.supremeai.ai.provider does not exist
import com.supremeai.ai.provider.AIProvider;
```

**কারণ:** `ai/provider/AIProvider.java` মুছে ফেলা হয়েছে কিন্তু `AIVotingSystem` এখনও সেই package import করছে।

**সমাধান — এই লাইন পরিবর্তন করুন:**
```java
// ❌ পুরনো (ভুল)
import com.supremeai.ai.provider.AIProvider;

// ✅ নতুন (সঠিক)
import com.supremeai.provider.AIProvider;
```

**ফাইল:** `src/main/java/com/supremeai/ai/AIVotingSystem.java` লাইন ৩

**যাচাই:**
```bash
grep -rn "com.supremeai.ai.provider" src/main/java/ | grep import
```
উপরের command-এ যা বের হবে সব ফাইলে `ai.provider` → `provider` করুন।

---

## 📊 PART 2 — বর্তমান প্রজেক্টের পূর্ণ স্বাস্থ্য রিপোর্ট

| বিভাগ | অবস্থা | সমস্যা |
|-------|--------|--------|
| **Build** | ❌ FAILED | `AIVotingSystem` import error |
| **Java Files** | ⚠️ 580 files | 42 packages, কিছু duplicate |
| **Controllers** | ⚠️ 88 files | Legacy controller আছে |
| **Services** | ⚠️ 132 files | Too large, বিভক্ত করা দরকার |
| **Tests** | ⚠️ 166 files | Coverage অপর্যাপ্ত |
| **Duplicate Classes** | ❌ 3টি | AIProvider, OpenAIProvider, RateLimitingFilter |
| **WebSocket Configs** | ❌ 3টি | @EnableWebSocket conflict |
| **Missing @Document** | ❌ 4টি | HealingEvent, AIBehaviorProfile, KnowledgeEntry, ReasoningLog |
| **Missing Repository** | ❌ 2টি | ProviderTaskPerformance, AIBehaviorProfile |
| **Security Risk** | 🔴 HIGH | service-account.json root-এ আছে |
| **Dashboard** | ⚠️ 191 files | Hardcoded URL আছে |

---

## 🔧 PART 3 — Critical Fix Instructions (কোড সহ)

### FIX-01: Build Error — Import Fix

**সব wrong import খুঁজুন:**
```bash
grep -rn "com.supremeai.ai.provider" src/main/java/ | grep "^src" | cut -d: -f1 | sort -u
```

**প্রতিটি ফাইলে replace করুন:**
```bash
find src/main/java/com/supremeai -name "*.java" -exec \
  sed -i 's/import com.supremeai.ai.provider.AIProvider/import com.supremeai.provider.AIProvider/g' {} \;
```

**যাচাই:**
```bash
./gradlew compileJava 2>&1 | grep -E "error:|BUILD"
```

---

### FIX-02: Security — service-account.json

**এখনই করুন:**
```bash
# .gitignore এ যোগ করুন
echo "service-account.json" >> .gitignore
echo "*.json.bak" >> .gitignore
echo "app.jar" >> .gitignore
echo "temp_build/" >> .gitignore

# Git tracking থেকে সরান (ফাইল মুছবে না)
git rm --cached service-account.json 2>/dev/null || true
```

**environment variable ব্যবহার করুন:**
```yaml
# application.yml এ এভাবে রাখুন:
firebase:
  service-account: ${FIREBASE_SERVICE_ACCOUNT_JSON:}
```

---

### FIX-03: Duplicate Class — ai/provider/ Package

**মুছুন:**
```bash
rm src/main/java/com/supremeai/ai/provider/AIProvider.java
rm src/main/java/com/supremeai/ai/provider/OpenAIProvider.java
# ফোল্ডার যদি empty হয়:
rmdir src/main/java/com/supremeai/ai/provider/ 2>/dev/null || true
```

**তারপর import fix করুন (FIX-01 দেখুন)**

---

### FIX-04: WebSocket Config Merge

**সমস্যা:** ৩টি `@EnableWebSocket` class আছে।

**সমাধান — শুধু `WebSocketConfig.java` রাখুন:**

`websocket/AdminWebSocketConfig.java` এবং `websocket/SimulatorWebSocketConfig.java` এর `registerWebSocketHandlers()` content → `config/WebSocketConfig.java` তে merge করুন, তারপর সেই দুটি ফাইল মুছুন।

```bash
# Merge করার পরে মুছুন:
rm src/main/java/com/supremeai/websocket/AdminWebSocketConfig.java
rm src/main/java/com/supremeai/websocket/SimulatorWebSocketConfig.java
```

---

### FIX-05: HealingEvent — @Document যোগ করুন

**ফাইল:** `src/main/java/com/supremeai/model/HealingEvent.java`

```java
// এই import যোগ করুন:
import com.google.cloud.spring.data.firestore.Document;

// Class declaration এর উপরে যোগ করুন:
@Document(collectionName = "healing_events")
public class HealingEvent {
    // ... existing fields
}
```

**তারপর Repository তৈরি করুন:**
```java
// নতুন ফাইল: src/main/java/com/supremeai/repository/HealingEventRepository.java
package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.HealingEvent;
import org.springframework.stereotype.Repository;

@Repository
public interface HealingEventRepository
    extends FirestoreReactiveRepository<HealingEvent> {
}
```

---

### FIX-06: AIBehaviorProfile — @Document + Repository

**ফাইল খুঁজুন:**
```bash
find src/main/java/com/supremeai -name "AIBehaviorProfile.java"
```

**@Document যোগ করুন:**
```java
@Document(collectionName = "ai_behavior_profiles")
public class AIBehaviorProfile {
    // ...
}
```

**Repository:**
```java
package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.AIBehaviorProfile;
import org.springframework.stereotype.Repository;

@Repository
public interface AIBehaviorProfileRepository
    extends FirestoreReactiveRepository<AIBehaviorProfile> {
}
```

---

### FIX-07: ProviderTaskPerformanceRepository তৈরি করুন

```java
// নতুন ফাইল: src/main/java/com/supremeai/repository/ProviderTaskPerformanceRepository.java
package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.ProviderTaskPerformance;
import org.springframework.stereotype.Repository;

@Repository
public interface ProviderTaskPerformanceRepository
    extends FirestoreReactiveRepository<ProviderTaskPerformance> {
}
```

---

### FIX-08: Legacy UserChatController মুছুন

```bash
# প্রথমে নিশ্চিত করুন কোথাও ব্যবহার হচ্ছে না:
grep -rn "UserChatController\|chat-legacy" src/ dashboard/src/

# যদি কোথাও ব্যবহার না হয়:
rm src/main/java/com/supremeai/controller/UserChatController.java
```

---

### FIX-09: RateLimiterConfiguration Empty Class মুছুন

```bash
cat src/main/java/com/supremeai/config/RateLimiterConfiguration.java
# Empty হলে:
rm src/main/java/com/supremeai/config/RateLimiterConfiguration.java
```

---

### FIX-10: Dashboard Hardcoded URL Fix

**ফাইল:** `dashboard/src/components/RepoToPromptEngine.tsx`

```typescript
// ❌ ভুল
const API_URL = "http://localhost:8080";

// ✅ সঠিক
const API_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
```

**`dashboard/.env.example` এ যোগ করুন:**
```env
VITE_API_BASE_URL=http://localhost:8080
VITE_FIREBASE_API_KEY=your_key_here
```

---

## 🏗️ PART 4 — Ideal Architecture (লক্ষ্য কাঠামো)

```
SupremeAI Backend — Feature-Based Package Structure

com.supremeai/
├── core/              → Config, Exception, Filter, Response, Util
├── auth/              → JWT, AuthController, AuthService
├── ai/                → Provider interface, Factory, Fallback, Discovery
├── chat/              → ChatController, ChatService, ChatModel, ChatRepo
├── learning/          → LearningController, Router (একটিই), KnowledgeBase
├── orchestration/     → AgentOrchestrator, SwarmController
├── healing/           → HealingEvent(@Document✅), SelfHealingService
├── simulator/         → SimulatorController, ReverseEngineeringJob
├── browser/           → BrowserAutomation, StoredCredential
├── admin/             → AdminDashboard, AdminConfig, AdminUser
├── security/          → SecurityConfig, RateLimiter (একটিই)
└── websocket/         → WebSocketConfig (একটিই), Handlers
```

---

## ✅ PART 5 — Quality Gates (প্রতিটি PR-এর আগে চেক করুন)

### Gate 1: Build
```bash
./gradlew clean build -x test
# ✅ BUILD SUCCESSFUL হতে হবে
```

### Gate 2: Duplicate Check
```bash
find src/main/java/com/supremeai -name "*.java" -exec basename {} .java \; | sort | uniq -d
# ✅ কোনো output আসা উচিত নয়
```

### Gate 3: Endpoint Conflict Check
```bash
grep -rn "@RequestMapping" src/main/java/com/supremeai/controller/ | grep -oP '"[^"]*"' | sort | uniq -d
# ✅ কোনো output আসা উচিত নয়
```

### Gate 4: Security Check
```bash
git status | grep "service-account\|\.env\b"
# ✅ কোনো output আসা উচিত নয়
```

### Gate 5: Test
```bash
./gradlew test
# ✅ BUILD SUCCESSFUL, 0 failures
```

### Gate 6: Missing Annotation Check
```bash
grep -rL "@Document" src/main/java/com/supremeai/model/*.java | grep -v "DTO\|Enum\|enum\|UserTier"
# ✅ কোনো output আসা উচিত নয়
```

---

## 📋 PART 6 — Sprint Execution Plan

### 🔴 Sprint 0 — এখনই (আজকে)
```
[ ] FIX-01: Build error — AIVotingSystem import fix
[ ] FIX-02: service-account.json → .gitignore
[ ] FIX-03: ai/provider/ duplicate package মুছুন
[ ] ./gradlew build যাচাই করুন → BUILD SUCCESSFUL হতে হবে
```

### 🟠 Sprint 1 — এই সপ্তাহে
```
[ ] FIX-04: WebSocket config merge
[ ] FIX-05: HealingEvent @Document + Repository
[ ] FIX-06: AIBehaviorProfile @Document + Repository
[ ] FIX-07: ProviderTaskPerformanceRepository তৈরি
[ ] FIX-08: UserChatController (legacy) মুছুন
[ ] FIX-09: RateLimiterConfiguration (empty) মুছুন
[ ] Root scripts → scripts/ ফোল্ডারে সরান
```

### 🟡 Sprint 2 — পরের সপ্তাহে
```
[ ] FIX-10: Dashboard hardcoded URL fix
[ ] Service package → feature-based বিভক্ত করুন
[ ] Controller package → feature-based বিভক্ত করুন
[ ] KnowledgeEntry, ReasoningLog, ConsensusResult → @Document + Repository
[ ] ConfigService vs ConfigServiceLocal → @Primary দিয়ে fix
[ ] SelfLearningRouter + EnhancedSelfLearningRouter → একটি রাখুন
```

### 🔵 Sprint 3 — পরবর্তী ২ সপ্তাহে
```
[ ] Test coverage → 30%+ এ নিয়ে যান
[ ] E2E integration tests → 5টি critical flow
[ ] Python microservices → Cloud Run deploy
[ ] GitHub Actions CI/CD → automate
[ ] Prometheus + Grafana monitoring → setup
[ ] API documentation → OpenAPI সব endpoint
```

---

## 🚦 PART 7 — Agent Execution Rules (সর্বোচ্চ অগ্রাধিকার)

### কাজ শুরুর আগে (MANDATORY):
```
1. docs/status/MASTER_TODO.md পড়ুন
2. docs/reports/CONFLICT_AND_DUPLICATE_ANALYSIS.md পড়ুন
3. এই ফাইলের Quality Gates (PART 5) মনে রাখুন
```

### কাজের পরে (MANDATORY):
```
1. MASTER_TODO.md → [x] করুন
2. নতুন সমস্যা → "নতুন সমস্যা লগ" এ যোগ করুন
3. Gate 1 (Build) চালিয়ে যাচাই করুন
4. DB পরিবর্তন হলে → DATABASE_LINKAGE_MAP.md আপডেট করুন
```

### নিষেধ:
```
❌ BUILD FAILED রেখে কাজ complete বলবেন না
❌ Duplicate class তৈরি করবেন না
❌ Secret/credential commit করবেন না
❌ Root directory তে script রাখবেন না
❌ MASTER_TODO.md আপডেট না করে শেষ বলবেন না
```

---

## 🔗 PART 8 — Reference Documents

| ফাইল | উদ্দেশ্য |
|------|---------|
| `docs/status/MASTER_TODO.md` | সব pending কাজ |
| `docs/reports/CONFLICT_AND_DUPLICATE_ANALYSIS.md` | Duplicate & conflict list |
| `docs/DATABASE_LINKAGE_MAP.md` | DB model-collection-repository map |
| `docs/CODEBASE_ORGANIZATION_GUIDE.md` | Package structure গাইড |
| `AGENTS.md` | Agent workflow rules |

---

**মনে রাখুন:** একটি flawless project মানে:
- ✅ BUILD SUCCESSFUL সবসময়
- ✅ কোনো duplicate class নেই
- ✅ কোনো secret committed নেই
- ✅ প্রতিটি model-এ @Document আছে
- ✅ প্রতিটি repository connected
- ✅ সব endpoint documented
- ✅ Tests pass করছে

**শেষ আপডেট:** 2026-05-15
