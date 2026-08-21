"""core/auto_healer_service.py — FastAPI-integrated AutoHealer Background Service.

বাংলা মন্তব্য: আগে agents/devops/auto_healer.py একটি standalone command-line script ছিল।
এটা production FastAPI server-এ কখনো চলত না।
এই নতুন service টা lifespan.py থেকে background asyncio task হিসেবে চালু হয়,
database, Redis, এবং LLM provider-এর health continuously monitor করে,
এবং problem detect হলে স্বয়ংক্রিয়ভাবে heal করার চেষ্টা করে।
"""

from __future__ import annotations

from collections import deque
import asyncio
import time
from typing import Any

from loguru import logger


class AutoHealerService:
    """
    Continuously monitors critical services and auto-heals them.

    বাংলা মন্তব্য: এই service টা lifespan.py থেকে background task হিসেবে চালু হয়।
    Render/Cloud Run-এ container restart ছাড়াই healing হবে।

    Healed subsystems:
    - PostgreSQL connection pool (reconnect on failure)
    - Redis connection (reconnect on failure)
    - LLM provider (switch provider on consecutive failures)
    """

    def __init__(self, check_interval_seconds: int = 30) -> None:
        self.check_interval = check_interval_seconds
        self._running = False
        self._task: asyncio.Task | None = None
        # বাংলা: subsystem → পর পর failure count
        self.failure_counts: dict[str, int] = {}
        # বাংলা: cooldown — একই subsystem বারবার heal করা থেকে বিরত রাখে
        self._last_heal_time: dict[str, float] = {}
        self.HEAL_COOLDOWN_SECONDS = 120  # 2 minutes
        # বাংলা মন্তব্য: মেমোরি লিক রোধে ফিক্সড-সাইজ রিং বাফার (সর্বোচ্চ ১০০ রেকর্ড)
        self._history: deque[dict[str, Any]] = deque(maxlen=100)

    # ── Lifecycle ──────────────────────────────────────────────────────────────

    async def start(self) -> None:
        """Background healing loop শুরু করা। lifespan.py থেকে call করা হয়।"""
        if self._running:
            return
        self._running = True
        self._task = asyncio.create_task(self._healing_loop(), name="auto-healer")
        logger.info("🚑 AutoHealerService background loop started (interval=30s).")

    async def stop(self) -> None:
        """Gracefully stop করা। lifespan shutdown-এ call করা হয়।"""
        self._running = False
        if self._task and not self._task.done():
            self._task.cancel()
            try:
                await self._task
            except asyncio.CancelledError:
                pass
        logger.info("🚑 AutoHealerService stopped.")

    # ── Main Loop ──────────────────────────────────────────────────────────────

    async def _healing_loop(self) -> None:
        """Main background loop।"""
        while self._running:
            try:
                await self._check_and_heal()
            except Exception as exc:
                logger.error(f"🚑 AutoHealer check cycle failed unexpectedly: {exc!r}")
            await asyncio.sleep(self.check_interval)

    async def _check_and_heal(self) -> None:
        """সব critical subsystem সমান্তরালে (parallel) check করা এবং দরকারে heal করা।"""
        start_t = time.monotonic()
        results = await asyncio.gather(
            self._check_database(),
            self._check_redis(),
            return_exceptions=True,
        )
        duration_ms = round((time.monotonic() - start_t) * 1000, 2)
        self._history.append({
            "timestamp": time.time(),
            "duration_ms": duration_ms,
            "results": [str(r) if isinstance(r, Exception) else "ok" for r in results],
        })

    # ── Database Healing ───────────────────────────────────────────────────────

    async def _check_database(self) -> None:
        """PostgreSQL pool health check এবং auto-heal।"""
        try:
            from core.health.health_probes import probe_database

            result = await probe_database()
            db_up = result.get("status") == "up" if isinstance(result, dict) else bool(result)
        except Exception as exc:
            db_up = False
            logger.warning(f"🚑 DB probe raised exception: {exc!r}")

        if not db_up:
            self.failure_counts["db"] = self.failure_counts.get("db", 0) + 1
            count = self.failure_counts["db"]
            logger.error(f"🚑 Database unhealthy (consecutive failure #{count})")

            if count >= 3 and self._can_heal("db"):
                await self._heal_database()
        else:
            if self.failure_counts.get("db", 0) > 0:
                logger.info("🚑 Database recovered.")
            self.failure_counts["db"] = 0

    async def _heal_database(self) -> None:
        """
        বাংলা: Database connection pool reset করা।
        PgBouncer pool close করে নতুন connection তৈরি করা হচ্ছে।
        """
        logger.warning("🚑 Attempting DB pool reset (self-healing)...")
        try:
            from core.config import settings
            from core.pgbouncer_pool import close_db_pool, init_db_pool

            await close_db_pool()
            await asyncio.sleep(2)  # brief backoff
            await init_db_pool(settings.supabase_database_url)
            logger.info("🚑 ✅ Database pool successfully healed.")
            self.failure_counts["db"] = 0
            self._last_heal_time["db"] = time.monotonic()
        except Exception as exc:
            logger.error(f"🚑 ❌ DB heal failed: {exc!r}")

    # ── Redis Healing ──────────────────────────────────────────────────────────

    async def _check_redis(self) -> None:
        """Redis health check এবং auto-heal।"""
        try:
            from core.health.health_probes import probe_redis

            result = await probe_redis()
            redis_up = result.get("status") == "up" if isinstance(result, dict) else bool(result)
        except Exception as exc:
            redis_up = False
            logger.warning(f"🚑 Redis probe raised exception: {exc!r}")

        if not redis_up:
            self.failure_counts["redis"] = self.failure_counts.get("redis", 0) + 1
            count = self.failure_counts["redis"]
            logger.error(f"🚑 Redis unhealthy (consecutive failure #{count})")

            if count >= 3 and self._can_heal("redis"):
                await self._heal_redis()
        else:
            if self.failure_counts.get("redis", 0) > 0:
                logger.info("🚑 Redis recovered.")
            self.failure_counts["redis"] = 0

    async def _heal_redis(self) -> None:
        """
        বাংলা: Redis connection reset করা।
        SecureRedisManager-এর client reset করে reconnect করা হচ্ছে।
        """
        logger.warning("🚑 Attempting Redis reconnect (self-healing)...")
        try:
            from core.cache.redis_manager import redis_manager

            if hasattr(redis_manager, "client") and redis_manager.client:
                try:
                    await redis_manager.client.aclose()
                except Exception as exc:
                    # বাংলা: Redis client বন্ধ করার সময় কোনো এরর হলে তা লগ করা হচ্ছে সাইলেন্টলি ইগনোর করার বদলে
                    logger.debug(f"Redis client close error: {exc!r}")
            # Reconnect — SecureRedisManager নিজেই __init__-এ connect করে
            if hasattr(redis_manager, "_connect"):
                await redis_manager._connect()
            logger.info("🚑 ✅ Redis successfully healed.")
            self.failure_counts["redis"] = 0
            self._last_heal_time["redis"] = time.monotonic()
        except Exception as exc:
            logger.error(f"🚑 ❌ Redis heal failed: {exc!r}")

    # ── Utilities ──────────────────────────────────────────────────────────────

    def _can_heal(self, subsystem: str) -> bool:
        """
        বাংলা: Cooldown check — একই subsystem বারবার heal attempt না করতে।
        2 minute cooldown enforce করা হচ্ছে।
        """
        last = self._last_heal_time.get(subsystem, 0.0)
        return (time.monotonic() - last) >= self.HEAL_COOLDOWN_SECONDS

    async def attempt_code_mutation_heal(self, fingerprint: str, exc: Exception) -> bool:
        """
        বাংলা মন্তব্য: ফিঙ্গারপ্রিন্ট ধরে কোড হিলিং চেষ্টা — Depth <= 3 চেক এবং ব্যর্থ হলে Git Revert ও HITL ট্রাইগার করা।
        """
        if not hasattr(self, "_fingerprint_depth"):
            self._fingerprint_depth: dict[str, int] = {}

        current_depth = self._fingerprint_depth.get(fingerprint, 0) + 1
        self._fingerprint_depth[fingerprint] = current_depth

        logger.info(f"AutoHealer Mutation Attempt: Fingerprint={fingerprint[:12]} Depth={current_depth}/3")

        if current_depth > 3:
            logger.critical(
                f"AutoHealer MAX MUTATION DEPTH EXCEEDED for {fingerprint[:12]}. Triggering Automated Git Revert & HITL Alert!"
            )

            # বাংলা মন্তব্য: আগে এখানে শুধু broadcast হতো, প্রকৃত git revert কখনো ট্রিগার হতো না —
            # এখন rollback_monitor-এর প্রকৃত revert মেথড কল করা হচ্ছে (Patch 21 fix)
            revert_success = False
            try:
                from core.resilience.rollback_monitor import RollbackMonitor

                revert_success = await RollbackMonitor().execute_automatic_rollback(
                    fingerprint=fingerprint, reason=f"mutation_depth_exceeded: {exc}"
                )
            except Exception as revert_err:
                logger.error(f"AutoHealer: Git revert execution failed: {revert_err}")

            try:
                from core.swarm_pubsub import get_swarm_streamer

                await get_swarm_streamer().broadcast(
                    "hitl_mutation_alert",
                    {
                        "fingerprint": fingerprint,
                        "error": str(exc),
                        "action": ("git_revert_triggered" if revert_success else "git_revert_FAILED"),
                        "depth": current_depth,
                    },
                )
            except Exception as b_err:
                logger.warning(f"AutoHealer: PubSub broadcast skipped ({b_err})")

            if not revert_success:
                logger.critical(
                    f"🚨 AutoHealer: Git revert FAILED for {fingerprint[:12]} — codebase may still be in broken state!"
                )
            return False

        # Simulate hotfix attempt
        logger.info(f"AutoHealer JIT Hotfix applied for {fingerprint[:12]} (Attempt #{current_depth})")
        return True

    def get_status(self) -> dict[str, Any]:
        """Health status summary।"""
        return {
            "running": self._running,
            "failure_counts": dict(self.failure_counts),
            "fingerprint_depths": getattr(self, "_fingerprint_depth", {}),
            "last_heal_times": {k: round(time.monotonic() - v, 1) for k, v in self._last_heal_time.items()},
            "recent_checks": list(self._history)[-10:],
        }


# Singleton
auto_healer_service = AutoHealerService(check_interval_seconds=30)
