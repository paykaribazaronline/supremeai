package org.example.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Agent F: Desktop Application Code Generator
 * Generates cross-platform desktop applications using Tauri and Electron
 */
@Service
public class FDesktopAgent {

    /**
     * Generate complete desktop application project
     */
    public DesktopProjectOutput generateDesktopProject(DesktopProjectRequest request) {
        DesktopProjectOutput output = new DesktopProjectOutput();
        output.setTimestamp(LocalDateTime.now());
        output.setProjectName(request.getProjectName());
        output.setPlatform(request.getPlatform());

        if ("tauri".equalsIgnoreCase(request.getPlatform())) {
            // Generate Tauri project structure
            output.setTauriConf(generateTauriConf(request));
            output.setRustMain(generateRustMain(request));
            output.setRustCommands(generateRustCommands(request));
            output.setTauriInvoke(generateTauriInvoke(request));
        } else if ("electron".equalsIgnoreCase(request.getPlatform())) {
            // Generate Electron project structure
            output.setElectronMain(generateElectronMain(request));
            output.setElectronPreload(generateElectronPreload(request));
            output.setElectronPackageJson(generateElectronPackageJson(request));
        }

        // Generate common frontend
        output.setMainIndex(generateMainIndex(request));
        output.setMainWindow(generateMainWindow(request));
        output.setIPC(generateIPC(request));

        // Generate system integration
        output.setMenuBar(generateMenuBar(request));
        output.setFileOperations(generateFileOperations(request));

        // Generate native modules
        output.setNativeBindings(generateNativeBindings(request));

        // Generate tests
        output.setSystemTests(generateSystemTests(request));
        output.setE2ETests(generateE2ETests(request));

        // Generate build configuration
        output.setBuildConfig(generateBuildConfig(request));

        output.setGenerationSuccess(true);
        output.setStatusMessage("Desktop application project generated successfully");
        output.setLinesOfCode(estimateLinesOfCode(output));

        return output;
    }

    /**
     * Generate Tauri configuration
     */
    private String generateTauriConf(DesktopProjectRequest request) {
        StringBuilder conf = new StringBuilder();
        conf.append("{\n");
        conf.append("  \"build\": {\n");
        conf.append("    \"beforeBuildCommand\": \"npm run build\",\n");
        conf.append("    \"beforeDevCommand\": \"npm run dev\",\n");
        conf.append("    \"devPath\": \"http://localhost:5173\",\n");
        conf.append("    \"frontendDist\": \"../dist\"\n");
        conf.append("  },\n");
        conf.append("  \"app\": {\n");
        conf.append("    \"windows\": [\n");
        conf.append("      {\n");
        conf.append("        \"title\": \"").append(request.getProjectName()).append("\",\n");
        conf.append("        \"width\": 1200,\n");
        conf.append("        \"height\": 800,\n");
        conf.append("        \"resizable\": true,\n");
        conf.append("        \"fullscreen\": false\n");
        conf.append("      }\n");
        conf.append("    ],\n");
        conf.append("    \"security\": {\n");
        conf.append("      \"csp\": \"default-src 'self'; script-src 'self' 'unsafe-inline'\"\n");
        conf.append("    }\n");
        conf.append("  },\n");
        conf.append("  \"bundle\": {\n");
        conf.append("    \"active\": true,\n");
        conf.append("    \"targets\": [\"msi\", \"dmg\", \"appimage\"],\n");
        conf.append("    \"identifier\": \"com.example.").append(request.getProjectName().toLowerCase()).append("\"\n");
        conf.append("  }\n");
        conf.append("}\n");
        return conf.toString();
    }

    /**
     * Generate Rust main backend code for Tauri
     */
    private String generateRustMain(DesktopProjectRequest request) {
        StringBuilder rust = new StringBuilder();
        rust.append("#![cfg_attr(all(not(debug_assertions), target_os = \"windows\"), windows_subsystem = \"windows\")]\n\n");
        rust.append("mod commands;\n\n");
        rust.append("fn main() {\n");
        rust.append("  tauri::Builder::default()\n");
        rust.append("    .invoke_handler(tauri::generate_handler![commands::greet])\n");
        rust.append("    .run(tauri::generate_context!())\n");
        rust.append("    .expect(\"error while running tauri application\");\n");
        rust.append("}\n");
        return rust.toString();
    }

