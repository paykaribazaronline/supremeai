# 📄 ফাইল: backend/config/byoc_limits.json

**প্রকার:** .json  
**সাইজ:** 334 বাইট  
**আপডেট:** 2026-07-07T20:32:00.991119

---

## কোড

```json
{
  "limits": {
    "free": {
      "max_containers": 1,
      "max_memory": "256Mi",
      "max_cpu": "500m"
    },
    "pro": {
      "max_containers": 5,
      "max_memory": "1024Mi",
      "max_cpu": "2000m"
    },
    "enterprise": {
      "max_containers": 50,
      "max_memory": "4096Mi",
      "max_cpu": "8000m"
    }
  }
}

```