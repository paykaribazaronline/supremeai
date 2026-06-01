# 🔧 SupremeAI: চিহ্নিত সমস্যা ও সমাধান পরিকল্পনা
**ভাষা:** বাংলা | **তারিখ:** ৩১ মে ২০২৬

---

## ১. ব্লকিং কল সমস্যা (`.block()` Problem)

### সমস্যার বিবরণ
Spring WebFlux (Project Reactor) একটি **non-blocking** reactive framework। এতে event loop thread-এ `.block()` ব্যবহার করলে পুরো সার্ভার হ্যাং হতে পারে।

### কোথায় সমস্যা আছে?

| ফাইল | লাইন | সমস্যাযুক্ত কোড |
|---|---|---|
| `NeuralChatService.java` | 205 | `stubLocalProvider.generate(msg).block()` |
| `AIFallbackOrchestrator.java` | 352 | `providerRepository.findById(...).block()` |
| `AIFallbackOrchestrator.java` | 361 | `.collectList().block()` |
| `AIFallbackOrchestrator.java` | 379 | `provider.generate(prompt).block()` |
| `AIFallbackOrchestrator.java` | 388 | `stubLocalProvider.generate(...).block()` |
| `NeuralChatService.java` | 425 | `webClient...bodyToMono().block()` (DDG API) |

### কীভাবে ঠিক করবেন?

#### ক) `NeuralChatService.java` — StubLocal Fallback
```java
// ❌ আগের কোড (Line 203-206)
log.info("[NeuralChat] Falling back to Tier 3 (StubLocal)");
String stubResponse = stubLocalProvider.generate(userMessage).block();
return NeuralResponse.fromStub(stubResponse, "stub_local_fallback");

// ✅ ঠিক করা কোড
log.info("[NeuralChat] Falling back to Tier 3 (StubLocal)");
return stubLocalProvider.generate(userMessage)
    .map(stubResponse -> NeuralResponse.fromStub(stubResponse, "stub_local_fallback"))
    .defaultIfEmpty(NeuralResponse.fromStub(
        "আমি লোকাল মোডে সক্রিয়।", "stub_empty_fallback"
    ));
```

#### খ) `AIFallbackOrchestrator.java` — tryPrivateCloudFailover()
```java
// ❌ আগের কোড (Blocking DB lookup)
airllmConfig = providerRepository.findById(fallbackProviderName)
    .block(Duration.ofSeconds(2));  // ← ব্লকিং

// ✅ ঠিক করা কোড (Reactive DB lookup)
return providerRepository.findById(fallbackProviderName)
    .switchIfEmpty(providerRepository.findById(fallbackProviderName.toLowerCase()))
    .defaultIfEmpty(createDefaultSidecarConfig())  // null-safe
    .flatMap(config -> {
        AIProvider provider = providerFactory.createProviderFromConfig(config);
        return provider != null 
            ? provider.generate(prompt) 
            : stubLocalProvider.generate(prompt);
    })
    .onErrorResume(e -> stubLocalProvider.generate("Offline: " + prompt));
```

---

## ২. দ্বৈত API এন্ডপয়েন্ট সমস্যা (Dual Endpoint Problem)

### সমস্যার বিবরণ
`ChatController.java`-এ দুটো এন্ডপয়েন্ট প্রায় একই কাজ করে:

```
POST /api/chat/send    → executeResponseForRequest() → NeuralChat + Voting
POST /api/chat/message → processChatWithHistory()   → History + Voting + NeuralChat
```

**পার্থক্য:** `/message` এ chat history সেভ হয়, `/send`-এ হয় না।

### সমাধান
```java
// ChatController.java এ এই অ্যানোটেশন যোগ করুন
@Deprecated(since = "2.0", forRemoval = true)
@PostMapping("/send")
public Mono<ResponseEntity<Object>> sendMessage(...) {
    // এই এন্ডপয়েন্টটি /message-এ রিডাইরেক্ট করুন
    return handleChatMessage(request);
}
```

---

## ৩. soloMode Flag রিয়েল-টাইম আপডেট সমস্যা

### সমস্যার বিবরণ
```java
// AIFallbackOrchestrator.java, Line 61
private volatile boolean soloMode = false;
```

এই flag স্টার্টআপে একবার সেট হয়। রানটাইমে Firestore-এ নতুন প্রোভাইডার যোগ করলে flag আপডেট হয় না।

### সমাধান: ScheduledRefresh যোগ করুন
```java
// AIFallbackOrchestrator.java-এ যোগ করুন
@Scheduled(fixedDelay = 60000) // প্রতি ১ মিনিটে চেক
public void refreshSoloModeStatus() {
    providerRepository.findByStatus("active")
        .count()
        .subscribe(count -> {
            boolean newSoloMode = (count == 0);
            if (newSoloMode != soloMode) {
                soloMode = newSoloMode;
                log.info("[SoloMode] Status changed to: {}", soloMode ? "SOLO" : "CONNECTED");
            }
        });
}
```

---

## ৪. Core Knowledge কেন্দ্রীভূত করার পরিকল্পনা

### বর্তমান অবস্থা (বিক্ষিপ্ত)
```
NeuralChatService.java          → learningOrchestrator.findCoreKnowledgeSolution()
MultiAIVotingService.java       → learningOrchestrator.findCoreKnowledgeSolution()
StubLocalProvider.java          → নিজস্ব KNOWLEDGE_BASE Map
AIFallbackOrchestrator.java     → GlobalKnowledgeBase
```

