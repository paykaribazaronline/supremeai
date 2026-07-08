# 📄 ফাইল: backend/core/semantic_cache.py

**প্রকার:** .py  
**সাইজ:** 4,819 বাইট  
**আপডেট:** 2026-07-08T10:08:43.813053

---

## কোড

```py
# Vector Semantic Cache Engine for SupremeAI 2.0
# বাংলা মন্তব্য: এটি ফায়ারস্টোর বাদ দিয়ে সরাসরি experience_db.py (ChromaDB/Qdrant) ব্যবহার করে এবং ডাইনামিক থ্রেশহোল্ড সেট করে।

from loguru import logger

from adaptive_engine.experience_db import Experience
from adaptive_engine.experience_db import ExperienceDatabase
from core.config_cache import config_cache


# বাংলা মন্তব্য: ক্যাশ পলিসি — এখন প্রকৃত অর্থে Database-Driven!
# get_cache_threshold() ConfigCache থেকে value নেয়, যা SystemConfig DB টেবিলে persist করে।
# Admin চাইলে Admin Dashboard বা API কলের মাধ্যমে threshold পরিবর্তন করতে পারে —
# কোন re-deploy লাগবে না। ConfigCache TTL (৬০ সেকেন্ড) পর্যন্ত in-memory serve করে,
# তারপর DB থেকে reload করে।


def get_cache_threshold(task_type: str) -> float:
    """
    task_type অনুযায়ী ক্যাশ থ্রেশহোল্ড রিটার্ন করে — **DB-Driven**।
    
    ConfigCache SystemConfig টেবিল থেকে কনফিগ লোড করে:
      - cache_threshold_code = 0.95
      - cache_threshold_general = 0.85
      - cache_threshold_reasoning = 0.80
      - ইত্যাদি
    
    Admin চাইলে Dashboard থেকে এগুলো পরিবর্তন করতে পারে — re-deploy ছাড়াই।
    TTL-এর মধ্যে in-memory ক্যাশ serve হবে, প্রতি request-এ DB hit হবে না।
    """  # noqa: W293
    task_lower = task_type.lower()

    # Try ConfigCache first (DB-driven)
    cached_default = config_cache.get(f"cache_threshold_{task_lower}")
    if cached_default is not None:
        return float(cached_default)

    # Fallback: check if any key prefix matches
    all_thresholds = config_cache.get_all("cache_threshold_")
    for key, threshold in all_thresholds.items():
        config_task = key.replace("cache_threshold_", "")
        if config_task in task_lower:
            return float(threshold)

    # Ultimate fallback
    return 0.85


class CacheEntry:
    def __init__(self, provider: str, model: str, response: str):
        self.provider = provider
        self.model = model
        self.response = response

class SemanticCache:
    def __init__(self):
        # Initialize Experience Database as the vector backend
        self.db = ExperienceDatabase()
        logger.info("SemanticCache initialized using ExperienceDatabase vector backend")

    async def query_similar(self, prompt: str, task_type: str = "general") -> CacheEntry | None:
        try:
            # বাংলা মন্তব্য: কাজের ধরণের ওপর ভিত্তি করে ডাইনামিক থ্রেশহোল্ড সেট করা হচ্ছে
            threshold = get_cache_threshold(task_type)

            hits = self.db.find_similar(prompt, limit=1, threshold=threshold)
            if hits:
                best_hit = hits[0]
                logger.info(
                    f"⚡ [SEMANTIC CACHE HIT] Task: {task_type} | Score: {best_hit['score']:.4f} | Source: {best_hit['source']}"
                )
                return CacheEntry(
                    provider=best_hit.get("source", "chroma"),
                    model="cached_semantic",
                    response=best_hit.get("response", "")
                )
            return None
        except Exception as e:  # noqa: BLE001
            logger.error(f"⚠️ SemanticCache lookup failed: {e}")
            return None

    async def set(self, prompt: str, response: str, task_type: str = "general") -> None:
        try:
            # বাংলা মন্তব্য: সফল ও ভেরিফাইড কোড/রেসপন্স এক্সপেরিয়েন্স ডেটাবেসে রাইট করা হচ্ছে
            exp = Experience(
                request=prompt,
                generated_code=response if "code" in task_type.lower() else None,
                action_taken=response if "code" not in task_type.lower() else "Code Generated",
                result="success"
            )
            self.db.record_experience(exp)
            logger.info(f"💾 Successfully recorded successful experience pattern for {task_type}")
        except Exception as e:  # noqa: BLE001
            logger.error(f"❌ Failed to save experience pattern: {e}")

```