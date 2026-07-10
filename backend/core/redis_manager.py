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
            
    async def close(self):
        """FastAPI lifespan শাটডাউন সিকোয়েন্সের সাথে সামঞ্জস্য রাখার জন্য গেটওয়ে ক্লোজার।"""
        if self.client:
            await self.client.aclose()
            logger.info("💀 Upstash Async connection wrapper gracefully terminated.")
