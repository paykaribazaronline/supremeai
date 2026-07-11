# 📄 ফাইল: vercel.json

**প্রকার:** .json  
**সাইজ:** 797 বাইট  
**আপডেট:** 2026-07-11T11:29:21.143147

---

## কোড

```json
{
  "version": 2,
  "buildCommand": "pnpm --filter supremeai-studio-client build:user",
  "ignoreCommand": "git diff --quiet HEAD^ HEAD ./apps/studio-client",
  "outputDirectory": "apps/studio-client/dist-user",
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