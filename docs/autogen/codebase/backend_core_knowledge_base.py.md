# 📄 ফাইল: backend/core/knowledge_base.py

**প্রকার:** .py  
**সাইজ:** 1,767 বাইট  
**আপডেট:** 2026-07-11T15:50:11.300116

---

## কোড

```py
import json
import os


# বাংলা মন্তব্য: টেস্ট ও রিলায়েবিলিটি গেটের জন্য environment overrides fallback নির্ধারণ করা হলো
BASE_DIR = os.getenv("SUPREMEAI_BASE_DIR") or os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR = os.getenv("SUPREMEAI_DATA_DIR") or os.path.join(BASE_DIR, "data")
MEMORY_FILE_PATH = os.getenv("SUPREMEAI_MEMORY_FILE_PATH") or os.path.join(DATA_DIR, "memory_vault.json")

# ফাইল না থাকলে তৈরি করে নিবে
if not os.path.exists(DATA_DIR):
    os.makedirs(DATA_DIR)
if not os.path.exists(MEMORY_FILE_PATH):
    with open(MEMORY_FILE_PATH, "w") as f:
        json.dump({}, f)


def get_from_memory(prompt: str):
    """ইউজারের প্রম্পটটি আগে সমাধান করা হয়েছে কি না, তা চেক করবে"""
    with open(MEMORY_FILE_PATH) as f:
        memory = json.load(f)
        # সিম্পল কি-ওয়ার্ড বা হ্যাশ ম্যাচিং (পরবর্তীতে আমরা ভেক্টর ডাটাবেস অ্যাড করব)
        return memory.get(prompt, None)


def save_to_memory(prompt: str, solution_code: str):
    """নতুন সমাধান শিখলে সেটি জিরো-কস্ট মেমোরিতে সেভ করে রাখবে"""
    with open(MEMORY_FILE_PATH) as f:
        memory = json.load(f)

    memory[prompt] = solution_code

    with open(MEMORY_FILE_PATH, "w") as f:
        json.dump(memory, f, indent=4)
    print("🧠 [Auto-Didact] New skill learned and saved to memory vault!")  # noqa: T201

```