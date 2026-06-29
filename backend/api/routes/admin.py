from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from admin.god import AdminGodLayer # Your existing god.py

router = APIRouter(prefix="/api/admin", tags=["Admin Control Center"])
god_layer = AdminGodLayer(db_path="data/admin_rules.db")

class RuleUpdate(BaseModel):
    key: str
    value: str

@router.post("/rules")
async def update_constitutional_rule(payload: RuleUpdate):
    """Update God.py constitutional rules directly from the Command Center UI"""
    try:
        god_layer.set_rule(payload.key, payload.value)
        return {"status": "success", "message": f"Rule {payload.key} updated to {payload.value}"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

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
