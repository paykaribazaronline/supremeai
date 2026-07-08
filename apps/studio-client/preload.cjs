const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('supremeDesktopAPI', {
    readLocalFile: (filePath) => ipcRenderer.invoke('fs:read', filePath),
    writeLocalFile: (filePath, content) => ipcRenderer.invoke('fs:write', { filePath, content })
});
