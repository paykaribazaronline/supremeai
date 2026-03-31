package org.example.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Agent E: Web/React PWA Code Generator
 * Generates Progressive Web Applications using React TypeScript
 */
@Service
public class EWebAgent {

    /**
     * Generate complete React PWA project
     */
    public ReactProjectOutput generateReactProject(ReactProjectRequest request) {
        ReactProjectOutput output = new ReactProjectOutput();
        output.setTimestamp(LocalDateTime.now());
        output.setProjectName(request.getProjectName());
        output.setReactVersion(request.getReactVersion());

        // Generate package.json
        output.setPackageJson(generatePackageJson(request));

        // Generate TypeScript configuration
        output.setTsConfig(generateTsConfig(request));

        // Generate React components
        output.setAppComponent(generateAppComponent(request));
        output.setMainComponents(generateMainComponents(request));
        output.setHookComponents(generateHookComponents(request));

        // Generate state management (Redux)
        output.setReduxStore(generateReduxStore(request));
        output.setReduxSlices(generateReduxSlices(request));

        // Generate API client
        output.setApiClient(generateApiClient(request));

        // Generate styles
        output.setTailwindConfig(generateTailwindConfig(request));
        output.setGlobalStyles(generateGlobalStyles(request));

        // Generate PWA manifests
        output.setManifestJson(generateManifestJson(request));
        output.setServiceWorker(generateServiceWorker(request));

        // Generate tests
        output.setComponentTests(generateComponentTests(request));
        output.setIntegrationTests(generateIntegrationTests(request));

        // Generate utilities
        output.setUtilities(generateUtilities(request));

        // Generate environment configuration
        output.setEnvConfig(generateEnvConfig(request));

        output.setGenerationSuccess(true);
        output.setStatusMessage("React PWA project generated successfully");
        output.setLinesOfCode(estimateLinesOfCode(output));

        return output;
    }

