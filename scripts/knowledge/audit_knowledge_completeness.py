#!/usr/bin/env python3
"""
audit_knowledge_completeness.py — SupremAI Knowledge Gap Scanner

Scans core_knowledge.json + autonomous_seed_knowledge.json + system_learning
against the mandatory offline-coverage checklist and produces a gap report.

Exit codes:
  0 = PASS  (all mandatory categories have >= 3 entries)
  1 = FAIL  (one or more critical gaps found)
  2 = ERROR (file not found, invalid JSON, Firestore unavailable)

Usage:
  python scripts/audit_knowledge_completeness.py
  python scripts/audit_knowledge_completeness.py --format json   # JSON output
  python scripts/audit_knowledge_completeness.py --report audit_report.json
"""

import json
import sys
import os
import re
from collections import defaultdict
from datetime import datetime

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CORE_KNOWLEDGE_PATH = os.path.join(
    REPO_ROOT, "src", "main", "resources", "core_knowledge.json"
)
AUTONOMOUS_SEED_PATH = os.path.join(REPO_ROOT, "autonomous_seed_knowledge.json")
REFRESH_FIRESTORE = "--firestore" in sys.argv

MANDATORY_CATEGORIES = {
    "Network failure recovery": ["network", "timeout", "dns", "firewall", "connection"],
    "Memory exhaustion": ["memory", "outofmemory", "heap", "oom"],
    "Database migration": ["migration", "flyway", "liquibase", "database"],
    "SSL/TLS certificate": ["ssl", "tls", "certificate"],
    "Rate limiting / quota": ["rate", "limit", "quota", "throttl"],
    "Complete AI blackout": ["blackout", "thunder", "ai fail", "all ai", "emergency"],
    "Cascading failure": ["cascad", "cascade", "multi-provider"],
    "Self-healing recovery": ["self-heal", "auto-recovery", "self-repair"],
    "Graceful degradation": ["graceful", "degradation", "degrad"],
    "Provider health / quarantine": ["quarantine", "health check", "provider isolat"],
    "Provider migration": ["migrat", "provider migration", "knowledge transfer"],
    "Seed rebuild": ["seed rebuild", "knowledge seed", "reconstruct"],
    "Confidence-weighted voting": ["voting", "confidence-weighted", "quality gate"],
    "Zero-AI offline operation": ["zero ai", "offline mode", "no ai", "standalone"],
    "Knowledge bootstrap from zero": ["bootstrap", "from scratch", "fresh start"],
    "Local AI model setup": ["local ai", "ollama", "llama.cpp", "on-device"],
    "P2P knowledge sync": ["p2p", "peer-to-peer", "peer"],
    "Observability (Prometheus/grafana)": [
        "prometheus",
        "grafana",
        "metric",
        "observability",
    ],
}


def safe_load_json(path):
    if not os.path.exists(path):
        return [], f"File not found: {path}"
    try:
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
        return data, None
    except json.JSONDecodeError as e:
        return [], f"Invalid JSON: {path} — {e}"


def text_of(entry):
    """Return the concatenation of task + solution for a knowledge entry."""
    task = str(entry.get("task", "")).lower()
    solution = str(entry.get("solution", "")).lower()
    return task + " " + solution


def count_category(all_entries, category_name, keywords):
    """Return entries that match any of the category keywords."""
    matched = []
    seen_ids = set()
    for entry in all_entries:
        t = text_of(entry)
        if any(kw in t for kw in keywords):
            # Avoid counting the same entry twice
            eid = entry.get("task", str(entry))
            if eid not in seen_ids:
                seen_ids.add(eid)
                matched.append(entry)
    return matched


def check_freshness(all_entries):
    """Return list of stale entries not used in > 90 days (tracked via timesApplied + lastUsed if present)."""
    stale = []
    for e in all_entries:
        last_used = e.get("lastUsed")
        if last_used and isinstance(last_used, str):
            try:
                lu_dt = datetime.fromisoformat(last_used)
                age_days = (datetime.now() - lu_dt).days
                if age_days > 90 and (e.get("timesApplied", 0) == 0):
                    stale.append({"task": e.get("task", "?"), "lastUsed": last_used})
            except ValueError:
                pass
    return stale


def check_broken_paths(all_entries):
    """Flag entries that reference paths/tools/versions that may no longer exist."""
    broken = []
    # Common stale-path patterns
    stale_patterns = [
        r"supremeai-a/supremeai_ecosystem_plan\.md",  # doc moved/renamed
        r"http://localhost:\d+/admin",  # vary in dev (may be correct)
        r"GET /api/admin/providers/health[^\"']*",  # endpoint may not exist yet
        r"kill-switch diagnose",  # may not exist on all installs
    ]
    for e in all_entries:
        t = text_of(e)
        for pat in stale_patterns:
            hits = re.findall(pat, t, re.IGNORECASE)
            if hits:
                broken.append(
                    {
                        "task": e.get("task", "?"),
                        "pattern": pat,
                        "hits": hits,
                        "action": "REVIEW — verify path/tool/endpoint still exists",
                    }
                )
    return broken


def check_duplicate_keywords(all_entries):
    """Find duplicate task keyword strings across entries."""
    dupes = defaultdict(list)
    for e in all_entries:
        task_keywords = e.get("task", "")
        for kw in task_keywords.split():
            if len(kw) >= 3:
                dupes[kw].append(e.get("task", "?"))
    return {kw: tasks for kw, tasks in dupes.items() if len(tasks) > 1}


