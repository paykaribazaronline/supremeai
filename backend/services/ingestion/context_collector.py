"""Developer Context Auto-Ingestor Service.

Inspired by OpenHuman's multi-app integration & context ingestion loops,
this service continuously and asynchronously collects workspace context,
git repository state, recent commits, and active file diffs, feeding them
into SupremeAI's Hierarchical Memory Tree and pgvector memory.
"""

from __future__ import annotations

import asyncio
import os
import subprocess
import time
from dataclasses import dataclass, field
from loguru import logger

from backend.engine.compression.token_juice import TokenJuice
from backend.memory.hierarchical_tree import HierarchicalMemoryTree


@dataclass
class WorkspaceSnapshot:
    """Snapshot of current workspace and git status."""
    timestamp: float = field(default_factory=time.time)
    active_branch: str = "main"
    modified_files: list[str] = field(default_factory=list)
    untracked_files: list[str] = field(default_factory=list)
    recent_commits: list[str] = field(default_factory=list)
    compressed_diff_summary: str = ""
    total_uncommitted_changes: int = 0


class DeveloperContextCollector:
    """Continuous background collector for developer repository and workspace context."""

    def __init__(
        self,
        workspace_root: str | None = None,
        memory_tree: HierarchicalMemoryTree | None = None,
        compressor: TokenJuice | None = None,
    ):
        self.workspace_root = workspace_root or os.getcwd()
        self.memory_tree = memory_tree or HierarchicalMemoryTree(root_title="SupremeAI Live Context")
        self.compressor = compressor or TokenJuice()
        self._is_running = False
        self._last_snapshot: WorkspaceSnapshot | None = None

    def get_git_branch(self) -> str:
        """Fetch current git branch name."""
        try:
            res = subprocess.run(
                ["git", "rev-parse", "--abbrev-ref", "HEAD"],
                cwd=self.workspace_root,
                capture_output=True,
                text=True,
                timeout=3,
                check=False,
            )
            if res.returncode == 0:
                return res.stdout.strip()
        except Exception as e:
            logger.debug(f"Git branch fetch error: {e}")
        return "unknown"

    def get_git_status(self) -> tuple[list[str], list[str]]:
        """Fetch modified and untracked files."""
        modified = []
        untracked = []
        try:
            res = subprocess.run(
                ["git", "status", "--porcelain"],
                cwd=self.workspace_root,
                capture_output=True,
                text=True,
                timeout=3,
                check=False,
            )
            if res.returncode == 0:
                for line in res.stdout.splitlines():
                    status = line[:2]
                    file_path = line[3:].strip()
                    if "??" in status:
                        untracked.append(file_path)
                    else:
                        modified.append(file_path)
        except Exception as e:
            logger.debug(f"Git status fetch error: {e}")
        return modified, untracked

    def get_recent_commits(self, count: int = 5) -> list[str]:
        """Fetch recent commit messages."""
        commits = []
        try:
            res = subprocess.run(
                ["git", "log", f"-n{count}", "--oneline"],
                cwd=self.workspace_root,
                capture_output=True,
                text=True,
                timeout=3,
                check=False,
            )
            if res.returncode == 0:
                commits = [line.strip() for line in res.stdout.splitlines() if line.strip()]
        except Exception as e:
            logger.debug(f"Git commit fetch error: {e}")
        return commits

    def get_git_diff_summary(self) -> str:
        """Fetch and compress unstaged & staged diffs using TokenJuice."""
        try:
            res = subprocess.run(
                ["git", "diff", "HEAD"],
                cwd=self.workspace_root,
                capture_output=True,
                text=True,
                timeout=5,
                check=False,
            )
            if res.returncode == 0 and res.stdout:
                compressed = self.compressor.compress(res.stdout, content_type="git_diff")
                return compressed.compressed_text
        except Exception as e:
            logger.debug(f"Git diff fetch error: {e}")
        return ""

    def capture_snapshot(self) -> WorkspaceSnapshot:
        """Capture complete current developer context snapshot."""
        branch = self.get_git_branch()
        modified, untracked = self.get_git_status()
        commits = self.get_recent_commits(5)
        diff_summary = self.get_git_diff_summary()

        snapshot = WorkspaceSnapshot(
            active_branch=branch,
            modified_files=modified,
            untracked_files=untracked,
            recent_commits=commits,
            compressed_diff_summary=diff_summary,
            total_uncommitted_changes=len(modified) + len(untracked),
        )
        self._last_snapshot = snapshot

        # Ingest into memory tree
        branch_node = self.memory_tree.add_branch(
            title=f"Workspace Snapshot ({branch})",
            category="dev",
            tags=["git", "workspace", "live_context"],
        )
        self.memory_tree.add_leaf(
            title=f"Active State @ {time.strftime('%H:%M:%S')}",
            content=(
                f"Branch: {branch}\n"
                f"Modified Files ({len(modified)}): {', '.join(modified[:10])}\n"
                f"Recent Commits: {'; '.join(commits[:3])}\n"
                f"Diff Preview: {diff_summary[:400]}"
            ),
            branch_id=branch_node.id,
            category="dev",
            tags=["workspace_snapshot"],
        )

        return snapshot

    async def run_periodic_collector(self, interval_seconds: int = 300) -> None:
        """Background asynchronous periodic collection loop."""
        self._is_running = True
        while self._is_running:
            try:
                self.capture_snapshot()
            except Exception as e:
                logger.debug(f"Periodic snapshot error: {e}")
            await asyncio.sleep(interval_seconds)

    def stop(self) -> None:
        """Stop background collection loop."""
        self._is_running = False
