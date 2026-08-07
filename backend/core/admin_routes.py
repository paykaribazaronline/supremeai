"""This module defines FastAPI routes for the SupremeAI project's administrative interface, providing secure authentication mechanisms, system monitoring, and configuration management. It supports both traditional password-based and Firebase-authenticated admin logins with Time-based One-Time Password (TOTP) verification, alongside endpoints for observing cloud resource distribution, free-tier usage, token budgets, GCP service health, rules engine management, and available AI skills. This centralizes control and visibility for the AI ecosystem's backend operations.

Key Components:
- `router`: The FastAPI APIRouter instance for admin-specific endpoints.
- `_hash_password()`: Hashes a given password using bcrypt.
- `_verify_password()`: Verifies a plain-text password against a bcrypt hash.
- `_get_admin_credentials()`: Retrieves the admin password hash from environment variables.
- `admin_login()`: Handles the initial step of traditional admin login, requiring a TOTP code.
- `admin_verify()`: Completes traditional admin login by verifying password and TOTP, issuing a JWT.
- `admin_firebase_login()`: Authenticates administrators via Firebase ID tokens, checks roles, and initiates TOTP flow if needed.
- `admin_firebase_totp_setup()`: Generates a TOTP secret and provisioning URI for Firebase-authenticated admins.
- `admin_firebase_totp_verify()`: Verifies a TOTP code for Firebase-authenticated admins, finalizing setup or issuing a JWT.
- `cloud_distribution()`: Provides statistics on the distribution of requests across LLM providers.
- `free_tier_status()`: Returns the overall status of free-tier usage.
- `free_tier_provider_status()`: Returns the free-tier status for a specific LLM provider.
- `free_tier_pause_provider()`: Pauses a free-tier provider for a specified duration.
- `free_tier_override_limits()`: Overrides the usage limits for a free-tier provider.
- `token_budget_stats()`: Provides statistics on token budget consumption.
- `gcp_health()`: Performs health checks for various Google Cloud Platform services.
- `gcp_verification_queue_stats()`: Returns statistics for the GCP verification queue.
- `gcp_pubsub_stats()`: Returns statistics for GCP Pub/Sub.
- `get_admin_rules()`: Retrieves the current rules from the rules engine.
- `post_admin_rules()`: Updates the rules within the rules engine.
- `get_skills()`: Lists available AI skills and their descriptions.
- `verify_totp_code()`: Verifies a Time-based One-Time Password (TOTP) code.
- `check_totp()`: An alias for `verify_totp_code()`, used for TOTP verification.

Dependencies:
- `base64`: For Base32 encoding/decoding in TOTP.
- `hashlib`: For SHA1 hashing in TOTP.
- `hmac`: For HMAC-SHA1 in TOTP.
- `os`: For environment variable access and secure random generation.
- `struct`: For packing/unpacking binary data in TOTP.
- `time`: For time-related operations in TOTP and JWT expiration.
- `fastapi`: For defining API routes and handling HTTP requests/responses.
- `loguru`: For structured logging.
- `bcrypt`: (Optional) For secure password hashing and verification.
- `core.services`: For accessing various core services like parallel router, GCP router, queues, and rules engine.
- `core.config`: For accessing application settings (e.g., `settings.jwt_secret`, `settings.admin_emails`).
- `core.messaging.events`: For `get_firebase_auth` to interact with Firebase Admin SDK.
- `core.gcp_firestore`: For `get_firestore_client` to interact with Firestore for admin user management.
- `models.admin`: For Pydantic models defining admin request payloads.
- `jose.jwt`: For encoding JSON Web Tokens (JWTs).
- `google.cloud.firestore`: For Firestore field deletion.
- `core.llm.free_tier_tracker`: For managing and monitoring LLM free-tier usage.
- `core.llm.token_budget`: For managing and monitoring LLM token budgets."""

import base64
import hashlib
import hmac
import os
import struct
import time
import uuid

from fastapi import APIRouter, Body, Depends, HTTPException, status
from loguru import logger
from redis.exceptions import RedisError