### প্রস্তাবিত কেন্দ্রীয় সার্ভিস
```java
@Service
public class UnifiedOfflineKnowledgeService {

    private final SupremeLearningOrchestrator learningOrchestrator;
    private final StubLocalProvider stubLocalProvider;
    
    /**
     * সব স্তরের অফলাইন জ্ঞান একই জায়গা থেকে সরবরাহ করে
     */
    public Mono<String> findAnswer(String query) {
        // স্তর ১: core_knowledge.json
        return Mono.fromCallable(() -> learningOrchestrator.findCoreKnowledgeSolution(query))
            .filter(ans -> ans != null && !ans.isEmpty())
            // স্তর ২: StubLocalProvider (rule-based)
            .switchIfEmpty(stubLocalProvider.generate(query));
    }
}
```

---

## ৫. Intelligence Gap → Auto-Save পরিকল্পনা

**ডকুমেন্ট রেফারেন্স:** `self_healing_and_improvement_architecture.md`, Section ১.৪

### কীভাবে কাজ করবে?
যখন কোনো কোয়ারেন্টাইন (Circuit Breaker Open) হয়, সিস্টেম সেই ব্যর্থ টাস্কটি `core_knowledge.json`-এ "Intelligence Gap" হিসেবে সেভ করবে।

```java
// AIFallbackOrchestrator.java-এ যোগ করুন
private void recordIntelligenceGap(String taskCategory, String prompt, String failReason) {
    Map<String, Object> gap = new HashMap<>();
    gap.put("category", taskCategory);
    gap.put("prompt", prompt);
    gap.put("failReason", failReason);
    gap.put("timestamp", Instant.now().toString());
    gap.put("status", "UNRESOLVED");
    
    // Firestore-এ intelligence_gaps collection-এ সেভ করুন
    knowledgeBase.saveIntelligenceGap(gap).subscribe(
        saved -> log.info("[Intelligence Gap] Recorded for future training: {}", taskCategory),
        err -> log.warn("[Intelligence Gap] Failed to record gap: {}", err.getMessage())
    );
}
```

---

## ৬. SuperFly 94M ইন্টিগ্রেশন রোডম্যাপ

**রেফারেন্স:** `hybrid_nano_cloud_architecture.bn.md`

### ধাপ ১: নতুন AIProvider ইন্টারফেস ইমপ্লিমেন্ট করুন
```java
@Component("superfly")
public class SuperFlyProvider implements AIProvider {

    private final WebClient localClient;

    public SuperFlyProvider() {
        // SuperFly sidecar port: 8082 (প্রস্তাবিত)
        this.localClient = WebClient.create("http://localhost:8082");
    }

    @Override
    public Mono<String> generate(String prompt) {
        return localClient.post()
            .uri("/generate")
            .bodyValue(Map.of(
                "prompt", prompt,
                "max_tokens", 256,
                "model", "superfly-94m"
            ))
            .retrieve()
            .bodyToMono(Map.class)
            .map(res -> (String) res.get("text"))
            .timeout(Duration.ofSeconds(5))  // 94M মডেল দ্রুত হওয়া উচিত
            .onErrorResume(e -> Mono.empty());
    }

    @Override
    public String getName() { return "superfly"; }
}
```

### ধাপ ২: Firestore-এ SuperFly Provider যোগ করুন
```json
{
  "name": "superfly",
  "type": "superfly",
  "baseUrl": "http://localhost:8082",
  "status": "active",
  "isLocalOnly": true,
  "canParticipateInVoting": false,
  "description": "On-device 94M parameter model for greeting/simple queries"
}
```

### ধাপ ৩: ChatController Level 0-এ SuperFly ব্যবহার করুন
```java
// ChatController.java - Greeting bypass-এ
if (validation.getIntentType() == IntentType.GREETING) {
    return superFlyProvider.generate(message)
        .map(answer -> buildResponse(answer, "SUPERFLY_ON_DEVICE"))
        .onErrorResume(e -> Mono.just(buildStaticGreetingResponse()));
}
```

---

## ৭. সার্বিক উন্নতির টাইমলাইন

| অগ্রাধিকার | কাজ | জটিলতা | আনুমানিক সময় |
|---|---|---|---|
| 🔴 Critical | `.block()` সরানো | মাঝারি | ১-২ দিন |
| 🔴 Critical | `/send` vs `/message` দ্বৈততা | সহজ | ৩ ঘণ্টা |
| 🟡 High | soloMode realtime refresh | মাঝারি | ৪ ঘণ্টা |
| 🟡 High | UnifiedOfflineKnowledgeService | মাঝারি | ১ দিন |
| 🟢 Medium | Intelligence Gap auto-save | মাঝারি | ১ দিন |
| 🔵 Future | SuperFly 94M integration | কঠিন | ৩-৫ দিন |
| 🔵 Future | ChickenBrain URL Generator | কঠিন | ৩-৫ দিন |

---

> **গুরুত্বপূর্ণ নোট:** `.block()` সমস্যাটি সর্বোচ্চ অগ্রাধিকার পাওয়া উচিত কারণ এটি প্রোডাকশনে সার্ভার ক্র্যাশ ঘটাতে পারে। বাকি সব উন্নতি সেটি ঠিক করার পরে করা উচিত।
