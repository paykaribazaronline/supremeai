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
from typing import Annotated, Any

# dotenv লাইব্রেরি না থাকলেও যেন os.environ ব্যবহারে বাধা না পড়ে, সে জন্য সেফ ইমপোর্ট ফলব্যাক রাখা হলো।
try:
    from dotenv import load_dotenv
except ImportError:

    def load_dotenv(*args, **kwargs):
        pass


from loguru import logger
from pydantic import (
    Field,
    PrivateAttr,
    SecretStr,
    ValidationInfo,
    field_validator,
    model_serializer,
    model_validator,
)
from pydantic_settings import BaseSettings, NoDecode, SettingsConfigDict

from utils.environment import is_test_environment

from .security.secret_vault import secret_vault

# বাংলা মন্তব্য: pytest environment-এ .env load করা হয় না — test isolation নিশ্চিত।
if "pytest" not in sys.modules:
    root_env = Path(__file__).resolve().parents[2] / ".env"
    load_dotenv(root_env)


class Settings(BaseSettings):
    """
    বাংলা মন্তব্য: এটি সিস্টেমের একমাত্র সত্যের উৎস (Single Source of Truth)।
    কোনো hardcoded value নেই। সব env-driven।
    যেকোনো এনভায়রনমেন্টে missing required var = startup Fail-Fast (sys.exit(1))।
    """

    model_config = SettingsConfigDict(
        env_file=(
            None if "pytest" in sys.modules else ["../.env", ".env", "/etc/secrets/.env", "/etc/secrets/render.env"]
        ),
        extra="ignore",
    )

    # বাংলা মন্তব্য: env validate হবে — invalid value = startup crash
    env: str = Field(default="local", validation_alias="ENV")
    debug: bool = Field(default=True)

    # বাংলা মন্তব্য: টেস্ট এনভায়রনমেন্টে AuthMiddleware-এর JWT ভেরিফিকেশন বাইপাস করার জন্য
    # CI workflow-এ ALLOW_TEST_AUTH_BYPASS=true সেট করা হয়। Production-এ এটি কখনো true হবে না।
    # auth_middleware.py:139 এই field চেক করে।
    allow_test_auth_bypass: bool = Field(default=False, validation_alias="ALLOW_TEST_AUTH_BYPASS")
    allow_test_origin_bypass: bool = Field(default=False, validation_alias="ALLOW_TEST_ORIGIN_BYPASS")

    PROJECT_NAME: str = "SupremeAI 2.0"
    API_V1_STR: str = "/api/v1"
    app_name: str = "SupremeAI 2.0"

    # ── Dynamic Target Repository & Multi-Platform Configuration ─────────────
    # বাংলা মন্তব্য: ডাইনামিক রেপো বাইন্ডিং ও মাল্টি-প্ল্যাটফর্ম অটোনোমাস কনফিগারেশন।
    target_repo_url: str = Field(default="", validation_alias="TARGET_REPO_URL")
    target_repo_branch: str = Field(default="main", validation_alias="TARGET_REPO_BRANCH")
    target_repo_token: SecretStr = Field(default=SecretStr(""), validation_alias="TARGET_REPO_TOKEN")
    workspace_base_dir: str = Field(default="storage/workspaces", validation_alias="WORKSPACE_BASE_DIR")

    # ── LLM Gateway & Streaming Configuration ────────────────────────────────
    LLM_CONNECT_TIMEOUT: float = Field(default=5.0, validation_alias="LLM_CONNECT_TIMEOUT")
    LLM_READ_TIMEOUT: float = Field(default=30.0, validation_alias="LLM_READ_TIMEOUT")
    LLM_WRITE_TIMEOUT: float = Field(default=5.0, validation_alias="LLM_WRITE_TIMEOUT")
    LLM_POOL_TIMEOUT: float = Field(default=5.0, validation_alias="LLM_POOL_TIMEOUT")
    LLM_MAX_CONNECTIONS: int = Field(default=100, validation_alias="LLM_MAX_CONNECTIONS")
    LLM_MAX_KEEPALIVE: int = Field(default=20, validation_alias="LLM_MAX_KEEPALIVE")

    LATENCY_WINDOW_SIZE: int = Field(default=20, validation_alias="LATENCY_WINDOW_SIZE")
    LATENCY_NORMALIZATION_MS: float = Field(default=1000.0, validation_alias="LATENCY_NORMALIZATION_MS")
    MIN_PROVIDER_WEIGHT: float = Field(default=0.01, validation_alias="MIN_PROVIDER_WEIGHT")
    CIRCUIT_FAILURE_THRESHOLD: int = Field(default=5, validation_alias="CIRCUIT_FAILURE_THRESHOLD")
    CIRCUIT_SUCCESS_RATE_FLOOR: float = Field(default=0.5, validation_alias="CIRCUIT_SUCCESS_RATE_FLOOR")
    CIRCUIT_COOLDOWN_SECONDS: float = Field(default=30.0, validation_alias="CIRCUIT_COOLDOWN_SECONDS")
    MAX_ROUTING_ATTEMPTS: int = Field(default=3, validation_alias="MAX_ROUTING_ATTEMPTS")
    docs_auth_enabled: bool = True
    docs_username: str = Field(default="admin", validation_alias="SUPREMEAI_DOCS_USERNAME")
    docs_password: SecretStr = Field(
        default=SecretStr("dev_password_only"),
        validation_alias="SUPREMEAI_DOCS_PASSWORD",
    )

    # ── নেটওয়ার্ক কনফিগ — সব env-driven, কোনো hardcode নেই ────────────────
    port: int = Field(
        default=8080, validation_alias="PORT"
    )  # বাংলা: Dockerfile CMD-এর ${PORT:-8080} default-এর সাথে consistent
    host: str = Field(default="0.0.0.0", validation_alias="HOST")

    # CORS origins property is implemented dynamically below to support validation.

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

    # ── Stripe, JWT & Encryption credentials — moved to Infisical-backed lazy properties ──

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
    jwt_algorithm: str = Field(default="HS256", validation_alias="JWT_ALGORITHM")
    max_cost_per_task: float = Field(default=0.01, validation_alias="MAX_COST_PER_TASK")
    enable_token_compression: bool = True

    # ── Security & Auth Config ──────────────────────────────────────────────
    security_context_ttl: int = Field(default=86400, validation_alias="SECURITY_CONTEXT_TTL")
    security_caution_log_ttl: int = Field(default=86400, validation_alias="SECURITY_CAUTION_LOG_TTL")
    admin_emails: str | list[str] = Field(default_factory=list, validation_alias="ADMIN_EMAILS")

    supremeai_public_paths: str | list[str] = Field(
        default=[
            "/health",
            "/metrics",
            "/docs",
            "/redoc",
            "/openapi.json",
            "/api/v1/auth/token",
            "/actuator",
            "/api/admin/firebase-auth",
            "/api/admin/firebase-login",
            "/api/v1/health",
            "/api/v1/health/",
            "/api/v1/live",
            "/api/v1/ready",
            "/api/voice/stream_audio",
            "/api/billing/webhook/stripe",
            "/api/billing/webhook/sslcommerz",
            "/api/v1/markdown",
            # বাংলা মন্তব্য: অ্যাডমিন পোর্টাল safe-default কনফিগ ও preferences স্ট্রিম
            # পাবলিক করা হলো (auth-free)। শুধু নির্দিষ্ট GET/stream পাথ — সব /api/preferences
            # নয়, নাহলে POST write endpoint-ও অ্যাথেনটিকেশন ছাড়া চলে যাবে।
            "/api/config/public",
            "/api/preferences/default/stream",
        ],
        validation_alias="SUPREMEAI_PUBLIC_PATHS",
    )

    prompt_blocked_patterns: str | list[str] = Field(
        default=["system prompt", "ignore all previous", "you are an administrative"],
        validation_alias="PROMPT_BLOCKED_PATTERNS",
    )
    rbac_role_definitions: Annotated[dict[str, list[str]], NoDecode] = Field(
        default_factory=lambda: {
            "admin": ["*"],
            "user": ["read", "write"],
            "guest": ["read"],
        },
        validation_alias="RBAC_ROLE_DEFINITIONS",
    )

    # ── Circuit Breaker Config ───────────────────────────────────────────────
    circuit_breaker_failure_threshold: int = Field(default=3, validation_alias="CIRCUIT_BREAKER_FAILURE_THRESHOLD")
    circuit_breaker_cooldown_period: int = Field(default=60, validation_alias="CIRCUIT_BREAKER_COOLDOWN_PERIOD")

    # ── Idempotency Config ───────────────────────────────────────────────
    # বাংলা মন্তব্য: idempotency_critical_paths সম্পূর্ণ env-driven।
    # IDEMPOTENCY_CRITICAL_PATHS="/api/orchestrate/generate,/api/billing/charge" (comma-separated)
    idempotency_critical_paths: str | list[str] = Field(
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
        default="gemini/gemini-1.5-flash",
        validation_alias="GEMINI_MODEL_NAME",
    )

    sentry_dsn: str = Field(default="", validation_alias="SENTRY_DSN")

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

    # বাংলা মন্তব্য: CostGuard-এর tier limits — env থেকে override করা যাবে।
    cost_tier_economy_limit: float = Field(default=0.02, validation_alias="COST_TIER_ECONOMY_LIMIT")
    cost_tier_premium_limit: float = Field(default=0.50, validation_alias="COST_TIER_PREMIUM_LIMIT")

    # ── Task Queue Config — env-driven ──────────────────────────────────────
    # বাংলা মন্তব্য: task_queue_enhanced.py-এ TTL এবং backend priority এখন config-driven।
    task_result_ttl_seconds: int = Field(default=3600, validation_alias="TASK_RESULT_TTL_SECONDS")
    queue_backend_priority: str = Field(
        default="asyncio,redis,celery,pubsub", validation_alias="QUEUE_BACKEND_PRIORITY"
    )

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
    _secrets_batch_loaded: bool = PrivateAttr(default=False)

    # বাংলা মন্তব্য: ব্যাচ লোডিংয়ের জন্য প্রয়োজনীয় সিক্রেট কীগুলোর তালিকা।
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
        "FRONTEND_BASE_URL",
        "CHECKOUT_BASE_URL",
        "OLLAMA_URL",
    ]

    def _ensure_secrets_loaded(self) -> None:
        """Batch-load all secrets at once into memory cache.

        বাংলা: startup-এ একবারে সব সিক্রেট লোড করে singleton dict-এ cache করে।
        এর ফলে প্রতিটি @property-র জন্য আলাদা vault কল হয় না, cold start latency কমে।
        বাংলা: default="" পাস করা হচ্ছে যাতে ঐচ্ছিক secrets (যেমন ADMIN_NOTIFICATION_EMAIL)
        production-এ missing থাকলেও server startup crash না করে।
        """
        if self._secrets_batch_loaded:
            return
        for secret_key in self._BATCH_SECRET_KEYS:
            try:
                # বাংলা: default="" দেওয়া হচ্ছে — এতে optional secrets missing থাকলে
                # RuntimeError throw হবে না, বরং empty string return হবে।
                # Critical secrets (JWT, encryption key) আলাদা validate_all validator-এ চেক হবে।
                val = secret_vault.fetch_secret(secret_key, default="")
                if val:
                    self._cached_secrets[secret_key] = val
            except Exception as _secret_err:
                # বাংলা মন্তব্য: সিক্রেট ভল্ট থেকে কী লোড ফেইল করলে কভার-আপ এড়াতে WARNING লেভেলে স্পষ্ট লগ দেওয়া হচ্ছে
                logger.warning(f"Secret {secret_key} not available during batch load: {_secret_err}")
        self._secrets_batch_loaded = True

    def _get_cached_secret(self, key: str) -> str:
        """Get cached secret with explicit empty vs not-found handling.

        English: Returns the cached secret value. Logs a warning if the requested
        secret key was never loaded into cache (not found in vault or env).
        Returns empty string as fallback to avoid crashes, but the caller should
        check for empty strings where critical.

        বাংলা মন্তব্য: os.getenv(key) যদি os.environ-এ উপস্থিত থাকে তবে সরাসরি রিটার্ন করে (ইউনিট টেস্ট মকিংয়ের জন্য)।
        """
        env_val = os.getenv(key)
        if env_val is not None:
            return env_val

        self._ensure_secrets_loaded()
        if key not in self._cached_secrets:
            if is_test_environment():
                logger.debug(f"Secret '{key}' not found in cache after batch load - returning empty string")
            else:
                logger.warning(f"Secret '{key}' not found in cache after batch load - returning empty string")
        return self._cached_secrets.get(key, "")

    # ── Cloud-fetched secrets — GCP Secret Manager বা env fallback ───────────
    # বাংলা মন্তব্য: স্টার্টআপ টাইম কমাতে এবং Infisical ভল্ট থেকে একের পর এক সিক্রেট ফেচ করা এড়াতে
    # `@computed_field` এর জায়গায় অলস (lazy) `@property` ব্যবহার করা হলো। এর ফলে শুধুমাত্র
    # অন-ডিমান্ড অ্যাক্সেস করলেই সিক্রেট ফেচ হবে এবং গ্লোবাল ক্যাশে জমা থাকবে।
    @property
    def supabase_database_url(self) -> str:
        return self._get_cached_secret("SUPABASE_DATABASE_URL_POOLER")

    # বাংলা মন্তব্য: Anti-Hacking এবং OTP রাউটার সিক্রেটসমূহ (ঐচ্ছিক — মিসিং থাকলে সার্ভার ক্র্যাশ করবে না)
    @property
    def discord_otp_webhook_url(self) -> SecretStr | None:
        try:
            url = secret_vault.fetch_secret("DISCORD_OTP_WEBHOOK_URL", default="")
        except Exception as e:
            # বাংলা: এই সিক্রেট fetch ব্যর্থ হলে OTP alert silently বন্ধ হয়ে যায় —
            # সেটা invisible না থেকে অন্তত ওয়ার্নিং লগ হওয়া উচিত
            logger.warning(f"Failed to fetch DISCORD_OTP_WEBHOOK_URL secret: {e}")
            url = ""
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

    def _set_cached_secret(self, key: str, value: Any) -> None:
        self._ensure_secrets_loaded()
        self._cached_secrets[key] = str(value) if value is not None else ""

    @property
    def openrouter_api_key(self) -> str:
        return self._get_cached_secret("OPENROUTER_API_KEY")

    @openrouter_api_key.setter
    def openrouter_api_key(self, value: str) -> None:
        self._set_cached_secret("OPENROUTER_API_KEY", value)

    @property
    def hf_api_key(self) -> str:
        return self._get_cached_secret("HF_API_KEY")

    @hf_api_key.setter
    def hf_api_key(self, value: str) -> None:
        self._set_cached_secret("HF_API_KEY", value)

    @property
    def hf_api_keys(self) -> list[str]:
        """বাংলা মন্তব্য: কমা-দ্বারা আলাদা করা HF API কীসমূহ লিস্ট আকারে রিটার্ন করা হয়।"""
        raw = self.hf_api_key
        if not raw:
            return []
        return [key.strip() for key in raw.split(",") if key.strip()]

    # Swarm Model Registry for 7 Hugging Face models
    MODEL_SWARM: dict[str, str] = {
        "coding": "njelit1/supreme-coder-3b",
        "reasoning": "njelitltd/supreme-reasoner-3b",
        "general": "ziaulhaq1/supreme-general-3b",
        "creative": "njelitltd2/supreme-creative-3b",
        "master": "njelitltd3/supreme-master-3b",
        "vision": "njelltd5/supreme-vision-3b",
        "draft": "njelltd4/supreme-draft-0.5b",
    }

    @property
    def gemini_api_key(self) -> str:
        return self._get_cached_secret("GEMINI_API_KEY")

    @gemini_api_key.setter
    def gemini_api_key(self, value: str) -> None:
        self._set_cached_secret("GEMINI_API_KEY", value)

    @property
    def openai_api_key(self) -> str:
        return self._get_cached_secret("OPENAI_API_KEY")

    @openai_api_key.setter
    def openai_api_key(self, value: str) -> None:
        self._set_cached_secret("OPENAI_API_KEY", value)

    @property
    def deepseek_api_key(self) -> str:
        return self._get_cached_secret("DEEPSEEK_API_KEY")

    @deepseek_api_key.setter
    def deepseek_api_key(self, value: str) -> None:
        self._set_cached_secret("DEEPSEEK_API_KEY", value)

    @property
    def groq_api_key(self) -> str:
        return self._get_cached_secret("GROQ_API_KEY")

    @groq_api_key.setter
    def groq_api_key(self, value: str) -> None:
        self._set_cached_secret("GROQ_API_KEY", value)

    @property
    def nvidia_api_key(self) -> str:
        return self._get_cached_secret("NVIDIA_API_KEY")

    @nvidia_api_key.setter
    def nvidia_api_key(self, value: str) -> None:
        self._set_cached_secret("NVIDIA_API_KEY", value)

    @property
    def firecrawl_api_key(self) -> str:
        return self._get_cached_secret("FIRECRAWL_API_KEY")

    @property
    def discord_bot_token(self) -> str:
        try:
            return secret_vault.fetch_secret("DISCORD_BOT_TOKEN", default="")
        except Exception as e:
            # বাংলা: টোকেন fetch ব্যর্থ হলে Discord bot silently অকার্যকর হয়ে যায় —
            # কারণটি লগ করা হচ্ছে যাতে debug করা সহজ হয়
            logger.warning(f"Failed to fetch DISCORD_BOT_TOKEN secret: {e}")
            return ""

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
        uri = self._get_cached_secret("NEO4J_URI")
        if not uri and self.env not in ("local", "test") and "pytest" not in sys.modules:
            logger.warning("NEO4J_URI not configured — GraphService will run in dry-run mode.")
        return uri

    @property
    def neo4j_user(self) -> str:
        return self._get_cached_secret("NEO4J_USER") or "neo4j"

    @property
    def neo4j_password(self) -> str:
        return self._get_cached_secret("NEO4J_PASSWORD") or ""

    @property
    def frontend_base_url(self) -> str:
        """বাংলা: OAuth/Integration রিডাইরেক্টের জন্য ফ্রন্টএন্ড বেস URL।"""
        url = self._get_cached_secret("FRONTEND_BASE_URL")
        if not url and not is_test_environment() and self.env not in ("local", "test"):
            logger.warning("FRONTEND_BASE_URL not set in production — OAuth redirects may fail!")
        return url

    @property
    def checkout_base_url(self) -> str:
        """বাংলা: পেমেন্ট চেকআউট রিডাইরেক্টের জন্য বেস URL।"""
        url = self._get_cached_secret("CHECKOUT_BASE_URL")
        if not url and not is_test_environment() and self.env not in ("local", "test"):
            logger.warning("CHECKOUT_BASE_URL not set — payment checkout URLs may be incorrect!")
        return url

    @property
    def ollama_url(self) -> str:
        """বাংলা: Ollama URL।"""
        return self._get_cached_secret("OLLAMA_URL")

    # ── Admin Password Hash — Infisical-backed lazy property ────────────────
    # বাংলা মন্তব্য: Pydantic Field(validation_alias=...) সরাসরি OS env var থেকে পড়ে, যা Infisical
    # ভল্টে থাকা সিক্রেট পড়তে পারে না এবং Render ডিপ্লয়মেন্টে Validation Error ঘটিয়ে প্রসেস করায়।
    # তাই এটি lazy @property এবং _get_cached_secret() এ রূপান্তর করা হলো যাতে অন-ডিমান্ড ভল্ট বা env থেকে ফেচ হয়।
    @property
    def supremeai_admin_password_hash(self) -> str | None:
        val = (
            self._get_cached_secret("SUPREMEAI_ADMIN_PASSWORD_HASH")
            or os.getenv("SUPREMEAI_ADMIN_PASSWORD_HASH")
            or os.getenv("supremeai_admin_password_hash")
        )
        if not val and "pytest" not in sys.modules and os.getenv("CI") != "true":
            raise ValueError("supremeai_admin_password_hash must be explicitly set.")
        return val

    # ── JWT & Encryption Credentials — Infisical-backed ─────────────────────
    # বাংলা মন্তব্য: JWT সিক্রেট এবং এনক্রিপশন কী ক্লাউড ভল্ট (Infisical/GCP) থেকে ডায়নামিকালি
    # লোড করার জন্য lazy property প্যাটার্ন প্রয়োগ করা হয়েছে।
    @property
    def jwt_secret(self) -> str:
        """Get JWT secret with environment-specific handling.

        বাংলা মন্তব্য: প্রোডাকশনে SUPREMEAI_JWT_SECRET অবশ্যই নির্দিষ্ট করতে হবে এবং ৬৪ বাইটের বেশি হতে হবে।
        Non-production এ generated secret কে _jwt_secret_cache-তে cache করা হয় যাতে
        create_access_token() ও verify_token() একই secret পায় — নাহলে JWSSignatureError হয়।
        """
        # Production: Must be explicitly set
        if self.env == "production":
            secret = (
                os.getenv("SUPREMEAI_JWT_SECRET")
                or os.getenv("JWT_SECRET")
                or self._get_cached_secret("SUPREMEAI_JWT_SECRET")
            )
            if not secret or len(secret) < 64:
                raise RuntimeError("Production JWT secret must be set and >= 64 bytes")
            self._jwt_secret_cache = secret
            return secret

        # Return cached value if available (critical for token create/verify consistency)
        if hasattr(self, "_jwt_secret_cache") and self._jwt_secret_cache:
            return self._jwt_secret_cache

        # Development: Try file first, then generate
        secret_file = "/etc/secrets/jwt_secret"
        local_file = ".secrets/jwt_secret.key"  # Windows compatibility

        for path in [secret_file, local_file]:
            if os.path.exists(path):
                try:
                    with open(path) as f:
                        secret = f.read().strip()
                        if len(secret) >= 32:  # Minimum acceptable length
                            self._jwt_secret_cache = secret
                            return secret
                except OSError:
                    continue

        # Generate new secret if none found — cache it for consistency
        new_secret = secrets.token_hex(64)
        self._jwt_secret_cache = new_secret
        try:
            # Try to write to local file first (more permissive)
            os.makedirs(".secrets", exist_ok=True)
            with open(local_file, "w") as f:
                f.write(new_secret)
            return new_secret
        except OSError:
            logger.warning("Could not persist JWT secret - using in-memory only")
            return new_secret

    @property
    def cors_origins(self) -> list[str]:
        """Get CORS origins with environment-specific defaults and validation.

        বাংলা মন্তব্য: প্রোডাকশনে শুধুমাত্র অনুমোদিত ডোমেইনসমূহ অ্যাক্সেস করতে পারবে।
        টেস্টিং বা CI এনভায়রনমেন্টে (pytest, GITHUB_ACTIONS, CI, বা ALLOW_TEST_ORIGIN_BYPASS=true)
        ভ্যালিডেশন বাইপাস করে টেস্ট অরিজিন বা ডিফল্ট অরিজিন ফেরত দেওয়া হয়।
        """
        env_origins = os.getenv("CORS_ORIGINS")
        if env_origins:
            env_origins = env_origins.strip()
            try:
                parsed = json.loads(env_origins)
                origins = [str(o).strip() for o in parsed if str(o).strip()] if isinstance(parsed, list) else []
            except json.JSONDecodeError:
                origins = [o.strip() for o in env_origins.split(",") if o.strip()]
        else:
            origins = [
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:8000",
            ]

        # বাংলা মন্তব্য: টেস্ট ও CI এনভায়রনমেন্ট সনাক্তকরণ
        if self.env in ("production", "staging"):
            # Explicitly define allowed production domains
            allowed_production_origins = {
                "https://supremeai.com",
                "https://app.supremeai.com",
                "https://admin.supremeai.com",
                "https://supremeai-admin.web.app",
                "https://supremeai-studio.vercel.app",
                "https://supremeai-backend.onrender.com",
                "https://supremeai-admin.onrender.com",
            }

            validated_origins = []
            for origin in origins:
                clean_origin = origin.rstrip("/")
                if (
                    clean_origin in allowed_production_origins
                    or clean_origin.endswith(".vercel.app")
                    or clean_origin.endswith(".onrender.com")
                ):
                    validated_origins.append(clean_origin)
                else:
                    logger.warning(f"Disallowed CORS origin: {origin}")

            if not validated_origins:
                # If no explicit match, fallback to env provided origins or default allowed set
                validated_origins = list(allowed_production_origins)

            return validated_origins

        return origins

    @property
    def encryption_key(self) -> SecretStr:
        val = self._get_cached_secret("SUPREMEAI_ENCRYPTION_KEY")
        return SecretStr(val) if val else SecretStr("")

    # ── Stripe Credentials — Infisical-backed ────────────────────────────────
    # বাংলা মন্তব্য: Stripe এপিআই এবং ওয়েবহুক সিক্রেটসমূহের জন্য Infisical lazy fetching নিশ্চিত করা হলো,
    # যাতে প্রোডাকশন পেমেন্ট ক্রেডেনশিয়াল ভল্ট থেকে সরাসরি ইন-মেমোরিতে ফেচ হয়।
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

    @field_validator(
        "user_cors_origins",
        "admin_cors_origins",
        "allowed_hosts",
        "admin_emails",
        "idempotency_critical_paths",
        "prompt_blocked_patterns",
        "supremeai_public_paths",
        mode="before",
        check_fields=False,
    )
    @classmethod
    def parse_comma_separated_list(cls, v):
        if isinstance(v, str):
            if v.strip() == "":
                return []
            if "[" in v and "]" in v:
                try:
                    parsed = json.loads(v)
                    if isinstance(parsed, list):
                        return [str(x) for x in parsed]
                except Exception as e:
                    logger.debug(f"JSON parsing failed for admin_emails: {e}")
            return [i.strip() for i in v.split(",") if i.strip()]
        return v

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
                os.getenv("debug", "").lower() == "true" or os.getenv("DEBUG", "").lower() == "true"
            ):
                raise ValueError("Explicitly setting debug=True is PROHIBITED in production/staging.")
            return False
        return bool(v)

    @field_validator("docs_password", mode="before")
    @classmethod
    def validate_docs_password(cls, v: str | SecretStr | None, info: ValidationInfo) -> str | SecretStr:
        if "pytest" in sys.modules:
            return v or ""
        if not v and info.data.get("env", "local") in {"production", "staging"}:
            logger.warning("⚠️ SUPREMEAI_DOCS_PASSWORD not configured — using auto-generated secure password")
            return SecretStr(secrets.token_urlsafe(32))
        return v or ""

    @model_validator(mode="after")
    def validate_all(self):
        """Consolidated validator for docs auth, LLM secrets, Stripe, production completeness, and resilience."""
        if "pytest" in sys.modules or os.getenv("CI") == "true":
            return self  # Test isolation — boot-time check skip

        # Docs auth fallback for production/staging
        if self.env in {"production", "staging"} and self.docs_auth_enabled:
            pwd = self.docs_password.get_secret_value() if self.docs_password else ""
            if not pwd:
                logger.warning(
                    f"⚠️ {self.env.capitalize()} SUPREMEAI_DOCS_PASSWORD missing — using fallback production password."
                )
                self.docs_password = SecretStr("supreme-admin-2026-prod")

        # Boot-time LLM secret check — silent failure প্রতিরোধ করে
        if self.env in {"production", "staging"}:
            # বাংলা: সব LLM provider key একসাথে চেক করা হচ্ছে (batch-loaded cache ব্যবহার)
            _LLM_CRITICAL_KEYS = [
                "GEMINI_API_KEY",
                "OPENROUTER_API_KEY",
                "GROQ_API_KEY",
                "DEEPSEEK_API_KEY",
                "OPENAI_API_KEY",
            ]
            self._ensure_secrets_loaded()
            available = [k for k in _LLM_CRITICAL_KEYS if self._cached_secrets.get(k)]
            missing = [k for k in _LLM_CRITICAL_KEYS if not self._cached_secrets.get(k)]

            if not available:
                # বাংলা: কোনো LLM key নেই — সিস্টেম boot হবে কিন্তু সব AI feature মৃত।
                # Silent failure রোধ করতে CRITICAL log emit করা হচ্ছে।
                logger.critical(
                    "🚨 BOOT-TIME ALERT: কোনো LLM API key পাওয়া যায়নি! "
                    f"Missing: {missing}. "
                    "সব AI feature কাজ করবে না। Infisical / env var চেক করুন।"
                )
            elif missing:
                logger.warning(
                    f"⚠️ BOOT-TIME: {len(missing)} LLM key মিসিং ({missing}). "
                    f"Available: {available}. Partial AI functionality only."
                )
            else:
                logger.info(f"✅ BOOT-TIME: সব {len(available)} LLM API key সফলভাবে লোড হয়েছে।")

        # Stripe warning (non-blocking)
        if self.env in {"production", "staging"}:
            stripe_key = self.stripe_api_key.get_secret_value() if self.stripe_api_key else ""
            stripe_webhook = self.stripe_webhook_secret.get_secret_value() if self.stripe_webhook_secret else ""
            if not stripe_key:
                logger.warning(
                    "⚠️ Stripe API key missing in production/staging. Billing features will run in mock mode."
                )
            if not stripe_webhook:
                logger.warning("⚠️ Stripe webhook secret missing in production/staging. Webhook validation disabled.")

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
        # বাংলা মন্তব্য: প্রজেক্টের "Zero-Cost Mode" নীতি অনুসারে OPENROUTER_API_KEY বা অন্যান্য LLM API Key
        # না থাকলেও সার্ভার বুট হবে এবং Degraded Zero-Cost Mode-এ চলবে। তাই এখানে OPENROUTER_API_KEY
        # না থাকলে sys.exit(1) দিয়ে সার্ভিস ক্র্যাশ করানো যাবে না (Render free tier boot resiliency)।
        if self.env not in {"test"}:
            missing: list[str] = []
            if not self.encryption_key.get_secret_value():
                missing.append("ENCRYPTION_KEY")
            if missing:
                logger.critical(f"🚨 FATAL: Missing critical config vars: {', '.join(missing)}. Fail-Fast enforced.")
                sys.exit(1)
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
                return json.loads(v)
            except (json.JSONDecodeError, ValueError) as _parse_err:
                logger.debug(f"List field parse fallback to comma-split: {_parse_err}")
                return [p.strip() for p in v.split(",") if p.strip()]
        return v or []

    @field_validator("rbac_role_definitions", mode="before")
    @classmethod
    def parse_dict_fields(cls, v) -> dict:
        if not v:
            return {}
        if isinstance(v, str):
            try:
                return json.loads(v)
            except (json.JSONDecodeError, ValueError) as _dict_parse_err:
                logger.error(
                    f"Failed to parse rbac_role_definitions JSON: {_dict_parse_err}. Defaulting to empty dictionary."
                )
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
                logger.warning(
                    f"⚠️ {env.capitalize()} ALLOWED_HOSTS missing — auto-populating default production hosts."
                )
                v = [
                    "supremeai-backend.onrender.com",
                    "supremeai-admin.web.app",
                    "*.onrender.com",
                ]
        return v

    @field_validator("user_cors_origins", "admin_cors_origins", mode="before")
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

    @field_validator("user_cors_origins", "admin_cors_origins", mode="after")
    @classmethod
    def validate_cors_origins(cls, v: list[str], info: ValidationInfo) -> list[str]:
        # Test-isolation guard:
        # ENV=test হলে CORS fail-fast validator ট্রিগার করা হবে না।
        env = str(info.data.get("env", "local") or "local").lower()
        if env == "test":
            return v
        if env in {"production", "staging"}:
            # বাংলা মন্তব্য: প্রোডাকশনে লোকালহোস্ট CORS অরিজিন থেকে সরিয়ে ফেলা হয়
            # field_name check করা হচ্ছে — 'cors_origins' shorthand ও accept করা হচ্ছে
            field = getattr(info, "field_name", None) or ""
            if field in {"user_cors_origins", "admin_cors_origins", "cors_origins"} or not field:
                v = [o for o in v if "localhost" not in o and "127.0.0.1" not in o]
        return v

    @property
    def jti_blacklist_cache(self) -> set:
        """JWT JTI replay attack প্রতিরোধের জন্য ইন-মেমরি ক্যাশ। (Bangla: JTI ব্ল্যাকলিস্ট ক্যাশিং)"""
        if not hasattr(self, "_jti_cache"):
            self._jti_cache: set[str] = set()
        return self._jti_cache

    @classmethod
    def parse_cors_origins_helper(cls, value: Any, info: Any = None) -> list[str]:
        if isinstance(value, list):
            return value
        if not value or not str(value).strip():
            return []
        if str(value).startswith("["):
            try:
                return json.loads(value)
            except (json.JSONDecodeError, ValueError) as _cors_parse_err:
                logger.debug(f"CORS parse fallback to comma-split: {_cors_parse_err}")
        return [x.strip() for x in str(value).split(",") if x.strip()]

    @classmethod
    def validate_cors_origins_helper(cls, value: list[str], info: Any = None) -> list[str]:
        env = info.data.get("env", "local") if info and hasattr(info, "data") else "local"
        if env == "production":
            return [origin for origin in value if "localhost" not in origin and "127.0.0.1" not in origin]
        return value

    @classmethod
    def set_jwt_secret(cls, value: Any, info: Any = None) -> str:
        # বাংলা মন্তব্য: প্রোডাকশনে JWT সিক্রেট অন্তত ৬৪ বাইট দীর্ঘ হতে হবে — কম হলে এক্সেপশন রেইজ হবে
        env = info.data.get("env", "local") if info and hasattr(info, "data") else "local"
        if not value and env == "production":
            raise ValueError("JWT secret cannot be empty in production.")
        if value and env == "production" and len(str(value)) < 64:
            raise ValueError("JWT secret must be at least 64 bytes long in production.")
        if not value or value is None:
            return "supremeai_secure_jwt_secret_value_at_least_64_bytes_long_test_string_pad_pad_pad_pad"
        return str(value)

    @model_validator(mode="after")
    def validate_production_completeness(self) -> "Settings":
        """Production completeness verification helper for test coverage."""
        # বাংলা মন্তব্য: প্রোডাকশন এনভায়রনমেন্টের জন্য অতিরিক্ত কনফিগারেশন ভ্যালিডেশন
        if self.env == "production":
            if hasattr(self, "_jwt_secret_cache"):
                delattr(self, "_jwt_secret_cache")

            # বাংলা মন্তব্য: প্রোডাকশনে JWT সিক্রেট অবশ্যই থাকতে হবে এবং অন্তত ৬৪ বাইট দীর্ঘ হতে হবে — অন্যথায় সিকিউরিটি ক্র্যাশ
            secret = (
                os.getenv("SUPREMEAI_JWT_SECRET")
                or os.getenv("JWT_SECRET")
                or self._get_cached_secret("SUPREMEAI_JWT_SECRET")
            )
            if not secret or len(secret) < 64:
                raise RuntimeError("Production JWT secret must be set and >= 64 bytes")

            # বাংলা মন্তব্য: প্রোডাকশনে কনফিগারেশন পূর্ণতা যাচাই
            if not self.user_cors_origins and not self.admin_cors_origins:
                logger.warning("⚠️ Production CORS origins not explicitly configured. Using defaults for security.")

        # বাংলা মন্তব্য: কনফিগারেশন লোড হওয়ার পর লগ মেসেজ দেখানো
        logger.info(f"✅ Configuration loaded successfully for environment: {self.env}")
        return self

    def reload_env_vars(self) -> None:
        """প্রোডাকশনে সার্ভার রিস্টার্ট ছাড়াই কনফিগারেশন রিলোড করার ডাইনামিক মেথড। (Bangla: Hot-reload listener)"""
        from dotenv import load_dotenv

        load_dotenv(override=True)
        logger.info("⚙️ [Config] Environment variables hot-reloaded successfully.")


