# 📄 ফাইল: backend/core/secure_credential_store.py

**প্রকার:** .py  
**সাইজ:** 7,080 বাইট  
**আপডেট:** 2026-07-11T17:11:02.626167

---

## কোড

```py
from __future__ import annotations

import base64
import os
from abc import ABC
from abc import abstractmethod

from loguru import logger


try:
    from cryptography.fernet import Fernet
    from cryptography.fernet import InvalidToken

    CRYPTO_AVAILABLE = True
except ImportError:  # pragma: no cover
    CRYPTO_AVAILABLE = False


def generate_key() -> str:
    if not CRYPTO_AVAILABLE:
        raise RuntimeError("cryptography package is required for key generation")
    return Fernet.generate_key().decode()


class RotatingFernet:
    """
    বাংলা মন্তব্য: P0 Fix — Fernet key rotation with multiple-key decryption support.

    Encrypts always with the primary (latest) key.
    Decrypts by trying the primary key first; if that fails with InvalidToken
    it falls back to each previously known key in insertion order.

    All tokens are generated with a 24-hour TTL (86400 seconds).
    Expired tokens raise InvalidToken at decrypt time.
    """

    def __init__(self, keys: list[str]) -> None:
        if not keys:
            raise ValueError("At least one key is required")
        self._fernets = [Fernet(k.encode() if isinstance(k, str) else k) for k in keys]

    @property
    def primary(self) -> Fernet:
        return self._fernets[0]

    def encrypt(self, data: bytes) -> bytes:
        return self.primary.encrypt(data)

    def encrypt_at_time(self, data: bytes, current_time: int) -> bytes:
        return self.primary.encrypt_at_time(data, current_time)

    def decrypt(self, token: bytes, ttl: int | None = 86400) -> bytes:
        for fernet in self._fernets:
            try:
                return fernet.decrypt(token, ttl=ttl)
            except InvalidToken:
                continue
        raise InvalidToken("No valid key could decrypt token")

    def decrypt_at_time(self, token: bytes, ttl: int, current_time: int) -> bytes:
        last_exc: Exception | None = None
        for fernet in self._fernets:
            try:
                return fernet.decrypt_at_time(token, ttl, current_time)
            except InvalidToken as e:
                last_exc = e
                continue
        raise last_exc or InvalidToken("No valid key could decrypt token")


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
        self.rotating_fernet: RotatingFernet | None = None
        if CRYPTO_AVAILABLE:
            raw_key = encryption_key or os.getenv("SUPREMEAI_CREDENTIAL_ENC_KEY", "")
            if raw_key:
                try:
                    # Split by comma to support multiple keys (for rotation)
                    keys = [k.strip() for k in raw_key.split(",") if k.strip()]
                    self.rotating_fernet = RotatingFernet(keys)
                    self.enabled = True
                except Exception as exc:  # noqa: BLE001
                    logger.error(f"Failed to initialize Fernet: {exc}")

    def encrypt(self, plaintext: str) -> tuple[str, str | None]:
        if not self.enabled or not self.rotating_fernet:
            return plaintext, None
        try:
            token = self.rotating_fernet.encrypt(plaintext.encode())
            ciphertext = base64.urlsafe_b64encode(token).decode()
            return ciphertext, None
        except Exception as exc:  # noqa: BLE001
            logger.error(f"Encryption failed: {exc}")
            return plaintext, None

    def decrypt(self, ciphertext: str, key_ref: str | None) -> str:
        if not self.enabled or not self.rotating_fernet or key_ref:
            return ciphertext
        try:
            token = base64.urlsafe_b64decode(ciphertext.encode())
            plaintext = self.rotating_fernet.decrypt(token, ttl=86400)
            return plaintext.decode()
        except InvalidToken:
            logger.warning("Token expired or invalid — decryption failed")
            return ciphertext
        except Exception as exc:  # noqa: BLE001
            logger.error(f"Decryption failed: {exc}")
            return ciphertext


class CloudKMSProvider(EncryptionProvider):
    def __init__(self) -> None:
        self.kms_client = None
        self.key_name = os.getenv("KMS_KEY_NAME", "")
        self._init_kms()

    def _init_kms(self) -> None:
        if not self.key_name:
            return
        try:
            from google.cloud import kms

            self.kms_client = kms.KeyManagementServiceClient()
            logger.info("Cloud KMS initialized successfully.")
        except ImportError:
            logger.warning("google-cloud-kms not installed; Cloud KMS unavailable.")
        except Exception as exc:  # noqa: BLE001
            logger.error(f"Failed to initialize Cloud KMS: {exc}")

    def encrypt(self, plaintext: str) -> tuple[str, str | None]:
        if not self.kms_client or not self.key_name:
            logger.warning("KMS not configured; returning plaintext.")
            return plaintext, None
        try:
            response = self.kms_client.encrypt(request={"name": self.key_name, "plaintext": plaintext.encode()})
            ciphertext = base64.b64encode(response.ciphertext).decode()
            return ciphertext, self.key_name
        except Exception as exc:  # noqa: BLE001
            logger.error(f"KMS encrypt failed: {exc}")
            return plaintext, None

    def decrypt(self, ciphertext: str, key_ref: str | None) -> str:
        if not self.kms_client or not (key_ref or self.key_name):
            logger.warning("KMS not configured or missing key_ref; returning ciphertext as-is.")
            return ciphertext
        try:
            response = self.kms_client.decrypt(
                request={
                    "name": key_ref or self.key_name,
                    "ciphertext": base64.b64decode(ciphertext.encode()),
                }
            )
            return response.plaintext.decode()
        except Exception as exc:  # noqa: BLE001
            logger.error(f"KMS decrypt failed: {exc}")
            return ciphertext


class SecureCredentialStore:
    def __init__(self, provider: EncryptionProvider | None = None) -> None:
        self.provider: EncryptionProvider = provider or (CloudKMSProvider() if os.getenv("KMS_KEY_NAME") else LocalFernetProvider())

    def encrypt(self, plaintext: str) -> tuple[str, str | None]:
        return self.provider.encrypt(plaintext)

    def decrypt(self, ciphertext: str, key_ref: str | None = None) -> str:
        return self.provider.decrypt(ciphertext, key_ref)

    @staticmethod
    def mask(value: str, visible_chars: int = 4) -> str:
        if len(value) <= visible_chars:
            return "****"
        return value[:visible_chars] + "*" * (len(value) - visible_chars)

```