from __future__ import annotations

import os
import secrets
from typing import Optional

from fastapi import Request, HTTPException
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.responses import JSONResponse

from loguru import logger


def _get_bearer_token(request: Request) -> Optional[str]:
    auth = request.headers.get("authorization")
    if not auth:
        return None
    parts = auth.split()
    if len(parts) == 2 and parts[0].lower() == "bearer":
        return parts[1]
    return None


class AuthMiddleware(BaseHTTPMiddleware):
    def __init__(self, app) -> None:
        super().__init__(app)

    async def dispatch(self, request: Request, call_next):
        path = request.url.path

        # Strict admin origin check to prevent security blast radius breach
        admin_paths = ["/admin/", "/admin-api/", "/gcp/"]
        is_admin_path = any(path.startswith(admin_path) for admin_path in admin_paths) or path in {"/admin/rules", "/admin/cloud-distribution"}

        if is_admin_path:
            origin = request.headers.get("origin", "")
            referer = request.headers.get("referer", "")

            # Allow localhost / 127.0.0.1 only in development/test environments
            from config import settings
            is_env_local = settings.env.lower() in {"local", "test"}
            is_local = is_env_local and ("localhost" in origin or "127.0.0.1" in origin or "localhost" in referer or "127.0.0.1" in referer)

            # Allow supremeai-admin domain
            is_admin_domain = "supremeai-admin" in origin or "supremeai-admin" in referer

            # If request is in production and comes from general studio domain or any local bypass, block it.
            if not is_local and not is_admin_domain and (origin or referer):
                logger.warning(f"Forbidden admin access to {path} from unauthorized origin/referer: {origin} / {referer}")
                return JSONResponse(
                    status_code=403,
                    content={"detail": "Forbidden: Admin endpoints are restricted to the admin console domain."},
                )

        enabled = bool(os.getenv("SUPREMEAI_API_TOKEN"))
        if not enabled:
            return await call_next(request)

        public_paths = {
            "/health",
            "/actuator/health",
            "/docs",
            "/redoc",
            "/openapi.json",
        }
        if path in public_paths or path.startswith("/static"):
            return await call_next(request)

        token = _get_bearer_token(request)
        expected = os.getenv("SUPREMEAI_API_TOKEN") or ""
        if not token or not secrets.compare_digest(token, expected):
            logger.warning(f"Unauthorized access attempt to {path}")
            return JSONResponse(
                status_code=401,
                content={"detail": "Invalid or missing API token."},
            )
        return await call_next(request)
