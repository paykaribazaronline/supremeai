import os

from fastapi import APIRouter
from fastapi import HTTPException
from fastapi import Request
from loguru import logger
from pydantic import BaseModel


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

from core.gcp_firestore import get_firestore_client


from core.billing_plans import CheckoutRequest




@router.post("/checkout")
async def create_checkout_session(request: Request, payload: CheckoutRequest):
    token = None
    auth_header = request.headers.get("authorization", "")
    if auth_header.startswith("Bearer "):
        token = auth_header[7:]
    if not token:
        raise HTTPException(status_code=401, detail="Missing authorization token")
    try:
        from jose import jwt

        decoded = jwt.decode(token, settings.jwt_secret, algorithms=["HS256"])
        if decoded.get("user_id") != payload.user_id and decoded.get("sub") != payload.user_id:
            raise HTTPException(status_code=403, detail="User mismatch")
    except Exception as e:  # noqa: BLE001
        raise HTTPException(status_code=401, detail=f"Invalid token: {e}") from e
    try:
        stripe_key = settings.stripe_api_key
        if not stripe_key:
            if os.environ.get("SUPREMEAI_ENV") == "production":
                raise RuntimeError("Stripe API key not configured in production. Payment processing is unavailable.")
            logger.warning("Stripe API key not set in settings. Using mock checkout session.")
            return {
                "status": "mock",
                "session_id": "mock_session_123",
                "url": payload.success_url + "?session_id=mock_session_123",
            }

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
        except Exception as exc:  # noqa: BLE001
            # বল মনতবয: PostHog তলমটর বযরথ হল চকআউট পরসস আটকান উচত নয়;
            # তব নরব সযলপ ন কর ডবগ লগ কর হল
            logger.debug(f"PostHog checkout capture failed: {exc}")
        return {"status": "success", "session_id": session.id, "url": session.url}
    except Exception as e:  # noqa: BLE001
        logger.error(f"Failed to create Stripe checkout session: {e}")
        raise HTTPException(status_code=500, detail=str(e)) from e



