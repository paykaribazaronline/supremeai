"""Source Governance — Internet Learning. ROADMAP §7-§10.

Phase 7: Self-learning from approved external sources with policy gate.
DISCOVERED → APPROVAL_PENDING → ALLOWLISTED (or BLOCKED / DEFERRED).
"""

from __future__ import annotations

import enum
import re
import uuid
from datetime import UTC, datetime
from typing import Any

from pydantic import BaseModel, Field

from ecosystem._store import get_conn, jdump, jload


class SourceState(enum.StrEnum):
    UNKNOWN = "UNKNOWN"
    DISCOVERED = "DISCOVERED"
    APPROVAL_PENDING = "APPROVAL_PENDING"
    ALLOWLISTED = "ALLOWLISTED"
    BLOCKED = "BLOCKED"
    DEFERRED = "DEFERRED"


class SourceCategory(enum.StrEnum):
    AI_DOCS = "AI_DOCS"
    OSS_REPO = "OSS_REPO"
    TECH_DOCS = "TECH_DOCS"
    RESEARCH = "RESEARCH"
    STANDARDS = "STANDARDS"
    PUBLIC_API = "PUBLIC_API"
    TECH_BLOG = "TECH_BLOG"
    MODEL_PROVIDER_DOCS = "MODEL_PROVIDER_DOCS"
    APPROVED_SITE = "APPROVED_SITE"
    APPROVED_DATASET = "APPROVED_DATASET"
    UNKNOWN = "UNKNOWN"


_ALLOWED: dict[SourceState, set[SourceState]] = {
    SourceState.UNKNOWN: {SourceState.DISCOVERED, SourceState.BLOCKED},
    SourceState.DISCOVERED: {
        SourceState.APPROVAL_PENDING,
        SourceState.ALLOWLISTED,
        SourceState.BLOCKED,
        SourceState.DEFERRED,
    },
    SourceState.APPROVAL_PENDING: {
        SourceState.ALLOWLISTED,
        SourceState.BLOCKED,
        SourceState.DEFERRED,
    },
    SourceState.ALLOWLISTED: {SourceState.BLOCKED, SourceState.DEFERRED},
    SourceState.BLOCKED: {SourceState.DISCOVERED, SourceState.ALLOWLISTED},
    SourceState.DEFERRED: {
        SourceState.APPROVAL_PENDING,
        SourceState.ALLOWLISTED,
        SourceState.BLOCKED,
    },
}


class SourceStateError(Exception):
    pass


class SourcePolicy(BaseModel):
    policy_id: str = Field(default_factory=lambda: f"pol-{uuid.uuid4().hex[:16]}")
    url_pattern: str  # glob or regex pattern
    category: SourceCategory = SourceCategory.UNKNOWN
    state: SourceState = SourceState.UNKNOWN
    allowed_actions: list[str] = Field(default_factory=lambda: ["read"])
    source_weight: float = 1.0
    expires_at: str | None = None
    created_by: str = "system"
    created_at: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())
    updated_at: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())


class LearnedItem(BaseModel):
    item_id: str = Field(default_factory=lambda: f"learn-{uuid.uuid4().hex[:16]}")
    source_url: str
    source_id: str | None = None
    category: SourceCategory = SourceCategory.UNKNOWN
    title: str = ""
    content: str = ""
    summary: str = ""
    embedding: list[float] = Field(default_factory=list)
    value_score: float = 0.0
    reused_count: int = 0
    created_at: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())
    updated_at: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())
    pruned_at: str | None = None


