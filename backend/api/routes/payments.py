import os
from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel
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

from core.gcp_firestore import get_firestore_client

class CheckoutRequest(BaseModel):
    price_id: str
    success_url: str
    cancel_url: str
    user_id: str

@router.get("/plans")
async def get_subscription_plans():
    return {
        "plans": [
            {"id": "price_basic_monthly", "name": "Basic Plan", "price": 9.99, "currency": "usd", "interval": "month"},
            {"id": "price_premium_monthly", "name": "Premium Plan", "price": 49.99, "currency": "usd", "interval": "month"},
            {"id": "price_enterprise_monthly", "name": "Enterprise Plan", "price": 199.99, "currency": "usd", "interval": "month"},
        ]
    }

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
    except Exception as e:
        raise HTTPException(status_code=401, detail=f"Invalid token: {e}")
    try:
        stripe_key = settings.stripe_api_key
        if not stripe_key:
            logger.warning("Stripe API key not set in settings. Using mock checkout session.")
            return {
                "status": "mock",
                "session_id": "mock_session_123",
                "url": payload.success_url + "?session_id=mock_session_123"
            }
        
        stripe.api_key = stripe_key
        session = stripe.checkout.Session.create(
            payment_method_types=['card'],
            line_items=[{
                'price': payload.price_id,
                'quantity': 1,
            }],
            mode='subscription',
            success_url=payload.success_url + "?session_id={CHECKOUT_SESSION_ID}",
            cancel_url=payload.cancel_url,
            client_reference_id=payload.user_id,
            metadata={"price_id": payload.price_id}
        )
        try:
            from core.posthog_client import posthog_client
            posthog_client.capture(
                distinct_id=payload.user_id,
                event="checkout_session_created",
                properties={"price_id": payload.price_id}
            )
        except Exception:
            pass
        return {
            "status": "success",
            "session_id": session.id,
            "url": session.url
        }
    except Exception as e:
        logger.error(f"Failed to create Stripe checkout session: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/webhook")
async def stripe_webhook(request: Request):
    payload = await request.body()
    sig_header = request.headers.get("stripe-signature")
    endpoint_secret = settings.stripe_webhook_secret
    stripe_key = settings.stripe_api_key
    
    if not sig_header or not endpoint_secret or not stripe_key:
        # Mock or fallback behavior for development/testing
        logger.warning("Stripe webhook credentials missing. Logging webhook call.")
        return {"status": "ignored", "reason": "configuration_missing"}
        
    try:
        stripe.api_key = stripe_key
        event = stripe.Webhook.construct_event(
            payload, sig_header, endpoint_secret
        )
    except Exception as e:
        logger.error(f"Webhook signature verification failed: {e}")
        raise HTTPException(status_code=400, detail="Invalid signature")
        
    if event['type'] == 'checkout.session.completed':
        session = event['data']['object']
        user_id = session.get('client_reference_id')
        subscription_id = session.get('subscription')
        price_id = session.get('metadata', {}).get('price_id', '')
        logger.info(f"Subscription completed for user {user_id}: {subscription_id}")
        
        db = get_firestore_client()
        if db and user_id:
            try:
                db.collection("admin_users").document(user_id).update({
                    "subscription_status": "active",
                    "subscription_id": subscription_id,
                    "plan_id": price_id
                })
            except Exception as e:
                logger.error(f"Failed to update user subscription status in Firestore: {e}")
        try:
            from core.posthog_client import posthog_client
            posthog_client.capture(
                distinct_id=user_id or "anonymous",
                event="subscription_completed",
                properties={
                    "subscription_id": subscription_id,
                    "price_id": price_id
                }
            )
        except Exception:
            pass
            
    return {"status": "success"}
