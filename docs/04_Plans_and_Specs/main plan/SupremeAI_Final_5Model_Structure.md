
# SupremeAI - Final 5-Model Structure

> **Status:** 🟢 Updated for v5 Architecture

## With Free Multiple API Keys Support

---

# 1. Model Selection (Final 5)

| # | Model | Role | Provider | Cost | Why |
|---|-------|------|----------|------|-----|
| 1 | **Qwen 2.5 Coder 7B** | Primary Coder | GCP Run | $0 (Idle) | Best code generation |
| 2 | **Llama 3.1 8B** | General Chat | GCP Run | $0 (Idle) | Best all-rounder |
| 3 | **DeepSeek-V4-Pro** | Review/Debug | HF Endpoint| $0 (Idle) | State-of-the-art Reasoning |
| 4 | **Phi 3 Mini** | Fast Tasks | GCP Run | $0 (Idle) | Quick responses |
| 5 | **Nomic Embed** | Embeddings | GCP Run | $0 (Idle) | Search/Similarity |

**Infrastructure Strategy:** Scale-to-Zero (Serverless GPU)
**Deployment:** All models are hosted as independent Cloud Run services or Hugging Face Dedicated Endpoints.

---

# 2. Project Structure

```
supremeai/
├── .github/
│   └── workflows/
│       └── ci.yml
├── config/
│   ├── __init__.py
│   ├── settings.py          # Main config
│   ├── model_config.py      # Model definitions
│   └── api_keys.py          # API key management
├── core/
│   ├── __init__.py
│   ├── orchestrator.py      # Plan 1: Agent orchestrator
│   ├── router.py            # Model router
│   └── fallback.py          # System AI fallback
├── models/
│   ├── __init__.py
│   ├── local/
│   │   ├── __init__.py
│   │   ├── qwen_coder.py    # Qwen 2.5 Coder 7B
│   │   ├── llama_general.py # Llama 3.1 8B
│   │   ├── deepseek_debug.py# DeepSeek Coder 6.7B
│   │   ├── phi_fast.py      # Phi 3 Mini
│   │   └── nomic_embed.py   # Nomic Embed
│   └── external/
│       ├── __init__.py
│       ├── openai_client.py     # OpenAI API
│       ├── anthropic_client.py  # Claude API
│       ├── google_client.py     # Gemini API
│       ├── groq_client.py       # Groq API (fast)
│       └── together_client.py   # Together AI
├── api/
│   ├── __init__.py
│   ├── key_manager.py       # Plan 2: Key rotation
│   ├── key_validator.py     # Validate keys
│   ├── rotation_strategy.py # Rotation logic
│   └── free_tier_monitor.py # Monitor limits
├── learning/
│   ├── __init__.py
│   ├── knowledge_base.py    # Knowledge storage
│   ├── web_scraper.py       # Browser learning
│   └── pattern_learner.py   # Pattern learning
├── storage/
│   ├── __init__.py
│   ├── database.py          # SQLite/PostgreSQL
│   └── cache.py             # Redis/Memcached
├── github_integration/
│   ├── __init__.py
│   ├── repo_manager.py      # Dual repo
│   └── webhook_handler.py   # GitHub webhooks
├── voice/
│   ├── __init__.py
│   └── speech_processor.py  # Voice input
├── vision/
│   ├── __init__.py
│   └── image_processor.py   # Image understanding
├── dashboard/
│   ├── __init__.py
│   ├── admin.py             # Admin settings
│   └── user_ui.py           # User interface
├── utils/
│   ├── __init__.py
│   ├── logger.py
│   └── helpers.py
├── tests/
│   ├── __init__.py
│   ├── test_orchestrator.py
│   ├── test_key_manager.py
│   ├── test_models.py
│   └── test_integration.py
├── scripts/
│   ├── setup.sh
│   └── deploy.sh
├── docs/
│   ├── architecture.md
│   └── api_reference.md
├── requirements.txt
├── requirements-dev.txt
├── .env.example
├── .gitignore
└── README.md
```

---

# 3. Free API Keys Configuration

## Supported Free Tiers:

