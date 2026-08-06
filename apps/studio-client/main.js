import { app, BrowserWindow, ipcMain, nativeTheme, Menu, session, shell, Tray } from 'electron';
import path from 'path';
import { fileURLToPath } from 'url';

// বাংলা: ES module সোপে __dirname সংজ্ঞায়িত না থাকায় fileURLToPath দিয়ে তা তৈরি করা হয়েছে
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PRELOAD_PATH = path.join(__dirname, 'preload.cjs');
const IS_DEV = !app.isPackaged;
const API_BASE = process.env.VITE_API_URL || 'https://supremeai-backend.onrender.com';

let tray = null;

function createWindow() {
  const isDark = nativeTheme.shouldUseDarkColors;
  const win = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 1200,
    minHeight: 700,
    titleBarStyle: 'hiddenInset',
    backgroundColor: isDark ? '#0F172A' : '#F8FAFC',
    show: false,
    webPreferences: {
      preload: PRELOAD_PATH,
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      webSecurity: true,
    },
  });

  nativeTheme.on('updated', () => {
    const dark = nativeTheme.shouldUseDarkColors;
    win.setBackgroundColor(dark ? '#0F172A' : '#F8FAFC');
  });

  if (IS_DEV) {
    win.loadURL('http://127.0.0.1:5173');
  } else {
    const portal = process.env.VITE_PORTAL_TYPE === 'admin' ? 'dist-admin' : 'dist-user';
    win.loadFile(path.join(__dirname, portal, 'index.html'));
  }

  win.once('ready-to-show', () => {
    win.show();
    if (!IS_DEV) {
      try {
        const { autoUpdater } = require('electron-updater');
        autoUpdater.logger = console;
        autoUpdater.checkForUpdatesAndNotify().catch((err) => {
          console.log('[SupremeAI Desktop] Auto-update check skipped/idle:', err.message);
        });
      } catch (err) {
        console.log('[SupremeAI Desktop] electron-updater module standby:', err.message);
      }
    }
  });

  win.on('close', (event) => {
    if (!app.isQuitting && process.platform === 'darwin') {
      event.preventDefault();
      win.hide();
    }
  });

  return win;
}

function setupMenu(win) {
  const template = [
    ...(process.platform === 'darwin' ? [{
      label: app.getName(),
      submenu: [
        { label: 'About SupremeAI', role: 'about' },
        { type: 'separator' },
        { label: 'Quit', accelerator: 'Cmd+Q', click: () => { app.isQuitting = true; app.quit(); } },
      ],
    }] : []),
    {
      label: 'File',
      submenu: [
        { label: 'New Project', accelerator: 'CmdOrCtrl+N', click: () => win.webContents.send('menu:action', 'new-project') },
        { label: 'Open Project...', accelerator: 'CmdOrCtrl+O', click: () => win.webContents.send('menu:action', 'open-project') },
        { type: 'separator' },
        { label: 'Save', accelerator: 'CmdOrCtrl+S', click: () => win.webContents.send('menu:action', 'save') },
        { label: 'Save As...', accelerator: 'CmdOrCtrl+Shift+S', click: () => win.webContents.send('menu:action', 'save-as') },
      ],
    },
    {
      label: 'Edit',
      submenu: [
        { label: 'Undo', accelerator: 'CmdOrCtrl+Z', role: 'undo' },
        { label: 'Redo', accelerator: 'CmdOrCtrl+Y', role: 'redo' },
        { type: 'separator' },
        { label: 'Cut', accelerator: 'CmdOrCtrl+X', role: 'cut' },
        { label: 'Copy', accelerator: 'CmdOrCtrl+C', role: 'copy' },
        { label: 'Paste', accelerator: 'CmdOrCtrl+V', role: 'paste' },
        { label: 'Select All', accelerator: 'CmdOrCtrl+A', role: 'selectAll' },
      ],
    },
    {
      label: 'View',
      submenu: [
        { label: 'Reload', accelerator: 'CmdOrCtrl+R', click: () => win.reload() },
        { label: 'Toggle DevTools', accelerator: process.platform === 'darwin' ? 'Cmd+Option+I' : 'Ctrl+Shift+I', click: () => win.webContents.toggleDevTools() },
        { type: 'separator' },
        { label: 'Actual Size', accelerator: 'CmdOrCtrl+0', role: 'resetZoom' },
        { label: 'Zoom In', accelerator: 'CmdOrCtrl+Plus', role: 'zoomIn' },
        { label: 'Zoom Out', accelerator: 'CmdOrCtrl+-', role: 'zoomOut' },
        { type: 'separator' },
        { label: 'Toggle Full Screen', accelerator: process.platform === 'darwin' ? 'Ctrl+Cmd+F' : 'F11', role: 'togglefullscreen' },
      ],
    },
    {
      label: 'Help',
      submenu: [
        { label: 'Documentation', click: () => shell.openExternal('https://docs.supremeai.dev') },
        { label: 'Report Issue', click: () => shell.openExternal('https://github.com/paykaribazaronline/supremeai/issues') },
      ],
    },
  ];

  Menu.setApplicationMenu(Menu.buildFromTemplate(template));
}

