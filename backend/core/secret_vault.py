import os

from loguru import logger


try:
    from google.cloud import secretmanager
except ImportError:
    secretmanager = None


class ProductionSecretVault:
    """
    Enterprise Cloud Secret Vault.
    Fetches production API keys and database strings directly into memory from Google Secret Manager.
    Removes the need for plaintext .env files in cloud instances.
    """

    def __init__(self):
        self.project_id = os.getenv("GCP_PROJECT_ID")
        self.env = os.getenv("ENV", "local").lower()
        self.client = None
        # 🔒 বাংলা মন্তব্য: Secret Manager-এর $2.39 বিলিং স্পাইক বন্ধ করতে ইন-মেমোরি বাফার লেয়ার
        self._cached_secrets: dict[str, str] = {}
        logger.info("⚙️ Secure Local In-Memory Secret Cache Layer Initialized.")

        if secretmanager and self.env == "production":
            try:
                # Cloud Run-এর ডিফল্ট সার্ভিস অ্যাকাউন্ট অটোমেটিক্যালি অথোরাইজড হবে
                self.client = secretmanager.SecretManagerServiceClient()
                logger.info(f"🔒 Production Secret Vault hooked into GCP Project: {self.project_id}")
            except Exception as e:  # noqa: BLE001
                logger.warning(f"Failed to bind Secret Manager Service Client: {str(e)}. Falling back to raw env.")
        else:
            logger.info("⚙️ Local/Dev mode active or library missing. Bypassing Google Secret Manager.")

    def fetch_secret(self, secret_id: str, default: str = None) -> str:
        """গুগল সিক্রেট ম্যানেজার থেকে রিয়াল-টাইমে সিক্রেট ভ্যালু রিড করার মেকানিজম"""
        # ১. হট-লুপ চেকার: মেমোরিতে থাকলে সরাসরি রিটার্ন (Zero API Calls)
        if secret_id in self._cached_secrets:
            return self._cached_secrets[secret_id]

        # লোকাল মোড বা ক্লাউড রান এনভায়রনমেন্ট ভ্যারিয়েবল ব্যাকআপ চেক
        env_fallback = os.getenv(secret_id, default)
        if env_fallback:
            self._cached_secrets[secret_id] = env_fallback
            return env_fallback

        if not self.client or not self.project_id:
            if self.env == "production":
                raise RuntimeError(f"Secret {secret_id} not found and no local fallback allowed in production!")
            return default if default is not None else ""

        try:
            # GCP Secret Manager Standard Resource Path
            name = f"projects/{self.project_id}/secrets/{secret_id}/versions/latest"
            response = self.client.access_secret_version(request={"name": name})
            payload = response.payload.data.decode("UTF-8")
            secret_value = payload.strip()
            # ক্যাশ মেমরিতে রাইট ব্যাক (Write-back to cache)
            self._cached_secrets[secret_id] = secret_value
            return secret_value
        except Exception as e:  # noqa: BLE001
            logger.warning(f"⚠️ Unable to reach GCP Secret Manager for {secret_id}: {e}. Using fallback environment.")
            if self.env == "production":
                raise RuntimeError(f"Failed to fetch {secret_id} in production: {e}") from e
            return default if default is not None else ""

    async def fetch_secret_async(self, secret_id: str) -> str:
        """অ্যাসিঙ্ক ইভেন্ট লুপ ব্লক না করে সিক্রেট ফেচ করার মেথড"""
        import asyncio

        return await asyncio.to_thread(self.fetch_secret, secret_id)


# Global Vault Singleton Instance
# বাংলা মন্তব্য: P2 Fix — module loading-এর সময় synchronous GSM calls এড়াতে lazy initialization প্রয়োগ করা হলো।
_secret_vault_instance: ProductionSecretVault | None = None


def get_secret_vault() -> ProductionSecretVault:
    global _secret_vault_instance
    if _secret_vault_instance is None:
        _secret_vault_instance = ProductionSecretVault()
    return _secret_vault_instance


secret_vault = get_secret_vault()
