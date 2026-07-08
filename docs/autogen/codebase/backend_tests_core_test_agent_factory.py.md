# 📄 ফাইল: backend/tests/core/test_agent_factory.py

**প্রকার:** .py  
**সাইজ:** 2,681 বাইট  
**আপডেট:** 2026-07-08T03:25:22.513644

---

## কোড

```py
import pytest
import asyncio
from unittest.mock import AsyncMock, MagicMock, patch
from core.task_router import TaskRouter
from core.agent_factory import DynamicAgentFactory
from models.dynamic_agent import DynamicAgent

@pytest.mark.asyncio
async def test_agent_factory_creates_and_saves_agent():
    """এজেন্ট ফ্যাক্টরি এআই রেসপন্স থেকে স্ক্রিপ্ট বানিয়ে ডাটাবেজে সেভ করে তা নিশ্চিত করে।"""
    mock_db = MagicMock()
    mock_result = MagicMock()
    mock_result.scalars.return_value.first.return_value = None
    mock_db.execute = AsyncMock(return_value=mock_result)
    mock_db.commit = AsyncMock()
    
    factory = DynamicAgentFactory(mock_db)
    
    mock_res = {
        "text": '{"agent_name": "AmazonTracker", "description": "Track prices", "execution_steps": [{"action": "click"}]}'
    }
    
    with patch("core.agent_factory.llm_gateway.acompletion", new_callable=AsyncMock, return_value=mock_res):
        config = await factory.create_specialized_agent("Track prices on Amazon")
        assert config["agent_name"] == "AmazonTracker"
        assert config["execution_steps"] == [{"action": "click"}]
        mock_db.add.assert_called_once()
        mock_db.commit.assert_called_once()


@pytest.mark.asyncio
async def test_task_router_uses_saved_agent_from_db():
    """টাস্ক রাউটার যদি ডাটাবেজে ম্যাচিং এজেন্ট পায়, তবে সরাসরি সেটি ব্যবহার করে।"""
    router = TaskRouter()
    
    mock_agent = MagicMock()
    mock_agent.name = "AmazonTracker"
    mock_agent.execution_steps = [{"action": "click"}]
    
    mock_result = MagicMock()
    mock_result.scalars.return_value.first.return_value = mock_agent
    
    mock_session = AsyncMock()
    mock_session.execute.return_value = mock_result
    mock_session.__aenter__ = AsyncMock(return_value=mock_session)
    mock_session.__aexit__ = AsyncMock()
    
    router._run_browser_automation = AsyncMock(return_value={"status": "success", "data": "DOM Result"})
    
    with patch("database.session.AsyncSessionLocal", return_value=mock_session):
        response = await router.execute_scraping_task("AmazonTracker prices", "https://amazon.com")
        assert response["status"] == "success"
        assert "AmazonTracker" in response["tier"]
        assert response["data"] == "DOM Result"
        router._run_browser_automation.assert_called_once_with(
            "AmazonTracker prices", "https://amazon.com", [{"action": "click"}]
        )

```