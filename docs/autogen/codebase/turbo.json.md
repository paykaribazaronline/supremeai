# 📄 ফাইল: turbo.json

**প্রকার:** .json  
**সাইজ:** 506 বাইট  
**আপডেট:** 2026-07-04T23:04:44.992873

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
    "PINECONE_API_KEY"
  ],
  "tasks": {
    "build": {
      "dependsOn": ["^build"],
      "outputs": [
        ".next/**",
        "!.next/cache/**",
        "dist/**",
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
    }
  }
}

```