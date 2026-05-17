# Zero-Hardcode Orchestration Policy — Migration Checklist

> Generated: 2026-05-17
> Scope: Full codebase audit against Zero-Hardcode Orchestration Policy
> Total violations found: **80+ hardcoded values across 28 files**

---

## Legend

- 🔴 **CRITICAL** — Blocks dynamic provider management entirely
- 🟠 **HIGH** — Prevents admin from configuring without code changes
- 🟡 **MEDIUM** — Should be externalized for production readiness
- 🟢 **LOW** — Dev-only or acceptable defaults

---

## PHASE 1: CRITICAL — Provider Factory & Switch/Case Elimination

### 1.1 🔴 AIProviderFactory.java — Hardcoded Provider Switch/Case

**File:** `src/main/java/com/supremeai/provider/AIProviderFactory.java` (lines 88-181)

**Problem:** 26 provider types with hardcoded model names, URLs, and class mappings in a switch/case block. This is the **single biggest violation** of Zero-Hardcode policy. Even though `createProviderFromConfig()` (line 424) already supports dynamic creation, the switch/case takes priority for known types.

**Current state:** Lines 74-82 already have dynamic resolution via `providerMetadataService.getMetadata()`, but the switch/case on line 88 still falls back to hardcoded values.

**Required changes:**
- Remove the entire switch/case block (lines 88-181)
- Make the dynamic resolution path (lines 74-82) the **only** resolution path
- For providers that need special class instantiation (OpenAI, Anthropic, etc.), use a **type-based lookup map** stored in Firestore, not hardcoded
- The `getProvider()` method should:
  1. Look up provider metadata from Firestore by name
  2. If found, create `SupremeCloudProvider` with `metadata.getBaseUrl()` and `metadata.getModels()`
  3. If not found, throw `IllegalArgumentException`

**Impact:** All 26 provider types (gpt4, openai, claude, anthropic, gemini, google, groq, deepseek, ollama, huggingface, kimi, mistral, stepfun, codegeex4, gcp_qwen, gcp_llama, gcp_phi, gcp_nomic, hf_deepseek, hf_mistral, hf_llama, hf_codellama, hf_phi, render_*, hf_phi_vision, hf_paligemma, hf_e5_large, hf_bge)

---

### 1.2 🔴 MultiAIVotingService.java — Hardcoded Provider Arrays & Static Weights

**File:** `src/main/java/com/supremeai/service/MultiAIVotingService.java`

**Lines to fix:**
- **Lines 65-68:** `ALL_PROVIDERS` array — 16 hardcoded provider names
- **Line 70:** `DEFAULT_PROVIDERS` array — 5 hardcoded provider names
- **Line 79:** `@Value("${ai.active.providers:gemini,openai,claude}")` — hardcoded default
- **Lines 92-94:** `initializePerformanceTrackers()` only initializes for `DEFAULT_PROVIDERS`
- **Lines 783-800:** `staticWeights` map — 17 hardcoded provider weights
- **Line 813:** `staticWeights.getOrDefault(modelName.toLowerCase(), 0.5)` — hardcoded default weight

**Required changes:**
- Replace `ALL_PROVIDERS` with `providerRepository.findAll()` at runtime
- Replace `DEFAULT_PROVIDERS` with Firestore query for providers where `canParticipateInVoting == true`
- Replace `staticWeights` with `APIProvider.capabilityScores` from Firestore
- Remove `@Value("${ai.active.providers:...}")` default — use all active providers from DB
- `initializePerformanceTrackers()` should initialize for ALL providers from DB

---

### 1.3 🔴 SystemConfigSeeder.java — Hardcoded Provider Seeding

**File:** `src/main/java/com/supremeai/service/SystemConfigSeeder.java`

**Lines to fix:**
- **Lines 83-91:** `buildDefaultProviders()` — 5 hardcoded providers seeded into Firestore
- **Lines 110-118:** `getDefaultBaseUrl()` — hardcoded base URLs for 4 provider types
- **Lines 163-173:** `TelegramConfig` — hardcoded secrets (apiId, apiHash, botToken, channelId)
- **Lines 176-181:** `SupabaseConfig` — hardcoded dbUrl with password `njel.com.bd123`
- **Lines 184-281:** `config.setProviders()` — 12 hardcoded provider configs

**Required changes:**
- `buildDefaultProviders()` should seed **zero** providers — admin adds via dashboard
- `getDefaultBaseUrl()` should return empty string — admin fills via dashboard
- Telegram/Supabase secrets → environment variables or Secret Manager
- `config.setProviders()` → empty map, admin configures via dashboard

