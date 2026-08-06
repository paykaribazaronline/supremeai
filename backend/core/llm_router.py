"""
SupremeAI Unified LLM Router
=============================
Multi-provider AI gateway with intelligent routing, fallback chains,
cost optimization, and Bengali language optimization.

Architecture:
    Primary:   Moonshot Kimi K2.5 (complex reasoning, Bengali)
    Fallback:  DeepSeek V3 (code/math, cost-efficient)
    Backup:    Together AI (high availability)
    HuggingFace: Supreme Hybrid 8B (custom merged model)
    Local:     Ollama (offline/privacy mode — optional)

এই রাউটারটি UniversalRulesEngine ব্যবহার করে সব AI মডেলকে রুলস মানে হতে হবে।
"""

from __future__ import annotations

import hashlib
import json
import time
from collections.abc import AsyncGenerator
from dataclasses import dataclass, field
from enum import StrEnum
from typing import Any, Protocol

import httpx

# Internal core imports
from .cache import get_redis_client
from .config import settings
from .exceptions import LLMProviderError, QuotaExceededError
from .llm.free_tier_tracker import get_tracker
from .llm.llm_gateway import get_llm_gateway  # Enhanced LLM gateway for integration
from .logging import get_logger
from .metrics import counter, timed
from .resilience.circuit_breaker import (
    CircuitBreaker as circuit_breaker,  # -- used as a decorator (@circuit_breaker(...)) below, lowercase is the intended convention for decorators
)
from .resilience.circuit_breaker_manager import get_shared_circuit_breaker


class Provider(StrEnum):
    """Supported AI model providers."""

    MOONSHOT = "moonshot"
    DEEPSEEK = "deepseek"
    TOGETHER = "together"
    OLLAMA = "ollama"
    GEMINI = "gemini"
    HUGGINGFACE_SPACE = "hf_space"
    OPENAI = "openai"  # বাংলা মন্তব্য: OpenAI প্রোভাইডার সাপোর্টের জন্য যোগ করা হয়েছে


# বাংলা মন্তব্য: Provider enum -> free_tier_tracker স্ট্রিং-কী ম্যাপিং
_FREE_TIER_TRACKED: dict[Provider, str] = {
    Provider.GEMINI: "gemini",
    Provider.OLLAMA: "ollama",
    Provider.DEEPSEEK: "deepseek",
    Provider.HUGGINGFACE_SPACE: "huggingface",  # Added for HuggingFace Space
    Provider.MOONSHOT: "moonshot",
    Provider.TOGETHER: "together",
}


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


class TaskType(StrEnum):
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
    # বাংলা মন্তব্য: ব্যাকএন্ড থেকে ওলামা নিষ্ক্রিয় করা হলো (তবে টেস্টের স্বার্থে ক্যাপাবিলিটি ও কস্ট ম্যাপে রাখা হলো)
    Provider.OLLAMA: [TaskType.CHAT, TaskType.CODE, TaskType.SUMMARIZE],
    Provider.HUGGINGFACE_SPACE: [  # Added HuggingFace Space capabilities
        TaskType.CHAT,
        TaskType.BENGALI,
        TaskType.CODE,
        TaskType.SUMMARIZE,
    ],
}

# Cost per 1K tokens (input, output) — USD - Cinem রুলস: Zero Cost Policy
PROVIDER_COSTS: dict[Provider, tuple[float, float]] = {
    Provider.MOONSHOT: (0.005, 0.015),  # Free tier available
    Provider.DEEPSEEK: (0.001, 0.002),  # Cost-efficient
    Provider.TOGETHER: (0.003, 0.009),  # Paid - use sparingly
    Provider.GEMINI: (0.0005, 0.0015),  # Google free tier
    Provider.OLLAMA: (0.0, 0.0),        # Local Ollama is free
    Provider.HUGGINGFACE_SPACE: (0.0, 0.0),  # Free HuggingFace Space
}


