# 📄 ফাইল: backend/middleware/idempotency.py

**প্রকার:** .py  
**সাইজ:** 6,281 বাইট  
**আপডেট:** 2026-07-08T19:19:07.505871

---

## কোড

```py
import json

from fastapi import HTTPException
from fastapi import Request
from fastapi.responses import JSONResponse
from loguru import logger
from starlette.middleware.base import BaseHTTPMiddleware


# বাংলা মন্তব্য: Redis-based Distributed Idempotency Middleware
# পূর্বে Firestore (Firebase) ব্যবহার করা হতো, যা Serverless-এ ব্যয়বহুল এবং ধীর ছিল।
# এখন Redis SET NX (atomic, sub-millisecond) ব্যবহার করা হচ্ছে — fail-open মোডে।

IDEMPOTENCY_TTL_SECONDS = 120  # ২ মিনিট লক — নেটওয়ার্ক retry-র জন্য যথেষ্ট

# বাংলা মন্তব্য: যে endpoint-গুলোতে Idempotency চেক প্রযোজ্য
IDEMPOTENCY_PATHS = (
    "/api/task",
    "/api/github",
    "/api/auth/callback",
    "/api/pr",
    "/api/agent",
)


class IdempotencyMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        # শুধুমাত্র POST রিকোয়েস্ট এবং নির্দিষ্ট critical path-এর জন্য চেক করবে
        path = request.url.path
        if request.method != "POST" or not any(path.startswith(p) for p in IDEMPOTENCY_PATHS):
            return await call_next(request)

        idempotency_key = request.headers.get("Idempotency-Key")
        if not idempotency_key:
            # বাংলা মন্তব্য: ক্রিটিক্যাল POST রিকোয়েস্টে key না থাকলে reject করা হবে
            return JSONResponse(
                status_code=400,
                content={
                    "error": "Bad Request: 'Idempotency-Key' header is required for mutating operations.",
                    "hint": "Provide a unique UUID as 'Idempotency-Key' header."
                },
            )

        # বাংলা মন্তব্য: Redis lock অধিগ্রহণের চেষ্টা (SET NX — atomic)
        try:
            from core.redis_manager import acquire_idempotency_lock
            from core.redis_manager import cache_response_and_release_lock
            from core.redis_manager import redis_manager
            from core.redis_manager import release_idempotency_lock
        except ImportError:
            # Redis ইমপোর্ট ব্যর্থ হলে fail-open — request পাস করে দাও
            logger.warning("[Idempotency] Failed to import redis_manager — skipping check (fail-open)")
            return await call_next(request)

        # বাংলা মন্তব্য: Redis থেকে cached response চেক করা
        cached_response = None  # noqa: F841
        if redis_manager.client is not None:
            try:
                cached_key = f"idempotency:response:{idempotency_key}"
                cached = await redis_manager.client.get(cached_key)
                if cached:
                    logger.info(f"⚡ Idempotency Hit: serving cached response for key {idempotency_key}")
                    cached_data = json.loads(cached)
                    return JSONResponse(
                        status_code=cached_data.get("status_code", 200),
                        content=cached_data.get("body", {}),
                        headers={"X-Cache-Lookup": "HIT - Idempotency Lock"},
                    )
            except Exception as e:  # noqa: BLE001
                logger.warning(f"[Idempotency] Cache read failed — continuing: {e}")

        # বাংলা মন্তব্য: Processing lock অধিগ্রহণ
        acquired = await acquire_idempotency_lock(idempotency_key, IDEMPOTENCY_TTL_SECONDS)
        if not acquired:
            logger.warning(f"🛡️ Idempotency Block: {idempotency_key} is already being processed.")
            raise HTTPException(
                status_code=409,
                detail="Conflict: Request is already being processed. Duplicate execution blocked.",
            )

        try:
            response = await call_next(request)

            # বাংলা মন্তব্য: সফল রেসপন্স Redis-এ cache করা
            if response.status_code == 200 and redis_manager.client is not None:
                if hasattr(response, "body_iterator"):
                    response_body = [section async for section in response.body_iterator]
                    from starlette.responses import Response
                    body_bytes = b"".join(response_body)
                    response = Response(
                        content=body_bytes,
                        status_code=response.status_code,
                        headers=dict(response.headers),
                        media_type=response.media_type
                    )
                else:
                    body_bytes = response.body if hasattr(response, "body") else b"{}"

                try:
                    body_str = body_bytes.decode("utf-8")
                    cache_data = json.dumps({"status_code": 200, "body": json.loads(body_str)})
                    await cache_response_and_release_lock(
                        idempotency_key,
                        cache_data,
                        IDEMPOTENCY_TTL_SECONDS * 5
                    )
                except Exception as cache_err:  # noqa: BLE001
                    logger.warning(f"[Idempotency] Response caching failed (non-blocking): {cache_err}")
                    await release_idempotency_lock(idempotency_key)
            else:
                # বাংলা মন্তব্য: ব্যর্থ রিকোয়েস্টে লক রিলিজ করা যাতে retry পারে
                await release_idempotency_lock(idempotency_key)

            return response

        except Exception as e:
            # বাংলা মন্তব্য: Exception হলে লক রিলিজ করা
            await release_idempotency_lock(idempotency_key)
            logger.error(f"❌ Execution failed inside Idempotency block: {str(e)}")
            raise e

```