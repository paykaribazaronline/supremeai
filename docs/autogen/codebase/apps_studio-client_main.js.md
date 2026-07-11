# 📄 ফাইল: apps/studio-client/main.js

**প্রকার:** .js  
**সাইজ:** 2,687 বাইট  
**আপডেট:** 2026-07-11T13:36:50.185851

---

## কোড

```js
import { app, BrowserWindow, ipcMain, nativeTheme } from 'electron';
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs/promises';
import { readFileSync } from 'fs';

ipcMain.handle('fs:read', async (event, filePath) => {
    try {
        return await fs.readFile(filePath, 'utf-8');
    } catch (error) {
        return { error: error.message };
    }
});

ipcMain.handle('fs:write', async (event, { filePath, content }) => {
    try {
        await fs.writeFile(filePath, content, 'utf-8');
        return { success: true };
    } catch (error) {
        return { error: error.message };
    }
});
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PRELOAD_PATH = path.join(__dirname, 'preload.cjs');

// Load design tokens
let tokens = {};
try {
  const tokenPath = path.join(__dirname, '../../packages/design-tokens/outputs/json/tokens.json');
  tokens = JSON.parse(readFileSync(tokenPath, 'utf-8'));
} catch (e) {
  console.warn("Could not load design tokens. Falling back to defaults.");
}

function updateTitleBar(win) {
  if (!win) return;
  const isDark = nativeTheme.shouldUseDarkColors;
  // Fallback colors if tokens are missing
  const bgColor = isDark 
    ? (tokens['color-neutral-900'] || '#0F172A')
    : (tokens['color-neutral-50'] || '#F8FAFC');
  
  const symbolColor = tokens['color-brand-500'] || '#6366F1';
  
  win.setTitleBarOverlay({
    color: bgColor,
    symbolColor: symbolColor
  });
}

function createWindow() {
  const isDark = nativeTheme.shouldUseDarkColors;
  const bgColor = isDark 
    ? (tokens['color-neutral-900'] || '#0F172A')
    : (tokens['color-neutral-50'] || '#F8FAFC');
  const symbolColor = tokens['color-brand-500'] || '#6366F1';

  const win = new BrowserWindow({
    width: 1200,
    height: 800,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: true,
      preload: PRELOAD_PATH
    },
    titleBarStyle: 'hidden', // Modern look
    titleBarOverlay: {
      color: bgColor,
      symbolColor: symbolColor
    }
  });

  nativeTheme.on('updated', () => {
    updateTitleBar(win);
  });

  // Check if we are in development mode
  const isDev = !app.isPackaged;

  if (isDev) {
    win.loadURL('http://127.0.0.1:5173');
    // win.webContents.openDevTools();
  } else {
    win.loadFile(path.join(__dirname, 'dist/index.html'));
  }
}

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

```