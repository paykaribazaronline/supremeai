# tools/ সাবফোল্ডার গাইড

## উদ্দেশ্য

এই ফোল্ডারটি ডেভেলপমেন্ট টুলস, মাইন্টেন্যান্স স্ক্রিপ্ট এবং ইউটিলিটি গুলো ধারণ করে।

## ফাইলগুলো

| ফাইল                          | ব্যবহার                     |
| ----------------------------- | --------------------------- |
| `bootstrap_learn_test.ps1`    | বুটস্ট্র্যাপ টেস্ট          |
| `clean_unused_imports.py`     | বেকারি ইমপোর্ট মুছে         |
| `find_unused_imports.py`      | বেকারি ইমপোর্ট খুঁজে        |
| `remove_unused_imports.py`    | ইমপোর্ট মুছে                |
| `setup_gcp_billing_alerts.sh` | GCP বিলিং অ্যালার্ট সেট করে |
| `verify_learning_db.py`       | লার্নিং DB যাচাই করে        |
| `analyze_duplicates.ps1`      | ডাপ্লিকেট ফাইল বিশ্লেষণ     |
| `batch-error-scan.ps1`        | ব্যাচ এরর স্ক্যান           |
| `perf_quickcheck.ps1`         | পারফরম্যান্স চেক            |
| `git-autosync.ps1`            | গিট অথো-সিঙ্ক               |

## ব্যবহার

```bash
# ইউটিলিটি স্ক্রিপ্ট চালান
python tools/clean_unused_imports.py

# পারফরম্যান্স চেক
./tools/perf_quickcheck.ps1
```

## ক্লাউড টুলস

### GCP (Google Cloud Platform)

- বিলিং অ্যালার্ট: `setup_gcp_billing_alerts.sh`
- সিক্রেট অডিট: `gcp-secret-audit.sh`

### Firebase

- ফায়ারবেস স্ক্যান: `verify-firebase-saves.sh`
- ফায়ারবেস ডেপ্লোয়: `deploy.sh`

## মাইন্টেন্যান্স টুলস

1. **কোড ক্লিনাপ**: `clean_unused_imports.py`
2. **ডাপ্লিকেট চেক**: `analyze_duplicates.ps1`
3. **পারফরম্যান্স মনিটর**: `perf_quickcheck.ps1`
