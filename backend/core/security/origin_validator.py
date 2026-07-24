# বাংলা কমেন্ট: সুপ্রিম-এআই এর ট্রাস্টেড অরিজিন ভ্যালিডেশন মিডলওয়্যার।
# এটি ওয়াইল্ডকার্ড CORS বাইপাস রোধ করে এবং শুধুমাত্র অনুমোদিত ডোমেইন থেকে এপিআই অ্যাক্সেস নিশ্চিত করে।

from core.config import settings
from core.logging_config import logger
from fastapi import Request, status
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware


class TrustedOriginMiddleware(BaseHTTPMiddleware):
    def __init__(self, app):
        super().__init__(app)
        self._default_origins = {
            "https://supremeai-admin.web.app",
            "https://supremeai-backend.onrender.com",
            "https://supremeai-admin.onrender.com",
            "https://supremeai-studio-client.onrender.com",
            "https://supremeai-studio.vercel.app",
            "https://supremeai-lac.vercel.app",
        }

    @property
    def allowed_origins(self) -> set[str]:
        configured = set(settings.cors_origins) if settings.cors_origins else set()
        return configured.union(self._default_origins)

    async def dispatch(self, request: Request, call_next):
        import os

        request.headers.get("host", "").split(":")[0]
        env = os.getenv("ENV", "development").lower()
        origin = request.headers.get("Origin")
        allowed = self.allowed_origins

        # বাংলা মন্তব্য: OPTIONS preflight রিকোয়েস্ট সরাসরি 200 OK রেসপন্স ও CORS হেডার ফেরত পাঠাবে
        if request.method == "OPTIONS":
            if not origin or origin in allowed:
                headers = {
                    "Access-Control-Allow-Origin": origin or "*",
                    "Access-Control-Allow-Credentials": "true",
                    "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH",
                    "Access-Control-Allow-Headers": "Content-Type, Authorization, X-Requested-With, X-API-Key, Accept, Origin",
                }
                return JSONResponse(
                    status_code=status.HTTP_200_OK,
                    content={"status": "ok"},
                    headers=headers,
                )

        # বাংলা মন্তব্য: Host header client-controlled — শুধু Host string দেখে বাইপাস করা যাবে না।
        # টেস্ট বাইপাস এখন কেবল একটি explicit, config-gated flag দিয়ে নিয়ন্ত্রিত হয় (Patch 7 fix)।
        is_explicit_test_mode = env == "test" and getattr(
            settings, "allow_test_origin_bypass", False
        )
        if is_explicit_test_mode:
            response = await call_next(request)
            if origin:
                if origin in allowed or origin in {
                    "http://testserver",
                    "http://localhost",
                    "http://127.0.0.1",
                }:
                    response.headers["Access-Control-Allow-Origin"] = origin
                    response.headers["Access-Control-Allow-Credentials"] = "true"
            return response

        # বাংলা মন্তব্য: পাবলিক পাথ (যেমন /api/v1/health) সবসময় হোস্ট ভেরিফিকেশন বাইপাস করবে।
        public_paths = settings.supremeai_public_paths
        if any(
            request.url.path == p or request.url.path.startswith(p)
            for p in public_paths
        ):
            response = await call_next(request)
            if origin and origin in allowed:
                response.headers["Access-Control-Allow-Origin"] = origin
                response.headers["Access-Control-Allow-Credentials"] = "true"
            return response

        # যদি রিকোয়েস্টে অরিজিন হেডার থাকে (যেমন ব্রাউজার বেসড রিকোয়েস্ট), তবে সেটি হোয়াইটলিস্টে থাকতে হবে
        if origin and origin not in allowed:
            client_ip = request.client.host if request.client else "unknown"
            logger.critical(
                f"🔥 CSRF ALERT: Unauthorized Origin Access Blocked! Malicious Origin: {origin} from IP: {client_ip}"
            )
            return JSONResponse(
                status_code=status.HTTP_403_FORBIDDEN,
                content={
                    "detail": "Cross-Origin Request Blocked. Device identity unauthorized."
                },
            )

        # বাংলা মন্তব্য: হোস্ট হেডার ভ্যালিডেশন
        host_header = request.headers.get("Host")
        is_allowed = True
        if host_header:
            allowed_hosts = set(settings.allowed_hosts)
            is_allowed = host_header in allowed_hosts or any(
                host_header.endswith("." + h) for h in allowed_hosts
            )

        if host_header and not is_allowed:
            logger.critical(
                f"🚨 Security Intrusion: Host Header Tampering Detected -> {host_header}"
            )
            return JSONResponse(
                status_code=status.HTTP_403_FORBIDDEN,
                content={"detail": "Host verification failure."},
            )

        # বাংলা কমেন্ট: ভ্যালিডেশন সাকসেসফুল হলে রিকোয়েস্ট পরবর্তী প্রসেসে পাস হবে
        response = await call_next(request)

        # জিরো-গ্যাপ CORS হেডার ইনজেকশন (ওয়াইল্ডকার্ড মুক্ত)
        if origin and origin in allowed:
            response.headers["Access-Control-Allow-Origin"] = origin
            response.headers["Access-Control-Allow-Credentials"] = "true"
            response.headers["Access-Control-Allow-Methods"] = (
                "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH"
            )
            response.headers["Access-Control-Allow-Headers"] = (
                "Content-Type, Authorization, X-Requested-With, X-API-Key, Accept, Origin"
            )

        return response
