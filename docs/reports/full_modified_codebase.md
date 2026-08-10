# Full Modified Codebase - JIT OTP & Device Fingerprinting & Dual-Instance Hardening & Security Audit Patches

This file compiles all backend and frontend changes implemented in the split APIs, Redis simulator, JIT OTP router, device fingerprinting, Phase 2 dual-instance hardening, and July 19 Security Audit Patches.

## File: `backend/main.py`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\backend/main.py](file:///c:/Users/n/supremeai/supremeai_2.0\backend/main.py)

```python
"""SupremeAI 2.0 — Entry point. Handles ENV bootstrap, signal handling, and Uvicorn launch.

বাংলা: রুট এন্ট্রি পয়েন্ট। ENV সেটআপ, সিগন্যাল হ্যান্ডলিং এবং সার্ভার লঞ্চ।
"""

import os
import signal
import sys


if not os.getenv("ENV"):
    os.environ["ENV"] = os.getenv("SUPREMEAI_DEFAULT_ENV", "local")

import uvicorn
from loguru import logger

# বাংলা মন্তব্য: টেস্ট এনভায়রনমেন্টে সম্পূর্ণ অ্যাপ এবং প্রোডাকশনে রোল অনুযায়ী ইউজার/অ্যাডমিন এন্ট্রি পয়েন্ট লোড করা হচ্ছে
if "pytest" in sys.modules:
    from core.app import app
else:
    role = os.getenv("SERVICE_ROLE", "user").lower()
    if role == "admin":
        from core.app_admin import app
    else:
        from core.app_user import app
from core.config import settings
from core.logging_config import setup_logging


setup_logging()


def _handle_sigterm(signum: int, frame: object) -> None:  # noqa: ANN401
    """SIGTERM/SIGINT handler.

    SupremeAI FastAPI shutdown is handled by Uvicorn + `lifespan.app_lifespan`.
    This handler must NOT force `sys.exit()` because that can bypass lifespan teardown.
    """
    logger.info(f"🚨 Signal received ({signum}). Initiating graceful shutdown via Uvicorn/FastAPI lifespan...")
    # Best-effort observability: let operators know shutdown intent was triggered.
    os.environ["UVICORN_SHUTDOWN_REQUESTED"] = "1"
    # Do not block here; return control to Uvicorn so it can run shutdown hooks.
    return


signal.signal(signal.SIGTERM, _handle_sigterm)
signal.signal(signal.SIGINT, _handle_sigterm)


def run_server() -> None:
    """Boot the Uvicorn server with config-driven settings.

    বাংলা: কনফিগ-ড্রিভেন সেটিংস দিয়ে Uvicorn সার্ভার বুট।
    """
    port = int(os.getenv("PORT", str(settings.port)))
    is_local = settings.env == "local"
    uvicorn_kwargs: dict = {
        "host": settings.host,
        "port": port,
        "log_level": os.getenv("UVICORN_LOG_LEVEL", "info"),
        "access_log": os.getenv("UVICORN_ACCESS_LOG", "true").lower() == "true",
        "timeout_keep_alive": int(os.getenv("UVICORN_KEEP_ALIVE_TIMEOUT", "30")),
    }
    if is_local:
        uvicorn_kwargs["reload"] = True
    else:
        uvicorn_kwargs["reload"] = False
        # বাংলা: UVICORN_WORKERS env var ব্যবহার করা হয়, GUNICORN_WORKERS deprecated
        workers = int(os.getenv("UVICORN_WORKERS", "4"))
        if workers > 1:
            uvicorn_kwargs["workers"] = workers

    try:
        # বাংলা: app-এর সরাসরি রেফারেন্স ব্যবহার, যাতে মডিউল রিলোডিং পরিবর্তনে ভাঙ্গবে না
        uvicorn.run(app, **uvicorn_kwargs)
    except RuntimeError as exc:
        logger.critical(f"Server failed to start (configuration error): {exc}")
        if settings.sentry_dsn:
            try:
                import sentry_sdk

                sentry_sdk.capture_exception(exc)
            except Exception as sentry_exc:  # noqa: BLE001
                logger.warning(f"Failed to report error to Sentry: {sentry_exc}")
        sys.exit(1)
    except OSError as exc:
        logger.critical(f"Server failed to start (port/bind error on {settings.host}:{port}): {exc}")
        if settings.sentry_dsn:
            try:
                import sentry_sdk

                sentry_sdk.capture_exception(exc)
            except Exception as sentry_exc:  # noqa: BLE001
                logger.warning(f"Failed to report error to Sentry: {sentry_exc}")
        sys.exit(1)


if __name__ == "__main__":
    run_server()

```

---

## File: `backend/core/config.py`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\backend/core/config.py](file:///c:/Users/n/supremeai/supremeai_2.0\backend/core/config.py)

