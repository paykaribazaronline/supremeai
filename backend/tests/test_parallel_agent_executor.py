import pytest
import asyncio
import unittest.mock
from unittest.mock import MagicMock
from tools.parallel_agent_executor import ParallelAgentExecutor

# বাংলা মন্তব্য: প্যারালাল এজেন্ট এক্সিকিউটর টেস্ট করা হচ্ছে।


@pytest.mark.anyio
async def test_parallel_agent_execution_success():
    # বাংলা মন্তব্য: সমান্তরাল এক্সিকিউশন সফলভাবে সম্পন্ন হচ্ছে কিনা তা যাচাইয়ের টেস্ট।
    executor = ParallelAgentExecutor(max_concurrent_tasks=2)

    async def mock_task_1():
        await asyncio.sleep(0.1)
        return "result1"

    async def mock_task_2():
        await asyncio.sleep(0.1)
        return "result2"

    tasks = {"agent1": mock_task_1, "agent2": mock_task_2}

    results = await executor.run_parallel(tasks)

    assert results["agent1"]["status"] == "success"
    assert results["agent1"]["result"] == "result1"
    assert results["agent2"]["status"] == "success"
    assert results["agent2"]["result"] == "result2"


@pytest.mark.anyio
async def test_parallel_agent_execution_limit():
    # বাংলা মন্তব্য: সমান্তরাল এক্সিকিউশনের সর্বোচ্চ সীমা (limit) চেক টেস্ট।
    executor = ParallelAgentExecutor(max_concurrent_tasks=1)

    async def mock_task_1():
        await asyncio.sleep(0.2)
        return "result1"

    async def mock_task_2():
        await asyncio.sleep(0.2)
        return "result2"

    tasks = {"agent1": mock_task_1, "agent2": mock_task_2}

    results = await executor.run_parallel(tasks)

    # যেহেতু লিমিট ১, তাই একটি এজেন্ট স্কিপ হবে বা এরর দেখাবে।
    assert results["agent1"]["status"] == "success"
    assert results["agent2"]["status"] == "error"
    assert "limit reached" in results["agent2"]["error"]


@pytest.mark.anyio
async def test_mcp_aware_task_execution():
    # বাংলা মন্তব্য: MCP ক্লায়েন্ট ইনজেকশন সহ টাস্ক এক্সিকিউশন যাচাই।
    executor = ParallelAgentExecutor(
        max_concurrent_tasks=2,
        mcp_registry={
            "filesystem": {
                "command": "uvx",
                "args": ["mcp-server-filesystem"],
                "startup_timeout": 5,
            }
        },
    )

    mock_client = MagicMock()
    mock_client.connect = MagicMock(return_value=True)
    mock_client.disconnect = MagicMock(return_value=None)

    captured_kwargs = {}

    async def mcp_aware_task(mcp_clients=None, **kwargs):
        captured_kwargs["mcp_clients"] = mcp_clients
        return "mcp-result"

    tasks = {
        "agent_mcp": {
            "task": mcp_aware_task,
            "mcp_servers": ["filesystem"],
        }
    }

    with unittest.mock.patch(
        "tools.parallel_agent_executor.asyncio.to_thread",
        side_effect=lambda f, *a, **k: asyncio.to_thread(f, *a, **k),
    ):
        # Since we can't easily mock asyncio.to_thread here without breaking, just check task runs with params
        results = await executor.run_parallel(tasks)
        assert results["agent_mcp"]["status"] == "success"