class SourceGovernance:
    """Phase 7 — Source Governance. ROADMAP §7."""

    SOURCES_TABLE = "ecosystem_sources"
    POLICIES_TABLE = "ecosystem_source_policies"
    LEARNED_TABLE = "ecosystem_learned_items"

    def __init__(self) -> None:
        self._ensure_schema()

    def _ensure_schema(self) -> None:
        with get_conn() as conn:
            conn.execute(f"""CREATE TABLE IF NOT EXISTS {self.SOURCES_TABLE} (
                source_id TEXT PRIMARY KEY, url TEXT NOT NULL UNIQUE,
                category TEXT NOT NULL DEFAULT 'UNKNOWN', state TEXT NOT NULL DEFAULT 'UNKNOWN',
                first_seen_at TEXT NOT NULL, last_seen_at TEXT NOT NULL,
                metadata TEXT DEFAULT '{{}}')""")
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.SOURCES_TABLE}_state ON {self.SOURCES_TABLE}(state)"
            )
            conn.execute(f"""CREATE TABLE IF NOT EXISTS {self.POLICIES_TABLE} (
                policy_id TEXT PRIMARY KEY, url_pattern TEXT NOT NULL UNIQUE,
                category TEXT NOT NULL DEFAULT 'UNKNOWN', state TEXT NOT NULL DEFAULT 'UNKNOWN',
                allowed_actions TEXT DEFAULT '[]', source_weight REAL DEFAULT 1.0,
                expires_at TEXT, created_by TEXT DEFAULT 'system',
                created_at TEXT NOT NULL, updated_at TEXT NOT NULL)""")
            conn.execute(f"""CREATE TABLE IF NOT EXISTS {self.LEARNED_TABLE} (
                item_id TEXT PRIMARY KEY, source_url TEXT NOT NULL, source_id TEXT,
                category TEXT NOT NULL DEFAULT 'UNKNOWN', title TEXT DEFAULT '',
                content TEXT DEFAULT '', summary TEXT DEFAULT '',
                embedding TEXT DEFAULT '[]', value_score REAL DEFAULT 0,
                reused_count INTEGER DEFAULT 0, created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL, pruned_at TEXT)""")
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.LEARNED_TABLE}_value ON {self.LEARNED_TABLE}(value_score)"
            )
            conn.commit()

    def discover(
        self, url: str, *, category: SourceCategory = SourceCategory.UNKNOWN
    ) -> dict[str, Any]:
        now = datetime.now(UTC).isoformat()
        with get_conn() as conn:
            existing = conn.execute(
                f"SELECT * FROM {self.SOURCES_TABLE} WHERE url=?", (url,)
            ).fetchone()
            if existing:
                conn.execute(
                    f"UPDATE {self.SOURCES_TABLE} SET last_seen_at=? WHERE source_id=?",
                    (now, existing["source_id"]),
                )
                conn.commit()
                return self._source_row(existing)
            source_id = f"src-{uuid.uuid4().hex[:16]}"
            policy = self.match_policy(url)
            state = policy.state if policy else SourceState.DISCOVERED
            conn.execute(
                f"INSERT INTO {self.SOURCES_TABLE} (source_id, url, category, state, first_seen_at, last_seen_at, metadata) "
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                (source_id, url, category, state, now, now, "{}"),
            )
            conn.commit()
            return {
                "source_id": source_id,
                "url": url,
                "category": str(category),
                "state": str(state),
                "first_seen_at": now,
                "last_seen_at": now,
                "metadata": {},
            }

    def transition_source(self, source_id: str, to: SourceState) -> dict[str, Any]:
        with get_conn() as conn:
            r = conn.execute(
                f"SELECT * FROM {self.SOURCES_TABLE} WHERE source_id=?", (source_id,)
            ).fetchone()
            if r is None:
                raise SourceStateError(f"Source {source_id} not found")
            current = SourceState(r["state"])
            if to not in _ALLOWED.get(current, set()):
                raise SourceStateError(f"Illegal transition {current} → {to}")
            now = datetime.now(UTC).isoformat()
            conn.execute(
                f"UPDATE {self.SOURCES_TABLE} SET state=?, last_seen_at=? WHERE source_id=?",
                (to, now, source_id),
            )
            conn.commit()
            return {"source_id": source_id, "state": str(to), "updated_at": now}

    def is_allowed(self, url: str) -> bool:
        with get_conn() as conn:
            r = conn.execute(
                f"SELECT state FROM {self.SOURCES_TABLE} WHERE url=?", (url,)
            ).fetchone()
        if r:
            st = SourceState(r["state"])
            if st == SourceState.ALLOWLISTED:
                return True
            if st == SourceState.BLOCKED:
                return False
        policy = self.match_policy(url)
        if policy:
            return policy.state == SourceState.ALLOWLISTED
        return False

    def add_policy(self, p: SourcePolicy) -> SourcePolicy:
        with get_conn() as conn:
            conn.execute(self._policy_insert_sql(), self._policy_row(p))
            conn.commit()
        return p

    def match_policy(self, url: str) -> SourcePolicy | None:
        with get_conn() as conn:
            rows = conn.execute(
                f"SELECT * FROM {self.POLICIES_TABLE} ORDER BY created_at ASC"
            ).fetchall()
        for r in rows:
            pat = r["url_pattern"]
            matched = False
            try:
                matched = bool(re.match(pat, url))
            except re.error:
                matched = False
            if not matched:
                matched = self._glob_match(pat, url)
            if matched:
                return self._policy_from(r)
        return None

    def record_learned(self, item: LearnedItem) -> LearnedItem:
        with get_conn() as conn:
            conn.execute(self._learned_insert_sql(), self._learned_row(item))
            conn.commit()
        return item

    def list_learned(
        self, *, category: SourceCategory | None = None, min_value: float = 0.0, limit: int = 100
    ) -> list[LearnedItem]:
        clauses, params = ["value_score >= ?", "pruned_at IS NULL"], [min_value]
        if category:
            clauses.append("category=?")
            params.append(category)
        params.append(limit)
        with get_conn() as conn:
            rows = conn.execute(
                f"SELECT * FROM {self.LEARNED_TABLE} WHERE {' AND '.join(clauses)} "
                "ORDER BY value_score DESC, created_at DESC LIMIT ?",
                params,
            ).fetchall()
        return [self._learned_from(r) for r in rows]

    def prune_low_value(self, *, threshold: float = 0.1, max_age_days: int = 30) -> int:
        now = datetime.now(UTC).isoformat()
        with get_conn() as conn:
            cur = conn.execute(
                f"UPDATE {self.LEARNED_TABLE} SET pruned_at=? WHERE value_score < ? AND pruned_at IS NULL",
                (now, threshold),
            )
            n = cur.rowcount
            conn.commit()
        return n

    @staticmethod
    def _glob_match(pattern: str, url: str) -> bool:
        regex = "^" + re.escape(pattern).replace("\\*", ".*").replace("\\?", ".") + "$"
        return bool(re.match(regex, url))

    def _policy_insert_sql(self) -> str:
        cols = (
            "policy_id,url_pattern,category,state,allowed_actions,source_weight,expires_at,"
            "created_by,created_at,updated_at"
        )
        return f"INSERT INTO {self.POLICIES_TABLE} ({cols}) VALUES ({','.join(['?'] * 10)})"

    def _policy_row(self, p: SourcePolicy) -> tuple:
        return (
            p.policy_id,
            p.url_pattern,
            p.category,
            p.state,
            jdump(p.allowed_actions),
            p.source_weight,
            p.expires_at,
            p.created_by,
            p.created_at,
            p.updated_at,
        )

    def _policy_from(self, r: Any) -> SourcePolicy:
        return SourcePolicy(
            policy_id=r["policy_id"],
            url_pattern=r["url_pattern"],
            category=SourceCategory(r["category"]),
            state=SourceState(r["state"]),
            allowed_actions=jload(r["allowed_actions"], []),
            source_weight=float(r["source_weight"] or 1.0),
            expires_at=r["expires_at"],
            created_by=r["created_by"],
            created_at=r["created_at"],
            updated_at=r["updated_at"],
        )

    def _learned_insert_sql(self) -> str:
        cols = (
            "item_id,source_url,source_id,category,title,content,summary,embedding,"
            "value_score,reused_count,created_at,updated_at,pruned_at"
        )
        return f"INSERT INTO {self.LEARNED_TABLE} ({cols}) VALUES ({','.join(['?'] * 13)})"

    def _learned_row(self, i: LearnedItem) -> tuple:
        return (
            i.item_id,
            i.source_url,
            i.source_id,
            i.category,
            i.title,
            i.content,
            i.summary,
            jdump(i.embedding),
            i.value_score,
            i.reused_count,
            i.created_at,
            i.updated_at,
            i.pruned_at,
        )

    def _learned_from(self, r: Any) -> LearnedItem:
        return LearnedItem(
            item_id=r["item_id"],
            source_url=r["source_url"],
            source_id=r["source_id"],
            category=SourceCategory(r["category"]),
            title=r["title"],
            content=r["content"],
            summary=r["summary"],
            embedding=jload(r["embedding"], []),
            value_score=float(r["value_score"] or 0),
            reused_count=int(r["reused_count"] or 0),
            created_at=r["created_at"],
            updated_at=r["updated_at"],
            pruned_at=r["pruned_at"],
        )

    @staticmethod
    def _source_row(r: Any) -> dict[str, Any]:
        return {
            "source_id": r["source_id"],
            "url": r["url"],
            "category": r["category"],
            "state": r["state"],
            "first_seen_at": r["first_seen_at"],
            "last_seen_at": r["last_seen_at"],
            "metadata": jload(r["metadata"], {}),
        }


_governance: SourceGovernance | None = None


def get_source_governance() -> SourceGovernance:
    global _governance
    if _governance is None:
        _governance = SourceGovernance()
    return _governance


__all__ = [
    "SourceState",
    "SourceCategory",
    "SourcePolicy",
    "LearnedItem",
    "SourceGovernance",
    "get_source_governance",
    "SourceStateError",
]
