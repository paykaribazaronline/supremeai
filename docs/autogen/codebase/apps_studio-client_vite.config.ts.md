# 📄 ফাইল: apps/studio-client/vite.config.ts

**প্রকার:** .ts  
**সাইজ:** 1,866 বাইট  
**আপডেট:** 2026-07-11T09:05:57.924072

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
    drop: process.env.NODE_ENV === 'production' ? ['console', 'debugger'] : [],
  },
  resolve: {
    dedupe: ['react', 'react-dom', '@tanstack/react-query']
  },
  server: {
    headers: {
      'Cross-Origin-Embedder-Policy': 'require-corp',
      'Cross-Origin-Opener-Policy': 'same-origin',
    },
    proxy: {
      '/api': {
        target: process.env.VITE_API_URL || (process.env.NODE_ENV === 'production' ? '' : 'http://127.0.0.1:8000'),
        changeOrigin: true
      },
      '/admin-api': {
        target: process.env.VITE_API_URL || (process.env.NODE_ENV === 'production' ? '' : 'http://127.0.0.1:8000'),
        changeOrigin: true
      }
    }
  },
  preview: {
    proxy: {
      '/api': {
        target: process.env.VITE_API_URL || (process.env.NODE_ENV === 'production' ? '' : 'http://127.0.0.1:8000'),
        changeOrigin: true
      },
      '/admin-api': {
        target: process.env.VITE_API_URL || (process.env.NODE_ENV === 'production' ? '' : 'http://127.0.0.1:8000'),
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: process.env.VITE_PORTAL_TYPE === 'admin' ? 'dist-admin' : 'dist-user',
    emptyOutDir: true,
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