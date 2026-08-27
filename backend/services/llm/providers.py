"""Provider implementations for the SupremeAI LLM router.

This module contains provider adapters and small routing value objects that were
previously embedded in ``llm_router.py``.  The public classes are re-exported by
that module for backwards compatibility.
"""

from __future__ import annotations

import json
from collections.abc import AsyncGenerator
from dataclasses import dataclass
from enum import StrEnum
from typing import Any, Protocol

import httpx

from core.config import settings
from core.logging import get_logger
from core.metrics import timed
from core.resilience.circuit_breaker import CircuitBreaker as circuit_breaker

logger = get_logger(__name__)

class Provider(StrEnum):
    """Supported AI model providers."""

    MOONSHOT = "moonshot"
    DEEPSEEK = "deepseek"
    TOGETHER = "together"
    OLLAMA = "ollama"
    GEMINI = "gemini"
    HUGGINGFACE_SPACE = "hf_space"
    OPENAI = "openai"  # বাংলা মন্তব্য: OpenAI প্রোভাইডার সাপোর্টের জন্য যোগ করা হয়েছে

@dataclass
class StreamChunk:
    content: str
    is_finished: bool = False
    provider: Provider | None = None

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

class MoonshotProvider:
    """Moonshot AI (Kimi K2.5) — Primary for Bengali & complex reasoning."""

    name = Provider.MOONSHOT

    def __init__(self) -> None:
        self.api_key = getattr(settings, "moonshot_api_key", "") or ""
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
        if not self.api_key:
            # বাংলা: API key নেই — provider unavailable, fallback chain চলবে
            return False
        try:
            resp = await self.client.get("/models", timeout=5.0)
            return resp.status_code == 200
        except Exception as exc:
            logger.debug(f"MoonshotProvider health check failed: {exc}")
            return False

class DeepSeekProvider:
    """DeepSeek V3 — Fallback for code and cost-efficient tasks."""

    name = Provider.DEEPSEEK

    def __init__(self) -> None:
        self.api_key = getattr(settings, "deepseek_api_key", "") or ""
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
        if not self.api_key:
            return False
        try:
            resp = await self.client.get("/models", timeout=5.0)
            return resp.status_code == 200
        except Exception as exc:
            logger.debug(f"DeepSeekProvider health check failed: {exc}")
            return False

class TogetherProvider:
    """Together AI — Backup for high availability."""

    name = Provider.TOGETHER

    def __init__(self) -> None:
        self.api_key = getattr(settings, "together_api_key", "") or ""
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
        if not self.api_key:
            return False
        try:
            resp = await self.client.get("/models", timeout=5.0)
            return resp.status_code == 200
        except Exception as exc:
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