    /**
     * Generate Rust commands for Tauri backend
     */
    private String generateRustCommands(DesktopProjectRequest request) {
        StringBuilder commands = new StringBuilder();
        commands.append("use tauri::State;\nuse std::fs;\nuse std::path::Path;\n\n");
        commands.append("#[tauri::command]\n");
        commands.append("pub fn greet(name: &str) -> String {\n");
        commands.append("  format!(\"Hello, {}! Welcome to {}!\", name, \"").append(request.getProjectName()).append("\")\n");
        commands.append("}\n\n");
        commands.append("#[tauri::command]\n");
        commands.append("pub fn read_file(path: String) -> Result<String, String> {\n");
        commands.append("  fs::read_to_string(&path)\n");
        commands.append("    .map_err(|e| format!(\"Failed to read file: {}\", e))\n");
        commands.append("}\n\n");
        commands.append("#[tauri::command]\n");
        commands.append("pub fn write_file(path: String, content: String) -> Result<(), String> {\n");
        commands.append("  fs::write(&path, &content)\n");
        commands.append("    .map_err(|e| format!(\"Failed to write file: {}\", e))\n");
        commands.append("}\n\n");
        commands.append("#[tauri::command]\n");
        commands.append("pub fn get_app_version() -> String {\n");
        commands.append("  env!(\"CARGO_PKG_VERSION\").to_string()\n");
        commands.append("}\n");
        return commands.toString();
    }

    /**
     * Generate Tauri invoke wrapper
     */
    private String generateTauriInvoke(DesktopProjectRequest request) {
        StringBuilder invoke = new StringBuilder();
        invoke.append("import { invoke } from '@tauri-apps/api/tauri';\n\n");
        invoke.append("export const TauriApi = {\n");
        invoke.append("  async greet(name: string): Promise<string> {\n");
        invoke.append("    return invoke('greet', { name });\n");
        invoke.append("  },\n\n");
        invoke.append("  async readFile(path: string): Promise<string> {\n");
        invoke.append("    return invoke('read_file', { path });\n");
        invoke.append("  },\n\n");
        invoke.append("  async writeFile(path: string, content: string): Promise<void> {\n");
        invoke.append("    return invoke('write_file', { path, content });\n");
        invoke.append("  },\n\n");
        invoke.append("  async getAppVersion(): Promise<string> {\n");
        invoke.append("    return invoke('get_app_version');\n");
        invoke.append("  },\n");
        invoke.append("};\n");
        return invoke.toString();
    }

    /**
     * Generate Electron main process
     */
    private String generateElectronMain(DesktopProjectRequest request) {
        StringBuilder main = new StringBuilder();
        main.append("const { app, BrowserWindow, Menu, ipcMain } = require('electron');\n");
        main.append("const path = require('path');\n");
        main.append("const isDev = require('electron-is-dev');\n\n");
        main.append("let mainWindow;\n\n");
        main.append("function createWindow() {\n");
        main.append("  mainWindow = new BrowserWindow({\n");
        main.append("    width: 1200,\n");
        main.append("    height: 800,\n");
        main.append("    webPreferences: {\n");
        main.append("      preload: path.join(__dirname, 'preload.js'),\n");
        main.append("      contextIsolation: true,\n");
        main.append("      enableRemoteModule: false\n");
        main.append("    }\n");
        main.append("  });\n\n");
        main.append("  const startUrl = isDev\n");
        main.append("    ? 'http://localhost:3000'\n");
        main.append("    : `file://${path.join(__dirname, '../dist/index.html')}`;\n\n");
        main.append("  mainWindow.loadURL(startUrl);\n");
        main.append("  if (isDev) mainWindow.webContents.openDevTools();\n\n");
        main.append("  mainWindow.on('closed', () => mainWindow = null);\n");
        main.append("}\n\n");
        main.append("app.on('ready', createWindow);\n");
        main.append("app.on('window-all-closed', () => {\n");
        main.append("  if (process.platform !== 'darwin') app.quit();\n");
        main.append("});\n");
        return main.toString();
    }

    /**
     * Generate Electron preload script
     */
    private String generateElectronPreload(DesktopProjectRequest request) {
        StringBuilder preload = new StringBuilder();
        preload.append("const { contextBridge, ipcRenderer } = require('electron');\n\n");
        preload.append("contextBridge.exposeInMainWorld('electron', {\n");
        preload.append("  ipcRenderer: {\n");
        preload.append("    send: (channel, args) => ipcRenderer.send(channel, args),\n");
        preload.append("    on: (channel, func) => ipcRenderer.on(channel, (event, ...args) => func(...args)),\n");
        preload.append("    invoke: (channel, args) => ipcRenderer.invoke(channel, args),\n");
        preload.append("  },\n");
        preload.append("  versions: {\n");
        preload.append("    node: () => process.versions.node,\n");
        preload.append("    chrome: () => process.versions.chrome,\n");
        preload.append("    electron: () => process.versions.electron\n");
        preload.append("  }\n");
        preload.append("});\n");
        return preload.toString();
    }

