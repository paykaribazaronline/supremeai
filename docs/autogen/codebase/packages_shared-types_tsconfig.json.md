# 📄 ফাইল: packages/shared-types/tsconfig.json

**প্রকার:** .json  
**সাইজ:** 528 বাইট  
**আপডেট:** 2026-07-11T16:17:51.545483

---

## কোড

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "strict": true,
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
      "@supremeai/shared-types": ["./src/index.ts"]
    }
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist"]
}

```