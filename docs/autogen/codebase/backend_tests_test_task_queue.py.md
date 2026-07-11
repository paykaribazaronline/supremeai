# 📄 ফাইল: backend/tests/test_task_queue.py

**প্রকার:** .py  
**সাইজ:** 1,091 বাইট  
**আপডেট:** 2026-07-11T13:46:44.136268

---

## কোড

```py
import os


os.environ.setdefault("OPENROUTER_API_KEY", "")
os.environ.setdefault("HF_API_KEY", "")
os.environ.setdefault("OLLAMA_URL", "http://127.0.0.1:11434")


class TestTaskQueueFallback:
    def test_process_requirement_async_sync_fallback(self):
        from core.task_queue import process_requirement_async

        result = process_requirement_async("proj-1", "do something useful here")
        assert result["status"] in ("completed", "queued")
        if result["status"] == "completed":
            assert "Processed requirement" in result["result"]

    def test_mock_task_returns_expected_format(self):
        from core.task_queue import _process_requirement_task

        result = _process_requirement_task("my-project", "a description")
        assert "my-project" in result

    def test_task_queue_result_schema_sync_fallback(self):
        from core.task_queue import process_requirement_async

        result = process_requirement_async("proj-x", "a long description " * 100)
        assert "status" in result
        assert result["status"] in ("completed", "queued")

```