"""Tests for Hierarchical Memory Tree Engine."""

import pytest
from backend.memory.hierarchical_tree import HierarchicalMemoryTree


@pytest.fixture
def memory_tree():
    tree = HierarchicalMemoryTree(root_title="SupremeAI Test Root")
    return tree


def test_memory_tree_lifecycle_and_rollup(memory_tree):
    # 1. Add branches
    dev_branch = memory_tree.add_branch(title="Frontend Improvements", category="dev", tags=["react", "vite"])
    db_branch = memory_tree.add_branch(title="Database Indexing", category="database", tags=["postgres", "indexes"])

    assert dev_branch.id in memory_tree.root.children_ids
    assert db_branch.id in memory_tree.root.children_ids

    # 2. Add leaves
    leaf1 = memory_tree.add_leaf(
        title="CommandCenter Store Refactor",
        content="Migrated CommandCenter state to Zustand with full immutability guarantees.",
        branch_id=dev_branch.id,
        category="dev",
        tags=["frontend", "state"],
    )
    leaf2 = memory_tree.add_leaf(
        title="Playwright E2E Canvas Test",
        content="Added multi-workspace canvas drag-and-drop smoke tests.",
        branch_id=dev_branch.id,
        category="dev",
        tags=["e2e", "testing"],
    )

    # 3. Check hierarchical rollup
    branch_node = memory_tree.nodes[dev_branch.id]
    assert "CommandCenter Store Refactor" in branch_node.summary
    assert "Playwright E2E Canvas Test" in branch_node.summary
    assert "Frontend Improvements" in memory_tree.root.summary or "CommandCenter Store Refactor" in memory_tree.root.summary


def test_search_and_subtree(memory_tree):
    branch = memory_tree.add_branch(title="Security & Secrets", category="security", tags=["auth", "tokens"])
    memory_tree.add_leaf(
        title="Gitleaks Pre-commit Hook",
        content="Configured gitleaks scanning on git pre-commit hooks to block API key leaks.",
        branch_id=branch.id,
        category="security",
        tags=["security", "git"],
    )

    # Search by tag
    results = memory_tree.search_by_tag_or_category(tag="git")
    assert len(results) >= 1
    assert results[0].title == "Gitleaks Pre-commit Hook"

    # Semantic/keyword search
    search_res = memory_tree.search_semantic_text("pre-commit hooks scanning")
    assert len(search_res) >= 1
    assert "Gitleaks" in search_res[0].title

    # Subtree JSON representation
    subtree = memory_tree.get_subtree("root")
    assert subtree["id"] == "root"
    assert len(subtree["children"]) >= 1


def test_export_to_markdown_vault(memory_tree):
    branch = memory_tree.add_branch(title="Deployment Pipeline", category="devops", tags=["ci", "docker"])
    memory_tree.add_leaf(
        title="FastAPI Lifespan AutoHealer",
        content="Replaced legacy CLI cron with native FastAPI lifespan worker loop.",
        branch_id=branch.id,
        category="devops",
    )

    vault = memory_tree.export_to_markdown_vault()
    assert "Index.md" in vault
    assert "devops/Deployment Pipeline.md" in vault
    assert "FastAPI Lifespan AutoHealer" in vault["devops/Deployment Pipeline.md"]
