# backend/api/dependencies.py
from fastapi import Depends, Request, HTTPException
from backend.core.security import verify_token
from backend.core.tenant_db import TenantAwareFirestore
from loguru import logger

def get_current_user_token(request: Request) -> dict:
    auth_header = request.headers.get("Authorization")
    if not auth_header or not auth_header.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Unauthorized")
    token = auth_header.split(" ")[1]
    return verify_token(token)

def get_tenant_db(payload: dict = Depends(get_current_user_token)) -> TenantAwareFirestore:
    """
    Dependency Injection: Extracts tenant_id (user email/uid) from JWT 
    and returns a hard-isolated Firestore client.
    """
    tenant_id = payload.get("sub")
    if not tenant_id:
        logger.error("Token payload missing 'sub' (tenant_id) claim.")
        raise HTTPException(status_code=401, detail="Invalid token structure.")
    
    # রিটার্ন করছে আইসোলেটেড ডিবি ক্লায়েন্ট
    return TenantAwareFirestore(tenant_id=tenant_id)
