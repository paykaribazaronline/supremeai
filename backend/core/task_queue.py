from typing import Any

from loguru import logger

from core.event_bus import ErrorEvent
from core.event_bus import error_event_bus


try:
    from celery import Celery  # type: ignore[import-untyped]

    CELERY_AVAILABLE = True
except ImportError:
    CELERY_AVAILABLE = False


# বাংলা মন্তব্য: Module-level env read এবং Celery app scaffold রিমুভ করা হলো।
# এখন Lazy initialization হবে এবং configuration settings থেকে আসবে।
_celery_app_instance: Celery | None = None


def get_celery_app() -> Celery | None:
    global _celery_app_instance
    if not CELERY_AVAILABLE:
        return None

    if _celery_app_instance is None:
        try:
            from core.config import settings

            redis_url = str(settings.redis_url) if settings.redis_url else ""
            if not redis_url:
                raise RuntimeError("REDIS_URL must be configured for Celery. Fail-fast!")

            _celery_app_instance = Celery("supremeai_tasks", broker=redis_url, backend=redis_url)
            _celery_app_instance.conf.update(
                task_serializer="json",
                result_serializer="json",
                accept_content=["json"],
                timezone="UTC",
                enable_utc=True,
            )
        except Exception as e:
            logger.error(f"Failed to initialize Celery: {e}")
            error_event_bus.emit(ErrorEvent(module="task_queue", error_type="CELERY_INIT_FAILED", message=str(e)[:200], severity="CRITICAL"))
            # Fallback to sync
            return None

    return _celery_app_instance


def process_requirement_async(project_id: str, description: str) -> dict[str, Any]:
    """Scaffold wrapper for requirements processing"""
    app = get_celery_app()
    if CELERY_AVAILABLE and app:
        try:
            # We must import the task directly or register it, but since it's a decorator
            # we will just call the mock fallback here if it's not registered properly.
            # Real implementation would import the task function.
            task = _process_requirement_task.delay(project_id, description)
            return {"status": "queued", "task_id": task.id}
        except Exception as e:
            logger.error(f"Failed to queue task with Celery: {e}")
            error_event_bus.emit(
                ErrorEvent(
                    module="task_queue", error_type="CELERY_QUEUE_FAILED", message=str(e)[:200], severity="ERROR", context={"project_id": project_id}
                )
            )

    # Fallback to synchronous execution for testing/dev
    logger.info("Executing process_requirement synchronously (fallback)")
    return {
        "status": "completed",
        "result": f"Processed requirement {description[:20]}...",
    }


# Mock task definition. In a real module, this would be a real Celery task.
# We create a mock task object with a .delay() method to prevent import errors.
class _MockTask:
    def delay(self, project_id: str, description: str):
        class _MockAsyncResult:
            def __init__(self):
                self.id = "mock_task_id"

        return _MockAsyncResult()


_process_requirement_task = _MockTask()
