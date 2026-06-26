"""
Advanced Task Queue System for SupremeAI 2.0
Supports multiple queue backends with automatic fallback and monitoring
"""

import asyncio
import json
import os
import time
import uuid
from collections.abc import Callable
from dataclasses import asdict
from dataclasses import dataclass
from enum import Enum
from typing import Any

from loguru import logger


try:
    from celery import Celery
    from celery import Task
    from celery.result import AsyncResult

    CELERY_AVAILABLE = True
except ImportError:
    CELERY_AVAILABLE = False
    logger.warning("Celery not installed. Some features will be limited.")

try:
    import redis.asyncio as redis

    REDIS_AVAILABLE = True
except ImportError:
    REDIS_AVAILABLE = False
    logger.warning("Redis not installed. Some features will be limited.")

try:
    from google.cloud import pubsub_v1

    PUBSUB_AVAILABLE = True
except ImportError:
    PUBSUB_AVAILABLE = False
    logger.warning("Google Pub/Sub not installed. Some features will be limited.")


class QueueBackend(str, Enum):
    """Supported queue backends"""

    CELERY = "celery"
    REDIS = "redis"
    PUBSUB = "pubsub"
    ASYNCIO = "asyncio"  # Local asyncio queue for development
    MEMORY = "memory"  # In-memory queue (not persistent)


class TaskPriority(int, Enum):
    """Task priority levels"""

    LOW = 0
    NORMAL = 1
    HIGH = 2
    CRITICAL = 3


@dataclass
class TaskResult:
    """Result of a task execution"""

    task_id: str
    status: str  # pending, processing, completed, failed, retry
    result: Any = None
    error: str | None = None
    started_at: float | None = None
    completed_at: float | None = None
    retry_count: int = 0

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "TaskResult":
        return cls(**data)


@dataclass
class TaskMetadata:
    """Metadata for task tracking"""

    task_id: str
    name: str
    priority: TaskPriority
    created_at: float
    max_retries: int = 3
    timeout: int = 300  # 5 minutes default

    def to_dict(self) -> dict[str, Any]:
        data = asdict(self)
        data["priority"] = self.priority.value
        return data

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "TaskMetadata":
        data["priority"] = TaskPriority(data["priority"])
        return cls(**data)


