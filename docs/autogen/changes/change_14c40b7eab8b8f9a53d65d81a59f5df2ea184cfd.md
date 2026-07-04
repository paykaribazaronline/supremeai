# 📋 Commit 14c40b7eab8b8f9a53d65d81a59f5df2ea184cfd

## Commit Stats
```
commit 14c40b7eab8b8f9a53d65d81a59f5df2ea184cfd
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sat Jul 4 18:51:30 2026 +0600

    fix: CRITICAL PRODUCTION BLOCKER - Fix Admin Dashboard Blank Page & 404 Token Errors
    
    1. Resolved ReferenceError: getApiBaseUrl is not defined by centralizing getApiBaseUrl utility inside packages/ui-components/src/utils/api.ts, exporting it, and injecting it globally in apps/studio-client/src/main.tsx.
    2. Fixed firebase.json routing configuration. Added explicit rewrites for /admin-api/** endpoints to correctly proxy target requests to supremeai-api, stopping SPA fallback.

 apps/studio-client/src/contexts/ThemeContext.tsx |  1 +
 apps/studio-client/src/main.tsx                  |  4 ++++
 config/firebase.json                             | 14 ++++++++++++++
 packages/ui-components/src/index.ts              |  2 ++
 packages/ui-components/src/utils/api.ts          | 15 +++++++++++++++
 5 files changed, 36 insertions(+)

```

## Diff Detail
```diff
commit 14c40b7eab8b8f9a53d65d81a59f5df2ea184cfd
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sat Jul 4 18:51:30 2026 +0600

    fix: CRITICAL PRODUCTION BLOCKER - Fix Admin Dashboard Blank Page & 404 Token Errors
    
    1. Resolved ReferenceError: getApiBaseUrl is not defined by centralizing getApiBaseUrl utility inside packages/ui-components/src/utils/api.ts, exporting it, and injecting it globally in apps/studio-client/src/main.tsx.
    2. Fixed firebase.json routing configuration. Added explicit rewrites for /admin-api/** endpoints to correctly proxy target requests to supremeai-api, stopping SPA fallback.

diff --git a/apps/studio-client/src/contexts/ThemeContext.tsx b/apps/studio-client/src/contexts/ThemeContext.tsx
index caf21ce7a..454db9bbb 100644
--- a/apps/studio-client/src/contexts/ThemeContext.tsx
+++ b/apps/studio-client/src/contexts/ThemeContext.tsx
@@ -1,5 +1,6 @@
 import React, { createContext, useContext, useEffect, useState } from 'react';
 import { getAdminToken } from '../services/adminTokenStore';
+import { getApiBaseUrl } from '../utils/api';
 
 // বাংলা মন্তব্য: ৪টি থিম সাপোর্ট করা হচ্ছে — Dark Space, Sky Blue, Sunset Ember, Emerald Matrix
 type Theme = 'dark' | 'light' | 'sunset' | 'matrix';
diff --git a/apps/studio-client/src/main.tsx b/apps/studio-client/src/main.tsx
index 675bc81f3..c0aa3e891 100644
--- a/apps/studio-client/src/main.tsx
+++ b/apps/studio-client/src/main.tsx
@@ -3,6 +3,10 @@ import { StrictMode } from 'react'
 import { createRoot } from 'react-dom/client'
 import './index.css'
 import { App } from './App.tsx'
+import { getApiBaseUrl } from './utils/api';
+
+// Inject globally for any UI components or legacy scripts that expect it
+(window as any).getApiBaseUrl = getApiBaseUrl;
 
 import { ThemeProvider } from './contexts/ThemeContext'
 // Shared providers (react-query, monaco defaults)
diff --git a/config/firebase.json b/config/firebase.json
index 87339e687..4534c5de8 100644
--- a/config/firebase.json
+++ b/config/firebase.json
@@ -20,6 +20,13 @@
         "package-lock.json"
       ],
       "rewrites": [
+        {
+          "source": "/admin-api/**",
+          "run": {
+            "serviceId": "supremeai-api",
+            "region": "us-central1"
+          }
+        },
         {
           "source": "/api/**",
           "run": {
@@ -127,6 +134,13 @@
         "package-lock.json"
       ],
       "rewrites": [
+        {
+          "source": "/admin-api/**",
+          "run": {
+            "serviceId": "supremeai-api",
+            "region": "us-central1"
+          }
+        },
         {
           "source": "/api/**",
           "run": {
diff --git a/packages/ui-components/src/index.ts b/packages/ui-components/src/index.ts
index 5b74eb80f..e667449af 100644
--- a/packages/ui-components/src/index.ts
+++ b/packages/ui-components/src/index.ts
@@ -3,3 +3,5 @@ export { LiveSujonBackground } from './components/LiveSujonBackground';
 export { SharedProviders } from './contexts/SharedProviders';
 
 export { ChatBubble } from './ChatBubble';
+export { getApiBaseUrl } from './utils/api';
+  
\ No newline at end of file
diff --git a/packages/ui-components/src/utils/api.ts b/packages/ui-components/src/utils/api.ts
new file mode 100644
index 000000000..518a2ec8f
--- /dev/null
+++ b/packages/ui-components/src/utils/api.ts
@@ -0,0 +1,15 @@
+export const getApiBaseUrl = (): string => {
+  if (typeof window === 'undefined') {
+    return import.meta.env.VITE_API_BASE || import.meta.env.VITE_API_URL || 'http://localhost:8000';
+  }
+
+  if (import.meta.env.VITE_API_BASE) {
+    return import.meta.env.VITE_API_BASE;
+  }
+
+  if (import.meta.env.VITE_API_URL) {
+    return import.meta.env.VITE_API_URL;
+  }
+
+  return window.location.origin;
+};

```
