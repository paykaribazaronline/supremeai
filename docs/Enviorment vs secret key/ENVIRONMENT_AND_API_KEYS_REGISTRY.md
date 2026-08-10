# 🗝️ SupremeAI 2.0 — Multi-Platform Master Matrix (Table View)

_Status: ACTIVE_  
_Last Updated: 2026-07-27_

---

## 📌 Master Environment Matrix (সব প্ল্যাটফর্মের সংকলন টেবিল)

নিচের টেবিলে **SupremeAI 2.0** ইকোসিস্টেমের সকল এনভায়রনমেন্ট ভেরিয়েবল এবং কোন কোন প্ল্যাটফর্মে সেই ভেরিয়েবলটি থাকা **REQUIRED (বাধ্যতামূলক ✅)**, **OPTIONAL (ঐচ্ছিক 🟡)**, অথবা **NOT APPLICABLE (প্রযোজ্য নয় ❌)** তা একনজরে দেখানো হলো:

### 🔐 Core Authentication & Security
| Environment Variable / Secret Name | Render Backend | Render Admin | Vercel / Netlify | Cloudflare | Firebase / GCP | GitHub Actions | Infisical | .env File | Description (বাংলা বিবরণ) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **`ENV`** | ✅ MUST | ✅ MUST | 🟡 Opt | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Environment name (`production`/`staging`) |
| **`SUPREMEAI_JWT_SECRET`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | JWT token secret |
| **`ENCRYPTION_KEY`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Data encryption key |
| **`ENCRYPTION_KEY`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Payload encryption key |
| **`SUPREMEAI_ADMIN_PASSWORD_HASH`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Hashed admin password |
| **`SUPREMEAI_ADMIN_TOTP_SECRET`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Admin 2FA TOTP secret |
| **`SUPREMEAI_API_KEY`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Primary API authentication token |
| **`SUPREMEAI_API_KEY`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Primary API authentication token (first key) |
| **`SUPREMEAI_DOCS_PASSWORD`** | ❌ | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Admin docs protected password |
| **`SUPREMEAI_DOCS_USERNAME`** | ❌ | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Admin docs username |
| **`ADMIN_AUTHORIZED`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Admin authorization flag |
| **`AUTOFIX_AUTHORIZED`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Auto-fix authorization flag |

### 🗄️ Database & Storage
| Environment Variable / Secret Name | Render Backend | Render Admin | Vercel / Netlify | Cloudflare | Firebase / GCP | GitHub Actions | Infisical | .env File | Description (বাংলা বিবরণ) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **`SUPABASE_URL`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Supabase API endpoint |
| **`SUPABASE_KEY`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Supabase public client key |
| **`SUPABASE_SECRET_KEY`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Supabase admin secret key |
| **`SUPABASE_DATABASE_URL_POOLER`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | PostgreSQL PgBouncer URL |
| **`SUPABASE_DATABASE_URL`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | PostgreSQL Database URL |
| **`SUPABASE_JWKS_URL`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Supabase JWKS URL |
| **`SUPABASE_ACCESS_TOKEN`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Supabase access token |
| **`REDIS_URL`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Upstash Redis connection string |
| **`UPSTASH_REDIS_REST_URL`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Upstash REST API URL |
| **`UPSTASH_REDIS_REST_TOKEN`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Upstash REST bearer token |
| **`QDRANT_URL`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Qdrant vector database URL |
| **`QDRANT_API_KEY`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Qdrant vector database API key |
| **`NEO4J_URI`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Neo4j graph database URI |
| **`NEO4J_USER`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Neo4j database username |
| **`NEO4J_PASSWORD`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Neo4j database password |
| **`EXPERIENCE_DB_PATH`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Experience database path |
| **`CHROMADB_PATH`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | ChromaDB vector database path |