| Provider | Free Tier | Limit | Model Access |
|----------|-----------|-------|--------------|
| **OpenAI** | $5 credit | 3 months | GPT-3.5, GPT-4 |
| **Anthropic** | $5 credit | - | Claude 3 Haiku |
| **Google AI** | Free tier | 60 req/min | Gemini Pro, Flash |
| **Groq** | Free tier | - | Llama, Mixtral (fast) |
| **Together AI** | $5 credit | - | Various models |
| **Cohere** | Trial | - | Command models |
| **Mistral** | Free tier | - | Mistral 7B |

## config/api_keys.py:

```python
# Free API Key Configuration
FREE_API_PROVIDERS = {
    'openai': {
        'base_url': 'https://api.openai.com/v1',
        'models': ['gpt-3.5-turbo', 'gpt-4'],
        'free_credits': 5.0,
        'validity_days': 90,
    },
    'anthropic': {
        'base_url': 'https://api.anthropic.com',
        'models': ['claude-3-haiku-20240307'],
        'free_credits': 5.0,
        'validity_days': None,
    },
    'google': {
        'base_url': 'https://generativelanguage.googleapis.com',
        'models': ['gemini-pro', 'gemini-flash'],
        'free_requests_per_min': 60,
        'validity_days': None,  # Always free
    },
    'groq': {
        'base_url': 'https://api.groq.com/openai/v1',
        'models': ['llama3-8b-8192', 'mixtral-8x7b-32768'],
        'free_tier': True,
        'validity_days': None,
    },
    'together': {
        'base_url': 'https://api.together.xyz/v1',
        'models': ['mistralai/Mixtral-8x7B-Instruct-v0.1'],
        'free_credits': 5.0,
        'validity_days': None,
    },
    'mistral': {
        'base_url': 'https://api.mistral.ai/v1',
        'models': ['mistral-tiny', 'mistral-small'],
        'free_tier': True,
        'validity_days': None,
    }
}

# Priority order (best first)
PROVIDER_PRIORITY = [
    'groq',      # Fastest, always free
    'google',    # Generous limits
    'mistral',   # Good for small tasks
    'openai',    # $5 credit
    'anthropic', # $5 credit
    'together',  # $5 credit
]
```

---

# 4. Model Router Logic

```python
# core/router.py
class ModelRouter:
    def __init__(self):
        self.cloud_models = {
            'qwen_coder': CloudEndpoint('supreme-qwen-coder'),
            'llama_general': CloudEndpoint('supreme-llama-general'),
            'deepseek_pro': CloudEndpoint('supreme-deepseek-v4'),
            'phi_fast': CloudEndpoint('supreme-phi-fast'),
            'nomic_embed': CloudEndpoint('supreme-nomic-embed'),
        }
        self.external_clients = {
            'groq': GroqClient(),
            'google': GoogleClient(),
            'openai': OpenAIClient(),
        }
        self.key_manager = APIKeyManager()

    def route(self, task_type, task_input, user_preference=None):
        # Step 1: Check if local model can handle
        local_model = self.select_local_model(task_type)
        if local_model and self.is_available(local_model):
            return local_model.process(task_input)

        # Step 2: Try external API (free tier)
        external_provider = self.select_external_provider(task_type)
        if external_provider:
            return external_provider.process(task_input)

        # Step 3: Fallback to system AI
        return self.system_ai_fallback(task_input)

    def select_local_model(self, task_type):
        mapping = {
            'code_generation': 'qwen_coder',
            'code_review': 'deepseek_debug',
            'general_chat': 'llama_general',
            'quick_task': 'phi_fast',
            'embedding': 'nomic_embed',
        }
        return self.local_models.get(mapping.get(task_type))

    def select_external_provider(self, task_type):
        # Check which provider has available quota
        for provider_name in PROVIDER_PRIORITY:
            provider = self.external_clients.get(provider_name)
            if provider and provider.has_quota():
                return provider
        return None
```

---

# 5. API Key Rotation (Plan 2)

```python
# api/key_manager.py
class APIKeyManager:
    def __init__(self):
        self.keys = self.load_keys()
        self.usage_tracker = UsageTracker()
        self.rotation_threshold = 0.8  # 80%

    def get_key(self, provider):
        key_data = self.keys.get(provider)
        if not key_data:
            return None

        # Check usage ratio
        usage_ratio = key_data['used'] / key_data['limit']

        if usage_ratio >= self.rotation_threshold:
            # Try next provider
            next_provider = self.get_next_provider(provider)
            if next_provider:
                return self.get_key(next_provider)
            # All exhausted, use local
            return 'LOCAL_FALLBACK'

        key_data['used'] += 1
        return key_data['key']

    def get_next_provider(self, current):
        idx = PROVIDER_PRIORITY.index(current)
        if idx + 1 < len(PROVIDER_PRIORITY):
            return PROVIDER_PRIORITY[idx + 1]
        return None
```

