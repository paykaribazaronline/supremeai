import asyncio
import hashlib
import os
import time
from typing import Any

import httpx
from loguru import logger

from core.agent_orchestrator import route_request
from core.audit_logger import AuditLogger
from core.circuit_breaker import CircuitBreaker
from core.config import settings
from core.free_tier_tracker import FreeTierTracker
from core.free_tier_tracker import get_tracker
from core.input_sanitizer import InputSanitizer
from core.language_router import LanguageRouter
from core.semantic_cache import SemanticCache
from core.token_budget import TokenBudgetManager
from core.token_budget import get_budget_manager
from memory.long_term_memory import LongTermMemory
from tools.cot_reasoner import ChainOfThoughtReasoner

from .model_registry import ModelRegistry


MAX_AGENT_TOKENS = 5000
MAX_AGENT_ITERATIONS = 5


def _get_redis_queue():
    try:
        from core import app as app_mod

        return getattr(app_mod, "redis_queue", None)
    except Exception:
        return None


def run_async_as_sync(coro):
    import asyncio
    from concurrent.futures import ThreadPoolExecutor

    try:
        loop = asyncio.get_running_loop()
    except RuntimeError:
        loop = None

    if loop and loop.is_running():
        with ThreadPoolExecutor(max_workers=1) as executor:
            future = executor.submit(asyncio.run, coro)
            return future.result()
    else:
        return asyncio.run(coro)


def run_async_gen_as_sync(async_gen):
    import asyncio
    import queue
    import threading

    q = queue.Queue()
    done_sentinel = object()

    async def producer():
        try:
            async for item in async_gen:
                q.put(item)
        except Exception as e:
            q.put(e)
        finally:
            q.put(done_sentinel)

    def run_loop():
        asyncio.run(producer())

    t = threading.Thread(target=run_loop)
    t.start()

    while True:
        item = q.get()
        if item is done_sentinel:
            break
        if isinstance(item, Exception):
            raise item
        yield item
    t.join()


async def _await_maybe(val_or_coro):
    import inspect

    if inspect.isawaitable(val_or_coro):
        return await val_or_coro
    return val_or_coro


