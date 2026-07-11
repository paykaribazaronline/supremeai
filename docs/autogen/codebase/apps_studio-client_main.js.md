# 📄 ফাইল: apps/studio-client/main.js

**প্রকার:** .js  
**সাইজ:** 1,686 বাইট  
**আপডেট:** 2026-07-11T11:32:07.053859

---

## কোড

```js
import { app, BrowserWindow, ipcMain } from 'electron';
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs/promises';

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

function createWindow() {
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
      color: '#030712', // SupremeColors.bgVoid
      symbolColor: '#00f3ff' // SupremeColors.brandPrimary for symbols
    }
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