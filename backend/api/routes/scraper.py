"""
SupremeAI Scraper Routes

Exposes browser automation + web scraping as integrated endpoints.
"""

from __future__ import annotations

import os

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from services.scraper.browser_agent import BrowserAgent, BrowseRequest
from services.scraper.security import is_safe_url
from services.scraper.web_scraper import WebScraper

MAX_CONCURRENCY = int(os.getenv("SCRAPER_MAX_CONCURRENCY", "3"))
TIMEOUT_SECONDS = int(os.getenv("SCRAPER_TIMEOUT_SECONDS", "45"))

router = APIRouter(tags=["scraper"])

_scraper = WebScraper()
_agent = BrowserAgent(headless=True)


class ScrapeRequest(BaseModel):
    url: str
    extraction_prompt: str | None = None


@router.get("/health")
async def health_check():
    try:
        import playwright.async_api as _pw
        playwright_ok = callable(getattr(_pw, "async_playwright", None))
    except ImportError:
        playwright_ok = False

    return {
        "status": "healthy",
        "service": "supremeai-scraper-module",
        "max_concurrency": MAX_CONCURRENCY,
        "timeout_seconds": TIMEOUT_SECONDS,
        "playwright_available": playwright_ok,
    }


@router.post("/scrape")
async def scrape(request: ScrapeRequest):
    if not request.url:
        raise HTTPException(status_code=400, detail="URL is required")
    result = _scraper.fetch_page(request.url)
    return result


@router.post("/browse")
async def browse(request: BrowseRequest):
    if not request.url:
        raise HTTPException(status_code=400, detail="URL is required")
    result = await _agent.navigate_and_interact(
        url=request.url,
        action=request.action or "fetch",
        selector=request.selector,
        text=request.text,
        wait_for=request.wait_for,
    )
    return result


class RecipeRequest(BaseModel):
    steps: list = []
    initial_url: str | None = None


@router.post("/recipe")
async def recipe(request: RecipeRequest):
    if request.initial_url and not is_safe_url(request.initial_url):
        raise HTTPException(status_code=400, detail="SSRF check failed: Unauthorized internal access")
    result = await _agent.execute_recipe(steps=request.steps, initial_url=request.initial_url)
    return result
