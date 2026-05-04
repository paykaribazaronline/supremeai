# CodeGeeX4 Setup Guide for SupremeAI

## Overview
CodeGeeX4 is a powerful code-specialized AI model from BigModel.cn (智谱AI) with 128K context window and excellent code generation capabilities. This guide will help you set up CodeGeeX4 as a local model option in VS Code with SupremeAI.

## Features
- **Code-specialized**: Trained specifically on code, not general text
- **128K context**: Can understand entire codebases
- **50+ languages**: Supports most programming languages
- **Infilling**: Can fill in missing code in the middle of files
- **Repository Q&A**: Can answer questions about your codebase
- **Very affordable**: ~$0.0003-0.0014 per 1K tokens
- **Free tier**: New accounts get ¥50-100 credit (~$7-14)

## Setup Steps

### 1. Get CodeGeeX4 API Key

1. Go to **https://bigmodel.cn** (智谱AI开放平台)
2. Sign up for a free account
3. Navigate to **API Keys** (API 密钥管理)
4. Create a new key named `SupremeAI-Integration`
5. Copy the API key (format: `sk-xxxxxxxxxxxxxxxxxxxxxxxx`)

### 2. Configure Environment

Add to your `.env` file:
```bash
CODEGEEX4_API_KEY=your-api-key-here
```

### 3. Backend Configuration

The backend is already configured to use CodeGeeX4. The following settings are in `application.properties`:

```properties
# CodeGeeX4 API Key
supremeai.provider.codegeex4.api-key=${CODEGEEX4_API_KEY:}

# Quota settings
codegeex4.daily.quota=100000
codegeex4.monthly.quota=3000000

# Active providers (CodeGeeX4 is included)
supremeai.active.providers=codegeex4,groq,openai,anthropic,ollama,stepfun,deepseek
```

### 4. VS Code Extension

The VS Code extension supports CodeGeeX4 out of the box. Configure in settings:

```json
{
  "supremeai.codegeex4.enabled": true,
  "supremeai.codegeex4.model": "codegeex-4"
}
```

### 5. Build and Run

```bash
# Build the backend
./gradlew clean build -x test

# Run the backend
./gradlew bootRun
```

## Usage

### Via API

```bash
curl -X POST http://localhost:8080/api/ai/generate \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "codegeex4",
    "prompt": "Write a Python function to reverse a linked list"
  }'
```

### Via SupremeAI Dashboard

1. Open the dashboard at `http://localhost:5173/admin/apikeys`
2. Add a new provider: `codegeex4`
3. Enter your API key
4. Start using CodeGeeX4 for code generation!

### Via VS Code

1. Install the SupremeAI VS Code extension
2. Configure your API key in settings
3. Use CodeGeeX4 for:
   - Code completion
   - Code explanation
   - Bug fixing
   - Code review
   - Test generation

## Pricing

| Tier | Price/1K tokens | Best For |
|------|----------------|----------|
| **Lite** | ¥0.002 (~$0.00028) | Testing, development |
| **Std** | ¥0.005 (~$0.0007) | Production |
| **Pro** | ¥0.01 (~$0.0014) | Enterprise |

**Example Costs:**
- 1000 code generations/month (500 tokens each): **~$0.28/month**
- 10K code generations/month: **~$2.8/month**
- 100K code generations/month: **~$28/month**

## Comparison with Other Providers

| Provider | Cost/1K tokens | Context | Specialization |
|----------|----------------|---------|----------------|
| **CodeGeeX4** | $0.0003-0.0014 | 128K | Code (50+ languages) |
| StepFun | $0.0005-0.002 | ~32K | General AI |
| Groq (Llama) | Free | 8K | General |
| DeepSeek | $0.0004 | 64K | Code |
| GPT-4 | $0.03 | 8K | General |

## Local Alternative: Ollama

If you prefer 100% free and local:

```bash
# Install Ollama
curl -fsSL https://ollama.com/install.sh | sh

# Pull CodeGeeX4 model
ollama pull codegeex4

# Run locally
ollama run codegeex4
```

Configure SupremeAI to use local Ollama:
```properties
ai.providers.ollama.endpoint=http://localhost:11434/api/generate
```

## Troubleshooting

### API Key Errors
- **401 Unauthorized**: Check your API key is correct
- **429 Rate Limit**: You've exceeded your quota. Wait or add credits
- **500 Server Error**: BigModel server issue. Try again later

### Connection Issues
- Check your internet connection
- Verify the endpoint is accessible: `https://open.bigmodel.cn`
- Try the fallback endpoint: `https://open.bigmodel.cn/api/paas/v4/chat/completions`

### No Response
- Check logs: `tail -f logs/supremeai.log | grep -i codegeex`
- Verify API key is set correctly in `.env`
- Ensure backend is running: `./gradlew bootRun`

## Best Practices

1. **Use appropriate temperature**:
   - 0.3-0.5 for deterministic code
   - 0.7-1.0 for creative solutions

2. **Set max tokens**:
   - 500-1000 for code snippets
   - 2000-4000 for full functions

3. **Use system prompts**:
   ```
   You are CodeGeeX, a code expert. Write clean, efficient, well-documented code.
   ```

4. **Monitor usage**:
   - Check your quota at https://bigmodel.cn
   - Set up billing alerts

## Support

- **BigModel Docs**: https://open.bigmodel.cn/dev/api/code-model/codegeex-4
- **Pricing**: https://bigmodel.cn/pricing
- **Console**: https://bigmodel.cn

## License

CodeGeeX4 requires registration and acceptance of terms. Free tier available for testing.

---

**Ready to implement?** Your CodeGeeX4 provider is already configured in the codebase!

Just add your API key to `.env` and start the backend. 🚀