---

# 6. Environment Configuration (.env.example)

```bash
# Local Models (Ollama)
OLLAMA_HOST=http://localhost:11434

# External API Keys (Free tiers)
OPENAI_API_KEY=sk-your-openai-key
ANTHROPIC_API_KEY=sk-ant-your-anthropic-key
GOOGLE_API_KEY=your-google-api-key
GROQ_API_KEY=gsk-your-groq-key
TOGETHER_API_KEY=your-together-key
MISTRAL_API_KEY=your-mistral-key

# Database
DATABASE_URL=sqlite:///supremeai.db

# Application
DEBUG=False
SECRET_KEY=your-secret-key
PORT=8000

# Features
ENABLE_LOCAL_MODELS=True
ENABLE_EXTERNAL_APIS=True
ENABLE_VOICE=False
ENABLE_VISION=False
AUTO_APPROVE=False
```

---

# 7. Docker Compose (Optional)

```yaml
version: '3.8'
services:
  supremeai:
    build: .
    ports:
      - "8000:8000"
    environment:
      - DATABASE_URL=sqlite:///data/supremeai.db
    volumes:
      - ./data:/app/data
    depends_on:
      - ollama

  ollama:
    image: ollama/ollama
    volumes:
      - ollama:/root/.ollama
    ports:
      - "11434:11434"

  redis:
    image: redis:alpine
    ports:
      - "6379:6379"

volumes:
  ollama:
```

---

# 8. Quick Start Commands

```bash
# 1. Clone and setup
git clone https://github.com/paykaribazaronline/supremeai
cd supremeai
cp .env.example .env
# Edit .env with your API keys

# 2. Install dependencies
pip install -r requirements.txt

# 3. Provision Cloud Services (Scale-to-Zero)
gcloud run deploy qwen-coder --image gcr.io/$PROJECT_ID/qwen-coder --gpu 1 --no-cpu-throttling --min-instances 0
gcloud run deploy llama-general --image gcr.io/$PROJECT_ID/llama-general --gpu 1 --min-instances 0
# Repeat for all 5 models

# 4. Run tests
pytest

# 5. Start server
python src/main.py
```

---

# 9. Model Usage Decision Tree

```
User Request
    │
    ├── External APIs (Level 1) ──→ Groq/Google/OpenAI
    │   └── If disconnected/fail? (Level 2)
    │       └── Trigger Cloud Run (Level 3)
    │           ├── Code? ──→ Qwen 2.5 Coder (Cloud)
    │           ├── Debug? ──→ DeepSeek-V4-Pro (Cloud)
    │           ├── Chat? ──→ Llama 3.1 8B (Cloud)
    │           └── Embed? ──→ Nomic Embed (Cloud)
    │
    └── All Fail? ──→ Notify Admin / Queue
```

---

# 10. Monitoring Dashboard

```
┌─────────────────────────────────────┐
│  SupremeAI Dashboard                 │
├─────────────────────────────────────┤
│                                      │
│  Local Models:                       │
│  [🟢] Qwen 2.5 Coder 7B  - Active   │
│  [🟢] Llama 3.1 8B       - Active   │
│  [🟡] DeepSeek Coder 6.7B - Busy    │
│  [🟢] Phi 3 Mini         - Idle     │
│  [🟢] Nomic Embed        - Active   │
│                                      │
│  External APIs:                      │
│  [🟢] Groq        - 80% quota left  │
│  [🟢] Google      - Unlimited       │
│  [🟡] OpenAI      - $2.50 left      │
│  [🔴] Anthropic   - Exhausted       │
│                                      │
│  Active Users: 12                    │
│  Queue: 3 requests                   │
│  Avg Response: 2.3s                  │
│                                      │
└─────────────────────────────────────┘
```

---

**Document Status:** Ready for Implementation
**Next Step:** Start with local models + 1-2 free APIs
