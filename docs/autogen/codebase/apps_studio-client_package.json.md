# 📄 ফাইল: apps/studio-client/package.json

**প্রকার:** .json  
**সাইজ:** 2,576 বাইট  
**আপডেট:** 2026-07-04T10:39:00.889497

---

## কোড

```json
{
  "name": "supremeai-studio-client",
  "description": "SupremeAI Studio Client - Multi-cloud AI orchestration platform web interface",
  "author": "SupremeAI Team",
  "repository": "https://github.com/paykaribazaronline/supremeai",
  "private": true,
  "version": "0.0.1",
  "type": "module",
  "main": "main.js",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "build:report": "vite build --mode production --reporter=json",
    "lint": "eslint .",
    "preview": "vite preview",
    "electron:dev": "concurrently -k \"cross-env BROWSER=none pnpm run dev\" \"wait-on http://127.0.0.1:5173 && electron .\"",
    "electron:build": "pnpm run build && electron-builder",
    "test": "vitest run",
    "test:watch": "vitest"
  },
  "dependencies": {
    "@dataconnect/generated": "file:src/dataconnect-generated",
    "@supremeai/ui-components": "workspace:*",
    "@monaco-editor/react": "^4.7.0",
    "@tailwindcss/vite": "^4.2.4",
    "@tanstack/react-query": "^5.101.0",
    "firebase": "^10.8.0",
    "framer-motion": "^12.42.0",
    "i18next": "^23.4.0",
    "lucide-react": "^1.21.0",
    "react": "^19.2.5",
    "react-dom": "^19.2.5",
    "react-i18next": "^15.4.1",
    "reactflow": "^11.11.4",
    "recharts": "^3.8.1",
    "tailwindcss": "^4.2.4",
    "zustand": "^5.0.14"
  },
  "devDependencies": {
    "@eslint/js": "^10.0.1",
    "@testing-library/dom": "^10.4.1",
    "@testing-library/jest-dom": "^6.4.0",
    "@testing-library/react": "^16.0.0",
    "@testing-library/user-event": "^14.5.0",
    "@types/node": "^24.12.2",
    "@types/react": "^19.2.14",
    "@types/react-dom": "^19.2.3",
    "@vitejs/plugin-react": "^4.3.0",
    "concurrently": "^9.2.1",
    "cross-env": "^10.1.0",
    "electron": "^41.8.0",
    "electron-builder": "^24.13.3",
    "eslint": "^10.2.1",
    "eslint-plugin-react-hooks": "^7.1.1",
    "eslint-plugin-react-refresh": "^0.5.2",
    "globals": "^17.5.0",
    "jsdom": "^24.0.0",
    "typescript": "~6.0.2",
    "typescript-eslint": "^8.58.2",
    "vite": "^7.3.5",
    "vitest": "^3.2.6",
    "wait-on": "^9.0.5"
  },
  "build": {
    "appId": "com.supremeai.studio",
    "productName": "SupremeAI Studio",
    "directories": {
      "buildResources": "assets"
    },
    "files": [
      "dist/**/*",
      "node_modules/**/*",
      "package.json",
      "preload.cjs",
      "main.js"
    ],
    "win": {
      "target": [
        "nsis"
      ]
    },
    "nsis": {
      "oneClick": false,
      "allowToChangeInstallationDirectory": true
    },
    "publish": {
      "provider": "github"
    }
  }
}
 

```