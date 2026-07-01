import hashlib
import hmac
import os
import secrets
from datetime import UTC
from datetime import datetime
from datetime import timedelta

import jwt
from fastapi import HTTPException
from fastapi import status
from loguru import logger

from core.config import settings


# JWT
SECRET_KEY = settings.jwt_secret
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60

ADMIN_WHITELIST = os.getenv("ADMIN_EMAILS", "admin@supremeai.com").split(",")

if not SECRET_KEY:
    logger.critical(
        "🚨 FATAL: JWT Secret is missing! Halting boot process to prevent vulnerabilities."
    )
    raise RuntimeError("Security misconfiguration: Missing JWT Secret.")

# API Key settings
API_KEY_PREFIX = "sk-supreme"
API_KEY_RANDOM_BYTES = 32


def create_access_token(data: dict) -> str:
    """ক্রিপটগ্রাফিক সাইনড JWT জেনারেট করবে"""
    to_encode = data.copy()
    expire = datetime.now(UTC) + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    user_email = to_encode.get("sub")
    role = "admin" if user_email in ADMIN_WHITELIST else "user"
    to_encode.update({"role": role})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt


def verify_token(token: str) -> dict:
    """টোকেন ডিকোড এবং ভেরিফাই করবে"""
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        return payload
    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED, detail="Token has expired"
        ) from None
    except jwt.PyJWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials"
        ) from None


# ── API Key Crypto ────────────────────────────────────────────────────────


def _get_api_key_signing_secret() -> str:
    secret = os.getenv("API_KEY_SIGNING_SECRET") or settings.jwt_secret
    if not secret:
        raise RuntimeError("API_KEY_SIGNING_SECRET or JWT_SECRET must be set")
    return secret


def generate_api_key(prefix: str = API_KEY_PREFIX) -> str:
    random_part = secrets.token_urlsafe(API_KEY_RANDOM_BYTES)
    key = f"{prefix}-{random_part}"
    parts = key.split("-", 2)
    return f"{parts[0]}-{parts[1]}-{parts[2][:4]}-{parts[2][4:8]}-{parts[2][8:]}"


def hash_api_key(key: str) -> str:
    secret = _get_api_key_signing_secret()
    digest = hmac.new(secret.encode(), key.encode(), hashlib.sha256).hexdigest()
    return f"sha256${digest}"


def verify_api_key(plain_key: str, stored_hash: str) -> bool:
    expected = hash_api_key(plain_key)
    return hmac.compare_digest(expected, stored_hash)


def mask_api_key(key: str) -> str:
    parts = key.split("-")
    if len(parts) < 3:
        return key[:6] + "****"
    middle = parts[2]
    return f"{parts[0]}-{parts[1]}-{middle[:4]}****{middle[-4:]}"
