# 📄 ফাইল: packages/design-tokens/package.json

**প্রকার:** .json  
**সাইজ:** 287 বাইট  
**আপডেট:** 2026-07-11T09:15:33.959630

---

## কোড

```json
{
  "name": "@supremeai/design-tokens",
  "version": "1.0.0",
  "description": "Single source of truth for SupremeAI 2.0 design tokens",
  "main": "outputs/tokens.js",
  "types": "outputs/tokens.d.ts",
  "scripts": {
    "build": "node build.js && node scripts/copy-to-flutter.js"
  }
}

```