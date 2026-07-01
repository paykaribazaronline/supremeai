import os
import sys
from pathlib import Path

from dotenv import load_dotenv
from loguru import logger
from pydantic import Field
from pydantic import ValidationInfo
from pydantic import field_validator
from pydantic_settings import BaseSettings
from pydantic_settings import SettingsConfigDict

from .secret_vault import secret_vault


if "pytest" not in sys.modules:
    root_env = Path(__file__).resolve().parents[2] / ".env"
    load_dotenv(root_env)


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=None if "pytest" in sys.modules else ["../.env", ".env"],
        extra="ignore",
    )

    PROJECT_NAME: str = "SupremeAI 2.0"
    API_V1_STR: str = "/api/v1"

    app_name: str = "SupremeAI 2.0"
    env: str = "local"
    debug: bool = True
    docs_auth_enabled: bool = True
    docs_username: str = "admin"
    docs_password: str = ""

    port: int = 8000
    host: str = "0.0.0.0"  # nosec B104
    supremeai_admin_password_hash: str | None = None

    cors_origins: str | list[str] = [
        "http://127.0.0.1:3000",
        "http://127.0.0.1:8000",
        "http://localhost:5173",
        "https://supremeai-a.web.app",
        "https://supremeai-a.firebaseapp.com",
        "https://supremeai-admin.web.app",
        "https://supremeai-admin.firebaseapp.com",
    ]

    @field_validator("cors_origins", mode="before")
    @classmethod
    def sanitize_cors_origins(cls, v):
        import json

        if isinstance(v, str):
            v = v.strip()
            if not v:
                return []
            try:
                v = json.loads(v)
            except json.JSONDecodeError:
                v = [origin.strip() for origin in v.split(",") if origin.strip()]
        if not isinstance(v, list):
            return v
        localhost_origins = {"http://127.0.0.1:3000", "http://127.0.0.1:8000", "http://localhost:5173"}
        env = getattr(cls, "_env_context", "local")
        if env == "production":
            return [origin for origin in v if origin not in localhost_origins]
        return v

    # বাংলা মন্তব্য: এডমিন ইমেইল লিস্ট সরাসরি .env ফাইল থেকে লোড করা হবে
    admin_emails: list[str] = Field(
        default=["niloyjoy7@gmail.com"], validation_alias="ADMIN_EMAILS"
    )

    # বাংলা মন্তব্য: অনুমোদিত হোস্ট লিস্ট সরাসরি .env ফাইল থেকে লোড করা হবে
    allowed_hosts: list[str] = Field(
        default=["localhost", "njel.com.bd", "testserver", "run.app"],
        validation_alias="ALLOWED_HOSTS",
    )

    jwt_secret: str | None = Field(
        default=None, validation_alias="SUPREMEAI_JWT_SECRET"
    )

    # ⚡ ডাইনামিকলি সরাসরি ক্লাউড মেমরি থেকে সিক্রেট রিড করা হচ্ছে
    # ডিস্কে কোনো .env ফাইল না থাকলেও প্রোডাকশন এপিআই ১০০% স্মুথলি চলবে
    supabase_database_url: str = secret_vault.fetch_secret(
        "SUPABASE_DATABASE_URL_POOLER",
        "postgresql://localhost:5432/postgres",
    )
    redis_url: str = secret_vault.fetch_secret("REDIS_URL", "redis://localhost:6379/0")

    openrouter_api_key: str = secret_vault.fetch_secret("OPENROUTER_API_KEY", "")
    hf_api_key: str = secret_vault.fetch_secret("HF_API_KEY", "")
    gemini_api_key: str = secret_vault.fetch_secret("GEMINI_API_KEY", "")
    deepseek_api_key: str = secret_vault.fetch_secret("DEEPSEEK_API_KEY", "")
    groq_api_key: str = secret_vault.fetch_secret("GROQ_API_KEY", "")
    nvidia_api_key: str = secret_vault.fetch_secret("NVIDIA_API_KEY", "")
    firecrawl_api_key: str = secret_vault.fetch_secret("FIRECRAWL_API_KEY", "")
    discord_bot_token: str = secret_vault.fetch_secret("DISCORD_BOT_TOKEN", "")

    claude_openrouter_model: str = "anthropic/claude-3.5-haiku:free"

    gemini_rpm_limit: int = 9
    gemini_tpm_limit: int = 240_000
    gemini_rpd_limit: int = 475
    groq_rpm_limit: int = 28
    groq_tpm_limit: int = 28_500
    groq_rpd_limit: int = 13_680
    openrouter_rpm_limit: int = 19
    openrouter_rpd_limit: int = 45
    cloudflare_rpd_limit: int = 9_000
    nvidia_rpm_limit: int = 38
    nvidia_tpm_limit: int = 38_000
    huggingface_rpm_limit: int = 18
    huggingface_rpd_limit: int = 950

    max_prompt_tokens: int = 4_000
    max_response_tokens: int = 1_500
    enable_token_compression: bool = True
    sentry_dsn: str = ""
    ollama_url: str = "http://127.0.0.1:11434"
    gcp_project_id: str = "supremeai-a"
    gcp_region: str = "us-central1"

    stripe_api_key: str = ""
    stripe_webhook_secret: str = ""

    max_cost_per_task: float = 0.01
    admin_rules_db: str = "data/constitutional_rules.db"
    memory_db_dir: str = "data/memory"
    skill_registry_path: str = "data/skill_registry.json"
    ci_webhook_secret: str = secret_vault.fetch_secret(
        "CI_WEBHOOK_SECRET", "supreme-ci-secret-2026"
    )

    @field_validator("env")
    @classmethod
    def validate_env(cls, value: str) -> str:
        allowed = {"local", "staging", "production", "test"}
        if value.lower() not in allowed:
            raise ValueError(f"env must be one of {allowed}")
        return value.lower()

    @field_validator("admin_emails", mode="before")
    @classmethod
    def parse_admin_emails(cls, v) -> list[str]:
        # বাংলা মন্তব্য: কমা দ্বারা পৃথকীকৃত ইমেইল স্ট্রিংকে লিস্টে কনভার্ট করা হলো
        if isinstance(v, str):
            v = v.strip()
            if not v:
                return []
            return [email.strip() for email in v.split(",") if email.strip()]
        return v

    @field_validator("allowed_hosts", mode="before")
    @classmethod
    def parse_allowed_hosts(cls, v) -> list[str]:
        # বাংলা মন্তব্য: কমা দ্বারা পৃথকীকৃত ডোমেইন স্ট্রিংকে লিস্টে কনভার্ট করা হলো
        if isinstance(v, str):
            v = v.strip()
            if not v:
                return []
            return [host.strip() for host in v.split(",") if host.strip()]
        return v

    @field_validator("jwt_secret", mode="before")
    @classmethod
    def set_test_secret(cls, v: str | None, info: ValidationInfo) -> str | None:
        env = info.data.get("env", "local")
        if not v:
            if env == "production":
                raise ValueError(
                    "SUPREMEAI_JWT_SECRET environment variable must be set in production"
                )
            return "test-secret-placeholder"
        return v

    @field_validator("debug")
    @classmethod
    def debug_must_be_false_in_production(cls, v: bool, info: ValidationInfo) -> bool:
        env = info.data.get("env", "local")
        if env == "production" and v:
            return False
        return v

    @field_validator("cors_origins", mode="before")
    @classmethod
    def parse_cors_origins(cls, v):
        import json

        if isinstance(v, str):
            v = v.strip()
            if not v:
                return []
            try:
                return json.loads(v)
            except json.JSONDecodeError:
                return [origin.strip() for origin in v.split(",") if origin.strip()]
        return v

    def validate_config(self) -> None:
        if self.env.lower() == "production":
            missing = []
            if not self.openrouter_api_key:
                missing.append("openrouter_api_key")
            if not self.gemini_api_key:
                missing.append("gemini_api_key")
            if not self.sentry_dsn:
                logger.warning("Sentry DSN is not configured (strongly recommended)")
            if not self.jwt_secret:
                missing.append("secure JWT_SECRET")
            if missing:
                raise RuntimeError(
                    f"Missing required configurations for production: {', '.join(missing)}"
                )


settings = Settings()
# 🛑 ZERO-GAP: Fast Fail on missing production configuration keys
if settings.env == "production" or os.getenv("ENV") == "production":
    try:
        settings.validate_config()
        # Verify encryption key is configured
        if not os.getenv("SUPREMEAI_ENCRYPTION_KEY"):
            raise RuntimeError("SUPREMEAI_ENCRYPTION_KEY environment variable must be set in production")
    except Exception as exc:
        logger.critical(f"FATAL CONFIG ERROR: {exc}")
        sys.exit(1)

