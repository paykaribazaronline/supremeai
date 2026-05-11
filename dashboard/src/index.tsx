import React from 'react';
import { createRoot } from 'react-dom/client';
import './index.css';
import App from './App';

// Global polyfills
(window as any).global = window;
(window as any).process = { env: {} };

const rootElement = document.getElementById('root');
if (rootElement) {
  rootElement.innerHTML = '<h1 style="color: white; padding: 20px;">Mounting SupremeAI Dashboard...</h1>';
  console.log("Root element found, starting React render...");
  const root = createRoot(rootElement);
  root.render(
    <React.StrictMode>
      <App />
    </React.StrictMode>
  );
} else {
  console.error("Failed to find root element");
}
