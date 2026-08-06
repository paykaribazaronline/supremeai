"""
agents/__init__.py
==================
SupremeAI 2.0 — Agent Package Initialization

বাংলা মন্তব্য: সমস্ত এজেন্ট ক্লাস এবং ইউটিলিটি এক্সপোর্ট করা হয়।
নতুন এজেন্ট যোগ করলে এখানে রেজিস্টার করতে হবে।
"""

from __future__ import annotations

# Fixed imports to use relative paths
from .churn_prophet import ChurnProphet
from .ephemeral_executor import EphemeralExecutor
from .headless_terminal_agent import HeadlessTerminalAgent
from .insight_mage import InsightMage
from .internet_monitor_agent import InternetMonitorAgent
from .morphic_adapter import MorphicAdapter
from .performance_guardian import PerformanceGuardian
from .sentinel_agent import SentinelAgent
from .skill_gc import SkillGarbageCollector
from .skill_ingestor import SkillIngestor
from .skill_librarian import SkillLibrarian
from .vulnerability_prophet import VulnerabilityProphet

__all__ = [
    "ChurnProphet",
    "EphemeralExecutor",
    "ExecutionResult",
    "ExecutionStatus",
    "HeadlessTerminalAgent",
    "InsightMage",
    "InternetMonitorAgent",
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
from .ephemeral_executor import (ExecutionResult, ExecutionStatus,
                                 ResourceQuota, SecurityScanner)
