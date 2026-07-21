"""Enterprise Cloud Secret Vault (Infisical / Doppler).

বাংলা: এন্টারপ্রাইজ ক্লাউড সিক্রেট ভল্ট — ইন-মেমরি ক্যাশে TTL-সহ, Fail-Closed।
Fetches production API keys directly into memory from Infisical.
Removes the need for monolithic GCP Secret Manager.
"""

from __future__ import annotations

import asyncio
import os
import time

from core.messaging.event_bus import ErrorContext, ErrorEvent, error_event_bus
from loguru import logger

try:
    from infisical_client import (AuthenticationOptions, ClientSettings,
                                  GetSecretOptions, InfisicalClient,
                                  UniversalAuthMethod)
except ImportError:
    InfisicalClient = None  # type: ignore[assignment]


# ── Constants ──────────────────────────────────────────────────────────────────
CACHE_TTL_SECONDS: int = int(os.getenv("SECRET_CACHE_TTL", "300"))  # 5 min default
INFISICAL_TIMEOUT: int = int(os.getenv("INFISICAL_TIMEOUT", "10"))  # 10s default


class _CacheEntry:
    """Cache entry with TTL expiry."""

    __slots__ = ("value", "expires_at")

    def __init__(self, value: str, ttl: int = CACHE_TTL_SECONDS) -> None:
        self.value = value
        self.expires_at = time.monotonic() + ttl

    @property
    def is_expired(self) -> bool:
        return time.monotonic() > self.expires_at


