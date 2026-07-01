# বাংলা কমেন্ট: সুপ্রিম-এআই এর কোর অ্যাসিঙ্ক রেডিস এবং ফেল-ক্লোজড রেট-লিমিটিং ইঞ্জিন।
# রেডিস ডাউন থাকলে এটি কোনো সিকিউরিটি গেট বাইপাস করতে দেবে না (Fail-Closed)।

import redis.asyncio as aioredis
from fastapi import HTTPException
from fastapi import status

from core.config import settings
from core.logging_config import logger


class SecureRedisManager:
    def __init__(self):
        # বাংলা কমেন্ট: আপস্ট্যাশ বা ক্লাউড রেডিস ইউআরএল লোড করা হচ্ছে।
        self.redis_url = settings.redis_url
        self.client = None

    async def initialize(self):
        """অ্যাসিঙ্ক রেডিস কানেকশন পুল ইনিশিয়েট করার ফাংশন।"""
        if not self.redis_url:
            logger.critical("🔥 CRITICAL: REDIS_URL missing in configurations! System entering Fail-Closed state.")
            self.client = None
            return
        try:
            # P1 ফিক্স: সিনক্রোনাস ক্লায়েন্ট সরিয়ে পিওর অ্যাসিঙ্ক কানেকশন পুল ব্যবহার।
            self.client = aioredis.from_url(
                self.redis_url, 
                encoding="utf-8", 
                decode_responses=True,
                socket_timeout=2.0,  # ২ সেকেন্ডের বেশি লেটেন্সি হলে ড্রপ
                socket_connect_timeout=2.0
            )
            logger.success("🚀 Async Redis Client successfully connected with connection pool.")
        except Exception as e:
            logger.critical(f"🔥 Fail-Closed Triggered: Redis connection failed during init -> {str(e)}")
            self.client = None

    async def is_rate_limited(self, key: str, max_requests: int, window_seconds: int) -> bool:
        """
        সম্পূর্ণ ফেল-ক্লোজড রেট লিমিটিং চেকার। রেডিস ডাউন থাকলে রিকোয়েস্ট সরাসরি ব্লকড হবে।
        """
        # বাংলা কমেন্ট: Fail-Closed মেকানিজম এনফোর্সমেন্ট। 
        # রেডিস ক্লায়েন্ট যদি নাল বা ডাউন থাকে, তবে ট্রাফিকের সিকিউরিটি রক্ষার্থে রিকোয়েস্ট রিজেক্ট করা হবে।
        if self.client is None:
            logger.critical(f"🔒 Fail-Closed Active: Request blocked for key {key} because Redis engine is offline.")
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Security infrastructure unavailable. Request safely denied."
            )
            
        try:
            # পিওর অ্যাসিঙ্ক পাইপলাইনিং এবং অ্যাটমিক ইনক্রিমেন্ট অপারেশন (০% গ্যাপ)
            async with self.client.pipeline(transaction=True) as pipe:
                await pipe.incr(key)
                await pipe.expire(key, window_seconds)
                current_requests, _ = await pipe.execute()
                
            if current_requests > max_requests:
                logger.warning(f"🚨 Rate Limit Triggered for Key: {key}. Total: {current_requests}/{max_requests}")
                return True  # লিমিট ক্রস করেছে
                
            return False  # নিরাপদ
            
        except aioredis.RedisError as redis_err:
            # ❌ ওল্ড ভুল পদ্ধতি (Fail-Open): logger.error(e); return False (যা হ্যাকারদের এন্ট্রি দিত)
            # ✅ নিউ সঠিক পদ্ধতি: Fail-Closed এনফোর্সমেন্ট। এক্সেপশন এলে রিকোয়েস্ট হার্ড-ব্লক।
            logger.critical(f"🔥 SECURITY EMERGENCY: Redis operational failure during rate check -> {str(redis_err)}")
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Authentication and rate verification engine failure. Access blocked."
            ) from redis_err

# গ্লোবাল সিঙ্গেলটন ইনস্ট্যান্স জেনারেশন
redis_manager = SecureRedisManager()
