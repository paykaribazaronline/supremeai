# AI Provider Coverage

## Supported Providers

SupremeAI supports 11 AI providers for multi-agent consensus and code generation.

| Provider | Class | Status | Type |
|----------|-------|--------|------|
| OpenAI | `provider/OpenAIProvider.java` | Implemented | Cloud API |
| Anthropic | `provider/AnthropicProvider.java` | Implemented | Cloud API |
| Google Gemini | `provider/GeminiProvider.java` | Implemented | Cloud API |
| Groq | `provider/GroqProvider.java` | Implemented | Cloud API |
| HuggingFace | `provider/HuggingFaceProvider.java` | Implemented | Cloud API |
| Ollama | `provider/OllamaProvider.java` | Implemented | Local |
| Other providers | See `provider/` package | Varies | Various |

## Configuration

### Environment Variables

Set these in `src/main/resources/application.properties` or as environment variables:

```properties
# OpenAI
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4

# Anthropic
ANTHROPIC_API_KEY=sk-ant-...
ANTHROPIC_MODEL=claude-3-opus

# Google
GOOGLE_API_KEY=...
GOOGLE_MODEL=gemini-pro

# Groq
GROQ_API_KEY=gsk_...
GROQ_MODEL=mixtral-8x7b

# HuggingFace
HUGGINGFACE_API_KEY=hf_...

# Ollama (local)
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=llama3
```

### Provider Factory

**File:** `src/main/java/com/supremeai/provider/AIProviderFactory.java`

The factory creates and manages provider instances:

- Auto-discovers configured providers
- Handles failover between providers
- Manages API rate limits

### Multi-AI Consensus

**File:** `src/main/java/com/supremeai/service/MultiAIConsensusService.java`

The consensus service:

1. Sends requests to multiple providers
2. Collects responses
3. Votes on best answer
4. Returns consensus decision

## Provider Status Table

| Provider | API Key Required | Tested | Production Ready |
|----------|-----------------|--------|------------------|
| OpenAI | Yes | Yes | Yes |
| Anthropic | Yes | Yes | Yes |
| Google Gemini | Yes | Partial | Partial |
| Groq | Yes | Partial | Partial |
| HuggingFace | Yes | Partial | Partial |
| Ollama | No (local) | Partial | Partial |

## Failover System

**File:** `src/main/java/com/supremeai/fallback/AIFallbackOrchestrator.java`

Automatic failover:

1. Primary provider fails
2. Fallback to secondary
3. Continue until success or all providers exhausted

## Usage

### Single Provider

```java
AIProvider provider = providerFactory.getProvider(AIProviderType.OPENAI);
String response = provider.generate(prompt);
```

### Multi-Provider Consensus

```java
ConsensusResult result = consensusService.getConsensus(prompt, providers);
```

## Troubleshooting

### No Providers Configured

- Check environment variables are set
- Verify `application.properties` has API keys
- Restart application after configuration changes

### Provider Failing

- Check API key validity
- Verify network connectivity
- Review rate limits and quotas
- Check provider logs in `logs/` directory
