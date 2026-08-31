"""Capability Registry — ROADMAP §12, §13, §14, §15.

Phase 2: Capability Model
Phase 3: Capability Registry
Phase 4: Capability Lifecycle
"""

from __future__ import annotations

import enum
import uuid
from datetime import UTC, datetime
from typing import Any

from pydantic import BaseModel, Field

from ecosystem._store import get_conn, jdump, jload


class CapabilityLifecycleState(enum.StrEnum):
    IDEA = "IDEA"
    DISCOVERED = "DISCOVERED"
    PROPOSED = "PROPOSED"
    APPROVAL_PENDING = "APPROVAL_PENDING"
    APPROVED = "APPROVED"
    BUILDING = "BUILDING"
    VALIDATING = "VALIDATING"
    ACTIVE = "ACTIVE"
    MEASURED = "MEASURED"
    DEPRECATED = "DEPRECATED"
    ARCHIVED = "ARCHIVED"
    BLOCKED = "BLOCKED"


class CapabilityRuntimeTier(enum.StrEnum):
    HOT = "HOT"
    WARM = "WARM"
    COLD = "COLD"


_ALLOWED: dict[CapabilityLifecycleState, set[CapabilityLifecycleState]] = {
    CapabilityLifecycleState.IDEA: {
        CapabilityLifecycleState.DISCOVERED,
        CapabilityLifecycleState.PROPOSED,
        CapabilityLifecycleState.BLOCKED,
    },
    CapabilityLifecycleState.DISCOVERED: {
        CapabilityLifecycleState.PROPOSED,
        CapabilityLifecycleState.BLOCKED,
    },
    CapabilityLifecycleState.PROPOSED: {
        CapabilityLifecycleState.APPROVAL_PENDING,
        CapabilityLifecycleState.BLOCKED,
    },
    CapabilityLifecycleState.APPROVAL_PENDING: {
        CapabilityLifecycleState.APPROVED,
        CapabilityLifecycleState.BLOCKED,
    },
    CapabilityLifecycleState.APPROVED: {
        CapabilityLifecycleState.BUILDING,
        CapabilityLifecycleState.BLOCKED,
    },
    CapabilityLifecycleState.BUILDING: {
        CapabilityLifecycleState.VALIDATING,
        CapabilityLifecycleState.BLOCKED,
    },
    CapabilityLifecycleState.VALIDATING: {
        CapabilityLifecycleState.ACTIVE,
        CapabilityLifecycleState.BLOCKED,
    },
    CapabilityLifecycleState.ACTIVE: {
        CapabilityLifecycleState.MEASURED,
        CapabilityLifecycleState.DEPRECATED,
        CapabilityLifecycleState.BLOCKED,
    },
    CapabilityLifecycleState.MEASURED: {
        CapabilityLifecycleState.ACTIVE,
        CapabilityLifecycleState.DEPRECATED,
        CapabilityLifecycleState.ARCHIVED,
    },
    CapabilityLifecycleState.DEPRECATED: {CapabilityLifecycleState.ARCHIVED},
    CapabilityLifecycleState.ARCHIVED: {CapabilityLifecycleState.IDEA},
    CapabilityLifecycleState.BLOCKED: {
        CapabilityLifecycleState.PROPOSED,
        CapabilityLifecycleState.ARCHIVED,
    },
}


class CapabilityStateError(Exception):
    pass


class CapabilityExistsError(Exception):
    pass


class Capability(BaseModel):
    capability_id: str = Field(default_factory=lambda: f"cap-{uuid.uuid4().hex[:16]}")
    name: str
    purpose: str
    version: str = "0.1.0"
    signature: str
    category: str = "general"
    inputs: list[dict[str, Any]] = Field(default_factory=list)
    outputs: list[dict[str, Any]] = Field(default_factory=list)
    dependencies: list[str] = Field(default_factory=list)
    permissions: list[str] = Field(default_factory=list)
    execution_method: str = "in_process"
    resource_requirements: dict[str, Any] = Field(default_factory=dict)
    verification_strategy: dict[str, Any] = Field(default_factory=dict)
    security_level: str = "standard"
    quality_score: float = 0.0
    usage_count: int = 0
    success_rate: float = 0.0
    lifecycle_state: CapabilityLifecycleState = CapabilityLifecycleState.IDEA
    runtime_tier: CapabilityRuntimeTier = CapabilityRuntimeTier.WARM
    source: str = "internal"
    provenance: dict[str, Any] = Field(default_factory=dict)
    owner: str = "system"
    tenant_id: str | None = None
    activation_metadata: dict[str, Any] = Field(default_factory=dict)
    created_at: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())
    updated_at: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())
    promoted_at: str | None = None
    archived_at: str | None = None