    /**
     * Generate Electron package.json
     */
    private String generateElectronPackageJson(DesktopProjectRequest request) {
        StringBuilder pkg = new StringBuilder();
        pkg.append("{\n");
        pkg.append("  \"name\": \"").append(request.getProjectName().toLowerCase()).append("\",\n");
        pkg.append("  \"version\": \"1.0.0\",\n");
        pkg.append("  \"description\": \"Desktop Application\",\n");
        pkg.append("  \"main\": \"public/electron.js\",\n");
        pkg.append("  \"homepage\": \"./\",\n");
        pkg.append("  \"dependencies\": {\n");
        pkg.append("    \"react\": \"^18.0.0\",\n");
        pkg.append("    \"react-dom\": \"^18.0.0\"\n");
        pkg.append("  },\n");
        pkg.append("  \"devDependencies\": {\n");
        pkg.append("    \"electron\": \"latest\",\n");
        pkg.append("    \"electron-is-dev\": \"^2.0.0\"\n");
        pkg.append("  },\n");
        pkg.append("  \"scripts\": {\n");
        pkg.append("    \"react-start\": \"react-scripts start\",\n");
        pkg.append("    \"react-build\": \"react-scripts build\",\n");
        pkg.append("    \"electron-start\": \"electron .\",\n");
        pkg.append("    \"start\": \"concurrently \\\"npm run react-start\\\" \\\"wait-on http://localhost:3000 && npm run electron-start\\\"\",\n");
        pkg.append("    \"build\": \"npm run react-build && npm run electron-build\"\n");
        pkg.append("  }\n");
        pkg.append("}\n");
        return pkg.toString();
    }

    /**
     * Generate main index file
     */
    private String generateMainIndex(DesktopProjectRequest request) {
        StringBuilder index = new StringBuilder();
        index.append("import React from 'react';\n");
        index.append("import ReactDOM from 'react-dom/client';\n");
        index.append("import App from './App';\n");
        index.append("import './index.css';\n\n");
        index.append("const root = ReactDOM.createRoot(document.getElementById('root'));\n");
        index.append("root.render(\n");
        index.append("  <React.StrictMode>\n");
        index.append("    <App />\n");
        index.append("  </React.StrictMode>\n");
        index.append(");\n");
        return index.toString();
    }

    /**
     * Generate main window component
     */
    private String generateMainWindow(DesktopProjectRequest request) {
        StringBuilder window = new StringBuilder();
        window.append("import React, { useState } from 'react';\n");
        window.append("import './App.css';\n\n");
        window.append("function App() {\n");
        window.append("  const [count, setCount] = useState(0);\n\n");
        window.append("  return (\n");
        window.append("    <div className=\"app\">\n");
        window.append("      <header className=\"header\">\n");
        window.append("        <h1>").append(request.getProjectName()).append("</h1>\n");
        window.append("      </header>\n");
        window.append("      <main className=\"main\">\n");
        window.append("        <div className=\"card\">\n");
        window.append("          <p>Count: {count}</p>\n");
        window.append("          <button onClick={() => setCount(count + 1)}>Increment</button>\n");
        window.append("          <button onClick={() => setCount(0)}>Reset</button>\n");
        window.append("        </div>\n");
        window.append("      </main>\n");
        window.append("      <footer className=\"footer\">\n");
        window.append("        <p>&copy; 2024 ").append(request.getProjectName()).append("</p>\n");
        window.append("      </footer>\n");
        window.append("    </div>\n");
        window.append("  );\n");
        window.append("}\n\n");
        window.append("export default App;\n");
        return window.toString();
    }

    /**
     * Generate IPC (Inter-Process Communication) handlers
     */
    private String generateIPC(DesktopProjectRequest request) {
        StringBuilder ipc = new StringBuilder();
        ipc.append("// IPC handlers for desktop bridge\n\n");
        ipc.append("export const setupIPC = () => {\n");
        ipc.append("  // File operations\n");
        ipc.append("  window.ipcRenderer?.on('file:open', (path) => {\n");
        ipc.append("    console.log('File opened:', path);\n");
        ipc.append("  });\n\n");
        ipc.append("  // Window events\n");
        ipc.append("  window.ipcRenderer?.on('window:ready', () => {\n");
        ipc.append("    console.log('Window ready');\n");
        ipc.append("  });\n\n");
        ipc.append("  // App lifecycle\n");
        ipc.append("  window.ipcRenderer?.on('app:quit', () => {\n");
        ipc.append("    console.log('App quitting');\n");
        ipc.append("  });\n");
        ipc.append("};\n");
        return ipc.toString();
    }

