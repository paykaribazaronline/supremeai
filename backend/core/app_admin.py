"""SupremeAI 2.0 — Admin API entrypoint. Admin dashboard + Anti-Hacking Agent only.

বাংলা মন্তব্য: অ্যাডমিন এপিআই এন্ট্রি পয়েন্ট যা শুধুমাত্র অ্যাডমিন প্যানেল এবং সিকিউরিটি রাউটগুলো এক্সপোজ করে।
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from api.routers import include_admin_routers
from core.app_builder import build_app_shell, router_health_check
from core.config import settings
from middleware.anti_hacking import AntiHackingContextMiddleware

app: FastAPI = build_app_shell(title="SupremeAI Admin API")

# বাংলা মন্তব্ট: Production-এ ADMIN_CORS_ORIGINS ফাঁকা (empty) হলে
# বুট-টাইমে crash এড়াতে ডিফল্ট অ্যাডমিন origin অটো-পপুলেট করা হচ্ছে।
# শুধুমাত্র অ্যাডমিন কনসোল origin — Vercel/Netlify user client নয়।
if settings.env == "production":
    # বাংলা মন্তব্য: প্রোডাকশনে অ্যাডমিন CORS অরিজিনে '*' থাকলে বা খালি হলে ক্র্যাশ এড়াতে সেফ অরিজিন সেট করা হচ্ছে
    if not settings.admin_cors_origins or "*" in settings.admin_cors_origins:
        from loguru import logger

        # বাংলা মন্তব্য: Admin CORS ফলব্যাকে শুধুমাত্র অ্যাডমিন কনসোল origin রাখা হচ্ছে।
        # আগে ভুলবশত supremeai-backend* user backend URLs যোগ হয়েছিল — এগুলো CORS origin নয়,
        # বরং এগুলো server-to-server URLs। এটিই preflight OPTIONS এ HTTP ok status না পাওয়ার কারণ।
        logger.warning("⚠️ Production Admin CORS wildcard/drift detected. Setting default trusted admin origins.")
        settings.admin_cors_origins = [origin for origin in (settings.admin_cors_origins or []) if origin != "*"] + [
            "https://supremeai-admin.web.app",
        ]

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
# তাই এখানে মিনিমাম রাউটের উপস্থিতি চেক করা হচ্ছে যাতে প্রসেসটি ক্র্যাশ না করে।
# বাংলা মন্তব্য: preferences রাউটার যোগ হওয়ায় কাউন্ট ৫->৬ আপডেট করা হলো।
router_health_check(app, expected_count=6)
