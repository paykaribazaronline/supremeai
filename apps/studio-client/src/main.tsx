// SupremeAI Studio Client v0.0.1
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { App } from './App.tsx'
import { GlobalErrorBoundary } from './components/GlobalErrorBoundary';
import { setupGlobalFetchInterceptor } from './utils/apiInterceptor';
import { ToastProvider } from './contexts/ToastProvider';

setupGlobalFetchInterceptor();

import { startAntiSleepHeartbeat } from './services/heartbeat';
if (import.meta.env.PROD) {
  startAntiSleepHeartbeat();
}

// বাংলা মন্তব্য: অ্যাপ বুটের সাথে সাথে Firebase App ইনিশিয়ালাইজ করা হচ্ছে যাতে
// পরবর্তীতে কোনো auth call-এ "No Firebase App '[DEFAULT]'" এরর না আসে।
import { initFirebase } from './firebase';
initFirebase().catch((e) => {
  if (import.meta.env.PROD) {
    console.error('[Firebase] Eager initialization failed:', e);
  }
});

import { ThemeProvider } from './contexts/ThemeProvider'
// Shared providers (react-query, monaco defaults)
import { SharedProviders } from '@supremeai/ui-components'
import { BrowserRouter } from 'react-router-dom'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ToastProvider>
      <ThemeProvider>
        <SharedProviders>
          <BrowserRouter>
            <GlobalErrorBoundary>
              <App />
            </GlobalErrorBoundary>
          </BrowserRouter>
        </SharedProviders>
      </ThemeProvider>
    </ToastProvider>
  </StrictMode>,
)

// Register Service Worker for offline PWA capabilities
if ('serviceWorker' in navigator && import.meta.env.PROD) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').then((reg) => {
      console.log('[PWA] Service Worker registered:', reg.scope);
    }).catch((err) => {
      console.warn('[PWA] Service Worker registration failed:', err);
    });
  });
}
