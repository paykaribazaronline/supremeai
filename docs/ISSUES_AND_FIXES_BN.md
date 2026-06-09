# 🔧 SupremeAI — সমস্যা ও সমাধান গাইড (বাংলা)

> **তারিখ:** ৭ জুন, ২০২৬  
> **উদ্দেশ্য:** প্রজেক্টের বর্তমান সমস্যাগুলো চিহ্নিত করে সুনির্দিষ্ট সমাধান দেওয়া

---

## 🚨 সমস্যা #১ — JVM ক্র্যাশ লগ (সবচেয়ে জরুরি!)

**ফাইল:** `hs_err_pid4172.log`, `hs_err_pid5028.log`, `hs_err_pid7248.log` ইত্যাদি (৫টি ক্র্যাশ লগ!)

**কী হয়েছে:** Java Virtual Machine বারবার ক্র্যাশ হয়েছে। এটি মানে প্রোডাকশনে সার্ভার হঠাৎ বন্ধ হয়ে যাওয়ার ঝুঁকি আছে।

**কারণ হতে পারে:**
- Out of Memory (RAM শেষ)
- Playwright browser threads lock করে ফেলছে
- Reactive WebFlux + blocking call conflict

**সমাধান:**
```bash
# app.jar চালানোর সময় এই JVM flags দিন
java -Xmx2g -Xms512m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -jar app.jar
```

**চেক করুন:** `hs_err_pid7248.log` খুলে "SIGSEGV" বা "OutOfMemoryError" খুঁজুন।

---

## 🚨 সমস্যা #২ — SoloModeService.java প্রায় ফাঁকা!

**ফাইল:** `src/main/java/com/supremeai/service/SoloModeService.java` — মাত্র **১২১ বাইট**!

**কী হয়েছে:** Solo/Offline Mode-এর জন্য একটি ক্লাস তৈরি হয়েছে, কিন্তু ভেতরে কোনো লজিক নেই।

**প্রভাব:** ইন্টারনেট না থাকলে সিস্টেম সম্পূর্ণ অচল হয়ে যাবে।

**সমাধান — যা থাকা উচিত:**
```java
@Service
public class SoloModeService {
    
    // ১. হার্ডওয়্যার চেক করা
    public HardwareInfo detectHardware() {
        long ram = Runtime.getRuntime().maxMemory();
        int cpuCores = Runtime.getRuntime().availableProcessors();
        return new HardwareInfo(ram, cpuCores);
    }
    
    // ২. সঠিক লোকাল মডেল বেছে নেওয়া
    public String selectLocalModel(HardwareInfo hw) {
        if (hw.ramGB() >= 8) return "llama3:8b";
        if (hw.ramGB() >= 4) return "phi3:mini";
        return "tinyllama:1b"; // সবচেয়ে হালকা
    }
    
    // ৩. Ollama API কল
    public String askLocalModel(String prompt, String model) {
        // Ollama local API: http://localhost:11434
    }
}
```

---

## 🚨 সমস্যা #৩ — Intent Classification দুর্বল

**ফাইল:** `SupremeAIBrain.java` (২৯ KB)

**কী হয়েছে:** ব্যবহারকারীর প্রশ্ন বোঝার জন্য এরকম কোড আছে:
```java
// ❌ খুব সরল — এটি "complex" শব্দ দেখলেই জটিল মনে করে!
if (prompt.contains("complex") || prompt.contains("analyze")) {
    return TaskCategory.COMPLEX;
}
```

**প্রভাব:** "আমার জন্য একটু complex ব্যাখ্যা করো" বললে ভুল রাউট হয়।

**সমাধান:**
```java
// ✅ Embedding ব্যবহার করুন
// ১. OpenAI text-embedding-ada-002 দিয়ে প্রম্পটকে vector বানান
// ২. কাছের জানা category vector এর সাথে cosine similarity মাপুন
// ৩. সবচেয়ে কাছের category বেছে নিন
float[] promptVector = embeddingService.embed(prompt);
return classifier.findNearest(promptVector);
```

---

## 🚨 সমস্যা #৪ — টেস্ট কভারেজ মাত্র ৩১%

**ফাইল:** `build.gradle.kts` — `jacoco.line.minimum=0.00` (কোনো minimum নেই!)

**কী হয়েছে:** কোডের ৬৯% কখনো টেস্ট হয়নি। `MultiAIConsensusServiceTest`-এ ৫টি টেস্ট ফেইল করছে।

