# backend/core/llm/telemetry.py
"""LLM Call Telemetry — structured logging for every gateway call.

Stores a JSON-log line per call with: timestamp, session_id, provider,
model, task_type, latency_ms, tokens (if available), cost, success.

This is the data source for future self-evolving routing policies.
"""
from __future__ import annotations

import contextlib
import json
import time
from contextlib import asynccontextmanager
from dataclasses import dataclass, field, asdict
from datetime import datetime, UTC
from typing import Any
from collections.abc import AsyncIterator

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
        default_factory=lambda: datetime.now(UTC).isoformat()
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
        # বাংলা মন্তব্য: default=str — provider থেকে আসা কোনো field (যেমন usage.prompt_tokens)
        # যদি plain int/str/float না হয়ে কোনো non-JSON-native object হয়, json.dumps যেন
        # crash না করে বরং str() রূপান্তর করে log করে। টেলিমেট্রি সিরিয়ালাইজেশন কখনো
        # আসল LLM কলের ফলাফলকে mask/replace করবে না — এটা শুধু একটা log line, critical path না।
        return json.dumps(asdict(self), default=str)


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
        # বাংলা মন্তব্য: telemetry logging সম্পূর্ণ best-effort — এই ব্লকের কোনো ব্যর্থতা
        # (log_line সিরিয়ালাইজেশন, logger backend সমস্যা, ইত্যাদি) কখনোই context manager-এর
        # বাইরে propagate করবে না, কারণ সেটা করলে আসল yield-এর ফলাফল/exception-কে replace
        # করে ফেলবে (আগের বাগ: except ব্লক একই ব্যর্থ to_log_line() আবার কল করত, যা আবার
        # raise করে সফল LLM completion-কে "ALL_MODELS_FAILED"-এর মতো দেখাত)।
        try:
            log_line = record.to_log_line()
        except Exception as log_exc:
            logger.warning(f"[llm_telemetry] failed to serialize call record: {log_exc}")
        else:
            with contextlib.suppress(Exception):
                logger.bind(llm_telemetry=log_line).info("llm_call")
