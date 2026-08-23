"""Secret loading and lazy-secret behavior for SupremeAI settings."""

import json
import os
import secrets
import sys
from typing import Any

from loguru import logger
from pydantic import PrivateAttr, SecretStr, model_serializer

def __is_test_environment() -> bool:
    if os.getenv("ENV", "").lower() in {"production", "staging"}:
        return False
    return "pytest" in sys.modules or os.getenv("CI") == "true" or os.getenv("GITHUB_ACTIONS") == "true"

from .security.secret_vault import secret_vault


class SettingsSecretsMixin:
    # বাংলা মন্তব্য: Pydantic v2-এ Mixin-এর ভেতরে PrivateAttr ব্যবহার করলে
    # instance-level private attr initialize নাও হতে পারে (ModelPrivateAttr iterable error)।
    # তাই নিরাপদ সমাধান হিসেবে __dict__-এ আলাদা namespace ব্যবহার করা হচ্ছে।
    _cached_secrets: dict[str, str] = PrivateAttr(default_factory=dict)
    _secrets_batch_loaded: bool = PrivateAttr(default=False)

    def _get_private_state(self) -> dict:
        """Mixin-এ নিরাপদ private state access।"""
        if "_cached_secrets" not in self.__dict__:
            self.__dict__["_cached_secrets"] = {}
        if "_secrets_batch_loaded" not in self.__dict__:
            self.__dict__["_secrets_batch_loaded"] = False
        return self.__dict__

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
        "SUPREMEAI_API_KEY",
        "NEO4J_URI",
        "NEO4J_USER",
        "NEO4J_PASSWORD",
        "SUPREMEAI_ADMIN_PASSWORD_HASH",
        "SUPREMEAI_JWT_SECRET",
        "ENCRYPTION_KEY",
        "STRIPE_API_KEY",
        "STRIPE_WEBHOOK_SECRET",
        "FIREBASE_SERVICE_ACCOUNT_JSON",
        "LANGFUSE_PUBLIC_KEY",
        "LANGFUSE_SECRET_KEY",
        "TELEGRAM_BOT_TOKEN",
        "ADMIN_TELEGRAM_CHAT_ID",
    ]

    def _ensure_secrets_loaded(self) -> None:
        """Batch-load all secrets at once into memory cache.

        বাংলা: startup-এ একবারে সব সিক্রেট লোড করে singleton dict-এ cache করে।
        এর ফলে প্রতিটি @property-র জন্য আলাদা vault কল হয় না, cold start latency কমে।
        বাংলা: default="" পাস করা হচ্ছে যাতে ঐচ্ছিক secrets (যেমন ADMIN_NOTIFICATION_EMAIL)
        production-এ missing থাকলেও server startup crash না করে।
        বাংলা: `_get_private_state()` ব্যবহার করা হচ্ছে — Pydantic v2 Mixin-এ PrivateAttr
        সরাসরি iterable না হওয়ায় `__dict__`-ভিত্তিক state নিশ্চিত করা হয়।
        """
        state = self._get_private_state()
        if state["_secrets_batch_loaded"]:
            return
        cached = state["_cached_secrets"]
        for secret_key in self._BATCH_SECRET_KEYS:
            try:
                # বাংলা: default="" দেওয়া হচ্ছে — এতে optional secrets missing থাকলে
                # RuntimeError throw হবে না, বরং empty string return হবে।
                # Critical secrets (JWT, encryption key) আলাদা validate_all validator-এ চেক হবে।
                val = secret_vault.fetch_secret(secret_key, default="")
                if val:
                    cached[secret_key] = val
            except Exception as _secret_err:
                # বাংলা: RuntimeError সহ সব exception gracefully handle করা হচ্ছে।
                # যদি কোনো optional secret missing থাকে, server startup block হবে না।
                logger.debug(f"Secret {secret_key} not available during batch load: {_secret_err}")
        state["_secrets_batch_loaded"] = True

    def _get_cached_secret(self, key: str) -> str:
        """Get cached secret with explicit empty vs not-found handling.

        English: Returns the cached secret value. Logs a warning if the requested
        secret key was never loaded into cache (not found in vault or env).
        Returns empty string as fallback to avoid crashes, but the caller should
        check for empty strings where critical.

        বাংলা মন্তব্য: ব্যাচ লোড করা ক্যাশ থেকে সিক্রেট রিটার্ন করে।
        প্রথম কলেই সব সিক্রেট লোড করা হয়, এরপর শুধু মেমোরি থেকে রিটার্ন।
        """
        self._ensure_secrets_loaded()
        cached = self._get_private_state()["_cached_secrets"]
        if key not in cached:
            if _is_test_environment():
                logger.debug(f"Secret '{key}' not found in cache after batch load - returning empty string")
            else:
                logger.warning(f"Secret '{key}' not found in cache after batch load - returning empty string")
        return cached.get(key, "")

    # ── Cloud-fetched secrets — GCP Secret Manager বা env fallback ───────────
    # বাংলা মন্তব্য: স্টার্টআপ টাইম কমাতে এবং Infisical ভল্ট থেকে একের পর এক সিক্রেট ফেচ করা এড়াতে
    # `@computed_field` এর জায়গায় অলস (lazy) `@property` ব্যবহার করা হলো। এর ফলে শুধুমাত্র
    # অন-ডিমান্ড অ্যাক্সেস করলেই সিক্রেট ফেচ হবে এবং গ্লোবাল ক্যাশে জমা থাকবে।
    @property
    def supabase_database_url(self) -> str:
        return self._get_cached_secret("SUPABASE_DATABASE_URL_POOLER")

    @property
    def neon_database_url(self) -> str:
        return self._get_cached_secret("NEON_DATABASE_URL")

    # বাংলা মন্তব্য: Anti-Hacking এবং OTP রাউটার সিক্রেটসমূহ (ঐচ্ছিক — মিসিং থাকলে সার্ভার ক্র্যাশ করবে না)
    @property
    def discord_otp_webhook_url(self) -> SecretStr | None:
        try:
            url = secret_vault.fetch_secret("DISCORD_OTP_WEBHOOK_URL", default="")
        except Exception:
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
        self._get_private_state()["_cached_secrets"][key] = str(value) if value is not None else ""

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
    def langfuse_public_key(self) -> str:
        return self._get_cached_secret("LANGFUSE_PUBLIC_KEY")

    @property
    def langfuse_secret_key(self) -> str:
        return self._get_cached_secret("LANGFUSE_SECRET_KEY")

    @property
    def discord_bot_token(self) -> str:
        try:
            return secret_vault.fetch_secret("DISCORD_BOT_TOKEN", default="")
        except Exception:
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

    @property
    def firebase_service_account_json(self) -> str:
        return self._get_cached_secret("FIREBASE_SERVICE_ACCOUNT_JSON")

    # ── System API Token — settings-এ মাইগ্রেট করা হলো ──────────────────────
    # বাংলা মন্তব্য: আগে auth_middleware.py সরাসরি os.getenv("SUPREMEAI_API_KEY") করত।
    # এখন এই computed field settings-এর Single Source of Truth।
    @property
    def supremeai_api_token(self) -> str:
        return self._get_cached_secret("SUPREMEAI_API_KEY")

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
        force_strict = os.getenv("STRICT_CORS_TEST", "").lower() in ("true", "1")
        is_test_or_ci = not force_strict and (
            "pytest" in sys.modules
            or os.getenv("CI", "").lower() in ("true", "1")
            or os.getenv("GITHUB_ACTIONS", "").lower() in ("true", "1")
            or os.getenv("ALLOW_TEST_ORIGIN_BYPASS", "").lower() in ("true", "1")
            or self.is_origin_bypass_allowed
            or self.env in ("test", "testing", "local")
        )

        if self.env in ("production", "staging"):
            # বাংলা মন্তব্য: পুরনো hardcoded domain allowlist সরানো হয়েছে।
            # render.yaml-এ operator যেসব domain সেট করেন (onrender.com, vercel.app, web.app)
            # সেগুলো আগের stale list-এর সাথে না মেলায় সব origin reject হতো → RuntimeError → crash।
            # এখন শুধু scheme (https://) validate করা হয় — operator-configured যেকোনো domain গ্রহণযোগ্য।
            validated_origins = []
            for origin in origins:
                if (
                    origin.startswith("https://")
                    or "localhost" in origin
                    or "127.0.0.1" in origin
                    or is_test_or_ci
                ):
                    validated_origins.append(origin)
                else:
                    logger.warning(f"Rejecting non-HTTPS CORS origin in production: {origin}")

            if not validated_origins:
                if is_test_or_ci:
                    return (
                        origins
                        if origins
                        else ["http://localhost:3000", "http://localhost:5173", "http://localhost:8000"]
                    )
                raise RuntimeError(
                    "No valid CORS origins provided. "
                    "Ensure CORS_ORIGINS env var contains https:// origins."
                )

            return validated_origins

        return origins

    @property
    def encryption_key(self) -> SecretStr:
        val = self._get_cached_secret("ENCRYPTION_KEY")
        if val:
            return SecretStr(val)

        # Fallback: Generate a valid Fernet key from any available LLM API key
        import base64
        import hashlib
        fallback_material = (
            self._get_cached_secret("GEMINI_API_KEY")
            or self._get_cached_secret("OPENROUTER_API_KEY")
            or self._get_cached_secret("GROQ_API_KEY")
            or self._get_cached_secret("DEEPSEEK_API_KEY")
            or self._get_cached_secret("OPENAI_API_KEY")
            or "supremeai-default-fallback-encryption-key-2026-v2"
        )
        if fallback_material:
            digest = hashlib.sha256(fallback_material.encode("utf-8")).digest()
            fernet_key = base64.urlsafe_b64encode(digest).decode("utf-8")
            return SecretStr(fernet_key)
        return SecretStr("")

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

    @property
    def telegram_bot_token(self) -> str:
        return self._get_cached_secret("TELEGRAM_BOT_TOKEN") or os.getenv("TELEGRAM_BOT_TOKEN", "")

    @property
    def admin_telegram_chat_id(self) -> str:
        return self._get_cached_secret("ADMIN_TELEGRAM_CHAT_ID") or os.getenv("ADMIN_TELEGRAM_CHAT_ID", "")

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
        result["firebase_service_account_json"] = redacted
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
            "langfuse_public_key",
            "langfuse_secret_key",
        ]:
            result[key_field] = redacted
        # Include non-secret properties
        result["neo4j_uri"] = self.neo4j_uri
        result["neo4j_user"] = self.neo4j_user
        result["admin_notification_email"] = self.admin_notification_email
        return result
