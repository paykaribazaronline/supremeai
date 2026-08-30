"""Production-grade config for the SupremeAI ecosystem test harness.

বাংলা: এই config-টি সব env-driven। নিচের সব key Render/Heroku/etc-এর
Environment tab-এ set করতে হবে। কোনো key না দিলেও foundation চালু হবে,
শুধু live adapter test ছাড়া।

Production-এ যা যা লাগবে:
- ADMIN_TOKEN             (বাধ্যতামূলক — admin endpoints এর জন্য)
- RENDER_API_KEY          (Render adapter test)
- RENDER_SERVICE_ID       (Render adapter test)
- GITHUB_TOKEN            (GitHub adapter test)
- GITHUB_REPO             (GitHub adapter test)
- SUPABASE_URL            (Supabase adapter test — your existing project)
- SUPABASE_SERVICE_KEY    (Supabase adapter test)
- FRONTEND_ORIGIN         (CORS — frontend domain, default *)

Optional advanced:
- SENTRY_DSN              (error tracking)
- LOG_LEVEL               (debug/info/warning/error, default info)
- REDIS_URL               (future — distributed cache, not needed for now)
"""

from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass
class Settings:
    # ── Server ────────────────────────────────────────────────────────────
    host: str = "0.0.0.0"
    port: int = 8000
    env: str = "production"
    log_level: str = "info"

    # ── Admin auth (MANDATORY) ────────────────────────────────────────────
    # বাংলা: একটা long random string। generate করতে:
    #   python -c "import secrets; print(secrets.token_urlsafe(32))"
    admin_token: str = "CHANGE-ME-TO-A-LONG-RANDOM-STRING"

    # ── CORS ──────────────────────────────────────────────────────────────
    # বাংলা: frontend-এর origin। Render deploy করলে সেটা এখানে দিন।
    # Multiple origin হলে comma-separated। default '*' = সব origin।
    frontend_origin: str = "*"

    # ── Render API (optional — for live Render adapter test) ─────────────
    # বাংলা: https://dashboard.render.com → আপনার account → Account Settings
    # → API Keys → Create API key
    render_api_key: str = ""
    # বাংলা: আপনার test service-এর ID। Render dashboard-এ service খুললে
    # URL-এ থাকে: render.com/web/srv-xxxxxxxxxxxx
    render_service_id: str = ""

    # ── GitHub API (optional — for live GitHub adapter test) ──────────────
    # বাংলা: https://github.com/settings/tokens → Generate new token (classic)
    # Scopes: repo (full), workflow (optional for CI)
    # ⚠️ Test শেষে অবশ্যই revoke করবেন।
    github_token: str = ""
    # বাংলা: আপনার test repo — যেটা আপনি এই প্যাকেজ push করবেন
    github_repo: str = ""  # e.g. your-username/supremeai-ecosystem-test

    # ── Supabase API (optional — for live Supabase adapter test) ──────────
    # বাংলা: https://app.supabase.com → আপনার existing project →
    # Project Settings → API → "Project URL" + "service_role secret"
    # ⚠️ service_role key কখনো client-এ expose করবেন না।
    supabase_url: str = ""          # e.g. https://xxxxx.supabase.co
    supabase_service_key: str = ""  # service_role key

    # ── Optional: Sentry error tracking ───────────────────────────────────
    sentry_dsn: str = ""

    # ── Optional: Redis (future — not needed for foundation test) ────────
    redis_url: str = ""

    # ── Ecosystem settings ────────────────────────────────────────────────
    auto_seed: bool = True       # seed default capabilities + policies on boot
    ecosystem_db_path: str = ""  # default: ./data/ecosystem.db

    # ── Feature flags ─────────────────────────────────────────────────────
    # বাংলা: production-এ strict mode চালু থাকবে। development-এ false।
    strict_admin_auth: bool = True  # যদি False হয়, admin endpoints খোলা থাকবে!

    @classmethod
    def from_env(cls) -> "Settings":
        return cls(
            host=os.getenv("HOST", "0.0.0.0"),
            port=int(os.getenv("PORT", "8000")),
            env=os.getenv("ENV", "production"),
            log_level=os.getenv("LOG_LEVEL", "info"),
            admin_token=os.getenv("ADMIN_TOKEN", "CHANGE-ME-TO-A-LONG-RANDOM-STRING"),
            frontend_origin=os.getenv("FRONTEND_ORIGIN", "*"),
            render_api_key=os.getenv("RENDER_API_KEY", ""),
            render_service_id=os.getenv("RENDER_SERVICE_ID", ""),
            github_token=os.getenv("GITHUB_TOKEN", ""),
            github_repo=os.getenv("GITHUB_REPO", ""),
            supabase_url=os.getenv("SUPABASE_URL", ""),
            supabase_service_key=os.getenv("SUPABASE_SERVICE_KEY", ""),
            sentry_dsn=os.getenv("SENTRY_DSN", ""),
            redis_url=os.getenv("REDIS_URL", ""),
            auto_seed=os.getenv("AUTO_SEED", "true").lower() == "true",
            ecosystem_db_path=os.getenv("ECOSYSTEM_DB_PATH", ""),
            strict_admin_auth=os.getenv("STRICT_ADMIN_AUTH", "true").lower() == "true",
        )

    # ── Provider availability checks ──────────────────────────────────────
    def has_render(self) -> bool:
        return bool(self.render_api_key and self.render_service_id)

    def has_github(self) -> bool:
        return bool(self.github_token and self.github_repo)

    def has_supabase(self) -> bool:
        return bool(self.supabase_url and self.supabase_service_key)

    def has_sentry(self) -> bool:
        return bool(self.sentry_dsn)

    def cors_origins(self) -> list[str]:
        if self.frontend_origin == "*":
            return ["*"]
        return [o.strip() for o in self.frontend_origin.split(",") if o.strip()]

    # ── Safety check ──────────────────────────────────────────────────────
    def is_safe_for_production(self) -> tuple[bool, list[str]]:
        """Roadmap §28 — production safety validation. Returns (ok, warnings)."""
        warnings: list[str] = []
        if self.admin_token.startswith("CHANGE-ME") or len(self.admin_token) < 16:
            warnings.append("ADMIN_TOKEN is default/short — set a strong random token (>=16 chars)")
        if not self.strict_admin_auth:
            warnings.append("STRICT_ADMIN_AUTH=false — admin endpoints will be open!")
        if self.env == "production" and self.has_github() and "ghp_test" in self.github_token.lower():
            warnings.append("GITHUB_TOKEN looks like a test token — revoke + regenerate")
        return (len(warnings) == 0, warnings)


settings = Settings.from_env()
