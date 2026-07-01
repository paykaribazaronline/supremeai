import base64
import hashlib
import hmac
import os
import struct
import time

from fastapi import APIRouter
from fastapi import Body
from fastapi import HTTPException
from loguru import logger

from core import services
from core.config import settings
from core.events import get_firebase_auth
from core.gcp_firestore import get_firestore_client
from models.admin import AdminFirebaseLoginRequest
from models.admin import AdminFirebaseTotpSetupRequest
from models.admin import AdminFirebaseTotpVerifyRequest
from models.admin import AdminLoginRequest
from models.admin import AdminVerifyRequest

try:
    import bcrypt
except Exception:  # pragma: no cover - optional fallback
    bcrypt = None


router = APIRouter()


def _hash_password(password: str) -> str:
    if not bcrypt:
        raise RuntimeError("bcrypt is required but not installed")
    return bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()


def _verify_password(password: str, hashed: str) -> bool:
    if not bcrypt or not hashed:
        return False
    try:
        return bcrypt.checkpw(password.encode(), hashed.encode())
    except Exception:
        return False


def _get_admin_credentials():
    expected_hash = os.getenv("SUPREMEAI_ADMIN_PASSWORD_HASH")
    if not expected_hash:
        raise HTTPException(
            status_code=500, detail="Admin password hash is not configured on server"
        )
    return expected_hash


auth = get_firebase_auth()


@router.post("/api/admin/login")
def admin_login(payload: AdminLoginRequest):
    password = payload.password
    expected_hash = _get_admin_credentials()
    if not _verify_password(password, expected_hash):
        raise HTTPException(status_code=401, detail="Invalid password")

    totp_secret = os.getenv("SUPREMEAI_ADMIN_TOTP_SECRET")
    if not totp_secret:
        raise HTTPException(
            status_code=500, detail="TOTP secret not configured on server"
        )
    return {"status": "otp_required", "message": "Google Authenticator code required."}


@router.post("/api/admin/verify")
def admin_verify(payload: AdminVerifyRequest):
    password = payload.password
    otp = payload.otp

    expected_hash = _get_admin_credentials()
    if not _verify_password(password, expected_hash):
        raise HTTPException(status_code=401, detail="Invalid password")

    totp_secret = os.getenv("SUPREMEAI_ADMIN_TOTP_SECRET")
    if not totp_secret:
        raise HTTPException(
            status_code=500, detail="TOTP secret not configured on server"
        )

    if not otp or not verify_totp_code(otp.strip(), totp_secret):
        raise HTTPException(status_code=401, detail="Invalid Google Authenticator code")

    from jose import jwt

    jwt_payload = {"uid": "admin", "role": "admin", "exp": int(time.time()) + 3600 * 24}
    jwt_secret = settings.jwt_secret
    token = jwt.encode(jwt_payload, jwt_secret, algorithm="HS256")
    return {"status": "success", "token": token}

# বাংলা মন্তব্য: শুধুমাত্র স্ট্যান্ডার্ড ২-স্টেপ পাসওয়ার্ড + TOTP ফ্লো এবং ৭-ডিজিট ফায়ারবেস অথেনটিকেশন ফ্লোটি সক্রিয় রাখা হয়েছে।