# বাংলা মন্তব্য: TOTP ব্রুট-ফোর্স প্রতিরোধে Redis lockout constants
_TOTP_MAX_ATTEMPTS = 5
_TOTP_LOCKOUT_SECONDS = 600  # 10 minutes

from api.dependencies import get_current_admin
from core import services
from core.config import settings
from core.gcp_firestore import get_firestore_client
from core.messaging.events import get_firebase_auth
from models.admin import (
    AdminFirebaseLoginRequest,
    AdminFirebaseTotpSetupRequest,
    AdminFirebaseTotpVerifyRequest,
)

router = APIRouter()


auth = get_firebase_auth()


# বাংলা মন্তব্য: শুধুমাত্র স্ট্যান্ডার্ড ২-স্টেপ পাসওয়ার্ড + TOTP ফ্লো এবং ৭-ডিজিট ফায়ারবেস অথেনটিকেশন ফ্লোটি সক্রিয় রাখা হয়েছে।


@router.post("/api/admin/firebase-login")
def admin_firebase_login(payload: AdminFirebaseLoginRequest):
    id_token = payload.id_token
    is_production = getattr(settings, "env", "local").lower() == "production"

    try:
        if id_token.startswith("mock-"):
            if is_production or getattr(settings, "env", "local").lower() not in (
                "local",
                "test",
                "testing",
            ):
                raise HTTPException(
                    status_code=403,
                    detail="Mock tokens are strictly forbidden outside of local testing environments.",
                )
            uid = "mock-admin-uid"
            email = settings.admin_emails[0] if settings.admin_emails else "admin@example.com"
            logger.warning(f"Bypassing verification using mock token mode. Token: {id_token[:20]}...")
        elif auth:
            decoded_token = auth.verify_id_token(id_token)
            uid = decoded_token.get("uid", decoded_token.get("sub", "mock-admin-uid"))
            email = decoded_token.get("email", "")
            logger.info(f"Verified Firebase token for email: {email}")
        else:
            raise HTTPException(
                status_code=401,
                detail="Firebase Admin SDK is unavailable. Cannot authenticate.",
            )
    except HTTPException:
        raise
    except Exception as e:
        logger.exception("Token verification/decoding failed")
        raise HTTPException(status_code=401, detail="Authentication failed") from e

    db = get_firestore_client()
    role = "user"
    totp_secret = None

    if db:
        try:
            doc_ref = db.collection("admin_users").document(uid)
            doc = doc_ref.get()
            if doc.exists:
                data = doc.to_dict()
                role = data.get("role", "user")
                totp_secret = data.get("totp_secret")
            elif email.lower() in [e.lower() for e in settings.admin_emails]:
                role = "admin"
                doc_ref.set({"email": email, "role": "admin", "created_at": str(time.time())})
        except Exception as e:
            logger.critical(f"Firestore admin lookup failed (Possible DB connection issue/attack): {e}")
            role = "user"
    elif email.lower() in [e.lower() for e in settings.admin_emails]:
        role = "admin"
    else:
        role = "user"

    if role != "admin":
        logger.warning(f"Unauthorized admin access attempt by UID: {uid}, Email: {email}")
        raise HTTPException(status_code=403, detail="Forbidden: Not authorized as an admin role user")

    if not totp_secret:
        return {"status": "totp_setup_required", "uid": uid, "email": email}

    return {"status": "totp_required", "uid": uid}


