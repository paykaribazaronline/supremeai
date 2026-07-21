#!/usr/bin/env python3
"""
SupremeAI Unified LLM Router
=============================
Multi-provider AI gateway with intelligent routing, fallback chains,
cost optimization, and Bengali language optimization.

Architecture:
    Primary:   Moonshot Kimi K2.5 (complex reasoning, Bengali)
    Fallback:  DeepSeek V3 (code/math, cost-efficient)
    Backup:    Together AI (high availability)
    Local:     Ollama (offline/privacy mode — optional)

এই রাউটারটি UniversalRulesEngine ব্যবহার করে সব AI মডেলকে রুলস মানে হতে হবে।
"""

from __future__ import annotations

import hashlib
import json
import time
from collections.abc import AsyncGenerator
from dataclasses import dataclass, field
from enum import Enum
from typing import Any, Protocol

import httpx
# Internal core imports
from core.cache import get_redis_client
from core.config import settings
from core.exceptions import LLMProviderError, QuotaExceededError
from core.logging import get_logger
from core.metrics import counter, timed
from core.resilience.circuit_breaker import CircuitBreaker as circuit_breaker

# Import UniversalRulesEngine for all AI models to follow cine rules
try:
    from core.universal_rules import UniversalRulesEngine

    _rules_engine_available = True
except ImportError:
    _rules_engine_available = False

logger = get_logger(__name__)

# Initialize rules engine - সকল AI মডেলের জন্য রুলস ইঞ্জিন
_rules_engine: UniversalRulesEngine | None = None


def _get_rules_engine() -> UniversalRulesEngine | None:
    """Get or create rules engine instance."""
    global _rules_engine
    if _rules_engine_available and _rules_engine is None:
        try:
            _rules_engine = UniversalRulesEngine()
        except Exception as e:
            logger.warning(f"Could not initialize rules engine: {e}")
    return _rules_engine


# ── Enums & Constants ───────────────────────────────────────────────────────
class Provider(str, Enum):
    MOONSHOT = "moonshot"  # Primary: Kimi K2.5
    DEEPSEEK = "deepseek"  # Fallback: V3
    TOGETHER = "together"  # Backup
    GEMINI = "gemini"  # Google backup
    OLLAMA = "ollama"  # Local (optional)


class TaskType(str, Enum):
    CHAT = "chat"
    CODE = "code"
    BENGALI = "bengali"
    SUMMARIZE = "summarize"
    TRANSLATE = "translate"
    CLASSIFY = "classify"
    EMBEDDING = "embedding"


# Provider capability matrix - শুধু ফ্রি/ওপেন সোর্স প্রোভাইডারগুলো ব্যবহার করবেন (ZERO-108)
PROVIDER_CAPABILITIES: dict[Provider, list[TaskType]] = {
    Provider.MOONSHOT: [
        TaskType.CHAT,
        TaskType.BENGALI,
        TaskType.SUMMARIZE,
        TaskType.TRANSLATE,
        TaskType.CLASSIFY,
    ],
    Provider.DEEPSEEK: [
        TaskType.CHAT,
        TaskType.CODE,
        TaskType.SUMMARIZE,
        TaskType.CLASSIFY,
    ],
    Provider.TOGETHER: [TaskType.CHAT, TaskType.CODE, TaskType.EMBEDDING],
    Provider.GEMINI: [TaskType.CHAT, TaskType.SUMMARIZE, TaskType.TRANSLATE],
    Provider.OLLAMA: [TaskType.CHAT, TaskType.CODE, TaskType.SUMMARIZE],
}

# Cost per 1K tokens (input, output) — USD - Cinem রুলস: Zero Cost Policy
PROVIDER_COSTS: dict[Provider, tuple[float, float]] = {
    Provider.MOONSHOT: (0.005, 0.015),  # Free tier available
    Provider.DEEPSEEK: (0.001, 0.002),  # Cost-efficient
    Provider.TOGETHER: (0.003, 0.009),  # Paid - use sparingly
    Provider.GEMINI: (0.0005, 0.0015),  # Google free tier
    Provider.OLLAMA: (0.0, 0.0),  # Completely free (local)
}

