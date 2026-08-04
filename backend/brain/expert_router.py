# backend/brain/expert_router.py
"""
SupremeAI Mixture of Experts (MoE) Router
Analyzes prompt domain (Bengali language, Coding/Technical, Reasoning/Logic, General)
and maps it to optimized primary and fallback model chains.
"""

from enum import Enum

from loguru import logger


class ExpertType(Enum):
    BENGALI = "bengali"  # Bangla language, Banglish, BD context
    CODER = "coder"  # Programming, DevOps, API, Technical
    REASONER = "reasoner"  # Math, Logic, Analysis, Strategy
    GENERAL = "general"  # General conversation


class SupremeMoERouter:
    """
    Mixture of Experts Router — Routes requests based on prompt classification.
    """

    EXPERT_MODEL_MAP: dict[ExpertType, list[str]] = {
        ExpertType.BENGALI: [
            "hf_space/supreme-hybrid-8b",
            "groq/llama-3.3-70b-versatile",
            "gemini/gemini-2.5-flash",
        ],
        ExpertType.CODER: [
            "deepseek/deepseek-coder",
            "groq/qwen-2.5-coder-32b",
            "openai/gpt-4o",
        ],
        ExpertType.REASONER: [
            "deepseek/deepseek-chat",
            "groq/deepseek-r1-distill-llama-70b",
            "gemini/gemini-2.5-pro",
        ],
        ExpertType.GENERAL: [
            "gemini/gemini-2.5-flash",
            "groq/llama-3.1-8b-instant",
            "openai/gpt-4o-mini",
        ],
    }

    BENGALI_KEYWORDS = [
        "বাংলা",
        "bangla",
        "bangladesh",
        "dhaka",
        "ki",
        "kemon",
        "acho",
        "kemon acho",
        "apni",
        "tumi",
        "ami",
        "কি",
        "কেন",
        "কিভাবে",
        "ব্যাখ্যা করো",
        "ধন্যবাদ",
        "হ্যালো",
    ]

    CODER_KEYWORDS = [
        "code",
        "python",
        "javascript",
        "typescript",
        "bug",
        "error",
        "function",
        "api",
        "docker",
        "deploy",
        "class",
        "async",
        "def",
        "return",
        "import",
        "const",
        "let",
    ]

    REASONING_KEYWORDS = [
        "calculate",
        "math",
        "logic",
        "prove",
        "analyze",
        "compare",
        "optimize",
        "algorithm",
        "equation",
        "theorem",
    ]

    @classmethod
    def classify_prompt(cls, prompt: str) -> ExpertType:
        """Classify prompt into ExpertType based on Unicode ranges and keyword triggers."""
        if not prompt:
            return ExpertType.GENERAL

        # Check Bengali Unicode Range (U+0980 to U+09FF)
        if any("\u0980" <= ch <= "\u09ff" for ch in prompt):
            return ExpertType.BENGALI

        prompt_lower = prompt.lower()
        words = set(prompt_lower.split())

        # Check explicit keywords
        if any(
            kw in words or (len(kw) > 3 and kw in prompt_lower)
            for kw in cls.BENGALI_KEYWORDS
        ):
            return ExpertType.BENGALI

        if any(
            kw in words or (len(kw) > 3 and kw in prompt_lower)
            for kw in cls.CODER_KEYWORDS
        ):
            return ExpertType.CODER

        if any(
            kw in words or (len(kw) > 3 and kw in prompt_lower)
            for kw in cls.REASONING_KEYWORDS
        ):
            return ExpertType.REASONER

        return ExpertType.GENERAL

    @classmethod
    def get_model_chain(cls, prompt: str) -> list[str]:
        """Return model fallback chain based on expert classification."""
        expert = cls.classify_prompt(prompt)
        chain = cls.EXPERT_MODEL_MAP[expert]
        logger.info(
            f"🔀 [MoE Router] Prompt classified as '{expert.value}' -> Chain: {chain}"
        )
        return chain
