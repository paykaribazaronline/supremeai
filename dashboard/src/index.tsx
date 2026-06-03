import React from 'react';
import { createRoot } from 'react-dom/client';
import axios from 'axios';
import './index.css';
import './i18n/conf'; // Initialize i18n
import App from './App';
import { RoleProvider } from './contexts/RoleContext';

// Set global axios baseURL
axios.defaults.baseURL = import.meta.env.VITE_API_URL || '';

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
      <RoleProvider>
        <App />
      </RoleProvider>
    </React.StrictMode>
  );
} else {
  console.error("Failed to find root element");
}
