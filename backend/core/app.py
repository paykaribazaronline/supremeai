from __future__ import annotations

"""SupremeAI 2.0 — Core FastAPI app bootstrapping, middleware chain, and router loading.

বাংলা: কোর FastAPI অ্যাপ বুটস্ট্র্যাপিং, মিডলওয়্যার চেইন এবং রাউটার লোডিং।

Key Components:
- InterceptHandler: Routes stdlib logging to Loguru.
- router_health_check: Ensures minimum route count on startup.
"""


from api.routers import register_all_routers
from core.admin_routes import router as admin_router
from core.app_builder import build_app_shell, router_health_check
from core.config import settings
from fastapi.middleware.cors import CORSMiddleware

# For backward compatibility and test suites
# বাংলা মন্তব্য: ব্যাকওয়ার্ড কম্প্যাটিবিলিটি এবং টেস্ট কেসের জন্য ডিফল্ট গ্লোবাল অ্যাপ
app = build_app_shell(title=f"{settings.app_name} (Production Ready)")
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
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
    if not settings.cors_origins:
        raise RuntimeError(
            "🔥 CRITICAL: Production CORS drift detected. cors_origins cannot be empty in production."
        )
    if "*" in settings.cors_origins:
        raise RuntimeError(
            "🚨 SECURITY: Wildcard '*' is strictly prohibited in production CORS mesh. Set CORS_ORIGINS env var."
        )

app.include_router(admin_router)
register_all_routers(app)
router_health_check(app)
