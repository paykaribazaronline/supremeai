from fastapi import APIRouter
from fastapi import HTTPException
from fastapi import Query
from pydantic import BaseModel

from database.supabase_client import db


router = APIRouter(prefix="/preferences", tags=["preferences"])


class PreferenceUpdate(BaseModel):
    theme: str | None = None
    default_model: str | None = None
    max_tokens: int | None = None
    auto_save: bool | None = None
    custom_shortcuts: dict | None = None
    verbosity: str | None = None
    preferred_frameworks: list[str] | None = None


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
        res = (
            db.client.table("user_preferences")
            .select("*")
            .eq("user_id", user_id)
            .execute()
        )
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
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@router.post("/")
async def upsert_preferences(
    user_id: str = Query(default="default"), payload: PreferenceUpdate = ...
):
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
        raise HTTPException(status_code=500, detail=str(exc)) from exc
