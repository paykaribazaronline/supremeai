# 🎯 SupremeAI Dual Model Setup - QUICK REFERENCE

## 📋 Token Configuration

```
✅ 2 HuggingFace Tokens:
   - From: SAME account
   - Purpose: Primary + Fallback (rate limit protection)
   - Get from: https://huggingface.co/settings/tokens

✅ 2 NGROK Tokens:
   - From: DIFFERENT accounts (Account 1 & Account 2)
   - Purpose: Primary + Fallback (tunnel resilience)
   - Get from: https://dashboard.ngrok.com/get-started/your-authtoken
```

---

## 🚀 Quick Start (5 Minutes)

### 1️⃣ Setup (Local Machine)

```bash
cd c:\Users\Nazifa\supremeai\colab

# Option A: Interactive setup
python setup_dual_models.py

# Option B: Manual - Open notebook JSON and edit Cell 2
```

### 2️⃣ Colab (in browser)

1. Open notebook: [SupremeAI_AirLLM_Server.ipynb](https://colab.research.google.com/)
2. Runtime → Change runtime type → Select **T4 GPU**
3. Run cells in order:
   - **Cell 1**: Install (2 min)
   - **Cell 2**: Config (paste tokens)
   - **Cell 3**: Load model (3-5 min)
   - **Cell 4**: Start server (see NGROK URL)

### 3️⃣ Register with SupremeAI

```bash
export SUPREMEAI_BASE_URL="https://supremeai-565236080752.us-central1.run.app"
export SUPREMEAI_SETUP_TOKEN="your_token"
export AIRLLM_PUBLIC_URL="https://xxxxx.ngrok-free.dev"
export AIRLLM_MODEL="gemma"

python colab/airllm_auto_refresh.py
```

---

## 🎛️ Models at a Glance

| Feature | Gemma 2B | Llama 70B |
|---------|----------|-----------|
| **Size** | 2B params | 70B params |
| **Speed** | ⚡ 1-2 sec | 🐢 5-30 sec |
| **Quality** | ★★★ Good | ★★★★★ Excellent |
| **Max Output** | 256 tokens | 512 tokens |
| **Best For** | Quick responses | Complex reasoning |

---

## 🔌 API Endpoints

```bash
# Chat (OpenAI compatible)
POST {NGROK_URL}/v1/chat/completions

# Health check
GET  {NGROK_URL}/health

# List models
GET  {NGROK_URL}/v1/models

# Model status
GET  {NGROK_URL}/status/models
```

---

## 🔄 Switching Models

```python
# In Colab Cell 2, change:
ACTIVE_MODEL = 'gemma'   # ← Change to 'llama'

# Then restart:
# 1. Run Cell 3
# 2. Run Cell 4
# Takes ~3-5 minutes
```

---

## 🚨 Troubleshooting

| Problem | Solution |
|---------|----------|
| Rate limited | Wait 1-24h or use different HF account |
| NGROK fails | Switch to fallback token (auto) |
| No GPU | Colab → Runtime → Change to T4 |
| Model not loading | Check HF token validity |
| Slow response | Switch to Gemma 2B from Llama |

---

## 📊 Monitoring

```bash
# Test endpoint (after Colab is running)
python test_dual_models.py https://xxxxx.ngrok-free.dev --model gemma

# View real-time health
curl https://xxxxx.ngrok-free.dev/health | python -m json.tool
```

---

## 🎯 Token Configuration Template

```python
# Cell 2 - Replace these:
HF_TOKENS = [
    'hf_YOUR_TOKEN_1',   # ← Primary (same account)
    'hf_YOUR_TOKEN_2'    # ← Backup (same account)
]

NGROK_TOKENS = {
    'primary': 'ngrok_token_account_1',    # ← Account 1
    'fallback': 'ngrok_token_account_2'    # ← Account 2
}

ACTIVE_MODEL = 'gemma'  # ← Change to 'llama' to switch
```

---

## ✅ Verification Checklist

- [ ] 2 HF tokens created (same account)
- [ ] 2 NGROK accounts created (different)
- [ ] 2 NGROK tokens extracted
- [ ] Tokens pasted into Colab Cell 2
- [ ] T4 GPU selected in Colab runtime
- [ ] All 4 cells run successfully
- [ ] Health endpoint returns 200 OK
- [ ] Chat endpoint returns valid response
- [ ] Model switching tested (Gemma ↔ Llama)

---

## 🔗 Files Overview

| File | Purpose |
|------|---------|
| `SupremeAI_AirLLM_Server.ipynb` | Main Colab notebook (4 cells) |
| `setup_dual_models.py` | Interactive token setup helper |
| `airllm_auto_refresh.py` | Auto-register with SupremeAI Cloud |
| `test_dual_models.py` | Test endpoint connectivity |
| `DUAL_MODEL_SETUP.md` | Full detailed guide |
| `QUICK_REFERENCE.md` | This file |

---

## 💡 Pro Tips

1. ✅ Create NGROK backup account TODAY (before it's needed)
2. ✅ Bookmark health endpoint for quick status checks
3. ✅ Test both models before production use
4. ✅ Monitor Colab session timer (12-hour limit)
5. ✅ Keep Colab running in background tab while using

---

## 🆘 Support

For issues:

1. Check [DUAL_MODEL_SETUP.md](DUAL_MODEL_SETUP.md) troubleshooting section
2. Run `test_dual_models.py` to diagnose
3. Check Colab cell errors (scroll up)
4. Verify tokens are valid (test with curl)

---

**Status: ✅ Ready to Deploy**
Setup date: April 2026
Last updated: 2026-04-08
