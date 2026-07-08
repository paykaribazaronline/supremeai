# 📄 ফাইল: tools/vscode-extension/tsconfig.json

**প্রকার:** .json  
**সাইজ:** 660 বাইট  
**আপডেট:** 2026-07-08T19:31:06.623246

---

## কোড

```json
{
  "compilerOptions": {
    "module": "commonjs",
    "target": "ES2022",
    "outDir": "out",
    "lib": [
      "ES2022",
      "DOM",
      "DOM.Iterable"
    ],
    "sourceMap": false,
    "rootDir": "src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true,
    "declaration": true,
    "declarationMap": false,
    "noImplicitOverride": true,
    "incremental": true,
    "types": [
      "vscode",
      "node",
      "vitest/globals"
    ]
  },
  "include": [
    "src/**/*"
  ],
  "exclude": [
    "node_modules",
    ".vscode-test",
    "test"
  ]
}
```