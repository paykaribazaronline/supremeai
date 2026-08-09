# বাংলা কমেন্ট: সুপ্রিম-এআই এর ট্রাস্টেড অরিজিন ভ্যালিডেশন মিডলওয়্যার।
# এটি ওয়াইল্ডকার্ড CORS বাইপাস রোধ করে এবং শুধুমাত্র অনুমোদিত ডোমেইন থেকে এপিআই অ্যাক্সেস নিশ্চিত করে।

import os

from fastapi import Request, status
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware

from core.config import settings
from core.cors_policy import (
    ADMIN_ORIGIN_DENYLIST,
    USER_ORIGIN_DENYLIST,
    resolve_admin_cors_origins,
    resolve_user_cors_origins,
)
from core.logging_config import logger

# বাংলা মন্তব্য: portal-ভিত্তিক ডিফল্ট ট্রাস্টেড অরিজিন — User instance ও Admin instance
# কখনোই একে অপরের ব্রাউজার অরিজিন ট্রাস্ট করবে না (আর্কিটেকচারাল আইসোলেশন)।
USER_DEFAULT_TRUSTED_ORIGINS: frozenset[str] = frozenset(
    {
        "https://supremeai-a.web.app",
        "https://supremeai-backend.onrender.com",
        "https://supremeai-studio-client.onrender.com",
        "https://supremeai-studio.vercel.app",
        "https://supremeai-lac.vercel.app",
    }
)
ADMIN_DEFAULT_TRUSTED_ORIGINS: frozenset[str] = frozenset(
    {
        "https://supremeai-admin.web.app",
        "https://supremeai-admin.onrender.com",
    }
)


