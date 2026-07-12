import asyncio
import json
import os
import time
from pathlib import Path

from loguru import logger
from core.circuit_breaker import CircuitBreaker

try:
    from qdrant_client import QdrantClient

    HAS_QDRANT = True
except ImportError:
    HAS_QDRANT = False

class ErrorRemediation:
    def __init__(self) -> None:
        self.qdrant: QdrantClient | None = None
        if HAS_QDRANT:
            url = os.getenv("QDRANT_URL", "localhost")
            self.qdrant = QdrantClient(url=url, prefer_grpc=False)

        self.circuit_breaker = CircuitBreaker(name="qdrant", failure_threshold=3, recovery_timeout=60.0)
        self.fallback_path = Path(__file__).parent.parent / "data" / "error_remediation_fallback.json"
        self._ensure_fallback_file()

    def _ensure_fallback_file(self) -> None:
        try:
            self.fallback_path.parent.mkdir(parents=True, exist_ok=True)
            if not self.fallback_path.exists():
                with open(self.fallback_path, "w", encoding="utf-8") as f:
                    json.dump({"default_fix": "Retry with exponential backoff"}, f, indent=2)
        except Exception as e:  # noqa: BLE001
            import logging

            logging.warning(f"Exception suppressed: {e}")

    def _load_local_fallback(self) -> str | None:
        try:
            with open(self.fallback_path, encoding="utf-8") as f:
                data = json.load(f)
            return data.get("default_fix") or data.get("fallbacks", {}).get("default")
        except Exception as exc:  # noqa: BLE001
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
                self.circuit_breaker.mark_success()
                return result
            except Exception as exc:  # noqa: BLE001
                last_exception = exc
                self.circuit_breaker.mark_failure()
                logger.debug(f"Qdrant lookup attempt {attempt} failed: {exc}")
                if attempt < max_attempts:
                    await asyncio.sleep(min(base_delay * (2 ** (attempt - 1)), 5.0))
        # বল মনতবয: সব রটর শষ হওয়র পর last_exception কখনই বযবহত হত ন (নরব সযলপ);
        # এখন চডনত বযরথতর করণ warning হসব লগ কর হয় যত ডবগ কর সহজ হয়
        if last_exception is not None:
            logger.warning(f"Qdrant lookup exhausted {max_attempts} attempts; falling back. Last error: {last_exception}")
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
