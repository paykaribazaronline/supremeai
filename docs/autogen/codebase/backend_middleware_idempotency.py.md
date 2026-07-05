# 📄 ফাইল: backend/middleware/idempotency.py

**প্রকার:** .py  
**সাইজ:** 6,024 বাইট  
**আপডেট:** 2026-07-05T15:31:52.351681

---

## কোড

```py
import json
from datetime import UTC
from datetime import datetime
from datetime import timedelta

from fastapi import HTTPException
from fastapi import Request
from fastapi.responses import JSONResponse
from loguru import logger
from starlette.middleware.base import BaseHTTPMiddleware

from utils.environment import is_test_environment

# শেয়ার্ড ইউটিলিটি — Firestore ইনিশিয়ালাইজেশন ও টেস্ট ডিটেকশন কেন্দ্রীভূত
from utils.firestore_helpers import get_firestore_db


class IdempotencyMiddleware(BaseHTTPMiddleware):
    def __init__(self, app):
        super().__init__(app)
        self.collection_name = "idempotency_locks"
        # রিফ্যাক্টর: সরাসরি firestore.Client() এর বদলে শেয়ার্ড হেল্পার ব্যবহার
        import os
        is_local_or_prod = os.getenv("ENV") in ("local", "production")
        if is_test_environment() or is_local_or_prod:
            self.db = None
        else:
            self.db = get_firestore_db()

    async def dispatch(self, request: Request, call_next):
        # শুধুমাত্র POST রিকোয়েস্ট এবং জেনারেশন এন্ডপয়েন্টের জন্য চেক করবে
        if (
            request.method != "POST"
            or "/api/task" not in request.url.path
            or not self.db
        ):
            return await call_next(request)

        idempotency_key = request.headers.get("Idempotency-Key")
        if not idempotency_key:
            # ক্রিটিক্যাল এআই জেনারেশন রিকোয়েস্টে কি (Key) না থাকলে রিজেক্ট
            return JSONResponse(
                status_code=400,
                content={
                    "error": "Bad Request: 'Idempotency-Key' header is strictly required for mutating tasks."
                },
            )

        lock_ref = self.db.collection(self.collection_name).document(idempotency_key)
        lock_doc = lock_ref.get()

        now = datetime.now(UTC)

        if lock_doc.exists:
            lock_data = lock_doc.to_dict()
            status = lock_data.get("status")
            expires_at = lock_data.get("expires_at")
            # Parse expires_at if it's a string (from Firestore)
            if isinstance(expires_at, str):
                expires_at = datetime.fromisoformat(expires_at.replace("Z", "+00:00"))
            # লক এক্সপায়ার হয়ে গেছে কি না চেক
            if expires_at and now > expires_at:
                # এক্সপায়ারড লক ডিলিট করে নতুন ট্রাইয়ের সুযোগ দেওয়া
                lock_ref.delete()
            elif status == "processing":
                logger.warning(
                    f"🛡️ Idempotency Block: Request {idempotency_key} is already being processed. Dropping concurrent call."
                )
                raise HTTPException(
                    status_code=409,
                    detail="Conflict: Request is already being processed. Duplicate execution blocked.",
                )

            elif status == "completed":
                logger.info(
                    f"⚡ Idempotency Hit: Serving cached response for key {idempotency_key} directly from state."
                )
                return JSONResponse(
                    status_code=200,
                    content=json.loads(lock_data.get("response_body", "{}")),
                    headers={"X-Cache-Lookup": "HIT - Idempotency Lock"},
                )

        # ১. রিকোয়েস্ট প্রসেস শুরুর আগে "processing" লক বসানো (Race Condition Prevention)
        lock_ref.set(
            {
                "status": "processing",
                "created_at": now,
                "expires_at": now + timedelta(hours=2),  # ২ ঘণ্টার সেফটি উইন্ডো
            }
        )

        try:
            # রিকোয়েস্ট এক্সিকিউট করা
            response = await call_next(request)

            # ২. রেসপন্স সফল হলে স্ট্যাটাস "completed" করে সেভ রাখা
            if response.status_code == 200:
                if hasattr(response, "body_iterator"):
                    response_body = [
                        section async for section in response.body_iterator
                    ]
                    from starlette.responses import Response
                    body_bytes = b"".join(response_body)
                    response = Response(
                        content=body_bytes,
                        status_code=response.status_code,
                        headers=dict(response.headers),
                        media_type=response.media_type
                    )
                else:
                    response_body = [response.body]
                body_str = b"".join(response_body).decode("utf-8")

                lock_ref.update(
                    {
                        "status": "completed",
                        "response_body": body_str,
                        "completed_at": datetime.now(UTC),
                    }
                )
            else:
                # রিকোয়েস্ট ফেইল করলে লক রিমুভ করা যাতে ইউজার আবার ট্রাই করতে পারে
                lock_ref.delete()

            return response

        except Exception as e:
            # ইন্টারনাল এরর হলে লক ডিলিট করা
            lock_ref.delete()
            logger.error(f"❌ Execution failed inside Idempotency block: {str(e)}")
            raise e


```