# 🔍 GitHub Tracked Unrelated Files Audit Report

**তারিখ:** ২০০-০৮-০৯  
**উদ্দেশ্য:** GitHub রিপোজিটরিতে ট্র্যাক করা ফাইলগুলোর মধ্যে কোন কোন ফাইল SupremeAI-এর কোর অ্যাপ্লিকেশনের (Backend, Studio Client, Mobile, Cloud Services) সাথে সম্পর্কিত নয় তা নিখুঁতভাবে অডিট ও চিহ্নিত করা।

---

## 📋 সংক্ষেপ (Executive Summary)

সম্পূর্ণ রিপোজিটরিতে `git ls-files` এবং ডিপ-কোড সার্চ চালিয়ে মোট **১১টি ফাইল ও ফোল্ডার চিহ্নিত করা হয়েছে**, যা গিটহাবে পুশ করা রয়েছে কিন্তু SupremeAI অ্যাপ্লিকেশনের সাথে সরাসরি কোনো কার্যকারী সম্পর্ক নেই। 

এগুলোর বেশিরভাগই IDE এক্সটেনশন (যেমন: Kilo AI, Continue), অস্থায়ী স্ক্রিপ্ট (Temporary Fix Scripts), অথবা অটো-জেনারেটেড টেস্টিং ক্যাশ।

---

## 📁 চিহ্নিত ফাইলের তালিকা ও বিস্তারিত বিশ্লেষণ

| ফাইল / ফোল্ডার পাথ | ক্যাটাগরি | প্রজেক্টের সাথে সম্পর্ক ও অডিট বিবরণ | প্রস্তাবিত অ্যাকশন |
| :--- | :--- | :--- | :--- |
| **`.kilo/agent/bangla-tips.md`** | IDE (Kilo AI) | Kilo AI VS Code এক্সটেনশনের লোকাল এজেন্ট ফাইল। অ্যাপের সাথে কোনো সম্পর্ক নেই। | `git rm --cached` |
| **`.kilo/agent/config.json`** | IDE (Kilo AI) | Kilo AI কনফিগ ক্যাশ। | `git rm --cached` |
| **`.kilo/mcp/README.md`** | IDE (Kilo AI) | Kilo MCP ডেমো রিডমি ফাইল। | `git rm --cached` |
| **`.kilo/validate.py`** | IDE (Kilo AI) | Kilo এর লোকাল ডামি ভ্যালিডেশন পাইথন ফাইল। | `git rm --cached` |
| **`.kilo/yaml_test.py`** | IDE (Kilo AI) | Kilo এর লোকাল YAML টেস্ট ফাইল। | `git rm --cached` |
| **`.continue/prompts/new-prompt.md`** | IDE (Continue) | Continue VS Code এক্সটেনশনের ডিফল্ট অটো-জেনারেটেড প্রম্পট ফাইল। | `git rm --cached` |
| **`f`** | Temp Script | `backend/pyproject.toml`-এ cryptography সংস্করণ ঠিক করার সাময়িক স্ক্রিপ্ট ফাইল। | `git rm --cached` (বা ফাইল ডিলিট) |
| **`sphere.html`** | Temp UI Demo | ৩ডি গ্লোয়িং স্পিয়ার থ্রি.জেএস ডেমো ফাইল। মূল অ্যাপ্লিকেশনের অংশ নয়। | `git rm --cached` |
| **`fix_bare_yields.py`** | Temp Script | পাইথন টেস্ট ফাইলের yield ফিক্সের সাময়িক স্ক্রিপ্ট। | `git rm --cached` (বা ফাইল ডিলিট) |
| **`tests/test_db_path`** | Test Artifact | টেস্ট এক্সিকিউশনের সময় জেনারেট হওয়া SQLite ডাটাবেজ ফাইল। | `git rm --cached` |
| **`.scribe_cache.json`** | Cache Artifact | Scribe Agent টুলের জেনারেটেড ক্যাশ ফাইল। `.gitignore`-এ থাকলেও গিটে রয়েছে। | `git rm --cached` |

---

## 🛠 কীভাবে গিটহাব থেকে ফাইলগুলো মুক্ত করবেন (Clean Up Guide)

এই ফাইলগুলো লোকাল সিস্টেম থেকে না মুছে কেবল গিটহাবের রিপোজিটরি থেকে সরানোর জন্য নিচের কমান্ডটি গিট টার্মিনালে রান করতে হবে:

```bash
# ১. গিট ট্র্যাকিং থেকে ফাইলগুলো সরাতে
git rm -r --cached .kilo/ .continue/ f sphere.html fix_bare_yields.py tests/test_db_path .scribe_cache.json

# ২. কমিট তৈরি করতে
git commit -m "chore: remove unrelated IDE configs and temp test artifacts from git tracking"
```

---

> **নোট:** এই ফাইলগুলো অপসারণ করলে SupremeAI-এর ব্যাকএন্ড, ফ্রন্টএন্ড বা সিআই পাইপলাইনে ০% ক্ষতিকর প্রভাব পড়বে।
