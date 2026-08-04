"""
SupremeAI 2.0 Enhanced LLM Router
=============================
Multi-provider AI gateway with intelligent routing, fallback chains,
cost optimization, and Bengali language optimization.
"""

from __future__ import annotations

import json
import time
from dataclasses import dataclass
from enum import Enum, StrEnum

# Internal core imports
from core.cache import get_redis_client
from core.error_bus import with_error_bus
from core.llm.llm_gateway import get_llm_gateway
from core.logging import get_logger
from core.resilience.circuit_breaker_manager import get_shared_circuit_breaker


class Provider(StrEnum):
    """Supported AI model providers."""

    MOONSHOT = "moonshot"
    DEEPSEEK = "deepseek"
    TOGETHER = "together"
    OLLAMA = "ollama"
    GEMINI = "gemini"
    HUGGINGFACE_SPACE = "hf_space"
    OPENAI = "openai"
    BHASHA = "bhasha"  # Supreme-Bhasha for Bengali


class TaskCategory(Enum):
    """Categories for different types of tasks."""

    CHAT = "chat"
    CODE = "code"
    BENGALI = "bengali"
    ANALYSIS = "analysis"
    REASONING = "reasoning"
    CREATIVE = "creative"
    TRANSLATION = "translation"


@dataclass
class ModelPerformanceStats:
    """Statistics for tracking model performance."""

    success_rate: float = 0.5
    avg_response_time: float = 1.0
    accuracy_score: float = 0.5
    usage_count: int = 0
    error_count: int = 0
    last_updated: str = ""


@dataclass
class RouteResult:
    """Result of a routing decision."""

    provider: Provider
    content: str
    tokens_used: int
    cost_usd: float
    latency_ms: float
    cached: bool = False
    fallback_used: bool = False


