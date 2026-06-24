from fastapi import Request, HTTPException
from starlette.middleware.base import BaseHTTPMiddleware
from backend.core.security import verify_token
from loguru import logger

class ZeroTrustAuthMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        # পাবলিক রাউটগুলো বাইপাস করা (যেমন লগইন, হেলথ চেক)
        public_paths = ["/docs", "/openapi.json", "/health", "/api/auth/login", "/api/admin/login", "/api/admin/verify"]
        if any(request.url.path.startswith(path) for path in public_paths):
            return await call_next(request)

        auth_header = request.headers.get("Authorization")
        
        if not auth_header or not auth_header.startswith("Bearer "):
            logger.warning(f"🚨 Blocked unauthorized request to {request.url.path}")
            # 'test-token' বাইপাস চিরতরে রিমুভ করা হয়েছে
            raise HTTPException(status_code=401, detail="Missing or invalid Authorization header")

        token = auth_header.split(" ")[1]
        
        try:
            # ক্রিপ্টোগ্রাফিক ভেরিফিকেশন কল
            payload = verify_token(token)
            request.state.user = payload
            
            # অ্যাডমিন রাউটের জন্য স্ট্রিক্ট রোল চেক
            if request.url.path.startswith("/api/admin") and payload.get("role") != "admin":
                logger.critical(f"🔒 Privilege Escalation Blocked for user: {payload.get('sub')}")
                raise HTTPException(status_code=403, detail="Insufficient privileges. Admin access required.")
                
        except Exception as e:
            raise HTTPException(status_code=401, detail=str(e))

        return await call_next(request)
