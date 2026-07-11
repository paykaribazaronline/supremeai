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
        import sys

        current_module = sys.modules.get(__name__, sys.modules.get("core.playwright_manager"))
        current_async_playwright = getattr(current_module, "async_playwright", async_playwright) if current_module else async_playwright
        if not callable(current_async_playwright):
            raise RuntimeError("Playwright is not installed.")
        _playwright_runner = await current_async_playwright().start()
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
    try:
        if _global_browser:
            await _global_browser.close()
    except Exception as e:  # noqa: BLE001
        logger.error(f"Error closing global browser: {e}")

    try:
        if _playwright_runner:
            await _playwright_runner.stop()
    except Exception as e:  # noqa: BLE001
        logger.error(f"Error stopping global playwright runner: {e}")
    finally:
        _global_browser = None
        _playwright_runner = None
