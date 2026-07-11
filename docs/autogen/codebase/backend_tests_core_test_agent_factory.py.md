# 📄 ফাইল: backend/tests/core/test_agent_factory.py

**প্রকার:** .py  
**সাইজ:** 2,029 বাইট  
**আপডেট:** 2026-07-11T17:00:45.012586

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

    mock_res = {"text": '{"agent_name": "AmazonTracker", "description": "Track prices", "execution_steps": [{"action": "click"}]}'}

    with patch("core.llm_gateway.LLMGateway.acompletion", new_callable=AsyncMock, return_value=mock_res):
        config = await factory.create_specialized_agent("Track prices on Amazon")
        assert config["agent_name"] == "AmazonTracker"
        assert config["execution_steps"] == [{"action": "click"}]
        mock_db.add.assert_called_once()
        mock_db.commit.assert_called_once()


@pytest.mark.asyncio
async def test_task_router_dispatches_local_scraping_task():
    """Ensures the TaskRouter correctly dispatches a 'web_scraping_local' task to the local executor."""
    router = TaskRouter()

    router.local_executor.execute_local_code = AsyncMock(return_value={"status": "success", "data": "DOM Result"})

    task_context = {"task_type": "web_scraping_local", "code": "print('scraping')", "cost_limit": 0.05}

    response = await router.route_and_dispatch(task_context)

    assert response["status"] == "success"
    assert response["cost"] == 0.05
    assert response["data"] == "DOM Result"
    router.local_executor.execute_local_code.assert_called_once_with("print('scraping')")

```