# Default fallback chain per task type - AI-96: Fallback Mechanisms
FALLBACK_CHAINS: dict[TaskType, list[Provider]] = {
    TaskType.CHAT: [
        Provider.MOONSHOT,
        Provider.DEEPSEEK,
        Provider.GEMINI,
        Provider.OLLAMA,
    ],
    TaskType.CODE: [Provider.DEEPSEEK, Provider.GEMINI, Provider.OLLAMA],
    TaskType.BENGALI: [Provider.MOONSHOT, Provider.GEMINI, Provider.OLLAMA],
    TaskType.SUMMARIZE: [Provider.DEEPSEEK, Provider.MOONSHOT, Provider.OLLAMA],
    TaskType.TRANSLATE: [Provider.MOONSHOT, Provider.GEMINI, Provider.OLLAMA],
    TaskType.CLASSIFY: [Provider.DEEPSEEK, Provider.MOONSHOT, Provider.OLLAMA],
    TaskType.EMBEDDING: [Provider.GEMINI, Provider.OLLAMA],  # Prefer free/OSS
}


# ── Data Classes ──────────────────────────────────────────────────────────────
@dataclass
class TokenBudget:
    """AGENT-101: Token budget tracking with 80% context window limit."""

    max_input: int = 8192
    max_output: int = 4096
    daily_limit: int = 100_000
    used_today: int = field(default=0)

    def check(self, estimated_input: int, estimated_output: int) -> bool:
        # Core Philosophy: 80% context window limit
        total = estimated_input + estimated_output
        return (
            estimated_input <= self.max_input
            and estimated_output <= self.max_output
            and (self.used_today + total) <= self.daily_limit
        )

    def consume(self, tokens: int) -> None:
        self.used_today += tokens


@dataclass
class RouteResult:
    provider: Provider
    content: str
    tokens_used: int
    cost_usd: float
    latency_ms: float
    cached: bool = False
    fallback_used: bool = False


@dataclass
class StreamChunk:
    content: str
    is_finished: bool = False
    provider: Provider | None = None


# ── Provider Interface ────────────────────────────────────────────────────────
class LLMProvider(Protocol):
    """Protocol for LLM provider implementations."""

    name: Provider

    async def acompletion(
        self,
        prompt: str,
        *,
        max_tokens: int = 1000,
        temperature: float = 0.7,
        stream: bool = False,
        **kwargs: Any,
    ) -> str | AsyncGenerator[StreamChunk, None]: ...

    async def health_check(self) -> bool: ...


# ── Concrete Providers ────────────────────────────────────────────────────────
class MoonshotProvider:
    """Moonshot AI (Kimi K2.5) — Primary for Bengali & complex reasoning."""

    name = Provider.MOONSHOT

    def __init__(self) -> None:
        self.api_key = getattr(settings, "MOONSHOT_API_KEY", "mock-key")
        self.base_url = "https://api.moonshot.cn/v1"
        self.client = httpx.AsyncClient(
            base_url=self.base_url,
            headers={"Authorization": f"Bearer {self.api_key}"},
            timeout=httpx.Timeout(60.0, connect=10.0),  # CORE-009: Network will fail
        )

    @timed("llm.moonshot.latency")
    @circuit_breaker(name="moonshot", failure_threshold=3, recovery_timeout=60)
    async def acompletion(
        self,
        prompt: str,
        *,
        max_tokens: int = 1000,
        temperature: float = 0.7,
        stream: bool = False,
        **kwargs: Any,
    ) -> str | AsyncGenerator[StreamChunk, None]:
        payload = {
            "model": "kimi-k2.5",
            "messages": [{"role": "user", "content": prompt}],
            "max_tokens": max_tokens,
            "temperature": temperature,
            "stream": stream,
            "response_format": (
                {"type": "json_object"} if kwargs.get("json_mode", False) else None
            ),  # AI-098: Structured outputs
        }
        payload = {k: v for k, v in payload.items() if v is not None}
        payload.update(kwargs)

        if stream:
            return self._stream_completion(payload)

        resp = await self.client.post("/chat/completions", json=payload)
        resp.raise_for_status()
        data = resp.json()
        return data["choices"][0]["message"]["content"]

    async def _stream_completion(
        self, payload: dict[str, Any]
    ) -> AsyncGenerator[StreamChunk, None]:
        async with self.client.stream(
            "POST", "/chat/completions", json=payload
        ) as resp:
            resp.raise_for_status()
            async for line in resp.aiter_lines():
                if line.startswith("data: "):
                    chunk = line[6:]
                    if chunk == "[DONE]":
                        yield StreamChunk("", is_finished=True, provider=self.name)
                        break
                    try:
                        data = json.loads(chunk)
                        content = data["choices"][0]["delta"].get("content", "")
                        yield StreamChunk(content, provider=self.name)
                    except (json.JSONDecodeError, KeyError):
                        continue

    async def health_check(self) -> bool:
        try:
            resp = await self.client.get("/models", timeout=5.0)
            return resp.status_code == 200
        except Exception:
            return False