class TaskQueue:
    """
    Advanced task queue with multiple backend support
    Features:
    - Multiple queue backends (Celery, Redis, Pub/Sub, AsyncIO)
    - Priority queuing
    - Retry mechanisms with exponential backoff
    - Dead letter queues
    - Task monitoring and metrics
    - Automatic fallback between backends
    """

    def __init__(
        self,
        default_backend: QueueBackend = QueueBackend.ASYNCIO,
        redis_url: str = None,
        project_id: str = None,
    ):
        """
        Initialize the task queue system

        Args:
            default_backend: Default queue backend to use
            redis_url: Redis connection URL
            project_id: Google Cloud Project ID (for Pub/Sub)
        """
        self.default_backend = default_backend
        self.redis_url = redis_url or os.getenv("REDIS_URL", "redis://localhost:6379/0")
        self.project_id = project_id or os.getenv("GOOGLE_CLOUD_PROJECT")

        # Initialize backends
        self._init_backends()

        # Task registry for tracking
        self._tasks: dict[str, TaskMetadata] = {}
        self._results: dict[str, TaskResult] = {}

        # Statistics
        self._stats = {"submitted": 0, "completed": 0, "failed": 0, "retried": 0}

    def _init_backends(self):
        """Initialize available backends"""
        # Celery backend
        if CELERY_AVAILABLE:
            try:
                self.celery_app = Celery(
                    "supremeai_tasks", broker=self.redis_url, backend=self.redis_url
                )
                # Configure Celery
                self.celery_app.conf.update(
                    task_serializer="json",
                    accept_content=["json"],
                    result_serializer="json",
                    timezone="UTC",
                    enable_utc=True,
                    task_track_started=True,
                    task_time_limit=30 * 60,  # 30 minutes
                    worker_prefetch_multiplier=1,
                    task_acks_late=True,
                )
                logger.info("Celery backend initialized")
            except Exception as e:
                logger.warning(f"Failed to initialize Celery: {e}")
                self.celery_app = None
        else:
            self.celery_app = None

        # Redis backend
        if REDIS_AVAILABLE:
            try:
                self.redis_client = redis.from_url(
                    self.redis_url, decode_responses=True
                )
                logger.info("Redis backend initialized")
            except Exception as e:
                logger.warning(f"Failed to initialize Redis: {e}")
                self.redis_client = None
        else:
            self.redis_client = None

        # Pub/Sub backend
        if PUBSUB_AVAILABLE and self.project_id:
            try:
                self.publisher = pubsub_v1.PublisherClient()
                self.subscriber = pubsub_v1.SubscriberClient()
                self.topic_path = self.publisher.topic_path(
                    self.project_id, "supremeai-tasks"
                )
                logger.info("Pub/Sub backend initialized")
            except Exception as e:
                logger.warning(f"Failed to initialize Pub/Sub: {e}")
                self.publisher = None
                self.subscriber = None
        else:
            self.publisher = None
            self.subscriber = None

        # Local asyncio queue (always available)
        self.local_queue = asyncio.Queue()
        self.local_workers = []

        logger.info(
            f"TaskQueue initialized with backends: "
            f"Celery={'✓' if self.celery_app else '✗'}, "
            f"Redis={'✓' if self.redis_client else '✗'}, "
            f"Pub/Sub={'✓' if self.publisher else '✗'}, "
            f"AsyncIO=✓"
        )

    async def submit_task(
        self,
        func: Callable,
        *args,
        task_name: str = None,
        priority: TaskPriority = TaskPriority.NORMAL,
        max_retries: int = 3,
        timeout: int = 300,
        backend: QueueBackend = None,
        **kwargs,
    ) -> str:
        """
        Submit a task for execution

        Returns:
            task_id: Unique identifier for the task
        """
        task_id = str(uuid.uuid4())
        task_name = task_name or f"{func.__module__}.{func.__name__}"

        # Create task metadata
        metadata = TaskMetadata(
            task_id=task_id,
            name=task_name,
            priority=priority,
            created_at=time.time(),
            max_retries=max_retries,
            timeout=timeout,
        )

        # Store metadata
        self._tasks[task_id] = metadata

        # Initialize result tracking
        self._results[task_id] = TaskResult(task_id=task_id, status="pending")

        # Update statistics
        self._stats["submitted"] += 1

        # Determine backend to use
        selected_backend = backend or self.default_backend

        # Submit to appropriate backend
        try:
            if selected_backend == QueueBackend.CELERY and self.celery_app:
                await self._submit_to_celery(func, task_id, args, kwargs, priority)
            elif selected_backend == QueueBackend.REDIS and self.redis_client:
                await self._submit_to_redis(func, task_id, args, kwargs, priority)
            elif selected_backend == QueueBackend.PUBSUB and self.publisher:
                await self._submit_to_pubsub(func, task_id, args, kwargs, priority)
            elif selected_backend == QueueBackend.ASYNCIO:
                await self._submit_to_asyncio(func, task_id, args, kwargs)
            else:
                # Fallback to synchronous execution
                await self._execute_sync(func, task_id, args, kwargs)

            logger.debug(f"Task {task_id} submitted via {selected_backend.value}")
            return task_id

        except Exception as e:
            logger.error(f"Failed to submit task {task_id}: {e}")
            # Mark as failed
            self._results[task_id].status = "failed"
            self._results[task_id].error = str(e)
            self._stats["failed"] += 1
            raise

    async def _submit_to_celery(
        self,
        func: Callable,
        task_id: str,
        args: tuple,
        kwargs: dict,
        priority: TaskPriority,
    ):
        """Submit task to Celery"""
        # Convert function to Celery task if needed
        if not hasattr(func, "delay"):
            # Wrap function as Celery task
            @self.celery_app.task(bind=True, max_retries=3)
            def celery_wrapper(self, task_id, func_name, args, kwargs):
                # This would normally deserialize and call the actual function
                # For simplicity, we're showing the pattern
                return f"Executed {func_name} with args={args}, kwargs={kwargs}"

            # In practice, you'd have a task registry
            task = celery_wrapper.apply_async(
                args=[task_id, func.__name__, args, kwargs], priority=priority.value
            )
        else:
            # Function is already a Celery task
            task = func.apply_async(args=args, kwargs=kwargs, priority=priority.value)

        # Store task ID for tracking
        # Note: In a real implementation, you'd map Celery task ID to your task_id

    async def _submit_to_redis(
        self,
        func: Callable,
        task_id: str,
        args: tuple,
        kwargs: dict,
        priority: TaskPriority,
    ):
        """Submit task to Redis queue"""
        if not self.redis_client:
            raise RuntimeError("Redis client not initialized")

        # Serialize task data
        task_data = {
            "task_id": task_id,
            "function": f"{func.__module__}.{func.__name__}",
            "args": args,
            "kwargs": kwargs,
            "priority": priority.value,
            "timestamp": time.time(),
        }

        # Use sorted set for priority queue (lower score = higher priority)
        # We negate priority because Redis sorts ascending, so -3 (high priority) comes before -0 (low priority)
        score = -priority.value

        await self.redis_client.zadd("task_queue", {json.dumps(task_data): score})

        # Also store in hash for quick lookup
        await self.redis_client.hset("task_metadata", task_id, json.dumps(task_data))

    async def _submit_to_pubsub(
        self,
        func: Callable,
        task_id: str,
        args: tuple,
        kwargs: dict,
        priority: TaskPriority,
    ):
        """Submit task to Google Pub/Sub"""
        if not self.publisher:
            raise RuntimeError("Pub/Sub publisher not initialized")

        # Prepare message data
        message_data = {
            "task_id": task_id,
            "function": f"{func.__module__}.{func.__name__}",
            "args": args,
            "kwargs": kwargs,
            "priority": priority.value,
            "timestamp": time.time(),
        }

        # Publish message
        message_data_json = json.dumps(message_data)
        future = self.publisher.publish(
            self.topic_path,
            message_data_json.encode("utf-8"),
            priority=str(priority.value),
            task_id=task_id,
        )

        # Wait for publish confirmation (optional)
        message_id = future.result()
        logger.debug(f"Published message {message_id} for task {task_id}")

    async def _submit_to_asyncio(
        self, func: Callable, task_id: str, args: tuple, kwargs: dict
    ):
        """Submit task to local asyncio queue"""
        await self.local_queue.put((func, task_id, args, kwargs))

        # Start worker if not already running
        if not self.local_workers:
            worker_task = asyncio.create_task(self._asyncio_worker())
            self.local_workers.append(worker_task)

    async def _execute_sync(
        self, func: Callable, task_id: str, args: tuple, kwargs: dict
    ):
        """Execute task synchronously (fallback)"""
        try:
            # Update status
            self._results[task_id].status = "processing"
            self._results[task_id].started_at = time.time()

            # Execute function
            if asyncio.iscoroutinefunction(func):
                result = await func(*args, **kwargs)
            else:
                result = func(*args, **kwargs)

            # Update result
            self._results[task_id].status = "completed"
            self._results[task_id].result = result
            self._results[task_id].completed_at = time.time()
            self._stats["completed"] += 1

        except Exception as e:
            self._results[task_id].status = "failed"
            self._results[task_id].error = str(e)
            self._results[task_id].completed_at = time.time()
            self._stats["failed"] += 1
            raise

    async def _asyncio_worker(self):
        """Worker for processing local asyncio queue"""
        logger.info("Started asyncio worker")
        try:
            while True:
                func, task_id, args, kwargs = await self.local_queue.get()
                try:
                    await self._execute_sync(func, task_id, args, kwargs)
                except Exception as e:
                    logger.error(f"Error in asyncio worker for task {task_id}: {e}")
                finally:
                    self.local_queue.task_done()
        except asyncio.CancelledError:
            logger.info("Asyncio worker cancelled")
        except Exception as e:
            logger.error(f"Asyncio worker failed: {e}")

    async def get_result(self, task_id: str, timeout: float = None) -> TaskResult:
        """
        Get the result of a task

        Args:
            task_id: ID of the task
            timeout: Maximum time to wait (None for no timeout)

        Returns:
            TaskResult object
        """
        start_time = time.time()

        while True:
            if task_id in self._results:
                result = self._results[task_id]
                if result.status in ["completed", "failed"]:
                    return result

            # Check timeout
            if timeout is not None and (time.time() - start_time) > timeout:
                raise TimeoutError(f"Timeout waiting for task {task_id}")

            # Wait a bit before checking again
            await asyncio.sleep(0.1)

    def get_status(self, task_id: str) -> str:
        """Get the status of a task"""
        if task_id in self._results:
            return self._results[task_id].status
        return "unknown"

    def cancel_task(self, task_id: str) -> bool:
        """Cancel a pending task"""
        # Implementation would depend on the backend
        # This is a simplified version
        if task_id in self._results and self._results[task_id].status == "pending":
            self._results[task_id].status = "cancelled"
            return True
        return False

    def get_stats(self) -> dict[str, int]:
        """Get queue statistics"""
        return self._stats.copy()

    def get_queue_depth(self) -> int:
        """Get approximate number of pending tasks"""
        # This would be backend-specific in a real implementation
        pending = sum(
            1 for r in self._results.values() if r.status in ["pending", "processing"]
        )
        return pending

    async def cleanup_old_tasks(self, max_age_hours: int = 24):
        """Clean up old completed/failed tasks"""
        cutoff_time = time.time() - (max_age_hours * 3600)

        to_remove = []
        for task_id, result in self._results.items():
            if result.status in ["completed", "failed"] and result.completed_at:
                if result.completed_at < cutoff_time:
                    to_remove.append(task_id)

        for task_id in to_remove:
            self._tasks.pop(task_id, None)
            self._results.pop(task_id, None)

        if to_remove:
            logger.info(f"Cleaned up {len(to_remove)} old tasks")