@router.post("/api/admin/firebase-totp-setup")
def admin_firebase_totp_setup(payload: AdminFirebaseTotpSetupRequest):
    id_token = payload.id_token
    is_production = getattr(settings, "env", "local").lower() == "production"

    try:
        if id_token.startswith("mock-"):
            # বাংলা মন্তব্য: প্রোডাকশনে mock টোকেন দিয়ে TOTP সেটআপ বাইপাস কঠোরভাবে নিষিদ্ধ
            if is_production:
                raise HTTPException(
                    status_code=403,
                    detail="Mock tokens are strictly forbidden in production.",
                )
            uid = "mock-admin-uid"
            email = settings.admin_emails[0] if settings.admin_emails else "admin@example.com"
        elif auth:
            decoded_token = auth.verify_id_token(id_token)
            uid = decoded_token.get("uid", decoded_token.get("sub", "mock-admin-uid"))
            email = decoded_token.get("email", "")
        else:
            raise HTTPException(
                status_code=401,
                detail="Firebase Admin SDK is unavailable. Cannot authenticate.",
            )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=401, detail=f"Token decoding failed: {e!s}") from e

    secret = base64.b32encode(os.urandom(10)).decode("utf-8")

    db = get_firestore_client()
    if db:
        try:
            db.collection("admin_users").document(uid).update({"temp_totp_secret": secret})
        except Exception as e:
            logger.error(f"Failed to store temp TOTP secret in Firestore: {e}")

    # বাংলা মন্তব্য: ৬ ডিজিটের ওটিপি রিকোয়েস্ট করা হলো
    provisioning_uri = f"otpauth://totp/SupremeAI:{email}?secret={secret}&issuer=SupremeAI&digits=6"
    return {"secret": secret, "provisioning_uri": provisioning_uri}


@router.post("/api/admin/firebase-totp-verify")
async def admin_firebase_totp_verify(payload: AdminFirebaseTotpVerifyRequest):
    id_token = payload.id_token
    otp = payload.otp
    is_production = getattr(settings, "env", "local").lower() == "production"

    try:
        if id_token.startswith("mock-"):
            # বাংলা মন্তব্য: প্রোডাকশনে mock টোকেন দিয়ে TOTP ভেরিফিকেশন বাইপাস কঠোরভাবে নিষিদ্ধ
            if is_production:
                raise HTTPException(
                    status_code=403,
                    detail="Mock tokens are strictly forbidden in production.",
                )
            uid = "mock-admin-uid"
        elif auth:
            decoded_token = auth.verify_id_token(id_token)
            uid = decoded_token.get("uid", decoded_token.get("sub", "mock-admin-uid"))
        else:
            raise HTTPException(
                status_code=401,
                detail="Firebase Admin SDK is unavailable. Cannot authenticate.",
            )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=401, detail=f"Token decoding failed: {e!s}") from e

    db = get_firestore_client()
    totp_secret = None
    temp_totp_secret = None

    if db:
        try:
            doc = db.collection("admin_users").document(uid).get()
            if doc.exists:
                data = doc.to_dict()
                totp_secret = data.get("totp_secret")
                temp_totp_secret = data.get("temp_totp_secret")
        except RedisError as exc:
            # Redis ডাউন থাকলে fail-closed নীতি বজায় রাখতে HTTP 503 রিটার্ন করা হচ্ছে
            logger.error(f"Redis unavailable during TOTP validation: {exc}")
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Authentication security service temporarily unavailable",
            )
        except Exception as e:
            logger.error(f"Failed to retrieve TOTP secret: {e}")

    secret_to_use = totp_secret or temp_totp_secret
    if not secret_to_use:
        secret_to_use = os.getenv("SUPREMEAI_ADMIN_TOTP_SECRET")
        if not secret_to_use:
            raise HTTPException(status_code=500, detail="TOTP secret not configured on server")

    # বাংলা মন্তব্য: Redis TOTP lockout — ব্রুট-ফোর্স অ্যাটাক প্রতিরোধ (Patch 3 fix)
    lockout_key = f"admin:totp:lockout:{uid}"
    attempt_key = f"admin:totp:attempts:{uid}"
    _redis = None
    try:
        from core.cache.redis_manager import redis_manager

        _redis = redis_manager.client
    except Exception as e:
        logger.debug(f"Redis client not available: {e}")

    if _redis:
        try:
            if await _redis.get(lockout_key):
                raise HTTPException(
                    status_code=429,
                    detail="TOTP verification locked. Please wait 10 minutes.",
                )
        except HTTPException:
            raise
        except Exception as e:
            # বাংলা মন্তব্য: সিকিউরিটি গার্ড — Redis ডাউন থাকলে লকআউট বাইপাস রোধ করতে fail-closed নীতি (HTTP 503) প্রয়োগ করা হচ্ছে
            logger.error(f"Redis lockout check failed — fail-closed: {e}")
            raise HTTPException(
                status_code=503,
                detail="Security service temporarily unavailable. Please retry later.",
            ) from e

    if not check_totp(otp.strip(), secret_to_use):
        # বাংলা মন্তব্য: ব্যর্থ OTP attempt counter বাড়ানো হচ্ছে, সীমা ছাড়ালে lockout
        if _redis:
            try:
                attempts = await _redis.incr(attempt_key)
                await _redis.expire(attempt_key, _TOTP_LOCKOUT_SECONDS)
                if int(attempts) >= _TOTP_MAX_ATTEMPTS:
                    await _redis.setex(lockout_key, _TOTP_LOCKOUT_SECONDS, "locked")
                    logger.critical(f"TOTP lockout triggered for uid={uid} after {attempts} failed attempts")
            except Exception as e:
                logger.warning(f"Redis attempt tracking failed: {e}")
        raise HTTPException(status_code=401, detail="Invalid verification code")

    # বাংলা মন্তব্য: সফল OTP-এ attempt counter রিসেট
    if _redis:
        try:
            await _redis.delete(attempt_key)
        except Exception as e:
            logger.debug(f"Failed to clear Redis attempts: {e}")

    if temp_totp_secret and not totp_secret and db:
        try:
            from google.cloud import firestore

            db.collection("admin_users").document(uid).update(
                {
                    "totp_secret": temp_totp_secret,
                    "temp_totp_secret": firestore.DELETE_FIELD,
                }
            )
        except Exception as e:
            logger.error(f"Failed to promote temp TOTP secret: {e}")

    from jose import jwt

    now = int(time.time())
    # বাংলা মন্তব্য: jti (JWT ID) + sub + iat যোগ করা হলো — JWT replay attack প্রতিরোধ (Patch 6 fix)
    jwt_payload = {
        "sub": uid,
        "uid": uid,
        "role": "admin",
        "exp": now + 3600 * 24,
        "iat": now,
        "jti": uuid.uuid4().hex,
    }
    jwt_secret = settings.jwt_secret
    token = jwt.encode(jwt_payload, jwt_secret, algorithm="HS256")

    return {"status": "success", "token": token}


