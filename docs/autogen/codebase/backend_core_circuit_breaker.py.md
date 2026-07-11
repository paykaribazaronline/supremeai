# 📄 ফাইল: backend/core/circuit_breaker.py

**প্রকার:** .py  
**সাইজ:** 5,955 বাইট  
**আপডেট:** 2026-07-11T13:49:08.312724

---

## কোড

```py
from __future__ import annotations

import json
import threading
import time
import uuid
from collections.abc import Callable
from datetime import datetime
from typing import Any
from typing import TypeVar

from loguru import logger

from core.log_batcher import batcher


# বাংলা মন্তব্য: BLE001 ফিক্স — নির্দিষ্ট এরর টাইপ ব্যবহার করা হয়েছে ব্লাইন্ড এক্সেপশন এভয়ড করার জন্য
SPECIFIC_EXCEPTIONS = (ConnectionError, TimeoutError, ValueError)


T = TypeVar("T")


class CircuitBreaker:
    def __init__(
        self,
        name: str,
        failure_threshold: int = 5,
        recovery_timeout: float = 20.0,
        half_open_after: float = 10.0,
        redis_queue: Any = None,
    ) -> None:
        self.name = name
        self.failure_threshold = failure_threshold
        self.recovery_timeout = recovery_timeout
        self.half_open_after = half_open_after
        self.failures = 0
        self.state = "CLOSED"
        self.opened_at: float | None = None
        self.last_failure_at: float | None = None
        self.redis_queue = redis_queue
        self._key_prefix = f"cb:{name}"
        self._restore_from_redis()
        self._lock = threading.Lock()
        self._half_open_in_flight = 0

    def _restore_from_redis(self) -> None:
        if not self.redis_queue or not getattr(self.redis_queue, "configured", False):
            return
        try:
            raw = self.redis_queue.get(f"{self._key_prefix}:state")
            if raw:
                data = json.loads(raw)
                self.failures = int(data.get("failures", 0))
                self.state = data.get("state", "CLOSED")
                self.opened_at = data.get("opened_at")
                self.last_failure_at = data.get("last_failure_at")
        except SPECIFIC_EXCEPTIONS as exc:
            # বাংলা মন্তব্য: রেডিস থেকে স্টেট রিস্টোর করার সময় নির্দিষ্ট এররগুলো ক্যাচ করা হয়েছে
            # সমস্যা ট্র্যাক করার জন্য নরমালি ডিবাগ লগ করা হল যত রেডিস সমস্যা দশযমন থাকে
            logger.debug(f"CircuitBreaker redis restore failed: {exc}")

    def _persist_to_redis(self) -> None:
        if not self.redis_queue or not getattr(self.redis_queue, "configured", False):
            return
        try:
            data = {
                "failures": self.failures,
                "state": self.state,
                "opened_at": self.opened_at,
                "last_failure_at": self.last_failure_at,
            }
            self.redis_queue.set(f"{self._key_prefix}:state", json.dumps(data), ex=600)
        except SPECIFIC_EXCEPTIONS as exc:
            # বাংলা মন্তব্য: রেডিসে পার্সিস্ট করার সময় নির্দিষ্ট এররগুলো ক্যাচ করা হয়েছে
            # সমস্যা ট্র্যাক করার জন্য নরমালি ডিবাগ লগ করা হল
            logger.debug(f"CircuitBreaker redis persist failed: {exc}")

    def allow_request(self) -> bool:
        if self.state == "OPEN":
            if self.opened_at is not None and (time.time() - self.opened_at) >= self.recovery_timeout:
                self.state = "HALF_OPEN"
                self._persist_to_redis()
                return True
            return False
        if self.state == "HALF_OPEN":
            with self._lock:
                if self._half_open_in_flight >= 1:
                    return False
                self._half_open_in_flight += 1
                return True
        return True

    def mark_success(self) -> None:
        with self._lock:
            self._half_open_in_flight = max(0, self._half_open_in_flight - 1)
        if self.state != "CLOSED":
            self._emit_alert("CIRCUIT_CLOSED")
        self.failures = 0
        self.state = "CLOSED"
        self.opened_at = None
        self.last_failure_at = None
        self._persist_to_redis()

    def mark_failure(self) -> None:
        with self._lock:
            self._half_open_in_flight = max(0, self._half_open_in_flight - 1)
        now = time.time()
        self.last_failure_at = now
        self.failures += 1
        if self.failures >= self.failure_threshold and self.state != "OPEN":
            self.state = "OPEN"
            self.opened_at = now
            self._emit_alert("CIRCUIT_OPEN")
        self._persist_to_redis()

    def _emit_alert(self, status: str) -> None:
        try:
            log_entry = {
                "id": str(uuid.uuid4()),
                "session_id": "swarm_health",
                "log_type": "alert",
                "message": f"{self.name}: {status}",
                "created_at": datetime.utcnow().isoformat(),
                "model": self.name,
                "status": status,
            }
            batcher.emit(log_entry)
        except SPECIFIC_EXCEPTIONS as e:
            # বাংলা মন্তব্য: অ্যালার্ট ইমিট করার সময় নির্দিষ্ট এররগুলো ক্যাচ করা হয়েছে
            logger.debug(f"Failed to emit alert: {e}")

    async def call(self, func: Callable[..., T], *args: object, **kwargs: object) -> T:
        if not self.allow_request():
            raise RuntimeError(f"Circuit breaker {self.name} is open")
        try:
            result = await func(*args, **kwargs)
            self.mark_success()
            return result
        except Exception:  # noqa: BLE001
            self.mark_failure()
            raise

```