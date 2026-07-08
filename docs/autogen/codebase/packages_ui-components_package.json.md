# 📄 ফাইল: packages/ui-components/package.json

**প্রকার:** .json  
**সাইজ:** 740 বাইট  
**আপডেট:** 2026-07-08T04:03:20.267560

---

## কোড

```json
{
  "name": "@supremeai/ui-components",
  "version": "0.1.0",
  "private": false,
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
  "peerDependencies": {
    "react": "^18 || ^19",
    "react-dom": "^18 || ^19",
    "@tanstack/react-query": "^5.0.0",
    "@monaco-editor/react": "^4.0.0"
  },
  "peerDependenciesMeta": {
    "react": { "optional": false },
    "react-dom": { "optional": false }
  },
  "devDependencies": {
    "@types/react": "^19.0.0",
    "@types/react-dom": "^19.0.0",
    "typescript": "^5.4.0"
  },
  "files": ["src/**/*"],
  "license": "MIT"
}

```