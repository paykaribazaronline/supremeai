# 📄 ফাইল: backend/core/redis_manager.py

**প্রকার:** .py  
**সাইজ:** 4,346 বাইট  
**আপডেট:** 2026-07-11T13:38:55.658911

---

## কোড

```py
# backend/core/redis_manager.py
import os

import httpx
from loguru import logger


class SecureRedisManager:
    def __init__(self):
        # .env বা এনভায়রনমেন্ট থেকে Upstash ক্রেডেনশিয়াল ফেচ করা হচ্ছে
        self.url = os.getenv("UPSTASH_REDIS_REST_URL")
        self.token = os.getenv("UPSTASH_REDIS_REST_TOKEN")
        self.headers = {"Authorization": f"Bearer {self.token}"} if self.token else {}
        self.client = httpx.AsyncClient(base_url=self.url, headers=self.headers) if self.url else None

        if self.client:
            logger.info("⚡ Serverless Upstash Redis REST Provider Active.")
        else:
            logger.critical("🔥 CRITICAL: Serverless Redis Endpoint Missing! System entering Fail-Closed state.")

    async def set_cache(self, key: str, value: str, ex_seconds: int = 3600) -> bool:
        """Upstash REST API এর মাধ্যমে কি-ভ্যালু পেয়ার সেভ করার মেথড।"""
        if not self.client:
            return False
        try:
            # Upstash REST কমান্ড স্ট্রাকচার: ["SET", key, value, "EX", seconds]
            payload = ["SET", key, value, "EX", str(ex_seconds)]
            response = await self.client.post("/", json=payload)
            return response.status_code == 200
        except Exception as exc:  # noqa: BLE001
            logger.error(f"❌ Upstash Cache Write Operation Failed for {key}: {exc}")
            return False

    async def get_cache(self, key: str) -> str | None:
        """Upstash REST API এর মাধ্যমে ডাটা রিড করার মেthod।"""
        if not self.client:
            return None
        try:
            payload = ["GET", key]
            response = await self.client.post("/", json=payload)
            if response.status_code == 200:
                res_data = response.json()
                return res_data.get("result")
            return None
        except Exception as exc:  # noqa: BLE001
            logger.error(f"❌ Upstash Cache Read Operation Failed for {key}: {exc}")
            return None

    async def set_agent_heartbeat(self, agent_id: str, status: str, latency_ms: int, ttl: int = 5) -> bool:
        """এজেন্ট হার্টবিট সেট করার মেথড।"""
        if not self.client:
            return False
        import json

        key = f"health:{agent_id}"
        value = json.dumps({"status": status, "latency": latency_ms})
        return await self.set_cache(key, value, ex_seconds=ttl)

    async def get_agents_health(self, agent_ids: list[str]) -> dict:
        """একাধিক এজেন্টের হেলথ স্ট্যাটাস একসাথে MGET দিয়ে ফেচ করে।"""
        if not self.client or not agent_ids:
            return {}
        import json

        keys = [f"health:{agent_id}" for agent_id in agent_ids]
        payload = ["MGET"] + keys
        try:
            response = await self.client.post("/", json=payload)
            if response.status_code == 200:
                res_data = response.json().get("result", [])
                health_data = {}
                for agent_id, raw_val in zip(agent_ids, res_data, strict=False):
                    if raw_val:
                        try:
                            # Handle both stringified json and already parsed dict from Upstash
                            health_data[agent_id] = json.loads(raw_val) if isinstance(raw_val, str) else raw_val
                        except json.JSONDecodeError:
                            health_data[agent_id] = {"status": "dead", "latency": 0}
                    else:
                        health_data[agent_id] = {"status": "dead", "latency": 0}
                return health_data
            return {}
        except Exception as exc:  # noqa: BLE001
            logger.error(f"❌ Upstash MGET Failed for health check: {exc}")
            return {}

    async def close(self):
        if self.client:
            await self.client.aclose()
            logger.info("💀 Upstash Async connection wrapper gracefully terminated.")


# Create the singleton instance
redis_manager = SecureRedisManager()

```