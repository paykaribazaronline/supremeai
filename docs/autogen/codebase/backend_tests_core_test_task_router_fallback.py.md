# 📄 ফাইল: backend/tests/core/test_task_router_fallback.py

**প্রকার:** .py  
**সাইজ:** 2,971 বাইট  
**আপডেট:** 2026-07-08T02:42:51.251656

---

## কোড

```py
import pytest
import asyncio
from unittest.mock import AsyncMock, patch
from core.task_router import TaskRouter

@pytest.mark.asyncio
async def test_fallback_layer2_success():
    """Layer 2 (Browser Automation) সফল হলে ফলব্যাক লেয়ার ট্রিগার হবে না তা নিশ্চিত করে।"""
    router = TaskRouter()
    
    # Layer 2 সাকসেস মক করা হলো
    router._run_browser_automation = AsyncMock(return_value={"status": "success", "data": "Target Data"})
    router._execute_api_fallback = AsyncMock()

    response = await router.execute_scraping_task(
        task_prompt="Extract pricing", 
        contextual_url="https://example.com/products"
    )

    assert response["status"] == "success"
    assert response["tier"] == "Layer 2 (Zero-Cost Browser)"
    assert response["data"] == "Target Data"
    router._run_browser_automation.assert_called_once()
    router._execute_api_fallback.assert_not_called()


@pytest.mark.asyncio
async def test_fallback_layer2_timeout_drops_to_layer3():
    """Layer 2 টাইমআউট হলে এটি সফলভাবে Layer 3 এপিআই ফলব্যাকে ডাউনগ্রেড করে।"""
    router = TaskRouter()
    
    # Layer 2 টাইমআউট এরর মক করা হলো
    router._run_browser_automation = AsyncMock(side_effect=asyncio.TimeoutError())
    router._execute_api_fallback = AsyncMock(return_value={"status": "success", "tier": "Layer 3 (Economy API)", "data": "Fallback Data"})

    response = await router.execute_scraping_task(
        task_prompt="Extract pricing", 
        contextual_url="https://example.com/products"
    )

    assert response["status"] == "success"
    assert response["tier"] == "Layer 3 (Economy API)"
    assert response["data"] == "Fallback Data"
    router._run_browser_automation.assert_called_once()
    router._execute_api_fallback.assert_called_once_with("Extract pricing")


@pytest.mark.asyncio
async def test_fallback_layer2_failure_drops_to_layer3():
    """Layer 2 এ যেকোনো সাধারণ এক্সেপশন ঘটলে এপিআই ফলব্যাক ট্রিগার করে।"""
    router = TaskRouter()
    
    # Layer 2 ফেইল এরর মক করা হলো
    router._run_browser_automation = AsyncMock(side_effect=Exception("Blocked by Cloudflare CAPTCHA"))
    router._execute_api_fallback = AsyncMock(return_value={"status": "success", "tier": "Layer 3 (Economy API)", "data": "Fallback Data"})

    response = await router.execute_scraping_task(
        task_prompt="Extract pricing", 
        contextual_url="https://example.com/products"
    )

    assert response["status"] == "success"
    assert response["tier"] == "Layer 3 (Economy API)"
    router._run_browser_automation.assert_called_once()
    router._execute_api_fallback.assert_called_once()

```