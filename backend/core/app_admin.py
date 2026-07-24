"""SupremeAI 2.0 — Admin API entrypoint. Admin dashboard + Anti-Hacking Agent only.

বাংলা মন্তব্য: অ্যাডমিন এপিআই এন্ট্রি পয়েন্ট যা শুধুমাত্র অ্যাডমিন প্যানেল এবং সিকিউরিটি রাউটগুলো এক্সপোজ করে।
"""

from api.routers import include_admin_routers
from core.app_builder import build_app_shell, router_health_check
from core.config import settings
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from middleware.anti_hacking import AntiHackingContextMiddleware

app: FastAPI = build_app_shell(title="SupremeAI Admin API")

# বাংলা মন্তব্ট: Production-এ ADMIN_CORS_ORIGINS ফাঁকা (empty) হলে
# বুট-টাইমে crash এড়াতে ডিফল্ট অ্যাডমিন origin অটো-পপুলেট করা হচ্ছে।
# শুধুমাত্র অ্যাডমিন কনসোল origin — Vercel/Netlify user client নয়।
if settings.env == "production":
    if not settings.admin_cors_origins:
        from loguru import logger

        logger.warning(
            "⚠️ Production Admin CORS drift detected. Auto-populating default trusted admin origins."
        )
        settings.admin_cors_origins = [
            "https://supremeai-admin.web.app",
            "https://supremeai-backend.onrender.com",
        ]
    if "*" in settings.admin_cors_origins:
        raise RuntimeError(
            "🚨 SECURITY: Wildcard '*' is strictly prohibited in production Admin CORS. Set ADMIN_CORS_ORIGINS."
        )

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.admin_cors_origins,
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

# Anti-Hacking Agent hook — runs before routes, on Admin API only
app.add_middleware(AntiHackingContextMiddleware)

include_admin_routers(app)

# বাংলা মন্তব্য: অ্যাডমিন এপিআইতে ইউজারের মতো ২০টি রাউটার লোড হয় না (কেবল ADMIN_ROUTERS লোড হয়)।
# তাই এখানে মিনিমাম ৫টি রাউটের উপস্থিতি চেক করা হচ্ছে যাতে প্রসেসটি ক্র্যাশ না করে।
router_health_check(app, expected_count=5)
