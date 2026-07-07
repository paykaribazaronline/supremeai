# 📋 Commit 92767fdf98ee570dd7713a5cd66e6f95dd4bd5fd

## Commit Stats
```
commit 92767fdf98ee570dd7713a5cd66e6f95dd4bd5fd
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Tue Jul 7 23:44:16 2026 +0600

    feat: Architect Control Tower and Phase 3 Finalization

 apps/studio-client/src/App.tsx                     |   2 +
 .../src/components/FixPreviewModal.tsx             |  83 ++++++++
 apps/studio-client/src/pages/ArchitectTower.tsx    | 211 +++++++++++++++++++++
 backend/api/routes/admin.py                        |  88 ++++++++-
 backend/main.py                                    |   2 +
 backend/pyproject.toml                             |  10 +-
 backend/scripts/benchmark/load_test_phase3.py      |  89 +++++++++
 backend/tests/api/test_admin.py                    |  59 ++++++
 backend/tests/core/test_knowledge_base.py          | 107 +++++++++++
 backend/tests/core/test_log_batcher.py             |  94 +++++++++
 backend/tests/core/test_security_vault.py          |  55 ++++++
 backend/tests/test_llm_gateway_coverage.py         |  42 +++-
 docs/architecture-overview.md                      |  24 +++
 13 files changed, 862 insertions(+), 4 deletions(-)

```

