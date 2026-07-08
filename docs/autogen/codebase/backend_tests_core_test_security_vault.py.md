# 📄 ফাইল: backend/tests/core/test_security_vault.py

**প্রকার:** .py  
**সাইজ:** 1,540 বাইট  
**আপডেট:** 2026-07-08T01:44:17.666123

---

## কোড

```py
import importlib
import os
import sys
from unittest.mock import patch

import pytest

# Set ENCRYPTION_KEY before importing core.security_vault to avoid import-time crash
os.environ.setdefault("ENCRYPTION_KEY", "9llmzMU2XSRhbAS-R__JMW1XLZzc0ll7obD_RqaVwno=")

# Reload in case module was partially imported
if "core.security_vault" in sys.modules:
    importlib.reload(sys.modules["core.security_vault"])

from core.security_vault import encrypt_token, decrypt_token


def test_encrypt_token_returns_string():
    result = encrypt_token("my-secret")
    assert isinstance(result, str)
    assert result != ""


def test_decrypt_token_returns_plaintext():
    encrypted = encrypt_token("my-secret")
    result = decrypt_token(encrypted)
    assert result == "my-secret"


def test_encrypt_empty_plain_text():
    assert encrypt_token("") == ""


def test_decrypt_empty_cipher_text():
    assert decrypt_token("") == ""


def test_decrypt_invalid_token_returns_empty():
    result = decrypt_token("invalid-token")
    assert result == ""


@patch("core.security_vault.fernet")
def test_encrypt_token_uses_fernet(mock_fernet):
    mock_fernet.encrypt.return_value = b"encrypted-bytes"
    result = encrypt_token("hello")
    assert result == "encrypted-bytes"
    mock_fernet.encrypt.assert_called_once_with(b"hello")


@patch("core.security_vault.fernet")
def test_decrypt_token_handles_exception(mock_fernet):
    mock_fernet.decrypt.side_effect = Exception("Decryption failed")
    result = decrypt_token("invalid")
    assert result == ""

```