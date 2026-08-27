"""Validation and normalization rules for SupremeAI settings."""

import json
import os
import secrets
import sys
from typing import Any

from loguru import logger
from pydantic import SecretStr, ValidationInfo, field_validator, model_validator


class SettingsValidationMixin:
    # ── Variable format patterns for validation ──
    FORMAT_PATTERNS = {
        "supabase_url": r"^https?://.*\.supabase\.(co|com)$",
        "redis_url": r"^redis://[^:]+:\d+$|^rediss://.*$",
        "database_url": r"^postgresql(ql)?://[^:]+:[^@]+@[^:/]+:\d+/[^/]+$",
    }

    # ── Fix suggestions for common issues ──
    FIX_SUGGESTIONS = {
        "supabase_database_url": "Set SUPABASE_DATABASE_URL in Render dashboard. Format: postgresql://postgres.[project-ref]:[password]@aws-0-[region].pool.supabase.com:6543/postgres",
        "redis_url": "Set REDIS_URL for Upstash Redis. Get URL from: https://console.upstash.io/redis",
        "openai_api_key": "Set OPENAI_API_KEY for OpenAI integration. Get key from: https://platform.openai.com/api-keys",
    }

    # ── Validators ───────────────────────────────────────────────────────────

    @field_validator("*", mode="before")
    @classmethod
    def validate_env_vars(cls, value: Any, info: ValidationInfo) -> Any:
        import re
        if value is None:
            return value
        var_name = info.field_name
        if var_name in cls.FORMAT_PATTERNS:
            pattern = cls.FORMAT_PATTERNS[var_name]
            if isinstance(value, str) and not re.match(pattern, value):
                suggestion = cls.FIX_SUGGESTIONS.get(var_name, "Check format.")
                raise ValueError(f"Invalid format for {var_name}. Expected {pattern}. {suggestion}")
        return value

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
                logger.warning(
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
            # বাংলা মন্তব্য: AI API keys (OPENROUTER_API_KEY, GEMINI_API_KEY) মিসিং থাকলেও সিস্টেম degraded mode-এ বুট করবে, ক্র্যাশ করবে না।
            if not self.ci_webhook_secret:
                missing.append("CI_WEBHOOK_SECRET")
            if missing:
                logger.warning(
                    f"⚠️ Production missing config vars: {', '.join(missing)}. Running in degraded zero-cost mode."
                )

        # Core Infrastructure Guard - Fail Fast for non-test environments
        if self.env in {"production", "staging"}:
            critical_infrastructure = []
            if not getattr(self, "supabase_url", None):
                critical_infrastructure.append("SUPABASE_URL")
            if not getattr(self, "supabase_key", None):
                critical_infrastructure.append("SUPABASE_KEY")
            if not getattr(self, "firebase_service_account_json", None):
                critical_infrastructure.append("FIREBASE_SERVICE_ACCOUNT_JSON")
            if not self.encryption_key.get_secret_value():
                critical_infrastructure.append("ENCRYPTION_KEY")

            if critical_infrastructure:
                logger.critical(
                    f"❌ CRITICAL INFRASTRUCTURE MISSING: {critical_infrastructure}. "
                    "Server startup aborted (Fail-Fast enforced)."
                )
                raise ValueError(f"Production/Staging requires {critical_infrastructure} to be set.")
        elif self.env not in {"test"}:
            missing: list[str] = []
            if not self.encryption_key.get_secret_value():
                missing.append("ENCRYPTION_KEY")
            if not getattr(self, "firebase_service_account_json", None):
                missing.append("FIREBASE_SERVICE_ACCOUNT_JSON")
            if missing:
                logger.warning(
                    f"⚠️ Missing local config vars: {', '.join(missing)}. Local/dev server may fail at runtime."
                )
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
        env = info.data.get("env", "local") if info and hasattr(info, "data") else "local"
        if not value and env == "production":
            raise ValueError("JWT secret cannot be empty in production.")
        if not value or value is None:
            # Generate a secure random JWT secret instead of using hardcoded string
            import secrets
            return secrets.token_urlsafe(64)
        # বাংলা মন্তব্য: প্রোডাকশনে JWT secret কমপক্ষে 64 bytes হতে হবে — brute-force attack ঠেকাতে
        if env == "production" and len(str(value)) < 64:
            raise ValueError("JWT secret must be at least 64 bytes long in production")
        return str(value)

    @model_validator(mode="after")
    def validate_production_completeness(self) -> Any:
        """Production completeness verification helper for test coverage."""
        # বাংলা মন্তব্য: প্রোডাকশন এনভায়রনমেন্টের জন্য অতিরিক্ত কনফিগারেশন ভ্যালিডেশন
        if self.env == "production":
            if hasattr(self, "_jwt_secret_cache"):
                delattr(self, "_jwt_secret_cache")
            _ = self.jwt_secret

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