---

### 1.4 🔴 SystemAutoDetectService.java — Hardcoded Provider Priority Order

**File:** `src/main/java/com/supremeai/service/SystemAutoDetectService.java`

**Lines to fix:**
- **Lines 26-37:** API key fields tied to specific providers with hardcoded `@Value` property names
- **Lines 43-51:** `PROVIDER_ORDER` array — hardcoded fallback chain
- **Lines 98-128:** `createProvider()` switch/case with hardcoded provider-to-key mapping

**Required changes:**
- Replace `PROVIDER_ORDER` with `providerRepository.findAll()` sorted by `APIProvider.priority`
- Replace `createProvider()` switch/case with dynamic `createProviderFromConfig()`
- API keys should be resolved from Firestore `APIProvider.apiKey`, not `@Value` properties

---

### 1.5 🔴 ApiKeyRotationService.java — Hardcoded Provider Rotation Configs

**File:** `src/main/java/com/supremeai/security/ApiKeyRotationService.java`

**Lines to fix:**
- **Lines 42-54:** `PROVIDER_CONFIGS` — hardcoded map with test endpoints, auth methods, rotation days for 11 providers

**Required changes:**
- Move all provider configs to Firestore `APIProvider.config` map
- Load dynamically at runtime

---

## PHASE 2: HIGH — Provider Implementation Classes

### 2.1 🟠 Provider API URLs — Should Use Firestore baseUrl

Each provider class has hardcoded `API_URL` and `DEFAULT_MODEL`. These should be removed in favor of Firestore-driven configuration.

| File | Line | Hardcoded URL | Hardcoded Model |
|------|------|---------------|-----------------|
| `OpenAIProvider.java` | 14-15 | `https://api.openai.com/v1/chat/completions` | `gpt-3.5-turbo` |
| `AnthropicProvider.java` | 16-17 | `https://api.anthropic.com/v1/messages` | `claude-3-sonnet-20240229` |
| `GeminiProvider.java` | 15 | `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent` | — |
| `DeepSeekProvider.java` | 19 | `https://api.deepseek.com/v1/chat/completions` | — |
| `GroqProvider.java` | 16 | `https://api.groq.com/openai/v1/chat/completions` | — |
| `MistralProvider.java` | 16 | `https://api.mistral.ai/v1/chat/completions` | — |
| `KimiProvider.java` | 16 | `https://api.moonshot.cn/v1/chat/completions` | — |
| `StepFunProvider.java` | 18 | `https://api.stepfun.com/v1/chat/completions` | — |
| `CodeGeeX4Provider.java` | 19 | `https://open.bigmodel.cn/api/coding/paas/v4/chat/completions` | — |
| `HuggingFaceProvider.java` | — | HF inference URL | — |

**Required changes:**
- Remove hardcoded `API_URL` and `DEFAULT_MODEL` constants
- Accept `baseUrl` and `modelName` as constructor parameters (already supported by `SupremeCloudProvider`)
- All provider instantiation should go through `createProviderFromConfig()` which reads from Firestore

### 2.2 🟠 OllamaProvider.java & LocalInferenceProvider.java — Hardcoded localhost

| File | Line | Hardcoded Value |
|------|------|-----------------|
| `OllamaProvider.java` | 24 | `"http://localhost:11434"` and `"codegeex4"` |
| `LocalInferenceProvider.java` | 14 | `"http://localhost:11434"` and `"default"` |

**Required changes:**
- Accept `baseUrl` and `modelName` from Firestore config
- Keep localhost as `@Value` default only, not hardcoded in business logic

### 2.3 🟠 VisionService.java — Hardcoded API URLs

| File | Line | Hardcoded Value |
|------|------|-----------------|
| `VisionService.java` | 228 | `https://api.openai.com/v1/chat/completions` |
| `VisionService.java` | 262 | `https://generativelanguage.googleapis.com/v1beta/models/` |

**Required changes:**
- Resolve provider URLs from Firestore, not hardcoded strings

---

## PHASE 3: HIGH — Voting & Weight System

### 3.1 🟠 TenAIVotingSystem.java — Duplicate Hardcoded Weights

**File:** `src/main/java/com/supremeai/service/TenAIVotingSystem.java`

**Lines to fix:**
- **Lines 29-40:** `TEN_AI_MODELS` array — exactly 10 models hardcoded
- **Lines 378-390:** Duplicate `staticWeight` switch/case

