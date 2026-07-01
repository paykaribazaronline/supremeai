# Secure billing and transactional routing endpoints
# বাংলা মন্তব্য: ওয়ালেট ব্যালেন্স চেক, পেমেন্ট টপ-আপ, এবং স্ট্রাইপ/লোকাল পেমেন্ট গেটওয়ে ওয়েবহুক রাউট।

import os
import uuid
from decimal import Decimal

import stripe
from fastapi import APIRouter
from fastapi import Depends
from fastapi import HTTPException
from fastapi import Request
from fastapi import status
from loguru import logger
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from sqlalchemy.orm.exc import StaleDataError

from core.token_deductor import TokenDeductor
from database.session import get_db_session
from models.wallet import TransactionLedgerEntry
from models.wallet import UserWallet


router = APIRouter(prefix="/api/billing", tags=["Billing & Credit Wallet"])
token_deductor = TokenDeductor()

stripe.api_key = os.getenv("STRIPE_SECRET_KEY")
STRIPE_WEBHOOK_SECRET = os.getenv("STRIPE_WEBHOOK_SECRET")


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
            version=1
        )
        session.add(wallet)
        await session.commit()
    return wallet


# ==========================================
# 📊 ROUTE: Fetch Current Wallet Details
# ==========================================
@router.get("/wallet")
async def get_wallet_balance(session: AsyncSession = Depends(get_db_session)):
    user_id = "default_user_session"
    wallet = await _ensure_wallet(session, user_id)
    return {
        "user_id": wallet.user_id,
        "balance_usd": float(wallet.balance_usd),
        "monthly_allowance_usd": float(wallet.monthly_allowance_usd)
    }


# ==========================================
# 📊 ROUTE: Fetch Transaction History Log
# ==========================================
@router.get("/history")
async def get_transaction_history(session: AsyncSession = Depends(get_db_session)):
    user_id = "default_user_session"
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
            "timestamp": entry.timestamp.isoformat() if entry.timestamp else None
        }
        for entry in entries
    ]


# ==========================================
# 💳 ROUTE: Add Funds / TopUp Checkout
# ==========================================
@router.post("/add-funds")
async def add_funds(amount: float, session: AsyncSession = Depends(get_db_session)):
    if amount <= 0.0:
        raise HTTPException(status_code=400, detail="Topup amount must be greater than zero.")
    
    user_id = "default_user_session"
    await _ensure_wallet(session, user_id)
    
    checkout_id = str(uuid.uuid4())
    return {
        "status": "pending",
        "checkout_id": checkout_id,
        "checkout_url": f"https://checkout.supremeai.test/pay/{checkout_id}?amount={amount}",
        "message": "Checkout session generated. Complete transaction using checkout_url."
    }


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

    if not STRIPE_WEBHOOK_SECRET or not sig_header:
        logger.critical("Stripe webhook secrets or signature missing. Possible attack blocked.")
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Security validation failed")

    try:
        event = stripe.Webhook.construct_event(payload, sig_header, STRIPE_WEBHOOK_SECRET)
    except stripe.error.SignatureVerificationError as e:
        logger.warning("Invalid Stripe signature detected. Dropping request.")
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid signature") from e
    except Exception as e:
        logger.error(f"Webhook payload validation error: {str(e)}")
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Payload validation failed") from e

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
                    raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User wallet not found")

                wallet.balance_usd += amount_received

                entry = TransactionLedgerEntry(
                    transaction_id=payment_intent["id"],
                    user_id=user_id,
                    amount_usd=amount_received,
                    transaction_type="stripe_topup",
                    description=f"Stripe Top-up (Intent: {payment_intent['id']})"
                )
                session.add(entry)
            
            logger.success(f"Successfully credited ${amount_received} to user {user_id}")

    except StaleDataError as e:
        logger.critical(f"Concurrency Failure on Webhook for user {user_id}. Requires manual intervention.")
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Transaction conflict. Please contact support.") from e
    except Exception as e:
        logger.error(f"Internal server error during webhook processing: {str(e)}")
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Internal server error") from e

    return {"status": "success"}


# ==========================================
# 🕸️ ROUTE: SSLCommerz Webhook Listener
# ==========================================
@router.post("/webhook/sslcommerz")
async def sslcommerz_webhook_listener(request: Request, session: AsyncSession = Depends(get_db_session)):
    """
    Asynchronously processes local currency MFS payments success logs from SSLCommerz.
    """
    try:
        payload = await request.json()
        status_val = payload.get("status")
        
        if status_val == "VALID":
            user_id = payload.get("value_a", "default_user_session")
            amount_bdt = float(payload.get("amount", 0))
            amount_usd = Decimal(str(round(amount_bdt * 0.0085, 6)))

            async with session.begin():
                result = await session.execute(select(UserWallet).where(UserWallet.user_id == user_id))
                wallet = result.scalars().first()

                if not wallet:
                    logger.error(f"Wallet not found for user: {user_id} during SSLCommerz top-up.")
                    raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User wallet not found")

                wallet.balance_usd += amount_usd

                tx_id = str(uuid.uuid4())
                entry = TransactionLedgerEntry(
                    transaction_id=tx_id,
                    user_id=user_id,
                    amount_usd=amount_usd,
                    transaction_type="topup",
                    description=f"Fund deposit via SSLCommerz (Tk.{amount_bdt} MFS)"
                )
                session.add(entry)
            return {"status": "processed", "message": f"Successfully credited ${amount_usd} (BDT {amount_bdt}) via SSLCommerz."}

        return {"status": "ignored", "message": "SSLCommerz payment not VALID."}
    except StaleDataError as e:
        logger.critical(f"Concurrency Failure on SSLCommerz Webhook for user {user_id}.")
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Transaction conflict.") from e
    except Exception as e:
        logger.error(f"SSLCommerz Webhook processing failed: {str(e)}")
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Internal server error") from e
