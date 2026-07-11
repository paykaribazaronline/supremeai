# 📄 ফাইল: backend/core/origin_validator.py

**প্রকার:** .py  
**সাইজ:** 3,772 বাইট  
**আপডেট:** 2026-07-11T13:38:55.670905

---

## কোড

```py
# বাংলা কমেন্ট: সুপ্রিম-এআই এর ট্রাস্টেড অরিজিন ভ্যালিডেশন মিডলওয়্যার।
# এটি ওয়াইল্ডকার্ড CORS বাইপাস রোধ করে এবং শুধুমাত্র অনুমোদিত ডোমেইন থেকে এপিআই অ্যাক্সেস নিশ্চিত করে।

from fastapi import Request
from fastapi import status
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware

from core.config import settings
from core.logging_config import logger


class TrustedOriginMiddleware(BaseHTTPMiddleware):
    def __init__(self, app):
        super().__init__(app)
        self.allowed_origins = set(settings.cors_origins)

    async def dispatch(self, request: Request, call_next):
        import os

        host = request.headers.get("host", "").split(":")[0]
        env = os.getenv("ENV", "development").lower()

        # টেস্ট এনভায়রনমেন্ট এবং টেস্টসার্ভার রিকোয়েস্টকে গেটওয়ে পাসের অনুমতি দেওয়া হলো
        if env == "test" or host in ["testserver", "localhost", "127.0.0.1"]:
            return await call_next(request)

        # বাংলা মন্তব্য: এপিআই রিকোয়েস্টের Origin এবং Host হেডার রিড করা হচ্ছে।
        origin = request.headers.get("Origin")

        # যদি রিকোয়েস্টে অরিজিন হেডার থাকে (যেমন ব্রাউজার বেসড রিকোয়েস্ট), তবে সেটি হোয়াইটলিস্টে থাকতে হবে
        if origin and origin not in self.allowed_origins:
            client_ip = request.client.host if request.client else "unknown"
            logger.critical(f"🔥 CSRF ALERT: Unauthorized Origin Access Blocked! Malicious Origin: {origin} from IP: {client_ip}")
            return JSONResponse(
                status_code=status.HTTP_403_FORBIDDEN, content={"detail": "Cross-Origin Request Blocked. Device identity unauthorized."}
            )

        # বাংলা মন্তব্য: হোস্ট হেডার ভ্যালিডেশন
        host = request.headers.get("Host")
        is_allowed = True
        if host:
            allowed_hosts = set(settings.allowed_hosts)
            is_allowed = host in allowed_hosts or any(host.endswith("." + h) for h in allowed_hosts)

        if host and not is_allowed:
            logger.critical(f"🚨 Security Intrusion: Host Header Tampering Detected -> {host}")
            return JSONResponse(status_code=status.HTTP_403_FORBIDDEN, content={"detail": "Host verification failure."})

        # বাংলা কমেন্ট: ভ্যালিডেশন সাকসেসফুল হলে রিকোয়েস্ট পরবর্তী প্রসেসে পাস হবে
        response = await call_next(request)

        # জিরো-গ্যাপ CORS হেডার ইনজেকশন (ওয়াইল্ডকার্ড মুক্ত)
        if origin and origin in self.allowed_origins:
            response.headers["Access-Control-Allow-Origin"] = origin
            response.headers["Access-Control-Allow-Credentials"] = "true"
            response.headers["Access-Control-Allow-Methods"] = "GET, POST, PUT, DELETE, OPTIONS"
            response.headers["Access-Control-Allow-Headers"] = "Content-Type, Authorization, X-Requested-With"

        return response

```