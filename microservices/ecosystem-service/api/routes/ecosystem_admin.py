"""Ecosystem admin/governance routes — proposals, policies, capability lifecycle.

বাংলা: এই router-টি শুধু admin-এর জন্য। এটি ROADMAP §27 (admin decision memory),
§9 (permission-first), §13 (capability lifecycle), §8 (source governance),
§47 (centralized admin experience) কে surface করে।

is_admin=True — তাই routers.py-তে register করার সময় get_current_user_token
dependency স্বয়ংক্রিয়ভাবে apply হবে। অতিরিক্ত admin gate verify_admin_session_fail_closed।
"""

from __future__ import annotations

from typing import Any

from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel, Field

from ecosystem import (
    Capability,
    CapabilityLifecycleState,
    CapabilityRuntimeTier,
    ProposalKind,
    ProposalPriority,
    ProposalState,
    SourceCategory,
    SourcePolicy,
    SourceState,
    get_approval_workflow,
    get_capability_registry,
    get_governance_engine,
    get_learning_loop,
    get_source_governance,
)
from ecosystem.approval_workflow import ApprovalDecision, ApprovalProposal
from ecosystem.learning_loop import LearningStage, LearningOpportunity
from ecosystem.task_engine import TaskState, get_task_engine

# বাংলা: standalone test harness — auth.py থেকে simple Bearer token admin auth।
# ⚠️ আসল production supremeai-তে core.security.authentication.auth_middleware
#    verify_admin_session_fail_closed ব্যবহার করা হবে।
try:
    from auth import verify_admin_session_fail_closed
    _ADMIN_DEP = [Depends(verify_admin_session_fail_closed)]
except ImportError:
    # বাংলা: যদি auth.py না থাকে, সব admin endpoint খোলা থাকবে (শুধু test)।
    _ADMIN_DEP = []

router = APIRouter(
    prefix="/api/v1/ecosystem/admin",
    tags=["ecosystem-admin"],
    dependencies=_ADMIN_DEP,
)


# ===========================================================================
# Capability lifecycle (ROADMAP §13)
# ===========================================================================


class CapabilityCreateRequest(BaseModel):
    name: str
    purpose: str
    signature: str
    category: str = "general"
    version: str = "0.1.0"
    execution_method: str = "in_process"
    security_level: str = "standard"
    runtime_tier: str = "WARM"
    inputs: list[dict[str, Any]] = Field(default_factory=list)
    outputs: list[dict[str, Any]] = Field(default_factory=list)
    dependencies: list[str] = Field(default_factory=list)
    permissions: list[str] = Field(default_factory=list)
    owner: str = "system"
    tenant_id: str | None = None


@router.post("/capabilities")
def admin_create_capability(req: CapabilityCreateRequest) -> dict[str, Any]:
    """ROADMAP §12 — register a new capability (explosion guard enforces uniqueness)."""
    try:
        runtime_tier = CapabilityRuntimeTier(req.runtime_tier)
    except ValueError:
        raise HTTPException(400, f"invalid runtime_tier: {req.runtime_tier}")
    cap = Capability(
        name=req.name,
        purpose=req.purpose,
        signature=req.signature,
        category=req.category,
        version=req.version,
        execution_method=req.execution_method,
        security_level=req.security_level,
        runtime_tier=runtime_tier,
        inputs=req.inputs,
        outputs=req.outputs,
        dependencies=req.dependencies,
        permissions=req.permissions,
        owner=req.owner,
        tenant_id=req.tenant_id,
    )
    try:
        return get_capability_registry().register(cap).model_dump()
    except Exception as exc:
        raise HTTPException(409, str(exc))


class LifecycleTransitionRequest(BaseModel):
    to_state: str
    actor: str = "admin"
    reason: str | None = None


@router.post("/capabilities/{capability_id}/lifecycle")
def admin_transition_capability(
    capability_id: str, req: LifecycleTransitionRequest
) -> dict[str, Any]:
    try:
        to_state = CapabilityLifecycleState(req.to_state)
    except ValueError:
        raise HTTPException(400, f"invalid state: {req.to_state}")
    try:
        cap = get_capability_registry().transition(
            capability_id, to_state, actor=req.actor, reason=req.reason
        )
    except Exception as exc:
        raise HTTPException(409, str(exc))
    return cap.model_dump()


@router.post("/capabilities/{capability_id}/promote")
def admin_promote_capability(capability_id: str, actor: str = "admin") -> dict[str, Any]:
    """ROADMAP §15 — promote warm/cold capability to HOT runtime."""
    try:
        return get_capability_registry().promote(capability_id, actor=actor).model_dump()
    except Exception as exc:
        raise HTTPException(409, str(exc))


@router.post("/capabilities/{capability_id}/archive")
def admin_archive_capability(capability_id: str, actor: str = "admin") -> dict[str, Any]:
    try:
        return get_capability_registry().archive(capability_id, actor=actor).model_dump()
    except Exception as exc:
        raise HTTPException(409, str(exc))


# ===========================================================================
# Approval workflow (ROADMAP §9, §26, §27)
# ===========================================================================


