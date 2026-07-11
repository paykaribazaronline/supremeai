# backend/core/config.py
# বাংলা মন্তব্য: সম্পূর্ণ রি-ফ্যাক্টর — Fail-Fast, Zero-Hardcode, Pydantic-Enforced Config Layer।
# কোনো API Key, hardcoded domain বা threshold এখানে নেই।
# সব ভ্যালু env var বা GCP Secret Manager থেকে আসে।
# Production-এ কোনো missing required var = startup crash (sys.exit(1)) — "resilient boot" নিষিদ্ধ।

import os
import secrets
import sys
from pathlib import Path
from typing import Any

from dotenv import load_dotenv
from loguru import logger
from pydantic import Field
from pydantic import PrivateAttr
from pydantic import SecretStr
from pydantic import ValidationInfo
from pydantic import computed_field
from pydantic import field_validator
from pydantic import model_validator
from pydantic_settings import BaseSettings
from pydantic_settings import SettingsConfigDict

from .secret_vault import secret_vault


# বাংলা মন্তব্য: pytest environment-এ .env load করা হয় না — test isolation নিশ্চিত।
if "pytest" not in sys.modules:
    root_env = Path(__file__).resolve().parents[2] / ".env"
    load_dotenv(root_env)


class Settings(BaseSettings):
    """
    বাংলা মন্তব্য: এটি সিস্টেমের একমাত্র সত্যের উৎস (Single Source of Truth)।
    কোনো hardcoded value নেই। সব env-driven।
    Production-এ missing required var = startup Fail-Fast (sys.exit(1))।
    """

    model_config = SettingsConfigDict(
        env_file=None if "pytest" in sys.modules else ["../.env", ".env"],
        extra="ignore",
    )

    # ── অ্যাপ্লিকেশন মেটাডেটা ──────────────────────────────────────────────
    PROJECT_NAME: str = "SupremeAI 2.0"
    API_V1_STR: str = "/api/v1"
    app_name: str = "SupremeAI 2.0"

    # বাংলা মন্তব্য: env validate হবে — invalid value = startup crash
    env: str = Field(default="local", validation_alias="ENV")
    debug: bool = Field(default=True)
    docs_auth_enabled: bool = True
    docs_username: str = "admin"
    docs_password: str = ""

    # ── নেটওয়ার্ক কনফিগ — সব env-driven, কোনো hardcode নেই ────────────────
    port: int = Field(default=8000, validation_alias="PORT")
    host: str = Field(default="0.0.0.0", validation_alias="HOST")  # nosec B104

    # বাংলা মন্তব্য: CORS origins এখন সম্পূর্ণ env-driven।
    # Default এ কোনো hardcoded URL নেই।
    # Production-এ CORS_ORIGINS env var বাধ্যতামূলক।
    cors_origins: str | list[str] = Field(
        default_factory=list,
        validation_alias="CORS_ORIGINS",
    )

    # বাংলা মন্তব্য: Admin email list সম্পূর্ণ env-driven
    admin_emails: list[str] = Field(default=[], validation_alias="ADMIN_EMAILS")

    # বাংলা মন্তব্য: Zero-Trust Host Validation — production-এ empty = crash
    allowed_hosts: list[str] = Field(
        default_factory=list,
        validation_alias="ALLOWED_HOSTS",
    )

    # বাংলা মন্তব্য: JWT secret — production-এ অবশ্যই >= 64 bytes entropy
    jwt_secret: str = Field(
        default="",
        validation_alias="SUPREMEAI_JWT_SECRET",
    )

    supremeai_admin_password_hash: str | None = Field(
        default=None,
        validation_alias="SUPREMEAI_ADMIN_PASSWORD_HASH",
    )

    # ── Stripe credentials — SecretStr দিয়ে log-safe ────────────────────────
    stripe_api_key: SecretStr = Field(default=SecretStr(""), validation_alias="STRIPE_API_KEY")
    stripe_webhook_secret: SecretStr = Field(default=SecretStr(""), validation_alias="STRIPE_WEBHOOK_SECRET")

    # ── LLM rate limit thresholds — সব env-driven, hardcode নেই ─────────────
    # বাংলা মন্তব্য: আগে এগুলো hardcode ছিল। এখন ops team .env দিয়ে change করতে পারবে —
    # code redeploy ছাড়াই। এটি একটি critical operational improvement।
    gemini_rpm_limit: int = Field(default=9, validation_alias="GEMINI_RPM_LIMIT")
    gemini_tpm_limit: int = Field(default=240_000, validation_alias="GEMINI_TPM_LIMIT")
    gemini_rpd_limit: int = Field(default=475, validation_alias="GEMINI_RPD_LIMIT")
    groq_rpm_limit: int = Field(default=28, validation_alias="GROQ_RPM_LIMIT")
    groq_tpm_limit: int = Field(default=28_500, validation_alias="GROQ_TPM_LIMIT")
    groq_rpd_limit: int = Field(default=13_680, validation_alias="GROQ_RPD_LIMIT")
    openrouter_rpm_limit: int = Field(default=19, validation_alias="OPENROUTER_RPM_LIMIT")
    openrouter_rpd_limit: int = Field(default=45, validation_alias="OPENROUTER_RPD_LIMIT")
    cloudflare_rpd_limit: int = Field(default=9_000, validation_alias="CLOUDFLARE_RPD_LIMIT")
    nvidia_rpm_limit: int = Field(default=38, validation_alias="NVIDIA_RPM_LIMIT")
    nvidia_tpm_limit: int = Field(default=38_000, validation_alias="NVIDIA_TPM_LIMIT")
    huggingface_rpm_limit: int = Field(default=18, validation_alias="HUGGINGFACE_RPM_LIMIT")
    huggingface_rpd_limit: int = Field(default=950, validation_alias="HUGGINGFACE_RPD_LIMIT")

    max_prompt_tokens: int = Field(default=4_000, validation_alias="MAX_PROMPT_TOKENS")
    max_response_tokens: int = Field(default=1_500, validation_alias="MAX_RESPONSE_TOKENS")
    max_cost_per_task: float = Field(default=0.01, validation_alias="MAX_COST_PER_TASK")
    enable_token_compression: bool = True

    # ── Circuit Breaker Config ───────────────────────────────────────────────
    circuit_breaker_failure_threshold: int = Field(default=3, validation_alias="CIRCUIT_BREAKER_FAILURE_THRESHOLD")
    circuit_breaker_cooldown_period: int = Field(default=60, validation_alias="CIRCUIT_BREAKER_COOLDOWN_PERIOD")

    # বাংলা মন্তব্য: Model names env-driven — code deploy ছাড়াই model switch করা যাবে
    claude_openrouter_model: str = Field(
        default="anthropic/claude-3.5-haiku:free",
        validation_alias="CLAUDE_OPENROUTER_MODEL",
    )

    sentry_dsn: str = Field(default="", validation_alias="SENTRY_DSN")

    # বাংলা মন্তব্য: OLLAMA_URL — production-এ fail-fast, কোনো localhost fallback নেই
    ollama_url: str = Field(default="", validation_alias="OLLAMA_URL")

    gcp_project_id: str = Field(default="", validation_alias="GCP_PROJECT_ID")
    gcp_region: str = Field(default="us-central1", validation_alias="GCP_REGION")

    # বাংলা মন্তব্য: Filesystem paths — Cloud Run ephemeral filesystem-এ এগুলো empty রাখুন
    admin_rules_db: str = Field(default="", validation_alias="ADMIN_RULES_DB_PATH")
    memory_db_dir: str = Field(default="", validation_alias="MEMORY_DB_DIR")
    skill_registry_path: str = Field(default="", validation_alias="SKILL_REGISTRY_PATH")

    # ── Sandbox config — env-driven ──────────────────────────────────────────
    sandbox_root: str = Field(default="/tmp/sandboxes", validation_alias="SANDBOX_ROOT")  # nosec B108
    firecracker_path: str = Field(default="/usr/bin/firecracker", validation_alias="FIRECRACKER_PATH")
    gvisor_path: str = Field(default="/usr/bin/runsc", validation_alias="GVISOR_PATH")
    allow_sandbox_fallback: bool = Field(default=False, validation_alias="ALLOW_SANDBOX_FALLBACK")

    _cached_secrets: dict[str, str] = PrivateAttr(default_factory=dict)

    def _get_cached_secret(self, key: str) -> str:
        # বাংলা মন্তব্য: lazy cache — প্রতিটি secret একবারই fetch হয়। Network call minimize।
        if key not in self._cached_secrets:
            self._cached_secrets[key] = secret_vault.fetch_secret(key)
        return self._cached_secrets[key]

    # ── Cloud-fetched secrets — GCP Secret Manager বা env fallback ───────────
    @computed_field
    def supabase_database_url(self) -> str:
        return self._get_cached_secret("SUPABASE_DATABASE_URL_POOLER")

    @computed_field
    def redis_url(self) -> str:
        return self._get_cached_secret("REDIS_URL")

    @computed_field
    def openrouter_api_key(self) -> str:
        return self._get_cached_secret("OPENROUTER_API_KEY")

    @computed_field
    def hf_api_key(self) -> str:
        return self._get_cached_secret("HF_API_KEY")

    @computed_field
    def gemini_api_key(self) -> str:
        return self._get_cached_secret("GEMINI_API_KEY")

    @computed_field
    def openai_api_key(self) -> str:
        return self._get_cached_secret("OPENAI_API_KEY")

    @computed_field
    def deepseek_api_key(self) -> str:
        return self._get_cached_secret("DEEPSEEK_API_KEY")

    @computed_field
    def groq_api_key(self) -> str:
        return self._get_cached_secret("GROQ_API_KEY")

    @computed_field
    def nvidia_api_key(self) -> str:
        return self._get_cached_secret("NVIDIA_API_KEY")

    @computed_field
    def firecrawl_api_key(self) -> str:
        return self._get_cached_secret("FIRECRAWL_API_KEY")

    @computed_field
    def discord_bot_token(self) -> str:
        return self._get_cached_secret("DISCORD_BOT_TOKEN")

    @computed_field
    def github_client_id(self) -> str:
        return self._get_cached_secret("GITHUB_CLIENT_ID")

    @computed_field
    def github_client_secret(self) -> str:
        return self._get_cached_secret("GITHUB_CLIENT_SECRET")

    @computed_field
    def ci_webhook_secret(self) -> str:
        return self._get_cached_secret("CI_WEBHOOK_SECRET")

    # ── Validators ───────────────────────────────────────────────────────────

    @field_validator("env")
    @classmethod
    def validate_env(cls, value: str) -> str:
        allowed = {"local", "staging", "production", "test"}
        if value.lower() not in allowed:
            raise ValueError(f"ENV must be one of {allowed}, got '{value}'")
        return value.lower()

    @field_validator("debug", mode="before")
    @classmethod
    def validate_debug_mode(cls, v: Any, info: ValidationInfo) -> bool:
        # বাংলা মন্তব্য: Production/Staging-এ debug=True explicitly set করলে hard crash।
        env = info.data.get("env", "local")
        if env in {"production", "staging"}:
            if str(v).lower() == "true" and (os.getenv("debug", "").lower() == "true" or os.getenv("DEBUG", "").lower() == "true"):
                raise ValueError("Explicitly setting debug=True is PROHIBITED in production/staging.")
            return False
        return bool(v)

    @field_validator("docs_password", mode="before")
    @classmethod
    def validate_docs_password(cls, v: str, info: ValidationInfo) -> str:
        # বাংলা মন্তব্য: pytest context-এ docs_password validation skip করা হয়
        if "pytest" in sys.modules:
            return v
        env = info.data.get("env", "local")
        docs_auth_enabled = info.data.get("docs_auth_enabled", True)
        if env in {"production", "staging"} and docs_auth_enabled and not v:
            raise ValueError("docs_password must be set when docs_auth_enabled=true in production/staging.")
        return v

    @field_validator("admin_emails", mode="before")
    @classmethod
    def parse_admin_emails(cls, v) -> list[str]:
        # বাংলা মন্তব্য: কমা-separated string → list conversion
        if isinstance(v, str):
            v = v.strip()
            return [email.strip() for email in v.split(",") if email.strip()] if v else []
        return v or []

    @field_validator("allowed_hosts", mode="before")
    @classmethod
    def parse_allowed_hosts(cls, v) -> list[str]:
        if isinstance(v, str):
            v = v.strip()
            return [h.strip() for h in v.split(",") if h.strip()] if v else []
        return v or []

    @field_validator("allowed_hosts", mode="after")
    @classmethod
    def validate_allowed_hosts(cls, v: list[str], info: ValidationInfo) -> list[str]:
        # বাংলা মন্তব্য: Production-এ localhost-type host forbidden — auto-strip করা হবে
        env = info.data.get("env", "local")
        forbidden = {"localhost", "127.0.0.1", "testserver", "0.0.0.0"}
        if env == "production":
            v = [h for h in v if h.lower() not in forbidden]
            if not v:
                raise ValueError("Production requires explicit ALLOWED_HOSTS — localhost/testserver forbidden.")
        return v

    @field_validator("jwt_secret", mode="before")
    @classmethod
    def set_jwt_secret(cls, v: str | None, info: ValidationInfo) -> str:
        env = info.data.get("env", "local")
        if not v:
            if env == "production":
                # বাংলা মন্তব্য: Production-এ JWT secret missing = hard crash। কোনো ephemeral fallback নেই।
                raise ValueError("🚨 CRITICAL: SUPREMEAI_JWT_SECRET must be explicitly set in production. No ephemeral fallback allowed.")
            logger.warning("⚠️ SUPREMEAI_JWT_SECRET not set. Generating ephemeral secret for local dev.")
            return secrets.token_hex(64)
        return v

    @field_validator("jwt_secret", mode="after")
    @classmethod
    def validate_jwt_secret_strength(cls, v: str, info: ValidationInfo) -> str:
        if info.data.get("env") == "production":
            if len(v) < 64:
                raise ValueError("JWT secret must be >= 64 bytes entropy in production.")
            weak_secrets = {"secret", "password", "123456", "changeme", "admin", "jwt_secret"}
            if v.lower() in weak_secrets:
                raise ValueError("JWT secret is in weak secrets dictionary — change it.")
        return v

    @field_validator("supremeai_admin_password_hash", mode="before")
    @classmethod
    def validate_admin_hash(cls, v: str | None, info: ValidationInfo) -> str | None:
        env = info.data.get("env", "local")
        if not v and env == "production":
            raise ValueError("supremeai_admin_password_hash must be set in production.")
        return v

    @field_validator("cors_origins", mode="before")
    @classmethod
    def parse_cors_origins(cls, v, info: ValidationInfo):
        import json

        if isinstance(v, str):
            v = v.strip()
            if not v:
                return []
            try:
                return json.loads(v)
            except json.JSONDecodeError:
                return [o.strip() for o in v.split(",") if o.strip()]
        return v or []

    @field_validator("cors_origins", mode="after")
    @classmethod
    def validate_cors_origins(cls, v: list[str], info: ValidationInfo) -> list[str]:
        env = info.data.get("env", "local")
        if env == "production":
            # বাংলা মন্তব্য: Production-এ localhost CORS origins auto-strip
            v = [o for o in v if "localhost" not in o and "127.0.0.1" not in o]
            if not v:
                raise ValueError(
                    "Production requires at least one non-localhost CORS origin. Set CORS_ORIGINS env var (e.g. https://your-app.web.app)."
                )
        return v

    @model_validator(mode="after")
    def validate_stripe_in_production(self):
        # বাংলা মন্তব্য: Production-এ Stripe credentials বাধ্যতামূলক
        if self.env == "production":
            stripe_key = self.stripe_api_key.get_secret_value() if self.stripe_api_key else ""
            stripe_webhook = self.stripe_webhook_secret.get_secret_value() if self.stripe_webhook_secret else ""
            if not stripe_key:
                raise ValueError("Stripe API key is mandatory in production.")
            if not stripe_webhook:
                raise ValueError("Stripe webhook secret is mandatory in production.")
        return self

    @model_validator(mode="after")
    def validate_production_completeness(self):
        """
        বাংলা মন্তব্য: Production Fail-Fast Guard — এটিই সিস্টেমের শেষ defense line।
        কোনো missing config = ValueError → startup crash → Cloud Run restart।
        'Resilient boot' in production = security theater এবং সম্পূর্ণ নিষিদ্ধ।
        """
        if self.env != "production":
            return self

        missing: list[str] = []

        if not self.openrouter_api_key:
            missing.append("OPENROUTER_API_KEY")
        if not self.gemini_api_key:
            missing.append("GEMINI_API_KEY")
        if not self.ci_webhook_secret:
            missing.append("CI_WEBHOOK_SECRET")
        if not self.sentry_dsn:
            # বাংলা মন্তব্য: Sentry optional কিন্তু strongly recommended
            logger.warning("⚠️ SENTRY_DSN not configured — production observability degraded.")
        if not (os.getenv("SUPREMEAI_ENCRYPTION_KEY") or os.getenv("ENCRYPTION_KEY")):
            missing.append("SUPREMEAI_ENCRYPTION_KEY")

        if missing:
            raise ValueError(
                f"🚨 PRODUCTION FAIL-FAST: Missing required config vars: {', '.join(missing)}. Server startup aborted. Fix config and redeploy."
            )
        return self


