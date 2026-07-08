# 📄 ফাইল: config/routing_policy.json

**প্রকার:** .json  
**সাইজ:** 495 বাইট  
**আপডেট:** 2026-07-08T12:03:41.171850

---

## কোড

```json
{
  "rules": [
    { "complexity_max": 3, "model": "ollama/llama3.2" },
    { "complexity_max": 6, "model": "openrouter/mistral-7b-free" },
    { "complexity_max": 8, "model": "gemini/gemini-flash" },
    { "complexity_max": 10, "model": "openai/gpt-4o-mini" }
  ],
  "user_mode_overrides": {
    "FAST_TRACK": { "prefer_local": true, "max_wait_ms": 500 },
    "LEARNING": { "explanatory_models": ["gemini/gemini-pro"] },
    "PRODUCTION": { "prefer_accuracy": true, "min_complexity": 7 }
  }
}

```