# scripts/ ফোল্ডার গাইড

## উদ্দেশ্য

এই ফোল্ডারটি SupremeAI-র ইউটিলিটি স্ক্রিপ্টগুলো ধারণ করে, যা ডেভেলপমেন্ট, ডিপ্লোয়মেন্ট এবং মাইন্টেন্যান্সের কাজে ব্যবহৃত হয়।

## ফোল্ডার সাবস্ট্রাকচার

```
scripts/
├── auth/      # প্রমাণীকরণ স্ক্রিপ্টস
├── deploy/    # ডিপ্লোয়মেন্ট স্ক্রিপ্টস
├── knowledge/ # জ্ঞান ব্যবস্থাপনা
├── seed/      # ডাটা সিডিং স্ক্রিপ্টস
├── test/      # টেস্টিং স্ক্রিপ্টস
└── tools/     # ইউটিলিটি টুলস
```

## ফোল্ডার বিস্তারণ

### auth/ - প্রমাণীকরণ স্ক্রিপ্টস

- `add_admin.js` - এডমিন ইউজার যোগ করে
- `debug_login.js` - লগইন ডিবাগ করে
- `set-admin-claims.js` - এডমিন ক্লেমস সেট করে

### deploy/ - ডিপ্লোয়মেন্ট স্ক্রিপ্টস

- `deploy.sh` - মূল ডিপ্লোয়মেন্ট স্ক্রিপ্ট
- `get-fresh-token.js.example` - টোকেন রিফ্রেশ
- `setup-admin-user.js.example` - এডমিন ইউজার সেটআপ

### knowledge/ - জ্ঞান ব্যবস্থাপনা

- `seed_copilot_knowledge.py` - কোপিলট জ্ঞান সিড করে
- `seed_massive_knowledge.py` - ম্যাসিভ জ্ঞান সিডিং

### seed/ - ডাটা সিডিং

- `seed_part*_*.py` - বিভিন্ন বিষয়ে জ্ঞান সিড করে
- `seed_resilience_knowledge.py` - রেসিলিয়েন্স জ্ঞান

### test/ - টেস্টিং স্ক্রিপ্টস

- `integration_test.py` - ইন্টিগ্রেশন টেস্ট
- `test_resilience_endpoints.py` - রেসিলিয়েন্স টেস্ট

### tools/ - ইউটিলিটি টুলস

- `bootstrap_learn_test.ps1` - বুটস্ট্র্যাপ টেস্ট
- `clean_unused_imports.py` - বেকারি ইমপোর্ট মুছে
- `verify_learning_db.py` - লার্নিং DB যাচাই

## ব্যবহার গুরুত্ব

```bash
# একটি স্ক্রিপ্ট চালান
node scripts/deploy/deploy.js

# পাইথন স্ক্রিপ্ট রান করন
python scripts/seed/seed_part1_ai_fundamentals.py
```
