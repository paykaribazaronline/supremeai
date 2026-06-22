import uuid
from typing import Dict, Any
from loguru import logger
from database.supabase_client import db

class ViralReferralEngine:
    def __init__(self):
        logger.info("Initialized ViralReferralEngine")

    def generate_referral_code(self, user_id: str) -> str:
        code = f"SUPREME-{uuid.uuid4().hex[:6].upper()}"
        logger.info(f"Generated referral code {code} for user {user_id}")
        if db.client:
            try:
                db.client.table("referral_codes").upsert({
                    "code": code,
                    "referrer_id": user_id,
                    "status": "active",
                }).execute()
            except Exception as exc:
                logger.debug(f"Referral code persistence failed: {exc}")
        return code

    async def process_signup(self, new_user_id: str, referral_code: str) -> Dict[str, Any]:
        logger.info(f"Processing referral {referral_code} for new user {new_user_id}")
        referrer_id = None
        if db.client:
            try:
                res = db.client.table("referral_codes").select("referrer_id").eq("code", referral_code).execute()
                rows = res.data
                if rows:
                    referrer_id = rows[0].get("referrer_id")
            except Exception as exc:
                logger.debug(f"Referral lookup failed: {exc}")
        if not referrer_id:
            referrer_id = "unknown"
        reward_amount = 10.0
        if db.client:
            try:
                db.client.table("referral_redemptions").insert({
                    "code": referral_code,
                    "new_user_id": new_user_id,
                    "referrer_id": referrer_id,
                    "reward_amount": reward_amount,
                }).execute()
            except Exception as exc:
                logger.debug(f"Referral redemption persistence failed: {exc}")
        return {
            "status": "success",
            "referrer_id": referrer_id,
            "reward_applied": reward_amount,
        }