class DeepSeekProvider:
    """DeepSeek V3 — Fallback for code and cost-efficient tasks."""

    name = Provider.DEEPSEEK

    def __init__(self) -> None:
        self.api_key = getattr(settings, "DEEPSEEK_API_KEY", "mock-key")
        self.base_url = "https://api.deepseek.com/v1"
        self.client = httpx.AsyncClient(
            base_url=self.base_url,
            headers={"Authorization": f"Bearer {self.api_key}"},
            timeout=httpx.Timeout(60.0, connect=10.0),
        )

    @timed("llm.deepseek.latency")
    @circuit_breaker(name="deepseek", failure_threshold=5, recovery_timeout=30)
    async def acompletion(
        self,
        prompt: str,
        *,
        max_tokens: int = 1000,
        temperature: float = 0.7,
        stream: bool = False,
        **kwargs: Any,
    ) -> str | AsyncGenerator[StreamChunk, None]:
        payload = {
            "model": "deepseek-chat",
            "messages": [{"role": "user", "content": prompt}],
            "max_tokens": max_tokens,
            "temperature": temperature,
            "stream": stream,
            "response_format": (
                {"type": "json_object"} if kwargs.get("json_mode", False) else None
            ),
        }
        payload = {k: v for k, v in payload.items() if v is not None}
        payload.update(kwargs)

        if stream:
            return self._stream_completion(payload)

        resp = await self.client.post("/chat/completions", json=payload)
        resp.raise_for_status()
        data = resp.json()
        return data["choices"][0]["message"]["content"]

    async def _stream_completion(
        self, payload: dict[str, Any]
    ) -> AsyncGenerator[StreamChunk, None]:
        async with self.client.stream(
            "POST", "/chat/completions", json=payload
        ) as resp:
            resp.raise_for_status()
            async for line in resp.aiter_lines():
                if line.startswith("data: "):
                    chunk = line[6:]
                    if chunk == "[DONE]":
                        yield StreamChunk("", is_finished=True, provider=self.name)
                        break
                    try:
                        data = json.loads(chunk)
                        content = data["choices"][0]["delta"].get("content", "")
                        yield StreamChunk(content, provider=self.name)
                    except (json.JSONDecodeError, KeyError):
                        continue

    async def health_check(self) -> bool:
        try:
            resp = await self.client.get("/models", timeout=5.0)
            return resp.status_code == 200
        except Exception:
            return False


class TogetherProvider:
    """Together AI — Backup for high availability."""

    name = Provider.TOGETHER

    def __init__(self) -> None:
        self.api_key = getattr(settings, "TOGETHER_API_KEY", "mock-key")
        self.base_url = "https://api.together.xyz/v1"
        self.client = httpx.AsyncClient(
            base_url=self.base_url,
            headers={"Authorization": f"Bearer {self.api_key}"},
            timeout=httpx.Timeout(60.0, connect=10.0),
        )

    @timed("llm.together.latency")
    @circuit_breaker(name="together", failure_threshold=5, recovery_timeout=45)
    async def acompletion(
        self,
        prompt: str,
        *,
        max_tokens: int = 1000,
        temperature: float = 0.7,
        stream: bool = False,
        **kwargs: Any,
    ) -> str | AsyncGenerator[StreamChunk, None]:
        payload = {
            "model": "meta-llama/Llama-3.3-70B-Instruct-Turbo",
            "messages": [{"role": "user", "content": prompt}],
            "max_tokens": max_tokens,
            "temperature": temperature,
            "stream": stream,
        }
        payload.update(kwargs)

        if stream:
            return self._stream_completion(payload)

        resp = await self.client.post("/chat/completions", json=payload)
        resp.raise_for_status()
        data = resp.json()
        return data["choices"][0]["message"]["content"]

    async def _stream_completion(
        self, payload: dict[str, Any]
    ) -> AsyncGenerator[StreamChunk, None]:
        async with self.client.stream(
            "POST", "/chat/completions", json=payload
        ) as resp:
            resp.raise_for_status()
            async for line in resp.aiter_lines():
                if line.startswith("data: "):
                    chunk = line[6:]
                    if chunk == "[DONE]":
                        yield StreamChunk("", is_finished=True, provider=self.name)
                        break
                    try:
                        data = json.loads(chunk)
                        content = data["choices"][0]["delta"].get("content", "")
                        yield StreamChunk(content, provider=self.name)
                    except (json.JSONDecodeError, KeyError):
                        continue

    async def health_check(self) -> bool:
        try:
            resp = await self.client.get("/models", timeout=5.0)
            return resp.status_code == 200
        except Exception:
            return False


