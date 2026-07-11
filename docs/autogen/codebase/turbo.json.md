# 📄 ফাইল: turbo.json

**প্রকার:** .json  
**সাইজ:** 677 বাইট  
**আপডেট:** 2026-07-11T20:08:21.301634

---

## কোড

```json
{
  "$schema": "https://turbo.build/schema.json",
  "globalDependencies": ["**/.env.*local"],
  "globalEnv": [
    "NODE_ENV",
    "API_URL",
    "SUPABASE_URL",
    "PINECONE_API_KEY",
    "VITE_PORTAL_TYPE",
    "VITE_API_URL"
  ],
  "tasks": {
    "build": {
      "dependsOn": ["^build"],
      "outputs": [
        ".next/**",
        "!.next/cache/**",
        "dist/**",
        "dist-admin/**",
        "dist-user/**",
        "build/**"
      ]
    },
    "test": {
      "dependsOn": ["build"]
    },
    "lint": {},
    "dev": {
      "cache": false,
      "persistent": true
    },
    "@supremeai/design-tokens#build": {
      "outputs": ["outputs/**"]
    }
  }
}
```