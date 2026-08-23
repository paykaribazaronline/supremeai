"""
Web scraper with HTTP fallback (extracted from backend/tools/browser/web_scraper.py).
Uses httpx + BeautifulSoup for lightweight HTTP fetching when Playwright
is unavailable or overkill.
"""

from typing import Any

import httpx
from bs4 import BeautifulSoup
from loguru import logger

from security import is_safe_url
import os

# 🔧 DYNAMIC CONFIG: User-Agent from env, with safe default
_DEFAULT_USER_AGENT = os.getenv(
    "SCRAPER_USER_AGENT",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
)
_HTTP_TIMEOUT = float(os.getenv("SCRAPER_HTTP_TIMEOUT", "15.0"))  # 🔧 DYNAMIC timeout


class WebScraper:
    def fetch_page(self, url: str) -> dict[str, Any]:
        logger.info(f"Fetching page: {url}")
        if not is_safe_url(url):
            logger.error(f"SSRF Attempt Blocked: {url}")
            return {
                "success": False,
                "error": "SSRF check failed: Unauthorized internal access",
                "url": url,
            }
        try:
            headers = {"User-Agent": _DEFAULT_USER_AGENT}  # 🔧 DYNAMIC
            response = httpx.get(url, headers=headers, timeout=_HTTP_TIMEOUT, follow_redirects=True)
            response.raise_for_status()
            soup = BeautifulSoup(response.text, "html.parser")
            title = soup.title.get_text(strip=True) if soup.title else "No Title"
            for tag in soup(["script", "style", "nav", "footer", "header"]):
                tag.decompose()
            text = " ".join(soup.get_text(separator=" ").split())[:3000]
            links = [a.get("href", "") for a in soup.find_all("a", href=True)][:20]
            return {
                "success": True,
                "url": url,
                "title": title,
                "content": text,
                "links": links,
                "status_code": response.status_code,
            }
        except (httpx.RequestError, httpx.HTTPStatusError) as e:
            logger.error(f"Failed to fetch {url}: {e}")
            return {"success": False, "error": str(e), "url": url}
