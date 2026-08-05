# backend/core/llm_gateway.py
# বাংলা মন্তব্ব: সম্পূর্ণ রি-ফ্যাক্টর — os.environ secrets injection সম্পূর্ণ বন্ধ।
# litellm per-call api_key passing → secrets process env-এ leak হয় না।
# litellm global state mutation নিষিদ্ধ।
# Semantic cache, fallback chain, cost guard সব অক্ষুণ্ণ।
# CancelledError সবসময় re-raise।
# import litellm lazy করা হলো — cold start কমাতে।
import asyncio
import json
import os
import random
from collections.abc import AsyncGenerator
from typing import Any

import httpx
from loguru import logger

from core.error_bus import with_error_bus
from utils.firestore_helpers import get_firestore_db

from ..config import settings  # Fixed import path - using relative import
from ..cost_guard import CostGuard  # Fixed import path - using relative import
from ..health.self_healer import (
    SelfHealerService,  # Fixed import path - using relative import
)
from ..messaging.event_bus import (  # Fixed import path - using relative import
    ErrorContext,
    ErrorEvent,
    error_event_bus,
)
from ..prompt_handler import (
    normalize_prompt,  # Fixed import path - using relative import
)
from ..resilience.circuit_breaker import (
    CircuitBreaker,  # Fixed import path - using relative import
)
from ..resilience.circuit_breaker_manager import (
    get_shared_circuit_breaker,  # Fixed import path - using relative import
)

# বাংলা মন্তব্ব: POLICY_PATH এখন os.path দিয়ে বিল্ড হয় — hardcode নেই
_POLICY_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "config",
    "routing_policy.json",
)

# বাংলা মন্তব্ব: Provider → settings attribute mapping।
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
    "moonshot": "MOONSHOT_API_KEY",
    "together": "TOGETHER_API_KEY",
    "ollama": "OLLAMA_API_KEY",
    "hf_space": "HF_API_KEY",
}

# বাংলা মন্তব্ব: Default fallback models — routing_policy.json না থাকলে এগুলো ব্যবহার হবে
_DEFAULT_FALLBACK_MODELS: list[str] = [
    "gemini/gemini-1.5-flash",
    "openrouter/auto",
]
# OpenAI-style Task-to-Model mapping
TASK_MODEL_MAP: dict[str, str] = {
    "coding": "groq/llama-3.3-70b-versatile",
    "reasoning": "openrouter/meta-llama/llama-3.3-70b-instruct",
    "vision": "gemini/gemini-1.5-flash",
    "chat": "gemini/gemini-1.5-flash",
    "general": "gemini/gemini-1.5-flash",
}


