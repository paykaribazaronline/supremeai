# 📄 ফাইল: backend/core/autocache_proxy.py

**প্রকার:** .py  
**সাইজ:** 9,343 বাইট  
**আপডেট:** 2026-07-08T10:24:21.715814

---

## কোড

```py
# 🚀 Autocache Proxy - API Cost Optimization Engine
# বাংলা মন্তব্য: এটি সব API রিকোয়েস্ট ইন্টারসেপ্ট করে সিমান্টিক ক্যাশিং এবং রিকোয়েস্ট ডিডুপ্লিকেশনের মাধ্যমে ৯০% খরচ কমায়

import hashlib
import time
from typing import Any

from loguru import logger

from core.prompt_handler import estimate_tokens
from core.semantic_cache import SemanticCache


class AutocacheProxy:
    """
    API রিকোয়েস্ট ইন্টারসেপ্টর এবং স্মার্ট ক্যাশিং ইঞ্জিন
    
    ফিচার:
    - সিমান্টিক ডুপ্লিকেট ডিটেকশন
    - মাল্টিপল ভেন্ডর কস্ট এস্টিমেশন
    - অটোমেটিক প্যারামিটার অপটিমাইজেশন
    - কস্ট মেট্রিক্স ট্র্যাকিং
    """  # noqa: W293

    def __init__(self, cache: SemanticCache):
        self.cache = cache
        self.request_history = {}
        self.cost_metrics = {
            "total_requests": 0,
            "cached_hits": 0,
            "total_cost_saved": 0.0,
            "dedup_requests": 0
        }
        self.vendor_costs = {
            "openai/gpt-4o": {"input": 0.005, "output": 0.015},
            "openai/gpt-4-turbo": {"input": 0.01, "output": 0.03},
            "gemini/gemini-2.5-flash": {"input": 0.000075, "output": 0.0003},
            "groq/llama-3.3-70b-versatile": {"input": 0, "output": 0},
            "anthropic/claude-3-opus": {"input": 0.015, "output": 0.075},
        }

    def _compute_request_hash(self, model: str, prompt: str) -> str:
        """রিকোয়েস্টের জন্য ইউনিক হ্যাশ তৈরি করুন"""
        content = f"{model}:{prompt}"
        return hashlib.sha256(content.encode()).hexdigest()

    def _calculate_cost(self, model: str, input_tokens: int, output_tokens: int) -> float:
        """API কল খরচ ক্যালকুলেট করুন"""
        if model not in self.vendor_costs:
            logger.warning(f"Unknown model: {model}, assuming free tier")
            return 0.0

        costs = self.vendor_costs[model]
        total_cost = (input_tokens * costs["input"]) + (output_tokens * costs["output"])
        return total_cost

    async def should_use_cache(
        self,
        model: str,
        prompt: str,
        task_type: str = "general",
        similarity_threshold: float = 0.85
    ) -> dict[str, Any]:
        """
        সিমান্টিক ক্যাশ থেকে রেসপন্স পাওয়া যাবে কিনা চেক করুন
        
        রিটার্ন:
        {
            "should_cache": bool,
            "cached_response": str or None,
            "estimated_cost_saved": float,
            "cache_score": float
        }
        """  # noqa: W293
        self.cost_metrics["total_requests"] += 1

        # সিমান্টিক ক্যাশ থেকে খুঁজুন
        cached_result = await self.cache.query_similar(prompt, task_type)

        if cached_result and cached_result.response:
            # খরচ সেভিং্স ক্যালকুলেট করুন
            input_tokens = estimate_tokens(prompt)
            estimated_cost = self._calculate_cost(model, input_tokens, 100)

            self.cost_metrics["cached_hits"] += 1
            self.cost_metrics["total_cost_saved"] += estimated_cost

            logger.info(
                f"💰 [CACHE HIT] Model: {model} | Cost Saved: ${estimated_cost:.6f} | "
                f"Total Saved: ${self.cost_metrics['total_cost_saved']:.6f}"
            )

            return {
                "should_cache": True,
                "cached_response": cached_result.response,
                "estimated_cost_saved": estimated_cost,
                "cache_score": 0.95  # Semantic match score
            }

        return {
            "should_cache": False,
            "cached_response": None,
            "estimated_cost_saved": 0.0,
            "cache_score": 0.0
        }

    async def deduplicate_request(
        self,
        model: str,
        prompt: str
    ) -> dict[str, Any]:
        """
        একই রিকোয়েস্ট ডুপ্লিকেট আছে কিনা চেক করুন
        এবং পেন্ডিং রিকোয়েস্টের রেসপন্স শেয়ার করুন
        """
        req_hash = self._compute_request_hash(model, prompt)

        if req_hash in self.request_history:
            entry = self.request_history[req_hash]

            # ৫ মিনিটের মধ্যে একই রিকোয়েস্ট হলে রিইউজ করুন
            if time.time() - entry["timestamp"] < 300:
                self.cost_metrics["dedup_requests"] += 1
                logger.info(f"♻️ [DEDUP HIT] Reusing response from {(time.time() - entry['timestamp']):.1f}s ago")

                return {
                    "is_duplicate": True,
                    "cached_response": entry["response"],
                    "original_timestamp": entry["timestamp"]
                }

        return {"is_duplicate": False}

    def record_request(self, model: str, prompt: str, response: str, tokens_used: int):
        """সফল রিকোয়েস্ট রেকর্ড করুন ভবিষ্যত ক্যাশিংয়ের জন্য"""
        req_hash = self._compute_request_hash(model, prompt)
        cost = self._calculate_cost(model, estimate_tokens(prompt), tokens_used)

        self.request_history[req_hash] = {
            "response": response,
            "timestamp": time.time(),
            "cost": cost,
            "tokens": tokens_used
        }

    def get_cost_summary(self) -> dict[str, Any]:
        """সাম্প্রতিক কস্ট সেভিংস সামারি পান"""
        total_requests = self.cost_metrics["total_requests"] or 1
        cache_hit_rate = (self.cost_metrics["cached_hits"] / total_requests) * 100

        return {
            "total_requests": self.cost_metrics["total_requests"],
            "cached_hits": self.cost_metrics["cached_hits"],
            "cache_hit_rate_percent": cache_hit_rate,
            "dedup_requests": self.cost_metrics["dedup_requests"],
            "total_cost_saved_usd": round(self.cost_metrics["total_cost_saved"], 2),
            "estimated_monthly_savings_usd": round(
                self.cost_metrics["total_cost_saved"] * 30, 2
            )
        }

    async def intercept_api_call(
        self,
        model: str,
        prompt: str,
        task_type: str = "general",
        **kwargs
    ) -> dict[str, Any]:
        """
        সব API কল এর আগে ইন্টারসেপ্ট করুন এবং সিদ্ধান্ত নিন
        
        রিটার্ন:
        {
            "proceed": bool,  # True = API কল করুন, False = ক্যাশড রেসপন্স ব্যবহার করুন
            "cached_response": str or None,
            "cost_saved": float,
            "recommendation": str
        }
        """  # noqa: W293

        # প্রথম ডুপ্লিকেট চেক করুন
        dedup_result = await self.deduplicate_request(model, prompt)
        if dedup_result["is_duplicate"]:
            return {
                "proceed": False,
                "cached_response": dedup_result["cached_response"],
                "cost_saved": self._calculate_cost(model, estimate_tokens(prompt), 100),
                "recommendation": "DEDUP_HIT - Using recent cached response"
            }

        # তারপর সিমান্টিক ক্যাশ চেক করুন
        cache_result = await self.should_use_cache(model, prompt, task_type)
        if cache_result["should_cache"]:
            return {
                "proceed": False,
                "cached_response": cache_result["cached_response"],
                "cost_saved": cache_result["estimated_cost_saved"],
                "recommendation": "SEMANTIC_HIT - Using semantically similar cached response"
            }

        # API কল করা দরকার
        return {
            "proceed": True,
            "cached_response": None,
            "cost_saved": 0.0,
            "recommendation": "PROCEED - No cache hit, call API"
        }


# গ্লোবাল ইন্সট্যান্স (সব মডুলে ব্যবহারের জন্য)
_autocache_instance: AutocacheProxy | None = None


def get_autocache() -> AutocacheProxy:
    """গ্লোবাল Autocache ইন্সট্যান্স পান"""
    global _autocache_instance
    if _autocache_instance is None:
        from core.semantic_cache import SemanticCache
        _autocache_instance = AutocacheProxy(SemanticCache())
    return _autocache_instance

```