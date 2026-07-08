# 📄 ফাইল: backend/tests/core/test_task_router_fallback.py

**প্রকার:** .py  
**সাইজ:** 3,340 বাইট  
**আপডেট:** 2026-07-08T18:50:08.159960

---

## কোড

```py
import pytest
import asyncio
from unittest.mock import AsyncMock, patch
from core.task_router import TaskRouter

@pytest.fixture
def router():
    r = TaskRouter()
    r.skill_manager.get_or_create_skill = AsyncMock(return_value={"execution_steps": []})
    return r

@pytest.mark.asyncio
async def test_fallback_layer2_success(router):
    """Layer 2 (Browser Automation) সফল হলে ফলব্যাক লেয়ার ট্রিগার হবে না তা নিশ্চিত করে।"""
    router._execute_local_playwright_recipe = AsyncMock(return_value={"status": "success", "data": "Target Data"})
    
    with patch("core.task_router.cost_guard") as mock_cost, \
         patch("core.task_router.llm_gateway") as mock_llm:
         
        response = await router.execute_scraping_task(
            task_prompt="Extract pricing", 
            contextual_url="https://example.com/products"
        )
        
    assert response["status"] == "success"
    assert "Layer 2" in response["execution_tier"]
    assert response["data"] == "Target Data"
    router._execute_local_playwright_recipe.assert_called_once()
    mock_llm.acompletion.assert_not_called()

@pytest.mark.asyncio
async def test_fallback_layer2_timeout_drops_to_layer3(router):
    """Layer 2 টাইমআউট হলে এটি সফলভাবে Layer 3 এপিআই ফলব্যাকে ডাউনগ্রেড করে।"""
    router._execute_local_playwright_recipe = AsyncMock(side_effect=TimeoutError())
    
    with patch("core.task_router.cost_guard") as mock_cost, \
         patch("core.task_router.llm_gateway") as mock_llm:
        mock_cost.validate_budget.return_value = True
        mock_llm.acompletion = AsyncMock(return_value={"success": True, "text": "Fallback Data"})
         
        response = await router.execute_scraping_task(
            task_prompt="Extract pricing", 
            contextual_url="https://example.com/products"
        )
        
    assert response["status"] == "success"
    assert "Layer 3" in response["execution_tier"]
    assert response["data"] == "Fallback Data"
    router._execute_local_playwright_recipe.assert_called_once()
    mock_llm.acompletion.assert_called_once()

@pytest.mark.asyncio
async def test_fallback_layer2_failure_drops_to_layer3(router):
    """Layer 2 এ যেকোনো সাধারণ এক্সেপশন ঘটলে এপিআই ফলব্যাক ট্রিগার করে।"""
    router._execute_local_playwright_recipe = AsyncMock(side_effect=Exception("Blocked by Cloudflare CAPTCHA"))
    
    with patch("core.task_router.cost_guard") as mock_cost, \
         patch("core.task_router.llm_gateway") as mock_llm:
        mock_cost.validate_budget.return_value = True
        mock_llm.acompletion = AsyncMock(return_value={"success": True, "text": "Fallback Data"})
         
        response = await router.execute_scraping_task(
            task_prompt="Extract pricing", 
            contextual_url="https://example.com/products"
        )
        
    assert response["status"] == "success"
    assert "Layer 3" in response["execution_tier"]
    assert response["data"] == "Fallback Data"
    router._execute_local_playwright_recipe.assert_called_once()
    mock_llm.acompletion.assert_called_once()

```