import time
import asyncio
from typing import Dict, List, Tuple, Optional
from loguru import logger

# Health checker simulation - in production this would come from actual health checks
PROVIDER_HEALTH = {
    'groq': {'status': 'ok', 'latency_ms': 50},
    'google': {'status': 'ok', 'latency_ms': 250},
    'openrouter': {'status': 'degraded', 'latency_ms': 1200},
    'together': {'status': 'ok', 'latency_ms': 300},
    'nvidia': {'status': 'ok', 'latency_ms': 180},
    'huggingface': {'status': 'ok', 'latency_ms': 800},
}

class PerformanceAwareRouter:
    """
    Performance-Aware Router for SupremeAI 2.0
    Routes requests based on cost, latency, and provider health status
    """
    
    def __init__(self):
        self.providers = [
            {"name": "groq", "cost_per_1k": 0.0001, "quality": 9},
            {"name": "google", "cost_per_1k": 0.00025, "quality": 10},
            {"name": "together", "cost_per_1k": 0.0002, "quality": 8},
            {"name": "openrouter", "cost_per_1k": 0.001, "quality": 9},
            {"name": "nvidia", "cost_per_1k": 0.0005, "quality": 8},
            {"name": "huggingface", "cost_per_1k": 0.00005, "quality": 6},  # Free but slower
        ]
        
        # Weights for scoring algorithm (can be tuned)
        self.latency_weight = 0.5
        self.cost_weight = 0.3
        self.quality_weight = 0.2
    
    def _calculate_score(self, provider_info: dict) -> float:
        """
        Calculate composite score for a provider
        Lower score = better performance
        """
        health = PROVIDER_HEALTH.get(provider_info['name'], {'status': 'down', 'latency_ms': 9999})
        
        # If provider is down, return infinity to avoid selecting it
        if health['status'] != 'ok':
            return float('inf')
        
        # Normalize metrics to 0-1 scale (lower is better for latency and cost)
        # Higher is better for quality, so we invert it
        max_latency = 2000  # ms - assume anything slower is unusable
        max_cost = 0.01     # $0.01 per 1k tokens - reasonable upper bound
        min_quality = 1     # Assume quality scores 1-10
        max_quality = 10
        
        # Normalize latency (0 = best, 1 = worst)
        normalized_latency = min(health['latency_ms'] / max_latency, 1.0)
        
        # Normalize cost (0 = best, 1 = worst)
        normalized_cost = min(provider_info['cost_per_1k'] / max_cost, 1.0)
        
        # Normalize quality (0 = worst, 1 = best) then invert for scoring
        normalized_quality = (provider_info['quality'] - min_quality) / (max_quality - min_quality)
        normalized_quality_inverse = 1.0 - normalized_quality  # So higher quality = lower score
        
        # Calculate weighted score
        score = (
            (normalized_latency * self.latency_weight) +
            (normalized_cost * self.cost_weight) +
            (normalized_quality_inverse * self.quality_weight)
        )
        
        return score
    
    async def route(self, prompt: str, task_type: str = "general") -> Dict[str, Any]:
        """
        Select the best provider based on performance metrics
        
        Returns:
            Dict containing provider info and selection metadata
        """
        scored_providers = []
        
        # Score all providers
        for provider in self.providers:
            score = self._calculate_score(provider)
            scored_providers.append((provider, score))
        
        # Sort by score (ascending - lower is better)
        scored_providers.sort(key=lambda x: x[1])
        
        # Check if all providers are down
        if not scored_providers or scored_providers[0][1] == float('inf'):
            raise Exception("No healthy providers available")
        
        # Select the best provider
        best_provider, best_score = scored_providers[0]
        
        # Log the decision for monitoring/debugging
        logger.info(
            f"🚀 PERFORMANCE ROUTING: Selected '{best_provider['name']}' "
            f"(score: {best_score:.3f}, latency: {PROVIDER_HEALTH[best_provider['name']]['latency_ms']}ms, "
            f"cost: ${best_provider['cost_per_1k']:.5f}/1k, quality: {best_provider['quality']}/10)"
        )
        
        # Log alternatives for debugging
        if len(scored_providers) > 1:
            alternatives = [
                f"{p['name']}({s:.3f})" for p, s in scored_providers[1:4]  # Top 3 alternatives
            ]
            logger.debug(f"🔄 Alternatives: {', '.join(alternatives)}")
        
        return {
            "provider": best_provider['name'],
            "score": best_score,
            "latency_ms": PROVIDER_HEALTH[best_provider['name']]['latency_ms'],
            "cost_per_1k": best_provider['cost_per_1k'],
            "quality": best_provider['quality'],
            "alternatives_scored": len(scored_providers)
        }

# Global instance
performance_router = PerformanceAwareRouter()

# Pro Tip (বাংলা): শুধুমাত্র একটি মেট্রিকের উপর নির্ভর করবেন না।
# খরচ, গতি এবং আউটপুট কোয়ালিটির মধ্যে একটি ভারসাম্য তৈরি করুন।
# আপনার রাউটিং লজিকে A/B টেস্টিং অন্তর্ভুক্ত করুন, যাতে নতুন মডেলগুলোর পারফরম্যান্স
# স্বয়ংক্রিয়ভাবে মূল্যায়ন করা যায় এবং সেরাটি বেছে নেওয়া যায়।
# উদাহরণস্বরূপ, আপনি 90% ট্রাফিককে বর্তमान সর্বোৎপ পারফরমিং মডেলে পাঠাতে পারেন
# এবং 10% ট্রাফিকে নতুন মডেলে পাঠাকেই তার عملکرد বিবেচনা করতে পারেন।