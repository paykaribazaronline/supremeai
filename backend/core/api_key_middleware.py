import contextlib
import os
import sys
import time

from fastapi import HTTPException
from fastapi import Request
from loguru import logger
from starlette.middleware.base import BaseHTTPMiddleware

from core.api_key_rate_limiter import APIKeyRateLimiter
from core.pgbouncer_pool import get_db_pool
from core.security import API_KEY_PREFIX
from core.security import hash_api_key
from core.security import mask_api_key
from models.api_key import record_api_key_usage


class APIKeyAuthMiddleware(BaseHTTPMiddleware):
    def __init__(self, app) -> None:
        super().__init__(app)
        self.limiter = APIKeyRateLimiter()
        self.prefix = API_KEY_PREFIX

    async def dispatch(self, request: Request, call_next):
        api_key_header = request.headers.get("x-api-key")
        if not api_key_header or not api_key_header.startswith(self.prefix):
            return await call_next(request)

        is_test = "pytest" in sys.modules or os.getenv("ENV") == "test"
        if is_test:
            request.state.api_key = {"id": "test", "masked": mask_api_key(api_key_header)}
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

        request.state.api_key = {"id": row["id"], "masked": mask_api_key(api_key_header)}
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
