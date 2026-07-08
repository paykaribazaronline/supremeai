# 📄 ফাইল: packages/ui-components/tsconfig.json

**প্রকার:** .json  
**সাইজ:** 553 বাইট  
**আপডেট:** 2026-07-08T10:38:47.140873

---

## কোড

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "strict": true,
    "jsx": "react-jsx",
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "declaration": true,
    "declarationMap": true,
    "outDir": "./dist",
    "rootDir": "./src",
    "baseUrl": ".",
    "ignoreDeprecations": "5.0",
    "paths": {
      "@supremeai/ui-components": ["./src/index.ts"]
    }
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist"]
}

```