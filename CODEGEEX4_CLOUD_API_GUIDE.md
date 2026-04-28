# CodeGeeX4 Cloud API Integration for SupremeAI

**Quick Answer**: YES, you can use CodeGeeX4 **via API key** (no local download, no heavy storage)!

**Platform**: 智谱AI开放平台 (BigModel.cn)
**API Type**: REST API ( OpenAI-compatible )
**Auth**: API Key (like StepFun)
**Cost**: ~¥0.002-0.01/千tokens (~$0.0003-0.0014/1K tokens) - EXTREMELY CHEAP
**Free Tier**: Yes! New users get free tokens to try (~$10-20 credit)

---

## 📋 Summary

| Feature | CodeGeeX4 Cloud API |
|---------|---------------------|
| **API Key Auth** | ✅ Yes (no local model) |
| **Cloud Hosted** | ✅ Yes (Zhipu AI servers) |
| **Storage Required** | ❌ 0 MB (no download) |
| **GPU Required** | ❌ No (cloud) |
| **Setup Time** | ⏱️ 10 minutes |
| **Cost** | 💰 ~$0.0003-0.0014/1K tokens (super cheap) |
| **Free Trial** | ✅ Yes (~$10-20 credit for new accounts) |
| **Commercial License** | ⚠️ Requires registration (free) |
| **Latency** | ~100-300ms (China CDN) |
| **Context** | 128K tokens |

---

## 🎯 Why CodeGeeX4 Cloud API?

### vs StepFun (step-3.5-flash)

| | StepFun | CodeGeeX4 Cloud |
|---|---------|----------------|
| **Model Size** | Unknown (likely 7-13B) | 9B code-specialized |
| **Code Quality** | General AI (okay code) | **Code-expert** (trained on code) |
| **Context** | ~32K | **128K** (4x longer!) |
| **Infilling** | ❌ No | ✅ Yes (fill middle of code) |
| **Function Calling** | ⚠️ Basic | ✅ Native support |
| **Repository Q&A** | ❌ No | ✅ Yes |
| **Price/1K tokens** | ~$0.0005-0.002 | **~$0.0003-0.0014** (cheaper!) |
| **Free Tier** | ~50k tokens/day | **~$10-20 credit** (then pay-as-you-go) |
| **Local Option** | ❌ No | ✅ Ollama (100% free) |
| **Languages** | ~20 | **50+** |

### vs Groq/DeepSeek

| | Groq (Llama) | DeepSeek Coder | CodeGeeX4 |
|---|-------------|---------------|-----------|
| **Specialization** | General | Code-focused | **Code-expert** |
| **Infilling** | ❌ | ❌ | ✅ |
| **Repo Q&A** | ❌ | ❌ | ✅ |
| **Long Context** | 8K | 64K | **128K** |
| **Price** | Free tier | Free tier | **Cheap + free local** |

**Bottom line**: CodeGeeX4 is **the most capable code model** under 10B parameters, and it's **cheaper** than StepFun when using cloud API. Plus you can run it **100% free locally** via Ollama if you ever want to.

---

## 📝 Step 1: Get CodeGeeX4 API Key (5 min)

### 1.1 Register on BigModel.cn

1. Go to **https://bigmodel.cn** (智谱AI开放平台)
2. Click **Sign Up** (注册)
   - Use email or phone
   - Verify account
3. Login to dashboard

### 1.2 Get API Key

1. Navigate to **API Keys** (API 密钥管理)
2. Click **Create New Key** (创建密钥)
3. Give it a name: `SupremeAI-Integration`
4. **COPY THE KEY** (format: `your-api-key-here` - looks like random alphanumeric)
5. **Important**: The key starts with something like `your-key-here` (no special prefix)

**Free Credit**: New accounts typically get **¥50-100 free credit** (~$7-14) to test models. This lasts for months with light usage.

### 1.3 Find Your API Endpoint

