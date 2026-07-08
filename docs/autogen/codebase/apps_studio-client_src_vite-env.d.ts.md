# 📄 ফাইল: apps/studio-client/src/vite-env.d.ts

**প্রকার:** .ts  
**সাইজ:** 159 বাইট  
**আপডেট:** 2026-07-08T10:24:21.765818

---

## কোড

```ts
/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

```