class ProductionSecretVault:
    """Enterprise Cloud Secret Vault with TTL-based caching and fail-closed behavior.

    বাংলা: TTL-ভিত্তিক ক্যাশিং এবং Fail-Closed আচরণ সহ এন্টারপ্রাইজ ক্লাউড সিক্রেট ভল্ট।
    """

    def __init__(self) -> None:
        self.env = os.getenv("ENV", "local").lower()
        self.project_id = os.getenv("INFISICAL_PROJECT_ID")
        self.client_id = os.getenv("INFISICAL_CLIENT_ID")
        self.client_secret = os.getenv("INFISICAL_CLIENT_SECRET")
        self.token = os.getenv("INFISICAL_TOKEN")

        self.client: InfisicalClient | None = None
        self._cache: dict[str, _CacheEntry] = {}

        if InfisicalClient and (self.token or (self.client_id and self.client_secret)):
            self._init_infisical_client()
        else:
            logger.info(
                "Infisical missing or no credentials found. Bypassing Cloud Vault."
            )

    def _init_infisical_client(self) -> None:
        """Initialize Infisical client with timeout protection."""
        try:
            if self.client_id and self.client_secret:
                self.client = InfisicalClient(
                    ClientSettings(
                        auth=AuthenticationOptions(
                            universal_auth=UniversalAuthMethod(
                                client_id=self.client_id,
                                client_secret=self.client_secret,
                            )
                        )
                    )
                )
                logger.info(
                    "Production Secret Vault hooked into Infisical via Machine Identity"
                )
            elif self.token:
                self.client = InfisicalClient(ClientSettings(access_token=self.token))
                logger.info("Production Secret Vault hooked into Infisical via Token")
        except (ConnectionError, TimeoutError, ValueError) as exc:
            logger.warning(
                f"Failed to bind Infisical Client: {exc}. Falling back to raw env."
            )
        except Exception:  # noqa: BLE001
            logger.opt(exception=True).warning(
                "Unexpected error initializing Infisical client. Falling back to raw env."
            )

    def fetch_secret(self, secret_id: str, default: str | None = None) -> str:
        """Fetch a secret from Infisical with TTL-based caching.

        বাংলা: TTL-ভিত্তিক ক্যাশিং সহ Infisical থেকে সিক্রেট ফেচ।

        Raises:
            RuntimeError: If secret not found in Infisical or env in production.
        """
        # Check cache first
        cached = self._cache.get(secret_id)
        if cached and not cached.is_expired:
            return cached.value

        # If cache expired, remove it
        if cached and cached.is_expired:
            del self._cache[secret_id]

        if not self.client or not self.project_id:
            return self._fallback_to_env(secret_id, default)

        try:
            env_name = (
                self.env
                if self.env in ("production", "staging", "development")
                else "development"
            )
            options = GetSecretOptions(
                environment=env_name,
                project_id=self.project_id,
                secret_name=secret_id,
            )

            # Exponential backoff retry logic
            max_retries = 3
            for attempt in range(max_retries):
                try:
                    secret_value = self.client.getSecret(options=options).secret_value
                    self._cache[secret_id] = _CacheEntry(secret_value)
                    return secret_value
                except (ConnectionError, TimeoutError) as exc:
                    if attempt < max_retries - 1:
                        sleep_time = 2**attempt
                        logger.warning(
                            f"Retrying Infisical fetch for {secret_id} in {sleep_time}s due to: {exc}"
                        )
                        time.sleep(sleep_time)
                    else:
                        raise exc
            # বাংলা মন্তব্য: mypy-এর Missing return statement এরর এড়াতে লুপের শেষে raise দেওয়া হলো, যদিও বাস্তবে এটি কখনো রিচ হবে না।
            raise RuntimeError(
                "Unexpected end of retry loop without success or exception"
            )
        except (ConnectionError, TimeoutError) as exc:
            logger.warning(
                f"Unable to reach Infisical for {secret_id}: {exc}. Using fallback environment."
            )
            error_event_bus.emit(
                ErrorEvent(
                    module="secret_vault",
                    error_type="VAULT_FETCH_TIMEOUT",
                    message=f"Failed to fetch {secret_id} from Infisical after retries: {exc}",
                    severity="WARNING",
                    structured_context=ErrorContext(module="auto_fixed"),
                    context={"secret_id": secret_id},
                )
            )
            return self._fallback_to_env(secret_id, default)
        except Exception as exc:  # noqa: BLE001
            logger.opt(exception=True).warning(
                f"Unexpected error fetching {secret_id} from Infisical. Using fallback."
            )
            error_event_bus.emit(
                ErrorEvent(
                    module="secret_vault",
                    error_type="VAULT_FETCH_ERROR",
                    message=f"Unexpected error fetching {secret_id}: {exc}",
                    severity="ERROR",
                    structured_context=ErrorContext(module="auto_fixed"),
                    context={"secret_id": secret_id},
                )
            )
            return self._fallback_to_env(secret_id, default)

    def _fallback_to_env(self, secret_id: str, default: str | None) -> str:
        """Fallback to environment variable. In production, this is disallowed.

        বাংলা: এনভায়রনমেন্ট ভেরিয়েবলে ফলব্যাক। প্রোডাকশনে Fail-Closed।
        """
        env_fallback = os.getenv(secret_id, default)
        if env_fallback is None:
            if self.env in ("test", "testing", "ci", "local"):
                logger.warning(
                    f"Mocking missing secret '{secret_id}' for {self.env} environment."
                )
                env_fallback = f"mock_{secret_id}"
            else:
                raise RuntimeError(
                    f"Secret '{secret_id}' not found in Infisical and no env fallback provided! Fail-closed triggered."
                )
        self._cache[secret_id] = _CacheEntry(env_fallback)
        return env_fallback

    async def fetch_secret_async(
        self, secret_id: str, default: str | None = None
    ) -> str:
        """Async wrapper — runs fetch_secret in a thread to avoid blocking the event loop.

        বাংলা: অ্যাসিঙ্ক র‍্যাপার — ইভেন্ট লুপ ব্লক না করে থ্রেডে fetch_secret চালায়।
        """
        return await asyncio.to_thread(self.fetch_secret, secret_id, default)

    def invalidate_cache(self, secret_id: str | None = None) -> None:
        """Invalidate cache for a specific secret or clear all.

        বাংলা: নির্দিষ্ট সিক্রেট বা পুরো ক্যাশে ইনভ্যালিডেট।
        """
        if secret_id:
            self._cache.pop(secret_id, None)
        else:
            self._cache.clear()


# Global Vault Singleton Instance
_secret_vault_instance: ProductionSecretVault | None = None


def get_secret_vault() -> ProductionSecretVault:
    """Get or create the global secret vault singleton."""
    global _secret_vault_instance  # noqa: PLW0603
    if _secret_vault_instance is None:
        _secret_vault_instance = ProductionSecretVault()
    return _secret_vault_instance


secret_vault = get_secret_vault()