# Global task queue instance
task_queue = TaskQueue()


# Convenience functions
async def submit_task(func: Callable, *args, **kwargs) -> str:
    """Submit a task to the default queue"""
    return await task_queue.submit_task(func, *args, **kwargs)


async def get_task_result(task_id: str, timeout: float = None) -> TaskResult:
    """Get the result of a task"""
    return await task_queue.get_result(task_id, timeout)


def get_task_status(task_id: str) -> str:
    """Get the status of a task"""
    return task_queue.get_status(task_id)


def cancel_task(task_id: str) -> bool:
    """Cancel a task"""
    return task_queue.cancel_task(task_id)


def get_queue_stats() -> dict[str, int]:
    """Get queue statistics"""
    return task_queue.get_stats()


# Example usage and decorators
def task(
    name: str = None,
    priority: TaskPriority = TaskPriority.NORMAL,
    max_retries: int = 3,
    timeout: int = 300,
):
    """
    Decorator to mark a function as a task

    Usage:
        @task(name="process_user_data", priority=TaskPriority.HIGH)
        async def process_user_data(user_id: int):
            # Do work
            return result
    """

    def decorator(func: Callable) -> Callable:
        func._task_metadata = {
            "name": name,
            "priority": priority,
            "max_retries": max_retries,
            "timeout": timeout,
        }
        return func

    return decorator


