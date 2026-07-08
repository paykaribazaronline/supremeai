import pytest
import asyncio
from unittest.mock import AsyncMock, MagicMock, patch
from core.task_router import TaskRouter

@pytest.fixture
def mock_db_context():
    """ডাটাবেজ ও ফ্যাক্টরি মক করার জন্য ফিক্সচার।"""
    mock_result = MagicMock()
    mock_result.scalars.return_value.first.return_value = None
    
    mock_session = AsyncMock()
    mock_session.execute.return_value = mock_result
    mock_session.__aenter__ = AsyncMock(return_value=mock_session)
    mock_session.__aexit__ = AsyncMock()
    
    mock_factory = MagicMock()
    mock_factory.create_specialized_agent = AsyncMock(return_value={
        "agent_name": "MockAgent", 
        "execution_steps": []
    })
    
    return mock_session, mock_factory


@pytest.mark.asyncio
async def test_fallback_layer2_success(mock_db_context):
    """Layer 2 (Browser Automation) সফল হলে ফলব্যাক লেয়ার ট্রিগার হবে না তা নিশ্চিত করে।"""
    router = TaskRouter()
    mock_session, mock_factory = mock_db_context
    
    # Layer 2 সাকসেস মক করা হলো
    router._run_browser_automation = AsyncMock(return_value={"status": "success", "data": "Target Data"})
    router._execute_api_fallback = AsyncMock()

    # বাংলা মন্তব্য: ডাটাবেজ টেবিল এরর এড়াতে সেশন ও ফ্যাক্টরি প্যাক প্যাচ করা হলো
    with patch("database.session.AsyncSessionLocal", return_value=mock_session), \
         patch("core.agent_factory.DynamicAgentFactory", return_value=mock_factory):
        response = await router.execute_scraping_task(
            task_prompt="Extract pricing", 
            contextual_url="https://example.com/products"
        )

    assert response["status"] == "success"
    assert "Layer 2" in response["tier"]
    assert response["data"] == "Target Data"
    router._run_browser_automation.assert_called_once()
    router._execute_api_fallback.assert_not_called()


@pytest.mark.asyncio
async def test_fallback_layer2_timeout_drops_to_layer3(mock_db_context):
    """Layer 2 টাইমআউট হলে এটি সফলভাবে Layer 3 এপিআই ফলব্যাকে ডাউনগ্রেড করে।"""
    router = TaskRouter()
    mock_session, mock_factory = mock_db_context
    
    # Layer 2 টাইমআউট এরর মক করা হলো
    router._run_browser_automation = AsyncMock(side_effect=TimeoutError())
    router._execute_api_fallback = AsyncMock(return_value={"status": "success", "tier": "Layer 3 (Economy API)", "data": "Fallback Data"})

    with patch("database.session.AsyncSessionLocal", return_value=mock_session), \
         patch("core.agent_factory.DynamicAgentFactory", return_value=mock_factory):
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
async def test_fallback_layer2_failure_drops_to_layer3(mock_db_context):
    """Layer 2 এ যেকোনো সাধারণ এক্সেপশন ঘটলে এপিআই ফলব্যাক ট্রিগার করে।"""
    router = TaskRouter()
    mock_session, mock_factory = mock_db_context
    
    # Layer 2 ফেইল এরর মক করা হলো
    router._run_browser_automation = AsyncMock(side_effect=Exception("Blocked by Cloudflare CAPTCHA"))
    router._execute_api_fallback = AsyncMock(return_value={"status": "success", "tier": "Layer 3 (Economy API)", "data": "Fallback Data"})

    with patch("database.session.AsyncSessionLocal", return_value=mock_session), \
         patch("core.agent_factory.DynamicAgentFactory", return_value=mock_factory):
        response = await router.execute_scraping_task(
            task_prompt="Extract pricing", 
            contextual_url="https://example.com/products"
        )

    assert response["status"] == "success"
    assert response["tier"] == "Layer 3 (Economy API)"
    router._run_browser_automation.assert_called_once()
    router._execute_api_fallback.assert_called_once()
