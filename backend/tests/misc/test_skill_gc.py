import sys
import tarfile
from unittest.mock import MagicMock

import pytest

# Import guard: agents package init may import optional google.genai.
if "google" not in sys.modules:
    sys.modules["google"] = MagicMock()
if "google.genai" not in sys.modules:
    sys.modules["google.genai"] = MagicMock()

from agents.skill_gc import SkillGarbageCollector


@pytest.mark.anyio
async def test_skill_garbage_collector_archives_and_removes(tmp_path, monkeypatch):
    base = tmp_path / "skills"
    (base / "approved").mkdir(parents=True)
    (base / "archive").mkdir(parents=True)

    # Create dummy skill dir
    skill_dir = base / "approved" / "old_skill"
    skill_dir.mkdir()
    (skill_dir / "file.py").write_text("x=1")

    # Patch index manager + manifest
    class FakeIndex:
        def __init__(self):
            self.path = str(base / "index.json")

        def load_index(self):
            return {
                "old_skill": {
                    "usage_count": 1,
                    "is_pinned": False,
                    "is_system": False,
                    "status": "deprecated_pending",
                    "created_at": "2000-01-01T00:00:00",
                    "last_used_at": "2000-01-01T00:00:00",
                }
            }

        def update_skill(self, manifest):
            return True

    # Patch SkillManifest construction via monkeypatching class used in skill_gc
    from agents import skill_gc as mod

    class FakeManifest:
        def __init__(self, **meta):
            self.__dict__.update(meta)
            self.usage_count = meta.get("usage_count")
            self.is_pinned = meta.get("is_pinned", False)
            self.created_at = meta.get("created_at")
            self.last_used_at = meta.get("last_used_at")
            self.status = meta.get("status")

    monkeypatch.setattr(mod, "SkillIndexManager", FakeIndex)
    monkeypatch.setattr(mod, "SkillManifest", FakeManifest)

    # Patch SkillStatus values
    class FakeSkillStatus:
        APPROVED = "approved"
        DEPRECATED_PENDING = "deprecated_pending"

    monkeypatch.setattr(mod, "SkillStatus", FakeSkillStatus)

    # SkillGarbageCollector তৈরি করা হচ্ছে — patches সব সক্রিয় থাকা অবস্থায়
    gc = SkillGarbageCollector(base_skills_dir=str(base))

    # GC is 2-phase:
    # 1) APPROVED -> DEPRECATED_PENDING
    # 2) DEPRECATED_PENDING -> archive + purge
    # So run twice to ensure purge happens.
    gc.run_daily_cleanup(usage_threshold=5, days_threshold=1)
    purged = gc.run_daily_cleanup(usage_threshold=5, days_threshold=1)
    assert "old_skill" in purged

    # Ensure archive exists
    archive_files = list((base / "archive").glob("old_skill_*.tar.gz"))
    assert len(archive_files) == 1

    with tarfile.open(archive_files[0], "r:gz") as tf:
        assert any(m.name.startswith("old_skill") for m in tf.getmembers())