def _extract_knowledge_entries(json_data):
    """Extract the list of knowledge entries from either a flat list or an envelope object."""
    if isinstance(json_data, list):
        return json_data
    if isinstance(json_data, dict):
        # Common envelopes: {"seed_knowledge": [...]}, {"entries": [...]}, {"knowledge": [...]}
        for key in ("seed_knowledge", "entries", "knowledge", "learnings", "items"):
            if key in json_data and isinstance(json_data[key], list):
                return json_data[key]
        return []  # no recognizable entry list
    return []


def make_report(core, autonomous, seed_entries):
    now = datetime.utcnow().isoformat() + "Z"
    autonomous_entries = _extract_knowledge_entries(autonomous)
    # Combined entry pool
    combined = list(core) + list(autonomous_entries) + list(seed_entries)
    total = len(combined)

    # Coverage scan
    gap_results = {}
    for cat, keywords in MANDATORY_CATEGORIES.items():
        matched = count_category(combined, cat, keywords)
        gap_results[cat] = {
            "count": len(matched),
            "threshold": 3,
            "status": "OK" if len(matched) >= 3 else "GAP",
            "keywords": keywords,
        }

    # Staleness
    stale = check_freshness(combined)

    # Broken paths
    broken = check_broken_paths(combined)

    # Duplicate keywords
    dupes = check_duplicate_keywords(core)

    # Score
    ok_categories = sum(1 for v in gap_results.values() if v["status"] == "OK")
    total_categories = len(gap_results)
    coverage_pct = round(ok_categories / total_categories * 100, 1)

    # Gaps summary
    all_gaps = [
        {"category": k, **v} for k, v in gap_results.items() if v["status"] == "GAP"
    ]
    critical_gaps = [g for g in all_gaps if g["count"] == 0]

    report = {
        "scanTimestamp": now,
        "version": "1.0",
        "summary": {
            "totalEntries": total,
            "core_knowledge_entries": len(core),
            "autonomous_seed_entries": len(autonomous),
            "system_learning_entries": len(seed_entries),
            "mandatoryCategories": total_categories,
            "categoriesPass": ok_categories,
            "categoriesFail": total_categories - ok_categories,
            "coveragePercent": coverage_pct,
        },
        "gapsFound": len(all_gaps),
        "criticalGaps": len(critical_gaps),
        "gapDetails": all_gaps,
        "staleEntries": stale,
        "brokenPathEntries": broken,
        "duplicateKeywords": dupes if dupes else None,
        "overallPass": len(critical_gaps) == 0 and coverage_pct >= 80,
    }
    return report


def print_human(report):
    s = report["summary"]
    print("\n" + "=" * 65)
    print("  KNOWLEDGE GAP SCAN REPORT")
    print("=" * 65)
    print(f"\nScanned at : {report['scanTimestamp']}")
    print(f"Total entries: {s['totalEntries']}")
    print(f"  core_knowledge.json      : {s['core_knowledge_entries']}")
    print(f"  autonomous_seed_knowledge: {s['autonomous_seed_entries']}")
    print(f"  system_learning (seed)   : {s['system_learning_entries']}")
    print(
        f"\nCoverage : {s['coveragePercent']}% ({s['categoriesPass']}/{s['mandatoryCategories']} categories)"
    )
    print(f"Gaps found: {report['gapsFound']}  (CRITICAL: {report['criticalGaps']})")

    if report["gapDetails"]:
        print("\n  --- Gap Details ---")
        for g in report["gapDetails"]:
            flag = "!! CRITICAL" if g["count"] == 0 else "  ! WARN"
            print(f"  {flag}  [{g['count']}/{g['threshold']}]  {g['category']}")

    if report["staleEntries"]:
        print(f"\n  Stale entries (unused > 90d): {len(report['staleEntries'])}")
        for e in report["staleEntries"]:
            print(f"    - {e['task'][:60]}  (lastUsed: {e['lastUsed']})")

    if report.get("brokenPathEntries"):
        print(
            f"\n  Path/tool/endpoint review needed: {len(report['brokenPathEntries'])}"
        )
        for b in report["brokenPathEntries"]:
            print(f"    - {b['task'][:60]}  | {b['action']}")

    if report.get("duplicateKeywords"):
        dupe_count = len(report["duplicateKeywords"])
        print(f"\n  Duplicate keywords in core_knowledge.json: {dupe_count}")
        for kw, tasks in list(report["duplicateKeywords"].items())[:10]:
            print(f"    - '{kw}' appears in: {tasks[:2]}")

    print(f"\nOverall: {'PASS  ✓' if report['overallPass'] else 'FAIL  ✗'}")
    print("=" * 65)


def main():
    core, core_err = safe_load_json(CORE_KNOWLEDGE_PATH)
    autonomous, auto_err = safe_load_json(AUTONOMOUS_SEED_PATH)

    if core_err:
        print(f"ERROR: {core_err}", file=sys.stderr)
        sys.exit(2)
    if auto_err:
        print(f"WARNING: {auto_err}")

    # Build a synthetic seed_entries list from core_knowledge task+solution entries (flat list)
    seed_entries = list(core)  # core is already the array of {task, solution}

    report = make_report(core, autonomous, seed_entries)

    fmt = "json" if "--format" in sys.argv and "json" in sys.argv else "human"
    if fmt == "json":
        print(json.dumps(report, indent=2, ensure_ascii=False))
    else:
        print_human(report)

    if "--report" in sys.argv:
        idx = sys.argv.index("--report")
        if idx + 1 < len(sys.argv):
            out_path = sys.argv[idx + 1]
            os.makedirs(os.path.dirname(out_path) or ".", exist_ok=True)
            with open(out_path, "w") as f:
                json.dump(report, f, indent=2, ensure_ascii=False)
            print(f"\nReport written to: {out_path}")

    sys.exit(0 if report["overallPass"] else 1)


if __name__ == "__main__":
    main()
