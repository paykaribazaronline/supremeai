import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// বাংলা মন্তব্য: Portal-ভিত্তিক local dev proxy target — admin dev server কখনোই user backend-এ
// (এবং উল্টোটাও) route করবে না, যাতে dev/prod আচরণ একই থাকে (সম্পূর্ণ আইসোলেশন)।
const IS_ADMIN_PORTAL = process.env.VITE_PORTAL_TYPE === 'admin'
const ADMIN_BACKEND = process.env.VITE_ADMIN_BACKEND || 'https://supremeai-admin.onrender.com'
const USER_BACKEND = process.env.VITE_USER_BACKEND || process.env.VITE_API_URL || 'https://supremeai-backend.onrender.com'
const PORTAL_BACKEND = IS_ADMIN_PORTAL ? ADMIN_BACKEND : USER_BACKEND

const devProxy = {
  '/api': {
    target: PORTAL_BACKEND,
    changeOrigin: true
  },
  '/admin-api': {
    target: ADMIN_BACKEND,
    changeOrigin: true
  },
  '/auth': {
    target: PORTAL_BACKEND,
    changeOrigin: true
  }
}

// https://vite.dev/config/
export default defineConfig({
  base: process.env.ELECTRON === 'true' ? './' : '/', // Use './' for Electron, '/' for Web to fix client-side routing and MIME issues
  plugins: [
    react({ jsxRuntime: 'automatic' }),
    tailwindcss({
      config: './tailwind.config.js',
    })
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
    // বাংলা মন্তব্য: প্রোডাকশন-গ্রেড ক্লাউড ব্যাকএন্ড টার্গেট সিঙ্ক (Render Admin/User Service)
    proxy: devProxy
  },
  preview: {
    proxy: devProxy
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
    sourcemap: 'hidden',
  },
  envPrefix: ['VITE_', 'NEXT_PUBLIC_'],
})
