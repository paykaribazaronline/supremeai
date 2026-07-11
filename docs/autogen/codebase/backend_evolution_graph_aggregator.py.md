# 📄 ফাইল: backend/evolution/graph_aggregator.py

**প্রকার:** .py  
**সাইজ:** 2,229 বাইট  
**আপডেট:** 2026-07-11T19:00:24.702301

---

## কোড

```py
import json

from core.services import registry


class GraphAggregator:
    def __init__(self):
        self.redis = registry.get_service("redis_manager")
        self.CACHE_KEY = "swarm_graph_state"

    def _compute_delta(self, old_state: dict, new_state: dict) -> dict:
        """দুটি স্টেটের মধ্যে পার্থক্য বের করে শুধুমাত্র চেঞ্জগুলো রিটার্ন করে।"""
        delta = {"added": {"nodes": [], "edges": []}, "removed": {"nodes": [], "edges": []}}

        old_nodes = {n["id"]: n for n in old_state.get("nodes", [])}
        new_nodes = {n["id"]: n for n in new_state.get("nodes", [])}

        # Nodes Delta
        delta["added"]["nodes"] = [new_nodes[n] for n in new_nodes if n not in old_nodes]
        delta["removed"]["nodes"] = [old_nodes[n] for n in old_nodes if n not in new_nodes]

        # Edges Delta (সহজ করার জন্য ID-র বদলে source+target কী ব্যবহার করছি)
        old_edges = {(e["source"], e["target"]): e for e in old_state.get("edges", [])}
        new_edges = {(e["source"], e["target"]): e for e in new_state.get("edges", [])}

        delta["added"]["edges"] = [new_edges[e] for e in new_edges if e not in old_edges]
        delta["removed"]["edges"] = [old_edges[e] for e in old_edges if e not in new_edges]

        return delta

    async def get_swarm_delta(self, current_full_graph: dict) -> dict:
        """মেইন এন্ট্রি পয়েন্ট: ক্যাশ থেকে পুরনো স্টেট এনে ডিফারেন্স ক্যালকুলেট করে।"""
        if not self.redis:
            return current_full_graph

        cached_json = await self.redis.get_cache(self.CACHE_KEY)
        old_state = json.loads(cached_json) if cached_json else {"nodes": [], "edges": []}

        # ডেল্টা ক্যালকুলেট
        delta = self._compute_delta(old_state, current_full_graph)

        # ক্যাশ আপডেট
        await self.redis.set_cache(self.CACHE_KEY, json.dumps(current_full_graph), ex_seconds=3600)

        return delta

```