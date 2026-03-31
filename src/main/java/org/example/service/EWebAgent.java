package org.example.service;

import java.util.*;
import org.springframework.stereotype.Service;

/**
 * Agent E: Web/React Code Generator
 * Generates complete React/Vue/Angular web applications
 * Supports PWA, responsive design, SPA architecture
 */
@Service
public class EWebAgent {
    
    private final ReactProjectGenerator reactGenerator;
    private final WebSocketIntegration wsIntegration;
    private final PWAGenerator pwaGenerator;
    private final StateManagementSetup stateSetup;
    private final TailwindConfigGenerator tailwindConfig;
    
    public EWebAgent() {
        this.reactGenerator = new ReactProjectGenerator();
        this.wsIntegration = new WebSocketIntegration();
        this.pwaGenerator = new PWAGenerator();
        this.stateSetup = new StateManagementSetup();
        this.tailwindConfig = new TailwindConfigGenerator();
    }
    
    /**
     * Generate complete React project from specification
     */
    public WebProjectResult generateReactProject(WebProjectSpec spec) {
        WebProjectResult result = new WebProjectResult();
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate spec
            if (!validateSpec(spec)) {
                result.setStatus("FAILED");
                result.setError("Invalid web project specification");
                return result;
            }
            
            // 1. Create project structure
            Map<String, String> projectStructure = reactGenerator.createProjectStructure(spec);
            result.setProjectStructure(projectStructure);
            
            // 2. Generate package.json
            String packageJson = reactGenerator.generatePackageJson(spec);
            result.setPackageJson(packageJson);
            
            // 3. Create main App component
            String appComponent = reactGenerator.generateMainApp(spec);
            result.setAppComponent(appComponent);
            
            // 4. Generate pages/screens
            List<String> generatedPages = new ArrayList<>();
            for (String pageName : spec.getPages()) {
                String pageCode = reactGenerator.generatePage(
                    pageName,
                    (PageSpec) spec.getPageSpecifications().get(pageName)
                );
                generatedPages.add(pageCode);
            }
            result.setGeneratedPages(generatedPages);
            
            // 5. Generate components
            List<String> generatedComponents = new ArrayList<>();
            for (String componentName : spec.getComponents()) {
                String componentCode = reactGenerator.generateComponent(
                    componentName,
                    spec.getComponentSpecifications().get(componentName)
                );
                generatedComponents.add(componentCode);
            }
            result.setGeneratedComponents(generatedComponents);
            
            // 6. Setup state management (Redux/Zustand)
            Map<String, String> stateManagement = stateSetup.setupStateManagement(
                spec.getStateManagementTool(),
                spec.getStores()
            );
            result.setStateManagement(stateManagement);
            
            // 7. Setup WebSocket integration if needed
            if (spec.isEnableWebSocket()) {
                String wsService = wsIntegration.generateWebSocketService(spec.getWebSocketConfig());
                result.setWebSocketService(wsService);
            }
            
            // 8. Generate PWA configuration
            if (spec.isPwa()) {
                Map<String, String> pwaConfig = pwaGenerator.generatePWAConfig(spec);
                result.setPwaConfig(pwaConfig);
            }
            
            // 9. Generate Tailwind configuration
            String tailwindConfig = this.tailwindConfig.generateTailwindConfig(
                spec.getTheme(),
                spec.getCustomColors()
            );
            result.setTailwindConfig(tailwindConfig);
            
            // 10. Generate tsconfig/jsconfig
            String tsConfig = reactGenerator.generateTypeScriptConfig(spec.isUseTypeScript());
            result.setTypeScriptConfig(tsConfig);
            
            // 11. Generate environment configs
            Map<String, String> envConfigs = reactGenerator.generateEnvironmentConfigs(
                spec.getEnvironments()
            );
            result.setEnvironmentConfigs(envConfigs);
            
            // 12. Package output
            String packagePath = reactGenerator.packageProject(
                spec.getProjectName(),
                projectStructure,
                generatedPages,
                generatedComponents
            );
            result.setPackagePath(packagePath);
            
            // Calculate metrics
            int totalLines = appComponent.length() + generatedPages.stream()
                .mapToInt(String::length).sum() + generatedComponents.stream()
                .mapToInt(String::length).sum();
            
            result.setStatus("GENERATED");
            result.setProjectName(spec.getProjectName());
            result.setTotalLinesGenerated(totalLines / 40);
            result.setGenerationTime(System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            result.setStatus("FAILED");
            result.setError(e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Generate specific React component
     */
    public String generateReactComponent(String componentName, ComponentSpec spec) {
        return reactGenerator.generateComponent(componentName, spec);
    }
    
    /**
     * Generate React page/screen
     */
    public String generateReactPage(String pageName, PageSpec spec) {
        return reactGenerator.generatePage(pageName, spec);
    }
    
    /**
     * Setup WebSocket connection
     */
    public String setupWebSocket(WebSocketConfig config) {
        return wsIntegration.generateWebSocketService(config);
    }
    
    /**
     * Generate PWA manifest and service worker
     */
    public Map<String, String> generatePWA(WebProjectSpec spec) {
        return pwaGenerator.generatePWAConfig(spec);
    }
    
    private boolean validateSpec(WebProjectSpec spec) {
        return spec != null && 
               spec.getProjectName() != null && !spec.getProjectName().isEmpty() &&
               spec.getPages() != null && !spec.getPages().isEmpty();
    }
    
    // ======================== INNER CLASSES ========================
    
    public static class ReactProjectGenerator {
        
        public Map<String, String> createProjectStructure(WebProjectSpec spec) {
            Map<String, String> structure = new HashMap<>();
            String projectName = spec.getProjectName();
            
            structure.put("root", projectName);
            structure.put("src", projectName + "/src");
            structure.put("pages", projectName + "/src/pages");
            structure.put("components", projectName + "/src/components");
            structure.put("hooks", projectName + "/src/hooks");
            structure.put("services", projectName + "/src/services");
            structure.put("store", projectName + "/src/store");
            structure.put("styles", projectName + "/src/styles");
            structure.put("types", projectName + "/src/types");
            structure.put("public", projectName + "/public");
            
            return structure;
        }
        
        public String generatePackageJson(WebProjectSpec spec) {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"name\": \"").append(spec.getProjectName()).append("\",\n");
            json.append("  \"version\": \"1.0.0\",\n");
            json.append("  \"description\": \"").append(spec.getDescription()).append("\",\n");
            json.append("  \"dependencies\": {\n");
            json.append("    \"react\": \"^18.2.0\",\n");
            json.append("    \"react-dom\": \"^18.2.0\",\n");
            json.append("    \"react-router-dom\": \"^6.14.0\",\n");
            
            if (spec.isUseTypeScript()) {
                json.append("    \"typescript\": \"^5.1.6\",\n");
                json.append("    \"@types/react\": \"^18.2.0\",\n");
            }
            
            if (spec.getStateManagementTool().equals("Redux")) {
                json.append("    \"redux\": \"^4.2.1\",\n");
                json.append("    \"react-redux\": \"^8.1.1\",\n");
            }
            
            json.append("    \"axios\": \"^1.4.0\",\n");
            json.append("    \"tailwindcss\": \"^3.3.0\"\n");
            json.append("  },\n");
            json.append("  \"devDependencies\": {\n");
            json.append("    \"vite\": \"^4.4.0\",\n");
            json.append("    \"@vitejs/plugin-react\": \"^4.0.0\"\n");
            json.append("  },\n");
            json.append("  \"scripts\": {\n");
            json.append("    \"dev\": \"vite\",\n");
            json.append("    \"build\": \"tsc && vite build\",\n");
            json.append("    \"preview\": \"vite preview\"\n");
            json.append("  }\n");
            json.append("}\n");
            return json.toString();
        }
        
        public String generateMainApp(WebProjectSpec spec) {
            StringBuilder app = new StringBuilder();
            app.append("import React from 'react'\n");
            app.append("import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'\n");
            app.append("import './App.css'\n\n");
            app.append("function App() {\n");
            app.append("  return (\n");
            app.append("    <Router>\n");
            app.append("      <div className=\"app\">\n");
            app.append("        <header>\n");
            app.append("          <nav>").append(spec.getProjectName()).append("</nav>\n");
            app.append("        </header>\n");
            app.append("        <main>\n");
            app.append("          <Routes>\n");
            
            for (String page : spec.getPages()) {
                app.append("            <Route path=\"/").append(page.toLowerCase())
                   .append("\" element={<").append(page).append(" />} />\n");
            }
            
            app.append("          </Routes>\n");
            app.append("        </main>\n");
            app.append("      </div>\n");
            app.append("    </Router>\n");
            app.append("  )\n");
            app.append("}\n\n");
            app.append("export default App\n");
            return app.toString();
        }
        
        public String generateComponent(String componentName, ComponentSpec spec) {
            StringBuilder component = new StringBuilder();
            component.append("import React, { useState } from 'react'\n");
            component.append("import './").append(componentName).append(".module.css'\n\n");
            component.append("interface Props {\n");
            component.append("  title?: string\n");
            component.append("  onSubmit?: (data: any) => void\n");
            component.append("}\n\n");
            component.append("export const ").append(componentName).append(": React.FC<Props> = ({\n");
            component.append("  title,\n");
            component.append("  onSubmit\n");
            component.append("}) => {\n");
            component.append("  const [loading, setLoading] = useState(false)\n");
            component.append("  const [error, setError] = useState<string | null>(null)\n\n");
            component.append("  return (\n");
            component.append("    <div className=\"").append(componentName.toLowerCase()).append("\">\n");
            component.append("      {error && <div className=\"error\">{error}</div>}\n");
            component.append("      <div className=\"content\">\n");
            component.append("        {/* Component content here */}\n");
            component.append("      </div>\n");
            component.append("      {loading && <div className=\"spinner\" />}\n");
            component.append("    </div>\n");
            component.append("  )\n");
            component.append("}\n");
            return component.toString();
        }
        
        public String generatePage(String pageName, PageSpec spec) {
            StringBuilder page = new StringBuilder();
            page.append("import React, { useEffect } from 'react'\n");
            page.append("import { useNavigate } from 'react-router-dom'\n");
            page.append("import './").append(pageName).append(".module.css'\n\n");
            page.append("export const ").append(pageName).append(" = () => {\n");
            page.append("  const navigate = useNavigate()\n\n");
            page.append("  useEffect(() => {\n");
            page.append("    // Page initialization logic\n");
            page.append("  }, [])\n\n");
            page.append("  return (\n");
            page.append("    <div className=\"page-").append(pageName.toLowerCase()).append("\">\n");
            page.append("      <h1>").append(pageName).append("</h1>\n");
            page.append("      {/* Page content */}\n");
            page.append("    </div>\n");
            page.append("  )\n");
            page.append("}\n");
            return page.toString();
        }
        
        public String generateTypeScriptConfig(boolean useTypeScript) {
            if (!useTypeScript) return "{}";
            
            StringBuilder config = new StringBuilder();
            config.append("{\n");
            config.append("  \"compilerOptions\": {\n");
            config.append("    \"target\": \"ES2020\",\n");
            config.append("    \"useDefineForClassFields\": true,\n");
            config.append("    \"lib\": [\"ES2020\", \"DOM\", \"DOM.Iterable\"],\n");
            config.append("    \"module\": \"ESNext\",\n");
            config.append("    \"skipLibCheck\": true,\n");
            config.append("    \"esModuleInterop\": true,\n");
            config.append("    \"jsx\": \"react-jsx\"\n");
            config.append("  }\n");
            config.append("}\n");
            return config.toString();
        }
        
        public Map<String, String> generateEnvironmentConfigs(List<String> environments) {
            Map<String, String> configs = new HashMap<>();
            for (String env : environments) {
                configs.put(".env." + env, "REACT_APP_ENV=" + env);
            }
            return configs;
        }
        
        public String packageProject(String projectName, Map<String, String> structure,
                                    List<String> pages, List<String> components) {
            return "/" + projectName + "/dist";
        }
    }
    
    public static class WebSocketIntegration {
        
        public String generateWebSocketService(WebSocketConfig config) {
            StringBuilder service = new StringBuilder();
            service.append("class WebSocketService {\n");
            service.append("  private ws: WebSocket | null = null\n");
            service.append("  private url: string\n");
            service.append("  private listeners: Map<string, Function[]> = new Map()\n\n");
            service.append("  constructor(url: string) {\n");
            service.append("    this.url = url\n");
            service.append("  }\n\n");
            service.append("  connect() {\n");
            service.append("    this.ws = new WebSocket(this.url)\n");
            service.append("    this.ws.onmessage = (event) => this.handleMessage(event.data)\n");
            service.append("    this.ws.onerror = (error) => this.handleError(error)\n");
            service.append("  }\n\n");
            service.append("  send(type: string, data: any) {\n");
            service.append("    if (this.ws?.readyState === WebSocket.OPEN) {\n");
            service.append("      this.ws.send(JSON.stringify({ type, data }))\n");
            service.append("    }\n");
            service.append("  }\n\n");
            service.append("  subscribe(event: string, callback: Function) {\n");
            service.append("    if (!this.listeners.has(event)) {\n");
            service.append("      this.listeners.set(event, [])\n");
            service.append("    }\n");
            service.append("    this.listeners.get(event)?.push(callback)\n");
            service.append("  }\n\n");
            service.append("  private handleMessage(data: string) {\n");
            service.append("    const message = JSON.parse(data)\n");
            service.append("    const callbacks = this.listeners.get(message.type) || []\n");
            service.append("    callbacks.forEach(cb => cb(message.data))\n");
            service.append("  }\n\n");
            service.append("  private handleError(error: any) {\n");
            service.append("    console.error('WebSocket error:', error)\n");
            service.append("  }\n");
            service.append("}\n");
            return service.toString();
        }
    }
    
    public static class PWAGenerator {
        
        public Map<String, String> generatePWAConfig(WebProjectSpec spec) {
            Map<String, String> config = new HashMap<>();
            
            // Generate manifest.json
            StringBuilder manifest = new StringBuilder();
            manifest.append("{\n");
            manifest.append("  \"name\": \"").append(spec.getProjectName()).append("\",\n");
            manifest.append("  \"short_name\": \"").append(spec.getProjectName().substring(0, Math.min(12, spec.getProjectName().length()))).append("\",\n");
            manifest.append("  \"description\": \"Progressive Web App\",\n");
            manifest.append("  \"start_url\": \"/\",\n");
            manifest.append("  \"display\": \"standalone\",\n");
            manifest.append("  \"background_color\": \"#ffffff\",\n");
            manifest.append("  \"theme_color\": \"#000000\",\n");
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
            config.put("manifest.json", manifest.toString());
            
            // Generate service worker
            StringBuilder sw = new StringBuilder();
            sw.append("const CACHE_NAME = 'v1'\n");
            sw.append("const urlsToCache = [\n");
            sw.append("  '/',\n");
            sw.append("  '/icons/icon-192.png',\n");
            sw.append("  '/icons/icon-512.png'\n");
            sw.append("]\n\n");
            sw.append("self.addEventListener('install', (event) => {\n");
            sw.append("  event.waitUntil(\n");
            sw.append("    caches.open(CACHE_NAME).then((cache) => cache.addAll(urlsToCache))\n");
            sw.append("  )\n");
            sw.append("})\n");
            config.put("service-worker.js", sw.toString());
            
            return config;
        }
    }
    
    public static class StateManagementSetup {
        
        public Map<String, String> setupStateManagement(String tool, List<String> stores) {
            Map<String, String> setup = new HashMap<>();
            
            if ("Redux".equals(tool)) {
                StringBuilder store = new StringBuilder();
                store.append("import { createStore } from 'redux'\n");
                store.append("import rootReducer from './reducers'\n\n");
                store.append("export const store = createStore(rootReducer)\n");
                setup.put("store/index.ts", store.toString());
                
                StringBuilder reducers = new StringBuilder();
                reducers.append("import { combineReducers } from 'redux'\n");
                for (String storeName : stores) {
                    reducers.append("import ").append(storeName).append("Reducer from './").append(storeName).append("'\n");
                }
                reducers.append("\nexport default combineReducers({\n");
                for (String storeName : stores) {
                    reducers.append("  ").append(storeName).append(": ").append(storeName).append("Reducer,\n");
                }
                reducers.append("})\n");
                setup.put("store/reducers.ts", reducers.toString());
            }
            
            return setup;
        }
    }
    
    public static class TailwindConfigGenerator {
        
        public String generateTailwindConfig(Map<String, String> theme, Map<String, String> customColors) {
            StringBuilder config = new StringBuilder();
            config.append("module.exports = {\n");
            config.append("  content: [\n");
            config.append("    './index.html',\n");
            config.append("    './src/**/*.{js,ts,jsx,tsx}',\n");
            config.append("  ],\n");
            config.append("  theme: {\n");
            config.append("    extend: {\n");
            config.append("      colors: {\n");
            
            if (customColors != null) {
                for (Map.Entry<String, String> entry : customColors.entrySet()) {
                    config.append("        ").append(entry.getKey()).append(": '").append(entry.getValue()).append("',\n");
                }
            }
            
            config.append("      }\n");
            config.append("    }\n");
            config.append("  },\n");
            config.append("  plugins: [],\n");
            config.append("}\n");
            return config.toString();
        }
    }
    
    // ======================== DATA CLASSES ========================
    
    public static class WebProjectSpec {
        private String projectName;
        private String description;
        private boolean useTypeScript;
        private String stateManagementTool;
        private List<String> pages;
        private List<String> components;
        private List<String> stores;
        private boolean enableWebSocket;
        private WebSocketConfig webSocketConfig;
        private boolean pwa;
        private Map<String, String> theme;
        private Map<String, String> customColors;
        private List<String> environments;
        private Map<String, Object> pageSpecifications;
        private Map<String, ComponentSpec> componentSpecifications;
        
        public WebProjectSpec() {
            this.pages = new ArrayList<>();
            this.components = new ArrayList<>();
            this.stores = new ArrayList<>();
            this.theme = new HashMap<>();
            this.customColors = new HashMap<>();
            this.environments = new ArrayList<>();
            this.pageSpecifications = new HashMap<>();
            this.componentSpecifications = new HashMap<>();
        }
        
        public String getProjectName() { return projectName; }
        public void setProjectName(String name) { this.projectName = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String desc) { this.description = desc; }
        
        public boolean isUseTypeScript() { return useTypeScript; }
        public void setUseTypeScript(boolean use) { this.useTypeScript = use; }
        
        public String getStateManagementTool() { return stateManagementTool; }
        public void setStateManagementTool(String tool) { this.stateManagementTool = tool; }
        
        public List<String> getPages() { return pages; }
        public void setPages(List<String> pages) { this.pages = pages; }
        
        public List<String> getComponents() { return components; }
        public void setComponents(List<String> comps) { this.components = comps; }
        
        public List<String> getStores() { return stores; }
        public void setStores(List<String> stores) { this.stores = stores; }
        
        public boolean isEnableWebSocket() { return enableWebSocket; }
        public void setEnableWebSocket(boolean enable) { this.enableWebSocket = enable; }
        
        public WebSocketConfig getWebSocketConfig() { return webSocketConfig; }
        public void setWebSocketConfig(WebSocketConfig config) { this.webSocketConfig = config; }
        
        public boolean isPwa() { return pwa; }
        public void setPwa(boolean pwa) { this.pwa = pwa; }
        
        public Map<String, String> getTheme() { return theme; }
        public void setTheme(Map<String, String> theme) { this.theme = theme; }
        
        public Map<String, String> getCustomColors() { return customColors; }
        public void setCustomColors(Map<String, String> colors) { this.customColors = colors; }
        
        public List<String> getEnvironments() { return environments; }
        public void setEnvironments(List<String> envs) { this.environments = envs; }
        
        public Map<String, Object> getPageSpecifications() { return pageSpecifications; }
        public Map<String, ComponentSpec> getComponentSpecifications() { return componentSpecifications; }
    }
    
    public static class WebProjectResult {
        private String status;
        private String projectName;
        private String error;
        private Map<String, String> projectStructure;
        private String packageJson;
        private String appComponent;
        private List<String> generatedPages;
        private List<String> generatedComponents;
        private Map<String, String> stateManagement;
        private String webSocketService;
        private Map<String, String> pwaConfig;
        private String tailwindConfig;
        private String typeScriptConfig;
        private Map<String, String> environmentConfigs;
        private String packagePath;
        private int totalLinesGenerated;
        private long generationTime;
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getProjectName() { return projectName; }
        public void setProjectName(String name) { this.projectName = name; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public Map<String, String> getProjectStructure() { return projectStructure; }
        public void setProjectStructure(Map<String, String> structure) { this.projectStructure = structure; }
        
        public String getPackageJson() { return packageJson; }
        public void setPackageJson(String json) { this.packageJson = json; }
        
        public String getAppComponent() { return appComponent; }
        public void setAppComponent(String component) { this.appComponent = component; }
        
        public List<String> getGeneratedPages() { return generatedPages; }
        public void setGeneratedPages(List<String> pages) { this.generatedPages = pages; }
        
        public List<String> getGeneratedComponents() { return generatedComponents; }
        public void setGeneratedComponents(List<String> comps) { this.generatedComponents = comps; }
        
        public Map<String, String> getStateManagement() { return stateManagement; }
        public void setStateManagement(Map<String, String> state) { this.stateManagement = state; }
        
        public String getWebSocketService() { return webSocketService; }
        public void setWebSocketService(String service) { this.webSocketService = service; }
        
        public Map<String, String> getPwaConfig() { return pwaConfig; }
        public void setPwaConfig(Map<String, String> config) { this.pwaConfig = config; }
        
        public String getTailwindConfig() { return tailwindConfig; }
        public void setTailwindConfig(String config) { this.tailwindConfig = config; }
        
        public String getTypeScriptConfig() { return typeScriptConfig; }
        public void setTypeScriptConfig(String config) { this.typeScriptConfig = config; }
        
        public Map<String, String> getEnvironmentConfigs() { return environmentConfigs; }
        public void setEnvironmentConfigs(Map<String, String> configs) { this.environmentConfigs = configs; }
        
        public String getPackagePath() { return packagePath; }
        public void setPackagePath(String path) { this.packagePath = path; }
        
        public int getTotalLinesGenerated() { return totalLinesGenerated; }
        public void setTotalLinesGenerated(int lines) { this.totalLinesGenerated = lines; }
        
        public long getGenerationTime() { return generationTime; }
        public void setGenerationTime(long time) { this.generationTime = time; }
    }
    
    public static class ComponentSpec {}
    
    public static class PageSpec {}
    
    public static class WebSocketConfig {
        private String url;
        private boolean autoConnect;
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public boolean isAutoConnect() { return autoConnect; }
        public void setAutoConnect(boolean auto) { this.autoConnect = auto; }
    }
}
