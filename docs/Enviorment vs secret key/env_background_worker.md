# Environment 3: Background Worker Environment Variables (`env_background_worker.md`)
*Service: `supremeai-background-worker` / Mode: Background Service*

| No. | Environment Variable Key | Description (বিবরণ) | Status / Sync |
| :--- | :--- | :--- | :--- |
| 1 | `ENV` | অ্যাপ্লিকেশনের মোড (`production`) | Configured |
| 2 | `SERVICE_ROLE` | ওয়ার্কারের ভূমিকা (`user`) | Configured |
| 3 | `ALLOWED_HOSTS` | Host header validation (`supremeai-backend.onrender.com`) | Configured |
| 4 | `REDIS_URL` | Redis টাস্ক কিউ এবং স্টেট সিঙ্ক সংযোগ | Vault / Secret |
| 5 | `SUPABASE_DATABASE_URL_POOLER` | Supabase DB কানেকশন স্ট্রিং | Vault / Secret |
| 6 | `SUPREMEAI_JWT_SECRET` | সিস্টেমে ইন্টারনাল কাজের জন্য সেসন সিক্রেট | Vault / Secret |
| 7 | `INFISICAL_TOKEN` | Infisical token | Vault / Secret |
| 8 | `INFISICAL_CLIENT_SECRET` | Infisical client secret | Vault / Secret |
