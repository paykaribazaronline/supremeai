import json
from fastapi import Request, HTTPException
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware
try:
    from google.cloud import firestore
except ImportError:
    pass
from loguru import logger
from datetime import datetime, timedelta, timezone

class IdempotencyMiddleware(BaseHTTPMiddleware):
    def __init__(self, app):
        super().__init__(app)
        self.collection_name = "idempotency_locks"
        self.db = None
        import os
        import sys
        is_test = (
            "pytest" in sys.modules
            or os.getenv("ENV") == "test"
            or os.getenv("env") == "local"
            or os.getenv("env") == "production"
        )
        if not is_test:
            try:
                self.db = firestore.Client()
            except Exception as e:
                logger.warning(f"Failed to initialize Firestore for IdempotencyMiddleware: {e}")

    async def dispatch(self, request: Request, call_next):
        # শুধুমাত্র POST রিকোয়েস্ট এবং জেনারেশন এন্ডপয়েন্টের জন্য চেক করবে
        if request.method != "POST" or "/api/task" not in request.url.path or not self.db:
            return await call_next(request)

        idempotency_key = request.headers.get("Idempotency-Key")
        if not idempotency_key:
            # ক্রিটিক্যাল এআই জেনারেশন রিকোয়েস্টে কি (Key) না থাকলে রিজেক্ট
            return JSONResponse(
                status_code=400,
                content={"error": "Bad Request: 'Idempotency-Key' header is strictly required for mutating tasks."}
            )

        lock_ref = self.db.collection(self.collection_name).document(idempotency_key)
        lock_doc = lock_ref.get()

        now = datetime.now(timezone.utc)

        if lock_doc.exists:
            lock_data = lock_doc.to_dict()
            status = lock_data.get("status")
            expires_at = lock_data.get("expires_at")
            # Parse expires_at if it's a string (from Firestore)
            if isinstance(expires_at, str):
                expires_at = datetime.fromisoformat(expires_at.replace('Z', '+00:00'))
            # লক এক্সপায়ার হয়ে গেছে কি না চেক
            if expires_at and now > expires_at:
                # এক্সপায়ারড লক ডিলিট করে নতুন ট্রাইয়ের সুযোগ দেওয়া
                lock_ref.delete()
            else:
                if status == "processing":
                    logger.warning(f"🛡️ Idempotency Block: Request {idempotency_key} is already being processed. Dropping concurrent call.")
                    raise HTTPException(status_code=409, detail="Conflict: Request is already being processed. Duplicate execution blocked.")
                
                elif status == "completed":
                    logger.info(f"⚡ Idempotency Hit: Serving cached response for key {idempotency_key} directly from state.")
                    return JSONResponse(
                        status_code=200,
                        content=json.loads(lock_data.get("response_body", "{}")),
                        headers={"X-Cache-Lookup": "HIT - Idempotency Lock"}
                    )

        # ১. রিকোয়েস্ট প্রসেস শুরুর আগে "processing" লক বসানো (Race Condition Prevention)
        lock_ref.set({
            "status": "processing",
            "created_at": now,
            "expires_at": now + timedelta(hours=2) # ২ ঘণ্টার সেফটি উইন্ডো
        })

        try:
            # রিকোয়েস্ট এক্সিকিউট করা
            response = await call_next(request)
            
            # ২. রেসপন্স সফল হলে স্ট্যাটাস "completed" করে সেভ রাখা
            if response.status_code == 200:
                if hasattr(response, 'body_iterator'):
                    response_body = [section async for section in response.body_iterator]
                    response.body_iterator = __import__('anyio').from_thread.run(self._recreate_iterator, response_body)
                else:
                    response_body = [response.body]
                body_str = b"".join(response_body).decode("utf-8")

                lock_ref.update({
                    "status": "completed",
                    "response_body": body_str,
                    "completed_at": datetime.now(timezone.utc)
                })
            else:
                # রিকোয়েস্ট ফেইল করলে লক রিমুভ করা যাতে ইউজার আবার ট্রাই করতে পারে
                lock_ref.delete()
                
            return response

        except Exception as e:
            # ইন্টারনাল এরর হলে লক ডিলিট করা
            lock_ref.delete()
            logger.error(f"❌ Execution failed inside Idempotency block: {str(e)}")
            raise e

    async def _recreate_iterator(self, body):
        for chunk in body:
            yield chunk
