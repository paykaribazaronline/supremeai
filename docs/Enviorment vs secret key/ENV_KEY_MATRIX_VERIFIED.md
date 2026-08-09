# SupremeAI 2.0 — যাচাইকৃত (Verified) Environment ↔ Secret Key ম্যাট্রিক্স

_তৈরি হয়েছে সরাসরি কোড পড়ে (render.yaml, backend/core/config.py, GitHub Actions workflows, Firebase Functions, Java worker, frontend) — কোনো পুরনো doc-কে সরাসরি বিশ্বাস না করে। যেখানে repo-র বিদ্যমান `docs/Enviorment vs secret key/` doc-এর সাথে গ্যাপ পাওয়া গেছে, সেটাও নিচে আলাদা করে দেখানো হলো।_

---

## ০. Methodology (কীভাবে এই লিস্ট বানানো হয়েছে)

1. `render.yaml` থেকে সরাসরি প্রতিটি Render service-এর `envVars:` block পড়া হয়েছে — এটাই deploy-time ground truth
2. `backend/core/config.py`-এর `_BATCH_SECRET_KEYS` লিস্ট এবং `sys.exit(1)` / `raise RuntimeError` করা fail-fast validator গুলো পড়ে **আসল criticality** বের করা হয়েছে (মতামত না, কোড যা literally করে সেটাই)
3. পুরো `backend/` এ `os.getenv(...)` এর সব ব্যবহার grep করে ৩৫০+ ইউনিক নাম বের করা হয়েছে, তারপর সেগুলো registry-র সাথে diff করে **missing entries** বের করা হয়েছে
4. `.github/workflows/*.yml` এ `secrets.*` reference grep করে GitHub Actions-এর আসল secret লিস্ট বের করা হয়েছে
5. Firebase Functions, Java worker (`application.yml`), Frontend (Vite `import.meta.env`), Cloudflare Worker, Mobile (Flutter `--dart-define`) — প্রতিটা platform আলাদা ভাবে scan করা হয়েছে

---

## ১. Criticality Tier (গুরুত্ব অনুযায়ী ভাগ — কোড থেকে যাচাই করা)

| Tier | মানে | কী হয় missing থাকলে |
|---|---|---|
| **Tier 0 — HARD CRASH** | না থাকলে সার্ভার boot-ই হবে না | `sys.exit(1)` অথবা `raise RuntimeError` |
| **Tier 1 — DEGRADED BOOT** | সার্ভার চলবে, কিন্তু নির্দিষ্ট feature বন্ধ থাকবে, boot-time CRITICAL/WARNING লগ হবে | Log warning, feature silently degraded |
| **Tier 2 — PLATFORM REQUIRED** | নির্দিষ্ট platform (Render/GitHub Actions/Firebase) এর জন্য practically must, কিন্তু কোড নিজে crash করায় না | Deploy/workflow ভেঙে যাবে (runtime না, pipeline-level) |
| **Tier 3 — OPTIONAL / LONG-TAIL** | Feature-flag ধরনের, একটা নির্দিষ্ট integration বা agent-এর জন্য | শুধু সেই একটা feature কাজ করবে না |

### Tier 0 — এই ৩টা key literally সার্ভার ক্র্যাশ করায় (কোড-verified)
| Key | কোথায় check হয় | শর্ত |
|---|---|---|
| `ENCRYPTION_KEY` | `config.py` validator, `env != "test"` হলে | সবসময় (test বাদে সব env) |
| `SUPREMEAI_JWT_SECRET` (বা `JWT_SECRET`) | `jwt_secret` property | শুধু `env == "production"`, এবং ≥64 বাইট না হলেও crash |
| `SUPREMEAI_ADMIN_PASSWORD_HASH` | `supremeai_admin_password_hash` property | `pytest`/`CI` না হলে সব env-এ |

### Tier 1 — Boot হবে কিন্তু critical log/degraded mode (কোড-verified, `_BATCH_SECRET_KEYS` + boot validator থেকে)
LLM keys-এর অন্তত ১টা না থাকলে "🚨 BOOT-TIME ALERT: কোনো LLM API key পাওয়া যায়নি" — সব AI feature মৃত থাকবে:
`GEMINI_API_KEY`, `OPENROUTER_API_KEY`, `GROQ_API_KEY`, `DEEPSEEK_API_KEY`, `OPENAI_API_KEY`

Stripe না থাকলে billing mock-mode-এ চলে (warning, crash না): `STRIPE_API_KEY`, `STRIPE_WEBHOOK_SECRET`

