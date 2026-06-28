import asyncio
import json
import os
import random
import string
import time
from pathlib import Path
from typing import Any

from loguru import logger

HAS_PLAYWRIGHT = True
try:  # pragma: no cover - optional dependency
    from playwright.async_api import BrowserContext, Page, async_playwright
except ImportError:  # pragma: no cover - optional dependency
    HAS_PLAYWRIGHT = False


class BrowserStealth:
    def __init__(self) -> None:
        self.playwright = None
        self.context: BrowserContext | None = None

    async def create_stealth_browser(self) -> Any:
        if not HAS_PLAYWRIGHT:
            raise RuntimeError("playwright not installed")
        self.playwright = await async_playwright().start()
        browser = await self.playwright.chromium.launch(headless=os.getenv("BROWSER_HEADLESS", "true").lower() != "false")
        args = [
            "--no-sandbox",
            "--disable-setuid-sandbox",
            "--disable-plugins-discovery",
            "--disable-default-apps",
            "--disable-prediction-service",
            "--disable-component-update",
            "--disable-popup-blocking",
        ]
        self.context = await browser.new_context(
            user_agent=os.getenv(
                "STEALTH_USER_AGENT",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            ),
            locale="en-US",
            java_script_enabled=True,
            bypass_csp=True,
            extra_http_headers={
                "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                "Accept-Language": "en-US,en;q=0.9",
                "Upgrade-Insecure-Requests": "1",
            },
        )
        await self.context.route("**/*.{png,jpg,jpeg,gif,svg,woff,woff2}", lambda route: route.abort())
        await self.context.add_init_script(
            """
            () => {
              Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3, 4, 5]});
              Object.defineProperty(navigator, 'languages', {get: () => ['en-US', 'en']});
              Object.defineProperty(navigator, 'webdriver', {get: () => undefined});
              window.chrome = window.chrome || {runtime: {}};
            }
            """
        )
        return self.context

    async def simulate_human_behavior(self, page: Page) -> None:
        try:
            for _ in range(random.randint(1, 3)):
                await page.mouse.move(random.randint(0, 400), random.randint(0, 400), steps=random.randint(3, 6))
                await asyncio.sleep(random.uniform(0.3, 1.2))
                await page.mouse.wheel(0, random.randint(-120, 120))
                await page.keyboard.press(random.choice(["Space", "PageDown", "End"]))
            if random.random() > 0.6:
                await page.mouse.click(random.randint(50, 300), random.randint(80, 300), delay=random.randint(80, 220))
        except Exception as exc:
            logger.debug(f"Human behavior simulation skipped: {exc}")

    async def safe_screenshot(self, page: Page, path: str | None = None) -> str | None:
        try:
            target = path or f"data/artifacts/screenshot_{int(time.time())}_{''.join(random.choices(string.ascii_lowercase + string.digits, k=6))}.png"
            Path("data/artifacts").mkdir(parents=True, exist_ok=True)
            await page.screenshot(path=target, full_page=True)
            return target
        except Exception as exc:
            logger.debug(f"screenshot failed: {exc}")
            return None

    async def close(self) -> None:
        try:
            if self.context:
                await self.context.close()
            if self.playwright:
                await self.playwright.stop()
        except Exception:
            pass
