# 📋 Commit f1cbf044f78b0b06ffa4fccfd34bb6983fb049e2

## Commit Stats
```
commit f1cbf044f78b0b06ffa4fccfd34bb6983fb049e2
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Tue Jul 7 17:14:06 2026 +0600

    feat: implement domain-driven routing, fix CI artifact path bug, and resolve 401/429 admin API cascade

 .github/workflows/supreme-core-ci.yml              |  6 +-
 apps/studio-client/src/App.tsx                     | 71 +++++++++++-----------
 .../src/components/admin/CloudOrchestrator.tsx     | 10 ++-
 .../src/components/admin/HealthBanner.tsx          |  5 ++
 .../src/components/admin/RBACManager.tsx           | 24 ++++----
 5 files changed, 63 insertions(+), 53 deletions(-)

```

## Diff Detail
```diff
commit f1cbf044f78b0b06ffa4fccfd34bb6983fb049e2
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Tue Jul 7 17:14:06 2026 +0600

    feat: implement domain-driven routing, fix CI artifact path bug, and resolve 401/429 admin API cascade

diff --git a/.github/workflows/supreme-core-ci.yml b/.github/workflows/supreme-core-ci.yml
index 1c9f16dda..3ab5d0cf4 100644
--- a/.github/workflows/supreme-core-ci.yml
+++ b/.github/workflows/supreme-core-ci.yml
@@ -326,6 +326,8 @@ jobs:
             ${{ runner.os }}-turbo-
 
       - name: Build & Lint Frontend Packages
+        env:
+          VITE_PORTAL_TYPE: 'admin'
         run: pnpm turbo run build lint --filter=supremeai-studio-client --filter=@supremeai/web-chat --filter=supremeai-vscode --cache-dir=.turbo
 
       - name: Run Studio Client Vitest with JSON Report
@@ -614,11 +616,11 @@ jobs:
         uses: actions/download-artifact@v4
         with:
           name: frontend-dist
-          path: apps
+          path: .
 
       - name: 🌐 Deploy to Firebase
         run: |
-          npx -y firebase-tools deploy --only hosting --project ${{ secrets.GCP_PROJECT_ID }} --token "${{ secrets.FIREBASE_TOKEN }}" || true
+          npx -y firebase-tools deploy --only hosting --project ${{ secrets.GCP_PROJECT_ID }} --token "${{ secrets.FIREBASE_TOKEN }}"
 
   sync-mirror:
     name: 📤 Sync to Secondary Repo
diff --git a/apps/studio-client/src/App.tsx b/apps/studio-client/src/App.tsx
index 661bce5a2..d583ca628 100644
--- a/apps/studio-client/src/App.tsx
+++ b/apps/studio-client/src/App.tsx
@@ -1,5 +1,5 @@
 import React, { useEffect, useState, useMemo } from "react";
-import { Routes, Route } from "react-router-dom";
+import { Routes, Route, Navigate } from "react-router-dom";
 import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
 import { useStore } from "./store/useStore";
 
@@ -91,30 +91,12 @@ function AdminShell() {
     }
   }, [theme]);
 
+  // বাংলা মন্তব্য: health-map, costs, users-এর raw fetch() কল সরানো হয়েছে —
+  // useDashboardData.ts ও useAdminApi.ts-এ React Query হুক এই ডেটা ইতিমধ্যে ফেচ করে।
+  // ডুপ্লিকেট কল সরানোর ফলে 429 রেট লিমিট স্টর্ম বন্ধ হবে।
   useEffect(() => {
     if (!adminAuthenticated) return;
 
-    const API_BASE = getApiBaseUrl();
-    const headers = {
-      "Authorization": `Bearer ${getAdminToken()}`,
-      "Content-Type": "application/json"
-    };
-
-    fetch(`${API_BASE}/admin-api/health-map`, { headers })
-      .then(res => res.json())
-      .then(data => setHealthMap(data))
-      .catch(err => console.error("Error fetching health map:", err));
-
-    fetch(`${API_BASE}/admin-api/costs`, { headers })
-      .then(res => res.json())
-      .then(data => setCostReport(data.report || ""))
-      .catch(err => console.error("Error fetching cost report:", err));
-
-    fetch(`${API_BASE}/admin-api/users`, { headers })
-      .then(res => res.json())
-      .then(data => setAdminUsers(data))
-      .catch(err => console.error("Error fetching users:", err));
-
     setEnvConfig({
       "ENV": "local",
       "DEBUG": "true",
@@ -287,6 +269,9 @@ function AdminShell() {
   );
 }
 
+// .env থেকে পোর্টাল টাইপটি পড়বে (ডিফল্ট: user)
+const PORTAL_TYPE = import.meta.env.VITE_PORTAL_TYPE || 'user';
+
 export const App: React.FC = () => {
   const {
     isServerOnline, setServerStatus, deployGate, fetchGateStatus
@@ -485,21 +470,33 @@ export const App: React.FC = () => {
     <ErrorBoundary>
       <QueryClientProvider client={queryClient}>
         <Routes>
-          {/* ১. পাবলিক/ইউজার রাউট */}
-          <Route path="/" element={legacyWorkspace} />
-          
-          {/* ২. অ্যাডমিন রাউট */}
-          <Route path="/admin/*" element={<AdminShell />} />
-          
-          {/* ৩. প্রোডাকশন ড্যাশবোর্ড শেল */}
-          <Route path="/workspace/*" element={
-            <DashboardShell
-              theme={theme}
-              toggleTheme={toggleTheme}
-              isServerOnline={isServerOnline}
-              workspace={legacyWorkspace}
-            />
-          } />
+          {PORTAL_TYPE === 'admin' ? (
+            /* =========================================
+               ADMIN PORTAL (supremeai-admin.web.app)
+            ========================================= */
+            <>
+              <Route path="/" element={<Navigate to="/admin" replace />} />
+              <Route path="/admin/*" element={<AdminShell />} />
+              <Route path="*" element={<Navigate to="/admin" replace />} />
+            </>
+          ) : (
+            /* =========================================
+               USER PORTAL (supremeai-lac.vercel.app)
+            ========================================= */
+            <>
+              <Route path="/" element={legacyWorkspace} />
+              <Route path="/workspace/*" element={
+                <DashboardShell
+                  theme={theme}
+                  toggleTheme={toggleTheme}
+                  isServerOnline={isServerOnline}
+                  workspace={legacyWorkspace}
+                />
+              } />
+              {/* ইউজাররা /admin এ যাওয়ার চেষ্টা করলে হোমপেজে পাঠিয়ে দেবে */}
+              <Route path="/admin/*" element={<Navigate to="/" replace />} />
+            </>
+          )}
         </Routes>
       </QueryClientProvider>
     </ErrorBoundary>
diff --git a/apps/studio-client/src/components/admin/CloudOrchestrator.tsx b/apps/studio-client/src/components/admin/CloudOrchestrator.tsx
index 9a4271f2f..f385db652 100644
--- a/apps/studio-client/src/components/admin/CloudOrchestrator.tsx
+++ b/apps/studio-client/src/components/admin/CloudOrchestrator.tsx
@@ -1,6 +1,9 @@
 import { useQuery } from '@tanstack/react-query';
 import { Card, Badge, Skeleton } from '../ui';
 import { Globe, HardDrive, Cpu, Network, RefreshCw } from 'lucide-react';
+// বাংলা মন্তব্য: raw fetch()-এর বদলে apiClient ব্যবহার করা হচ্ছে — auth হেডার ও থ্রটল গ্যারান্টি দেয়
+import { apiClient } from '../../services/apiClient';
+import { getAdminToken } from '../../services/adminTokenStore';
 
 const CLOUD_PROVIDERS = [
   { id: 'gcp', name: 'Google Cloud Platform', color: '#4285f4', icon: Globe },
@@ -13,9 +16,12 @@ const CLOUD_PROVIDERS = [
 ];
 
 export function CloudOrchestrator() {
+  // বাংলা মন্তব্য: queryKey ম্যাচ করানো হয়েছে useDashboardData.useHealthMap()-এর সাথে — ক্যাশ শেয়ার হবে, ডুপ্লিকেট ফেচ বন্ধ
   const { data: health, isLoading } = useQuery({
-    queryKey: ['cloud-health'],
-    queryFn: () => fetch('/admin-api/health-map').then(r => r.json()),
+    queryKey: ['dashboard', 'health'],
+    queryFn: () => apiClient.get<any>('/admin-api/health-map'),
+    enabled: !!getAdminToken(),
+    staleTime: 20_000,
   });
 
   const providerHealth = Object.entries(health || {}).map(([id, data]: [string, any]) => ({
diff --git a/apps/studio-client/src/components/admin/HealthBanner.tsx b/apps/studio-client/src/components/admin/HealthBanner.tsx
index a208fe6bd..308190441 100644
--- a/apps/studio-client/src/components/admin/HealthBanner.tsx
+++ b/apps/studio-client/src/components/admin/HealthBanner.tsx
@@ -1,12 +1,17 @@
 import { useQuery } from '@tanstack/react-query';
 import { motion, AnimatePresence } from 'framer-motion';
 import { apiClient } from '../../services/apiClient';
+// বাংলা মন্তব্য: টোকেন গার্ড — টোকেন ছাড়া health-map রিকোয়েস্ট যাবে না, 401 স্টর্ম ঠেকাবে
+import { getAdminToken } from '../../services/adminTokenStore';
 
 const HealthBanner: React.FC = () => {
   const { data: health } = useQuery({
     queryKey: ['dashboard', 'health'],
     queryFn: () => apiClient.get<{ gcp: { status: string }; railway: { status: string }; render: { status: string } }>('/admin-api/health-map'),
     refetchInterval: (query: any) => query.state.error ? false : 30000,
+    // বাংলা মন্তব্য: টোকেন না থাকলে কোয়েরি ডিসেবল — অপ্রয়োজনীয় 401 ঠেকাতে
+    enabled: !!getAdminToken(),
+    staleTime: 20_000,
   });
 
   const isDegraded = (health?.gcp && health.gcp.status === 'degraded') || (health?.railway && health.railway.status === 'degraded') || (health?.render && health.render.status === 'degraded');
diff --git a/apps/studio-client/src/components/admin/RBACManager.tsx b/apps/studio-client/src/components/admin/RBACManager.tsx
index 707c37a53..babd47654 100644
--- a/apps/studio-client/src/components/admin/RBACManager.tsx
+++ b/apps/studio-client/src/components/admin/RBACManager.tsx
@@ -2,29 +2,29 @@ import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
 import { useState } from 'react';
 import { Card, Badge } from '../ui';
 import { Shield, UserPlus, Trash2, Settings2, CheckCircle2, XCircle } from 'lucide-react';
+// বাংলা মন্তব্য: raw fetch()-এর বদলে apiClient ব্যবহার — auth হেডার ও থ্রটল নিশ্চিত করে
+import { apiClient } from '../../services/apiClient';
+import { getAdminToken } from '../../services/adminTokenStore';
 
 export function RBACManager() {
+  // বাংলা মন্তব্য: queryKey ম্যাচ করানো হয়েছে useAdminApi.useAdminUsers()-এর সাথে — ক্যাশ শেয়ার হবে
   const { data: users } = useQuery({
-    queryKey: ['users'],
-    queryFn: () => fetch('/admin-api/users').then(r => r.json()),
+    queryKey: ['admin', 'users'],
+    queryFn: () => apiClient.get<any[]>('/admin-api/users'),
+    enabled: !!getAdminToken(),
+    staleTime: 30_000,
   });
   const qc = useQueryClient();
   const [newUser, setNewUser] = useState({ username: '', role: 'Operator', permissions: 'read,write' });
 
   const addUser = useMutation({
-    mutationFn: (user: any) =>
-      fetch('/admin-api/users', {
-        method: 'POST',
-        headers: { 'Content-Type': 'application/json' },
-        body: JSON.stringify(user),
-      }).then(r => r.json()),
-    onSuccess: () => qc.invalidateQueries({ queryKey: ['users'] }),
+    mutationFn: (user: any) => apiClient.post('/admin-api/users', user),
+    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'users'] }),
   });
 
   const deleteUser = useMutation({
-    mutationFn: (username: string) =>
-      fetch(`/admin-api/users/${username}`, { method: 'DELETE' }).then(r => r.json()),
-    onSuccess: () => qc.invalidateQueries({ queryKey: ['users'] }),
+    mutationFn: (username: string) => apiClient.delete(`/admin-api/users/${username}`),
+    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'users'] }),
   });
 
   const roleColors: Record<string, 'purple' | 'info' | 'warning' | 'default'> = {

```
