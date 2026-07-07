# 📄 ফাইল: vercel.json

**প্রকার:** .json  
**সাইজ:** 267 বাইট  
**আপডেট:** 2026-07-07T22:11:19.713228

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
  }
}

```