class EnhancedLLMRouter:
    """Enhanced LLM router with intelligent routing based on command classification."""

    def __init__(self):
        self.redis_client = get_redis_client()
        self.performance_tracker: dict[Provider, ModelPerformanceStats] = {}
        self.task_provider_map: dict[TaskCategory, list[Provider]] = {
            TaskCategory.BENGALI: [Provider.BHASHA, Provider.MOONSHOT, Provider.GEMINI],
            TaskCategory.CODE: [Provider.DEEPSEEK, Provider.TOGETHER, Provider.GEMINI],
            TaskCategory.ANALYSIS: [Provider.MOONSHOT, Provider.TOGETHER, Provider.GEMINI],
            TaskCategory.CHAT: [Provider.MOONSHOT, Provider.GEMINI, Provider.DEEPSEEK],
            TaskCategory.REASONING: [Provider.MOONSHOT, Provider.TOGETHER, Provider.GEMINI],
            TaskCategory.CREATIVE: [Provider.GEMINI, Provider.MOONSHOT, Provider.TOGETHER],
            TaskCategory.TRANSLATION: [Provider.GEMINI, Provider.MOONSHOT, Provider.DEEPSEEK],
        }
        self.logger = get_logger(__name__)

    async def classify_command(self, command: str) -> TaskCategory:
        """Classify command using NLP techniques."""
        command_lower = command.lower()

        # Bengali language detection
        if any(ord(char) > 255 for char in command[:100]):  # Check for non-ASCII characters
            bangla_chars = [char for char in command if "\u0980" <= char <= "\u09ff"]
            if len(bangla_chars) > len(command) * 0.1:  # More than 10% Bangla chars
                return TaskCategory.BENGALI

        # Keyword-based classification
        if any(
            keyword in command_lower
            for keyword in ["code", "programming", "function", "debug", "algorithm", "implementation"]
        ):
            return TaskCategory.CODE
        elif any(
            keyword in command_lower for keyword in ["analyze", "analysis", "report", "trend", "pattern", "insight"]
        ):
            return TaskCategory.ANALYSIS
        elif any(
            keyword in command_lower for keyword in ["reason", "think", "logic", "problem", "solution", "explain"]
        ):
            return TaskCategory.REASONING
        elif any(keyword in command_lower for keyword in ["write", "create", "generate", "story", "poem", "idea"]):
            return TaskCategory.CREATIVE
        elif any(keyword in command_lower for keyword in ["translate", "convert", "language", "english", "bengali"]):
            return TaskCategory.TRANSLATION
        else:
            return TaskCategory.CHAT

    async def select_optimal_model(self, command: str, context: dict | None = None) -> Provider:
        """Select the optimal model based on command classification and context."""
        task_category = await self.classify_command(command)

        # Get available providers for this task category
        available_providers = self.task_provider_map.get(task_category, [Provider.GEMINI])

        # Filter out providers that are currently unavailable (based on circuit breakers)
        filtered_providers = []
        for provider in available_providers:
            cb = get_shared_circuit_breaker(f"llm_{provider.value}")
            if not cb.is_open():
                filtered_providers.append(provider)

        if not filtered_providers:
            # If all primary providers are down, use any available provider
            filtered_providers = available_providers

        # Select based on performance metrics if available
        best_provider = filtered_providers[0]  # Default to first in list
        best_score = 0.0

        for provider in filtered_providers:
            stats = self.performance_tracker.get(provider, ModelPerformanceStats())
            # Calculate a composite score based on success rate and efficiency
            score = (
                (stats.success_rate * 0.4) + (1 / (stats.avg_response_time + 0.1) * 0.3) + (stats.accuracy_score * 0.3)
            )

            if score > best_score:
                best_score = score
                best_provider = provider

        return best_provider

    @with_error_bus("route_request")
    async def route_request(self, command: str, context: dict | None = None) -> RouteResult:
        """Route the request to the optimal provider based on command and context."""
        start_time = time.time()

        # Select optimal provider
        provider = await self.select_optimal_model(command, context)

        # Get the LLM gateway and make the request
        gateway = get_llm_gateway()

        try:
            response = await gateway.agenerate(prompt=command, provider=provider.value, **context or {})

            end_time = time.time()
            latency_ms = (end_time - start_time) * 1000

            # Update performance statistics
            await self._update_performance_stats(provider, True, latency_ms)

            return RouteResult(
                provider=provider,
                content=response.get("content", ""),
                tokens_used=response.get("tokens_used", 0),
                cost_usd=response.get("cost_usd", 0.0),
                latency_ms=latency_ms,
            )

        except Exception as e:
            end_time = time.time()
            latency_ms = (end_time - start_time) * 1000

            # Update performance statistics for failure
            await self._update_performance_stats(provider, False, latency_ms)

            # Try fallback if primary provider failed
            task_category = await self.classify_command(command)
            available_providers = self.task_provider_map.get(task_category, [Provider.GEMINI])

            # Try next available provider
            primary_idx = available_providers.index(provider) if provider in available_providers else -1

            for i in range(primary_idx + 1, len(available_providers)):
                fallback_provider = available_providers[i]
                cb = get_shared_circuit_breaker(f"llm_{fallback_provider.value}")

                if not cb.is_open():
                    try:
                        fallback_response = await gateway.agenerate(
                            prompt=command, provider=fallback_provider.value, **context or {}
                        )

                        fallback_latency = (time.time() - start_time) * 1000

                        # Update performance stats for successful fallback
                        await self._update_performance_stats(fallback_provider, True, fallback_latency)

                        return RouteResult(
                            provider=fallback_provider,
                            content=fallback_response.get("content", ""),
                            tokens_used=fallback_response.get("tokens_used", 0),
                            cost_usd=fallback_response.get("cost_usd", 0.0),
                            latency_ms=fallback_latency,
                            fallback_used=True,
                        )
                    except Exception:
                        await self._update_performance_stats(
                            fallback_provider, False, (time.time() - start_time) * 1000
                        )
                        continue

            # If all providers failed, raise the original exception
            raise e

    async def _update_performance_stats(self, provider: Provider, success: bool, latency_ms: float):
        """Update performance statistics for a provider."""
        if provider not in self.performance_tracker:
            self.performance_tracker[provider] = ModelPerformanceStats()

        stats = self.performance_tracker[provider]
        stats.usage_count += 1

        if not success:
            stats.error_count += 1

        # Update moving average for response time
        total_time = (stats.avg_response_time * (stats.usage_count - 1) + latency_ms) / stats.usage_count
        stats.avg_response_time = total_time

        # Update success rate
        stats.success_rate = (stats.usage_count - stats.error_count) / stats.usage_count

        # Store in Redis for persistence across restarts
        stats_key = f"llm_stats:{provider.value}"
        self.redis_client.setex(
            stats_key,
            86400 * 7,  # 7 days expiry
            json.dumps(
                {
                    "success_rate": stats.success_rate,
                    "avg_response_time": stats.avg_response_time,
                    "accuracy_score": stats.accuracy_score,
                    "usage_count": stats.usage_count,
                    "error_count": stats.error_count,
                    "last_updated": time.time(),
                }
            ),
        )
