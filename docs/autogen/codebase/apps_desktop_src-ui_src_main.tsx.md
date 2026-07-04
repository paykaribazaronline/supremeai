# 📄 ফাইল: apps/desktop/src-ui/src/main.tsx

**প্রকার:** .tsx  
**সাইজ:** 351 বাইট  
**আপডেট:** 2026-07-04T12:27:27.531970

---

## কোড

```tsx
import React from "react"
import ReactDOM from "react-dom/client"
import App from "./App"
import { SharedProviders } from '@supremeai/ui-components'
import "./index.css"

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <SharedProviders>
      <App />
    </SharedProviders>
  </React.StrictMode>
)

```