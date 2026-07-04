# 📄 ফাইল: backend/core/error_remediation.py

**প্রকার:** .py  
**সাইজ:** 5,066 বাইট  
**আপডেট:** 2026-07-04T08:34:55.068999

---

## কোড

```py
import asyncio
import json
import os
import time
from pathlib import Path

from loguru import logger


try:
    from qdrant_client import QdrantClient
    HAS_QDRANT = True
except ImportError:
    HAS_QDRANT = False


class CircuitBreaker:
    def __init__(self, failure_threshold: int = 3, recovery_timeout: float = 60.0):
        self.failure_threshold = failure_threshold
        self.recovery_timeout = recovery_timeout
        self.failure_count = 0
        self.last_failure_time: float | None = None
        self.state = "closed"

    def allow_request(self) -> bool:
        if self.state == "open":
            if self.last_failure_time and (time.monotonic() - self.last_failure_time) >= self.recovery_timeout:
                self.state = "half-open"
                return True
            return False
        return True

    def record_success(self):
        self.failure_count = 0
        self.state = "closed"

    def record_failure(self):
        self.failure_count += 1
        self.last_failure_time = time.monotonic()
        if self.failure_count >= self.failure_threshold:
            self.state = "open"


class ErrorRemediation:
    def __init__(self) -> None:
        self.qdrant: QdrantClient | None = None
        if HAS_QDRANT:
            url = os.getenv("QDRANT_URL", "localhost")
            self.qdrant = QdrantClient(url=url, prefer_grpc=False)

        self.circuit_breaker = CircuitBreaker(failure_threshold=3, recovery_timeout=60.0)
        self.fallback_path = Path(__file__).parent.parent / "data" / "error_remediation_fallback.json"
        self._ensure_fallback_file()

    def _ensure_fallback_file(self) -> None:
        try:
            self.fallback_path.parent.mkdir(parents=True, exist_ok=True)
            if not self.fallback_path.exists():
                with open(self.fallback_path, "w", encoding="utf-8") as f:
                    json.dump({"default_fix": "Retry with exponential backoff"}, f, indent=2)
        except Exception:
            pass

    def _load_local_fallback(self) -> str | None:
        try:
            with open(self.fallback_path, encoding="utf-8") as f:
                data = json.load(f)
            return data.get("default_fix") or data.get("fallbacks", {}).get("default")
        except Exception as exc:
            # বল মনতবয: ফলবযক ফইল পড়ত বযরথ হল আগ নরবই None রটরন করত;
            # এখন কন কর ফলবযক অকরযকর হল ত ডবগ লগ কর দশযমন কর হল
            logger.debug(f"Local fallback load failed from {self.fallback_path}: {exc}")
            return None

    async def _backoff_retry(self, operation, max_attempts: int = 3, base_delay: float = 0.5):
        # বল মনতবয: শষ ব‍্যরথতর exception ধর রখর জন‍্য last_exception ইনশয়লইজ কর হল,
        # নহল লপর পর এই ভরযবল undefined থকত (ruff F821) ও চডনত এরর লগ কর যত ন
        last_exception: Exception | None = None
        for attempt in range(1, max_attempts + 1):
            if not self.circuit_breaker.allow_request():
                logger.warning("Circuit breaker open; skipping Qdrant lookup.")
                return None
            try:
                result = await operation()
                self.circuit_breaker.record_success()
                return result
            except Exception as exc:
                last_exception = exc
                self.circuit_breaker.record_failure()
                logger.debug(f"Qdrant lookup attempt {attempt} failed: {exc}")
                if attempt < max_attempts:
                    await asyncio.sleep(min(base_delay * (2 ** (attempt - 1)), 5.0))
        # বল মনতবয: সব রটর শষ হওয়র পর last_exception কখনই বযবহত হত ন (নরব সযলপ);
        # এখন চডনত বযরথতর করণ warning হসব লগ কর হয় যত ডবগ কর সহজ হয়
        if last_exception is not None:
            logger.warning(
                f"Qdrant lookup exhausted {max_attempts} attempts; "
                f"falling back. Last error: {last_exception}"
            )
        return None

    async def lookup_fix(self, error_sig: str) -> str | None:
        if not self.qdrant or not self.circuit_breaker.allow_request():
            logger.warning("Qdrant client not available or circuit breaker is open. Using local fallback.")
            return self._load_local_fallback()

        async def _search():
            return self.qdrant.search(
                collection_name="error_patterns",
                query_vector=[0.0] * 384,  # Placeholder for actual embedding
                limit=1,
            )

        results = await self._backoff_retry(_search)
        if results:
            return results[0].payload.get("fix")
        return self._load_local_fallback()

```