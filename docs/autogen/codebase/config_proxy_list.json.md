# 📄 ফাইল: config/proxy_list.json

**প্রকার:** .json  
**সাইজ:** 267 বাইট  
**আপডেট:** 2026-07-07T17:30:18.345998

---

## কোড

```json
{
  "free_providers": ["pubproxy.com/api", "api.proxyscrape.com"],
  "premium_providers": {
    "bright_data": { "enabled": false, "use_cases": ["payments", "kyc"] }
  },
  "rotation_strategy": "round_robin",
  "max_retries": 3,
  "hitl_required_for_premium": true
}

```