**Required changes:**
- Remove `TenAIVotingSystem` entirely or unify with `MultiAIVotingService`
- Use dynamic weights from `APIProvider.capabilityScores`

### 3.2 🟠 UsageOptimizationService.java — Hardcoded Model Tiers

**File:** `src/main/java/com/supremeai/service/UsageOptimizationService.java`

**Lines to fix:**
- **Lines 51-77:** `MODEL_TIERS` — 21 model-to-tier-and-cost mappings
- **Lines 268-278:** `getProviderForModel()` — model-name-prefix-to-provider mapping
- **Lines 280-291:** `getDefaultModelForProvider()` — duplicate of `ProviderAdminService.determineDefaultModel()`

**Required changes:**
- Move `MODEL_TIERS` to Firestore collection
- Remove duplicate `getDefaultModelForProvider()` — single source of truth
- `getProviderForModel()` should query Firestore model registry

---

## PHASE 4: MEDIUM — Roles, Capabilities & Config

### 4.1 🟡 ProviderRoleSuggestionService.java — Hardcoded Role Keywords

**File:** `src/main/java/com/supremeai/service/ProviderRoleSuggestionService.java` (lines 14-20)

**Problem:** Role keywords hardcoded in a `Map.of()`:
```java
private static final Map<String, List<String>> ROLE_KEYWORDS = Map.of(
    "coding", List.of("coder", "code", "deepseek", "llama-3-70b", "gpt-4"),
    "security", List.of("audit", "exploit", "security", "defense", "hacking"),
    "reasoning", List.of("o1", "pro", "large", "opus", "r1"),
    "fast_chat", List.of("flash", "mini", "haiku", "8b", "turbo"),
    "multimodal", List.of("vision", "audio", "omni", "gpt-4o", "gemini")
);
```

**Required changes:**
- Move to Firestore `role_config` collection
- Admin-editable via dashboard

### 4.2 🟡 ProviderAdminService.java — Hardcoded Type Detection

**File:** `src/main/java/com/supremeai/admin/ProviderAdminService.java`

**Lines to fix:**
- **Lines 251-261:** `determineTypeFromName()` — name-based type detection
- **Lines 263-275:** `determineDefaultModel()` — type-to-default-model mapping

**Required changes:**
- Type should be explicitly set by admin, not guessed from name
- Default model should come from Firestore config

### 4.3 🟡 ProviderInitializationService.java — Hardcoded Capabilities

**File:** `src/main/java/com/supremeai/service/ProviderInitializationService.java` (lines 61-92)

**Problem:** Capability lists hardcoded per provider name pattern (gemini→chat+reasoning+multimodal, openai→chat+code, etc.)

**Required changes:**
- Capabilities should be auto-detected or admin-configurable, not hardcoded

### 4.4 🟡 FocusDetectorService.java — Hardcoded Focus Keywords

**File:** `src/main/java/com/supremeai/learning/FocusDetectorService.java` (lines 18-64)

**Problem:** 10 focus areas with hardcoded keyword lists

**Required changes:**
- Move to Firestore `focus_config` collection

### 4.5 🟡 RouterKnowledgeInitializer.java — Hardcoded Provider Preferences

**File:** `src/main/java/com/supremeai/learning/RouterKnowledgeInitializer.java` (lines 58-109, 177-200)

**Problem:** Hardcoded task-type-to-provider mappings and benchmark scores

**Required changes:**
- Load from `core_knowledge.json` or Firestore, not hardcoded in Java

### 4.6 🟡 SupremeLearningOrchestrator.java — Hardcoded Preferences

**File:** `src/main/java/com/supremeai/learning/SupremeLearningOrchestrator.java` (lines 649-651)

**Problem:** Hardcoded provider preferences for CODE_GENERATION, LANGUAGE_TASKS, MULTIMODAL

**Required changes:**
- Load from Firestore or `core_knowledge.json`

---

## PHASE 5: MEDIUM — Frontend (Dashboard)

### 5.1 🟡 constants.ts — Hardcoded Models & Endpoints

**File:** `dashboard/src/components/api-keys/constants.ts`

**Lines to fix:**
- **Lines 5-42:** `POPULAR_MODELS` — 24 hardcoded model entries
- **Lines 44-63:** `PROVIDER_ENDPOINTS` — 17 provider-to-URL mappings

**Required changes:**
- Fetch from backend API (`/api/admin/providers/discover` already exists)

### 5.2 🟡 ProviderModal.tsx — Hardcoded Architecture Options

