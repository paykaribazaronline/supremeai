# 📄 ফাইল: backend/core/security_vault.py

**প্রকার:** .py  
**সাইজ:** 1,015 বাইট  
**আপডেট:** 2026-07-07T16:46:48.501671

---

## কোড

```py
import os

from cryptography.fernet import Fernet


# The key should be a 32-url-safe-base64-encoded bytes (Fernet key)
# In production, this must be set in environment variables!
ENCRYPTION_KEY = os.environ.get("ENCRYPTION_KEY")
if not ENCRYPTION_KEY:
    raise ValueError("CRITICAL: ENCRYPTION_KEY environment variable is not set. Halting application for security reasons.")

fernet = Fernet(ENCRYPTION_KEY.encode('utf-8'))

def encrypt_token(plain_text: str) -> str:
    """Encrypts a token using AES (Fernet)"""
    if not plain_text:
        return ""
    encrypted_bytes = fernet.encrypt(plain_text.encode('utf-8'))
    return encrypted_bytes.decode('utf-8')

def decrypt_token(cipher_text: str) -> str:
    """Decrypts a token using AES (Fernet)"""
    if not cipher_text:
        return ""
    try:
        decrypted_bytes = fernet.decrypt(cipher_text.encode('utf-8'))
        return decrypted_bytes.decode('utf-8')
    except Exception as e:
        print(f"Error decrypting token: {e}")
        return ""

```