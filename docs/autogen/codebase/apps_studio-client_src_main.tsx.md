# 📄 ফাইল: apps/studio-client/src/main.tsx

**প্রকার:** .tsx  
**সাইজ:** 530 বাইট  
**আপডেট:** 2026-07-04T12:50:55.989247

---

## কোড

```tsx
// SupremeAI Studio Client v0.0.1
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { App } from './App.tsx'

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