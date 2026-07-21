"""This module provides robust error remediation strategies for the
SupremeAI project, combining a vector database (Qdrant) for dynamic fix
lookups with a resilient local fallback mechanism. It ensures system stability
by incorporating circuit breaking and exponential backoff for external service
interactions, offering automated suggestions for known error patterns to
enhance the overall reliability of the AI ecosystem.

Key Components:
- `ErrorRemediation`: Central class for managing error remediation
  strategies, including Qdrant integration, circuit breaking, and local
  fallback logic.
- `_ensure_fallback_file()`: Initializes and ensures the presence of the
  local JSON file used for fallback error fixes.
- `_load_local_fallback()`: Retrieves a default error fix from the module's
  local fallback JSON file.
- `_backoff_retry()`: Async utility that retries a given operation with
  exponential backoff, respecting the circuit breaker's state.
- `lookup_fix()`: Primary method to find a remediation fix for a given error
  signature, leveraging Qdrant or falling back to a local solution.
- `_compute_embedding()`: Generates a vector embedding from an error signature
  string using sentence-transformers (preferred) or a deterministic hash-based
  fallback.

Fixes Applied (Autonomous Architecture Audit):
- 🛑 [CRITICAL] Removed zero-vector placeholder `[0.0] * 384` — replaced with proper embedding function
- 🔴 [HIGH] Added collection auto-creation with correct vector size (384 dimensions)
- 🟡 [MEDIUM] Qdrant URL now uses `settings` instead of raw `os.getenv()`
- 🟡 [MEDIUM] Lazy Qdrant client initialization to avoid unnecessary memory allocation
- 🟢 [LOW] Added `insert_error_pattern()` for self-healing capability
"""

import asyncio
import hashlib
import json
import os
from collections.abc import Callable
from pathlib import Path

from core.messaging.event_bus import ErrorContext, ErrorEvent, error_event_bus
from core.resilience.circuit_breaker import CircuitBreaker
from loguru import logger

# ── Embedding Provider ─────────────────────────────────────────────────────────
_SENTENCE_TRANSFORMER_AVAILABLE = False
_SENTENCE_TRANSFORMER_MODEL = None

try:
    from sentence_transformers import SentenceTransformer

    _SENTENCE_TRANSFORMER_AVAILABLE = True
except ImportError:
    _SENTENCE_TRANSFORMER_AVAILABLE = False


def _compute_embedding(text: str, vector_size: int = 384) -> list[float]:
    """Generate a dense vector embedding from an error signature string.

    Priority:
    1. sentence-transformers (if available) — produces meaningful semantic embeddings
    2. Deterministic hash-based fallback — produces reproducible pseudo-embeddings

    Args:
        text: Input error signature string.
        vector_size: Target embedding dimensionality (default: 384 for Qdrant compatibility).

    Returns:
        A list of floats of length `vector_size` representing the embedding.
    """
    global _SENTENCE_TRANSFORMER_MODEL  # noqa: PLW0603

    if _SENTENCE_TRANSFORMER_AVAILABLE:
        try:
            if _SENTENCE_TRANSFORMER_MODEL is None:
                # বাংলা মন্তব্য: Lazy load — all-MiniLM-L6-v2 (384-dim) Qdrant-এর সাথে compatible
                _SENTENCE_TRANSFORMER_MODEL = SentenceTransformer("all-MiniLM-L6-v2")
            embedding = _SENTENCE_TRANSFORMER_MODEL.encode(
                text, normalize_embeddings=True
            )
            return embedding.tolist()  # type: ignore[union-attr]
        except Exception as exc:  # noqa: BLE001
            logger.debug(
                f"sentence-transformers encode failed, falling back to hash embedding: {exc}"
            )

    # ── Deterministic hash-based fallback ──────────────────────────────────────
    # বাংলা মন্তব্য: SHA-256-এর প্রতিটি বাইটকে [0,1] রেঞ্জে normalize করে
    # reproducible pseudo-embedding তৈরি করা হয়। এটি semantic নয় কিন্তু
    # identical error strings-এর জন্য consistent result দেয়।
    raw = hashlib.sha256(text.encode("utf-8")).digest()  # 32 bytes
    result: list[float] = []
    for i in range(vector_size):
        idx = i % len(raw)
        # Map byte [0,255] → float [-1.0, 1.0]
        normalized = (raw[idx] / 127.5) - 1.0
        result.append(round(normalized, 6))
    return result


# ── Qdrant Integration ────────────────────────────────────────────────────────

try:
    from qdrant_client import QdrantClient
    from qdrant_client.http import models as qdrant_models
    from qdrant_client.http.exceptions import UnexpectedResponse

    HAS_QDRANT = True
except ImportError:
    HAS_QDRANT = False

# Qdrant default collection configuration
QDRANT_COLLECTION_NAME = "error_patterns"
QDRANT_VECTOR_SIZE = 384
QDRANT_DISTANCE = qdrant_models.Distance.COSINE if HAS_QDRANT else None


