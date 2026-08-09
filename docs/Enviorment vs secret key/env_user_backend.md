# Environment 1: User Backend Environment Variables (`env_user_backend.md`)
*Service: `supremeai-backend` / Role: `user`*

| No. | Environment Variable Key | Description (বিবরণ) | Status / Sync |
| :--- | :--- | :--- | :--- |
| 1 | `ENV` | অ্যাপ্লিকেশনের মোড (`production` / `staging` / `local`) | Configured |
| 2 | `PORT` | সার্ভিস রান করার পোর্ট (ডিফল্ট: `8080`) | Configured |
| 3 | `SERVICE_ROLE` | সার্ভিসের ভূমিকা (User Instance-এ মান `user`) | Configured (`user`) |
| 4 | `USER_CORS_ORIGINS` | ক্লায়েন্ট ফ্রন্টএন্ড origins (Vercel, Firebase, Render) | Configured |
| 5 | `ALLOWED_HOSTS` | Host header validation whitelist (`supremeai-backend.onrender.com`) | Configured |
| 6 | `SUPREMEAI_USER_BACKEND_URL` | User Backend Public Service URL | Configured |
| 7 | `SUPREMEAI_JWT_SECRET` / `JWT_SECRET` | JWT সেসন টোকেন সাইনিং সিক্রেট | Vault / Secret |
| 8 | `SUPREMEAI_ENCRYPTION_KEY` / `ENCRYPTION_KEY` | কাস্টম ইউজার সিক্রেট ও API Keys এনক্রিপশন কী | Vault / Secret |
| 9 | `SUPREMEAI_DOCS_PASSWORD` / `DOCS_PASSWORD` | FastAPI `/docs` এবং `/redoc` পাসওয়ার্ড প্রোটেকশন | Vault / Secret |
| 10 | `SUPREMEAI_API_TOKEN` | অভ্যন্তরীণ সার্ভিস এবং এডমিন ইন্টার-কমিউনিকেশন টোকেন | Vault / Secret |
| 11 | `REDIS_URL` | Redis ক্যাশিং এবং সেসন ডাটাবেজ URL | Vault / Secret |
| 12 | `UPSTASH_REDIS_REST_URL` | Upstash Serverless Redis REST endpoint URL | Vault / Secret |
| 13 | `UPSTASH_REDIS_REST_TOKEN` | Upstash Serverless Redis REST token | Vault / Secret |
| 14 | `SUPABASE_URL` | Supabase প্রজেক্ট API URL | Vault / Secret |
| 15 | `SUPABASE_KEY` | Supabase anon/public key | Vault / Secret |
| 16 | `SUPABASE_DATABASE_URL_POOLER` | Supabase PostgreSQL direct connection pooler string | Vault / Secret |
| 17 | `GEMINI_API_KEY` | Google Gemini AI API key | Vault / Secret |
| 18 | `GROQ_API_KEY` | Groq Llama/Mixtral LLM API key | Vault / Secret |
| 19 | `OPENAI_API_KEY` | OpenAI API key | Vault / Secret |
| 20 | `OPENROUTER_API_KEY` | OpenRouter gateway API key | Vault / Secret |
| 21 | `OPENHANDS_API_KEY` | OpenHands Sandbox execution agent key | Vault / Secret |
| 22 | `STRIPE_API_KEY` | Stripe Secret key | Vault / Secret |
| 23 | `STRIPE_PUBLISHABLE_KEY` | Stripe Publishable key | Vault / Secret |
| 24 | `STRIPE_WEBHOOK_SECRET` | Stripe payment webhook secret | Vault / Secret |
| 25 | `CI_WEBHOOK_SECRET` | CI/CD build deployment trigger secret | Vault / Secret |
| 26 | `INFISICAL_TOKEN` | Infisical Secret Manager token | Vault / Secret |
| 27 | `INFISICAL_CLIENT_SECRET` | Infisical Client secret | Vault / Secret |
