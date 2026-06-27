from __future__ import annotations

import os
import secrets

from fastapi.responses import JSONResponse
from loguru import logger

from core.config import settings


def _get_bearer_token(headers) -> str | None:
    for k, v in headers:
        if k.lower() == b"authorization":
            auth = v.decode("utf-8")
            parts = auth.split()
            if len(parts) == 2 and parts[0].lower() == "bearer":
                return parts[1]
    return None


class AuthMiddleware:
    def __init__(self, app) -> None:
        self.app = app

    async def __call__(self, scope, receive, send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        path = scope.get("path", "")
        headers = scope.get("headers", [])

        # Strict admin origin check to prevent security blast radius breach
        admin_paths = ["/admin/", "/admin-api/", "/gcp/"]
        is_admin_path = any(path.startswith(admin_path) for admin_path in admin_paths) or path in {"/admin/rules", "/admin/cloud-distribution"}

        if is_admin_path:
            origin = ""
            referer = ""
            for k, v in headers:
                if k.lower() == b"origin":
                    origin = v.decode("utf-8")
                elif k.lower() == b"referer":
                    referer = v.decode("utf-8")

            # Allow supremeai-admin domain
            is_admin_domain = "supremeai-admin" in origin or "supremeai-admin" in referer

            # If request comes from general studio domain or unauthorized source, block it.
            if not is_admin_domain and (origin or referer):
                logger.warning(f"Forbidden admin access to {path} from unauthorized origin/referer: {origin} / {referer}")
                response = JSONResponse(
                    status_code=403,
                    content={"detail": "Forbidden: Admin endpoints are restricted to the admin console domain."},
                )
                await response(scope, receive, send)
                return

            # --- Agentic Security Check: Verify Backend JWT for Admin Routes ---
            token = _get_bearer_token(headers)
            if not token:
                logger.warning(f"Missing bearer token for admin path: {path}")
                response = JSONResponse(
                    status_code=401,
                    content={"detail": "Missing Authorization Token for admin route."},
                )
                await response(scope, receive, send)
                return
            try:
                from jose import jwt

                jwt_secret = settings.jwt_secret
                decoded = jwt.decode(token, jwt_secret, algorithms=["HS256"])
                if decoded.get("role") != "admin":
                    response = JSONResponse(
                        status_code=403,
                        content={"detail": "Forbidden: User does not have admin role."},
                    )
                    await response(scope, receive, send)
                    return
            except Exception as e:
                logger.error(f"JWT validation failed for admin path: {e}")
                response = JSONResponse(
                    status_code=401,
                    content={"detail": f"Invalid Admin Authorization Token: {str(e)}"},
                )
                await response(scope, receive, send)
                return

        enabled = bool(os.getenv("SUPREMEAI_API_TOKEN"))
        if not enabled:
            await self.app(scope, receive, send)
            return

        public_paths = {
            "/health",
            "/actuator/health",
            "/docs",
            "/redoc",
            "/openapi.json",
            "/api/admin/login",
            "/api/admin/verify",
            "/api/admin/firebase-login",
            "/api/admin/firebase-totp-setup",
            "/api/admin/firebase-totp-verify",
        }
        if path in public_paths or path.startswith("/static"):
            await self.app(scope, receive, send)
            return

        token = _get_bearer_token(headers)
        expected = os.getenv("SUPREMEAI_API_TOKEN") or ""
        if not token or not secrets.compare_digest(token, expected):
            logger.warning(f"Unauthorized access attempt to {path}")
            response = JSONResponse(
                status_code=401,
                content={"detail": "Invalid or missing API token."},
            )
            await response(scope, receive, send)
            return
        await self.app(scope, receive, send)
