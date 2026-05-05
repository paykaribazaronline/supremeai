#!/usr/bin/env python3
"""
seed_run_all.py — Orchestrator for all SupremeAI knowledge seed scripts.

Runs seed_part1 through seed_part8 sequentially with a configurable
inter-part delay to avoid Firebase Firestore quota exhaustion.

Usage:
  # Full run (all 8 parts):
  pip install firebase-admin
  python seed_run_all.py

  # Dry run (no Firebase, just preview counts):
  python seed_run_all.py --dry-run

  # Run only specific parts (comma-separated):
  python seed_run_all.py --parts 1,3,5

  # Custom delay between parts (seconds, default: 5):
  python seed_run_all.py --part-delay 10

  # Custom batch size and inter-batch delay:
  SEED_BATCH_SIZE=10 SEED_BATCH_DELAY=2.0 python seed_run_all.py
"""

from __future__ import annotations

import argparse
import os
import sys
import time

sys.path.insert(0, os.path.dirname(__file__))

# ── Import all parts ──────────────────────────────────────────────────────────

from seed_part1_ai_fundamentals import SYSTEM_LEARNINGS as P1_SL, AI_FUNDAMENTALS_DOCS as P1_AF
from seed_part2_software_architecture import (
    SYSTEM_LEARNINGS as P2_SL, SOFTWARE_ARCHITECTURE_DOCS as P2_SA,
)
from seed_part3_databases import SYSTEM_LEARNINGS as P3_SL, DATABASE_KNOWLEDGE_DOCS as P3_DB
from seed_part4_security import SYSTEM_LEARNINGS as P4_SL, SECURITY_KNOWLEDGE_DOCS as P4_SK
from seed_part5_devops_cloud import SYSTEM_LEARNINGS as P5_SL, DEVOPS_KNOWLEDGE_DOCS as P5_DK
from seed_part6_performance import SYSTEM_LEARNINGS as P6_SL, PERFORMANCE_KNOWLEDGE_DOCS as P6_PK
from seed_part7_testing import SYSTEM_LEARNINGS as P7_SL, TESTING_KNOWLEDGE_DOCS as P7_TK
from seed_part8_system_design import (
    SYSTEM_LEARNINGS as P8_SL, SYSTEM_DESIGN_KNOWLEDGE_DOCS as P8_SD,
)

# ── Part registry ─────────────────────────────────────────────────────────────

PARTS = [
    {
        "number": 1,
        "name": "AI Fundamentals",
        "collections": {
            "system_learning": P1_SL,
            "ai_fundamentals": P1_AF,
        },
    },
    {
        "number": 2,
        "name": "Software Architecture",
        "collections": {
            "system_learning": P2_SL,
            "software_architecture": P2_SA,
        },
    },
    {
        "number": 3,
        "name": "Databases & Data Engineering",
        "collections": {
            "system_learning": P3_SL,
            "database_knowledge": P3_DB,
        },
    },
    {
        "number": 4,
        "name": "Security",
        "collections": {
            "system_learning": P4_SL,
            "security_knowledge": P4_SK,
        },
    },
    {
        "number": 5,
        "name": "DevOps & Cloud",
        "collections": {
            "system_learning": P5_SL,
            "devops_knowledge": P5_DK,
        },
    },
    {
        "number": 6,
        "name": "Performance Optimisation",
        "collections": {
            "system_learning": P6_SL,
            "performance_knowledge": P6_PK,
        },
    },
    {
        "number": 7,
        "name": "Testing Strategies",
        "collections": {
            "system_learning": P7_SL,
            "testing_knowledge": P7_TK,
        },
    },
    {
        "number": 8,
        "name": "System Design",
        "collections": {
            "system_learning": P8_SL,
            "system_design_knowledge": P8_SD,
        },
    },
]


# ── Helpers ───────────────────────────────────────────────────────────────────

def _part_total(part: dict) -> int:
    return sum(len(docs) for docs in part["collections"].values())


