# Admin Backend Environment Variables List (এডমিন ব্যাকএন্ড এনভায়রনমেন্ট ভেরিয়েবল তালিকা)

নিচে Admin Backend (`supremeai-admin` / `core.app_admin`)-এ ব্যবহৃত সকল Environment Variable Keys-এর তালিকা দেওয়া হলো:

| ক্রমিক নম্বর | Environment Variable Key | বিবরণ / বিবরণী |
| :--- | :--- | :--- |
| 1 | `CI_WEBHOOK_SECRET` | CI/CD ওয়েবহুক ভ্যালিডেশন সিক্রেট |
| 2 | `DOCS_PASSWORD` | ডকুমেন্টেশন এক্সেস পাসওয়ার্ড (`SUPREMEAI_DOCS_PASSWORD`) |
| 3 | `ENCRYPTION_KEY` | ডাটা এনক্রিপশন ও সিকিউরিটি কী |
| 4 | `ENV` | অ্যাপ্লিকেশনের এনভায়রনমেন্ট মোড (`production` / `development`) |
| 5 | `GEMINI_API_KEY` | Google Gemini AI API Key |
| 6 | `GROQ_API_KEY` | Groq LLM API Key |
| 7 | `INFISICAL_TOKEN` | Infisical সিক্রেট ম্যানেজমেন্ট টোকেন |
| 8 | `JWT_SECRET` | JWT অথেন্টিকেশন সাইনিং সিক্রেট |
| 9 | `OPENHANDS_API_KEY` | OpenHands সার্ভিস API Key |
| 10 | `OPENROUTER_API_KEY` | OpenRouter LLM Gateway API Key |
| 11 | `REDIS_URL` | Redis ক্যাশে ডাটাবেজ সংযোগ URL |
| 12 | `SERVICE_ROLE` | সার্ভিস রোল (Admin Instance-এ `admin`) |
| 13 | `STRIPE_API_KEY` | Stripe পেমেন্ট গেটওয়ে API Key |
| 14 | `STRIPE_PUBLISHABLE_KEY` | Stripe পাবলিক / ক্লায়েন্ট কী |
| 15 | `STRIPE_WEBHOOK_SECRET` | Stripe পেমেন্ট ইভেন্ট ওয়েবহুক সিক্রেট |
| 16 | `SUPREMEAI_ADMIN_PASSWORD_HASH` | Admin এক্সেসের জন্য পাসওয়ার্ড হ্যাশ |
| 17 | `SUPREMEAI_ENCRYPTION_KEY` | SupremeAI কাস্টম এনক্রিপশন সিক্রেট কী |
| 18 | `SUPREMEAI_JWT_SECRET` | SupremeAI অ্যাডমিন সেসন JWT সিক্রেট |
| 19 | `UPSTASH_REDIS_REST_TOKEN` | Upstash Serverless Redis REST API টোকেন |
| 20 | `UPSTASH_REDIS_REST_URL` | Upstash Serverless Redis REST endpoint URL |
| 21 | `ADMIN_CORS_ORIGINS` | Admin Console Origin Whitelist |
| 22 | `ALLOWED_HOSTS` | Admin Server Host Validation |
