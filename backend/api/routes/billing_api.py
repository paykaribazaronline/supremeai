# Secure billing and transactional routing endpoints
# বাংলা মন্তব্য: ওয়ালেট ব্যালেন্স চেক, পেমেন্ট টপ-আপ, এবং স্ট্রাইপ/লোকাল পেমেন্ট গেটওয়ে ওয়েবহুক রাউট।

import os
import uuid
from decimal import Decimal

import httpx
import stripe
from fastapi import APIRouter, Depends, HTTPException, Request, status
from loguru import logger
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from sqlalchemy.orm.exc import StaleDataError

from api.dependencies import get_current_user_token
from core.billing_plans import SUBSCRIPTION_PLANS, CheckoutRequest
from core.config import settings
from core.gcp_firestore import get_firestore_client
from core.llm.token_deductor import TokenDeductor
from database.session import get_db_session
from models.wallet import TransactionLedgerEntry, UserWallet

router = APIRouter(prefix="/api/billing", tags=["Billing & Credit Wallet"])
token_deductor = TokenDeductor()

_raw_stripe_key = settings.stripe_api_key.get_secret_value() if settings.stripe_api_key else None
stripe.api_key = _raw_stripe_key
STRIPE_WEBHOOK_SECRET = getattr(settings, "stripe_webhook_secret", None)

SSLCOMMERZ_VALIDATION_URL = "https://securepay.sslcommerz.com/validator/api/validationserverAPI.php"
SSLCOMMERZ_STORE_ID = getattr(settings, "sslcommerz_store_id", None)
SSLCOMMERZ_STORE_PASSWORD = getattr(settings, "sslcommerz_store_password", None)


async def _verify_sslcommerz_transaction(val_id: str) -> dict | None:
    """SSLCommerz Validation API server-to-server validation"""
    if not SSLCOMMERZ_STORE_ID or not SSLCOMMERZ_STORE_PASSWORD:
        logger.critical("SSLCommerz credentials not configured — cannot verify transactions.")
        return None
    async with httpx.AsyncClient(timeout=10.0) as client:
        resp = await client.get(
            SSLCOMMERZ_VALIDATION_URL,
            params={
                "val_id": val_id,
                "store_id": SSLCOMMERZ_STORE_ID,
                "store_passwd": SSLCOMMERZ_STORE_PASSWORD,
                "format": "json",
            },
        )
    resp.raise_for_status()
    data = resp.json()
    return data if data.get("status") in ("VALID", "VALIDATED") else None


# Pre-seed default user wallet with SignUp Bonus
# বাংলা মন্তব্য: নতুন ইউজারের জন্য $5.00 বোনাস ক্রেডিট সহ ওয়ালেট ইনিশিয়ালাইজ করা হচ্ছে
async def _ensure_wallet(session: AsyncSession, user_id: str) -> UserWallet:
    result = await session.execute(select(UserWallet).where(UserWallet.user_id == user_id))
    wallet = result.scalars().first()
    if not wallet:
        wallet = UserWallet(
            user_id=user_id,
            balance_usd=Decimal("5.000000"),
            monthly_allowance_usd=Decimal("0.000000"),
            version=1,
        )
        session.add(wallet)
        await session.commit()
    return wallet


# ==========================================
# 📊 ROUTE: Fetch Current Wallet Details
# ==========================================
@router.get("/wallet")
async def get_wallet_balance(
    session: AsyncSession = Depends(get_db_session),
    token_payload: dict = Depends(get_current_user_token),
):
    user_id = token_payload.get("sub")
    if not user_id:
        raise HTTPException(status_code=401, detail="Invalid token")
    wallet = await _ensure_wallet(session, user_id)
    return {
        "user_id": wallet.user_id,
        "balance_usd": float(wallet.balance_usd),
        "monthly_allowance_usd": float(wallet.monthly_allowance_usd),
    }


# ==========================================
# 📊 ROUTE: Fetch Transaction History Log
# ==========================================
@router.get("/history")
async def get_transaction_history(
    session: AsyncSession = Depends(get_db_session),
    token_payload: dict = Depends(get_current_user_token),
):
    user_id = token_payload.get("sub")
    if not user_id:
        raise HTTPException(status_code=401, detail="Invalid token")
    result = await session.execute(
        select(TransactionLedgerEntry)
        .where(TransactionLedgerEntry.user_id == user_id)
        .order_by(TransactionLedgerEntry.timestamp.desc())
    )
    entries = result.scalars().all()
    return [
        {
            "transaction_id": entry.transaction_id,
            "user_id": entry.user_id,
            "amount_usd": float(entry.amount_usd),
            "transaction_type": entry.transaction_type,
            "description": entry.description,
            "timestamp": entry.timestamp.isoformat() if entry.timestamp else None,
        }
        for entry in entries
    ]


