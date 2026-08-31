"""SupremeAI Ecosystem Foundation — autonomous, centrally-governed subsystem.

বাংলা: এই প্যাকেজটি ROADMAP-এ বর্ণিত autonomous ecosystem-এর foundation layer।
এটি বিদ্যমান SupremeAI FastAPI ব্যাকএন্ডের সাথে drop-in হিসেবে কাজ করে এবং
নিচের building block গুলো সরবরাহ করে:

  - Capability Registry   (REUSE > ADAPT > EXTEND > CREATE)
  - Resource Registry      (dynamic resource_id, provider adapter base)
  - Task Engine            (state machine + verification + repair)
  - Source Governance      (permission-first learning, allowlist, budget)
  - Approval Workflow      (admin decision memory, dedup, cooldown, policy)
  - Unified Health Model   (HEALTHY/DEGRADED/WARNING/CRITICAL/UNKNOWN/MAINTENANCE)
  - Deployment Tracker     (correlation IDs across user→task→cap→resource→deploy)
  - MCP Skeleton           (generic control op → registry → adapter; NOT god object)
  - Governance             (safe/low-risk/high-risk + budgets)
  - Learning Loop          (discovery → policy → research → proposal → approve → build → register → reuse)
  - Correlation IDs        (request_id, task_id, job_id, deployment_id, capability_id, audit_id)

ডিজাইন নীতি (ROADMAP §1): "Design for the future; implement only what the
current phase actually needs." তাই এই প্যাকেজটি foundation ই দেয় —
distributed render, heavy Kaggle compute, foundation-model retraining ইত্যাদি
later phase-এ এই registry-র উপরে বসবে।

সব টেবিল self-contained SQLite (pending_tasks.py-র একই প্যাটার্ন) — Alembic
migration ছাড়াই idempotent auto-create হয়, তাই production-এ zero-risk deploy।
"""

from __future__ import annotations

from ecosystem.capability_registry import (
    Capability,
    CapabilityLifecycleState,
    CapabilityRuntimeTier,
    CapabilityRegistry,
    get_capability_registry,
)
from ecosystem.correlation import (
    CorrelationContext,
    bind_correlation,
    current_correlation,
    new_correlation_context,
)
from ecosystem.deployment_tracker import (
    DeploymentRecord,
    DeploymentTracker,
    get_deployment_tracker,
)
from ecosystem.governance import (
    ActionRisk,
    BudgetKind,
    Budgets,
    GovernanceEngine,
    RiskDecision,
    get_governance_engine,
)
from ecosystem.health_model import (
    HealthStatus,
    UnifiedHealth,
    HealthAggregator,
    get_health_aggregator,
)
from ecosystem.learning_loop import (
    LearningLoop,
    LearningStage,
    get_learning_loop,
)
from ecosystem.mcp_skeleton import (
    MCPOperationCategory,
    MCPSkeleton,
    get_mcp_skeleton,
)
from ecosystem.resource_registry import (
    ProviderKind,
    ResourceRecord,
    ResourceRegistry,
    ResourceState,
    BaseProviderAdapter,
    get_resource_registry,
)
from ecosystem.source_governance import (
    LearnedItem,
    SourceCategory,
    SourceGovernance,
    SourcePolicy,
    SourceState,
    get_source_governance,
)
from ecosystem.task_engine import (
    TaskEngine,
    TaskOwner,
    TaskRecord,
    TaskState,
    get_task_engine,
)
from ecosystem.approval_workflow import (
    ApprovalDecision,
    ApprovalProposal,
    ApprovalWorkflow,
    ProposalKind,
    ProposalPriority,
    ProposalState,
    get_approval_workflow,
)

__version__ = "1.0.0"

__all__ = [
    # capability
    "Capability",
    "CapabilityLifecycleState",
    "CapabilityRuntimeTier",
    "CapabilityRegistry",
    "get_capability_registry",
    # correlation
    "CorrelationContext",
    "current_correlation",
    "bind_correlation",
    "new_correlation_context",
    # deployment
    "DeploymentRecord",
    "DeploymentTracker",
    "get_deployment_tracker",
    # governance
    "ActionRisk",
    "BudgetKind",
    "Budgets",
    "GovernanceEngine",
    "RiskDecision",
    "get_governance_engine",
    # health
    "HealthStatus",
    "UnifiedHealth",
    "HealthAggregator",
    "get_health_aggregator",
    # learning
    "LearningLoop",
    "LearningStage",
    "get_learning_loop",
    # mcp
    "MCPOperationCategory",
    "MCPSkeleton",
    "get_mcp_skeleton",
    # resource
    "ProviderKind",
    "ResourceRecord",
    "ResourceRegistry",
    "ResourceState",
    "BaseProviderAdapter",
    "get_resource_registry",
    # source governance
    "LearnedItem",
    "SourceCategory",
    "SourceState",
    "SourcePolicy",
    "SourceGovernance",
    "get_source_governance",
    # task
    "TaskEngine",
    "TaskOwner",
    "TaskRecord",
    "TaskState",
    "get_task_engine",
    # approval
    "ApprovalDecision",
    "ApprovalProposal",
    "ApprovalWorkflow",
    "ProposalKind",
    "ProposalPriority",
    "ProposalState",
    "get_approval_workflow",
    "__version__",
]