@router.get("/admin/cloud-distribution")
def cloud_distribution(_admin: dict = Depends(get_current_admin)):
    return {
        "distribution": services.parallel_router.get_distribution_stats(),
        "total_requests": sum(p["current_requests"] for p in services.parallel_router.PROVIDERS.values()),
        "active_providers": sum(1 for p in services.parallel_router.PROVIDERS.values() if p["status"] == "active"),
        "strategy": "parallel_active_active",
        "rebalance_interval": "1 hour",
    }


@router.get("/admin/free-tier-status")
def free_tier_status(_admin: dict = Depends(get_current_admin)):
    from core.llm.free_tier_tracker import get_tracker

    tracker = get_tracker()
    return tracker.get_status()


@router.get("/admin/free-tier-status/{provider}")
def free_tier_provider_status(provider: str, _admin: dict = Depends(get_current_admin)):
    from fastapi import HTTPException

    from core.llm.free_tier_tracker import get_tracker

    tracker = get_tracker()
    status = tracker.get_provider_status(provider)
    if status is None:
        raise HTTPException(status_code=404, detail=f"Provider '{provider}' not tracked")
    return status


@router.post("/admin/free-tier-pause/{provider}")
def free_tier_pause_provider(
    provider: str,
    payload: dict = Body(default={"seconds": 60}),
    _admin: dict = Depends(get_current_admin),
):
    from core.llm.free_tier_tracker import get_tracker

    seconds = float(payload.get("seconds", 60))
    tracker = get_tracker()
    tracker.mark_rate_limited(provider, pause_seconds=seconds)
    logger.warning(f"Admin {_admin.get('sub')} paused provider '{provider}' for {seconds}s")
    return {"status": "paused", "provider": provider, "seconds": seconds}


