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
from models.admin import AdminLoginRequest
from models.admin import AdminVerifyRequest


router = APIRouter()

auth = get_firebase_auth()


@router.post("/api/admin/login")
def admin_login(payload: AdminLoginRequest):
    password = payload.password
    expected_password = settings.docs_password
    if not expected_password:
        raise HTTPException(
            status_code=500, detail="Admin password not configured on server"
        )
    if password != expected_password:
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

    expected_password = settings.docs_password
    if not expected_password:
        raise HTTPException(
            status_code=500, detail="Admin password not configured on server"
        )
    if password != expected_password:
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

# বাংলা মন্তব্য: শুধুমাত্র স্ট্যান্ডার্ড ২-স্টেপ পাসওয়ার্ড + TOTP ফ্লোটি সক্রিয় রাখা হয়েছে। অন্যান্য মেথড বাতিল করা হলো।


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
            code = f"{h_num % 1000000:06d}"
            if code == user_otp:
                return True
        return False
    except Exception:
        return False


def check_totp(user_otp: str, base32_secret: str) -> bool:
    try:
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
            code = f"{h_num % 1000000:06d}"
            if code == user_otp:
                return True
        return False
    except Exception:
        return False
