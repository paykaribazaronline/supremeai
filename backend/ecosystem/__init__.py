"""Ecosystem — SupremeAI orchestration modules.

Phases 2-14 (ROADMAP §1-§57). Each module is self-contained and idempotent.
All persistence uses the shared SQLite store from ecosystem._store.
"""

from ecosystem._store import (
    ensure_columns,
    get_conn,
    get_db_path,
    jdump,
    jload,
)
from ecosystem.approval_workflow import (
    ApprovalDecision,
    ApprovalProposal,
    ApprovalWorkflow,
    ProposalCooldownError,
    ProposalKind,
    ProposalPriority,
    ProposalState,
    ProposalStateError,
    get_approval_workflow,
)
from ecosystem.capability_registry import (
    Capability,
    CapabilityExistsError,
    CapabilityLifecycleState,
    CapabilityRegistry,
    CapabilityRuntimeTier,
    CapabilityStateError,
    get_capability_registry,
)
from ecosystem.correlation import (
    CorrelationContext,
    current_correlation,
    new_correlation_context,
)
from ecosystem.deployment_tracker import (
    DeploymentNotFoundError,
    DeploymentRecord,
    DeploymentStateError,
    DeploymentStatus,
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
    HealthAggregator,
    HealthStatus,
    MemoryInfo,
    UnifiedHealth,
    get_health_aggregator,
)
from ecosystem.learning_loop import (
    EvolutionSignal,
    LearningLoop,
    LearningOpportunity,
    LearningStage,
    LearningStageError,
    get_learning_loop,
)
from ecosystem.mcp_skeleton import (
    MCPActionDenied,
    MCPOperationCategory,
    MCPOperationError,
    MCPOperationNotRegisteredError,
    MCPSkeleton,
    get_mcp_skeleton,
)
from ecosystem.resource_registry import (
    AdapterNotRegisteredError,
    BaseProviderAdapter,
    ProviderKind,
    ResourceExistsError,
    ResourceNotFoundError,
    ResourceRecord,
    ResourceRegistry,
    ResourceState,
    get_resource_registry,
)
from ecosystem.source_governance import (
    LearnedItem,
    SourceCategory,
    SourceGovernance,
    SourcePolicy,
    SourceState,
    SourceStateError,
    get_source_governance,
)
from ecosystem.task_engine import (
    TaskEngine,
    TaskNotFoundError,
    TaskOwner,
    TaskRecord,
    TaskRetryExceeded,
    TaskState,
    TaskStateError,
    TaskTimeoutError,
    get_task_engine,
)

__all__ = [
    # storage (shared)
    "get_conn",
    "get_db_path",
    "ensure_columns",
    "jdump",
    "jload",
    # correlation (Phase 1)
    "CorrelationContext",
    "current_correlation",
    "new_correlation_context",
    # capabilities (Phase 2-4)
    "Capability",
    "CapabilityLifecycleState",
    "CapabilityRuntimeTier",
    "CapabilityStateError",
    "CapabilityExistsError",
    "CapabilityRegistry",
    "get_capability_registry",
    # task engine (Phase 5)
    "TaskState",
    "TaskOwner",
    "TaskRecord",
    "TaskEngine",
    "get_task_engine",
    "TaskStateError",
    "TaskNotFoundError",
    "TaskRetryExceeded",
    "TaskTimeoutError",
    # resource registry (Phase 12)
    "ProviderKind",
    "ResourceState",
    "ResourceRecord",
    "BaseProviderAdapter",
    "ResourceRegistry",
    "get_resource_registry",
    "ResourceExistsError",
    "ResourceNotFoundError",
    "AdapterNotRegisteredError",
    # source governance (Phase 7)
    "SourceState",
    "SourceCategory",
    "SourcePolicy",
    "LearnedItem",
    "SourceGovernance",
    "get_source_governance",
    "SourceStateError",
    # approval workflow (Phase 9)
    "ProposalKind",
    "ProposalPriority",
    "ProposalState",
    "ApprovalProposal",
    "ApprovalDecision",
    "ApprovalWorkflow",
    "get_approval_workflow",
    "ProposalStateError",
    "ProposalCooldownError",
    # governance (Phase 6)
    "ActionRisk",
    "BudgetKind",
    "Budgets",
    "RiskDecision",
    "GovernanceEngine",
    "get_governance_engine",
    # health model (Phase 13)
    "HealthStatus",
    "MemoryInfo",
    "UnifiedHealth",
    "HealthAggregator",
    "get_health_aggregator",
    # deployment tracker (Phase 13)
    "DeploymentStatus",
    "DeploymentRecord",
    "DeploymentTracker",
    "get_deployment_tracker",
    "DeploymentStateError",
    "DeploymentNotFoundError",
    # mcp skeleton (Phase 14)
    "MCPOperationCategory",
    "MCPSkeleton",
    "get_mcp_skeleton",
    "MCPOperationError",
    "MCPOperationNotRegisteredError",
    "MCPActionDenied",
    # learning loop (Phase 8)
    "LearningStage",
    "EvolutionSignal",
    "LearningOpportunity",
    "LearningLoop",
    "get_learning_loop",
    "LearningStageError",
]
