# 📄 ফাইল: backend/core/secure_credential_store.py

**প্রকার:** .py  
**সাইজ:** 5,176 বাইট  
**আপডেট:** 2026-07-05T19:04:56.644472

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
                except Exception as exc:
                    logger.warning(f"Invalid credential encryption key: {exc}")
        if not self.enabled:
            logger.warning("Credential encryption is disabled. Credentials will be stored as plaintext.")

    def encrypt(self, plaintext: str) -> tuple[str, str | None]:
        if not self.enabled or self.fernet is None:
            return plaintext, "local:plaintext"
        try:
            token = self.fernet.encrypt(plaintext.encode()).decode()
            return token, "local:fernet"
        except Exception as exc:
            logger.error(f"LocalFernetProvider encryption failed: {exc}")
            return plaintext, "local:plaintext"

    def decrypt(self, ciphertext: str, key_ref: str | None) -> str:
        if not self.enabled or self.fernet is None or key_ref == "local:plaintext":
            return ciphertext
        try:
            return self.fernet.decrypt(ciphertext.encode()).decode()
        except Exception as exc:
            logger.error(f"LocalFernetProvider decryption failed: {exc}")
            return ciphertext


class CloudKMSProvider(EncryptionProvider):
    def __init__(self):
        # In a real scenario, initialize GCP KMS Client or Supabase Vault Client here
        logger.info("Initializing CloudKMSProvider for envelope encryption.")

    def encrypt(self, plaintext: str) -> tuple[str, str | None]:
        # STUB for Production Cloud KMS
        # Actually call the KMS API
        logger.debug("CloudKMSProvider: encrypting payload...")
        # For now, fallback to base64 mock
        encoded = base64.b64encode(plaintext.encode()).decode()
        return f"kms_enc_{encoded}", "gcp:kms:keyring123"

    def decrypt(self, ciphertext: str, key_ref: str | None) -> str:
        # STUB for Production Cloud KMS
        logger.debug(f"CloudKMSProvider: decrypting payload with key_ref {key_ref}...")
        if ciphertext.startswith("kms_enc_"):
            encoded = ciphertext.replace("kms_enc_", "")
            return base64.b64decode(encoded.encode()).decode()
        return ciphertext


def get_encryption_provider() -> EncryptionProvider:
    # Use config environment to route to the correct provider
    env = getattr(settings, "environment", "development")
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
        except Exception as exc:
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
        except Exception as exc:
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