package com.supremeai.provider;

public enum AIProviderType {
    // Tier 1: External APIs
    GROQ_LLAMA3,
    GEMINI_PRO,
    ANTHROPIC_CLAUDE,
    OPENAI,
    DEEPSEEK,
    KIMI,
    MISTRAL,
    STEPFUN,
    HUGGINGFACE_FREE,

    // Tier 2: Private Cloud-Native Models (Scale-to-Zero)
    CLOUD_QWEN,
    CLOUD_LLAMA,
    CLOUD_DEEPSEEK,
    CLOUD_PHI,
    CLOUD_NOMIC,

    // Tier 3: Local Fallbacks
    OLLAMA
}