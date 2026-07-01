# Universal LLM Gateway for SupremeAI 2.0 (LiteLLM Integration)
# বাংলা মন্তব্য: এটি লাইটএলএলএম ব্যবহার করে মাল্টিপল এআই ভেন্ডর রাউটিং, ফলব্যাক চেইন এবং কস্ট ট্র্যাকিং হ্যান্ডেল করে।

import json
import os
from collections.abc import AsyncGenerator
from typing import Any

import litellm
from loguru import logger

from core.config import settings


# Load routing policy configuration
POLICY_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "config", "routing_policy.json")

class LLMGateway:
    def __init__(self):
        self.routing_policy = self._load_routing_policy()
        self._inject_secrets()
        self._setup_callbacks()
        
        # Configure litellm global settings
        litellm.drop_params = True
        litellm.telemetry = False

        # Initialize semantic cache engine
        from core.semantic_cache import SemanticCache
        self.cache = SemanticCache()

    def _load_routing_policy(self) -> dict[str, Any]:
        try:
            if os.path.exists(POLICY_PATH):
                with open(POLICY_PATH, encoding="utf-8") as f:
                    return json.load(f)
            logger.warning("Routing policy file not found, using default fallback configs.")
        except Exception as e:
            logger.error(f"Error loading routing policy: {e}")
        
        return {
            "complexity_rules": {
                "easy": ["groq/llama-3.3-70b-versatile"],
                "medium": ["gemini/gemini-3.5-flash"],
                "hard": ["openai/gpt-4o-mini"]
            },
            "fallback_chain": ["groq/llama-3.3-70b-versatile", "gemini/gemini-3.5-flash"]
        }

    def _inject_secrets(self):
        # Inject API keys from core settings dynamically into environment for LiteLLM
        # বাংলা মন্তব্য: ডাইনামিক সিক্রেট ইনজেকশন — core settings থেকে কীসমূহ প্রোভাইড করা হচ্ছে
        keys = {
            "GROQ_API_KEY": getattr(settings, "groq_api_key", ""),
            "GEMINI_API_KEY": getattr(settings, "gemini_api_key", ""),
            "OPENAI_API_KEY": getattr(settings, "openai_api_key", ""),
            "DEEPSEEK_API_KEY": getattr(settings, "deepseek_api_key", ""),
            "OPENROUTER_API_KEY": getattr(settings, "openrouter_api_key", ""),
            "HF_API_KEY": getattr(settings, "hf_api_key", "")
        }
        for env_name, key_val in keys.items():
            if key_val:
                os.environ[env_name] = key_val
                logger.info(f"Loaded and injected key: {env_name}")

    def _setup_callbacks(self):
        # বাংলা মন্তব্য: লিঙ্কিং ও কস্ট ট্র্যাকিংয়ের জন্য কলব্যাক মেকানিজম যুক্ত করা হলো
        def success_callback(kwargs, response_obj, start_time, end_time):
            try:
                model = kwargs.get("model", "unknown")
                usage = getattr(response_obj, "usage", None)
                prompt_tokens = usage.prompt_tokens if usage else 0
                completion_tokens = usage.completion_tokens if usage else 0
                # Extract cost dynamically calculated by litellm
                cost = response_obj._response_metadata.get("api_cost", 0.0) if hasattr(response_obj, "_response_metadata") else 0.0
                
                logger.info(
                    f"🟢 [LLMGateway Success] Model: {model} | Cost: ${cost:.6f} | "
                    f"Tokens: P={prompt_tokens} C={completion_tokens} | Duration: {end_time - start_time:.2f}s"
                )
            except Exception as e:
                logger.warning(f"Error executing success callback: {e}")

        def failure_callback(kwargs, exception_obj, start_time, end_time):
            model = kwargs.get("model", "unknown")
            logger.error(
                f"🔴 [LLMGateway Failure] Model: {model} failed! | Error: {str(exception_obj)} | "
                f"Duration: {end_time - start_time:.2f}s"
            )

        litellm.success_callback = [success_callback]
        litellm.failure_callback = [failure_callback]

    async def acompletion(
        self,
        prompt: str | list[dict[str, Any]],
        task_type: str = "general",
        stream: bool = False,
        timeout: float = 12.0
    ) -> Any:
        """
        Main async completion interface with robust fallback routing.
        """
        # Determine initial models by task difficulty
        difficulty = "easy"
        
        # Determine prompt text for complexity checking if it's a list
        prompt_text = ""
        if isinstance(prompt, str):
            prompt_text = prompt
        elif isinstance(prompt, list) and len(prompt) > 0:
            prompt_text = str(prompt[-1].get("content", ""))

        if "reasoning" in task_type.lower() or "math" in task_type.lower() or "code" in task_type.lower():
            difficulty = "hard"
        elif "agent" in task_type.lower() or "analysis" in task_type.lower():
            difficulty = "medium"

        # ── Intercept Semantic Cache ──
        # বাংলা মন্তব্য: এপিআই কল করার পূর্বে সেমান্টিক ক্যাশ চেক করা হচ্ছে
        if prompt_text and not stream:
            cached_res = await self.cache.query_similar(prompt_text, task_type=task_type)
            if cached_res:
                return {
                    "success": True,
                    "text": cached_res.response,
                    "model": cached_res.model,
                    "cost": 0.0,
                    "cached": True
                }

        model_candidates = self.routing_policy.get("complexity_rules", {}).get(difficulty, [])
        fallbacks = self.routing_policy.get("fallback_chain", [])
        
        # Merge target candidate with the fallback chain to prevent duplication
        call_chain = []
        for m in (model_candidates + fallbacks):
            if m not in call_chain:
                call_chain.append(m)

        # বাংলা মন্তব্য: পেলোড নরমালাইজেশন — স্ট্রিং অথবা মেসেজ লিস্ট দুই ফরম্যাটই সাপোর্ট করে
        if isinstance(prompt, list):
            messages = prompt
        else:
            messages = [{"role": "user", "content": prompt}]
        
        if stream:
            return self._stream_completion(messages, call_chain, timeout)

        # Non-streaming completion
        last_exception = None
        for model in call_chain:
            try:
                logger.info(f"Attempting completion with model: {model}")
                response = await litellm.acompletion(
                    model=model,
                    messages=messages,
                    timeout=timeout,
                    stream=False
                )
                return {
                    "success": True,
                    "text": response.choices[0].message.content,
                    "model": model,
                    "cost": response._response_metadata.get("api_cost", 0.0) if hasattr(response, "_response_metadata") else 0.0
                }
            except Exception as e:
                last_exception = e
                logger.warning(f"Model {model} failed in chain. Exception: {e}")
                continue

        raise last_exception or RuntimeError("All routing models failed to produce a completion.")

    async def _stream_completion(self, messages: list[dict[str, str]], call_chain: list[str], timeout: float) -> AsyncGenerator[str, None]:
        # Handle streaming responses with fallback failover support
        # বাংলা মন্তব্য: স্ট্রিমিং সম্পন্ন করার জন্য জেনারেটর মেথড
        last_exception = None
        for model in call_chain:
            try:
                logger.info(f"Attempting streaming with model: {model}")
                response_stream = await litellm.acompletion(
                    model=model,
                    messages=messages,
                    timeout=timeout,
                    stream=True
                )
                async for chunk in response_stream:
                    content = chunk.choices[0].delta.content
                    if content:
                        yield content
                return # Successfully streamed out all tokens
            except Exception as e:
                last_exception = e
                logger.warning(f"Model {model} streaming failed, trying fallback...")
                continue
        
        raise last_exception or RuntimeError("All streaming fallback options failed.")

# Initialize global gateway singleton
llm_gateway = LLMGateway()