class ProposalCreateRequest(BaseModel):
    kind: str
    title: str
    description: str
    priority: str = "MEDIUM"
    risk_level: str = "medium"
    dedup_key: str | None = None
    payload: dict[str, Any] = Field(default_factory=dict)
    evidence: list[dict[str, Any]] = Field(default_factory=list)
    cost_estimate: dict[str, Any] = Field(default_factory=dict)
    proposed_by: str = "system"
    tenant_id: str | None = None


@router.get("/proposals")
def admin_list_proposals(
    kind: str | None = None,
    priority: str | None = None,
    limit: int = Query(50, le=200),
) -> list[dict[str, Any]]:
    wf = get_approval_workflow()
    props = wf.list_pending(
        kind=ProposalKind(kind) if kind else None,
        priority=ProposalPriority(priority) if priority else None,
        limit=limit,
    )
    return [p.model_dump() for p in props]


@router.post("/proposals")
def admin_create_proposal(req: ProposalCreateRequest) -> dict[str, Any]:
    try:
        kind = ProposalKind(req.kind)
        priority = ProposalPriority(req.priority)
    except ValueError as exc:
        raise HTTPException(400, str(exc))
    proposal = ApprovalProposal(
        kind=kind,
        title=req.title,
        description=req.description,
        priority=priority,
        risk_level=req.risk_level,
        dedup_key=req.dedup_key,
        payload=req.payload,
        evidence=req.evidence,
        cost_estimate=req.cost_estimate,
        proposed_by=req.proposed_by,
        tenant_id=req.tenant_id,
    )
    return get_approval_workflow().propose(proposal).model_dump()


class ProposalDecisionRequest(BaseModel):
    decision: str  # APPROVED | REJECTED | DEFERRED
    resolved_by: str
    reason: str | None = None
    policy_scope: str | None = None
    policy_value: str | None = None


@router.post("/proposals/{proposal_id}/decide")
def admin_decide_proposal(proposal_id: str, req: ProposalDecisionRequest) -> dict[str, Any]:
    try:
        decision_state = ProposalState(req.decision)
    except ValueError:
        raise HTTPException(400, f"invalid decision: {req.decision}")
    if decision_state not in {
        ProposalState.APPROVED,
        ProposalState.REJECTED,
        ProposalState.DEFERRED,
    }:
        raise HTTPException(400, "decision must be APPROVED, REJECTED or DEFERRED")
    decision = ApprovalDecision(
        proposal_id=proposal_id,
        decision=decision_state,
        resolved_by=req.resolved_by,
        reason=req.reason,
        policy_scope=req.policy_scope,
        policy_value=req.policy_value,
    )
    try:
        return get_approval_workflow().decide(decision).model_dump()
    except Exception as exc:
        raise HTTPException(409, str(exc))


@router.post("/proposals/{proposal_id}/executed")
def admin_mark_executed(proposal_id: str, executed_by: str = "admin") -> dict[str, Any]:
    try:
        return get_approval_workflow().mark_executed(proposal_id, executed_by=executed_by).model_dump()
    except Exception as exc:
        raise HTTPException(409, str(exc))


@router.get("/decisions")
def admin_list_decisions(dedup_key: str | None = None, limit: int = 100) -> list[dict[str, Any]]:
    return get_approval_workflow().list_decisions(dedup_key=dedup_key, limit=limit)


# ===========================================================================
# Source governance (ROADMAP §7–§10, §57)
# ===========================================================================


class SourceDiscoverRequest(BaseModel):
    url: str
    category: str | None = None


@router.post("/sources/discover")
def admin_discover_source(req: SourceDiscoverRequest) -> dict[str, Any]:
    return get_source_governance().discover(
        req.url,
        category=SourceCategory(req.category) if req.category else None,
    )


class SourceTransitionRequest(BaseModel):
    to_state: str
    actor: str = "admin"


@router.post("/sources/{source_id}/transition")
def admin_transition_source(source_id: str, req: SourceTransitionRequest) -> dict[str, Any]:
    try:
        state = SourceState(req.to_state)
    except ValueError:
        raise HTTPException(400, f"invalid state: {req.to_state}")
    try:
        return get_source_governance().transition_source(source_id, state, actor=req.actor)
    except Exception as exc:
        raise HTTPException(409, str(exc))


class PolicyCreateRequest(BaseModel):
    name: str
    scope: str = "domain"
    scope_value: str
    decision: str = "ALLOWLISTED"
    reason: str | None = None
    rate_limit_per_minute: int = 30
    crawl_budget_per_day: int = 500
    requires_approval: bool = False
    created_by: str = "admin"


@router.post("/policies")
def admin_create_policy(req: PolicyCreateRequest) -> dict[str, Any]:
    policy = SourcePolicy(
        name=req.name,
        scope=req.scope,
        scope_value=req.scope_value,
        decision=SourceState(req.decision),
        reason=req.reason,
        rate_limit_per_minute=req.rate_limit_per_minute,
        crawl_budget_per_day=req.crawl_budget_per_day,
        requires_approval=req.requires_approval,
        created_by=req.created_by,
    )
    return get_source_governance().add_policy(policy).model_dump()


