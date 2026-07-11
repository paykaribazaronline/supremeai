# 📄 ফাইল: render.yaml

**প্রকার:** .yaml  
**সাইজ:** 714 বাইট  
**আপডেট:** 2026-07-11T15:50:11.260265

---

## কোড

```yaml
services:
  - type: web
    name: supremeai-backend
    env: image
    image:
      url: ghcr.io/paykaribazaronline/supremeai-backend:latest
    region: singapore
    plan: free
    healthCheckPath: /health
    envVars:
      - key: ENV
        value: production
      - key: PORT
        value: 10000
      - key: SUPABASE_URL
        sync: false
      - key: SUPABASE_KEY
        sync: false
      - key: OPENROUTER_API_KEY
        sync: false
      - key: GEMINI_API_KEY
        sync: false
      - key: SUPREMEAI_JWT_SECRET
        sync: false
      - key: SUPREMEAI_ADMIN_PASSWORD_HASH
        sync: false
      - key: STRIPE_API_KEY
        sync: false
      - key: STRIPE_WEBHOOK_SECRET
        sync: false

```