"""Hierarchical Memory Tree — Deterministic Multi-Tier Memory Engine.

Inspired by OpenHuman's hierarchical memory architecture, this module organizes
agent session memories, code insights, and system evolutions into a structured tree:
Level 0: Raw Leaf Observations (<3k tokens)
Level 1: Topic/Entity/Session Branches
Level 2: Global Root System Summary

Supports Markdown Vault export and semantic hierarchical retrieval.
"""

from __future__ import annotations

import json
import time
import uuid
from dataclasses import asdict, dataclass, field
from typing import Any, Dict, List, Optional


@dataclass
class MemoryNode:
    """A single node in the Hierarchical Memory Tree."""
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    level: int = 0  # 0 = Leaf, 1 = Branch/Topic, 2 = Root
    title: str = ""
    content: str = ""
    summary: str = ""
    category: str = "general"  # dev, business, ux, system, bugfix
    tags: List[str] = field(default_factory=list)
    parent_id: Optional[str] = None
    children_ids: List[str] = field(default_factory=list)
    metadata: Dict[str, Any] = field(default_factory=dict)
    created_at: float = field(default_factory=time.time)
    updated_at: float = field(default_factory=time.time)


class HierarchicalMemoryTree:
    """Deterministic Memory Tree with recursive rollup and Markdown Vault export."""

    def __init__(self, root_title: str = "SupremeAI Knowledge Root"):
        self.nodes: Dict[str, MemoryNode] = {}
        # Initialize root node
        self.root = MemoryNode(
            id="root",
            level=2,
            title=root_title,
            content="Global Knowledge Base Root for SupremeAI Self-Evolving Brain",
            summary="Root of all system, dev, and operational memories",
            category="system",
            tags=["root", "global"],
        )
        self.nodes["root"] = self.root

    def add_branch(self, title: str, category: str = "general", tags: Optional[List[str]] = None) -> MemoryNode:
        """Create a Level 1 topic/entity branch under root."""
        branch = MemoryNode(
            level=1,
            title=title,
            category=category,
            tags=tags or [],
            parent_id="root",
        )
        self.nodes[branch.id] = branch
        self.root.children_ids.append(branch.id)
        return branch

    def add_leaf(
        self,
        title: str,
        content: str,
        branch_id: Optional[str] = None,
        category: str = "general",
        tags: Optional[List[str]] = None,
        metadata: Optional[Dict[str, Any]] = None,
    ) -> MemoryNode:
        """Add a Level 0 leaf observation (<3000 tokens) under a branch or root."""
        # Truncate / summarize if content is very long
        summary = content[:280] + ("..." if len(content) > 280 else "")

        target_parent = branch_id if (branch_id and branch_id in self.nodes) else "root"

        leaf = MemoryNode(
            level=0,
            title=title,
            content=content,
            summary=summary,
            category=category,
            tags=tags or [],
            parent_id=target_parent,
            metadata=metadata or {},
        )
        self.nodes[leaf.id] = leaf
        self.nodes[target_parent].children_ids.append(leaf.id)
        
        # Roll up summary to parent
        self.rollup_summaries(target_parent)
        return leaf

    def rollup_summaries(self, node_id: str) -> None:
        """Deterministically fold children summaries upwards to update branch/root summaries."""
        if node_id not in self.nodes:
            return

        curr_node = self.nodes[node_id]
        if not curr_node.children_ids:
            return

        child_summaries = [
            f"- [{self.nodes[cid].title}]: {self.nodes[cid].summary}"
            for cid in curr_node.children_ids
            if cid in self.nodes
        ]
        
        curr_node.summary = "\n".join(child_summaries[:15])
        curr_node.updated_at = time.time()

        # If parent exists, propagate rollup to root
        if curr_node.parent_id and curr_node.parent_id in self.nodes:
            self.rollup_summaries(curr_node.parent_id)

    def search_by_tag_or_category(self, tag: Optional[str] = None, category: Optional[str] = None) -> List[MemoryNode]:
        """Search memory nodes matching tag or category."""
        results = []
        for node in self.nodes.values():
            if tag and tag.lower() in [t.lower() for t in node.tags]:
                results.append(node)
            elif category and node.category.lower() == category.lower():
                results.append(node)
        return results

    def search_semantic_text(self, query: str, top_k: int = 5) -> List[MemoryNode]:
        """Keyword/semantic overlap search through titles, tags, summaries, and contents."""
        query_words = set(w.lower() for w in query.split() if len(w) > 2)
        if not query_words:
            return list(self.nodes.values())[:top_k]

        scored: List[tuple[int, MemoryNode]] = []
        for node in self.nodes.values():
            if node.id == "root":
                continue
            
            score = 0
            # Title matches have highest weight (3x)
            title_lower = node.title.lower()
            score += 3 * sum(1 for w in query_words if w in title_lower)
            
            # Tag matches have 2x weight
            tag_text = " ".join(node.tags).lower()
            score += 2 * sum(1 for w in query_words if w in tag_text)
            
            # Summary / Content matches (1x)
            body_text = f"{node.summary} {node.content}".lower()
            score += sum(1 for w in query_words if w in body_text)

            if score > 0:
                scored.append((score, node))

        scored.sort(key=lambda x: x[0], reverse=True)
        return [item[1] for item in scored[:top_k]]

    def get_subtree(self, node_id: str) -> Dict[str, Any]:
        """Get structured hierarchical JSON representation of a node and all its descendants."""
        if node_id not in self.nodes:
            return {}

        node = self.nodes[node_id]
        return {
            "id": node.id,
            "level": node.level,
            "title": node.title,
            "summary": node.summary,
            "category": node.category,
            "tags": node.tags,
            "children": [self.get_subtree(cid) for cid in node.children_ids if cid in self.nodes],
        }

    def export_to_markdown_vault(self) -> Dict[str, str]:
        """Export the memory tree to an Obsidian-compatible Markdown vault format.
        
        Returns a mapping of virtual file paths -> Markdown content.
        """
        vault: Dict[str, str] = {}

        # 1. Global Index
        index_lines = [
            f"# {self.root.title}",
            "",
            f"**Last Updated:** {time.strftime('%Y-%m-%d %H:%M:%S', time.gmtime(self.root.updated_at))}",
            "",
            "## Summary",
            self.root.summary or "No summary available.",
            "",
            "## Topic Branches",
        ]
        
        for branch_id in self.root.children_ids:
            if branch_id in self.nodes:
                branch = self.nodes[branch_id]
                index_lines.append(f"- [[{branch.category}/{branch.title}|{branch.title}]] ({len(branch.children_ids)} entries)")

        vault["Index.md"] = "\n".join(index_lines)

        # 2. Branches & Leaves
        for node in self.nodes.values():
            if node.level == 1:  # Branch
                branch_path = f"{node.category}/{node.title}.md"
                content_lines = [
                    f"# {node.title}",
                    f"**Category:** `{node.category}` | **Tags:** {', '.join(node.tags)}",
                    "",
                    "## Branch Summary",
                    node.summary,
                    "",
                    "## Observations & Artifacts",
                ]
                for cid in node.children_ids:
                    if cid in self.nodes:
                        child = self.nodes[cid]
                        content_lines.append(f"### {child.title}")
                        content_lines.append(child.content)
                        content_lines.append("")
                vault[branch_path] = "\n".join(content_lines)

        return vault
