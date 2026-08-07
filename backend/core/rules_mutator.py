from loguru import logger


class RulesMutator:
    """
    Dynamic Rules Mutation (The Shapeshifter).
    Dynamically blocks and releases malicious IPs in Upstash Redis.
    """

    def __init__(self) -> None:
        self.cooldown_seconds = 1800  # Default 30 minutes block

    # বাংলা মন্তব্য: প্রতি রিকোয়েস্টে যেন Sync Redis HTTP Call দিয়ে event loop ব্লক না হয়, সেজন্য
    # module-level shared in-memory TTL cache ব্যবহার করা হচ্ছে (সব instance শেয়ার করে)।
    # ক্যাশে TTL ৩ সেকেন্ড — একই IP-এর জন্য sync Redis কল সর্বোচ্চ ৩৩% এর কম (শুধু cold miss-এ)।
    _local_blocked_cache: dict[str, tuple[bool, float]] = {}

    def is_ip_blocked(self, ip_address: str) -> bool:
        import time

        now = time.time()
        # চেক করি ৩-সেকেন্ডের লোকাল ক্যাশে আছে কিনা
        if ip_address in self._local_blocked_cache:
            is_blocked, expire_at = self._local_blocked_cache[ip_address]
            if now < expire_at:
                return is_blocked

        from core import services

        is_blocked = False
        if hasattr(services, "redis_queue") and services.redis_queue and services.redis_queue.configured:
            redis_key = f"blocklist:ip:{ip_address}"
            try:
                val = services.redis_queue.get(redis_key)
                if val is not None:
                    is_blocked = val != "ok"
            except Exception as e:
                logger.error(f"Redis connection failed during is_ip_blocked: {e}")

        # ৩ সেকেন্ডের জন্য লোকালি ক্যাশ করি
        self._local_blocked_cache[ip_address] = (is_blocked, now + 3.0)
        return is_blocked

    def block_ip(self, ip_address: str, reason: str = "suspicious_activity") -> bool:
        logger.warning(f"RulesMutator: Blocking IP {ip_address} due to {reason}.")
        from core import services

        if hasattr(services, "redis_queue") and services.redis_queue and services.redis_queue.configured:
            redis_key = f"blocklist:ip:{ip_address}"
            try:
                services.redis_queue.set(redis_key, f"blocked:{reason}", ex=self.cooldown_seconds)
                return True
            except Exception as e:
                logger.error(f"Redis connection failed during block_ip: {e}")
        return False

    def release_ip(self, ip_address: str) -> bool:
        logger.info(f"RulesMutator: Releasing block on IP {ip_address}.")
        from core import services

        if hasattr(services, "redis_queue") and services.redis_queue and services.redis_queue.configured:
            redis_key = f"blocklist:ip:{ip_address}"
            try:
                services.redis_queue.set(redis_key, "", ex=1)
                return True
            except Exception as e:
                logger.error(f"Redis connection failed during release_ip: {e}")
        return False