### 🤖 AI & LLM Services
| Environment Variable / Secret Name | Render Backend | Render Admin | Vercel / Netlify | Cloudflare | Firebase / GCP | GitHub Actions | Infisical | .env File | Description (বাংলা বিবরণ) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **`OPENROUTER_API_KEY`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | OpenRouter model hub key |
| **`DEEPSEEK_API_KEY`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | DeepSeek code reasoning key |
| **`GEMINI_API_KEY`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Google Gemini API key |
| **`GROQ_API_KEY`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Groq ultra-fast Llama-3 key |
| **`NVIDIA_API_KEY`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | NVIDIA NIM inference key |
| **`OPENAI_API_KEY`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | OpenAI GPT-4o key |
| **`ANTHROPIC_API_KEY`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Anthropic Claude 3.5 key |
| **`HF_API_KEY`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | HuggingFace open models key |
| **`FIRECRAWL_API_KEY`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Firecrawl web scraper key |
| **`DEVIN_API_KEY`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Devin autonomous coding agent key |
| **`GEMINI_MODEL_NAME`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Google Gemini model name |
| **`CLAUDE_OPENROUTER_MODEL`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Claude model name for OpenRouter |
| **`MODEL_ID`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Model ID for Hugging Face Space |

### 💰 Payment & Billing
| Environment Variable / Secret Name | Render Backend | Render Admin | Vercel / Netlify | Cloudflare | Firebase / GCP | GitHub Actions | Infisical | .env File | Description (বাংলা বিবরণ) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **`STRIPE_API_KEY`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Stripe billing secret key |
| **`STRIPE_PUBLISHABLE_KEY`** | ✅ MUST | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Stripe public client key |
| **`STRIPE_WEBHOOK_SECRET`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Stripe webhook signature secret |
| **`STRIPE_AGENT_API_KEY`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Stripe agent API key |

### 📧 Communication & Notifications
| Environment Variable / Secret Name | Render Backend | Render Admin | Vercel / Netlify | Cloudflare | Firebase / GCP | GitHub Actions | Infisical | .env File | Description (বাংলা বিবরণ) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **`TWILIO_ACCOUNT_SID`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Twilio SMS service account SID |
| **`TWILIO_AUTH_TOKEN`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Twilio SMS service authentication token |
| **`DISCORD_WEBHOOK_URL`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Discord notification webhook URL |
| **`DISCORD_BOT_TOKEN`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Discord bot token |
| **`DISCORD_OTP_WEBHOOK_URL`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Discord OTP verification webhook URL |
| **`DISCORD_APP_ID`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Discord application ID |
| **`DISCORD_PUBLIC_KEY`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Discord public key |
| **`SLACK_WEBHOOK_URL`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Slack notification webhook URL |
| **`RESEND_API_KEY`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Resend email service API key |

### 🌐 Platform Integration & Deployment
| Environment Variable / Secret Name | Render Backend | Render Admin | Vercel / Netlify | Cloudflare | Firebase / GCP | GitHub Actions | Infisical | .env File | Description (বাংলা বিবরণ) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **`GITHUB_TOKEN` / `GITHUB_API_TOKEN`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ✅ MUST | ✅ MUST | ✅ | GitHub API automation token |
| **`SUPREMEAI_GITHUB_TOKEN`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ✅ MUST | ✅ MUST | ❌ | GitHub API automation token (extended) |
| **`GITHUB_CLIENT_ID`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | GitHub OAuth client ID |
| **`GITHUB_CLIENT_SECRET`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | GitHub OAuth client secret |
| **`RENDER_API_KEY`** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ MUST | ✅ | Primary Render API token |
| **`RENDER_API_KEY_BACKUP`** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ MUST | ✅ | Admin Render API token |
| **`RENDER_DEPLOY_HOOK_URL`** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ MUST | ✅ | Render deploy hook URL |
| **`RENDER_DEPLOY_HOOK_URL_BACKUP`** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ MUST | ❌ | Backup Render deploy hook URL |
| **`RENDER_SERVICE_ID`** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ MUST | ❌ | Render service ID |
| **`VERCEL_TOKEN`** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ MUST | ❌ | Vercel deployment token |
| **`VERCEL_PROJECT_ID`** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ MUST | ❌ | Vercel project ID |
| **`VERCEL_ORG_ID`** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ MUST | ❌ | Vercel organization ID |
| **`VERCEL_OIDC_TOKEN`** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ MUST | ❌ | Vercel OIDC token |
| **`NETLIFY_AUTH_TOKEN`** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ MUST | ❌ | Netlify authentication token |
| **`NETLIFY_SITE_ID`** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ MUST | ❌ | Netlify site ID |
| **`CLOUDFLARE_API_TOKEN`** | ❌ | ❌ | ❌ | ✅ MUST | ❌ | ❌ | ✅ MUST | ❌ | Cloudflare zone edit token |
| **`CLOUDFLARE_API_KEY`** | ❌ | ❌ | ❌ | ✅ MUST | ❌ | ❌ | ✅ MUST | ❌ | Cloudflare API key |
| **`CLOUDFLARE_ZONE_ID`** | ❌ | ❌ | ❌ | ✅ MUST | ❌ | ❌ | ✅ MUST | ❌ | Cloudflare domain zone ID |
| **`CLOUDFLARE_WORKERS_API_TOKEN`** | ❌ | ❌ | ❌ | ✅ MUST | ❌ | ❌ | ✅ MUST | ❌ | Cloudflare Workers API token |

