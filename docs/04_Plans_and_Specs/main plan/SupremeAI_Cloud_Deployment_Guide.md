# SupremeAI Cloud-Native Deployment Guide (GCP Cloud Run)

> **Status:** 🟢 Updated for v5 Architecture


এই ডকুমেন্টে ৫টি এআই মডেলকে গুগল ক্লাউড রানে (Scale-to-Zero) ডেপ্লয় করার কমান্ড এবং কনফিগারেশন দেওয়া হলো।

## ১. মডেল ডেপ্লয়মেন্ট কমান্ডস (GCP Cloud Run)

নিচের কমান্ডগুলো সরাসরি টার্মিনালে রান করুন। প্রতিটি মডেলের জন্য আলাদা সার্ভিস তৈরি হবে।

### ক. Qwen 2.5 Coder 7B (Primary Coder)
```bash
gcloud run deploy ai-qwen-coder \
  --image=ollama/ollama \
  --platform=managed \
  --region=us-central1 \
  --memory=8Gi \
  --cpu=4 \
  --timeout=3600 \
  --no-allow-unauthenticated \
  --set-env-vars=OLLAMA_MODEL=qwen2.5-coder:7b \
  --min-instances=0 \
  --max-instances=5
```

### খ. Llama 3.1 8B (General Chat)
```bash
gcloud run deploy ai-llama-chat \
  --image=ollama/ollama \
  --platform=managed \
  --region=us-central1 \
  --memory=8Gi \
  --cpu=4 \
  --no-allow-unauthenticated \
  --set-env-vars=OLLAMA_MODEL=llama3.1:8b \
  --min-instances=0
```

### গ. Phi 3 Mini (Fast Tasks)
```bash
gcloud run deploy ai-phi-mini \
  --image=ollama/ollama \
  --platform=managed \
  --region=us-central1 \
  --memory=4Gi \
  --cpu=2 \
  --no-allow-unauthenticated \
  --set-env-vars=OLLAMA_MODEL=phi3:mini \
  --min-instances=0
```

---

## ২. ফায়ারবেস (Firestore) মেটাডাটা আপডেট স্ক্রিপ্ট

ডেপ্লয়মেন্ট শেষে যে URL-গুলো পাবেন, সেগুলো ফায়ারবেসে আপডেট করতে হবে যাতে ব্যাকএন্ড অটোমেটিকলি নতুন এন্ডপয়েন্ট খুঁজে পায়।

| Provider Key | Service URL (Example) |
| :--- | :--- |
| **qwen-coder** | https://supreme-ai-qwen-coder-565236080752.us-central1.run.app |
| **llama-3-1** | https://supreme-ai-llama-3-1-565236080752.us-central1.run.app |
| **deepseek-pro** | https://supreme-ai-deepseek-pro-565236080752.us-central1.run.app |

### আপডেট করার নিয়ম:
1.  SupremeAI Dashboard-এ যান।
2.  Settings -> AI Providers এ ক্লিক করুন।
3.  উপরে উল্লেখিত Key অনুযায়ী URL আপডেট করে 'Sync to Firestore' দিন।

---

## ৩. গুরুত্বপূর্ণ নোট (Resilience Check)

*   **Scale-to-Zero:** প্রতিটি মডেলের জন্য `--min-instances=0` সেট করা হয়েছে। এতে মডেলগুলো আইডল থাকলে কোনো খরচ হবে না। 
*   **Cold Start:** প্রথমবার কল করার সময় মডেল লোড হতে কয়েক সেকেন্ড সময় নিতে পারে। এটি আমাদের রেজিলিয়েন্স প্রোটোকলের অংশ।
*   **Security:** `--no-allow-unauthenticated` ফ্ল্যাগটি নিশ্চিত করে যে শুধুমাত্র আমাদের ব্যাকএন্ড (JWT Token দিয়ে) এই সার্ভিসগুলো কল করতে পারবে।

