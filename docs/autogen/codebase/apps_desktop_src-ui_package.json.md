# 📄 ফাইল: apps/desktop/src-ui/package.json

**প্রকার:** .json  
**সাইজ:** 1,114 বাইট  
**আপডেট:** 2026-07-05T01:29:35.686191

---

## কোড

```json
{
  "name": "supremeai-desktop-ui",
  "version": "0.1.0",
  "private": true,
  "dependencies": {
    "@tauri-apps/api": "^1.5.0",
    "@testing-library/jest-dom": "^5.16.5",
    "@testing-library/react": "^13.4.0",
    "@testing-library/user-event": "^13.5.0",
    "@types/jest": "^29.0.0",
    "@types/node": "^16.18.0",
    "@types/react": "^19.0.0",
    "@types/react-dom": "^19.0.0",
    "react": "^19.2.7",
    "react-dom": "^19.2.7",
    "react-router-dom": "^6.4.0",
    "typescript": "^5.4.0",
    "zustand": "^4.3.9",
    "@supremeai/ui-components": "workspace:*"
  },
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "packageManager": "pnpm@9.0.0",
  "eslintConfig": {
    "extends": [
      "react-app",
      "react-app/jest"
    ]
  },
  "browserslist": {
    "production": [
      ">0.2%",
      "not dead",
      "not op_mini all"
    ],
    "development": [
      "last 1 chrome version",
      "last 1 firefox version",
      "last 1 safari version"
    ]
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^2.0.0",
    "vite": "^7.3.5"
  }
}
```