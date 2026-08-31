"""Approval Workflow — Safe Capability Self-Creation. ROADMAP §9, §26-§28.

Phase 9: High-risk actions require admin approval before execution.
Dedup key + cooldown (3600s) prevents admin fatigue.
"""

from __future__ import annotations

import enum
import hashlib
import uuid
from datetime import UTC, datetime
from typing import Any

from pydantic import BaseModel, Field

from ecosystem._store import get_conn, jdump, jload

_COOLDOWN_SECONDS = 3600


class ProposalKind(enum.StrEnum):
    NEW_SOURCE = "NEW_SOURCE"
    NEW_CAPABILITY = "NEW_CAPABILITY"
    CAPABILITY_PROMOTION = "CAPABILITY_PROMOTION"
    CAPABILITY_ARCHIVE = "CAPABILITY_ARCHIVE"
    DEPLOYMENT = "DEPLOYMENT"
    DB_MIGRATION = "DB_MIGRATION"
    SECRET_ROTATION = "SECRET_ROTATION"
    HIGH_RISK_ACTION = "HIGH_RISK_ACTION"
    LEARNING_PROPOSAL = "LEARNING_PROPOSAL"


class ProposalPriority(enum.StrEnum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"


class ProposalState(enum.StrEnum):
    PENDING = "PENDING"
    APPROVED = "APPROVED"
    REJECTED = "REJECTED"
    DEFERRED = "DEFERRED"
    EXECUTED = "EXECUTED"
    SUPERSEDED = "SUPERSEDED"
    EXPIRED = "EXPIRED"


_ALLOWED: dict[ProposalState, set[ProposalState]] = {
    ProposalState.PENDING: {
        ProposalState.APPROVED,
        ProposalState.REJECTED,
        ProposalState.DEFERRED,
        ProposalState.EXPIRED,
        ProposalState.SUPERSEDED,
    },
    ProposalState.APPROVED: {
        ProposalState.EXECUTED,
        ProposalState.EXPIRED,
        ProposalState.SUPERSEDED,
    },
    ProposalState.REJECTED: {ProposalState.PENDING, ProposalState.SUPERSEDED},
    ProposalState.DEFERRED: {
        ProposalState.PENDING,
        ProposalState.EXPIRED,
        ProposalState.SUPERSEDED,
    },
    ProposalState.EXECUTED: set(),
    ProposalState.SUPERSEDED: set(),
    ProposalState.EXPIRED: set(),
}


class ProposalStateError(Exception):
    pass


class ProposalCooldownError(Exception):
    pass


class ApprovalProposal(BaseModel):
    proposal_id: str = Field(default_factory=lambda: f"prop-{uuid.uuid4().hex[:16]}")
    kind: ProposalKind
    priority: ProposalPriority = ProposalPriority.MEDIUM
    title: str
    summary: str = ""
    payload: dict[str, Any] = Field(default_factory=dict)
    dedup_key: str = ""
    state: ProposalState = ProposalState.PENDING
    risk_level: str = "MEDIUM"
    requested_by: str = "system"
    tenant_id: str | None = None
    correlation: dict[str, Any] = Field(default_factory=dict)
    created_at: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())
    updated_at: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())
    decided_at: str | None = None
    executed_at: str | None = None
    expires_at: str | None = None


class ApprovalDecision(BaseModel):
    decision_id: str = Field(default_factory=lambda: f"dec-{uuid.uuid4().hex[:16]}")
    proposal_id: str
    decision: ProposalState  # APPROVED / REJECTED / DEFERRED
    decided_by: str
    rationale: str = ""
    correlation: dict[str, Any] = Field(default_factory=dict)
    decided_at: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())


