# 📄 ফাইল: backend/core/self_healer.py

**প্রকার:** .py  
**সাইজ:** 3,272 বাইট  
**আপডেট:** 2026-07-08T03:57:12.399613

---

## কোড

```py
import uuid
from datetime import UTC
from datetime import datetime
from typing import Any

from loguru import logger

from core.event_bus import ErrorEvent
from core.event_bus import error_event_bus


class SelfHealerService:
    def __init__(self, db: Any):
        self._db = db

    def _generate_trace_id(self) -> str:
        return f"err-trace-{uuid.uuid4().hex[:12]}"

    def _safety_check(self, proposed_fix: str) -> None:
        """
        Safety Filter: Ensure dangerous commands are not proposed in the fix.
        """
        dangerous_keywords = ["exec(", "eval(", "os.system", "subprocess.call", "__import__"]
        for keyword in dangerous_keywords:
            if keyword in proposed_fix:
                raise ValueError(f"Dangerous keyword '{keyword}' detected in proposed fix. Rejected by Safety Filter.")

    async def propose_fix(
        self,
        tenant_id: str,
        error_pattern: str,
        proposed_fix: str,
        impact_score: float,
        dependency_tree: list[str]
    ) -> str:
        """
        Generates and stores an automatic fix for an error in the Firestore database
        with a 'pending_review' status for Human-in-the-Loop (HITL) approval.
        """
        self._safety_check(proposed_fix)

        # Ensure impact score is valid
        if not (0.0 <= impact_score <= 1.0):
            raise ValueError("Impact score must be between 0.0 and 1.0")

        trace_id = self._generate_trace_id()
        fix_id = f"fix-{uuid.uuid4().hex[:8]}"

        doc_ref = self._db.collection(f"tenants/{tenant_id}/fixes").document(fix_id)

        fix_data = {
            "trace_id": trace_id,
            "timestamp": datetime.now(UTC).isoformat(),
            "error_pattern": error_pattern,
            "proposed_fix": proposed_fix,
            "impact_score": impact_score,
            "dependency_tree": dependency_tree,
            "status": "pending_review",
            "reviewed_by": None,
            "applied_at": None
        }

        import asyncio
        if asyncio.iscoroutinefunction(doc_ref.set):
            await doc_ref.set(fix_data)
        else:
            doc_ref.set(fix_data)

        logger.info(f"Generated auto-fix {fix_id} for trace {trace_id} (Status: pending_review)")
        return fix_id

    async def test_fix_in_sandbox(self, fix_id: str, tenant_id: str) -> bool:
        """
        Tests the proposed fix in an isolated sandbox environment.
        (Placeholder for actual sandbox testing logic)
        """
        logger.info(f"Testing fix {fix_id} in sandbox environment for tenant {tenant_id}")
        # Here we would normally use the cloud_sandbox_orchestrator
        # For now, return True as a placeholder
        return True

async def _self_healer_error_listener(event: ErrorEvent):
    """
    Listens to the centralized error event bus.
    If an error meets the criteria, it can trigger the self healer's propose_fix logic.
    """
    logger.info(f"SelfHealer triggered by event from {event.module}: {event.error_type}")
    # In a full implementation, this would instantiate SelfHealerService and call propose_fix
    # based on the severity and context of the event.

# Register the listener
error_event_bus.register_listener(_self_healer_error_listener)

```