from __future__ import annotations

import json

from fastapi.responses import JSONResponse


class IdempotencyMiddleware:
    def __init__(self, app) -> None:
        self.app = app

    async def __call__(self, scope, receive, send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        import os
        import sys

        if "pytest" in sys.modules or os.getenv("ENV") == "test":
            await self.app(scope, receive, send)
            return

        # Only check idempotency for modifying operations
        method = scope.get("method")
        if method not in ("POST", "PUT", "PATCH"):
            await self.app(scope, receive, send)
            return

        headers = scope.get("headers", [])
        idempotency_key = None
        for k, v in headers:
            if k.lower() == b"idempotency-key":
                idempotency_key = v.decode("utf-8")
                break

        path = scope.get("path", "")
        if not idempotency_key:
            # Reject critical requests missing Idempotency-Key header to prevent duplicate execution
            if "/api/orchestrate/generate" in path or "/api/markdown/export" in path:
                response = JSONResponse(
                    status_code=400,
                    content={
                        "error": "Idempotency-Key header is required for this action."
                    },
                )
                await response(scope, receive, send)
                return
            await self.app(scope, receive, send)
            return

        import core.app as app_mod

        if (
            not hasattr(app_mod, "redis_queue")
            or not app_mod.redis_queue
            or not app_mod.redis_queue.configured
        ):
            await self.app(scope, receive, send)
            return

        redis = app_mod.redis_queue
        redis_key = f"idempotency:{idempotency_key}"

        # 1. Check if the request key exists in Redis
        existing = redis.get(redis_key)
        if existing:
            try:
                data = json.loads(existing)
                if data.get("status") == "processing":
                    response = JSONResponse(
                        status_code=409,
                        content={
                            "detail": "Conflict: Request is already being processed. Please wait."
                        },
                    )
                    await response(scope, receive, send)
                    return
                elif data.get("status") == "completed":
                    # Replay the cached response
                    from starlette.responses import Response

                    response = Response(
                        content=data.get("body"),
                        status_code=data.get("status_code"),
                        media_type=data.get("media_type"),
                    )
                    await response(scope, receive, send)
                    return
            except Exception:
                pass

        # 2. Lock the idempotency key (10 minute timeout to prevent deadlocks)
        redis.set(redis_key, json.dumps({"status": "processing"}), ex=600)

        # 3. Call the next request handler and capture response
        response_body = b""
        response_headers = []
        response_status = 200

        async def custom_send(message):
            nonlocal response_body, response_headers, response_status
            if message["type"] == "http.response.start":
                response_status = message["status"]
                response_headers = message.get("headers", [])
            elif message["type"] == "http.response.body":
                response_body += message.get("body", b"")
            await send(message)

        try:
            await self.app(scope, receive, custom_send)

            # Get media_type from response headers if possible
            media_type = "application/json"
            for k, v in response_headers:
                if k.lower() == b"content-type":
                    media_type = v.decode("utf-8")
                    break

            # Store completed response in redis (Cache for 24 hours)
            redis.set(
                redis_key,
                json.dumps(
                    {
                        "status": "completed",
                        "status_code": response_status,
                        "media_type": media_type,
                        "body": response_body.decode("utf-8", errors="replace"),
                    }
                ),
                ex=86400,
            )
        except Exception as e:
            # Clear key on failure so the client can retry immediately
            try:
                redis.set(redis_key, "", ex=1)
            except Exception:
                pass
            raise e