class TrustedOriginMiddleware(BaseHTTPMiddleware):
    def __init__(self, app, portal_role: str | None = None):
        super().__init__(app)
        # বাংলা মন্তব্য: portal_role না দিলে SERVICE_ROLE থেকে নেওয়া হয় — main.py এই একই
        # ফ্ল্যাগ দিয়েই app_user vs app_admin বেছে নেয়, তাই দুটো সবসময় সিঙ্কে থাকে।
        self._portal_role_override = portal_role

    @property
    def portal_role(self) -> str:
        if self._portal_role_override:
            return str(self._portal_role_override).lower()
        try:
            role = str(getattr(settings, "service_role", "user") or "user").lower()
        except Exception:
            role = "user"
        return "admin" if role == "admin" else "user"

    @property
    def _default_origins(self) -> set[str]:
        return set(ADMIN_DEFAULT_TRUSTED_ORIGINS if self.portal_role == "admin" else USER_DEFAULT_TRUSTED_ORIGINS)

    @property
    def allowed_origins(self) -> set[str]:
        # বাংলা মন্তব্য: আগে user + admin উভয় CORS origin একসাথে union করা হতো, ফলে User backend
        # অ্যাডমিন কনসোলের preflight-এ (এবং উল্টোটাও) Access-Control-Allow-Origin ফেরত দিত।
        # এখন শুধুমাত্র নিজের portal-এর অরিজিন সেট ট্রাস্ট করা হয় + বিপরীত পাশের denylist প্রয়োগ।
        is_admin = self.portal_role == "admin"
        allowed: set[str] = self._default_origins
        try:
            if is_admin:
                configured = list(getattr(settings, "admin_cors_origins", None) or [])
                allowed = allowed.union(resolve_admin_cors_origins(configured))
            else:
                configured = list(getattr(settings, "user_cors_origins", None) or [])
                if not configured:
                    configured = list(settings.cors_origins or [])
                allowed = allowed.union(resolve_user_cors_origins(configured))

            # বাংলা মন্তব্য: local/dev-এ localhost অরিজিন ব্লক হলে ডেভেলপমেন্ট অচল হয়ে যায় —
            # production/staging-এ এই ছাড় দেওয়া হয় না।
            env = str(getattr(settings, "env", "local") or "local").lower()
            if env not in {"production", "staging"}:
                allowed = allowed.union(
                    {o for o in (settings.cors_origins or []) if "localhost" in o or "127.0.0.1" in o}
                )
        except Exception as exc:
            # Defensive: never let a settings/parse error turn an OPTIONS preflight into a 500
            logger.warning(f"⚠️ TrustedOriginMiddleware failed to read CORS origins, using defaults only: {exc}")

        # বাংলা মন্তব্য: চূড়ান্ত ধাপে বিপরীত portal-এর অরিজিন ও wildcard ছেঁকে ফেলা হয় (defense in depth)।
        denylist = USER_ORIGIN_DENYLIST if is_admin else ADMIN_ORIGIN_DENYLIST
        return {o for o in allowed if o and o != "*" and o not in denylist}

    async def dispatch(self, request: Request, call_next):
        _env = os.getenv("ENV", "development").lower()
        origin = request.headers.get("Origin")
        # বাংলা মন্তব্য: allowed_origins (settings.cors_origins সহ) শুধু তখনই কম্পিউট করা হয়
        # যখন request-এ আসলে Origin হেডার আছে -- না হলে (server-to-server call, health
        # check, same-origin request) CORS_ORIGINS মিসকনফিগার/আনকনফিগারড থাকলেও
        # অকারণে প্রতিটা request crash করবে না।
        allowed = self.allowed_origins if origin else set()

        # বাংলা মন্তব্য: OPTIONS preflight রিকোয়েস্ট সরাসরি 200 OK রেসপন্স ও ক্লায়েন্টের প্রয়োজনীয় CORS হেডার ফেরত পাঠাবে।
        # Defense-in-depth: অনুমোদিত অরিজিনের preflight সবসময় 200 দেবে এবং ভিতরের Auth/APIKey/AutonoGuard/Honeypot/Chaos
        # মিডলওয়্যারে যাবে না। আগে শর্ত ছিল `origin in allowed` — ফলে যদি admin origin allowlist-এ না থাকতো,
        # preflight ভিতরের AuthMiddleware-এ গিয়ে 401/403 দিত ও ব্রাউজার CORS preflight fail করত ("doesn't have HTTP ok status")।
        # এখন যেকোনো OPTIONS (যার Access-Control-Request-Method আছে = আসল preflight) সরাসরি 200 দেওয়া হয়;
        # শুধু অনুমোদিত অরিজিনের জন্যই Access-Control-Allow-Origin হেডার সেট করা হয়, বাকিগুলো 200 দেয় কিন্তু
        # allow-origin দেয় না — তাই ব্রাউজার তখনই ব্লক করে, কিন্তু preflight নিজে 200 পায় (auth middleware-এর 401 নয়)।
        if request.method == "OPTIONS":
            requested_headers = request.headers.get(
                "Access-Control-Request-Headers",
                "Content-Type, Authorization, X-Requested-With, X-API-Key, Accept, Origin, X-Device-Fingerprint, X-CSRF-Token, X-JIT-OTP, X-Request-ID, X-Tenant-ID, X-Correlation-ID",
            )
            headers = {
                "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH",
                "Access-Control-Allow-Headers": requested_headers,
            }
            if not origin or origin in allowed:
                headers["Access-Control-Allow-Origin"] = origin or "*"
                headers["Access-Control-Allow-Credentials"] = "true"
            return JSONResponse(
                status_code=status.HTTP_200_OK,
                content={"status": "ok"},
                headers=headers,
            )

        if os.getenv("ALLOW_TEST_ORIGIN_BYPASS", "").lower() == "true" or _env in {"test", "testing", "ci"}:
            pass
        elif origin and origin not in allowed:
            client_ip = request.client.host if request.client else "unknown"
            logger.critical(
                f"🔥 CSRF ALERT: Unauthorized Origin Access Blocked! Malicious Origin: {origin} from IP: {client_ip}"
            )
            return JSONResponse(
                status_code=status.HTTP_403_FORBIDDEN,
                content={"detail": "Cross-Origin Request Blocked. Device identity unauthorized."},
            )

        # বাংলা মন্তব্য: পাবলিক পাথ (যেমন /api/v1/health) সবসময় হোস্ট ভেরিফিকেশন বাইপাস করবে।
        public_paths = settings.supremeai_public_paths
        if any(request.url.path == p or request.url.path.startswith(p) for p in public_paths):
            response = await call_next(request)
            if origin and origin in allowed:
                response.headers["Access-Control-Allow-Origin"] = origin
                response.headers["Access-Control-Allow-Credentials"] = "true"
            return response

        # বাংলা মন্তব্য: হোস্ট হেডার ভ্যালিডেশন
        host_header = request.headers.get("Host")
        is_allowed = True
        if host_header:
            allowed_hosts = set(settings.allowed_hosts)
            allowed_hosts.add("testserver")
            allowed_hosts.add("localhost")
            allowed_hosts.add("127.0.0.1")
            is_allowed = host_header in allowed_hosts or any(host_header.endswith("." + h) for h in allowed_hosts)

        if host_header and not is_allowed:
            logger.critical(f"🚨 Security Intrusion: Host Header Tampering Detected -> {host_header}")
            return JSONResponse(
                status_code=status.HTTP_403_FORBIDDEN,
                content={"detail": "Host verification failure."},
            )

        # বাংলা কমেন্ট: ভ্যালিডেশন সাকসেসফুল হলে রিকোয়েস্ট পরবর্তী প্রসেসে পাস হবে
        response = await call_next(request)

        # Security Hardening Headers (Zero-Trust Guard)
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["X-Frame-Options"] = "SAMEORIGIN"
        response.headers["X-XSS-Protection"] = "1; mode=block"
        response.headers["Referrer-Policy"] = "strict-origin-when-cross-origin"
        if request.url.scheme == "https":
            response.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"

        # বাংলা মন্তব্য: জিরো-গ্যাপ CORS হেডার ইনজেকশন — Cache-Control, Pragma সহ ফ্রন্টএন্ড হেডার অনুমোদিত
        if origin and origin in allowed:
            response.headers["Access-Control-Allow-Origin"] = origin
            response.headers["Access-Control-Allow-Credentials"] = "true"
            response.headers["Access-Control-Allow-Methods"] = "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH"
            response.headers["Access-Control-Allow-Headers"] = (
                "Content-Type, Authorization, X-Requested-With, X-API-Key, Accept, Origin, "
                "X-Device-Fingerprint, X-CSRF-Token, X-JIT-OTP, X-Request-ID, X-Tenant-ID, "
                "X-Correlation-ID, Cache-Control, Pragma, *"
            )

        return response