@router.post("/api/admin/firebase-login")
def admin_firebase_login(payload: AdminFirebaseLoginRequest):
    id_token = payload.id_token
    is_production = getattr(settings, "env", "local").lower() == "production"

    try:
        if id_token.startswith("mock-"):
            if is_production:
                raise HTTPException(
                    status_code=403, detail="Mock tokens are strictly forbidden in production."
                )
            uid = "mock-admin-uid"
            email = settings.admin_emails[0] if settings.admin_emails else "admin@example.com"
            logger.warning(
                f"Bypassing verification using mock token mode. Token: {id_token[:20]}..."
            )
        elif auth:
            decoded_token = auth.verify_id_token(id_token)
            uid = decoded_token.get("uid", decoded_token.get("sub", "mock-admin-uid"))
            email = decoded_token.get("email", "")
            logger.info(f"Verified Firebase token for email: {email}")
        else:
            # Always enforce signature verification; offline verification bypass removed
            raise HTTPException(
                status_code=401, detail="Firebase Admin SDK is unavailable. Cannot authenticate."
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
                doc_ref.set(
                    {"email": email, "role": "admin", "created_at": str(time.time())}
                )
        except Exception as e:
            logger.critical(
                f"Firestore admin lookup failed (Possible DB connection issue/attack): {e}"
            )
            role = "user"
    elif email.lower() in [e.lower() for e in settings.admin_emails]:
        role = "admin"
    else:
        role = "user"

    if role != "admin":
        logger.warning(f"Unauthorized admin access attempt by UID: {uid}, Email: {email}")
        raise HTTPException(
            status_code=403, detail="Forbidden: Not authorized as an admin role user"
        )

    if not totp_secret:
        return {"status": "totp_setup_required", "uid": uid, "email": email}

    return {"status": "totp_required", "uid": uid}


@router.post("/api/admin/firebase-totp-setup")
def admin_firebase_totp_setup(payload: AdminFirebaseTotpSetupRequest):
    id_token = payload.id_token

    try:
        if id_token.startswith("mock-"):
            uid = "mock-admin-uid"
            email = settings.admin_emails[0] if settings.admin_emails else "admin@example.com"
        elif auth:
            decoded_token = auth.verify_id_token(id_token)
            uid = decoded_token.get("uid", decoded_token.get("sub", "mock-admin-uid"))
            email = decoded_token.get("email", "")
        else:
            raise HTTPException(
                status_code=401, detail="Firebase Admin SDK is unavailable. Cannot authenticate."
            )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=401, detail=f"Token decoding failed: {str(e)}"
        ) from e

    secret = base64.b32encode(os.urandom(10)).decode("utf-8")

    db = get_firestore_client()
    if db:
        try:
            db.collection("admin_users").document(uid).update({"temp_totp_secret": secret})
        except Exception as e:
            logger.error(f"Failed to store temp TOTP secret in Firestore: {e}")

    # বাংলা মন্তব্য: ৭ ডিজিটের ওটিপি রিকোয়েস্ট করার জন্য digits=7 যোগ করা হলো
    provisioning_uri = (
        f"otpauth://totp/SupremeAI:{email}?secret={secret}&issuer=SupremeAI&digits=7"
    )
    return {"secret": secret, "provisioning_uri": provisioning_uri}


@router.post("/api/admin/firebase-totp-verify")
def admin_firebase_totp_verify(payload: AdminFirebaseTotpVerifyRequest):
    id_token = payload.id_token
    otp = payload.otp

    try:
        if id_token.startswith("mock-"):
            uid = "mock-admin-uid"
        elif auth:
            decoded_token = auth.verify_id_token(id_token)
            uid = decoded_token.get("uid", decoded_token.get("sub", "mock-admin-uid"))
        else:
            raise HTTPException(
                status_code=401, detail="Firebase Admin SDK is unavailable. Cannot authenticate."
            )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=401, detail=f"Token decoding failed: {str(e)}"
        ) from e

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
        except Exception as e:
            logger.error(f"Failed to retrieve TOTP secret: {e}")

    secret_to_use = totp_secret or temp_totp_secret
    if not secret_to_use:
        secret_to_use = os.getenv("SUPREMEAI_ADMIN_TOTP_SECRET")
        if not secret_to_use:
            raise HTTPException(
                status_code=500, detail="TOTP secret not configured on server"
            )

    # বাংলা মন্তব্য: ৭ ডিজিটের কোড ভেরিফিকেশন করা হবে check_totp মেথডের মাধ্যমে
    if not check_totp(otp.strip(), secret_to_use):
        raise HTTPException(status_code=401, detail="Invalid verification code")

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

    jwt_payload = {"uid": uid, "role": "admin", "exp": int(time.time()) + 3600 * 24}
    jwt_secret = settings.jwt_secret
    token = jwt.encode(jwt_payload, jwt_secret, algorithm="HS256")

    return {"status": "success", "token": token}


@router.get("/admin/cloud-distribution")
def cloud_distribution():
    return {
        "distribution": services.parallel_router.get_distribution_stats(),
        "total_requests": sum(
            p["current_requests"] for p in services.parallel_router.PROVIDERS.values()
        ),
        "active_providers": sum(
            1
            for p in services.parallel_router.PROVIDERS.values()
            if p["status"] == "active"
        ),
        "strategy": "parallel_active_active",
        "rebalance_interval": "1 hour",
    }