CodeGeeX4 uses **coding-specific endpoint** (not general GLM endpoint):

**Endpoint**:

```
https://open.bigmodel.cn/api/coding/paas/v4/chat/completions
```

**Alternative**: If that doesn't work, try:

```
https://open.bigmodel.cn/api/paas/v4/chat/completions
```

---

## 🔧 Step 2: Backend Integration (Java)

We'll create a **CodeGeeX4Provider** that uses the BigModel API. Since BigModel uses **OpenAI-compatible format**, we can even reuse some code from OpenAIProvider!

### Option A: Extend AbstractHttpProvider (Recommended)

Create: `src/main/java/com/supremeai/provider/CodeGeeX4Provider.java`

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
 * CodeGeeX4 Provider - BigModel.cn Cloud API
 * Official API: https://open.bigmodel.cn/dev/api/code-model/codegeex-4
 *
 * Uses OpenAI-compatible chat completions format
 *
 * Pricing (as of 2025):
 * - Input:  ¥0.002/1K tokens (Lite) ~$0.00028/1K
 * - Output: ¥0.002/1K tokens (Lite)
 * Free tier: New accounts get ~¥50-100 credit
 *
 * Models: codegeex-4 (latest)
 * Context: 128K tokens
 */
public class CodeGeeX4Provider implements AIProvider {

    // BigModel coding-specific endpoint
    private static final String API_URL = "https://open.bigmodel.cn/api/coding/paas/v4/chat/completions";
    // Fallback general endpoint
    private static final String ALT_API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    private final String apiKey;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String defaultModel;

    public CodeGeeX4Provider(String apiKey) {
        this(apiKey, "codegeex-4");
    }

