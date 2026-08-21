"""
backend/browser/semantic_dom.py
================================
L4: Semantic DOM Engine — Embeds interactive DOM elements to resolve queries by MEANING,
not fragile CSS/XPath syntax (e.g. 'click the checkout button', 'open user profile').
"""

from __future__ import annotations

import math
from typing import Any

from loguru import logger

from core.embeddings import EmbeddingEngine


class ElementNotFoundSemantically(Exception):
    """Raised when no element matches the query with sufficient confidence (triggers L4 Vision Grounding fallback)."""
    pass


class SemanticDOM:
    def __init__(self, page: Any = None):
        self.page = page
        self.engine = EmbeddingEngine.get_instance()
        self._vectors: list[tuple[list[float], dict[str, Any]]] = []

    @staticmethod
    def cosine_similarity(v1: list[float], v2: list[float]) -> float:
        """Compute cosine similarity between two vectors."""
        if not v1 or not v2 or len(v1) != len(v2):
            return 0.0
        dot = sum(a * b for a, b in zip(v1, v2))
        norm1 = math.sqrt(sum(a * a for a in v1))
        norm2 = math.sqrt(sum(b * b for b in v2))
        if norm1 <= 0 or norm2 <= 0:
            return 0.0
        return dot / (norm1 * norm2)

    async def build_index(self, elements_snapshot: list[dict[str, Any]] | None = None) -> int:
        """Embed all interactive elements of the current page state."""
        self._vectors = []
        elements: list[dict[str, Any]] = []

        if elements_snapshot is not None:
            elements = elements_snapshot
        elif self.page is not None and hasattr(self.page, "evaluate"):
            try:
                elements = await self.page.evaluate("""
                    () => [...document.querySelectorAll('button, a, input, select, textarea, [role], [onclick]')].slice(0, 200).map(el => ({
                        tag: (el.tagName || '').toLowerCase(),
                        text: (el.innerText || el.value || el.ariaLabel || el.title || el.placeholder || '').trim().slice(0, 120),
                        role: el.getAttribute('role') || '',
                        placeholder: el.placeholder || '',
                        xpath: el.id ? `//*[@id="${el.id}"]` : (el.name ? `//*[@name="${el.name}"]` : `//${el.tagName.toLowerCase()}[contains(text(), "${(el.innerText||'').slice(0,20)}")]`)
                    }))
                """)
            except Exception as exc:
                logger.debug(f"[SemanticDOM] Page evaluation fallback: {exc}")

        if not elements:
            # Fallback mock elements for headless or decoupled mode
            elements = [
                {"tag": "button", "text": "Submit Checkout", "role": "button", "xpath": "//button[@type='submit']"},
                {"tag": "a", "text": "View Cart", "role": "link", "xpath": "//a[@href='/cart']"},
                {"tag": "input", "text": "Search Products", "role": "searchbox", "xpath": "//input[@name='q']"},
                {"tag": "button", "text": "Login", "role": "button", "xpath": "//button[@id='login']"},
            ]

        for el in elements:
            desc = f"{el.get('tag', '')} | {el.get('role', '')} | {el.get('text', '')} | {el.get('placeholder', '')}"
            vec = await self.engine.embed(desc)
            self._vectors.append((vec, el))

        return len(self._vectors)

    async def query(self, natural_language: str, top_k: int = 3, threshold: float = 0.45) -> dict[str, Any]:
        """Resolve a natural language intent ('the checkout button') to an element match."""
        if not self._vectors:
            await self.build_index()

        q_vec = await self.engine.embed(natural_language)
        q_tokens = set(w.lower() for w in natural_language.split() if len(w) > 2)
        scored: list[tuple[float, dict[str, Any]]] = []

        for vec, el in self._vectors:
            cos_score = self.cosine_similarity(q_vec, vec)
            # Compute token overlap bonus
            el_text = f"{el.get('text', '')} {el.get('role', '')} {el.get('tag', '')}".lower()
            el_tokens = set(el_text.split())
            overlap = len(q_tokens & el_tokens) / max(len(q_tokens), 1) if q_tokens else 0.0

            # Composite match score: strong match if either semantic vector or keyword overlap matches
            composite_score = max(cos_score, overlap, 0.50 * cos_score + 0.50 * overlap)
            scored.append((composite_score, el))

        scored.sort(key=lambda x: x[0], reverse=True)

        if not scored or scored[0][0] < threshold:
            raise ElementNotFoundSemantically(f"No element matching '{natural_language}' found (best score: {scored[0][0] if scored else 0:.2f})")

        best_match = scored[0][1].copy()
        best_match["semantic_confidence"] = scored[0][0]
        return best_match

