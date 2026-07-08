# 📄 ফাইল: vercel.json

**প্রকার:** .json  
**সাইজ:** 781 বাইট  
**আপডেট:** 2026-07-08T17:52:37.372663

---

## কোড

```json
{
  "buildCommand": "pnpm turbo run build --filter=supremeai-studio-client",
  "ignoreCommand": "git diff --quiet HEAD^ HEAD ./apps/studio-client",
  "outputDirectory": "apps/studio-client/dist",
  "framework": "vite",
  "env": {
    "VITE_PORTAL_TYPE": "user"
  },
  "rewrites": [
    {
      "source": "/api/(.*)",
      "destination": "https://supremeai-api-lhlwyikwlq-uc.a.run.app/api/$1"
    },
    {
      "source": "/admin-api/(.*)",
      "destination": "https://supremeai-api-lhlwyikwlq-uc.a.run.app/admin-api/$1"
    },
    {
      "source": "/ws/(.*)",
      "destination": "https://supremeai-api-lhlwyikwlq-uc.a.run.app/ws/$1"
    },
    {
      "source": "/telemetry/(.*)",
      "destination": "https://supremeai-api-lhlwyikwlq-uc.a.run.app/telemetry/$1"
    }
  ]
}

```