# 📄 ফাইল: backend/memory/checkpoint_resume.py

**প্রকার:** .py  
**সাইজ:** 795 বাইট  
**আপডেট:** 2026-07-07T15:42:20.853647

---

## কোড

```py
from tools.checkpoint_manager import CheckpointManager


class CheckpointResume:
    def __init__(self, db_path: str | None = None):
        self.manager = CheckpointManager(db_path=db_path)

    def save(self, task_id: str, step_index: int, state: dict):
        return self.manager.save(task_id, step_index, state)

    def load(self, task_id: str):
        checkpoint = self.manager.load(task_id)
        if not checkpoint:
            return None
        return {
            "task_id": checkpoint.task_id,
            "step_index": checkpoint.step_index,
            "state": checkpoint.state,
            "resumed": checkpoint.resumed,
        }

    def list_all(self):
        return self.manager.list_all()

    def clear(self, task_id: str):
        return self.manager.clear(task_id)

```