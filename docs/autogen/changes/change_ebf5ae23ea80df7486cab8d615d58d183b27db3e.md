# 📋 Commit ebf5ae23ea80df7486cab8d615d58d183b27db3e

## Commit Stats
```
commit ebf5ae23ea80df7486cab8d615d58d183b27db3e
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Tue Jul 7 23:22:59 2026 +0600

    feat: integrate CostGuard and SelfHealerService into LLMGateway and Sandbox Orchestrator for Phase 3

 backend/core/cloud_sandbox_orchestrator.py    | 56 ++++++++++++++++++++++++++-
 backend/core/llm_gateway.py                   | 29 +++++++++++++-
 backend/tests/core/test_integration_phase3.py | 52 +++++++++++++++++++++++++
 3 files changed, 134 insertions(+), 3 deletions(-)

```

## Diff Detail
```diff
commit ebf5ae23ea80df7486cab8d615d58d183b27db3e
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Tue Jul 7 23:22:59 2026 +0600

    feat: integrate CostGuard and SelfHealerService into LLMGateway and Sandbox Orchestrator for Phase 3

diff --git a/backend/core/cloud_sandbox_orchestrator.py b/backend/core/cloud_sandbox_orchestrator.py
index 14cc1cdbd..fe12259d6 100644
--- a/backend/core/cloud_sandbox_orchestrator.py
+++ b/backend/core/cloud_sandbox_orchestrator.py
@@ -14,6 +14,11 @@ from typing import Any
 
 import httpx
 from loguru import logger
+import datetime
+
+from utils.firestore_helpers import get_firestore_db
+from core.self_healer import SelfHealerService
+from core.config_proxy import DynamicConfigProxy
 
 
 class CloudSandboxOrchestrator:
@@ -35,6 +40,7 @@ class CloudSandboxOrchestrator:
             headers=headers,
             timeout=60.0,
         )
+        self._active_sandboxes = {}
         logger.info(f"Initialized CloudSandboxOrchestrator (Provider: {self.provider})")
 
     def _get_base_url(self) -> str:
@@ -48,8 +54,10 @@ class CloudSandboxOrchestrator:
     async def create_sandbox(self, spec: dict[str, Any]) -> dict[str, Any] | None:
         if not self.api_key:
             logger.warning("Cannot create sandbox: API key is missing. Running in mock/dry-run mode.")
+            mock_id = f"mock-sandbox-id-{os.urandom(4).hex()}"
+            self._active_sandboxes[mock_id] = {"created_at": datetime.datetime.now(datetime.timezone.utc), "status": "running"}
             return {
-                "id": "mock-sandbox-id-12345",
+                "id": mock_id,
                 "status": "running",
                 "provider": self.provider,
                 "mock": True
@@ -63,7 +71,10 @@ class CloudSandboxOrchestrator:
             response = await self.client.post(endpoint, json=payload)
             response.raise_for_status()
             data = response.json()
-            logger.success(f"Successfully created sandbox with ID: {data.get('id')}")
+            sandbox_id = data.get('id')
+            if sandbox_id:
+                self._active_sandboxes[sandbox_id] = {"created_at": datetime.datetime.now(datetime.timezone.utc), "status": "running"}
+            logger.success(f"Successfully created sandbox with ID: {sandbox_id}")
             return data
         except httpx.HTTPStatusError as e:
             logger.error(f"Failed to create sandbox. Status: {e.response.status_code}, Body: {e.response.text}")
@@ -125,11 +136,52 @@ class CloudSandboxOrchestrator:
             response = await self.client.post(endpoint)
             response.raise_for_status()
             logger.success(f"Sandbox {sandbox_id} destroyed successfully.")
+            if sandbox_id in self._active_sandboxes:
+                del self._active_sandboxes[sandbox_id]
             return True
         except httpx.HTTPStatusError as e:
             logger.error(f"Failed to destroy sandbox {sandbox_id}. Status: {e.response.status_code}")
         return False
 
+    async def auto_destroy_worker(self, tenant_id: str):
+        """
+        Background worker that checks TTL and terminates idle/crashed sandboxes.
+        Integrates with SelfHealer to log errors if termination is due to a crash or timeout.
+        """
+        logger.info("Started Sandbox Auto-Destroy Worker")
+        db = get_firestore_db()
+        config_proxy = DynamicConfigProxy(tenant_id, db) if db else None
+        
+        while True:
+            try:
+                # Default 10 minutes TTL
+                ttl_minutes = await config_proxy.get("SANDBOX_TTL_MINUTES", 10) if config_proxy else 10
+                ttl_delta = datetime.timedelta(minutes=ttl_minutes)
+                now = datetime.datetime.now(datetime.timezone.utc)
+                
+                for sandbox_id, data in list(self._active_sandboxes.items()):
+                    created_at = data.get("created_at")
+                    if created_at and (now - created_at) > ttl_delta:
+                        logger.warning(f"Sandbox {sandbox_id} exceeded TTL of {ttl_minutes}m. Terminating...")
+                        
+                        # If we assume it timed out or crashed, notify SelfHealer
+                        if db:
+                            healer = SelfHealerService(db)
+                            await healer.propose_fix(
+                                tenant_id=tenant_id,
+                                error_pattern=f"SandboxTimeout: Sandbox {sandbox_id} was active for > {ttl_minutes}m",
+                                proposed_fix=f"# Recommend analyzing sandbox logs or increasing TTL for task.",
+                                impact_score=0.3,
+                                dependency_tree=["core.cloud_sandbox_orchestrator"]
+                            )
+                        
+                        await self.destroy_sandbox(sandbox_id)
+                        
+                await asyncio.sleep(60) # Check every minute
+            except Exception as e:
+                logger.error(f"Auto-Destroy Worker encountered an error: {e}")
+                await asyncio.sleep(60)
+
     # ------------------------------------------------------------------------
     # 🤖 FREEBUFF AI WORKER INTEGRATION
     # ------------------------------------------------------------------------
diff --git a/backend/core/llm_gateway.py b/backend/core/llm_gateway.py
index 67f392620..c7864d4a9 100644
--- a/backend/core/llm_gateway.py
+++ b/backend/core/llm_gateway.py
@@ -9,6 +9,10 @@ from typing import Any
 import litellm
 from loguru import logger
 
+from utils.firestore_helpers import get_firestore_db
+from core.cost_guard import CostGuard
+from core.self_healer import SelfHealerService
+
 from core.config import settings
 from core.prompt_handler import normalize_prompt
 
@@ -98,6 +102,7 @@ class LLMGateway:
         timeout: float = 12.0,
         model: str | None = None,
         provider: str | None = None,
+        tenant_id: str | None = None,
         **kwargs,
     ) -> Any:
         """
@@ -131,6 +136,14 @@ class LLMGateway:
                     "cached": True
                 }
 
+        # ── Pre-flight Cost Guard Check ──
+        if tenant_id:
+            db = get_firestore_db()
+            if db:
+                cost_guard = CostGuard(db)
+                # For pre-flight, estimate a fixed cost for simplicity (e.g. 0.01)
+                await cost_guard.check_budget(tenant_id, 0.01)
+
         model_candidates = self.routing_policy.get("complexity_rules", {}).get(difficulty, [])
         fallbacks = self.routing_policy.get("fallback_chain", [])
         
@@ -180,7 +193,21 @@ class LLMGateway:
                 logger.warning(f"Model {model} failed in chain. Exception: {e}")
                 continue
 
-        raise last_exception or RuntimeError("All routing models failed to produce a completion.")
+        # ── Trigger Self Healer on Failure ──
+        final_exception = last_exception or RuntimeError("All routing models failed to produce a completion.")
+        if tenant_id:
+            db = get_firestore_db()
+            if db:
+                healer = SelfHealerService(db)
+                error_msg = str(final_exception)
+                await healer.propose_fix(
+                    tenant_id=tenant_id,
+                    error_pattern=f"LLMGateway Exception: {error_msg[:100]}",
+                    proposed_fix=f"# Recommend checking fallback models or API keys for error:\n# {error_msg}",
+                    impact_score=0.2,
+                    dependency_tree=["core.llm_gateway"]
+                )
+        raise final_exception
 
     async def _stream_completion(self, messages: list[dict[str, str]], call_chain: list[str], timeout: float) -> AsyncGenerator[str, None]:
         # Handle streaming responses with fallback failover support
diff --git a/backend/tests/core/test_integration_phase3.py b/backend/tests/core/test_integration_phase3.py
new file mode 100644
index 000000000..34e93b18c
--- /dev/null
+++ b/backend/tests/core/test_integration_phase3.py
@@ -0,0 +1,52 @@
+import pytest
+import litellm
+from unittest.mock import patch, MagicMock
+from core.llm_gateway import llm_gateway
+from utils.firestore_helpers import get_firestore_db
+
+@pytest.fixture
+def mock_db_integration():
+    db = MagicMock()
+    doc_ref_budget = MagicMock()
+    snapshot_budget = MagicMock()
+    snapshot_budget.exists = True
+    snapshot_budget.to_dict.return_value = {"monthly_limit": 10.0, "spent_amount": 0.0}
+    doc_ref_budget.get.return_value = snapshot_budget
+    
+    doc_ref_fixes = MagicMock()
+    
+    def collection_side_effect(path):
+        col_mock = MagicMock()
+        if "budget" in path:
+            col_mock.document.return_value = doc_ref_budget
+        elif "fixes" in path:
+            col_mock.document.return_value = doc_ref_fixes
+        return col_mock
+        
+    db.collection.side_effect = collection_side_effect
+    return db, doc_ref_fixes
+
+@pytest.mark.asyncio
+@patch("core.llm_gateway.get_firestore_db")
+@patch("litellm.acompletion")
+async def test_llm_gateway_self_healer_integration(mock_acompletion, mock_get_firestore_db, mock_db_integration):
+    db, doc_ref_fixes = mock_db_integration
+    mock_get_firestore_db.return_value = db
+    
+    # Force acompletion to fail
+    mock_acompletion.side_effect = Exception("LiteLLM RateLimitError")
+    
+    with pytest.raises(Exception, match="LiteLLM RateLimitError"):
+        await llm_gateway.acompletion(
+            prompt="Hello", 
+            model="openai/gpt-3.5-turbo", 
+            tenant_id="tenant-integration"
+        )
+        
+    # Verify SelfHealer was called and pending_review is saved
+    doc_ref_fixes.set.assert_called_once()
+    payload = doc_ref_fixes.set.call_args[0][0]
+    
+    assert payload.get("status") == "pending_review"
+    assert "LiteLLM RateLimitError" in payload.get("error_pattern", "")
+    assert "core.llm_gateway" in payload.get("dependency_tree", [])

```
