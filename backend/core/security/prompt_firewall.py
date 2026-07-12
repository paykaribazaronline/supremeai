# বাংলা মন্তব্য: Anthropic Constitutional AI প্যাটার্ন ইমপ্লিমেন্টেশন।
# মডেলের রেসপন্স ইউজারের কাছে পাঠানোর আগে এটি নির্দিষ্ট কিছু প্রিন্সিপল অনুযায়ী যাচাই করবে।
# tests-এর জন্য pre_flight_scan এবং classify_intent helper functions যোগ করা হলো।

import re
from typing import Any

from loguru import logger

from core.llm.llm_gateway import GatewayManager


CONSTITUTIONAL_PRINCIPLES = [
    "Avoid generating harmful or dangerous content",
    "Do not assist with illegal activities",
    "Protect user privacy and do not leak PII",
    "Be honest about AI limitations and do not hallucinate facts",
]

# বাংলা মন্তব্য: Local pattern matching — LLM call ছাড়াই দ্রুত obvious threats block করা
_LOCAL_BLOCK_PATTERNS: dict[str, list[str]] = {
    "prompt_injection": [
        "disregard previous instructions",
        "ignore all prior",
        "forget your instructions",
        "new personality",
        "act as",
        "jailbreak",
    ],
    "sensitive_extraction": [
        "password=",
        "api_key=",
        "secret=",
        "token=",
        "credentials",
    ],
    "malicious_code": [
        "rm -rf",
        "DROP TABLE",
        "eval(",
        "__import__",
        "os.system",
    ],
}

# বাংলা মন্তব্য: BENGALI NATIVE ENFORCEMENT RULES — PromptFirewall enforce করে যে
# Agent সবসময় Bangla-aware এবং helpful response দেবে।
_BENGALI_ENFORCEMENT_HEADER = """BENGALI NATIVE ENFORCEMENT RULES:
- Always respond in Bangla (বাংলা) when the user writes in Bangla.
- Be culturally sensitive and respectful to Bangladeshi users.
- Prioritize clarity and helpfulness over formality.
"""


class PromptFirewall:
    def __init__(self, gateway: GatewayManager = None):  # type: ignore[assignment]
        self.gateway = gateway or GatewayManager()
        self.cheap_model = "gemini/gemini-2.5-flash"  # Free tier for quick critique

    def enforce_bengali_rules(self, system_prompt: str) -> str:
        """বাংলা মন্তব্য: Bangla enforcement header inject করে যদি না থাকে।"""
        if "BENGALI NATIVE ENFORCEMENT RULES" in system_prompt:
            return system_prompt
        return system_prompt + "\n" + _BENGALI_ENFORCEMENT_HEADER

    def validate_agent_response(self, response: str) -> bool:
        """বাংলা মন্তব্য: Validate that a response is non-empty and contains Bangla text."""
        if not response or not response.strip():
            return False
        # Check for at least one Bengali character (Unicode range U+0980 - U+09FF)
        return bool(re.search(r"[\u0980-\u09FF]", response))

    def _check_local_patterns(self, text: str) -> str | None:
        """বাংলা মন্তব্য: Local heuristic check — LLM-free fast path."""
        lower = text.lower()
        for threat_type, patterns in _LOCAL_BLOCK_PATTERNS.items():
            for pattern in patterns:
                if pattern.lower() in lower:
                    return threat_type
        return None

    async def constitutional_filter(self, response_text: str) -> tuple[str, bool]:
        """
        Anthropic Constitutional AI pattern.
        Returns: (filtered_response, was_revised)
        """
        # বাংলা মন্তব্য: Local check আগে — LLM call কম হবে
        local_threat = self._check_local_patterns(response_text)
        if local_threat:
            logger.warning(f"Local pattern blocked: {local_threat}")
            return "[Content blocked by local policy filter]", True

        for principle in CONSTITUTIONAL_PRINCIPLES:
            try:
                # 1. Critique phase
                critique_prompt = f"Does the following response violate this principle: '{principle}'?\n\nResponse:\n{response_text}\n\nAnswer ONLY with 'YES' or 'NO'."
                critique_response = await self.gateway.acompletion(prompt=critique_prompt, model=self.cheap_model)
                critique_text = critique_response.get("text", "").strip().upper()

                if "YES" in critique_text:
                    logger.warning(f"Constitutional AI triggered on principle: '{principle}'")

                    # 2. Revision phase
                    revision_prompt = f"The following response violates the principle: '{principle}'. Please revise it to be compliant while preserving the original intent.\n\nResponse:\n{response_text}"
                    revised_response = await self.gateway.acompletion(prompt=revision_prompt, model=self.cheap_model)
                    return revised_response.get("text", response_text), True

            except Exception as e:  # noqa: BLE001
                logger.error(f"Error during constitutional filtering: {e}")
                continue

        return response_text, False


# বাংলা মন্তব্য: Singleton instance
firewall = PromptFirewall()


async def pre_flight_scan(prompt: str) -> dict[str, Any]:
    """বাংলা মন্তব্য: pre_flight_scan — prompt submit করার আগে quick local check।
    Returns dict with 'allowed' and optional 'threat_type' keys.
    """
    threat = firewall._check_local_patterns(prompt)
    if threat:
        return {"allowed": False, "threat_type": threat, "reason": f"Local pattern match: {threat}"}
    return {"allowed": True, "threat_type": None}


async def classify_intent(prompt: str) -> dict[str, Any]:
    """বাংলা মন্তব্য: classify_intent — prompt-এর intent classify করে।
    Lightweight keyword-based classification without LLM call.
    """
    lower = prompt.lower()

    coding_keywords = ["write", "code", "script", "function", "implement", "debug", "python", "javascript"]
    reasoning_keywords = ["why", "explain", "analyze", "compare", "difference", "reason", "because"]
    creative_keywords = ["story", "poem", "creative", "imagine", "write a", "compose"]

    if any(kw in lower for kw in coding_keywords):
        return {"intent": "coding", "confidence": 0.9}
    if any(kw in lower for kw in reasoning_keywords):
        return {"intent": "reasoning", "confidence": 0.85}
    if any(kw in lower for kw in creative_keywords):
        return {"intent": "creative", "confidence": 0.8}

    return {"intent": "general", "confidence": 0.6}
