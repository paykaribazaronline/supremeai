from __future__ import annotations

import os
import time
import json
from typing import Callable, Optional, TypeVar, Dict, Any

T = TypeVar('T')


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
        self.state = 'CLOSED'
        self.opened_at: Optional[float] = None
        self.last_failure_at: Optional[float] = None
        self.redis_queue = redis_queue
        self._key_prefix = f"cb:{name}"
        self._restore_from_redis()

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
        except Exception:
            pass

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
        except Exception:
            pass

    def allow_request(self) -> bool:
        if self.state == 'OPEN':
            if self.opened_at is not None and (time.time() - self.opened_at) >= self.recovery_timeout:
                self.state = 'HALF_OPEN'
                self._persist_to_redis()
                return True
            return False
        if self.state == 'HALF_OPEN':
            return True
        return True

    def mark_success(self) -> None:
        self.failures = 0
        self.state = 'CLOSED'
        self.opened_at = None
        self.last_failure_at = None
        self._persist_to_redis()

    def mark_failure(self) -> None:
        now = time.time()
        self.last_failure_at = now
        self.failures += 1
        if self.failures >= self.failure_threshold:
            self.state = 'OPEN'
            self.opened_at = now
        self._persist_to_redis()

    async def call(self, func: Callable[..., T], *args: object, **kwargs: object) -> T:
        if not self.allow_request():
            raise RuntimeError(f'Circuit breaker {self.name} is open')
        try:
            result = await func(*args, **kwargs)
            self.mark_success()
            return result
        except Exception:
            self.mark_failure()
            raise
