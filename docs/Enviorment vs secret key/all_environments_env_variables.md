# SupremeAI Complete Environment Variables Blueprint (সকল এনভায়রনমেন্টের ভ্যারিয়েবল তালিকা)

সিস্টেমে ব্যবহৃত প্রতিটি আলাদা সার্ভিস/এনভায়রনমেন্টের জন্য প্রয়োজনীয় সকল Environment Variable Keys-এর বিস্তারিত বিবরণী ও তালিকা নিচে প্রদান করা হলো:

---

## 1. User Backend Environment Variables (`supremeai-backend` / `SERVICE_ROLE=user`)
*মেইন ইউজার API সার্ভিস যা সকল ইউজার রিকোয়েস্ট, LLM গেটওয়ে এবং সাধারণ অথেনটিকেশন প্রসেস করে।*

| No. | Environment Variable Key | Description (বিবরণ) |
| :--- | :--- | :--- |
| 1 | `ENV` | অ্যাপ্লিকেশনের মোড (`production` / `staging` / `local`) |
| 2 | `PORT` | সার্ভিস রান করার পোর্ট (ডিফল্ট: `8080`) |
| 3 | `SERVICE_ROLE` | সার্ভিসের ভূমিকা (User Instance-এ মান `user`) |
| 4 | `USER_CORS_ORIGINS` | ক্লায়েন্ট ফ্রন্টএন্ড origins (Vercel, Firebase, Render) |
| 5 | `ALLOWED_HOSTS` | Host header validation whitelist (`supremeai-backend.onrender.com`) |
| 6 | `SUPREMEAI_USER_BACKEND_URL` | User Backend Public Service URL |
| 7 | `SUPREMEAI_JWT_SECRET` / `JWT_SECRET` | JWT टोकन সাইনিং এবং ভেরিফিকেশনের সিক্রেট |
| 8 | `ENCRYPTION_KEY` / `ENCRYPTION_KEY` | কাস্টম ইউজার সিক্রেট ও API Keys এনক্রিপশন কী |
| 9 | `SUPREMEAI_DOCS_PASSWORD` / `DOCS_PASSWORD` | FastAPI `/docs` এবং `/redoc` পাসওয়ার্ড প্রোটেকশন |
| 10 | `SUPREMEAI_API_KEY` | অভ্যন্তরীণ সার্ভিস এবং এডমিন ইন্টার-কমিউনিকেশন টোকেন |
| 11 | `REDIS_URL` | Redis ক্যাশিং এবং সেসন ডাটাবেজ URL |
| 12 | `UPSTASH_REDIS_REST_URL` | Upstash Serverless Redis REST endpoint URL |
| 13 | `UPSTASH_REDIS_REST_TOKEN` | Upstash Serverless Redis REST token |
| 14 | `SUPABASE_URL` | Supabase প্রজেক্ট API URL |
| 15 | `SUPABASE_KEY` | Supabase anon/public key |
| 16 | `SUPABASE_DATABASE_URL_POOLER` | Supabase PostgreSQL direct connection pooler string |
| 17 | `GEMINI_API_KEY` | Google Gemini AI API key |
| 18 | `GROQ_API_KEY` | Groq Llama/Mixtral LLM API key |
| 19 | `OPENAI_API_KEY` | OpenAI API key |
| 20 | `OPENROUTER_API_KEY` | OpenRouter gateway API key |
| 21 | `OPENHANDS_API_KEY` | OpenHands Sandbox execution agent key |
| 22 | `STRIPE_API_KEY` | Stripe Payment Gateway Secret key |
| 23 | `STRIPE_PUBLISHABLE_KEY` | Stripe Payment Gateway Publishable key |
| 24 | `STRIPE_WEBHOOK_SECRET` | Stripe payment webhook validation secret |
| 25 | `CI_WEBHOOK_SECRET` | CI/CD build deployment trigger webhook secret |
| 26 | `INFISICAL_TOKEN` | Infisical Secret Manager access token |
| 27 | `INFISICAL_CLIENT_SECRET` | Infisical Client authentication secret |

---

## 2. Admin Backend Environment Variables (`supremeai-admin` / `SERVICE_ROLE=admin`)
*আইসোলেটেড অ্যাডমিন প্যানেল ব্যাকএন্ড সার্ভিস যা অ্যাডমিন ড্যাশবোর্ড, মনিটরিং ও সিস্টেম কন্ট্রোল পরিচালনা করে।*

