# ruff: noqa: E501
"""
SupremeAI — Code-to-Database Sync Pipeline
==========================================

Syncs code changes to database for tracking and analysis.
- Incremental code indexing
- Change detection
- Metadata extraction
- Zero-cost: uses Upstash Redis for state tracking
"""

from __future__ import annotations

import hashlib
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

from core.cache import get_cache
from loguru import logger

# ── Constants ────────────────────────────────────────────────────────────────
SYNC_CACHE_TTL = 86400  # 24 hours


class CodeToDBSync:
    """
    Synchronizes codebase to database for context awareness.
    """

    def __init__(self) -> None:
        self.cache = get_cache()
        self._last_sync_key = "code_sync:last_run"
        self._file_hashes_key = "code_sync:file_hashes"

    async def sync_project(self, project_path: str) -> dict[str, Any]:
        """
        Sync project files to database.

        Args:
            project_path: Path to project root.

        Returns:
            Sync summary.
        """
        project = Path(project_path)
        if not project.exists():
            return {"status": "error", "message": "Project not found"}

        # Get last sync state
        await self.cache.get(self._last_sync_key)
        file_hashes = await self.cache.get(self._file_hashes_key) or {}

        changed_files = []
        current_hashes = {}

        for py_file in project.rglob("*.py"):
            # Skip hidden directories
            if any(p.startswith(".") for p in py_file.parts):
                continue

            try:
                content = py_file.read_text(encoding="utf-8", errors="ignore")
                file_hash = hashlib.sha256(content.encode()).hexdigest()[:16]
                current_hashes[str(py_file)] = file_hash

                if file_hashes.get(str(py_file)) != file_hash:
                    changed_files.append(str(py_file))
            except Exception as e:
                logger.debug(f"Failed to hash {py_file}: {e}")

        # Update state
        await self.cache.set(self._file_hashes_key, current_hashes, ttl=SYNC_CACHE_TTL)
        await self.cache.set(
            self._last_sync_key, datetime.now(UTC).isoformat(), ttl=SYNC_CACHE_TTL
        )

        return {
            "status": "success",
            "project": project_path,
            "files_processed": len(current_hashes),
            "changed_files": len(changed_files),
            "changed_file_list": changed_files[:20],  # Top 20
            "synced_at": datetime.now(UTC).isoformat(),
        }

    async def get_file_metadata(self, file_path: str) -> dict[str, Any]:
        """Extract metadata from file for database storage."""
        path = Path(file_path)
        if not path.exists():
            return {}

        content = path.read_text(encoding="utf-8", errors="ignore")

        # Extract imports
        import re

        imports = re.findall(r"^import\s+(\w+)|^from\s+(\w+)", content, re.MULTILINE)

        # Extract classes and functions
        classes = re.findall(r"^class\s+(\w+)", content, re.MULTILINE)
        functions = re.findall(r"^def\s+(\w+)", content, re.MULTILINE)

        return {
            "file_path": file_path,
            "language": "python",
            "size_bytes": len(content),
            "imports": [i[0] or i[1] for i in imports],
            "classes": classes,
            "functions": functions,
            "last_indexed": datetime.now(UTC).isoformat(),
        }


# Singleton
_sync_instance: CodeToDBSync | None = None


def get_code_sync() -> CodeToDBSync:
    """Get or create the singleton CodeToDBSync instance."""
    global _sync_instance
    if _sync_instance is None:
        _sync_instance = CodeToDBSync()
    return _sync_instance
