import os
from typing import Any

import httpx
from fastapi import APIRouter
from fastapi import HTTPException
from fastapi import Request
from fastapi import Response
from fastapi.responses import JSONResponse
from loguru import logger
from pydantic import BaseModel

from core.auth_middleware import AuthMiddleware
from core.rate_limiter import RateLimiter


auth_middleware = AuthMiddleware.__new__(AuthMiddleware)
auth_middleware.enabled = bool(os.getenv("SUPREMEAI_API_TOKEN"))
rate_limiter = RateLimiter()

from brain.api_router import ApiRouter


api_router = ApiRouter()
router = APIRouter(prefix="/api/v1/gateway", tags=["gateway"])


class GatewayRequest(BaseModel):
    path: str
    method: str = "GET"
    payload: dict[str, Any] | None = None
    source: str | None = None  # 'vscode' | 'flutter' | 'telegram' | 'web'
    headers: dict[str, str] | None = None


class InternalGateway:
    def __init__(self):
        self.n8n_url = os.getenv("N8N_URL", "http://127.0.0.1:5678")

    def trigger_n8n_workflow(
        self, webhook_path: str, payload: dict[str, Any]
    ) -> dict[str, Any]:
        url = f"{self.n8n_url}/{webhook_path.lstrip('/')}"
        logger.info(f"Triggering n8n workflow at {url}")
        try:
            response = httpx.post(url, json=payload, timeout=10.0)
            return {
                "success": response.is_success,
                "status_code": response.status_code,
                "data": response.json() if response.is_success else response.text,
            }
        except Exception as exc:
            logger.error(f"n8n trigger failed: {exc}")
            return {"success": False, "error": str(exc)}

    def trigger_make_webhook(
        self, webhook_url: str, payload: dict[str, Any]
    ) -> dict[str, Any]:
        logger.info("Triggering Make.com webhook")
        try:
            response = httpx.post(webhook_url, json=payload, timeout=10.0)
            return {"success": response.is_success, "response": response.text}
        except Exception as exc:
            return {"success": False, "error": str(exc)}


APIGateway = InternalGateway
ALLOWED_BACKEND_PATHS = {
    "vscode": [
        "/api/chat/completion",
        "/api/chat/stream",
        "/api/knowledge/learn",
        "/api/memory/ingest",
        "/api/codeflow/analyze",
    ],
    "flutter": ["/api/chat/message", "/api/chat/history", "/api/knowledge/stats"],
    "telegram": ["/api/chat/message", "/api/knowledge/feedback"],
    "web": ["/api/chat/message", "/api/chat/stream"],
}


@router.post("/forward")
async def gateway_forward(request: GatewayRequest, http_request: Request) -> Response:
    source = (request.source or "web").lower()
    if source not in ALLOWED_BACKEND_PATHS:
        raise HTTPException(status_code=400, detail="unknown source")

    allowed = ALLOWED_BACKEND_PATHS.get(source, [])
    normalized = request.path.strip().lower()
    if not any(
        normalized == allowed_path.lower()
        or normalized.startswith(allowed_path.lower() + "/")
        for allowed_path in allowed
    ):
        logger.warning(f"Blocked path for source={source}: {request.path}")
        raise HTTPException(status_code=403, detail="path not allowed for source")

    client_ip = http_request.client.host if http_request.client else "127.0.0.1"
    if not rate_limiter.check(client_ip):
        raise HTTPException(status_code=429, detail="rate limit exceeded")

    backend_url = os.getenv("SUPREMEAI_BACKEND_URL", "http://127.0.0.1:8000/api/v1")
    target = backend_url.rstrip("/") + "/" + request.path.lstrip("/")

    headers = dict(request.headers or {})
    headers.setdefault("X-Source", source)

    # API Key Rotation & Free Tier Tracking Integration
    if any(
        endpoint in normalized
        for endpoint in ["chat/completion", "chat/stream", "chat/message"]
    ):
        try:
            from tools.multi_account_rotator import get_rotator, TaskType
            from core.free_tier_tracker import get_tracker

            tracker = get_tracker()
            rotator = get_rotator()

            best_provider_name = tracker.get_best_provider()
            if best_provider_name:
                # Tell rotator to get an account (task=CHAT)
                provider_account = rotator.get_best_provider_for_task(TaskType.CHAT)
                if provider_account:
                    provider, account = provider_account
                    if account and account.api_key:
                        headers["X-Dynamic-Provider"] = provider.name
                        headers["X-Dynamic-API-Key"] = account.api_key
                        # Record a basic hit (backend should ideally report exact tokens later)
                        tracker.record(provider.name, token_count=100)
                        logger.info(
                            f"Injected {provider.name} key from rotator for {normalized}"
                        )
        except Exception as e:
            logger.warning(f"Failed to inject dynamic API key: {e}")

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            req_method = (request.method or "GET").upper()
            if req_method == "POST":
                response = await client.post(
                    target, json=request.payload or {}, headers=headers
                )
            else:
                response = await client.get(target, headers=headers)

            # If rate limited (429), pause the provider
            if response.status_code == 429 and "X-Dynamic-Provider" in headers:
                try:
                    failed_provider = headers["X-Dynamic-Provider"]
                    tracker.mark_rate_limited(failed_provider, pause_seconds=60)
                    logger.warning(
                        f"Provider {failed_provider} hit 429, paused for 60s."
                    )
                except Exception:
                    pass

        return JSONResponse(content=response.json(), status_code=response.status_code)
    except httpx.HTTPStatusError as exc:
        raise HTTPException(status_code=exc.response.status_code)
    except Exception as exc:
        logger.exception("gateway forward failed")
        raise HTTPException(status_code=502, detail=str(exc))


@router.post("/dispatch/{capability}")
async def api_dispatch(capability: str, payload: dict[str, Any]) -> JSONResponse:
    try:
        result = api_router.dispatch(capability, payload or {})
    except KeyError as exc:
        raise HTTPException(status_code=404, detail=str(exc))
    status = 200 if result.get("success", True) else 502
    return JSONResponse(content=result, status_code=status)


@router.post("/n8n")
async def trigger_n8n(
    webhook_path: str = "", payload: dict[str, Any] = None
) -> JSONResponse:
    if payload is None:
        payload = {}
    internal = InternalGateway()
    result = internal.trigger_n8n_workflow(webhook_path, payload)
    status = 200 if result.get("success") else 502
    return JSONResponse(content=result, status_code=status)


@router.post("/make")
async def trigger_make(
    webhook_url: str = "", payload: dict[str, Any] = None
) -> JSONResponse:
    if payload is None:
        payload = {}
    internal = InternalGateway()
    result = internal.trigger_make_webhook(webhook_url, payload)
    status = 200 if result.get("success") else 502
    return JSONResponse(content=result, status_code=status)
