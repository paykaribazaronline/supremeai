import hashlib
import hmac
import ipaddress
import os
import secrets
import socket
from datetime import UTC, datetime, timedelta
from urllib.parse import urlparse

import jwt
from fastapi import HTTPException, status
from loguru import logger


def _get_jwt_secret() -> str:
    from core.config import settings

    secret = settings.jwt_secret
    if not secret:
        logger.critical("FATAL: JWT Secret is missing! Halting boot process.")
        raise RuntimeError("Security misconfiguration: Missing JWT Secret.")
    return secret


ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60

# API Key settings
API_KEY_PREFIX = "sk-supreme"
API_KEY_RANDOM_BYTES = 32


def create_access_token(data: dict) -> str:
    import uuid

    from core.config import settings

    to_encode = data.copy()
    expire = datetime.now(UTC) + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update(
        {
            "exp": expire,
            "jti": to_encode.get("jti") or f"jti-{uuid.uuid4().hex[:16]}",
        }
    )
    user_email = to_encode.get("sub")
    role = "admin" if user_email in settings.admin_emails else "user"
    to_encode.update({"role": role})
    encoded_jwt = jwt.encode(to_encode, _get_jwt_secret(), algorithm=ALGORITHM)
    return encoded_jwt


BLACKLIST_PREFIX = "jwt:blacklist:"
BLACKLIST_TTL = 86400  # 24 hours


async def revoke_token(jti: str, exp: int | None = None) -> None:
    """বাংলা মন্তব্য: JWT ID (jti) দিয়ে টোকেন রিভোক করে। Redis TTL দিয়ে অটো-ক্লিন হয়।"""
    import time

    from core.cache.redis_manager import redis_manager

    if redis_manager and getattr(redis_manager, "client", None):
        ttl = max(1, (exp - int(time.time())) if exp else BLACKLIST_TTL)
        try:
            await redis_manager.client.setex(
                f"{BLACKLIST_PREFIX}{jti}", min(ttl, BLACKLIST_TTL), "revoked"
            )
            logger.info(f"✅ JWT Token revoked: {jti}")
        except Exception as e:
            logger.warning(f"⚠️ Failed to revoke token in Redis: {e}")


async def is_token_revoked(jti: str) -> bool:
    """বাংলা মন্তব্য: টোকেন রিভোক করা হয়েছে কিনা Redis থেকে চেক করে।"""
    from core.cache.redis_manager import redis_manager

    if not redis_manager or not getattr(redis_manager, "client", None):
        return False  # Redis ডাউন থাকলে গ্রেসফুলি সার্ভিস বজায় থাকে
    try:
        return await redis_manager.client.exists(f"{BLACKLIST_PREFIX}{jti}") > 0
    except Exception:
        return False


def verify_token(token: str) -> dict:
    try:
        payload = jwt.decode(token, _get_jwt_secret(), algorithms=[ALGORITHM])
        jti = payload.get("jti")
        if jti:
            # Sync wrapper for sync verify_token callers
            import asyncio

            try:
                loop = asyncio.get_event_loop()
                if loop.is_running():
                    # If called inside active event loop, task check will handle in async auth middleware
                    pass
                else:
                    revoked = loop.run_until_complete(is_token_revoked(jti))
                    if revoked:
                        raise HTTPException(
                            status_code=status.HTTP_401_UNAUTHORIZED,
                            detail="Token has been revoked",
                        )
            except RuntimeError:
                pass
        return payload
    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED, detail="Token has expired"
        ) from None
    except jwt.PyJWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials"
        ) from None


def _get_api_key_signing_secret() -> str:
    from core.config import settings

    secret = os.getenv("API_KEY_SIGNING_SECRET") or settings.jwt_secret
    if not secret:
        raise RuntimeError("API_KEY_SIGNING_SECRET or JWT_SECRET must be set")
    return secret


def generate_api_key(prefix: str = API_KEY_PREFIX) -> str:
    random_part = (
        secrets.token_urlsafe(API_KEY_RANDOM_BYTES).replace("-", "").replace("_", "")
    )
    key = f"{prefix}-{random_part}"
    parts = key.split("-", 2)
    return f"{parts[0]}-{parts[1]}-{parts[2][:4]}-{parts[2][4:8]}-{parts[2][8:]}"


def hash_api_key(key: str) -> str:
    secret = _get_api_key_signing_secret()
    digest = hmac.new(secret.encode(), key.encode(), hashlib.sha256).hexdigest()
    return f"sha256${digest}"


def verify_api_key(plain_key: str, stored_hash: str) -> bool:
    # Constant-time comparison using hmac.compare_digest
    expected = hash_api_key(plain_key)
    return hmac.compare_digest(expected, stored_hash)


def verify_api_key_with_expiry(
    plain_key: str, stored_hash: str, expires_at: int | None = None
) -> bool:
    """বাংলা মন্তব্য: API Key হ্যাশ ভেরিফাই করে এবং একই সাথে Expiration টাইম চেক করে।"""
    import time

    if expires_at is not None and time.time() > expires_at:
        logger.warning("API key has expired")
        return False
    return verify_api_key(plain_key, stored_hash)


def mask_api_key(key: str) -> str:
    parts = key.split("-")
    if len(parts) < 3:
        return key[:6] + "****"
    middle = parts[2]
    return f"{parts[0]}-{parts[1]}-{middle[:4]}****{middle[-4:]}"


def is_safe_url(url: str) -> bool:
    try:
        parsed = urlparse(url)
        hostname = parsed.hostname
        if not hostname:
            return False
        if hostname == "169.254.169.254" or hostname.endswith(".local"):
            return False
        ip = socket.gethostbyname(hostname)
        ip_obj = ipaddress.ip_address(ip)
        return not (ip_obj.is_private or ip_obj.is_loopback or ip_obj.is_link_local)
    except (ValueError, socket.gaierror, OSError) as e:
        logger.warning(f"URL safety check failed for '{url}': {e}")
        return False
