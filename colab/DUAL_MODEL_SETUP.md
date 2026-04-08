# SupremeAI Dual Model Setup Guide

## 📋 Quick Overview

**What's New:**

- ✅ Support for 2 models: Gemma 2B (fast) + Llama-3.3-70B (powerful)
- ✅ Token rotation for HuggingFace (fallback if rate limited)
- ✅ Token rotation for NGROK (fallback to alternate account)
- ✅ Single-cell switching between models
- ✅ Enhanced API endpoints for model management

**Token Configuration:**

- 2 HuggingFace tokens (same account for quota sharing)
- 2 NGROK tokens (different accounts for maximum resilience)

---

## 🎯 Setup Steps

### Step 1: Get Your Tokens

#### HuggingFace Tokens (Same Account)

1. Go to https://huggingface.co/settings/tokens
2. Create 2 tokens with `read` permission:
   - Token 1: For primary use
   - Token 2: Backup (rate limit failover)
   - **Both from same account** = shared quota

#### NGROK Tokens (Different Accounts)

1. Account 1: https://dashboard.ngrok.com/get-started/your-authtoken
   - Create account 1 at https://ngrok.com
   - Get auth token from dashboard
2. Account 2: https://dashboard.ngrok.com/get-started/your-authtoken
   - Create separate account 2 at https://ngrok.com
   - Get auth token from dashboard
   - **Different accounts** = independent tunnels

---

### Step 2: Run Setup Helper (Optional but Recommended)

```bash
cd c:\Users\Nazifa\supremeai\colab
python setup_dual_models.py
```

This will:

1. Prompt you for 2 HF tokens (same account)
2. Prompt you for 2 NGROK tokens (different accounts)
3. Generate ready-to-use Colab code
4. Save config to `colab_token_config.json`

---

### Step 3: Configure Colab Notebook

Open [SupremeAI_AirLLM_Server.ipynb](SupremeAI_AirLLM_Server.ipynb)

**Cell 1 (Dependencies):** Click run ✅

```python
# Step 1: Install dependencies
!pip uninstall -y transformers optimum accelerate bitsandbytes airllm -q 2>/dev/null
!pip install -q 'transformers>=4.41.0' optimum accelerate bitsandbytes
!pip install -q airllm
!pip install -q flask flask-cors pyngrok
```

**Cell 2 (Configuration):** Edit and run ✅

```python
# Replace these values:
HF_TOKENS = [
    'hf_YOUR_TOKEN_1_HERE',      # Token 1 (same account)
    'hf_YOUR_TOKEN_2_HERE'       # Token 2 (same account)
]

NGROK_TOKENS = {
    'primary': 'ngrok_token_account_1_here',    # Account 1
    'fallback': 'ngrok_token_account_2_here'    # Account 2
}

# Choose which model to run:
ACTIVE_MODEL = 'gemma'  # Change to 'llama' to switch
```

**Cell 3 (Load Model):** Run ✅

- Loads Gemma 2B or Llama-3.3-70B (based on Cell 2)
- Auto-fallback if HF token #1 fails (uses token #2)

**Cell 4 (Start Server):** Run ✅

- Starts Flask API server
- Creates NGROK tunnel
- Auto-fallback if NGROK token #1 fails (uses token #2)

---

## 🚀 Usage

### Available Models

| Model | Size | Speed | Quality | Max Tokens | Compression | Best For |
|-------|------|-------|---------|-----------|------------|----------|
| **Gemma 2B** | 2B params | ⚡ <2s | ★★★☆☆ | 256 | 4-bit | Quick Q&A, chat |
| **Llama 70B** | 70B params | 🐢 5-30s | ★★★★★ | 512 | 4-bit | Complex tasks, reasoning |

### API Endpoints

#### 1. Chat Completions (OpenAI Compatible)

```bash
curl -X POST https://your-ngrok-url/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "messages": [{"role": "user", "content": "Hello"}],
    "max_tokens": 256
  }'
```

**Response:**

```json
{
  "id": "chatcmpl-...",
  "object": "chat.completion",
  "created": 1712600000,
  "model": "google/gemma-2b-it",
  "chosen_model": "gemma",
  "choices": [{
    "index": 0,
    "message": {"role": "assistant", "content": "Hello! How can I help?"},
    "finish_reason": "stop"
  }],
  "usage": {
    "prompt_tokens": 5,
    "completion_tokens": 8,
    "total_tokens": 13
  }
}
```

#### 2. Health Check

```bash
curl https://your-ngrok-url/health
```

**Response:**

```json
{
  "status": "healthy",
  "provider": "airllm-colab",
  "active_model": "gemma",
  "model_id": "google/gemma-2b-it",
  "available_models": ["gemma", "llama"],
  "gpu": "Tesla T4",
  "uptime": 3600,
  "requests": 150,
  "errors": 2,
  "hf_token_used": 1
}
```

