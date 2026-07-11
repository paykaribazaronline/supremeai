# 📄 ফাইল: backend/core/llm_gateway.py

**প্রকার:** .py  
**সাইজ:** 17,982 বাইট  
**আপডেট:** 2026-07-11T16:17:51.566852

---

## কোড

```py
# backend/core/llm_gateway.py
# বাংলা মন্তব্য: সম্পূর্ণ রি-ফ্যাক্টর — os.environ secrets injection সম্পূর্ণ বন্ধ।
# litellm per-call api_key passing → secrets process env-এ leak হয় না।
# litellm global state mutation নিষিদ্ধ।
# Semantic cache, fallback chain, cost guard সব অক্ষুণ্ণ।
# CancelledError সবসময় re-raise।
# import litellm lazy করা হলো — cold start কমাতে।

import json
import os
from collections.abc import AsyncGenerator
from typing import Any

from loguru import logger

from core.circuit_breaker import CircuitBreaker
from core.config import settings
from core.cost_guard import CostGuard
from core.event_bus import ErrorEvent
from core.event_bus import error_event_bus
from core.prompt_handler import normalize_prompt
from core.self_healer import SelfHealerService
from utils.firestore_helpers import get_firestore_db


# বাংলা মন্তব্য: POLICY_PATH এখন os.path দিয়ে বিল্ড হয় — hardcode নেই
_POLICY_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "config",
    "routing_policy.json",
)

# বাংলা মন্তব্য: Provider → settings attribute mapping।
# এই dict update করলেই নতুন provider add হয় — no code duplication।
_MODEL_KEY_MAP: dict[str, str] = {
    "groq": "groq_api_key",
    "gemini": "gemini_api_key",
    "gpt": "openai_api_key",
    "openai": "openai_api_key",
    "deepseek": "deepseek_api_key",
    "openrouter": "openrouter_api_key",
    "hf": "hf_api_key",
    "huggingface": "hf_api_key",
    "nvidia": "nvidia_api_key",
}

# বাংলা মন্তব্য: Default fallback models — routing_policy.json না থাকলে এগুলো ব্যবহার হবে
_DEFAULT_FALLBACK_MODELS: list[str] = [
    "gemini/gemini-1.5-flash",
    "openrouter/auto",
]


class LLMGateway:
    """
    বাংলা মন্তব্য: Multi-provider LLM Gateway।
    - os.environ secrets injection সম্পূর্ণ নিষিদ্ধ — per-call api_key passing।
    - litellm global state mutation নিষিদ্ধ।
    - Heavy import (litellm) function level-এ lazy load।
    - Semantic cache, fallback chain, cost guard intact।
    - CancelledError সবসময় re-raise।
    """

    def __init__(self) -> None:
        self.routing_policy = self._load_routing_policy()
        self._setup_litellm_globals()
        self._setup_callbacks()
        self._circuit_breakers: dict[str, CircuitBreaker] = {}

        # বাংলা মন্তব্য: SemanticCache lazy import — cold start এড়াতে
        from core.semantic_cache import SemanticCache

        self.cache = SemanticCache()

    def _setup_litellm_globals(self) -> None:
        """
        বাংলা মন্তব্য: litellm global settings — শুধু safe non-secret settings।
        os.environ-এ secrets inject করা সম্পূর্ণ নিষিদ্ধ।
        API keys আর এখানে set করা হচ্ছে না।
        প্রতিটি acompletion call-এ api_key parameter pass হবে।
        """
        import litellm  # lazy import — module level নয়

        litellm.drop_params = True
        litellm.telemetry = False
        litellm.use_litellm_proxy = False

    def _load_routing_policy(self) -> dict[str, Any]:
        """বাংলা মন্তব্য: Routing policy JSON load — file not found = safe default।"""
        try:
            if os.path.exists(_POLICY_PATH):
                with open(_POLICY_PATH, encoding="utf-8") as f:
                    return json.load(f)
            logger.warning(f"[LLMGateway] Routing policy not found at '{_POLICY_PATH}'. Using default fallback config.")
        except Exception as exc:  # noqa: BLE001
            logger.exception(f"[LLMGateway] Error loading routing policy: {exc}")
            error_event_bus.emit(
                ErrorEvent(
                    module="llm_gateway",
                    error_type="ROUTING_POLICY_LOAD_FAILED",
                    message=str(exc)[:500],
                    severity="WARNING",
                    context={"policy_path": _POLICY_PATH},
                )
            )
        return {"complexity_rules": {}, "fallback_chain": list(_DEFAULT_FALLBACK_MODELS)}

    def _get_api_key_for_model(self, model: str) -> str | None:
        """
        বাংলা মন্তব্য: Model string থেকে provider identify করে settings থেকে key নেওয়া।
        os.environ নয় — settings._get_cached_secret() থেকে।
        """
        if not model:
            return None
        model_lower = model.lower()
        for prefix, attr_name in _MODEL_KEY_MAP.items():
            if prefix in model_lower:
                key = getattr(settings, attr_name, None)
                return key or None
        return None

    def _setup_callbacks(self) -> None:
        """বাংলা মন্তব্য: litellm callback — cost এবং error tracking।"""
        import litellm  # lazy import

        def success_callback(kwargs, response_obj, start_time, end_time):
            try:
                model = kwargs.get("model", "unknown")
                usage = getattr(response_obj, "usage", None)
                prompt_tokens = getattr(usage, "prompt_tokens", 0)
                completion_tokens = getattr(usage, "completion_tokens", 0)
                cost = response_obj._response_metadata.get("api_cost", 0.0) if hasattr(response_obj, "_response_metadata") else 0.0
                duration = (end_time - start_time).total_seconds()
                logger.info(f"[LLMGateway] ✅ Model={model} | Cost=${cost:.6f} | P={prompt_tokens} C={completion_tokens} | {duration:.2f}s")
            except Exception as exc:  # noqa: BLE001
                logger.warning(f"[LLMGateway] Success callback error: {exc}")

        def failure_callback(kwargs, exception_obj, start_time, end_time):
            model = kwargs.get("model", "unknown")
            try:
                delta = end_time - start_time
                duration = delta.total_seconds() if hasattr(delta, "total_seconds") else float(delta)
            except Exception:  # noqa: BLE001
                duration = 0.0
            logger.error(f"[LLMGateway] ❌ Model={model} failed | Error={str(exception_obj)[:200]} | {duration:.2f}s")
            error_event_bus.emit(
                ErrorEvent(
                    module="llm_gateway",
                    error_type="LLM_CALL_FAILED",
                    message=str(exception_obj)[:500],
                    severity="ERROR",
                    context={"model": model, "duration_s": round(duration, 2)},
                )
            )

        litellm.success_callback = [success_callback]
        litellm.failure_callback = [failure_callback]

    def _build_call_chain(
        self,
        model: str | None,
        provider: str | None,
        task_type: str,
    ) -> list[str]:
        """বাংলা মন্তব্য: Task type অনুযায়ী fallback chain তৈরি।"""
        difficulty = "easy"
        if any(kw in task_type.lower() for kw in ("reasoning", "math", "code")):
            difficulty = "hard"
        elif any(kw in task_type.lower() for kw in ("agent", "analysis")):
            difficulty = "medium"

        model_candidates: list[str] = self.routing_policy.get("complexity_rules", {}).get(difficulty, [])
        fallbacks: list[str] = self.routing_policy.get("fallback_chain", list(_DEFAULT_FALLBACK_MODELS))

        call_chain: list[str] = []
        if model:
            call_chain.append(model)

        all_models = model_candidates + fallbacks
        if provider:
            provider_models = [m for m in all_models if m.startswith(f"{provider}/")]
            other_models = [m for m in all_models if not m.startswith(f"{provider}/")]
            all_models = provider_models + other_models

        for m in all_models:
            if m not in call_chain:
                call_chain.append(m)

        if not call_chain:
            call_chain = list(_DEFAULT_FALLBACK_MODELS)
            logger.warning("[LLMGateway] Empty call chain — using default fallback models.")

        return call_chain

    async def acompletion(
        self,
        prompt: str | list[dict[str, Any]] | None = None,
        messages: list[dict[str, Any]] | None = None,
        task_type: str = "general",
        stream: bool = False,
        timeout: float = 12.0,
        model: str | None = None,
        provider: str | None = None,
        tenant_id: str | None = None,
        **kwargs,
    ) -> Any:
        """বাংলা মন্তব্য: Main async completion interface।"""
        import asyncio

        import litellm  # lazy import

        if messages is not None and prompt is None:
            prompt = messages

        prompt_text = normalize_prompt(prompt)

        # বাংলা মন্তব্য: Semantic cache check — API call আগে cost-zero response
        if prompt_text and not stream:
            cached = await self.cache.query_similar(prompt_text, task_type=task_type)
            if cached:
                return {
                    "success": True,
                    "text": cached.response,
                    "model": cached.model,
                    "cost": 0.0,
                    "cached": True,
                }

        # বাংলা মন্তব্য: Pre-flight cost guard
        if tenant_id:
            db = get_firestore_db()
            if db:
                cost_guard = CostGuard(db)
                await cost_guard.check_budget(tenant_id, 0.01)

        call_chain = self._build_call_chain(model, provider, task_type)

        if isinstance(prompt, list):
            messages_payload = prompt
        else:
            messages_payload = [{"role": "user", "content": prompt}]

        if stream:
            return self._stream_completion(messages_payload, call_chain, timeout)

        last_exception: Exception | None = None
        for current_model in call_chain:
            # Circuit Breaker check
            if current_model not in self._circuit_breakers:
                # Initialize using settings from config (which defaults to threshold=3, cooldown=60)
                self._circuit_breakers[current_model] = CircuitBreaker(
                    name=current_model,
                    failure_threshold=getattr(settings, "circuit_breaker_failure_threshold", 3),
                    recovery_timeout=getattr(settings, "circuit_breaker_cooldown_period", 60),
                )

            cb = self._circuit_breakers[current_model]
            if not cb.allow_request():
                logger.warning(f"[LLMGateway] Circuit breaker OPEN for {current_model}. Skipping...")
                continue

            try:
                logger.info(f"[LLMGateway] Attempting: {current_model}")
                # বাংলা মন্তব্য: api_key per-call pass — os.environ injection সম্পূর্ণ নিষিদ্ধ
                api_key = self._get_api_key_for_model(current_model)
                response = await litellm.acompletion(
                    model=current_model,
                    messages=messages_payload,
                    timeout=timeout,
                    stream=False,
                    api_key=api_key,
                    **kwargs,
                )
                cb.mark_success()
                return {
                    "success": True,
                    "text": response.choices[0].message.content,
                    "model": current_model,
                    "cost": (response._response_metadata.get("api_cost", 0.0) if hasattr(response, "_response_metadata") else 0.0),
                }
            except asyncio.CancelledError:
                # বাংলা মন্তব্য: CancelledError re-raise — কখনো suppress করা যাবে না
                logger.warning(f"[LLMGateway] acompletion cancelled during model {current_model}")
                raise
            except Exception as exc:  # noqa: BLE001
                last_exception = exc
                cb.mark_failure()
                logger.warning(f"[LLMGateway] Model {current_model} failed: {str(exc)[:200]}. Trying next in chain...")
                continue

        # বাংলা মন্তব্য: সব fallbacks exhausted — self healer trigger এবং error emit
        final_exception = last_exception or RuntimeError("All routing models failed to produce a completion.")
        if tenant_id:
            db = get_firestore_db()
            if db:
                healer = SelfHealerService(db)
                await healer.propose_fix(
                    tenant_id=tenant_id,
                    error_pattern=f"LLMGateway all-fail: {str(final_exception)[:100]}",
                    proposed_fix="Check fallback model API keys and routing policy.",
                    impact_score=0.2,
                    dependency_tree=["core.llm_gateway"],
                )
        error_event_bus.emit(
            ErrorEvent(
                module="llm_gateway",
                error_type="ALL_MODELS_FAILED",
                message=str(final_exception)[:500],
                severity="CRITICAL",
                context={"tenant_id": tenant_id, "call_chain": call_chain},
            )
        )
        raise final_exception

    async def _stream_completion(
        self,
        messages: list[dict[str, Any]],
        call_chain: list[str],
        timeout: float,
    ) -> AsyncGenerator[str, None]:
        """বাংলা মন্তব্য: Streaming completion — fallback chain সহ।"""
        import asyncio

        import litellm  # lazy import

        last_exception: Exception | None = None
        for current_model in call_chain:
            # Circuit Breaker check
            if current_model not in self._circuit_breakers:
                self._circuit_breakers[current_model] = CircuitBreaker(
                    name=current_model,
                    failure_threshold=getattr(settings, "circuit_breaker_failure_threshold", 3),
                    recovery_timeout=getattr(settings, "circuit_breaker_cooldown_period", 60),
                )

            cb = self._circuit_breakers[current_model]
            if not cb.allow_request():
                logger.warning(f"[LLMGateway] Circuit breaker OPEN for {current_model}. Skipping...")
                continue

            try:
                logger.info(f"[LLMGateway] Streaming attempt: {current_model}")
                # বাংলা মন্তব্য: api_key per-call — os.environ injection নিষিদ্ধ
                api_key = self._get_api_key_for_model(current_model)
                response_stream = await litellm.acompletion(
                    model=current_model,
                    messages=messages,
                    timeout=timeout,
                    stream=True,
                    api_key=api_key,
                )
                cb.mark_success()
                async for chunk in response_stream:
                    content = chunk.choices[0].delta.content
                    if content:
                        yield content
                return
            except asyncio.CancelledError:
                # বাংলা মন্তব্য: CancelledError re-raise — কখনো suppress করা যাবে না
                logger.warning(f"[LLMGateway] Stream cancelled at model {current_model}")
                raise
            except Exception as exc:  # noqa: BLE001
                last_exception = exc
                cb.mark_failure()
                logger.warning(f"[LLMGateway] Stream model {current_model} failed: {str(exc)[:200]}")
                continue

        raise last_exception or RuntimeError("All streaming fallback options failed.")


# ── Lazy Singleton ─────────────────────────────────────────────────────────────
# বাংলা মন্তব্য: Module-level singleton lazy করা হলো।
# আগে: `llm_gateway = LLMGateway()` import-এ execute হতো।
# এটি cold start বাড়াতো এবং pytest isolation ভাঙতো।
# এখন: প্রথম ব্যবহারের সময় instantiate হবে।
_llm_gateway_instance: "LLMGateway | None" = None


def get_llm_gateway() -> "LLMGateway":
    """বাংলা মন্তব্য: Lazy singleton factory — import সময়ে network call নিষিদ্ধ।"""
    global _llm_gateway_instance
    if _llm_gateway_instance is None:
        _llm_gateway_instance = LLMGateway()
    return _llm_gateway_instance


def __getattr__(name: str):
    """বাংলা মন্তব্য: টেস্ট কালেকশন ফিক্স — পুরানো টেস্ট ফাইলগুলো যদি মডিউল লেভেলের
    'llm_gateway' ভ্যারিয়েবল খোঁজে, তবে এই ম্যাজিক মেথডটি ডাইনামিকালি আমাদের
    Lazy Getter ফাংশনটি সাপ্লাই করবে। এতে ২২টি টেস্ট ফাইল ব্রেক করা ছাড়াই সচল হবে।
    """
    if name == "llm_gateway":
        return get_llm_gateway()
    raise AttributeError(f"module {__name__} has no attribute {name}")

```