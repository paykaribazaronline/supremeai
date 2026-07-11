# 📄 ফাইল: backend/core/playwright_manager.py

**প্রকার:** .py  
**সাইজ:** 2,324 বাইট  
**আপডেট:** 2026-07-11T13:28:08.947911

---

## কোড

```py
from typing import Any

from loguru import logger


try:
    from playwright.async_api import Browser
    from playwright.async_api import Playwright
    from playwright.async_api import async_playwright
except ImportError:
    Browser = Any
    Playwright = Any
    async_playwright = None

_playwright_runner: Playwright | None = None
_global_browser: Browser | None = None


async def get_global_browser() -> Browser:
    """গ্লোবাল ব্রাউজার ইনস্ট্যান্স রিটার্ন করে (Lazy Initialization Pattern)"""
    global _playwright_runner, _global_browser
    if _global_browser is None:
        logger.info("🚀 Starting a new headless Global Chromium instance...")
        if async_playwright is None:
            raise RuntimeError("Playwright is not installed.")
        _playwright_runner = await async_playwright().start()
        _global_browser = await _playwright_runner.chromium.launch(
            headless=True,
            args=[
                "--no-sandbox",
                "--disable-setuid-sandbox",
                "--disable-dev-shm-usage",
            ],
        )
    return _global_browser


async def shutdown_global_browser():
    """Lifespan Hook দ্বারা কল করা হবে - কন্টেইনার শাটডাউনের সময় জম্বি প্রসেস ক্লিন করে"""
    global _playwright_runner, _global_browser
    logger.info("🛡️ Initiating Playwright Global Lifespan Cleanup...")
    try:
        if _global_browser:
            logger.info("Closing active global Chromium engine...")
            await _global_browser.close()
        if _playwright_runner:
            logger.info("Stopping playwright runner core context...")
            await _playwright_runner.stop()
        logger.info("✅ All Playwright OS processes terminated cleanly.")
    except (RuntimeError, OSError, ConnectionError, Exception) as e:  # noqa: BLE001
        # প্লে-রাইট শাটডাউন করার সময় যেকোনো ধরনের ত্রুটি লগ করা হলো
        logger.critical(f"❌ Error during global browser termination sequence: {str(e)}")
    finally:
        _global_browser = None
        _playwright_runner = None

```