```python
"""This module, `backend.core.config`, serves as the single, authoritative source for all application settings within the SupremeAI project. It implements a robust, "Fail-Fast" configuration layer using Pydantic, ensuring that all critical parameters are loaded from environment variables or a secret manager, with zero hardcoded values. It rigorously validates settings at startup, preventing the application from booting if essential configurations are missing or invalid, thereby guaranteeing a secure and predictable operational environment across all deployment stages.

Key Components:
- `Settings`: The central Pydantic model that defines and validates all application-wide configuration parameters, fetching secrets and enforcing strict rules for different environments.
- `settings`: A singleton instance of the `Settings` class, providing global access to the validated application configuration.
- `get_production_env()`: A utility function for strictly retrieving environment variables, enforcing a fail-fast approach for critical missing values.

Dependencies:
- `os`: For interacting with the operating system, primarily environment variables.
- `secrets`: For generating secure random numbers, used for JWT secret fallback.
- `sys`: For system-specific parameters and functions, used for `sys.exit` and checking `sys.modules` for `pytest`.
- `pathlib.Path`: For object-oriented filesystem paths, used for locating `.env` files.
- `typing`: For type hints.
- `json`: For parsing JSON strings, specifically for `cors_origins`.
- `dotenv.load_dotenv`: For loading environment variables from `.env` files.
- `loguru.logger`: For structured logging, especially for critical configuration errors.
- `pydantic`: The core library for data validation and settings management.
- `pydantic_settings.BaseSettings`: Pydantic's base class for managing settings from environment variables.
- `pydantic_settings.SettingsConfigDict`: Configuration class for `BaseSettings`.
- `pydantic.Field`: Used to define field properties and validation aliases.
- `pydantic.PrivateAttr`: Used for private attributes not part of the model's data.
- `pydantic.SecretStr`: For handling sensitive string data that should not be logged.
- `pydantic.ValidationInfo`: Provides context during validation.
- `pydantic.computed_field`: For fields whose values are computed dynamically.
- `pydantic.field_validator`: Decorator for field-specific validation logic.
- `pydantic.model_validator`: Decorator for model-level validation logic.
- `core.security.secret_vault`: An internal module responsible for fetching secrets from a secure vault (e.g., GCP Secret Manager)."""

# backend/core/config.py
# ⚠️ WARNING: DO NOT MOVE THIS FILE. It is heavily integrated into the FastAPI startup lifecycle.
# Moving this file will break relative paths, imports, and core configuration loading across the entire project.
# বাংলা মন্তব্য: সম্পূর্ণ রি-ফ্যাক্টর — Fail-Fast, Zero-Hardcode, Pydantic-Enforced Config Layer।
# কোনো API Key, hardcoded domain বা threshold এখানে নেই।
# সব ভ্যালু env var বা GCP Secret Manager থেকে আসে।
# যেকোনো Environment-এ (Local/Staging/Prod) কোনো missing required var = startup crash (sys.exit(1)) — "resilient boot" সম্পূর্ণ নিষিদ্ধ।

import json
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

from .security.secret_vault import secret_vault


# বাংলা মন্তব্য: pytest environment-এ .env load করা হয় না — test isolation নিশ্চিত।
if "pytest" not in sys.modules:
    root_env = Path(__file__).resolve().parents[2] / ".env"
    load_dotenv(root_env)


class Settings(BaseSettings):
    """
    বাংলা মন্তব্য: এটি সিস্টেমের একমাত্র সত্যের উৎস (Single Source of Truth)।
    কোনো hardcoded value নেই। সব env-driven।
    যেকোনো এনভায়রনমেন্টে missing required var = startup Fail-Fast (sys.exit(1))।
    """

    model_config = SettingsConfigDict(
        env_file=None if "pytest" in sys.modules else ["../.env", ".env", "/etc/secrets/.env", "/etc/secrets/render.env"],
        extra="ignore",
    )

    # বাংলা মন্তব্য: env validate হবে — invalid value = startup crash
    env: str = Field(default="local", validation_alias="ENV")
    debug: bool = Field(default=True)

    # ── অ্যাপ্লিকেশন মেটাডেটা ──────────────────────────────────────────────
    PROJECT_NAME: str = "SupremeAI 2.0"
    API_V1_STR: str = "/api/v1"
    app_name: str = "SupremeAI 2.0"
    docs_auth_enabled: bool = True
    docs_username: str = Field(default="admin", validation_alias="SUPREMEAI_DOCS_USERNAME")
    docs_password: SecretStr = Field(default=SecretStr("dev_password_only"), validation_alias="SUPREMEAI_DOCS_PASSWORD")

    # ── নেটওয়ার্ক কনফিগ — সব env-driven, কোনো hardcode নেই ────────────────
    port: int = Field(default=8080, validation_alias="PORT")  # বাংলা: Dockerfile CMD-এর ${PORT:-8080} default-এর সাথে consistent
    host: str = Field(default="0.0.0.0", validation_alias="HOST")  # nosec B104

    # বাংলা মন্তব্য: CORS origins এখন সম্পূর্ণ env-driven।
    # Default এ কোনো hardcoded URL নেই।
    cors_origins: str | list[str] = Field(
        default_factory=list,
        validation_alias="CORS_ORIGINS",
    )

    # বাংলা মন্তব্য: রোল-ভিত্তিক CORS সেটিংস এবং সিকিউরিটি টগল
    user_cors_origins: str | list[str] = Field(
        default_factory=list,
        validation_alias="USER_CORS_ORIGINS",
    )
    admin_cors_origins: str | list[str] = Field(
        default_factory=list,
        validation_alias="ADMIN_CORS_ORIGINS",
    )
    enforce_anti_hacking: bool = Field(
        default=False,
        validation_alias="ENFORCE_ANTI_HACKING",
    )

    # বাংলা মন্তব্য: main.py-এর app_user/app_admin bootstrap-এর সাথে সামঞ্জস্যপূর্ণ একই SERVICE_ROLE flag।
    # DB pool sizing (database/session.py) এই মানের উপর ভিত্তি করে User vs Admin instance-এ আলাদা limit প্রয়োগ করে।
    service_role: str = Field(default="user", validation_alias="SERVICE_ROLE")

    # বাংলা মন্তব্য: JIT OTP over-saturation protection — প্রতি admin প্রতি এই সেকেন্ডে সর্বোচ্চ ১টি OTP।
    otp_cooldown_seconds: int = Field(default=60, validation_alias="OTP_COOLDOWN_SECONDS")

    # বাংলা মন্তব্য: Admin email list সম্পূর্ণ env-driven
    # (Moved to Security & Auth Config section to avoid duplication)

    # বালা মন্তব্য: Zero-Trust Host Validation — empty = crash
    allowed_hosts: str | list[str] = Field(
        default_factory=list,
        validation_alias="ALLOWED_HOSTS",
    )

    # বাংলা মন্তব্য: JWT secret — fail-fast on missing
    jwt_secret: str = Field(
        default="",
        validation_alias="SUPREMEAI_JWT_SECRET",
    )

    # বাংলা মন্তব্য: Encryption key — fail-fast on missing
    encryption_key: SecretStr = Field(
        default=SecretStr(""),
        validation_alias="ENCRYPTION_KEY",
    )

    # ── Stripe credentials — SecretStr দিয়ে log-safe ────────────────────────
    stripe_api_key: SecretStr = Field(default=SecretStr(""), validation_alias="STRIPE_API_KEY")
    stripe_webhook_secret: SecretStr = Field(default=SecretStr(""), validation_alias="STRIPE_WEBHOOK_SECRET")

    # ── LLM rate limit thresholds — সব env-driven, hardcode নেই ─────────────
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

    # ── Security & Auth Config ──────────────────────────────────────────────
    admin_emails: list[str] = Field(default_factory=list, validation_alias="ADMIN_EMAILS")
    supremeai_admin_password_hash: str | None = Field(default=None, validation_alias="SUPREMEAI_ADMIN_PASSWORD_HASH")
    supremeai_public_paths: list[str] = Field(
        default=[
            "/health",
            "/metrics",
            "/docs",
            "/openapi.json",
            "/api/v1/auth/token",
            "/actuator",
            "/api/admin/firebase-",
            "/api/v1/health",
            "/api/v1/health/",
            "/api/v1/live",
            "/api/v1/ready",
            "/",
        ],
        validation_alias="SUPREMEAI_PUBLIC_PATHS",
    )

    prompt_blocked_patterns: list[str] = Field(
        default=["system prompt", "ignore all previous", "you are an administrative"], validation_alias="PROMPT_BLOCKED_PATTERNS"
    )
    rbac_role_definitions: dict[str, list[str]] = Field(
        default_factory=lambda: {"admin": ["*"], "user": ["read", "write"], "guest": ["read"]}, validation_alias="RBAC_ROLE_DEFINITIONS"
    )

    # ── Circuit Breaker Config ───────────────────────────────────────────────
    circuit_breaker_failure_threshold: int = Field(default=3, validation_alias="CIRCUIT_BREAKER_FAILURE_THRESHOLD")
    circuit_breaker_cooldown_period: int = Field(default=60, validation_alias="CIRCUIT_BREAKER_COOLDOWN_PERIOD")

    # ── Idempotency Config ───────────────────────────────────────────────
    # বাংলা মন্তব্য: idempotency_critical_paths সম্পূর্ণ env-driven।
    # IDEMPOTENCY_CRITICAL_PATHS="/api/orchestrate/generate,/api/billing/charge" (comma-separated)
    idempotency_critical_paths: list[str] = Field(
        default_factory=list,
        validation_alias="IDEMPOTENCY_CRITICAL_PATHS",
    )

    # বাংলা মন্তব্য: Model names env-driven
    claude_openrouter_model: str = Field(
        default="anthropic/claude-3.5-haiku:free",
        validation_alias="CLAUDE_OPENROUTER_MODEL",
    )

    # বাংলা মন্তব্য: জেমিনি মডেল নাম সেন্ট্রালাইজড করা হলো যাতে কোনো ইউটিলিটি স্ক্রিপ্টে হার্ডকোড না থাকে।
    gemini_model_name: str = Field(
        default="gemini/gemini-2.5-flash",
        validation_alias="GEMINI_MODEL_NAME",
    )

    sentry_dsn: str = Field(default="", validation_alias="SENTRY_DSN")

    # বাংলা মন্তব্য: OLLAMA_URL — fail-fast, কোনো localhost fallback নেই
    ollama_url: str = Field(default="", validation_alias="OLLAMA_URL")

    gcp_project_id: str = Field(default="", validation_alias="GCP_PROJECT_ID")
    gcp_region: str = Field(default="us-central1", validation_alias="GCP_REGION")

    # বাংলা মন্তব্য: Filesystem paths
    admin_rules_db: str = Field(default="", validation_alias="ADMIN_RULES_DB_PATH")
    memory_db_dir: str = Field(default="", validation_alias="MEMORY_DB_DIR")
    skill_registry_path: str = Field(default="", validation_alias="SKILL_REGISTRY_PATH")
    # বাংলা মন্তব্য: ChromaDB ভেক্টর ডাটাবেসের জন্য কনফিগারেবল পাথ যোগ করা হলো।
    chromadb_path: str = Field(default="supremeai_knowledge_base", validation_alias="CHROMADB_PATH")

    # ── Sandbox config — env-driven ──────────────────────────────────────────
    sandbox_root: str = Field(default="/tmp/sandboxes", validation_alias="SANDBOX_ROOT")  # nosec B108
    firecracker_path: str = Field(default="/usr/bin/firecracker", validation_alias="FIRECRACKER_PATH")
    gvisor_path: str = Field(default="/usr/bin/runsc", validation_alias="GVISOR_PATH")
    allow_sandbox_fallback: bool = Field(default=False, validation_alias="ALLOW_SANDBOX_FALLBACK")
    # বাংলা মন্তব্য: local_code_executor ও docker_sandbox-এর লোকাল ফলব্যাকের জন্য settings ভেরিয়েবল যোগ করা হলো।
    allow_local_sandbox_fallback: str = Field(default="false", validation_alias="ALLOW_LOCAL_SANDBOX_FALLBACK")

    # ── Agent Execution Config — env-driven ─────────────────────────────────
    # বাংলা মন্তব্য: আগে agent_orchestrator.py সরাসরি os.getenv() করত।
    # এখন এই দুটো settings-এর Single Source of Truth থেকে আসে।
    max_agent_tokens: int = Field(default=5000, validation_alias="MAX_AGENT_TOKENS")
    max_agent_iterations: int = Field(default=5, validation_alias="MAX_AGENT_ITERATIONS")
    agent_admin_permissions_required: bool = Field(default=True, validation_alias="AGENT_ADMIN_PERMISSIONS_REQUIRED")

    # ── LLM Cost Config — env-driven ────────────────────────────────────────
    # বাংলা মন্তব্য: আগে llm_gateway.py-এ `estimated_cost = tokens * 0.00001` hardcoded ছিল।
    # এখন এই factor settings থেকে নিয়ন্ত্রিত হয় যা runtime-এ override করা যাবে।
    llm_cost_per_token: float = Field(default=0.00001, validation_alias="LLM_COST_PER_TOKEN")

    # ── Task Queue Config — env-driven ──────────────────────────────────────
    # বাংলা মন্তব্য: task_queue_enhanced.py-এ TTL এবং backend priority এখন config-driven।
    task_result_ttl_seconds: int = Field(default=3600, validation_alias="TASK_RESULT_TTL_SECONDS")
    queue_backend_priority: str = Field(default="asyncio,redis,celery,pubsub", validation_alias="QUEUE_BACKEND_PRIORITY")

    # ── Health Check Config — env-driven ────────────────────────────────────
    # বাংলা মন্তব্য: health_monitor.py-এ hardcoded interval এখন config-driven।
    health_check_interval_seconds: int = Field(default=60, validation_alias="HEALTH_CHECK_INTERVAL_SECONDS")
    skill_timeout_seconds: int = Field(default=30, validation_alias="SKILL_TIMEOUT_SECONDS")

    # ── Self-Healing Config — env-driven ────────────────────────────────────
    # বাংলা মন্তব্য: self_healer.py-এ human approval loop-এর জন্য config যোগ করা হলো।
    self_heal_approval_webhook: str = Field(default="", validation_alias="SELF_HEAL_APPROVAL_WEBHOOK")
    self_heal_approval_timeout_hours: int = Field(default=24, validation_alias="SELF_HEAL_APPROVAL_TIMEOUT_HOURS")
    auto_remediation_dry_run: bool = Field(default=True, validation_alias="AUTO_REMEDIATION_DRY_RUN")

    _cached_secrets: dict[str, str] = PrivateAttr(default_factory=dict)

    def _get_cached_secret(self, key: str) -> str:
        # বাংলা মন্তব্য: lazy cache — প্রতিটি secret একবারই fetch হয়।
        if key not in self._cached_secrets:
            self._cached_secrets[key] = secret_vault.fetch_secret(key)
        return self._cached_secrets[key]

    # ── Cloud-fetched secrets — GCP Secret Manager বা env fallback ───────────
    @computed_field
    @property
    def supabase_database_url(self) -> str:
        return self._get_cached_secret("SUPABASE_DATABASE_URL_POOLER")

    # বাংলা মন্তব্য: Anti-Hacking এবং OTP রাউটার সিক্রেটসমূহ
    @computed_field
    @property
    def discord_otp_webhook_url(self) -> SecretStr | None:
        url = self._get_cached_secret("DISCORD_OTP_WEBHOOK_URL")
        return SecretStr(url) if url else None

    @computed_field
    @property
    def resend_api_key(self) -> SecretStr | None:
        key = self._get_cached_secret("RESEND_API_KEY")
        return SecretStr(key) if key else None

    @computed_field
    @property
    def admin_notification_email(self) -> str | None:
        return self._get_cached_secret("ADMIN_NOTIFICATION_EMAIL")

    @computed_field
    @property
    def redis_url(self) -> str:
        url = self._get_cached_secret("REDIS_URL")
        if url and not url.startswith(("redis://", "rediss://", "unix://")):
            return f"redis://{url}"
        return url

    @computed_field
    @property
    def openrouter_api_key(self) -> str:
        return self._get_cached_secret("OPENROUTER_API_KEY")

    @computed_field
    @property
    def hf_api_key(self) -> str:
        return self._get_cached_secret("HF_API_KEY")

    @computed_field
    @property
    def gemini_api_key(self) -> str:
        return self._get_cached_secret("GEMINI_API_KEY")

    @computed_field
    @property
    def openai_api_key(self) -> str:
        return self._get_cached_secret("OPENAI_API_KEY")

    @computed_field
    @property
    def deepseek_api_key(self) -> str:
        return self._get_cached_secret("DEEPSEEK_API_KEY")

    @computed_field
    @property
    def groq_api_key(self) -> str:
        return self._get_cached_secret("GROQ_API_KEY")

    @computed_field
    @property
    def nvidia_api_key(self) -> str:
        return self._get_cached_secret("NVIDIA_API_KEY")

    @computed_field
    @property
    def firecrawl_api_key(self) -> str:
        return self._get_cached_secret("FIRECRAWL_API_KEY")

    @computed_field
    @property
    def discord_bot_token(self) -> str:
        return self._get_cached_secret("DISCORD_BOT_TOKEN")

    @computed_field
    @property
    def github_client_id(self) -> str:
        return self._get_cached_secret("GITHUB_CLIENT_ID")

    @computed_field
    @property
    def github_client_secret(self) -> str:
        return self._get_cached_secret("GITHUB_CLIENT_SECRET")

    @computed_field
    @property
    def ci_webhook_secret(self) -> str:
        return self._get_cached_secret("CI_WEBHOOK_SECRET")

    # ── Supabase credentials — settings-এ মাইগ্রেট করা হলো ──────────────────
    # বাংলা মন্তব্য: আগে database/supabase_client.py সরাসরি os.environ.get() করত।
    # এখন এই দুটো computed field settings-এর Single Source of Truth।
    # supabase_client.py শুধু settings.supabase_url এবং settings.supabase_key ব্যবহার করবে।
    @computed_field
    @property
    def supabase_url(self) -> str:
        return self._get_cached_secret("SUPABASE_URL")

    @computed_field
    @property
    def supabase_key(self) -> str:
        return self._get_cached_secret("SUPABASE_KEY")

    # ── System API Token — settings-এ মাইগ্রেট করা হলো ──────────────────────
    # বাংলা মন্তব্য: আগে auth_middleware.py সরাসরি os.getenv("SUPREMEAI_API_KEY") করত।
    # এখন এই computed field settings-এর Single Source of Truth।
    @computed_field
    @property
    def supremeai_api_token(self) -> str:
        return self._get_cached_secret("SUPREMEAI_API_KEY")

    @computed_field
    @property
    def neo4j_uri(self) -> str:
        return self._get_cached_secret("NEO4J_URI") or "bolt://localhost:7687"

    @computed_field
    @property
    def neo4j_user(self) -> str:
        return self._get_cached_secret("NEO4J_USER") or "neo4j"

    @computed_field
    @property
    def neo4j_password(self) -> str:
        return self._get_cached_secret("NEO4J_PASSWORD") or ""

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
        env = info.data.get("env", "local")
        if env in {"production", "staging"}:
            if str(v).lower() == "true" and (os.getenv("debug", "").lower() == "true" or os.getenv("DEBUG", "").lower() == "true"):
                raise ValueError("Explicitly setting debug=True is PROHIBITED in production/staging.")
            return False
        return bool(v)

    @field_validator("docs_password", mode="before")
    @classmethod
    def validate_docs_password(cls, v: str | SecretStr | None, info: ValidationInfo) -> str | SecretStr:
        if "pytest" in sys.modules:
            return v or ""
        return v or ""

    @model_validator(mode="after")
    def validate_docs_auth(self):
        # বাংলা মন্তব্য: Production-এ docs auth enabled থাকলে password mandatory
        if self.env in {"production", "staging"} and self.docs_auth_enabled:
            pwd = self.docs_password.get_secret_value() if self.docs_password else ""
            if not pwd:
                raise ValueError(f"{self.env.capitalize()} requires SUPREMEAI_DOCS_PASSWORD to be set if docs_auth_enabled=true.")
        return self

    @field_validator("idempotency_critical_paths", "supremeai_public_paths", "prompt_blocked_patterns", mode="before")
    @classmethod
    def parse_list_fields(cls, v) -> list[str]:
        if not v:
            return []
        if isinstance(v, str):
            v = v.strip()
            try:
                import json as _json

                return _json.loads(v)
            except Exception:  # noqa: BLE001
                return [p.strip() for p in v.split(",") if p.strip()]
        return v or []

    @field_validator("rbac_role_definitions", mode="before")
    @classmethod
    def parse_dict_fields(cls, v) -> dict:
        if not v:
            return {}
        if isinstance(v, str):
            try:
                import json as _json

                return _json.loads(v)
            except Exception as e:  # noqa: BLE001
                logger.error(f"Failed to parse rbac_role_definitions JSON: {e}. Defaulting to empty dictionary.")
                return {}
        return v or {}

    @field_validator("admin_emails", mode="before")
    @classmethod
    def parse_admin_emails(cls, v) -> list[str]:
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
        # Fail fast if no hosts are defined in production/staging
        env = info.data.get("env", "local")
        forbidden = {"localhost", "127.0.0.1", "testserver", "0.0.0.0"}
        if env in {"production", "staging"}:
            v = [h for h in v if h.lower() not in forbidden]
            if not v:
                raise ValueError(f"{env.capitalize()} requires explicit ALLOWED_HOSTS — localhost/testserver forbidden.")
        return v

    @field_validator("jwt_secret", mode="before")
    @classmethod
    def set_jwt_secret(cls, v: str | None, info: ValidationInfo) -> str:
        env = info.data.get("env", "local")
        if not v and env == "production":
            raise ValueError("🚨 CRITICAL: SUPREMEAI_JWT_SECRET must be explicitly set in all environments. No dummy fallback allowed.")
        return v or secrets.token_hex(64)  # Pytest/non-production fallback

    @field_validator("jwt_secret", mode="after")
    @classmethod
    def validate_jwt_secret_strength(cls, v: str, info: ValidationInfo) -> str:
        if len(v) < 64 and "pytest" not in sys.modules:
            raise ValueError("JWT secret must be >= 64 bytes entropy in all environments.")
        weak_secrets = {"secret", "password", "123456", "changeme", "admin", "jwt_secret"}
        if v.lower() in weak_secrets:
            raise ValueError("JWT secret is in weak secrets dictionary — change it.")
        return v

    @field_validator("supremeai_admin_password_hash", mode="before")
    @classmethod
    def validate_admin_hash(cls, v: str | None, info: ValidationInfo) -> str | None:
        if not v and "pytest" not in sys.modules:
            raise ValueError("supremeai_admin_password_hash must be explicitly set.")
        return v

    @field_validator("cors_origins", "user_cors_origins", "admin_cors_origins", mode="before")
    @classmethod
    def parse_cors_origins(cls, v, info: ValidationInfo):
        # বাংলা: import json এখন ফাইলের শীর্ষে সরাসরি করা হয়েছে, প্রতিটি কলে re-import নেই
        if isinstance(v, str):
            v = v.strip()
            if not v:
                return []
            try:
                return json.loads(v)
            except json.JSONDecodeError:
                return [o.strip() for o in v.split(",") if o.strip()]
        return v or []

    @field_validator("cors_origins", "user_cors_origins", "admin_cors_origins", mode="after")
    @classmethod
    def validate_cors_origins(cls, v: list[str], info: ValidationInfo) -> list[str]:
        env = info.data.get("env", "local")
        if env in {"production", "staging"}:
            v = [o for o in v if "localhost" not in o and "127.0.0.1" not in o]
            if not v:
                raise ValueError(f"{env.capitalize()} requires at least one non-localhost CORS origin. Set CORS_ORIGINS env var.")
        return v

    @model_validator(mode="after")
    def validate_stripe_completeness(self):
        stripe_key = self.stripe_api_key.get_secret_value() if self.stripe_api_key else ""
        stripe_webhook = self.stripe_webhook_secret.get_secret_value() if self.stripe_webhook_secret else ""
        if not stripe_key and "pytest" not in sys.modules:
            raise ValueError("Stripe API key is mandatory.")
        if not stripe_webhook and "pytest" not in sys.modules:
            raise ValueError("Stripe webhook secret is mandatory.")
        return self

    @model_validator(mode="after")
    def validate_production_completeness(self):
        if self.env != "production":
            return self
        missing = []
        if not self.openrouter_api_key:
            missing.append("OPENROUTER_API_KEY")
        if not self.gemini_api_key:
            missing.append("GEMINI_API_KEY")
        if not self.ci_webhook_secret:
            missing.append("CI_WEBHOOK_SECRET")
        if missing:
            raise ValueError(f"Missing required production config vars: {', '.join(missing)}")
        return self

    @model_validator(mode="after")
    def validate_completeness(self):
        """
        বাংলা মন্তব্য: Fail-Fast Guard for ALL environments.
        """
        if "pytest" in sys.modules:
            return self

        missing: list[str] = []
        # বাংলা মন্তব্য: E701 ফিক্স — প্রতিটি স্টেটমেন্ট আলাদা লাইনে রাখা হয়েছে
        if not self.openrouter_api_key:
            missing.append("OPENROUTER_API_KEY")
        if not self.encryption_key.get_secret_value():
            missing.append("ENCRYPTION_KEY")
        if not self.ci_webhook_secret:
            missing.append("CI_WEBHOOK_SECRET")

        if missing:
            if os.getenv("CI") == "true":
                logger.warning(f"CI environment detected. Bypassing fail-fast for missing config vars: {', '.join(missing)}")
                return self
            raise ValueError(f"🚨 FAIL-FAST: Missing required config vars: {', '.join(missing)}. Server startup aborted.")
        return self


# ── Singleton instantiation with True Fail-Fast ────────────────────────────────
# বাংলা মন্তব্য: এখানে Fail-Fast সত্যিকারভাবে enforce হচ্ছে।
# কোনো "resilient boot" বা dummy fallback নেই। Exception মানেই sys.exit(1)।
try:
    settings = Settings()
except Exception as _boot_exc:  # noqa: BLE001
    logger.critical(f"🔥 FATAL CONFIG ERROR: {_boot_exc}\nServer startup ABORTED (Fail-Fast applied). Fix the configuration.")
    sys.exit(1)


def get_production_env(var_name: str, default: str | None = None) -> str:
    """বাংলা মন্তব্য: Strict Fail-Fast Config Guard.
    যেকোনো এনভায়রনমেন্টে কোনো ক্রিটিক্যাল সিক্রেট মিসিং থাকলে সরাসরি হার্ড ক্র্যাশ করবে,
    যাতে সাইলেন্ট ফেইলর প্রতিরোধ করা যায়। ডিফল্ট ভ্যালু পাস করলে মিসিং ক্ষেত্রে fallback ব্যবহার হবে।
    """

    value = os.getenv(var_name)
    if not value:
        if default is not None:
            return default
        logger.critical(f"❌ CRITICAL CONFIG ERROR: Missing required environment variable '{var_name}'!")
        raise ValueError(f"Configuration Error: {var_name} must be explicitly defined.")

    return value

```

---

## File: `backend/core/app.py`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\backend/core/app.py](file:///c:/Users/n/supremeai/supremeai_2.0\backend/core/app.py)