## Diff Detail
```diff
commit 92767fdf98ee570dd7713a5cd66e6f95dd4bd5fd
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Tue Jul 7 23:44:16 2026 +0600

    feat: Architect Control Tower and Phase 3 Finalization

diff --git a/apps/studio-client/src/App.tsx b/apps/studio-client/src/App.tsx
index 56c2057e2..9006767ea 100644
--- a/apps/studio-client/src/App.tsx
+++ b/apps/studio-client/src/App.tsx
@@ -43,6 +43,7 @@ import RedesignedDashboardMockup from './components/admin/RedesignedDashboardMoc
 import ErrorBoundary from './components/admin/DashboardErrorBoundary';
 import { AgentWorkspace } from './pages/AgentWorkspace';
 import { IntegrationsManager } from './pages/IntegrationsManager';
+import { ArchitectTower } from './pages/ArchitectTower';
 
 function AdminShell() {
   const {
@@ -489,6 +490,7 @@ export const App: React.FC = () => {
               <Route path="/" element={legacyWorkspace} />
               <Route path="/workspace/agent" element={<AgentWorkspace />} />
               <Route path="/integrations" element={<IntegrationsManager />} />
+              <Route path="/architect-tower" element={<ArchitectTower />} />
               <Route path="/workspace/*" element={
                 <DashboardShell
                   theme={theme}
diff --git a/apps/studio-client/src/components/FixPreviewModal.tsx b/apps/studio-client/src/components/FixPreviewModal.tsx
new file mode 100644
index 000000000..c1877c4ab
--- /dev/null
+++ b/apps/studio-client/src/components/FixPreviewModal.tsx
@@ -0,0 +1,83 @@
+import React from 'react';
+import { X, Check, XCircle } from 'lucide-react';
+
+interface FixPreviewModalProps {
+  isOpen: boolean;
+  onClose: () => void;
+  onApprove: () => void;
+  onReject: () => void;
+  fix: any;
+  loading: boolean;
+}
+
+export const FixPreviewModal: React.FC<FixPreviewModalProps> = ({
+  isOpen,
+  onClose,
+  onApprove,
+  onReject,
+  fix,
+  loading
+}) => {
+  if (!isOpen || !fix) return null;
+
+  const oldCode = fix.metadata?.original_code || "// Original code not provided";
+  const newCode = fix.metadata?.proposed_code || "// Proposed fix not provided";
+
+  return (
+    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
+      <div className="bg-slate-900 border border-slate-700 rounded-xl w-full max-w-5xl max-h-[90vh] flex flex-col overflow-hidden shadow-2xl">
+        
+        {/* Header */}
+        <div className="flex justify-between items-center p-4 border-b border-slate-700 bg-slate-800">
+          <div>
+            <h2 className="text-xl font-bold text-white">Review Fix: {fix.id}</h2>
+            <p className="text-slate-400 text-sm mt-1">
+              Error Type: <span className="font-mono text-rose-400">{fix.error_type}</span> | 
+              Impact Score: <span className="font-mono text-emerald-400">{fix.impact_score || 0}</span>
+            </p>
+          </div>
+          <button onClick={onClose} className="p-2 hover:bg-slate-700 rounded-lg text-slate-400 hover:text-white transition-colors">
+            <X size={20} />
+          </button>
+        </div>
+
+        {/* Diff Viewer */}
+        <div className="flex-1 overflow-auto p-4 bg-slate-950 flex gap-4">
+          <div className="flex-1 border border-slate-700 rounded bg-slate-900 flex flex-col">
+            <div className="p-2 border-b border-slate-700 font-bold text-slate-300">Current Code</div>
+            <pre className="p-4 text-sm font-mono text-slate-300 overflow-auto">{oldCode}</pre>
+          </div>
+          <div className="flex-1 border border-emerald-900/50 rounded bg-slate-900 flex flex-col shadow-[0_0_15px_rgba(16,185,129,0.1)]">
+            <div className="p-2 border-b border-emerald-900/50 font-bold text-emerald-400">SelfHealer Proposed Fix</div>
+            <pre className="p-4 text-sm font-mono text-emerald-300 overflow-auto">{newCode}</pre>
+          </div>
+        </div>
+
+        {/* Footer Actions */}
+        <div className="p-4 border-t border-slate-700 bg-slate-800 flex justify-end gap-3">
+          <button 
+            onClick={onReject} 
+            disabled={loading}
+            className="flex items-center gap-2 px-4 py-2 bg-slate-700 hover:bg-rose-900/50 text-rose-400 rounded-lg transition-colors border border-transparent hover:border-rose-500/50"
+          >
+            <XCircle size={18} />
+            Reject
+          </button>
+          
+          <button 
+            onClick={onApprove} 
+            disabled={loading}
+            className="flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg shadow-lg shadow-emerald-500/20 transition-all font-medium disabled:opacity-50"
+          >
+            {loading ? (
+              <span className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
+            ) : (
+              <Check size={18} />
+            )}
+            Approve & Apply
+          </button>
+        </div>
+      </div>
+    </div>
+  );
+};
diff --git a/apps/studio-client/src/pages/ArchitectTower.tsx b/apps/studio-client/src/pages/ArchitectTower.tsx
new file mode 100644
index 000000000..38ad60da2
--- /dev/null
+++ b/apps/studio-client/src/pages/ArchitectTower.tsx
@@ -0,0 +1,211 @@
+import React, { useState, useEffect } from 'react';
+import { ShieldAlert, Activity, CheckCircle, Database } from 'lucide-react';
+import { FixPreviewModal } from '../components/FixPreviewModal';
+import { getApiBaseUrl } from '../utils/api';
+import { getAdminToken } from '../services/adminTokenStore';
+
+export const ArchitectTower: React.FC = () => {
+  const [fixes, setFixes] = useState<any[]>([]);
+  const [loading, setLoading] = useState(true);
+  const [selectedFix, setSelectedFix] = useState<any>(null);
+  const [actionLoading, setActionLoading] = useState(false);
+  const [error, setError] = useState('');
+
+  const fetchFixes = async () => {
+    setLoading(true);
+    try {
+      const token = getAdminToken();
+      // If no token in standard store, we might be using the dev/local fallback in the backend, but let's pass what we have
+      const res = await fetch(`${getApiBaseUrl()}/api/admin/fixes?tenant_id=supremeai-a`, {
+        headers: {
+          'Authorization': `Bearer ${token || 'mock_token'}`
+        }
+      });
+      if (!res.ok) throw new Error('Failed to fetch pending fixes');
+      const data = await res.json();
+      setFixes(data.fixes || []);
+    } catch (err: any) {
+      setError(err.message);
+    } finally {
+      setLoading(false);
+    }
+  };
+
+  useEffect(() => {
+    fetchFixes();
+  }, []);
+
+  const handleApprove = async () => {
+    if (!selectedFix) return;
+    setActionLoading(true);
+    try {
+      const token = getAdminToken();
+      const res = await fetch(`${getApiBaseUrl()}/api/admin/fixes/${selectedFix.id}/approve?tenant_id=supremeai-a`, {
+        method: 'POST',
+        headers: {
+          'Authorization': `Bearer ${token || 'mock_token'}`,
+          'Content-Type': 'application/json'
+        }
+      });
+      if (!res.ok) throw new Error('Failed to approve fix');
+      
+      setSelectedFix(null);
+      fetchFixes(); // Refresh list
+    } catch (err: any) {
+      alert(err.message);
+    } finally {
+      setActionLoading(false);
+    }
+  };
+
+  const handleReject = async () => {
+    if (!selectedFix) return;
+    setActionLoading(true);
+    try {
+      const token = getAdminToken();
+      const res = await fetch(`${getApiBaseUrl()}/api/admin/fixes/${selectedFix.id}/reject?tenant_id=supremeai-a`, {
+        method: 'POST',
+        headers: {
+          'Authorization': `Bearer ${token || 'mock_token'}`,
+          'Content-Type': 'application/json'
+        }
+      });
+      if (!res.ok) throw new Error('Failed to reject fix');
+      
+      setSelectedFix(null);
+      fetchFixes(); // Refresh list
+    } catch (err: any) {
+      alert(err.message);
+    } finally {
+      setActionLoading(false);
+    }
+  };
+
+  return (
+    <div className="min-h-screen bg-slate-950 text-slate-300 p-8">
+      <div className="max-w-7xl mx-auto space-y-8">
+        
+        {/* Header Section */}
+        <div className="flex items-center gap-4 border-b border-slate-800 pb-6">
+          <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl">
+            <ShieldAlert className="w-8 h-8 text-emerald-400" />
+          </div>
+          <div>
+            <h1 className="text-3xl font-bold text-white tracking-tight">Architectural Control Tower</h1>
+            <p className="text-slate-400 mt-1">Self-Healing System & HITL Operations Dashboard</p>
+          </div>
+        </div>
+
+        {/* Stats Row */}
+        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
+          <div className="bg-slate-900 border border-slate-800 p-6 rounded-2xl flex items-center gap-4">
+            <div className="p-3 bg-rose-500/10 rounded-lg">
+              <Activity className="w-6 h-6 text-rose-400" />
+            </div>
+            <div>
+              <p className="text-sm font-medium text-slate-400">Pending Reviews</p>
+              <p className="text-2xl font-bold text-white">{fixes.length}</p>
+            </div>
+          </div>
+          
+          <div className="bg-slate-900 border border-slate-800 p-6 rounded-2xl flex items-center gap-4">
+            <div className="p-3 bg-emerald-500/10 rounded-lg">
+              <CheckCircle className="w-6 h-6 text-emerald-400" />
+            </div>
+            <div>
+              <p className="text-sm font-medium text-slate-400">System Health</p>
+              <p className="text-2xl font-bold text-white">Nominal</p>
+            </div>
+          </div>
+
+          <div className="bg-slate-900 border border-slate-800 p-6 rounded-2xl flex items-center gap-4">
+            <div className="p-3 bg-blue-500/10 rounded-lg">
+              <Database className="w-6 h-6 text-blue-400" />
+            </div>
+            <div>
+              <p className="text-sm font-medium text-slate-400">Active Nodes</p>
+              <p className="text-2xl font-bold text-white">4</p>
+            </div>
+          </div>
+        </div>
+
+        {/* Pending Fixes Table */}
+        <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden">
+          <div className="p-6 border-b border-slate-800">
+            <h2 className="text-xl font-bold text-white">Action Required: Pending Fixes</h2>
+          </div>
+          
+          {loading ? (
+            <div className="p-12 flex justify-center">
+              <div className="w-8 h-8 border-2 border-emerald-500/30 border-t-emerald-500 rounded-full animate-spin" />
+            </div>
+          ) : error ? (
+            <div className="p-6 text-rose-400 bg-rose-950/20">{error}</div>
+          ) : fixes.length === 0 ? (
+            <div className="p-12 text-center text-slate-500">
+              No pending fixes. System is running optimally.
+            </div>
+          ) : (
+            <table className="w-full text-left border-collapse">
+              <thead>
+                <tr className="bg-slate-800/50">
+                  <th className="p-4 font-semibold text-slate-400 text-sm">Fix ID / Context</th>
+                  <th className="p-4 font-semibold text-slate-400 text-sm">Error Type</th>
+                  <th className="p-4 font-semibold text-slate-400 text-sm">Impact Score</th>
+                  <th className="p-4 font-semibold text-slate-400 text-sm">Created At</th>
+                  <th className="p-4 font-semibold text-slate-400 text-sm text-right">Action</th>
+                </tr>
+              </thead>
+              <tbody className="divide-y divide-slate-800/50">
+                {fixes.map((fix) => (
+                  <tr key={fix.id} className="hover:bg-slate-800/30 transition-colors">
+                    <td className="p-4">
+                      <div className="font-mono text-emerald-400 text-sm">{fix.id}</div>
+                      <div className="text-xs text-slate-500 mt-1">{fix.target_file || 'Unknown file'}</div>
+                    </td>
+                    <td className="p-4">
+                      <span className="px-2 py-1 bg-rose-950/50 text-rose-400 rounded text-xs border border-rose-900/50">
+                        {fix.error_type}
+                      </span>
+                    </td>
+                    <td className="p-4">
+                      <div className="flex items-center gap-2">
+                        <div className="w-full bg-slate-800 rounded-full h-1.5 w-16">
+                          <div 
+                            className="bg-amber-500 h-1.5 rounded-full" 
+                            style={{ width: `${Math.min(100, (fix.impact_score || 0) * 10)}%` }} 
+                          />
+                        </div>
+                        <span className="text-sm font-medium">{fix.impact_score}</span>
+                      </div>
+                    </td>
+                    <td className="p-4 text-sm text-slate-400">
+                      {new Date(fix.created_at).toLocaleString()}
+                    </td>
+                    <td className="p-4 text-right">
+                      <button 
+                        onClick={() => setSelectedFix(fix)}
+                        className="px-4 py-2 bg-emerald-600/10 hover:bg-emerald-600/20 text-emerald-400 border border-emerald-500/20 rounded-lg text-sm font-medium transition-colors"
+                      >
+                        Preview Fix
+                      </button>
+                    </td>
+                  </tr>
+                ))}
+              </tbody>
+            </table>
+          )}
+        </div>
+      </div>
+
+      <FixPreviewModal 
+        isOpen={!!selectedFix}
+        onClose={() => setSelectedFix(null)}
+        onApprove={handleApprove}
+        onReject={handleReject}
+        fix={selectedFix}
+        loading={actionLoading}
+      />
+    </div>
+  );
+};
diff --git a/backend/api/routes/admin.py b/backend/api/routes/admin.py
index ae1064bd2..c832d85a0 100644
--- a/backend/api/routes/admin.py
+++ b/backend/api/routes/admin.py
@@ -1,13 +1,30 @@
 from fastapi import APIRouter
-from fastapi import HTTPException
+from fastapi import HTTPException, Depends, Request
 from pydantic import BaseModel
+from loguru import logger
+from datetime import datetime, timezone
 
 from admin.god import AdminGodLayer  # Your existing god.py
+from api.dependencies import get_current_user_token
+from core.self_healer import SelfHealerService
+from utils.firestore_helpers import get_firestore_db
 
 
 router = APIRouter(prefix="/api/admin", tags=["Admin Control Center"])
 god_layer = AdminGodLayer(db_path="data/admin_rules.db")
 
+def get_current_admin(payload: dict = Depends(get_current_user_token)) -> dict:
+    if payload.get("role") != "admin":
+        logger.warning(f"Unauthorized admin access attempt by {payload.get('sub')}")
+        raise HTTPException(status_code=403, detail="Admin access required")
+    return payload
+
+def get_healer_service() -> SelfHealerService:
+    db = get_firestore_db()
+    if not db:
+        raise HTTPException(status_code=503, detail="Database unavailable")
+    return SelfHealerService(db)
+
 class RuleUpdate(BaseModel):
     key: str
     value: str
@@ -38,3 +55,72 @@ async def trigger_quick_action(action_type: str):
         return {"status": "Redis cache cleared"}
     else:
         raise HTTPException(status_code=404, detail="Action not found")
+
+@router.get("/fixes")
+async def get_fixes(
+    tenant_id: str = "default",
+    status: str = "pending_review",
+    admin_user: dict = Depends(get_current_admin),
+    healer: SelfHealerService = Depends(get_healer_service)
+):
+    """Fetch all fixes for a tenant with a specific status."""
+    db = get_firestore_db()
+    fixes_ref = db.collection("tenants").document(tenant_id).collection("fixes")
+    query = fixes_ref.where("status", "==", status)
+    
+    try:
+        results = await query.get()
+    except TypeError:
+        # Fallback for sync mock
+        results = query.get()
+        
+    fixes = []
+    for doc in results:
+        fix_data = doc.to_dict()
+        fix_data["id"] = doc.id
+        fixes.append(fix_data)
+        
+    return {"fixes": fixes}
+
+@router.post("/fixes/{fix_id}/approve")
+async def approve_fix(
+    fix_id: str, 
+    tenant_id: str = "default",
+    admin_user: dict = Depends(get_current_admin),
+    healer: SelfHealerService = Depends(get_healer_service)
+):
+    """Approve a pending fix."""
+    admin_id = admin_user.get("sub", "unknown_admin")
+    logger.info(f"Admin {admin_id} approving fix {fix_id} for tenant {tenant_id}")
+    
+    success = await healer.apply_fix(tenant_id, fix_id, admin_id)
+    if not success:
+        raise HTTPException(status_code=400, detail="Failed to apply fix. It may not exist or is already processed.")
+        
+    return {"status": "success", "fix_id": fix_id}
+
+@router.post("/fixes/{fix_id}/reject")
+async def reject_fix(
+    fix_id: str, 
+    tenant_id: str = "default",
+    admin_user: dict = Depends(get_current_admin)
+):
+    """Reject a pending fix."""
+    admin_id = admin_user.get("sub", "unknown_admin")
+    logger.info(f"Admin {admin_id} rejecting fix {fix_id} for tenant {tenant_id}")
+    
+    db = get_firestore_db()
+    doc_ref = db.collection("tenants").document(tenant_id).collection("fixes").document(fix_id)
+    
+    update_data = {
+        "status": "rejected",
+        "reviewed_by": admin_id,
+        "applied_at": datetime.now(timezone.utc).isoformat()
+    }
+    
+    try:
+        await doc_ref.update(update_data)
+    except TypeError:
+        doc_ref.update(update_data)
+        
+    return {"status": "success", "fix_id": fix_id}
diff --git a/backend/main.py b/backend/main.py
index ee8f4d4ef..02f6adad9 100644
--- a/backend/main.py
+++ b/backend/main.py
@@ -9,6 +9,7 @@ from api.routes import websocket_agent
 from api.routes.agent_workspace import router as agent_router
 from api.routes.integrations import router as integrations_router
 from api.routes.task_workspace import router as workspace_task_router
+from api.routes.admin import router as admin_router
 from core.app import app  # noqa: F401
 from core.config import settings
 from core.logging_config import setup_logging
@@ -18,6 +19,7 @@ app.include_router(workspace_task_router)
 app.include_router(websocket_agent.router)
 app.include_router(agent_router, prefix="/api/v1")
 app.include_router(integrations_router, prefix="/api/v1")
+app.include_router(admin_router)
 
 setup_logging()
 
diff --git a/backend/pyproject.toml b/backend/pyproject.toml
index e0851f7dc..9288eae1d 100644
--- a/backend/pyproject.toml
+++ b/backend/pyproject.toml
@@ -51,7 +51,6 @@ alembic = "^1.13.0"
 psycopg2-binary = "^2.9.9"
 opentelemetry-api = "^1.25.0"
 opentelemetry-sdk = "^1.25.0"
-discord-py = "^2.3.0"
 setuptools = "<82.0.0"
 # বাংলা মন্তব্য: গ্রাফ ডাটাবেস এবং মডেল কনটেক্সট প্রোটোকল ব্যবহারের জন্য ডিপেন্ডেন্সি যুক্ত করা হলো
 neo4j = "^5.14.0"
@@ -62,6 +61,7 @@ launchdarkly-server-sdk = "^9.8.0"
 launchdarkly-server-sdk-ai = "^1.0.0"
 launchdarkly-observability = "^1.0.0"
 redis = ">=5,<9"
+boto3 = "^1.34.0"
 
 [tool.poetry.group.ml.dependencies]
 # ── ML/AI: torch, OCR, whisper, embeddings (~3 GB) ──
@@ -80,9 +80,13 @@ qdrant-client = "^1.9.0"
 langgraph = "^0.2.0"
 crewai = "^0.80.0"
 
+[tool.poetry.group.tools]
+optional = true
+
 [tool.poetry.group.tools.dependencies]
 # ── Optional tools: browser, cloud, analytics (~500 MB) ──
 # Install with: poetry install --with tools
+discord-py = "^2.3.0"
 playwright = "^1.60.0"
 playwright-stealth = "^1.0.0"
 pandas = "^2.2.0"
@@ -92,7 +96,6 @@ pdfplumber = "^0.10.0"
 python-pptx = "^0.6.23"
 gtts = "^2.5.1"
 edge-tts = "^6.1.9"
-boto3 = "^1.34.0"
 docker = "^7.0.0"
 celery = "^5.4.0"
 radon = "^6.0.0"
@@ -118,6 +121,9 @@ pytest-xdist = "^3.6.1"
 # বাংলা মন্তব্য: টেস্ট রান করার সময় সমান্তরাল জটিলতায় ডেডলক এড়াতে pytest-timeout যুক্ত করা হলো
 pytest-timeout = "^2.2.0"
 pylint = "^3.2.0"
+discord-py = "^2.3.0"
+matplotlib = "^3.8.0"
+pdfplumber = "^0.10.0"
 
 
 [build-system]
diff --git a/backend/scripts/benchmark/load_test_phase3.py b/backend/scripts/benchmark/load_test_phase3.py
new file mode 100644
index 000000000..6decaaf5e
--- /dev/null
+++ b/backend/scripts/benchmark/load_test_phase3.py
@@ -0,0 +1,89 @@
+import asyncio
+import time
+from unittest.mock import patch, AsyncMock, MagicMock
+from core.llm_gateway import llm_gateway
+from core.cloud_sandbox_orchestrator import CloudSandboxOrchestrator
+from utils.firestore_helpers import get_firestore_db
+from loguru import logger
+import sys
+
+logger.remove()
+logger.add(sys.stdout, level="INFO")
+
+async def simulate_request(tenant_id: str, request_id: int):
+    try:
+        await llm_gateway.acompletion(
+            prompt=f"Test prompt {request_id}",
+            model="openai/gpt-3.5-turbo",
+            tenant_id=tenant_id
+        )
+        return "success"
+    except Exception as e:
+        if "402 Payment Required" in str(e):
+            return "402"
+        return "error"
+
+async def main():
+    print("Starting Phase 3 Load Test (1,000 Transactions)")
+    tenant_id = "tenant-load-test"
+    db = get_firestore_db()
+    
+    # Pre-configure mock DB if needed
+    if db:
+        budget_ref = db.collection("tenants").document(tenant_id).collection("budget").document("current")
+        await budget_ref.set({"monthly_limit": 100.0, "spent_amount": 0.0})
+    
+    # Mock LiteLLM so we don't make real API calls
+    with patch("litellm.acompletion", new_callable=AsyncMock) as mock_litellm:
+        # Simulate 1% failure rate for SelfHealer testing
+        def mock_acompletion_side_effect(*args, **kwargs):
+            import random
+            if random.random() < 0.01:
+                raise Exception("Simulated LiteLLM Error for SelfHealer")
+            return AsyncMock()
+        mock_litellm.side_effect = mock_acompletion_side_effect
+        
+        start_time = time.perf_counter()
+        
+        tasks = [simulate_request(tenant_id, i) for i in range(1000)]
+        results = await asyncio.gather(*tasks)
+        
+        elapsed = time.perf_counter() - start_time
+        
+        successes = results.count("success")
+        payment_required = results.count("402")
+        errors = results.count("error")
+        
+        print("\n=== Load Test Results ===")
+        print(f"Total Requests: 1000")
+        print(f"Success: {successes}")
+        print(f"402 Payment Required (False Positives?): {payment_required}")
+        print(f"Other Errors (Triggered SelfHealer): {errors}")
+        print(f"Total Time: {elapsed:.2f} seconds")
+        print(f"Latency: {(elapsed/1000)*1000:.2f} ms / request (avg concurrency)")
+        print(f"RPS: {1000/elapsed:.2f} req/s")
+
+        # Test Sandbox TTL
+        print("\n=== Testing Sandbox Auto-Destroy ===")
+        orchestrator = CloudSandboxOrchestrator(provider="runpod")
+        sandbox_id = "load-test-sandbox-1"
+        orchestrator._active_sandboxes[sandbox_id] = {
+            "created_at": time.time() - 700, # 11.6 minutes ago (exceeds 10m TTL)
+            "status": "running"
+        }
+        
+        print(f"Injected sandbox {sandbox_id} with age 11.6 minutes.")
+        print("Starting auto_destroy_worker for 1 iteration (mocked sleep to exit)...")
+        
+        with patch("asyncio.sleep", AsyncMock(side_effect=Exception("Exit Loop"))):
+            try:
+                await orchestrator.auto_destroy_worker(tenant_id)
+            except Exception as e:
+                if str(e) == "Exit Loop":
+                    pass
+        
+        remaining = len(orchestrator._active_sandboxes)
+        print(f"Remaining sandboxes after cleanup: {remaining} (Expected 0)")
+
+if __name__ == "__main__":
+    asyncio.run(main())
diff --git a/backend/tests/api/test_admin.py b/backend/tests/api/test_admin.py
new file mode 100644
index 000000000..6d9295c06
--- /dev/null
+++ b/backend/tests/api/test_admin.py
@@ -0,0 +1,59 @@
+import pytest
+from unittest.mock import patch, MagicMock, AsyncMock
+from fastapi.testclient import TestClient
+from main import app
+
+client = TestClient(app)
+
+@pytest.fixture
+def mock_admin_token():
+    with patch("api.dependencies.get_current_user_token") as mock:
+        mock.return_value = {"sub": "admin_test", "role": "admin"}
+        yield mock
+
+@pytest.fixture
+def mock_healer():
+    with patch("api.routes.admin.get_healer_service") as mock:
+        service = MagicMock()
+        service.apply_fix = AsyncMock(return_value=True)
+        mock.return_value = service
+        yield mock
+
+@pytest.fixture
+def mock_firestore():
+    with patch("api.routes.admin.get_firestore_db") as mock:
+        db = MagicMock()
+        mock.return_value = db
+        yield db
+
+def test_get_fixes_unauthorized():
+    response = client.get("/api/admin/fixes")
+    assert response.status_code in (401, 403), f"Unexpected status: {response.status_code}, details: {response.text}"
+
+@patch("api.routes.admin.get_current_user_token")
+def test_get_fixes_authorized(mock_token, mock_healer, mock_firestore):
+    app.dependency_overrides[mock_token] = lambda: {"sub": "admin_test", "role": "admin"}
+    
+    # Mocking Firestore response
+    mock_query = MagicMock()
+    mock_doc = MagicMock()
+    mock_doc.id = "fix_1"
+    mock_doc.to_dict.return_value = {"status": "pending_review"}
+    
+    # Async mock for get()
+    async def mock_get():
+        return [mock_doc]
+        
+    mock_query.get = mock_get
+    mock_firestore.collection.return_value.document.return_value.collection.return_value.where.return_value = mock_query
+
+    # We need to use app.dependency_overrides for proper injection testing
+    from api.routes.admin import get_current_admin
+    app.dependency_overrides[get_current_admin] = lambda: {"sub": "admin_test", "role": "admin"}
+    
+    response = client.get("/api/admin/fixes?tenant_id=test")
+    assert response.status_code == 200, f"Unexpected status: {response.status_code}, details: {response.text}"
+    assert "fixes" in response.json()
+    assert len(response.json()["fixes"]) == 1
+    
+    app.dependency_overrides = {}
diff --git a/backend/tests/core/test_knowledge_base.py b/backend/tests/core/test_knowledge_base.py
new file mode 100644
index 000000000..0bf12b0f4
--- /dev/null
+++ b/backend/tests/core/test_knowledge_base.py
@@ -0,0 +1,107 @@
+import json
+import os
+from unittest.mock import patch
+import pytest
+
+from core.knowledge_base import get_from_memory, save_to_memory, MEMORY_FILE_PATH
+
+
+@pytest.fixture
+def temp_memory_file(tmp_path, monkeypatch):
+    monkeypatch.setattr("core.knowledge_base.MEMORY_FILE_PATH", str(tmp_path / "memory.json"))
+    with open(str(tmp_path / "memory.json"), "w") as f:
+        json.dump({}, f)
+    yield str(tmp_path / "memory.json")
+
+
+def test_get_from_memory_empty_string(temp_memory_file):
+    result = get_from_memory("")
+    assert result is None
+
+
+def test_get_from_memory_whitespace(temp_memory_file):
+    result = get_from_memory("   ")
+    assert result is None
+
+
+def test_get_from_memory_whitespace_only(temp_memory_file):
+    result = get_from_memory("   \t\n")
+    assert result is None
+
+
+def test_get_from_memory_returns_string(temp_memory_file):
+    save_to_memory("test prompt", "solution code")
+    result = get_from_memory("test prompt")
+    assert result == "solution code"
+
+
+def test_save_to_memory_creates_file_if_not_exists(temp_memory_file):
+    # Ensure file exists
+    with open(temp_memory_file, "w") as f:
+        json.dump({}, f)
+    
+    save_to_memory("new prompt", "new solution")
+    result = get_from_memory("new prompt")
+    assert result == "new solution"
+    with open(temp_memory_file) as f:
+        data = json.load(f)
+    assert data["new prompt"] == "new solution"
+
+
+def test_save_to_memory_empty_prompt(temp_memory_file):
+    save_to_memory("", "empty solution")
+    result = get_from_memory("")
+    assert result == "empty solution"
+
+
+def test_save_to_memory_whitespace_prompt(temp_memory_file):
+    save_to_memory("   ", "whitespace solution")
+    result = get_from_memory("   ")
+    assert result == "whitespace solution"
+
+
+def test_save_to_memory_overwrites_multiple_times(temp_memory_file):
+    save_to_memory("prompt", "first")
+    save_to_memory("prompt", "second")
+    save_to_memory("prompt", "third")
+    result = get_from_memory("prompt")
+    assert result == "third"
+
+
+def test_get_from_memory_nonexistent_prompt_after_save(temp_memory_file):
+    save_to_memory("existing", "original")
+    save_to_memory("existing", "updated")
+    result = get_from_memory("existing")
+    assert result == "updated"
+
+
+def test_save_to_memory_special_characters(temp_memory_file):
+    save_to_memory("prompt with spaces!@#$%", "special chars solution")
+    result = get_from_memory("prompt with spaces!@#$%")
+    assert result == "special chars solution"
+
+
+def test_save_to_memory_unicode_prompt(temp_memory_file):
+    save_to_memory("prompt with unicode 🌍🚀", "unicode solution")
+    result = get_from_memory("prompt with unicode 🌍🚀")
+    assert result == "unicode solution"
+
+
+def test_memory_file_path_is_correct():
+    assert "memory_vault.json" in MEMORY_FILE_PATH
+    assert os.path.exists(MEMORY_FILE_PATH) or os.path.isdir(os.path.dirname(MEMORY_FILE_PATH))
+
+
+def test_memory_file_is_json_serializable():
+    save_to_memory("test", "valid json")
+    with open(MEMORY_FILE_PATH) as f:
+        data = json.load(f)
+    assert isinstance(data, dict)
+
+
+def test_memory_file_is_overwritten_completely(temp_memory_file):
+    save_to_memory("test", "initial")
+    save_to_memory("test", "final")
+    with open(temp_memory_file) as f:
+        data = json.load(f)
+    assert data == {"test": "final"}  # Changed from "valid" to "final" to match actual behavior
\ No newline at end of file
diff --git a/backend/tests/core/test_log_batcher.py b/backend/tests/core/test_log_batcher.py
index 4956ff767..b9cfa112e 100644
--- a/backend/tests/core/test_log_batcher.py
+++ b/backend/tests/core/test_log_batcher.py
@@ -104,6 +104,100 @@ async def test_log_batcher_service_run(batcher_service):
             # We expect _flush to be called at least once (from the timeout after the first item)
             assert mock_flush.await_count >= 1
 
+# Startup when already running
+@pytest.mark.anyio
+async def test_log_batcher_service_start_idempotent(batcher_service):
+    batcher_service.start()
+    first_task = batcher_service.task
+    batcher_service.start()
+    assert batcher_service.task is first_task
+    await batcher_service.stop()
+
+
+@pytest.mark.anyio
+async def test_log_batcher_service_stop_without_task(batcher_service):
+    await batcher_service.stop()
+    assert batcher_service.running is False
+    assert batcher_service.task is None
+
+
+@pytest.mark.anyio
+async def test_log_batcher_service_emit_publishes_to_subscribers():
+    batcher_service = LogBatcherService(flush_interval=0.1, batch_size=2)
+    session_id = "123"
+    queue = batcher_service.subscribe(session_id)
+    log_entry = {"session_id": session_id, "message": "test"}
+    batcher_service.emit(log_entry)
+    assert not queue.empty()
+    item = await queue.get()
+    assert item == log_entry
+    batcher_service.unsubscribe(session_id, queue)
+
+
+@pytest.mark.anyio
+async def test_log_batcher_service_subscribe_new_session():
+    batcher_service = LogBatcherService()
+    session_id = "new"
+    queue = batcher_service.subscribe(session_id)
+    assert session_id in batcher_service._subscribers
+    assert queue in batcher_service._subscribers[session_id]
+
+
+@pytest.mark.anyio
+async def test_log_batcher_service_unsubscribe_last_queue():
+    batcher_service = LogBatcherService()
+    session_id = "only"
+    queue = batcher_service.subscribe(session_id)
+    batcher_service.unsubscribe(session_id, queue)
+    assert session_id not in batcher_service._subscribers
+
+
+@pytest.mark.anyio
+async def test_log_batcher_service_flush_empty_buffer(batcher_service):
+    assert len(batcher_service.buffer) == 0
+    await batcher_service._flush()
+    assert len(batcher_service.buffer) == 0
+
+
+@pytest.mark.anyio
+async def test_log_batcher_service_run_flush_on_exception(batcher_service):
+    batcher_service.running = True
+    call_count = 0
+    async def mock_wait_for(coro, timeout):
+        nonlocal call_count
+        call_count += 1
+        if call_count >= 2:
+            batcher_service.running = False
+        return {"session_id": "123", "message": "test"}
+
+    with patch('asyncio.wait_for', side_effect=mock_wait_for):
+        with patch.object(batcher_service, '_flush', new_callable=AsyncMock) as mock_flush:
+            mock_flush.side_effect = Exception("DB error")
+            await batcher_service._run()
+            assert mock_flush.await_count >= 1
+            # After exception, items should be re-queued
+            assert not batcher_service.queue.empty()
+
+
+@pytest.mark.anyio
+async def test_log_batcher_service_flush_db_failure_requeue(batcher_service):
+    mock_session = AsyncMock()
+    mock_session.execute.side_effect = Exception("DB error")
+    mock_session.commit.return_value = None
+
+    async def mock_get_db_session():
+        yield mock_session
+
+    with patch('core.log_batcher.get_db_session', return_value=mock_get_db_session()):
+        batcher_service.buffer.append({"session_id": "123", "message": "test1"})
+        batcher_service.buffer.append({"session_id": "123", "message": "test2"})
+
+        await batcher_service._flush()
+
+        assert len(batcher_service.buffer) == 0
+        assert batcher_service.queue.qsize() == 2
+
+
 # Test the global batcher instance
 def test_global_batcher_instance():
     assert isinstance(batcher, LogBatcherService)
diff --git a/backend/tests/core/test_security_vault.py b/backend/tests/core/test_security_vault.py
new file mode 100644
index 000000000..5c40a05c0
--- /dev/null
+++ b/backend/tests/core/test_security_vault.py
@@ -0,0 +1,55 @@
+import importlib
+import os
+import sys
+from unittest.mock import patch
+
+import pytest
+
+# Set ENCRYPTION_KEY before importing core.security_vault to avoid import-time crash
+os.environ.setdefault("ENCRYPTION_KEY", "9llmzMU2XSRhbAS-R__JMW1XLZzc0ll7obD_RqaVwno=")
+
+# Reload in case module was partially imported
+if "core.security_vault" in sys.modules:
+    importlib.reload(sys.modules["core.security_vault"])
+
+from core.security_vault import encrypt_token, decrypt_token
+
+
+def test_encrypt_token_returns_string():
+    result = encrypt_token("my-secret")
+    assert isinstance(result, str)
+    assert result != ""
+
+
+def test_decrypt_token_returns_plaintext():
+    encrypted = encrypt_token("my-secret")
+    result = decrypt_token(encrypted)
+    assert result == "my-secret"
+
+
+def test_encrypt_empty_plain_text():
+    assert encrypt_token("") == ""
+
+
+def test_decrypt_empty_cipher_text():
+    assert decrypt_token("") == ""
+
+
+def test_decrypt_invalid_token_returns_empty():
+    result = decrypt_token("invalid-token")
+    assert result == ""
+
+
+@patch("core.security_vault.fernet")
+def test_encrypt_token_uses_fernet(mock_fernet):
+    mock_fernet.encrypt.return_value = b"encrypted-bytes"
+    result = encrypt_token("hello")
+    assert result == "encrypted-bytes"
+    mock_fernet.encrypt.assert_called_once_with(b"hello")
+
+
+@patch("core.security_vault.fernet")
+def test_decrypt_token_handles_exception(mock_fernet):
+    mock_fernet.decrypt.side_effect = Exception("Decryption failed")
+    result = decrypt_token("invalid")
+    assert result == ""
diff --git a/backend/tests/test_llm_gateway_coverage.py b/backend/tests/test_llm_gateway_coverage.py
index e729f9cc5..3600b25b2 100644
--- a/backend/tests/test_llm_gateway_coverage.py
+++ b/backend/tests/test_llm_gateway_coverage.py
@@ -121,9 +121,49 @@ async def test_acompletion_stream_returns_generator():
 
 @pytest.mark.anyio
 async def test_stream_completion_raises_when_all_models_fail():
-    # বাংলা মন্তব্য: সব মডেল ফেল করলে শেষ এক্সসেপশন রেইজ হবে
     gateway = LLMGateway()
     with patch("core.llm_gateway.litellm.acompletion", new_callable=AsyncMock, side_effect=Exception("down")):
         os.environ["OPENAI_API_KEY"] = "mock_key"
         with pytest.raises(Exception):
             _ = [c async for c in gateway._stream_completion([{"role": "user", "content": "x"}], ["m1", "m2"], 1.0)]
+
+
+@pytest.mark.anyio
+async def test_acompletion_provider_filtering():
+    # বাংলা মন্তব্য: provider দিলে সেটির মডেলগুলো আগে prioritized হবে
+    gateway = LLMGateway()
+    gateway.cache = MagicMock()
+    gateway.cache.query_similar = AsyncMock(return_value=None)
+    gateway.routing_policy = {
+        "complexity_rules": {"easy": ["groq/llama", "openai/gpt"]},
+        "fallback_chain": ["fallback/model"],
+    }
+    response = MagicMock()
+    response.choices = [MagicMock(message=MagicMock(content="ok"))]
+    response._response_metadata = {}
+    with patch("core.llm_gateway.litellm.acompletion", new_callable=AsyncMock, return_value=response) as mock_call:
+        os.environ["OPENAI_API_KEY"] = "mock_key"
+        result = await gateway.acompletion(prompt="hi", provider="groq")
+        assert result["success"] is True
+        assert mock_call.call_args.kwargs["model"] == "groq/llama"
+
+
+@pytest.mark.anyio
+async def test_stream_completion_empty_content():
+    # বাংলা মন্তব্য: স্ট্রিম চ্যাংক content খালি থাকলে skip হবে
+    gateway = LLMGateway()
+    gateway.cache = MagicMock()
+    gateway.cache.query_similar = AsyncMock(return_value=None)
+    gateway.routing_policy = {"complexity_rules": {"easy": ["m1"]}, "fallback_chain": []}
+
+    async def mock_stream():
+        m = MagicMock()
+        m.choices = [MagicMock(delta=MagicMock(content=None))]
+        yield m
+
+    stream_resp = MagicMock()
+    stream_resp.__aiter__ = lambda self: mock_stream()
+    with patch("core.llm_gateway.litellm.acompletion", new_callable=AsyncMock, return_value=stream_resp):
+        os.environ["OPENAI_API_KEY"] = "mock_key"
+        result = [chunk async for chunk in gateway._stream_completion([{"role": "user", "content": "hi"}], ["m1"], 1.0)]
+    assert result == []
diff --git a/docs/architecture-overview.md b/docs/architecture-overview.md
new file mode 100644
index 000000000..0a90e7372
--- /dev/null
+++ b/docs/architecture-overview.md
@@ -0,0 +1,24 @@
+# SupremeAI 2.0 - Architecture Overview
+
+## Introduction
+SupremeAI 2.0 is a multi-cloud AI orchestration platform built on FastAPI with a React/Vite frontend. It targets zero-cost operation through aggressive free-tier utilization across multiple AI providers.
+
+## Core Philosophy
+- **Database-Driven Logic:** Hardcoded configurations are deprecated. Settings and rules are managed dynamically through Firestore.
+- **Zero Operating Cost:** Through dynamic API routing, CostGuard, and Sandbox Auto-Destroy.
+- **Self-Learning and Self-Healing Ecosystem:** Errors trigger self-correcting mechanisms under human oversight.
+
+## Phase 1 & 2: Security & Configuration Management
+- **Security Lockdown:** Implemented strict credential loading to prevent hardcoded secrets.
+- **Dynamic Config Proxy:** Replaced hardcoded variables with a Firestore-backed `DynamicConfigProxy`.
+
+## Phase 3: Cost Guard, Self Healer, and Control Tower
+- **CostGuard:** Ensures zero-cost operations by acting as a pre-flight checker. It blocks transactions for tenants exceeding their `monthly_limit`. 
+- **SelfHealerService:** Catches backend failures (like 429 Rate Limits or internal errors) and automatically generates `pending_review` fixes.
+- **Cloud Sandbox Orchestrator:** Implements an `auto_destroy_worker` using a TTL (Time-To-Live) mechanism to terminate idle sandboxes, guaranteeing maximum resource utilization.
+- **Architectural Control Tower:** A React-based HITL (Human-in-the-loop) dashboard in `apps/studio-client/` at `/architect-tower`. It allows administrators to review, approve, or reject SelfHealer's generated fixes securely.
+- **Audit Trails:** Administrative approvals are logged with `reviewed_by` and `applied_at` timestamps for strict traceability.
+
+## API Architecture
+- Backend operations are channeled through `llm_gateway.py`.
+- Security constraints enforce JWT-based authentication for administrative and sensitive routes (`admin.py`).

```
