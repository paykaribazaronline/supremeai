# 📄 ফাইল: backend/config/routing_policy.json

**প্রকার:** .json  
**সাইজ:** 504 বাইট  
**আপডেট:** 2026-07-07T21:29:49.076632

---

## কোড

```json
{
  "complexity_rules": {
    "easy": [
      "ollama/qwen2.5-coder:1.5b",
      "groq/llama-3.3-70b-versatile"
    ],
    "medium": [
      "deepseek/deepseek-chat",
      "gemini/gemini-1.5-flash"
    ],
    "hard": [
      "deepseek/deepseek-chat",
      "gemini/gemini-3.5-flash",
      "gemini/gemini-1.5-pro",
      "openai/gpt-4o-mini"
    ]
  },
  "fallback_chain": [
    "deepseek/deepseek-chat",
    "groq/llama-3.3-70b-versatile",
    "gemini/gemini-3.5-flash",
    "openai/gpt-4o-mini"
  ]
}

```