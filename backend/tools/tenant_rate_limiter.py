import time
from typing import Any

from loguru import logger

from core.config import settings


class TenantRateLimiter:
    """
    Per-organization rate limiting in Redis with Stripe billing tier integration.
    (Closes Gap #55)
    """

    def __init__(self, redis_client=None):
        self.redis_client = redis_client
        self.queue = self._resolve_redis_queue()
        self._init_billing_tiers()
        logger.info("Initialized TenantRateLimiter with Redis and Stripe billing tiers")

    def _resolve_redis_queue(self):
        # বাংলা মন্তব্য: যদি কনস্ট্রাক্টরে নির্দিষ্ট কোনো redis_client দেওয়া থাকে (যেমন টেস্টে), তবে সেটিকেই অগ্রাধিকার দেওয়া হলো
        if self.redis_client is not None:
            return self.redis_client
        try:
            import core.services as app_mod

            return getattr(app_mod, "redis_queue", None)
        except Exception:
            return None

    def _init_billing_tiers(self) -> None:
        self.billing_tiers = {
            "free": {
                "rpm": 60,
                "rpd": 1000,
                "cost_per_call": 0.0,
            },
            "pro": {
                "rpm": 500,
                "rpd": 50000,
                "cost_per_call": 0.001,
            },
            "enterprise": {
                "rpm": 2000,
                "rpd": 500000,
                "cost_per_call": 0.0005,
            },
        }

    def _redis_key(self, tenant_id: str, suffix: str) -> str:
        return f"rate:{tenant_id}:{suffix}"

    async def get_tier(self, tenant_id: str) -> str:
        if not self.queue or not getattr(self.queue, "configured", False):
            return "free"
        try:
            tier = self.queue.get(f"billing:tier:{tenant_id}")
            if tier is not None:
                return tier.decode("utf-8") if isinstance(tier, bytes) else str(tier)
        except Exception as exc:
            logger.debug(f"Tier lookup failed: {exc}")
        return "free"

    async def set_tier(self, tenant_id: str, tier: str) -> None:
        if not self.queue or not getattr(self.queue, "configured", False):
            return
        if tier not in self.billing_tiers:
            raise ValueError(f"Invalid tier: {tier}")
        try:
            self.queue.set(f"billing:tier:{tenant_id}", tier, ex=3600)
        except Exception as exc:
            logger.debug(f"Tier update failed: {exc}")

    async def check_quota(
        self,
        tenant_id: str,
        cost: float,
        admin_override: bool = False,
    ) -> dict[str, Any]:
        tier_key = await self.get_tier(tenant_id)
        tier = self.billing_tiers.get(tier_key, self.billing_tiers["free"])

        if admin_override:
            logger.debug(f"Admin override for tenant {tenant_id}")
            return {
                "allowed": True,
                "reason": "admin_override",
                "tier": tier_key,
            }

        if not self.queue or not getattr(self.queue, "configured", False):
            return {"allowed": True, "reason": "no_redis", "tier": tier_key}

        now = int(time.time())
        minute_key = self._redis_key(tenant_id, f"{now // 60}:rpm")
        day_key = self._redis_key(tenant_id, f"{now // 86400}:rpd")

        try:
            rpm = int(self.queue.get(minute_key) or 0)
            rpd = int(self.queue.get(day_key) or 0)

            if rpm >= tier["rpm"]:
                logger.warning(f"Tenant {tenant_id} exceeded RPM ({rpm}/{tier['rpm']})")
                return {
                    "allowed": False,
                    "reason": "rpm_exceeded",
                    "current": rpm,
                    "limit": tier["rpm"],
                }

            if rpd >= tier["rpd"]:
                logger.warning(f"Tenant {tenant_id} exceeded RPD ({rpd}/{tier['rpd']})")
                return {
                    "allowed": False,
                    "reason": "rpd_exceeded",
                    "current": rpd,
                    "limit": tier["rpd"],
                }
        except Exception as exc:
            logger.debug(f"Redis quota check failed: {exc}")
            return {"allowed": True, "reason": "redis_error", "tier": tier_key}

        return {"allowed": True, "reason": "ok", "tier": tier_key}

    async def record_usage(
        self,
        tenant_id: str,
        cost: float,
        tokens: int,
    ) -> dict[str, Any]:
        tier_key = await self.get_tier(tenant_id)
        self.billing_tiers.get(tier_key, self.billing_tiers["free"])

        if not self.queue or not getattr(self.queue, "configured", False):
            return {
                "status": "success",
                "tenant_id": tenant_id,
                "tier": tier_key,
                "billed": 0.0,
            }

        now = int(time.time())
        minute_key = self._redis_key(tenant_id, f"{now // 60}:rpm")
        day_key = self._redis_key(tenant_id, f"{now // 86400}:rpd")
        cost_key = self._redis_key(tenant_id, "cost")
        tokens_key = self._redis_key(tenant_id, "tokens")

        try:
            if hasattr(self.queue, "pipeline"):
                pipe = self.queue.pipeline()
                pipe.incr(minute_key, 1)
                pipe.expire(minute_key, 90)
                pipe.incr(day_key, 1)
                pipe.expire(day_key, 86400 + 300)
                pipe.incrbyfloat(cost_key, cost)
                pipe.set(
                    tokens_key,
                    str(int(self.queue.get(tokens_key) or 0) + tokens),
                    ex=86400 + 300,
                )
                pipe.execute()
            else:
                self.queue.incr(minute_key, 1)
                self.queue.set(minute_key, str(self.queue.get(minute_key) or 1), ex=90)
                self.queue.incr(day_key, 1)
                self.queue.set(
                    day_key, str(self.queue.get(day_key) or 1), ex=86400 + 300
                )
                self.queue.set(
                    cost_key,
                    str(float(self.queue.get(cost_key) or 0.0) + cost),
                    ex=86400 + 300,
                )
                self.queue.set(
                    tokens_key,
                    str(int(self.queue.get(tokens_key) or 0) + tokens),
                    ex=86400 + 300,
                )
        except Exception as exc:
            logger.debug(f"Redis usage recording failed: {exc}")

        total_cost = float(self.queue.get(cost_key) or 0.0) if self.queue else 0.0
        if total_cost > 0 and settings.stripe_api_key:
            self._maybe_charge_stripe(tenant_id, total_cost)

        return {
            "status": "success",
            "tenant_id": tenant_id,
            "tier": tier_key,
            "cost_recorded": cost,
            "total_cost": total_cost,
        }

    def _maybe_charge_stripe(self, tenant_id: str, amount: float) -> None:
        """Charge tenant via Stripe when usage exceeds free tier threshold."""
        if amount < 1.0:
            return
        try:
            import stripe

            stripe.api_key = settings.stripe_api_key
            customer_id = (
                self.queue.get(f"stripe:customer:{tenant_id}") if self.queue else None
            )
            if not customer_id:
                logger.debug(f"No Stripe customer for tenant {tenant_id}")
                return
            customer_id = (
                customer_id.decode("utf-8")
                if isinstance(customer_id, bytes)
                else str(customer_id)
            )
            stripe.InvoiceItem.create(
                customer=customer_id,
                amount=int(amount * 100),
                currency="usd",
                description=f"SupremeAI usage - tenant {tenant_id}",
            )
            logger.info(f"Stripe usage recorded for tenant {tenant_id}: ${amount:.4f}")
        except Exception as exc:
            logger.debug(f"Stripe charge failed: {exc}")
