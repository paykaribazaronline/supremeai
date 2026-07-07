# 📄 ফাইল: backend/core/cost_guard.py

**প্রকার:** .py  
**সাইজ:** 1,840 বাইট  
**আপডেট:** 2026-07-07T22:11:19.748843

---

## কোড

```py
from typing import Any

from fastapi import HTTPException
from loguru import logger


class CostGuard:
    def __init__(self, db: Any):
        self._db = db

    async def check_budget(self, tenant_id: str, estimated_cost: float) -> bool:
        """
        Pre-flight Check:
        Check if the tenant has enough budget for the estimated cost.
        Raises HTTPException 402 if budget exceeded.
        """
        try:
            doc_ref = self._db.collection(f"tenants/{tenant_id}/budget").document("status")

            import asyncio
            if asyncio.iscoroutinefunction(doc_ref.get):
                snapshot = await doc_ref.get()
            else:
                snapshot = doc_ref.get()

            if not snapshot.exists:
                # If no budget info found, we might want to default to a free tier or reject.
                # Assuming safe rejection or default limit. Let's raise an error for strict mode.
                raise HTTPException(status_code=402, detail="Payment Required: No budget configured.")

            data = snapshot.to_dict()
            monthly_limit = float(data.get("monthly_limit", 0.0))
            spent_amount = float(data.get("spent_amount", 0.0))

            if spent_amount + estimated_cost > monthly_limit:
                logger.warning(f"Tenant {tenant_id} exceeded budget. Spent: {spent_amount}, Limit: {monthly_limit}, Estimated: {estimated_cost}")
                raise HTTPException(status_code=402, detail="Payment Required: Budget Exceeded")

            return True
        except HTTPException:
            raise
        except Exception as e:
            logger.error(f"CostGuard DB Error: {e}")
            # Failsafe: if DB is down, maybe reject or accept? Zero-Gap means strict.
            raise RuntimeError(f"CostGuard failed to verify budget: {e}") from e

```