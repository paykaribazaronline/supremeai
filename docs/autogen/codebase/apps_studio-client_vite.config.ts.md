# 📄 ফাইল: apps/studio-client/vite.config.ts

**প্রকার:** .ts  
**সাইজ:** 798 বাইট  
**আপডেট:** 2026-07-04T23:38:49.248719

---

## কোড

```ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  base: './', // Important for Electron to load local files
  plugins: [
    react({ jsxRuntime: 'automatic' }),
    tailwindcss()
  ],
  esbuild: {
    jsx: 'automatic',
  },
  resolve: {
    dedupe: ['react', 'react-dom', '@tanstack/react-query']
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-ui': ['framer-motion', 'lucide-react', 'recharts'],
          'vendor-flow': ['reactflow'],
          'vendor-query': ['@tanstack/react-query'],
        },
      },
    },
    chunkSizeWarningLimit: 600,
    sourcemap: false,
  },
  envPrefix: ['VITE_', 'NEXT_PUBLIC_'],
})

```