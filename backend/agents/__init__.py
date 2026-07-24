#!/usr/bin/env python3
"""
agents/__init__.py
==================
SupremeAI 2.0 — Agent Package Initialization

বাংলা মন্তব্য: সমস্ত এজেন্ট ক্লাস এবং ইউটিলিটি এই প্যাকেজ থেকে এক্সপোর্ট করা হয়।
নতুন এজেন্ট যোগ করলে এখানে রেজিস্টার করতে হবে।
"""

from __future__ import annotations

from agents.churn_prophet import ChurnProphet
from agents.ephemeral_executor import EphemeralExecutor
from agents.headless_terminal_agent import HeadlessTerminalAgent
from agents.insight_mage import InsightMage
from agents.morphic_adapter import MorphicAdapter
from agents.performance_guardian import PerformanceGuardian
from agents.sentinel_agent import SentinelAgent
from agents.skill_gc import SkillGarbageCollector
from agents.skill_ingestor import SkillIngestor
from agents.skill_librarian import SkillLibrarian
from agents.vulnerability_prophet import VulnerabilityProphet

__all__ = [
    "ChurnProphet",
    "EphemeralExecutor",
    "ExecutionResult",
    "ExecutionStatus",
    "HeadlessTerminalAgent",
    "InsightMage",
    "MorphicAdapter",
    "PerformanceGuardian",
    "ResourceQuota",
    "SecurityScanner",
    "SentinelAgent",
    "SkillGarbageCollector",
    "SkillIngestor",
    "SkillLibrarian",
    "VulnerabilityProphet",
]

# Re-export ephemeral executor types for convenience
from agents.ephemeral_executor import (ExecutionResult, ExecutionStatus,
                                       ResourceQuota, SecurityScanner)

# Lazy registry for agent discovery
_AGENT_REGISTRY: dict[str, type] = {
    "churn_prophet": ChurnProphet,
    "ephemeral_executor": EphemeralExecutor,
    "headless_terminal": HeadlessTerminalAgent,
    "insight_mage": InsightMage,
    "morphic_adapter": MorphicAdapter,
    "performance_guardian": PerformanceGuardian,
    "sentinel": SentinelAgent,
    "skill_gc": SkillGarbageCollector,
    "skill_ingestor": SkillIngestor,
    "skill_librarian": SkillLibrarian,
    "vulnerability_prophet": VulnerabilityProphet,
}


def get_agent_class(agent_name: str) -> type | None:
    """Retrieve agent class by name from the registry."""
    return _AGENT_REGISTRY.get(agent_name.lower())


def list_agents() -> list[str]:
    """List all registered agent names."""
    return sorted(_AGENT_REGISTRY.keys())


def register_agent(name: str, agent_class: type) -> None:
    """Dynamically register a new agent type."""
    _AGENT_REGISTRY[name.lower()] = agent_class
