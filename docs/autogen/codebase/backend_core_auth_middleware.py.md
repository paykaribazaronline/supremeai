# 📄 ফাইল: backend/core/auth_middleware.py

**প্রকার:** .py  
**সাইজ:** 13,544 বাইট  
**আপডেট:** 2026-07-11T11:05:10.177352

---

## কোড

```py
from __future__ import annotations

import os
import secrets

from fastapi import HTTPException
from fastapi import Request
from fastapi import status
from fastapi.responses import JSONResponse
from jose import JWTError
from jose import jwt
from jose.exceptions import ExpiredSignatureError
from loguru import logger

from core.config import settings
from utils.environment import is_test_environment


def _get_bearer_token(headers) -> str | None:
    for k, v in headers:
        if k.lower() == b"authorization":
            auth = v.decode("utf-8")
            parts = auth.split()
            if len(parts) == 2 and parts[0].lower() == "bearer":
                return parts[1]
    return None


class AuthMiddleware:
    def __init__(self, app) -> None:
        self.app = app

    async def __call__(self, scope, receive, send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        path = scope.get("path", "")
        # বাংলা মন্তব্য: ASGI request scope variants-এর জন্য path resolution fallback যোগ করা হলো।
        if not path and scope.get("raw_path"):
            # বাংলা মন্তব্য: P1 Fix — Exception Swallowing পরিহার করে নির্দিষ্ট এক্সেপশন ক্যাচ করা হলো
            try:
                path = scope["raw_path"].decode("utf-8").split("?")[0]
            except (UnicodeDecodeError, IndexError, KeyError) as e:
                logger.error(f"Failed to parse raw_path: {e}")
                pass
        headers = scope.get("headers", [])

        # Strict admin origin check to prevent security blast radius breach
        admin_paths = ["/admin/", "/admin-api/", "/gcp/"]
        is_admin_path = any(path.startswith(admin_path) for admin_path in admin_paths) or path in {"/admin/rules", "/admin/cloud-distribution"}

        # বাংলা মন্তব্য: টেস্ট এনভায়রনমেন্টে থাকলে authentication bypass করার লজিক পুনঃস্থাপন করা হলো
        is_test = is_test_environment()

        if is_admin_path and not is_test:
            origin = ""
            referer = ""
            for k, v in headers:
                if k.lower() == b"origin":
                    origin = v.decode("utf-8")
                elif k.lower() == b"referer":
                    referer = v.decode("utf-8")

            # বাংলা মন্তব্য: P0 Fix — Admin domain allowlist, strict matching।
            # Production-এ http://localhost: সম্পূর্ণ নিষিদ্ধ।
            # আগের বাগ: `if not is_admin_domain and (origin or referer):` — এই শর্তে
            # origin ও referer উভয়ই ফাঁকা থাকলে (যেমন সরাসরি curl) bypass হতো।
            # এখন: origin/referer ফাঁকা থাকলেও admin path block করা হচ্ছে।
            ALLOWED_ADMIN_ORIGINS = {
                "https://supremeai-admin.web.app",
                "https://supremeai-admin.web.app/",
            }

            def _is_allowed_admin_domain(value: str) -> bool:
                cleaned = value.lower().strip()
                if not cleaned:
                    return False
                # Production-এ localhost সম্পূর্ণ নিষিদ্ধ
                if getattr(settings, "env", "local") == "production":
                    return cleaned.rstrip("/") in {o.rstrip("/") for o in ALLOWED_ADMIN_ORIGINS}
                # Non-production: localhost allowed on any port
                return (
                    cleaned.rstrip("/") in {o.rstrip("/") for o in ALLOWED_ADMIN_ORIGINS}
                    or cleaned.startswith("http://localhost:")
                    or cleaned.startswith("https://localhost:")
                )

            is_admin_domain = _is_allowed_admin_domain(origin) or _is_allowed_admin_domain(referer)

            # বাংলা মন্তব্য: Origin/Referer ফাঁকা হলেও block — `and (origin or referer)` শর্ত সরানো হয়েছে।
            # এটি সরাসরি curl বা internal service call দিয়ে admin bypass আটকায়।
            if not is_admin_domain:
                logger.warning(f"Forbidden admin access to {path} | origin='{origin}' referer='{referer}' — no authorized domain header.")
                response = JSONResponse(
                    status_code=403,
                    content={"detail": "Forbidden: Admin endpoints are restricted to the admin console domain."},
                )
                await response(scope, receive, send)
                return

            # --- Agentic Security Check: Verify Backend JWT for Admin Routes ---
            token = _get_bearer_token(headers)
            if not token:
                logger.warning(f"Missing bearer token for admin path: {path}")
                response = JSONResponse(
                    status_code=401,
                    content={"detail": "Missing Authorization Token for admin route."},
                    headers={"WWW-Authenticate": "Bearer"},
                )
                await response(scope, receive, send)
                return
            try:
                jwt_secret = settings.jwt_secret
                decoded = jwt.decode(token, jwt_secret, algorithms=["HS256"])
                if decoded.get("role") not in {"admin", "master_admin"}:
                    response = JSONResponse(
                        status_code=403,
                        content={"detail": "Forbidden: User does not have admin role."},
                    )
                    await response(scope, receive, send)
                    return
            except Exception as e:  # noqa: BLE001
                logger.error(f"Admin JWT validation failed: {e}")
                response = JSONResponse(
                    status_code=401,
                    content={"detail": "Invalid authorization token"},
                )
                await response(scope, receive, send)
                return

        cleaned_path = path.lower().rstrip("/")
        public_paths = {
            "/health",
            "/actuator/health",
            "/docs",
            "/redoc",
            "/openapi.json",
            "/api/admin/login",
            "/api/admin/verify",
            "/api/admin/firebase-login",
            "/api/admin/firebase-totp-setup",
            "/api/admin/firebase-totp-verify",
            "/orchestrator/tick",
            # বাংলা মন্তব্য: পাবলিক কনফিগ এবং টাস্ক স্ট্রিম এন্ডপয়েন্ট সবার জন্য উন্মুক্ত করা হলো
            "/api/config/public",
            "/api/task/stream",
            "/api/health",
        }
        # বাংলা মন্তব্য: public paths dynamically matching using substring or clean compare.
        is_public = (
            cleaned_path in public_paths
            or any(cleaned_path.startswith(p + "/") for p in public_paths)
            or path.startswith("/static")
            or not cleaned_path
        )
        logger.debug(f"[AuthMiddleware] path='{path}' cleaned_path='{cleaned_path}' is_public={is_public}")
        if is_public:
            await self.app(scope, receive, send)
            return

        token = _get_bearer_token(headers)

        if is_test:
            expected_token = os.getenv("SUPREMEAI_API_TOKEN")
            # বাংলা মন্তব্য: P1 Fix — test bypass logic সংশোধন করা হলো।
            # আগে: expected_token সেট থাকলে এবং match না করলে `pass` (fallthrough) হতো —
            # তারপর নিচের `enabled` check-এ পড়ে production logic execute হতো।
            # এখন: test mode-এ expected_token না থাকলে বা match করলে pass, না করলে block।
            if not expected_token or secrets.compare_digest(token or "", expected_token):
                await self.app(scope, receive, send)
                return
            # Token mismatch in test mode — fall through to normal auth

        enabled = bool(os.getenv("SUPREMEAI_API_TOKEN"))
        if not enabled:
            if settings.env == "production":
                raise RuntimeError("SUPREMEAI_API_TOKEN must be set in production — fail-closed enforced.")
            await self.app(scope, receive, send)
            return

        expected = os.getenv("SUPREMEAI_API_TOKEN") or ""
        if not token or not secrets.compare_digest(token, expected):
            logger.warning(f"Unauthorized access attempt to {path}")
            response = JSONResponse(
                status_code=401,
                content={"detail": "Invalid or missing API token."},
            )
            await response(scope, receive, send)
            return
        await self.app(scope, receive, send)


# বাংলা কমেন্ট: সুপ্রিম-এআই এর ফেল-ক্লোজড অথেনটিকেশন এনফোর্সমেন্ট ইঞ্জিন।
# যেকোনো ভেরিফিকেশন ফেইলিওর বা এক্সেপশনে এটি সরাসরি রিকোয়েস্ট হার্ড-ব্লক করে (Fail-Closed)।


async def verify_admin_session_fail_closed(request: Request) -> dict:
    """
    টোকেন অথেনটিকেশন এবং ডিকোডিং মেকানিজম।
    সামান্যতম গ্যাপ বা এক্সেপশন দেখা দিলে এটি সরাসরি Fail-Closed প্রোটোকল ট্রিগার করে।
    """  # noqa: W291
    # বাংলা কমেন্ট: Authorization হেডার এক্সট্রাকশন
    auth_header = request.headers.get("Authorization")
    if not auth_header or not auth_header.startswith("Bearer "):
        client_ip = request.client.host if request.client else "unknown"
        logger.warning(f"🔒 Access Denied: Missing or malformed Bearer token from IP: {client_ip}")
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Authentication credentials missing or malformed.")

    token = auth_header.split(" ")[1]
    jwt_secret = settings.jwt_secret  # ক্লাউড সিক্রেট ভল্ট থেকে লোডকৃত

    if not jwt_secret:
        logger.critical("🔥 Security Emergency: SUPREMEAI_JWT_SECRET is unconfigured! Fail-Closed triggered.")
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Security authentication cluster is hard-locked.")

    try:
        # P2 ফিক্স: টোকেন ডিকোড এবং ভ্যালিডেশন ওয়ান-শট এক্সিকিউশন
        payload = jwt.decode(token, jwt_secret, algorithms=["HS256"])

        user_id = payload.get("sub")
        role = payload.get("role")

        # বাংলা মন্তব্য: ০% গ্যাপ পলিসি — পেলোডে যদি প্রয়োজনীয় ফিল্ড মিসিং থাকে বা রোল অসঙ্গতি থাকে, সরাসরি রিজেক্ট।
        # এখানে 'admin' এবং 'master_admin' উভয় রোলকেই অনুমতি প্রদান করা হলো।
        if not user_id or role not in {"admin", "master_admin"}:
            logger.critical(f"🚨 Security Alert: Token payload identity mismatch or unauthorized role: {role}")
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Administrative identity verification failed.")

        logger.success(f"🔱 Admin Session Authorized for User: {user_id}")
        return payload

    except ExpiredSignatureError as jwt_err:
        logger.warning(f"🔒 Fail-Closed: Expired JWT token blocked -> {str(jwt_err)}")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Session has expired or token is invalid.",
        ) from None

    except JWTError as jwt_err:
        logger.warning(f"🔒 Fail-Closed: Invalid JWT token blocked -> {str(jwt_err)}")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Session has expired or token is invalid.",
        ) from None

    except Exception as fatal_exception:  # noqa: BLE001
        # ❌ পুরানো ভুল পদ্ধতি (Fail-Open): return None বা পাস করা
        # ✅ নতুন সঠিক পদ্ধতি: P1/P2 Fail-Closed এনফোর্সমেন্ট। যেকোনো আননোন ক্র্যাশে রিকোয়েস্ট হার্ড-ব্লক।
        logger.critical(f"🔥 FATAL AUTH EXCEPTION: Dynamic crash detected during auth flow -> {str(fatal_exception)}")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Security handshake verification failure. Access safely denied.",
        ) from None

```