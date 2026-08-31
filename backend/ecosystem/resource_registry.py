"""Resource Registry — ROADMAP §36-§38.

Phase 12: Provider-agnostic resource registry with adapter dispatch.
Supports RENDER / GITHUB / KAGGLE / SUPABASE / FIREBASE / REDIS / CI / CUSTOM.
"""

from __future__ import annotations

import enum
import importlib
import uuid
from abc import ABC, abstractmethod
from datetime import UTC, datetime
from typing import Any

from pydantic import BaseModel, Field

from ecosystem._store import get_conn, jdump, jload


class ProviderKind(enum.StrEnum):
    RENDER = "RENDER"
    GITHUB = "GITHUB"
    KAGGLE = "KAGGLE"
    SUPABASE = "SUPABASE"
    FIREBASE = "FIREBASE"
    REDIS = "REDIS"
    CI = "CI"
    CUSTOM = "CUSTOM"


class ResourceState(enum.StrEnum):
    REGISTERED = "REGISTERED"
    HEALTHY = "HEALTHY"
    DEGRADED = "DEGRADED"
    CRITICAL = "CRITICAL"
    UNKNOWN = "UNKNOWN"
    MAINTENANCE = "MAINTENANCE"
    OFFLINE = "OFFLINE"


class ResourceExistsError(Exception):
    pass


class ResourceNotFoundError(Exception):
    pass


class AdapterNotRegisteredError(Exception):
    pass


class ResourceRecord(BaseModel):
    resource_id: str = Field(default_factory=lambda: f"res-{uuid.uuid4().hex[:16]}")
    provider: ProviderKind
    external_id: str
    name: str = ""
    state: ResourceState = ResourceState.REGISTERED
    endpoint: str | None = None
    metadata: dict[str, Any] = Field(default_factory=dict)
    deployment_id: str | None = None
    owner: str = "system"
    tenant_id: str | None = None
    last_health: dict[str, Any] = Field(default_factory=dict)
    created_at: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())
    updated_at: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())


class BaseProviderAdapter(ABC):
    """Abstract base for provider adapters. ROADMAP §36."""

    provider: ProviderKind = ProviderKind.CUSTOM

    @abstractmethod
    def list_resources(self) -> list[dict[str, Any]]: ...
    @abstractmethod
    def get_resource(self, external_id: str) -> dict[str, Any] | None: ...
    @abstractmethod
    def get_health(self, external_id: str) -> dict[str, Any]: ...
    @abstractmethod
    def get_metrics(self, external_id: str) -> dict[str, Any]: ...
    @abstractmethod
    def get_logs(self, external_id: str, *, limit: int = 100) -> list[dict[str, Any]]: ...
    @abstractmethod
    def get_deployment(self, external_id: str) -> dict[str, Any] | None: ...
    @abstractmethod
    def restart(self, external_id: str) -> dict[str, Any]: ...
    @abstractmethod
    def deploy(self, external_id: str, payload: dict[str, Any]) -> dict[str, Any]: ...
    @abstractmethod
    def rollback(self, external_id: str, *, version: str | None = None) -> dict[str, Any]: ...


