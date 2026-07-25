"""This module centralizes the definition of data models and static configuration for billing and subscription plans within the SupremeAI backend. It provides a standardized structure for initiating checkout requests and a comprehensive list of available subscription tiers, ensuring consistency across payment processing, user management, and feature access services.

Key Components:
- `CheckoutRequest`: A Pydantic model defining the required parameters for initiating a user checkout process for a specific billing plan.
- `SUBSCRIPTION_PLANS`: A constant list of dictionaries, each representing a distinct subscription plan with details like ID, name, price, currency, interval, and included features.

Dependencies:
- `pydantic`: Used for defining `CheckoutRequest` to ensure robust data validation and serialization.
"""  # noqa: E501

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
        "features": [
            "Unlimited AI Credits",
            "Premium Models (GPT-4, Claude Opus)",
            "Priority Support",
        ],
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
