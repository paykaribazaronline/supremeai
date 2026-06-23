#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# ============================================================================
# file >> token_budget.py
# project >> SupremeAI 2.0
# purpose >> Token budget
# module >> core
# ============================================================================
Token budget management for free-tier AI providers.
Estimates, compresses, and enforces token limits to avoid
wasting free-tier quota on over-long prompts.

Key features:
- Character-based token estimation (no tiktoken dependency needed)
- Prompt truncation with smart sentence-boundary detection
- Per-provider token budget awareness
- Usage statistics
"""
from __future__ import annotations

import re
import time
from dataclasses import dataclass, field
from typing import Any, Dict, Optional, Tuple

from loguru import logger


# ---------------------------------------------------------------------------
# Per-provider max token budgets (conservative — leaves headroom for system prompts)
# ---------------------------------------------------------------------------
PROVIDER_TOKEN_BUDGETS: Dict[str, Dict[str, int]] = {
    "gemini": {
        "max_input_tokens": 6_000,    # context window is much larger but TPM is the constraint
        "max_output_tokens": 2_000,
    },
    "groq": {
        "max_input_tokens": 4_000,    # 30k TPM shared; keep inputs short
        "max_output_tokens": 1_500,
    },
    "openrouter": {
        "max_input_tokens": 4_000,
        "max_output_tokens": 1_500,
    },
    "cloudflare": {
        "max_input_tokens": 3_000,    # Cloudflare Workers AI has smaller context
        "max_output_tokens": 1_000,
    },
    "nvidia": {
        "max_input_tokens": 4_000,
        "max_output_tokens": 1_500,
    },
    "huggingface": {
        "max_input_tokens": 1_500,    # HF inference API has small context windows
        "max_output_tokens": 500,
    },
    "ollama": {
        "max_input_tokens": 8_000,    # local — can handle larger context
        "max_output_tokens": 4_000,
    },
    "deepseek": {
        "max_input_tokens": 8_000,
        "max_output_tokens": 4_000,
    },
    "default": {
        "max_input_tokens": 4_000,
        "max_output_tokens": 1_500,
    },
}

# Rough chars-per-token ratio (average English text ≈ 4 chars/token)
_CHARS_PER_TOKEN: float = 4.0


def estimate_tokens(text: str) -> int:
    """
    Fast character-based token count estimate.
    Accurate enough for budget enforcement without requiring tiktoken.

    English text: ~4 chars/token
    Code: ~3.5 chars/token (more tokens per char due to punctuation)
    CJK: ~2 chars/token
    """
    if not text:
        return 0

    # Heuristic: detect code blocks → lower chars/token ratio
    if "```" in text or "def " in text or "class " in text:
        ratio = 3.5
    # CJK unicode range detection
    elif any("\u4e00" <= c <= "\u9fff" for c in text[:100]):
        ratio = 2.0
    else:
        ratio = _CHARS_PER_TOKEN

    return max(1, int(len(text) / ratio))


def truncate_to_token_limit(text: str, max_tokens: int, from_end: bool = False) -> str:
    """
    Truncate *text* so that estimate_tokens(result) <= max_tokens.
    Tries to cut at sentence boundaries to preserve coherence.
    Falls back to hard character truncation when no boundary is found.
    If from_end=True, keeps the END of the text (useful for recent context).
    """
    if estimate_tokens(text) <= max_tokens:
        return text

    # Target character count
    target_chars = int(max_tokens * _CHARS_PER_TOKEN)

    if from_end:
        truncated = text[-target_chars:]
        # Trim to first sentence boundary
        match = re.search(r"[.!?\n]", truncated)
        if match:
            truncated = truncated[match.start() + 1:]
        return truncated.strip()
    else:
        truncated = text[:target_chars]
        # Trim to last sentence boundary; fall back to hard cut if none found
        match = re.search(r"[.!?\n](?=[^.!?\n]*$)", truncated)
        if match:
            truncated = truncated[:match.start() + 1]
        return truncated.strip()


@dataclass
class TokenBudgetStats:
    """Accumulates per-provider token usage stats."""
    provider: str
    total_input_tokens: int = 0
    total_output_tokens: int = 0
    total_calls: int = 0
    truncated_calls: int = 0
    tokens_saved_by_truncation: int = 0
    started_at: float = field(default_factory=time.time)

    def record_call(
        self,
        input_tokens: int,
        output_tokens: int,
        was_truncated: bool = False,
        tokens_saved: int = 0,
    ) -> None:
        self.total_input_tokens += input_tokens
        self.total_output_tokens += output_tokens
        self.total_calls += 1
        if was_truncated:
            self.truncated_calls += 1
            self.tokens_saved_by_truncation += tokens_saved

    def to_dict(self) -> Dict[str, Any]:
        return {
            "provider": self.provider,
            "total_calls": self.total_calls,
            "total_input_tokens": self.total_input_tokens,
            "total_output_tokens": self.total_output_tokens,
            "avg_input_tokens": (
                self.total_input_tokens // self.total_calls if self.total_calls else 0
            ),
            "truncated_calls": self.truncated_calls,
            "tokens_saved_by_truncation": self.tokens_saved_by_truncation,
            "tracking_since": self.started_at,
        }


class TokenBudgetManager:
    """
    Manages per-provider token budgets and prompt compression.

    Usage::

        budget = TokenBudgetManager()

        # Compress a prompt before sending
        compressed, meta = budget.prepare_prompt("very long prompt...", provider="groq")

        # After getting a response, record usage
        budget.record_usage(
            provider="groq",
            input_tokens=meta["estimated_input_tokens"],
            output_tokens=budget.estimate("response text"),
        )

        # Get stats
        stats = budget.get_stats()
    """

    def __init__(
        self,
        custom_budgets: Optional[Dict[str, Dict[str, int]]] = None,
    ) -> None:
        self._budgets = {**PROVIDER_TOKEN_BUDGETS, **(custom_budgets or {})}
        self._stats: Dict[str, TokenBudgetStats] = {}

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def estimate(self, text: str) -> int:
        """Estimate token count for *text*."""
        return estimate_tokens(text)

    def prepare_prompt(
        self,
        prompt: str,
        provider: str = "default",
        system_prompt: Optional[str] = None,
        reserve_output_tokens: bool = True,
    ) -> Tuple[str, Dict[str, Any]]:
        """
        Validate and compress *prompt* to fit within *provider*'s input budget.

        Returns (processed_prompt, metadata_dict).
        Metadata includes estimated token counts and whether truncation happened.
        """
        limits = self._budgets.get(provider, self._budgets["default"])
        max_input = limits["max_input_tokens"]

        # Reduce input budget if we need to reserve space for output
        if reserve_output_tokens:
            max_input = max(500, max_input - limits.get("max_output_tokens", 0) // 2)

        original_tokens = estimate_tokens(prompt)
        system_tokens = estimate_tokens(system_prompt) if system_prompt else 0
        available_for_user = max_input - system_tokens

        truncated = False
        tokens_saved = 0

        if original_tokens > available_for_user:
            truncated_prompt = truncate_to_token_limit(prompt, available_for_user)
            tokens_saved = original_tokens - estimate_tokens(truncated_prompt)
            logger.info(
                f"[TokenBudget] Truncated prompt for {provider}: "
                f"{original_tokens} → {estimate_tokens(truncated_prompt)} tokens "
                f"(saved {tokens_saved})"
            )
            prompt = truncated_prompt
            truncated = True

        final_tokens = estimate_tokens(prompt)

        meta: Dict[str, Any] = {
            "provider": provider,
            "estimated_input_tokens": final_tokens,
            "original_tokens": original_tokens,
            "truncated": truncated,
            "tokens_saved": tokens_saved,
            "max_output_tokens": limits["max_output_tokens"],
        }

        # Record stats
        self._get_stats(provider).record_call(
            input_tokens=final_tokens,
            output_tokens=0,  # will be updated after response
            was_truncated=truncated,
            tokens_saved=tokens_saved,
        )

        return prompt, meta

    def record_usage(
        self,
        provider: str,
        input_tokens: int,
        output_tokens: int,
    ) -> None:
        """Record actual token usage after a completed API call."""
        self._get_stats(provider).total_output_tokens += output_tokens
        logger.debug(
            f"[TokenBudget] {provider} usage: "
            f"in={input_tokens} out={output_tokens} "
            f"total_in={self._get_stats(provider).total_input_tokens}"
        )

    def fits_in_budget(self, prompt: str, provider: str = "default") -> bool:
        """Return True if *prompt* fits within provider's input token budget."""
        limits = self._budgets.get(provider, self._budgets["default"])
        return estimate_tokens(prompt) <= limits["max_input_tokens"]

    def get_stats(self) -> Dict[str, Any]:
        """Return token usage stats for all providers."""
        return {
            provider: stats.to_dict()
            for provider, stats in self._stats.items()
        }

    def get_provider_budget(self, provider: str) -> Dict[str, int]:
        """Return token limits for a specific provider."""
        return self._budgets.get(provider, self._budgets["default"])

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _get_stats(self, provider: str) -> TokenBudgetStats:
        if provider not in self._stats:
            self._stats[provider] = TokenBudgetStats(provider=provider)
        return self._stats[provider]


# ---------------------------------------------------------------------------
# Module-level singleton
# ---------------------------------------------------------------------------
_manager: Optional[TokenBudgetManager] = None


def get_budget_manager(
    custom_budgets: Optional[Dict[str, Dict[str, int]]] = None,
) -> TokenBudgetManager:
    """Return the module-level singleton TokenBudgetManager."""
    global _manager
    if _manager is None:
        _manager = TokenBudgetManager(custom_budgets=custom_budgets)
        logger.info("[TokenBudget] TokenBudgetManager initialized")
    return _manager
