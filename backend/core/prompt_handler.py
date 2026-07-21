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


def format_unified_chat_prompt(
    message: str, history: list[dict[str, str]] = None
) -> str:
    """
    Centralized prompt builder for unifying chat history with the current task.
    Prevents context loss and DRY violations across multiple routers.
    """
    if not history:
        return message

    formatted_prompt = ""
    for msg in history:
        role = "User" if msg.get("role") == "user" else "Assistant"
        formatted_prompt += f"{role}: {msg.get('content', '')}\n"
    formatted_prompt += f"User: {message}\nAssistant:"
    return formatted_prompt
