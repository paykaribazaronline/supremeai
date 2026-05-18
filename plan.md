# SupremeAI — Multi-Orchestrated Cloud AI Migration Plan

## মূল নীতি
> **শুধুমাত্র multi-type orchestrated AI models ব্যবহার হবে। কোনো single AI model default হিসেবে থাকবে না।**

## বর্তমান সমস্যা (যা ঠিক করতে হবে)

### 1. Hardcoded Fallback Chains
- `GracefulDegradationService`: `local → groq → openai → anthropic` — hardcoded
- `MultiAIVotingService.performMetaSynthesis()`: `gemini → openai` — hardcoded
- `SuperHubOrchestrator`: `dev_hub`, `lang_hub` — hardcoded hub IDs

### 2. Hardcoded Provider Switch/Case
- `AIProviderFactory.getProvider()`: 12+ switch/case branches for individual providers
- প্রতিটি provider আলাদাভাবে instantiate হচ্ছে

### 3. Hardcoded Provider Arrays
- `MultiAIVotingService.ALL_PROVIDERS`, `DEFAULT_PROVIDERS` — empty but present
- `GracefulDegradationService.PROVIDER_FALLBACK_CHAIN` — hardcoded list

### 4. Local-First Fallback
- `SupremeCoreProvider`: helper AI না থাকলে local seed knowledge ব্যবহার করে
- `OLLAMA` default fallback হিসেবে আছে

## নতুন আর্কিটেকচার

### Cloud-Only Multi-Orchestrated Models
```
┌─────────────────────────────────────────────────────────┐
│                  SupremeAI Super-Hub                     │
│                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │  gcp_qwen    │  │  gcp_llama   │  │  gcp_phi    │     │
│  │  (Code)      │  │  (Chat)      │  │  (Fast)     │     │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘     │
│         │                │                │             │
│  ┌──────┴──────┐  ┌──────┴──────┐  ┌──────┴──────┐     │
│  │  gcp_nomic   │  │ hf_deepseek  │  │  (more...)  │     │
│  │  (Embed)     │  │  (Review)    │  │             │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
│                                                         │
│  ALL providers loaded from Firestore api_providers      │
│  NO hardcoded single-AI fallback                        │
└─────────────────────────────────────────────────────────┘
```

### Provider Resolution Chain
1. **Firestore `api_providers`** → dynamic config (baseUrl, model, apiKey, priority, roles)
2. **Firestore `provider_types`** → type-level defaults
3. **SupremeCloudProvider** → generic cloud provider for ALL cloud models
4. **NO local/Ollama fallback** — cloud-only

## কাজের তালিকা

### Phase 1: Hardcoded Fallback সরানো ✅
- [x] `GracefulDegradationService` — hardcoded `PROVIDER_FALLBACK_CHAIN` সরিয়ে Firestore থেকে dynamic load
- [x] `MultiAIVotingService.performMetaSynthesis()` — `gemini → openai` hardcoded সরানো, dynamic provider selection
- [x] `SuperHubOrchestrator` — hardcoded hub IDs সরানো, dynamic cloud provider failover

### Phase 2: AIProvider Factory সম্পূর্ণ Dynamic করা ✅
- [x] `switch/case` সরিয়ে সব provider কে `SupremeCloudProvider` দিয়ে resolve করা
- [x] Firestore থেকে provider config load করা unknown provider-এর জন্য
- [x] `AIProviderType` enum — cloud-only types (CLOUD_QWEN, CLOUD_LLAMA, CLOUD_DEEPSEEK, CLOUD_PHI, CLOUD_NOMIC, CLOUD_CUSTOM)

### Phase 3: Core Knowledge আপডেট ✅
- [x] `core_knowledge.json` — cloud-only model registry আপডেট
- [x] `operational_constraints.provider_policy` — cloud-only, no-local-fallback, auto-failover

### Phase 4: Configuration আপডেট ✅
- [x] `application.properties` — Ollama/local references disabled, cloud-only mode flag
- [x] `ProviderConfig` — hardcoded provider beans সরিয়ে শুধু `SupremeCoreProvider` bean
- [x] `SupremeCoreProvider` — local seed/memory fallback সরিয়ে cloud-only orchestration

### Phase 5: Build & Verify
- [ ] `./gradlew clean build -x test` — compilation verify
- [ ] `./gradlew test` — tests pass verify
