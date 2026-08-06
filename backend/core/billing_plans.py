"""This module centralizes the definition of data models and static configuration for billing and subscription plans within the SupremeAI backend. It provides a standardized structure for initiating checkout requests and a comprehensive list of available subscription tiers, ensuring consistency across payment processing, user management, and feature access services.

Key Components:
- `CheckoutRequest`: A Pydantic model defining the required parameters for initiating a user checkout process for a specific billing plan.
- `SUBSCRIPTION_PLANS`: A constant dict keyed by plan name, each containing details like price, cost (Decimal), currency, interval, and included features.

Dependencies:
- `pydantic`: Used for defining `CheckoutRequest` to ensure robust data validation and serialization.
"""

from dataclasses import dataclass
from decimal import Decimal

from pydantic import BaseModel


class CheckoutRequest(BaseModel):
    price_id: str
    success_url: str
    cancel_url: str
    user_id: str | None = None


# বাংলা মন্তব্য: SubscriptionPlan dataclass — cost (Decimal) সহ সব tier-এর তথ্য।
# SUBSCRIPTION_PLANS dict কারণ tests `SUBSCRIPTION_PLANS["free"]` এভাবে access করে।
@dataclass
class SubscriptionPlan:
    id: str
    name: str
    price: float
    cost: Decimal
    currency: str
    interval: str
    features: list


SUBSCRIPTION_PLANS: dict[str, SubscriptionPlan] = {
    "free": SubscriptionPlan(
        id="price_free",
        name="Free Plan",
        price=0,
        cost=Decimal("0.00"),
        currency="usd",
        interval="month",
        features=["100 AI Credits", "Basic Models", "Community Support"],
    ),
    "pro": SubscriptionPlan(
        id="price_pro_monthly",
        name="Pro Plan",
        price=9.99,
        cost=Decimal("9.99"),
        currency="usd",
        interval="month",
        features=["1000 AI Credits", "Advanced Models", "Priority Support"],
    ),
    "enterprise": SubscriptionPlan(
        id="price_enterprise_monthly",
        name="Enterprise Plan",
        price=199.99,
        cost=Decimal("199.99"),
        currency="usd",
        interval="month",
        features=[
            "Unlimited AI Credits",
            "Dedicated Account Manager",
            "Custom SLAs",
            "API Access",
        ],
    ),
}

# বাংলা মন্তব্য: backward compatibility-র জন্য list format preserve করা হলো
SUBSCRIPTION_PLANS_LIST = [
    {
        "id": p.id,
        "name": p.name,
        "price": p.price,
        "currency": p.currency,
        "interval": p.interval,
        "features": p.features,
    }
    for p in SUBSCRIPTION_PLANS.values()
]