#### 3. List Models

```bash
curl https://your-ngrok-url/v1/models
```

Response shows all available models with `active` flag.

#### 4. Model Status

```bash
curl https://your-ngrok-url/status/models
```

Detailed status of all models.

---

### Switching Models

To switch from Gemma to Llama (or vice versa):

1. Go back to **Cell 2** in Colab
2. Change:

   ```python
   ACTIVE_MODEL = 'llama'  # was 'gemma'
   ```

3. Run **Cell 3** (restart model loading)
4. Run **Cell 4** (restart server with new model)

⏱️ Restart takes ~3-5 minutes (model loading time)

---

## 🔄 Token Rotation & Fallback

### HuggingFace Token Fallback

If Token #1 hits rate limit:

```
Cell 3 execution:
[Attempt 1] Loading gemma model...
❌ HF Token #1 failed: Rate limit exceeded
[Attempt 2] Loading gemma model...
✅ Model loaded in 45s
   Using HF Token #2
```

### NGROK Token Fallback

If Account 1 token fails:

```
Cell 4 execution:
[NGROK] Attempting with primary token...
❌ primary token failed: ...
[NGROK] Attempting with fallback token...
✅ NGROK connected via fallback
```

---

## 📊 Monitoring

### Key Metrics

- **Requests**: Track API calls
- **Errors**: Failed requests
- **Uptime**: Server running time
- **HF Token Used**: Which token is active
- **GPU**: Hardware info (Tesla T4 on Colab)

### Health Check Frequency

Register periodic health checks in SupremeAI:

```bash
export SUPREMEAI_BASE_URL="https://supremeai-565236080752.us-central1.run.app"
export SUPREMEAI_SETUP_TOKEN="your_token_here"
export AIRLLM_PUBLIC_URL="https://xxxxx.ngrok-free.dev"
export AIRLLM_MODEL="gemma"

# Run auto-refresh (register endpoint)
python colab/airllm_auto_refresh.py
```

---

## ⚠️ Troubleshooting

### Issue: HuggingFace Token Rate Limited

**Problem:** Both tokens hit rate limit  
**Solution:**

- Wait 1-24 hours for quota reset
- Use different accounts for future tokens
- Increase model `max_length` to reduce token usage

### Issue: NGROK Token Exhausted

**Problem:** Both accounts suspended  
**Solution:**

- Subscribe to NGROK Pro ($15/month per account)
- Create new NGROK accounts
- Use different physical locations

### Issue: Model Takes Too Long

**Solution Options:**

1. Switch to faster model: `ACTIVE_MODEL = 'gemma'`
2. Reduce `max_tokens` in config
3. Reduce `max_length` in tokenizer
4. Check GPU availability in Colab (Runtime > Change runtime type)

### Issue: No GPU Available

**Fix:**

1. Go to Colab: Runtime > Change runtime type
2. SELECT: Hardware accelerator = **T4 GPU**
3. Click "Save"
4. Restart cells

---

## 🎛️ Advanced Configuration

### Custom Compression

Change in Cell 2:

```python
MODELS['gemma']['compression'] = '8bit'  # More accurate, slower
MODELS['llama']['compression'] = '2bit'  # Faster, less accurate
```

### Custom Max Tokens

```python
MODELS['gemma']['max_tokens'] = 128   # Shorter responses
MODELS['llama']['max_tokens'] = 1024  # Longer responses
```

### Custom Port

```python
PORT = 9000  # Instead of 8081
```

---

## 📝 Quick Reference

| Action | Steps |
|--------|-------|
| **Start Server** | Run Cells 1 → 2 → 3 → 4 |
| **Switch Model** | Edit Cell 2 → Run Cell 3 → Run Cell 4 |
| **Use from SupremeAI** | Set `AIRLLM_ENDPOINT` env var, run `airllm_auto_refresh.py` |
| **Check Status** | `curl {NGROK_URL}/health` |
| **List Models** | `curl {NGROK_URL}/v1/models` |

---

## 🔗 Related Files

- [SupremeAI_AirLLM_Server.ipynb](SupremeAI_AirLLM_Server.ipynb) - Main Colab notebook
- [setup_dual_models.py](setup_dual_models.py) - Token setup helper
- [airllm_auto_refresh.py](airllm_auto_refresh.py) - Auto-register with SupremeAI
- [README.md](README.md) - Original AirLLM guide

---

## 💡 Best Practices

1. ✅ Create backup NGROK accounts NOW (before they're needed)
2. ✅ Store tokens in environment variables, not in code
3. ✅ Monitor health checks regularly
4. ✅ Watch quota usage to predict rate limits
5. ✅ Keep Colab runtime running for persistence (⏰ 12-hour session limit)
6. ✅ Test both models before going to production
7. ✅ Document which model works best for your use case

**Happy Inferencing! 🚀**
