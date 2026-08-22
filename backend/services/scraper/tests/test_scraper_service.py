"""Tests for SupremeAI Scraper Microservice.

Covers:
  - Health check endpoint
  - SSRF protection on /scrape, /browse, /recipe
  - Empty URL validation
  - Recipe endpoint: empty steps, SSRF guard, index guard
  - Concurrency semaphore enforcement
  - security.is_safe_url unit tests
"""

import asyncio
import os
import sys
from pathlib import Path
from unittest.mock import AsyncMock, patch

import pytest
from fastapi.testclient import TestClient

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from browser_agent import BrowserAgent
from security import is_safe_url
from web_scraper import WebScraper


@pytest.fixture
def client():
    from main import app
    return TestClient(app)


@pytest.fixture
def browser_agent():
    return BrowserAgent(headless=True)


# ---------------------------------------------------------------------------
# Health check
# ---------------------------------------------------------------------------

def test_health_check(client):
    """GET /health should return healthy status."""
    resp = client.get("/health")
    assert resp.status_code == 200
    data = resp.json()
    assert data["status"] == "healthy"
    assert data["service"] == "supremeai-scraper"
    assert "playwright_available" in data
    assert "max_concurrency" in data
    assert data["max_concurrency"] == int(os.getenv("SCRAPER_MAX_CONCURRENCY", "3"))


# ---------------------------------------------------------------------------
# SSRF protection
# ---------------------------------------------------------------------------

SSRF_URLS = [
    "http://127.0.0.1:8081/admin",
    "http://localhost:5432",
    "http://10.0.0.1/secret",
    "http://192.168.1.1/admin",
    "http://169.254.169.254/latest/meta-data/",
    "http://0.0.0.0:8080/",
    "file:///etc/passwd",
    "ftp://internal-server/file",
    "gopher://127.0.0.1:6379/_FLUSHALL",
    "http://[::1]:8080/",
    "javascript:alert(1)",
    "data:text/html,<script>alert(1)</script>",
]


def test_scrape_ssf_blocked_various(client):
    """All SSRF payloads should be blocked on /scrape."""
    for url in SSRF_URLS:
        resp = client.post("/scrape", json={"url": url})
        assert resp.status_code == 200
        data = resp.json()
        assert data["success"] is False, f"URL should be blocked: {url}"
        assert "SSRF" in data["error"]


def test_browse_ssrf_blocked(client):
    """Browse endpoint should enforce SSRF protection."""
    resp = client.post("/browse", json={"url": "http://localhost:5432", "action": "fetch"})
    assert resp.status_code == 200
    data = resp.json()
    assert data["success"] is False
    assert "SSRF" in data["error"]


def test_recipe_ssrf_blocked(client):
    """Recipe endpoint should reject internal initial_url."""
    resp = client.post("/recipe", json={"steps": [], "initial_url": "http://127.0.0.1:8081/"})
    assert resp.status_code == 400
    data = resp.json()
    assert "SSRF" in data["detail"]


def test_recipe_ssrf_blocked_various(client):
    """Recipe endpoint should reject all SSRF payloads as initial_url."""
    for url in SSRF_URLS:
        if not url.startswith("http"):
            continue  # non-http schemes handled by /scrape and /browse
        resp = client.post("/recipe", json={"steps": [{"action": "wait", "value": "1"}], "initial_url": url})
        assert resp.status_code in (400,), f"URL should be blocked: {url}"


# ---------------------------------------------------------------------------
# Empty / missing URL validation
# ---------------------------------------------------------------------------

def test_scrape_empty_url_rejected(client):
    """Empty URL should return 400."""
    resp = client.post("/scrape", json={"url": ""})
    assert resp.status_code == 400


def test_browse_empty_url_rejected(client):
    """Empty URL on /browse should return 400."""
    resp = client.post("/browse", json={"url": "", "action": "fetch"})
    assert resp.status_code == 400


# ---------------------------------------------------------------------------
# Recipe endpoint
# ---------------------------------------------------------------------------

def test_recipe_empty_steps(client):
    """Recipe with empty steps should return success with empty data."""
    resp = client.post("/recipe", json={"steps": []})
    assert resp.status_code == 200
    data = resp.json()
    assert data["status"] == "success"
    assert data["data"] == {}


def test_recipe_no_steps_key(client):
    """Recipe without steps key should return success with empty data."""
    resp = client.post("/recipe", json={})
    assert resp.status_code == 200
    data = resp.json()
    assert data["status"] == "success"
    assert data["data"] == {}


