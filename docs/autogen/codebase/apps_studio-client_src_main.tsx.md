# 📄 ফাইল: apps/studio-client/src/main.tsx

**প্রকার:** .tsx  
**সাইজ:** 697 বাইট  
**আপডেট:** 2026-07-04T13:24:28.401780

---

## কোড

```tsx
// SupremeAI Studio Client v0.0.1
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { App } from './App.tsx'
import { getApiBaseUrl } from './utils/api';

// Inject globally for any UI components or legacy scripts that expect it
(window as any).getApiBaseUrl = getApiBaseUrl;

import { ThemeProvider } from './contexts/ThemeContext'
// Shared providers (react-query, monaco defaults)
import { SharedProviders } from '@supremeai/ui-components'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider>
      <SharedProviders>
        <App />
      </SharedProviders>
    </ThemeProvider>
  </StrictMode>,
)


```