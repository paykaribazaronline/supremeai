"""Authentication and rate limiting helpers for the admin dashboard."""

from __future__ import annotations

import asyncio
import secrets

from fastapi import Depends, HTTPException, Request
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
import jwt
from loguru import logger

from core.config import settings

security = HTTPBearer()
_in_memory_jwt_blacklist: set[str] = set()


async def require_admin_token(credentials: HTTPAuthorizationCredentials = Depends(security)):
    token = credentials.credentials
    try:
        jwt_secret = settings.jwt_secret
        decoded = jwt.decode(token, jwt_secret, algorithms=["HS256"])
        if decoded.get("role") != "admin":
            raise HTTPException(status_code=403, detail="Forbidden: User does not have admin role.")

        jti = decoded.get("jti")
        if jti:
            import core.services as app_mod

            redis_queue = getattr(app_mod, "redis_queue", None)
            if redis_queue and getattr(redis_queue, "configured", False):
                # বাংলা: UpstashRedisQueue.get সিঙ্ক্রোনাস (httpx ক্লায়েন্ট) — async route-এ
                # সরাসরি কল করলে event loop ব্লক হয়। asyncio.to_thread দিয়ে offload করা হলো।
                try:
                    blocked = await asyncio.to_thread(redis_queue.get, f"jwt_blacklist:{jti}")
                    if blocked is not None:
                        raise HTTPException(status_code=401, detail="Token has been revoked.")
                except HTTPException:
                    raise
                except Exception as exc:
                    logger.warning(f"Redis blacklist check failed for jti={jti}: {exc}")
            else:
                if jti in _in_memory_jwt_blacklist:
                    raise HTTPException(status_code=401, detail="Token has been revoked.")
                logger.warning("Redis not configured; falling back to in-memory JWT blacklist check.")

        return decoded
    except HTTPException:
        # বাংলা: HTTPException যেগুলো নিজে রেইজ করেছি সেগুলো পুনরায় রেইজ করি।
        raise
    except Exception as err:
        logger.warning("Admin token validation failed", exc_info=True)
        expected = getattr(settings, "supremeai_api_token", None) or ""
        if expected and secrets.compare_digest(token, expected):
            return {"uid": "admin", "role": "admin"}
        raise HTTPException(status_code=401, detail="Authentication failed.") from err


async def admin_rate_limit(request: Request):
    """বাংলা: admin rate limiter — async-friendly।

    UpstashRedisQueue এর get/set সিঙ্ক্রোনাস, তাই to_thread দিয়ে offload করা হলো।
    """
    import core.services as app_mod

    client_ip = request.client.host if request.client else "unknown"
    key = f"rate_limit:admin:{client_ip}"
    limit = 600
    window = 60

    redis_queue = getattr(app_mod, "redis_queue", None)
    if redis_queue and getattr(redis_queue, "configured", False):
        try:
            current_hits = await asyncio.to_thread(redis_queue.get, key)
            if current_hits is not None and int(current_hits) >= limit:
                logger.warning(f"Distributed admin rate limit exceeded for {client_ip}")
                raise HTTPException(
                    status_code=429,
                    detail="Too many admin requests. Please try again later.",
                )
            await asyncio.to_thread(redis_queue.set, key, int(current_hits or 0) + 1, ex=window)
        except HTTPException:
            raise
        except Exception as exc:
            logger.warning(f"Admin distributed rate-limit check failed: {exc}")
    return True
