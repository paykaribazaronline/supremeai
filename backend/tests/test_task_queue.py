import os


os.environ.setdefault("OPENROUTER_API_KEY", "")
os.environ.setdefault("HF_API_KEY", "")
os.environ.setdefault("OLLAMA_URL", "http://127.0.0.1:11434")


class TestTaskQueueFallback:
    def test_process_requirement_async_sync_fallback(self):
        from core.task_queue_enhanced import process_requirement_async

        result = process_requirement_async("proj-1", "do something useful here")
        assert result["status"] in ("completed", "queued")
        if result["status"] == "completed":
            assert "Processed requirement" in result["result"]

    def test_task_queue_result_schema_sync_fallback(self):
        from core.task_queue_enhanced import process_requirement_async

        result = process_requirement_async("proj-x", "a long description " * 100)
        assert "status" in result
        assert result["status"] in ("completed", "queued")