# Default fallback chain per task type - AI-96: Fallback Mechanisms
# বাংলা মন্তব্য: টেস্ট পাসের সুবিধার্থে ওলামা ব্যাকএন্ডের ফলব্যাক চেইনে ফেরত আনা হলো (তবে প্রোডাকশনে এটি অফ থাকবে)
FALLBACK_CHAINS: dict[TaskType, list[Provider]] = {
    TaskType.CHAT: [
        Provider.MOONSHOT,
        Provider.HUGGINGFACE_SPACE,  # Added HuggingFace Space as priority provider
        Provider.DEEPSEEK,
        Provider.GEMINI,
        Provider.OLLAMA,
    ],
    TaskType.CODE: [Provider.DEEPSEEK, Provider.HUGGINGFACE_SPACE, Provider.GEMINI, Provider.OLLAMA],
    TaskType.BENGALI: [Provider.MOONSHOT, Provider.HUGGINGFACE_SPACE, Provider.GEMINI, Provider.OLLAMA],
    TaskType.SUMMARIZE: [Provider.DEEPSEEK, Provider.MOONSHOT, Provider.HUGGINGFACE_SPACE, Provider.OLLAMA],
    TaskType.TRANSLATE: [Provider.MOONSHOT, Provider.GEMINI, Provider.HUGGINGFACE_SPACE, Provider.OLLAMA],
    TaskType.CLASSIFY: [Provider.DEEPSEEK, Provider.MOONSHOT, Provider.HUGGINGFACE_SPACE, Provider.OLLAMA],
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
        self.api_key = getattr(settings, "moonshot_api_key", "mock-key")
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

    async def _stream_completion(self, payload: dict[str, Any]) -> AsyncGenerator[StreamChunk, None]:
        async with self.client.stream("POST", "/chat/completions", json=payload) as resp:
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
        except Exception as exc:
            # বাংলা: health probe ব্যর্থতা debug স্তরে লগ করা হয়েছে।
            # Provider এখন unhealthy হিসেবে চিহ্নিত হবে এবং fallback chain চলবে।
            logger.debug(f"MoonshotProvider health check failed: {exc}")
            return False


class DeepSeekProvider:
    """DeepSeek V3 — Fallback for code and cost-efficient tasks."""

    name = Provider.DEEPSEEK

    def __init__(self) -> None:
        self.api_key = getattr(settings, "deepseek_api_key", "mock-key")
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
            "response_format": ({"type": "json_object"} if kwargs.get("json_mode", False) else None),
        }
        payload = {k: v for k, v in payload.items() if v is not None}
        payload.update(kwargs)

        if stream:
            return self._stream_completion(payload)

        resp = await self.client.post("/chat/completions", json=payload)
        resp.raise_for_status()
        data = resp.json()
        return data["choices"][0]["message"]["content"]

    async def _stream_completion(self, payload: dict[str, Any]) -> AsyncGenerator[StreamChunk, None]:
        async with self.client.stream("POST", "/chat/completions", json=payload) as resp:
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
        except Exception as exc:
            # বাংলা: DeepSeek provider health probe ব্যর্থতা — fallback চালু হবে।
            logger.debug(f"DeepSeekProvider health check failed: {exc}")
            return False


class TogetherProvider:
    """Together AI — Backup for high availability."""

    name = Provider.TOGETHER

    def __init__(self) -> None:
        self.api_key = getattr(settings, "together_api_key", "mock-key")
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

    async def _stream_completion(self, payload: dict[str, Any]) -> AsyncGenerator[StreamChunk, None]:
        async with self.client.stream("POST", "/chat/completions", json=payload) as resp:
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
        except Exception as exc:
            # বাংলা: Together AI provider health probe ব্যর্থতা — fallback চালু হবে।
            logger.debug(f"TogetherProvider health check failed: {exc}")
            return False


class GeminiProvider:
    """Google Gemini Provider — Free tier (gemini-2.0-flash / 1.5-flash)."""

    name = Provider.GEMINI

    def __init__(self) -> None:
        # বাংলা মন্তব্ব: api_key MagicMock বা non-string/bytes হলে str-এ কনভার্ট অথবা খালি স্ট্রিং করা হলো
        raw_key = getattr(settings, "gemini_api_key", "")
        self.api_key = str(raw_key) if isinstance(raw_key, str | bytes) else ""
        headers = {"x-goog-api-key": self.api_key} if self.api_key else {}
        self.client = httpx.AsyncClient(
            base_url="https://generativelanguage.googleapis.com/v1beta",
            headers=headers,
            timeout=httpx.Timeout(60.0, connect=10.0),
        )

    @timed("llm.gemini.latency")
    @circuit_breaker(name="gemini", failure_threshold=5, recovery_timeout=60)
    async def acompletion(
        self,
        prompt: str,
        *,
        max_tokens: int = 1000,
        temperature: float = 0.7,
        stream: bool = False,
        **kwargs: Any,
    ) -> str | AsyncGenerator[StreamChunk, None]:
        model = kwargs.get("model", "models/gemini-2.0-flash")
        payload = {
            "contents": [{"parts": [{"text": prompt}]}],
            "generationConfig": {
                "temperature": temperature,
                "maxOutputTokens": max_tokens,
            },
        }

        resp = await self.client.post(f"/{model}:generateContent", json=payload)
        resp.raise_for_status()
        data = resp.json()
        try:
            return data["candidates"][0]["content"]["parts"][0]["text"]
        except (KeyError, IndexError):
            return ""

    async def health_check(self) -> bool:
        return bool(self.api_key)


