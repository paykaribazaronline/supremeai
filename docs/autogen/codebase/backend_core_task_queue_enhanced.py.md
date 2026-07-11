# 📄 ফাইল: backend/core/task_queue_enhanced.py

**প্রকার:** .py  
**সাইজ:** 21,590 বাইট  
**আপডেট:** 2026-07-11T20:08:21.355056

---

## কোড

```py
# backend/core/task_queue_enhanced.py
# বাংলা মন্তব্য: সম্পূর্ণ রি-ফ্যাক্টর — Polling anti-pattern উপড়ে ফেলা হয়েছে।
# get_result() এর while True sleep(0.1) loop → asyncio.Event callback model।
# _asyncio_worker() এর while True → gracefully cancellable coroutine।
# Bounded memory: max tracked tasks cap enforce।
# CancelledError সবসময় re-raise।
import asyncio
import contextlib
import inspect
import json
import time
import uuid
from collections import OrderedDict
from collections.abc import Callable
from dataclasses import asdict
from dataclasses import dataclass
from enum import Enum
from enum import StrEnum
from typing import Any

from loguru import logger

from core.config import settings


# ── Lazy Loading Flags ────────────────────────────────────────────────────────
# বাংলা মন্তব্য: Heavy libraries lazy-load করা হচ্ছে — module import time কমানো
# import redis, celery, pubsub — এগুলো function level-এ load হবে
CELERY_AVAILABLE: bool = False
REDIS_AVAILABLE: bool = False
PUBSUB_AVAILABLE: bool = False


def _check_celery_available() -> bool:
    global CELERY_AVAILABLE
    if not CELERY_AVAILABLE:
        try:
            import celery  # noqa: F401

            CELERY_AVAILABLE = True
        except ImportError:
            pass
    return CELERY_AVAILABLE


def _check_redis_available() -> bool:
    global REDIS_AVAILABLE
    if not REDIS_AVAILABLE:
        try:
            import redis.asyncio  # noqa: F401

            REDIS_AVAILABLE = True
        except ImportError:
            pass
    return REDIS_AVAILABLE


def _check_pubsub_available() -> bool:
    global PUBSUB_AVAILABLE
    if not PUBSUB_AVAILABLE:
        try:
            from google.cloud import pubsub_v1  # noqa: F401

            PUBSUB_AVAILABLE = True
        except ImportError:
            pass
    return PUBSUB_AVAILABLE


# ── Data Models ────────────────────────────────────────────────────────────────
class QueueBackend(StrEnum):
    CELERY = "celery"
    REDIS = "redis"
    PUBSUB = "pubsub"
    ASYNCIO = "asyncio"
    MEMORY = "memory"


class TaskPriority(int, Enum):
    LOW = 0
    NORMAL = 1
    HIGH = 2
    CRITICAL = 3


@dataclass
class TaskResult:
    task_id: str
    status: str  # pending, processing, completed, failed, cancelled
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
    task_id: str
    name: str
    priority: TaskPriority
    created_at: float
    max_retries: int = 3
    timeout: int = 300

    def to_dict(self) -> dict[str, Any]:
        data = asdict(self)
        data["priority"] = self.priority.value
        return data


# ── Core Queue Implementation ──────────────────────────────────────────────────
class TaskQueue:
    """
    বাংলা মন্তব্য: Anti-Polling Task Queue।
    get_result() এ while True sleep loop নেই।
    asyncio.Event দিয়ে completion notification।
    Bounded memory — max 10k tracked tasks।
    CancelledError always propagated।
    """

    def __init__(
        self,
        default_backend: QueueBackend = QueueBackend.ASYNCIO,
        redis_url: str | None = None,
        project_id: str | None = None,
        max_tracked_tasks: int = 10_000,
    ):
        self.default_backend = default_backend
        # বাংলা মন্তব্য: Settings থেকে URL নেওয়া — hardcode নিষিদ্ধ
        self.redis_url = redis_url or settings.redis_url
        self.project_id = project_id or settings.gcp_project_id

        # বাংলা মন্তব্য: OrderedDict — FIFO eviction policy (oldest evicted first)
        self._MAX_TRACKED_TASKS = max_tracked_tasks
        self._tasks: OrderedDict[str, TaskMetadata] = OrderedDict()
        self._results: OrderedDict[str, TaskResult] = OrderedDict()

        # বাংলা মন্তব্য: Anti-Polling core — প্রতিটি task-এর জন্য asyncio.Event
        # get_result() এটির জন্য wait() করবে — sleep loop নয়
        self._completion_events: dict[str, asyncio.Event] = {}

        self._stats: dict[str, int] = {
            "submitted": 0,
            "completed": 0,
            "failed": 0,
            "retried": 0,
            "evicted": 0,
        }

        # বাংলা মন্তব্য: backends lazily initialized — module import time-এ network call নেই
        self.local_queue: asyncio.Queue = asyncio.Queue()
        self._worker_task: asyncio.Task | None = None
        self._shutdown_event = asyncio.Event()

        logger.info(f"[TaskQueue] Initialized with backend={default_backend.value}, max_tracked={max_tracked_tasks}")

    def _evict_oldest_if_needed(self) -> None:
        """
        বাংলা মন্তব্য: Memory bound enforcement।
        Oldest completed/failed tasks evict করা হয় — memory leak prevent।
        """
        while len(self._results) >= self._MAX_TRACKED_TASKS:
            # বাংলা মন্তব্য: FIFO — সবচেয়ে পুরানো টাস্ক বাদ যাবে
            oldest_id, oldest_result = next(iter(self._results.items()))
            if oldest_result.status in ("completed", "failed", "cancelled"):
                self._tasks.pop(oldest_id, None)
                self._results.pop(oldest_id, None)
                self._completion_events.pop(oldest_id, None)
                self._stats["evicted"] += 1
                logger.debug(f"[TaskQueue] Evicted old task: {oldest_id}")
            else:
                # বাংলা মন্তব্য: সব tasks pending হলে evict করা যাবে না — log এবং break
                logger.warning(f"[TaskQueue] Max tracked tasks ({self._MAX_TRACKED_TASKS}) reached with all pending tasks. Cannot evict.")
                break

    async def submit_task(
        self,
        func: Callable,
        *args,
        task_name: str | None = None,
        priority: TaskPriority = TaskPriority.NORMAL,
        max_retries: int = 3,
        timeout: int = 300,
        backend: QueueBackend | None = None,
        **kwargs,
    ) -> str:
        """
        বাংলা মন্তব্য: Task submit করুন। asyncio.Event তৈরি হবে।
        get_result() এই event-এর জন্য wait করবে।
        """
        self._evict_oldest_if_needed()

        task_id = str(uuid.uuid4())
        task_name = task_name or f"{func.__module__}.{func.__name__}"

        metadata = TaskMetadata(
            task_id=task_id,
            name=task_name,
            priority=priority,
            created_at=time.time(),
            max_retries=max_retries,
            timeout=timeout,
        )

        self._tasks[task_id] = metadata
        self._results[task_id] = TaskResult(task_id=task_id, status="pending")
        # বাংলা মন্তব্য: completion event তৈরি — get_result() এটির await করবে
        self._completion_events[task_id] = asyncio.Event()
        self._stats["submitted"] += 1

        selected_backend = backend or self.default_backend

        try:
            if selected_backend == QueueBackend.REDIS and _check_redis_available():
                await self._submit_to_redis(func, task_id, args, kwargs, priority)
            elif selected_backend == QueueBackend.PUBSUB and _check_pubsub_available():
                await self._submit_to_pubsub(func, task_id, args, kwargs, priority)
            elif selected_backend == QueueBackend.CELERY and _check_celery_available():
                await self._submit_to_celery(func, task_id, args, kwargs, priority)
            else:
                # বাংলা মন্তব্য: asyncio local queue — development এবং fallback
                await self._submit_to_asyncio(func, task_id, args, kwargs)

            logger.debug(f"[TaskQueue] Task {task_id} submitted via {selected_backend.value}")
            return task_id

        except asyncio.CancelledError:
            # বাংলা মন্তব্য: CancelledError re-raise — কখনো suppress করা যাবে না
            logger.warning(f"[TaskQueue] Task submission cancelled for {task_id}")
            self._results[task_id].status = "cancelled"
            self._completion_events[task_id].set()
            raise
        except Exception as exc:  # noqa: BLE001
            logger.exception(f"[TaskQueue] Failed to submit task {task_id}: {exc}")
            self._results[task_id].status = "failed"
            self._results[task_id].error = str(exc)
            self._stats["failed"] += 1
            # বাংলা মন্তব্য: failure-এ event set করা — caller unblocked হবে
            self._completion_events[task_id].set()
            raise

    async def get_result(self, task_id: str, timeout: float | None = None) -> TaskResult:
        """
        বাংলা মন্তব্য: Anti-Polling result retrieval।
        আগের ভাঙা কোড: while True: await asyncio.sleep(0.1)
        এখন: asyncio.Event.wait() — CPU বা billing waste নেই।
        Serverless-friendly — instance idle থাকে।
        """
        if task_id not in self._results:
            raise KeyError(f"Task {task_id} not found in queue.")

        event = self._completion_events.get(task_id)

        if event and not event.is_set():
            try:
                # বাংলা মন্তব্য: blocking wait নয় — event-driven await
                await asyncio.wait_for(event.wait(), timeout=timeout)
            except TimeoutError:
                raise TimeoutError(f"Timeout ({timeout}s) waiting for task {task_id}. Current status: {self._results[task_id].status}") from None
            except asyncio.CancelledError:
                # বাংলা মন্তব্য: CancelledError re-raise — graceful shutdown support
                logger.warning(f"[TaskQueue] get_result cancelled for task {task_id}")
                raise

        return self._results[task_id]

    def _mark_complete(self, task_id: str) -> None:
        """
        বাংলা মন্তব্য: Task completion signal — asyncio.Event set করা।
        get_result()-এর await unblock হবে।
        """
        event = self._completion_events.get(task_id)
        if event:
            event.set()

    async def _execute_task(self, func: Callable, task_id: str, args: tuple, kwargs: dict) -> None:
        """বাংলা মন্তব্য: Task execution — result storage এবং event notification।"""
        result_obj = self._results.get(task_id)
        if not result_obj:
            logger.error(f"[TaskQueue] Task {task_id} result object missing before execution.")
            return

        try:
            result_obj.status = "processing"
            result_obj.started_at = time.time()

            if inspect.iscoroutinefunction(func):
                output = await func(*args, **kwargs)
            else:
                # বাংলা মন্তব্য: sync function-কে thread pool-এ run করা — event loop block হয় না
                output = await asyncio.to_thread(func, *args, **kwargs)

            result_obj.status = "completed"
            result_obj.result = output
            result_obj.completed_at = time.time()
            self._stats["completed"] += 1

        except asyncio.CancelledError:
            # বাংলা মন্তব্য: CancelledError re-raise — কখনো suppress করা যাবে না
            result_obj.status = "cancelled"
            result_obj.completed_at = time.time()
            logger.warning(f"[TaskQueue] Task {task_id} cancelled during execution.")
            raise
        except Exception as exc:  # noqa: BLE001
            logger.exception(
                f"[TaskQueue] Task {task_id} failed: {exc}",
                # বাংলা মন্তব্য: logger.exception() — full stack trace সহ
            )
            result_obj.status = "failed"
            result_obj.error = str(exc)
            result_obj.completed_at = time.time()
            self._stats["failed"] += 1
        finally:
            # বাংলা মন্তব্য: সবসময় event set — caller কখনো stuck থাকবে না
            self._mark_complete(task_id)

    async def _submit_to_asyncio(self, func: Callable, task_id: str, args: tuple, kwargs: dict) -> None:
        """বাংলা মন্তব্য: Local asyncio queue-এ submit এবং worker ensure।"""
        await self.local_queue.put((func, task_id, args, kwargs))
        if self._worker_task is None or self._worker_task.done():
            self._worker_task = asyncio.create_task(self._asyncio_worker(), name=f"task_queue_worker_{int(time.time())}")

    async def _asyncio_worker(self) -> None:
        """
        বাংলা মন্তব্য: Gracefully cancellable worker।
        আগের ভাঙা কোড: while True — infinite loop।
        এখন: shutdown_event check সহ clean exit।
        """
        logger.info("[TaskQueue] AsyncIO worker started.")
        while not self._shutdown_event.is_set():
            try:
                # বাংলা মন্তব্য: timeout দিয়ে wait — shutdown signal check করা যাবে
                func, task_id, args, kwargs = await asyncio.wait_for(self.local_queue.get(), timeout=1.0)
                try:
                    await self._execute_task(func, task_id, args, kwargs)
                finally:
                    self.local_queue.task_done()

            except TimeoutError:
                # বাংলা মন্তব্য: queue empty — loop continue, shutdown check হবে
                continue
            except asyncio.CancelledError:
                # বাংলা মন্তব্য: CancelledError = graceful shutdown signal
                logger.info("[TaskQueue] AsyncIO worker received cancellation. Shutting down.")
                raise  # re-raise — কখনো suppress করা যাবে না
            except Exception as exc:  # noqa: BLE001
                # বাংলা মন্তব্য: worker crash — log করা এবং continue
                logger.exception(f"[TaskQueue] AsyncIO worker unexpected error: {exc}")
                await asyncio.sleep(1)  # brief pause before retry

        logger.info("[TaskQueue] AsyncIO worker shutdown complete.")

    async def _submit_to_redis(
        self,
        func: Callable,
        task_id: str,
        args: tuple,
        kwargs: dict,
        priority: TaskPriority,
    ) -> None:
        """বাংলা মন্তব্য: Redis sorted set-এ task push। Lazy import।"""
        import redis.asyncio as aioredis  # lazy import — module level নয়

        client = aioredis.from_url(self.redis_url, decode_responses=True)
        task_data = {
            "task_id": task_id,
            "function": f"{func.__module__}.{func.__name__}",
            "args": list(args),
            "kwargs": kwargs,
            "priority": priority.value,
            "timestamp": time.time(),
        }
        score = -priority.value  # higher priority = lower score = earlier dequeue
        async with client:
            await client.zadd("supremeai:task_queue", {json.dumps(task_data): score})
            await client.hset("supremeai:task_metadata", task_id, json.dumps(task_data))

    async def _submit_to_pubsub(
        self,
        func: Callable,
        task_id: str,
        args: tuple,
        kwargs: dict,
        priority: TaskPriority,
    ) -> None:
        """বাংলা মন্তব্য: GCP Pub/Sub publish। Blocking call-কে thread-এ offload।"""
        from google.cloud import pubsub_v1  # lazy import

        publisher = pubsub_v1.PublisherClient()
        topic_path = publisher.topic_path(self.project_id, "supremeai-tasks")
        message_data = {
            "task_id": task_id,
            "function": f"{func.__module__}.{func.__name__}",
            "args": list(args),
            "kwargs": kwargs,
            "priority": priority.value,
            "timestamp": time.time(),
        }
        payload = json.dumps(message_data).encode("utf-8")
        future = publisher.publish(topic_path, payload, priority=str(priority.value), task_id=task_id)
        # বাংলা মন্তব্য: blocking future.result() → thread pool offload
        message_id = await asyncio.to_thread(future.result, 30)
        logger.debug(f"[TaskQueue] Pub/Sub message {message_id} for task {task_id}")

    async def _submit_to_celery(
        self,
        func: Callable,
        task_id: str,
        args: tuple,
        kwargs: dict,
        priority: TaskPriority,
    ) -> None:
        """বাংলা মন্তব্য: Celery task submit — lazy import।"""
        from celery import Celery  # lazy import

        celery_app = Celery("supremeai_tasks", broker=self.redis_url, backend=self.redis_url)
        # বাংলা মন্তব্য: Celery send_task — function reference safe
        await asyncio.to_thread(
            celery_app.send_task,
            func.__name__,
            args=args,
            kwargs=kwargs,
            priority=priority.value,
        )

    async def shutdown(self) -> None:
        """বাংলা মন্তব্য: Graceful shutdown — worker task cancel করা।"""
        logger.info("[TaskQueue] Initiating graceful shutdown...")
        self._shutdown_event.set()
        if self._worker_task and not self._worker_task.done():
            self._worker_task.cancel()
            with contextlib.suppress(asyncio.CancelledError):
                await self._worker_task
                # বাংলা মন্তব্য: expected — worker successfully cancelled
        logger.info("[TaskQueue] Shutdown complete.")

    def get_status(self, task_id: str) -> str:
        result = self._results.get(task_id)
        return result.status if result else "unknown"

    def cancel_task(self, task_id: str) -> bool:
        result = self._results.get(task_id)
        if result and result.status == "pending":
            result.status = "cancelled"
            self._mark_complete(task_id)
            return True
        return False

    def get_stats(self) -> dict[str, int]:
        return {**self._stats, "current_queue_depth": self.local_queue.qsize()}

    async def cleanup_old_tasks(self, max_age_hours: int = 24) -> None:
        """বাংলা মন্তব্য: Periodic cleanup — old completed/failed tasks remove।"""
        cutoff_time = time.time() - (max_age_hours * 3600)
        to_remove = [
            tid
            for tid, result in self._results.items()
            if result.status in ("completed", "failed", "cancelled") and result.completed_at and result.completed_at < cutoff_time
        ]
        for tid in to_remove:
            self._tasks.pop(tid, None)
            self._results.pop(tid, None)
            self._completion_events.pop(tid, None)
        if to_remove:
            logger.info(f"[TaskQueue] Cleaned up {len(to_remove)} old tasks.")


# ── Singleton factory ─────────────────────────────────────────────────────────
_task_queue_instance: TaskQueue | None = None


def get_task_queue() -> TaskQueue:
    """বাংলা মন্তব্য: Lazy singleton — import time-এ initialization নিষিদ্ধ।"""
    global _task_queue_instance
    if _task_queue_instance is None:
        _task_queue_instance = TaskQueue()
    return _task_queue_instance


# ── Public convenience API ────────────────────────────────────────────────────
async def submit_task(func: Callable, *args, **kwargs) -> str:
    return await get_task_queue().submit_task(func, *args, **kwargs)


async def get_task_result(task_id: str, timeout: float | None = None) -> TaskResult:
    return await get_task_queue().get_result(task_id, timeout)


def get_task_status(task_id: str) -> str:
    return get_task_queue().get_status(task_id)


def cancel_task(task_id: str) -> bool:
    return get_task_queue().cancel_task(task_id)


def get_queue_stats() -> dict[str, int]:
    return get_task_queue().get_stats()

```