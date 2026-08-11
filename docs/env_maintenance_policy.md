# SupremeAI 2.0 - Environment & Secret Maintenance Policy (Single Source of Truth)

এই ডকুমেন্টটি SupremeAI 2.0-এর নতুন "Hybrid Secret Architecture"-এর সম্পূর্ণ নির্দেশিকা এবং **Single Source of Truth**। কোন কি (key) কোথায় থাকবে তার পূর্ণাঙ্গ তালিকা এখানে দেওয়া হলো।

## 🏗️ মূল আর্কিটেকচার (Hybrid Approach)
সিকিউরিটি, স্কেলাবিলিটি এবং মেইনটেইনবিলিটির কথা মাথায় রেখে প্রোজেক্টের সিক্রেটগুলোকে **দুটি ভাগে** ভাগ করা হয়েছে:

1. **Infisical Vault (Central Truth):** সমস্ত API Keys, Database URLs, Tokens, Webhook Secrets ইত্যাদি যা এনভায়রনমেন্ট ভেদে পরিবর্তন হয় না। 
2. **Environment-Specific `.env` (Local to Service):** শুধুমাত্র সেই সব ভ্যালু যেগুলো সার্ভিস-স্পেসিফিক বা বুটস্ট্র্যাপ করার জন্য অপরিহার্য।

---

## 🔒 Category 1: Infisical Vault (Shared Secrets)

যেসব সিক্রেট সব এনভায়রনমেন্টে (বা একাধিক এনভায়রনমেন্টে) **একই ভ্যালু** শেয়ার করে, সেগুলো শুধু Infisical-এ থাকবে। 

