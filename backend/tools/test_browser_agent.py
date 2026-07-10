import asyncio
from unittest.mock import AsyncMock
from unittest.mock import MagicMock
from unittest.mock import patch

import httpx
import pytest

from tools.browser_agent import BrowserAgent
from core.playwright_manager import get_global_browser, shutdown_global_browser
from core.security_utils import is_safe_url


# pytest-asyncio ব্যবহারের জন্য এই ফাইলএর সমস্ত টেস্টকে async হিসেবে চিহ্নিত করা হলো
pytestmark = pytest.mark.asyncio


@pytest.fixture
def agent():
    """টেস্টের জন্য একটি BrowserAgent ইনস্ট্যান্স প্রদান করে।"""
    return BrowserAgent()


@pytest.fixture(autouse=True)
async def reset_globals():
    """প্রতিটি টেস্টের পরে গ্লোবাল প্লে-রাইট ভ্যারিয়েবল রিসেট করা নিশ্চিত করে।"""
    yield
    # শাটডাউন ফাংশন কল করে রিসোর্স ক্লিন আপ করা
    await shutdown_global_browser()


# --- is_safe_url ফাংশনের জন্য টেস্ট ---


@pytest.mark.parametrize(
    "url, expected",
    [
        ("https://google.com", True),
        ("http://example.com", True),
        ("ftp://test.com", True),
    ],
)
@patch("socket.gethostbyname", return_value="8.8.8.8")
def test_is_safe_url_public(mock_gethostbyname, url, expected):
    """পাবলিক এবং নিরাপদ URL গুলোকে সঠিকভাবে চিহ্নিত করে কিনা তা পরীক্ষা করে।"""
    assert is_safe_url(url) is expected


@pytest.mark.parametrize(
    "url, unsafe_ip",
    [
        ("http://localhost", "127.0.0.1"),
        ("http://192.168.1.10", "192.168.1.10"),
        ("http://10.0.0.1", "10.0.0.1"),
        ("http://169.254.169.254/latest/meta-data", "169.254.169.254"),
        ("http://my-internal-service.local", "192.168.0.5"),
    ],
)
def test_is_safe_url_private(url, unsafe_ip):
    """SSRF অ্যাটাক প্রতিরোধের জন্য প্রাইভেট এবং সংরক্ষিত IP অ্যাড্রেস ব্লক করে কিনা তা পরীক্ষা করে।"""
    with patch("socket.gethostbyname", return_value=unsafe_ip):
        assert is_safe_url(url) is False


@patch("socket.gethostbyname", side_effect=socket.gaierror)
def test_is_safe_url_invalid_hostname(mock_gethostbyname):
    """ভুল বা ইনভ্যালিড হোস্টনেম হ্যান্ডেল করতে পারে কিনা তা পরীক্ষা করে।"""
    assert is_safe_url("http://non-existent-domain-xyz.com") is False


# --- গ্লোবাল ব্রাউজার ম্যানেজমেন্ট টেস্ট ---


@patch("tools.browser_agent.async_playwright")
async def test_get_global_browser_initialization(mock_async_playwright):
    """প্রথমবার কল করার সময় প্লে-রাইট এবং ব্রাউজার সঠিকভাবে চালু হয় কিনা তা পরীক্ষা করে।"""
    mock_playwright = AsyncMock()
    mock_browser = AsyncMock()
    mock_playwright.chromium.launch.return_value = mock_browser
    mock_async_playwright.return_value.start.return_value = mock_playwright

    # প্রথমবার কল
    browser1 = await get_global_browser()
    assert browser1 is mock_browser
    mock_async_playwright.return_value.start.assert_called_once()
    mock_playwright.chromium.launch.assert_called_once()

    # দ্বিতীয়বার কল (cached)
    browser2 = await get_global_browser()
    assert browser2 is browser1  # একই ইনস্ট্যান্স রিটার্ন করা উচিত
    mock_async_playwright.return_value.start.assert_called_once()  # পুনরায় কল করা উচিত নয়


# --- BrowserAgent ক্লাস টেস্ট ---


@patch("tools.browser_agent.is_safe_url", return_value=True)
@patch(
    "httpx.get",
    return_value=MagicMock(
        status_code=200,
        text="<html><head><title>Test Page</title></head><body><p>Hello</p><a href='/link'>Link</a></body></html>",
        raise_for_status=MagicMock(),
    ),
)
def test_fetch_page_success(mock_get, mock_is_safe, agent):
    """একটি ওয়েবপেজ সফলভাবে ফেচ এবং পার্স করতে পারে কিনা তা পরীক্ষা করে।"""
    result = agent.fetch_page("http://example.com")
    assert result["success"] is True
    assert result["title"] == "Test Page"
    assert "Hello" in result["content"]
    assert "/link" in result["links"]
    mock_is_safe.assert_called_once_with("http://example.com")


@patch("tools.browser_agent.is_safe_url", return_value=False)
def test_fetch_page_unsafe_url(mock_is_safe, agent):
    """নিরাপদ নয় এমন URL ব্লক করে কিনা তা পরীক্ষা করে।"""
    result = agent.fetch_page("http://localhost")
    assert result["success"] is False
    assert "SSRF" in result["error"]