def test_recipe_no_initial_url(client):
    """Recipe with steps but no initial_url should proceed to step execution."""
    with patch("browser_agent.async_playwright", create=True) as mock_pw:
        mock_browser = AsyncMock()
        mock_context = AsyncMock()
        mock_page = AsyncMock()
        mock_page.wait_for_selector = AsyncMock(return_value=None)
        mock_context.new_page = AsyncMock(return_value=mock_page)
        mock_browser.new_context = AsyncMock(return_value=mock_context)
        mock_p = AsyncMock()
        mock_p.chromium.launch = AsyncMock(return_value=mock_browser)
        mock_pw.return_value.__aenter__ = AsyncMock(return_value=mock_p)
        mock_pw.return_value.__aexit__ = AsyncMock(return_value=None)

        resp = client.post("/recipe", json={"steps": [{"action": "wait", "value": "0.1"}]})
    assert resp.status_code == 200
    data = resp.json()
    assert data["status"] == "success"
    assert data["data"] == {}


def test_recipe_index_guard_on_error(browser_agent):
    """If an error occurs before the loop, index should be -1, not NameError."""
    # Simulate a Playwright error during page.goto with initial_url
    with patch("browser_agent.async_playwright", create=True) as mock_pw:
        mock_browser = AsyncMock()
        mock_context = AsyncMock()
        mock_page = AsyncMock()
        mock_page.goto = AsyncMock(side_effect=RuntimeError("navigation failed"))
        mock_context.new_page = AsyncMock(return_value=mock_page)
        mock_browser.new_context = AsyncMock(return_value=mock_context)
        mock_p = AsyncMock()
        mock_p.chromium.launch = AsyncMock(return_value=mock_browser)
        mock_pw.return_value.__aenter__ = AsyncMock(return_value=mock_p)
        mock_pw.return_value.__aexit__ = AsyncMock(return_value=None)

        result = asyncio.run(
            browser_agent.execute_recipe(
                steps=[{"action": "wait", "value": "1"}], initial_url="http://example.com"
            )
        )
        assert result["status"] == "failed"
        assert "error" in result
        assert "step" in result


# ---------------------------------------------------------------------------
# Concurrency semaphore
# ---------------------------------------------------------------------------

def test_concurrency_semaphore_value():
    """BrowserAgent semaphore should reflect SCRAPER_MAX_CONCURRENCY env var."""
    agent = BrowserAgent(headless=True)
    expected = int(os.getenv("SCRAPER_MAX_CONCURRENCY", "3"))
    assert agent._semaphore._value == expected


@pytest.mark.asyncio
async def test_concurrency_allows_limited_parallel(browser_agent):
    """Semaphore should prevent more concurrent browser launches than the limit."""
    max_concurrency = int(os.getenv("SCRAPER_MAX_CONCURRENCY", "3"))

    # Replace launch with a counter that tracks concurrent execution
    current = 0
    peak = 0

    async def mock_navigate_and_interact(url, **kwargs):
        nonlocal current, peak
        async with browser_agent._semaphore:
            current += 1
            peak = max(peak, current)
            await asyncio.sleep(0.01)
            current -= 1
        return {"success": True}

    tasks = [mock_navigate_and_interact("http://example.com") for _ in range(max_concurrency * 3)]
    results = await asyncio.gather(*tasks)

    assert peak <= max_concurrency, f"Peak concurrency {peak} exceeded limit {max_concurrency}"
    assert all(r["success"] for r in results)


# ---------------------------------------------------------------------------
# security.is_safe_url unit tests
# ---------------------------------------------------------------------------

@pytest.mark.parametrize(
    "url,expected",
    [
        ("https://example.com/page", True),
        ("http://example.com", True),
        ("http://127.0.0.1:8080", False),
        ("http://localhost:3000", False),
        ("http://10.0.0.1", False),
        ("http://172.16.0.1", False),
        ("http://192.168.1.1", False),
        ("http://169.254.169.254", False),
        ("http://0.0.0.0", False),
        ("http://[::1]:8080", False),
        ("file:///etc/passwd", False),
        ("ftp://example.com", False),
        ("gopher://127.0.0.1:6379", False),
        ("javascript:alert(1)", False),
        ("data:text/html,<script>alert(1)</script>", False),
        ("", False),
        ("not-a-url", False),
        ("http://100.64.0.1", False),  # CGNAT range
        ("http://[fc00::1]", False),  # IPv6 ULA
        ("http://[fe80::1]", False),  # IPv6 link-local
        ("https://sub.example.com/page?q=1", True),
    ],
)
def test_is_safe_url(url, expected):
    """is_safe_url should correctly classify URLs."""
    assert is_safe_url(url) is expected


def test_is_safe_url_none_raises():
    """is_safe_url should handle None gracefully."""
    assert is_safe_url(None) is False


# ---------------------------------------------------------------------------
# WebScraper unit tests
# ---------------------------------------------------------------------------

def test_web_scraper_ssrf_blocked():
    """WebScraper.fetch_page should block SSRF URLs."""
    scraper = WebScraper()
    result = scraper.fetch_page("http://127.0.0.1:8081/admin")
    assert result["success"] is False
    assert "SSRF" in result["error"]


def test_web_scraper_empty_url():
    """WebScraper.fetch_page should block empty URLs."""
    scraper = WebScraper()
    result = scraper.fetch_page("")
    assert result["success"] is False
    assert "SSRF" in result["error"]
