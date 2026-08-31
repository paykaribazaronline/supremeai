"""Ecosystem admin routes — Phase 9, 15 (ROADMAP §9, §13, §27, §47).

বাংলা: admin endpoints. Production-এ core.security.authentication.auth_middleware
verify_admin_session_fail_closed ব্যবহার হবে (JWT ভিত্তিক)।
"""

from __future__ import annotations

import os
from typing import Any

from fastapi import APIRouter, Depends, HTTPException, Query, Request
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
from ecosystem.learning_loop import LearningOpportunity, LearningStage
from ecosystem.task_engine import TaskState, get_task_engine


# বাংলা: inline admin auth — production-এ JWT দিয়ে replace হবে।
async def _verify_admin(request: Request) -> dict:
    token = os.getenv("ADMIN_TOKEN", "")
    if not token:
        raise HTTPException(500, "ADMIN_TOKEN not configured")
    auth = request.headers.get("Authorization", "")
    if not auth.startswith("Bearer "):
        raise HTTPException(401, "Missing Bearer token")
    if auth[7:] != token:
        raise HTTPException(403, "Invalid admin token")
    return {"role": "admin"}


router = APIRouter(
    prefix="/api/v1/ecosystem/admin",
    tags=["ecosystem-admin"],
    dependencies=[Depends(_verify_admin)],
)


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
def admin_create_capability(req: CapabilityCreateRequest) -> dict:
    try:
        rt = CapabilityRuntimeTier(req.runtime_tier)
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
        runtime_tier=rt,
        inputs=req.inputs,
        outputs=req.outputs,
        dependencies=req.dependencies,
        permissions=req.permissions,
        owner=req.owner,
        tenant_id=req.tenant_id,
    )
    try:
        return get_capability_registry().register(cap).model_dump()
    except Exception as e:
        raise HTTPException(409, str(e))


class LifecycleTransitionRequest(BaseModel):
    to_state: str
    actor: str = "admin"
    reason: str | None = None


@router.post("/capabilities/{capability_id}/lifecycle")
def admin_transition_capability(capability_id: str, req: LifecycleTransitionRequest) -> dict:
    try:
        to = CapabilityLifecycleState(req.to_state)
    except ValueError:
        raise HTTPException(400, f"invalid state: {req.to_state}")
    try:
        return (
            get_capability_registry()
            .transition(capability_id, to, actor=req.actor, reason=req.reason)
            .model_dump()
        )
    except Exception as e:
        raise HTTPException(409, str(e))


@router.post("/capabilities/{capability_id}/promote")
def admin_promote_capability(capability_id: str, actor: str = "admin") -> dict:
    try:
        return get_capability_registry().promote(capability_id, actor=actor).model_dump()
    except Exception as e:
        raise HTTPException(409, str(e))


@router.post("/capabilities/{capability_id}/archive")
def admin_archive_capability(capability_id: str, actor: str = "admin") -> dict:
    try:
        return get_capability_registry().archive(capability_id, actor=actor).model_dump()
    except Exception as e:
        raise HTTPException(409, str(e))


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
    kind: str | None = None, priority: str | None = None, limit: int = Query(50, le=200)
) -> list[dict]:
    return [
        p.model_dump()
        for p in get_approval_workflow().list_pending(
            kind=ProposalKind(kind) if kind else None,
            priority=ProposalPriority(priority) if priority else None,
            limit=limit,
        )
    ]


@router.post("/proposals")
def admin_create_proposal(req: ProposalCreateRequest) -> dict:
    try:
        k = ProposalKind(req.kind)
        p = ProposalPriority(req.priority)
    except ValueError as e:
        raise HTTPException(400, str(e))
    prop = ApprovalProposal(
        kind=k,
        title=req.title,
        description=req.description,
        priority=p,
        risk_level=req.risk_level,
        dedup_key=req.dedup_key,
        payload=req.payload,
        evidence=req.evidence,
        cost_estimate=req.cost_estimate,
        proposed_by=req.proposed_by,
        tenant_id=req.tenant_id,
    )
    return get_approval_workflow().propose(prop).model_dump()


class ProposalDecisionRequest(BaseModel):
    decision: str
    resolved_by: str
    reason: str | None = None
    policy_scope: str | None = None
    policy_value: str | None = None


@router.post("/proposals/{proposal_id}/decide")
def admin_decide_proposal(proposal_id: str, req: ProposalDecisionRequest) -> dict:
    try:
        d = ProposalState(req.decision)
    except ValueError:
        raise HTTPException(400, f"invalid decision: {req.decision}")
    if d not in {ProposalState.APPROVED, ProposalState.REJECTED, ProposalState.DEFERRED}:
        raise HTTPException(400, "must be APPROVED/REJECTED/DEFERRED")
    dec = ApprovalDecision(
        proposal_id=proposal_id,
        decision=d,
        resolved_by=req.resolved_by,
        reason=req.reason,
        policy_scope=req.policy_scope,
        policy_value=req.policy_value,
    )
    try:
        return get_approval_workflow().decide(dec).model_dump()
    except Exception as e:
        raise HTTPException(409, str(e))


@router.get("/decisions")
def admin_list_decisions(dedup_key: str | None = None, limit: int = 100) -> list[dict]:
    return get_approval_workflow().list_decisions(dedup_key=dedup_key, limit=limit)


class OpportunityCreateRequest(BaseModel):
    requirement: str
    signal_id: str | None = None
    source_url: str | None = None
    usefulness: str = "unknown"
    feasibility: str = "unknown"
    risk: str = "medium"
    cost: str = "medium"
    maintenance: str = "low"


@router.get("/opportunities")
def admin_list_opportunities(
    stage: str | None = None, limit: int = Query(50, le=200)
) -> list[dict]:
    return [
        o.model_dump()
        for o in get_learning_loop().list_opportunities(
            stage=LearningStage(stage) if stage else None, limit=limit
        )
    ]


@router.post("/opportunities")
def admin_surface_opportunity(req: OpportunityCreateRequest) -> dict:
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


class OpportunityAdvanceRequest(BaseModel):
    to_stage: str
    note: str | None = None


@router.post("/opportunities/{opportunity_id}/advance")
def admin_advance_opportunity(opportunity_id: str, req: OpportunityAdvanceRequest) -> dict:
    try:
        s = LearningStage(req.to_stage)
    except ValueError:
        raise HTTPException(400, f"invalid stage: {req.to_stage}")
    return get_learning_loop().advance_stage(opportunity_id, s).model_dump()


@router.get("/overview")
def admin_overview() -> dict:
    caps = get_capability_registry().list(limit=500)
    pending = get_approval_workflow().list_pending(limit=50)
    opps = get_learning_loop().list_opportunities(limit=50)
    escalated = get_task_engine().list(state=TaskState.ESCALATED, limit=20)
    return {
        "capabilities": {
            "total": len(caps),
            "active": sum(1 for c in caps if c.lifecycle_state == CapabilityLifecycleState.ACTIVE),
            "archived": sum(
                1 for c in caps if c.lifecycle_state == CapabilityLifecycleState.ARCHIVED
            ),
        },
        "approvals_pending": len(pending),
        "learning_opportunities": {
            "total": len(opps),
            "awaiting_approval": sum(1 for o in opps if o.stage == LearningStage.AWAITING_APPROVAL),
        },
        "escalated_tasks": len(escalated),
    }


__all__ = ["router"]
