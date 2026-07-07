# 📄 ফাইল: apps/desktop/src-ui/tsconfig.json

**প্রকার:** .json  
**সাইজ:** 558 বাইট  
**আপডেট:** 2026-07-07T08:33:49.630601

---

## কোড

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "esModuleInterop": false,
    "allowSyntheticDefaultImports": true,
    "strict": true,
    "forceConsistentCasingInFileNames": true,
    "moduleResolution": "node",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx"
  },
  "include": ["src", "src/vite-env.d.ts"],
  "references": [{ "path": "./tsconfig.node.json" }]
}

```