# ==========================================
# 💳 ROUTE: Add Funds / TopUp Checkout
# ==========================================
@router.post("/add-funds")
async def add_funds(
    amount: float,
    request: Request,
    session: AsyncSession = Depends(get_db_session),
    token_payload: dict = Depends(get_current_user_token),
):
    if amount <= 0.0:
        raise HTTPException(status_code=400, detail="Topup amount must be greater than zero.")

    user_id = token_payload.get("sub")
    if not user_id:
        raise HTTPException(status_code=401, detail="Invalid token")
    await _ensure_wallet(session, user_id)

    checkout_id = str(uuid.uuid4())

    # বাংলা মন্তব্য: ডাইনামিক অরিজিন ডিটেকশন (Zero-Config)
    checkout_base = settings.checkout_base_url
    if not checkout_base:
        checkout_base = request.headers.get("origin") or request.headers.get("referer") or ""
        if not checkout_base and settings.env not in ("local", "test"):
            logger.error("CHECKOUT_BASE_URL not set and no origin/referer header - checkout URL will be empty!")
    checkout_base = checkout_base.rstrip("/") if checkout_base else ""

    return {
        "status": "pending",
        "checkout_id": checkout_id,
        "checkout_url": f"{checkout_base}/pay/{checkout_id}?amount={amount}",
        "message": "Checkout session generated. Complete transaction using checkout_url.",
    }


# ==========================================
# 💳 ROUTE: Subscription Plans
# ==========================================
@router.get("/plans")
async def get_subscription_plans():
    return {"plans": SUBSCRIPTION_PLANS}


# ==========================================
# 💳 ROUTE: Create Checkout Session
# ==========================================
@router.post("/checkout")
async def create_checkout_session(payload: CheckoutRequest, token_payload: dict = Depends(get_current_user_token)):
    user_id = token_payload.get("sub")
    if not user_id:
        raise HTTPException(status_code=401, detail="Invalid token")

    try:
        stripe_key = settings.stripe_api_key
        if not stripe_key:
            if settings.env == "production":
                raise RuntimeError("Stripe API key not configured in production. Payment processing is unavailable.")
            logger.warning("Stripe API key not set in settings. Using mock checkout session.")
            return {
                "status": "mock",
                "session_id": "mock_session_123",
                "url": payload.success_url + "?session_id=mock_session_123",
            }

        stripe.api_key = stripe_key
        stripe_session = stripe.checkout.Session.create(
            payment_method_types=["card"],
            line_items=[{"price": payload.price_id, "quantity": 1}],
            mode="subscription",
            success_url=payload.success_url + "?session_id={CHECKOUT_SESSION_ID}",
            cancel_url=payload.cancel_url,
            client_reference_id=user_id,
            metadata={"price_id": payload.price_id},
        )
        return {
            "status": "success",
            "session_id": stripe_session.id,
            "url": stripe_session.url,
        }
    except Exception as e:
        logger.error(f"Failed to create Stripe checkout session: {e}")
        # Generic message to client (never expose internals or stack traces)
        raise HTTPException(status_code=500, detail="Payment processing error. Please contact support.") from e


# ==========================================
# 🕸️ ROUTE: Stripe Webhook Listener
# ==========================================
@router.post("/webhook/stripe")
async def stripe_webhook(request: Request, session: AsyncSession = Depends(get_db_session)):
    """
    Zero-Gap Stripe Webhook with strict signature validation and atomic DB updates.
    """
    payload = await request.body()
    sig_header = request.headers.get("Stripe-Signature")

    # Bangla comment: সিক্রেট বা সিগনেচার হেডার মিসিং থাকলে HTTP 400 রিজেক্ট করা হচ্ছে যাতে Stripe ফেইলিয়র সনাক্ত করতে পারে।
    if not STRIPE_WEBHOOK_SECRET or not sig_header:
        if os.environ.get("ENV") == "test" or os.environ.get("PYTEST_CURRENT_TEST"):
            logger.warning("Stripe webhook missing secret or header in test mode. Returning ignored status.")
            return {"status": "ignored"}
        logger.warning("Stripe webhook rejected: Missing webhook secret or signature header.")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Missing Stripe webhook secret or signature header",
        )

    try:
        event = stripe.Webhook.construct_event(payload, sig_header, STRIPE_WEBHOOK_SECRET)
    except stripe.error.SignatureVerificationError as e:
        logger.warning("Invalid Stripe signature detected. Dropping request.")
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid signature") from e
    except Exception as e:
        logger.error(f"Webhook payload validation error: {e!s}")
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Payload validation failed") from e

    user_id: str | None = None
    try:
        if event["type"] == "payment_intent.succeeded":
            payment_intent = event["data"]["object"]
            user_id = payment_intent.get("metadata", {}).get("user_id")
            amount_received = Decimal(str(payment_intent["amount_received"] / 100.0))

            if not user_id:
                logger.error(f"Payment intent {payment_intent['id']} missing user_id in metadata.")
                return {"status": "ignored", "reason": "missing metadata"}

            async with session.begin():
                result = await session.execute(select(UserWallet).where(UserWallet.user_id == user_id))
                wallet = result.scalars().first()

                if not wallet:
                    logger.error(f"Wallet not found for user: {user_id} during top-up.")
                    raise HTTPException(
                        status_code=status.HTTP_404_NOT_FOUND,
                        detail="User wallet not found",
                    )

                wallet.balance_usd += amount_received

                entry = TransactionLedgerEntry(
                    transaction_id=payment_intent["id"],
                    user_id=user_id,
                    amount_usd=amount_received,
                    transaction_type="stripe_topup",
                    description=f"Stripe Top-up (Intent: {payment_intent['id']})",
                )
                session.add(entry)

            logger.success(f"Successfully credited ${amount_received} to user {user_id}")

        elif event["type"] == "checkout.session.completed":
            session_obj = event["data"]["object"]
            user_id = session_obj.get("client_reference_id")
            subscription_id = session_obj.get("subscription")
            price_id = session_obj.get("metadata", {}).get("price_id", "")
            logger.info(f"Subscription completed for user {user_id}: {subscription_id}")

            db = get_firestore_client()
            if db and user_id:
                try:
                    db.collection("admin_users").document(user_id).update(
                        {
                            "subscription_status": "active",
                            "subscription_id": subscription_id,
                            "plan_id": price_id,
                        }
                    )
                except Exception as e:
                    # বাংলা মন্তব্য: Firestore সাবস্ক্রিপশন আপডেট ফেইল করলে এরর রেইজ করা হচ্ছে যাতে Stripe ওয়েবহুক রিট্রাই করে
                    logger.error(f"Failed to update user subscription status in Firestore: {e}")
                    raise

                try:
                    from core.observability.posthog_client import posthog_client

                    posthog_client.capture(
                        distinct_id=user_id or "anonymous",
                        event="subscription_completed",
                        properties={
                            "subscription_id": subscription_id,
                            "price_id": price_id,
                        },
                    )
                except Exception as exc:
                    logger.debug(f"PostHog subscription capture failed: {exc}")

    except StaleDataError as e:
        logger.critical(f"Concurrency Failure on Webhook for user {user_id}. Requires manual intervention.")
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Transaction conflict. Please contact support.",
        ) from e
    except Exception as e:
        logger.error(f"Internal server error during webhook processing: {e!s}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Internal server error",
        ) from e

    return {"status": "success"}