# ── Singleton instantiation with True Fail-Fast ────────────────────────────────
# বাংলা মন্তব্য: এখানে Fail-Fast সত্যিকারভাবে enforce হচ্ছে।
# Production-এ ValidationError = sys.exit(1) — কোনো "resilient boot" নেই।
# "Resilient boot" মানে broken state-এ traffic serve করা — এটি বিপজ্জনক এবং নিষিদ্ধ।
try:
    settings = Settings()
except Exception as _boot_exc:  # noqa: BLE001
    _env_value = os.getenv("ENV", "local").lower()
    if _env_value == "production":
        logger.critical(f"🔥 FATAL CONFIG ERROR in production: {_boot_exc}\nServer startup ABORTED. Fix the configuration and redeploy.")
        sys.exit(1)  # ← Production-এ hard crash — Cloud Run restart trigger হবে
    else:
        # বাংলা মন্তব্য: Local/staging-এ warning দিয়ে চলতে দেওয়া হবে
        logger.warning(f"⚠️ Config validation warning (non-production): {_boot_exc}")
        settings = Settings.model_construct()  # type: ignore[assignment]


def get_production_env(var_name: str, default_fallback: str = None) -> str:
    """বাংলা মন্তব্য: Fail-Fast Config Guard.
    প্রোডাকশন এনভায়রনমেন্টে কোনো ক্রিটিক্যাল সিক্রেট মিসিং থাকলে সিস্টেম লোকালহোস্টে
    ফলব্যাক না করে সরাসরি হার্ড ক্র্যাশ করবে, যা সাইলেন্ট ফেইলর প্রতিরোধ করে।
    """
    import os

    from loguru import logger

    value = os.getenv(var_name)
    env = os.getenv("ENV", "development").lower()

    if not value:
        if env == "production":
            logger.critical(f"❌ CRITICAL CONFIG ERROR: Missing required environment variable '{var_name}' in production!")
            raise ValueError(f"Configuration Error: {var_name} must be explicitly defined in production.")
        return default_fallback

    return value