@router.post("/admin/free-tier-override/{provider}")
def free_tier_override_limits(
    provider: str,
    payload: dict = Body(...),
    _admin: dict = Depends(get_current_admin),
):
    from core.llm.free_tier_tracker import get_tracker

    tracker = get_tracker()
    tracker.override_limits(provider, payload)
    logger.warning(f"Admin {_admin.get('sub')} overrode limits for '{provider}': {payload}")
    return {"status": "updated", "provider": provider, "new_limits": payload}


@router.get("/admin/token-budget-stats")
def token_budget_stats(_admin: dict = Depends(get_current_admin)):
    from core.llm.token_budget import get_budget_manager

    manager = get_budget_manager()
    return manager.get_stats()


@router.get("/gcp/health")
def gcp_health(_admin: dict = Depends(get_current_admin)):
    return {
        "status": "ok",
        "cloud_run": services.gcp_router.health_check(timeout=3),
        "firestore_mode": services.verification_queue.provider,
        "pubsub_mode": services.gcp_pubsub_queue.provider,
        "cloud_functions": services.cloud_function_client.get_config(),
    }


@router.get("/gcp/verification-queue/stats")
def gcp_verification_queue_stats(_admin: dict = Depends(get_current_admin)):
    return services.verification_queue.stats()


@router.get("/gcp/pubsub/stats")
def gcp_pubsub_stats(_admin: dict = Depends(get_current_admin)):
    return services.gcp_pubsub_queue.stats()


@router.get("/admin/rules")
def get_admin_rules(_admin: dict = Depends(get_current_admin)):
    return services.rules_engine.rules


@router.post("/admin/rules")
def post_admin_rules(payload: dict = Body(...), _admin: dict = Depends(get_current_admin)):
    new_rules = payload.get("rules")
    if new_rules:
        success = services.rules_engine.save_rules(new_rules)
        if success:
            return {"status": "success"}
    return {"status": "error", "message": "Failed to save rules"}


@router.get("/skills")
def get_skills(_admin: dict = Depends(get_current_admin)):
    return {
        "web_scraper": {
            "name": "web_scraper",
            "version": "1.0.0",
            "description": "Scrapes website contents using BeautifulSoup.",
        },
        "csv_exporter": {
            "name": "csv_exporter",
            "version": "1.0.0",
            "description": "Exports tabular data to CSV using pandas.",
        },
    }


def check_totp(user_otp: str, base32_secret: str) -> bool:
    try:
        # বাংলা মন্তব্য: বেস-৩২ সিক্রেট কি প্যাডিং ঠিক করা হলো
        missing_padding = len(base32_secret) % 8
        if missing_padding:
            base32_secret += "=" * (8 - missing_padding)
        key = base64.b32decode(base32_secret.upper())
        current_time = int(time.time() // 30)
        for drift in [-1, 0, 1]:
            msg = struct.pack(">Q", current_time + drift)
            h = hmac.new(key, msg, hashlib.sha1).digest()
            o = h[19] & 15
            h_num = struct.unpack(">I", h[o : o + 4])[0] & 0x7FFFFFFF
            # বাংলা মন্তব্য: ৬ ডিজিটের ওটিপি জেনারেট করা হলো
            code = f"{h_num % 1000000:06d}"
            # বাংলা মন্তব্য: টাইমিং অ্যাটাক প্রতিরোধে constant-time তুলনা ব্যবহার করা হলো
            if hmac.compare_digest(code, user_otp):
                return True
        return False
    except Exception as e:
        # বাংলা: fail-closed (False) আচরণ অপরিবর্তিত রাখা হলো — এটা 2FA verification,
        # তাই নিরাপত্তার জন্য এটাই সঠিক ডিফল্ট। শুধু কারণ লগ করা হচ্ছে (secret/OTP বাদে)
        # যাতে বোঝা যায় এটা invalid code নাকি malformed input/আসল বাগ
        logger.warning(f"TOTP verification raised an exception (not treated as valid): {type(e).__name__}: {e}")
        return False


# বাংলা মন্তব্য: verify_totp_code এখন check_totp-এর backward-compatible alias (Patch 5 fix)
# duplicate function সরানো হয়েছে, কিন্তু tests ও external callers-এর জন্য alias রাখা হয়েছে
verify_totp_code = check_totp
