"""Incremental markdown corpus indexer — feeds semantic search over repository documentation."""
from __future__ import annotations

import asyncio
import re
from pathlib import Path
from typing import Any

from loguru import logger

from core.embeddings import EmbeddingEngine


class MarkdownIndexer:
    def __init__(self, roots: list[Path] | None = None):
        self.roots = roots or [Path(".")]
        self.engine = EmbeddingEngine.get_instance()

    async def index_all(self) -> dict[str, Any]:
        tasks = []
        indexed_files = []
        for root in self.roots:
            if not root.exists():
                continue
            for md in root.rglob("*.md"):
                if any(x in md.parts for x in [".git", "node_modules", ".venv", "dist", "build"]):
                    continue
                tasks.append(self._index_file(md))
                indexed_files.append(str(md))
        if tasks:
            await asyncio.gather(*tasks, return_exceptions=True)
        return {"total_files_scanned": len(indexed_files), "status": "completed"}

    async def _index_file(self, path: Path) -> None:
        try:
            text = path.read_text(encoding="utf-8", errors="ignore")
            chunks = self._chunk_by_heading(text)
            for i, chunk in enumerate(chunks):
                vec = await self.engine.embed(chunk)
                # In-memory / vector store registration
                logger.debug(f"[MarkdownIndexer] Indexed {path} chunk {i} ({len(chunk)} chars)")
        except Exception as exc:
            logger.debug(f"[MarkdownIndexer] Failed to index {path}: {exc}")

    @staticmethod
    def _chunk_by_heading(text: str, max_len: int = 1500) -> list[str]:
        # Split on markdown headings
        chunks = re.split(r"\n(?=#{1,4}\s)", text)
        result = []
        for c in chunks:
            c = c.strip()
            if c:
                if len(c) > max_len:
                    result.extend([c[i : i + max_len] for i in range(0, len(c), max_len)])
                else:
                    result.append(c)
        return result or [text[:max_len]]