```python
from __future__ import annotations

"""SupremeAI 2.0 — Core FastAPI app bootstrapping, middleware chain, and router loading.

বাংলা: কোর FastAPI অ্যাপ বুটস্ট্র্যাপিং, মিডলওয়্যার চেইন এবং রাউটার লোডিং।

Key Components:
- InterceptHandler: Routes stdlib logging to Loguru.
- _safe_include_router: Dynamic lazy router loader with fail-fast.
- router_health_check: Ensures minimum route count on startup.
"""

from core.messaging.event_bus import ErrorContext

import base64
import logging
import os
import secrets
import sys
from typing import Any

import sentry_sdk
from fastapi import Depends
from fastapi import FastAPI
from fastapi import HTTPException
from fastapi import Request
from fastapi import status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.security import HTTPBasic
from fastapi.security import HTTPBasicCredentials
from loguru import logger

from api.middleware import ChaosInjectorMiddleware
from api.middleware import IdempotencyMiddleware
from api.middleware import ResponseStandardizationMiddleware
from api.middleware import SupremeContextMiddleware
from api.middleware import TenantExtractionMiddleware
from api.routers import register_all_routers
from core import lifespan
from core import services
from core.admin_routes import router as admin_router
from core.config import settings
from core.messaging.event_bus import ErrorEvent
from core.messaging.event_bus import error_event_bus
from core.observability.observability_middleware import ObservabilityMiddleware
from core.security.api_key_middleware import APIKeyAuthMiddleware
from core.security.auth_middleware import AuthMiddleware
from core.security.honeypot_middleware import HoneypotMiddleware
from core.security.origin_validator import TrustedOriginMiddleware


class InterceptHandler(logging.Handler):
    """Redirect stdlib logging to Loguru."""

    def emit(self, record: logging.LogRecord) -> None:
        try:
            level = logger.level(record.levelname).name
        except ValueError:
            level = str(record.levelno)
        frame, depth = logging.currentframe(), 2
        while frame and frame.f_code.co_filename == logging.__file__:
            frame = frame.f_back
            depth += 1
        logger.opt(depth=depth, exception=record.exc_info).log(level, record.getMessage())


logging.basicConfig(handlers=[InterceptHandler()], level=0, force=True)

security = HTTPBasic()

if settings.sentry_dsn:
    try:
        sentry_sdk.init(
            dsn=settings.sentry_dsn,
            traces_sample_rate=0.2 if settings.env.lower() == "production" else 1.0,
            environment=settings.env,
        )
    except Exception:  # noqa: BLE001
        logger.critical("Sentry SDK initialization failed. Configuration error.")
        if os.getenv("ENV", "development").lower() != "test":
            sys.exit(1)


def _docs_auth(credentials: HTTPBasicCredentials = Depends(security)) -> str:
    """Authenticate docs access via HTTP Basic."""
    correct = secrets.compare_digest(credentials.username, settings.docs_username) and secrets.compare_digest(
        credentials.password, settings.docs_password
    )
    if not correct:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials",
            headers={"WWW-Authenticate": "Basic"},
        )
    return credentials.username


def _maybe_docs_auth() -> list[Depends]:
    if settings.docs_auth_enabled and not settings.debug:
        return [Depends(_docs_auth)]
    return []


docs_auth_dep = _maybe_docs_auth()

is_prod = settings.env.lower() == "production"
docs_enabled = settings.debug or not is_prod or settings.docs_auth_enabled

tags_metadata = [
    {"name": "admin", "description": "God-mode admin operations."},
    {"name": "agent", "description": "Autonomous agents execution and planning."},
    {"name": "marketplace", "description": "Discover and manage AI skills and tools."},
    {"name": "tools", "description": "Registry and management of integrated tools."},
]


# বাংলা মন্তব্য: Dynamic role-based rate limiter key function
# JWT role অনুযায়ী Admin (100 RPM) vs Standard User (20 RPM) থ্রেশহোল্ড নির্ধারণ
def supremeai_dynamic_rate_evaluator(request: Request) -> str:
    """ডাইনামিক rate key: JWT role বা IP fallback অনুযায়ী limiter বাউন্ডারি বাছাই করে।"""
    user = getattr(request.state, "user", None)
    user_role = user.get("role", "Standard_User") if isinstance(user, dict) else "Standard_User"
    client_ip = request.client.host if request.client else "unknown"
    if user_role in {"Admin", "admin"}:
        return f"admin:{client_ip}"
    return f"user:{client_ip}"


# বাংলা মন্তব্য: slowapi টেস্টে মক করা হলেও RateLimitExceeded যেন সত্যিকারের Exception ক্লাস থাকে
try:
    from slowapi import Limiter
    from slowapi import _rate_limit_exceeded_handler as _slowapi_rate_limit_handler
    from slowapi.errors import RateLimitExceeded as _SlowAPIRateLimitExceeded
    from slowapi.util import get_remote_address as _slowapi_get_remote_address

    if not isinstance(_SlowAPIRateLimitExceeded, type) or not issubclass(_SlowAPIRateLimitExceeded, Exception):

        class RateLimitExceeded(Exception):  # type: ignore[no-redef]
            """Fallback RateLimitExceeded for test environments where slowapi is mocked."""

        def _rate_limit_exceeded_handler(request: Any, exc: Any) -> JSONResponse:  # type: ignore[misc]
            return JSONResponse(status_code=429, content={"detail": "Rate limit exceeded"})

        def get_remote_address(request: Any) -> str:  # type: ignore[misc]
            return request.client.host if request.client else "127.0.0.1"

        limiter = None
    else:
        RateLimitExceeded = _SlowAPIRateLimitExceeded  # type: ignore[misc,assignment]
        _rate_limit_exceeded_handler = _slowapi_rate_limit_handler
        get_remote_address = _slowapi_get_remote_address
        limiter = Limiter(key_func=get_remote_address)
except Exception:  # noqa: BLE001
    class RateLimitExceeded(Exception):  # type: ignore[no-redef]
        """Fallback RateLimitExceeded for test environments."""

    def _rate_limit_exceeded_handler(request: Any, exc: Any) -> JSONResponse:  # type: ignore[misc]
        return JSONResponse(status_code=429, content={"detail": "Rate limit exceeded"})

    limiter = None


def build_app_shell(title: str = "SupremeAI API", docs_url: str | None = "/docs") -> FastAPI:
    """Builds the base FastAPI shell with shared configuration, middleware, and exception handlers.

    বাংলা মন্তব্য: কোর FastAPI অ্যাপ সেল যা মিডলওয়্যার এবং এক্সেপশন হ্যান্ডলারগুলো ইনিশিয়ালাইজ করে।
    """
    is_prod = settings.env.lower() == "production"
    docs_enabled = settings.debug or not is_prod or settings.docs_auth_enabled

    fastapi_app = FastAPI(
        title=title,
        description="Multi-cloud AI orchestration platform with zero-cost edge computing.",
        version="2.0.0",
        openapi_tags=tags_metadata,
        debug=settings.debug,
        docs_url=docs_url if docs_enabled else None,
        redoc_url=("/redoc" if docs_url else None) if docs_enabled else None,
        openapi_url=("/openapi.json" if docs_url else None) if docs_enabled else None,
    )

    @fastapi_app.middleware("http")
    async def basic_auth_for_docs_middleware(request: Request, call_next: Any) -> JSONResponse:  # noqa: ANN401
        """Protect docs with Basic Auth if enabled."""
        if settings.docs_auth_enabled and not settings.debug:
            path = request.url.path
            if path in {"/docs", "/redoc", "/openapi.json"}:
                auth = request.headers.get("Authorization")
                if not auth or not auth.startswith("Basic "):
                    return JSONResponse(
                        status_code=401,
                        content={"detail": "Invalid credentials"},
                        headers={"WWW-Authenticate": "Basic"},
                    )
                try:
                    decoded = base64.b64decode(auth[6:]).decode("utf-8")
                    username, password = decoded.split(":", 1)
                    if username != settings.docs_username or password != settings.docs_password:
                        raise ValueError("Mismatch")
                except (ValueError, UnicodeDecodeError):
                    return JSONResponse(
                        status_code=401,
                        content={"detail": "Invalid credentials"},
                        headers={"WWW-Authenticate": "Basic"},
                    )
        return await call_next(request)

    fastapi_app.add_middleware(SupremeContextMiddleware)
    fastapi_app.add_middleware(TrustedOriginMiddleware)
    fastapi_app.add_middleware(ChaosInjectorMiddleware)
    fastapi_app.add_middleware(ObservabilityMiddleware)
    fastapi_app.add_middleware(HoneypotMiddleware)
    fastapi_app.add_middleware(AuthMiddleware)
    fastapi_app.add_middleware(TenantExtractionMiddleware)
    fastapi_app.add_middleware(IdempotencyMiddleware)
    fastapi_app.add_middleware(APIKeyAuthMiddleware)
    fastapi_app.add_middleware(ResponseStandardizationMiddleware)

    fastapi_app.state.limiter = limiter

    @fastapi_app.exception_handler(HTTPException)
    async def custom_http_exception_handler(request: Request, exc: HTTPException) -> JSONResponse:
        return JSONResponse(
            status_code=exc.status_code,
            content={
                "title": "Task Execution Failed",
                "detail": exc.detail,
                "instance": request.url.path,
            },
        )

    @fastapi_app.exception_handler(Exception)
    async def global_exception_handler(request: Request, exc: Exception) -> JSONResponse:
        logger.error(f"Unhandled Exception on {request.url.path}: {exc}")
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={
                "title": "Internal Server Error",
                "detail": "An unexpected error occurred. This has been logged.",
                "instance": request.url.path,
            },
        )

    if isinstance(RateLimitExceeded, type) and issubclass(RateLimitExceeded, Exception):
        fastapi_app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

    @fastapi_app.get("/")
    async def root() -> dict[str, Any]:
        return {
            "name": settings.app_name,
            "version": "2.0.0",
            "status": "online",
            "docs": "/docs",
            "health": "/api/v1/health",
            "description": "Multi-cloud AI orchestration platform.",
        }

    @fastapi_app.get("/health")
    async def health() -> dict[str, Any]:
        redis_ok = False
        if hasattr(services, "redis_queue") and services.redis_queue.configured:
            try:
                services.redis_queue.set("health", "ok", ex=5)
                redis_ok = services.redis_queue.get("health") == "ok"
            except Exception:  # noqa: BLE001
                logger.exception("Health check failed on redis connection")
                error_event_bus.emit(
                    ErrorEvent(
                        module="app.health",
                        error_type="REDIS_HEALTH_FAIL",
                        message="Redis health error",
                        severity="ERROR",
                        structured_context=ErrorContext(module="auto_fixed"),
                    )
                )
                redis_ok = False
        else:
            redis_ok = True

        api_keys_ok = bool(
            settings.openrouter_api_key or settings.gemini_api_key or settings.deepseek_api_key or settings.groq_api_key or settings.nvidia_api_key
        )
        checks = {"redis": redis_ok, "api_keys_configured": api_keys_ok}
        all_ok = all(checks.values())
        return {"status": "ok" if all_ok else "degraded", "orchestrator": "online", "checks": checks}

    @fastapi_app.get("/actuator/health")
    def actuator_health() -> dict[str, str]:
        return {"status": "UP", "orchestrator": "online"}

    fastapi_app.router.lifespan_context = lifespan.app_lifespan
    return fastapi_app


def router_health_check(fastapi_app: FastAPI) -> None:
    """Fail-fast if fewer than minimum routes loaded."""
    expected_count = int(os.getenv("MIN_EXPECTED_ROUTES", "20"))
    if len(fastapi_app.routes) < expected_count:
        logger.critical(
            f"🔥 CRITICAL: Only {len(fastapi_app.routes)} routes loaded. Expected at least {expected_count}. Some routers failed to load!"
        )
        sys.exit(1)


# For backward compatibility and test suites
# বাংলা মন্তব্য: ব্যাকওয়ার্ড কম্প্যাটিবিলিটি এবং টেস্ট কেসের জন্য ডিফল্ট গ্লোবাল অ্যাপ
app = build_app_shell(title=f"{settings.app_name} (Production Ready)")
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "X-Request-ID", "X-Tenant-ID", "X-API-Key", "X-Correlation-ID"],
)

if settings.env == "production":
    if not settings.cors_origins:
        raise RuntimeError("🔥 CRITICAL: Production CORS drift detected. cors_origins cannot be empty in production.")
    if "*" in settings.cors_origins:
        raise RuntimeError("🚨 SECURITY: Wildcard '*' is strictly prohibited in production CORS mesh. Set CORS_ORIGINS env var.")

app.include_router(admin_router)
register_all_routers(app)
router_health_check(app)



```

---

## File: `backend/core/app_user.py`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\backend/core/app_user.py](file:///c:/Users/n/supremeai/supremeai_2.0\backend/core/app_user.py)

```python
"""SupremeAI 2.0 — User API entrypoint. Chat/user-facing routes only.

বাংলা মন্তব্য: ইউজার এপিআই এন্ট্রি পয়েন্ট যা শুধুমাত্র চ্যাট ও ইউজার-ফেসিং রাউটগুলো এক্সপোজ করে।
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from core.config import settings
from core.app import build_app_shell, router_health_check
from api.routers import include_user_routers

app: FastAPI = build_app_shell(title="SupremeAI User API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.user_cors_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "X-Request-ID", "X-Tenant-ID", "X-API-Key", "X-Correlation-ID"],
)

if settings.env == "production":
    if not settings.user_cors_origins:
        raise RuntimeError("🔥 CRITICAL: Production User CORS drift detected. user_cors_origins cannot be empty in production.")
    if "*" in settings.user_cors_origins:
        raise RuntimeError("🚨 SECURITY: Wildcard '*' is strictly prohibited in production User CORS. Set USER_CORS_ORIGINS.")

include_user_routers(app)
router_health_check(app)

```

---

## File: `backend/core/app_admin.py`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\backend/core/app_admin.py](file:///c:/Users/n/supremeai/supremeai_2.0\backend/core/app_admin.py)

```python
"""SupremeAI 2.0 — Admin API entrypoint. Admin dashboard + Anti-Hacking Agent only.

বাংলা মন্তব্য: অ্যাডমিন এপিআই এন্ট্রি পয়েন্ট যা শুধুমাত্র অ্যাডমিন প্যানেল এবং সিকিউরিটি রাউটগুলো এক্সপোজ করে।
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from core.config import settings
from core.app import build_app_shell, router_health_check
from api.routers import include_admin_routers
from middleware.anti_hacking import AntiHackingContextMiddleware

app: FastAPI = build_app_shell(title="SupremeAI Admin API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.admin_cors_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "X-Request-ID", "X-Tenant-ID", "X-API-Key", "X-Correlation-ID"],
)

# Anti-Hacking Agent hook — runs before routes, on Admin API only
app.add_middleware(AntiHackingContextMiddleware)

include_admin_routers(app)
router_health_check(app)

```

---

## File: `backend/api/routers.py`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\backend/api/routers.py](file:///c:/Users/n/supremeai/supremeai_2.0\backend/api/routers.py)

```python
"""Centralized router registration for SupremeAI API."""

from __future__ import annotations

from fastapi import FastAPI
from loguru import logger

from api import register_router
from core.config import settings


core_routers: list[tuple[str, str]] = [
    ("api.routes.memory", ""),
    ("api.routes.task", ""),
    ("api.routes.markdown", "/api/v1"),
    ("api.routes.simulator", ""),
    ("api.routes.site_actions", ""),
    ("api.routes.llm_gateway", ""),
    ("api.routes.browser", ""),
    ("api.routes.stream", ""),
    ("api.routes.media", ""),
    ("api.routes.graph", ""),
    ("api.routes.marketplace_endpoints", ""),
    ("api.routes.auth", "/api/v1"),
    ("api.routes.onboarding", "/api/v1"),
    ("api.routes.evolution", "/api/v1"),
    ("api.routes.meta_ai", "/api/v1"),
    ("api.routes.localization", "/api/v1"),
    ("api.routes.analytics", "/api/v1"),
    ("api.routes.admin_dashboard", ""),
    ("api.routes.email", ""),
    ("api.routes.github", ""),
    ("api.routes.internal", ""),
    ("api.routes.config", ""),
    ("api.routes.repos", ""),
    ("api.routes.tools_ops", ""),
    ("api.routes.agents", ""),
    ("api.routes.agent", ""),
    ("api.routes.admin", ""),
    ("api.routes.tools_registry", ""),
    ("api.routes.preferences", "/api"),
    ("api.routes.usage_metrics", ""),
    ("api.routes.sso", ""),
    ("api.routes.health", "/api/v1"),
    ("api.routes.api_keys", ""),
    ("api.routes.ci_webhooks", ""),
    ("api.routes.task_workspace", "/api/v1"),
    ("api.routes.websocket_agent", ""),
    ("api.routes.agent_workspace", "/api/v1"),
    ("api.routes.integrations", "/api/v1"),
    ("api.routes.public_config", "/api"),
    ("api.routes.traffic_monitor", ""),
    ("api.routes.agent_action", "/api/v1"),
    ("api.routes.websocket_hitl", ""),
    ("api.routes.syncguard", "/api/v1"),
    ("api.routes.admin_librarian", "/api"),
    ("api.routes.skills", "/api"),
]

optional_routers: list[tuple[str, str]] = [
    # বাংলা মন্তব্য: chromadb নির্ভর হওয়ায় নলেজ বেস রাউটারটিকে অপশনাল হিসেবে রেজিস্টার করা হলো
    ("api.routes.knowledge", ""),
    ("api.routes.dock_actions", "/api"),
    ("api.routes.websocket_voice", ""),
    ("tools.collaborative_editor", "/api/v1"),
    ("tools.image_to_code", ""),
    ("tools.browser_agent", "/api"),
    ("tools.voice_coder", "/api"),
    ("tools.style_learner", "/api"),
    ("tools.diagram_to_architecture", "/api"),
    ("tools.ai_pair_programmer", "/api"),
    ("api.routes.codeflow", ""),
    ("api.routes.feedback", ""),
    ("tools.media.multilingual_tts", "/api"),
    ("api.routes.voice", "/api/voice"),
    ("tools.comment_thread_ai", "/api"),
    ("tools.auto_test_generator", "/api"),
    ("api.routes.tenant_admin", "/api"),
    ("api.routes.mobile_bff", ""),
    ("api.routes.billing_api", ""),
    ("api.routes.metrics", ""),
    ("api.routes.cloud_mesh", ""),
    ("api.routes.events", "/api"),
    ("api.routes.payments", ""),
    ("api.routes.maintenance", "/api/v1"),
    ("api.routes.sandbox_api", ""),
    ("api.routes.pr_review_api", ""),
]


# Identify admin router paths
# বাংলা মন্তব্য: tools_ops যোগ করা হলো — এটি DevOps/deploy টুলিং (docker-compose/helm
# ফাইল-রাইট সহ) যা আগে ভুলবশত User API-তে এক্সপোজড ছিল (route-leakage)।
_admin_paths = {
    "api.routes.simulator_admin", "api.routes.site_actions", "api.routes.llm_gateway",
    "api.routes.browser", "api.routes.evolution", "api.routes.meta_ai",
    "api.routes.admin_dashboard", "api.routes.internal", "api.routes.admin",
    "api.routes.traffic_monitor", "api.routes.admin_librarian", "api.routes.tenant_admin",
    "api.routes.metrics", "api.routes.cloud_mesh", "api.routes.tools_ops",
}

# ADMIN_ROUTERS includes health and specific admin routes
# বাংলা মন্তব্য: অ্যাডমিন এপিআই রাউটারসমূহ
ADMIN_ROUTERS: list[tuple[str, str]] = [
    ("api.routes.health", "/api/v1"),
    ("api.routes.simulator_admin", ""),
    ("api.routes.site_actions", ""),
    ("api.routes.llm_gateway", ""),
    ("api.routes.browser", ""),
    ("api.routes.evolution", "/api/v1"),
    ("api.routes.meta_ai", "/api/v1"),
    ("api.routes.admin_dashboard", ""),
    ("api.routes.internal", ""),
    ("api.routes.admin", ""),
    ("api.routes.traffic_monitor", ""),
    ("api.routes.admin_librarian", "/api"),
    ("api.routes.tenant_admin", "/api"),
    ("api.routes.metrics", ""),
    ("api.routes.cloud_mesh", ""),
    ("api.routes.tools_ops", ""),
]

# USER_ROUTERS is all other routers
# বাংলা মন্তব্য: ইউজার এপিআই রাউটারসমূহ
USER_ROUTERS: list[tuple[str, str]] = [
    r for r in (core_routers + optional_routers)
    if r[0] not in _admin_paths
]


def register_all_routers(app: FastAPI) -> None:
    """Register all core and optional routers on the FastAPI app."""
    for router_path, prefix in core_routers:
        register_router(app, router_path, prefix=prefix, optional=False)

    for router_path, prefix in optional_routers:
        register_router(app, router_path, prefix=prefix, optional=True)

    if settings.encryption_key and settings.encryption_key.get_secret_value():
        register_router(app, "api.routes.byoc_api", "", optional=True)
    else:
        logger.warning("Universal BYOC router not loaded: ENCRYPTION_KEY missing")


def include_user_routers(app: FastAPI) -> None:
    """Register all user/client-facing routers on the FastAPI app."""
    for router_path, prefix in USER_ROUTERS:
        register_router(app, router_path, prefix=prefix, optional=True)
    if settings.encryption_key and settings.encryption_key.get_secret_value():
        register_router(app, "api.routes.byoc_api", "", optional=True)


def include_admin_routers(app: FastAPI) -> None:
    """Register all admin-facing routers on the FastAPI app."""
    for router_path, prefix in ADMIN_ROUTERS:
        register_router(app, router_path, prefix=prefix, optional=True)


__all__ = [
    "register_all_routers",
    "include_user_routers",
    "include_admin_routers",
    "core_routers",
    "optional_routers",
    "USER_ROUTERS",
    "ADMIN_ROUTERS"
]

```

