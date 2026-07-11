# 📄 ফাইল: config/vercel.json

**প্রকার:** .json  
**সাইজ:** 257 বাইট  
**আপডেট:** 2026-07-11T15:05:35.218832

---

## কোড

```json
{
  "$schema": "https://openapi.vercel.sh/vercel.json",
  "buildCommand": "pnpm turbo run build --filter=supremeai-studio-client",
  "outputDirectory": "apps/studio-client/dist",
  "installCommand": "pnpm install --frozen-lockfile",
  "framework": "vite"
}

```