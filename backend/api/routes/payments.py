import os

from fastapi import APIRouter, HTTPException, Request
from loguru import logger

try:
    import stripe
except ImportError:
    # Fallback mock stripe with minimal interface for testing
    class _MockStripe:
        class Checkout:
            class Session:
                @staticmethod
                def create(*args, **kwargs):
                    raise RuntimeError("Stripe not configured")

        class Webhook:
            @staticmethod
            def construct_event(*args, **kwargs):
                raise RuntimeError("Stripe webhook not configured")

    stripe = _MockStripe
from core.config import settings

router = APIRouter(prefix="/payments", tags=["payments"])

from core.billing_plans import CheckoutRequest


@router.post("/checkout")
async def create_checkout_session(request: Request, payload: CheckoutRequest):
    token = None
    auth_header = request.headers.get("authorization", "")
    if auth_header.startswith("Bearer "):
        token = auth_header[7:]
    if not token:
        raise HTTPException(status_code=401, detail="Missing authorization token")

    import jwt

    try:
        decoded = jwt.decode(token, settings.jwt_secret, algorithms=["HS256"])
    except Exception as e:
        # বাংলা মন্তব্য: সিকিউরিটি ইনফরমেশন লিক এড়াতে জেনেরিক এরর মেসেজ রিটার্ন করা হচ্ছে।
        raise HTTPException(status_code=401, detail="Invalid token. Please re-authenticate.") from e

    if decoded.get("user_id") != payload.user_id and decoded.get("sub") != payload.user_id:
        raise HTTPException(status_code=403, detail="User mismatch")

    try:
        stripe_key = settings.stripe_api_key
        if not stripe_key:
            is_production = os.environ.get("SUPREMEAI_ENV", "local").lower() == "production"
            if is_production:
                logger.critical("🚨 STRIPE PAYMENT GATEWAY MISCONFIGURED: API key missing in production")
                raise HTTPException(
                    status_code=503,
                    detail="Payment processing unavailable: Stripe not configured",
                )
            logger.warning("Stripe API key not set — returning 503 Service Unavailable (no mock)")
            raise HTTPException(
                status_code=503,
                detail="Stripe not configured. Payment processing unavailable in non-production environments.",
            )

        stripe.api_key = stripe_key
        session = stripe.checkout.Session.create(
            payment_method_types=["card"],
            line_items=[
                {
                    "price": payload.price_id,
                    "quantity": 1,
                }
            ],
            mode="subscription",
            success_url=payload.success_url + "?session_id={CHECKOUT_SESSION_ID}",
            cancel_url=payload.cancel_url,
            client_reference_id=payload.user_id,
            metadata={"price_id": payload.price_id},
        )
        try:
            from core.observability.posthog_client import posthog_client

            posthog_client.capture(
                distinct_id=payload.user_id,
                event="checkout_session_created",
                properties={"price_id": payload.price_id},
            )
        except Exception as exc:
            # বল মনতবয: PostHog তলমটর বযরথ হল চকআউট পরসস আটকান উচত নয়;
            # তব নরব সযলপ ন কর ডবগ লগ কর হল
            logger.debug(f"PostHog checkout capture failed: {exc}")
        return {"status": "success", "session_id": session.id, "url": session.url}
    except Exception as e:
        logger.error(f"Failed to create Stripe checkout session: {e}")
        # বাংলা মন্তব্য: সিকিউরিটি ইনফরমেশন লিক এড়াতে জেনেরিক এরর মেসেজ রিটার্ন করা হচ্ছে।
        raise HTTPException(status_code=500, detail="Payment processing error. Please contact support.") from e


# বাংলা মন্তব্য: সাবস্ক্রিপশন প্ল্যানগুলোর লিস্ট প্রদান করার জন্য এন্ডপয়েন্ট
@router.get("/plans")
async def get_plans():
    from core.billing_plans import SUBSCRIPTION_PLANS

    return {"plans": SUBSCRIPTION_PLANS}


# বাংলা মন্তব্য: স্ট্রাইপ ওয়েবহুক ইভেন্ট রিসিভ ও যাচাই করার জন্য এন্ডপয়েন্ট
# ⚠️ WARNING: This endpoint only verifies the webhook signature and returns success.
# It does NOT process payments, credit wallets, or activate subscriptions.
# The real payment processing webhook is at /api/billing/webhook/stripe (billing_api.py).
# If Stripe Dashboard is configured to send to this URL, customers will be charged
# but never receive their product — this is a silent revenue leak.
@router.post("/webhook")
async def stripe_webhook_endpoint(request: Request):
    logger.critical(
        "🔴 STRIPE WEBHOOK HIT DEPRECATED ENDPOINT /payments/webhook — this endpoint does NOT process payments. "
        "The correct endpoint is /api/billing/webhook/stripe. Update the Stripe Dashboard webhook URL immediately. "
        "Customers charged via this endpoint will NOT have their wallets credited or subscriptions activated."
    )
    sig_header = request.headers.get("stripe-signature", "")
    webhook_secret = None
    if settings.stripe_webhook_secret:
        # settings.stripe_webhook_secret is SecretStr, so get its value
        webhook_secret = settings.stripe_webhook_secret.get_secret_value()

    if not webhook_secret or not sig_header:
        # Fail-safe: misconfiguration shouldn't break production/CI; ignore webhook.
        logger.warning("Stripe webhook ignored (missing secret or signature header).")
        return {
            "status": "ignored",
            "reason": "missing_stripe_webhook_secret_or_signature",
        }

    payload = await request.body()
    try:
        event = stripe.Webhook.construct_event(payload, sig_header, webhook_secret)
        return {"status": "success", "event_type": event.get("type")}
    except Exception as e:
        logger.error(f"Webhook signature verification failed: {e}")
        # বাংলা মন্তব্য: সিকিউরিটি ইনফরমেশন লিক এড়াতে জেনেরিক এরর মেসেজ রিটার্ন করা হচ্ছে।
        raise HTTPException(status_code=400, detail="Webhook payload validation failed") from e
