# SupremeAI 2.0 — Hybrid Secret Management Plan

_স্ট্যাটাস: খসড়া (Draft) | রিভিশন: ২ (User Feedback অনুযায়ী আপডেটেড)_

এই ডকুমেন্টে `.env` ফাইলে থাকা সকল key-কে তাদের সঠিক ম্যানেজমেন্ট স্ট্র্যাটেজি অনুযায়ী দুটি মূল গ্রুপে ভাগ করা হয়েছে।

---

## 🟢 গ্রুপ ১: Infisical থেকে ম্যানেজ করা হবে (Global / Shared Secrets)
আপনার প্রজেক্টের আর্কিটেকচার অনুযায়ী এই key-গুলোর ভ্যালু সব এনভায়রনমেন্টেই (প্রায়) একই থাকছে (যেহেতু Vercel/Render-এর নির্দিষ্ট একটাই মেইন প্রজেক্ট)। তাই এগুলো **সবই Infisical-এ** থাকবে। 

### 🔐 Authentication & Security
- `SUPREMEAI_JWT_SECRET`
- `SUPREMEAI_ADMIN_PASSWORD_HASH`
- `SUPREMEAI_ADMIN_TOTP_SECRET`
- `SUPREMEAI_API_KEY`
- `ENCRYPTION_KEY`
- `API_KEY_SIGNING_SECRET`
- `CI_WEBHOOK_SECRET`
- `JIT_OTP_SECRET`
- `DOCS_PASSWORD`

### 🤖 AI & LLM API Keys
- `GEMINI_API_KEY`
- `GROQ_API_KEY`
- `MISTRAL_API_KEY`
- `OPENROUTER_API_KEY`
- `FIRECRAWL_API_KEY`
- `OPENHANDS_API_KEY`
- `OLLAMA_URL`

### 🗄️ Database & Services (Supabase/Qdrant/Redis)
- `SUPABASE_URL`
- `SUPABASE_KEY`
- `SUPABASE_DATABASE_URL`
- `SUPABASE_DATABASE_URL_POOLER`
- `SUPABASE_SECRET_KEY`
- `SUPABASE_SERVICE_ROLE_KEY`
- `SUPABASE_ACCESS_TOKEN`
- `SUPABASE_JWKS_URL`
- `REDIS_URL`
- `UPSTASH_REDIS_REST_URL`
- `UPSTASH_REDIS_REST_TOKEN`
- `QDRANT_URL`
- `QDRANT_API_KEY`

### 💰 Payments, Automation & Third-Party
- `STRIPE_API_KEY`
- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`
- `STRIPE_PUBLISHABLE_KEY`
- `GITHUB_API_TOKEN` / `GITHUB_TOKEN`
- `RESEND_API_KEY`
- `LAUNCHDARKLY_API_KEY`
- `FIREBASE_SERVICE_ACCOUNT_JSON`
- `GCP_KMS_KEY_RING`
- `VERCEL_TOKEN` (যেহেতু একটাই Vercel প্রজেক্ট)
- `VERCEL_ORG_ID`
- `VERCEL_PROJECT_ID`
- `RENDER_API_KEY`
- `RENDER_API_KEY_BACKUP`
- `RENDER_DEPLOY_HOOK_URL`

### 🌐 Routing & Configuration (Project-wide uniform)
- `API_V1_STR`
- `ADMIN_AUTHORIZED`
- `AUTOFIX_AUTHORIZED`
- `ADMIN_EMAILS`
- `ADMIN_NOTIFICATION_EMAIL`
- `LOW_MEMORY_MODE`
- `ALLOW_TEST_AUTH_BYPASS`
- `ALLOW_TEST_ORIGIN_BYPASS`
- `ALLOWED_HOSTS`
- `CORS_ORIGINS`
- `USER_CORS_ORIGINS`
- `ADMIN_CORS_ORIGINS`
- `CHECKOUT_BASE_URL`
- `SUPREMEAI_USER_BACKEND_URL`
- `SUPREMEAI_ADMIN_BACKEND_URL`
- `VITE_PRIMARY_BACKEND`
- `VITE_SECONDARY_BACKEND`
- `DISCORD_WEBHOOK_URL`
- `DISCORD_OTP_WEBHOOK_URL`

*(**নোট:** যেহেতু এই কনফিগারেশনগুলোও সব এনভায়রনমেন্টে একই, তাই এগুলোকেও নিরাপদে Infisical-এ রাখা যাবে।)*

---

## 🛠️ গ্রুপ ২: ম্যানুয়ালি প্ল্যাটফর্মে অ্যাড করা হবে (Platform-Native Configs)
যেগুলো প্ল্যাটফর্ম নিজে থেকে কন্ট্রোল করে বা যেগুলো না থাকলে প্ল্যাটফর্ম Infisical-এ ঢুকতেই পারবে না, শুধু সেগুলোই এখানে থাকবে।

### 🔑 Infisical Connection (Vault Auth)
_(এই key-গুলো সব প্ল্যাটফর্মে ম্যানুয়ালি সেট করতে হবে, যাতে তারা Vault-এ ঢুকতে পারে)_
- `INFISICAL_TOKEN`
- `INFISICAL_PROJECT_ID`
- `INFISICAL_CLIENT_ID`
- `INFISICAL_CLIENT_SECRET`

### 🚀 Platform Specific System Metadata
_(এগুলো Render বা Vercel নিজে থেকেই তাদের ইন্টারনাল সিস্টেমের জন্য ইনজেক্ট করে, তাই এগুলো Infisical-এ রাখার দরকার নেই)_
- `PORT` (Render-এ বাই-ডিফল্ট ডাইনামিক থাকে)
- `NODE_ENV`
- `ENV` (production/staging)
- `SERVICE_ROLE` (admin / user)

---

## 🗑️ গ্রুপ ৩: ডুপ্লিকেট ক্লিনআপ (Action Required)
এই key-গুলো মূলত একই জিনিস নির্দেশ করে, কিন্তু আলাদা নামে `.env`-তে পড়ে আছে। কোড ক্লিনআপ করে এগুলোকে নির্দিষ্ট একটি নামে নিয়ে আসতে হবে।

- ⚠️ **`SUPREMEAI_API_KEY`** এবং **`SUPREMEAI_API_KEY`** → মার্জ করে শুধু **`SUPREMEAI_API_KEY`** রাখা।
- ⚠️ **`ENCRYPTION_KEY`** এবং **`ENCRYPTION_KEY`** → মার্জ করে শুধু **`ENCRYPTION_KEY`** রাখা।
- ⚠️ **`DISCORD_OTP_WEBHOOK_URL`** এবং **`DISCORD_WEBHOOK_URL`** → (যদি ভ্যালু একই হয় তবে যেকোনো একটি ব্যবহার করা)।

### ✅ বৈধ ভ্যারিয়েশন (এগুলো মুছতে হবে না)
- `GITHUB_PAT_AUTO_FIX`, `GITHUB_PAT_MODELS`, `GITHUB_PAT_NILOYJOY7`: এগুলো AI মডেল বা নির্দিষ্ট টাস্কের জন্য আলাদা করে তৈরি করা, তাই এগুলোকে ডুপ্লিকেট হিসেবে ধরা হবে না, এরা স্বকীয়ভাবেই Infisical-এ থাকবে।
