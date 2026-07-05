# 📄 ফাইল: apps/desktop/src-ui/vite.config.ts

**প্রকার:** .ts  
**সাইজ:** 248 বাইট  
**আপডেট:** 2026-07-05T19:50:39.062566

---

## কোড

```ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 1420,
    strictPort: true
  },
  preview: {
    port: 1420
  }
})
```