**File:** `dashboard/src/components/providers/ProviderModal.tsx` (lines 119-125)

**Problem:** Only 4 architecture types (openai, anthropic, google, custom)

**Required changes:**
- Fetch from backend

### 5.3 🟡 App.tsx — Hardcoded Demo Model Statuses

**File:** `dashboard/src/App.tsx` (lines 27-32)

**Problem:** 5 hardcoded model statuses with fake latency/memory data

**Required changes:**
- Fetch from backend health API

### 5.4 🟡 ChatWithAI.tsx — Hardcoded Demo Agents

**File:** `dashboard/src/components/ChatWithAI.tsx` (lines 146-149)

**Problem:** 3 hardcoded demo agents

**Required changes:**
- Fetch from backend or remove

---

## PHASE 6: MEDIUM — Config Files & Secrets

### 6.1 🟡 application.properties — Hardcoded Secrets

**File:** `src/main/resources/application.properties`

| Line | Hardcoded Value | Required Change |
|------|-----------------|-----------------|
| 39 | `jwt.secret=${JWT_SECRET:SupremeAI32CharacterDefaultSecretKeyForHS256}` | Remove default, require env var |
| 143-149 | Firebase config with `supremeai-a`, `565236080752` | Remove defaults |
| 206 | `n8n.url` with hardcoded Cloud Run URL | Remove default |
| 207 | `n8n.api.key=SupremeAI_n8n_2026_Secure!` | Remove default, use env var |

### 6.2 🟡 ai-cloud-endpoints.json — Hardcoded Cloud Run URLs

**File:** `src/main/resources/ai-cloud-endpoints.json`

**Problem:** Entire file is 5 hardcoded Cloud Run endpoints with embedded project number

**Required changes:**
- Remove file entirely
- All endpoints should come from Firestore `APIProvider.baseUrl`

### 6.3 🟡 initial_models.json — Hardcoded Provider Definitions

**File:** `src/main/resources/initial_models.json`

**Problem:** 4 hardcoded provider definitions with URLs, models, auth methods

**Required changes:**
- Remove file or make it empty
- Admin configures via dashboard

### 6.4 🟡 N8nIntegrationService.java — Hardcoded n8n URL & Key

**File:** `src/main/java/com/supremeai/service/N8nIntegrationService.java` (lines 29-30)

**Required changes:**
- Remove hardcoded defaults, require env vars

### 6.5 🟡 SecurityConfig.java — Hardcoded CORS Origins

**File:** `src/main/java/com/supremeai/config/SecurityConfig.java` (lines 59, 168-171)

**Problem:** CORS origins with localhost hardcoded

**Required changes:**
- Use separate profiles for dev/prod
- Prod profile should have no localhost

---

## PHASE 7: LOW — Cleanup & Polish

### 7.1 🟢 SystemMonitoringController.java — Hardcoded Model List

**File:** `src/main/java/com/supremeai/controller/SystemMonitoringController.java` (line 30)

**Problem:** `String[] models = {"qwen", "llama", "phi", "nomic", "deepseek"}`

**Required changes:**
- Fetch from Firestore

### 7.2 🟢 VotingController.java — Hardcoded Default Providers

**File:** `src/main/java/com/supremeai/controller/VotingController.java` (line 27)

**Problem:** `@Value("${supremeai.active.providers:groq,openai,anthropic,ollama}")`

**Required changes:**
- Use all active providers from DB

### 7.3 🟢 ProviderModal.tsx — Hardcoded Default Base URL

**File:** `dashboard/src/components/providers/ProviderModal.tsx` (line 154)

**Problem:** `initialValue="https://api.openai.com/v1"`

**Required changes:**
- Leave empty or dynamic

### 7.4 🟢 ChatMessage.java — Inverted Role Assignment

**File:** `src/main/java/com/supremeai/model/ChatMessage.java` (line 29)

**Problem:** `this.role = isAdmin ? "ai" : "user"` — semantically inverted

**Required changes:**
- Fix to use proper role values

### 7.5 🟢 WikipediaExtractor.java — Hardcoded Tech Keywords

**File:** `src/main/java/com/supremeai/learning/active/WikipediaExtractor.java` (lines 85-90)

**Problem:** 27 hardcoded technology keywords

**Required changes:**
- Move to config or Firestore

### 7.6 🟢 Legacy AIProviderFactory

**File:** `src/main/java/com/supremeai/ai/provider/AIProviderFactory.java` (line 14)

**Problem:** Hardcoded OpenAI provider in constructor with `System.getenv().getOrDefault("OPENAI_API_KEY", "")`