| No. | Environment Variable Key | Description (বিবরণ) |
| :--- | :--- | :--- |
| 1 | `ENV` | অ্যাপ্লিকেশনের মোড (`production` / `staging` / `local`) |
| 2 | `PORT` | সার্ভিস রান করার পোর্ট (ডিফল্ট: `8080`) |
| 3 | `SERVICE_ROLE` | সার্ভিসের ভূমিকা (Admin Instance-এ মান `admin`) |
| 4 | `ADMIN_CORS_ORIGINS` | শুধুমাত্র এডমিন প্যানেল frontend origins (`["https://supremeai-admin.web.app"]`) |
| 5 | `ALLOWED_HOSTS` | Admin server Host validation (`supremeai-admin.onrender.com`) |
| 6 | `MIN_EXPECTED_ROUTES` | হেলথচেক এর জন্য ন্যূনতম প্রত্যাশিত রুট (যেমন: `5`) |
| 7 | `SUPREMEAI_ADMIN_BACKEND_URL` | Admin Backend Public Service URL |
| 8 | `SUPREMEAI_ADMIN_PASSWORD_HASH` | Admin প্যানেল এক্সেস করার জন্য ক্রিপ্টোগ্রাফিক পাসওয়ার্ড হ্যাশ |
| 9 | `SUPREMEAI_JWT_SECRET` / `JWT_SECRET` | Admin সেসন JWT টোকেন সাইনিং সিক্রেট |
| 10 | `ENCRYPTION_KEY` / `ENCRYPTION_KEY` | সিস্টেম ওয়াইড মাস্টার এনক্রিপশন কী |
| 11 | `SUPREMEAI_DOCS_PASSWORD` / `DOCS_PASSWORD` | Admin API Docs এক্সেস পাসওয়ার্ড |
| 12 | `SUPREMEAI_API_KEY` | অ্যাডমিন সার্ভিস ইন্টার-কমিউনিকেশন টোকেন |
| 13 | `REDIS_URL` | Redis ক্যাশিং URL |
| 14 | `UPSTASH_REDIS_REST_URL` | Upstash Serverless Redis REST endpoint URL |
| 15 | `UPSTASH_REDIS_REST_TOKEN` | Upstash Serverless Redis REST token |
| 16 | `SUPABASE_URL` | Supabase API URL |
| 17 | `SUPABASE_KEY` | Supabase Anon Key |
| 18 | `SUPABASE_DATABASE_URL_POOLER` | Supabase PostgreSQL direct connection pooler string |
| 19 | `GEMINI_API_KEY` | Google Gemini AI API key |
| 20 | `GROQ_API_KEY` | Groq LLM API key |
| 21 | `OPENAI_API_KEY` | OpenAI API key |
| 22 | `OPENROUTER_API_KEY` | OpenRouter gateway API key |
| 23 | `DISCORD_OTP_WEBHOOK_URL` | JIT Admin OTP অ্যালার্ট ডিসকর্ড ওয়েবহুক URL |
| 24 | `RESEND_API_KEY` | Admin নোটিফিকেশন ইমেইল পাঠানোর API key |
| 25 | `ADMIN_NOTIFICATION_EMAIL` | সিস্টেম অ্যালার্ট গ্রহণের অ্যাডমিন ইমেইল এড্রেস |
| 26 | `CI_WEBHOOK_SECRET` | CI/CD deployment trigger secret |
| 27 | `INFISICAL_TOKEN` | Infisical secret manager token |
| 28 | `INFISICAL_CLIENT_SECRET` | Infisical client secret |

---

## 3. Background Worker Environment Variables (`supremeai-background-worker`)
*ব্যাকগ্রাউন্ড পাইপলাইন, অটোহিলার এবং সেন্ডিনেল মনিটরিংয়ের জন্য ব্যাকগ্রাউন্ড ওয়ার্কার সার্ভিস।*

| No. | Environment Variable Key | Description (বিবরণ) |
| :--- | :--- | :--- |
| 1 | `ENV` | অ্যাপ্লিকেশনের মোড (`production`) |
| 2 | `SERVICE_ROLE` | ওয়ার্কারের ভূমিকা (`user`) |
| 3 | `ALLOWED_HOSTS` | Host header validation (`supremeai-backend.onrender.com`) |
| 4 | `REDIS_URL` | Redis টাস্ক কিউ এবং স্টেট সিঙ্ক সংযোগ |
| 5 | `SUPABASE_DATABASE_URL_POOLER` | Supabase DB কানেকশন স্ট্রিং |
| 6 | `SUPREMEAI_JWT_SECRET` | সিস্টেমে ইন্টারনাল কাজের জন্য সেসন সিক্রেট |
| 7 | `INFISICAL_TOKEN` | Infisical token |
| 8 | `INFISICAL_CLIENT_SECRET` | Infisical client secret |

---

## 4. Studio Client / Web Frontend Environment Variables (`supremeai-studio-client`)
*ইউজার এবং অ্যাডমিন ফ্রন্টএন্ড ওয়েব ক্লায়েন্ট (React / Vite Build-time & Runtime env)।*

| No. | Environment Variable Key | Description (বিবরণ) |
| :--- | :--- | :--- |
| 1 | `VITE_API_URL` | User Backend API-এর বেস URL (`https://supremeai-backend.onrender.com`) |
| 2 | `VITE_API_BASE` | API রুট পাথ এনভায়রনমেন্ট (`https://supremeai-backend.onrender.com`) |
| 3 | `VITE_ADMIN_API_URL` | Admin Backend API-এর বেস URL (`https://supremeai-admin.onrender.com`) |
| 4 | `STRIPE_PUBLISHABLE_KEY` | Stripe Frontend পেমেন্ট গেটওয়ে পাবলিক কী |
