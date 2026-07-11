# 📄 ফাইল: backend/api/routes/admin.py

**প্রকার:** .py  
**সাইজ:** 4,437 বাইট  
**আপডেট:** 2026-07-11T14:41:19.335771

---

## কোড

```py
from datetime import UTC
from datetime import datetime
from pathlib import Path

from fastapi import APIRouter
from fastapi import Depends
from fastapi import HTTPException
from loguru import logger
from pydantic import BaseModel

from admin.god import AdminGodLayer  # Your existing god.py
from api.dependencies import get_current_user_token
from core.self_healer import SelfHealerService
from utils.firestore_helpers import get_firestore_db


router = APIRouter(prefix="/api/admin", tags=["Admin Control Center"])
_db_path = str(Path(__file__).resolve().parent.parent.parent / "data" / "admin_rules.db")
god_layer = AdminGodLayer(db_path=_db_path)


def get_current_admin(payload: dict = Depends(get_current_user_token)) -> dict:
    if payload.get("role") != "admin":
        logger.warning(f"Unauthorized admin access attempt by {payload.get('sub')}")
        raise HTTPException(status_code=403, detail="Admin access required")
    return payload


def get_healer_service() -> SelfHealerService:
    db = get_firestore_db()
    if not db:
        raise HTTPException(status_code=503, detail="Database unavailable")
    return SelfHealerService(db)


class RuleUpdate(BaseModel):
    key: str
    value: str


@router.post("/rules")
async def update_constitutional_rule(payload: RuleUpdate):
    """Update God.py constitutional rules directly from the Command Center UI"""
    try:
        god_layer.set_rule(payload.key, payload.value)
        return {"status": "success", "message": f"Rule {payload.key} updated to {payload.value}"}
    except Exception as e:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=str(e)) from e


@router.post("/actions/{action_type}")
async def trigger_quick_action(action_type: str):
    """Trigger 1-click Quick Actions from Dashboard"""
    # Verify if admin actions are currently allowed by god.py
    god_layer.enforce("admin_action")

    if action_type == "rollback":
        # Add rollback logic here
        return {"status": "Rollback initiated"}
    elif action_type == "backup":
        # Add backup trigger here
        return {"status": "Backup triggered"}
    elif action_type == "cache":
        # Add redis flush logic here
        return {"status": "Redis cache cleared"}
    else:
        raise HTTPException(status_code=404, detail="Action not found")


@router.get("/fixes")
async def get_fixes(
    tenant_id: str = "default",
    status: str = "pending_review",
    admin_user: dict = Depends(get_current_admin),
    healer: SelfHealerService = Depends(get_healer_service),
):
    """Fetch all fixes for a tenant with a specific status."""
    db = get_firestore_db()
    fixes_ref = db.collection("tenants").document(tenant_id).collection("fixes")
    query = fixes_ref.where("status", "==", status)

    try:
        results = await query.get()
    except TypeError:
        # Fallback for sync mock
        results = query.get()

    fixes = []
    for doc in results:
        fix_data = doc.to_dict()
        fix_data["id"] = doc.id
        fixes.append(fix_data)

    return {"fixes": fixes}


@router.post("/fixes/{fix_id}/approve")
async def approve_fix(
    fix_id: str, tenant_id: str = "default", admin_user: dict = Depends(get_current_admin), healer: SelfHealerService = Depends(get_healer_service)
):
    """Approve a pending fix."""
    admin_id = admin_user.get("sub", "unknown_admin")
    logger.info(f"Admin {admin_id} approving fix {fix_id} for tenant {tenant_id}")

    success = await healer.apply_fix(tenant_id, fix_id, admin_id)
    if not success:
        raise HTTPException(status_code=400, detail="Failed to apply fix. It may not exist or is already processed.")

    return {"status": "success", "fix_id": fix_id}


@router.post("/fixes/{fix_id}/reject")
async def reject_fix(fix_id: str, tenant_id: str = "default", admin_user: dict = Depends(get_current_admin)):
    """Reject a pending fix."""
    admin_id = admin_user.get("sub", "unknown_admin")
    logger.info(f"Admin {admin_id} rejecting fix {fix_id} for tenant {tenant_id}")

    db = get_firestore_db()
    doc_ref = db.collection("tenants").document(tenant_id).collection("fixes").document(fix_id)

    update_data = {"status": "rejected", "reviewed_by": admin_id, "applied_at": datetime.now(UTC).isoformat()}

    try:
        await doc_ref.update(update_data)
    except TypeError:
        doc_ref.update(update_data)

    return {"status": "success", "fix_id": fix_id}

```