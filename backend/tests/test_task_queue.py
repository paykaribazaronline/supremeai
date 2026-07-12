import os
import pytest


os.environ.setdefault("OPENROUTER_API_KEY", "")
os.environ.setdefault("HF_API_KEY", "")
os.environ.setdefault("OLLAMA_URL", "http://127.0.0.1:11434")


class TestTaskQueueBasic:
    @pytest.mark.asyncio
    async def test_submit_and_get_result(self):
        from core.queue.task_queue_enhanced import submit_task, get_task_result

        async def mock_task(project_id, req):
            return {"status": "completed", "result": f"Processed {req} for {project_id}"}

        task_id = await submit_task(mock_task, "proj-1", "do something useful here")
        result = await get_task_result(task_id, timeout=2.0)
        assert result.status == "completed"
        assert "Processed" in result.result["result"]

    @pytest.mark.asyncio
    async def test_task_failure(self):
        from core.queue.task_queue_enhanced import submit_task, get_task_result

        async def failing_task():
            raise ValueError("Intentional failure")

        task_id = await submit_task(failing_task)
        result = await get_task_result(task_id, timeout=2.0)
        assert result.status == "failed"
        assert "Intentional failure" in result.error
