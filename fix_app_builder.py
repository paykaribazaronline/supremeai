from pathlib import Path

file_path = Path(r"f:\supremeai\backend\core\app_builder.py")
content = file_path.read_text(encoding="utf-8")

# 1. ChaosInjectorMiddleware
content = content.replace("app.add_middleware(ChaosInjectorMiddleware)", "app.add_middleware(ChaosInjectorMiddleware)  # type: ignore")

# 2. RateLimitMiddleware
content = content.replace(
    "app.add_middleware(RateLimitMiddleware)",
    "from core.rate_limit import RateLimiter\n    app.add_middleware(RateLimitMiddleware, limiter=RateLimiter())"
)

# 3. origins list addition
content = content.replace(
    "origins = list(set(settings.user_cors_origins + settings.admin_cors_origins))",
    "def _ensure_list(v):\n        return [v] if isinstance(v, str) else list(v)\n    origins = list(set(_ensure_list(settings.user_cors_origins) + _ensure_list(settings.admin_cors_origins)))"
)

file_path.write_text(content, encoding="utf-8")
print("Done fixing app_builder.py")
