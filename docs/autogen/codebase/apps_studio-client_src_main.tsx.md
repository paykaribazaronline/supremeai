# 📄 ফাইল: apps/studio-client/src/main.tsx

**প্রকার:** .tsx  
**সাইজ:** 369 বাইট  
**আপডেট:** 2026-07-03T20:48:16.997826

---

## কোড

```tsx
// SupremeAI Studio Client v0.0.1
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { App } from './App.tsx'

import { ThemeProvider } from './contexts/ThemeContext'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider>
      <App />
    </ThemeProvider>
  </StrictMode>,
)


```