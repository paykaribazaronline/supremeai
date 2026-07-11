# 📄 ফাইল: scripts/health_check/auto_health_check.py

**প্রকার:** .py  
**সাইজ:** 1,196 বাইট  
**আপডেট:** 2026-07-11T19:26:12.031842

---

## কোড

```py
import os
import sys
import httpx
import logging
from redis import Redis
from loguru import logger

# Configure logging
logging.basicConfig(level=logging.INFO)

API_URL = os.getenv("API_URL", "http://127.0.0.1:8000")
REDIS_URL = os.getenv("REDIS_URL", "redis://localhost:6379")

def check_api():
    try:
        response = httpx.get(f"{API_URL}/api/v1/health", timeout=5.0)
        response.raise_for_status()
        logger.info(f"✅ API Health Check Passed: {API_URL}")
        return True
    except Exception as e:
        logger.error(f"❌ API Health Check Failed: {e}")
        return False

def check_redis():
    try:
        client = Redis.from_url(REDIS_URL, socket_timeout=3.0)
        client.ping()
        logger.info(f"✅ Redis Health Check Passed: {REDIS_URL}")
        return True
    except Exception as e:
        logger.error(f"❌ Redis Health Check Failed: {e}")
        return False

if __name__ == "__main__":
    logger.info("Starting Auto Health Check...")
    api_ok = check_api()
    # redis_ok = check_redis() # Optional if redis is not running locally in CI
    
    if not api_ok:
        sys.exit(1)
    logger.info("All Health Checks Passed.")
    sys.exit(0)

```