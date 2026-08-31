"""Seed the ecosystem foundation with default capabilities, policies, and resources.

বাংলা: এই script-টি ecosystem-এর "cold start" কে সহজ করে — admin-এর জন্য কিছু
default capability, source policy, এবং well-known resource registry entry
তৈরি করে দেয়। এটি idempotent: একাধিকবার চালালেও duplicate হবে না।

চালানোর নিয়ম (backend ফোল্ডার থেকে):
    python -m scripts.seed_ecosystem          # বা
    python scripts/seed_ecosystem.py
"""

from __future__ import annotations

import sys
from pathlib import Path

# বাংলা: script সরাসরি চালালেও backend ফোল্ডার sys.path-এ থাকে।
_THIS = Path(__file__).resolve()
_BACKEND = _THIS.parent.parent
if str(_BACKEND) not in sys.path:
    sys.path.insert(0, str(_BACKEND))

from ecosystem import (  # noqa: E402
    Capability,
    CapabilityLifecycleState,
    CapabilityRuntimeTier,
    ProviderKind,
    ResourceRecord,
    SourceCategory,
    SourcePolicy,
    SourceState,
    get_capability_registry,
    get_resource_registry,
    get_source_governance,
)


# ---------------------------------------------------------------------------
# Default capabilities (ROADMAP §15 — HOT capabilities by default)
# ---------------------------------------------------------------------------

DEFAULT_CAPABILITIES = [
    {
        "name": "PDF Text Extraction",
        "purpose": "Extract text content from PDF documents",
        "signature": "pdf.extract.text.v1",
        "category": "document",
        "execution_method": "in_process",
        "runtime_tier": "HOT",
        "security_level": "standard",
    },
    {
        "name": "Web Research",
        "purpose": "Search and retrieve information from approved internet sources",
        "signature": "web.research.v1",
        "category": "research",
        "execution_method": "worker",
        "runtime_tier": "HOT",
        "security_level": "standard",
    },
    {
        "name": "Code Generation",
        "purpose": "Generate code from natural-language requirements",
        "signature": "code.generate.v1",
        "category": "coding",
        "execution_method": "in_process",
        "runtime_tier": "HOT",
        "security_level": "standard",
    },
    {
        "name": "GitHub Operations",
        "purpose": "Create branches, commits, pull requests on GitHub repositories",
        "signature": "github.ops.v1",
        "category": "devops",
        "execution_method": "worker",
        "runtime_tier": "HOT",
        "security_level": "elevated",
    },
    {
        "name": "Document Generation",
        "purpose": "Generate structured documents (PDF, DOCX, XLSX)",
        "signature": "document.generate.v1",
        "category": "document",
        "execution_method": "in_process",
        "runtime_tier": "HOT",
        "security_level": "standard",
    },
    {
        "name": "Browser Automation",
        "purpose": "Headless browser automation via Playwright",
        "signature": "browser.automate.v1",
        "category": "automation",
        "execution_method": "browser",
        "runtime_tier": "WARM",
        "security_level": "elevated",
    },
    {
        "name": "Heavy Compute (Kaggle)",
        "purpose": "GPU inference, large batch, embeddings on Kaggle",
        "signature": "compute.kaggle.v1",
        "category": "compute",
        "execution_method": "kaggle",
        "runtime_tier": "COLD",
        "security_level": "standard",
    },
]


# ---------------------------------------------------------------------------
# Default source policies (ROADMAP §7, §8)
# ---------------------------------------------------------------------------

DEFAULT_POLICIES = [
    {
        "name": "Allowlist official AI documentation",
        "scope": "category",
        "scope_value": "AI_DOCS",
        "decision": "ALLOWLISTED",
        "reason": "Approved AI provider documentation",
        "rate_limit_per_minute": 60,
        "crawl_budget_per_day": 1000,
    },
    {
        "name": "Allowlist approved OSS repos",
        "scope": "category",
        "scope_value": "OSS_REPO",
        "decision": "ALLOWLISTED",
        "reason": "Open-source repositories on GitHub",
        "rate_limit_per_minute": 60,
        "crawl_budget_per_day": 2000,
    },
    {
        "name": "Allowlist technical documentation",
        "scope": "category",
        "scope_value": "TECH_DOCS",
        "decision": "ALLOWLISTED",
        "reason": "Standards / technical documentation sites",
        "rate_limit_per_minute": 30,
        "crawl_budget_per_day": 800,
    },
    {
        "name": "Defer research papers",
        "scope": "category",
        "scope_value": "RESEARCH",
        "decision": "DEFERRED",
        "reason": "Requires per-source approval due to licensing",
        "rate_limit_per_minute": 10,
        "crawl_budget_per_day": 100,
        "requires_approval": True,
    },
    {
        "name": "Block unknown domains by default",
        "scope": "category",
        "scope_value": "UNKNOWN",
        "decision": "BLOCKED",
        "reason": "Unknown sources require admin approval before crawling",
        "rate_limit_per_minute": 0,
        "crawl_budget_per_day": 0,
        "requires_approval": True,
    },
]


def seed_capabilities() -> tuple[int, int]:
    """ROADMAP §12 — seed default HOT capabilities."""
    reg = get_capability_registry()
    created, skipped = 0, 0
    for item in DEFAULT_CAPABILITIES:
        if reg.find_by_signature(item["signature"]):
            skipped += 1
            continue
        cap = Capability(
            name=item["name"],
            purpose=item["purpose"],
            signature=item["signature"],
            category=item["category"],
            execution_method=item["execution_method"],
            runtime_tier=CapabilityRuntimeTier(item["runtime_tier"]),
            security_level=item["security_level"],
            lifecycle_state=CapabilityLifecycleState.ACTIVE,
            source="internal",
            owner="system",
            activation_metadata={"seed": True},
        )
        reg.register(cap)
        created += 1
    return created, skipped


def seed_policies() -> tuple[int, int]:
    """ROADMAP §8 — seed default source policies."""
    gov = get_source_governance()
    created, skipped = 0, 0
    for item in DEFAULT_POLICIES:
        # idempotency: skip if a policy with same scope+value+decision exists
        existing = gov.match_policy(
            domain=item["scope_value"] if item["scope"] == "domain" else None,
            category=SourceCategory(item["scope_value"]) if item["scope"] == "category" else None,
        )
        if existing and existing.decision == SourceState(item["decision"]):
            skipped += 1
            continue
        policy = SourcePolicy(
            name=item["name"],
            scope=item["scope"],
            scope_value=item["scope_value"],
            decision=SourceState(item["decision"]),
            reason=item["reason"],
            rate_limit_per_minute=item["rate_limit_per_minute"],
            crawl_budget_per_day=item["crawl_budget_per_day"],
            requires_approval=item.get("requires_approval", False),
        )
        gov.add_policy(policy)
        created += 1
    return created, skipped


def main() -> int:
    print(">>> Seeding SupremeAI ecosystem foundation...")

    cap_created, cap_skipped = seed_capabilities()
    print(f"  capabilities: created={cap_created} skipped(existing)={cap_skipped}")

    pol_created, pol_skipped = seed_policies()
    print(f"  source policies: created={pol_created} skipped(existing)={pol_skipped}")

    print(">>> Ecosystem seed complete. (idempotent — safe to re-run)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