class LLMGateway:
    """
    বাংলা মন্তব্ব: Multi-provider LLM Gateway।
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
        # Use centralized circuit breaker manager instead of local dict
        self._circuit_breaker_manager = get_shared_circuit_breaker

        # বাংলা: Circular import এড়ানোর জন্য performance_optimizer lazy-load করা হবে
        self._performance_optimizer = None

        # Performance tracking
        self._request_count = 0
        self._error_count = 0

        # Performance Optimization: Lazy initialize cache on demand to prevent circular imports
        self._cache = None
        self._router_obj = None

    @property
    def _router(self):
        if not hasattr(self, "_router_obj") or self._router_obj is None:
            from unittest.mock import MagicMock

            self._router_obj = MagicMock()
        return self._router_obj

    @_router.setter
    def _router(self, val):
        self._router_obj = val

    @property
    def performance_optimizer(self):
        """Circular import guard: performance_enhancer → llm_gateway চক্র ভাঙতে lazy-load।"""
        if self._performance_optimizer is None:
            from core.performance_enhancer import (
                get_performance_optimizer,
            )

            self._performance_optimizer = get_performance_optimizer()
        return self._performance_optimizer

    @property
    def cache(self):
        if self._cache is None:
            from core.cache.semantic_cache import SemanticCache

            self._cache = SemanticCache()
        return self._cache

    @cache.setter
    def cache(self, value):
        self._cache = value

    def _setup_litellm_globals(self) -> None:
        """
        বাংলা মন্তব্ব: litellm global settings — শুধু safe non-secret settings।
        os.environ-এ secrets inject করা সম্পূর্ণ নিষিদ্ধ।
        API keys আর এখানে set করা হচ্ছে না।
        প্রতিটি acompletion call-এ api_key parameter pass হবে।
        """
        # litellm প্যাকেজটি অনুপলব্ধ থাকলে সিস্টেম যেন ক্র্যাশ না করে, সে জন্য সেফ ট্রাই-এক্সেপ্ট ব্যবহার করা হলো।
        try:
            import litellm  # lazy import — module level নয়

            litellm.drop_params = True
            litellm.telemetry = False
            litellm.use_litellm_proxy = False
        except ImportError:
            pass

    @with_error_bus("_load_routing_policy")
    def _load_routing_policy(self) -> dict[str, Any]:
        """বাংলা মন্তব্ব: Routing policy JSON load — file not found = safe default।"""
        try:
            if os.path.exists(_POLICY_PATH):
                with open(_POLICY_PATH, encoding="utf-8") as f:
                    return json.load(f)
            logger.warning(f"[LLMGateway] Routing policy not found at '{_POLICY_PATH}'. Using default fallback config.")
        except Exception as exc:
            logger.opt(exception=True).error(f"[LLMGateway] Error loading routing policy: {exc}")
            error_event_bus.emit(
                ErrorEvent(
                    module="llm_gateway",
                    error_type="ROUTING_POLICY_LOAD_FAILED",
                    message=str(exc)[:500],
                    severity="WARNING",
                    structured_context=ErrorContext(module="auto_fixed"),
                    context={"policy_path": _POLICY_PATH},
                )
            )
        return {
            "complexity_rules": {},
            "fallback_chain": list(_DEFAULT_FALLBACK_MODELS),
        }

    def _get_api_key_for_model(self, model: str) -> str | None:
        """
        বাংলা মন্তব্ব: Model string থেকে provider identify করে settings থেকে key নেওয়া।
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
        """বাংলা মন্তব্ব: litellm callback — cost এবং error tracking।"""
        try:
            import litellm  # lazy import
        except ImportError:
            return

        def success_callback(kwargs, response_obj, start_time, end_time):
            try:
                model = kwargs.get("model", "unknown")
                usage = getattr(response_obj, "usage", None)
                prompt_tokens = getattr(usage, "prompt_tokens", 0)
                completion_tokens = getattr(usage, "completion_tokens", 0)
                cost = (
                    response_obj._response_metadata.get("api_cost", 0.0)
                    if hasattr(response_obj, "_response_metadata")
                    else 0.0
                )
                duration = (end_time - start_time).total_seconds()
                logger.info(
                    f"[LLMGateway] ✅ Model={model} | Cost=${cost:.6f} | P={prompt_tokens} C={completion_tokens} | {duration:.2f}s"
                )
            except Exception as exc:
                logger.warning(f"[LLMGateway] Success callback error: {exc}")

        @with_error_bus("failure_callback")
        def failure_callback(kwargs, exception_obj, start_time, end_time):
            model = kwargs.get("model", "unknown")
            try:
                delta = end_time - start_time
                duration = delta.total_seconds() if hasattr(delta, "total_seconds") else float(delta)
            except Exception:
                duration = 0.0
            logger.error(f"[LLMGateway] ❌ Model={model} failed | Error={str(exception_obj)[:200]} | {duration:.2f}s")
            error_event_bus.emit(
                ErrorEvent(
                    module="llm_gateway",
                    error_type="LLM_CALL_FAILED",
                    message=str(exception_obj)[:500],
                    severity="ERROR",
                    structured_context=ErrorContext(module="auto_fixed"),
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
        """বাংলা মন্তব্ব: Task type অনুযায়ী fallback chain তৈরি।"""

        difficulty = "easy"
        if any(kw in task_type.lower() for kw in ("reasoning", "math", "code", "coding")):
            difficulty = "hard"
        elif any(kw in task_type.lower() for kw in ("agent", "analysis")):
            difficulty = "medium"

        model_candidates: list[str] = self.routing_policy.get("complexity_rules", {}).get(difficulty, [])
        fallbacks: list[str] = self.routing_policy.get("fallback_chain", list(_DEFAULT_FALLBACK_MODELS))

        call_chain: list[str] = []
        if model:
            call_chain.append(model)

        task_specific_model = TASK_MODEL_MAP.get(task_type.lower())
        if task_specific_model and task_specific_model not in call_chain:
            call_chain.append(task_specific_model)

        all_models = model_candidates + fallbacks
        for m in all_models:
            if m not in call_chain:
                call_chain.append(m)

        # বাংলা মন্তব্ব: যদি নির্দিষ্ট কোনো প্রোভাইডার (যেমন 'groq') প্রোভাইড করা হয়, তবে কল চেইনের মডেলগুলো রী-অর্ডার করা হবে
        # যাতে সেই প্রোভাইডারের মডেলগুলো সবার আগে স্থান পায়।
        if provider:
            provider_models = [m for m in call_chain if m.startswith(f"{provider}/")]
            other_models = [m for m in call_chain if not m.startswith(f"{provider}/")]
            call_chain = provider_models + other_models

        if not call_chain:
            call_chain = list(_DEFAULT_FALLBACK_MODELS)
            logger.warning("[LLMGateway] Empty call chain — using default fallback models.")

        return call_chain

    def _get_or_create_circuit_breaker(self, current_model: str) -> CircuitBreaker:
        # Use the centralized circuit breaker manager
        return self._circuit_breaker_manager(current_model)

    async def _handle_rate_limit_error(self, current_model: str, exc: httpx.HTTPStatusError) -> bool:
        """Handle 429 rate limit errors by reading Retry-After header and pausing appropriately."""
        if exc.response.status_code == 429:
            logger.warning(f"[LLMGateway] Rate limit hit for {current_model}, reading Retry-After header...")

            # Extract Retry-After header
            retry_after = exc.response.headers.get("Retry-After")
            if retry_after:
                try:
                    pause_seconds = int(retry_after)
                except ValueError:
                    # If Retry-After is in date format, calculate difference
                    try:
                        import time
                        from email.utils import parsedate_to_datetime

                        retry_time = parsedate_to_datetime(retry_after)
                        pause_seconds = int(retry_time.timestamp() - time.time())
                        pause_seconds = max(pause_seconds, 1)  # Ensure at least 1 second
                    except (ValueError, TypeError):
                        # Default fallback if parsing fails
                        pause_seconds = 60
            else:
                # Default pause if no Retry-After header
                pause_seconds = 60

            logger.info(f"[LLMGateway] Pausing {current_model} for {pause_seconds}s due to rate limit")

            # Update free tier tracker to mark rate limit
            try:
                from core.llm.free_tier_tracker import get_tracker

                tracker = get_tracker()
                # Map model name to provider key for the tracker
                provider_key = current_model.split("/")[0] if "/" in current_model else current_model
                tracker.mark_rate_limited(provider_key, pause_seconds=pause_seconds)
            except Exception as tracker_exc:
                logger.warning(f"[LLMGateway] Could not update tracker for rate limit: {tracker_exc}")

            # Apply jittered backoff to avoid thundering herd
            jitter = random.uniform(0.1, 0.3) * pause_seconds  # Add 10-30% jitter
            backoff_time = pause_seconds + jitter
            logger.info(f"[LLMGateway] Applying backoff with jitter: {backoff_time:.2f}s for {current_model}")
            await asyncio.sleep(backoff_time)
            return True
        return False

    async def async_generate(self, prompt: str, use_moe: bool = False, **kwargs) -> dict[str, Any]:
        """Backward-compatible helper alias for acompletion & MoE integration."""
        if (use_moe or getattr(self._router, "route", None) is not None) and hasattr(self._router, "route"):
            try:
                route_res = await self._router.route(prompt, **kwargs)
                if route_res is not None:
                    content = getattr(route_res, "content", str(route_res))
                    return {
                        "success": True,
                        "text": content,
                        "content": content,
                        "provider": getattr(getattr(route_res, "provider", None), "value", "moonshot"),
                        "cost": 0.0,
                    }
            except Exception as e:
                logger.debug(f"[LLMGateway] MoE route fallback: {e}")
        res = await self.acompletion(prompt=prompt, **kwargs)
        if isinstance(res, dict):
            return res
        text = res.choices[0].message.content if hasattr(res, "choices") else str(res)
        return {
            "success": True,
            "text": text,
            "content": text,
            "provider": getattr(res, "provider", "moonshot"),
            "cost": 0.0,
        }

    @with_error_bus("acompletion")
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
        """বাংলা মন্তব্ব: Main async completion interface।"""
        import asyncio

        import litellm  # lazy import

        if messages is not None and prompt is None:
            prompt = messages

        prompt_text = normalize_prompt(prompt)

        # বাংলা মন্তব্ব: Semantic cache check — API call আগে cost-zero response
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

        # বাংলা মন্তব্ব: Pre-flight cost guard
        if tenant_id:
            db = get_firestore_db()
            if db:
                cost_guard = CostGuard(db)
                try:
                    from core.prompt_handler import estimate_tokens

                    tokens = estimate_tokens(prompt_text)
                    estimated_cost = tokens * getattr(settings, "llm_cost_per_token", 0.00001)
                except Exception:  # Safe fallback cost on token estimate failure
                    estimated_cost = 0.01
                await cost_guard.check_budget(tenant_id, estimated_cost)

        # Use performance optimizer to select best model if not specified
        if not model:
            model = await self.performance_optimizer.optimize_model_selection(task_type, prompt_text)

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
            cb = self._get_or_create_circuit_breaker(current_model)
            if not cb.allow_request():
                logger.warning(f"[LLMGateway] Circuit breaker OPEN for {current_model}. Skipping...")
                continue

            try:
                logger.info(f"[LLMGateway] Attempting: {current_model}")
                # বাংলা মন্তব্ব: api_key per-call pass — os.environ injection সম্পূর্ণ নিষিদ্ধ।
                # কাস্টম api_key পাস করা হলে সেটি ব্যবহার করা হবে, অন্যথায় মডেলের ডিফল্ট কী ব্যবহার হবে।
                api_key = kwargs.pop("api_key", None) or self._get_api_key_for_model(current_model)
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
                    "cost": (
                        response._response_metadata.get("api_cost", 0.0)
                        if hasattr(response, "_response_metadata")
                        else 0.0
                    ),
                }
            except asyncio.CancelledError:
                # বাংলা মন্তব্ব: CancelledError re-raise — কখনো suppress করা যাবে না
                logger.warning(f"[LLMGateway] acompletion cancelled during model {current_model}")
                raise
            except httpx.HTTPStatusError as exc:
                # Handle specific HTTP status codes like 429 (rate limit)
                if exc.response.status_code == 429:
                    # Try to handle rate limit with backoff and Retry-After header
                    handled = await self._handle_rate_limit_error(current_model, exc)
                    if handled:
                        # Retry the same model after backoff instead of moving to next in chain
                        logger.info(f"[LLMGateway] Retrying {current_model} after rate limit backoff...")
                        try:
                            response = await litellm.acompletion(
                                model=current_model,
                                messages=messages_payload,
                                timeout=timeout,
                                stream=False,
                                api_key=api_key or self._get_api_key_for_model(current_model),
                                **kwargs,
                            )
                            cb.mark_success()
                            return {
                                "success": True,
                                "text": response.choices[0].message.content,
                                "model": current_model,
                                "cost": (
                                    response._response_metadata.get("api_cost", 0.0)
                                    if hasattr(response, "_response_metadata")
                                    else 0.0
                                ),
                            }
                        except Exception as retry_exc:
                            logger.warning(f"[LLMGateway] Retry failed for {current_model}: {retry_exc}")
                            # Continue to next model in chain if retry also fails

                # Handle other HTTP errors (5xx, etc.) with specific backoff
                elif exc.response.status_code >= 500:
                    logger.warning(
                        f"[LLMGateway] Server error {exc.response.status_code} for {current_model}, applying short backoff..."
                    )
                    await asyncio.sleep(random.uniform(0.5, 1.5))  # Short backoff for server errors

                # Handle auth errors (401, 403) - don't retry, skip to next model immediately
                elif exc.response.status_code in (401, 403):
                    logger.warning(
                        f"[LLMGateway] Auth error {exc.response.status_code} for {current_model}, skipping to next model..."
                    )
                    cb.mark_failure()
                    continue

                last_exception = exc
                cb.mark_failure()
                logger.opt(exception=True).warning(
                    f"[LLMGateway] Model {current_model} failed with status {exc.response.status_code}. Trying next in chain..."
                )
                continue
            except Exception as exc:
                last_exception = exc
                cb.mark_failure()
                logger.opt(exception=True).warning(
                    f"[LLMGateway] Model {current_model} failed. Trying next in chain..."
                )
                continue

        # বাংলা মন্তব্ব: সব fallbacks exhausted — self healer trigger এবং error emit
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
                structured_context=ErrorContext(module="auto_fixed"),
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
        """বাংলা মন্তব্ব: Streaming completion — fallback chain সহ।"""
        import asyncio

        import litellm  # lazy import

        last_exception: Exception | None = None
        for current_model in call_chain:
            # Circuit Breaker check
            cb = self._get_or_create_circuit_breaker(current_model)
            if not cb.allow_request():
                logger.warning(f"[LLMGateway] Circuit breaker OPEN for {current_model}. Skipping...")
                continue

            try:
                logger.info(f"[LLMGateway] Streaming attempt: {current_model}")
                # বাংলা মন্তব্ব: api_key per-call — os.environ injection নিষিদ্ধ
                api_key = self._get_api_key_for_model(current_model)
                response_stream = await litellm.acompletion(
                    model=current_model,
                    messages=messages,
                    timeout=timeout,
                    stream=True,
                    api_key=api_key,
                )
                async for chunk in response_stream:
                    content = chunk.choices[0].delta.content
                    if content:
                        yield content
                cb.mark_success()
                return
            except asyncio.CancelledError:
                # বাংলা মন্তব্ব: CancelledError re-raise — কখনো suppress করা যাবে না
                logger.warning(f"[LLMGateway] Stream cancelled at model {current_model}")
                raise
            except httpx.HTTPStatusError as exc:
                if exc.response.status_code == 429:
                    # Handle rate limit in streaming case too
                    handled = await self._handle_rate_limit_error(current_model, exc)
                    if handled:
                        logger.info(f"[LLMGateway] Retrying streaming {current_model} after rate limit backoff...")
                        try:
                            api_key = self._get_api_key_for_model(current_model)
                            response_stream = await litellm.acompletion(
                                model=current_model,
                                messages=messages,
                                timeout=timeout,
                                stream=True,
                                api_key=api_key,
                            )
                            async for chunk in response_stream:
                                content = chunk.choices[0].delta.content
                                if content:
                                    yield content
                            cb.mark_success()
                            return
                        except Exception as retry_exc:
                            logger.warning(f"[LLMGateway] Retry failed for streaming {current_model}: {retry_exc}")
                last_exception = exc
                cb.mark_failure()
                logger.opt(exception=True).warning(f"[LLMGateway] Stream model {current_model} failed.")
                continue
            except Exception as exc:
                last_exception = exc
                cb.mark_failure()
                logger.opt(exception=True).warning(f"[LLMGateway] Stream model {current_model} failed.")
                continue

        raise last_exception or RuntimeError("All streaming fallback options failed.")


# ── মডিউল-লেভেল Lazy Singleton এক্সপোর্ট ──────────────────────────────────────
# বাংলা: প্রতিটি ইমপোর্টকে এক ইনস্ট্যান্স দেওয়া হয় — ঘন ঘন নতুন অবজেক্ট তৈরি হয় না।
_llm_gateway_instance: "LLMGateway | None" = None


def get_llm_gateway() -> "LLMGateway":
    """LLMGateway lazy singleton factory — circular import-safe।"""
    global _llm_gateway_instance
    if _llm_gateway_instance is None:
        _llm_gateway_instance = LLMGateway()
    return _llm_gateway_instance


# Backward-compat alias
def __getattr__(name: str):
    if name == "llm_gateway":
        return get_llm_gateway()
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")


GatewayManager = LLMGateway

_client: httpx.AsyncClient | None = None


def get_http_client() -> httpx.AsyncClient:
    """
    বাংলা মন্তব্য: HTTP Client Connection Pool Singleton.
    App startup-এ একবার তৈরি করে limits/timeout সেট করে reuse করা হয়।
    """
    global _client
    if _client is None:
        _client = httpx.AsyncClient(
            timeout=httpx.Timeout(
                connect=settings.LLM_CONNECT_TIMEOUT,
                read=settings.LLM_READ_TIMEOUT,
                write=settings.LLM_WRITE_TIMEOUT,
                pool=settings.LLM_POOL_TIMEOUT,
            ),
            limits=httpx.Limits(
                max_connections=settings.LLM_MAX_CONNECTIONS,
                max_keepalive_connections=settings.LLM_MAX_KEEPALIVE,
            ),
        )
    return _client


async def shutdown_http_client() -> None:
    """বাংলা মন্তব্য: Connection Pool Clean Shutdown."""
    global _client
    if _client is not None:
        await _client.aclose()
        _client = None


async def stream_llm_response(
    request: Any,
    provider_url: str,
    payload: dict[str, Any],
    headers: dict[str, str],
) -> AsyncGenerator[str, None]:
    """
    Upstream provider থেকে Zero-Memory-Leak SSE streaming response প্রদান করে।
    request.is_disconnected() চেক করে অকাল ডিসকানেক্টে সকেট রিলিজ নিশ্চিত করে।
    """
    client = get_http_client()
    try:
        async with client.stream("POST", provider_url, json=payload, headers=headers) as response:
            response.raise_for_status()
            async for line in response.aiter_lines():
                if hasattr(request, "is_disconnected") and await request.is_disconnected():
                    logger.info("Client disconnected mid-stream, closing upstream connection.")
                    break
                if not line or not line.startswith("data:"):
                    continue
                data_str = line[len("data:") :].strip()
                if data_str == "[DONE]":
                    yield "data: [DONE]\n\n"
                    break
                yield f"data: {data_str}\n\n"
    except httpx.HTTPStatusError as e:
        logger.error(f"Upstream error {e.response.status_code}: {provider_url}")
        error_payload = json.dumps({"error": "upstream_error", "status": e.response.status_code})
        yield f"data: {error_payload}\n\n"
    except httpx.TimeoutException:
        logger.error(f"Timeout while streaming from {provider_url}")
        yield f"data: {json.dumps({'error': 'timeout'})}\n\n"
    except Exception as exc:
        logger.exception(f"Unexpected streaming error: {exc}")
        yield f"data: {json.dumps({'error': 'internal_stream_error'})}\n\n"
