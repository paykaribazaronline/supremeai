"""SupremeAI 2.0 — Custom core exceptions.

বাংলা মন্তব্য: প্রজেক্টের বিভিন্ন কোর মডিউল (যেমন llm_router) এবং মডিউলগুলোর এরর হ্যান্ডলিংয়ের জন্য
প্রয়োজনীয় কাস্টম এক্সেপশন ক্লাসসমূহ।
"""

from typing import Any


class LLMProviderError(RuntimeError):
    """Raised when an LLM provider request fails.

    বাংলা মন্তব্য: এলএলএম প্রোভাইডারের কল ফেইল বা কোনো নেটওয়ার্ক ত্রুটির জন্য এই এক্সেপশন ব্যবহৃত হয়।
    """

    def __init__(self, message: str, details: dict[str, Any] | None = None) -> None:
        self.message = message
        self.details = details or {}
        super().__init__(self.message)


class QuotaExceededError(RuntimeError):
    """Raised when the LLM provider rate limit or budget quota is exceeded.

    বাংলা মন্তব্য: এলএলএম প্রোভাইডারের কোটা লিমিট ক্রস করলে বা বাজেট ফুরিয়ে গেলে এই এক্সেপশন ফায়ার হয়।
    """

    def __init__(
        self, message: str = "LLM Provider Rate Limit or Budget Quota Exceeded."
    ) -> None:
        super().__init__(message)
