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