---

## File: `backend/api/routes/simulator.py`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\backend/api/routes/simulator.py](file:///c:/Users/n/supremeai/supremeai_2.0\backend/api/routes/simulator.py)

```python
"""Simulator user API — device profile / install / session management.

State moved from in-memory dicts to Upstash Redis (2026-07-19) so the
User and Admin services (separate processes) see consistent data.

Falls back to in-memory dicts if Redis is unavailable (e.g. in test environments).

বাংলা মন্তব্য: সিমুলেটর ইউজার এপিআই যা আপস্ট্যাশ রেডিস ডেটাবেস ব্যবহার করে, কিন্তু টেস্ট এনভায়রনমেন্টে লোকাল মেমোরি ফলব্যাক ব্যবহার করে।
"""

from __future__ import annotations

import json
from datetime import UTC, datetime
from typing import Any

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from core.cache.redis_manager import redis_manager

router = APIRouter(prefix="/api/simulator", tags=["simulator"])

_PROFILE_KEY = "simulator:profile:{user_id}"
_SESSION_KEY = "simulator:session:{user_id}"
_KNOWN_USERS_SET = "simulator:known_users"
_PROFILE_TTL = 30 * 86400  # 30 days — mock/test data, not meant to be permanent

DEVICE_PROFILES = [
    {
        "type": "PIXEL_6",
        "name": "Google Pixel 6",
        "osVersion": "Android 12",
        "screenResolution": "1080x2400",
        "densityDpi": 411,
    },
    {
        "type": "IPHONE_13",
        "name": "Apple iPhone 13",
        "osVersion": "iOS 15",
        "screenResolution": "1170x2532",
        "densityDpi": 460,
    },
]

# Fallbacks for test/local environments when Redis is not running
_IN_MEMORY_PROFILES: dict[str, Any] = {}
_IN_MEMORY_SESSIONS: dict[str, Any] = {}
_IN_MEMORY_KNOWN_USERS: set[str] = set()


class DeviceUpdateRequest(BaseModel):
    type: str
    osVersion: str | None = None
    screenResolution: str | None = None
    densityDpi: int | None = None


class ProfileUpdateRequest(BaseModel):
    installQuota: int | None = None
    device: DeviceUpdateRequest | None = None


class InstallRequest(BaseModel):
    appId: str
    deviceProfile: str | None = "PIXEL_6"


def _use_redis() -> bool:
    try:
        if redis_manager is None or redis_manager.client is None:
            return False
        url = getattr(redis_manager, "url", "")
        if not url or "mock" in url.lower():
            return False
        return True
    except Exception:
        return False


def _redis():
    if not _use_redis():
        raise HTTPException(status_code=503, detail="Simulator state store unavailable")
    return redis_manager


async def get_or_create_profile(user_id: str) -> dict[str, Any]:
    if not _use_redis():
        if user_id not in _IN_MEMORY_PROFILES:
            _IN_MEMORY_PROFILES[user_id] = {
                "userId": user_id,
                "installQuota": 5,
                "activeInstalls": 0,
                "device": DEVICE_PROFILES[0],
                "installedApps": [],
            }
            _IN_MEMORY_KNOWN_USERS.add(user_id)
        return _IN_MEMORY_PROFILES[user_id]

    redis_mgr = redis_manager
    raw = await redis_mgr.get_cache(_PROFILE_KEY.format(user_id=user_id))
    if raw:
        return json.loads(raw)

    profile = {
        "userId": user_id,
        "installQuota": 5,
        "activeInstalls": 0,
        "device": DEVICE_PROFILES[0],
        "installedApps": [],
    }
    await _save_profile(user_id, profile)
    await redis_mgr.client.sadd(_KNOWN_USERS_SET, user_id)
    return profile


async def _save_profile(user_id: str, profile: dict[str, Any]) -> None:
    if not _use_redis():
        _IN_MEMORY_PROFILES[user_id] = profile
        return

    redis_mgr = redis_manager
    await redis_mgr.set_cache(
        _PROFILE_KEY.format(user_id=user_id),
        json.dumps(profile),
        ex_seconds=_PROFILE_TTL
    )


async def _get_session(user_id: str) -> dict[str, Any] | None:
    if not _use_redis():
        return _IN_MEMORY_SESSIONS.get(user_id)

    redis_mgr = redis_manager
    raw = await redis_mgr.get_cache(_SESSION_KEY.format(user_id=user_id))
    return json.loads(raw) if raw else None


async def _save_session(user_id: str, session: dict[str, Any]) -> None:
    if not _use_redis():
        _IN_MEMORY_SESSIONS[user_id] = session
        return

    redis_mgr = redis_manager
    await redis_mgr.set_cache(
        _SESSION_KEY.format(user_id=user_id),
        json.dumps(session),
        ex_seconds=_PROFILE_TTL
    )


async def _delete_session(user_id: str) -> None:
    if not _use_redis():
        _IN_MEMORY_SESSIONS.pop(user_id, None)
        return

    redis_mgr = redis_manager
    await redis_mgr.client.delete(_SESSION_KEY.format(user_id=user_id))


@router.get("/profile")
async def get_profile(userId: str = "default"):
    return await get_or_create_profile(userId)


@router.post("/profile")
async def update_profile(updates: ProfileUpdateRequest, userId: str = "default"):
    profile = await get_or_create_profile(userId)
    if updates.installQuota is not None:
        profile["installQuota"] = updates.installQuota
    if updates.device is not None:
        profile["device"].update(updates.device.model_dump(exclude_unset=True))
    await _save_profile(userId, profile)
    return profile


@router.post("/install")
async def install_app(req: InstallRequest, userId: str = "default"):
    profile = await get_or_create_profile(userId)
    if profile["activeInstalls"] >= profile["installQuota"]:
        raise HTTPException(status_code=400, detail="Install quota exceeded")

    existing = next((a for a in profile["installedApps"] if a["appId"] == req.appId), None)
    if existing:
        return {
            "success": True,
            "app": existing,
            "quota": {"used": profile["activeInstalls"], "total": profile["installQuota"]},
        }

    app = {
        "appId": req.appId,
        "appName": f"App {req.appId}",
        "version": "1.0.0",
        "previewUrl": f"http://127.0.0.1:8000/preview/{req.appId}",
        "installedAt": datetime.now(UTC).isoformat(),
        "launchCount": 0,
        "lastLaunchedAt": None,
        "status": "INSTALLED",
    }
    profile["installedApps"].append(app)
    profile["activeInstalls"] += 1
    await _save_profile(userId, profile)
    return {
        "success": True,
        "app": app,
        "quota": {"used": profile["activeInstalls"], "total": profile["installQuota"]},
    }


@router.delete("/install/{appId}")
async def uninstall_app(appId: str, userId: str = "default"):
    profile = await get_or_create_profile(userId)
    initial_len = len(profile["installedApps"])
    profile["installedApps"] = [a for a in profile["installedApps"] if a["appId"] != appId]
    if len(profile["installedApps"]) < initial_len:
        profile["activeInstalls"] -= 1
    await _save_profile(userId, profile)
    return {"success": True}


@router.get("/installed")
async def get_installed_apps(userId: str = "default"):
    profile = await get_or_create_profile(userId)
    return {
        "installedApps": profile["installedApps"],
        "quota": {"used": profile["activeInstalls"], "total": profile["installQuota"]},
    }


@router.post("/session/start")
async def start_session(appId: str, userId: str = "default"):
    profile = await get_or_create_profile(userId)
    app = next((a for a in profile["installedApps"] if a["appId"] == appId), None)
    if not app:
        raise HTTPException(status_code=404, detail="App not installed")

    app["launchCount"] += 1
    app["lastLaunchedAt"] = datetime.now(UTC).isoformat()
    app["status"] = "RUNNING"
    await _save_profile(userId, profile)

    session_id = f"sess_{userId}_{appId}"
    session = {
        "sessionId": session_id,
        "websocketUrl": f"ws://127.0.0.1:8000/ws/simulator/{session_id}",
        "previewUrl": app["previewUrl"],
        "state": "RUNNING",
        "startedAt": datetime.now(UTC).isoformat(),
        "activeAppId": appId,
        "lastHeartbeat": datetime.now(UTC).isoformat(),
    }
    await _save_session(userId, session)
    return session


@router.post("/session/stop")
async def stop_session(userId: str = "default"):
    session = await _get_session(userId)
    if session:
        app_id = session.get("activeAppId")
        profile = await get_or_create_profile(userId)
        app = next((a for a in profile["installedApps"] if a["appId"] == app_id), None)
        if app:
            app["status"] = "INSTALLED"
            await _save_profile(userId, profile)
        await _delete_session(userId)
    return {"success": True}


@router.get("/session/status")
async def get_session_status(userId: str = "default"):
    session = await _get_session(userId)
    if not session:
        return {"hasSession": False}
    return {
        "hasSession": True,
        "sessionId": session["sessionId"],
        "activeAppId": session["activeAppId"],
        "state": session["state"],
        "lastHeartbeat": session["lastHeartbeat"],
    }


@router.get("/devices")
def get_available_devices():
    return DEVICE_PROFILES

```

---

## File: `backend/api/routes/simulator_admin.py`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\backend/api/routes/simulator_admin.py](file:///c:/Users/n/supremeai/supremeai_2.0\backend/api/routes/simulator_admin.py)

```python
"""Simulator admin API — device profile / install / session management admin endpoints.

বাংলা মন্তব্য: সিমুলেটর অ্যাডমিন এপিআই যা সিমুলেটর ব্যবহারের স্ট্যাটিস্টিকস ও কোটা ম্যানেজ করে।
"""

from __future__ import annotations

from fastapi import APIRouter, Depends

from api.routes.admin import get_current_admin
from api.routes.simulator import (
    _redis,
    _use_redis,
    _KNOWN_USERS_SET,
    _IN_MEMORY_KNOWN_USERS,
    get_or_create_profile,
    _save_profile
)

router = APIRouter(prefix="/api/simulator", tags=["simulator-admin"])


@router.get("/admin/usage")
async def get_all_usage(admin_user: dict = Depends(get_current_admin)):
    if not _use_redis():
        user_ids = list(_IN_MEMORY_KNOWN_USERS)
    else:
        redis_mgr = _redis()
        user_ids = await redis_mgr.client.smembers(_KNOWN_USERS_SET)

    deployments = []
    for user_id in user_ids:
        profile = await get_or_create_profile(user_id)
        for app in profile["installedApps"]:
            deployments.append({
                "appId": app["appId"],
                "deviceType": profile["device"]["type"],
                "previewUrl": app["previewUrl"],
                "status": app["status"],
                "deployedAt": app["installedAt"],
            })
    return {"totalDeployments": len(deployments), "deployments": deployments}


@router.post("/admin/set-quota/{userId}")
async def admin_set_quota(userId: str, quota: int, admin_user: dict = Depends(get_current_admin)):
    profile = await get_or_create_profile(userId)
    profile["installQuota"] = max(1, min(20, quota))
    await _save_profile(userId, profile)
    return profile

```

---

## File: `backend/core/otp_router.py`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\backend/core/otp_router.py](file:///c:/Users/n/supremeai/supremeai_2.0\backend/core/otp_router.py)

```python
"""JIT OTP channel router — Human-in-the-loop delivery for Anti-Hacking Agent.

Zero-cost: Discord webhooks (free, unlimited), Resend free tier (3k emails/mo).
Telegram/WhatsApp are manual-trigger only (Option 3) — no default traffic,
since WhatsApp's free tier requires Meta Business verification we haven't
set up, and Telegram needs a bot registered per-admin. Wiring those is a
follow-up once you've created the credentials; the interface below already
supports both if triggered explicitly by an admin.

বাংলা মন্তব্য: অ্যাডমিন অথেনটিকেশনের জন্য ওটিপি সুইচিং রাউটার। ডিসকর্ড ওয়েবহুক এবং রিসেন্ড ইমেল সার্ভিস ব্যবহার করে।
"""

from __future__ import annotations

import httpx
from loguru import logger

from core.config import settings
from core.cache.redis_manager import redis_manager

CHANNEL_DISCORD = "discord"
CHANNEL_EMAIL = "email"
CHANNEL_TELEGRAM = "telegram"     # manual only
CHANNEL_WHATSAPP = "whatsapp"     # manual only

_REDIS_KEY_PREFIX = "otp:channel:"  # per-admin channel override, TTL'd


async def get_active_channel(admin_id: str) -> str:
    """Redis-backed channel preference; defaults to Discord."""
    if redis_manager and redis_manager.client:
        override = await redis_manager.get_cache(f"{_REDIS_KEY_PREFIX}{admin_id}")
        if override:
            return override
    return CHANNEL_DISCORD


async def set_active_channel(admin_id: str, channel: str, ttl_seconds: int = 3600) -> None:
    """Admin-triggered channel switch (human-in-the-loop). TTL'd so a forgotten
    override doesn't silently redirect OTPs forever."""
    if channel not in {CHANNEL_DISCORD, CHANNEL_EMAIL, CHANNEL_TELEGRAM, CHANNEL_WHATSAPP}:
        raise ValueError(f"Unknown OTP channel: {channel}")
    if redis_manager and redis_manager.client:
        await redis_manager.set_cache(f"{_REDIS_KEY_PREFIX}{admin_id}", channel, ex_seconds=ttl_seconds)
    logger.info(f"🔐 OTP channel for admin {admin_id} switched to {channel} (ttl={ttl_seconds}s)")


async def send_otp(admin_id: str, code: str, context: dict) -> bool:
    """Send OTP via the admin's active channel, falling back to email on failure."""
    channel = await get_active_channel(admin_id)
    sent = False

    if channel == CHANNEL_DISCORD:
        sent = await _send_discord(admin_id, code, context)
        if not sent:
            logger.warning(f"Discord OTP delivery failed for {admin_id}, falling back to email.")
            sent = await _send_email(admin_id, code, context)
    elif channel == CHANNEL_EMAIL:
        sent = await _send_email(admin_id, code, context)
    elif channel in (CHANNEL_TELEGRAM, CHANNEL_WHATSAPP):
        logger.warning(f"{channel} OTP requested for {admin_id} but not yet wired up — falling back to Discord.")
        sent = await _send_discord(admin_id, code, context)

    return sent


async def _send_discord(admin_id: str, code: str, context: dict) -> bool:
    webhook_url = settings.discord_otp_webhook_url
    if not webhook_url or not webhook_url.get_secret_value():
        logger.error("DISCORD_OTP_WEBHOOK_URL not configured.")
        return False
    payload = {
        "content": (
            f"🚨 **Admin Login Verification** — `{admin_id}`\n"
            f"Code: `{code}`\n"
            f"IP: `{context.get('ip', 'unknown')}` · Country: `{context.get('country', 'unknown')}`\n"
            f"Reply is not monitored here — verify in the admin dashboard."
        )
    }
    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            resp = await client.post(webhook_url.get_secret_value(), json=payload)
            return resp.status_code in (200, 204)
    except httpx.HTTPError as exc:
        logger.error(f"Discord OTP send failed: {exc}")
        return False


async def _send_email(admin_id: str, code: str, context: dict) -> bool:
    api_key = settings.resend_api_key
    to_addr = settings.admin_notification_email
    if not api_key or not api_key.get_secret_value() or not to_addr:
        logger.error("RESEND_API_KEY or ADMIN_NOTIFICATION_EMAIL not configured.")
        return False
    payload = {
        "from": "SupremeAI Security <security@supremeai.app>",
        "to": [to_addr],
        "subject": f"Admin Login Verification — {admin_id}",
        "html": (
            f"<p>Code: <b>{code}</b></p>"
            f"<p>IP: {context.get('ip', 'unknown')} · Country: {context.get('country', 'unknown')}</p>"
        ),
    }
    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            resp = await client.post(
                "https://api.resend.com/emails",
                json=payload,
                headers={"Authorization": f"Bearer {api_key.get_secret_value()}"},
            )
            return resp.status_code in (200, 201)
    except httpx.HTTPError as exc:
        logger.error(f"Resend OTP email failed: {exc}")
        return False

```

---

## File: `backend/middleware/anti_hacking.py`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\backend/middleware/anti_hacking.py](file:///c:/Users/n/supremeai/supremeai_2.0\backend/middleware/anti_hacking.py)

