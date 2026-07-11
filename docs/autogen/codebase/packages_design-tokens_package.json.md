# 📄 ফাইল: packages/design-tokens/package.json

**প্রকার:** .json  
**সাইজ:** 345 বাইট  
**আপডেট:** 2026-07-11T13:13:34.418259

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
  },
  "dependencies": {
    "style-dictionary": "^5.5.0"
  }
}

```