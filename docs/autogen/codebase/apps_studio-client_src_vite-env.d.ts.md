# 📄 ফাইল: apps/studio-client/src/vite-env.d.ts

**প্রকার:** .ts  
**সাইজ:** 159 বাইট  
**আপডেট:** 2026-07-07T19:34:31.472786

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