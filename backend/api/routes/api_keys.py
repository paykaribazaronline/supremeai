"""
API Key Management Routes
"""
from __future__ import annotations

import time

from fastapi import APIRouter
from fastapi import HTTPException
from fastapi import Request
from fastapi import status
from loguru import logger
from pydantic import BaseModel
from pydantic import Field
from pydantic import field_validator

from core.api_key_rate_limiter import APIKeyRateLimiter
from core.security import generate_api_key
from core.security import hash_api_key
from core.security import mask_api_key
from core.security import verify_api_key
from models.api_key import create_api_key
from models.api_key import delete_api_key
from models.api_key import get_all_api_keys
from models.api_key import get_api_key_by_id
from models.api_key import get_api_key_usage
from models.api_key import get_api_key_usage_stats
from models.api_key import get_api_keys_by_user
from models.api_key import record_api_key_event
from models.api_key import record_api_key_usage
from models.api_key import revoke_api_key
from models.api_key import rotate_api_key


router = APIRouter(prefix="/api/api-keys", tags=["api-keys"])
limiter = APIKeyRateLimiter()
KEY_USAGE_LIMIT = 100
ALERT_RPM_THRESHOLD = 50
BULK_DELETE_LIMIT = 50


class CreateAPIKeyRequest(BaseModel):
    user_id: str = Field(..., min_length=1, description="Owner user ID (email or uid)")
    name: str = Field(..., min_length=1, max_length=255)
    rate_limit_rps: int = Field(default=6, ge=1, le=1000)
    expires_in_days: int | None = Field(default=None, ge=1, description="Expires in N days, null = no expiry")

    @field_validator("user_id", "name", mode="before")
    @classmethod
    def strip(cls, v):
        return v.strip() if isinstance(v, str) else v


class UpdateAPIKeyRequest(BaseModel):
    name: str | None = Field(default=None, max_length=255)
    rate_limit_rps: int | None = Field(default=None, ge=1, le=1000)


class RotateAPIKeyRequest(BaseModel):
    old_key: str = Field(..., min_length=1)
    grace_period_hours: int = Field(default=24, ge=0, le=168)


class BulkDeleteRequest(BaseModel):
    key_ids: list[int] = Field(..., min_length=1, max_length=50)


def _get_current_user(request: Request) -> str:
    user = getattr(request.state, "user", None)
    if not user:
        raise HTTPException(status_code=401, detail="Authentication required")
    return user.get("sub", "")


def _get_api_key_owner(request: Request) -> str | None:
    ak = getattr(request.state, "api_key", None)
    if not ak:
        return None
    key_id = ak.get("id")
    if key_id:
        return f"ak_{key_id}"
    return None


@router.post("/create", status_code=status.HTTP_201_CREATED)
async def create_key(req: CreateAPIKeyRequest, request: Request):
    key = generate_api_key()
    key_hash = hash_api_key(key)
    key_masked = mask_api_key(key)
    key_prefix = key[:12]

    expires_at = None
    if req.expires_in_days:
        expires_at = int(time.time()) + req.expires_in_days * 86400

    owner = _get_current_user(request)
    if requester := _get_api_key_owner(request):
        owner = f"{owner}:api_key:{requester}"

    rec = await create_api_key(
        user_id=owner,
        name=req.name,
        key_hash=key_hash,
        key_masked=key_masked,
        key_prefix=key_prefix,
        rate_limit_rps=req.rate_limit_rps,
        expires_at=expires_at,
    )
    if not rec:
        raise HTTPException(status_code=500, detail="Failed to create API key")

    logger.info(f"API key created: {key_masked} by {owner}")
    return {
        "id": rec["id"],
        "name": rec["name"],
        "key": key,
        "key_masked": key_masked,
        "rate_limit_rps": rec["rate_limit_rps"],
        "expires_at": rec.get("expires_at"),
        "created_at": rec.get("created_at"),
        "warning": "Store this key securely. It will not be shown again.",
    }


@router.get("/")
async def list_user_keys(request: Request, limit: int = 50, offset: int = 0):
    owner = _get_current_user(request)
    keys = await get_api_keys_by_user(owner)
    return {"keys": keys[:limit], "total": len(keys)}


@router.get("/all")
async def list_all_keys(request: Request, limit: int = 100, offset: int = 0):
    _get_current_user(request)
    keys = await get_all_api_keys(limit=limit, offset=offset)
    return {"keys": keys, "total": len(keys)}


