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

        if secretmanager and self.env == "production":
            try:
                # Cloud Run-এর ডিফল্ট সার্ভিস অ্যাকাউন্ট অটোমেটিক্যালি অথোরাইজড হবে
                self.client = secretmanager.SecretManagerServiceClient()
                logger.info(f"🔒 Production Secret Vault hooked into GCP Project: {self.project_id}")
            except Exception as e:
                logger.warning(f"Failed to bind Secret Manager Service Client: {str(e)}. Falling back to raw env.")
        else:
            logger.info("⚙️ Local/Dev mode active or library missing. Bypassing Google Secret Manager.")

    def fetch_secret(self, secret_id: str, default_fallback: str = "") -> str:
        """গুগল সিক্রেট ম্যানেজার থেকে রিয়াল-টাইমে সিক্রেট ভ্যালু রিড করার মেকানিজম"""
        # লোকাল মোড বা ক্লাউড রান এনভায়রনমেন্ট ভ্যারিয়েবল ব্যাকআপ চেক
        env_fallback = os.getenv(secret_id)
        if env_fallback:
            return env_fallback

        if not self.client or not self.project_id:
            return default_fallback

        try:
            # GCP Secret Manager Standard Resource Path
            name = f"projects/{self.project_id}/secrets/{secret_id}/versions/latest"
            response = self.client.access_secret_version(request={"name": name})
            payload = response.payload.data.decode("UTF-8")
            return payload.strip()
        except Exception as e:
            logger.error(f"❌ Failed to fetch secret [{secret_id}] from GSM: {str(e)}. Using fallback.")
            return default_fallback

# Global Vault Singleton Instance
secret_vault = ProductionSecretVault()
