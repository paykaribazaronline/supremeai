from __future__ import annotations

from typing import Dict, Any, List

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from core.config import settings

try:
    from jose import JWTError, jwt
except ImportError:
    JWTError = Exception  # type: ignore[misc,assignment]
    jwt = None  # type: ignore[assignment]

try:
    from tools.sso_integrator import SSOIntegrator
    sso = SSOIntegrator()
except Exception:
    sso = None  # type: ignore[assignment]

router = APIRouter(prefix="/auth/sso", tags=["sso"])

class SAMLAssertionRequest(BaseModel):
    assertion: str

class SSOLoginResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user_id: str
    roles: List[str]
    email: str
    method: str

@router.post("/saml", response_model=SSOLoginResponse)
async def saml_login(payload: SAMLAssertionRequest):
    if sso is None:
        raise HTTPException(status_code=503, detail="SSO service is unavailable")
    result = await sso.verify_saml_assertion(payload.assertion)
    roles = await sso.map_roles(result.get("groups", []))
    primary_role = roles[0] if roles else "viewer"
    token_data = {
        "sub": result.get("user_id", "unknown"),
        "role": primary_role,
        "email": result.get("email", ""),
        "method": result.get("method", "mock_fallback"),
    }
    if jwt is None:
        raise HTTPException(status_code=503, detail="JWT library is unavailable")
    access_token = jwt.encode(token_data, settings.jwt_secret, algorithm="HS256")
    return SSOLoginResponse(
        access_token=access_token,
        user_id=token_data["sub"],
        roles=roles,
        email=token_data["email"],
        method=token_data["method"],
    )
