# tools/ সাবফোল্ডার গাইড

## উদ্দেশ্য

এই ফোল্ডারটি ডেভেলপমেন্ট টুলস, মাইন্টেন্যান্স স্ক্রিপ্ট এবং ইউটিলিটি গুলো ধারণ করে।

## ফাইলগুলো

| ফাইল | ব্যবহার |
|------|--------|
| `bootstrap_learn_test.ps1` | বুটস্ট্র্যাপ টেস্ট |
| `clean_unused_imports.py` | বেকারি ইমপোর্ট মুছে |
| `find_unused_imports.py` | বেকারি ইমপোর্ট খুঁজে |
| `remove_unused_imports.py` | ইমপোর্ট মুছে |
| `setup_gcp_billing_alerts.sh` | GCP বিলিং অ্যালার্ট সেট করে |
| `verify_learning_db.py` | লার্নিং DB যাচাই করে |

## ব্যবহার

```bash
# ইউটিলিটি স্ক্রিপ্ট চালান
python tools/clean_unused_imports.py
```