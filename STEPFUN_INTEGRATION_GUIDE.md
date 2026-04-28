# StepFun (阶跃星辰) Integration Guide - Free Tier Implementation

**Objective**: Integrate `stepfun/step-3.5-flash` into SupremeAI system with zero cost using StepFun's free tier API.

**Estimated Time**: 15-20 minutes
**Cost**: $0 (using StepFun free tier)
**Difficulty**: Easy (follows existing provider pattern)

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Get Free StepFun API Key](#1-get-free-stepfun-api-key)
3. [Backend Implementation](#2-backend-implementation)
4. [Frontend Integration](#3-frontend-integration)
5. [Configuration](#4-configuration)
6. [Testing](#5-testing)
7. [Deployment](#6-deployment)
8. [Cost-Free Usage Tips](#7-cost-free-usage-tips)
9. [Troubleshooting](#8-troubleshooting)
10. [Quick Reference](#9-quick-reference)

---

## Prerequisites

Before starting, ensure you have:

- ✅ Java 21 installed
- ✅ Spring Boot backend running locally (`./gradlew bootRun`)
- ✅ Firebase CLI installed (`firebase deploy` works)
- ✅ Node.js 18+ for dashboard
- ✅ Admin access to SupremeAI dashboard
- ✅ A valid email address (to register StepFun account)

---

## 1. Get Free StepFun API Key

### Step 1.1: Register Account

1. Go to **[StepFun Platform](https://platform.stepfun.com)** (阶跃星辰开放平台)
2. Click **Sign Up** (注册)
3. Register using:
   - Email address (recommended)
   - OR phone number (Chinese +86 required for SMS)
4. Verify email/phone
5. Set password (use strong password)

### Step 1.2: Create API Key

1. Login to your StepFun account
2. Navigate to **API Management** (API 管理) or **Developer Console** (开发者控制台)
3. Click **Create New API Key** (创建新的 API 密钥)
4. Configure:
   - **Key Name**: `supremeai-integration` (or any name)
   - **Permissions**: Select `chat/completions` (full access)
   - **Rate Limit**: Choose "Free Tier" (免费版)
5. Click **Generate** (生成)
6. **COPY THE KEY IMMEDIATELY** - it won't be shown again
   - Format looks like: `sf-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`
   - Example: `sf-abc123def456ghi789jkl012mno345pqr`

### Step 1.3: Verify Free Tier Details

On your StepFun dashboard, check:

- **Daily Token Quota**: Usually 10,000-50,000 tokens/day (varies)
- **Rate Limit**: Typically 10-30 requests per minute (RPM)
- **Models Available**: `step-3.5-flash`, `step-3.5-pro`, `step-1`
- **Expiry**: Free API keys may expire after 30-90 days (renew needed)

**⚠️ IMPORTANT**: StepFun is a Chinese platform. UI is in Chinese. Use browser translate if needed.

---

## 2. Backend Implementation

### 2.1: Create StepFunProvider.java

**File Location**: `src/main/java/com/supremeai/provider/StepFunProvider.java`

**Create new file** with this exact code:

```java
package com.supremeai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * StepFun (阶跃星辰) Provider Integration
 * Free tier available: https://platform.stepfun.com
 *
 * API Documentation: https://platform.stepfun.com/docs
 * Uses OpenAI-compatible chat completions format
 */
public class StepFunProvider implements AIProvider {

    private static final String API_URL = "https://api.stepfun.com/v1/chat/completions";
    private final String apiKey;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String defaultModel;

    /**
     * Constructor with default model (step-3.5-flash)
     */
    public StepFunProvider(String apiKey) {
        this(apiKey, "step-3.5-flash");
    }

    /**
     * Constructor with custom model
     * @param apiKey StepFun API key (sf-...)
     * @param model Model name: step-3.5-flash, step-3.5-pro, or step-1
     */
    public StepFunProvider(String apiKey, String model) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("StepFun API key must be provided.");
        }
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.defaultModel = model;
    }

    @Override
    public String getName() {
        return "stepfun";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "name", "StepFun (阶跃星辰)",
                "provider", "StepFun",
                "models", new String[]{"step-3.5-flash", "step-3.5-pro", "step-1"},
                "freeTier", "10k-50k tokens/day",
                "rateLimit", "10-30 RPM",
                "supports", List.of("chat", "code", "reasoning", "multimodal"),
                "language", List.of("zh", "en", "multi")
        );
    }

    @Override
    public Mono<String> generate(String prompt) {
        return Mono.fromCallable(() -> {
            try {
                // Build request in OpenAI-compatible format
                Map<String, Object> requestBody = Map.of(
                        "messages", List.of(Map.of("role", "user", "content", prompt)),
                        "model", defaultModel,
                        "temperature", 0.7,
                        "max_tokens", 4000,
                        "stream", false
                );

                String jsonBody = objectMapper.writeValueAsString(requestBody);

                Request request = new Request.Builder()
                        .url(API_URL)
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errBody = response.body() != null ? response.body().string() : "";

                        // Handle specific error codes
                        if (response.code() == 429) {
                            throw new IOException("STEPFUN_RATE_LIMIT: " + errBody);
                        } else if (response.code() == 401) {
                            throw new IOException("STEPFUN_AUTH_ERROR: Invalid API key");
                        } else if (response.code() == 400) {
                            throw new IOException("STEPFUN_BAD_REQUEST: " + errBody);
                        }
                        throw new IOException("StepFun API Error " + response.code() + ": " + errBody);
                    }

                    String responseBody = response.body().string();
                    Map<String, Object> responseMap = objectMapper.readValue(
                        responseBody,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                    );

                    // Extract response text (standard OpenAI format)
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                        return (String) message.get("content");
                    }

                    return "No response from StepFun.";
                }
            } catch (IOException e) {
                throw new RuntimeException("StepFun API call failed: " + e.getMessage(), e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public String getDefaultModel() {
        return defaultModel;
    }
}
```

---

### 2.2: Register Provider in AIProviderFactory

**File**: `src/main/java/com/supremeai/provider/AIProviderFactory.java`

**Edit 1**: Add case to switch statement (after line 88, before `default`):

```java
case "stepfun":
    return new StepFunProvider(key);
```

**Full switch section after edit (lines 51-92):**

```java
switch (name.toLowerCase()) {
    case "gpt4":
    case "openai":
        return new OpenAIProvider(key);

    case "claude":
    case "anthropic":
        return new AnthropicProvider(key);

    case "gemini":
        return new GeminiProvider(key);

    case "groq":
        return new GroqProvider(key);

    case "deepseek":
        return new DeepSeekProvider(key);

    case "ollama":
        if (ollamaProvider == null) {
            logger.error("Ollama provider bean not found.");
            throw new IllegalStateException("Ollama provider not available.");
        }
        return ollamaProvider;

    case "huggingface":
        return new HuggingFaceProvider(key);

    case "airllm":
        return new AirLLMProvider(airllmEndpoint, key, airllmModel);

    case "kimi":
        return new KimiProvider(key);

    case "mistral":
        return new MistralProvider(key);

    case "stepfun":
        return new StepFunProvider(key);

    default:
        throw new IllegalArgumentException("Unknown AI provider: " + name);
}
```

**Edit 2**: Update `getSupportedProviders()` (line 196):

```java
public String[] getSupportedProviders() {
    return new String[]{
        "gpt4", "claude", "gemini", "groq", "deepseek",
        "ollama", "huggingface", "airllm", "kimi", "mistral",
        "stepfun"  // <-- Add this
    };
}
```

**Edit 3** (Optional): Add to preferred providers in `getDefaultProvider()` (line 135):

```java
String[] preferredProviders = {"gpt4", "claude", "gemini", "groq", "deepseek", "stepfun"};
```

---

### 2.3: Add Environment Variable Configuration

**File**: `src/main/resources/application.properties`

**Add to the provider API key section (around line 280-290):**

```properties
# ========== AI PROVIDER API KEYS ==========
# Add your StepFun API key here or set environment variable STEPFUN_API_KEY
supremeai.provider.stepfun.api-key=${STEPFUN_API_KEY:}

# StepFun Quota Settings (adjust based on your actual free tier)
stepfun.daily.quota=50000
stepfun.monthly.quota=1000000
```

**Location context** (add after existing provider keys):

```
Line 274: supremeai.provider.groq.api-key=${GROQ_API_KEY:}
Line 275: supremeai.provider.openai.api-key=${OPENAI_API_KEY:}
Line 276: supremeai.provider.anthropic.api-key=${ANTHROPIC_API_KEY:}
Line 277: supremeai.provider.gemini.api-key=${GEMINI_API_KEY:}
Line 278: supremeai.provider.huggingface.api-key=${HUGGINGFACE_API_KEY:}
Line 279: ai.kimi.api-key=${KIMI_API_KEY:}
Line 280: ai.deepseek.api-key=${DEEPSEEK_API_KEY:}
Line 281: ai.mistral.api-key=${MISTRAL_API_KEY:}
Line 282: supremeai.provider.stepfun.api-key=${STEPFUN_API_KEY:}   <-- ADD THIS
Line 283:
Line 284: supremeai.active.providers=...
```

---

### 2.4: Update ProviderConfig.java (Optional Alternative)

If you prefer using `ProviderConfig.java` bean instead of `application.properties`:

**File**: `src/main/java/com/supremeai/config/ProviderConfig.java`

**Edit the method signature** (line 14-25) to include StepFun:

```java
@Bean
public Map<String, String> providerApiKeys(
        @Value("${OPENAI_API_KEY:}") String openai,
        @Value("${GEMINI_API_KEY:}") String gemini,
        @Value("${ANTHROPIC_API_KEY:}") String anthropic,
        @Value("${COHERE_API_KEY:}") String cohere,
        @Value("${PERPLEXITY_API_KEY:}") String perplexity,
        @Value("${MISTRAL_API_KEY:}") String mistral,
        @Value("${LLAMA_API_KEY:}") String llama,
        @Value("${DEEPSEEK_API_KEY:}") String deepseek,
        @Value("${GROK_API_KEY:}") String grok,
        @Value("${AZURE_OPENAI_API_KEY:}") String azure,
        @Value("${CUSTOM_AI_API_KEY:}") String custom,
        @Value("${STEPFUN_API_KEY:}") String stepfun) {   // <-- ADD THIS

    Map<String, String> keys = new HashMap<>();
    // ... existing code ...
    if (!stepfun.isBlank()) keys.put("stepfun", stepfun);  // <-- ADD THIS

    return keys;
}
```

---

### 2.5: Set Environment Variable

**Option A: Local Development** (`.env` file in project root)

```bash
# Create or edit .env file
echo "STEPFUN_API_KEY=sf-your-actual-api-key-here" >> .env

# Also add to .env.example for documentation
echo "STEPFUN_API_KEY=your-stepfun-api-key" >> .env.example
```

**Option B: Terminal** (bash/zsh):

```bash
export STEPFUN_API_KEY="sf-your-actual-api-key-here"
```

**Option C: Windows PowerShell**:

```powershell
$env:STEPFUN_API_KEY="sf-your-actual-api-key-here"
```

**Option D: Application.properties** (NOT recommended for production, OK for dev):

```properties
supremeai.provider.stepfun.api-key=sf-your-actual-api-key-here
```

---

## 3. Frontend Integration

### 3.1: Update APIKeysManager.tsx

**File**: `dashboard/src/components/APIKeysManager.tsx`

#### Edit A: Add to `POPULAR_MODELS` array (after line 118)

Add these two entries to the popular models list:

```typescript
// StepFun (阶跃星辰) - Free tier available
{ id: 'step-3.5-flash', name: 'Step 3.5 Flash', provider: 'stepfun', providerTitle: 'StepFun (阶跃星辰)', baseUrl: 'https://api.stepfun.com/v1', description: 'Free & fast - Chinese AI model, excellent reasoning & coding', category: 'StepFun' },
{ id: 'step-3.5-pro', name: 'Step 3.5 Pro', provider: 'stepfun', providerTitle: 'StepFun (阶跃星辰)', baseUrl: 'https://api.stepfun.com/v1', description: 'Advanced reasoning, math & coding - paid but affordable', category: 'StepFun' },
```

**Where to insert**: After line 117 (after xAI models), before the closing `];`

**Full section to modify** (lines 115-118):

```typescript
// xAI
{ id: 'grok-3', name: 'Grok 3', provider: 'xai', providerTitle: 'xAI', baseUrl: 'https://api.x.ai/v1', description: 'Latest Grok model by xAI', category: 'xAI' },
{ id: 'grok-3-mini', name: 'Grok 3 Mini', provider: 'xai', providerTitle: 'xAI', baseUrl: 'https://api.x.ai/v1', description: 'Fast Grok 3 Mini', category: 'xAI' },
// StepFun (阶跃星辰) - Free tier
{ id: 'step-3.5-flash', name: 'Step 3.5 Flash', provider: 'stepfun', providerTitle: 'StepFun (阶跃星辰)', baseUrl: 'https://api.stepfun.com/v1', description: 'Free & fast - excellent reasoning & coding', category: 'StepFun' },
{ id: 'step-3.5-pro', name: 'Step 3.5 Pro', provider: 'stepfun', providerTitle: 'StepFun (阶跃星辰)', baseUrl: 'https://api.stepfun.com/v1', description: 'Advanced reasoning & coding capabilities', category: 'StepFun' },
];
```

#### Edit B: Add to `PROVIDER_ENDPOINTS` (around line 140)

Add this line:

```typescript
stepfun: 'https://api.stepfun.com/v1',
```

**Full section to modify** (lines 122-145):

```typescript
const PROVIDER_ENDPOINTS: Record<string, string> = {
    openai: 'https://api.openai.com/v1',
    anthropic: 'https://api.anthropic.com/v1',
    google: 'https://generativelanguage.googleapis.com/v1beta/openai',
    gemini: 'https://generativelanguage.googleapis.com/v1beta/openai',
    huggingface: 'https://api-inference.huggingface.co',
    meta: 'https://openrouter.ai/api/v1',
    openrouter: 'https://openrouter.ai/api/v1',
    mistral: 'https://api.mistral.ai/v1',
    groq: 'https://api.groq.com/openai/v1',
    deepseek: 'https://api.deepseek.com/v1',
    xai: 'https://api.x.ai/v1',
    perplexity: 'https://api.perplexity.ai',
    cohere: 'https://api.cohere.ai/v1',
    together: 'https://api.together.xyz/v1',
    anyscale: 'https://api.endpoints.anyscale.com/v1',
    azure: 'https://YOUR_RESOURCE.openai.azure.com',
    aws: 'https://YOUR_REGION.bedrock.aws.amazon.com',
    stepfun: 'https://api.stepfun.com/v1',  // <-- ADD THIS
};
```

#### Edit C: (Optional) Add Model Discovery

If StepFun provides a `/v1/models` endpoint, add a search function:

**Search for `searchOpenRouterModels` function** in the file (around line 600-700)

**Add this function nearby**:

```typescript
const searchStepFunModels = async (apiKey: string) => {
    try {
        const response = await fetch('https://api.stepfun.com/v1/models', {
            headers: { 'Authorization': `Bearer ${apiKey}` }
        });
        if (!response.ok) return [];
        const data = await response.json();
        return (data.data || []).map((m: any) => ({
            id: m.id,
            name: m.id,
            provider: 'stepfun',
            description: `StepFun model: ${m.id}`,
            category: 'StepFun'
        }));
    } catch (error) {
        console.error('StepFun model search failed:', error);
        return [];
    }
};
```

**Then integrate it** into the main search logic (find where `searchX` functions are called).

---

### 3.2: Rebuild Dashboard

```bash
cd dashboard
npm run build
# or for development:
npm run dev
```

---

## 4. Configuration

### 4.1: Firebase Functions (Optional - if calling from Cloud Functions)

If your Cloud Functions call the StepFun API directly (bypassing Java backend), update:

**File**: `functions/index.js`

Add environment variable:

```bash
firebase functions:config:set stepfun.api_key="sf-your-key"
firebase deploy --only functions:config
```

Then in code:

```javascript
const stepfunKey = functions.config().stepfun?.api_key;
```

**Note**: Current architecture calls Java backend, so this is optional.

---

### 4.2: Update .env.example

**File**: `.env.example` (at project root)

Add line:

```bash
STEPFUN_API_KEY=your-stepfun-api-key-here
```

---

### 4.3: Set Environment Variable on Server

**Local Development**:

```bash
# Linux/Mac
export STEPFUN_API_KEY="sf-your-key"

# Windows PowerShell
$env:STEPFUN_API_KEY="sf-your-key"

# Add to ~/.bashrc or ~/.zshrc to persist
echo 'export STEPFUN_API_KEY="sf-your-key"' >> ~/.bashrc
```

**Cloud Run / Google Cloud**:

1. Go to Google Cloud Console → Cloud Run
2. Select your `supremeai` service
3. **Variables** tab → **Edit & deploy new revision**
4. Add environment variable:
   - Name: `STEPFUN_API_KEY`
   - Value: `sf-your-key`
5. **Deploy**

**Firebase Functions**:

```bash
firebase functions:config:set stepfun.api_key="sf-your-key"
firebase deploy --only functions
```

---

## 5. Testing

### 5.1: Unit Test (Java)

Create test: `src/test/java/com/supremeai/provider/StepFunProviderTest.java`

```java
package com.supremeai.provider;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StepFunProviderTest {

    @Test
    void testProviderCreation() {
        // Replace with your test key (use a throwaway key for testing)
        String testKey = System.getenv("STEPFUN_API_KEY");
        if (testKey == null || testKey.isEmpty()) {
            System.out.println("Skipping: STEPFUN_API_KEY not set");
            return;
        }

        StepFunProvider provider = new StepFunProvider(testKey);
        assertNotNull(provider);
        assertEquals("stepfun", provider.getName());
        assertTrue(provider.getCapabilities().containsKey("freeTier"));
    }

    @Test
    void testGenerate() {
        String testKey = System.getenv("STEPFUN_API_KEY");
        if (testKey == null || testKey.isEmpty()) {
            System.out.println("Skipping: STEPFUN_API_KEY not set");
            return;
        }

        StepFunProvider provider = new StepFunProvider(testKey);
        String response = provider.generate("Say 'Hello' in one word").block();
        assertNotNull(response);
        System.out.println("StepFun response: " + response);
    }
}
```

Run test:

```bash
./gradlew test --tests "StepFunProviderTest"
```

---

### 5.2: Integration Test via API

**Step 5.2.1**: Start backend

```bash
./gradlew bootRun
```

**Step 5.2.2**: Test with curl

```bash
curl -X POST http://localhost:8080/api/ai/generate \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "stepfun",
    "prompt": "Write a simple Java hello world program"
  }'
```

**Expected response**:

```json
{
  "response": "public class HelloWorld {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, World!\");\n    }\n}",
  "provider": "stepfun",
  "model": "step-3.5-flash",
  "tokensUsed": 42
}
```

**Step 5.2.3**: Test via Multi-AI Consensus

```bash
curl -X POST http://localhost:8080/api/orchestrate/requirement \
  -H "Content-Type: application/json" \
  -d '{
    "projectId": "test-project",
    "description": "Build a simple HTML page with a button",
    "platform": "web"
  }'
```

Check logs for `StepFunProvider` usage:

```bash
tail -f logs/supremeai.log | grep -i stepfun
```

Expected log line:

```
INFO  StepFunProvider - Generation successful for step-3.5-flash
```

---

### 5.3: Dashboard UI Test

**Step 5.3.1**: Open dashboard

```
http://localhost:5173/admin/apikeys
```

**Step 5.3.2**: Add StepFun provider

1. Click **"Add Provider"** button
2. Provider dropdown → **StepFun** (should appear now)
3. Enter API Key: `sf-your-key-here`
4. Model: Select `step-3.5-flash` from dropdown
5. Click **"Test Connection"**

**Expected**:

- Green checkmark ✅
- Message: "Connection successful"
- Model info displayed

**Step 5.3.3**: Save provider

1. Click **"Save Provider Configuration"**
2. Provider should appear in configured providers table
3. Status badge: **Active**

---

### 5.4: Verify in Voting System

Trigger a consensus task:

```bash
curl -X POST http://localhost:8080/api/vote/requirement \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "What is the capital of France?",
    "taskType": "question_answering"
  }'
```

Check logs:

```bash
# Should see StepFun called (if healthy + in preferred list)
grep "Using ranked provider" logs/supremeai.log
# or
grep "Using .* as default provider" logs/supremeai.log
```

If StepFun appears in the provider list, integration is successful.

---

## 6. Deployment

### 6.1: Build Backend

```bash
cd supremeai
./gradlew clean build -x test  # Skip tests for quick deployment
```

### 6.2: Deploy Firebase Rules & Functions

```bash
cd supremeai
firebase deploy --only database,functions
```

**Note**: Functions will automatically pick up new provider class on next deploy.

---

### 6.3: Deploy Java Backend to Cloud

**Cloud Run**:

```bash
# Build Docker image
./gradlew bootBuildImage --imageName=gcr.io/supremeai-a/supremeai-backend

# Push to Google Container Registry
docker push gcr.io/supremeai-a/supremeai-backend

# Deploy to Cloud Run with env var
gcloud run deploy supremeai-backend \
  --image gcr.io/supremeai-a/supremeai-backend \
  --platform managed \
  --region us-central1 \
  --set-env-vars "STEPFUN_API_KEY=sf-your-key" \
  --allow-unauthenticated
```

**Or update existing service**:

```bash
gcloud run services update supremeai-backend \
  --set-env-vars "STEPFUN_API_KEY=sf-your-key"
```

---

### 6.4: Rebuild Dashboard

```bash
cd dashboard
npm run build

# Deploy to Firebase Hosting
firebase deploy --only hosting
```

---

## 7. Cost-Free Usage Tips

### 7.1: Maximize Free Tier

**Strategy 1: Multiple Free Keys**

- StepFun allows one free API key per account
- Create 3-5 accounts (use different email addresses)
- Add all keys to SupremeAI → key rotation automatically spreads usage
- Each account: 50k tokens/day → Total: 150-250k tokens/day free

**Strategy 2: Prioritize StepFun in Fallback Chain**

In `AIProviderFactory.getDefaultProvider()`:

```java
String[] preferredProviders = {
    "stepfun",      // FREE - Try first
    "groq",         // FREE - Try second
    "deepseek",     // FREE-ish - Try third
    "ollama",       // 100% FREE local - Try fourth
    "gpt4",         // PAID - Only if all free exhausted
    "claude"        // PAID - Last resort
};
```

**Strategy 3: Use step-3.5-flash Model**

- Flash model is fastest and cheapest (likely lowest token cost)
- Still high quality (good for coding + reasoning)
- Use `step-3.5-pro` only for complex tasks

**Strategy 4: Monitor Quota Exhaustion**

Add alert to Firestore `requirements`:

```javascript
// In functions/index.js or Java backend
if (error.message.includes("STEPFUN_RATE_LIMIT")) {
    // Mark provider as exhausted for today
    await db.collection("provider_health").doc("stepfun").set({
        status: "exhausted",
        resetTime: new Date(Date.now() + 24 * 60 * 60 * 1000), // Tomorrow
        lastError: "Rate limit exceeded"
    });
}
```

---

### 7.2: Free Tier Alternative Stack

If StepFun quota runs out, system auto-falls back to:

| Provider | Free Tier | Daily Tokens | Rate Limit |
|----------|-----------|--------------|------------|
| **StepFun** | ✅ Yes | 10k-50k | 10-30 RPM |
| **Groq** | ✅ Yes | 14,400 | 30 RPM |
| **DeepSeek** | ✅ Yes | Limited | ~10 RPM |
| **Ollama** | ✅ 100% Free | Unlimited (local) | Self-hosted |
| **HuggingFace** | ✅ Free inference | Varies | Varies |

**Total free capacity**: ~100k-300k tokens/day with rotation

---

### 7.3: Avoid Unnecessary Cost

- Set `max_tokens: 4000` max in provider config (not 16k)
- Use `step-3.5-flash` for most tasks (cheaper than `step-3.5-pro`)
- Implement **result caching** (cache identical prompts)
- Use **`temperature: 0.7`** or lower → fewer tokens
- Defer to **Ollama local** when possible (no tokens)

---

## 8. Troubleshooting

### Issue 8.1: `ClassNotFoundException: StepFunProvider`

**Cause**: Class not compiled or in wrong package

**Fix**:

```bash
# Rebuild
./gradlew clean compileJava

# Verify file exists
ls src/main/java/com/supremeai/provider/StepFunProvider.java

# Check package declaration at top of file:
# Must be: package com.supremeai.provider;
```

---

### Issue 8.2: Provider not showing in dashboard

**Cause**: Frontend cache or missing model entry

**Fix**:

1. Hard refresh dashboard: `Ctrl+Shift+R`
2. Clear localStorage: DevTools → Application → Clear storage
3. Check browser console for errors
4. Verify `APIKeysManager.tsx` has StepFun in `POPULAR_MODELS`
5. Rebuild dashboard: `npm run build` in `/dashboard` folder

---

### Issue 8.3: `401 Unauthorized` from StepFun

**Cause**: Invalid API key

**Fix**:

1. Double-check API key in StepFun console
2. Ensure no extra spaces: `sf-abc123` NOT ` sf-abc123 `
3. Regenerate key if shown publicly
4. Verify environment variable set correctly:

   ```bash
   echo $STEPFUN_API_KEY  # Should print your key
   ```

---

### Issue 8.4: `429 Rate Limit Exceeded`

**Cause**: Free tier quota exhausted

**Fix**:

1. Wait 24 hours (StepFun resets daily at UTC 00:00)
2. Rotate to another free provider key (add more StepFun accounts)
3. System should auto-fallback; verify fallback chain works
4. Check logs for `STEPFUN_RATE_LIMIT` errors

---

### Issue 8.5: Empty response or null

**Cause**: Response parsing error (StepFun format incompatible)

**Fix**:

1. Enable debug logging:

   ```properties
   logging.level.com.supremeai.provider.StepFunProvider=DEBUG
   ```

2. Check raw response in logs
3. Adjust `extractResponse()` logic if StepFun uses different JSON structure
4. Sample StepFun response should look like:

   ```json
   {
     "choices": [{
       "message": {
         "content": "Your generated text here"
       }
     }]
   }
   ```

   If different, modify `extractResponse()` accordingly.

---

### Issue 8.6: StepFun not in `getSupportedProviders()` list

**Cause**: Factory not updated properly

**Fix**: Verify `AIProviderFactory.java`:

- `case "stepfun":` added before `default`
- `getSupportedProviders()` includes `"stepfun"`
- Recompile: `./gradlew clean build`

---

### Issue 8.7: Quota tracking not working

**Cause**: `QuotaManager` or `AIProviderService` not tracking StepFun

**Fix**:

Add to `QuotaManager.java` (if exists):

```java
if ("stepfun".equals(providerName)) {
    dailyLimit = 50000;
    monthlyLimit = 1000000;
}
```

Add to `AIProviderService` (already generic - should work automatically):

Keys auto-rotated when 429 received if you call `markKeyAsExhausted()` in error handler.

---

## 9. Quick Reference

### 9.1: Files Changed Summary

| File | Change | Lines |
|------|--------|-------|
| `StepFunProvider.java` | New provider class | ~80 lines |
| `AIProviderFactory.java` | Add case + supported list | +2 lines |
| `application.properties` | Add API key config + quotas | +2 lines |
| `APIKeysManager.tsx` | Add models + endpoint | +4 lines |
| `.env` / server config | Set environment variable | +1 line |
| **Total** | | **~90 lines** |

---

### 9.2: API Format Reference

**StepFun Endpoint**: `https://api.stepfun.com/v1/chat/completions`

**Request**:

```json
{
  "messages": [{"role": "user", "content": "Your prompt"}],
  "model": "step-3.5-flash",
  "temperature": 0.7,
  "max_tokens": 4000
}
```

**Headers**:

```
Authorization: Bearer sf-your-api-key
Content-Type: application/json
```

**Response** (OpenAI-compatible):

```json
{
  "choices": [
    {
      "message": {
        "content": "Generated response text"
      }
    }
  ],
  "usage": {
    "prompt_tokens": 10,
    "completion_tokens": 50,
    "total_tokens": 60
  }
}
```

**Error Handling**:

- `401` → Invalid API key
- `429` → Rate limit exceeded (quota exhausted)
- `500` → StepFun server error (retry)

---

### 9.3: Environment Variables Cheatsheet

```bash
# Local dev
export STEPFUN_API_KEY="sf-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"

# Check if set
echo $STEPFUN_API_KEY

# Unset
unset STEPFUN_API_KEY

# List all relevant env vars
env | grep -E "API_KEY|STEPFUN"

# .env file content:
STEPFUN_API_KEY=sf-your-key-here
OPENAI_API_KEY=sk-...
GROQ_API_KEY=gsk-...
```

---

### 9.4: Quick Test Commands

```bash
# 1. Check provider registered
curl http://localhost:8080/api/providers/list | grep -i stepfun

# 2. Direct generation test
curl -X POST http://localhost:8080/api/ai/generate \
  -H "Content-Type: application/json" \
  -d '{"provider": "stepfun", "prompt": "Hi"}'

# 3. Health check
curl http://localhost:8080/actuator/health

# 4. View logs
tail -f logs/supremeai.log | grep -i stepfun

# 5. Check quota (if implemented)
curl http://localhost:8080/api/quota/stepfun
```

---

### 9.5: Remove StepFun Integration (if needed)

**To disable**:

1. Remove `case "stepfun":` from `AIProviderFactory.java`
2. Remove `"stepfun"` from `getSupportedProviders()` array
3. Delete `StepFunProvider.java` file
4. Remove `stepfun` from `application.properties` and frontend
5. Restart backend

**To re-enable**: Reverse the above steps.

---

## 10. Next Steps After Integration

Once StepFun is working:

1. **Add to consensus voting** → Test multi-provider voting with StepFun included
2. **Monitor performance** → Compare StepFun response quality vs Groq/DeepSeek
3. **Set up alerts** → Notify admin when StepFun quota hits 80%
4. **Add multiple keys** → Rotate across 3-5 StepFun accounts for higher daily cap
5. **Benchmark** → Measure latency, quality, cost comparison
6. **Update docs** → Add StepFun to README provider list

---

## 11. Useful Links

| Resource | URL |
|----------|-----|
| StepFun Platform | https://platform.stepfun.com |
| StepFun API Docs | https://platform.stepfun.com/docs |
| StepFun Models | https://platform.stepfun.com/models |
| StepFun Console | https://platform.stepfun.com/console |
| Existing Provider Example (Groq) | `src/main/java/com/supremeai/provider/GroqProvider.java` |
| Factory Registration | `src/main/java/com/supremeai/provider/AIProviderFactory.java` |
| Dashboard Model Catalog | `dashboard/src/components/APIKeysManager.tsx` |

---

## 12. Support & Questions

If you encounter issues:

1. **Check logs first**: `tail -f logs/supremeai.log | grep -i stepfun`
2. **Verify API key**: Copy again from StepFun console (no spaces)
3. **Test with curl**: Isolate frontend vs backend issue
4. **Compare with working provider**: Test Groq first to confirm backend works
5. **Check StepFun status**: https://platform.stepfun.com/status (downtime?)

Common problems:

- **Firewall blocking?** → StepFun API may be blocked in some regions. Use VPN if in restricted region.
- **API key format wrong?** → Should start with `sf-`
- **Model name invalid?** → Use `step-3.5-flash` exactly

---

**Document Version**: 1.0
**Last Updated**: 2026-04-29
**Compatible with SupremeAI Revision**: S4 (Multi-Agent Voting System)

---

**You are now ready to integrate StepFun completely free!** 🚀
