"""Learning Loop — Proactive Capability Forecasting. ROADMAP §25, §57.

Phase 8: Signals → opportunities → proposals → capabilities.
Stage machine drives self-evolution with safety gates.
"""

from __future__ import annotations

import enum
import uuid
from datetime import UTC, datetime
from typing import Any

from pydantic import BaseModel, Field

from ecosystem._store import get_conn, jdump, jload


class LearningStage(enum.StrEnum):
    DISCOVERY = "DISCOVERY"
    SOURCE_CHECK = "SOURCE_CHECK"
    POLICY_GATE = "POLICY_GATE"
    RESEARCH = "RESEARCH"
    KNOWLEDGE_RECORDED = "KNOWLEDGE_RECORDED"
    GAP_SIGNAL = "GAP_SIGNAL"
    CAPABILITY_OPPORTUNITY = "CAPABILITY_OPPORTUNITY"
    PRACTICALITY_ANALYSIS = "PRACTICALITY_ANALYSIS"
    PROPOSAL = "PROPOSAL"
    AWAITING_APPROVAL = "AWAITING_APPROVAL"
    BUILDING = "BUILDING"
    VALIDATING = "VALIDATING"
    REGISTERED = "REGISTERED"
    REUSED = "REUSED"
    REJECTED = "REJECTED"
    ARCHIVED = "ARCHIVED"


_ALLOWED: dict[LearningStage, set[LearningStage]] = {
    LearningStage.DISCOVERY: {LearningStage.SOURCE_CHECK, LearningStage.REJECTED},
    LearningStage.SOURCE_CHECK: {LearningStage.POLICY_GATE, LearningStage.REJECTED},
    LearningStage.POLICY_GATE: {LearningStage.RESEARCH, LearningStage.REJECTED},
    LearningStage.RESEARCH: {
        LearningStage.KNOWLEDGE_RECORDED,
        LearningStage.GAP_SIGNAL,
        LearningStage.REJECTED,
    },
    LearningStage.KNOWLEDGE_RECORDED: {LearningStage.GAP_SIGNAL, LearningStage.ARCHIVED},
    LearningStage.GAP_SIGNAL: {LearningStage.CAPABILITY_OPPORTUNITY, LearningStage.ARCHIVED},
    LearningStage.CAPABILITY_OPPORTUNITY: {
        LearningStage.PRACTICALITY_ANALYSIS,
        LearningStage.ARCHIVED,
    },
    LearningStage.PRACTICALITY_ANALYSIS: {LearningStage.PROPOSAL, LearningStage.REJECTED},
    LearningStage.PROPOSAL: {LearningStage.AWAITING_APPROVAL, LearningStage.REJECTED},
    LearningStage.AWAITING_APPROVAL: {LearningStage.BUILDING, LearningStage.REJECTED},
    LearningStage.BUILDING: {LearningStage.VALIDATING, LearningStage.REJECTED},
    LearningStage.VALIDATING: {LearningStage.REGISTERED, LearningStage.REJECTED},
    LearningStage.REGISTERED: {LearningStage.REUSED, LearningStage.ARCHIVED},
    LearningStage.REUSED: {LearningStage.ARCHIVED},
    LearningStage.REJECTED: {LearningStage.DISCOVERY, LearningStage.ARCHIVED},
    LearningStage.ARCHIVED: set(),
}


class LearningStageError(Exception):
    pass


class EvolutionSignal(BaseModel):
    signal_id: str = Field(default_factory=lambda: f"sig-{uuid.uuid4().hex[:16]}")
    source: str  # origin: telemetry | source | gap | forecast
    kind: str = "gap"
    content: dict[str, Any] = Field(default_factory=dict)
    value_score: float = 0.0
    stage: LearningStage = LearningStage.DISCOVERY
    opportunity_id: str | None = None
    correlation: dict[str, Any] = Field(default_factory=dict)
    created_at: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())
    updated_at: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())


class LearningOpportunity(BaseModel):
    opportunity_id: str = Field(default_factory=lambda: f"opp-{uuid.uuid4().hex[:16]}")
    signal_id: str | None = None
    capability_hint: str
    gap_description: str = ""
    predicted_value: float = 0.0
    predicted_effort: float = 0.0
    stage: LearningStage = LearningStage.CAPABILITY_OPPORTUNITY
    proposal_id: str | None = None
    correlation: dict[str, Any] = Field(default_factory=dict)
    created_at: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())
    updated_at: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())
    archived_at: str | None = None