def print_grand_summary(results: dict[int, dict[str, int]]) -> None:
    grand_total = 0
    print("\n" + "=" * 72)
    print("✅  ALL PARTS COMPLETE — GRAND SUMMARY")
    print("=" * 72)
    for part_num, col_counts in sorted(results.items()):
        part_name = next(p["name"] for p in PARTS if p["number"] == part_num)
        part_total = sum(col_counts.values())
        grand_total += part_total
        print(f"\n  Part {part_num}: {part_name} ({part_total} docs)")
        for col_name, count in col_counts.items():
            print(f"      • {col_name}: {count}")
    print(f"\n  {'─' * 50}")
    print(f"  GRAND TOTAL: {grand_total} documents seeded across all collections")
    from seed_lib import FIREBASE_PROJECT_ID
    print(f"\n🔗  https://console.firebase.google.com/project/{FIREBASE_PROJECT_ID}/firestore")
    print("\n💡  SupremeAI now knows about:")
    print("    1 — LLMs, RAG, prompt engineering, fine-tuning, AI agents")
    print("    2 — SOLID, design patterns, microservices, DDD, EDA")
    print("    3 — SQL, NoSQL, Firestore, indexes, transactions, migrations")
    print("    4 — OWASP Top 10, JWT, OAuth2, encryption, secrets, CORS")
    print("    5 — Docker, Kubernetes, Cloud Run, CI/CD, observability, SRE")
    print("    6 — Caching, JVM tuning, async, pagination, React performance")
    print("    7 — TDD, JUnit 5, Mockito, Playwright, Flutter testing, PITest")
    print("    8 — CAP theorem, scalability, messaging, gRPC, Event Sourcing, CQRS")
    print()


# ── Main ──────────────────────────────────────────────────────────────────────

def main() -> None:
    parser = argparse.ArgumentParser(
        description="Seed all SupremeAI knowledge parts into Firebase Firestore"
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Preview document counts without connecting to Firebase",
    )
    parser.add_argument(
        "--parts",
        default="",
        help="Comma-separated part numbers to run, e.g. '1,3,5' (default: all)",
    )
    parser.add_argument(
        "--part-delay",
        type=float,
        default=5.0,
        help="Seconds to pause between parts (default: 5.0)",
    )
    args = parser.parse_args()

    # Determine which parts to run
    if args.parts:
        requested = set(int(x.strip()) for x in args.parts.split(","))
        parts_to_run = [p for p in PARTS if p["number"] in requested]
    else:
        parts_to_run = PARTS

    print("\n🚀  SupremeAI — Mega Knowledge Seed Orchestrator")
    print(f"    Parts to run: {[p['number'] for p in parts_to_run]}")

    # ── Dry-run mode ─────────────────────────────────────────────────────────
    if args.dry_run:
        print("\n🔍  DRY RUN — no Firebase writes\n")
        grand_total = 0
        for part in parts_to_run:
            part_total = _part_total(part)
            grand_total += part_total
            print(f"  Part {part['number']}: {part['name']} — {part_total} documents")
            for col_name, docs in part["collections"].items():
                print(f"      {col_name}: {len(docs)}")
        print(f"\n  {'─'*50}")
        print(f"  GRAND TOTAL: {grand_total} documents")
        print("\n✅  Structures valid — ready to seed Firebase.")
        sys.exit(0)

    # ── Firebase connection ───────────────────────────────────────────────────
    try:
        import firebase_admin
        from firebase_admin import credentials, firestore as fs_module
    except ImportError:
        print("❌  firebase-admin not installed.  Run:  pip install firebase-admin")
        sys.exit(1)

    from seed_lib import init_firestore, batch_set, BATCH_SIZE, BATCH_DELAY_S

    try:
        db = init_firestore(firebase_admin, credentials, fs_module)
        print("✅  Connected to Firestore!\n")
    except Exception as exc:
        print(f"❌  Firebase connection failed: {exc}")
        sys.exit(1)

    # ── Seed each part ────────────────────────────────────────────────────────
    results: dict[int, dict[str, int]] = {}

    for idx, part in enumerate(parts_to_run):
        print(f"\n{'='*72}")
        print(f"  Part {part['number']}/{len(PARTS)}: {part['name']}")
        print(f"{'='*72}")
        part_results: dict[str, int] = {}

        for col_name, docs in part["collections"].items():
            print(f"\n📁  Seeding '{col_name}' ({len(docs)} documents) …")
            try:
                count = batch_set(db, col_name, docs, batch_size=BATCH_SIZE, delay=BATCH_DELAY_S)
                part_results[col_name] = count
                print(f"   ✅  '{col_name}' done — {count} documents written")
            except Exception as exc:
                print(f"   ❌  Failed to seed '{col_name}': {exc}")
                part_results[col_name] = 0

        results[part["number"]] = part_results
        part_total = sum(part_results.values())
        print(f"\n  ✔  Part {part['number']} complete: {part_total} documents seeded")

        # Pause between parts (not after the last one)
        if idx < len(parts_to_run) - 1:
            print(f"\n  ⏳  Waiting {args.part_delay}s before next part …")
            time.sleep(args.part_delay)

    print_grand_summary(results)
    sys.exit(0)


if __name__ == "__main__":
    main()
