# infra/ ফোল্ডার গাইড

## উদ্দেশ্য

এই ফোল্ডারটি সিস্টেম ইন্ফ্রাস্ট্রাকচার সম্পর্কিত ফাইলগুলো ধারণ করে।

## ফাইলগুলো

| ফাইল | ব্যবহার |
|------|--------|
| `Dockerfile` | ডকার ইমেজ বিল্ড করে |

## ক্লাউড ডিপ্লোয়মেন্ট

### Google Cloud Platform (GCP)
- সার্ভিস অ্যাকাউন্ট `config/service-account.json` এ রয়েছে
- ক্লাউড রান করতে: `gcloud run deploy`
- বিলিং অ্যালার্ট `scripts/tools/setup_gcp_billing_alerts.sh`

### Firebase
- ফায়ারবেস প্রজেক্ট কনফিগ `config/firebase.json`
- ফায়ারবেস ইমুলেটর চালান: `firebase emulators:start`
- ক্লাউড ফাংশন্স: `firebase deploy --only functions`

### Firestore ডাটাবেস
- সিকিউরিটি রুলস `config/firestore.rules`
- ইন্ডেক্স `config/firestore.indexes.json`
- ডাটা ম্যানেজমেন্ট `scripts/tools/firebase_collections_setup.py`

## ডিপ্লয়মেন্ট প্রক্রিয়া

1. বিল্ড করন: `./gradlew build`
2. ডকার বিল্ড: `docker build -t supremeai .`
3. রান: `docker run -p 8080:8080 supremeai`