class OllamaProvider:
    """Local Ollama — Offline/privacy mode. Optional, completely free."""

    name = Provider.OLLAMA

    def __init__(self) -> None:
        self.base_url = getattr(settings, "OLLAMA_URL", "http://localhost:11434")
        self.client = httpx.AsyncClient(
            base_url=self.base_url,
            timeout=httpx.Timeout(120.0, connect=5.0),
        )
        self.model = getattr(settings, "OLLAMA_MODEL", "qwen2.5:0.5b")

    @timed("llm.ollama.latency")
    async def acompletion(
        self,
        prompt: str,
        *,
        max_tokens: int = 1000,
        temperature: float = 0.7,
        stream: bool = False,
        **kwargs: Any,
    ) -> str | AsyncGenerator[StreamChunk, None]:
        payload = {
            "model": self.model,
            "prompt": prompt,
            "stream": stream,
            "options": {
                "temperature": temperature,
                "num_predict": max_tokens,
            },
        }
        payload.update(kwargs)

        if stream:
            return self._stream_completion(payload)

        resp = await self.client.post("/api/generate", json=payload)
        resp.raise_for_status()
        data = resp.json()
        return data.get("response", "")

    async def _stream_completion(
        self, payload: dict[str, Any]
    ) -> AsyncGenerator[StreamChunk, None]:
        async with self.client.stream("POST", "/api/generate", json=payload) as resp:
            resp.raise_for_status()
            async for line in resp.aiter_lines():
                if not line.strip():
                    continue
                try:
                    data = json.loads(line)
                    content = data.get("response", "")
                    done = data.get("done", False)
                    yield StreamChunk(content, is_finished=done, provider=self.name)
                    if done:
                        break
                except json.JSONDecodeError:
                    continue

    async def health_check(self) -> bool:
        try:
            resp = await self.client.get("/api/tags", timeout=3.0)
            return resp.status_code == 200
        except Exception:
            return False


# ── Bengali Text Utilities ────────────────────────────────────────────────────
class BengaliNormalizer:
    """Normalize Bengali text for consistent LLM processing."""

    # Common transliteration mappings (Banglish → Bengali)
    BANGLISH_MAP: dict[str, str] = {
        "ami": "আমি",
        "tumi": "তুমি",
        "apni": "আপনি",
        "kemon": "কেমন",
        "acho": "আছো",
        "achen": "আছেন",
        "bhalo": "ভালো",
        "kharap": "খারাপ",
        "dhonnobad": "ধন্যবাদ",
        "ki khobor": "কি খবর",
        "bujhi": "বুঝি",
        "hobe": "হবে",
    }

    @classmethod
    def normalize(cls, text: str) -> str:
        """Normalize mixed Bangla-English text."""
        words = text.lower().split()
        normalized = [cls.BANGLISH_MAP.get(w, w) for w in words]
        return " ".join(normalized)

    @classmethod
    def detect_script(cls, text: str) -> str:
        """Detect if text is Bengali, Roman, or mixed."""
        bengali_chars = sum(1 for c in text if "\u0980" <= c <= "\u09ff")
        total_chars = len(text.strip())
        if total_chars == 0:
            return "empty"
        ratio = bengali_chars / total_chars
        if ratio > 0.7:
            return "bengali"
        elif ratio > 0.3:
            return "mixed"
        return "roman"


