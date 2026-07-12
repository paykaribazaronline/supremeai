from pydantic import BaseModel


class CheckoutRequest(BaseModel):
    price_id: str
    success_url: str
    cancel_url: str
    user_id: str | None = None


# Shared subscription plans data across billing and payments APIs
SUBSCRIPTION_PLANS = [
    {
        "id": "price_basic_monthly",
        "name": "Basic Plan",
        "price": 9.99,
        "currency": "usd",
        "interval": "month",
        "features": ["1000 AI Credits", "Basic Models", "Standard Support"],
    },
    {
        "id": "price_premium_monthly",
        "name": "Premium Plan",
        "price": 49.99,
        "currency": "usd",
        "interval": "month",
        "features": ["Unlimited AI Credits", "Premium Models (GPT-4, Claude Opus)", "Priority Support"],
    },
    {
        "id": "price_enterprise_monthly",
        "name": "Enterprise Plan",
        "price": 199.99,
        "currency": "usd",
        "interval": "month",
        "features": ["Dedicated Account Manager", "Custom SLAs", "API Access"],
    },
]