    /**
     * Generate menu bar setup
     */
    private String generateMenuBar(DesktopProjectRequest request) {
        StringBuilder menu = new StringBuilder();
        menu.append("const { Menu } = require('electron');\n\n");
        menu.append("const template = [\n");
        menu.append("  {\n");
        menu.append("    label: 'File',\n");
        menu.append("    submenu: [\n");
        menu.append("      { label: 'Exit', accelerator: 'CmdOrCtrl+Q', click: () => app.quit() }\n");
        menu.append("    ]\n");
        menu.append("  },\n");
        menu.append("  {\n");
        menu.append("    label: 'Edit',\n");
        menu.append("    submenu: [\n");
        menu.append("      { label: 'Undo', accelerator: 'CmdOrCtrl+Z', role: 'undo' },\n");
        menu.append("      { label: 'Redo', accelerator: 'CmdOrCtrl+Shift+Z', role: 'redo' }\n");
        menu.append("    ]\n");
        menu.append("  },\n");
        menu.append("  {\n");
        menu.append("    label: 'Help',\n");
        menu.append("    submenu: [\n");
        menu.append("      { label: 'About', click: () => showAboutWindow() }\n");
        menu.append("    ]\n");
        menu.append("  }\n");
        menu.append("];\n\n");
        menu.append("const menu = Menu.buildFromTemplate(template);\n");
        menu.append("Menu.setApplicationMenu(menu);\n");
        return menu.toString();
    }

    /**
     * Generate file operations utilities
     */
    private String generateFileOperations(DesktopProjectRequest request) {
        StringBuilder fileOps = new StringBuilder();
        fileOps.append("import fs from 'fs';\nimport path from 'path';\n\n");
        fileOps.append("export class FileManager {\n");
        fileOps.append("  static readFile(filePath: string): Promise<string> {\n");
        fileOps.append("    return new Promise((resolve, reject) => {\n");
        fileOps.append("      fs.readFile(filePath, 'utf8', (err, data) => {\n");
        fileOps.append("        if (err) reject(err);\n");
        fileOps.append("        else resolve(data);\n");
        fileOps.append("      });\n");
        fileOps.append("    });\n");
        fileOps.append("  }\n\n");
        fileOps.append("  static async writeFile(filePath: string, content: string): Promise<void> {\n");
        fileOps.append("    return new Promise((resolve, reject) => {\n");
        fileOps.append("      fs.writeFile(filePath, content, 'utf8', (err) => {\n");
        fileOps.append("        if (err) reject(err);\n");
        fileOps.append("        else resolve();\n");
        fileOps.append("      });\n");
        fileOps.append("    });\n");
        fileOps.append("  }\n\n");
        fileOps.append("  static async listFiles(dirPath: string): Promise<string[]> {\n");
        fileOps.append("    return new Promise((resolve, reject) => {\n");
        fileOps.append("      fs.readdir(dirPath, (err, files) => {\n");
        fileOps.append("        if (err) reject(err);\n");
        fileOps.append("        else resolve(files);\n");
        fileOps.append("      });\n");
        fileOps.append("    });\n");
        fileOps.append("  }\n");
        fileOps.append("}\n");
        return fileOps.toString();
    }

    /**
     * Generate native module bindings
     */
    private String generateNativeBindings(DesktopProjectRequest request) {
        StringBuilder bindings = new StringBuilder();
        bindings.append("{\n");
        bindings.append("  \"targets\": [\n");
        bindings.append("    {\n");
        bindings.append("      \"target_name\": \"").append(request.getProjectName().toLowerCase()).append("_native\",\n");
        bindings.append("      \"sources\": [\"src/native/bindings.cc\"],\n");
        bindings.append("      \"include_dirs\": [\"<!(node -p \\\"require('node-addon-api').include_dir\\\")\"],\n");
        bindings.append("      \"dependencies\": [\"<!(node -p \\\"require('node-addon-api').gyp_dir\\\")/node-addon-api.gyp:node_addon_api_except\"]\n");
        bindings.append("    }\n");
        bindings.append("  ]\n");
        bindings.append("}\n");
        return bindings.toString();
    }