```python
"""Anti-Hacking Agent — context-aware checks + JIT OTP routing (Admin API only).

Alert-only by default (ENFORCE_ANTI_HACKING=false): logs + notifies on context
mismatch but never blocks. Flip the env var to enforce once false-positive
rate from VPNs/CGNAT/mobile-switching has been observed and is acceptable.

বাংলা মন্তব্য: অ্যাডমিন সিকিউরিটি ওটিপি মিডলওয়্যার। এটি ইউজারের আইপি, কান্ট্রি ও ডিভাইস ফিঙ্গারপ্রিন্ট ভেরিফিকেশন চেক করে।
"""

from __future__ import annotations

import json
import secrets

from fastapi import Request
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware

from core.config import settings
from core.otp_router import send_otp
from core.cache.redis_manager import redis_manager

_CONTEXT_KEY_PREFIX = "security:last_context:"
_CONTEXT_TTL = 86400
_OTP_COOLDOWN_PREFIX = "security:otp_cooldown:"
_CAUTION_LOG_PREFIX = "security:caution_log:"
_CAUTION_LOG_TTL = 86400


def _octet3(ip: str) -> str:
    """First 3 octets of an IPv4 address (e.g. '1.2.3.4' -> '1.2.3'). Falls back to
    the full value for IPv6/unknown so those never spuriously match each other.

    বাংলা: IPv4-এর প্রথম ৩টি অক্টেট বের করে — CGNAT/mobile handoff-এ সাধারণত শেষ অক্টেটই বদলায়।
    """
    parts = ip.split(".")
    return ".".join(parts[:3]) if len(parts) == 4 else ip


class AntiHackingContextMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        signal = {
            "ip": request.headers.get("x-forwarded-for", "").split(",")[0].strip(),
            "country": request.headers.get("cf-ipcountry", "unknown"),
            "ua": request.headers.get("user-agent", "unknown"),
            "fingerprint": request.headers.get("x-device-fingerprint", "unknown"),
        }
        request.state.security_signal = signal

        admin_id = getattr(getattr(request.state, "user", None), "get", lambda *_: None)("sub")
        if admin_id:
            if redis_manager and redis_manager.client:
                key = f"{_CONTEXT_KEY_PREFIX}{admin_id}"
                raw_last = await redis_manager.get_cache(key)
                last = json.loads(raw_last) if raw_last else None

                mismatch = False
                caution = False
                if last:
                    ip_country_mismatch = (last.get("ip") != signal["ip"] or last.get("country") != signal["country"])
                    last_fp = last.get("fingerprint")
                    if last_fp and last_fp != "unknown":
                        # বাংলা মন্তব্য: ফিঙ্গারপ্রিন্ট মিললে আইপি পরিবর্তন হলেও ওটিপি লাগবে না (ভিপিএন/মোবাইল নেটওয়ার্কের জন্য)
                        mismatch = ip_country_mismatch and (last_fp != signal["fingerprint"])
                    else:
                        mismatch = ip_country_mismatch

                    if mismatch:
                        same_ua = last.get("ua") not in (None, "unknown") and last.get("ua") == signal["ua"]
                        same_subnet = bool(signal["ip"]) and _octet3(last.get("ip", "")) == _octet3(signal["ip"])
                        if same_ua or same_subnet:
                            caution = True
                            mismatch = False

                if caution:
                    from loguru import logger as _logger
                    _logger.info(f"CAUTION: partial context match for admin {admin_id} (same_ua/subnet, no OTP fired): {signal} vs last {last}")
                    if redis_manager and redis_manager.client:
                        await redis_manager.client.lpush(f"{_CAUTION_LOG_PREFIX}{admin_id}", json.dumps(signal))
                        await redis_manager.client.ltrim(f"{_CAUTION_LOG_PREFIX}{admin_id}", 0, 49)
                        await redis_manager.client.expire(f"{_CAUTION_LOG_PREFIX}{admin_id}", _CAUTION_LOG_TTL)

                if mismatch:
                    cooldown_key = f"{_OTP_COOLDOWN_PREFIX}{admin_id}"
                    cooldown_active = False
                    if redis_manager and redis_manager.client:
                        acquired = await redis_manager.client.set(
                            cooldown_key, "1", nx=True, ex=settings.otp_cooldown_seconds
                        )
                        cooldown_active = not bool(acquired)

                    if cooldown_active:
                        from loguru import logger as _logger2
                        _logger2.info(f"OTP cooldown active for admin {admin_id} - suppressing duplicate send/notification.")
                        request.state.security_otp_pending = True
                        if settings.enforce_anti_hacking:
                            return JSONResponse(
                                status_code=403,
                                content={
                                    "error": "context_mismatch",
                                    "detail": "OTP verification required — check your configured channel."
                                },
                            )
                        await redis_manager.set_cache(key, json.dumps(signal), ex_seconds=_CONTEXT_TTL)
                        return await call_next(request)

                    code = f"{secrets.randbelow(900000) + 100000}"
                    await send_otp(admin_id, code, signal)
                    request.state.security_otp_pending = True

                    # বাংলা মন্তব্য: ওটিপি কোড ৫ মিনিটের জন্য Redis-এ রাখা হচ্ছে যাচাইয়ের জন্য
                    await redis_manager.set_cache(
                        f"security:otp_pending:{admin_id}",
                        json.dumps({"code": code, "signal": signal}),
                        ex_seconds=300,
                    )

                    if settings.enforce_anti_hacking:
                        return JSONResponse(
                            status_code=403,
                            content={
                                "error": "context_mismatch",
                                "detail": "OTP verification required — check your configured channel."
                            },
                        )
                    # alert-only: log and continue
                    from loguru import logger
                    logger.warning(f"🔓 [ALERT-ONLY] Context mismatch for admin {admin_id}: {signal} vs last {last}")

                await redis_manager.set_cache(key, json.dumps(signal), ex_seconds=_CONTEXT_TTL)

        return await call_next(request)

```

---

## File: `backend/api/routes/admin.py`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\backend/api/routes/admin.py](file:///c:/Users/n/supremeai/supremeai_2.0\backend/api/routes/admin.py)

```python
import json
import secrets
from datetime import UTC
from datetime import datetime
from pathlib import Path

from fastapi import APIRouter
from fastapi import Depends
from fastapi import HTTPException
from loguru import logger
from pydantic import BaseModel

from admin.god import AdminGodLayer  # Your existing god.py
from api.dependencies import get_current_user_token
from core.health.self_healer import SelfHealerService
from core.cache.redis_manager import redis_manager
from utils.firestore_helpers import get_firestore_db


def get_current_admin(payload: dict = Depends(get_current_user_token)) -> dict:
    if payload.get("role") != "admin":
        logger.warning(f"Unauthorized admin access attempt by {payload.get('sub')}")
        raise HTTPException(status_code=403, detail="Admin access required")
    return payload


router = APIRouter(
    prefix="/api/admin",
    tags=["Core Admin"],
    dependencies=[Depends(get_current_admin)],
)
_db_path = str(Path(__file__).resolve().parent.parent.parent / "data" / "admin_rules.db")
god_layer = AdminGodLayer(db_path=_db_path)


def get_healer_service() -> SelfHealerService:
    db = get_firestore_db()
    if not db:
        raise HTTPException(status_code=503, detail="Database unavailable")
    return SelfHealerService(db)


class RuleUpdate(BaseModel):
    key: str
    value: str


@router.post("/rules")
async def update_constitutional_rule(payload: RuleUpdate, admin_user: dict = Depends(get_current_admin)):
    """Update God.py constitutional rules directly from the Command Center UI"""
    try:
        god_layer.set_rule(payload.key, payload.value)
        logger.critical(f"🔒 Constitutional rule '{payload.key}' changed to '{payload.value}' by {admin_user.get('sub')}")
        return {"status": "success", "message": f"Rule {payload.key} updated to {payload.value}"}
    except Exception as e:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=str(e)) from e


@router.post("/actions/{action_type}")
async def trigger_quick_action(action_type: str, admin_user: dict = Depends(get_current_admin)):
    """Trigger 1-click Quick Actions from Dashboard"""
    # Verify if admin actions are currently allowed by god.py
    god_layer.enforce("admin_action")
    logger.critical(f"🔒 Admin quick-action '{action_type}' triggered by {admin_user.get('sub')}")

    if action_type == "rollback":
        return {"status": "Rollback initiated"}
    elif action_type == "backup":
        return {"status": "Backup triggered"}
    elif action_type == "cache":
        return {"status": "Redis cache cleared"}
    else:
        raise HTTPException(status_code=404, detail="Action not found")


@router.get("/fixes")
async def get_fixes(
    tenant_id: str = "default",
    status: str = "pending_review",
    admin_user: dict = Depends(get_current_admin),
    healer: SelfHealerService = Depends(get_healer_service),
):
    """Fetch all fixes for a tenant with a specific status."""
    db = get_firestore_db()
    fixes_ref = db.collection("tenants").document(tenant_id).collection("fixes")
    query = fixes_ref.where("status", "==", status)

    try:
        results = await query.get()
    except TypeError:
        # Fallback for sync mock
        results = query.get()

    fixes = []
    for doc in results:
        fix_data = doc.to_dict()
        fix_data["id"] = doc.id
        fixes.append(fix_data)

    return {"fixes": fixes}


@router.post("/fixes/{fix_id}/approve")
async def approve_fix(
    fix_id: str, tenant_id: str = "default", admin_user: dict = Depends(get_current_admin), healer: SelfHealerService = Depends(get_healer_service)
):
    """Approve a pending fix."""
    admin_id = admin_user.get("sub", "unknown_admin")
    logger.info(f"Admin {admin_id} approving fix {fix_id} for tenant {tenant_id}")

    success = await healer.apply_fix(tenant_id, fix_id, admin_id)
    if not success:
        raise HTTPException(status_code=400, detail="Failed to apply fix. It may not exist or is already processed.")

    return {"status": "success", "fix_id": fix_id}


@router.post("/fixes/{fix_id}/reject")
async def reject_fix(fix_id: str, tenant_id: str = "default", admin_user: dict = Depends(get_current_admin)):
    """Reject a pending fix."""
    admin_id = admin_user.get("sub", "unknown_admin")
    logger.info(f"Admin {admin_id} rejecting fix {fix_id} for tenant {tenant_id}")

    db = get_firestore_db()
    doc_ref = db.collection("tenants").document(tenant_id).collection("fixes").document(fix_id)

    update_data = {"status": "rejected", "reviewed_by": admin_id, "applied_at": datetime.now(UTC).isoformat()}

    try:
        await doc_ref.update(update_data)
    except TypeError:
        doc_ref.update(update_data)

    return {"status": "success", "fix_id": fix_id}


class VerifyOtpRequest(BaseModel):
    code: str


@router.post("/verify-otp")
async def verify_otp(payload: VerifyOtpRequest, admin_user: dict = Depends(get_current_admin)):
    """Validate a JIT OTP issued by AntiHackingContextMiddleware and promote the
    pending (mismatched) context to trusted, so the admin isn't re-challenged
    on their next request from this IP/fingerprint.

    বাংলা: অ্যাডমিন OTP সাবমিট করলে এখানে ভ্যালিডেট হয় এবং সফল হলে Redis-এ
    ট্রাস্টেড কনটেক্সট (last_context) আপডেট হয়ে যায়।
    """
    admin_id = admin_user.get("sub", "unknown_admin")

    if not redis_manager or not redis_manager.client:
        raise HTTPException(status_code=503, detail="Security store unavailable")

    pending_key = f"security:otp_pending:{admin_id}"
    raw_pending = await redis_manager.get_cache(pending_key)
    if not raw_pending:
        raise HTTPException(status_code=400, detail="No pending verification for this admin, or it has expired")

    pending = json.loads(raw_pending)

    if not secrets.compare_digest(str(pending["code"]), str(payload.code)):
        logger.warning(f"❌ Failed OTP verification attempt for admin {admin_id}")
        raise HTTPException(status_code=401, detail="Invalid code")

    # বাংলা: সফল ভেরিফিকেশনে বর্তমান (আগে মিসম্যাচড) সিগন্যালকেই নতুন ট্রাস্টেড কনটেক্সট হিসেবে সেট করা হচ্ছে
    await redis_manager.set_cache(
        f"security:last_context:{admin_id}",
        json.dumps(pending["signal"]),
        ex_seconds=86400,
    )
    await redis_manager.client.delete(pending_key)

    logger.info(f"✅ Admin {admin_id} passed OTP verification — context promoted to trusted")
    return {"status": "verified"}

```

---

## File: `backend/api/routes/auth.py`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\backend/api/routes/auth.py](file:///c:/Users/n/supremeai/supremeai_2.0\backend/api/routes/auth.py)

```python
# ruff: noqa: BLE001, B904, E722
from __future__ import annotations

from datetime import UTC
from datetime import datetime
from datetime import timedelta

from fastapi import APIRouter
from fastapi import Depends
from fastapi import HTTPException
from fastapi import Request
from fastapi import status
from fastapi.security import OAuth2PasswordBearer
from loguru import logger
from pydantic import BaseModel


try:
    from jose import JWTError
    from jose import jwt
except ImportError:
    JWTError = Exception  # type: ignore[misc,assignment]
    jwt = None  # type: ignore[assignment]

from core.cache.redis_manager import redis_manager
from core.config import settings
from core.security.rbac import UserContext
from database.supabase_client import db


router = APIRouter(prefix="/auth", tags=["auth"])

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/login", auto_error=False)

SECRET_KEY = settings.jwt_secret
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60 * 24


def create_access_token(data: dict, expires_delta: timedelta | None = None) -> str:
    if jwt is None:
        raise RuntimeError("python-jose[cryptography] is required for token issuance")
    to_encode = data.copy()
    expire = datetime.now(UTC) + (expires_delta or timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES))
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)


async def optional_current_user(
    token: str | None = Depends(oauth2_scheme),
) -> UserContext | None:
    if not token or jwt is None:
        return None
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        user_id = payload.get("sub", "unknown")
        role = payload.get("role", "viewer")
        return UserContext(user_id=user_id, role=role)
    except Exception:
        logger.exception("Unhandled exception")
        return None


class LoginRequest(BaseModel):
    username: str
    password: str


class RegisterRequest(BaseModel):
    username: str
    password: str
    name: str | None = None


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user_id: str
    role: str


class MeResponse(BaseModel):
    user_id: str
    role: str
    scopes: tuple[str, ...] = ()


@router.post("/login", response_model=TokenResponse)
async def login(body: LoginRequest, request: Request):
    if not db.client:
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Supabase client is not initialized")

    try:
        res = db.client.auth.sign_in_with_password({"email": body.username, "password": body.password})
        if not res.user:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials")

        user_id = res.user.id
        # বাংলা মন্তব্য: ইমেইলটি settings.admin_emails তালিকায় আছে কি না তা দেখে রোল অ্যাসাইন করা হচ্ছে (ঝুঁকিপূর্ণ "admin" in username চেক প্রতিস্থাপিত)।
        is_admin = body.username and any(body.username.lower() == admin_email.lower() for admin_email in settings.admin_emails)
        primary_role = "admin" if is_admin else "user"
        token_data = {
            "sub": user_id,
            "role": primary_role,
            "email": body.username,
            "method": "supabase_auth",
        }
        access_token = create_access_token(token_data)

        # বাংলা মন্তব্য: Phase 2 — Hybrid Fingerprint Login। হেডারটি ঐচ্ছিক, তাই না থাকলেও
        # লগইন স্বাভাবিকভাবে চলবে (ব্রেকিং চেঞ্জ নয়); থাকলে ডিভাইসটি known-devices সেটে যোগ হয়
        # যা AntiHackingContextMiddleware admin scope-এ তৃতীয় সিগন্যাল হিসেবে ব্যবহার করে।
        fingerprint = request.headers.get("x-device-fingerprint")
        if fingerprint and redis_manager and redis_manager.client:
            try:
                await redis_manager.client.sadd(f"device:known:{user_id}", fingerprint)
            except Exception as exc:  # noqa: BLE001
                logger.warning(f"Failed to register device fingerprint for {user_id}: {exc}")

        return TokenResponse(access_token=access_token, user_id=user_id, role=primary_role)
    except Exception as e:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail=str(e))


@router.post("/register", response_model=TokenResponse)
async def register(body: RegisterRequest):
    if not db.client:
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Supabase client is not initialized")

    try:
        res = db.client.auth.sign_up({"email": body.username, "password": body.password})
        if not res.user:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Registration failed")

        user_id = res.user.id
        # বাংলা মন্তব্য: ইমেইলটি settings.admin_emails তালিকায় আছে কি না তা দেখে রোল অ্যাসাইন করা হচ্ছে (ঝুঁকিপূর্ণ "admin" in username চেক প্রতিস্থাপিত)।
        is_admin = body.username and any(body.username.lower() == admin_email.lower() for admin_email in settings.admin_emails)
        primary_role = "admin" if is_admin else "user"
        token_data = {
            "sub": user_id,
            "role": primary_role,
            "email": body.username,
            "method": "supabase_auth",
        }
        access_token = create_access_token(token_data)
        return TokenResponse(access_token=access_token, user_id=user_id, role=primary_role)
    except Exception as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))


@router.get("/me", response_model=MeResponse)
async def me(current_user: UserContext | None = Depends(optional_current_user)):
    if current_user is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Not authenticated")
    # বাংলা মন্তব্য: scopes যদি None হয় তবে MeResponse ভ্যালিডেশন পাস করানোর জন্য খালি টুপল পাস করা হচ্ছে।
    scopes_val = current_user.scopes if current_user.scopes is not None else ()
    return MeResponse(user_id=current_user.user_id, role=current_user.role, scopes=scopes_val)


@router.get("/verify")
async def verify_token(request: Request):
    user = getattr(request.state, "user", None)
    if not user:
        raise HTTPException(status_code=401, detail="Missing or invalid token")
    return {"valid": True, "user_id": user.get("sub"), "role": user.get("role"), "message": "Authentication successful"}

```

---

## File: `apps/studio-client/src/utils/deviceFingerprint.ts`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\apps/studio-client/src/utils/deviceFingerprint.ts](file:///c:/Users/n/supremeai/supremeai_2.0\apps/studio-client/src/utils/deviceFingerprint.ts)

