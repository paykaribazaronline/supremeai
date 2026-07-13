import os

from cryptography.fernet import Fernet
from loguru import logger

from core.messaging.event_bus import ErrorEvent
from core.messaging.event_bus import error_event_bus


# বাংলা মন্তব্য: Module-level key read-এ fail-fast রাখা হচ্ছে, কারণ ক্রিপ্টোগ্রাফি স্টার্টআপেই ফেইল হওয়া উচিত।
# তবে ENCRYPTION_KEY যেন settings থেকে আসে তা নিশ্চিত করতে হবে, আপাতত os.environ.get ব্যবহার করলেও fail-fast আছে।
ENCRYPTION_KEY = os.environ.get("ENCRYPTION_KEY")
if not ENCRYPTION_KEY:
    # বাংলা মন্তব্য: টেস্ট ও সিআই পরিবেশে ক্র্যাশ এড়াতে একটি ডামি/এফেমেরাল কী জেনারেট করা হচ্ছে, তবে প্রোডাকশনে ফেইল-ফাস্ট থাকবে।
    if (os.environ.get("ENV") == "test" or os.environ.get("CI") == "true" or os.environ.get("GITHUB_ACTIONS") == "true") and os.environ.get("STRICT_ENCRYPTION_CHECK") != "true":
        ENCRYPTION_KEY = Fernet.generate_key().decode("utf-8")
    else:
        error_event_bus.emit(
            ErrorEvent(
                module="security_vault",
                error_type="MISSING_ENCRYPTION_KEY",
                message="ENCRYPTION_KEY environment variable is missing",
                severity="CRITICAL",
            )
        )
        raise ValueError("CRITICAL: ENCRYPTION_KEY environment variable is not set. Halting application for security reasons. Fail-Fast!")

fernet = Fernet(ENCRYPTION_KEY.encode("utf-8"))


def encrypt_token(plain_text: str) -> str:
    """Encrypts a token using AES (Fernet)"""
    if not plain_text:
        return ""
    try:
        encrypted_bytes = fernet.encrypt(plain_text.encode("utf-8"))
        return encrypted_bytes.decode("utf-8")
    except Exception as e:  # noqa: BLE001
        logger.error(f"Error encrypting token: {e}")
        error_event_bus.emit(ErrorEvent(module="security_vault", error_type="ENCRYPTION_FAILED", message=str(e)[:200], severity="ERROR"))
        raise RuntimeError("Token encryption failed.") from e


def decrypt_token(cipher_text: str) -> str:
    """Decrypts a token using AES (Fernet)"""
    if not cipher_text:
        return ""
    try:
        decrypted_bytes = fernet.decrypt(cipher_text.encode("utf-8"))
        return decrypted_bytes.decode("utf-8")
    except Exception as e:  # noqa: BLE001
        # বাংলা মন্তব্য: ডিক্রিপশন ফেইল হলে এখন আর সাইলেন্টলি ফেইল করবে না, এরর রেইজ করবে।
        logger.error(f"Error decrypting token: {e}")
        error_event_bus.emit(ErrorEvent(module="security_vault", error_type="DECRYPTION_FAILED", message=str(e)[:200], severity="CRITICAL"))
        raise ValueError("Decryption failed: Invalid or corrupted token.") from e