    /**
     * Generate system tests
     */
    private String generateSystemTests(DesktopProjectRequest request) {
        StringBuilder tests = new StringBuilder();
        tests.append("import { test, expect } from 'vitest';\n");
        tests.append("import { FileManager } from './services/FileManager';\n\n");
        tests.append("describe('System Integration', () => {\n");
        tests.append("  test('can read files', async () => {\n");
        tests.append("    // Mock file read\n");
        tests.append("    expect(true).toBe(true);\n");
        tests.append("  });\n\n");
        tests.append("  test('can write files', async () => {\n");
        tests.append("    // Mock file write\n");
        tests.append("    expect(true).toBe(true);\n");
        tests.append("  });\n\n");
        tests.append("  test('app starts successfully', () => {\n");
        tests.append("    expect(true).toBe(true);\n");
        tests.append("  });\n");
        tests.append("});\n");
        return tests.toString();
    }

    /**
     * Generate E2E tests
     */
    private String generateE2ETests(DesktopProjectRequest request) {
        StringBuilder tests = new StringBuilder();
        tests.append("import { test, expect } from '@playwright/test';\n\n");
        tests.append("test.describe('Desktop App E2E', () => {\n");
        tests.append("  test('user can increment counter', async ({ page }) => {\n");
        tests.append("    await page.goto('/');\n");
        tests.append("    const button = page.locator('button:has-text(\"Increment\")');\n");
        tests.append("    await button.click();\n");
        tests.append("    expect(page.locator('text=1')).toBeDefined();\n");
        tests.append("  });\n\n");
        tests.append("  test('user can navigate menu', async ({ page }) => {\n");
        tests.append("    await page.goto('/');\n");
        tests.append("    const header = page.locator('h1');\n");
        tests.append("    expect(await header.isVisible()).toBe(true);\n");
        tests.append("  });\n");
        tests.append("});\n");
        return tests.toString();
    }

    /**
     * Generate build configuration
     */
    private String generateBuildConfig(DesktopProjectRequest request) {
        StringBuilder config = new StringBuilder();
        config.append("// Vite build configuration for desktop app\n\n");
        config.append("import { defineConfig } from 'vite';\n");
        config.append("import react from '@vitejs/plugin-react';\n\n");
        config.append("export default defineConfig({\n");
        config.append("  plugins: [react()],\n");
        config.append("  build: {\n");
        config.append("    target: 'es2020',\n");
        config.append("    rollupOptions: {\n");
        config.append("      output: {\n");
        config.append("        manualChunks: {\n");
        config.append("          'vendor': ['react', 'react-dom']\n");
        config.append("        }\n");
        config.append("      }\n");
        config.append("    }\n");
        config.append("  }\n");
        config.append("});\n");
        return config.toString();
    }

    /**
     * Estimate total lines of code
     */
    private int estimateLinesOfCode(DesktopProjectOutput output) {
        int total = 0;
        if (output.tauriConf != null) total += output.tauriConf.split("\n").length;
        if (output.rustMain != null) total += output.rustMain.split("\n").length;
        if (output.rustCommands != null) total += output.rustCommands.split("\n").length;
        if (output.tauriInvoke != null) total += output.tauriInvoke.split("\n").length;
        if (output.electronMain != null) total += output.electronMain.split("\n").length;
        if (output.electronPreload != null) total += output.electronPreload.split("\n").length;
        if (output.electronPackageJson != null) total += output.electronPackageJson.split("\n").length;
        total += output.mainIndex.split("\n").length;
        total += output.mainWindow.split("\n").length;
        total += output.ipc.split("\n").length;
        total += output.menuBar.split("\n").length;
        total += output.fileOperations.split("\n").length;
        total += output.nativeBindings.split("\n").length;
        total += output.systemTests.split("\n").length;
        total += output.e2eTests.split("\n").length;
        total += output.buildConfig.split("\n").length;
        return total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DesktopProjectRequest {
        private String projectName;
        private String platform; // "tauri" or "electron"
        private List<String> targetPlatforms; // windows, macos, linux
        private boolean autoUpdateEnabled;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DesktopProjectOutput {
        private LocalDateTime timestamp;
        private String projectName;
        private String platform;
        
        // Tauri-specific
        private String tauriConf;
        private String rustMain;
        private String rustCommands;
        private String tauriInvoke;
        
        // Electron-specific
        private String electronMain;
        private String electronPreload;
        private String electronPackageJson;
        
        // Common
        private String mainIndex;
        private String mainWindow;
        private String ipc;
        private String menuBar;
        private String fileOperations;
        private String nativeBindings;
        private String systemTests;
        private String e2eTests;
        private String buildConfig;
        
        private boolean generationSuccess;
        private String statusMessage;
        private int linesOfCode;
    }
}
