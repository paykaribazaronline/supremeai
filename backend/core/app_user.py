"""SupremeAI 2.0 — User API entrypoint. Chat/user-facing routes only.

বাংলা মন্তব্য: ইউজার এপিআই এন্ট্রি পয়েন্ট যা শুধুমাত্র চ্যাট ও ইউজার-ফেসিং রাউটগুলো এক্সপোজ করে।
"""

from api.routers import include_user_routers
from core.app_builder import build_app_shell, router_health_check
from core.config import settings
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

app: FastAPI = build_app_shell(title="SupremeAI User API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.user_cors_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"],
    allow_headers=[
        "Authorization",
        "Content-Type",
        "X-Request-ID",
        "X-Tenant-ID",
        "X-API-Key",
        "X-Correlation-ID",
    ],
)

if settings.env == "production":
    if not settings.user_cors_origins:
        from loguru import logger

        logger.warning(
            "⚠️ Production User CORS drift detected. Auto-populating default trusted production origins."
        )
        settings.user_cors_origins = [
            "https://supremeai-studio.vercel.app",
            "https://tiny-stroopwafel-2d981c.netlify.app",
            "https://supremeai-admin.web.app",
            "https://supremeai-backend.onrender.com",
        ]
    if "*" in settings.user_cors_origins:
        raise RuntimeError(
            "🚨 SECURITY: Wildcard '*' is strictly prohibited in production User CORS. Set USER_CORS_ORIGINS."
        )

include_user_routers(app)
router_health_check(app)
