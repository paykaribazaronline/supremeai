# 📄 ফাইল: backend/core/prompt_handler.py

**প্রকার:** .py  
**সাইজ:** 634 বাইট  
**আপডেট:** 2026-07-11T14:41:19.322502

---

## কোড

```py
from typing import Any


def normalize_prompt(prompt: str | list[dict[str, Any]]) -> str:
    """
    Extracts the textual representation of a prompt for hashing, token estimation,
    or complexity checks.
    """
    if isinstance(prompt, str):
        return prompt
    elif isinstance(prompt, list) and len(prompt) > 0:
        return str(prompt[-1].get("content", ""))
    return ""


def estimate_tokens(text: str | list[dict[str, Any]]) -> int:
    """
    Estimates the number of tokens in a prompt (rough estimate: 4 chars = 1 token).
    """
    normalized_text = normalize_prompt(text)
    return len(normalized_text) // 4

```