class LearnedItemCreateRequest(BaseModel):
    source_url: str
    source_id: str | None = None
    source_type: str = "UNKNOWN"
    title: str | None = None
    summary: str | None = None
    provenance: dict[str, Any] = Field(default_factory=dict)
    confidence: float = 0.0
    cross_check_status: str = "pending"
    policy_decision: str = "unknown"
    capabilities_affected: list[str] = Field(default_factory=list)
    relevance: float = 0.0


@router.post("/learned")
def admin_record_learned(req: LearnedItemCreateRequest) -> dict[str, Any]:
    from ecosystem.source_governance import LearnedItem

    item = LearnedItem(
        source_url=req.source_url,
        source_id=req.source_id,
        source_type=SourceCategory(req.source_type),
        title=req.title,
        summary=req.summary,
        provenance=req.provenance,
        confidence=req.confidence,
        cross_check_status=req.cross_check_status,
        policy_decision=req.policy_decision,
        capabilities_affected=req.capabilities_affected,
        relevance=req.relevance,
    )
    return get_source_governance().record_learned(item).model_dump()


@router.post("/learned/prune")
def admin_prune_learned(older_than_days: int = 30, min_relevance: float = 0.1) -> dict[str, Any]:
    """ROADMAP §56 — discard low-value transient knowledge."""
    removed = get_source_governance().prune_low_value(
        older_than_days=older_than_days, min_relevance=min_relevance
    )
    return {"removed": removed}


# ===========================================================================
# Learning loop / proactive evolution (ROADMAP §25, §57)
# ===========================================================================


class OpportunityCreateRequest(BaseModel):
    requirement: str
    signal_id: str | None = None
    source_url: str | None = None
    usefulness: str = "unknown"
    feasibility: str = "unknown"
    risk: str = "medium"
    cost: str = "medium"
    maintenance: str = "low"


@router.post("/opportunities")
def admin_surface_opportunity(req: OpportunityCreateRequest) -> dict[str, Any]:
    opp = LearningOpportunity(
        requirement=req.requirement,
        signal_id=req.signal_id,
        source_url=req.source_url,
        usefulness=req.usefulness,
        feasibility=req.feasibility,
        risk=req.risk,
        cost=req.cost,
        maintenance=req.maintenance,
    )
    return get_learning_loop().surface_opportunity(opp).model_dump()


@router.get("/opportunities")
def admin_list_opportunities(
    stage: str | None = None, limit: int = Query(50, le=200)
) -> list[dict[str, Any]]:
    """ROADMAP §57 — list learning opportunities for the admin dashboard."""
    opps = get_learning_loop().list_opportunities(
        stage=LearningStage(stage) if stage else None, limit=limit
    )
    return [o.model_dump() for o in opps]


class OpportunityAdvanceRequest(BaseModel):
    to_stage: str
    note: str | None = None


@router.post("/opportunities/{opportunity_id}/advance")
def admin_advance_opportunity(
    opportunity_id: str, req: OpportunityAdvanceRequest
) -> dict[str, Any]:
    try:
        stage = LearningStage(req.to_stage)
    except ValueError:
        raise HTTPException(400, f"invalid stage: {req.to_stage}")
    return get_learning_loop().advance_stage(opportunity_id, stage).model_dump()


# ===========================================================================
# Governance audit (ROADMAP §28, §54)
# ===========================================================================


@router.get("/governance/decisions")
def admin_governance_decisions(limit: int = 100) -> list[dict[str, Any]]:
    """ROADMAP §28 — recent autonomous-action gating decisions for audit."""
    from ecosystem._store import get_conn

    with get_conn() as conn:
        rows = conn.execute(
            "SELECT * FROM ecosystem_governance_decisions "
            "ORDER BY decided_at DESC LIMIT ?",
            (limit,),
        ).fetchall()
    return [dict(r) for r in rows]


# ===========================================================================
# Admin overview (ROADMAP §47)
# ===========================================================================


@router.get("/overview")
def admin_overview() -> dict[str, Any]:
    """ROADMAP §47 — single centralized admin surface: System / Resources /
    Capabilities / Tasks / Learning / Deployments / Approvals / Incidents."""
    caps = get_capability_registry().list(limit=500)
    pending_props = get_approval_workflow().list_pending(limit=50)
    opps = get_learning_loop().list_opportunities(limit=50)
    escalated_tasks = get_task_engine().list(state=TaskState.ESCALATED, limit=20)
    return {
        "capabilities": {
            "total": len(caps),
            "active": sum(1 for c in caps if c.lifecycle_state == CapabilityLifecycleState.ACTIVE),
            "archived": sum(1 for c in caps if c.lifecycle_state == CapabilityLifecycleState.ARCHIVED),
        },
        "approvals_pending": len(pending_props),
        "learning_opportunities": {
            "total": len(opps),
            "awaiting_approval": sum(
                1 for o in opps if o.stage == LearningStage.AWAITING_APPROVAL
            ),
        },
        "escalated_tasks": len(escalated_tasks),
    }


__all__ = ["router"]
