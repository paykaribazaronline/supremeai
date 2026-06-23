import asyncio
import httpx
from bs4 import BeautifulSoup
from loguru import logger
from typing import Dict, Any, Optional
import ipaddress
import socket
from urllib.parse import urlparse
from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel

from backend.api.routes.admin_dashboard import require_admin_token

router = APIRouter(prefix="/browser", tags=["browser-agent"])

def is_safe_url(url: str) -> bool:
    try:
        parsed = urlparse(url)
        hostname = parsed.hostname
        if not hostname: return False
        if hostname == "169.254.169.254" or hostname.endswith(".local"): return False
        ip = socket.gethostbyname(hostname)
        ip_obj = ipaddress.ip_address(ip)
        if ip_obj.is_private or ip_obj.is_loopback or ip_obj.is_link_local: return False
        return True
    except Exception:
        return False
# Global tracking references for runtime execution
try:
    from playwright.async_api import async_playwright, Browser, Playwright
except ImportError:
    Browser = Any
    Playwright = Any
    async_playwright = None

_playwright_runner: Optional[Playwright] = None
_global_browser: Optional[Browser] = None

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
            args=["--no-sandbox", "--disable-setuid-sandbox", "--disable-dev-shm-usage"]
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
    except Exception as e:
        logger.critical(f"❌ Error during global browser termination sequence: {str(e)}")
    finally:
        _global_browser = None
        _playwright_runner = None


class BrowseRequest(BaseModel):
    url: str
    action: Optional[str] = "fetch"   # fetch | click | screenshot | scroll | type
    selector: Optional[str] = None
    text: Optional[str] = None
    wait_for: Optional[str] = None    # CSS selector to wait for


class BrowserAgent:
    """Controls browser actions — httpx (fast) + Playwright (full JS)."""

    def __init__(self):
        self._pw_browser = None
        logger.info("Initialized BrowserAgent")

    # ── Simple fetch (no JS needed) ────────────────────────────────
    def fetch_page(self, url: str) -> Dict[str, Any]:
        logger.info(f"Fetching page: {url}")
        if not is_safe_url(url):
            logger.error(f"SSRF Attempt Blocked: {url}")
            return {"success": False, "error": "SSRF check failed: Unauthorized internal access", "url": url}
        try:
            headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"}
            response = httpx.get(url, headers=headers, timeout=15.0, follow_redirects=True)
            response.raise_for_status()
            soup = BeautifulSoup(response.text, "html.parser")
            title = soup.title.string.strip() if soup.title else "No Title"
            for tag in soup(["script", "style", "nav", "footer", "header"]):
                tag.decompose()
            text = " ".join(soup.get_text(separator=" ").split())[:3000]
            links = [a.get("href", "") for a in soup.find_all("a", href=True)][:20]
            return {
                "success": True, "url": url, "title": title,
                "content": text, "links": links,
                "status_code": response.status_code,
            }
        except Exception as e:
            logger.error(f"Failed to fetch {url}: {e}")
            return {"success": False, "error": str(e), "url": url}

    # ── Playwright (JS-heavy pages) ────────────────────────────────
    async def _get_playwright(self):
        # Delegate to the global browser singleton
        return await get_global_browser()

    async def navigate_and_interact(
        self, url: str,
        action: str = "fetch",
        selector: Optional[str] = None,
        text: Optional[str] = None,
        wait_for: Optional[str] = None,
    ) -> Dict[str, Any]:
        if not is_safe_url(url):
            logger.error(f"SSRF Attempt Blocked: {url}")
            return {"success": False, "error": "SSRF check failed: Unauthorized internal access", "url": url}

        browser = await self._get_playwright()
        if not browser:
            # Fallback to httpx
            return self.fetch_page(url)

        page = await browser.new_page()
        try:
            await page.goto(url, wait_until="domcontentloaded", timeout=30000)

            if wait_for:
                await page.wait_for_selector(wait_for, timeout=10000)

            if action == "click" and selector:
                await page.click(selector)
                await page.wait_for_load_state("networkidle", timeout=10000)

            elif action == "type" and selector and text:
                await page.fill(selector, text)

            elif action == "scroll":
                await page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
                await asyncio.sleep(1)

            elif action == "screenshot":
                screenshot = await page.screenshot(type="png")
                import base64
                b64 = base64.b64encode(screenshot).decode()
                title = await page.title()
                return {"success": True, "url": url, "title": title, "screenshot_base64": b64}

            title = await page.title()
            content_html = await page.content()
            soup = BeautifulSoup(content_html, "html.parser")
            for tag in soup(["script", "style"]):
                tag.decompose()
            text_content = " ".join(soup.get_text(separator=" ").split())[:3000]
            links = [a.get("href", "") for a in soup.find_all("a", href=True)][:20]
            current_url = page.url

            return {
                "success": True, "url": current_url, "title": title,
                "content": text_content, "links": links, "action": action,
            }
        except Exception as e:
            logger.error(f"Playwright action failed: {e}")
            return {"success": False, "error": str(e), "url": url}
        finally:
            await page.close()

    async def extract_data(self, url: str, extraction_prompt: str) -> Dict[str, Any]:
        """Fetch page and use AI to extract structured data."""
        page_data = self.fetch_page(url)
        if not page_data["success"]:
            return page_data

        try:
            from brain.model_router import ModelRouter
            router = ModelRouter()
            prompt = (
                f"Extract the following from this web page content:\n{extraction_prompt}\n\n"
                f"Page Title: {page_data.get('title')}\n"
                f"Content: {page_data.get('content', '')[:2000]}\n\n"
                "Return a clean JSON object with the extracted data."
            )
            result = await router.async_route_and_generate(prompt, task_type="reasoning", max_cost=0.02)
            extracted = result.get("text", "") if isinstance(result, dict) else ""
            return {"success": True, "url": url, "extracted": extracted, "raw": page_data}
        except Exception as e:
            return {"success": False, "error": str(e)}


_agent = BrowserAgent()


@router.post("/browse", dependencies=[Depends(require_admin_token)])
async def browse(request: BrowseRequest):
    """Navigate to a URL and perform browser actions (Admin Only)."""
    if request.action in ("click", "type", "scroll", "screenshot"):
        return await _agent.navigate_and_interact(
            url=request.url,
            action=request.action,
            selector=request.selector,
            text=request.text,
            wait_for=request.wait_for,
        )
    return _agent.fetch_page(request.url)


@router.post("/extract", dependencies=[Depends(require_admin_token)])
async def extract(url: str, extraction_prompt: str):
    """Fetch page and extract structured data with AI (Admin Only)."""
    return await _agent.extract_data(url, extraction_prompt)
