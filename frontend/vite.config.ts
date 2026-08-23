import { defineConfig, loadEnv } from 'vite'

// Load environment variables so the config guard can read them from .env.local
Object.assign(process.env, loadEnv(process.env.NODE_ENV || 'development', process.cwd(), ''))
import fs from 'fs'
import path from 'path'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// বাংলা মন্তব্য: Portal-ভিত্তিক local dev proxy target — admin dev server কখনোই user backend-এ
// (এবং উল্টোটাও) route করবে fix client-side routing and MIME issues
// 🔧 DYNAMIC CONFIG: No hardcoded URLs — Fail-Fast in production
const IS_ADMIN_PORTAL = process.env.VITE_PORTAL_TYPE === 'admin'
const ADMIN_BACKEND = process.env.VITE_ADMIN_BACKEND || process.env.RENDER_SERVICE_URL || ''
const USER_BACKEND = process.env.VITE_USER_BACKEND || process.env.VITE_API_URL || process.env.RENDER_SERVICE_URL || ''

// 🔒 PRODUCTION GUARD: Missing backend URL = Build failure (not silent wrong URL)
if (process.env.NODE_ENV === 'production' && !ADMIN_BACKEND && IS_ADMIN_PORTAL) {
  console.error('❌ FATAL: VITE_ADMIN_BACKEND environment variable is required in production!')
  process.exit(1)
}
if (process.env.NODE_ENV === 'production' && !USER_BACKEND && !IS_ADMIN_PORTAL) {
  console.error('❌ FATAL: VITE_USER_BACKEND environment variable is required in production!')
  process.exit(1)
}

const PORTAL_BACKEND = IS_ADMIN_PORTAL ? ADMIN_BACKEND : USER_BACKEND

// 🔬 Evolution v3.0: Dump build config for debugging
const buildInfoPlugin = () => {
  return {
    name: 'build-info-plugin',
    writeBundle(options: any) {
      if (process.env.NODE_ENV === 'production') {
        const buildInfo = {
          timestamp: new Date().toISOString(),
          portalType: IS_ADMIN_PORTAL ? 'admin' : 'user',
          backendUrl: PORTAL_BACKEND,
          coopHeader: process.env.COOP_HEADER,
          coepHeader: process.env.COEP_HEADER,
        }
        const outDir = options.dir || 'dist'
        fs.writeFileSync(path.join(outDir, 'build-info.json'), JSON.stringify(buildInfo, null, 2))
        console.log(`📋 Build info written to ${outDir}/build-info.json`)
      }
    }
  }
}

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
    }),
    buildInfoPlugin()
  ],
  esbuild: {
    jsx: 'automatic',
    drop: process.env.NODE_ENV === 'production' ? ['console', 'debugger'] : [],
  },
  resolve: {
    dedupe: ['react', 'react-dom', '@tanstack/react-query']
  },
  server: {
    // 🔧 DYNAMIC SECURITY HEADERS from environment
    headers: {
      'Cross-Origin-Embedder-Policy': process.env.COOP_HEADER || 'cross-origin',
      'Cross-Origin-Opener-Policy': process.env.COEP_HEADER || 'unsafe-none',
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
