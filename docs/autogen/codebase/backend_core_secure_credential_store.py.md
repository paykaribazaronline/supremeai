# 📄 ফাইল: backend/core/secure_credential_store.py

**প্রকার:** .py  
**সাইজ:** 5,428 বাইট  
**আপডেট:** 2026-07-08T01:31:17.987733

---

## কোড

```py
from __future__ import annotations

import base64
import os
from abc import ABC
from abc import abstractmethod
from typing import Any

from loguru import logger

from core.config import settings


try:
    from cryptography.fernet import Fernet
    CRYPTO_AVAILABLE = True
except ImportError:  # pragma: no cover
    CRYPTO_AVAILABLE = False


def generate_key() -> str:
    if not CRYPTO_AVAILABLE:
        raise RuntimeError("cryptography package is required for key generation")
    return Fernet.generate_key().decode()


class EncryptionProvider(ABC):
    @abstractmethod
    def encrypt(self, plaintext: str) -> tuple[str, str | None]:
        """Returns (ciphertext, key_ref)"""
        pass

    @abstractmethod
    def decrypt(self, ciphertext: str, key_ref: str | None) -> str:
        """Returns plaintext"""
        pass


class LocalFernetProvider(EncryptionProvider):
    def __init__(self, encryption_key: str | None = None) -> None:
        self.enabled = False
        self.fernet: Fernet | None = None
        if CRYPTO_AVAILABLE:
            raw_key = encryption_key or os.getenv("SUPREMEAI_CREDENTIAL_ENC_KEY", "")
            if raw_key:
                try:
                    self.fernet = Fernet(raw_key.encode())
                    self.enabled = True
                except Exception as exc:  # noqa: BLE001
                    logger.warning(f"Invalid credential encryption key: {exc}")
        if not self.enabled:
            logger.warning("Credential encryption is disabled. Credentials will be stored as plaintext.")

    def encrypt(self, plaintext: str) -> tuple[str, str | None]:
        if not self.enabled or self.fernet is None:
            return plaintext, "local:plaintext"
        try:
            token = self.fernet.encrypt(plaintext.encode()).decode()
            return token, "local:fernet"
        except Exception as exc:  # noqa: BLE001
            logger.error(f"LocalFernetProvider encryption failed: {exc}")
            return plaintext, "local:plaintext"

    def decrypt(self, ciphertext: str, key_ref: str | None) -> str:
        if not self.enabled or self.fernet is None or key_ref == "local:plaintext":
            return ciphertext
        try:
            return self.fernet.decrypt(ciphertext.encode()).decode()
        except Exception as exc:  # noqa: BLE001
            logger.error(f"LocalFernetProvider decryption failed: {exc}")
            return ciphertext


class CloudKMSProvider(EncryptionProvider):
    def __init__(self):
        from google.cloud import kms
        self.client = kms.KeyManagementServiceClient()
        self.key_name = os.environ.get("GCP_KMS_KEY_NAME")
        if not self.key_name:
            logger.warning("GCP_KMS_KEY_NAME is not set. CloudKMSProvider might fail if called.")
        logger.info("Initialized CloudKMSProvider for envelope encryption.")

    def encrypt(self, plaintext: str) -> tuple[str, str | None]:
        if not self.key_name:
            raise ValueError("GCP_KMS_KEY_NAME must be set for Cloud KMS encryption.")
        response = self.client.encrypt(
            request={"name": self.key_name, "plaintext": plaintext.encode()}
        )
        return base64.b64encode(response.ciphertext).decode(), self.key_name

    def decrypt(self, ciphertext: str, key_ref: str | None) -> str:
        if not self.key_name:
            raise ValueError("GCP_KMS_KEY_NAME must be set for Cloud KMS decryption.")
        response = self.client.decrypt(
            request={"name": self.key_name, "ciphertext": base64.b64decode(ciphertext)}
        )
        return response.plaintext.decode()


def get_encryption_provider() -> EncryptionProvider:
    # Use config environment to route to the correct provider
    env = getattr(settings, "env", "local")
    if env == "production":
        return CloudKMSProvider()
    return LocalFernetProvider()


class SecureCredentialStore:
    def __init__(self) -> None:
        self.provider = get_encryption_provider()

    def encrypt(self, payload: dict[str, Any]) -> dict[str, Any]:
        try:
            plaintext = __import__("json").dumps(payload, default=str)
            ciphertext, key_ref = self.provider.encrypt(plaintext)
            return {"__enc__": True, "payload": ciphertext, "key_ref": key_ref}
        except Exception as exc:  # noqa: BLE001
            logger.error(f"Credential encryption failed: {exc}")
            return payload

    def decrypt(self, payload: dict[str, Any]) -> dict[str, Any]:
        if not payload.get("__enc__"):
            return payload
        try:
            ciphertext = payload.get("payload", "")
            key_ref = payload.get("key_ref")
            plaintext = self.provider.decrypt(ciphertext, key_ref)
            return __import__("json").loads(plaintext)
        except Exception as exc:  # noqa: BLE001
            logger.error(f"Credential decryption failed: {exc}")
            return payload

    def mask(self, payload: dict[str, Any]) -> dict[str, Any]:
        masked = dict(payload)
        for field in ("password", "secret", "token"):
            if field in masked and masked[field]:
                val_str = str(masked[field])
                # Mask string methods to output ••••••••••{last_4_hash}
                last_4 = val_str[-4:] if len(val_str) >= 4 else val_str
                masked[field] = f"••••••••••{last_4}"
        return masked


```