"""Ecosystem — SupremeAI orchestration modules.

Phases 2-14 (ROADMAP §1-§57). Each module is self-contained and idempotent.
All persistence uses the shared SQLite store from ecosystem._store.
"""
from ecosystem._store import (
    get_conn, get_db_path, ensure_columns, jdump, jload,
)
from ecosystem.correlation import (
    CorrelationContext, current_correlation, new_correlation_context,
)
from ecosystem.capability_registry import (
    Capability, CapabilityLifecycleState, CapabilityRuntimeTier,
    CapabilityStateError, CapabilityExistsError, CapabilityRegistry,
    get_capability_registry,
)
from ecosystem.task_engine import (
    TaskState, TaskOwner, TaskRecord, TaskEngine, get_task_engine,
    TaskStateError, TaskNotFoundError, TaskRetryExceeded, TaskTimeoutError,
)
from ecosystem.resource_registry import (
    ProviderKind, ResourceState, ResourceRecord, BaseProviderAdapter,
    ResourceRegistry, get_resource_registry,
    ResourceExistsError, ResourceNotFoundError, AdapterNotRegisteredError,
)
from ecosystem.source_governance import (
    SourceState, SourceCategory, SourcePolicy, LearnedItem,
    SourceGovernance, get_source_governance, SourceStateError,
)
from ecosystem.approval_workflow import (
    ProposalKind, ProposalPriority, ProposalState, ApprovalProposal, ApprovalDecision,
    ApprovalWorkflow, get_approval_workflow, ProposalStateError, ProposalCooldownError,
)
from ecosystem.governance import (
    ActionRisk, BudgetKind, Budgets, RiskDecision,
    GovernanceEngine, get_governance_engine,
)
from ecosystem.health_model import (
    HealthStatus, MemoryInfo, UnifiedHealth, HealthAggregator, get_health_aggregator,
)
from ecosystem.deployment_tracker import (
    DeploymentStatus, DeploymentRecord, DeploymentTracker, get_deployment_tracker,
    DeploymentStateError, DeploymentNotFoundError,
)
from ecosystem.mcp_skeleton import (
    MCPOperationCategory, MCPSkeleton, get_mcp_skeleton,
    MCPOperationError, MCPOperationNotRegisteredError, MCPActionDenied,
)
from ecosystem.learning_loop import (
    LearningStage, EvolutionSignal, LearningOpportunity, LearningLoop,
    get_learning_loop, LearningStageError,
)


__all__ = [
    # storage (shared)
    "get_conn", "get_db_path", "ensure_columns", "jdump", "jload",
    # correlation (Phase 1)
    "CorrelationContext", "current_correlation", "new_correlation_context",
    # capabilities (Phase 2-4)
    "Capability", "CapabilityLifecycleState", "CapabilityRuntimeTier",
    "CapabilityStateError", "CapabilityExistsError", "CapabilityRegistry", "get_capability_registry",
    # task engine (Phase 5)
    "TaskState", "TaskOwner", "TaskRecord", "TaskEngine", "get_task_engine",
    "TaskStateError", "TaskNotFoundError", "TaskRetryExceeded", "TaskTimeoutError",
    # resource registry (Phase 12)
    "ProviderKind", "ResourceState", "ResourceRecord", "BaseProviderAdapter",
    "ResourceRegistry", "get_resource_registry",
    "ResourceExistsError", "ResourceNotFoundError", "AdapterNotRegisteredError",
    # source governance (Phase 7)
    "SourceState", "SourceCategory", "SourcePolicy", "LearnedItem",
    "SourceGovernance", "get_source_governance", "SourceStateError",
    # approval workflow (Phase 9)
    "ProposalKind", "ProposalPriority", "ProposalState", "ApprovalProposal", "ApprovalDecision",
    "ApprovalWorkflow", "get_approval_workflow", "ProposalStateError", "ProposalCooldownError",
    # governance (Phase 6)
    "ActionRisk", "BudgetKind", "Budgets", "RiskDecision",
    "GovernanceEngine", "get_governance_engine",
    # health model (Phase 13)
    "HealthStatus", "MemoryInfo", "UnifiedHealth", "HealthAggregator", "get_health_aggregator",
    # deployment tracker (Phase 13)
    "DeploymentStatus", "DeploymentRecord", "DeploymentTracker", "get_deployment_tracker",
    "DeploymentStateError", "DeploymentNotFoundError",
    # mcp skeleton (Phase 14)
    "MCPOperationCategory", "MCPSkeleton", "get_mcp_skeleton",
    "MCPOperationError", "MCPOperationNotRegisteredError", "MCPActionDenied",
    # learning loop (Phase 8)
    "LearningStage", "EvolutionSignal", "LearningOpportunity", "LearningLoop",
    "get_learning_loop", "LearningStageError",
]
