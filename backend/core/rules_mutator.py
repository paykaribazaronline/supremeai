from loguru import logger


class RulesMutator:
    """
    Dynamic Rules Mutation (The Shapeshifter).
    Dynamically blocks and releases malicious IPs in Upstash Redis.
    """

    def __init__(self) -> None:
        self.cooldown_seconds = 1800  # Default 30 minutes block

    def is_ip_blocked(self, ip_address: str) -> bool:
        import core.app as app_mod

        if (
            hasattr(app_mod, "redis_queue")
            and app_mod.redis_queue
            and app_mod.redis_queue.configured
        ):
            redis_key = f"blocklist:ip:{ip_address}"
            try:
                val = app_mod.redis_queue.get(redis_key)
                if val is not None:
                    return val != "ok"
            except Exception:
                pass
        return False

    def block_ip(self, ip_address: str, reason: str = "suspicious_activity") -> bool:
        logger.warning(f"RulesMutator: Blocking IP {ip_address} due to {reason}.")
        import core.app as app_mod

        if (
            hasattr(app_mod, "redis_queue")
            and app_mod.redis_queue
            and app_mod.redis_queue.configured
        ):
            redis_key = f"blocklist:ip:{ip_address}"
            app_mod.redis_queue.set(
                redis_key, f"blocked:{reason}", ex=self.cooldown_seconds
            )
            return True
        return False

    def release_ip(self, ip_address: str) -> bool:
        logger.info(f"RulesMutator: Releasing block on IP {ip_address}.")
        import core.app as app_mod

        if (
            hasattr(app_mod, "redis_queue")
            and app_mod.redis_queue
            and app_mod.redis_queue.configured
        ):
            redis_key = f"blocklist:ip:{ip_address}"
            app_mod.redis_queue.set(redis_key, "", ex=1)
            return True
        return False