class LearningLoop:
    """Phase 8 — Proactive Capability Forecasting. ROADMAP §25."""

    SIGNALS_TABLE = "ecosystem_learning_signals"
    OPPORTUNITIES_TABLE = "ecosystem_learning_opportunities"

    def __init__(self) -> None:
        self._ensure_schema()

    def _ensure_schema(self) -> None:
        with get_conn() as conn:
            conn.execute(f"""CREATE TABLE IF NOT EXISTS {self.SIGNALS_TABLE} (
                signal_id TEXT PRIMARY KEY, source TEXT NOT NULL, kind TEXT NOT NULL DEFAULT 'gap',
                content TEXT DEFAULT '{{}}', value_score REAL DEFAULT 0,
                stage TEXT NOT NULL DEFAULT 'DISCOVERY', opportunity_id TEXT,
                correlation TEXT DEFAULT '{{}}',
                created_at TEXT NOT NULL, updated_at TEXT NOT NULL)""")
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.SIGNALS_TABLE}_stage ON {self.SIGNALS_TABLE}(stage)"
            )
            conn.execute(f"""CREATE TABLE IF NOT EXISTS {self.OPPORTUNITIES_TABLE} (
                opportunity_id TEXT PRIMARY KEY, signal_id TEXT, capability_hint TEXT NOT NULL,
                gap_description TEXT DEFAULT '', predicted_value REAL DEFAULT 0,
                predicted_effort REAL DEFAULT 0,
                stage TEXT NOT NULL DEFAULT 'CAPABILITY_OPPORTUNITY',
                proposal_id TEXT, correlation TEXT DEFAULT '{{}}',
                created_at TEXT NOT NULL, updated_at TEXT NOT NULL, archived_at TEXT)""")
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.OPPORTUNITIES_TABLE}_stage ON {self.OPPORTUNITIES_TABLE}(stage)"
            )
            conn.commit()

    def record_signal(self, s: EvolutionSignal) -> EvolutionSignal:
        with get_conn() as conn:
            conn.execute(self._signal_insert_sql(), self._signal_row(s))
            conn.commit()
        return s

    def list_signals(
        self, *, stage: LearningStage | None = None, source: str | None = None, limit: int = 100
    ) -> list[EvolutionSignal]:
        clauses, params = [], []
        if stage:
            clauses.append("stage=?")
            params.append(stage)
        if source:
            clauses.append("source=?")
            params.append(source)
        where = ("WHERE " + " AND ".join(clauses)) if clauses else ""
        params.append(limit)
        with get_conn() as conn:
            rows = conn.execute(
                f"SELECT * FROM {self.SIGNALS_TABLE} {where} ORDER BY created_at DESC LIMIT ?",
                params,
            ).fetchall()
        return [self._signal_from(r) for r in rows]

    def surface_opportunity(self, opp: LearningOpportunity) -> LearningOpportunity:
        with get_conn() as conn:
            conn.execute(self._opp_insert_sql(), self._opp_row(opp))
            if opp.signal_id:
                conn.execute(
                    f"UPDATE {self.SIGNALS_TABLE} SET opportunity_id=?, updated_at=?, stage=? WHERE signal_id=?",
                    (
                        opp.opportunity_id,
                        datetime.now(UTC).isoformat(),
                        LearningStage.CAPABILITY_OPPORTUNITY,
                        opp.signal_id,
                    ),
                )
            conn.commit()
        return opp

    def advance_stage(
        self, opportunity_id: str, to: LearningStage, *, proposal_id: str | None = None
    ) -> LearningOpportunity:
        with get_conn() as conn:
            r = conn.execute(
                f"SELECT * FROM {self.OPPORTUNITIES_TABLE} WHERE opportunity_id=?",
                (opportunity_id,),
            ).fetchone()
            if r is None:
                raise LearningStageError(f"Opportunity {opportunity_id} not found")
            current = LearningStage(r["stage"])
            if to not in _ALLOWED.get(current, set()):
                raise LearningStageError(f"Illegal transition {current} → {to}")
            now = datetime.now(UTC).isoformat()
            sets: dict[str, Any] = {"updated_at": now, "stage": to}
            if proposal_id:
                sets["proposal_id"] = proposal_id
            if to == LearningStage.ARCHIVED:
                sets["archived_at"] = now
            sql = ", ".join(f"{k}=?" for k in sets)
            conn.execute(
                f"UPDATE {self.OPPORTUNITIES_TABLE} SET {sql} WHERE opportunity_id=?",
                list(sets.values()) + [opportunity_id],
            )
            conn.commit()
        return self.get_opportunity(opportunity_id)  # type: ignore[return-value]

    def get_opportunity(self, oid: str) -> LearningOpportunity | None:
        with get_conn() as conn:
            r = conn.execute(
                f"SELECT * FROM {self.OPPORTUNITIES_TABLE} WHERE opportunity_id=?", (oid,)
            ).fetchone()
        return self._opp_from(r) if r else None

    def list_opportunities(
        self,
        *,
        stage: LearningStage | None = None,
        include_archived: bool = False,
        limit: int = 100,
    ) -> list[LearningOpportunity]:
        clauses, params = [], []
        if stage:
            clauses.append("stage=?")
            params.append(stage)
        if not include_archived:
            clauses.append("archived_at IS NULL")
        where = ("WHERE " + " AND ".join(clauses)) if clauses else ""
        params.append(limit)
        with get_conn() as conn:
            rows = conn.execute(
                f"SELECT * FROM {self.OPPORTUNITIES_TABLE} {where} ORDER BY created_at DESC LIMIT ?",
                params,
            ).fetchall()
        return [self._opp_from(r) for r in rows]

    def _signal_insert_sql(self) -> str:
        cols = (
            "signal_id,source,kind,content,value_score,stage,opportunity_id,correlation,"
            "created_at,updated_at"
        )
        return f"INSERT INTO {self.SIGNALS_TABLE} ({cols}) VALUES ({','.join(['?'] * 10)})"

    def _signal_row(self, s: EvolutionSignal) -> tuple:
        return (
            s.signal_id,
            s.source,
            s.kind,
            jdump(s.content),
            s.value_score,
            s.stage,
            s.opportunity_id,
            jdump(s.correlation),
            s.created_at,
            s.updated_at,
        )

    def _signal_from(self, r: Any) -> EvolutionSignal:
        return EvolutionSignal(
            signal_id=r["signal_id"],
            source=r["source"],
            kind=r["kind"],
            content=jload(r["content"], {}),
            value_score=float(r["value_score"] or 0),
            stage=LearningStage(r["stage"]),
            opportunity_id=r["opportunity_id"],
            correlation=jload(r["correlation"], {}),
            created_at=r["created_at"],
            updated_at=r["updated_at"],
        )

    def _opp_insert_sql(self) -> str:
        cols = (
            "opportunity_id,signal_id,capability_hint,gap_description,predicted_value,"
            "predicted_effort,stage,proposal_id,correlation,created_at,updated_at,archived_at"
        )
        return f"INSERT INTO {self.OPPORTUNITIES_TABLE} ({cols}) VALUES ({','.join(['?'] * 12)})"

    def _opp_row(self, o: LearningOpportunity) -> tuple:
        return (
            o.opportunity_id,
            o.signal_id,
            o.capability_hint,
            o.gap_description,
            o.predicted_value,
            o.predicted_effort,
            o.stage,
            o.proposal_id,
            jdump(o.correlation),
            o.created_at,
            o.updated_at,
            o.archived_at,
        )

    def _opp_from(self, r: Any) -> LearningOpportunity:
        return LearningOpportunity(
            opportunity_id=r["opportunity_id"],
            signal_id=r["signal_id"],
            capability_hint=r["capability_hint"],
            gap_description=r["gap_description"],
            predicted_value=float(r["predicted_value"] or 0),
            predicted_effort=float(r["predicted_effort"] or 0),
            stage=LearningStage(r["stage"]),
            proposal_id=r["proposal_id"],
            correlation=jload(r["correlation"], {}),
            created_at=r["created_at"],
            updated_at=r["updated_at"],
            archived_at=r["archived_at"],
        )


_loop: LearningLoop | None = None


def get_learning_loop() -> LearningLoop:
    global _loop
    if _loop is None:
        _loop = LearningLoop()
    return _loop


__all__ = [
    "LearningStage",
    "EvolutionSignal",
    "LearningOpportunity",
    "LearningLoop",
    "get_learning_loop",
    "LearningStageError",
]
