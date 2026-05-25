#!/usr/bin/env python3
"""
seed_lib.py — Shared utilities for all SupremeAI seed-part scripts.

Provides:
  • _learning()       – Build a SystemLearning-compatible document dict
  • init_firestore()  – Connect to Firebase (cert file → ADC fallback)
  • batch_set()       – Write documents with rate-limiting delay between
                        chunks to avoid Firestore quota errors
  • run_part()        – Standard entry-point for every part script
"""

from __future__ import annotations

import os
import sys
import time
import uuid
from typing import Any, Dict, List, Optional

# ── Project settings ──────────────────────────────────────────────────────────
FIREBASE_PROJECT_ID: str = "supremeai-a"
CREDENTIALS_FILE: Optional[str] = os.getenv("FIREBASE_CREDENTIALS_FILE")

# How many Firestore writes per batch before we pause
BATCH_SIZE: int = int(os.getenv("SEED_BATCH_SIZE", "20"))
# Seconds to sleep between batches (prevents Firestore quota exhaustion)
BATCH_DELAY_S: float = float(os.getenv("SEED_BATCH_DELAY", "1.0"))


# ── Document builder ──────────────────────────────────────────────────────────

def _learning(
    type_: str,
    category: str,
    content: str,
    solutions: List[str],
    severity: str,
    confidence: float,
    resolved: bool = True,
    resolution: Optional[str] = None,
    error_count: int = 0,
    times_applied: int = 0,
    context: Optional[Dict[str, Any]] = None,
) -> Dict[str, Any]:
    """Return a dict that matches the SystemLearning.java Firestore model exactly."""
    return {
        "id": str(uuid.uuid4()),
        "type": type_,            # ERROR | PATTERN | IMPROVEMENT | REQUIREMENT
        "category": category,
        "content": content,
        "errorCount": error_count,
        "solutions": solutions,
        "context": context or {},
        "timestamp": int(time.time() * 1000),
        "severity": severity,     # CRITICAL | HIGH | MEDIUM | LOW
        "resolved": resolved,
        "resolution": resolution or (solutions[0] if solutions else ""),
        "timesApplied": times_applied,
        "confidenceScore": confidence,
    }


# ── Firebase helpers ──────────────────────────────────────────────────────────

def init_firestore(firebase_admin, credentials, firestore):
    """Initialize Firebase app (cert file first, then ADC fallback)."""
    if firebase_admin._apps:
        return firestore.client()

    cert_error = None
    if CREDENTIALS_FILE and os.path.exists(CREDENTIALS_FILE):
        try:
            cred = credentials.Certificate(CREDENTIALS_FILE)
            firebase_admin.initialize_app(cred)
            print(f"✅ Firebase initialised using credentials file: {CREDENTIALS_FILE}")
            return firestore.client()
        except Exception as err:
            cert_error = err
            print(f"⚠️  Credentials file error: {err}  →  trying ADC …")

    try:
        firebase_admin.initialize_app()
        print("✅ Firebase initialised using Application Default Credentials (ADC)")
        return firestore.client()
    except Exception as adc_error:
        base = f"ADC error: {adc_error}"
        if cert_error:
            base = f"Cert error: {cert_error} | {base}"
        raise RuntimeError(
            f"No valid Firebase credentials. {base}\n"
            "Set FIREBASE_CREDENTIALS_FILE or run: gcloud auth application-default login"
        )


def batch_set(
    db,
    collection_name: str,
    docs: Dict[str, Any],
    batch_size: int = BATCH_SIZE,
    delay: float = BATCH_DELAY_S,
) -> int:
    """
    Write *docs* into *collection_name* in chunks of *batch_size*.
    Pauses *delay* seconds between chunks to stay within Firestore quota.
    Returns the total number of documents written.
    """
    col = db.collection(collection_name)
    items = list(docs.items())
    total = 0

    for chunk_start in range(0, len(items), batch_size):
        chunk = items[chunk_start : chunk_start + batch_size]
        batch = db.batch()
        for doc_id, data in chunk:
            batch.set(col.document(doc_id), data)
        batch.commit()
        total += len(chunk)

        end_idx = chunk_start + len(chunk)
        print(f"   ✔ [{end_idx}/{len(items)}] written to '{collection_name}'")

        if end_idx < len(items):
            time.sleep(delay)

    return total


# ── Standard part runner ──────────────────────────────────────────────────────

def run_part(
    part_name: str,
    collections: Dict[str, Dict[str, Any]],
    dry_run_summary_fn=None,
):
    """
    Standard entry-point used by every seed_partN_*.py script.

    Args:
        part_name          : Human-readable name, e.g. "Part 1 — AI Fundamentals"
        collections        : { collection_name: { doc_id: doc_data, … }, … }
        dry_run_summary_fn : Optional callable that prints a custom dry-run summary.
    """
    print(f"\n🚀  SupremeAI Knowledge Seed — {part_name}\n")

    dry_run = "--dry-run" in sys.argv

    # ── Dry-run mode ──────────────────────────────────────────────────────────
    if dry_run:
        print("🔍  DRY RUN — no Firebase writes\n")
        total = 0
        for col_name, docs in collections.items():
            print(f"   {col_name}: {len(docs)} documents")
            total += len(docs)
        print(f"   {'─'*45}")
        print(f"   TOTAL: {total} documents")
        if dry_run_summary_fn:
            dry_run_summary_fn()
        print("\n✅  Structures valid — ready to seed Firebase.")
        sys.exit(0)

    # ── Firebase connection ───────────────────────────────────────────────────
    try:
        import firebase_admin
        from firebase_admin import credentials, firestore
    except ImportError:
        print("❌  firebase-admin not installed.  Run:  pip install firebase-admin")
        print("    Or preview without Firebase:      python <script>.py --dry-run")
        sys.exit(1)

    print("=" * 72)
    for col_name, docs in collections.items():
        print(f"   {col_name}: {len(docs)} documents")
    print("=" * 72)

    try:
        db = init_firestore(firebase_admin, credentials, firestore)
        print("\n✅  Connected to Firestore!\n")

        results: Dict[str, int] = {}
        for col_name, docs in collections.items():
            print(f"\n📁  Seeding '{col_name}' …")
            results[col_name] = batch_set(db, col_name, docs)

        # Summary
        total = sum(results.values())
        print("\n" + "=" * 72)
        print(f"✅  {part_name} — COMPLETE")
        print("=" * 72)
        for col_name, count in results.items():
            print(f"   • {col_name}: {count} documents")
        print(f"\n   TOTAL: {total} documents seeded")
        print(f"\n🔗  https://console.firebase.google.com/project/{FIREBASE_PROJECT_ID}/firestore")
        sys.exit(0)

    except Exception as exc:
        print(f"\n❌  Error: {exc}")
        sys.exit(1)
