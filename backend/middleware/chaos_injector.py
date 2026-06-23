import asyncio
import random
import os
from fastapi import Request, HTTPException
from starlette.middleware.base import BaseHTTPMiddleware
from fastapi.responses import JSONResponse
from loguru import logger

class ChaosInjectorMiddleware(BaseHTTPMiddleware):
    """
    Enterprise Fault Injection & Chaos Engine.
    Simulates real-world network degradation, packet loss, and latency spikes.
    Active ONLY when LOCAL_CHAOS_MODE=true.
    """
    def __init__(self, app):
        super().__init__(app)
        self.chaos_enabled = os.getenv("LOCAL_CHAOS_MODE", "false").lower() == "true"
        # ক্যাওস প্যারামিটারস (প্রোডাকশন গ্রেড ফল্ট সিমুলেশন)
        self.packet_drop_rate = 0.20  # ২০% চান্স যে রিকোয়েস্ট মাঝপথে ড্রপ/ফেইল করবে
        self.max_latency_spike = 3.5  # সর্বোচ্চ ৩.৫ সেকেন্ড পর্যন্ত কৃত্রিম ডিলে

    async def dispatch(self, request: Request, call_next):
        if not self.chaos_enabled:
            return await call_next(request)

        # ১. কৃত্রিম ল্যাটেন্সি স্পাইক সিমুলেশন (Slow Network/API Gateway Latency)
        if random.random() < 0.30:  # ৩০% রিকোয়েস্টে নেটওয়ার্ক ল্যাগ তৈরি হবে
            delay = random.uniform(0.5, self.max_latency_spike)
            logger.warning(f"🔌 [CHAOS ENGINE] Injecting artificial network lag: {delay:.2f}s on {request.url.path}")
            await asyncio.sleep(delay)

        # ২. কৃত্রিম প্যাকেট ড্রপ/কানেকশন ফেইলর সিমুলেশন (Packet Loss / Upstream Outage)
        if random.random() < self.packet_drop_rate:
            logger.critical(f"💥 [CHAOS ENGINE] Simulated Packet Drop! Severing connection for {request.url.path}")
            return JSONResponse(
                status_code=504,
                content={
                    "title": "Gateway Timeout (Chaos Simulated)",
                    "detail": "Upstream connection dropped due to artificial network degradation.",
                    "instance": request.url.path
                }
            )

        # ৩. যদি রিকোয়েস্ট ক্যাওস ফিল্টার সারভাইভ করে, তবে নরমাল এক্সিকিউশন হবে
        return await call_next(request)
