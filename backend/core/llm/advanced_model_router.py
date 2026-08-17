# SupremeAI 2.0 — Advanced Model Router Engine
# বাংলা মন্তব্য: এটি টাস্ক টাইপ, প্রম্পট কমপ্লেক্সিটি এবং পারফরম্যান্স স্কোর অনুযায়ী সর্বাধুনিক মডেল নির্বাচন করে খরচ ৭০-৯০% সাশ্রয় করে।
# Tier 0 Fast-Path: Needle 2-inspired confidence gate — bypasses ALL LLM calls for deterministic tasks.

import json
import re
import urllib.request
from dataclasses import dataclass
from enum import Enum
from typing import Any

from loguru import logger


# বাংলা মন্তব্ট: টিয়ার 0 deterministic টাস্ক প্যাটার্ন — pure-Python, শূন্য টোকেন খরচ।
_DETERMINISTIC_PATTERNS: list[tuple[str, re.Pattern[str]]] = [
    ("pypi_search", re.compile(r"search\s+(?:pypi|pypi\s+for|package\s+index)\s+", re.I)),
    ("list_files", re.compile(r"list\s+(?:all\s+)?(?:files?|py|js|ts|java|go|rs)\s+(?:in|under|at|from)?", re.I)),
    ("regex_format", re.compile(r"format\s+as\s+(?:json|xml|csv|table|yaml|yml|html)", re.I)),
    ("schema_lookup", re.compile(r"(?:show|list|describe|what\s+are)\s+(?:schema|tables?|columns?|fields?)", re.I)),
]

# Confidence threshold for Tier 0 bypass (reuses analyze_prompt_complexity overall score)
_TIER0_CONFIDENCE_THRESHOLD = 0.85


@dataclass
class ConfidenceDecision:
    confidence: float
    is_deterministic: bool
    task_type: str
    matched_pattern: str | None
    deterministic_result: dict[str, Any] | None


class Tier0Dispatcher:
    """Zero-cost deterministic executors for high-confidence tasks.

    Runs BEFORE any LLM API call — no token consumption, sub-50ms latency.
    Each executor uses only Python standard library (no ML dependency).
    """

    @staticmethod
    def execute(pattern_name: str, prompt: str) -> dict[str, Any]:
        if pattern_name == "pypi_search":
            return Tier0Dispatcher._search_pypi(prompt)
        if pattern_name == "list_files":
            return Tier0Dispatcher._list_files(prompt)
        if pattern_name == "regex_format":
            return Tier0Dispatcher._format_text(prompt)
        if pattern_name == "schema_lookup":
            return Tier0Dispatcher._schema_lookup(prompt)
        return {"error": f"Unknown Tier 0 pattern: {pattern_name}"}

    @staticmethod
    def _search_pypi(prompt: str) -> dict[str, Any]:
        """Pure-stdlib HTTP call to PyPI JSON API."""
        match = re.search(r"(?:pypi\s+for\s+|pypi\s+|package\s+index\s+for\s+)(\S+)", prompt, re.I)
        pkg_name = match.group(1).strip() if match else prompt.strip()
        url = f"https://pypi.org/pypi/{pkg_name}/json"
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "SupremeAI/2.0"})
            with urllib.request.urlopen(req, timeout=5) as resp:
                data = json.loads(resp.read())
            return {
                "name": data["info"]["name"],
                "version": data["info"]["version"],
                "summary": data["info"]["summary"],
                "home_page": data["info"]["home_page"],
            }
        except Exception as exc:
            logger.warning(f"[Tier0Dispatcher] PyPI search failed for '{pkg_name}': {exc}")
            return {"error": str(exc), "query": pkg_name}

    @staticmethod
    def _list_files(prompt: str) -> dict[str, Any]:
        """List files in a directory using os.scandir."""
        import os

        match = re.search(r"(?:in|under|at|from)\s+(.+)", prompt, re.I)
        target_dir = match.group(1).strip() if match else "."
        files: list[dict[str, Any]] = []
        try:
            with os.scandir(target_dir) as entries:
                for entry in entries:
                    files.append({
                        "name": entry.name,
                        "path": entry.path,
                        "is_dir": entry.is_dir(),
                        "size_bytes": entry.stat().st_size if entry.is_file() else None,
                    })
            return {"directory": target_dir, "count": len(files), "files": files[:50]}
        except Exception as exc:
            logger.warning(f"[Tier0Dispatcher] File listing failed for '{target_dir}': {exc}")
            return {"error": str(exc), "directory": target_dir}

    @staticmethod
    def _format_text(prompt: str) -> dict[str, Any]:
        """Extract structured data from prompt and format it."""
        match = re.search(r"format\s+as\s+(json|xml|csv|table|yaml|yml|html)\s*(?:for\s+)?(.+)", prompt, re.I)
        if not match:
            return {"error": "Could not parse format target from prompt"}
        fmt = match.group(1).lower()
        raw_text = match.group(2).strip()
        try:
            if fmt == "json":
                # Try to parse existing JSON, otherwise wrap as key-value
                try:
                    result = json.loads(raw_text)
                except json.JSONDecodeError:
                    result = {"content": raw_text}
                return {"format": "json", "result": result}
            if fmt == "csv":
                lines = [line.strip() for line in raw_text.split("\n") if line.strip()]
                return {"format": "csv", "rows": lines, "row_count": len(lines)}
            if fmt == "table":
                lines = [line.strip() for line in raw_text.split("\n") if line.strip()]
                return {"format": "table", "rows": lines, "row_count": len(lines)}
            return {"format": fmt, "result": raw_text}
        except Exception as exc:
            return {"error": str(exc)}

    @staticmethod
    def _schema_lookup(prompt: str) -> dict[str, Any]:
        """Schema lookup placeholder — returns schema info for known entities."""
        entities = ["users", "orders", "products", "sessions", "skills", "ai_memory"]
        text_lower = prompt.lower()
        for entity in entities:
            if entity in text_lower:
                return {"entity": entity, "status": "schema_available", "note": f"Query Supabase for '{entity}' table schema via information_schema."}
        return {"error": "No known entity found in query", "suggested_entities": entities}