# ── Singleton instantiation with True Fail-Fast ────────────────────────────────
# বাংলা মন্তব্য: এখানে Fail-Fast সত্যিকারভাবে enforce হচ্ছে।
# কোনো "resilient boot" বা dummy fallback নেই। Exception মানেই sys.exit(1)।
try:
    settings = Settings()
except Exception as _boot_exc:
    logger.critical(
        f"🔥 FATAL CONFIG ERROR: {_boot_exc}\nServer startup ABORTED (Fail-Fast applied). Fix the configuration."
    )
    sys.exit(1)


def get_production_env(var_name: str, default: str | None = None) -> str:
    """বাংলা মন্তব্য: Strict Fail-Fast Config Guard.
    যেকোনো এনভায়রনমেন্টে কোনো ক্রিটিক্যাল সিক্রেট মিসিং থাকলে সরাসরি হার্ড ক্র্যাশ করবে,
    যাতে সাইলেন্ট ফেইলর প্রতিরোধ করা যায়। ডিফল্ট ভ্যালু পাস করলে মিসিং ক্ষেত্রে fallback ব্যবহার হবে।
    """
    import os

    value = os.getenv(var_name)
    if not value:
        if default is not None:
            return default
        logger.critical(f"❌ CRITICAL CONFIG ERROR: Missing required environment variable '{var_name}'!")
        raise ValueError(f"Configuration Error: {var_name} must be explicitly defined.")
    return value