@router.get("/{key_id}")
async def get_key(key_id: int, request: Request):
    owner = _get_current_user(request)
    rec = await get_api_key_by_id(key_id)
    if not rec or rec["user_id"] != owner:
        raise HTTPException(status_code=404, detail="API key not found")
    return rec


@router.post("/{key_id}/revoke")
async def revoke_key(key_id: int, request: Request):
    owner = _get_current_user(request)
    rec = await get_api_key_by_id(key_id)
    if not rec or rec["user_id"] != owner:
        raise HTTPException(status_code=404, detail="API key not found")
    updated = await revoke_api_key(key_id)
    return {"status": "revoked", "key": updated}


@router.delete("/{key_id}")
async def delete_key(key_id: int, request: Request):
    owner = _get_current_user(request)
    rec = await get_api_key_by_id(key_id)
    if not rec or rec["user_id"] != owner:
        raise HTTPException(status_code=404, detail="API key not found")
    ok = await delete_api_key(key_id)
    if not ok:
        raise HTTPException(status_code=500, detail="Failed to delete key")
    return {"status": "deleted", "key_id": key_id}


@router.post("/{key_id}/rotate")
async def rotate_key(key_id: int, req: RotateAPIKeyRequest, request: Request):
    owner = _get_current_user(request)
    rec = await get_api_key_by_id(key_id)
    if not rec or rec["user_id"] != owner:
        raise HTTPException(status_code=404, detail="API key not found")

    if not verify_api_key(req.old_key, rec["key_hash"]):
        await record_api_key_event(
            key_id, "rotate_failed", "Old key mismatch",
            request.client.host if request.client else None,
        )
        raise HTTPException(status_code=400, detail="Old key verification failed")

    new_key = generate_api_key()
    updated = await rotate_api_key(
        key_id=key_id,
        new_key_hash=hash_api_key(new_key),
        new_key_masked=mask_api_key(new_key),
        new_key_prefix=new_key[:12],
    )
    if not updated:
        raise HTTPException(status_code=500, detail="Failed to rotate key")

    await record_api_key_event(key_id, "rotated", f"Grace period: {req.grace_period_hours}h")
    logger.info(f"API key rotated: {key_id}")
    return {
        "status": "rotated",
        "new_key": new_key,
        "key_masked": updated["key_masked"],
        "grace_period_hours": req.grace_period_hours,
    }


@router.get("/{key_id}/usage")
async def key_usage(key_id: int, request: Request, limit: int = 100):
    owner = _get_current_user(request)
    rec = await get_api_key_by_id(key_id)
    if not rec or rec["user_id"] != owner:
        raise HTTPException(status_code=404, detail="API key not found")
    usage = await get_api_key_usage(key_id, limit=min(limit, KEY_USAGE_LIMIT))
    return {"usage": usage[:limit]}


@router.get("/{key_id}/stats")
async def key_stats(key_id: int, request: Request):
    owner = _get_current_user(request)
    rec = await get_api_key_by_id(key_id)
    if not rec or rec["user_id"] != owner:
        raise HTTPException(status_code=404, detail="API key not found")
    stats = await get_api_key_usage_stats(key_id)
    return stats


@router.post("/{key_id}/usage")
async def record_usage_hook(key_id: int, request: Request, payload: dict):
    endpoint = payload.get("endpoint", "unknown")
    status_code = payload.get("status_code", 200)
    latency_ms = payload.get("latency_ms", 0.0)
    await record_api_key_usage(
        key_id, endpoint, status_code, latency_ms,
        request.client.host if request.client else None,
    )
    return {"recorded": True}


@router.get("/{key_id}/admin/quota-alert")
async def quota_alert(key_id: int):
    _ = _get_current_user.__wrapped__ if hasattr(_get_current_user, "__wrapped__") else None
    alert = await get_api_key_usage_stats(key_id)
    rpm_used = alert.get("total_requests", 0)
    return {"key_id": key_id, "rpm_used": rpm_used, "alert": rpm_used > ALERT_RPM_THRESHOLD}


@router.post("/admin/bulk-delete")
async def bulk_delete(request: Request, req: BulkDeleteRequest):
    owner = _get_current_user(request)
    results = {"deleted": [], "failed": []}
    for kid in req.key_ids[:50]:
        rec = await get_api_key_by_id(kid)
        if not rec or rec["user_id"] != owner:
            results["failed"].append(kid)
            continue
        ok = await delete_api_key(kid)
        results["deleted"].append(kid) if ok else results["failed"].append(kid)
    return results
