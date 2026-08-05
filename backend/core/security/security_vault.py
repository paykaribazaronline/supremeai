"""Security Vault - Fernet encryption with fail-fast key validation.

বাংলা: সিকিউরিটি ভল্ট — STRICT_ENCRYPTION_CHECK=true মোডে encryption key শুধুই raw environment থেকে নেয়া হবে।
"""

from __future__ import annotations

import os
import sys

from cryptography.fernet import Fernet
from loguru import logger

from core.config import settings
from core.error_bus import with_error_bus
from core.messaging.event_bus import ErrorContext, ErrorEvent, error_event_bus
from core.security.secure_credential_store import RotatingFernet

# Fail-fast policy:
# STRICT_ENCRYPTION_CHECK=true হলে encryption key শুধুই raw environment থেকে নেয়া হবে।
# (settings singleton/test computed secret এ stale value থাকতে পারে)
strict_enabled = os.environ.get("STRICT_ENCRYPTION_CHECK") == "true"

if strict_enabled:
    ENCRYPTION_KEY = os.environ.get("ENCRYPTION_KEY") or os.environ.get("SUPREMEAI_ENCRYPTION_KEY")
    if not ENCRYPTION_KEY:
        error_event_bus.emit(
            ErrorEvent(
                module="security_vault",
                error_type="MISSING_ENCRYPTION_KEY",
                message="ENCRYPTION_KEY environment variable is missing",
                severity="CRITICAL",
                structured_context=ErrorContext(module="auto_fixed"),
            )
        )
        raise ValueError(
            "CRITICAL: ENCRYPTION_KEY environment variable is not set. Halting application for security reasons. Fail-Fast!"
        )
else:
    # Normal mode: settings.encryption_key থেকে আসে (computed field via secret_vault)
    ENCRYPTION_KEY = (
        settings.encryption_key.get_secret_value() if settings.encryption_key else os.environ.get("ENCRYPTION_KEY")
    )

    if not ENCRYPTION_KEY:
        # বাংলা মন্তব্য: টেস্ট ও সিআই পরিবেশে ক্র্যাশ এড়াতে একটি ডামি/এফেমেরাল কী জেনারেট করা হচ্ছে।
        if (
            os.environ.get("ENV") in {"test", "testing", "ci"}
            or os.environ.get("CI") == "true"
            or os.environ.get("GITHUB_ACTIONS") == "true"
            or "pytest" in sys.modules
        ):
            ENCRYPTION_KEY = Fernet.generate_key().decode("utf-8")
        else:
            error_event_bus.emit(
                ErrorEvent(
                    module="security_vault",
                    error_type="MISSING_ENCRYPTION_KEY",
                    message="ENCRYPTION_KEY environment variable is missing",
                    severity="CRITICAL",
                    structured_context=ErrorContext(module="auto_fixed"),
                )
            )
            raise ValueError(
                "CRITICAL: ENCRYPTION_KEY environment variable is not set. Halting application for security reasons. Fail-Fast!"
            )


# Encryption key rotation support.
_raw_keys = [
    k
    for k in os.environ.get(
        "ENCRYPTION_KEYS",
        os.environ.get("SUPREMEAI_CREDENTIAL_ENC_KEY", ENCRYPTION_KEY or ""),
    ).split(",")
    if k.strip()
]

if not _raw_keys:
    raise ValueError("CRITICAL: No encryption keys configured (ENCRYPTION_KEYS). Fail-Fast!")

_vault = RotatingFernet(_raw_keys)


@with_error_bus("encrypt_token")
def encrypt_token(plain_text: str) -> str:
    """Encrypts a token using Fernet via central RotatingFernet."""

    if not plain_text:
        return ""

    try:
        return _vault.encrypt(plain_text.encode("utf-8")).decode("utf-8")
    except Exception as e:
        logger.error(f"Error encrypting token: {e}")
        error_event_bus.emit(
            ErrorEvent(
                module="security_vault",
                error_type="ENCRYPTION_FAILED",
                message=str(e)[:200],
                severity="ERROR",
                structured_context=ErrorContext(module="auto_fixed"),
            )
        )
        raise RuntimeError("Token encryption failed.") from e


@with_error_bus("decrypt_token")
def decrypt_token(cipher_text: str, ttl: int | None = None) -> str:
    """Decrypts a token using Fernet via central RotatingFernet."""

    if not cipher_text:
        return ""

    # ttl=None keeps the RotatingFernet default behavior.
    try:
        return _vault.decrypt(cipher_text.encode("utf-8"), ttl=ttl).decode("utf-8")
    except Exception as e:
        logger.error(f"Error decrypting token: {e}")
        error_event_bus.emit(
            ErrorEvent(
                module="security_vault",
                error_type="DECRYPTION_FAILED",
                message=str(e)[:200],
                severity="CRITICAL",
                structured_context=ErrorContext(module="auto_fixed"),
            )
        )
        raise ValueError("Decryption failed: Invalid or corrupted token.") from e
