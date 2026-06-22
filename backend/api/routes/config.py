from fastapi import APIRouter, HTTPException
from typing import Optional
from database.supabase_client import db

router = APIRouter(prefix="/config", tags=["config"])

@router.get("/{key}")
async def get_config(key: str):
    if not db.client:
        raise HTTPException(status_code=503, detail="Database not configured")
    res = db.client.table("system_config").select("*").eq("key", key).execute()
    if not res.data:
        raise HTTPException(status_code=404, detail="Config not found")
    return res.data[0]

@router.put("/{key}")
async def update_config(key: str, value: dict, category: Optional[str] = None, description: Optional[str] = None):
    # TODO: Add admin dependency here
    if not db.client:
        raise HTTPException(status_code=503, detail="Database not configured")
        
    data = {"key": key, "value": value}
    if category: data["category"] = category
    if description: data["description"] = description
        
    res = db.client.table("system_config").upsert(data).execute()
    return {"status": "success", "config": res.data[0] if res.data else None}

@router.get("/category/{category}")
async def get_configs_by_category(category: str):
    if not db.client:
        raise HTTPException(status_code=503, detail="Database not configured")
    res = db.client.table("system_config").select("*").eq("category", category).execute()
    return {"items": res.data or [], "total": len(res.data or [])}