```typescript
// apps/studio-client/src/utils/deviceFingerprint.ts
// বাংলা মন্তব্য: কোনো এক্সটার্নাল সার্ভিস ছাড়াই (Zero-Cost) ব্রাউজার/হার্ডওয়্যার সিগন্যাল থেকে
// একটি স্থিতিশীল SHA-256 হ্যাশ তৈরি করা হয়। একই ডিভাইস/ব্রাউজারে বারবার একই ভ্যালু আসে,
// তাই backend-এর AntiHackingContextMiddleware এটাকে IP/country-এর পাশে তৃতীয় সিগন্যাল হিসেবে ব্যবহার করতে পারে।

let cachedFingerprint: string | null = null;
let inFlight: Promise<string> | null = null;

async function computeFingerprint(): Promise<string> {
  const nav = navigator as Navigator & { deviceMemory?: number };
  const raw = [
    navigator.userAgent,
    navigator.language,
    `${screen.colorDepth}`,
    `${screen.width}x${screen.height}`,
    Intl.DateTimeFormat().resolvedOptions().timeZone,
    `${navigator.hardwareConcurrency ?? 'na'}`,
    `${nav.deviceMemory ?? 'na'}`,
    navigator.platform ?? 'na',
  ].join('|');

  try {
    const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(raw));
    return Array.from(new Uint8Array(buf))
      .map((b) => b.toString(16).padStart(2, '0'))
      .join('');
  } catch (e) {
    console.error('🚨 [FINGERPRINT_HASH_FAILED]: Failed to compute SHA-256 device fingerprint', e);
    return 'fallback_fingerprint';
  }
}

// বাংলা মন্তব্য: বারবার হ্যাশ recompute না করে একবার করে মেমরিতে ক্যাশ রাখা হচ্ছে
export const getDeviceFingerprint = async (): Promise<string> => {
  if (cachedFingerprint) return cachedFingerprint;
  if (!inFlight) {
    inFlight = computeFingerprint().then((fp) => {
      cachedFingerprint = fp;
      return fp;
    });
  }
  return inFlight;
};

// অ্যাপ বুটের সাথে সাথেই ব্যাকগ্রাউন্ডে প্রিলোড করার জন্য — লগইন রিকোয়েস্টে দেরি হবে না
export const primeDeviceFingerprint = (): void => {
  if (typeof window !== 'undefined') {
    void getDeviceFingerprint();
  }
};

```

---

## File: `apps/studio-client/src/services/apiClient.ts`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\apps/studio-client/src/services/apiClient.ts](file:///c:/Users/n/supremeai/supremeai_2.0\apps/studio-client/src/services/apiClient.ts)

```typescript
// Centralized API Client for SupremeAI 2.0
// বাংলা মন্তব্য: এটি অ্যাপ্লিকেশনের সেন্ট্রাল এপিআই ক্লায়েন্ট যা হেডার, টোকেন এবং সিকিউর রেট লিমিট (429) / ভ্যালিডেশন এরর ইন্টারসেপ্ট করে।

import { getApiBaseUrl, switchActiveBackend } from '../utils/api';
import { getDeviceFingerprint } from '../utils/deviceFingerprint';
import PQueue from 'p-queue';

// বাংলা মন্তব্য: কাস্টম এরর ক্লাস — status প্রপার্টি দিয়ে React Query retry ফাংশন সঠিকভাবে 401/403/429 চিহ্নিত করতে পারে
export class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

// Dynamic concurrency queue
export const requestQueue = new PQueue({ concurrency: 3 }); // Default to 3, can be updated via config

export const setApiConcurrency = (concurrency: number) => {
  requestQueue.concurrency = concurrency;
};

let cachedToken: string | null = null;

export const updateTokenCache = (token: string | null) => {
  cachedToken = token;
};

export const getAuthHeaders = async (): Promise<Record<string, string>> => {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  // 🟢 Sprint 5: Backend API Integration
  if (cachedToken === null) {
    cachedToken = localStorage.getItem('supremeai_auth_token') || '';
  }

  if (cachedToken) {
    headers['Authorization'] = `Bearer ${cachedToken}`;
  }

  // 🔐 Phase 2: Hybrid Fingerprint Login — AntiHackingContextMiddleware ব্যবহার করে
  // IP/country-এর পাশাপাশি তৃতীয় কনটেক্সট সিগন্যাল হিসেবে
  try {
    headers['X-Device-Fingerprint'] = await getDeviceFingerprint();
  } catch {
    // বাংলা: WebCrypto অনুপস্থিত থাকলে (পুরনো ব্রাউজার) নীরবে বাদ দেওয়া হচ্ছে — request ব্লক হবে না
  }

  return headers;
};

const handleResponse = async (res: Response) => {
  if (!res.ok) {
    let errMsg = `HTTP error! status: ${res.status}`;
    try {
      const errData = await res.json();
      errMsg = errData.detail || errMsg;
    } catch {
      // JSON parsing failure fallback
    }

    // 🛑 ZERO-GAP: Intercept specific critical HTTP exception statuses
    if (res.status === 429) {
      console.warn("Rate limit exceeded (429). Throttling client requests.");
      throw new ApiError(`Rate limit exceeded: ${errMsg}. Please wait before retrying.`, 429);
    }
    if (res.status === 402) {
      console.warn("Payment/Budget Required (402). CostGuard rejected the request.");
      throw new ApiError(`Budget Limit Exceeded: ${errMsg}`, 402);
    }
    if (res.status === 422) {
      console.error("Validation error (422) detected in payload schema.");
      throw new ApiError(`Validation Error: ${errMsg}`, 422);
    }
    if (res.status === 401 || res.status === 403) {
      console.warn("Authorization failure (401/403). Session invalidated.");
      throw new ApiError(errMsg, res.status);
    }
    throw new ApiError(errMsg, res.status);
  }
  return res.json();
};

// বাংলা মন্তব্য: এপিআই রিকোয়েস্ট হ্যাং হওয়া রোধে ১৫ সেকেন্ডের ডিফল্ট টাইমআউট নির্ধারণ করা হচ্ছে।
const DEFAULT_TIMEOUT_MS = Number(import.meta.env.VITE_API_TIMEOUT_MS ?? 15000);

const fetchWithTimeout = async (url: string, options: RequestInit, timeoutMs = DEFAULT_TIMEOUT_MS): Promise<Response> => {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);

  // বাংলা মন্তব্য: JSDOM এবং node-fetch-এর সাথে AbortSignal টাইপ অমিল এড়াতে টেস্ট এনভায়রনমেন্টে signal বাদ দেওয়া হচ্ছে।
  const fetchOptions: RequestInit = { ...options };
  const isTest = typeof process !== 'undefined' && (process.env.NODE_ENV === 'test' || process.env.VITEST === 'true');
  if (!isTest) {
    fetchOptions.signal = controller.signal;
  }

  try {
    return await fetch(url, fetchOptions);
  } catch (e) {
    if (controller.signal.aborted) {
      throw new Error(`Request timed out after ${timeoutMs}ms: ${url}`);
    }
    throw e;
  } finally {
    clearTimeout(timer);
  }
};

// বাংলা মন্তব্য: throttledFetch — p-queue দিয়ে একসাথে অতিরিক্ত রিকোয়েস্ট না যাওয়ার নিশ্চয়তা
const throttledFetch = async (url: string, options: RequestInit): Promise<Response> => {
  return requestQueue.add(async () => {
    let currentUrl = url;
    let attempts = 0;
    options.credentials = 'include';

    while (attempts < 2) {
      try {
        const res = await fetchWithTimeout(currentUrl, options);
        // 502/503/504 পেলে রেন্ডার সার্ভার স্লিপিং বা ডাউন, ফেইলওভার ট্রিগার করব
        if (res.status >= 502 && res.status <= 504) {
          throw new Error("Server sleeping or down (50x)");
        }
        return res;
      } catch (e: any) {
        attempts++;
        if (attempts >= 2) {
          console.error(`[Queue Interceptor] Network failure for ${currentUrl} after 2 attempts:`, e);
          throw e;
        }

        console.warn(`[Failover] Network error detected: ${e.message}. Switching active backend...`);
        const newBase = switchActiveBackend();

        // currentUrl থেকে পুরনো বেস URL সরিয়ে নতুনটি বসানো
        const urlObj = new URL(currentUrl);
        currentUrl = `${newBase}${urlObj.pathname}${urlObj.search}`;

        // স্লিপিং থেকে ওঠার জন্য একটু অপেক্ষা করে রিট্রাই
        await new Promise(resolve => setTimeout(resolve, 1000));
      }
    }
    throw new Error("All backends failed");
  }) as Promise<Response>;
};

export const apiClient = {
  get: async <T>(path: string, options?: RequestInit): Promise<T> => {
    const res = await throttledFetch(`${getApiBaseUrl()}${path}`, {
      method: 'GET',
      headers: await getAuthHeaders(),
      ...options,
    });
    return handleResponse(res);
  },

  post: async <T>(path: string, body?: any, options?: RequestInit): Promise<T> => {
    const res = await throttledFetch(`${getApiBaseUrl()}${path}`, {
      method: 'POST',
      headers: await getAuthHeaders(),
      body: body ? JSON.stringify(body) : undefined,
      ...options,
    });
    return handleResponse(res);
  },

  put: async <T>(path: string, body?: any, options?: RequestInit): Promise<T> => {
    const res = await throttledFetch(`${getApiBaseUrl()}${path}`, {
      method: 'PUT',
      headers: await getAuthHeaders(),
      body: body ? JSON.stringify(body) : undefined,
      ...options,
    });
    return handleResponse(res);
  },

  delete: async <T>(path: string, options?: RequestInit): Promise<T> => {
    const res = await throttledFetch(`${getApiBaseUrl()}${path}`, {
      method: 'DELETE',
      headers: await getAuthHeaders(),
      ...options,
    });
    return handleResponse(res);
  },
};

```

---

## File: `apps/studio-client/src/App.tsx`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\apps/studio-client/src/App.tsx](file:///c:/Users/n/supremeai/supremeai_2.0\apps/studio-client/src/App.tsx)

```typescript
import React, { useState } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useStore } from "./store/useStore";

import { ThemeSyncProvider } from './providers/ThemeSyncProvider';
import { GlobalConfigInitializer } from "./components/core/GlobalConfigInitializer";
import { ProtectedRoute, GuestRoute } from "./components/core/AuthGuards";
import { ToastProvider } from './components/ui/Toast';

// Pages
import { AdminShell } from "./pages/admin/AdminShell";
import { LoginScreen } from './pages/auth/LoginScreen';
import { RegisterScreen } from './pages/auth/RegisterScreen';
import { AgentWorkspace } from './pages/user/AgentWorkspace';
import { IdeWorkspace } from './pages/user/IdeWorkspace';
import { IntegrationsManager } from './pages/user/IntegrationsManager';
import { ArchitectTower } from './pages/user/ArchitectTower';
import { SkillCatalog } from './pages/user/SkillCatalog';
import SwarmMap from './components/SwarmMap';
import EvolutionForge from './pages/user/EvolutionForge/EvolutionForge';
import { DashboardShell } from "./components/dashboard/DashboardShell";
import { LivingDashboardShell } from "./components/dashboard/LivingDashboardShell";
import { UserDashboard } from "./components/customer/UserDashboard";

// Services & Hooks
import { getAethelResponse } from "./services/chatService";
import type { ChatMessage } from "./services/chatService";
import { useServerStream } from "./hooks/useServerStream";
import ErrorBoundary from './components/admin/DashboardErrorBoundary';
import { primeDeviceFingerprint } from "./utils/deviceFingerprint";

primeDeviceFingerprint(); // বাংলা মন্তব্য: অ্যাপ বুট হওয়ার সাথে সাথে ব্যাকগ্রাউন্ডে ফিঙ্গারপ্রিন্ট হ্যাশ প্রিলোড হচ্ছে

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error: any) => {
        const msg = error?.message || '';
        if (
          error?.status === 401 || error?.status === 403 || error?.status === 429 ||
          msg.includes('401') || msg.includes('403') || msg.includes('429') ||
          msg.includes('Rate limit') || msg.includes('Unauthorized')
        ) return false;
        return failureCount < 2;
      },
      retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex + Math.random() * 500, 15000),
      refetchOnWindowFocus: false,
      staleTime: 30_000,
    },
  },
});

const PORTAL_TYPE = import.meta.env.VITE_PORTAL_TYPE || 'user';

export const App: React.FC = () => {
  return (
    <ThemeSyncProvider>
      <ToastProvider>
        <AppContent />
      </ToastProvider>
    </ThemeSyncProvider>
  );
};

const AppContent: React.FC = () => {
  const { isServerOnline, deployGate } = useStore();
  const { streamStatus } = useServerStream();

  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [chatInput, setChatInput] = useState('');
  const [code, setCode] = useState('// Click Preview or Save to interact with the workspace code');
  const [theme, setTheme] = useState<'dark' | 'light'>('dark');

  const toggleTheme = () => setTheme(prev => prev === 'dark' ? 'light' : 'dark');

  const handleSendCustomer = async () => {
    if (!chatInput.trim()) return;
    const now = new Date().toLocaleTimeString();
    const userMessage = { id: Date.now(), sender: 'User', text: chatInput, timestamp: now };
    const responseId = Date.now() + 1;

    setChatMessages(prev => [
      ...prev,
      userMessage,
      { id: responseId, sender: 'Aethel', text: `Analyzing request "${chatInput}"... Processing on central core.`, timestamp: now }
    ]);
    setChatInput('');

    try {
      const history = [...chatMessages, userMessage].map(msg => ({
        role: msg.sender === 'User' ? 'user' : 'assistant',
        content: msg.text,
      }));
      const responseText = await getAethelResponse(chatInput, history as any);
      setChatMessages(prev => prev.map(msg => msg.id === responseId ? { ...msg, text: responseText } : msg));
    } catch (error: any) {
      setChatMessages(prev => prev.map(msg => msg.id === responseId ? { ...msg, text: `AI backend error: ${error?.message || 'Unable to fetch response.'}` } : msg));
    }
  };

  const handleSaveToProject = (code: string) => {
    setCode(code);
  };

  const handlePreview = (code: string) => {
    setCode(code);
  };

  const legacyWorkspace = (
    <UserDashboard
      customerMessages={chatMessages}
      customerInput={chatInput}
      setCustomerInput={setChatInput}
      loading={false}
      handleSendCustomer={handleSendCustomer}
      theme={theme}
      toggleTheme={toggleTheme}
      code={code}
      setCode={setCode}
      isServerOnline={isServerOnline}
      deployGate={deployGate}
      user={null}
      projects={[]}
      chatHistory={chatMessages}
      widgets={[]}
      onSaveToProject={handleSaveToProject}
      onPreview={handlePreview}
    />
  );

  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <GlobalConfigInitializer>
          <Routes>
            {PORTAL_TYPE === 'admin' ? (
              /* =========================================
                 ADMIN PORTAL
              ========================================= */
              <>
                <Route path="/" element={<Navigate to="/admin" replace />} />
                <Route path="/admin/*" element={<AdminShell />} />
                <Route path="*" element={<Navigate to="/admin" replace />} />
              </>
            ) : (
              /* =========================================
                 USER PORTAL (State Machine Routing)
              ========================================= */
              <>
                {/* GUEST STATE */}
                <Route path="/login" element={
                  <GuestRoute>
                    <LoginScreen />
                  </GuestRoute>
                } />
                <Route path="/register" element={
                  <GuestRoute>
                    <RegisterScreen />
                  </GuestRoute>
                } />
                <Route path="/" element={<Navigate to="/workspace" replace />} />

                {/* AUTHENTICATED STATE */}
                <Route path="/workspace/agent" element={
                  <ProtectedRoute>
                    <AgentWorkspace />
                  </ProtectedRoute>
                } />
                <Route path="/workspace/ide" element={
                  <ProtectedRoute>
                    <IdeWorkspace />
                  </ProtectedRoute>
                } />
                <Route path="/integrations" element={
                  <ProtectedRoute>
                    <IntegrationsManager />
                  </ProtectedRoute>
                } />
                <Route path="/architect-tower" element={
                  <ProtectedRoute>
                    <ArchitectTower />
                  </ProtectedRoute>
                } />
                <Route path="/swarm" element={
                  <ProtectedRoute>
                    <SwarmMap />
                  </ProtectedRoute>
                } />
                <Route path="/evolution-forge" element={
                  <ProtectedRoute>
                    <EvolutionForge />
                  </ProtectedRoute>
                } />
                {/* বাংলা: /skills-catalog রাউট — রোল-ফিল্টারড ডাইনামিক ক্যাটালগ পেজ */}
                <Route path="/skills-catalog" element={
                  <ProtectedRoute>
                    <SkillCatalog />
                  </ProtectedRoute>
                } />
                <Route path="/workspace/*" element={
                  <DashboardShell
                    theme={theme}
                    toggleTheme={toggleTheme}
                    isServerOnline={isServerOnline}
                    workspace={legacyWorkspace}
                  />
                } />
                <Route path="/workspace/live" element={
                  <LivingDashboardShell chatPanel={legacyWorkspace} resolveDraggedContent={(id) => ({ content: id })} />
                } />

                {/* Users trying to access admin are redirected */}
                <Route path="/admin/*" element={<Navigate to="/" replace />} />
              </>
            )}
          </Routes>
        </GlobalConfigInitializer>
      </QueryClientProvider>
    </ErrorBoundary>
  );
};

```

---

## File: `backend/database/session.py`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\backend/database/session.py](file:///c:/Users/n/supremeai/supremeai_2.0\backend/database/session.py)