---

## ২. Render — `supremeai-backend` (web, production)
_সরাসরি `render.yaml` থেকে (`sync: false` মানে Render dashboard-এ manually set করতে হয়)_

| Key | Tier | নোট |
|---|---|---|
| `REDIS_URL` | 1 (batch secret) | |
| `UPSTASH_REDIS_REST_URL` / `UPSTASH_REDIS_REST_TOKEN` | 2 | |
| `SUPABASE_URL` / `SUPABASE_KEY` | 1 (batch secret) | |
| `SUPABASE_DATABASE_URL_POOLER` | 1 (batch secret) | pgbouncer pooled connection |
| `OPENAI_API_KEY` / `OPENROUTER_API_KEY` / `GEMINI_API_KEY` | 1 | LLM-এর অন্তত একটা লাগবে |
| `SUPREMEAI_JWT_SECRET` | **0 (crash)** | ≥64 bytes |
| `SUPREMEAI_ADMIN_PASSWORD_HASH` | **0 (crash)** | |
| `SUPREMEAI_ENCRYPTION_KEY` / `ENCRYPTION_KEY` | **0 (crash)** | ⚠️ দুইটা আলাদা নামে আছে — নিচে গ্যাপ সেকশনে দেখুন |
| `SUPREMEAI_DOCS_PASSWORD` | 2 | না থাকলে fallback password ব্যবহার হয় (নিরাপত্তা ঝুঁকি!) |
| `SUPREMEAI_API_TOKEN` | 1 | |
| `STRIPE_API_KEY` / `STRIPE_WEBHOOK_SECRET` | 1 | |
| `CI_WEBHOOK_SECRET` | 1 | |
| `INFISICAL_TOKEN` / `INFISICAL_CLIENT_SECRET` | 2 | vault access-এর জন্য নিজেই |

## ৩. Render — `supremeai-admin` (web, production)
উপরের প্রায় সব + অতিরিক্ত:

| Key | Tier | নোট |
|---|---|---|
| `DISCORD_OTP_WEBHOOK_URL` | 1 | Admin OTP notification |
| `RESEND_API_KEY` | 1 | Admin email |
| `ADMIN_NOTIFICATION_EMAIL` | 1 | |

## ৪. Render — `supremeai-background-worker` (worker, production)
ছোট subset — শুধু যা task processing-এ লাগে:

| Key | Tier |
|---|---|
| `REDIS_URL` | 1 |
| `SUPABASE_DATABASE_URL_POOLER` | 1 |
| `SUPREMEAI_JWT_SECRET` | 0 (crash, production হলে) |
| `INFISICAL_TOKEN` / `INFISICAL_CLIENT_SECRET` | 2 |

## ৫. Render — `supremeai-studio-client` (frontend static)
Secret না, শুধু public build-time URL:
`VITE_API_URL`, `VITE_API_BASE` (উভয়ই backend-এর public URL — non-sensitive)

---

## ৬. GitHub Actions — সব workflow থেকে সংগৃহীত (`secrets.*` grep, ৩৬টা)

