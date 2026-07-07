# 📋 Commit b40e5e1eac9e1e1b50b3b1b771691793a41eefec

## Commit Stats
```
commit b40e5e1eac9e1e1b50b3b1b771691793a41eefec
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Tue Jul 7 23:17:23 2026 +0600

    feat: implement CostGuard service with tests for Phase 3 cost control

 backend/core/cost_guard.py            | 43 ++++++++++++++++++++++
 backend/tests/core/test_cost_guard.py | 68 +++++++++++++++++++++++++++++++++++
 2 files changed, 111 insertions(+)

```

## Diff Detail
```diff
commit b40e5e1eac9e1e1b50b3b1b771691793a41eefec
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Tue Jul 7 23:17:23 2026 +0600

    feat: implement CostGuard service with tests for Phase 3 cost control

diff --git a/backend/core/cost_guard.py b/backend/core/cost_guard.py
new file mode 100644
index 000000000..18c8d17a8
--- /dev/null
+++ b/backend/core/cost_guard.py
@@ -0,0 +1,43 @@
+from typing import Any
+from fastapi import HTTPException
+from loguru import logger
+
+class CostGuard:
+    def __init__(self, db: Any):
+        self._db = db
+
+    async def check_budget(self, tenant_id: str, estimated_cost: float) -> bool:
+        """
+        Pre-flight Check:
+        Check if the tenant has enough budget for the estimated cost.
+        Raises HTTPException 402 if budget exceeded.
+        """
+        try:
+            doc_ref = self._db.collection(f"tenants/{tenant_id}/budget").document("status")
+            
+            import asyncio
+            if asyncio.iscoroutinefunction(doc_ref.get):
+                snapshot = await doc_ref.get()
+            else:
+                snapshot = doc_ref.get()
+                
+            if not snapshot.exists:
+                # If no budget info found, we might want to default to a free tier or reject.
+                # Assuming safe rejection or default limit. Let's raise an error for strict mode.
+                raise HTTPException(status_code=402, detail="Payment Required: No budget configured.")
+                
+            data = snapshot.to_dict()
+            monthly_limit = float(data.get("monthly_limit", 0.0))
+            spent_amount = float(data.get("spent_amount", 0.0))
+            
+            if spent_amount + estimated_cost > monthly_limit:
+                logger.warning(f"Tenant {tenant_id} exceeded budget. Spent: {spent_amount}, Limit: {monthly_limit}, Estimated: {estimated_cost}")
+                raise HTTPException(status_code=402, detail="Payment Required: Budget Exceeded")
+                
+            return True
+        except HTTPException:
+            raise
+        except Exception as e:
+            logger.error(f"CostGuard DB Error: {e}")
+            # Failsafe: if DB is down, maybe reject or accept? Zero-Gap means strict.
+            raise RuntimeError(f"CostGuard failed to verify budget: {e}")
diff --git a/backend/tests/core/test_cost_guard.py b/backend/tests/core/test_cost_guard.py
new file mode 100644
index 000000000..f17593b64
--- /dev/null
+++ b/backend/tests/core/test_cost_guard.py
@@ -0,0 +1,68 @@
+import pytest
+from unittest.mock import MagicMock
+from fastapi import HTTPException
+from core.cost_guard import CostGuard
+
+@pytest.fixture
+def mock_db():
+    return MagicMock()
+
+@pytest.mark.asyncio
+async def test_cost_guard_allows_when_under_budget(mock_db):
+    doc_ref = MagicMock()
+    snapshot = MagicMock()
+    snapshot.exists = True
+    snapshot.to_dict.return_value = {
+        "monthly_limit": 10.0,
+        "spent_amount": 5.0
+    }
+    doc_ref.get.return_value = snapshot
+    mock_db.collection.return_value.document.return_value = doc_ref
+    
+    guard = CostGuard(mock_db)
+    result = await guard.check_budget("tenant-1", 1.0)
+    assert result is True
+
+@pytest.mark.asyncio
+async def test_cost_guard_blocks_when_over_budget(mock_db):
+    doc_ref = MagicMock()
+    snapshot = MagicMock()
+    snapshot.exists = True
+    snapshot.to_dict.return_value = {
+        "monthly_limit": 10.0,
+        "spent_amount": 9.5
+    }
+    doc_ref.get.return_value = snapshot
+    mock_db.collection.return_value.document.return_value = doc_ref
+    
+    guard = CostGuard(mock_db)
+    with pytest.raises(HTTPException) as exc:
+        await guard.check_budget("tenant-1", 1.0)
+    
+    assert exc.value.status_code == 402
+    assert "Budget Exceeded" in exc.value.detail
+
+@pytest.mark.asyncio
+async def test_cost_guard_blocks_when_no_budget_doc(mock_db):
+    doc_ref = MagicMock()
+    snapshot = MagicMock()
+    snapshot.exists = False
+    doc_ref.get.return_value = snapshot
+    mock_db.collection.return_value.document.return_value = doc_ref
+    
+    guard = CostGuard(mock_db)
+    with pytest.raises(HTTPException) as exc:
+        await guard.check_budget("tenant-1", 1.0)
+    
+    assert exc.value.status_code == 402
+    assert "No budget configured" in exc.value.detail
+
+@pytest.mark.asyncio
+async def test_cost_guard_raises_runtime_error_on_db_failure(mock_db):
+    doc_ref = MagicMock()
+    doc_ref.get.side_effect = Exception("Firestore Offline")
+    mock_db.collection.return_value.document.return_value = doc_ref
+    
+    guard = CostGuard(mock_db)
+    with pytest.raises(RuntimeError, match="CostGuard failed to verify budget: Firestore Offline"):
+        await guard.check_budget("tenant-1", 1.0)

```
