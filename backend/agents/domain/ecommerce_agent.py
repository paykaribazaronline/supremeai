"""
SupremeAI — E-commerce Agent
=============================
Product recommendations and customer service automation.
Provides product analysis, review summarization, and shopping assistance.
"""

from __future__ import annotations

import hashlib
import logging
from dataclasses import dataclass
from datetime import UTC, datetime
from typing import Any

from core.cache import get_cache
from core.error_bus import with_error_bus
from core.llm_router import LLMRouter

logger = logging.getLogger("supremeai.ecommerce")

ECOMMERCE_CACHE_TTL = 1800  # 30 minutes


@dataclass(frozen=True)
class Product:
    """Immutable product record."""

    id: str
    name: str
    category: str
    price: float
    rating: float
    description: str
    tags: list[str]
    in_stock: bool


@dataclass(frozen=True)
class Recommendation:
    """Immutable product recommendation."""

    product: Product
    relevance_score: float
    reason: str


@dataclass(frozen=True)
class ReviewSummary:
    """Immutable review summary."""

    product_id: str
    average_rating: float
    total_reviews: int
    pros: list[str]
    cons: list[str]
    sentiment: str


class EcommerceAgent:
    """
    Product recommendations and customer service automation.
    """

    def __init__(self, llm_router: LLMRouter | None = None) -> None:
        self.llm = llm_router or LLMRouter()
        self.cache = get_cache()
        self._products: dict[str, Product] = {}

    def _cache_key(self, prefix: str, identifier: str) -> str:
        raw = f"ecommerce:{prefix}:{identifier}:{datetime.now(UTC).strftime('%Y%m%d%H')}"
        return f"ecommerce:{hashlib.sha256(raw.encode()).hexdigest()[:16]}"

    def add_product(self, product: Product) -> None:
        """Add a product to the catalog."""
        self._products[product.id] = product

    def get_product(self, product_id: str) -> Product | None:
        """Get product by ID."""
        return self._products.get(product_id)

    async def recommend_products(
        self,
        user_preferences: list[str],
        budget_max: float = 999999,
        category: str | None = None,
        top_k: int = 5,
    ) -> list[Recommendation]:
        """Recommend products based on user preferences."""
        candidates = list(self._products.values())

        # Filter by category
        if category:
            candidates = [p for p in candidates if p.category.lower() == category.lower()]

        # Filter by budget
        candidates = [p for p in candidates if p.price <= budget_max]

        # Score by preference match
        scored = []
        for product in candidates:
            score = 0.0
            product_tags = set(t.lower() for t in product.tags)
            pref_set = set(p.lower() for p in user_preferences)

            tag_matches = product_tags & pref_set
            score += len(tag_matches) * 0.3

            # Rating boost
            score += (product.rating / 5.0) * 0.2

            # Category match boost
            if category and product.category.lower() == category.lower():
                score += 0.2

            scored.append((score, product))

        scored.sort(key=lambda x: x[0], reverse=True)
        scored = scored[:top_k]

        recommendations = []
        for score, product in scored:
            matched_tags = ", ".join(t for t in product.tags if t.lower() in set(p.lower() for p in user_preferences))
            reason = (
                f"Matches your interest in {matched_tags}" if matched_tags else f"Top-rated {product.category} product"
            )

            recommendations.append(
                Recommendation(
                    product=product,
                    relevance_score=round(score, 2),
                    reason=reason,
                )
            )

        return recommendations

    @with_error_bus("summarize_reviews")
    async def summarize_reviews(self, reviews: list[dict[str, Any]]) -> ReviewSummary:
        """Summarize product reviews."""
        if not reviews:
            return ReviewSummary(
                product_id="",
                average_rating=0.0,
                total_reviews=0,
                pros=[],
                cons=[],
                sentiment="neutral",
            )

        avg_rating = sum(r.get("rating", 0) for r in reviews) / len(reviews)
        sentiments = [r.get("sentiment", "neutral") for r in reviews]
        dominant_sentiment = max(set(sentiments), key=sentiments.count) if sentiments else "neutral"

        prompt = (
            f"Summarize these {len(reviews)} product reviews into pros and cons:\n\n"
            + "\n".join(f"- {r.get('text', '')}" for r in reviews[:20])
            + "\n\nRespond as JSON with: pros (list), cons (list)"
        )

        try:
            result = await self.llm.route(prompt=prompt, task_type="reasoning", max_tokens=500)
            import json

            content = result.get("content", "{}")
            data = json.loads(content) if isinstance(content, str) else content
            pros = data.get("pros", [])
            cons = data.get("cons", [])
        except Exception:
            pros = []
            cons = []

        return ReviewSummary(
            product_id=reviews[0].get("product_id", ""),
            average_rating=round(avg_rating, 1),
            total_reviews=len(reviews),
            pros=pros,
            cons=cons,
            sentiment=dominant_sentiment,
        )


# Singleton
_ecommerce_instance: EcommerceAgent | None = None


def get_ecommerce_agent() -> EcommerceAgent:
    """Get or create the singleton EcommerceAgent."""
    global _ecommerce_instance
    if _ecommerce_instance is None:
        _ecommerce_instance = EcommerceAgent()
    return _ecommerce_instance
