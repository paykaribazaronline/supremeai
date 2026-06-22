from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel
from typing import Optional, List
from database.supabase_client import db

router = APIRouter(prefix="/preferences", tags=["preferences"])


class PreferenceUpdate(BaseModel):
    theme: Optional[str] = None
    default_model: Optional[str] = None
    max_tokens: Optional[int] = None
    auto_save: Optional[bool] = None
    custom_shortcuts: Optional[dict] = None
    verbosity: Optional[str] = None
    preferred_frameworks: Optional[List[str]] = None


@router.get("/")
async def get_preferences(user_id: str = Query(default="default")):
    if not db.client:
        return {
            "user_id": user_id,
            "theme": "dark",
            "default_model": "gpt-4o",
            "max_tokens": 4096,
            "auto_save": True,
            "custom_shortcuts": {},
        }
    try:
        res = db.client.table("user_preferences").select("*").eq("user_id", user_id).execute()
        rows = res.data or []
        if rows:
            return rows[0]
        return {
            "user_id": user_id,
            "theme": "dark",
            "default_model": "gpt-4o",
            "max_tokens": 4096,
            "auto_save": True,
            "custom_shortcuts": {},
        }
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))


@router.post("/")
async def upsert_preferences(user_id: str = Query(default="default"), payload: PreferenceUpdate = ...):
    if not db.client:
        return {"status": "success", "preferences": payload.dict(exclude_none=True)}
    data = payload.dict(exclude_none=True)
    if not data:
        raise HTTPException(status_code=400, detail="No fields to update")
    data["user_id"] = user_id
    try:
        res = db.client.table("user_preferences").upsert(data).execute()
        return {"status": "success", "preferences": res.data[0] if res.data else data}
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))
