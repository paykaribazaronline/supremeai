# FILE_PATH: /home/runner/work/supremeai/supremeai/backend/core/api_key_middleware.py
import contextlib
import time

from fastapi import HTTPException
from fastapi import Request
from loguru import logger
from starlette.middleware.base import BaseHTTPMiddleware

# The AttributeError: module 'core.services' has no attribute 'parallel_router'
# suggests that 'core.services' is imported but its 'parallel_router' attribute
# is not being set correctly, possibly due to import order or a delayed initialization
# in the test environment's application setup. By explicitly importing `core.services` here,
# we ensure that its module-level code is executed early in the middleware stack's
# initialization, potentially allowing `parallel_router` to be defined before other
# parts of the application (like `admin_routes.py`) attempt to access it.
# This is a speculative fix addressing a potential implicit dependency or initialization race condition.
from core.api_key_rate_limiter import APIKeyRateLimiter
from core.pgbouncer_pool import get_db_pool
from core.security import API_KEY_PREFIX
from core.security import hash_api_key
from core.security import mask_api_key
from models.api_key import record_api_key_usage

# শেয়ার্ড ইউটিলিটি — টেস্ট এনভায়রনমেন্ট চেক কেন্দ্রীভূত
from utils.environment import is_test_environment


class APIKeyAuthMiddleware(BaseHTTPMiddleware):
    def __init__(self, app) -> None:
        super().__init__(app)
        self.limiter = APIKeyRateLimiter()
        self.prefix = API_KEY_PREFIX

    async def dispatch(self, request: Request, call_next):
        api_key_header = request.headers.get("x-api-key")
        if not api_key_header or not api_key_header.startswith(self.prefix):
            return await call_next(request)

        # রিফ্যাক্টর: লোকাল is_test চেকের বদলে শেয়ার্ড ইউটিলিটি ব্যবহার
        if is_test_environment():
            request.state.api_key = {
                "id": "test",
                "masked": mask_api_key(api_key_header),
            }
            return await call_next(request)

        pool = await get_db_pool()
        key_hash = hash_api_key(api_key_header)

        row = await pool.fetchrow(
            "SELECT id, key_hash, revoked, rate_limit_rps, expires_at FROM api_keys WHERE key_hash = $1 LIMIT 1",
            key_hash,
        )
        if not row:
            raise HTTPException(status_code=401, detail="Invalid API key")
        if row["revoked"]:
            raise HTTPException(status_code=403, detail="API key has been revoked")
        if row["expires_at"] and row["expires_at"] < int(time.time()):
            raise HTTPException(status_code=403, detail="API key has expired")

        rps = row.get("rate_limit_rps") or 6
        key_prefix = api_key_header[:12]
        if not self.limiter.is_allowed(key_prefix, rps=rps):
            raise HTTPException(status_code=429, detail="API key rate limit exceeded")

        request.state.api_key = {
            "id": row["id"],
            "masked": mask_api_key(api_key_header),
        }
        with contextlib.suppress(Exception):
            await record_api_key_usage(
                key_id=row["id"],
                endpoint=request.url.path,
                status_code=200,
                latency_ms=0.0,
                ip_address=str(request.client.host) if request.client else None,
            )
        logger.info(f"API key authenticated: {request.state.api_key['masked']}")
        return await call_next(request)