class CapabilityRegistry:
    """Phase 3 — Capability Registry. ROADMAP §12."""

    TABLE = "ecosystem_capabilities"

    def __init__(self) -> None:
        self._ensure_schema()

    def _ensure_schema(self) -> None:
        with get_conn() as conn:
            conn.execute(f"""CREATE TABLE IF NOT EXISTS {self.TABLE} (
                capability_id TEXT PRIMARY KEY, name TEXT NOT NULL, purpose TEXT NOT NULL,
                version TEXT NOT NULL DEFAULT '0.1.0', signature TEXT NOT NULL UNIQUE,
                category TEXT NOT NULL DEFAULT 'general', inputs TEXT DEFAULT '[]', outputs TEXT DEFAULT '[]',
                dependencies TEXT DEFAULT '[]', permissions TEXT DEFAULT '[]', execution_method TEXT DEFAULT 'in_process',
                resource_requirements TEXT DEFAULT '{{}}', verification_strategy TEXT DEFAULT '{{}}', security_level TEXT DEFAULT 'standard',
                quality_score REAL DEFAULT 0, usage_count INTEGER DEFAULT 0, success_rate REAL DEFAULT 0,
                lifecycle_state TEXT DEFAULT 'IDEA', runtime_tier TEXT DEFAULT 'WARM', source TEXT DEFAULT 'internal',
                provenance TEXT DEFAULT '{{}}', owner TEXT DEFAULT 'system', tenant_id TEXT,
                activation_metadata TEXT DEFAULT '{{}}', created_at TEXT NOT NULL, updated_at TEXT NOT NULL,
                promoted_at TEXT, archived_at TEXT)""")
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.TABLE}_state ON {self.TABLE}(lifecycle_state)"
            )
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.TABLE}_sig ON {self.TABLE}(signature)"
            )
            conn.commit()

    def register(self, c: Capability) -> Capability:
        if self.find_by_signature(c.signature):
            raise CapabilityExistsError(
                f"Capability '{c.signature}' already exists (§55 explosion guard)"
            )
        with get_conn() as conn:
            conn.execute(self._insert_sql(), self._row(c))
            conn.commit()
        return c

    def get(self, cid: str) -> Capability | None:
        with get_conn() as conn:
            r = conn.execute(f"SELECT * FROM {self.TABLE} WHERE capability_id=?", (cid,)).fetchone()
        return self._from(r) if r else None

    def find_by_signature(self, sig: str) -> Capability | None:
        with get_conn() as conn:
            r = conn.execute(f"SELECT * FROM {self.TABLE} WHERE signature=?", (sig,)).fetchone()
        return self._from(r) if r else None

    def list(
        self,
        *,
        state: CapabilityLifecycleState | None = None,
        category: str | None = None,
        limit: int = 200,
    ) -> list[Capability]:
        clauses, params = [], []
        if state:
            clauses.append("lifecycle_state=?")
            params.append(state)
        if category:
            clauses.append("category=?")
            params.append(category)
        where = ("WHERE " + " AND ".join(clauses)) if clauses else ""
        params.append(limit)
        with get_conn() as conn:
            rows = conn.execute(
                f"SELECT * FROM {self.TABLE} {where} ORDER BY created_at DESC LIMIT ?", params
            ).fetchall()
        return [self._from(r) for r in rows]

    def transition(
        self, cid: str, to: CapabilityLifecycleState, *, actor: str = "system"
    ) -> Capability:
        c = self.get(cid)
        if c is None:
            raise CapabilityStateError(f"Capability {cid} not found")
        if to not in _ALLOWED.get(c.lifecycle_state, set()):
            raise CapabilityStateError(f"Illegal transition {c.lifecycle_state} → {to}")
        now = datetime.now(UTC).isoformat()
        sets = {"updated_at": now, "lifecycle_state": to}
        if to == CapabilityLifecycleState.MEASURED:
            sets["promoted_at"] = now
        if to == CapabilityLifecycleState.ARCHIVED:
            sets["archived_at"] = now
        sql = ", ".join(f"{k}=?" for k in sets)
        with get_conn() as conn:
            conn.execute(
                f"UPDATE {self.TABLE} SET {sql} WHERE capability_id=?", list(sets.values()) + [cid]
            )
            conn.commit()
        return self.get(cid)  # type: ignore[return-value]

    def promote(self, cid: str, *, actor: str = "system") -> Capability:
        c = self.get(cid)
        if c is None:
            raise CapabilityStateError(cid)
        if c.lifecycle_state not in {
            CapabilityLifecycleState.MEASURED,
            CapabilityLifecycleState.ACTIVE,
        }:
            raise CapabilityStateError("must be MEASURED or ACTIVE to promote")
        with get_conn() as conn:
            conn.execute(
                f"UPDATE {self.TABLE} SET runtime_tier=?, updated_at=? WHERE capability_id=?",
                (CapabilityRuntimeTier.HOT, datetime.now(UTC).isoformat(), cid),
            )
            conn.commit()
        return self.get(cid)  # type: ignore[return-value]

    def archive(self, cid: str, *, actor: str = "system") -> Capability:
        return self.transition(cid, CapabilityLifecycleState.ARCHIVED, actor=actor)

    def record_usage(self, cid: str, *, success: bool) -> Capability:
        with get_conn() as conn:
            r = conn.execute(
                f"SELECT usage_count, success_rate FROM {self.TABLE} WHERE capability_id=?", (cid,)
            ).fetchone()
            if r is None:
                raise CapabilityStateError(cid)
            n = int(r["usage_count"]) + 1
            rate = (
                float(r["success_rate"])
                + ((1.0 if success else 0.0) - float(r["success_rate"])) / n
            )
            conn.execute(
                f"UPDATE {self.TABLE} SET usage_count=?, success_rate=?, updated_at=? WHERE capability_id=?",
                (n, rate, datetime.now(UTC).isoformat(), cid),
            )
            conn.commit()
        return self.get(cid)  # type: ignore[return-value]

    def search(
        self, requirement: str, *, signature_hint: str | None = None, limit: int = 10
    ) -> list[Capability]:
        """Phase 3 §14 — REUSE > ADAPT > EXTEND > CREATE."""
        clauses = ["lifecycle_state IN (?, ?)"]
        params: list[Any] = [CapabilityLifecycleState.ACTIVE, CapabilityLifecycleState.MEASURED]
        if signature_hint:
            clauses.append("signature LIKE ?")
            params.append(f"%{signature_hint}%")
        req = f"%{requirement.lower()}%"
        clauses.append("(LOWER(name) LIKE ? OR LOWER(purpose) LIKE ?)")
        params.extend([req, req])
        params.append(limit)
        with get_conn() as conn:
            rows = conn.execute(
                f"SELECT * FROM {self.TABLE} WHERE {' AND '.join(clauses)} ORDER BY usage_count DESC LIMIT ?",
                params,
            ).fetchall()
        return [self._from(r) for r in rows]

    def _insert_sql(self) -> str:
        cols = "capability_id,name,purpose,version,signature,category,inputs,outputs,dependencies,permissions,execution_method,resource_requirements,verification_strategy,security_level,quality_score,usage_count,success_rate,lifecycle_state,runtime_tier,source,provenance,owner,tenant_id,activation_metadata,created_at,updated_at,promoted_at,archived_at"
        return f"INSERT INTO {self.TABLE} ({cols}) VALUES ({','.join(['?'] * 28)})"

    def _row(self, c: Capability) -> tuple:
        return (
            c.capability_id,
            c.name,
            c.purpose,
            c.version,
            c.signature,
            c.category,
            jdump(c.inputs),
            jdump(c.outputs),
            jdump(c.dependencies),
            jdump(c.permissions),
            c.execution_method,
            jdump(c.resource_requirements),
            jdump(c.verification_strategy),
            c.security_level,
            c.quality_score,
            c.usage_count,
            c.success_rate,
            c.lifecycle_state,
            c.runtime_tier,
            c.source,
            jdump(c.provenance),
            c.owner,
            c.tenant_id,
            jdump(c.activation_metadata),
            c.created_at,
            c.updated_at,
            c.promoted_at,
            c.archived_at,
        )

    def _from(self, r: Any) -> Capability:
        return Capability(
            capability_id=r["capability_id"],
            name=r["name"],
            purpose=r["purpose"],
            version=r["version"],
            signature=r["signature"],
            category=r["category"],
            inputs=jload(r["inputs"], []),
            outputs=jload(r["outputs"], []),
            dependencies=jload(r["dependencies"], []),
            permissions=jload(r["permissions"], []),
            execution_method=r["execution_method"],
            resource_requirements=jload(r["resource_requirements"], {}),
            verification_strategy=jload(r["verification_strategy"], {}),
            security_level=r["security_level"],
            quality_score=float(r["quality_score"] or 0),
            usage_count=int(r["usage_count"] or 0),
            success_rate=float(r["success_rate"] or 0),
            lifecycle_state=CapabilityLifecycleState(r["lifecycle_state"]),
            runtime_tier=CapabilityRuntimeTier(r["runtime_tier"]),
            source=r["source"],
            provenance=jload(r["provenance"], {}),
            owner=r["owner"],
            tenant_id=r["tenant_id"],
            activation_metadata=jload(r["activation_metadata"], {}),
            created_at=r["created_at"],
            updated_at=r["updated_at"],
            promoted_at=r["promoted_at"],
            archived_at=r["archived_at"],
        )


_registry: CapabilityRegistry | None = None


def get_capability_registry() -> CapabilityRegistry:
    global _registry
    if _registry is None:
        _registry = CapabilityRegistry()
    return _registry


__all__ = [
    "Capability",
    "CapabilityLifecycleState",
    "CapabilityRuntimeTier",
    "CapabilityStateError",
    "CapabilityExistsError",
    "CapabilityRegistry",
    "get_capability_registry",
]