class ApprovalWorkflow:
    """Phase 9 — Approval Workflow. ROADMAP §26-§28."""

    PROPOSALS_TABLE = "ecosystem_proposals"
    DECISIONS_TABLE = "ecosystem_proposal_decisions"

    def __init__(self) -> None:
        self._ensure_schema()

    def _ensure_schema(self) -> None:
        with get_conn() as conn:
            conn.execute(f"""CREATE TABLE IF NOT EXISTS {self.PROPOSALS_TABLE} (
                proposal_id TEXT PRIMARY KEY, kind TEXT NOT NULL,
                priority TEXT NOT NULL DEFAULT 'MEDIUM', title TEXT NOT NULL, summary TEXT DEFAULT '',
                payload TEXT DEFAULT '{{}}', dedup_key TEXT DEFAULT '',
                state TEXT NOT NULL DEFAULT 'PENDING', risk_level TEXT DEFAULT 'MEDIUM',
                requested_by TEXT DEFAULT 'system', tenant_id TEXT, correlation TEXT DEFAULT '{{}}',
                created_at TEXT NOT NULL, updated_at TEXT NOT NULL,
                decided_at TEXT, executed_at TEXT, expires_at TEXT)""")
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.PROPOSALS_TABLE}_state ON {self.PROPOSALS_TABLE}(state)"
            )
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.PROPOSALS_TABLE}_kind ON {self.PROPOSALS_TABLE}(kind)"
            )
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.PROPOSALS_TABLE}_dedup ON {self.PROPOSALS_TABLE}(dedup_key)"
            )
            conn.execute(f"""CREATE TABLE IF NOT EXISTS {self.DECISIONS_TABLE} (
                decision_id TEXT PRIMARY KEY, proposal_id TEXT NOT NULL, decision TEXT NOT NULL,
                decided_by TEXT NOT NULL, rationale TEXT DEFAULT '',
                correlation TEXT DEFAULT '{{}}', decided_at TEXT NOT NULL,
                FOREIGN KEY (proposal_id) REFERENCES {self.PROPOSALS_TABLE}(proposal_id))""")
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.DECISIONS_TABLE}_prop ON {self.DECISIONS_TABLE}(proposal_id)"
            )
            conn.commit()

    def propose(self, p: ApprovalProposal) -> ApprovalProposal:
        if not p.dedup_key:
            p.dedup_key = self._make_dedup_key(p.kind, p.title, p.payload)
        # dedup + cooldown — admin fatigue prevention (§28)
        with get_conn() as conn:
            r = conn.execute(
                f"SELECT proposal_id, state, created_at FROM {self.PROPOSALS_TABLE} "
                "WHERE dedup_key=? ORDER BY created_at DESC LIMIT 1",
                (p.dedup_key,),
            ).fetchone()
        if r:
            age = (datetime.now(UTC) - datetime.fromisoformat(r["created_at"])).total_seconds()
            if age < _COOLDOWN_SECONDS and r["state"] in {
                ProposalState.PENDING.value,
                ProposalState.APPROVED.value,
            }:
                raise ProposalCooldownError(
                    f"Proposal already exists within cooldown ({int(_COOLDOWN_SECONDS - age)}s remaining): {r['proposal_id']}"
                )
        with get_conn() as conn:
            conn.execute(self._proposal_insert_sql(), self._proposal_row(p))
            # supersede any prior PENDING with same dedup key
            conn.execute(
                f"UPDATE {self.PROPOSALS_TABLE} SET state=?, updated_at=? "
                "WHERE dedup_key=? AND state=? AND proposal_id != ?",
                (
                    ProposalState.SUPERSEDED,
                    datetime.now(UTC).isoformat(),
                    p.dedup_key,
                    ProposalState.PENDING,
                    p.proposal_id,
                ),
            )
            conn.commit()
        return p

    def decide(self, proposal_id: str, decision: ApprovalDecision) -> ApprovalProposal:
        p = self.get(proposal_id)
        if p is None:
            raise ProposalStateError(f"Proposal {proposal_id} not found")
        if decision.decision not in {
            ProposalState.APPROVED,
            ProposalState.REJECTED,
            ProposalState.DEFERRED,
        }:
            raise ProposalStateError(f"Invalid decision: {decision.decision}")
        if decision.decision not in _ALLOWED.get(p.state, set()):
            raise ProposalStateError(f"Illegal transition {p.state} → {decision.decision}")
        decision.proposal_id = proposal_id
        now = datetime.now(UTC).isoformat()
        decision.decided_at = now
        with get_conn() as conn:
            conn.execute(
                f"UPDATE {self.PROPOSALS_TABLE} SET state=?, decided_at=?, updated_at=? WHERE proposal_id=?",
                (decision.decision, now, now, proposal_id),
            )
            conn.execute(self._decision_insert_sql(), self._decision_row(decision))
            conn.commit()
        return self.get(proposal_id)  # type: ignore[return-value]

    def mark_executed(self, proposal_id: str) -> ApprovalProposal:
        p = self.get(proposal_id)
        if p is None:
            raise ProposalStateError(proposal_id)
        if ProposalState.EXECUTED not in _ALLOWED.get(p.state, set()):
            raise ProposalStateError(f"Cannot mark executed from {p.state}")
        now = datetime.now(UTC).isoformat()
        with get_conn() as conn:
            conn.execute(
                f"UPDATE {self.PROPOSALS_TABLE} SET state=?, executed_at=?, updated_at=? WHERE proposal_id=?",
                (ProposalState.EXECUTED, now, now, proposal_id),
            )
            conn.commit()
        return self.get(proposal_id)  # type: ignore[return-value]

    def get(self, pid: str) -> ApprovalProposal | None:
        with get_conn() as conn:
            r = conn.execute(
                f"SELECT * FROM {self.PROPOSALS_TABLE} WHERE proposal_id=?", (pid,)
            ).fetchone()
        return self._proposal_from(r) if r else None

    def list_pending(
        self,
        *,
        kind: ProposalKind | None = None,
        priority: ProposalPriority | None = None,
        limit: int = 100,
    ) -> list[ApprovalProposal]:
        clauses, params = ["state=?"], [ProposalState.PENDING]
        if kind:
            clauses.append("kind=?")
            params.append(kind)
        if priority:
            clauses.append("priority=?")
            params.append(priority)
        params.append(limit)
        with get_conn() as conn:
            rows = conn.execute(
                f"SELECT * FROM {self.PROPOSALS_TABLE} WHERE {' AND '.join(clauses)} "
                "ORDER BY created_at DESC LIMIT ?",
                params,
            ).fetchall()
        return [self._proposal_from(r) for r in rows]

    def list_decisions(self, proposal_id: str) -> list[ApprovalDecision]:
        with get_conn() as conn:
            rows = conn.execute(
                f"SELECT * FROM {self.DECISIONS_TABLE} WHERE proposal_id=? ORDER BY decided_at DESC",
                (proposal_id,),
            ).fetchall()
        return [self._decision_from(r) for r in rows]

    def requires_approval(self, kind: ProposalKind, *, risk_level: str = "MEDIUM") -> bool:
        high_risk_kinds = {
            ProposalKind.DB_MIGRATION,
            ProposalKind.SECRET_ROTATION,
            ProposalKind.HIGH_RISK_ACTION,
            ProposalKind.DEPLOYMENT,
            ProposalKind.CAPABILITY_PROMOTION,
        }
        if kind in high_risk_kinds:
            return True
        return risk_level in {"HIGH", "CRITICAL"}

    def _make_dedup_key(self, kind: ProposalKind, title: str, payload: dict[str, Any]) -> str:
        raw = f"{kind}|{title}|{jdump(payload)}"
        return hashlib.sha256(raw.encode("utf-8")).hexdigest()[:32]

    def _proposal_insert_sql(self) -> str:
        cols = (
            "proposal_id,kind,priority,title,summary,payload,dedup_key,state,risk_level,"
            "requested_by,tenant_id,correlation,created_at,updated_at,decided_at,executed_at,expires_at"
        )
        return f"INSERT INTO {self.PROPOSALS_TABLE} ({cols}) VALUES ({','.join(['?'] * 17)})"

    def _proposal_row(self, p: ApprovalProposal) -> tuple:
        return (
            p.proposal_id,
            p.kind,
            p.priority,
            p.title,
            p.summary,
            jdump(p.payload),
            p.dedup_key,
            p.state,
            p.risk_level,
            p.requested_by,
            p.tenant_id,
            jdump(p.correlation),
            p.created_at,
            p.updated_at,
            p.decided_at,
            p.executed_at,
            p.expires_at,
        )

    def _proposal_from(self, r: Any) -> ApprovalProposal:
        return ApprovalProposal(
            proposal_id=r["proposal_id"],
            kind=ProposalKind(r["kind"]),
            priority=ProposalPriority(r["priority"]),
            title=r["title"],
            summary=r["summary"],
            payload=jload(r["payload"], {}),
            dedup_key=r["dedup_key"],
            state=ProposalState(r["state"]),
            risk_level=r["risk_level"],
            requested_by=r["requested_by"],
            tenant_id=r["tenant_id"],
            correlation=jload(r["correlation"], {}),
            created_at=r["created_at"],
            updated_at=r["updated_at"],
            decided_at=r["decided_at"],
            executed_at=r["executed_at"],
            expires_at=r["expires_at"],
        )

    def _decision_insert_sql(self) -> str:
        cols = "decision_id,proposal_id,decision,decided_by,rationale,correlation,decided_at"
        return f"INSERT INTO {self.DECISIONS_TABLE} ({cols}) VALUES ({','.join(['?'] * 7)})"

    def _decision_row(self, d: ApprovalDecision) -> tuple:
        return (
            d.decision_id,
            d.proposal_id,
            d.decision,
            d.decided_by,
            d.rationale,
            jdump(d.correlation),
            d.decided_at,
        )

    def _decision_from(self, r: Any) -> ApprovalDecision:
        return ApprovalDecision(
            decision_id=r["decision_id"],
            proposal_id=r["proposal_id"],
            decision=ProposalState(r["decision"]),
            decided_by=r["decided_by"],
            rationale=r["rationale"],
            correlation=jload(r["correlation"], {}),
            decided_at=r["decided_at"],
        )


_workflow: ApprovalWorkflow | None = None


def get_approval_workflow() -> ApprovalWorkflow:
    global _workflow
    if _workflow is None:
        _workflow = ApprovalWorkflow()
    return _workflow


__all__ = [
    "ProposalKind",
    "ProposalPriority",
    "ProposalState",
    "ApprovalProposal",
    "ApprovalDecision",
    "ApprovalWorkflow",
    "get_approval_workflow",
    "ProposalStateError",
    "ProposalCooldownError",
]
