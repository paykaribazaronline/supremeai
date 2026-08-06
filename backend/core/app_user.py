"""SupremeAI 2.0 — User API entrypoint. Chat/user-facing routes only.

বাংলা মন্তব্য: ইউজার এপিআই এন্ট্রি পয়েন্ট যা শুধুমাত্র চ্যাট ও ইউজার-ফেসিং রাউটগুলো এক্সপোজ করে।
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from api.routers import include_user_routers
from core.app_builder import build_app_shell, router_health_check
from core.config import settings

app: FastAPI = build_app_shell(title="SupremeAI User API")

if settings.env == "production":
    # বাংলা মন্তব্য: প্রোডাকশনে CORS অরিজিনে '*' থাকলে বা খালি হলে ক্র্যাশ এড়াতে সেফ প্রোডাকশন অরিজিন সেট করা হচ্ছে
    if not settings.user_cors_origins or "*" in settings.user_cors_origins:
        from loguru import logger

        logger.warning("⚠️ Production User CORS wildcard/drift detected. Setting default trusted production origins.")
        settings.user_cors_origins = [origin for origin in (settings.user_cors_origins or []) if origin != "*"] + [
            "https://supremeai-lac.vercel.app",
            "https://supremeai-studio.vercel.app",
            "https://tiny-stroopwafel-2d981c.netlify.app",
            "https://supremeai-admin.web.app",
            "https://supremeai-a.web.app",
            "https://supremeai-backend.onrender.com",
            "https://supremeai-backend-08zd.onrender.com",
        ]

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

include_user_routers(app)
router_health_check(app)
