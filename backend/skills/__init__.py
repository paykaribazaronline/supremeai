# backend/skills/__init__.py
"""SupremeAI Skills Package (Backward Compatibility Facade).

Unifies dynamic skill provisioning with core skills.
"""

from __future__ import annotations

from core.skills import (
    BaseSkill,
    CodeGenerationSkill,
    ExperiencePersistenceSkill,
    GithubSyncSkill,
    NotionSyncSkill,
    ResearchSkill,
    SlackIntegrationSkill,
    StaticAnalysisSkill,
    SystemDesignSkill,
    ToolExecutionSkill,
    ToolSynthesisSkill,
)
from skills.provisioner import SkillProvisioner
from skills.skill_registry import SkillRegistry

__all__ = [
    "BaseSkill",
    "SystemDesignSkill",
    "CodeGenerationSkill",
    "StaticAnalysisSkill",
    "ResearchSkill",
    "ToolSynthesisSkill",
    "ToolExecutionSkill",
    "ExperiencePersistenceSkill",
    "SlackIntegrationSkill",
    "NotionSyncSkill",
    "GithubSyncSkill",
    "SkillRegistry",
    "SkillProvisioner",
]
