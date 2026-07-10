# 📄 ফাইল: backend/api/routes/execution_policies.py

**প্রকার:** .py  
**সাইজ:** 1,293 বাইট  
**আপডেট:** 2026-07-10T19:10:52.055985

---

## কোড

```py
from fastapi import APIRouter
from pydantic import BaseModel


router = APIRouter(prefix="/api/admin/execution-policies", tags=["Guardrails"])


class ExecutionPolicyModel(BaseModel):
    id: str
    scope: str
    target_name: str
    max_timeout_ms: int
    max_compute_usd: float
    max_retries: int
    cb_failure_threshold: int
    cooldown_window_sec: int


# In-memory mock for DB layer built in phase 1 (execution_policy table)
MOCK_POLICIES = [
    {
        "id": "pol_global",
        "scope": "global",
        "target_name": "*",
        "max_timeout_ms": 30000,
        "max_compute_usd": 1.0,
        "max_retries": 3,
        "cb_failure_threshold": 5,
        "cooldown_window_sec": 300,
    },
    {
        "id": "pol_stripe",
        "scope": "platform",
        "target_name": "stripe.com",
        "max_timeout_ms": 15000,
        "max_compute_usd": 0.5,
        "max_retries": 1,
        "cb_failure_threshold": 3,
        "cooldown_window_sec": 600,
    },
]


@router.get("/")
def get_policies():
    return {"items": MOCK_POLICIES}


@router.put("/{policy_id}")
def update_policy(policy_id: str, updates: dict):
    for pol in MOCK_POLICIES:
        if pol["id"] == policy_id:
            pol.update(updates)
            return pol
    return {"error": "not found"}

```