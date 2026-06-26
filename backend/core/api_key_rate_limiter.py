from __future__ import annotations

import time


class APIKeyRateLimiter:
    """Sliding-window rate limiter scoped per API key prefix."""

    def __init__(self, requests_per_minute: int = 60, burst: int = 20) -> None:
        self.requests_per_minute = requests_per_minute
        self.burst = burst
        self.window = 60.0
        self._hits: dict[str, list[float]] = {}

    def _cleanup(self, key: str, now: float) -> None:
        self._hits[key] = [t for t in self._hits.get(key, []) if now - t < self.window]

    def is_allowed(self, key_prefix: str, rps: int = 6) -> bool:
        now = time.time()
        self._cleanup(key_prefix, now)
        hits = self._hits.setdefault(key_prefix, [])
        if len(hits) >= self.burst:
            return False
        hits.append(now)
        return True

    def remaining(self, key_prefix: str) -> int:
        now = time.time()
        self._cleanup(key_prefix, now)
        return max(0, self.burst - len(self._hits.get(key_prefix, [])))