### ☁️ Cloud & Infrastructure
| Environment Variable / Secret Name | Render Backend | Render Admin | Vercel / Netlify | Cloudflare | Firebase / GCP | GitHub Actions | Infisical | .env File | Description (বাংলা বিবরণ) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **`FIREBASE_SERVICE_ACCOUNT_JSON`**| ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | ✅ MUST | ❌ | Firebase Admin SDK JSON |
| **`FIRESTORE_PRIVATE_KEY`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ✅ MUST | ❌ | ✅ MUST | ❌ | Firestore private key |
| **`FIREBASE_SERVICE_ACCOUNT_PATH`** | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | ❌ | ❌ | Firebase service account file path |
| **`GCP_KMS_KEY_RING`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ✅ MUST | ❌ | ✅ MUST | ✅ | GCP KMS key ring name |
| **`GCP_PROJECT_ID`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ✅ MUST | ❌ | ✅ MUST | ❌ | Google Cloud Platform project ID |
| **`GCP_REGION`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ✅ MUST | ❌ | ✅ MUST | ❌ | Google Cloud Platform region |
| **`GOOGLE_CLOUD_PROJECT`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ✅ MUST | ❌ | ✅ MUST | ❌ | Google Cloud project ID (alternative) |
| **`EVOLUTION_DB_PATH_GCS`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ✅ MUST | ❌ | ✅ MUST | ❌ | Evolution engine DB path (GCS) |
| **`GCP_FIRESTORE_SQLITE_PATH`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ✅ MUST | ❌ | ✅ MUST | ❌ | Firestore SQLite database path |

### 🛠️ Monitoring & Observability
| Environment Variable / Secret Name | Render Backend | Render Admin | Vercel / Netlify | Cloudflare | Firebase / GCP | GitHub Actions | Infisical | .env File | Description (বাংলা বিবরণ) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **`SENTRY_DSN`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Sentry error monitoring DSN |
| **`SENTRY_AUTH_TOKEN`** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | ❌ | Sentry authentication token |
| **`PROMETHEUS_METRICS_PORT`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Prometheus metrics port |
| **`LANGSMITH_API_KEY`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | LangSmith tracing & evaluation API key |
| **`LAUNCHDARKLY_SDK_KEY`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | LaunchDarkly feature flag SDK key |
| **`LAUNCHDARKLY_API_KEY`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | LaunchDarkly API key |

### 🎥 Media & Video Services
| Environment Variable / Secret Name | Render Backend | Render Admin | Vercel / Netlify | Cloudflare | Firebase / GCP | GitHub Actions | Infisical | .env File | Description (বাংলা বিবরণ) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **`RUNWAY_API_KEY`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | 🟡 Opt | ❌ | Runway AI video generation key |
| **`KLING_API_KEY`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | 🟡 Opt | ❌ | Kling AI video generation key |
| **`RUNPOD_API_KEY`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | 🟡 Opt | ❌ | RunPod GPU training key |