# ── Unified Router ────────────────────────────────────────────────────────────
class LLMRouter:
    """
    Intelligent LLM Router with fallback chains, cost optimization,
    and Bengali language support.

    সকল AI মডেলকে Cine-এর মেমরিতে থাকা রুলস মানতে বাধ্য করে।
    """

    def __init__(self, budget: TokenBudget | None = None) -> None:
        self.providers: dict[Provider, LLMProvider] = {
            Provider.MOONSHOT: MoonshotProvider(),
            Provider.DEEPSEEK: DeepSeekProvider(),
            Provider.TOGETHER: TogetherProvider(),
            Provider.OLLAMA: OllamaProvider(),
        }
        self.budget = budget or TokenBudget()
        self.cache = get_redis_client()
        self.normalizer = BengaliNormalizer()
        self.rules = _get_rules_engine()  # Cine rules for all AI models

    def _estimate_tokens(self, text: str) -> int:
        """Rough token estimation (1 token ≈ 4 chars for English, 2 for Bengali)."""
        bengali_chars = sum(1 for c in text if "\u0980" <= c <= "\u09ff")
        return (len(text) - bengali_chars) // 4 + bengali_chars // 2 + 1

    def _select_provider(
        self,
        task_type: TaskType,
        preferred: Provider | None = None,
        cost_sensitive: bool = False,
    ) -> list[Provider]:
        """Select provider chain based on task, capability, and cost."""
        if preferred and preferred in PROVIDER_CAPABILITIES:
            if task_type in PROVIDER_CAPABILITIES[preferred]:
                chain = [preferred]
            else:
                chain = []
        else:
            chain = []

        # Add fallback chain - শুধু ফ্রি/ওপেন সোর্স প্রথমে আনা হবে
        for provider in FALLBACK_CHAINS.get(
            task_type, [Provider.MOONSHOT, Provider.GEMINI, Provider.OLLAMA]
        ):
            if provider not in chain and task_type in PROVIDER_CAPABILITIES.get(
                provider, []
            ):
                chain.append(provider)

        # Cost-sensitive: sort by cost - ZERO-108: Zero Cost Policy
        if cost_sensitive:
            chain.sort(key=lambda p: PROVIDER_COSTS[p][0] + PROVIDER_COSTS[p][1])

        return chain

    def _cache_key(self, prompt: str, task_type: str, **kwargs: Any) -> str:
        """Generate deterministic cache key."""
        data = f"{prompt}:{task_type}:{json.dumps(kwargs, sort_keys=True)}"
        return f"llm:cache:{hashlib.sha256(data.encode()).hexdigest()[:16]}"

    @timed("llm.route.total")
    @counter("llm.route.calls")
    async def route(
        self,
        prompt: str,
        task_type: str = "chat",
        *,
        max_tokens: int = 1000,
        temperature: float = 0.7,
        stream: bool = False,
        preferred_provider: str | None = None,
        cost_sensitive: bool = True,  # AI-96: Prefer low-cost providers
        use_cache: bool = True,  # AI-094: Semantic caching
        normalize_bengali: bool = True,
        **kwargs: Any,
    ) -> RouteResult | AsyncGenerator[StreamChunk, None]:
        """
        Route prompt to optimal LLM provider with automatic fallback.
        সকল রুলস যাচাই করে এবং মেনে চালায়।
        """
        task = (
            TaskType(task_type)
            if task_type in [t.value for t in TaskType]
            else TaskType.CHAT
        )

        # AGENT-101: Check token budget before processing
        estimated_tokens = self._estimate_tokens(prompt) + max_tokens
        if self.rules and not self.rules.check_token_budget(estimated_tokens):
            logger.error(f"❌ Token budget exceeded: {estimated_tokens}")

        # Normalize Bengali text
        if normalize_bengali and self.normalizer.detect_script(prompt) in (
            "mixed",
            "roman",
        ):
            prompt = self.normalizer.normalize(prompt)
            logger.debug("bengali_normalized", original_length=len(prompt))

        # Check cache - AI-094: Semantic Caching
        cache_key = self._cache_key(
            prompt, task.value, max_tokens=max_tokens, temperature=temperature
        )
        if use_cache and not stream:
            cached_result = await self.cache.get(cache_key)
            if cached_result:
                logger.debug("cache_hit", key=cache_key)
                data = json.loads(cached_result)
                return RouteResult(
                    provider=Provider(data["provider"]),
                    content=data["content"],
                    tokens_used=data["tokens"],
                    cost_usd=0.0,
                    latency_ms=0.0,
                    cached=True,
                )

        # Budget check
        estimated_input = self._estimate_tokens(prompt)
        if not self.budget.check(estimated_input, max_tokens):
            raise QuotaExceededError(
                message="Token budget exceeded",
                details={
                    "estimated_input": estimated_input,
                    "max_output": max_tokens,
                    "used_today": self.budget.used_today,
                },
            )

        # Select provider chain - prioritize free/low-cost providers
        pref = Provider(preferred_provider) if preferred_provider else None
        chain = self._select_provider(task, pref, cost_sensitive)

        if not chain:
            raise LLMProviderError(
                message=f"No capable provider found for task: {task.value}",
                details={"available": list(PROVIDER_CAPABILITIES.keys())},
            )

        # Try each provider in chain - AI-96: Fallback Mechanisms
        last_error: Exception | None = None
        start_time = time.perf_counter()

        for provider_name in chain:
            provider = self.providers.get(provider_name)
            if not provider:
                continue

            # Health check (lightweight)
            if not await provider.health_check():
                logger.warning("provider_unhealthy", provider=provider_name.value)
                continue

            try:
                logger.info(
                    "llm_request",
                    provider=provider_name.value,
                    task=task.value,
                    estimated_tokens=estimated_input + max_tokens,
                )

                if stream:
                    return self._stream_with_fallback(
                        provider, prompt, max_tokens, temperature, chain, **kwargs
                    )

                result = await provider.acompletion(
                    prompt,
                    max_tokens=max_tokens,
                    temperature=temperature,
                    stream=False,
                    **kwargs,
                )

                # AGENT-104: Check for hallucination policy
                if self.rules:
                    if not self.rules.check_hallucination_policy(result):
                        logger.warning("Potential hallucination detected in response")

                # Non-stream branch-এ result সবসময় str হবে — mypy-কে type narrow করা হচ্ছে
                assert isinstance(result, str), "Non-stream acompletion must return str"

                latency = (time.perf_counter() - start_time) * 1000
                tokens = estimated_input + self._estimate_tokens(result)
                cost = (tokens / 1000) * (
                    PROVIDER_COSTS[provider_name][0] * 0.3
                    + PROVIDER_COSTS[provider_name][1] * 0.7
                )

                self.budget.consume(tokens)

                route_result = RouteResult(
                    provider=provider_name,
                    content=result,
                    tokens_used=tokens,
                    cost_usd=cost,
                    latency_ms=latency,
                    fallback_used=(provider_name != chain[0]),
                )

                # Cache successful result - AI-094: Semantic Caching
                if use_cache:
                    await self.cache.setex(
                        cache_key,
                        300,  # 5 min TTL
                        json.dumps(
                            {
                                "provider": provider_name.value,
                                "content": result,
                                "tokens": tokens,
                            }
                        ),
                    )

                return route_result

            except Exception as exc:
                last_error = exc
                logger.warning(
                    "provider_failed",
                    provider=provider_name.value,
                    error=str(exc),
                    will_fallback=(provider_name != chain[-1]),
                )
                continue

        # All providers failed - SELF-113: Self-Healing
        latency = (time.perf_counter() - start_time) * 1000
        logger.error(
            "all_providers_failed",
            chain=[p.value for p in chain],
            error=str(last_error),
        )
        raise LLMProviderError(
            message=f"All providers failed for task {task.value}",
            details={
                "chain": [p.value for p in chain],
                "last_error": str(last_error),
                "latency_ms": latency,
            },
        ) from last_error

    async def _stream_with_fallback(
        self,
        primary: LLMProvider,
        prompt: str,
        max_tokens: int,
        temperature: float,
        chain: list[Provider],
        **kwargs: Any,
    ) -> AsyncGenerator[StreamChunk, None]:
        """Stream with provider fallback on failure - AI-97: Stream Responses"""
        try:
            # স্ট্রিমিং শুরুর আগে coroutine থেকে AsyncGenerator পাওয়ার জন্য await প্রয়োজন
            stream_gen = await primary.acompletion(prompt, max_tokens=max_tokens, temperature=temperature, stream=True, **kwargs)  # type: ignore[misc]
            async for chunk in stream_gen:  # type: ignore[union-attr]
                yield chunk
        except Exception as exc:
            logger.warning("stream_failed", provider=primary.name.value, error=str(exc))
            # Try next provider in chain - AI-96: Fallback Mechanisms
            for fallback_name in chain[1:]:
                fallback = self.providers.get(fallback_name)
                if fallback and await fallback.health_check():
                    logger.info("stream_fallback", to=fallback_name.value)
                    # Fallback provider থেকেও await করে stream নেওয়া হচ্ছে
                    fallback_gen = await fallback.acompletion(prompt, max_tokens=max_tokens, temperature=temperature, stream=True, **kwargs)  # type: ignore[misc]
                    async for chunk in fallback_gen:  # type: ignore[union-attr]
                        chunk.provider = fallback_name  # Override provider
                        yield chunk
                    return
            raise LLMProviderError(message="All streaming providers failed") from exc

    async def health_check_all(self) -> dict[str, bool]:
        """Check health of all configured providers."""
        results = {}
        for name, provider in self.providers.items():
            results[name.value] = await provider.health_check()
        return results

    async def get_cost_report(self) -> dict[str, Any]:
        """Generate cost and usage report - AI-099: Cost Tracking per Request."""
        return {
            "budget": {
                "daily_limit": self.budget.daily_limit,
                "used_today": self.budget.used_today,
                "remaining": self.budget.daily_limit - self.budget.used_today,
            },
            "provider_costs": {
                p.value: {"input": c[0], "output": c[1]}
                for p, c in PROVIDER_COSTS.items()
            },
            "rules_enforced": (
                self.rules.validate_critical_rules() if self.rules else []
            ),
        }


