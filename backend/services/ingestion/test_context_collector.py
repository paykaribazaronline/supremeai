"""Tests for Developer Context Auto-Ingestor Service."""

import pytest

from backend.services.ingestion.context_collector import (
    DeveloperContextCollector,
    WorkspaceSnapshot,
)


@pytest.fixture
def collector():
    return DeveloperContextCollector()


def test_developer_context_collector_snapshot(collector):
    snapshot = collector.capture_snapshot()
    assert isinstance(snapshot, WorkspaceSnapshot)
    assert snapshot.active_branch is not None
    assert isinstance(snapshot.modified_files, list)
    assert isinstance(snapshot.recent_commits, list)

    # Verify that memory tree has recorded the snapshot
    tree = collector.memory_tree
    dev_branches = [b for b in tree.nodes.values() if b.level == 1 and b.category == "dev"]
    assert len(dev_branches) >= 1
    assert "Workspace Snapshot" in dev_branches[0].title


def test_developer_context_collector_git_methods(collector):
    branch = collector.get_git_branch()
    assert isinstance(branch, str)

    modified, untracked = collector.get_git_status()
    assert isinstance(modified, list)
    assert isinstance(untracked, list)

    commits = collector.get_recent_commits(3)
    assert isinstance(commits, list)