class ModelRouter:
    """
    Routes tasks to the cheapest capable model with fallbacks.
    Uses ModelRegistry for tier-based model selection.
    Priority:
      1. OpenRouter (cheap + capable)
      2. Gemini (Google AI Studio - Free Tier)
      3. DeepSeek (Free/Cheap Tier)
      4. Groq (Free Tier)
      5. Nvidia (NIM Free API)
      6. HuggingFace (free tier)
      7. Ollama local (offline fallback, disabled in production)
    """

    def __init__(self):
        self.openrouter_api_key = os.getenv("OPENROUTER_API_KEY", "")
        self.hf_api_key = os.getenv("HF_API_KEY", "")
        self.gemini_api_key = os.getenv("GEMINI_API_KEY", "")
        self.deepseek_api_key = os.getenv("DEEPSEEK_API_KEY", "")
        self.groq_api_key = os.getenv("GROQ_API_KEY", "")
        self.nvidia_api_key = os.getenv("NVIDIA_API_KEY", "")
        self.firecrawl_api_key = os.getenv("FIRECRAWL_API_KEY", "")
        self.cloudflare_account_id = os.getenv("CLOUDFLARE_ACCOUNT_ID", "")
        self.cloudflare_api_token = os.getenv("CLOUDFLARE_API_TOKEN", "")
        self.ollama_url = os.getenv("OLLAMA_URL", "http://127.0.0.1:11434")
        self.default_model = os.getenv("DEFAULT_MODEL", "google/gemma-4-31b-it:free")
        self.local_model = os.getenv("LOCAL_MODEL", "llama3")
        self.cot_reasoner = ChainOfThoughtReasoner(max_iterations=3)
        self.input_sanitizer = InputSanitizer()
        self.audit_logger = AuditLogger()
        self._registry = ModelRegistry()
        self._local_rag = None
        self.long_term_memory = LongTermMemory()
        self.language_router = LanguageRouter()
        redis_queue = _get_redis_queue()
        self._breakers = {
            "openrouter": CircuitBreaker("openrouter", redis_queue=redis_queue),
            "gemini": CircuitBreaker("gemini", redis_queue=redis_queue),
            "deepseek": CircuitBreaker("deepseek", redis_queue=redis_queue),
            "groq": CircuitBreaker("groq", redis_queue=redis_queue),
            "nvidia": CircuitBreaker("nvidia", redis_queue=redis_queue),
            "huggingface": CircuitBreaker("huggingface", redis_queue=redis_queue),
            "ollama": CircuitBreaker("ollama", redis_queue=redis_queue),
            "cloudflare": CircuitBreaker("cloudflare", redis_queue=redis_queue),
        }
        self._http_client = httpx.AsyncClient(timeout=30.0)
        self._cache: dict[str, tuple[dict[str, Any], float]] = {}
        self._cache_ttl = 300.0
        # Free-tier tracking (Antigravity 2026-06-22)
        self._free_tier: FreeTierTracker = get_tracker()
        self._budget: TokenBudgetManager = get_budget_manager()

    def _cache_key(self, prompt: str, task_type: str) -> str:
        return hashlib.sha256(f"{prompt}:{task_type}".encode()).hexdigest()

    def _get_from_cache(self, key: str) -> dict[str, Any] | None:
        entry = self._cache.get(key)
        if not entry:
            return None
        result, ts = entry
        if time.time() - ts > self._cache_ttl:
            self._cache.pop(key, None)
            return None
        return result

    def _put_in_cache(self, key: str, value: dict[str, Any]) -> None:
        if len(self._cache) >= 10000:
            oldest = min(self._cache, key=lambda k: self._cache[k][1])
            self._cache.pop(oldest, None)
        self._cache[key] = (value, time.time())

    def _get_local_rag(self):
        if self._local_rag is None:
            try:
                from tools.local_search_rag import LocalSearchRAG

                self._local_rag = LocalSearchRAG()
            except ImportError:
                self._local_rag = None
        return self._local_rag

    @property
    def local_rag(self):
        return self._get_local_rag()

    def _get_keys(self, raw_str: str) -> list[str]:
        if not raw_str:
            return []
        keys = []
        raw_clean = raw_str.replace("\n", ",").replace(";", ",")
        for part in raw_clean.split(","):
            part_stripped = part.strip()
            if part_stripped:
                keys.append(part_stripped)
        return keys

    def _estimate_complexity(self, task_type: str, prompt: str, max_cost: float) -> str:
        upper_task = (task_type or "").upper()
        upper_prompt = (prompt or "").upper()
        prompt_len = len(prompt)

        # Specialized/scientific/medical domains detection
        specialized_keywords = [
            "MEDICAL",
            "SCIENCE",
            "SCIENTIFIC",
            "CLINICAL",
            "GENETICS",
            "PHYSICS",
            "BIO",
            "CHEMISTRY",
        ]
        is_specialized = "SPECIALIZED" in upper_task or any(
            kw in upper_task or kw in upper_prompt for kw in specialized_keywords
        )

        if (
            max_cost >= 0.25
            or "MATH" in upper_task
            or "REASONING" in upper_task
            or prompt_len > 2000
            or is_specialized
        ):
            return "specialized" if is_specialized else "phd_math"
        if "CODE" in upper_task or "CODING" in upper_task or prompt_len > 1000:
            return "code"
        if "SEARCH" in upper_task or "RAG" in upper_task or "RESEARCH" in upper_task:
            return "search"
        if "OCR" in upper_task or "VISION" in upper_task:
            return "vision"
        if "VALIDATION" in upper_task or "SCHEMA" in upper_task:
            return "structured"
        return "general"

    def _has_key_for_provider(self, provider: str) -> bool:
        if provider == "openai":
            return bool(self.openrouter_api_key)
        if provider == "anthropic":
            return bool(self.openrouter_api_key)
        if provider == "google":
            return bool(self.gemini_api_key) or bool(self.openrouter_api_key)
        if provider == "deepseek":
            return bool(self.deepseek_api_key) or bool(self.openrouter_api_key)
        if provider == "groq":
            return bool(self.groq_api_key) or bool(self.openrouter_api_key)
        if provider == "nvidia":
            return bool(self.nvidia_api_key) or bool(self.openrouter_api_key)
        if provider == "huggingface":
            return bool(self.hf_api_key)
        if provider == "cloudflare":
            return bool(self.cloudflare_api_token) and bool(self.cloudflare_account_id)
        if provider == "ollama":
            from core.config import settings

            return settings.env.lower() != "production"
        return False

    def _select_model_by_tier(self, target_tier: int) -> tuple[str, str]:
        tier_models = []
        for model_id, metadata in self._registry.MODELS.items():
            if metadata.get("tier") == target_tier:
                tier_models.append((model_id, metadata))

        tier_models.sort(key=lambda x: x[1].get("rank", 999))

        for _model_id, metadata in tier_models:
            provider = metadata.get("provider")
            if self._has_key_for_provider(provider):
                if provider in ("openai", "anthropic") and self.openrouter_api_key:
                    return "openrouter", metadata.get("openrouter_id")
                if provider == "google":
                    if self.gemini_api_key:
                        # [2026-06-21] gemini-1.5-flash deprecated June 2026, using gemini-3.5-flash
                        return "gemini", "gemini-3.5-flash"
                    return "openrouter", metadata.get("openrouter_id")
                if provider == "deepseek":
                    if self.deepseek_api_key:
                        return "deepseek", "deepseek-chat"
                    return "openrouter", metadata.get("openrouter_id")
                if provider == "groq":
                    if self.groq_api_key:
                        # [2026-06-21] llama3-8b-8192 deprecated, using llama-3.3-70b-versatile
                        return "groq", "llama-3.3-70b-versatile"
                    return "openrouter", metadata.get("openrouter_id")
                if provider == "nvidia":
                    if self.nvidia_api_key:
                        # [2026-06-21] meta/llama3-8b-instruct 404, using meta/llama-3.1-8b-instruct
                        return "nvidia", "meta/llama-3.1-8b-instruct"
                    return "openrouter", metadata.get("openrouter_id")
                if provider == "huggingface" and self.hf_api_key:
                    return "huggingface", "google/flan-t5-base"
                if provider == "ollama":
                    return "ollama", metadata.get("ollama_id", self.local_model)

        if target_tier == 1:
            return self._select_model_by_tier(2)
        elif target_tier == 2:
            return self._select_model_by_tier(5)
        elif target_tier == 5:
            from core.config import settings

            if settings.env.lower() != "production":
                return "ollama", self._registry.MODELS.get("local-qwen-0.5b", {}).get(
                    "ollama_id", self.local_model
                )
            else:
                if self.gemini_api_key:
                    # [2026-06-21] gemini-1.5-flash deprecated, using gemini-3.5-flash
                    return "gemini", "gemini-3.5-flash"
                if self.openrouter_api_key:
                    return "openrouter", self.default_model
                raise RuntimeError(
                    "No available LLM providers configured in production."
                )
        return "ollama", self.local_model

    def _quick_provider_hint(self, task_type: str) -> str:
        """Return the most likely provider name for token budget estimation (fast, no API checks)."""
        if self.gemini_api_key:
            return "gemini"
        if self.groq_api_key:
            return "groq"
        if self.openrouter_api_key:
            return "openrouter"
        return "default"

    def _pick_provider(
        self, task_type: str, prompt: str, max_cost: float
    ) -> tuple[str, str]:
        from core.config import settings

        is_production = settings.env.lower() == "production"

        if task_type == "completion":
            # [2026-06-21] Updated completion provider model names
            if self.gemini_api_key:
                return "gemini", "gemini-3.5-flash"
            if self.openrouter_api_key:
                return "openrouter", "google/gemma-4-31b-it:free"
            if self.deepseek_api_key:
                return "deepseek", "deepseek-chat"
            if self.groq_api_key:
                return "groq", "llama-3.3-70b-versatile"
            if is_production:
                raise RuntimeError(
                    "Production mode requires cloud API keys. Ollama is disabled in production."
                )
            reg_info = self._registry.get_model("local-qwen-0.5b")
            return "ollama", reg_info.get("ollama_id", self.local_model)

        complexity = self._estimate_complexity(task_type, prompt, max_cost)

        if complexity in ("phd_math", "specialized"):
            target_tier = 1
        elif complexity == "code":
            target_tier = 2
        elif (
            task_type in ("translation", "sentiment", "summaries")
            or complexity == "search"
        ):
            target_tier = 5
        else:
            target_tier = 5

        return self._select_model_by_tier(target_tier)

    def _warn_if_low_key_redundancy(self, provider: str) -> None:
        key_map = {
            "openrouter": self.openrouter_api_key,
            "gemini": self.gemini_api_key,
            "deepseek": self.deepseek_api_key,
            "groq": self.groq_api_key,
            "nvidia": self.nvidia_api_key,
            "huggingface": self.hf_api_key,
        }
        raw = key_map.get(provider, "")
        count = len(self._get_keys(raw))
        if count == 1:
            logger.warning(
                f"Provider '{provider}' has only 1 API key configured. Add fallback keys to improve availability."
            )

    def route_and_generate(
        self, prompt: str, task_type: str = "general", max_cost: float = 0.01
    ) -> dict[str, Any]:
        return run_async_as_sync(
            self.async_route_and_generate(prompt, task_type, max_cost)
        )

    async def async_route_and_generate(
        self, prompt: str, task_type: str = "general", max_cost: float = 0.01
    ) -> dict[str, Any]:
        logger.info(
            f"Routing task_type='{task_type}' max_cost={max_cost} "
            f"openrouter={'yes' if self.openrouter_api_key else 'no'}"
        )

        routing = route_request(prompt, task_type)
        logger.info(
            f"Semantic routing: intent={routing.intent}, tier={routing.tier}, expensive={routing.requires_expensive}"
        )

        sanitized = self.input_sanitizer.sanitize(prompt)
        if not sanitized.get("is_valid", True):
            return {
                "success": False,
                "error": f"Input validation/safety block: {sanitized.get('reason')}",
            }
        prompt = sanitized.get("prompt", prompt)

        # ---------------------------------------------------------------
        # [Antigravity 2026-06-22] Apply token budget compression before routing
        # so we never waste free-tier TPM on over-long prompts.
        # ---------------------------------------------------------------
        provider_hint = self._quick_provider_hint(task_type)
        if settings.enable_token_compression:
            prompt, _budget_meta = self._budget.prepare_prompt(
                prompt, provider=provider_hint
            )
        # ---------------------------------------------------------------

        upper_task = (task_type or "general").upper()

        # Multi-modal routing: Detect image/pdf file paths or vision task type
        if "VISION" in upper_task or any(
            ext in prompt.lower() for ext in [".png", ".jpg", ".jpeg", ".pdf"]
        ):
            try:
                from tools.vision_agent import VisionAgent

                vision_agent = VisionAgent()
                file_path = ""
                for w in prompt.split():
                    if any(
                        ext in w.lower() for ext in [".png", ".jpg", ".jpeg", ".pdf"]
                    ):
                        file_path = w.strip("()[]\"'")
                        break
                if file_path and os.path.exists(file_path):
                    logger.info(f"Vision task detected, routing path: {file_path}")
                    if file_path.lower().endswith(".pdf"):
                        res = vision_agent.analyze_pdf(file_path)
                    elif "chart" in prompt.lower():
                        res = vision_agent.analyze_chart(file_path)
                    else:
                        res = vision_agent.analyze_image(file_path)
                    return {
                        "success": res.get("success", False),
                        "provider": "local_vision",
                        "model": "VisionAgent",
                        "text": res.get("text")
                        or res.get("summary")
                        or res.get("error", "No text found"),
                        "cost": 0.0,
                        "structured": res.get("structured", {}),
                    }
            except Exception as e:
                logger.error(f"Failed to run local vision agent: {e}")
        enriched_prompt = prompt

        # Integrate ChainOfThought reasoning for math/reasoning tasks
        if "MATH" in upper_task or "REASONING" in upper_task:
            cot_prompt = self.cot_reasoner.build_prompt(prompt, task_type)
            enriched_prompt = cot_prompt
            logger.info("Using ChainOfThought reasoning for task")

        # Integrate local search RAG for search/research tasks
        if "SEARCH" in upper_task or "RAG" in upper_task or "RESEARCH" in upper_task:
            web_context = ""
            if self.firecrawl_api_key:
                try:
                    logger.info("Using Firecrawl web search for Perplexity-style RAG")
                    headers = {
                        "Authorization": f"Bearer {self.firecrawl_api_key}",
                        "Content-Type": "application/json",
                    }
                    response = run_async_as_sync(
                        self._http_client.post(
                            "https://api.firecrawl.dev/v1/search",
                            headers=headers,
                            json={"query": prompt, "limit": 3},
                            timeout=10.0,
                        )
                    )
                    if response.status_code == 200:
                        search_results = response.json()
                        web_docs = []
                        for item in search_results.get("data", []):
                            title = item.get("title", "Web Result")
                            url = item.get("url", "")
                            markdown_content = item.get("markdown", "") or item.get(
                                "snippet", ""
                            )
                            web_docs.append(
                                f"Source: {title} ({url})\nContent: {markdown_content[:1000]}"
                            )
                        if web_docs:
                            web_context = (
                                "\n\nWeb Search Context (Firecrawl):\n"
                                + "\n\n".join(web_docs)
                            )
                            logger.info("Firecrawl search context added successfully.")
                except Exception as fe:
                    logger.warning(f"Firecrawl web search failed: {fe}")

            try:
                rag_result = self.local_rag.semantic_search(prompt)
                sources = []
                if rag_result.get("status") == "ok" and rag_result.get("matches"):
                    for m in rag_result["matches"]:
                        doc_id = m.get("doc_id")
                        title = m.get("title", "")
                        fields = self.local_rag._index.get(doc_id, [])
                        text = fields[1] if len(fields) > 1 else ""
                        sources.append(f"Source: {title}\n{text}")
                if sources or web_context:
                    context_parts = []
                    if web_context:
                        context_parts.append(web_context)
                    if sources:
                        context_parts.append(
                            "\n\nLocal RAG Context:\n" + "\n\n".join(sources)
                        )
                    enriched_prompt = f"{prompt}\n" + "\n".join(context_parts)
            except Exception as exc:
                logger.warning(f"Local RAG search failed: {exc}")
                if web_context:
                    enriched_prompt = f"{prompt}\n{web_context}"

        # Integrate long-term memory context
        memory_context = self.long_term_memory.build_context()
        if memory_context:
            enriched_prompt = (
                f"{enriched_prompt}\n\nLong-term memory context:\n{memory_context}"
            )

        provider, model = self._pick_provider(task_type, prompt, max_cost)

        lang_route = self.language_router.route(prompt, task_type)
        detected_lang = lang_route.get("language", "english")
        lang_provider = lang_route.get("provider", "openrouter")
        if detected_lang != "english":
            provider = lang_provider
            logger.info(
                f"Language override: detected {detected_lang} => provider {provider}"
            )

        semantic_cache = SemanticCache()
        if semantic_cache.is_configured:
            cached = await semantic_cache.query_similar(prompt)
            if cached:
                logger.info(f"Semantic cache hit: {cached.provider}/{cached.model}")
                return {
                    "success": True,
                    "provider": cached.provider,
                    "model": cached.model,
                    "text": cached.response,
                    "cost": 0.0,
                    "cached": True,
                }

        self._warn_if_low_key_redundancy(provider)
        try:
            result = await self._call(provider, model, enriched_prompt)
            if result.get("success"):
                # [Antigravity 2026-06-22] Record free-tier usage after every successful call
                out_tokens = self._budget.estimate(result.get("text", ""))
                in_tokens = self._budget.estimate(enriched_prompt)
                self._free_tier.record(provider, token_count=in_tokens + out_tokens)
                self.long_term_memory.remember_fact(
                    content=result.get("text", "")[:200],
                    category="response",
                    source=provider,
                    importance=0.3,
                )
                if semantic_cache.is_configured:
                    await semantic_cache.set(
                        prompt, result.get("text", ""), provider, model
                    )
            if ("MATH" in upper_task or "REASONING" in upper_task) and result.get(
                "success"
            ):
                parsed = self.cot_reasoner.parse(result.get("text", ""))
                verification = self.cot_reasoner.verify(parsed.get("final_answer", ""))

                # o1-style self-critique/refinement loop on verification failure
                if verification.get("math_error") or not verification.get(
                    "matches", True
                ):
                    logger.info(
                        "CoT verification failed, initiating o1-style self-critique correction loop..."
                    )
                    error_details = verification.get(
                        "math_error", "Answer does not match expected constraints."
                    )
                    critique_prompt = (
                        f"Original Problem: {prompt}\n\n"
                        f"Your previous response:\n{result.get('text', '')}\n\n"
                        f"Critique / Error detected: {error_details}\n\n"
                        "Please analyze your errors, correct your reasoning step-by-step, and provide the correct answer."
                    )
                    retry_result = await self._call(provider, model, critique_prompt)
                    if retry_result.get("success"):
                        parsed = self.cot_reasoner.parse(retry_result.get("text", ""))
                        verification = self.cot_reasoner.verify(
                            parsed.get("final_answer", "")
                        )
                        result = retry_result

                result["reasoning"] = parsed
                result["cot_verification"] = verification
            return result
        except Exception as exc:
            logger.error(f"Provider {provider} failed: {exc}")
            return await self._fallback(prompt, provider, exc)

    async def _call(self, provider: str, model: str, prompt: str) -> dict[str, Any]:
        retries = 3
        delay = 1.0
        last_exc = None
        for i in range(retries):
            try:
                res = await self._call_with_breaker(provider, model, prompt)

                # If rate limited — pause provider in free-tier tracker
                if not res.get("success") and (
                    "rate limit" in str(res.get("error", "")).lower()
                    or "429" in str(res.get("error", ""))
                ):
                    self._free_tier.mark_rate_limited(provider, pause_seconds=60.0)
                    raise RuntimeError(f"Rate limited: {res.get('error')}")

                return res
            except Exception as exc:
                last_exc = exc
                if i == retries - 1:
                    break
                logger.warning(
                    f"Provider {provider} failed (attempt {i+1}/{retries}). Retrying in {delay}s... Error: {exc}"
                )
                await asyncio.sleep(delay)
                delay *= 2
        raise last_exc or RuntimeError(f"Failed to call provider {provider}")

    async def _call_with_breaker(
        self, provider: str, model: str, prompt: str
    ) -> dict[str, Any]:
        breaker = self._breakers.get(provider)
        if not breaker:
            return await self._execute_call(provider, model, prompt)

        if not breaker.allow_request():
            raise RuntimeError(f"Circuit breaker {provider} is open")

        try:
            res = await self._execute_call(provider, model, prompt)
            breaker.mark_success()
            return res
        except Exception:
            breaker.mark_failure()
            raise

    async def _execute_call(
        self, provider: str, model: str, prompt: str
    ) -> dict[str, Any]:
        if provider == "openrouter":
            return await self._call_openrouter(prompt, model)
        if provider == "huggingface":
            return await self._call_huggingface(prompt, model)
        if provider == "gemini":
            return await self._call_gemini(prompt, model)
        if provider == "deepseek":
            return await self._call_deepseek(prompt, model)
        if provider == "groq":
            return await self._call_groq(prompt, model)
        if provider == "nvidia":
            return await self._call_nvidia(prompt, model)
        if provider == "cloudflare":
            return await self._call_cloudflare(prompt, model)
        return await self._call_ollama(prompt, model)

    async def _fallback(self, prompt: str, failed: str, exc: Exception):
        from core.config import settings

        is_production = settings.env.lower() == "production"

        # [2026-06-21] Fallback order optimized: free providers first, deepseek removed (paid API, violates $0 cost policy)
        remaining = [
            "gemini",
            "openrouter",
            "groq",
            "cloudflare",
            "nvidia",
            "huggingface",
            "ollama",
        ]
        if is_production and "ollama" in remaining:
            remaining.remove("ollama")

        if failed in remaining:
            remaining.remove(failed)

        # Log rotation decision for explainable AI governance
        self.audit_logger.log_decision(
            action_type="agent_rotation",
            decision_details=f"Provider rotated from '{failed}' due to failure.",
            reasoning=f"Error: {exc}. Attempting fallback providers: {remaining}",
        )

        for prov in remaining:
            model = self._model_for(prov)
            try:
                return await self._call(prov, model, prompt)
            except Exception as e:
                logger.error(f"Fallback {prov} failed: {e}")
        return {
            "success": False,
            "error": f"All providers failed, last={exc}",
            "text": "All AI providers are unavailable right now.",
        }

    def _model_for(self, provider: str) -> str:
        # [2026-06-21] Updated all fallback model names to current versions
        # [2026-06-22] Added Claude via OpenRouter free tier
        from core.config import settings

        return {
            "openrouter": self.default_model,
            "huggingface": "google/flan-t5-base",
            "ollama": self.local_model,
            "gemini": "gemini-3.5-flash",
            "deepseek": "deepseek-chat",
            "groq": "llama-3.3-70b-versatile",
            "nvidia": "meta/llama-3.1-8b-instruct",
            "cloudflare": "@cf/meta/llama-3-8b-instruct",
            "claude": settings.claude_openrouter_model,  # Claude via OpenRouter free tier
        }.get(provider, self.default_model)

    async def _call_cloudflare(self, prompt: str, model: str) -> dict[str, Any]:
        if not self.cloudflare_account_id or not self.cloudflare_api_token:
            raise ValueError("Cloudflare credentials not configured.")

        headers = {
            "Authorization": f"Bearer {self.cloudflare_api_token}",
            "Content-Type": "application/json",
        }
        url = f"https://api.cloudflare.com/client/v4/accounts/{self.cloudflare_account_id}/ai/run/{model}"
        payload = {"messages": [{"role": "user", "content": prompt}]}
        try:
            start_time = time.time()
            response = await self._http_client.post(
                url, headers=headers, json=payload, timeout=30.0
            )
            latency = time.time() - start_time
            if response.status_code == 200:
                res_json = response.json()
                text = res_json.get("result", {}).get("response", "")
                return {
                    "success": True,
                    "provider": "cloudflare",
                    "model": model,
                    "text": text,
                    "cost": 0.0,
                    "latency": latency,
                }
            else:
                return {
                    "success": False,
                    "error": f"Cloudflare API error status={response.status_code}: {response.text}",
                    "text": "",
                }
        except Exception as e:
            return {
                "success": False,
                "error": f"Cloudflare call exception: {str(e)}",
                "text": "",
            }

    async def _call_openai_compatible(
        self,
        base_url: str,
        raw_keys: str,
        model: str,
        prompt: str,
        provider_name: str,
        extra_headers: dict[str, str] | None = None,
    ) -> dict[str, Any]:
        keys = self._get_keys(raw_keys)
        if not keys:
            raise ValueError(f"No {provider_name} API keys configured.")

        headers = {
            "Content-Type": "application/json",
        }
        if extra_headers:
            headers.update(extra_headers)
        payload = {
            "model": model,
            "messages": [{"role": "user", "content": prompt}],
        }

        last_exc = None
        for key in keys:
            try:
                auth_header = {"Authorization": f"Bearer {key}"}
                res = await self._http_client.post(
                    base_url,
                    headers={**headers, **auth_header},
                    json=payload,
                )
                res.raise_for_status()
                data = res.json()
                text = data["choices"][0]["message"]["content"]
                return {
                    "success": True,
                    "provider": provider_name,
                    "model": model,
                    "text": text,
                    "cost": 0.0,
                }
            except Exception as e:
                logger.warning(f"{provider_name.title()} key failed: {e}")
                last_exc = e
        raise last_exc or ValueError(f"{provider_name} API call failed.")

    async def _call_openrouter(self, prompt: str, model: str) -> dict[str, Any]:
        return await self._call_openai_compatible(
            "https://openrouter.ai/api/v1/chat/completions",
            self.openrouter_api_key,
            model,
            prompt,
            provider_name="openrouter",
            extra_headers={
                "HTTP-Referer": "https://supremeai.local",
                "X-Title": "SupremeAI 2.0",
            },
        )

    async def _call_gemini(self, prompt: str, model: str) -> dict[str, Any]:
        keys = self._get_keys(self.gemini_api_key)
        if not keys:
            raise ValueError("No Gemini API keys configured.")

        last_exc = None
        for key in keys:
            try:
                url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"
                headers = {
                    "x-goog-api-key": key,
                    "Content-Type": "application/json",
                }
                payload = {"contents": [{"parts": [{"text": prompt}]}]}
                res = await self._http_client.post(url, headers=headers, json=payload)
                res.raise_for_status()
                data = res.json()
                text = data["candidates"][0]["content"]["parts"][0]["text"]
                return {
                    "success": True,
                    "provider": "gemini",
                    "model": model,
                    "text": text,
                    "cost": 0.0,
                }
            except Exception as e:
                logger.warning(f"Gemini key failed: {e}")
                last_exc = e
        raise last_exc or ValueError("Gemini API call failed.")

    async def _call_deepseek(self, prompt: str, model: str) -> dict[str, Any]:
        return await self._call_openai_compatible(
            "https://api.deepseek.com/chat/completions",
            self.deepseek_api_key,
            model,
            prompt,
            provider_name="deepseek",
        )

    async def _call_groq(self, prompt: str, model: str) -> dict[str, Any]:
        return await self._call_openai_compatible(
            "https://api.groq.com/openai/v1/chat/completions",
            self.groq_api_key,
            model,
            prompt,
            provider_name="groq",
        )

    async def _call_nvidia(self, prompt: str, model: str) -> dict[str, Any]:
        return await self._call_openai_compatible(
            "https://integrate.api.nvidia.com/v1/chat/completions",
            self.nvidia_api_key,
            model,
            prompt,
            provider_name="nvidia",
        )

    async def _call_huggingface(self, prompt: str, model: str) -> dict[str, Any]:
        keys = self._get_keys(self.hf_api_key)
        if not keys:
            raise ValueError("No HuggingFace API keys configured.")

        last_exc = None
        for key in keys:
            try:
                headers = {"Authorization": f"Bearer {key}"}
                url = f"https://api-inference.huggingface.co/models/{model}"
                res = await self._http_client.post(
                    url, headers=headers, json={"inputs": prompt}, timeout=20.0
                )
                res.raise_for_status()
                data = res.json()
                text = (
                    data[0].get("generated_text", str(data))
                    if isinstance(data, list)
                    else str(data)
                )
                return {
                    "success": True,
                    "provider": "huggingface",
                    "model": model,
                    "text": text,
                    "cost": 0.0,
                }
            except Exception as e:
                logger.warning(f"HuggingFace key failed: {e}")
                last_exc = e
        raise last_exc or ValueError("HuggingFace API call failed.")

    async def _call_ollama(self, prompt: str, model: str) -> dict[str, Any]:
        url = f"{self.ollama_url}/api/generate"
        payload = {"model": model, "prompt": prompt, "stream": False}
        res = await self._http_client.post(url, json=payload, timeout=10.0)
        res.raise_for_status()
        data = res.json()
        return {
            "success": True,
            "provider": "ollama",
            "model": model,
            "text": data.get("response", ""),
            "cost": 0.0,
        }

    def route_and_generate_with_cot(
        self,
        prompt: str,
        task_type: str = "general",
        max_cost: float = 0.01,
        expected: str | None = None,
    ) -> dict[str, Any]:
        reasoning = self.cot_reasoner.refine_loop(
            prompt, context=task_type, expected=expected
        )
        generated = self.route_and_generate(
            prompt, task_type=task_type, max_cost=max_cost
        )
        if generated.get("success") and reasoning.get("final_answer"):
            generated["cot_verification"] = self.cot_reasoner.verify(
                reasoning["final_answer"], expected
            )
            generated["reasoning"] = reasoning
        return generated

    def query_local_rag(self, query: str) -> dict[str, Any]:
        try:
            return self.local_rag.semantic_search(query)
        except Exception as exc:
            logger.error(f"Local RAG semantic search failed: {exc}")
            return {"status": "error", "error": str(exc), "query": query, "matches": []}

    def route_and_stream(
        self, prompt: str, task_type: str = "general", max_cost: float = 0.01
    ):
        return run_async_gen_as_sync(
            self.async_route_and_stream(prompt, task_type, max_cost)
        )

    async def async_route_and_stream(
        self, prompt: str, task_type: str = "general", max_cost: float = 0.01
    ):
        provider, model = self._pick_provider(task_type, prompt, max_cost)
        logger.info(f"Streaming from provider={provider}, model={model}")
        try:
            if provider == "openrouter" and self.openrouter_api_key:
                res_stream = self._stream_openai_compatible(
                    "https://openrouter.ai/api/v1/chat/completions",
                    self._get_keys(self.openrouter_api_key)[0],
                    model,
                    prompt,
                )
            elif provider == "deepseek" and self.deepseek_api_key:
                res_stream = self._stream_openai_compatible(
                    "https://api.deepseek.com/chat/completions",
                    self._get_keys(self.deepseek_api_key)[0],
                    model,
                    prompt,
                )
            elif provider == "groq" and self.groq_api_key:
                res_stream = self._stream_openai_compatible(
                    "https://api.groq.com/openai/v1/chat/completions",
                    self._get_keys(self.groq_api_key)[0],
                    model,
                    prompt,
                )
            elif provider == "nvidia" and self.nvidia_api_key:
                res_stream = self._stream_openai_compatible(
                    "https://integrate.api.nvidia.com/v1/chat/completions",
                    self._get_keys(self.nvidia_api_key)[0],
                    model,
                    prompt,
                )
            elif provider == "gemini" and self.gemini_api_key:
                res_stream = self._stream_gemini(prompt, model)
            elif provider == "ollama":
                res_stream = self._stream_ollama(prompt, model)
            else:
                res = await self._call(provider, model, prompt)
                yield res.get("text", "")
                return

            if hasattr(res_stream, "__aiter__"):
                async for chunk in res_stream:
                    yield chunk
            else:
                for chunk in res_stream:
                    yield chunk
        except Exception as exc:
            logger.error(f"Streaming failed for {provider}: {exc}")
            yield f"Error during streaming: {exc}"

    async def _stream_openai_compatible(
        self, url: str, key: str, model: str, prompt: str
    ):
        headers = {
            "Authorization": f"Bearer {key}",
            "Content-Type": "application/json",
        }
        payload = {
            "model": model,
            "messages": [{"role": "user", "content": prompt}],
            "stream": True,
        }
        import json

        async with self._http_client.stream(
            "POST", url, headers=headers, json=payload
        ) as response:
            response.raise_for_status()
            async for line in response.iter_lines():
                if line.startswith("data: "):
                    data_str = line[6:]
                    if data_str.strip() == "[DONE]":
                        break
                    try:
                        data = json.loads(data_str)
                        chunk = data["choices"][0]["delta"].get("content", "")
                        if chunk:
                            yield chunk
                    except Exception:
                        pass

    async def _stream_gemini(self, prompt: str, model: str):
        keys = self._get_keys(self.gemini_api_key)
        if not keys:
            raise ValueError("No Gemini API keys configured.")
        key = keys[0]
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:streamGenerateContent"
        headers = {
            "x-goog-api-key": key,
            "Content-Type": "application/json",
        }
        payload = {"contents": [{"parts": [{"text": prompt}]}]}
        import json

        async with self._http_client.stream(
            "POST", url, headers=headers, json=payload
        ) as response:
            response.raise_for_status()
            async for line in response.iter_lines():
                if line:
                    try:
                        data = json.loads(line)
                        chunk = (
                            data.get("candidates", [{}])[0]
                            .get("content", {})
                            .get("parts", [{}])[0]
                            .get("text", "")
                        )
                        if chunk:
                            yield chunk
                    except Exception:
                        pass

    async def _stream_ollama(self, prompt: str, model: str):
        url = f"{self.ollama_url}/api/generate"
        payload = {"model": model, "prompt": prompt, "stream": True}
        import json

        async with self._http_client.stream("POST", url, json=payload) as response:
            response.raise_for_status()
            async for line in response.iter_lines():
                if line:
                    try:
                        data = json.loads(line)
                        chunk = data.get("response", "")
                        if chunk:
                            yield chunk
                    except Exception:
                        pass