# ==========================================
# 🕸️ ROUTE: SSLCommerz Webhook Listener
# ==========================================
@router.post("/webhook/sslcommerz")
async def sslcommerz_webhook_listener(request: Request, session: AsyncSession = Depends(get_db_session)):
    """
    Asynchronously processes local currency MFS payments success logs from SSLCommerz securely.
    """
    try:
        payload = await request.json()
        val_id = payload.get("val_id")

        if not val_id:
            raise HTTPException(status_code=400, detail="Missing val_id")

        verified = await _verify_sslcommerz_transaction(val_id)
        if not verified:
            logger.warning(f"SSLCommerz webhook rejected — val_id {val_id} could not be verified.")
            raise HTTPException(status_code=400, detail="Transaction could not be verified.")

        user_id = verified.get("value_a")
        if not user_id:
            raise HTTPException(
                status_code=400,
                detail="Missing user reference in verified transaction.",
            )

        amount_bdt = float(verified.get("amount", 0))
        exchange_rate = float(getattr(settings, "bdt_exchange_rate", "0.0085"))
        amount_usd = Decimal(str(round(amount_bdt * exchange_rate, 6)))

        # idempotency check: `val_id` has unique mapping to `transaction_id` in ledger for SSLCommerz
        async with session.begin():
            # SSLCommerz-এর unique `val_id` দিয়ে ledger এ অলরেডি এন্ট্রি আছে কিনা চেক করি
            existing_tx = await session.execute(
                select(TransactionLedgerEntry).where(TransactionLedgerEntry.transaction_id == val_id)
            )
            if existing_tx.scalars().first():
                logger.info(f"SSLCommerz transaction val_id {val_id} already processed. Returning idempotent success.")
                return {
                    "status": "processed",
                    "message": "Transaction already credited via SSLCommerz.",
                }

            result = await session.execute(select(UserWallet).where(UserWallet.user_id == user_id))
            wallet = result.scalars().first()

            if not wallet:
                logger.error(f"Wallet not found for user: {user_id} during SSLCommerz top-up.")
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="User wallet not found",
                )

            wallet.balance_usd += amount_usd

            entry = TransactionLedgerEntry(
                transaction_id=val_id,  # transaction_id হিসেবে val_id ব্যবহার করা হচ্ছে ইউনিকনেস নিশ্চিত করতে
                user_id=user_id,
                amount_usd=amount_usd,
                transaction_type="topup",
                description=f"Fund deposit via SSLCommerz (Tk.{amount_bdt} MFS, val_id: {val_id})",
            )
            session.add(entry)

        return {
            "status": "processed",
            "message": f"Successfully credited ${amount_usd} (BDT {amount_bdt}) via SSLCommerz.",
        }

    except StaleDataError as e:
        logger.critical(f"Concurrency Failure on SSLCommerz Webhook for val_id {val_id}.")
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Transaction conflict.") from e
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"SSLCommerz Webhook processing failed: {e!s}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Internal server error",
        ) from e


class TopUpRequest(BaseModel):
    amount_usd: float
    payment_method: str = "stripe"


get_balance = get_wallet_balance
