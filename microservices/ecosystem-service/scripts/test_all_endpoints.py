"""End-to-end test suite for the standalone ecosystem test harness.

বাংলা: এই script-টি পুরো acceptance checklist automated verify করে।
এক কমান্ডে চালানো যায়:

    python scripts/test_all_endpoints.py --base http://localhost:8000 --admin-token $ADMIN_TOKEN

অথবা deployed Render service-এর বিরুদ্ধে:

    python scripts/test_all_endpoints.py --base https://your-app.onrender.com --admin-token $ADMIN_TOKEN

যদি real provider credentials set করা থাকে (RENDER_API_KEY ইত্যাদি), সেগুলোও
live test হবে। না থাকলে adapter ছাড়াই foundation endpoints যাচাই হবে।
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from typing import Any

try:
    import httpx
except ImportError:
    print("ERROR: httpx required. pip install httpx", file=sys.stderr)
    sys.exit(1)


# ---------------------------------------------------------------------------
# Tiny test framework
# ---------------------------------------------------------------------------

passed: list[str] = []
failed: list[tuple[str, str]] = []
skipped: list[str] = []


def check(name: str, cond: bool, detail: str = "") -> None:
    if cond:
        passed.append(name)
        print(f"  ✓ {name}")
    else:
        failed.append((name, detail))
        print(f"  ✗ {name} — {detail}")


def skip(name: str, reason: str) -> None:
    skipped.append(name)
    print(f"  ⊘ {name} (skipped: {reason})")


def get(base: str, path: str, *, token: str | None = None, expect: int = 200) -> Any:
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    with httpx.Client(timeout=30) as c:
        r = c.get(f"{base}{path}", headers=headers)
        if r.status_code != expect:
            return {"_error": f"status {r.status_code}", "_body": r.text[:200]}
        try:
            return r.json()
        except Exception:
            return {"_raw": r.text[:200]}


def post(
    base: str, path: str, body: Any, *, token: str | None = None, expect: int = 200
) -> Any:
    headers = {"Authorization": f"Bearer {token}", "content-type": "application/json"} if token else {"content-type": "application/json"}
    with httpx.Client(timeout=30) as c:
        r = c.post(f"{base}{path}", headers=headers, json=body)
        if r.status_code != expect:
            return {"_error": f"status {r.status_code}", "_body": r.text[:200]}
        try:
            return r.json()
        except Exception:
            return {"_raw": r.text[:200]}


# ---------------------------------------------------------------------------
# Test plan
# ---------------------------------------------------------------------------


def test_health_root(base: str) -> None:
    print("\n── 0. Health + root ─────────────────────────────────────")
    r = get(base, "/health")
    check("GET /health returns ok", r.get("status") == "ok", str(r)[:120])
    r = get(base, "/")
    check("GET / returns service info", "endpoints" in r, str(r)[:120])


def test_user_capability_endpoints(base: str) -> None:
    print("\n── 1. Capability endpoints (ROADMAP §12, §14) ────────────")
    r = get(base, "/api/v1/ecosystem/capabilities")
    check("GET /capabilities returns list", isinstance(r, list) and len(r) >= 1, str(r)[:120])
    seeded_signatures = {c.get("signature") for c in r} if isinstance(r, list) else set()
    check("default seeded capabilities present", "pdf.extract.text.v1" in seeded_signatures, str(seeded_signatures))

    r = post(base, "/api/v1/ecosystem/capabilities/search", {"requirement": "extract text from pdf"})
    check("POST /capabilities/search returns candidates", "candidates" in r, str(r)[:120])
    check("search rule is REUSE>ADAPT>EXTEND>CREATE", r.get("rule", "").startswith("REUSE"), str(r.get("rule")))
    check("gap_detected flag present", "gap_detected" in r, str(r)[:120])


def test_task_lifecycle(base: str, token: str) -> None:
    print("\n── 2. Task engine lifecycle (ROADMAP §22, §23) ──────────")
    r = post(base, "/api/v1/ecosystem/tasks", {
        "goal": "Test: analyze a PDF and produce a report",
        "owner": "USER",
        "created_by": "test-script",
        "success_criteria": {"coverage": 1.0},
    })
    check("POST /tasks creates a task", "task_id" in r, str(r)[:120])
    tid = r.get("task_id")
    if not tid:
        return
    check("task has correlation task-id header", "x-correlation-task-id" in r.get("correlation", {}), str(r.get("correlation")))
    check("task starts in RECEIVED state", r.get("state") == "RECEIVED", str(r.get("state")))

    # advance through the happy path
    states = ["UNDERSTANDING", "PLANNING", "CAPABILITY_CHECK", "RESOURCE_CHECK",
              "PREPARING", "EXECUTING", "VERIFYING", "DELIVERING"]
    for s in states:
        r = post(base, f"/api/v1/ecosystem/tasks/{tid}/transition", {"to_state": s, "actor": "test-script"})
        if "_error" in r:
            check(f"transition to {s}", False, str(r)[:120])
            return
    r = post(base, f"/api/v1/ecosystem/tasks/{tid}/deliver", {"result": {"report": "done"}, "actor": "test-script"})
    check("task reaches COMPLETED", r.get("state") == "COMPLETED", str(r.get("state")))
    check("task has completed_at set", r.get("completed_at") is not None, str(r.get("completed_at")))


def test_resources_and_adapters(base: str, token: str) -> None:
    print("\n── 3. Resource registry + live adapters (ROADMAP §36, §37) ─")
    r = get(base, "/api/v1/ecosystem/resources")
    check("GET /resources returns list", isinstance(r, list), str(r)[:120])
    if isinstance(r, list) and r:
        providers_seen = {x.get("provider") for x in r}
        check("auto-registered providers detected", len(providers_seen) > 0, str(providers_seen))
        for res in r:
            rid = res.get("resource_id")
            prov = res.get("provider")
            # ROADMAP §45 — MCP get_health on a real resource
            mcp = post(base, "/api/v1/ecosystem/mcp/call", {
                "operation": "get_health",
                "arguments": {"resource_id": rid},
            })
            ok = mcp.get("ok") is True
            result = mcp.get("result", {}) if ok else mcp
            status = result.get("status") if isinstance(result, dict) else None
            check(f"MCP get_health({prov}) returns status", ok and status is not None, str(mcp)[:160])


def test_mcp_manifest_and_governance(base: str) -> None:
    print("\n── 4. MCP manifest + governance gate (ROADMAP §45, §28) ───")
    r = get(base, "/api/v1/ecosystem/mcp/manifest")
    check("manifest has observe/analyze/act", all(k in r for k in ("observe", "analyze", "act")), str(r)[:120])
    check("observe ops include get_health", "get_health" in r.get("observe", []), str(r.get("observe"))[:120])

    # ROADMAP §28 — deploy (high-risk) must be gated
    r = post(base, "/api/v1/ecosystem/mcp/call", {
        "operation": "deploy",
        "arguments": {"resource_id": "fake-res", "repository": "test", "commit_sha": "abc"},
    })
    check("MCP deploy (high-risk) blocked by governance", r.get("ok") is False and r.get("error") in ("approval_required", "denied_by_governance"), str(r)[:160])


def test_approval_workflow(base: str, token: str) -> None:
    print("\n── 5. Approval workflow + decision memory (ROADMAP §9, §26, §27) ─")
    # Create a proposal
    r = post(base, "/api/v1/ecosystem/admin/proposals", {
        "kind": "NEW_CAPABILITY",
        "title": "Add OCR capability (test)",
        "description": "scanned PDFs need OCR",
        "dedup_key": "test:ocr:v1",
        "priority": "HIGH",
        "risk_level": "medium",
    }, token=token)
    check("POST /admin/proposals creates proposal", "proposal_id" in r, str(r)[:120])
    pid = r.get("proposal_id")

    # Duplicate dedup test
    r2 = post(base, "/api/v1/ecosystem/admin/proposals", {
        "kind": "NEW_CAPABILITY",
        "title": "Add OCR capability (duplicate)",
        "description": "should be suppressed",
        "dedup_key": "test:ocr:v1",
    }, token=token)
    check("duplicate dedup_key suppressed (ROADMAP §26)", r2.get("state") in ("SUPERSEDED", "PENDING") and "suppressed" in r2.get("description", "").lower() or r2.get("proposal_id") == pid, str(r2)[:120])

    # Decide it
    if pid:
        r3 = post(base, f"/api/v1/ecosystem/admin/proposals/{pid}/decide", {
            "decision": "APPROVED",
            "resolved_by": "test-script",
            "reason": "verified during test",
            "policy_scope": "category",
            "policy_value": "document",
        }, token=token)
        check("POST /admin/proposals/{id}/decide approves", r3.get("state") == "APPROVED", str(r3)[:120])
        check("policy_generated captured", "decision" in r3.get("policy_generated", {}), str(r3.get("policy_generated")))

        # Decision memory persists
        r4 = get(base, "/api/v1/ecosystem/admin/decisions?dedup_key=test:ocr:v1", token=token)
        check("decision memory persisted", isinstance(r4, list) and len(r4) >= 1, str(r4)[:120])


def test_source_governance(base: str, token: str) -> None:
    print("\n── 6. Source governance (ROADMAP §7, §8, §9) ─────────────")
    # discover a covered URL (default policy allowlists AI_DOCS category)
    r = post(base, "/api/v1/ecosystem/admin/sources/discover", {
        "url": "https://openai.com/docs/test",
        "category": "AI_DOCS",
    }, token=token)
    check("discover covered URL auto-allowlisted", r.get("state") == "ALLOWLISTED", str(r)[:120])

    # discover unknown URL
    r = post(base, "/api/v1/ecosystem/admin/sources/discover", {
        "url": "https://random-unknown.example/x",
        "category": "UNKNOWN",
    }, token=token)
    check("discover unknown URL → DISCOVERED or BLOCKED", r.get("state") in ("DISCOVERED", "BLOCKED"), str(r)[:120])


def test_governance_engine(base: str, token: str) -> None:
    print("\n── 7. Governance decisions audit (ROADMAP §28, §54) ─────")
    r = get(base, "/api/v1/ecosystem/admin/governance/decisions?limit=10", token=token)
    check("GET /admin/governance/decisions returns list", isinstance(r, list), str(r)[:120])


def test_admin_overview(base: str, token: str) -> None:
    print("\n── 8. Admin overview (ROADMAP §47) ──────────────────────")
    r = get(base, "/api/v1/ecosystem/admin/overview", token=token)
    check("overview returns capability + approval + learning counts",
          "capabilities" in r and "approvals_pending" in r and "learning_opportunities" in r,
          str(r)[:120])


def test_admin_auth_rejects_unauthenticated(base: str) -> None:
    print("\n── 9. Admin auth gate (ROADMAP §28) ─────────────────────")
    r = get(base, "/api/v1/ecosystem/admin/overview")
    check("admin endpoint rejects unauthenticated call", isinstance(r, dict) and ("_error" in r or r.get("detail")), str(r)[:120])


def test_deployment_tracking(base: str, token: str) -> None:
    print("\n── 10. Deployment tracking (ROADMAP §40, §44) ───────────")
    # register a fake resource first
    r = post(base, "/api/v1/ecosystem/resources", {
        "name": "test-fake-resource",
        "provider": "custom",
        "type": "web_service",
    })
    rid = r.get("resource_id")
    if not rid:
        skip("deployment tracking test", "could not register a test resource")
        return
    # start a deployment via MCP
    r = post(base, "/api/v1/ecosystem/mcp/call", {
        "operation": "deploy",
        "arguments": {
            "resource_id": rid,
            "repository": "test",
            "commit_sha": "deadbeef",
            "triggered_by": "test-script",
        },
    })
    # ROADMAP §28 — deploy is high-risk; either blocked OR (if budget present) starts
    check("deploy MCP call returns governance outcome", r.get("ok") in (True, False), str(r)[:120])
    # trace by commit
    r = get(base, "/api/v1/ecosystem/deployments/trace/deadbeef")
    check("GET /deployments/trace/{commit} returns trace", "commit_sha" in r, str(r)[:120])


def test_learning_loop(base: str, token: str) -> None:
    print("\n── 11. Learning loop (ROADMAP §25, §57) ─────────────────")
    r = post(base, "/api/v1/ecosystem/admin/opportunities", {
        "requirement": "OCR scanned PDFs (test)",
        "usefulness": "high",
        "feasibility": "feasible",
        "risk": "low",
        "cost": "low",
    }, token=token)
    check("POST /admin/opportunities surfaces", "opportunity_id" in r, str(r)[:120])
    oid = r.get("opportunity_id")
    if not oid:
        return
    for stage in ["PRACTICALITY_ANALYSIS", "PROPOSAL", "AWAITING_APPROVAL"]:
        r = post(base, f"/api/v1/ecosystem/admin/opportunities/{oid}/advance", {"to_stage": stage}, token=token)
        check(f"advance to {stage}", r.get("stage") == stage, str(r)[:120])


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------


def main() -> int:
    ap = argparse.ArgumentParser(description="Standalone ecosystem test suite")
    ap.add_argument("--base", default=os.getenv("BASE_URL", "http://localhost:8000"), help="base URL")
    ap.add_argument("--admin-token", default=os.getenv("ADMIN_TOKEN", "test-admin-token-please-change"))
    args = ap.parse_args()
    base = args.base.rstrip("/")
    token = args.admin_token

    print(f">>> Testing ecosystem harness at {base}")
    print(f">>> admin token: {token[:6]}…{token[-2:] if len(token) > 8 else ''}")

    t0 = time.time()
    try:
        test_health_root(base)
        test_user_capability_endpoints(base)
        test_task_lifecycle(base, token)
        test_resources_and_adapters(base, token)
        test_mcp_manifest_and_governance(base)
        test_approval_workflow(base, token)
        test_source_governance(base, token)
        test_governance_engine(base, token)
        test_admin_overview(base, token)
        test_admin_auth_rejects_unauthenticated(base)
        test_deployment_tracking(base, token)
        test_learning_loop(base, token)
    except Exception as exc:  # noqa: BLE001
        print(f"\nFATAL: {exc}")
        failed.append(("suite", str(exc)))

    print("\n" + "═" * 60)
    print(f"RESULT: {len(passed)} passed, {len(failed)} failed, {len(skipped)} skipped ({time.time()-t0:.1f}s)")
    if failed:
        print("\nFAILURES:")
        for name, detail in failed:
            print(f"  ✗ {name}")
            print(f"      {detail}")
        return 1
    print("\n✅ ALL TESTS PASSED — ecosystem foundation verified.")
    print("   You can now safely apply the same patch to your production codebase.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