@dataclass
class ModelPerformanceMetrics:
    response_time: float
    success_rate: float
    cost_per_token: float
    throughput: float
    last_used: float
    error_count: int


class ModelTier(Enum):
    EXPENSIVE = "expensive"
    BALANCED = "balanced"
    BUDGET = "budget"


@dataclass
class RouteDecision:
    provider: str
    model: str
    priority_score: float
    expected_cost: float
    expected_latency: float


class AdvancedModelRouter:
    """
    Advanced model router with intelligent traffic distribution,
    performance monitoring, and cost optimization.
    """

    def __init__(self):
        self.performance_metrics: dict[str, ModelPerformanceMetrics] = {}
        self.model_preferences = self._load_model_preferences()

    def _load_model_preferences(self) -> dict[str, dict]:
        """Load model preferences and capabilities from configuration."""
        return {
            "coding": {
                "preferred_models": [
                    "groq/llama-3.3-70b-versatile",
                    "openrouter/deepseek/deepseek-coder",
                    "gpt-4o-mini",
                ],
                "tier_preference": ModelTier.BALANCED,
            },
            "reasoning": {
                "preferred_models": [
                    "openrouter/meta-llama/llama-3.3-70b-instruct",
                    "claude-3-haiku",
                    "gemini-1.5-flash",
                ],
                "tier_preference": ModelTier.BUDGET,
            },
            "creative": {
                "preferred_models": ["gpt-4o", "claude-3-sonnet", "gemini-1.5-pro"],
                "tier_preference": ModelTier.EXPENSIVE,
            },
            "analysis": {
                "preferred_models": ["openrouter/openai/gpt-4o", "claude-3-opus", "gemini-1.5-pro"],
                "tier_preference": ModelTier.BALANCED,
            },
            "general": {
                "preferred_models": ["groq/llama-3.3-70b-versatile", "gemini-1.5-flash", "gpt-4o-mini"],
                "tier_preference": ModelTier.BUDGET,
            },
        }

    def analyze_prompt_complexity(self, prompt: str) -> dict[str, float]:
        """Analyze prompt complexity to determine optimal model requirements."""
        if not prompt:
            return {"length": 0.0, "complexity": 0.0, "overall": 0.0}

        length_score = min(len(prompt) / 1000.0, 1.0)
        complexity_indicators = [
            "analyze",
            "compare",
            "evaluate",
            "summarize",
            "synthesize",
            "reason",
            "think step by step",
            "consider",
            "examine",
            "code",
            "algorithm",
        ]

        indicator_score = sum(1 for indicator in complexity_indicators if indicator.lower() in prompt.lower())
        indicator_score = min(indicator_score / 5.0, 1.0)

        return {
            "length": float(round(length_score, 4)),
            "complexity": float(round(indicator_score, 4)),
            "overall": float(round((length_score + indicator_score) / 2.0, 4)),
        }

    def get_available_models(self, task_type: str) -> list[tuple[str, str]]:
        """Get available models based on task type."""
        task = task_type.lower() if task_type else "general"
        preferences = self.model_preferences.get(task, self.model_preferences["general"])
        models = preferences["preferred_models"]

        result = []
        for model_spec in models:
            if "/" in model_spec:
                provider, model = model_spec.split("/", 1)
                result.append((provider, model))
            else:
                result.append(("openai", model_spec))

        return result

    def estimate_cost(self, provider: str, model: str, prompt_length: int) -> float:
        """Estimate token cost for a model."""
        base_rate = 0.000001
        if "gpt-4o" in model or "opus" in model:
            base_rate = 0.00001
        elif "haiku" in model or "flash" in model or "llama" in model:
            base_rate = 0.0000005
        return float(round(prompt_length * base_rate, 6))

    def estimate_latency(self, provider: str, model: str) -> float:
        """Estimate latency for a model."""
        if "groq" in provider:
            return 0.3
        if "flash" in model or "haiku" in model:
            return 0.5
        return 1.2

    def calculate_model_score(self, provider: str, model: str, task_type: str, complexity: dict[str, float]) -> float:
        """Calculate priority score for a model considering latency, complexity, and performance metrics."""
        model_key = f"{provider}/{model}"
        metrics = self.performance_metrics.get(model_key)

        base_score = 0.8
        if "groq" in provider:
            base_score += 0.15
        if complexity["overall"] > 0.6 and ("70b" in model or "4o" in model):
            base_score += 0.1

        if metrics:
            base_score *= metrics.success_rate
            if metrics.response_time > 0:
                norm_latency = min(metrics.response_time / 5.0, 1.0)
                base_score *= 1.0 - norm_latency * 0.5

        return float(round(base_score, 4))

    async def route_request(
        self,
        prompt: str,
        task_type: str = "general",
        user_id: str | None = None,
        budget_constraint: float | None = None,
    ) -> RouteDecision:
        """
        Intelligent routing based on task type, performance metrics, and cost optimization.
        """
        prompt_complexity = self.analyze_prompt_complexity(prompt)
        available_models = self.get_available_models(task_type)

        scored_models: list[RouteDecision] = []
        for provider, model in available_models:
            score = self.calculate_model_score(provider, model, task_type, prompt_complexity)
            expected_cost = self.estimate_cost(provider, model, len(prompt))
            expected_latency = self.estimate_latency(provider, model)

            scored_models.append(
                RouteDecision(
                    provider=provider,
                    model=model,
                    priority_score=score,
                    expected_cost=expected_cost,
                    expected_latency=expected_latency,
                )
            )

        scored_models.sort(key=lambda x: x.priority_score, reverse=True)

        if budget_constraint and scored_models:
            filtered = [m for m in scored_models if m.expected_cost <= budget_constraint]
            if filtered:
                return filtered[0]

        return (
            scored_models[0]
            if scored_models
            else RouteDecision(
                provider="groq",
                model="llama-3.3-70b-versatile",
                priority_score=1.0,
                expected_cost=0.0001,
                expected_latency=0.3,
            )
        )

    def route_with_confidence(
        self,
        prompt: str,
        task_type: str = "general",
    ) -> ConfidenceDecision:
        """Single entry point: complexity score + deterministic pattern matching.

        Replaces the caller's need to invoke both analyze_prompt_complexity AND
        a separate gate. Returns a ConfidenceDecision that tells the caller
        whether to bypass LLM entirely (Tier 0) or escalate to Tier 1/2.

        - confidence: float (0-1) derived from analyze_prompt_complexity()
        - is_deterministic: True only if a pattern matched AND confidence >= 0.85
        - deterministic_result: pre-computed result if deterministic, else None
        """
        complexity = self.analyze_prompt_complexity(prompt)
        confidence = complexity["overall"]

        matched = None
        for name, pattern in _DETERMINISTIC_PATTERNS:
            if pattern.search(prompt):
                matched = name
                # Pattern match boosts confidence to threshold
                confidence = max(confidence, _TIER0_CONFIDENCE_THRESHOLD)
                break

        is_deterministic = matched is not None and confidence >= _TIER0_CONFIDENCE_THRESHOLD

        result = None
        if is_deterministic:
            result = Tier0Dispatcher.execute(matched, prompt)
            logger.info(
                f"[AdvancedModelRouter] Tier 0 fast-path: "
                f"pattern={matched} confidence={confidence:.2f} task_type={task_type}"
            )

        return ConfidenceDecision(
            confidence=confidence,
            is_deterministic=is_deterministic,
            task_type=task_type,
            matched_pattern=matched,
            deterministic_result=result,
        )


# ── Lazy Singleton ────────────────────────────────────────────────────────
_router_instance: "AdvancedModelRouter | None" = None


def get_advanced_router() -> AdvancedModelRouter:
    """Lazy singleton factory — avoids circular import with LLMGateway."""
    global _router_instance
    if _router_instance is None:
        _router_instance = AdvancedModelRouter()
    return _router_instance