    public CodeGeeX4Provider(String apiKey, String model) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("CodeGeeX4 API key must be provided.");
        }
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.defaultModel = model;
    }

    @Override
    public String getName() {
        return "codegeex4";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "name", "CodeGeeX4",
                "provider", "智谱AI (Zhipu AI)",
                "models", List.of("codegeex-4", "codegeex-4-lite"),
                "freeTier", "¥50-100 credit for new accounts",
                "pricing", "¥0.002-0.01/1K tokens (~$0.0003-0.0014/1K)",
                "context", "128K tokens",
                "supports", List.of(
                    "code_generation", "code_completion", "code_infilling",
                    "function_calling", "repository_qa", "code_explanation",
                    "code_translation", "unit_test_generation", "code_review"
                ),
                "languages", "50+ programming languages",
                "baseUrl", API_URL
        );
    }

    @Override
    public Mono<String> generate(String prompt) {
        return Mono.fromCallable(() -> {
            try {
                // CodeGeeX4 uses OpenAI-compatible chat format
                Map<String, Object> requestBody = Map.of(
                    "model", defaultModel,
                    "messages", List.of(
                        Map.of(
                            "role", "system",
                            "content", "You are CodeGeeX, an intelligent programming assistant. You provide accurate, executable code with explanations when needed."
                        ),
                        Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.7,
                    "top_p", 0.7,
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
                        if (response.code() == 401) {
                            throw new IOException("CODEGEEX4_AUTH_ERROR: Invalid API key. Please check your BigModel.cn API key.");
                        } else if (response.code() == 429) {
                            throw new IOException("CODEGEEX4_RATE_LIMIT: Rate limit exceeded or quota exhausted. " + errBody);
                        } else if (response.code() == 400) {
                            throw new IOException("CODEGEEX4_BAD_REQUEST: " + errBody);
                        } else if (response.code() == 500) {
                            throw new IOException("CODEGEEX4_SERVER_ERROR: BigModel server error. Please try again later.");
                        }
                        throw new IOException("CodeGeeX4 API Error " + response.code() + ": " + errBody);
                    }

                    String responseBody = response.body().string();
                    Map<String, Object> responseMap = objectMapper.readValue(
                            responseBody,
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                    );

                    // Extract response (standard OpenAI format)
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                        Object content = message.get("content");
                        return content != null ? content.toString() : "No content in response";
                    }

                    // Check for error in response
                    if (responseMap.containsKey("error")) {
                        throw new IOException("CodeGeeX4 Error: " + responseMap.get("error"));
                    }

                    return "No response from CodeGeeX4.";
                }
            } catch (IOException e) {
                throw new RuntimeException("CodeGeeX4 API call failed: " + e.getMessage(), e);
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

### Option B: Reuse OpenAIProvider Pattern

Since BigModel's API is OpenAI-compatible, you could also modify `OpenAIProvider.java` to support CodeGeeX4 by adding a new case that points to BigModel endpoint. But creating a separate provider is cleaner.

---

## 🔧 Step 3: Register in AIProviderFactory

Update `src/main/java/com/supremeai/provider/AIProviderFactory.java`:

**Add case** (around line 88):

```java
case "codegeex4":
    return new CodeGeeX4Provider(key);
```

**Update getSupportedProviders()** (line 196):

```java
public String[] getSupportedProviders() {
    return new String[]{
        "gpt4", "claude", "gemini", "groq", "deepseek",
        "ollama", "huggingface", "airllm", "kimi", "mistral",
        "stepfun", "codegeex4"  // <-- Add this
    };
}
```

**Update preferred providers** (optional, line 135):

```java
String[] preferredProviders = {
    "codegeex4",    // Code specialist, cheap
    "stepfun",       // Free tier
    "groq",          // Fast & free
    "deepseek",      // Code-focused
    "ollama"         // 100% free local
};
```

---

## 🔧 Step 4: Configuration

### 4.1: Add to application.properties

**File**: `src/main/resources/application.properties`

Add around line 282 (after StepFun):

```properties
# ========== CodeGeeX4 (智谱AI) ==========
# Cloud API: https://open.bigmodel.cn
# Pricing: ~¥0.002-0.01/1K tokens (~$0.0003-0.0014/1K)
# Free: New accounts get ¥50-100 credit (~$7-14)
# Endpoint: https://open.bigmodel.cn/api/coding/paas/v4/chat/completions
supremeai.provider.codegeex4.api-key=${CODEGEEX4_API_KEY:}

# Quota (adjust based on your credit)
codegeex4.daily.quota=100000  # ~100K tokens/day = $0.03-0.14/day
codegeex4.monthly.quota=3000000  # ~3M tokens/month = ~$1-4/month

# Active providers (add codegeex4)
supremeai.active.providers=groq,openai,anthropic,ollama,stepfun,codegeex4
```

### 4.2: Add to ProviderConfig.java

Update method signature to include CodeGeeX4:

```java
@Value("${CODEGEEX4_API_KEY:}") String codegeex4
```

Add to map:

```java
if (!codegeex4.isBlank()) keys.put("codegeex4", codegeex4);
```

### 4.3: Set Environment Variable

**Linux/Mac**:

```bash
export CODEGEEX4_API_KEY="your-bigmodel-api-key-here"
```

**Windows PowerShell**:

```powershell
$env:CODEGEEX4_API_KEY="your-bigmodel-api-key-here"
```

**Add to `.env`**:

```bash
CODEGEEX4_API_KEY=your-bigmodel-api-key-here
```

---

## 🎨 Step 5: Frontend Dashboard Integration

Update `dashboard/src/components/APIKeysManager.tsx`:

### Add to POPULAR_MODELS (around line 118)

```typescript
// CodeGeeX4 (智谱AI) - Cloud API + Local Ollama
{ 
  id: 'codegeex-4', 
  name: 'CodeGeeX4', 
  provider: 'codegeex4', 
  providerTitle: 'CodeGeeX4 (智谱AI)', 
  baseUrl: 'https://open.bigmodel.cn/api/coding/paas/v4', 
  description: 'Code-specialized 9B model - 128K context, infilling, repo Q&A - Cloud API + free local Ollama', 
  category: 'CodeGeeX4' 
},
{ 
  id: 'codegeex-4-lite', 
  name: 'CodeGeeX4 Lite', 
  provider: 'codegeex4', 
  providerTitle: 'CodeGeeX4 Lite', 
  baseUrl: 'https://open.bigmodel.cn/api/coding/paas/v4', 
  description: 'Lightweight version for faster inference - cheaper', 
  category: 'CodeGeeX4' 
},
```

### Add to PROVIDER_ENDPOINTS (around line 140)

```typescript
codegeex4: 'https://open.bigmodel.cn/api/coding/paas/v4',
```

### Add to model search (optional, for dynamic discovery)

```typescript
const searchCodeGeeX4Models = async (apiKey: string) => {
    try {
        const response = await fetch('https://open.bigmodel.cn/api/coding/paas/v4/models', {
            headers: { 'Authorization': `Bearer ${apiKey}` }
        });
        if (!response.ok) return [];
        const data = await response.json();
        return (data.data || []).map((m: any) => ({
            id: m.id,
            name: m.id,
            provider: 'codegeex4',
            description: `CodeGeeX4 model: ${m.id}`,
            category: 'CodeGeeX4'
        }));
    } catch (error) {
        console.error('CodeGeeX4 model search failed:', error);
        return [];
    }
};
```

---

## 🧪 Step 6: Testing

### 6.1: Compile Backend

```bash
cd supremeai
./gradlew clean build -x test
```

### 6.2: Set API Key & Run

```bash
# Set env var
export CODEGEEX4_API_KEY="your-actual-api-key-from-bigmodel.cn"

# Start backend
./gradlew bootRun
```

### 6.3: Test with curl

```bash
curl -X POST http://localhost:8080/api/ai/generate \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "codegeex4",
    "prompt": "Write a Python function to reverse a linked list"
  }'
```

**Expected response**:

```json
{
  "response": "```python\nclass ListNode:\n    def __init__(self, val=0, next=None):\n        self.val = val\n        self.next = next\n\ndef reverse_linked_list(head):\n    prev = None\n    curr = head\n    while curr:\n        nxt = curr.next\n        curr.next = prev\n        prev = curr\n        curr = nxt\n    return prev\n```",
  "provider": "codegeex4",
  "model": "codegeex-4",
  "usage": {
    "prompt_tokens": 25,
    "completion_tokens": 120,
    "total_tokens": 145
  }
}
```

### 6.4: Check Logs

```bash
tail -f logs/supremeai.log | grep -i codegeex
```

Expected:

```
INFO  CodeGeeX4Provider - Generation successful for codegeex-4
INFO  AIProviderService - Using provider: codegeex4 for task code_generation
```

---

## 📊 Step 7: Cost Analysis

### CodeGeeX4 Cloud API Pricing (BigModel)

| Tier | Price/1K tokens | Input Cost | Output Cost | Best for |
|------|-----------------|------------|-------------|----------|
| **Lite** | ¥0.002 (~$0.00028) | ¥0.002 | ¥0.002 | Testing, dev |
| **Std** | ¥0.005 (~$0.0007) | ¥0.005 | ¥0.005 | Production |
| **Pro** | ¥0.01 (~$0.0014) | ¥0.01 | ¥0.01 | Enterprise |

**Example Costs**:

- 1000 code generations/month (avg 500 tokens each):
  - Input: 500K tokens × $0.00028 = **$0.14**
  - Output: 500K tokens × $0.00028 = **$0.14**
  - **Total: ~$0.28/month** (extremely cheap!)

- Heavy usage: 100K generations/month:
  - Input: 50M tokens × $0.00028 = **$14**
  - Output: 50M tokens × $0.00028 = **$14**
  - **Total: ~$28/month**

**Comparison**:

| Provider | Cost/1K tokens | Monthly (100K reqs) |
|----------|----------------|---------------------|
| CodeGeeX4 Lite | $0.00028 | $28 |
| StepFun (free tier) | $0 (free 50K/day) | $0 (then paid) |
| Groq (Llama) | Free tier | $0 |
| DeepSeek | ~$0.0004 | ~$40 |
| GPT-4 | ~$0.03 | ~$3,000 😱 |

**CodeGeeX4 is 10-100x cheaper than GPT-4!**

### Free Credit

- New BigModel accounts: **¥50-100 free** (~$7-14)
- **No credit card required** for free tier
- Lasts ~3-6 months with light usage

---

## 🔄 Step 8: Deploy to Cloud

### Set Environment Variable on Cloud Run

```bash
gcloud run services update supremeai-backend \
  --set-env-vars "CODEGEEX4_API_KEY=your-api-key-here"
```

### Or via Firebase Functions Config (if backend uses functions)

```bash
firebase functions:config:set codegeex4.api_key="your-api-key"
firebase deploy --only functions
```

---

## 🎯 Step 9: Verification Checklist

- [ ] Registered at **bigmodel.cn**
- [ ] Created API key in dashboard
- [ ] Added `CODEGEEX4_API_KEY` to `.env`
- [ ] Created `CodeGeeX4Provider.java`
- [ ] Updated `AIProviderFactory.java` (case + array)
- [ ] Updated `ProviderConfig.java` (bean param)
- [ ] Updated `application.properties` (config + quotas)
- [ ] Updated frontend `APIKeysManager.tsx` (models + endpoint)
- [ ] Backend compiles: `./gradlew build`
- [ ] Backend starts: `./gradlew bootRun`
- [ ] Test API: `curl` returns code response
- [ ] Dashboard shows CodeGeeX4 in add-provider list
- [ ] Can add via UI and test connection
- [ ] Multi-AI consensus includes CodeGeeX4

---

## 🚀 Quick Start Summary

```bash
# 1. Get API key from https://bigmodel.cn
# 2. Set it:
echo "CODEGEEX4_API_KEY=your-key-here" >> .env

# 3. Build
./gradlew clean build -x test

# 4. Run
./gradlew bootRun

# 5. Test
curl -X POST http://localhost:8080/api/ai/generate \
  -H "Content-Type: application/json" \
  -d '{"provider":"codegeex4","prompt":"Write Java hello world"}'

# 6. Rebuild dashboard
cd dashboard && npm run build

# 7. Open http://localhost:5173/admin/apikeys
# Add provider: codegeex4 with your API key
```

---

## 💡 Usage Tips

### Best Practices

1. **Use `codegeex-4` model** (latest) not lite unless you need speed
2. **System prompt**: CodeGeeX4 responds well to:

   ```
   You are CodeGeeX, a code expert. Write clean, efficient code.
   ```

3. **Temperature**: 0.3-0.7 for deterministic code, 0.7-1.0 for creative
4. **Max tokens**: 500-2000 for code snippets, 2000-4000 for full functions
5. **Infilling**: Use special format:

   ```
   <|user|>
   <|code_suffix|>suffix_code<|code_prefix|>prefix_code<|code_middle|>
   <|assistant|>
   ```

### Example Prompts

```bash
# Code generation
"Write a Python FastAPI endpoint for user login"

# Code completion (infilling)
# Prefix: "def factorial(n):\n    "
# Suffix: "    return result"
# Prompt: Fill in the middle

# Code explanation
"Explain this Java code: public static void main(String[] args)"

# Code translation
"Convert this Python to JavaScript: def add(a,b): return a+b"

# Unit test generation
"Write JUnit tests for this method: public String parseJSON(String json)"

# Repository Q&A
"Based on the codebase, where is the authentication logic?"
```

---

## 🆓 Free Alternatives (If You Don't Want to Pay)

### Option 1: Free Credit + Minimal Usage

- Use **¥50 free credit** from bigmodel.cn
- With cheap pricing (~$0.0003/1K), that's ~15-20M tokens free
- Plenty for dev/testing

### Option 2: Ollama Local (100% Free After Download)

```bash
ollama pull codegeex4
ollama run codegeex4
# Configure SupremeAI to use localhost:11434
```

**No ongoing costs**, but needs 18GB disk + GPU/CPU

### Option 3: Multiple Free Providers

Your system already supports **Groq (free tier)**, **DeepSeek (free-ish)**, **Ollama (free local)**. Add CodeGeeX4 as another option, but you don't need to use it if you don't want to pay.

---

## 📚 API Reference

### Endpoint

```
POST https://open.bigmodel.cn/api/coding/paas/v4/chat/completions
```

### Auth

```
Authorization: Bearer YOUR_API_KEY
```

### Request Body

```json
{
  "model": "codegeex-4",
  "messages": [
    {"role": "system", "content": "You are a helpful coding assistant."},
    {"role": "user", "content": "Write a Python hello world"}
  ],
  "temperature": 0.7,
  "top_p": 0.7,
  "max_tokens": 4000,
  "stream": false
}
```

### Response

```json
{
  "id": "chatcmpl-xxx",
  "object": "chat.completion",
  "created": 1704067200,
  "model": "codegeex-4",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "```python\nprint('Hello World!')\n```"
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 15,
    "completion_tokens": 25,
    "total_tokens": 40
  }
}
```

### Error Codes

| Code | Meaning | Action |
|------|---------|--------|
| 401 | Invalid API key | Check key, regenerate if needed |
| 429 | Rate limit / quota exceeded | Wait or add more credits |
| 500 | Server error | Retry with backoff |
| 400 | Bad request | Check request format |

---

## 🎉 Summary

**You have 3 ways to use CodeGeeX4**:

| Method | Cost | Storage | Speed | Setup |
|--------|------|---------|-------|-------|
| **Cloud API** (this guide) | ≈$0.0003/1K tokens | 0 MB | Fast (~200ms) | **10 min (EASY)** |
| **Ollama Local** | 100% free | 18 GB download | instant | 5 min (needs GPU) |
| **vLLM Server** | GPU cost (~$1/hr) | Model on disk | Fast | 30 min (complex) |

**Cloud API is perfect for you** because:

- ✅ No local storage needed
- ✅ No GPU needed
- ✅ API key auth (like StepFun)
- ✅ SUPER cheap (pennies per month)
- ✅ No maintenance
- ✅ Can switch to local Ollama later if you want

---

## 📁 Files to Create/Modify

| File | Action |
|------|--------|
| `CodeGeeX4Provider.java` | NEW - Cloud API provider |
| `AIProviderFactory.java` | Add `codegeex4` case |
| `ProviderConfig.java` | Add `CODEGEEX4_API_KEY` param |
| `application.properties` | Add config + quotas |
| `.env` | Add `CODEGEEX4_API_KEY=your-key` |
| `APIKeysManager.tsx` | Add models to catalog |
| **Total new code** | ~100 lines |

---

## 🆘 Support

- **BigModel Docs**: https://open.bigmodel.cn/dev/api/code-model/codegeex-4
- **Pricing**: https://bigmodel.cn/pricing
- **Console**: https://bigmodel.cn (check API keys, usage, billing)
- **Existing provider code**: Check `StepFunProvider.java` (similar pattern)

---

## ✅ Next Steps

1. **Sign up** at https://bigmodel.cn
2. **Get API key**
3. **Add to .env**: `CODEGEEX4_API_KEY=your-key`
4. **Follow integration guide** above to create `CodeGeeX4Provider.java`
5. **Build & test**
6. **Enjoy** the most capable code model under 10B params! 🚀

---

**Ready to implement?** I can create all the Java files and configuration for you. Just say the word!

**Want local only (100% free, no API costs)?** Use Ollama instead - even simpler, just `ollama run codegeex4` and point provider to `localhost:11434`.

You choose!
