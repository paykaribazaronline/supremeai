# 📄 ফাইল: apps/studio-client/src/main.tsx

**প্রকার:** .tsx  
**সাইজ:** 1,011 বাইট  
**আপডেট:** 2026-07-07T08:19:30.114605

---

## কোড

```tsx
// SupremeAI Studio Client v0.0.1
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { App } from './App.tsx'
import { getApiBaseUrl } from './utils/api';
import { setupGlobalFetchInterceptor } from './utils/apiInterceptor';
import { ToastProvider } from './contexts/ToastContext';

setupGlobalFetchInterceptor();

// Inject globally for any UI components or legacy scripts that expect it
(window as any).getApiBaseUrl = getApiBaseUrl;

import { ThemeProvider } from './contexts/ThemeContext'
// Shared providers (react-query, monaco defaults)
import { SharedProviders } from '@supremeai/ui-components'
import { BrowserRouter } from 'react-router-dom'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ToastProvider>
      <ThemeProvider>
        <SharedProviders>
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </SharedProviders>
      </ThemeProvider>
    </ToastProvider>
  </StrictMode>,
)


```