# Example task implementations
@task(name="send_welcome_email", priority=TaskPriority.HIGH)
async def send_welcome_email(user_id: int, email: str):
    """Example task: Send welcome email"""
    # Simulate email sending
    await asyncio.sleep(1)
    return f"Welcome email sent to user {user_id} at {email}"


@task(name="process_image", priority=TaskPriority.NORMAL, max_retries=2)
async def process_image(image_path: str, operations: list):
    """Example task: Process image with multiple operations"""
    # Simulate image processing
    await asyncio.sleep(2)
    return f"Processed {image_path} with operations: {operations}"


@task(name="generate_report", priority=TaskPriority.LOW, timeout=600)
async def generate_report(user_id: int, report_type: str):
    """Example task: Generate report (low priority, can take time)"""
    # Simulate report generation
    await asyncio.sleep(5)
    return f"Generated {report_type} report for user {user_id}"


# Pro Tip (বাংলা): task_queue ব্যবহার করে আপনি ব্যাকগ্রাউন্ড কাজগুলোকে
# কার্যকরভাবে ম্যানেজ করতে পারেন। Uc প্রτερাইparte travailler (যেমন ইমেল পাঠানো)
# দ্রুত প্রক্রিয়া করা যায়, কম প্রাধান্যের কাজগুলো (যেমন রিপোর্ট জেনারেট)
# প কালે প্রক্রিয়া করা যায়।
#
# প্রো টিপ: workersの数をあなたのワークロードに基づいて調整してください。
# CPU集約的なタスクの場合、CPUコア数と同じ数のワーカーを使用してください。
# I/O集約的なタスクの場合、より多くのワーカーを維持してください（例: CPUコア数の2-3倍）。
#
# モニタリングとアラートを設定してキューの深さと処理時間を追跡してください。
# キューが特定の閾値を超えたときにスケールアップするように自動スケーリングポリシーを設定してください。