    /**
     * Generate package.json with all dependencies
     */
    private String generatePackageJson(ReactProjectRequest request) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"name\": \"").append(request.getProjectName()).append("\",\n");
        json.append("  \"version\": \"1.0.0\",\n");
        json.append("  \"description\": \"React PWA").append(request.isOfflineSupportEnabled() ? " with offline support" : "").append("\",\n");
        json.append("  \"private\": true,\n");
        json.append("  \"dependencies\": {\n");
        json.append("    \"react\": \"^").append(request.getReactVersion()).append("\",\n");
        json.append("    \"react-dom\": \"^").append(request.getReactVersion()).append("\",\n");
        json.append("    \"@reduxjs/toolkit\": \"^1.9.0\",\n");
        json.append("    \"react-redux\": \"^8.1.0\",\n");
        json.append("    \"axios\": \"^1.4.0\",\n");
        json.append("    \"react-router-dom\": \"^6.12.0\",\n");
        json.append("    \"tailwindcss\": \"^3.3.0\",\n");
        json.append("    \"@heroicons/react\": \"^2.0.0\"\n");
        json.append("  },\n");
        json.append("  \"devDependencies\": {\n");
        json.append("    \"typescript\": \"^5.1.0\",\n");
        json.append("    \"@types/react\": \"^18.0.0\",\n");
        json.append("    \"@types/node\": \"^18.0.0\",\n");
        json.append("    \"@testing-library/react\": \"^13.4.0\",\n");
        json.append("    \"vitest\": \"^0.33.0\"\n");
        json.append("  },\n");
        json.append("  \"scripts\": {\n");
        json.append("    \"dev\": \"vite\",\n");
        json.append("    \"build\": \"tsc && vite build\",\n");
        json.append("    \"test\": \"vitest\",\n");
        json.append("    \"preview\": \"vite preview\"\n");
        json.append("  }\n");
        json.append("}\n");
        return json.toString();
    }

    /**
     * Generate TypeScript configuration
     */
    private String generateTsConfig(ReactProjectRequest request) {
        StringBuilder config = new StringBuilder();
        config.append("{\n");
        config.append("  \"compilerOptions\": {\n");
        config.append("    \"target\": \"ES2020\",\n");
        config.append("    \"useDefineForClassFields\": true,\n");
        config.append("    \"lib\": [\"ES2020\", \"DOM\", \"DOM.Iterable\"],\n");
        config.append("    \"module\": \"ESNext\",\n");
        config.append("    \"skipLibCheck\": true,\n");
        config.append("    \"esModuleInterop\": true,\n");
        config.append("    \"allowSyntheticDefaultImports\": true,\n");
        config.append("    \"strict\": true,\n");
        config.append("    \"resolveJsonModule\": true,\n");
        config.append("    \"jsx\": \"react-jsx\"\n");
        config.append("  },\n");
        config.append("  \"include\": [\"src\"],\n");
        config.append("  \"references\": [{ \"path\": \"./tsconfig.node.json\" }]\n");
        config.append("}\n");
        return config.toString();
    }

    /**
     * Generate main App component
     */
    private String generateAppComponent(ReactProjectRequest request) {
        StringBuilder component = new StringBuilder();
        component.append("import React from 'react';\n");
        component.append("import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';\n");
        component.append("import { Provider } from 'react-redux';\n");
        component.append("import store from './redux/store';\n");
        component.append("import Navbar from './components/Navbar';\n");
        component.append("import Footer from './components/Footer';\n");
        component.append("import HomePage from './pages/HomePage';\n");
        component.append("import Dashboard from './pages/Dashboard';\n");
        component.append("import Settings from './pages/Settings';\n");
        component.append("import './App.css';\n\n");
        component.append("function App() {\n");
        component.append("  React.useEffect(() => {\n");
        component.append("    if ('serviceWorker' in navigator) {\n");
        component.append("      navigator.serviceWorker.register('./serviceWorker.js')\n");
        component.append("        .then(reg => console.log('Service Worker registered', reg))\n");
        component.append("        .catch(err => console.log('Service Worker registration failed', err));\n");
        component.append("    }\n");
        component.append("  }, []);\n\n");
        component.append("  return (\n");
        component.append("    <Provider store={store}>\n");
        component.append("      <Router>\n");
        component.append("        <div className=\"app-container\">\n");
        component.append("          <Navbar />\n");
        component.append("          <main className=\"main-content\">\n");
        component.append("            <Routes>\n");
        component.append("              <Route path=\"/\" element={<HomePage />} />\n");
        component.append("              <Route path=\"/dashboard\" element={<Dashboard />} />\n");
        component.append("              <Route path=\"/settings\" element={<Settings />} />\n");
        component.append("            </Routes>\n");
        component.append("          </main>\n");
        component.append("          <Footer />\n");
        component.append("        </div>\n");
        component.append("      </Router>\n");
        component.append("    </Provider>\n");
        component.append("  );\n");
        component.append("}\n\n");
        component.append("export default App;\n");
        return component.toString();
    }

    /**
     * Generate main React components
     */
    private String generateMainComponents(ReactProjectRequest request) {
        StringBuilder components = new StringBuilder();
        components.append("// Navbar Component\n");
        components.append("export const Navbar = () => (\n");
        components.append("  <nav className=\"navbar\">\n");
        components.append("    <div className=\"nav-brand\">").append(request.getProjectName()).append("</div>\n");
        components.append("    <ul className=\"nav-menu\">\n");
        components.append("      <li><a href=\"/\">Home</a></li>\n");
        components.append("      <li><a href=\"/dashboard\">Dashboard</a></li>\n");
        components.append("      <li><a href=\"/settings\">Settings</a></li>\n");
        components.append("    </ul>\n");
        components.append("  </nav>\n");
        components.append(");\n\n");
        components.append("// Footer Component\n");
        components.append("export const Footer = () => (\n");
        components.append("  <footer className=\"footer\">\n");
        components.append("    <p>&copy; 2024 ").append(request.getProjectName()).append(". All rights reserved.</p>\n");
        components.append("  </footer>\n");
        components.append(");\n\n");
        components.append("// Card Component\n");
        components.append("export const Card = ({ title, children }) => (\n");
        components.append("  <div className=\"card\">\n");
        components.append("    <h3 className=\"card-title\">{title}</h3>\n");
        components.append("    <div className=\"card-content\">{children}</div>\n");
        components.append("  </div>\n");
        components.append(");\n\n");
        components.append("// Button Component\n");
        components.append("export const Button = ({ children, onClick, variant = 'primary' }) => (\n");
        components.append("  <button className={`btn btn-${variant}`} onClick={onClick}>\n");
        components.append("    {children}\n");
        components.append("  </button>\n");
        components.append(");\n");
        return components.toString();
    }

    /**
     * Generate custom React hooks
     */
    private String generateHookComponents(ReactProjectRequest request) {
        StringBuilder hooks = new StringBuilder();
        hooks.append("import { useEffect, useState } from 'react';\nimport { useDispatch } from 'react-redux';\n\n");
        hooks.append("export const useAsync = (callback, immediate = true) => {\n");
        hooks.append("  const [status, setStatus] = useState('idle');\n");
        hooks.append("  const [data, setData] = useState(null);\n");
        hooks.append("  const [error, setError] = useState(null);\n\n");
        hooks.append("  useEffect(() => {\n");
        hooks.append("    if (!immediate) return;\n");
        hooks.append("    setStatus('pending');\n");
        hooks.append("    callback()\n");
        hooks.append("      .then(res => { setData(res); setStatus('success'); })\n");
        hooks.append("      .catch(err => { setError(err); setStatus('error'); });\n");
        hooks.append("  }, [callback, immediate]);\n");
        hooks.append("  return { status, data, error };\n");
        hooks.append("};\n\n");
        hooks.append("export const useLocalStorage = (key, initialValue) => {\n");
        hooks.append("  const [storedValue, setStoredValue] = useState(() => {\n");
        hooks.append("    try {\n");
        hooks.append("      const item = window.localStorage.getItem(key);\n");
        hooks.append("      return item ? JSON.parse(item) : initialValue;\n");
        hooks.append("    } catch (error) {\n");
        hooks.append("      console.error(error);\n");
        hooks.append("      return initialValue;\n");
        hooks.append("    }\n");
        hooks.append("  });\n");
        hooks.append("  return [storedValue, (value) => {\n");
        hooks.append("    window.localStorage.setItem(key, JSON.stringify(value));\n");
        hooks.append("    setStoredValue(value);\n");
        hooks.append("  }];\n");
        hooks.append("};\n");
        return hooks.toString();
    }

    /**
     * Generate Redux store setup
     */
    private String generateReduxStore(ReactProjectRequest request) {
        StringBuilder store = new StringBuilder();
        store.append("import { configureStore } from '@reduxjs/toolkit';\n");
        store.append("import appReducer from './slices/appSlice';\n");
        store.append("import userReducer from './slices/userSlice';\n\n");
        store.append("const store = configureStore({\n");
        store.append("  reducer: {\n");
        store.append("    app: appReducer,\n");
        store.append("    user: userReducer,\n");
        store.append("  },\n");
        store.append("});\n\n");
        store.append("export type RootState = ReturnType<typeof store.getState>;\n");
        store.append("export type AppDispatch = typeof store.dispatch;\n\n");
        store.append("export default store;\n");
        return store.toString();
    }

    /**
     * Generate Redux slices
     */
    private String generateReduxSlices(ReactProjectRequest request) {
        StringBuilder slices = new StringBuilder();
        slices.append("import { createSlice, PayloadAction } from '@reduxjs/toolkit';\n\n");
        slices.append("interface AppState {\n");
        slices.append("  items: any[];\n");
        slices.append("  loading: boolean;\n");
        slices.append("  error: string | null;\n");
        slices.append("}\n\n");
        slices.append("const initialState: AppState = {\n");
        slices.append("  items: [],\n");
        slices.append("  loading: false,\n");
        slices.append("  error: null,\n");
        slices.append("};\n\n");
        slices.append("const appSlice = createSlice({\n");
        slices.append("  name: 'app',\n");
        slices.append("  initialState,\n");
        slices.append("  reducers: {\n");
        slices.append("    setLoading: (state, action) => { state.loading = action.payload; },\n");
        slices.append("    setItems: (state, action) => { state.items = action.payload; },\n");
        slices.append("    setError: (state, action) => { state.error = action.payload; },\n");
        slices.append("    addItem: (state, action) => { state.items.push(action.payload); },\n");
        slices.append("  },\n");
        slices.append("});\n\n");
        slices.append("export const { setLoading, setItems, setError, addItem } = appSlice.actions;\n");
        slices.append("export default appSlice.reducer;\n");
        return slices.toString();
    }

    /**
     * Generate API client
     */
    private String generateApiClient(ReactProjectRequest request) {
        StringBuilder apiClient = new StringBuilder();
        apiClient.append("import axios, { AxiosInstance } from 'axios';\n\n");
        apiClient.append("class ApiClient {\n");
        apiClient.append("  private client: AxiosInstance;\n\n");
        apiClient.append("  constructor() {\n");
        apiClient.append("    this.client = axios.create({\n");
        apiClient.append("      baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080/api',\n");
        apiClient.append("      timeout: 10000,\n");
        apiClient.append("    });\n");
        apiClient.append("  }\n\n");
        apiClient.append("  async getItems() {\n");
        apiClient.append("    const response = await this.client.get('/items');\n");
        apiClient.append("    return response.data;\n");
        apiClient.append("  }\n\n");
        apiClient.append("  async createItem(item: any) {\n");
        apiClient.append("    const response = await this.client.post('/items', item);\n");
        apiClient.append("    return response.data;\n");
        apiClient.append("  }\n\n");
        apiClient.append("  async deleteItem(id: string) {\n");
        apiClient.append("    const response = await this.client.delete(`/items/${id}`);\n");
        apiClient.append("    return response.data;\n");
        apiClient.append("  }\n");
        apiClient.append("}\n\n");
        apiClient.append("export default new ApiClient();\n");
        return apiClient.toString();
    }

    /**
     * Generate Tailwind CSS configuration
     */
    private String generateTailwindConfig(ReactProjectRequest request) {
        StringBuilder config = new StringBuilder();
        config.append("module.exports = {\n");
        config.append("  content: [\n");
        config.append("    './index.html',\n");
        config.append("    './src/**/*.{js,ts,jsx,tsx}',\n");
        config.append("  ],\n");
        config.append("  theme: {\n");
        config.append("    extend: {},\n");
        config.append("  },\n");
        config.append("  plugins: [],\n");
        config.append("};\n");
        return config.toString();
    }

    /**
     * Generate global styles
     */
    private String generateGlobalStyles(ReactProjectRequest request) {
        StringBuilder styles = new StringBuilder();
        styles.append(":root {\n");
        styles.append("  --primary-color: #3b82f6;\n");
        styles.append("  --secondary-color: #10b981;\n");
        styles.append("  --danger-color: #ef4444;\n");
        styles.append("  --spacing: 1rem;\n");
        styles.append("}\n\n");
        styles.append("body {\n");
        styles.append("  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto';\n");
        styles.append("  margin: 0;\n");
        styles.append("  padding: 0;\n");
        styles.append("}\n\n");
        styles.append(".navbar { background-color: var(--primary-color); color: white; padding: var(--spacing); }\n");
        styles.append(".footer { background-color: #333; color: white; padding: var(--spacing); margin-top: var(--spacing); }\n");
        styles.append(".card { border: 1px solid #e5e7eb; border-radius: 0.5rem; padding: var(--spacing); margin: var(--spacing); }\n");
        styles.append(".btn { padding: 0.5rem 1rem; border: none; border-radius: 0.25rem; cursor: pointer; }\n");
        styles.append(".btn-primary { background-color: var(--primary-color); color: white; }\n");
        styles.append(".btn-secondary { background-color: var(--secondary-color); color: white; }\n");
        return styles.toString();
    }

    /**
     * Generate PWA manifest.json
     */
    private String generateManifestJson(ReactProjectRequest request) {
        StringBuilder manifest = new StringBuilder();
        manifest.append("{\n");
        manifest.append("  \"name\": \"").append(request.getProjectName()).append("\",\n");
        manifest.append("  \"short_name\": \"").append(request.getProjectName()).append("\",\n");
        manifest.append("  \"description\": \"Progressive Web Application\",\n");
        manifest.append("  \"start_url\": \"/\",\n");
        manifest.append("  \"display\": \"standalone\",\n");
        manifest.append("  \"background_color\": \"#ffffff\",\n");
        manifest.append("  \"theme_color\": \"#3b82f6\",\n");
        manifest.append("  \"icons\": [\n");
        manifest.append("    {\n");
        manifest.append("      \"src\": \"/icon-192.png\",\n");
        manifest.append("      \"sizes\": \"192x192\",\n");
        manifest.append("      \"type\": \"image/png\"\n");
        manifest.append("    },\n");
        manifest.append("    {\n");
        manifest.append("      \"src\": \"/icon-512.png\",\n");
        manifest.append("      \"sizes\": \"512x512\",\n");
        manifest.append("      \"type\": \"image/png\"\n");
        manifest.append("    }\n");
        manifest.append("  ]\n");
        manifest.append("}\n");
        return manifest.toString();
    }

    /**
     * Generate Service Worker for offline support
     */
    private String generateServiceWorker(ReactProjectRequest request) {
        StringBuilder sw = new StringBuilder();
        sw.append("const CACHE_NAME = 'v1';\n");
        sw.append("const URLS_TO_CACHE = [\n");
        sw.append("  '/',\n");
        sw.append("  '/index.html',\n");
        sw.append("  '/css/main.css',\n");
        sw.append("  '/js/main.js'\n");
        sw.append("];\n\n");
        sw.append("self.addEventListener('install', event => {\n");
        sw.append("  event.waitUntil(\n");
        sw.append("    caches.open(CACHE_NAME)\n");
        sw.append("      .then(cache => cache.addAll(URLS_TO_CACHE))\n");
        sw.append("  );\n");
        sw.append("});\n\n");
        sw.append("self.addEventListener('fetch', event => {\n");
        sw.append("  event.respondWith(\n");
        sw.append("    caches.match(event.request)\n");
        sw.append("      .then(response => response || fetch(event.request))\n");
        sw.append("  );\n");
        sw.append("});\n");
        return sw.toString();
    }

    /**
     * Generate component tests
     */
    private String generateComponentTests(ReactProjectRequest request) {
        StringBuilder tests = new StringBuilder();
        tests.append("import { render, screen } from '@testing-library/react';\n");
        tests.append("import { Provider } from 'react-redux';\n");
        tests.append("import App from './App';\n");
        tests.append("import store from './redux/store';\n\n");
        tests.append("describe('App Component', () => {\n");
        tests.append("  test('renders navbar', () => {\n");
        tests.append("    render(<Provider store={store}><App /></Provider>);\n");
        tests.append("    expect(screen.getByText(/").append(request.getProjectName()).append("/i)).toBeInTheDocument();\n");
        tests.append("  });\n\n");
        tests.append("  test('renders navigation links', () => {\n");
        tests.append("    render(<Provider store={store}><App /></Provider>);\n");
        tests.append("    expect(screen.getByText(/Home/i)).toBeInTheDocument();\n");
        tests.append("    expect(screen.getByText(/Dashboard/i)).toBeInTheDocument();\n");
        tests.append("  });\n");
        tests.append("});\n");
        return tests.toString();
    }

    /**
     * Generate integration tests
     */
    private String generateIntegrationTests(ReactProjectRequest request) {
        StringBuilder tests = new StringBuilder();
        tests.append("import { render, screen, waitFor } from '@testing-library/react';\n");
        tests.append("import userEvent from '@testing-library/user-event';\n");
        tests.append("import { Provider } from 'react-redux';\n");
        tests.append("import Dashboard from './pages/Dashboard';\n");
        tests.append("import store from './redux/store';\n\n");
        tests.append("describe('Dashboard Integration', () => {\n");
        tests.append("  test('loads and displays items', async () => {\n");
        tests.append("    render(<Provider store={store}><Dashboard /></Provider>);\n");
        tests.append("    await waitFor(() => {\n");
        tests.append("      expect(screen.getByText(/Loading/i)).not.toBeInTheDocument();\n");
        tests.append("    });\n");
        tests.append("  });\n");
        tests.append("});\n");
        return tests.toString();
    }

    /**
     * Generate utility functions
     */
    private String generateUtilities(ReactProjectRequest request) {
        StringBuilder utils = new StringBuilder();
        utils.append("export const formatDate = (date: Date): string => {\n");
        utils.append("  return new Date(date).toLocaleDateString('en-US', {\n");
        utils.append("    year: 'numeric',\n");
        utils.append("    month: 'long',\n");
        utils.append("    day: 'numeric',\n");
        utils.append("  });\n");
        utils.append("};\n\n");
        utils.append("export const debounce = (fn: Function, delay: number) => {\n");
        utils.append("  let timeoutId: NodeJS.Timeout;\n");
        utils.append("  return (...args: any[]) => {\n");
        utils.append("    clearTimeout(timeoutId);\n");
        utils.append("    timeoutId = setTimeout(() => fn(...args), delay);\n");
        utils.append("  };\n");
        utils.append("};\n");
        return utils.toString();
    }

    /**
     * Generate environment configuration
     */
    private String generateEnvConfig(ReactProjectRequest request) {
        StringBuilder env = new StringBuilder();
        env.append("# API Configuration\n");
        env.append("REACT_APP_API_URL=http://localhost:8080/api\n");
        env.append("REACT_APP_ENV=development\n\n");
        env.append("# Feature Flags\n");
        env.append("REACT_APP_OFFLINE_MODE=true\n");
        env.append("REACT_APP_DEBUG_MODE=false\n");
        return env.toString();
    }

    /**
     * Estimate total lines of code
     */
    private int estimateLinesOfCode(ReactProjectOutput output) {
        int total = 0;
        total += output.packageJson.split("\n").length;
        total += output.tsConfig.split("\n").length;
        total += output.appComponent.split("\n").length;
        total += output.mainComponents.split("\n").length;
        total += output.hookComponents.split("\n").length;
        total += output.reduxStore.split("\n").length;
        total += output.reduxSlices.split("\n").length;
        total += output.apiClient.split("\n").length;
        total += output.tailwindConfig.split("\n").length;
        total += output.globalStyles.split("\n").length;
        total += output.manifestJson.split("\n").length;
        total += output.serviceWorker.split("\n").length;
        total += output.componentTests.split("\n").length;
        total += output.integrationTests.split("\n").length;
        total += output.utilities.split("\n").length;
        total += output.envConfig.split("\n").length;
        return total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReactProjectRequest {
        private String projectName;
        private String reactVersion;
        private boolean offlineSupportEnabled;
        private List<String> requiredDependencies;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReactProjectOutput {
        private LocalDateTime timestamp;
        private String projectName;
        private String reactVersion;
        private String packageJson;
        private String tsConfig;
        private String appComponent;
        private String mainComponents;
        private String hookComponents;
        private String reduxStore;
        private String reduxSlices;
        private String apiClient;
        private String tailwindConfig;
        private String globalStyles;
        private String manifestJson;
        private String serviceWorker;
        private String componentTests;
        private String integrationTests;
        private String utilities;
        private String envConfig;
        private boolean generationSuccess;
        private String statusMessage;
        private int linesOfCode;
    }
}