@patch("tools.browser_agent.is_safe_url", return_value=True)
@patch("httpx.get", side_effect=httpx.RequestError("Network error"))
def test_fetch_page_network_error(mock_get, mock_is_safe, agent):
    """নেটওয়ার্ক ত্রুটি সঠিকভাবে হ্যান্ডেল করে কিনা তা পরীক্ষা করে।"""
    result = agent.fetch_page("http://example.com")
    assert result["success"] is False
    assert "Network error" in result["error"]


@patch("tools.browser_agent.async_playwright")
async def test_execute_recipe_success(mock_async_playwright, agent):
    """একাধিক স্টেপ সহ একটি ডাইনামিক রেসিপি সফলভাবে কার্যকর করতে পারে কিনা তা পরীক্ষা করে।"""
    # প্লে-রাইট API মক করা
    mock_page = AsyncMock()
    mock_context = AsyncMock()
    mock_context.new_page.return_value = mock_page
    mock_browser = AsyncMock()
    mock_browser.new_context.return_value = mock_context
    mock_playwright = AsyncMock()
    mock_playwright.chromium.launch.return_value = mock_browser
    mock_async_playwright.return_value.__aenter__.return_value = mock_playwright

    mock_page.inner_text.return_value = "Extracted Value"

    recipe = [
        {"action": "navigate", "url": "http://example.com"},
        {"action": "click", "selector": "#button"},
        {"action": "type", "selector": "#input", "value": "test"},
        {"action": "extract", "selector": "#result"},
    ]

    result = await agent.execute_recipe(recipe)

    assert result["status"] == "success"
    assert result["data"]["#result"] == "Extracted Value"
    mock_page.goto.assert_called_once_with("http://example.com", wait_until="networkidle", timeout=30000)
    # HumanBehaviorSimulators মক করা হয়েছে
    # mock_page.click.assert_called_once_with("#button")
    # mock_page.fill.assert_called_once_with("#input", "test")
    mock_page.inner_text.assert_called_once_with("#result")
    assert mock_browser.close.called


@patch("tools.browser_agent.async_playwright")
async def test_execute_recipe_failure(mock_async_playwright, agent):
    """রেসিপি কার্যকর করার সময় ত্রুটি ঘটলে সঠিকভাবে রিপোর্ট করে কিনা তা পরীক্ষা করে।"""
    mock_page = AsyncMock()
    mock_page.goto.side_effect = asyncio.TimeoutError("Page load timeout")
    mock_context = AsyncMock()
    mock_context.new_page.return_value = mock_page
    mock_browser = AsyncMock()
    mock_browser.new_context.return_value = mock_context
    mock_playwright = AsyncMock()
    mock_playwright.chromium.launch.return_value = mock_browser
    mock_async_playwright.return_value.__aenter__.return_value = mock_playwright

    recipe = [{"action": "navigate", "url": "http://example.com"}]

    result = await agent.execute_recipe(recipe)

    assert result["status"] == "failed"
    assert "Page load timeout" in result["error"]
    assert result["step"] == 1  # কোন স্টেপে ফেইল করেছে তা চিহ্নিত করে
    assert mock_browser.close.called  # ফেইল করলেও ব্রাউজার ক্লিনআপ হয়


@patch("tools.browser_agent.BrowserAgent.fetch_page")
@patch("brain.model_router.ModelRouter.async_route_and_generate")
async def test_extract_data_success(mock_llm, mock_fetch, agent):
    """AI ব্যবহার করে ডেটা এক্সট্র্যাকশন সফলভাবে কাজ করে কিনা তা পরীক্ষা করে।"""
    mock_fetch.return_value = {"success": True, "content": "Page content", "title": "Title"}
    mock_llm.return_value = {"text": '{"key": "value"}'}

    result = await agent.extract_data("http://example.com", "extract the key")

    assert result["success"] is True
    assert result["extracted"] == '{"key": "value"}'
    mock_fetch.assert_called_once_with("http://example.com")
    mock_llm.assert_called_once()


@patch("tools.browser_agent.BrowserAgent.fetch_page", return_value={"success": False, "error": "Fetch failed"})
async def test_extract_data_fetch_fails(mock_fetch, agent):
    """পেজ ফেচ করতে ব্যর্থ হলে এক্সট্র্যাকশন সঠিকভাবে ফেইল করে কিনা তা পরীক্ষা করে।"""
    result = await agent.extract_data("http://example.com", "extract something")
    assert result["success"] is False
    assert result["error"] == "Fetch failed"


@patch("tools.browser_agent.BrowserAgent.fetch_page", return_value={"success": True, "content": "Content"})
@patch("brain.model_router.ModelRouter.async_route_and_generate", side_effect=Exception("LLM error"))
async def test_extract_data_llm_fails(mock_llm, mock_fetch, agent):
    """AI মডেল কল করতে ব্যর্থ হলে এক্সট্র্যাকশন সঠিকভাবে ফেইল করে কিনা তা পরীক্ষা করে।"""
    result = await agent.extract_data("http://example.com", "extract something")
    assert result["success"] is False
    assert "LLM error" in result["error"]


@patch("tools.browser_agent.async_playwright", None)
def test_playwright_not_installed(agent):
    """প্লে-রাইট ইনস্টল করা না থাকলে execute_recipe সঠিকভাবে ফেইল করে কিনা তা পরীক্ষা করে।"""
    import asyncio

    result = asyncio.run(agent.execute_recipe([]))
    assert result["status"] == "failed"
    assert "Playwright is not installed" in result["error"]