class ErrorRemediation:
    """Self-healing error remediation engine with Qdrant vector search + local fallback.

    Usage::
        remediator = ErrorRemediation()
        fix = await remediator.lookup_fix("ValueError: invalid literal for int()")
    """

    def __init__(self) -> None:
        self._qdrant: QdrantClient | None = None
        self._qdrant_initialized: bool = False
        self.fallback_path = (
            Path(__file__).parent.parent / "data" / "error_remediation_fallback.json"
        )
        self.circuit_breaker = CircuitBreaker(
            name="qdrant", failure_threshold=3, recovery_timeout=60.0
        )
        self._ensure_fallback_file()

    # ── Lazy Qdrant initializer ────────────────────────────────────────────────

    def _init_qdrant(self) -> None:
        """Initialize Qdrant client with proper collection auto-creation.

        This is intentionally a separate method (not in __init__) so that
        the Qdrant client is only instantiated when `lookup_fix()` is actually
        called. This avoids memory waste when `HAS_QDRANT=False` or when
        the application only uses the local fallback.
        """
        if self._qdrant_initialized:
            return
        if not HAS_QDRANT:
            self._qdrant_initialized = True
            return

        try:
            # Use settings if available, fallback to env var
            qdrant_url: str = ""
            try:
                from core.config import settings as app_settings

                qdrant_url = getattr(app_settings, "qdrant_url", "")
            except ImportError:
                pass
            if not qdrant_url:
                qdrant_url = os.getenv("QDRANT_URL", "http://localhost:6333")

            self._qdrant = QdrantClient(url=qdrant_url, prefer_grpc=False)
            self._ensure_qdrant_collection()
            self._qdrant_initialized = True
            logger.info(f"✅ Qdrant error remediation client connected ({qdrant_url})")
        except Exception as exc:  # noqa: BLE001
            logger.warning(
                f"⚠️ Qdrant initialization failed: {exc}. Using local fallback only."
            )
            self._qdrant = None
            self._qdrant_initialized = True

    def _ensure_qdrant_collection(self) -> None:
        """Auto-create the error_patterns Qdrant collection if it doesn't exist."""
        if not self._qdrant:
            return
        try:
            collections = self._qdrant.get_collections().collections
            existing_names = {c.name for c in collections}
            if QDRANT_COLLECTION_NAME not in existing_names:
                self._qdrant.recreate_collection(
                    collection_name=QDRANT_COLLECTION_NAME,
                    vectors_config=qdrant_models.VectorParams(
                        size=QDRANT_VECTOR_SIZE,
                        distance=QDRANT_DISTANCE or qdrant_models.Distance.COSINE,
                    ),
                )
                logger.info(
                    f"✅ Created Qdrant collection '{QDRANT_COLLECTION_NAME}' (size={QDRANT_VECTOR_SIZE})"
                )
        except UnexpectedResponse as exc:
            logger.warning(
                f"Qdrant collection check failed (may already exist or service unavailable): {exc}"
            )

    # ── Fallback file management ───────────────────────────────────────────────

    def _ensure_fallback_file(self) -> None:
        try:
            self.fallback_path.parent.mkdir(parents=True, exist_ok=True)
            if not self.fallback_path.exists():
                with open(self.fallback_path, "w", encoding="utf-8") as f:
                    json.dump(
                        {
                            "default_fix": "Retry with exponential backoff",
                            "fallbacks": {
                                "default": "Retry with exponential backoff",
                                "timeout": "Increase timeout or reduce payload size",
                                "rate_limit": "Implement exponential backoff with jitter",
                                "auth_error": "Verify API key and permissions",
                                "connection_refused": "Check service availability and firewall rules",
                            },
                        },
                        f,
                        indent=2,
                    )
        except OSError as e:
            logger.warning(
                f"Failed to create fallback file at {self.fallback_path}: {e}"
            )

    def _load_local_fallback(self, error_sig: str | None = None) -> str | None:
        """Load a fallback fix from the local JSON file.

        If `error_sig` is provided, it attempts a keyword-based match against
        the ``fallbacks`` dictionary keys; otherwise returns the default.
        """
        try:
            with open(self.fallback_path, encoding="utf-8") as f:
                data: dict = json.load(f)
            # Try keyword-specific fallback first
            if error_sig:
                sig_lower = error_sig.lower()
                for keyword, fix in data.get("fallbacks", {}).items():
                    if keyword in sig_lower:
                        return fix
            return data.get("default_fix") or data.get("fallbacks", {}).get("default")
        except Exception as exc:  # noqa: BLE001
            logger.debug(f"Local fallback load failed from {self.fallback_path}: {exc}")
            return None

    # ── Circuit-breaking retry ─────────────────────────────────────────────────

    async def _backoff_retry(
        self, operation: Callable, max_attempts: int = 3, base_delay: float = 0.5
    ):
        """Execute an async operation with exponential backoff and circuit breaking.

        Args:
            operation: An async callable to execute.
            max_attempts: Maximum retry attempts.
            base_delay: Initial delay in seconds; doubles each attempt.

        Returns:
            The operation result, or ``None`` if all attempts failed.
        """
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
                logger.debug(
                    f"Qdrant lookup attempt {attempt}/{max_attempts} failed: {exc}"
                )
                if attempt < max_attempts:
                    await asyncio.sleep(min(base_delay * (2 ** (attempt - 1)), 5.0))

        if last_exception is not None:
            logger.warning(
                f"Qdrant lookup exhausted {max_attempts} attempts; falling back. Last error: {last_exception}"
            )
        return None

    # ── Public API ─────────────────────────────────────────────────────────────

    async def lookup_fix(self, error_sig: str) -> str | None:
        """Find a remediation fix for a given error signature.

        Strategy:
        1. If Qdrant is available and circuit breaker is closed → vector search
        2. Fallback → local JSON file (keyword-matched or default)

        Args:
            error_sig: The error signature string to look up.

        Returns:
            A remediation suggestion string, or ``None`` if no fix is known.
        """
        # Lazy-init Qdrant on first call
        self._init_qdrant()

        if not self._qdrant or not self.circuit_breaker.allow_request():
            reason = (
                "Qdrant unavailable" if not self._qdrant else "Circuit breaker open"
            )
            logger.debug(f"{reason}. Using local fallback.")
            error_event_bus.emit(
                ErrorEvent(
                    module="error_remediation",
                    error_type="QDRANT_LOOKUP_SKIPPED",
                    message=reason,
                    severity="WARNING",
                    structured_context=ErrorContext(
                        module="error_remediation", extra={"error_sig": error_sig[:200]}
                    ),
                )
            )
            return self._load_local_fallback(error_sig)

        embedding = _compute_embedding(error_sig, QDRANT_VECTOR_SIZE)

        async def _search():
            # বাংলা মন্তব্য: Proper embedding-based search — no more zero vectors!
            return self._qdrant.search(
                collection_name=QDRANT_COLLECTION_NAME,
                query_vector=embedding,
                limit=1,
                score_threshold=0.6,  # Only return high-confidence matches
            )

        results = await self._backoff_retry(_search)
        if results and results[0].payload:
            fix: str | None = results[0].payload.get("fix")
            if fix:
                logger.info(
                    f"✅ Found Qdrant remediation (score={results[0].score:.3f})"
                )
                return fix

        error_event_bus.emit(
            ErrorEvent(
                module="error_remediation",
                error_type="QDRANT_NO_FIX_FOUND",
                message="No remediation found in Qdrant for error signature",
                severity="INFO",
                structured_context=ErrorContext(
                    module="error_remediation", extra={"error_sig": error_sig[:200]}
                ),
            )
        )
        return self._load_local_fallback(error_sig)

    async def insert_error_pattern(
        self,
        error_sig: str,
        fix: str,
        metadata: dict | None = None,
    ) -> bool:
        """Insert a new error-remediation pattern into Qdrant for future self-healing.

        Args:
            error_sig: The error signature string (e.g., exception message).
            fix: The remediation suggestion.
            metadata: Optional metadata (e.g., ``{"severity": "HIGH", "module": "billing"}``).

        Returns:
            ``True`` if the pattern was inserted successfully, ``False`` otherwise.
        """
        self._init_qdrant()
        if not self._qdrant:
            logger.warning("Qdrant not available — cannot insert error pattern.")
            return False

        embedding = _compute_embedding(error_sig, QDRANT_VECTOR_SIZE)
        payload: dict = {"error_sig": error_sig[:500], "fix": fix}
        if metadata:
            payload.update(metadata)

        try:
            self._qdrant.upsert(
                collection_name=QDRANT_COLLECTION_NAME,
                points=[
                    qdrant_models.PointStruct(
                        id=abs(hash(error_sig)) % (10**12),
                        vector=embedding,
                        payload=payload,
                    )
                ],
            )
            logger.info(
                f"✅ Inserted error pattern ({len(embedding)}-dim): {error_sig[:80]}"
            )
            error_event_bus.emit(
                ErrorEvent(
                    module="error_remediation",
                    error_type="ERROR_PATTERN_INSERTED",
                    message="Error pattern inserted into Qdrant",
                    severity="INFO",
                    structured_context=ErrorContext(
                        module="error_remediation",
                        extra={"error_sig": error_sig[:200], "fix_len": len(fix)},
                    ),
                )
            )
            return True
        except Exception as exc:  # noqa: BLE001
            logger.error(f"Failed to insert error pattern into Qdrant: {exc}")
            error_event_bus.emit(
                ErrorEvent(
                    module="error_remediation",
                    error_type="ERROR_PATTERN_INSERT_FAILED",
                    message=f"Failed to insert error pattern into Qdrant: {exc}",
                    severity="ERROR",
                    structured_context=ErrorContext(
                        module="error_remediation",
                        extra={
                            "error_sig": error_sig[:200],
                            "exception": str(exc)[:200],
                        },
                    ),
                )
            )
            return False


# ── Singleton (shared instance) ───────────────────────────────────────────────
error_remediator = ErrorRemediation()
lookup_fix = error_remediator.lookup_fix
insert_error_pattern = error_remediator.insert_error_pattern