**✅ Infisical-এ আপলোড করা সিক্রেটগুলোর পূর্ণাঙ্গ তালিকা (Verified):**
- `ADMIN_AUTHORIZED`
- `ADMIN_EMAILS`
- `ADMIN_NOTIFICATION_EMAIL`
- `ALLOW_TEST_AUTH_BYPASS`
- `ALLOW_TEST_ORIGIN_BYPASS`
- `API_KEY_SIGNING_SECRET`
- `API_V1_STR`
- `AUTOFIX_AUTHORIZED`
- `CI_WEBHOOK_SECRET`
- `CLOUDFLARE_API_KEY`
- `CLOUDFLARE_EMAIL`
- `DISCORD_OTP_WEBHOOK_URL`
- `DISCORD_WEBHOOK_URL`
- `DOCS_PASSWORD`
- `ENCRYPTION_KEY`
- `FIREBASE_SERVICE_ACCOUNT_JSON`
- `FIRECRAWL_API_KEY`
- `GCP_KMS_KEY_RING`
- `GEMINI_API_KEY`
- `GITHUB_API_TOKEN`
- `GITHUB_CLIENT_ID`
- `GITHUB_PAT_AUTO_FIX`
- `GITHUB_PAT_MODELS`
- `GITHUB_PAT_NILOYJOY7`
- `GITHUB_TOKEN`
- `GROQ_API_KEY`
- `JIT_OTP_SECRET`
- `LAUNCHDARKLY_API_KEY`
- `LOW_MEMORY_MODE`
- `MISTRAL_API_KEY`
- `OPENHANDS_API_KEY`
- `OPENROUTER_API_KEY`
- `PROJECT_NAME`
- `QDRANT_API_KEY`
- `QDRANT_URL`
- `REDIS_URL`
- `RENDER_API_KEY`
- `RENDER_API_KEY_BACKUP`
- `RESEND_API_KEY`
- `STRIPE_API_KEY`
- `STRIPE_PUBLISHABLE_KEY`
- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`
- `SUPABASE_ACCESS_TOKEN`
- `SUPABASE_DATABASE_URL`
- `SUPABASE_DATABASE_URL_POOLER`
- `SUPABASE_JWKS_URL`
- `SUPABASE_KEY`
- `SUPABASE_PUBLISHABLE_KEY`
- `SUPABASE_SECRET_KEY`
- `SUPABASE_SERVICE_ROLE_KEY`
- `SUPABASE_URL`
- `SUPREMEAI_ADMIN_PASSWORD_HASH`
- `SUPREMEAI_ADMIN_TOTP_SECRET`
- `SUPREMEAI_API_KEY`
- `SUPREMEAI_JWT_SECRET`
- `UPSTASH_REDIS_REST_TOKEN`
- `UPSTASH_REDIS_REST_URL`
- `VERCEL_TOKEN`
- `VITE_FIREBASE_API_KEY`
- `routeme_api_key`

> [!TIP]
> **কেন?** যদি কোনো API Key রোটেট (পরিবর্তন) করতে হয়, তাহলে শুধু Infisical-এ একবার আপডেট করলেই হবে। কোনো সার্ভিস রি-ডিপ্লয় করার দরকার নেই, Infisical SDK/Agent অটোমেটিক নতুন কি (key) ফেচ করে নেবে।

---



**❓ Missing / Pending Keys (ভ্যালু পাওয়া মাত্রই Infisical-এ অ্যাড করতে হবে):**
- `AIDER_API_KEY`
- `ALLOWED_TAKEOVER_TOKENS`
- `ANTHROPIC_API_KEY`
- `APP_STORE_CONNECT_API_KEY_CONTENT`
- `APP_STORE_CONNECT_API_KEY_ID`
- `AUTHORIZED_ADMINS`
- `BHASHA_BATCH_CONCURRENCY`
- `BHASHA_CACHE_TTL_HOURS`
- `BHASHA_MAX_CACHE`
- `BHASHA_MIN_QUALITY`
- `CLINE_API_KEY`
- `CLOUDFLARE_API_TOKEN`
- `CLOUDFLARE_WORKERS_API_TOKEN`
- `CODEIUM_API_KEY`
- `CONTINUE_API_KEY`
- `DB_PASSWORD`
- `DEEPSEEK_API_KEY`
- `DISCORD_ALERT_WEBHOOK`
- `DISCORD_BOT_TOKEN`
- `ENCRYPTION_KEYS`
- `GCP_ACCESS_TOKEN`
- `GCP_PROJECT_ID`
- `GCP_PUBSUB_SUBSCRIPTION`
- `GCP_PUBSUB_TOPIC`
- `GCP_SA_KEY`
- `GITHUB_CLIENT_SECRET`
- `GOOGLE_API_KEY`
- `GOOGLE_APPLICATION_CREDENTIALS`
- `GOOGLE_CLOUD_PROJECT`
- `GROQ_API_KEY_DEPLOYMENT_MONITOR`
- `HF_API_KEY`
- `JWT_SECRET`
- `KMS_KEY_NAME`
- `LANGSMITH_API_KEY`
- `LAUNCHDARKLY_AI_CONFIG_KEY`
- `LAUNCHDARKLY_SDK_KEY`
- `LOAD_TEST_TOKEN`
- `MINIO_ACCESS_KEY`
- `MINIO_SECRET_KEY`
- `MIRROR_REPO_TOKEN`
- `MOONSHOT_API_KEY`
- `NATS_TOKEN`
- `NEO4J_PASSWORD`
- `NEO4J_URI`
- `NEO4J_USER`
- `NETLIFY_AUTH_TOKEN`
- `OPENAI_API_KEY`
- `ORACLE_CLOUD_API_KEY`
- `PINECONE_API_KEY`
- `PLANDEX_API_KEY`
- `POSTHOG_API_KEY`
- `PYTHAGORA_API_KEY`
- `R2_ACCESS_KEY`
- `R2_SECRET_KEY`
- `RAILWAY_TOKEN`
- `SECONDARY_SERVICE_ACCOUNT_KEY`
- `SECRET`
- `SECRET_BACKEND`
- `SECRET_CACHE_TTL`
- `SENTRY_AUTH_TOKEN`
- `SLACK_BOT_TOKEN`
- `STAGING_REPO_TOKEN`
- `SUPREMEAI_CREDENTIAL_ENC_KEY`
- `SUPREMEAI_EMAIL_PASSWORD`
- `SUPREMEAI_GITHUB_TOKEN`
- `TELEGRAM_BOT_TOKEN`
- `TEST_VAULT_KEY`
- `TOGETHER_API_KEY`
- `VERCEL_OIDC_TOKEN`

---

## ⚠️ Category 2: Environment-Specific Keys (Local `.env`)

যেসব সিক্রেট Infisical-এ রাখলে অ্যাপ **ব্রেক** করবে বা যেগুলো এনভায়রনমেন্টের ওপর ভিত্তি করে ডাইনামিক, সেগুলো সার্ভিসের নিজস্ব প্ল্যাটফর্মে বা লোকাল ফাইলে থাকতে হবে।

**✅ এনভায়রনমেন্ট অনুযায়ী আলাদা থাকা সিক্রেটগুলোর পূর্ণাঙ্গ তালিকা:**

*(বি.দ্র. প্রতিটি ফাইলে বুটস্ট্র্যাপ করার জন্য `INFISICAL_CLIENT_ID`, `INFISICAL_CLIENT_SECRET`, `INFISICAL_PROJECT_ID`, `INFISICAL_ENV` কমনভাবে থাকবে, তাই নিচের তালিকায় সেগুলো দেখানো হয়নি)*

### ১. Render Backend (`render-backend.env`)
- `PORT`
- `ENV`
- `NODE_ENV`
- `SERVICE_ROLE` = `user`
- `ALLOWED_HOSTS`
- `CORS_ORIGINS`
- `USER_CORS_ORIGINS`
- `ADMIN_CORS_ORIGINS`
- `SUPREMEAI_USER_BACKEND_URL`
- `SUPREMEAI_ADMIN_BACKEND_URL`
- `CHECKOUT_BASE_URL`
- `CHROMADB_PATH`
- `EXPERIENCE_DB_PATH`
- `OLLAMA_URL`
- `RENDER_DEPLOY_HOOK_URL`

### ২. Render Admin (`render-admin.env`)
- `PORT`
- `ENV`
- `NODE_ENV`
- `SERVICE_ROLE` = `admin`
- `ALLOWED_HOSTS`
- `CORS_ORIGINS`
- `USER_CORS_ORIGINS`
- `ADMIN_CORS_ORIGINS`
- `SUPREMEAI_USER_BACKEND_URL`
- `SUPREMEAI_ADMIN_BACKEND_URL`
- `CHECKOUT_BASE_URL`
- `CHROMADB_PATH`
- `EXPERIENCE_DB_PATH`
- `OLLAMA_URL`

### ৩. Render Studio Client (`render-studio-client.env`)
- `PORT`
- `ENV`
- `NODE_ENV`
- `VITE_PRIMARY_BACKEND`
- `VITE_SECONDARY_BACKEND`

### ৪. Vercel Frontend (`vercel.env`)
- `NODE_ENV`
- `VERCEL_ORG_ID`
- `VERCEL_PROJECT_ID`
- `VITE_PRIMARY_BACKEND`
- `VITE_SECONDARY_BACKEND`

**❓ Missing / Pending Keys (শুধু Vercel Frontend-এর জন্য):**
- `VITE_ADMIN_BACKEND`
- `VITE_API_BASE`
- `VITE_API_BASE_URL`
- `VITE_API_URL`
- `VITE_FIREBASE_APP_ID`
- `VITE_FIREBASE_AUTH_DOMAIN`
- `VITE_FIREBASE_MESSAGING_SENDER_ID`
- `VITE_FIREBASE_PROJECT_ID`
- `VITE_FIREBASE_STORAGE_BUCKET`
- `VITE_SUPABASE_ANON_KEY`
- `VITE_SUPABASE_URL`
- `VITE_USER_BACKEND`

### ৫. GitHub Actions Primary (`github-primary.env`) & Secondary (`github-secondary.env`)

> [!IMPORTANT]
> `MAIN_REPO_TOKEN` — Primary GitHub অ্যাকাউন্ট থেকে Secondary/Target রেপোতে মিরর/পুশ করার জন্য এই PAT অপরিহার্য।

**✅ Configured Keys:**
- `ENV`
- `NODE_ENV`
- `RENDER_DEPLOY_HOOK_URL`
- `VERCEL_ORG_ID`
- `VERCEL_PROJECT_ID`
- `INFISICAL_CLIENT_ID`
- `INFISICAL_CLIENT_SECRET`
- `INFISICAL_PROJECT_ID`
- `INFISICAL_TOKEN`

**✅ GitHub Repository Secrets (Platform-level — GitHub Secrets UI-তে সেট করতে হবে):**
- `MAIN_REPO_TOKEN` — Primary → Target রেপো পুশ/মিরর করার PAT *(⚠️ এটি Infisical-এ নয়, GitHub Secrets-এ থাকবে)*
- `MIRROR_REPO_TOKEN` — Secondary রেপো মিরর PAT
- `STAGING_REPO_TOKEN` — Staging রেপো অ্যাক্সেস PAT
- `FIREBASE_SERVICE_ACCOUNT_SUPREMEAI_A` — Firebase Hosting CI Deploy Service Account JSON
- `GCP_SA_KEY` — Google Cloud Platform Service Account Key
- `GCP_PROJECT_ID` — GCP প্রজেক্ট ID
- `SENTRY_AUTH_TOKEN` — Sentry Error Monitoring Upload Token

**❓ Missing / Pending Keys (ভ্যালু পাওয়া মাত্রই GitHub Secrets-এ অ্যাড করতে হবে):**
- `ANDROID_KEYSTORE_BASE64` — Android APK Signing Keystore (Base64)
- `ANDROID_KEY_ALIAS` — Android Key Alias
- `ANDROID_KEY_PASSWORD` — Android Key Password
- `ANDROID_STORE_PASSWORD` — Android Store Password
- `APP_STORE_CONNECT_API_KEY_CONTENT` — iOS App Store Connect API Key Content
- `APP_STORE_CONNECT_API_KEY_ID` — iOS App Store Connect API Key ID
- `APP_STORE_CONNECT_API_ISSUER_ID` — iOS App Store Connect API Issuer ID
- `CLOUDFLARE_ZONE_ID` — Cloudflare Zone ID
- `CLOUDFLARE_API_TOKEN` — Cloudflare API Token
- `CLOUDFLARE_WORKERS_API_TOKEN` — Cloudflare Workers API Token
- `FIREBASE_SERVICE_ACCOUNT` — Firebase Service Account (Generic)
- `NETLIFY_AUTH_TOKEN` — Netlify Auth Token
- `NETLIFY_SITE_ID` — Netlify Site ID
- `PLAY_STORE_CONFIG_JSON` — Google Play Store Config JSON
- `RENDER_BACKUP_SVC_ID` — Render Backup Service ID
- `RENDER_DEPLOY_HOOK_URL_BACKUP` — Render Backup Deploy Hook URL
- `RENDER_PRIMARY_SVC_ID` — Render Primary Service ID
- `RENDER_WORKER_SVC_ID` — Render Worker Service ID
- `SUPREMEAI_GITHUB_TOKEN` — SupremeAI-specific GitHub PAT
- `VERCEL_OIDC_TOKEN` — Vercel OIDC Token

### ৬. Local Development (`local.env`)
- (Render Backend-এর সব কি, তবে ভ্যালুগুলো হবে `localhost` বা লোকাল পাথ)

---

## 🚀 নতুন সিক্রেট অ্যাড করার নিয়ম (Workflow)

যখনই প্রোজেক্টে নতুন কোনো API Key বা Secret অ্যাড করার প্রয়োজন হবে, তখন নিচের ফ্লো অনুসরণ করতে হবে:

1. **যাচাই করুন:** সিক্রেটটি কি সব জায়গায় একই? (যেমন: `NEW_AI_API_KEY`)
   - **হ্যাঁ:** সরাসরি Infisical ড্যাশবোর্ডে গিয়ে `prod` এবং `dev` এনভায়রনমেন্টে অ্যাড করে দিন। লোকাল `.env`-এ অ্যাড করার দরকার নেই।
   - **না:** এটি কি সার্ভার-স্পেসিফিক? (যেমন: `NEW_SERVER_PORT`) তাহলে এটি Infisical-এ অ্যাড করবেন না। যে যে সার্ভিসের জন্য প্রযোজ্য, শুধু তাদের `envs/` ফাইলে আপডেট করুন এবং ঐ নির্দিষ্ট ক্লাউড প্ল্যাটফর্মের (Render/Vercel) সেটিংসে গিয়ে ভ্যালু অ্যাড করুন।

2. **Group 2 Prevention Rule:** কখনোই লোকাল রুট `.env` ফাইলটি সরাসরি কপি-পেস্ট করে Infisical-এ ইমপোর্ট করবেন না। এতে লোকাল `PORT` বা `NODE_ENV=development` প্রোডাকশনে চলে যেতে পারে।

> [!WARNING]
> **Strict Restriction:** 
> `PORT`, `NODE_ENV`, `INFISICAL_*` — এই প্রিফিক্স/নামের কোনো ভেরিয়েবল কখনোই Infisical Vault-এর ভেতরে ಸೇভ করা যাবে না।
