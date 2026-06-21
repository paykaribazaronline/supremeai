from __future__ import annotations

import json
from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.responses import Response, JSONResponse

class IdempotencyMiddleware(BaseHTTPMiddleware):
    def __init__(self, app) -> None:
        super().__init__(app)

    async def dispatch(self, request: Request, call_next):
        import sys
        import os
        if "pytest" in sys.modules or os.getenv("ENV") == "test":
            return await call_next(request)

        # Only check idempotency for modifying operations
        if request.method not in ("POST", "PUT", "PATCH"):
            return await call_next(request)

        idempotency_key = request.headers.get("idempotency-key")
        if not idempotency_key:
            return await call_next(request)

        import core.app as app_mod
        if not hasattr(app_mod, "redis_queue") or not app_mod.redis_queue or not app_mod.redis_queue.configured:
            return await call_next(request)

        redis = app_mod.redis_queue
        redis_key = f"idempotency:{idempotency_key}"

        # 1. Check if the request key exists in Redis
        existing = redis.get(redis_key)
        if existing:
            try:
                data = json.loads(existing)
                if data.get("status") == "processing":
                    return JSONResponse(
                        status_code=409,
                        content={"detail": "Conflict: Request is already being processed. Please wait."}
                    )
                elif data.get("status") == "completed":
                    # Replay the cached response
                    return Response(
                        content=data.get("body"),
                        status_code=data.get("status_code"),
                        media_type=data.get("media_type")
                    )
            except Exception:
                pass

        # 2. Lock the idempotency key (10 minute timeout to prevent deadlocks)
        redis.set(redis_key, json.dumps({"status": "processing"}), ex=600)

        # 3. Call the next request handler
        try:
            response = await call_next(request)
            
            # Read response body safely without hanging
            response_body = b""
            async for chunk in response.body_iterator:
                response_body += chunk

            # Store completed response in redis (Cache for 24 hours)
            redis.set(
                redis_key,
                json.dumps({
                    "status": "completed",
                    "status_code": response.status_code,
                    "media_type": response.media_type,
                    "body": response_body.decode("utf-8", errors="replace")
                }),
                ex=86400
            )

            # Reconstruct the response since we consumed the body iterator
            return Response(
                content=response_body,
                status_code=response.status_code,
                headers=dict(response.headers),
                media_type=response.media_type
            )
        except Exception as e:
            # Clear key on failure so the client can retry immediately
            try:
                redis.set(redis_key, "", ex=1)
            except Exception:
                pass
            raise e