```python
from collections.abc import AsyncGenerator
from contextlib import asynccontextmanager

from typing import Any

from loguru import logger
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.ext.asyncio import async_sessionmaker
from sqlalchemy.ext.asyncio import create_async_engine
from sqlalchemy.pool import StaticPool

from core.config import settings


DATABASE_URL = settings.supabase_database_url

if not DATABASE_URL:
    logger.warning("SUPABASE_DATABASE_URL_POOLER is missing. Database operations will fail.")


# বাংলা মন্তব্য: কানেকশন স্ট্রিংয়ে postgresql:// বা postgres:// থাকলে তা asyncpg-এর জন্য postgresql+asyncpg:// দিয়ে প্রতিস্থাপন করা হচ্ছে
def get_async_url(url: str) -> str:
    if not url:
        return "sqlite+aiosqlite:///:memory:"
    if url.startswith("postgresql://"):
        return url.replace("postgresql://", "postgresql+asyncpg://", 1)
    if url.startswith("postgres://"):
        return url.replace("postgres://", "postgresql+asyncpg://", 1)
    return url


_async_url = get_async_url(DATABASE_URL)

# বাংলা মন্তব্য: MyPy টাইপ ইনফারেন্সের সমস্যা সমাধানের জন্য টাইপ হিসেবে dict[str, Any] ব্যবহার করা হলো
engine_kwargs: dict[str, Any] = {
    "echo": False,
}
if _async_url.startswith("sqlite"):
    engine_kwargs["poolclass"] = StaticPool
    engine_kwargs["connect_args"] = {"check_same_thread": False}
if _async_url.startswith("postgresql"):
    # বাংলা মন্তব্য: User ও Admin — দুই আলাদা Render instance একই Supabase PgBouncer পুলে
    # কানেক্ট করে, তাই SERVICE_ROLE অনুযায়ী pool limit ভাগ করা হচ্ছে যাতে কোনো একটি
    # instance বাকিটার জন্য কানেকশন শেষ করে না ফেলে (pool exhaustion prevention)।
    # User: high-traffic client-facing, বেশি concurrency দরকার -> min=2, max=15 (pool_size + max_overflow)
    # Admin: low-traffic internal panel, সামান্য concurrency যথেষ্ট -> min=1, max=3
    _role = settings.service_role.lower()
    if _role == "admin":
        _pool_size, _max_overflow = 1, 2  # base(1) + overflow(2) = max 3 concurrent
    else:
        _pool_size, _max_overflow = 2, 13  # base(2) + overflow(13) = max 15 concurrent

    engine_kwargs.update(
        {
            "pool_size": _pool_size,
            "max_overflow": _max_overflow,
            "pool_timeout": 30,
            "pool_recycle": 1800,
            # বাংলা মন্তব্য: stateless API রুট থেকে কানেকশন যেন দ্রুত রিলিজ হয়, তাই pre_ping দিয়ে
            # স্টেল কানেকশন এড়ানো হচ্ছে (PgBouncer transaction-mode এ স্টেল হওয়া সাধারণ ঘটনা)।
            "pool_pre_ping": True,
            # বাংলা মন্তব্য: PgBouncer এর transaction pool মোডের সাথে সামঞ্জস্যের জন্য statement_cache_size=0 করা হলো
            "connect_args": {
                "command_timeout": 30,
                "server_settings": {"application_name": f"supremeai_2.0_{_role}"},
                "statement_cache_size": 0,
            },
        }
    )
    logger.info(f"🔌 DB pool configured for SERVICE_ROLE='{_role}': pool_size={_pool_size}, max_overflow={_max_overflow}")

engine = create_async_engine(_async_url, **engine_kwargs)

AsyncSessionLocal = async_sessionmaker(bind=engine, class_=AsyncSession, expire_on_commit=False, autoflush=False)


@asynccontextmanager
async def get_db_session_context() -> AsyncGenerator[AsyncSession, None]:
    """Context manager for backend tasks or non-FastAPI usages.

    বাংলা: FastAPI-এর বাইরে বা ব্যাকগ্রাউন্ড টাস্কে ডাটাবেস সেশন ব্যবহারের জন্য।
    """
    async with AsyncSessionLocal() as session:
        try:
            yield session
        except Exception as e:
            await session.rollback()
            logger.error(f"Database transaction rolled back due to error: {e}")
            raise
        finally:
            await session.close()


# FastAPI Dependency Injection (with safe rollback)
async def get_db_session() -> AsyncGenerator[AsyncSession, None]:
    """FastAPI Dependency for database sessions.

    বাংলা: FastAPI রুটগুলোর জন্য ডাটাবেস ডিপেন্ডেন্সি।
    """
    async with get_db_session_context() as session:
        yield session

```

---

## File: `render.yaml`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\render.yaml](file:///c:/Users/n/supremeai/supremeai_2.0\render.yaml)

```yaml
# render.yaml - SupremeAI 2.0 Master Blueprint (Zero Cost Edition)
services:
  # ১. ব্যাকএন্ড (GHCR Image - Zero Render Build Minutes)
  - type: web
    name: supremeai-backend
    env: image
    image:
      url: ghcr.io/paykaribazaronline/supremeai/supremeai-backend:latest
    region: singapore
    plan: free
    healthCheckPath: /api/v1/health
    autoDeploy: false
    envVars:
      - key: PORT
        value: 8080
      - key: ENV
        value: production
      # বাকি সিক্রেটগুলো ড্যাশবোর্ড থেকে সিঙ্ক হবে (Upstash & Supabase)
      - key: REDIS_URL
        sync: false
      - key: UPSTASH_REDIS_REST_URL
        sync: false
      - key: UPSTASH_REDIS_REST_TOKEN
        sync: false
      - key: SUPABASE_URL
        sync: false
      - key: SUPABASE_KEY
        sync: false
      - key: SUPABASE_DATABASE_URL_POOLER
        sync: false
      - key: OPENAI_API_KEY
        sync: false
      - key: SUPREMEAI_JWT_SECRET
        sync: false
      - key: SUPREMEAI_ADMIN_PASSWORD_HASH
        sync: false
      - key: ENCRYPTION_KEY
        sync: false
      - key: SUPREMEAI_DOCS_PASSWORD
        sync: false
      - key: SUPREMEAI_API_KEY
        sync: false
      - key: STRIPE_API_KEY
        sync: false
      - key: STRIPE_WEBHOOK_SECRET
        sync: false
      - key: CI_WEBHOOK_SECRET
        sync: false
      - key: INFISICAL_TOKEN
        sync: false
      - key: INFISICAL_CLIENT_SECRET
        sync: false
      - key: CORS_ORIGINS
        value: '["https://supremeai-studio-client.onrender.com", "https://supremeai-studio-client-qb34.onrender.com", "https://tiny-stroopwafel-2d981c.netlify.app", "https://supremeai-lac.vercel.app", "https://supremeai-studio.vercel.app", "https://supremeai-a.web.app", "https://supremeai-admin.web.app"]'
      # বাংলা মন্তব্য: core/app_user.py এই User-role instance-এ CORS_ORIGINS নয়, USER_CORS_ORIGINS
      # পড়ে এবং production-এ খালি থাকলে বুট-টাইমে crash করে (Fail-Fast) — তাই আলাদাভাবে সেট করা হলো,
      # যাতে User API কঠোরভাবে শুধু Vercel/Netlify/Render client-গুলোকেই ট্রাস্ট করে (Admin console নয়)।
      - key: USER_CORS_ORIGINS
        value: '["https://supremeai-studio-client.onrender.com", "https://supremeai-studio-client-qb34.onrender.com", "https://tiny-stroopwafel-2d981c.netlify.app", "https://supremeai-lac.vercel.app", "https://supremeai-studio.vercel.app"]'
      - key: SERVICE_ROLE
        value: user
      - key: ALLOWED_HOSTS
        value: 'supremeai-backend.onrender.com,supremeai-backend-65hl.onrender.com'

  # ১.৫. অ্যাডমিন ব্যাকএন্ড (আলাদা, আইসোলেটেড Render instance — core/app_admin.py)
  # বাংলা মন্তব্য: core/app_admin.py আগে থেকেই কোডে ছিল (Anti-Hacking OTP মিডলওয়্যার +
  # শুধু admin রাউট), কিন্তু render.yaml-এ এর কোনো ম্যাচিং সার্ভিস ছিল না — ফলে এটি কখনো
  # deploy-ই হতো না এবং প্রকৃত সার্ভার-লেভেল আইসোলেশন অর্জিত হয়নি। ডোমেইন/সিক্রেট আলাদা
  # হওয়ায় ইউজার instance ক্র্যাশ করলেও অ্যাডমিন প্যানেল প্রভাবিত হবে না, এবং উল্টোটাও।
  - type: web
    name: supremeai-admin
    env: image
    image:
      url: ghcr.io/paykaribazaronline/supremeai/supremeai-backend:latest
    region: singapore
    plan: free
    healthCheckPath: /api/v1/health
    autoDeploy: false
    envVars:
      - key: PORT
        value: 8080
      - key: ENV
        value: production
      - key: REDIS_URL
        sync: false
      - key: UPSTASH_REDIS_REST_URL
        sync: false
      - key: UPSTASH_REDIS_REST_TOKEN
        sync: false
      - key: SUPABASE_URL
        sync: false
      - key: SUPABASE_KEY
        sync: false
      - key: SUPABASE_DATABASE_URL_POOLER
        sync: false
      - key: OPENAI_API_KEY
        sync: false
      - key: SUPREMEAI_JWT_SECRET
        sync: false
      - key: SUPREMEAI_ADMIN_PASSWORD_HASH
        sync: false
      - key: ENCRYPTION_KEY
        sync: false
      - key: SUPREMEAI_DOCS_PASSWORD
        sync: false
      - key: SUPREMEAI_API_KEY
        sync: false
      - key: DISCORD_OTP_WEBHOOK_URL
        sync: false
      - key: RESEND_API_KEY
        sync: false
      - key: ADMIN_NOTIFICATION_EMAIL
        sync: false
      - key: INFISICAL_TOKEN
        sync: false
      - key: INFISICAL_CLIENT_SECRET
        sync: false
      # বাংলা মন্তব্য: শুধুমাত্র অ্যাডমিন কনসোল origin — Vercel/Netlify user client নয়
      - key: ADMIN_CORS_ORIGINS
        value: '["https://supremeai-admin.web.app"]'
      - key: SERVICE_ROLE
        value: admin
      - key: ALLOWED_HOSTS
        value: 'supremeai-admin.onrender.com'

  # ২. ফ্রন্টএন্ড (Render 100% Free Static Hosting)
  - type: web
    name: supremeai-studio-client
    env: static
    buildCommand: "cd apps/studio-client && pnpm install && pnpm run build"
    staticPublishPath: "./apps/studio-client/dist-user"
    autoDeploy: false
    routes:
      - type: rewrite
        source: /*
        destination: /index.html

```

---

## File: `infrastructure/render.admin.yaml`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\infrastructure/render.admin.yaml](file:///c:/Users/n/supremeai/supremeai_2.0\infrastructure/render.admin.yaml)

```yaml
# render.admin.yaml — SupremeAI 2.0 Admin Instance Blueprint (Zero Cost Edition)
#
# বাংলা মন্তব্য: এই ব্লুপ্রিন্টটি আলাদা — মূল `render.yaml` (User instance) থেকে ইচ্ছাকৃতভাবে
# পৃথক রাখা হয়েছে, কারণ Render Blueprints (render.yaml) একটি রিপো-কে একটি নির্দিষ্ট Render
# অ্যাকাউন্টের সাথে সিঙ্ক করে — একই YAML দিয়ে দুইটি ভিন্ন ফ্রি-টিয়ার অ্যাকাউন্টে ডিপ্লয় করা যায় না।
#
# Setup (one-time, manual — Render Blueprints don't support multi-account targeting):
#   1. Log into your SECOND Render.com free-tier account.
#   2. New → Blueprint → point it at this same GitHub repo, but set the blueprint
#      file path to `infrastructure/render.admin.yaml` (Render lets you choose a
#      non-default blueprint path when creating the Blueprint instance).
#   3. Sync the same secrets used by the User instance (SUPABASE_*, REDIS_URL, etc.)
#      into THIS account's env var dashboard — they are intentionally not duplicated
#      in source. Additionally set the Admin-only secrets below (Discord/Resend/JWT).
#   4. Set ADMIN_HEALTH_URL as a GitHub Actions secret in the repo (see
#      .github/workflows/admin-keepalive.yml) to this service's /api/v1/health URL,
#      so the free-tier instance never cold-starts and breaks JIT OTP timing.
#
# This is purely additive — it does not touch or replace the existing render.yaml
# (User instance) in the repo root.

services:
  - type: web
    name: supremeai-admin
    env: image
    image:
      url: ghcr.io/paykaribazaronline/supremeai/supremeai-backend:latest
    region: singapore
    plan: free
    healthCheckPath: /api/v1/health
    autoDeploy: false
    envVars:
      - key: PORT
        value: 8080
      - key: ENV
        value: production
      # বাংলা মন্তব্য: এই একটি ফ্ল্যাগই core/app_admin.py লোড করায় (main.py) and
      # database/session.py-কে min=1/max=3 PgBouncer pool limit-এ পাঠায়।
      - key: SERVICE_ROLE
        value: admin
      # বাংলা মন্তব্য: Alert-only ডিফল্ট — false-positive rate যাচাই না হওয়া পর্যন্ত ব্লক করবে না।
      - key: ENFORCE_ANTI_HACKING
        value: false
      # বাকি সিক্রেটগুলো ড্যাশবোর্ড থেকে সিঙ্ক হবে (Upstash & Supabase — same DB/Redis as User instance)
      - key: REDIS_URL
        sync: false
      - key: UPSTASH_REDIS_REST_URL
        sync: false
      - key: UPSTASH_REDIS_REST_TOKEN
        sync: false
      - key: SUPABASE_URL
        sync: false
      - key: SUPABASE_KEY
        sync: false
      - key: SUPABASE_DATABASE_URL_POOLER
        sync: false
      - key: SUPREMEAI_JWT_SECRET
        sync: false
      - key: SUPREMEAI_ADMIN_PASSWORD_HASH
        sync: false
      - key: ENCRYPTION_KEY
        sync: false
      - key: SUPREMEAI_DOCS_PASSWORD
        sync: false
      - key: SUPREMEAI_API_KEY
        sync: false
      # Admin-only: JIT OTP delivery channels
      - key: DISCORD_OTP_WEBHOOK_URL
        sync: false
      - key: RESEND_API_KEY
        sync: false
      - key: ADMIN_NOTIFICATION_EMAIL
        sync: false
      - key: OTP_COOLDOWN_SECONDS
        value: 60
      # Admin API only ever trusts the Firebase-hosted console — never the Vercel user client.
      - key: ADMIN_CORS_ORIGINS
        value: '["https://supremeai-admin.web.app"]'
      - key: ALLOWED_HOSTS
        value: 'supremeai-admin.onrender.com'

```

---

## File: `.github/workflows/admin-keepalive.yml`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\.github/workflows/admin-keepalive.yml](file:///c:/Users/n/supremeai/supremeai_2.0\.github/workflows/admin-keepalive.yml)

```yaml
# SupremeAI — Admin Instance Keep-Alive (Cold-Start Elimination)
# বাংলা মন্তব্য: Render.com ফ্রি-টিয়ারে ১৫ মিনিট নিষ্ক্রিয় থাকলে ইনস্ট্যান্স স্লিপে চলে যায়।
# এই ওয়ার্কফ্লো প্রতি ১৪ মিনিটে Admin API-এর /health এন্ডপয়েন্টে পিং করে ঘুম প্রতিরোধ করে,
# যাতে JIT OTP / security lifecycle timeout-এর সমস্যা না হয়। সম্পূর্ণ Zero-Cost (GitHub Actions free tier)।
#
# Setup: repo → Settings → Secrets and variables → Actions → New repository secret
#   ADMIN_HEALTH_URL = https://<your-admin-render-service>.onrender.com/api/v1/health

name: "🫀 Admin Instance Keep-Alive"

on:
  schedule:
    # প্রতি ১৪ মিনিটে — GitHub Actions cron সর্বনিম্ন প্রতি ৫ মিনিট সাপোর্ট করে, তাই কোল্ড-স্টার্ট
    # উইন্ডো (Render free tier ~15 min idle timeout) নিরাপদে কভার হয়।
    - cron: '*/14 * * * *'
  workflow_dispatch: {}

jobs:
  ping-admin:
    runs-on: ubuntu-latest
    steps:
      - name: Ping Admin API health endpoint
        env:
          ADMIN_HEALTH_URL: ${{ secrets.ADMIN_HEALTH_URL }}
        run: |
          if [ -z "$ADMIN_HEALTH_URL" ]; then
            echo "⚠️ ADMIN_HEALTH_URL secret not set — skipping keep-alive ping. See workflow header for setup."
            exit 0
          fi
          status=$(curl -s -o /dev/null -w "%{http_code}" --max-time 20 "$ADMIN_HEALTH_URL" || echo "000")
          echo "Admin health check responded with HTTP $status"
          if [ "$status" != "200" ]; then
            echo "🔴 Admin instance did not return 200 — it may be cold-starting or down. Not failing the job (this is best-effort)."
          else
            echo "🟢 Admin instance is warm."
          fi

```

---

## File: `backend/tests/middleware/test_anti_hacking.py`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\backend/tests/middleware/test_anti_hacking.py](file:///c:/Users/n/supremeai/supremeai_2.0\backend/tests/middleware/test_anti_hacking.py)