### 🌐 Frontend & Client
| Environment Variable / Secret Name | Render Backend | Render Admin | Vercel / Netlify | Cloudflare | Firebase / GCP | GitHub Actions | Infisical | .env File | Description (বাংলা বিবরণ) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **`VITE_API_BASE_URL`** | ❌ | ❌ | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ❌ | Frontend base backend URL |
| **`VITE_SUPABASE_URL`** | ❌ | ❌ | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ❌ | Client-side Supabase URL |
| **`VITE_SUPABASE_ANON_KEY`** | ❌ | ❌ | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ❌ | Client-side Supabase key |
| **`VITE_API_BASE`** | ❌ | ❌ | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ❌ | Frontend API base URL |

### 🔐 Security & Management
| Environment Variable / Secret Name | Render Backend | Render Admin | Vercel / Netlify | Cloudflare | Firebase / GCP | GitHub Actions | Infisical | .env File | Description (বাংলা বিবরণ) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **`CI_WEBHOOK_SECRET`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ✅ MUST | ✅ MUST | ✅ | CI webhook signature secret |
| **`ADMIN_NOTIFICATION_EMAIL`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Security notification email |
| **`ADMIN_EMAILS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Admin notification email addresses |
| **`SERVICE_ROLE`** | ❌ | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | Admin service role flag (`admin`) |
| **`DOCS_PASSWORD`** | ❌ | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Admin docs protected password |
| **`INFISICAL_TOKEN`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Infisical project access token |
| **`INFISICAL_CLIENT_SECRET`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Infisical client secret |

### ⚙️ System Configuration
| Environment Variable / Secret Name | Render Backend | Render Admin | Vercel / Netlify | Cloudflare | Firebase / GCP | GitHub Actions | Infisical | .env File | Description (বাংলা বিবরণ) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **`SUPREMEAI_ENV`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | SupremeAI environment identifier |
| **`SUPREMEAI_DEFAULT_ENV`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Default SupremeAI environment |
| **`SUPREMEAI_PUBLIC_PATHS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | SupremeAI publicly accessible paths |
| **`ALLOWED_HOSTS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Allowed hosts for the application |
| **`OTP_COOLDOWN_SECONDS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | OTP cooldown period in seconds |
| **`SECURITY_CONTEXT_TTL`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Security context time-to-live |
| **`SECURITY_CAUTION_LOG_TTL`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Security caution log time-to-live |
| **`SUPREMEAI_PROXIES`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | SupremeAI proxy configuration |
| **`SUPREMEAI_BASE_DIR`** | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | SupremeAI base directory path |
| **`SUPREMEAI_DATA_DIR`** | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | SupremeAI data directory path |
| **`SUPREMEAI_MEMORY_FILE_PATH`** | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | SupremeAI memory vault file path |
| **`PROJECT_NAME`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Project name |
| **`PORT`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Server port |
| **`API_V1_STR`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | API version string |
| **`CORS_ORIGINS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | CORS allowed origins |
| **`HOST`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Server host address |

### 🤖 AI Agent Configuration
| Environment Variable / Secret Name | Render Backend | Render Admin | Vercel / Netlify | Cloudflare | Firebase / GCP | GitHub Actions | Infisical | .env File | Description (বাংলা বিবরণ) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **`OLLAMA_URL`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Ollama service URL |
| **`CHECKOUT_BASE_URL`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Checkout base URL |
| **`TIER8_AUTO_START`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Tier 8 auto-start flag |
| **`SWARM_MODEL`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Swarm model identifier |
| **`SWARM_HEARTBEAT_INTERVAL`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Swarm heartbeat interval |
| **`SWARM_DEFAULT_CONSENSUS`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Swarm default consensus |
| **`SWARM_BYZANTINE_TOLERANCE`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Swarm Byzantine tolerance |
| **`SWARM_AGENT_TIMEOUT`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Swarm agent timeout |
| **`SELF_IMPROVE_SCAN_INTERVAL`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Self-improve scan interval |
| **`SELF_IMPROVE_NESTING_THRESHOLD`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Self-improve nesting threshold |
| **`SELF_IMPROVE_MODEL`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Self-improve model |
| **`SELF_IMPROVE_MIN_CONFIDENCE`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Self-improve min confidence |
| **`SELF_IMPROVE_MAX_PROPOSALS`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Self-improve max proposals |
| **`SELF_IMPROVE_LONG_FUNC_THRESHOLD`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Self-improve long function threshold |
| **`MARKETPLACE_REVIEW_REQUIRED`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Marketplace review required flag |
| **`MARKETPLACE_REVIEW_MODEL`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Marketplace review model |
| **`MARKETPLACE_MIN_RATING`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Marketplace minimum rating |
| **`MARKETPLACE_AUTO_CURATE`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Marketplace auto-curate flag |
| **`EVO_SELECTION_PRESSURE`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Evolution selection pressure |
| **`EVO_POPULATION_SIZE`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Evolution population size |
| **`EVO_MUTATION_RATE`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Evolution mutation rate |
| **`EVO_MODEL`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Evolution model |
| **`EVO_MAX_GENERATIONS`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Evolution max generations |
| **`EVO_FITNESS_THRESHOLD`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Evolution fitness threshold |
| **`EVO_BENCHMARK_PROMPT`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Evolution benchmark prompt |
| **`EVO_BENCHMARK_EXPECTED`** | 🟡 Opt | 🟡 Opt | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Evolution benchmark expected result |

### 🧪 Testing & Development
| Environment Variable / Secret Name | Render Backend | Render Admin | Vercel / Netlify | Cloudflare | Firebase / GCP | GitHub Actions | Infisical | .env File | Description (বাংলা বিবরণ) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **`ALLOW_TEST_AUTH_BYPASS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Allow auth bypass in test environment |
| **`ALLOW_TEST_ORIGIN_BYPASS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Allow origin bypass in test environment |
| **`PYTEST_CURRENT_TEST`** | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | Pytest current test indicator |
| **`CI`** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | CI environment indicator |
| **`GITHUB_ACTIONS`** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | GitHub Actions indicator |

### 🚀 Performance & Limits
| Environment Variable / Secret Name | Render Backend | Render Admin | Vercel / Netlify | Cloudflare | Firebase / GCP | GitHub Actions | Infisical | .env File | Description (বাংলা বিবরণ) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **`LLM_CONNECT_TIMEOUT`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | LLM connection timeout |
| **`LLM_READ_TIMEOUT`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | LLM read timeout |
| **`LLM_WRITE_TIMEOUT`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | LLM write timeout |
| **`LLM_POOL_TIMEOUT`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | LLM pool timeout |
| **`LLM_MAX_CONNECTIONS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | LLM max connections |
| **`LLM_MAX_KEEPALIVE`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | LLM max keepalive connections |
| **`LATENCY_WINDOW_SIZE`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Latency window size |
| **`LATENCY_NORMALIZATION_MS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Latency normalization milliseconds |
| **`MIN_PROVIDER_WEIGHT`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Minimum provider weight |
| **`CIRCUIT_FAILURE_THRESHOLD`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Circuit breaker failure threshold |
| **`CIRCUIT_SUCCESS_RATE_FLOOR`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Circuit breaker success rate floor |
| **`CIRCUIT_COOLDOWN_SECONDS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Circuit breaker cooldown seconds |
| **`MAX_ROUTING_ATTEMPTS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Maximum routing attempts |
| **`GEMINI_RPM_LIMIT`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Gemini requests per minute limit |
| **`GEMINI_TPM_LIMIT`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Gemini tokens per minute limit |
| **`GEMINI_RPD_LIMIT`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Gemini requests per day limit |
| **`GROQ_RPM_LIMIT`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Groq requests per minute limit |
| **`GROQ_TPM_LIMIT`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Groq tokens per minute limit |
| **`GROQ_RPD_LIMIT`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Groq requests per day limit |
| **`OPENROUTER_RPM_LIMIT`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | OpenRouter requests per minute limit |
| **`OPENROUTER_RPD_LIMIT`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | OpenRouter requests per day limit |
| **`CLOUDFLARE_RPD_LIMIT`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Cloudflare requests per day limit |
| **`NVIDIA_RPM_LIMIT`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | NVIDIA requests per minute limit |
| **`NVIDIA_TPM_LIMIT`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | NVIDIA tokens per minute limit |
| **`HUGGINGFACE_RPM_LIMIT`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Hugging Face requests per minute limit |
| **`HUGGINGFACE_RPD_LIMIT`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Hugging Face requests per day limit |
| **`MAX_PROMPT_TOKENS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Maximum prompt tokens |
| **`MAX_RESPONSE_TOKENS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Maximum response tokens |
| **`MAX_COST_PER_TASK`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Maximum cost per task |
| **`MAX_AGENT_TOKENS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Maximum agent tokens |
| **`MAX_AGENT_ITERATIONS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Maximum agent iterations |
| **`LLM_COST_PER_TOKEN`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | LLM cost per token |

### 🔧 Advanced Configuration
| Environment Variable / Secret Name | Render Backend | Render Admin | Vercel / Netlify | Cloudflare | Firebase / GCP | GitHub Actions | Infisical | .env File | Description (বাংলা বিবরণ) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **`LOW_MEMORY_MODE`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ✅ | Low memory mode flag |
| **`ENABLE_TOKEN_COMPRESSION`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Enable token compression |
| **`AGENT_ADMIN_PERMISSIONS_REQUIRED`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Agent admin permissions required |
| **`AUTO_REMEDIATION_DRY_RUN`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Auto remediation dry run flag |
| **`ALLOW_SANDBOX_FALLBACK`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Allow sandbox fallback |
| **`ALLOW_LOCAL_SANDBOX_FALLBACK`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Allow local sandbox fallback |
| **`SANDBOX_ROOT`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Sandbox root directory |
| **`FIRECRACKER_PATH`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Firecracker binary path |
| **`GVISOR_PATH`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | gVisor binary path |
| **`HEALTH_CHECK_INTERVAL_SECONDS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Health check interval seconds |
| **`SKILL_TIMEOUT_SECONDS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Skill timeout seconds |
| **`TASK_RESULT_TTL_SECONDS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Task result TTL seconds |
| **`QUEUE_BACKEND_PRIORITY`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Queue backend priority |
| **`CIRCUIT_BREAKER_FAILURE_THRESHOLD`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Circuit breaker failure threshold |
| **`CIRCUIT_BREAKER_COOLDOWN_PERIOD`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Circuit breaker cooldown period |
| **`IDEMPOTENCY_CRITICAL_PATHS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Idempotency critical paths |
| **`PROMPT_BLOCKED_PATTERNS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Prompt blocked patterns |
| **`RBAC_ROLE_DEFINITIONS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | RBAC role definitions |
| **`USER_CORS_ORIGINS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | User CORS origins |
| **`ADMIN_CORS_ORIGINS`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Admin CORS origins |
| **`ENFORCE_ANTI_HACKING`** | ✅ MUST | ✅ MUST | ❌ | ❌ | ❌ | ❌ | ✅ MUST | ❌ | Enforce anti-hacking measures |

---

## 💡 Quick Rules Summary

1. **Backend & Admin (Render):** `.env` সিক্রেট ফাইল আপলোড করা সম্পন্ন। Render Backend এবং Render Admin উভয় সার্ভিসে ভেরিয়েবলগুলো লাইভ সিঙ্কড রয়েছে।
2. **Frontend Clients (Vercel / Netlify):** কেবল `VITE_` প্রিফিক্সড ক্লায়েন্ট ভেরিয়েবল ও কানেক্টেড কীসমূহ Vercel-এ সক্রিয় রয়েছে।
3. **CI/CD Automation (GitHub Actions):** `RENDER_API_KEY`, `VERCEL_TOKEN`, `NETLIFY_AUTH_TOKEN` এবং `CI_WEBHOOK_SECRET` সিক্রেট হিসেবে সেট করা রয়েছে।
