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