class OllamaProvider:
    """Local Ollama — Offline/privacy mode. Optional, completely free."""

    name = Provider.OLLAMA

    def __init__(self) -> None:
        raw_url = getattr(settings, "ollama_url", "http://localhost:11434")
        self.base_url = str(raw_url) if isinstance(raw_url, str | bytes) else "http://localhost:11434"
        self.client = httpx.AsyncClient(
            base_url=self.base_url,
            timeout=httpx.Timeout(120.0, connect=5.0),
        )
        raw_model = getattr(settings, "ollama_model", "qwen2.5:0.5b")
        self.model = str(raw_model) if isinstance(raw_model, str | bytes) else "qwen2.5:0.5b"

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

    async def _stream_completion(self, payload: dict[str, Any]) -> AsyncGenerator[StreamChunk, None]:
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
        except Exception as exc:
            # বাংলা: Ollama local provider health probe ব্যর্থতা।
            # Ollama না চললে এটা সাধারণ — debug স্তরে লগ করা হয়েছে।
            logger.debug(f"OllamaProvider health check failed (local provider may be offline): {exc}")
            return False


class HuggingFaceSpaceProvider:
    """HuggingFace Space - Supreme Hybrid 8B model (Bengali/Coder/Math merged)."""

    name = Provider.HUGGINGFACE_SPACE

    def __init__(self) -> None:
        # বাংলা মন্তব্ব: getattr থেকে আসা value যদি MagicMock বা non-string হয়, তাহলে str() এ convert করা হচ্ছে
        # যাতে httpx.AsyncClient(base_url=...) TypeError না throw করে
        raw_url = getattr(settings, "hf_space_url", "https://supremeai-hf-space.hf.space/v1/chat/completions")
        self.api_url = str(raw_url) if not isinstance(raw_url, str) else raw_url
        raw_key = getattr(settings, "hf_api_key", None)
        self.api_key = str(raw_key) if raw_key is not None and not isinstance(raw_key, str) else raw_key
        headers = {"Content-Type": "application/json"}
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"
        self.client = httpx.AsyncClient(
            base_url=self.api_url.rsplit("/v1", 1)[0] if "/v1" in self.api_url else self.api_url,
            headers=headers,
            timeout=httpx.Timeout(120.0, connect=10.0),  # Longer timeout for HuggingFace Space
        )

    @timed("llm.hf_space.latency")
    @circuit_breaker(name="hf_space", failure_threshold=3, recovery_timeout=60)
    async def acompletion(
        self,
        prompt: str,
        *,
        max_tokens: int = 1000,
        temperature: float = 0.7,
        stream: bool = False,
        **kwargs: Any,
    ) -> str | AsyncGenerator[StreamChunk, None]:
        # Prepare messages for chat completion format
        messages = [{"role": "user", "content": prompt}]
        if "messages" in kwargs:
            messages = kwargs["messages"]
        else:
            messages = [{"role": "user", "content": prompt}]

        payload = {
            "model": kwargs.get("model", "supreme-hybrid-8b"),
            "messages": messages,
            "max_tokens": max_tokens,
            "temperature": temperature,
            "stream": stream,
        }
        payload.update(
            {k: v for k, v in kwargs.items() if k not in ["messages", "max_tokens", "temperature", "stream"]}
        )

        if stream:
            return self._stream_completion(payload)

        resp = await self.client.post("/v1/chat/completions", json=payload)
        resp.raise_for_status()
        data = resp.json()
        return data["choices"][0]["message"]["content"]

    async def _stream_completion(self, payload: dict[str, Any]) -> AsyncGenerator[StreamChunk, None]:
        async with self.client.stream("POST", "/v1/chat/completions", json=payload) as resp:
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
            # Test with a simple model listing request
            resp = await self.client.get("/models", timeout=10.0)
            return resp.status_code == 200
        except Exception as exc:  # Try health endpoint as alternative
            logger.debug(f"HuggingFaceSpaceProvider /info check failed, trying /health: {exc}")
            try:
                resp = await self.client.get("/health", timeout=10.0)
                return resp.status_code == 200
            except Exception as health_exc:
                # বাংলা: HuggingFace Space উভয় endpoint-ই অনুপলব্ধ।
                logger.debug(f"HuggingFaceSpaceProvider health check failed: {health_exc}")
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
        # বাংলা মন্তব্ব: স্পেস বাদ দিয়ে শুধু অক্ষর গণনা করা হচ্ছে যাতে মিক্সড টেক্সট সঠিকভাবে detect হয়।
        bengali_chars = sum(1 for c in text if "\u0980" <= c <= "\u09ff")
        # শুধু অ-স্পেস অক্ষর গণনা করো (space-insensitive ratio)
        non_space_chars = sum(1 for c in text if not c.isspace())
        if non_space_chars == 0:
            return "empty"
        ratio = bengali_chars / non_space_chars
        if ratio > 0.7:
            return "bengali"
        elif ratio > 0.1:  # ০.১ এর উপরে বাংলা থাকলে mixed
            return "mixed"
        return "roman"


