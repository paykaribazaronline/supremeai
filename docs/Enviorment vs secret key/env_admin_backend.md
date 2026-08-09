# Environment 2: Admin Backend Environment Variables (`env_admin_backend.md`)
*Service: `supremeai-admin` / Role: `admin`*

| No. | Environment Variable Key | Description (বিবরণ) | Status / Sync |
| :--- | :--- | :--- | :--- |
| 1 | `ENV` | অ্যাপ্লিকেশনের মোড (`production` / `staging` / `local`) | Configured |
| 2 | `PORT` | সার্ভিস রান করার পোর্ট (ডিফল্ট: `8080`) | Configured |
| 3 | `SERVICE_ROLE` | সার্ভিসের ভূমিকা (Admin Instance-এ মান `admin`) | Configured (`admin`) |
| 4 | `ADMIN_CORS_ORIGINS` | শুধুমাত্র এডমিন প্যানেল origins (`["https://supremeai-admin.web.app"]`) | Configured |
| 5 | `ALLOWED_HOSTS` | Admin server Host validation (`supremeai-admin.onrender.com`) | Configured |
| 6 | `MIN_EXPECTED_ROUTES` | হেলথচেক এর জন্য ন্যূনতম প্রত্যাশিত রুট (`5`) | Configured |
| 7 | `SUPREMEAI_ADMIN_BACKEND_URL` | Admin Backend Public Service URL | Configured |
| 8 | `SUPREMEAI_ADMIN_PASSWORD_HASH` | Admin প্যানেল এক্সেস করার জন্য পাসওয়ার্ড হ্যাশ | Vault / Secret |
| 9 | `SUPREMEAI_JWT_SECRET` / `JWT_SECRET` | Admin সেসন JWT টোকেন সাইনিং সিক্রেট | Vault / Secret |
| 10 | `SUPREMEAI_ENCRYPTION_KEY` / `ENCRYPTION_KEY` | কাস্টম এনক্রিপশন কী | Vault / Secret |
| 11 | `SUPREMEAI_DOCS_PASSWORD` / `DOCS_PASSWORD` | Admin API Docs পাসওয়ার্ড | Vault / Secret |
| 12 | `SUPREMEAI_API_TOKEN` | অ্যাডমিন সার্ভিস ইন্টার-কমিউনিকেশন টোকেন | Vault / Secret |
| 13 | `REDIS_URL` | Redis ক্যাশিং URL | Vault / Secret |
| 14 | `UPSTASH_REDIS_REST_URL` | Upstash Serverless Redis REST endpoint URL | Vault / Secret |
| 15 | `UPSTASH_REDIS_REST_TOKEN` | Upstash Serverless Redis REST token | Vault / Secret |
| 16 | `SUPABASE_URL` | Supabase API URL | Vault / Secret |
| 17 | `SUPABASE_KEY` | Supabase Anon Key | Vault / Secret |
| 18 | `SUPABASE_DATABASE_URL_POOLER` | Supabase PostgreSQL connection pooler | Vault / Secret |
| 19 | `GEMINI_API_KEY` | Google Gemini AI API key | Vault / Secret |
| 20 | `GROQ_API_KEY` | Groq LLM API key | Vault / Secret |
| 21 | `OPENAI_API_KEY` | OpenAI API key | Vault / Secret |
| 22 | `OPENROUTER_API_KEY` | OpenRouter gateway API key | Vault / Secret |
| 23 | `DISCORD_OTP_WEBHOOK_URL` | JIT Admin OTP অ্যালার্ট ডিসকর্ড ওয়েবহুক URL | Vault / Secret |
| 24 | `RESEND_API_KEY` | Admin নোটিফিকেশন ইমেইল পাঠানোর API key | Vault / Secret |
| 25 | `ADMIN_NOTIFICATION_EMAIL` | অ্যাডমিন অ্যালার্ট ইমেইল এড্রেস | Vault / Secret |
| 26 | `CI_WEBHOOK_SECRET` | CI/CD deployment trigger secret | Vault / Secret |
| 27 | `INFISICAL_TOKEN` | Infisical token | Vault / Secret |
| 28 | `INFISICAL_CLIENT_SECRET` | Infisical client secret | Vault / Secret |