**সবচেয়ে ঝুঁকিপূর্ণ ক্লাসগুলো (টেস্ট নেই):**
- `SupremeAIBrain.java` — মূল রাউটিং লজিক
- `NeuralChatService.java` — চ্যাট প্রসেসিং
- `SelfHealingService.java` — self-healing লুপ

**সমাধান:**
```bash
# ফেইলিং টেস্ট আগে ঠিক করুন
./gradlew test --tests "*MultiAIConsensusServiceTest*"

# তারপর coverage রিপোর্ট দেখুন
./gradlew test -PrunCoverage=true jacocoTestReport

# টার্গেট: minimum 50% এ নিয়ে যান
# gradle.properties এ লিখুন:
jacoco.line.minimum=0.50
```

---

## 🚨 সমস্যা #৫ — Simulator (Plan 22) সম্পূর্ণ নেই

**ফাইল:** `SimulatorService.java`, `SimulatorDeploymentService.java`

**কী হয়েছে:** সিমুলেটরের ব্যাকএন্ড কোড আছে, কিন্তু ফ্রন্টেন্ডে লাইভ প্রিভিউ নেই। ইউজার কোড জেনারেট করলে দেখতে পায় না।

**সমাধান — সহজ পদ্ধতি:**
```typescript
// dashboard/src/components/SimulatorPreview.tsx
// একটি iframe দিয়ে Cloud Run URL লোড করুন
const SimulatorPreview = ({ deployedUrl }) => (
  <iframe 
    src={deployedUrl}
    className="w-full h-screen border rounded-lg"
    sandbox="allow-scripts allow-same-origin"
  />
);
```

---

## 🚨 সমস্যা #৬ — Context Window Overflow

**ফাইল:** `NeuralChatService.java`, `ChickenBrain` মার্জার লজিক

**কী হয়েছে:** ব্রাউজার থেকে বড় পেইজ scrape করলে সম্পূর্ণ টেক্সট AI-তে পাঠানো হয়। এতে:
- Token limit অতিক্রম হয়
- প্রতি রিকোয়েস্টে অনেক বেশি খরচ হয়

**সমাধান:**
```java
// সর্বোচ্চ ৮০০০ token রাখুন
public String truncateForAI(String content, int maxTokens) {
    // প্রতি ৪ অক্ষর ≈ ১ token
    int maxChars = maxTokens * 4;
    if (content.length() <= maxChars) return content;
    
    // শুরু এবং শেষ রেখে মাঝের অংশ কাটুন
    String start = content.substring(0, maxChars / 2);
    String end = content.substring(content.length() - maxChars / 2);
    return start + "\n...[সংক্ষিপ্ত করা হয়েছে]...\n" + end;
}
```

---

## 🚨 সমস্যা #৭ — Plan 18 নিরাপত্তা ঝুঁকি

**সমস্যা:** অন্যদের API Key ব্যবহার করার পরিকল্পনা।

**ঝুঁকি:**
- OpenAI/Google-এর Terms of Service লঙ্ঘন
- আইনগত সমস্যা হতে পারে
- ব্যবহারকারীর বিশ্বাস হারাতে পারে

**সমাধান:**
```bash
# Ollama ইনস্টল করুন (ফ্রি, লোকাল)
curl -fsSL https://ollama.ai/install.sh | sh

# Llama 3 ডাউনলোড করুন (৪.৭ GB)
ollama pull llama3:8b

# API: http://localhost:11434/api/generate
```

---

## 📊 সমস্যার স্ট্যাটাস ও অগ্রাধিকার তালিকা

| # | সমস্যা | গুরুত্ব | জটিলতা | স্ট্যাটাস |
|---|--------|---------|---------|-----------|
| ১ | JVM ক্র্যাশ | 🔴 Critical | মাঝারি | ✅ সম্পন্ন (Finished) |
| ২ | SoloModeService ফাঁকা | 🔴 High | বেশি | ✅ সম্পন্ন (Finished) |
| ৩ | ফেইলিং টেস্ট | 🟠 High | কম | ✅ সম্পন্ন (Finished) |
| ৪ | Intent Classification | 🟠 Medium | বেশি | ⏳ পরিকল্পিত (Planned) |
| ৫ | Simulator Preview | 🟡 Medium | মাঝারি | ✅ সম্পন্ন (Finished) |
| ৬ | Context Overflow | 🟡 Medium | কম | ✅ সম্পন্ন (Finished) |
| ৭ | Plan 18 ঝুঁকি | 🔴 High | বেশি | ⏳ পরিকল্পিত (Planned) |

---

*এই গাইড সরাসরি সোর্স কোড বিশ্লেষণ করে তৈরি করা হয়েছে।*
