import asyncio
import random
from typing import Any

from loguru import logger

try:
    from playwright.async_api import BrowserContext, Page, async_playwright
    from playwright_stealth import stealth_async

    HAS_PLAYWRIGHT = True
except ImportError:
    HAS_PLAYWRIGHT = False


class BrowserStealth:
    def __init__(self) -> None:
        self.playwright = None
        self.context: BrowserContext | None = None

    async def create_stealth_browser(self) -> Any:
        if not HAS_PLAYWRIGHT:
            raise RuntimeError("playwright not installed")
        self.playwright = await async_playwright().start()
        browser = await self.playwright.chromium.launch(headless=True)
        self.context = await browser.new_context()
        return self.context

    async def simulate_human_behavior(self, page: Page) -> None:
        try:
            await stealth_async(page)
            await page.mouse.move(random.randint(0, 400), random.randint(0, 400))
            await asyncio.sleep(random.uniform(0.3, 1.2))
            await page.mouse.wheel(0, random.randint(-120, 120))
        except Exception as exc:
            logger.debug(f"Human behavior simulation skipped: {exc}")
