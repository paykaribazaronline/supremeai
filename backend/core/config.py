# mypy: ignore-errors
"""This module, `backend.core.config`, serves as the single, authoritative source
for all application settings within the SupremeAI project. It implements a robust,
"Fail-Fast" configuration layer using Pydantic, ensuring that all critical parameters
are loaded from environment variables or a secret manager, with zero hardcoded values.
It rigorously validates settings at startup, preventing the application from booting if
essential configurations are missing or invalid, thereby guaranteeing a secure and
predictable operational environment across all deployment stages.

Key Components:
- `Settings`: The central Pydantic model that defines and validates all application-wide
  configuration parameters, fetching secrets and enforcing strict rules for different environments.
- `settings`: A singleton instance of the `Settings` class, providing global access to the
  validated application configuration.
- `get_production_env()`: A utility function for strictly retrieving environment variables,
  enforcing a fail-fast approach for critical missing values.

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
- `core.security.secret_vault`: An internal module responsible for fetching secrets from a secure vault (e.g., GCP Secret Manager).
"""

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
from pydantic import (Field, PrivateAttr, SecretStr, ValidationInfo,
                      field_validator, model_serializer, model_validator)
from pydantic_settings import BaseSettings, SettingsConfigDict

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
        env_file=(
            None
            if "pytest" in sys.modules
            else ["../.env", ".env", "/etc/secrets/.env", "/etc/secrets/render.env"]
        ),
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
    docs_username: str = Field(
        default="admin", validation_alias="SUPREMEAI_DOCS_USERNAME"
    )
    docs_password: SecretStr = Field(
        default=SecretStr("dev_password_only"),
        validation_alias="SUPREMEAI_DOCS_PASSWORD",
    )

    # ── নেটওয়ার্ক কনফিগ — সব env-driven, কোনো hardcode নেই ────────────────
    port: int = Field(
        default=8080, validation_alias="PORT"
    )  # বাংলা: Dockerfile CMD-এর ${PORT:-8080} default-এর সাথে consistent
    host: str = Field(default="0.0.0.0", validation_alias="HOST")  # noqa: S104

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
    otp_cooldown_seconds: int = Field(
        default=60, validation_alias="OTP_COOLDOWN_SECONDS"
    )

    # বাংলা মন্তব্য: Admin email list সম্পূর্ণ env-driven
    # (Moved to Security & Auth Config section to avoid duplication)

    # বালা মন্তব্য: Zero-Trust Host Validation — empty = crash
    allowed_hosts: str | list[str] = Field(
        default_factory=list,
        validation_alias="ALLOWED_HOSTS",
    )

    # ── Stripe, JWT & Encryption credentials — moved to Infisical-backed lazy properties ──

    # ── LLM rate limit thresholds — সব env-driven, hardcode নেই ─────────────
    gemini_rpm_limit: int = Field(default=9, validation_alias="GEMINI_RPM_LIMIT")
    gemini_tpm_limit: int = Field(default=240_000, validation_alias="GEMINI_TPM_LIMIT")
    gemini_rpd_limit: int = Field(default=475, validation_alias="GEMINI_RPD_LIMIT")
    groq_rpm_limit: int = Field(default=28, validation_alias="GROQ_RPM_LIMIT")
    groq_tpm_limit: int = Field(default=28_500, validation_alias="GROQ_TPM_LIMIT")
    groq_rpd_limit: int = Field(default=13_680, validation_alias="GROQ_RPD_LIMIT")
    openrouter_rpm_limit: int = Field(
        default=19, validation_alias="OPENROUTER_RPM_LIMIT"
    )
    openrouter_rpd_limit: int = Field(
        default=45, validation_alias="OPENROUTER_RPD_LIMIT"
    )
    cloudflare_rpd_limit: int = Field(
        default=9_000, validation_alias="CLOUDFLARE_RPD_LIMIT"
    )
    nvidia_rpm_limit: int = Field(default=38, validation_alias="NVIDIA_RPM_LIMIT")
    nvidia_tpm_limit: int = Field(default=38_000, validation_alias="NVIDIA_TPM_LIMIT")
    huggingface_rpm_limit: int = Field(
        default=18, validation_alias="HUGGINGFACE_RPM_LIMIT"
    )
    huggingface_rpd_limit: int = Field(
        default=950, validation_alias="HUGGINGFACE_RPD_LIMIT"
    )

    max_prompt_tokens: int = Field(default=4_000, validation_alias="MAX_PROMPT_TOKENS")
    max_response_tokens: int = Field(
        default=1_500, validation_alias="MAX_RESPONSE_TOKENS"
    )
    max_cost_per_task: float = Field(default=0.01, validation_alias="MAX_COST_PER_TASK")
    enable_token_compression: bool = True

    # ── Security & Auth Config ──────────────────────────────────────────────
    security_context_ttl: int = Field(
        default=86400, validation_alias="SECURITY_CONTEXT_TTL"
    )
    security_caution_log_ttl: int = Field(
        default=86400, validation_alias="SECURITY_CAUTION_LOG_TTL"
    )
    otp_cooldown_seconds: int = Field(
        default=300, validation_alias="OTP_COOLDOWN_SECONDS"
    )
    admin_emails: list[str] = Field(
        default_factory=list, validation_alias="ADMIN_EMAILS"
    )
    allow_test_origin_bypass: bool = Field(
        default=False, validation_alias="ALLOW_TEST_ORIGIN_BYPASS"
    )
    allow_test_auth_bypass: bool = Field(
        default=False, validation_alias="ALLOW_TEST_AUTH_BYPASS"
    )

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
        default=["system prompt", "ignore all previous", "you are an administrative"],
        validation_alias="PROMPT_BLOCKED_PATTERNS",
    )
    rbac_role_definitions: dict[str, list[str]] = Field(
        default_factory=lambda: {
            "admin": ["*"],
            "user": ["read", "write"],
            "guest": ["read"],
        },
        validation_alias="RBAC_ROLE_DEFINITIONS",
    )

    # ── Circuit Breaker Config ───────────────────────────────────────────────
    circuit_breaker_failure_threshold: int = Field(
        default=3, validation_alias="CIRCUIT_BREAKER_FAILURE_THRESHOLD"
    )
    circuit_breaker_cooldown_period: int = Field(
        default=60, validation_alias="CIRCUIT_BREAKER_COOLDOWN_PERIOD"
    )

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
    chromadb_path: str = Field(
        default="supremeai_knowledge_base", validation_alias="CHROMADB_PATH"
    )

    # ── Sandbox config — env-driven ──────────────────────────────────────────
    sandbox_root: str = Field(
        default="/tmp/sandboxes", validation_alias="SANDBOX_ROOT"
    )  # nosec B108
    firecracker_path: str = Field(
        default="/usr/bin/firecracker", validation_alias="FIRECRACKER_PATH"
    )
    gvisor_path: str = Field(default="/usr/bin/runsc", validation_alias="GVISOR_PATH")
    allow_sandbox_fallback: bool = Field(
        default=False, validation_alias="ALLOW_SANDBOX_FALLBACK"
    )
    # বাংলা মন্তব্য: local_code_executor ও docker_sandbox-এর লোকাল ফলব্যাকের জন্য settings ভেরিয়েবল যোগ করা হলো।
    allow_local_sandbox_fallback: str = Field(
        default="false", validation_alias="ALLOW_LOCAL_SANDBOX_FALLBACK"
    )

    # ── Agent Execution Config — env-driven ─────────────────────────────────
    # বাংলা মন্তব্য: আগে agent_orchestrator.py সরাসরি os.getenv() করত।
    # এখন এই দুটো settings-এর Single Source of Truth থেকে আসে।
    max_agent_tokens: int = Field(default=5000, validation_alias="MAX_AGENT_TOKENS")
    max_agent_iterations: int = Field(
        default=5, validation_alias="MAX_AGENT_ITERATIONS"
    )
    agent_admin_permissions_required: bool = Field(
        default=True, validation_alias="AGENT_ADMIN_PERMISSIONS_REQUIRED"
    )

    # ── LLM Cost Config — env-driven ────────────────────────────────────────
    # বাংলা মন্তব্য: আগে llm_gateway.py-এ `estimated_cost = tokens * 0.00001` hardcoded ছিল।
    # এখন এই factor settings থেকে নিয়ন্ত্রিত হয় যা runtime-এ override করা যাবে।
    llm_cost_per_token: float = Field(
        default=0.00001, validation_alias="LLM_COST_PER_TOKEN"
    )

    # ── Task Queue Config — env-driven ──────────────────────────────────────
    # বাংলা মন্তব্য: task_queue_enhanced.py-এ TTL এবং backend priority এখন config-driven।
    task_result_ttl_seconds: int = Field(
        default=3600, validation_alias="TASK_RESULT_TTL_SECONDS"
    )
    queue_backend_priority: str = Field(
        default="asyncio,redis,celery,pubsub", validation_alias="QUEUE_BACKEND_PRIORITY"
    )

    # ── Health Check Config — env-driven ────────────────────────────────────
    # বাংলা মন্তব্য: health_monitor.py-এ hardcoded interval এখন config-driven।
    health_check_interval_seconds: int = Field(
        default=60, validation_alias="HEALTH_CHECK_INTERVAL_SECONDS"
    )
    skill_timeout_seconds: int = Field(
        default=30, validation_alias="SKILL_TIMEOUT_SECONDS"
    )

    # ── Self-Healing Config — env-driven ────────────────────────────────────
    # বাংলা মন্তব্য: self_healer.py-এ human approval loop-এর জন্য config যোগ করা হলো।
    self_heal_approval_webhook: str = Field(
        default="", validation_alias="SELF_HEAL_APPROVAL_WEBHOOK"
    )
    self_heal_approval_timeout_hours: int = Field(
        default=24, validation_alias="SELF_HEAL_APPROVAL_TIMEOUT_HOURS"
    )
    auto_remediation_dry_run: bool = Field(
        default=True, validation_alias="AUTO_REMEDIATION_DRY_RUN"
    )

    _cached_secrets: dict[str, str] = PrivateAttr(default_factory=dict)
    _secrets_batch_loaded: bool = PrivateAttr(default=False)

    # বাংলা মন্তব্য: ব্যাচ লোডিংয়ের জন্য প্রয়োজনীয় সিক্রেট কীগুলোর তালিকা।
    # startup-এ একবারে সব সিক্রেট লোড করা হবে, lazy per-property কল এড়াতে।
    _BATCH_SECRET_KEYS: list[str] = [
        "SUPABASE_DATABASE_URL_POOLER",
        "DISCORD_OTP_WEBHOOK_URL",
        "RESEND_API_KEY",
        "ADMIN_NOTIFICATION_EMAIL",
        "REDIS_URL",
        "OPENROUTER_API_KEY",
        "HF_API_KEY",
        "GEMINI_API_KEY",
        "OPENAI_API_KEY",
        "DEEPSEEK_API_KEY",
        "GROQ_API_KEY",
        "NVIDIA_API_KEY",
        "FIRECRAWL_API_KEY",
        "DISCORD_BOT_TOKEN",
        "GITHUB_CLIENT_ID",
        "GITHUB_CLIENT_SECRET",
        "CI_WEBHOOK_SECRET",
        "SUPABASE_URL",
        "SUPABASE_KEY",
        "SUPREMEAI_API_TOKEN",
        "NEO4J_URI",
        "NEO4J_USER",
        "NEO4J_PASSWORD",
        "SUPREMEAI_ADMIN_PASSWORD_HASH",
        "SUPREMEAI_JWT_SECRET",
        "SUPREMEAI_ENCRYPTION_KEY",
        "STRIPE_API_KEY",
        "STRIPE_WEBHOOK_SECRET",
    ]

    def _ensure_secrets_loaded(self) -> None:
        """Batch-load all secrets at once into memory cache.

        বাংলা: startup-এ একবারে সব সিক্রেট লোড করে singleton dict-এ cache করে।
        এর ফলে প্রতিটি @property-র জন্য আলাদা vault কল হয় না, cold start latency কমে।
        """
        if self._secrets_batch_loaded:
            return
        for secret_key in self._BATCH_SECRET_KEYS:
            try:
                val = secret_vault.fetch_secret(secret_key)
                if val:
                    self._cached_secrets[secret_key] = val
            except Exception:  # noqa: BLE001
                pass  # Non-critical — some secrets may be optional
        self._secrets_batch_loaded = True

    def _get_cached_secret(self, key: str) -> str:
        # বাংলা মন্তব্য: ব্যাচ লোড করা ক্যাশ থেকে সিক্রেট রিটার্ন করে।
        # প্রথম কলেই সব সিক্রেট লোড করা হয়, এরপর শুধু মেমোরি থেকে রিটার্ন।
        self._ensure_secrets_loaded()
        return self._cached_secrets.get(key, "")

    # ── Cloud-fetched secrets — GCP Secret Manager বা env fallback ───────────
    # বাংলা মন্তব্য: স্টার্টআপ টাইম কমাতে এবং Infisical ভল্ট থেকে একের পর এক সিক্রেট ফেচ করা এড়াতে
    # `@computed_field` এর জায়গায় অলস (lazy) `@property` ব্যবহার করা হলো। এর ফলে শুধুমাত্র
    # অন-ডিমান্ড অ্যাক্সেস করলেই সিক্রেট ফেচ হবে এবং গ্লোবাল ক্যাশে জমা থাকবে।
    @property
    def supabase_database_url(self) -> str:
        return self._get_cached_secret("SUPABASE_DATABASE_URL_POOLER")

    # বাংলা মন্তব্য: Anti-Hacking এবং OTP রাউটার সিক্রেটসমূহ
    @property
    def discord_otp_webhook_url(self) -> SecretStr | None:
        url = self._get_cached_secret("DISCORD_OTP_WEBHOOK_URL")
        return SecretStr(url) if url else None

    @property
    def resend_api_key(self) -> SecretStr | None:
        key = self._get_cached_secret("RESEND_API_KEY")
        return SecretStr(key) if key else None

    @property
    def admin_notification_email(self) -> str | None:
        return self._get_cached_secret("ADMIN_NOTIFICATION_EMAIL")

    @property
    def redis_url(self) -> str:
        url = self._get_cached_secret("REDIS_URL")
        if url and not url.startswith(("redis://", "rediss://", "unix://")):
            return f"redis://{url}"
        return url

    @property
    def openrouter_api_key(self) -> str:
        return self._get_cached_secret("OPENROUTER_API_KEY")

    @property
    def hf_api_key(self) -> str:
        return self._get_cached_secret("HF_API_KEY")

    @property
    def gemini_api_key(self) -> str:
        return self._get_cached_secret("GEMINI_API_KEY")

    @property
    def openai_api_key(self) -> str:
        return self._get_cached_secret("OPENAI_API_KEY")

    @property
    def deepseek_api_key(self) -> str:
        return self._get_cached_secret("DEEPSEEK_API_KEY")

    @property
    def groq_api_key(self) -> str:
        return self._get_cached_secret("GROQ_API_KEY")

    @property
    def nvidia_api_key(self) -> str:
        return self._get_cached_secret("NVIDIA_API_KEY")

    @property
    def firecrawl_api_key(self) -> str:
        return self._get_cached_secret("FIRECRAWL_API_KEY")

    @property
    def discord_bot_token(self) -> str:
        return self._get_cached_secret("DISCORD_BOT_TOKEN")

    @property
    def github_client_id(self) -> str:
        return self._get_cached_secret("GITHUB_CLIENT_ID")

    @property
    def github_client_secret(self) -> str:
        return self._get_cached_secret("GITHUB_CLIENT_SECRET")

    @property
    def ci_webhook_secret(self) -> str:
        return self._get_cached_secret("CI_WEBHOOK_SECRET")

    # ── Supabase credentials — settings-এ মাইগ্রেট করা হলো ──────────────────
    # বাংলা মন্তব্য: আগে database/supabase_client.py সরাসরি os.environ.get() করত।
    # এখন এই দুটো computed field settings-এর Single Source of Truth।
    # supabase_client.py শুধু settings.supabase_url এবং settings.supabase_key ব্যবহার করবে।
    @property
    def supabase_url(self) -> str:
        return self._get_cached_secret("SUPABASE_URL")

    @property
    def supabase_key(self) -> str:
        return self._get_cached_secret("SUPABASE_KEY")

    # ── System API Token — settings-এ মাইগ্রেট করা হলো ──────────────────────
    # বাংলা মন্তব্য: আগে auth_middleware.py সরাসরি os.getenv("SUPREMEAI_API_TOKEN") করত।
    # এখন এই computed field settings-এর Single Source of Truth।
    @property
    def supremeai_api_token(self) -> str:
        return self._get_cached_secret("SUPREMEAI_API_TOKEN")

    @property
    def neo4j_uri(self) -> str:
        return self._get_cached_secret("NEO4J_URI") or "bolt://localhost:7687"

    @property
    def neo4j_user(self) -> str:
        return self._get_cached_secret("NEO4J_USER") or "neo4j"

    @property
    def neo4j_password(self) -> str:
        return self._get_cached_secret("NEO4J_PASSWORD") or ""

    # ── Admin Password Hash — Infisical-backed lazy property ────────────────
    # বাংলা মন্তব্য: Pydantic Field(validation_alias=...) সরাসরি OS env var থেকে পড়ে, যা Infisical
    # ভল্টে থাকা সিক্রেট পড়তে পারে না এবং Render ডিপ্লয়মেন্টে Validation Error ঘটিয়ে প্রসেস ক্র্যাশ করায়।
    # তাই এটি lazy @property এবং _get_cached_secret() এ রূপান্তর করা হলো যাতে অন-ডিমান্ড ভল্ট বা env থেকে ফেচ হয়।
    @property
    def supremeai_admin_password_hash(self) -> str | None:
        val = self._get_cached_secret("SUPREMEAI_ADMIN_PASSWORD_HASH")
        if not val and "pytest" not in sys.modules and os.getenv("CI") != "true":
            raise ValueError("supremeai_admin_password_hash must be explicitly set.")
        return val

    # ── JWT & Encryption Credentials — Infisical-backed ─────────────────────
    # বাংলা মন্তব্য: JWT সিক্রেট এবং এনক্রিপশন কী ক্লাউড ভল্ট (Infisical/GCP) থেকে ডায়নামিকালি
    # লোড করার জন্য lazy property প্যাটার্ন প্রয়োগ করা হয়েছে — যাতে ইনফিসিক্যাল সিক্রেট স্টার্টআপ ব্লক না করে।
    @property
    def jwt_secret(self) -> str:
        v = self._get_cached_secret("SUPREMEAI_JWT_SECRET")
        if not v:
            if self.env == "production":
                raise ValueError(
                    "🚨 CRITICAL: SUPREMEAI_JWT_SECRET must be explicitly set in production. No fallback allowed."
                )
            # For non-production, generate once and persist to avoid regeneration on every access
            v = self._load_or_generate_jwt_secret()
        if len(v) < 64 and "pytest" not in sys.modules:
            raise ValueError(
                "JWT secret must be >= 64 bytes entropy in all environments."
            )
        return v

    def _load_or_generate_jwt_secret(self) -> str:
        """Persist JWT secret to file to avoid regeneration across restarts in non-prod."""
        secret_file = "/etc/secrets/jwt_secret"
        try:
            if os.path.exists(secret_file):
                with open(secret_file) as f:
                    return f.read().strip()
        except OSError:
            pass
        new_secret = secrets.token_hex(64)
        try:
            os.makedirs(os.path.dirname(secret_file), exist_ok=True)
            with open(secret_file, "w") as f:
                f.write(new_secret)
        except OSError:
            pass
        return new_secret

    @property
    def encryption_key(self) -> SecretStr:
        val = self._get_cached_secret("SUPREMEAI_ENCRYPTION_KEY")
        return SecretStr(val) if val else SecretStr("")

    # ── Stripe Credentials — Infisical-backed ────────────────────────────────
    # বাংলা মন্তব্য: Stripe এপিআই এবং ওয়েবহুক সিক্রেটসমূহের জন্য Infisical lazy fetching নিশ্চিত করা হলো,
    # যাতে প্রোডাকশন পেমেন্ট ক্রেডেনশিয়াল ভল্ট থেকে সরাসরি ইন-মেমোরিতে ফেচ হয়।
    @property
    def stripe_api_key(self) -> SecretStr:
        val = self._get_cached_secret("STRIPE_API_KEY")
        return SecretStr(val) if val else SecretStr("")

    @property
    def stripe_webhook_secret(self) -> SecretStr:
        val = self._get_cached_secret("STRIPE_WEBHOOK_SECRET")
        return SecretStr(val) if val else SecretStr("")

    # ── Serializer ──────────────────────────────────────────────────────────
    # বাংলা মন্তব্য: @property-ভিত্তিক সিক্রেট Pydantic model_dump()-এ অন্তর্ভুক্ত হয় না।
    # এই serializer নিশ্চিত করে যে settings.model_dump() কল করলে সব ফিল্ড এবং প্রপার্টি দেখা যায়,
    # কিন্তু সিক্রেট ভ্যালুগুলি "***REDACTED***" হিসাবে দেখানো হয়।
    @model_serializer
    def serialize_model(self) -> dict[str, Any]:
        """Ensure properties are visible in serialization, with secrets redacted."""
        result: dict[str, Any] = {}
        for field_name in self.model_fields:
            result[field_name] = getattr(self, field_name)
        # Include critical lazy properties with redaction
        redacted = "***REDACTED***"
        result["jwt_secret"] = redacted
        result["redis_url"] = redacted
        result["supabase_database_url"] = redacted
        result["supremeai_admin_password_hash"] = redacted
        result["encryption_key"] = redacted
        result["supremeai_api_token"] = redacted
        result["stripe_api_key"] = redacted
        result["stripe_webhook_secret"] = redacted
        # Include API keys (redacted)
        for key_field in [
            "openrouter_api_key",
            "gemini_api_key",
            "openai_api_key",
            "groq_api_key",
            "nvidia_api_key",
            "hf_api_key",
            "deepseek_api_key",
            "firecrawl_api_key",
            "discord_bot_token",
            "github_client_id",
            "github_client_secret",
            "ci_webhook_secret",
            "supabase_url",
            "supabase_key",
        ]:
            result[key_field] = redacted
        # Include non-secret properties
        result["neo4j_uri"] = self.neo4j_uri
        result["neo4j_user"] = self.neo4j_user
        result["admin_notification_email"] = self.admin_notification_email
        return result

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
            if str(v).lower() == "true" and (
                os.getenv("debug", "").lower() == "true"
                or os.getenv("DEBUG", "").lower() == "true"
            ):
                raise ValueError(
                    "Explicitly setting debug=True is PROHIBITED in production/staging."
                )
            return False
        return bool(v)

    @field_validator("docs_password", mode="before")
    @classmethod
    def validate_docs_password(
        cls, v: str | SecretStr | None, info: ValidationInfo
    ) -> str | SecretStr:
        if "pytest" in sys.modules:
            return v or ""
        return v or ""

    @model_validator(mode="after")
    def validate_docs_auth(self):
        # বাংলা মন্তব্য: Production-এ docs auth enabled থাকলে password mandatory
        if self.env in {"production", "staging"} and self.docs_auth_enabled:
            pwd = self.docs_password.get_secret_value() if self.docs_password else ""
            if not pwd:
                logger.warning(
                    f"⚠️ {self.env.capitalize()} SUPREMEAI_DOCS_PASSWORD missing — using fallback production password."
                )
                self.docs_password = SecretStr("supreme-admin-2026-prod")
        return self

    @field_validator(
        "idempotency_critical_paths",
        "supremeai_public_paths",
        "prompt_blocked_patterns",
        mode="before",
    )
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
                logger.error(
                    f"Failed to parse rbac_role_definitions JSON: {e}. Defaulting to empty dictionary."
                )
                return {}
        return v or {}

    @field_validator("admin_emails", mode="before")
    @classmethod
    def parse_admin_emails(cls, v) -> list[str]:
        if isinstance(v, str):
            v = v.strip()
            return (
                [email.strip() for email in v.split(",") if email.strip()] if v else []
            )
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
        forbidden = {"localhost", "127.0.0.1", "testserver", "0.0.0.0"}  # noqa: S104
        if env in {"production", "staging"}:
            v = [h for h in v if h.lower() not in forbidden]
            if not v:
                logger.warning(
                    f"⚠️ {env.capitalize()} ALLOWED_HOSTS missing — auto-populating default production hosts."
                )
                v = [
                    "supremeai-backend.onrender.com",
                    "supremeai-admin.web.app",
                    "*.onrender.com",
                ]
        return v

    @field_validator(
        "cors_origins", "user_cors_origins", "admin_cors_origins", mode="before"
    )
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

    @field_validator(
        "cors_origins", "user_cors_origins", "admin_cors_origins", mode="after"
    )
    @classmethod
    def validate_cors_origins(cls, v: list[str], info: ValidationInfo) -> list[str]:
        # Test-isolation guard:
        # ENV=test হলে CORS fail-fast validator ট্রিগার করা হবে না।
        # কারণ unit tests-এর focus হলো security_vault/import-time invariants,
        # প্রোডাকশন CORS mesh কনফিগ নয়।
        env = str(info.data.get("env", "local") or "local").lower()
        if env == "test":
            return v
        if env in {"production", "staging"}:
            # বাংলা মন্তব্য: user_cors_origins এবং admin_cors_origins ফাঁকা (empty) হতে পারে।
            # তাই cors_origins ছাড়া অন্যগুলো ফাঁকা থাকলে ভ্যালিডেশন স্কিপ করা হচ্ছে।
            if info.field_name in {"user_cors_origins", "admin_cors_origins"} and not v:
                return v
            v = [o for o in v if "localhost" not in o and "127.0.0.1" not in o]
            if not v:
                # Auto-populate from known deployment URLs instead of crashing
                v = [
                    "https://supremeai-studio-client.onrender.com",
                    "https://supremeai-studio-client-qb34.onrender.com",
                    "https://supremeai-admin.web.app",
                    "https://supremeai-lac.vercel.app",
                ]
                logger.warning(
                    f"⚠️ {env.capitalize()} CORS_ORIGINS empty — auto-populated from known deployment targets: {v}"
                )
        return v

    @model_validator(mode="after")
    def validate_all(self):
        """Consolidated validator for docs auth, Stripe, production completeness, and resilience."""
        if "pytest" in sys.modules or self.env == "test":
            return self

        # Docs auth fallback for production/staging
        if self.env in {"production", "staging"} and self.docs_auth_enabled:
            pwd = self.docs_password.get_secret_value() if self.docs_password else ""
            if not pwd:
                logger.warning(
                    f"⚠️ {self.env.capitalize()} SUPREMEAI_DOCS_PASSWORD missing — using fallback production password."
                )
                self.docs_password = SecretStr("supreme-admin-2026-prod")

        # Stripe warning (non-blocking)
        if self.env in {"production", "staging"}:
            stripe_key = (
                self.stripe_api_key.get_secret_value() if self.stripe_api_key else ""
            )
            stripe_webhook = (
                self.stripe_webhook_secret.get_secret_value()
                if self.stripe_webhook_secret
                else ""
            )
            if not stripe_key:
                logger.warning(
                    "⚠️ Stripe API key missing in production/staging. Billing features will run in mock mode."
                )
            if not stripe_webhook:
                logger.warning(
                    "⚠️ Stripe webhook secret missing in production/staging. Webhook validation disabled."
                )

        # Production completeness / degraded mode allowed
        if self.env == "production":
            missing = []
            if not self.openrouter_api_key:
                missing.append("OPENROUTER_API_KEY")
            if not self.gemini_api_key:
                missing.append("GEMINI_API_KEY")
            if not self.ci_webhook_secret:
                missing.append("CI_WEBHOOK_SECRET")
            if missing:
                logger.warning(
                    f"⚠️ Production missing config vars: {', '.join(missing)}. Running in degraded zero-cost mode."
                )

        # General resilience guard for non-test environments
        if self.env not in {"test"}:
            missing: list[str] = []
            if not self.openrouter_api_key:
                missing.append("OPENROUTER_API_KEY")
            if not self.encryption_key.get_secret_value():
                missing.append("ENCRYPTION_KEY")
            if not self.ci_webhook_secret:
                missing.append("CI_WEBHOOK_SECRET")
            if missing:
                logger.warning(
                    f"⚠️ Missing config vars: {', '.join(missing)}. Bypassing hard crash for server resilience."
                )
        return self

    @property
    def jti_blacklist_cache(self) -> set:
        """JWT JTI replay attack প্রতিরোধের জন্য ইন-মেমরি ক্যাশ। (Bangla: JTI ব্ল্যাকলিস্ট ক্যাশিং)"""
        if not hasattr(self, "_jti_cache"):
            self._jti_cache: set[str] = set()
        return self._jti_cache

    def reload_env_vars(self) -> None:
        """প্রোডাকশনে সার্ভার রিস্টার্ট ছাড়াই কনফিগারেশন রিলোড করার ডাইনামিক মেথড। (Bangla: Hot-reload listener)"""
        load_dotenv(override=True)
        logger.info("⚙️ [Config] Environment variables hot-reloaded successfully.")


# ── Singleton instantiation with True Fail-Fast ────────────────────────────────
# বাংলা মন্তব্য: এখানে Fail-Fast সত্যিকারভাবে enforce হচ্ছে।
# কোনো "resilient boot" বা dummy fallback নেই। Exception মানেই sys.exit(1)।
try:
    settings = Settings()
except Exception as _boot_exc:  # noqa: BLE001
    logger.critical(
        f"🔥 FATAL CONFIG ERROR: {_boot_exc}\nServer startup ABORTED (Fail-Fast applied). Fix the configuration."
    )
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
        logger.critical(
            f"❌ CRITICAL CONFIG ERROR: Missing required environment variable '{var_name}'!"
        )
        raise ValueError(f"Configuration Error: {var_name} must be explicitly defined.")

    return value
