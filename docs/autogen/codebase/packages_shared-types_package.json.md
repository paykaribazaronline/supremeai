# 📄 ফাইল: packages/shared-types/package.json

**প্রকার:** .json  
**সাইজ:** 328 বাইট  
**আপডেট:** 2026-07-05T19:50:38.939373

---

## কোড

```json
{
  "name": "@supremeai/shared-types",
  "version": "1.0.0",
  "type": "module",
  "main": "./src/index.ts",
  "types": "./src/index.ts",
  "exports": {
    ".": {
      "types": "./src/index.ts",
      "import": "./src/index.ts"
    },
    "./package.json": "./package.json"
  },
  "dependencies": {
    "zod": "^3.23.0"
  }
}

```