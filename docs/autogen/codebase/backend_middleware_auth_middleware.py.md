# 📄 ফাইল: backend/middleware/auth_middleware.py

**প্রকার:** .py  
**সাইজ:** 4,600 বাইট  
**আপডেট:** 2026-07-08T18:50:08.137208

---

## কোড

```py


from fastapi import Request
from loguru import logger
from starlette.middleware.base import BaseHTTPMiddleware

from core.security import verify_token

# শেয়ার্ড ইউটিলিটি — টেস্ট এনভায়রনমেন্ট চেক কেন্দ্রীভূত
from utils.environment import is_test_environment


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
            "/ws/chat",
            "/ws",
            # বাংলা মন্তব্য: পাবলিক কনফিগ এন্ডপয়েন্টকে সবার জন্য উন্মুক্ত করা হলো যাতে লগইন ছাড়াও অ্যাক্সেস করা যায়
            "/api/config/public",
        ]
        if request.method == "OPTIONS":
            return await call_next(request)

        matched = (
            request.url.path in public_paths
            or any(request.url.path.startswith(p + "/") for p in public_paths)
        )
        if matched:
            return await call_next(request)

        # রিফ্যাক্টর: লোকাল is_test চেকের বদলে শেয়ার্ড ইউটিলিটি ব্যবহার
        is_test = is_test_environment()
        auth_header = request.headers.get("Authorization")

        if not auth_header or not auth_header.startswith("Bearer "):
            # বাংলা মন্তব্য: টেস্ট মোড বাইপাস লজিক — স্ট্রিম এন্ডপয়েন্ট ছাড়া সব পাথের জন্য অটো-লগইন
            if is_test and not request.url.path.startswith("/api/stream/"):
                request.state.user = {"sub": "admin@supremeai.com", "role": "admin"}
                request.state.tenant_id = "admin@supremeai.com"
                return await call_next(request)

            logger.warning(f"🚨 Blocked unauthorized request to {request.url.path}")
            from fastapi.responses import JSONResponse

            return JSONResponse(
                status_code=401,
                content={"detail": "Missing or invalid Authorization header"},
            )

        token = auth_header.split(" ")[1]

        try:
            if is_test:
                payload = {"sub": "admin@supremeai.com", "role": "admin"}
            else:
                payload = verify_token(token)
            request.state.user = payload
            request.state.tenant_id = payload.get("tenant_id") or payload.get("sub")

            # বাংলা মন্তব্য: শুধু "/api/admin" নয়, prefix ছাড়া রেজিস্টার হওয়া সব
            # অ্যাডমিন-লেভেল রাউটেও (/admin/*, /admin-api/*, /gcp/*) স্ট্রিক্ট রোল চেক
            # প্রয়োগ করা হলো — নয়তো সাধারণ ইউজার টোকেন দিয়ে admin_routes.py এর
            # /admin/rules, /admin/free-tier-override ইত্যাদি অ্যাক্সেস করা যেত (privilege escalation)।
            admin_prefixes = ("/api/admin", "/admin/", "/admin-api", "/gcp/")
            if (
                any(request.url.path.startswith(p) for p in admin_prefixes)
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

        except Exception as e:  # noqa: BLE001
            from fastapi.responses import JSONResponse

            logger.error(f"Token validation failed: {e}")
            return JSONResponse(status_code=401, content={"detail": "Invalid or missing API token."})

        return await call_next(request)

```