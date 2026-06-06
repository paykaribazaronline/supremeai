# knowledge/ সাবফোল্ডার গাইড

## উদ্দেশ্য

এই ফোল্ডারটি AI সিস্টেমের জ্ঞান ব্যবস্থাপনা সম্পর্কিত স্ক্রিপ্টগুলো ধারণ করে।

## ফাইলগুলো

| ফাইল                              | ব্যবহার                       |
| --------------------------------- | ----------------------------- |
| `seed_copilot_knowledge.py`       | কোপিলট AI এর জ্ঞান সিড করে    |
| `seed_massive_knowledge.py`       | ম্যাসিভ জ্ঞান ডাটাসেট সিড করে |
| `audit_knowledge_completeness.py` | জ্ঞানের পূর্ণতা যাচাই করে     |
| `auto_knowledge_harvester.py`     | স্বয়ংস্রুতি জ্ঞান সংগ্রহ করে |
| `expand_knowledge.py`             | জ্ঞানকে প্রসারিত করে          |
| `git_dna_harvester.py`            | জিট থেকে জ্ঞান বানায়         |
| `global_insight_harvester.py`     | গ্লোবাল ইনসাইট সংগ্রহ করে     |

## ক্লাউড ডাটাবেস কনফিগারেশন

### Firestore জ্ঞান স্টোরেজ

- জ্ঞান ডকুমেন্টগুলো `knowledge/` কলেকশনে সেভ হয়
- ইনডেক্স `config/firestore.indexes.json` এ আছে
- সিকিউরিটি রুলস `config/firestore.rules` এ

### জ্ঞান বীজ (Seed) রান করা

```bash
python knowledge/seed_copilot_knowledge.py
```

## জ্ঞান ব্যবস্থাপনা প্রক্রিয়া

1. **সিডিং**: `seed_*.py` স্ক্রিপ্টগুলো চালান
2. **হারভেস্টিং**: `*-harvester.py` স্ক্রিপ্টগুলো জ্ঞান সংগ্রহ করে
3. **অডিট**: `audit_*.py` জ্ঞানের গুণগতি যাচাই করে
4. **প্রসারণা**: `expand_knowledge.py` জ্ঞানকে প্রসারিত করে
