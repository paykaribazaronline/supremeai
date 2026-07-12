import os
import json
from redis import asyncio as aioredis
from loguru import logger

class SecureRedisManager:
    def __init__(self):
        self.url = os.getenv("REDIS_URL")
        if self.url:
            self.client = aioredis.from_url(self.url, decode_responses=True)
        else:
            self.client = None

        if self.client:
            logger.info("⚡ Serverless Upstash Redis REST Provider Active.")
        else:
            logger.critical("🔥 CRITICAL: Serverless Redis Endpoint Missing! System entering Fail-Closed state.")

    async def set_cache(self, key: str, value: str, ex_seconds: int = 3600) -> bool:
        """Native Redis API এর মাধ্যমে কি-ভ্যালু পেয়ার সেভ করার মেথড।"""
        if not self.client:
            return False
        try:
            await self.client.set(key, value, ex=ex_seconds)
            return True
        except Exception as exc:  # noqa: BLE001
            logger.error(f"❌ Redis Cache Write Operation Failed for {key}: {exc}")
            return False

    async def get_cache(self, key: str) -> str | None:
        """Native Redis API এর মাধ্যমে ডাটা রিড করার মেথড।"""
        if not self.client:
            return None
        try:
            return await self.client.get(key)
        except Exception as exc:  # noqa: BLE001
            logger.error(f"❌ Upstash Cache Read Operation Failed for {key}: {exc}")
            return None

    async def set_agent_heartbeat(self, agent_id: str, status: str, latency_ms: int, ttl: int = 5) -> bool:
        """এজেন্ট হার্টবিট সেট করার মেথড।"""
        if not self.client:
            return False

        key = f"health:{agent_id}"
        value = json.dumps({"status": status, "latency": latency_ms})
        return await self.set_cache(key, value, ex_seconds=ttl)

    async def get_agents_health(self, agent_ids: list[str]) -> dict:
        """একাধিক এজেন্টের হেলথ স্ট্যাটাস একসাথে MGET দিয়ে ফেচ করে।"""
        if not self.client or not agent_ids:
            return {}

        keys = [f"health:{agent_id}" for agent_id in agent_ids]
        try:
            res_data = await self.client.mget(keys)
            health_data = {}
            for agent_id, raw_val in zip(agent_ids, res_data, strict=False):
                if raw_val:
                    try:
                        health_data[agent_id] = json.loads(raw_val) if isinstance(raw_val, str) else raw_val
                    except json.JSONDecodeError:
                        health_data[agent_id] = {"status": "dead", "latency": 0}
                else:
                    health_data[agent_id] = {"status": "dead", "latency": 0}
            return health_data
        except Exception as exc:  # noqa: BLE001
            logger.error(f"❌ Redis MGET Failed for health check: {exc}")
            return {}

    async def close(self):
        if self.client:
            await self.client.aclose()
            logger.info("💀 Redis Async connection gracefully terminated.")


# Create the singleton instance
redis_manager = SecureRedisManager()