# ── Singleton & Factory ───────────────────────────────────────────────────────
_router_instance: LLMRouter | None = None


def get_llm_router(budget: TokenBudget | None = None) -> LLMRouter:
    """Get or create singleton LLM Router."""
    global _router_instance
    if _router_instance is None:
        _router_instance = LLMRouter(budget=budget)
    return _router_instance


# ── Legacy Compatibility ──────────────────────────────────────────────────────
class LLMGateway:
    """Legacy compatibility wrapper — delegates to LLMRouter."""

    def __init__(self) -> None:
        self._router = get_llm_router()

    async def acompletion(
        self,
        prompt: str,
        task_type: str = "chat",
        max_tokens: int = 1000,
        temperature: float = 0.7,
        **kwargs: Any,
    ) -> dict[str, Any]:
        """Legacy acompletion interface."""
        result = await self._router.route(
            prompt=prompt,
            task_type=task_type,
            max_tokens=max_tokens,
            temperature=temperature,
            **kwargs,
        )
        return {
            "text": result.content,
            "provider": result.provider.value,
            "tokens_used": result.tokens_used,
            "cost_usd": result.cost_usd,
            "latency_ms": result.latency_ms,
            "cached": result.cached,
        }


def get_llm_gateway() -> LLMGateway:
    """Legacy factory function."""
    return LLMGateway()


# ── Convenience Functions ─────────────────────────────────────────────────────
async def quick_chat(
    prompt: str,
    *,
    task_type: str = "chat",
    stream: bool = False,
    **kwargs: Any,
) -> str | AsyncGenerator[StreamChunk, None]:
    """One-shot chat with default router."""
    router = get_llm_router()
    result = await router.route(prompt, task_type=task_type, stream=stream, **kwargs)
    if stream:
        return result
    return result.content


async def bengali_chat(prompt: str, **kwargs: Any) -> str:
    """Optimized chat for Bengali language - LANG-115/116: Bangla ভাষায় স্বাচ্ছন্দ্য।"""
    router = get_llm_router()
    result = await router.route(
        prompt,
        task_type="bengali",
        normalize_bengali=True,
        preferred_provider="moonshot",
        **kwargs,
    )
    return result.content