class ResourceRegistry:
    """Phase 12 — Resource Registry. ROADMAP §36."""

    TABLE = "ecosystem_resources"
    ADAPTERS_TABLE = "ecosystem_resource_adapters"

    def __init__(self) -> None:
        self._ensure_schema()

    def _ensure_schema(self) -> None:
        with get_conn() as conn:
            conn.execute(f"""CREATE TABLE IF NOT EXISTS {self.TABLE} (
                resource_id TEXT PRIMARY KEY, provider TEXT NOT NULL, external_id TEXT NOT NULL,
                name TEXT DEFAULT '', state TEXT NOT NULL DEFAULT 'REGISTERED', endpoint TEXT,
                metadata TEXT DEFAULT '{{}}', deployment_id TEXT, owner TEXT DEFAULT 'system',
                tenant_id TEXT, last_health TEXT DEFAULT '{{}}',
                created_at TEXT NOT NULL, updated_at TEXT NOT NULL)""")
            conn.execute(
                f"CREATE UNIQUE INDEX IF NOT EXISTS idx_{self.TABLE}_ext ON {self.TABLE}(provider, external_id)"
            )
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.TABLE}_state ON {self.TABLE}(state)"
            )
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.TABLE}_provider ON {self.TABLE}(provider)"
            )
            conn.execute(f"""CREATE TABLE IF NOT EXISTS {self.ADAPTERS_TABLE} (
                provider TEXT PRIMARY KEY, adapter_path TEXT NOT NULL, created_at TEXT NOT NULL)""")
            conn.commit()

    def register(self, r: ResourceRecord) -> ResourceRecord:
        with get_conn() as conn:
            existing = conn.execute(
                f"SELECT resource_id FROM {self.TABLE} WHERE provider=? AND external_id=?",
                (r.provider, r.external_id),
            ).fetchone()
            if existing:
                raise ResourceExistsError(f"Resource {r.provider}:{r.external_id} already exists")
            conn.execute(self._insert_sql(), self._row(r))
            conn.commit()
        return r

    def get(self, rid: str) -> ResourceRecord | None:
        with get_conn() as conn:
            r = conn.execute(f"SELECT * FROM {self.TABLE} WHERE resource_id=?", (rid,)).fetchone()
        return self._from(r) if r else None

    def list(
        self,
        *,
        provider: ProviderKind | None = None,
        state: ResourceState | None = None,
        tenant_id: str | None = None,
        limit: int = 200,
    ) -> list[ResourceRecord]:
        clauses, params = [], []
        if provider:
            clauses.append("provider=?")
            params.append(provider)
        if state:
            clauses.append("state=?")
            params.append(state)
        if tenant_id:
            clauses.append("tenant_id=?")
            params.append(tenant_id)
        where = ("WHERE " + " AND ".join(clauses)) if clauses else ""
        params.append(limit)
        with get_conn() as conn:
            rows = conn.execute(
                f"SELECT * FROM {self.TABLE} {where} ORDER BY created_at DESC LIMIT ?", params
            ).fetchall()
        return [self._from(r) for r in rows]

    def update_state(
        self, rid: str, state: ResourceState, *, health: dict[str, Any] | None = None
    ) -> ResourceRecord:
        now = datetime.now(UTC).isoformat()
        sets: dict[str, Any] = {"updated_at": now, "state": state}
        if health is not None:
            sets["last_health"] = jdump(health)
        sql = ", ".join(f"{k}=?" for k in sets)
        with get_conn() as conn:
            cur = conn.execute(
                f"UPDATE {self.TABLE} SET {sql} WHERE resource_id=?", list(sets.values()) + [rid]
            )
            if cur.rowcount == 0:
                raise ResourceNotFoundError(rid)
            conn.commit()
        return self.get(rid)  # type: ignore[return-value]

    def link_deployment(self, rid: str, deployment_id: str) -> ResourceRecord:
        now = datetime.now(UTC).isoformat()
        with get_conn() as conn:
            cur = conn.execute(
                f"UPDATE {self.TABLE} SET deployment_id=?, updated_at=? WHERE resource_id=?",
                (deployment_id, now, rid),
            )
            if cur.rowcount == 0:
                raise ResourceNotFoundError(rid)
            conn.commit()
        return self.get(rid)  # type: ignore[return-value]

    def register_adapter(self, provider: ProviderKind, adapter_path: str) -> None:
        """Bind provider → 'module.path:ClassName' or 'module.path.ClassName'."""
        now = datetime.now(UTC).isoformat()
        with get_conn() as conn:
            conn.execute(
                f"INSERT INTO {self.ADAPTERS_TABLE} (provider, adapter_path, created_at) VALUES (?, ?, ?) "
                "ON CONFLICT(provider) DO UPDATE SET adapter_path=excluded.adapter_path, created_at=excluded.created_at",
                (provider, adapter_path, now),
            )
            conn.commit()

    def _load_adapter(self, provider: ProviderKind) -> BaseProviderAdapter:
        with get_conn() as conn:
            r = conn.execute(
                f"SELECT adapter_path FROM {self.ADAPTERS_TABLE} WHERE provider=?", (provider,)
            ).fetchone()
        if r is None:
            raise AdapterNotRegisteredError(f"No adapter registered for provider {provider}")
        path = r["adapter_path"]
        module_path, _, cls_name = path.partition(":")
        if not cls_name:
            module_path, _, cls_name = path.rpartition(".")
        if not module_path or not cls_name:
            raise AdapterNotRegisteredError(f"Invalid adapter path '{path}'")
        mod = importlib.import_module(module_path)
        cls = getattr(mod, cls_name)
        instance = cls()
        if not isinstance(instance, BaseProviderAdapter):
            raise AdapterNotRegisteredError(f"{path} is not a BaseProviderAdapter")
        return instance

    def control(self, rid: str, action: str, **kwargs: Any) -> dict[str, Any]:
        """Generic dispatch — ROADMAP §37 control plane."""
        r = self.get(rid)
        if r is None:
            raise ResourceNotFoundError(rid)
        adapter = self._load_adapter(r.provider)
        method = getattr(adapter, action, None)
        if method is None or not callable(method):
            raise AdapterNotRegisteredError(
                f"Action '{action}' not supported by {r.provider} adapter"
            )
        result = method(r.external_id, **kwargs) if kwargs else method(r.external_id)
        return {"resource_id": rid, "action": action, "result": result}

    def _insert_sql(self) -> str:
        cols = (
            "resource_id,provider,external_id,name,state,endpoint,metadata,deployment_id,"
            "owner,tenant_id,last_health,created_at,updated_at"
        )
        return f"INSERT INTO {self.TABLE} ({cols}) VALUES ({','.join(['?'] * 13)})"

    def _row(self, r: ResourceRecord) -> tuple:
        return (
            r.resource_id,
            r.provider,
            r.external_id,
            r.name,
            r.state,
            r.endpoint,
            jdump(r.metadata),
            r.deployment_id,
            r.owner,
            r.tenant_id,
            jdump(r.last_health),
            r.created_at,
            r.updated_at,
        )

    def _from(self, r: Any) -> ResourceRecord:
        return ResourceRecord(
            resource_id=r["resource_id"],
            provider=ProviderKind(r["provider"]),
            external_id=r["external_id"],
            name=r["name"],
            state=ResourceState(r["state"]),
            endpoint=r["endpoint"],
            metadata=jload(r["metadata"], {}),
            deployment_id=r["deployment_id"],
            owner=r["owner"],
            tenant_id=r["tenant_id"],
            last_health=jload(r["last_health"], {}),
            created_at=r["created_at"],
            updated_at=r["updated_at"],
        )


_registry: ResourceRegistry | None = None


def get_resource_registry() -> ResourceRegistry:
    global _registry
    if _registry is None:
        _registry = ResourceRegistry()
    return _registry


__all__ = [
    "ProviderKind",
    "ResourceState",
    "ResourceRecord",
    "BaseProviderAdapter",
    "ResourceRegistry",
    "get_resource_registry",
    "ResourceExistsError",
    "ResourceNotFoundError",
    "AdapterNotRegisteredError",
]
