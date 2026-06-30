# Encryption and GCP Service Account Manager
# বাংলা মন্তব্য: GCP ক্রেডেনশিয়াল ম্যানেজমেন্ট ও সেফ-স্টোরেজ এনক্রিপশন ডিক্রিপশন মডিউল।

import os
import json
from cryptography.fernet import Fernet
from loguru import logger
from google.oauth2 import service_account

# Global encryption key management
# বাংলা মন্তব্য: Fernet এনক্রিপশনের জন্য ৩২ বাইটের কী জেনারেট বা লোড করা হচ্ছে
_KEY = os.getenv("SUPREMEAI_ENCRYPTION_KEY")
if not _KEY:
    # Fallback key for testing/local
    _KEY = Fernet.generate_key().decode()

cipher = Fernet(_KEY.encode())


class CloudStatus:
    def __init__(self, connected: bool = False, provider: str = "gcp", region: str | None = None):
        self.connected = connected
        self.provider = provider
        self.region = region


class CloudResource:
    def __init__(self, id: str, type: str, name: str, status: str):
        self.id = id
        self.type = type
        self.name = name
        self.status = status


class GCPCredentialManager:
    """
    Encrypts, decrypts, and validates Google Cloud Service Account JSON credentials.
    """
    @staticmethod
    def encrypt_credentials(sa_dict: dict) -> bytes:
        # বাংলা মন্তব্য: সার্ভিস অ্যাকাউন্ট ডিকশনারি এনক্রিপ্ট করে সিকিউরড বাইটসে কনভার্ট করা হচ্ছে
        data_str = json.dumps(sa_dict)
        return cipher.encrypt(data_str.encode())

    @staticmethod
    def decrypt_credentials(encrypted_data: bytes) -> dict:
        # বাংলা মন্তব্য: এনক্রিপ্ট করা বাইটস থেকে পুনরায় ডিক্রিপ্ট করা হচ্ছে
        decrypted_str = cipher.decrypt(encrypted_data).decode()
        return json.loads(decrypted_str)

    @staticmethod
    def validate_service_account(sa_dict: dict) -> bool:
        # বাংলা মন্তব্য: সার্ভিস অ্যাকাউন্ট কী সঠিক কিনা তা google-auth লাইব্রেরি দিয়ে পরীক্ষা করা হচ্ছে
        try:
            service_account.Credentials.from_service_account_info(sa_dict)
            return True
        except Exception as e:
            logger.error(f"GCP Service Account validation failed: {e}")
            return False


async def ping() -> CloudStatus:
    # Default ping fallback behavior
    return CloudStatus(connected=False, provider="gcp")


async def list_resources() -> list[CloudResource]:
    return []