function setupTray(win) {
  try {
    const iconPath = path.join(__dirname, 'media', 'icon.png');
    tray = new Tray(iconPath);
    const contextMenu = Menu.buildFromTemplate([
      { label: 'Show SupremeAI Studio', click: () => { win.show(); win.focus(); } },
      { label: 'Hide to System Tray', click: () => win.hide() },
      { type: 'separator' },
      { label: 'Quit SupremeAI', click: () => { app.isQuitting = true; app.quit(); } }
    ]);
    tray.setToolTip('SupremeAI Autonomous Studio');
    tray.setContextMenu(contextMenu);
    tray.on('click', () => {
      if (win.isVisible()) win.hide();
      else { win.show(); win.focus(); }
    });
  } catch (err) {
    console.log('[SupremeAI Tray] Tray icon setup deferred:', err.message);
  }
}

function setupIPC(win) {
  ipcMain.handle('window:minimize', () => win.minimize());
  ipcMain.handle('window:maximize', () => win.isMaximized() ? win.unmaximize() : win.maximize());
  ipcMain.handle('window:close', () => win.close());

  ipcMain.handle('app:get-info', () => ({
    name: app.getName(),
    version: app.getVersion(),
    platform: process.platform,
    isDev: IS_DEV,
  }));

  ipcMain.handle('theme:get-system', () => nativeTheme.shouldUseDarkColors ? 'dark' : 'light');
  ipcMain.handle('theme:set', (event, theme) => {
    nativeTheme.themeSource = theme;
  });

  ipcMain.handle('api:call', async (event, { endpoint, method, body, headers }) => {
    if (!endpoint || typeof endpoint !== 'string' || !endpoint.startsWith('/')) {
      return { status: 400, ok: false, data: { detail: 'Invalid endpoint format' } };
    }
    try {
      const response = await fetch(`${API_BASE}${endpoint}`, {
        method: method || 'GET',
        headers: {
          'Content-Type': 'application/json',
          ...headers,
        },
        body: body ? JSON.stringify(body) : undefined,
      });
      const text = await response.text();
      let data;
      try {
        data = JSON.parse(text);
      } catch {
        data = text;
      }
      return { status: response.status, ok: response.ok, data };
    } catch (err) {
      return {
        status: 503,
        ok: false,
        data: { detail: 'SupremeAI backend unreachable', error: String(err) },
      };
    }
  });
}

app.whenReady().then(() => {
  const win = createWindow();
  setupMenu(win);
  setupIPC(win);
  setupTray(win);

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
    else win.show();
  });

  app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') app.quit();
  });
});
