import os

from loguru import logger


try:
    from infisical_client import AuthenticationOptions
    from infisical_client import ClientSettings
    from infisical_client import GetSecretOptions
    from infisical_client import InfisicalClient
    from infisical_client import UniversalAuthMethod
except ImportError:
    InfisicalClient = None


class ProductionSecretVault:
    """
    Enterprise Cloud Secret Vault (Infisical / Doppler).
    Fetches production API keys directly into memory from Infisical.
    Removes the need for monolithic GCP Secret Manager.
    """

    def __init__(self):
        self.env = os.getenv("ENV", "local").lower()
        self.project_id = os.getenv("INFISICAL_PROJECT_ID")
        self.client_id = os.getenv("INFISICAL_CLIENT_ID")
        self.client_secret = os.getenv("INFISICAL_CLIENT_SECRET")
        self.token = os.getenv("INFISICAL_TOKEN")

        self.client = None
        self._cached_secrets: dict[str, str] = {}
        logger.info("⚙️ Secure Local In-Memory Secret Cache Layer Initialized.")

        if InfisicalClient and (self.token or (self.client_id and self.client_secret)):
            try:
                # If using Universal Auth (Machine Identity Client ID + Secret)
                if self.client_id and self.client_secret:
                    self.client = InfisicalClient(
                        ClientSettings(
                            auth=AuthenticationOptions(universal_auth=UniversalAuthMethod(client_id=self.client_id, client_secret=self.client_secret))
                        )
                    )
                    logger.info("🔒 Production Secret Vault hooked into Infisical via Machine Identity")
                # If using legacy or single Service Token
                elif self.token:
                    # Some older Infisical SDKs support token initialization
                    self.client = InfisicalClient(ClientSettings(access_token=self.token))
                    logger.info("🔒 Production Secret Vault hooked into Infisical via Token")
            except Exception as e:  # noqa: BLE001
                logger.warning(f"Failed to bind Infisical Client: {str(e)}. Falling back to raw env.")
        else:
            logger.info("⚙️ Infisical missing or no credentials found. Bypassing Cloud Vault.")

    def fetch_secret(self, secret_id: str, default: str = None) -> str:
        """Infisical থেকে রিয়াল-টাইমে সিক্রেট ভ্যালু রিড করার মেকানিজম"""
        if secret_id in self._cached_secrets:
            return self._cached_secrets[secret_id]

        if not self.client or not self.project_id:
            return self._fallback_to_env(secret_id, default)

        try:
            # Fetch from Infisical Project
            options = GetSecretOptions(
                environment=self.env if self.env in ["production", "staging", "development"] else "development",
                project_id=self.project_id,
                secret_name=secret_id,
            )
            secret_value = self.client.getSecret(options=options).secret_value

            # Write-back to cache
            self._cached_secrets[secret_id] = secret_value
            return secret_value
        except Exception as e:  # noqa: BLE001
            logger.warning(f"⚠️ Unable to reach Infisical for {secret_id}: {e}. Using fallback environment.")
            return self._fallback_to_env(secret_id, default)

    def _fallback_to_env(self, secret_id: str, default: str | None) -> str:
        """Fallback to environment variable. In production, this is disallowed."""
        env_fallback = os.getenv(secret_id, default)
        if env_fallback is None:
            if self.env in ("test", "testing", "ci", "local"):
                logger.warning(f"⚠️ Mocking missing secret '{secret_id}' for {self.env} environment.")
                env_fallback = f"mock_{secret_id}"
            else:
                raise RuntimeError(f"Secret '{secret_id}' not found in Infisical and no env fallback provided! Fail-closed triggered.")
        self._cached_secrets[secret_id] = env_fallback
        return env_fallback

    async def fetch_secret_async(self, secret_id: str) -> str:
        """অ্যাসিঙ্ক ইভেন্ট লুপ ব্লক না করে সিক্রেট ফেচ করার মেথড"""
        import asyncio

        return await asyncio.to_thread(self.fetch_secret, secret_id)


# Global Vault Singleton Instance
_secret_vault_instance: ProductionSecretVault | None = None


def get_secret_vault() -> ProductionSecretVault:
    global _secret_vault_instance
    if _secret_vault_instance is None:
        _secret_vault_instance = ProductionSecretVault()
    return _secret_vault_instance


secret_vault = get_secret_vault()
