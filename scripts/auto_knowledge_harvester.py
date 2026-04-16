import firebase_admin
from firebase_admin import credentials, firestore
from datetime import datetime, timedelta
import hashlib
import json
import asyncio
import aiohttp

# Initialize Firebase
firebase_admin.initialize_app()
db = firestore.client()

PROMPT = open("universal_knowledge_seed_prompt.md").read().split("```")[1]


class KnowledgeHarvester:
    def __init__(self):
        self.knowledge_ref = db.collection("system_knowledge")
        self.provider_ref = db.collection("ai_providers")
        self.harvest_log = db.collection("harvest_logs")

    def hash_item(self, item):
        """Generate stable hash for knowledge item deduplication"""
        norm = json.dumps(
            {
                "title": item["title"].lower().strip(),
                "category": item["category"].lower().strip(),
            },
            sort_keys=True,
        )
        return hashlib.sha256(norm.encode()).hexdigest()

    def merge_best_item(self, existing, new_item, model_id):
        """Keep highest confidence, most detailed entry"""
        if new_item["confidence"] > existing.get("confidence", 0):
            merged = existing.copy()
            merged.update(new_item)
        else:
            merged = existing
            for k in ["verification_steps", "anti_patterns", "description"]:
                if k in new_item and len(str(new_item[k])) > len(
                    str(merged.get(k, ""))
                ):
                    merged[k] = new_item[k]

        if "source_models" not in merged:
            merged["source_models"] = []
        if model_id not in merged["source_models"]:
            merged["source_models"].append(model_id)

        merged["last_updated"] = datetime.utcnow().isoformat()
        merged["update_count"] = merged.get("update_count", 0) + 1
        return merged

    async def harvest_from_model(self, model_id, endpoint, api_key):
        """Harvest all knowledge from a single AI model"""
        log_doc = self.harvest_log.document()
        log_doc.set(
            {"model_id": model_id, "started_at": datetime.utcnow(), "status": "running"}
        )

        try:
            async with aiohttp.ClientSession() as session:
                async with session.post(
                    endpoint,
                    headers={
                        "Authorization": f"Bearer {api_key}",
                        "Content-Type": "application/json",
                    },
                    json={
                        "model": model_id,
                        "messages": [{"role": "user", "content": PROMPT}],
                        "temperature": 0.0,
                        "max_tokens": 128000,
                    },
                ) as resp:
                    data = await resp.json()

            content = data["choices"][0]["message"]["content"]
            items = json.loads(f"[{content.split('[')[1].split(']')[0]}]")

            inserted, merged, skipped = 0, 0, 0

            for item in items:
                item_hash = self.hash_item(item)
                existing = self.knowledge_ref.document(item_hash).get()

                if existing.exists:
                    merged_item = self.merge_best_item(
                        existing.to_dict(), item, model_id
                    )
                    self.knowledge_ref.document(item_hash).set(merged_item)
                    merged += 1
                else:
                    item["id"] = item_hash
                    item["source_models"] = [model_id]
                    item["first_seen"] = datetime.utcnow().isoformat()
                    item["last_updated"] = datetime.utcnow().isoformat()
                    self.knowledge_ref.document(item_hash).set(item)
                    inserted += 1

            log_doc.update(
                {
                    "status": "completed",
                    "items_inserted": inserted,
                    "items_merged": merged,
                    "completed_at": datetime.utcnow(),
                }
            )

            return inserted, merged

        except Exception as e:
            log_doc.update(
                {"status": "failed", "error": str(e), "completed_at": datetime.utcnow()}
            )
            raise

    async def run_continuous_harvest(self):
        """Run harvest loop across all connected AI providers"""
        while True:
            providers = [
                p.to_dict()
                for p in self.provider_ref.where("enabled", "==", True).stream()
            ]

            tasks = []
            for provider in providers:
                for model in provider.get("models", []):
                    tasks.append(
                        self.harvest_from_model(
                            model_id=model,
                            endpoint=provider["endpoint"],
                            api_key=provider["api_key"],
                        )
                    )

            await asyncio.gather(*tasks, return_exceptions=True)
            await asyncio.sleep(3600)  # Run every hour


if __name__ == "__main__":
    harvester = KnowledgeHarvester()
    asyncio.run(harvester.run_continuous_harvest())