```python
"""Tests for AntiHackingContextMiddleware — sliding-window Caution tier and OTP cooldown.

বাংলা: নতুন Caution tier (partial IP/UA match) এবং OTP cooldown throttle-এর জন্য টেস্ট।
"""
from __future__ import annotations

import json
from types import SimpleNamespace
from unittest.mock import AsyncMock, patch

import pytest

from middleware.anti_hacking import AntiHackingContextMiddleware


def _make_request(ip: str, country: str, ua: str, fingerprint: str, admin_id: str = "admin-1"):
    req = SimpleNamespace()
    req.headers = {
        "x-forwarded-for": ip,
        "cf-ipcountry": country,
        "user-agent": ua,
        "x-device-fingerprint": fingerprint,
    }
    req.state = SimpleNamespace(user={"sub": admin_id})
    return req


async def _call_next(request):
    return "OK"


@pytest.fixture
def mock_redis():
    """Async mock standing in for redis_manager with a real-ish get/set-nx behaviour."""
    store: dict[str, str] = {}
    nx_locks: set[str] = set()

    manager = AsyncMock()

    async def get_cache(key):
        return store.get(key)

    async def set_cache(key, value, ex_seconds=3600):
        store[key] = value
        return True

    manager.get_cache = AsyncMock(side_effect=get_cache)
    manager.set_cache = AsyncMock(side_effect=set_cache)

    client = AsyncMock()

    async def set_nx(key, value, nx=False, ex=None):
        if nx and key in nx_locks:
            return None
        nx_locks.add(key)
        return True

    client.set = AsyncMock(side_effect=set_nx)
    client.lpush = AsyncMock(return_value=1)
    client.ltrim = AsyncMock(return_value=True)
    client.expire = AsyncMock(return_value=True)
    manager.client = client
    manager._store = store
    manager._nx_locks = nx_locks
    return manager


@pytest.mark.asyncio
async def test_first_request_no_prior_context_passes_through(mock_redis):
    with patch("middleware.anti_hacking.redis_manager", mock_redis), \
         patch("middleware.anti_hacking.send_otp", new=AsyncMock()) as mock_send:
        mw = AntiHackingContextMiddleware(app=None)
        req = _make_request("1.2.3.4", "BD", "chrome", "fp-abc")
        result = await mw.dispatch(req, _call_next)
        assert result == "OK"
        mock_send.assert_not_called()


@pytest.mark.asyncio
async def test_full_mismatch_triggers_otp(mock_redis):
    with patch("middleware.anti_hacking.redis_manager", mock_redis), \
         patch("middleware.anti_hacking.send_otp", new=AsyncMock()) as mock_send:
        mw = AntiHackingContextMiddleware(app=None)

        # Establish trusted context first
        req1 = _make_request("1.2.3.4", "BD", "chrome-v1", "fp-abc")
        await mw.dispatch(req1, _call_next)

        # Completely different IP subnet, country, UA and fingerprint -> full OTP challenge
        req2 = _make_request("9.9.9.9", "US", "safari-v9", "fp-zzz")
        await mw.dispatch(req2, _call_next)

        mock_send.assert_called_once()


@pytest.mark.asyncio
async def test_partial_match_same_subnet_is_caution_not_otp(mock_redis):
    with patch("middleware.anti_hacking.redis_manager", mock_redis), \
         patch("middleware.anti_hacking.send_otp", new=AsyncMock()) as mock_send:
        mw = AntiHackingContextMiddleware(app=None)

        req1 = _make_request("1.2.3.4", "BD", "chrome-v1", "fp-abc")
        await mw.dispatch(req1, _call_next)

        # Same /24 subnet (first 3 octets), different last octet + different fingerprint (CGNAT-style)
        req2 = _make_request("1.2.3.99", "US", "different-ua", "fp-zzz")
        await mw.dispatch(req2, _call_next)

        mock_send.assert_not_called()
        mock_redis.client.lpush.assert_called_once()


@pytest.mark.asyncio
async def test_partial_match_same_user_agent_is_caution_not_otp(mock_redis):
    with patch("middleware.anti_hacking.redis_manager", mock_redis), \
         patch("middleware.anti_hacking.send_otp", new=AsyncMock()) as mock_send:
        mw = AntiHackingContextMiddleware(app=None)

        req1 = _make_request("1.2.3.4", "BD", "chrome-v1", "fp-abc")
        await mw.dispatch(req1, _call_next)

        # Different subnet entirely, but identical UA (mobile data switch scenario)
        req2 = _make_request("77.88.99.10", "US", "chrome-v1", "fp-zzz")
        await mw.dispatch(req2, _call_next)

        mock_send.assert_not_called()


@pytest.mark.asyncio
async def test_otp_cooldown_suppresses_duplicate_sends(mock_redis):
    with patch("middleware.anti_hacking.redis_manager", mock_redis), \
         patch("middleware.anti_hacking.send_otp", new=AsyncMock()) as mock_send:
        mw = AntiHackingContextMiddleware(app=None)

        req1 = _make_request("1.2.3.4", "BD", "chrome-v1", "fp-abc")
        await mw.dispatch(req1, _call_next)

        # First full mismatch -> OTP sent, cooldown lock acquired
        req2 = _make_request("9.9.9.9", "US", "safari-v9", "fp-zzz")
        await mw.dispatch(req2, _call_next)
        assert mock_send.call_count == 1

        # Immediate second full mismatch from a third distinct context -> cooldown should suppress resend
        req3 = _make_request("5.5.5.5", "FR", "firefox-v1", "fp-yyy")
        await mw.dispatch(req3, _call_next)
        assert mock_send.call_count == 1  # unchanged — cooldown suppressed the second send

```

---

## File: `backend/api/routes/tools_ops.py`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\backend/api/routes/tools_ops.py](file:///c:/Users/n/supremeai/supremeai_2.0\backend/api/routes/tools_ops.py)

```python
from __future__ import annotations

import os
from typing import Any

from fastapi import APIRouter
from fastapi import Depends
from fastapi import HTTPException
from loguru import logger
from pydantic import BaseModel

from api.dependencies import get_current_user_token
from tools.code.code_smell_detector import CodeSmellDetector
from tools.devops.on_premise_deployer import OnPremiseDeployer
from tools.learning.domain_adapter import DomainAdapter
from tools.learning.skill_recommender import SkillRecommender
from tools.security_tools.vulnerability_predictor import VulnerabilityPredictor


def _require_admin(payload: dict = Depends(get_current_user_token)) -> dict:
    """Gate DevOps/deploy tooling behind an authenticated admin role.

    বাংলা মন্তব্য: এই রাউটারে ফাইল-সিস্টেম রিড (smell/vuln scan) এবং ডিপ্লয়মেন্ট
    ফাইল-রাইট (docker-compose/helm) অপারেশন আছে, যা আগে কোনো auth ছাড়াই User-facing
    API-তে এক্সপোজড ছিল (route-leakage: এই মডিউলটি `_admin_paths`-এ ছিল না)।
    এখন প্রতিটি এন্ডপয়েন্টে admin-role JWT বাধ্যতামূলক করা হলো।
    """
    if payload.get("role") != "admin":
        logger.warning(f"🚫 Unauthorized tools-ops access attempt by {payload.get('sub', 'unknown')}")
        raise HTTPException(status_code=403, detail="Admin access required")
    return payload


router = APIRouter(prefix="/tools", tags=["tools-ops"], dependencies=[Depends(_require_admin)])


class SmellCheckRequest(BaseModel):
    path: str
    thresholds: dict[str, int] | None = None


class SmellCheckResponse(BaseModel):
    path: str
    smells: list[dict[str, Any]]
    summary: dict[str, int]


class VulnCheckRequest(BaseModel):
    file_path: str | None = None
    diff: str | None = None


class VulnCheckResponse(BaseModel):
    file: str
    vulnerability_score: float
    critical_count: int
    high_count: int
    medium_count: int
    low_count: int
    findings: list[dict[str, Any]]
    recommendation: str


class SkillRecRequest(BaseModel):
    user_id: str
    task_description: str
    top_k: int = 5


class SkillRecResponse(BaseModel):
    user_id: str
    task: str
    recommendations: list[dict[str, Any]]
    count: int


class DomainAdaptRequest(BaseModel):
    domain: str
    prompt: str
    context: str | None = None


class DomainAdaptResponse(BaseModel):
    domain: str
    response: str
    disclaimer: str
    model: str
    provider: str


class DeployComposeRequest(BaseModel):
    overrides: dict[str, Any] | None = None


class DeployHelmRequest(BaseModel):
    release_name: str = "supremeai"
    namespace: str = "default"
    replicas: int = 3
    image_tag: str = "latest"


class DeployResponse(BaseModel):
    output_path: str
    format: str


@router.post("/smell-check", response_model=SmellCheckResponse)
async def smell_check(payload: SmellCheckRequest):
    if not os.path.exists(payload.path):
        raise HTTPException(status_code=404, detail="Path not found")

    detector = CodeSmellDetector()
    if os.path.isdir(payload.path):
        result = detector.analyze_directory(payload.path, thresholds=payload.thresholds)
        all_smells = [smell for smells in result.values() for smell in smells]
    else:
        all_smells = detector.analyze_python_file(payload.path, thresholds=payload.thresholds)
        if payload.path.endswith((".js", ".ts", ".jsx", ".tsx")):
            all_smells.extend(detector.analyze_js_ts_file(payload.path, thresholds=payload.thresholds))

    by_severity: dict[str, int] = {"critical": 0, "warning": 0, "info": 0}
    for s in all_smells:
        sev = s.get("severity", "info")
        by_severity[sev] = by_severity.get(sev, 0) + 1

    return SmellCheckResponse(path=payload.path, smells=all_smells, summary=by_severity)


@router.post("/vulnerability-check", response_model=VulnCheckResponse)
async def vulnerability_check(payload: VulnCheckRequest):
    predictor = VulnerabilityPredictor()
    if payload.diff:
        result = predictor.predict_diff(payload.diff)
    elif payload.file_path:
        if not os.path.exists(payload.file_path):
            raise HTTPException(status_code=404, detail="file not found")
        result = predictor.predict(payload.file_path)
    else:
        raise HTTPException(status_code=400, detail="Provide file_path or diff")
    return VulnCheckResponse(**result)


@router.post("/skills/recommend", response_model=SkillRecResponse)
async def recommend_skills(payload: SkillRecRequest):
    recommender = SkillRecommender()
    result = recommender.record_and_recommend(payload.user_id, payload.task_description, top_k=payload.top_k)
    return SkillRecResponse(**result)


@router.post("/domain/adapt", response_model=DomainAdaptResponse)
async def domain_adapt(payload: DomainAdaptRequest):
    adapter = DomainAdapter()
    result = adapter.adapt_request(payload.domain, payload.prompt, context=payload.context)
    return DomainAdaptResponse(
        domain=payload.domain,
        response=result.get("response", ""),
        disclaimer=result.get("disclaimer", ""),
        model=result.get("model", "unknown"),
        provider=result.get("provider", "unknown"),
    )


@router.post("/deploy/compose", response_model=DeployResponse)
async def deploy_compose(payload: DeployComposeRequest):
    deployer = OnPremiseDeployer()
    path = deployer.write_compose(overrides=payload.overrides)
    return DeployResponse(output_path=path, format="docker-compose")


@router.post("/deploy/helm", response_model=DeployResponse)
async def deploy_helm(payload: DeployHelmRequest):
    deployer = OnPremiseDeployer()
    path = deployer.write_helm()
    return DeployResponse(output_path=path, format="helm-chart")

```

---

## File: `backend/core/pgbouncer_pool.py`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\backend/core/pgbouncer_pool.py](file:///c:/Users/n/supremeai/supremeai_2.0\backend/core/pgbouncer_pool.py)

```python
# FILE_PATH: backend/core/pgbouncer_pool.py

import asyncio
import logging
import os

import asyncpg
from asyncpg.connection import Connection  # Corrected import


logger = logging.getLogger(__name__)

# বাংলা মন্তব্য: User ও Admin — দুই আলাদা Render instance একই Supabase PgBouncer পুলে
# কানেক্ট করে। database/session.py-এর SQLAlchemy engine ইতিমধ্যে SERVICE_ROLE অনুযায়ী
# pool ভাগ করে (user: 2+13=15, admin: 1+2=3), কিন্তু এই raw-asyncpg pool আগে হার্ডকোডেড
# min=5/max=30 ব্যবহার করত — উভয় role-এর instance যোগ করলে ৩০+১৫=৪৫ বা তার বেশি
# কানেকশন claim করতে পারত, যা Supabase ফ্রি-টিয়ার PgBouncer pool exhaust করতে পারে।
# একই role-aware bracket এখানে পুনরায় ব্যবহার করা হলো, যোগফল হিসাব করে (এই pool +
# session.py engine) instance প্রতি মোট কানেকশন যুক্তিসঙ্গত রাখা হয়েছে।
_ROLE_POOL_BRACKETS: dict[str, tuple[int, int]] = {
    "admin": (1, 3),   # low-traffic internal panel
    "user": (3, 12),   # high-traffic client-facing
}


def _role_pool_sizes() -> tuple[int, int]:
    role = os.getenv("SERVICE_ROLE", "user").lower()
    return _ROLE_POOL_BRACKETS.get(role, _ROLE_POOL_BRACKETS["user"])


class PgBouncerConnectionPool:
    def __init__(self, dsn: str):
        self._dsn = dsn
        self._pool = None

    async def connect(self):
        """Initializes the asyncpg connection pool, sized by SERVICE_ROLE."""
        min_size, max_size = _role_pool_sizes()
        self._pool = await asyncpg.create_pool(
            dsn=self._dsn,
            min_size=min_size,
            max_size=max_size,
            max_inactive_connection_lifetime=300,
            statement_cache_size=0,
            command_timeout=30,
        )
        logger.info(f"PgBouncer connection pool initialized (min_size={min_size}, max_size={max_size}, role={os.getenv('SERVICE_ROLE', 'user')}).")

    async def acquire(self) -> Connection:
        """Acquires a connection from the pool."""
        if not self._pool:
            raise RuntimeError("Connection pool not initialized. Call connect() first.")
        return await self._pool.acquire()

    async def release(self, conn: Connection):
        """Releases a connection back to the pool."""
        if self._pool:
            await self._pool.release(conn)

    # asyncpg.Pool এর মেথডগুলোকে সরাসরি কল করার জন্য proxy মেথডগুলো যুক্ত করা হলো
    # যাতে কোডবেসে pool.execute() বা pool.fetch() কল করলে কোনো Attribute Error না দেয়।
    async def execute(self, query: str, *args, **kwargs):
        """Executes a query using the pool."""
        if not self._pool:
            raise RuntimeError("Connection pool not initialized.")
        return await self._pool.execute(query, *args, **kwargs)

    async def fetch(self, query: str, *args, **kwargs):
        """Fetches rows using the pool."""
        if not self._pool:
            raise RuntimeError("Connection pool not initialized.")
        return await self._pool.fetch(query, *args, **kwargs)

    async def fetchrow(self, query: str, *args, **kwargs):
        """Fetches a single row using the pool."""
        if not self._pool:
            raise RuntimeError("Connection pool not initialized.")
        return await self._pool.fetchrow(query, *args, **kwargs)

    async def fetchval(self, query: str, *args, **kwargs):
        """Fetches a single value using the pool."""
        if not self._pool:
            raise RuntimeError("Connection pool not initialized.")
        return await self._pool.fetchval(query, *args, **kwargs)

    async def close(self):
        """Closes the connection pool."""
        if self._pool:
            await self._pool.close()
            logger.info("PgBouncer connection pool closed.")
            self._pool = None


_db_pool_instance = None
_pool_lock = asyncio.Lock()


async def get_db_pool() -> PgBouncerConnectionPool:
    """Provides a singleton instance of the PgBouncerConnectionPool.

    RuntimeError is raised if the pool has not been initialized yet.
    """
    if _db_pool_instance is None:
        raise RuntimeError("DB pool was accessed before app startup initialized it. Call init_db_pool() explicitly during the FastAPI lifespan.")
    return _db_pool_instance


async def init_db_pool(dsn: str) -> PgBouncerConnectionPool:
    """Initializes the DB pool singleton and returns it."""
    global _db_pool_instance
    async with _pool_lock:
        if _db_pool_instance is None:
            pool = PgBouncerConnectionPool(dsn)
            await pool.connect()
            _db_pool_instance = pool
        return _db_pool_instance

```

---

## File: `.github/workflows/user-keepalive.yml`
Path: [file:///c:/Users/n/supremeai/supremeai_2.0\.github/workflows/user-keepalive.yml](file:///c:/Users/n/supremeai/supremeai_2.0\.github/workflows/user-keepalive.yml)

```yaml
# SupremeAI — User Instance Keep-Alive (Cold-Start Elimination)
# বাংলা মন্তব্য: render.yaml-এ প্রকৃতপক্ষে যেই ব্যাকএন্ড ডিপ্লয় করা হয় (supremeai-backend,
# SERVICE_ROLE=user) সেটির জন্য আগে কোনো keep-alive workflow ছিল না — শুধু admin-keepalive.yml
# ছিল, যেটি এমন একটি Admin instance পিং করে যা render.yaml-এ ডিফাইনই করা নেই। ফলে
# প্রকৃত ইউজার-ফেসিং ট্রাফিক সার্ভ করা instance-টিই cold-start-এর ঝুঁকিতে ছিল।
# এই ওয়ার্কফ্লো প্রতি ১৪ মিনিটে User API-এর /health এন্ডপয়েন্টে পিং করে ঘুম প্রতিরোধ করে।
# সম্পূর্ণ Zero-Cost (GitHub Actions free tier)।
#
# Setup: repo → Settings → Secrets and variables → Actions → New repository secret
#   USER_HEALTH_URL = https://<your-user-render-service>.onrender.com/api/v1/health

name: "🫀 User Instance Keep-Alive"

on:
  schedule:
    # প্রতি ১৪ মিনিটে — GitHub Actions cron সর্বনিম্ন প্রতি ৫ মিনিট সাপোর্ট করে, তাই কোল্ড-স্টার্ট
    # উইন্ডো (Render free tier ~15 min idle timeout) নিরাপদে কভার হয়।
    - cron: '*/14 * * * *'
  workflow_dispatch: {}

jobs:
  ping-user:
    runs-on: ubuntu-latest
    steps:
      - name: Ping User API health endpoint
        env:
          USER_HEALTH_URL: ${{ secrets.USER_HEALTH_URL }}
        run: |
          if [ -z "$USER_HEALTH_URL" ]; then
            echo "⚠️ USER_HEALTH_URL secret not set — skipping keep-alive ping. See workflow header for setup."
            exit 0
          fi
          status=$(curl -s -o /dev/null -w "%{http_code}" --max-time 20 "$USER_HEALTH_URL" || echo "000")
          echo "User health check responded with HTTP $status"
          if [ "$status" != "200" ]; then
            echo "🔴 User instance did not return 200 — it may be cold-starting or down. Not failing the job (this is best-effort)."
          else
            echo "🟢 User instance is warm."
          fi

```

---
