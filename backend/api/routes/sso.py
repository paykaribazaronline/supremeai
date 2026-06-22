from __future__ import annotations

import secrets
from typing import List, Optional

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


class OIDCDiscoveryRequest(BaseModel):
    issuer: str
    redirect_uri: str
    client_id: str
    scope: str = "openid profile email"


class OIDCLoginResponse(BaseModel):
    authorization_url: str
    state: str


class SSOLoginResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user_id: str
    roles: List[str]
    email: str
    method: str


class OIDCCallbackRequest(BaseModel):
    code: str
    state: str
    provider: str = "generic"


class ProviderSSORequest(BaseModel):
    client_id: Optional[str] = None
    redirect_uri: Optional[str] = None
    state: Optional[str] = None
    scope: str = "openid profile email"


@router.post("/oidc/discovery", response_model=OIDCLoginResponse)
async def oidc_discovery(payload: OIDCDiscoveryRequest):
    state = secrets.token_urlsafe(16)
    auth_url = (
        f"{payload.issuer}/authorize"
        f"?response_type=code"
        f"&client_id={payload.client_id}"
        f"&redirect_uri={payload.redirect_uri}"
        f"&scope={payload.scope}"
        f"&state={state}"
    )
    return OIDCLoginResponse(authorization_url=auth_url, state=state)


@router.post("/oidc/{provider}/authorize", response_model=OIDCLoginResponse)
async def oidc_provider_authorize(provider: str, payload: ProviderSSORequest):
    if sso is None:
        raise HTTPException(status_code=503, detail="SSO service is unavailable")
    client_id = payload.client_id or getattr(settings, "oidc_client_id", "")
    redirect_uri = payload.redirect_uri or getattr(settings, "oidc_redirect_uri", "")
    state = payload.state or secrets.token_urlsafe(16)
    try:
        auth_url = sso.get_oidc_auth_url(
            provider=provider,
            client_id=client_id,
            redirect_uri=redirect_uri,
            state=state,
            scope=payload.scope,
        )
        return OIDCLoginResponse(authorization_url=auth_url, state=state)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@router.post("/oidc/{provider}/callback", response_model=SSOLoginResponse)
async def oidc_provider_callback(provider: str, payload: OIDCCallbackRequest):
    if sso is None:
        raise HTTPException(status_code=503, detail="SSO service is unavailable")
    client_id = getattr(settings, "oidc_client_id", "")
    client_secret = getattr(settings, "oidc_client_secret", "")
    redirect_uri = getattr(settings, "oidc_redirect_uri", "")
    result = await sso.process_oidc_response(
        provider=provider,
        code=payload.code,
        state=payload.state,
    )
    if result.get("status") != "success":
        raise HTTPException(status_code=401, detail=result.get("message", "OIDC authentication failed"))
    primary_role = (result.get("roles") or ["viewer"])[0]
    token_data = {
        "sub": result.get("user_id", "unknown"),
        "role": primary_role,
        "email": result.get("email", ""),
        "method": result.get("method", f"oidc:{provider}"),
    }
    if jwt is None:
        raise HTTPException(status_code=503, detail="JWT library is unavailable")
    access_token = jwt.encode(token_data, settings.jwt_secret, algorithm="HS256")
    return SSOLoginResponse(
        access_token=access_token,
        user_id=token_data["sub"],
        roles=result.get("roles", []),
        email=token_data["email"],
        method=token_data["method"],
    )


@router.get("/oidc/{provider}/logout")
async def oidc_logout(provider: str):
    if sso is None:
        raise HTTPException(status_code=503, detail="SSO service is unavailable")
    logout_url = ""
    try:
        cfg = sso.OIDC_PROVIDERS.get(provider.lower(), {})
        base = cfg.get("end_session_endpoint", "").format(
            domain=getattr(settings, "oidc_domain", ""),
            tenant=getattr(settings, "oidc_tenant", ""),
        )
        logout_url = base or ""
    except Exception:
        pass
    return {"logout_url": logout_url, "provider": provider}


@router.post("/saml", response_model=SSOLoginResponse)
async def saml_login(payload: SAMLAssertionRequest):
    if sso is None:
        raise HTTPException(status_code=503, detail="SSO service is unavailable")
    result = await sso.process_sso_response({"SAMLResponse": payload.assertion})
    if result.get("status") != "success":
        raise HTTPException(status_code=401, detail=result.get("message", "SAML authentication failed"))
    roles = result.get("roles", ["viewer"])
    primary_role = roles[0] if roles else "viewer"
    token_data = {
        "sub": result.get("user_id", "unknown"),
        "role": primary_role,
        "email": result.get("email", ""),
        "method": result.get("method", "saml"),
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


@router.get("/metadata")
async def sso_metadata():
    if sso is None:
        raise HTTPException(status_code=503, detail="SSO service is unavailable")
    metadata = sso.get_metadata()
    from fastapi.responses import PlainTextResponse
    return PlainTextResponse(
        content=metadata.get("body", ""),
        media_type=metadata.get("content_type", "application/xml"),
    )
