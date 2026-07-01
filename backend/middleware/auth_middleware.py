
from fastapi import Request
from loguru import logger
from starlette.middleware.base import BaseHTTPMiddleware

from core.security import verify_token


class ZeroTrustAuthMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        public_paths = [
            "/docs",
            "/redoc",
            "/openapi.json",
            "/health",
            "/api/health",
            "/api/auth/login",
            "/api/admin/login",
            "/api/admin/verify",
            "/api/task/stream",
            "/api/v1/collaborate",
            "/api/v1/graph",
        ]
        if request.method == "OPTIONS":
            return await call_next(request)

        matched = (
            request.url.path in public_paths
            or any(request.url.path.startswith(p + "/") for p in public_paths)
        )
        if matched:
            return await call_next(request)

        auth_header = request.headers.get("Authorization")

        if not auth_header or not auth_header.startswith("Bearer "):
            logger.warning(f"🚨 Blocked unauthorized request to {request.url.path}")
            from fastapi.responses import JSONResponse

            return JSONResponse(
                status_code=401,
                content={"detail": "Missing or invalid Authorization header"},
            )

        token = auth_header.split(" ")[1]

        try:
            payload = verify_token(token)
            request.state.user = payload

            # অ্যাডমিন রাউটের জন্য স্ট্রিক্ট রোল চেক
            if (
                request.url.path.startswith("/api/admin")
                and payload.get("role") != "admin"
            ):
                logger.critical(
                    f"🔒 Privilege Escalation Blocked for user: {payload.get('sub')}"
                )
                from fastapi.responses import JSONResponse

                return JSONResponse(
                    status_code=403,
                    content={
                        "detail": "Insufficient privileges. Admin access required."
                    },
                )

        except Exception as e:
            from fastapi.responses import JSONResponse

            logger.error(f"Token validation failed: {e}")
            return JSONResponse(status_code=401, content={"detail": "Invalid or missing API token."})

        return await call_next(request)