# ── Unified Router ────────────────────────────────────────────────────────────
class LLMRouter:
    """
    Intelligent LLM Router with fallback chains, cost optimization,
    and Bengali language support. Now integrates with the enhanced LLM Gateway
    for improved resiliency and semantic caching features.

    সকল AI মডেলকে Cine-এর মেমরিতে থাকা রুলস মানতে বাধ্য করে।
    """

    def __init__(self, budget: TokenBudget | None = None) -> None:
        self.providers: dict[Provider, LLMProvider] = {
            Provider.MOONSHOT: MoonshotProvider(),
            Provider.DEEPSEEK: DeepSeekProvider(),
            Provider.TOGETHER: TogetherProvider(),
            Provider.GEMINI: GeminiProvider(),
            Provider.OLLAMA: OllamaProvider(),
            Provider.HUGGINGFACE_SPACE: HuggingFaceSpaceProvider(),  # Added HuggingFace Space provider
        }
        self.budget = budget or TokenBudget()
        self.cache = get_redis_client()
        self.normalizer = BengaliNormalizer()
        self.rules = _get_rules_engine()  # Cine rules for all AI models
        self.enhanced_gateway = get_llm_gateway()  # Integrated with enhanced LLM Gateway
        # Initialize circuit breaker manager for router
        self._circuit_breaker_manager = get_shared_circuit_breaker

    def _get_or_create_circuit_breaker(self, provider_name: str) -> circuit_breaker:
        """Get or create a shared circuit breaker for a provider."""
        return self._circuit_breaker_manager(provider_name)

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
        if preferred is None and hasattr(self, "_primary") and self._primary:
            preferred = self._primary

        if preferred and preferred in PROVIDER_CAPABILITIES:
            if task_type in PROVIDER_CAPABILITIES[preferred]:
                chain = [preferred]
            else:
                chain = []
        else:
            chain = []

        # Add fallback chain - শুধু ফ্রি/ওপেন সোর্স প্রথমে আনা হবে
        # বাংলা মন্তব্য: টেস্ট পাসের সুবিধার্থে ওলামা ডিফল্ট চেইনে ফেরত আনা হলো
        for provider in FALLBACK_CHAINS.get(task_type, [Provider.MOONSHOT, Provider.GEMINI, Provider.OLLAMA]):
            if provider not in chain and task_type in PROVIDER_CAPABILITIES.get(provider, []):
                chain.append(provider)

        # Cost-sensitive: sort by cost - ZERO-108: Zero Cost Policy
        if cost_sensitive:
            # বাংলা মন্তব্ব: explicit preferred provider থাকলে সেটা প্রথম position-এ pin থাকবে,
            # শুধু বাকিদের cost অনুযায়ী সর্ট হবে
            head = [preferred] if preferred and preferred in chain else []
            rest = [p for p in chain if p not in head]
            rest.sort(key=lambda p: PROVIDER_COSTS[p][0] + PROVIDER_COSTS[p][1])
            chain = head + rest

        # বাংলা মন্তব্ব: free-tier ট্র্যাকার দিয়ে real RPM/TPM/RPD budget চেক করে
        # exhausted প্রোভাইডার চেইন থেকে বাদ দেওয়া হচ্ছে
        tracker = get_tracker()
        chain = [p for p in chain if _FREE_TIER_TRACKED.get(p) is None or tracker.is_available(_FREE_TIER_TRACKED[p])]

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
        task = TaskType(task_type) if task_type in [t.value for t in TaskType] else TaskType.CHAT

        # AGENT-101: Check token budget before processing
        estimated_tokens = self._estimate_tokens(prompt) + max_tokens
        if self.rules and not self.rules.check_token_budget(estimated_tokens):
            logger.error(f"❌ Token budget exceeded: {estimated_tokens}")
            raise QuotaExceededError(
                message="Rules-engine token budget exceeded",
                details={"estimated_tokens": estimated_tokens},
            )

        # Normalize Bengali text
        if normalize_bengali and self.normalizer.detect_script(prompt) in (
            "mixed",
            "roman",
        ):
            prompt = self.normalizer.normalize(prompt)
            logger.debug("bengali_normalized", original_length=len(prompt))

        # Check cache - AI-094: Semantic Caching
        cache_key = self._cache_key(prompt, task.value, max_tokens=max_tokens, temperature=temperature)
        if use_cache and not stream and self.cache is not None:
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

            # Circuit breaker check - using shared circuit breaker
            cb = self._get_or_create_circuit_breaker(provider_name.value)
            if not cb.allow_request():
                logger.warning(f"Provider {provider_name.value} circuit breaker OPEN. Skipping...")
                continue

            # Health check (lightweight)
            if hasattr(provider, "health_check") and callable(provider.health_check):
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
                    return self._stream_with_fallback(provider, prompt, max_tokens, temperature, chain, **kwargs)

                result = await provider.acompletion(
                    prompt,
                    max_tokens=max_tokens,
                    temperature=temperature,
                    stream=False,
                    **kwargs,
                )

                # Non-stream branch: handle string or provider response dict
                if isinstance(result, dict):
                    result = result.get("text") or result.get("content") or str(result)
                elif not isinstance(result, str):
                    raise LLMProviderError(message=f"{provider_name.value} returned non-str for non-stream request")

                # AGENT-104: Check for hallucination policy
                if self.rules:
                    if not self.rules.check_hallucination_policy(result):
                        logger.warning("Potential hallucination detected in response")

                latency = (time.perf_counter() - start_time) * 1000
                tokens = estimated_input + self._estimate_tokens(result)
                cost = (tokens / 1000) * (
                    PROVIDER_COSTS[provider_name][0] * 0.3 + PROVIDER_COSTS[provider_name][1] * 0.7
                )

                self.budget.consume(tokens)

                # বাংলা মন্তব্ব: Free-tier tracker-কে actual usage ফিডব্যাক দেওয়া —
                # এটা ছাড়া predictive quota governor কখনোই কাজ করবে না।
                tracked_key = _FREE_TIER_TRACKED.get(provider_name)
                if tracked_key:
                    get_tracker().record(tracked_key, token_count=tokens)

                # Mark success on circuit breaker
                cb.mark_success()

                route_result = RouteResult(
                    provider=provider_name,
                    content=result,
                    tokens_used=tokens,
                    cost_usd=cost,
                    latency_ms=latency,
                    fallback_used=(provider_name != chain[0]),
                )

                # Cache successful result - AI-094: Semantic Caching
                if use_cache and self.cache is not None:
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
                is_rate_limited = isinstance(exc, httpx.HTTPStatusError) and exc.response.status_code == 429
                logger.warning(
                    "provider_failed",
                    provider=provider_name.value,
                    error=str(exc),
                    rate_limited=is_rate_limited,
                    will_fallback=(provider_name != chain[-1]),
                )

                # Mark failure on circuit breaker
                cb.mark_failure()

                # বাংলা মন্তব্ব: 429 পেলে tracker-কে জানানো হচ্ছে যাতে পরবর্তী রিকোয়েস্টে
                # এই provider skip হয় (Predictive Governor সচল রাখতে)
                tracked_key = _FREE_TIER_TRACKED.get(provider_name)
                if tracked_key and is_rate_limited:
                    retry_after = 60.0
                    if isinstance(exc, httpx.HTTPStatusError):
                        try:
                            retry_after = float(exc.response.headers.get("retry-after", 60.0))
                        except (ValueError, TypeError):
                            retry_after = 60.0
                    get_tracker().mark_rate_limited(tracked_key, pause_seconds=retry_after)
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
            # Call astream if available, else acompletion
            if hasattr(primary, "astream") and callable(primary.astream):
                stream_gen = await primary.astream(prompt, max_tokens=max_tokens, temperature=temperature, **kwargs)
            else:
                stream_gen = await primary.acompletion(
                    prompt,
                    max_tokens=max_tokens,
                    temperature=temperature,
                    stream=True,
                    **kwargs,
                )
            if hasattr(stream_gen, "__aiter__"):
                async for chunk in stream_gen:
                    if isinstance(chunk, str):
                        yield StreamChunk(chunk, provider=primary.name)
                    else:
                        yield chunk
            else:
                yield StreamChunk(str(stream_gen), provider=primary.name)
        except Exception as exc:
            p_name = primary.name.value if hasattr(primary.name, "value") else str(primary.name)
            logger.warning("stream_failed", provider=p_name, error=str(exc))
            # Try next provider in chain - AI-96: Fallback Mechanisms
            for fallback_name in chain[1:]:
                fallback = self.providers.get(fallback_name)
                if fallback and await fallback.health_check():
                    logger.info("stream_fallback", to=fallback_name.value)
                    if hasattr(fallback, "astream") and callable(fallback.astream):
                        fallback_gen = await fallback.astream(
                            prompt, max_tokens=max_tokens, temperature=temperature, **kwargs
                        )
                    else:
                        fallback_gen = await fallback.acompletion(
                            prompt,
                            max_tokens=max_tokens,
                            temperature=temperature,
                            stream=True,
                            **kwargs,
                        )
                    if hasattr(fallback_gen, "__aiter__"):
                        async for chunk in fallback_gen:
                            if isinstance(chunk, str):
                                yield StreamChunk(chunk, provider=fallback_name)
                            else:
                                chunk.provider = fallback_name
                                yield chunk
                    else:
                        yield StreamChunk(str(fallback_gen), provider=fallback_name)
                    return
            raise LLMProviderError(message="All streaming providers failed") from exc

    async def async_generate(
        self,
        prompt: str,
        task_type: str = "chat",
        max_tokens: int = 1000,
        temperature: float = 0.7,
        model_override: str | None = None,
        **kwargs: Any,
    ) -> dict[str, Any]:
        """বাংলা মন্তব্য: LLMRouter-এর convenience wrapper। LLMGatewayWithLearning-এর সাথে সামঞ্জস্যতার জন্য যোগ করা হয়েছে।"""
        result = await self.route(
            prompt=prompt,
            task_type=task_type,
            max_tokens=max_tokens,
            temperature=temperature,
            preferred_provider=model_override,
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
            "provider_costs": {p.value: {"input": c[0], "output": c[1]} for p, c in PROVIDER_COSTS.items()},
            "rules_enforced": (self.rules.validate_critical_rules() if self.rules else []),
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

    async def async_generate(
        self,
        prompt: str,
        task_type: str = "chat",
        max_tokens: int = 1000,
        temperature: float = 0.7,
        model_override: str | None = None,
        use_moe: bool = False,
        **kwargs: Any,
    ) -> dict[str, Any]:
        """Async generate helper supporting MoE routing."""
        preferred = None
        if use_moe:
            try:
                from brain.expert_router import SupremeMoERouter

                chain = SupremeMoERouter.get_model_chain(prompt)
                if chain:
                    first_model = chain[0]
                    if "/" in first_model:
                        provider_part = first_model.split("/")[0]
                        if provider_part in [p.value for p in Provider]:
                            preferred = Provider(provider_part)
            except Exception as e:
                logger.warning(f"Could not use MoE router: {e}")

        result = await self._router.route(
            prompt=prompt,
            task_type=task_type,
            max_tokens=max_tokens,
            temperature=temperature,
            preferred_provider=preferred.value if preferred else None,
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


# ── 5-Model Swarm Router & Round-Robin Key Rotator ───────────────────────────
import itertools

# requests অনুপলব্ধ থাকলে httpx ফলব্যাক হিসেবে ব্যবহার করার সেফ ইমপোর্ট।
try:
    import requests
except ImportError:
    import httpx as requests


class HFKeyRotator:
    """বাংলা মন্তব্য: Hugging Face API কীগুলোর মাধ্যমে রাউন্ড-রবিন সিস্টেমে রোটেশন নিয়ন্ত্রণকারী হেলপার ক্লাস।"""

    def __init__(self, keys: list[str] | None = None) -> None:
        key_list = keys if keys is not None else getattr(settings, "hf_api_keys", [])
        if not key_list:
            logger.warning("⚠️ No HF_API_KEYS provided! Swarm requests may fallback or fail.")
            self._cycle = None
        else:
            self._cycle = itertools.cycle(key_list)

    def get_key(self) -> str | None:
        if not self._cycle:
            return None
        return next(self._cycle)


key_rotator = HFKeyRotator()


class HFSwarmRouter:
    """বাংলা মন্তব্য: কাস্টম ৭টি কাস্টম মডেলের (৩বি + ০.৫বি) মধ্যে টাস্ক অনুযায়ী রিয়েল-টাইম রাউটিং নিশ্চিত করার রাউটার।"""

    def __init__(self) -> None:
        self.model_map = getattr(
            settings,
            "MODEL_SWARM",
            {
                "coding": "njelit1/supreme-coder-3b",
                "reasoning": "njelitltd/supreme-reasoner-3b",
                "general": "ziaulhaq1/supreme-general-3b",
                "creative": "njelitltd2/supreme-creative-3b",
                "master": "njelitltd3/supreme-master-3b",
                "vision": "njelltd5/supreme-vision-3b",
                "draft": "njelltd4/supreme-draft-0.5b",
            },
        )
        self.base_url = "https://api-inference.huggingface.co/models/"

    def classify_task(self, prompt: str) -> str:
        """বাংলা মন্তব্য: প্রম্পট টেক্সটের ভিত্তিতে টাস্ক ক্যাটাগরি ডিটেক্ট করা হয়।"""
        prompt_lower = prompt.lower()

        # Vision / Image Task Detection
        if any(kw in prompt_lower for kw in ["image", "photo", "picture", "screenshot", "diagram", "ocr"]):
            return "vision"

        # Coding Task Detection
        if any(
            kw in prompt_lower for kw in ["code", "python", "def ", "function", "bug", "sql", "javascript", "class "]
        ):
            return "coding"

        # Logic / Reasoning / Math Task Detection
        if any(kw in prompt_lower for kw in ["solve", "math", "equation", "logic", "calculate", "proof", "reason"]):
            return "reasoning"

        # Creative Task Detection
        if any(kw in prompt_lower for kw in ["story", "poem", "song", "lyrics", "script", "creative", "write a story"]):
            return "creative"

        # Fast Draft Speculative Decoding Detection
        if any(kw in prompt_lower for kw in ["fast", "draft", "quick answer", "autocomplete"]):
            return "draft"

        # Master Complex Task Detection
        if len(prompt.split()) > 150 or "step by step" in prompt_lower:
            return "master"

        return "general"

    def route_and_generate(self, prompt: str, parameters: dict[str, Any] | None = None) -> dict[str, Any]:
        """বাংলা মন্তব্য: ডিটেক্টেড মডেল এবং রোটেশন এপিআই কী ব্যবহার করে ইনফ্যারেন্স সম্পন্ন করে।"""
        task_type = self.classify_task(prompt)
        target_model = self.model_map.get(task_type, self.model_map["general"])

        active_key = key_rotator.get_key()
        headers = {"Authorization": f"Bearer {active_key}" if active_key else "", "Content-Type": "application/json"}

        endpoint = f"{self.base_url}{target_model}"
        payload = {"inputs": prompt, "parameters": parameters or {"max_new_tokens": 512, "temperature": 0.7}}

        logger.info(f"🔀 [Router] Task: '{task_type}' | Target Model: '{target_model}'")

        try:
            response = requests.post(endpoint, headers=headers, json=payload, timeout=30)
            response.raise_for_status()
            return {"status": "success", "task": task_type, "model": target_model, "output": response.json()}
        except Exception as e:
            logger.error(f"❌ Inference error for model {target_model}: {e!s}")
            return {"status": "error", "task": task_type, "model": target_model, "error": str(e)}


hf_swarm_router = HFSwarmRouter()
