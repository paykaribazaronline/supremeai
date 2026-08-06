import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

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
    proxy: {
      '/api': {
        target: process.env.VITE_API_URL || 'https://supremeai-backend.onrender.com',
        changeOrigin: true
      },
      '/admin-api': {
        target: process.env.VITE_API_URL || 'https://supremeai-admin.onrender.com',
        changeOrigin: true
      },
      '/auth': {
        target: process.env.VITE_API_URL || 'https://supremeai-backend.onrender.com',
        changeOrigin: true
      }
    }
  },
  preview: {
    proxy: {
      '/api': {
        target: process.env.VITE_API_URL || 'https://supremeai-backend.onrender.com',
        changeOrigin: true
      },
      '/admin-api': {
        target: process.env.VITE_API_URL || 'https://supremeai-admin.onrender.com',
        changeOrigin: true
      },
      '/auth': {
        target: process.env.VITE_API_URL || 'https://supremeai-backend.onrender.com',
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
    sourcemap: 'hidden',
  },
  envPrefix: ['VITE_', 'NEXT_PUBLIC_'],
})