@router.get("/admin/free-tier-status")
def free_tier_status():
    from core.free_tier_tracker import get_tracker

    tracker = get_tracker()
    return tracker.get_status()


@router.get("/admin/free-tier-status/{provider}")
def free_tier_provider_status(provider: str):
    from fastapi import HTTPException

    from core.free_tier_tracker import get_tracker

    tracker = get_tracker()
    status = tracker.get_provider_status(provider)
    if status is None:
        raise HTTPException(
            status_code=404, detail=f"Provider '{provider}' not tracked"
        )
    return status


@router.post("/admin/free-tier-pause/{provider}")
def free_tier_pause_provider(provider: str, payload: dict = Body(default={"seconds": 60})):
    from core.free_tier_tracker import get_tracker

    seconds = float(payload.get("seconds", 60))
    tracker = get_tracker()
    tracker.mark_rate_limited(provider, pause_seconds=seconds)
    return {"status": "paused", "provider": provider, "seconds": seconds}


@router.post("/admin/free-tier-override/{provider}")
def free_tier_override_limits(provider: str, payload: dict = Body(...)):
    from core.free_tier_tracker import get_tracker

    tracker = get_tracker()
    tracker.override_limits(provider, payload)
    return {"status": "updated", "provider": provider, "new_limits": payload}


@router.get("/admin/token-budget-stats")
def token_budget_stats():
    from core.token_budget import get_budget_manager

    manager = get_budget_manager()
    return manager.get_stats()


@router.get("/gcp/health")
def gcp_health():
    return {
        "status": "ok",
        "cloud_run": services.gcp_router.health_check(timeout=3),
        "firestore_mode": services.verification_queue.provider,
        "pubsub_mode": services.gcp_pubsub_queue.provider,
        "cloud_functions": services.cloud_function_client.get_config(),
    }


@router.get("/gcp/verification-queue/stats")
def gcp_verification_queue_stats():
    return services.verification_queue.stats()


@router.get("/gcp/pubsub/stats")
def gcp_pubsub_stats():
    return services.gcp_pubsub_queue.stats()


@router.get("/admin/rules")
def get_admin_rules():
    return services.rules_engine.rules


@router.post("/admin/rules")
def post_admin_rules(payload: dict = Body(...)):
    new_rules = payload.get("rules")
    if new_rules:
        success = services.rules_engine.save_rules(new_rules)
        if success:
            return {"status": "success"}
    return {"status": "error", "message": "Failed to save rules"}


@router.get("/skills")
def get_skills():
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


def verify_totp_code(user_otp: str, base32_secret: str) -> bool:
    try:
        # বাংলা মন্তব্য: বেস-৩২ সিক্রেট কি প্যাডিং ঠিক করা হলো
        missing_padding = len(base32_secret) % 8
        if missing_padding:
            base32_secret += "=" * (8 - missing_padding)
        key = base64.b32decode(base32_secret.upper())
        current_time = int(time.time() // 30)
        # বাংলা মন্তব্য: ওটিপি ড্র্রিফট উইন্ডো হ্যান্ডেল করা হলো অতিরিক্ত ৩টি স্লটের জন্য
        for drift in [-1, 0, 1]:
            msg = struct.pack(">Q", current_time + drift)
            h = hmac.new(key, msg, hashlib.sha1).digest()
            o = h[19] & 15
            h_num = struct.unpack(">I", h[o : o + 4])[0] & 0x7FFFFFFF
            # বাংলা মন্তব্য: ৭ ডিজিটের ওটিপি জেনারেট করা হলো এন্টারপ্রাইজ গ্রেড সিকিউরিটির জন্য
            code = f"{h_num % 10000000:07d}"
            if code == user_otp:
                return True
        return False
    except Exception:
        return False


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
            # বাংলা মন্তব্য: ৭ ডিজিটের ওটিপি জেনারেট করা হলো এন্টারপ্রাইজ গ্রেড সিকিউরিটির জন্য
            code = f"{h_num % 10000000:07d}"
            if code == user_otp:
                return True
        return False
    except Exception:
        return False