| গ্রুপ | Keys | Tier |
|---|---|---|
| **Repo sync (promotion pipeline)** | `MAIN_REPO_TOKEN`, `MIRROR_REPO_TOKEN`, `GITHUB_TOKEN` | 2 |
| **Render deploy** | `RENDER_API_KEY`, `RENDER_API_KEY_BACKUP`, `RENDER_DEPLOY_HOOK_URL`, `RENDER_DEPLOY_HOOK_URL_BACKUP`, `RENDER_PRIMARY_SVC_ID`, `RENDER_BACKUP_SVC_ID` | 2 |
| **Vercel deploy** | `VERCEL_TOKEN`, `VERCEL_PROJECT_ID`, `VERCEL_ORG_ID` | 2 |
| **Firebase / GCP deploy** | `FIREBASE_SERVICE_ACCOUNT`, `FIREBASE_SERVICE_ACCOUNT_SUPREMEAI_A`, `GCP_SA_KEY`, `GCP_PROJECT_ID`, `VITE_FIREBASE_API_KEY` | 2 |
| **Database/Cache (CI test env)** | `SUPABASE_URL`, `SUPABASE_KEY`, `SUPABASE_DATABASE_URL`, `SUPABASE_DATABASE_URL_POOLER`, `UPSTASH_REDIS_REST_URL`, `UPSTASH_REDIS_REST_TOKEN` | 2 |
| **Core secrets (CI runtime)** | `SUPREMEAI_JWT_SECRET`, `ENCRYPTION_KEY`, `GEMINI_API_KEY`, `OPENROUTER_API_KEY` | 2 |
| **Android signing** | `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, `ANDROID_STORE_PASSWORD`, `PLAY_STORE_CONFIG_JSON` | 3 (শুধু mobile release build-এ লাগে) |
| **iOS signing** | `APP_STORE_CONNECT_API_ISSUER_ID`, `APP_STORE_CONNECT_API_KEY_CONTENT`, `APP_STORE_CONNECT_API_KEY_ID` | 3 |
| **Notifications** | `DISCORD_WEBHOOK_URL` | 3 |

---

## ৭. Firebase Functions (`tools/firebase_functions_v1`, `infrastructure/firebase_functions`)

| Key | Tier | নোট |
|---|---|---|
| `GEMINI_API_KEY`, `GROQ_API_KEY`, `GROQ_API_KEY_DEPLOYMENT_MONITOR` | 2 | |
| `SUPREMEAI_EMAIL`, `SUPREMEAI_EMAIL_PASSWORD` | 2 | |
| `AUTHORIZED_ADMINS` | 2 | |
| `GCP_PROJECT_ID` | 2 | |
| `JAVA_BACKEND_URL`, `CHAT_API_URL`, `SUPREME_BACKEND_URL`, `SCRAPE_ENGINE_URL`, `BROWSER_AUTOMATION_URL` | 3 | non-secret, শুধু internal URL |

## ৮. Java Worker (`apps/java-worker`, `application.yml`)

| Key | Tier |
|---|---|
| `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` | 0 (Spring context boot fail করবে) |

## ৯. Frontend (`apps/studio-client`, Vite) — এগুলো secret না, public build var
`VITE_API_URL`, `VITE_API_BASE`, `VITE_ADMIN_BACKEND`, `VITE_USER_BACKEND`, `VITE_WS_BASE_URL`, `VITE_FIREBASE_*` (৬টা), `VITE_ENV`, `VITE_PORTAL_TYPE`, `VITE_GITHUB_REPO`, ইত্যাদি — কোনোটাই server secret না, তাই এগুলোকে GitHub Secret বানানোর দরকার নেই (build output-এ যেভাবেই হোক client browser-এ visible হয়ে যাবে)।

## ১০. Mobile (Flutter) — ভালো খবর
কোড scan করে দেখা গেছে mobile app-এ **কোনো secret key hardcode বা env হিসেবে embed করা নেই** — শুধু `--dart-define=API_BASE_URL` দিয়ে backend URL পাস হয়, সব actual secret backend-এ থাকে। এটাই সঠিক pattern, change করার দরকার নেই।

## ১১. Cloudflare Worker — একই রকম, শুধু non-secret URL
`PRIMARY_URL`, `BACKUP_URL`, `ADMIN_URL`, `BACKUP_HEALTH` — এগুলোও secret না, routing config মাত্র।

---

## ১২. ⚠️ বিদ্যমান `docs/Enviorment vs secret key/` doc-এর সাথে যেসব গ্যাপ পাওয়া গেছে

আপনার repo-তে আগে থেকেই একটা বেশ বড় registry doc আছে (`ENVIRONMENT_AND_API_KEYS_REGISTRY.md`, আজকেই commit হয়েছে — বেশ fresh, stale না)। তবে সরাসরি কোড-এর সাথে cross-check করে এই gap গুলো পাওয়া গেছে:

- **`ANTHROPIC_API_KEY`** — কোডে ব্যবহার হয় (LLM router-এ), কিন্তু registry doc-এ নেই
- **`INFISICAL_CLIENT_ID`** — vault authentication-এর একটা অংশ, কোডে আছে, registry-তে নেই (শুধু `INFISICAL_TOKEN`/`INFISICAL_CLIENT_SECRET` আছে — এই ৩টা মিলে vault client auth করে, একটা বাদ পড়লে vault access আংশিক ভাঙবে)
- **`ENCRYPTION_KEY` vs `SUPREMEAI_ENCRYPTION_KEY`** — কোডে দুটো আলাদা নাম ব্যবহার হয়েছে (একটা crash করায়, আরেকটা batch-secret) — এটা সম্ভবত ঐতিহাসিক duplication, স্পষ্ট করা দরকার কোনটা আসল single source of truth
- **Optional integration keys সম্পূর্ণ অনুপস্থিত registry থেকে**: `MINIO_ACCESS_KEY`/`MINIO_SECRET_KEY`, `PINECONE_API_KEY`, `QDRANT_URL`(alt names আছে কিন্তু host-level নেই), `R2_ACCESS_KEY`/`R2_SECRET_KEY`, `NATS_TOKEN`, `SLACK_BOT_TOKEN`, `TELEGRAM_BOT_TOKEN`, `POSTHOG_API_KEY` — এগুলো code-এ genuinely ব্যবহৃত হয় (optional feature হিসেবে) কিন্তু registry-তে documented না
- **Coding-agent keys** (`AIDER_API_KEY`, `CLINE_API_KEY`, `CODEIUM_API_KEY`, `CONTINUE_API_KEY`, `OPENHANDS_API_KEY`, `PLANDEX_API_KEY`, `PYTHAGORA_API_KEY`) — এগুলো optional third-party dev-tool integration, registry-তে নেই
- **৮টা placeholder file খালি** (`env_cloudflare.md`, `env_render_backend.md`, `env_render_admin.md`, `env_vercel_netlify.md`, `env_github_actions.md`, `env_dotenv.md`, `env_infisical.md`, `env_firebase_gcp.md`) — তৈরি হয়েছে কিন্তু কখনো লেখা হয়নি
- **ডেটাবেজ ও ভেক্টর স্টোর (Undocumented)**: `NEO4J_URI`, `NEO4J_USER`, `NEO4J_PASSWORD`, `QDRANT_API_KEY`, `SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_ACCESS_TOKEN`, `SUPABASE_DB_URL`
- **LLM ও AI প্রোভাইডার (Undocumented)**: `MISTRAL_API_KEY`, `HF_API_KEY`, `HF_TOKEN`, `HUGGINGFACE_TOKEN`, `NVIDIA_API_KEY`, `OLLAMA_URL`, `LANGSMITH_API_KEY`, `SAFETY_API_KEY`
- **অথেনটিকেশন ও সিকিউরিটি (Undocumented)**: `API_KEY_SIGNING_SECRET`, `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`, `GITHUB_API_TOKEN`, `JIT_OTP_SECRET`, `SUPREMEAI_ADMIN_TOTP_SECRET`, `SUPREMEAI_API_KEY`, `INFISICAL_PROJECT_ID`
- **ইনফ্রাস্ট্রাকচার ও ব্যাকআপ (Undocumented)**: `BACKUP_BUCKET`, `EVOLUTION_DB_PATH_GCS`, `GCP_ACCESS_TOKEN`, `GCP_FIRESTORE_SQLITE_PATH`, `GCP_PUBSUB_SQLITE_PATH`, `NETLIFY_API_KEY`, `SENTRY_AUTH_TOKEN`, `SENTRY_DSN`, `LAUNCHDARKLY_SDK_KEY`, `RATE_LIMIT_ENABLED`
- **নোটিফিকেশন ও এক্সটার্নাল সার্ভিস (Undocumented)**: `DISCORD_BOT_TOKEN`, `SENDGRID_API_KEY`, `SMTP_PASSWORD`, `SMTP_USER`, `STRIPE_SECRET_KEY`

---

## ১৩. মূল পর্যবেক্ষণ (সবচেয়ে গুরুত্বপূর্ণ)

আপনার backend-এ **৩৫০+ ইউনিক env var name** ব্যবহার হয় কোডে (বেশিরভাগই tuning knob/threshold, secret না)। এর মধ্যে সত্যিকারের secret/credential ধরনের key প্রায় ৮০-১০০টা। এই স্কেলে **ম্যানুয়াল ভাবে তালিকা maintain করা বাস্তবসম্মত না** — সময়ের সাথে সাথে এটা registry doc-গুলোর মতোই stale হয়ে যাবে (যেমনটা এইমাত্র registry-তে ৮টা gap পাওয়া গেল, doc আজকেই লেখা হয়েছে তারপরও)।

তাই আগের প্ল্যানে যা বলেছিলাম সেটাই সবচেয়ে জরুরি এখন: একটা **drift-detection script** (`scripts/audit_env_usage.py`) বানানো, যেটা প্রতি push-এ কোড scan করে registry-র সাথে diff করবে এবং mismatch পেলে CI fail করবে। এটা বানালে এই ধরনের manual list-টা নিজে থেকেই সবসময় সত্যি থাকবে।

চাইলে আমি এখনই এই matrix-টাকে `secrets_registry.yaml` মেশিন-রিডেবল ফরম্যাটে convert করে + `audit_env_usage.py` script লিখে দিতে পারি, যাতে এটা এক-বারের ম্যানুয়াল কাজ না হয়ে যায়।
