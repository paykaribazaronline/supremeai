# backend/core/llm/telemetry.py
"""LLM Call Telemetry — structured logging for every gateway call.

Stores a JSON-log line per call with: timestamp, session_id, provider,
model, task_type, latency_ms, tokens (if available), cost, success.

This is the data source for future self-evolving routing policies.
"""
from __future__ import annotations

import json
import time
from contextlib import asynccontextmanager
from dataclasses import dataclass, field, asdict
from datetime import datetime, timezone
from typing import Any, AsyncIterator

try:
    from loguru import logger
except ImportError:
    import logging
    logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
    logger = logging.getLogger("llm_telemetry")


@dataclass
class LLMCallRecord:
    """Immutable record of a single LLM gateway call."""

    timestamp: str = field(
        default_factory=lambda: datetime.now(timezone.utc).isoformat()
    )
    session_id: str = ""
    provider: str = ""
    model: str = ""
    task_type: str = "general"
    latency_ms: float = 0.0
    tokens_prompt: int | None = None
    tokens_completion: int | None = None
    cost_usd: float = 0.0
    success: bool = True
    error: str | None = None
    metadata: dict[str, Any] = field(default_factory=dict)

    def to_log_line(self) -> str:
        return json.dumps(asdict(self))


@asynccontextmanager
async def track_llm_call(
    *,
    session_id: str = "",
    provider: str = "",
    model: str = "",
    task_type: str = "general",
    metadata: dict[str, Any] | None = None,
) -> AsyncIterator[LLMCallRecord]:
    """Context manager that times an LLM call and emits a structured log."""
    record = LLMCallRecord(
        session_id=session_id,
        provider=provider,
        model=model,
        task_type=task_type,
        metadata=metadata or {},
    )
    t0 = time.perf_counter()
    try:
        yield record
        record.success = True
    except Exception as exc:
        record.success = False
        record.error = str(exc)[:500]
        raise
    finally:
        record.latency_ms = round((time.perf_counter() - t0) * 1000, 2)
        try:
            logger.bind(llm_telemetry=record.to_log_line()).info("llm_call")
        except Exception:
            logger.info(f"llm_call: {record.to_log_line()}")