**Required changes:**
- Remove legacy factory entirely (new one exists in `com.supremeai.provider`)

---

## Migration Strategy

### Step 1: Database Schema Preparation
Ensure Firestore `api_providers` collection has all needed fields:
- `baseUrl` — API endpoint URL
- `models[]` — list of supported models
- `modelName` — primary model name
- `capabilityScores` — map of task-type → score
- `assignedRoles` — list of work roles
- `priority` — routing priority
- `config` — extensible config map
- `type` — provider architecture type

### Step 2: Create Dynamic Provider Type Registry
Create a new Firestore collection `provider_types` that maps:
- `typeId` → `providerClass` (e.g., "openai" → "OpenAIProvider")
- `typeId` → `defaultBaseUrl` (e.g., "openai" → "https://api.openai.com/v1")
- `typeId` → `defaultModel` (e.g., "openai" → "gpt-4o-mini")

This replaces the switch/case with a database lookup.

### Step 3: Refactor AIProviderFactory
- Remove switch/case
- Make dynamic resolution the only path
- Use `provider_types` Firestore collection for class instantiation hints

### Step 4: Refactor Voting System
- Replace hardcoded arrays with Firestore queries
- Replace static weights with `capabilityScores`

### Step 5: Clean Up Config Files
- Remove `ai-cloud-endpoints.json`
- Remove `initial_models.json`
- Remove hardcoded secrets from `application.properties`

### Step 6: Frontend Updates
- Replace hardcoded constants with API calls
- Remove demo/mock data

### Step 7: Testing
- Verify all providers work via dynamic resolution
- Verify voting system uses dynamic weights
- Verify admin can add/edit/delete providers without code changes

---

## Files Requiring Changes (Priority Order)

| Priority | File | Change Type |
|----------|------|-------------|
| 🔴 P0 | `AIProviderFactory.java` | Remove switch/case, make fully dynamic |
| 🔴 P0 | `MultiAIVotingService.java` | Replace hardcoded arrays/weights with Firestore |
| 🔴 P0 | `SystemConfigSeeder.java` | Remove hardcoded seeding, use env vars for secrets |
| 🔴 P0 | `SystemAutoDetectService.java` | Replace hardcoded order with Firestore priority |
| 🔴 P0 | `ApiKeyRotationService.java` | Move configs to Firestore |
| 🟠 P1 | `OpenAIProvider.java` + 8 other provider classes | Accept baseUrl/modelName from constructor |
| 🟠 P1 | `OllamaProvider.java` | Accept baseUrl/modelName from config |
| 🟠 P1 | `LocalInferenceProvider.java` | Accept baseUrl/modelName from config |
| 🟠 P1 | `VisionService.java` | Resolve URLs from Firestore |
| 🟠 P1 | `TenAIVotingSystem.java` | Remove or unify with MultiAIVotingService |
| 🟠 P1 | `UsageOptimizationService.java` | Move model tiers to Firestore |
| 🟡 P2 | `ProviderRoleSuggestionService.java` | Move role keywords to Firestore |
| 🟡 P2 | `ProviderAdminService.java` | Remove hardcoded type detection |
| 🟡 P2 | `ProviderInitializationService.java` | Remove hardcoded capabilities |
| 🟡 P2 | `FocusDetectorService.java` | Move focus keywords to Firestore |
| 🟡 P2 | `RouterKnowledgeInitializer.java` | Load from core_knowledge.json |
| 🟡 P2 | `SupremeLearningOrchestrator.java` | Load from Firestore |
| 🟡 P2 | `constants.ts` (dashboard) | Fetch from API |
| 🟡 P2 | `ProviderModal.tsx` | Fetch architecture options from API |
| 🟡 P2 | `App.tsx` | Remove demo model data |
| 🟡 P2 | `application.properties` | Remove hardcoded secrets/defaults |
| 🟡 P2 | `SecurityConfig.java` | Externalize CORS origins |
| 🟢 P3 | `ai-cloud-endpoints.json` | Delete file |
| 🟢 P3 | `initial_models.json` | Delete file |
| 🟢 P3 | `N8nIntegrationService.java` | Remove hardcoded defaults |
| 🟢 P3 | `SystemMonitoringController.java` | Fetch models from Firestore |
| 🟢 P3 | `VotingController.java` | Remove hardcoded defaults |
| 🟢 P3 | `ChatMessage.java` | Fix role inversion |
| 🟢 P3 | `AIProviderFactory.java` (legacy) | Delete file |
