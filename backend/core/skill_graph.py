# backend/core/skill_graph.py
"""Semantic Skill Graph module.

Provides an in‑memory directed graph of skills based on their declared
`dependencies` and `compatibility` fields. The graph is used by the
Orchestrator to resolve execution order of dependent skills.

Only a lightweight prototype is required for now – it stores the graph
in memory using :pypi:`networkx` and offers utility methods for adding,
removing and topologically sorting skills.
"""

from __future__ import annotations

from typing import Any

import networkx as nx


class SkillGraph:
    """Manage a directed acyclic graph of skill dependencies.

    Nodes are skill identifiers (str). Each node stores the original skill
    metadata (dict) under the ``metadata`` attribute. Edges are added from a
    dependency to the dependent skill, i.e. ``dep -> skill``.
    """

    def __init__(self) -> None:
        self._graph: nx.DiGraph = nx.DiGraph()

    def add_skill(self, skill_id: str, metadata: dict[str, Any] | None = None) -> None:
        """Add a skill to the graph.

        Parameters
        ----------
        skill_id: str
            Unique identifier for the skill.
        metadata: dict, optional
            Arbitrary skill metadata. Expected keys include ``dependencies``
            (list of skill IDs) and ``compatibility`` (list or str).
        """
        if metadata is None:
            metadata = {}
        self._graph.add_node(skill_id, metadata=metadata)
        deps = metadata.get("dependencies", []) or []
        for dep in deps:
            if not self._graph.has_node(dep):
                self._graph.add_node(dep, metadata={})
            self._graph.add_edge(dep, skill_id)
        if not nx.is_directed_acyclic_graph(self._graph):
            self._graph.remove_node(skill_id)
            raise ValueError(f"Adding skill '{skill_id}' creates a cycle in the skill graph")

    def remove_skill(self, skill_id: str) -> None:
        """Remove a skill and all incident edges from the graph."""
        self._graph.remove_node(skill_id)

    def get_skill_metadata(self, skill_id: str) -> dict[str, Any] | None:
        if self._graph.has_node(skill_id):
            return self._graph.nodes[skill_id].get("metadata")
        return None

    def resolve_execution_order(self) -> list[str]:
        """Return a topologically sorted list of skill IDs respecting dependencies."""
        if not nx.is_directed_acyclic_graph(self._graph):
            raise ValueError("Skill graph contains cycles – cannot resolve order")
        return list(nx.topological_sort(self._graph))

    def __repr__(self) -> str:  # pragma: no cover
        return f"SkillGraph(nodes={list(self._graph.nodes)})"
