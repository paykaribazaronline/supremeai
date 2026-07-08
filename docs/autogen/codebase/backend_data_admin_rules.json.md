# 📄 ফাইল: backend/data/admin_rules.json

**প্রকার:** .json  
**সাইজ:** 794 বাইট  
**আপডেট:** 2026-07-08T10:08:43.871766

---

## কোড

```json
{
    "directions": {
        "count": 5,
        "names": [
            "North",
            "South",
            "East",
            "West",
            "Center"
        ],
        "description": "Admin has defined 5 directions. Center is the reference point."
    },
    "image_generation": {
        "allowed": true,
        "max_cost_per_image": 0.01,
        "require_consent": false,
        "preferred_providers": [
            "pollinations",
            "huggingface",
            "local"
        ]
    },
    "skill_installation": {
        "sandbox_duration_hours": 24,
        "auto_install": true,
        "max_install_time_seconds": 30
    },
    "cost_management": {
        "monthly_budget": 30.0,
        "alert_at_percent": 80.0,
        "hard_stop_at_percent": 100.0
    }
}
```