#!/usr/bin/env python3
"""
SupremeAI Massive Knowledge Seed - Main Runner
Seeds Firebase with 500+ knowledge documents across 20+ collections.

Usage:
  pip install firebase-admin
  python scripts/seed_massive_knowledge.py
  python scripts/seed_massive_knowledge.py --dry-run   # preview only
"""

import uuid
import time
import json
import sys
import os

# ── Firebase setup ──────────────────────────────────────────────────────────
FIREBASE_PROJECT_ID = "supremeai-a"
CREDENTIALS_FILE = os.getenv("FIREBASE_CREDENTIALS_FILE")
DRY_RUN = "--dry-run" in sys.argv

def _ts():
    return int(time.time() * 1000)

def _uid():
    return str(uuid.uuid4())

def _learning(type_, category, content, solutions, severity,
              confidence, resolved=True, resolution=None,
              error_count=0, times_applied=0, context=None):
    """Match SystemLearning.java model exactly."""
    return {
        "id": _uid(),
        "type": type_,
        "category": category,
        "content": content,
        "errorCount": error_count,
        "solutions": solutions,
        "context": context or {},
        "timestamp": _ts(),
        "severity": severity,
        "resolved": resolved,
        "resolution": resolution or (solutions[0] if solutions else ""),
        "timesApplied": times_applied,
        "confidenceScore": confidence,
    }

def _pattern(name, category, description, when_to_use, code_example,
             framework, confidence, times_used=0):
    """Reusable pattern document for patterns collection."""
    return {
        "id": _uid(),
        "name": name,
        "category": category,
        "description": description,
        "when_to_use": when_to_use,
        "code_example": code_example,
        "framework": framework,
        "confidence": confidence,
        "times_used": times_used,
        "timestamp": _ts(),
        "source": "COPILOT_MASSIVE_SEED",
    }

def _error_fix(error_msg, cause, fix, language, framework,
               confidence, occurrences=0, ai_fixed="Claude"):
    """Error-fix pair for generation_errors_and_fixes collection."""
    return {
        "id": _uid(),
        "error_message": error_msg,
        "cause": cause,
        "fix": fix,
        "language": language,
        "framework": framework,
        "confidence": confidence,
        "occurrences": occurrences,
        "ai_that_fixed": ai_fixed,
        "timestamp": _ts(),
        "source": "COPILOT_MASSIVE_SEED",
    }

def _code_template(name, language, framework, template_type,
                   code, description, tags):
    """Code template/snippet for code_templates collection."""
    return {
        "id": _uid(),
        "name": name,
        "language": language,
        "framework": framework,
        "template_type": template_type,
        "code": code,
        "description": description,
        "tags": tags,
        "timestamp": _ts(),
        "usage_count": 0,
        "source": "COPILOT_MASSIVE_SEED",
    }

def _best_practice(title, category, description, do_list, dont_list,
                   severity, applies_to):
    """Best practice rule for best_practices collection."""
    return {
        "id": _uid(),
        "title": title,
        "category": category,
        "description": description,
        "do": do_list,
        "dont": dont_list,
        "severity": severity,
        "applies_to": applies_to,
        "timestamp": _ts(),
        "source": "COPILOT_MASSIVE_SEED",
    }

# ── Import all data modules ────────────────────────────────────────────────
from seed_data.languages import LANGUAGE_LEARNINGS, LANGUAGE_PATTERNS
from seed_data.frameworks import FRAMEWORK_LEARNINGS, FRAMEWORK_PATTERNS, FRAMEWORK_TEMPLATES
from seed_data.design_patterns import DESIGN_PATTERN_LEARNINGS, DESIGN_PATTERNS
from seed_data.security import SECURITY_LEARNINGS, SECURITY_PRACTICES
from seed_data.devops import DEVOPS_LEARNINGS, DEVOPS_PATTERNS, DEVOPS_TEMPLATES
from seed_data.databases import DATABASE_LEARNINGS, DATABASE_PATTERNS
from seed_data.testing import TESTING_LEARNINGS, TESTING_PATTERNS, TESTING_TEMPLATES
from seed_data.errors import ERROR_FIXES
from seed_data.api_and_performance import API_LEARNINGS, API_PATTERNS, PERFORMANCE_LEARNINGS
from seed_data.ai_ml import AI_ML_LEARNINGS, AI_ML_PATTERNS
from seed_data.system_design import SYSTEM_DESIGN_LEARNINGS, SYSTEM_DESIGN_PATTERNS
from seed_data.practices import PRACTICE_LEARNINGS, PRACTICE_BEST_PRACTICES


def seed_to_firebase():
    """Push all data to Firebase."""
    if DRY_RUN:
        print("=== DRY RUN MODE ===")
        counts = {
            "system_learning": (len(LANGUAGE_LEARNINGS) + len(FRAMEWORK_LEARNINGS) +
                               len(DESIGN_PATTERN_LEARNINGS) + len(SECURITY_LEARNINGS) +
                               len(DEVOPS_LEARNINGS) + len(DATABASE_LEARNINGS) +
                               len(TESTING_LEARNINGS) + len(API_LEARNINGS) +
                               len(PERFORMANCE_LEARNINGS) + len(AI_ML_LEARNINGS) +
                               len(SYSTEM_DESIGN_LEARNINGS) + len(PRACTICE_LEARNINGS)),
            "patterns": (len(LANGUAGE_PATTERNS) + len(FRAMEWORK_PATTERNS) +
                        len(DESIGN_PATTERNS) + len(DEVOPS_PATTERNS) +
                        len(DATABASE_PATTERNS) + len(TESTING_PATTERNS) +
                        len(API_PATTERNS) + len(AI_ML_PATTERNS) +
                        len(SYSTEM_DESIGN_PATTERNS)),
            "generation_errors_and_fixes": len(ERROR_FIXES),
            "code_templates": (len(FRAMEWORK_TEMPLATES) + len(DEVOPS_TEMPLATES) +
                              len(TESTING_TEMPLATES)),
            "best_practices": len(SECURITY_PRACTICES) + len(PRACTICE_BEST_PRACTICES),
        }
        total = sum(counts.values())
        print(f"\nTotal documents to seed: {total}")
        for coll, count in counts.items():
            print(f"  {coll}: {count} documents")
        print("\nRun without --dry-run to seed Firebase.")
        return

    import firebase_admin
    from firebase_admin import credentials, firestore

    if CREDENTIALS_FILE and os.path.exists(CREDENTIALS_FILE):
        cred = credentials.Certificate(CREDENTIALS_FILE)
    else:
        cred = credentials.ApplicationDefault()

    if not firebase_admin._apps:
        firebase_admin.initialize_app(cred, {"projectId": FIREBASE_PROJECT_ID})

    db = firestore.client()

    def _batch_write(collection_name, docs_dict):
        """Write documents in batches of 400 (Firestore limit is 500)."""
        items = list(docs_dict.items())
        batch_size = 400
        written = 0
        for i in range(0, len(items), batch_size):
            batch = db.batch()
            chunk = items[i:i + batch_size]
            for doc_id, doc_data in chunk:
                ref = db.collection(collection_name).document(doc_id)
                batch.set(ref, doc_data, merge=True)
            batch.commit()
            written += len(chunk)
            print(f"  {collection_name}: {written}/{len(items)} written")
        return written

    total = 0
    print("\n" + "=" * 60)
    print("  SUPREMEAI MASSIVE KNOWLEDGE SEED")
    print("=" * 60)

    # 1. System Learnings
    all_learnings = {}
    for src in [LANGUAGE_LEARNINGS, FRAMEWORK_LEARNINGS, DESIGN_PATTERN_LEARNINGS,
                SECURITY_LEARNINGS, DEVOPS_LEARNINGS, DATABASE_LEARNINGS,
                TESTING_LEARNINGS, API_LEARNINGS, PERFORMANCE_LEARNINGS,
                AI_ML_LEARNINGS, SYSTEM_DESIGN_LEARNINGS, PRACTICE_LEARNINGS]:
        all_learnings.update(src)
    print(f"\n[1/5] Seeding system_learning ({len(all_learnings)} docs)...")
    total += _batch_write("system_learning", all_learnings)

    # 2. Patterns
    all_patterns = {}
    for src in [LANGUAGE_PATTERNS, FRAMEWORK_PATTERNS, DESIGN_PATTERNS,
                DEVOPS_PATTERNS, DATABASE_PATTERNS, TESTING_PATTERNS, API_PATTERNS,
                AI_ML_PATTERNS, SYSTEM_DESIGN_PATTERNS]:
        all_patterns.update(src)
    print(f"\n[2/5] Seeding patterns ({len(all_patterns)} docs)...")
    total += _batch_write("patterns", all_patterns)

    # 3. Error Fixes
    print(f"\n[3/5] Seeding generation_errors_and_fixes ({len(ERROR_FIXES)} docs)...")
    total += _batch_write("generation_errors_and_fixes", ERROR_FIXES)

    # 4. Code Templates
    all_templates = {}
    for src in [FRAMEWORK_TEMPLATES, DEVOPS_TEMPLATES, TESTING_TEMPLATES]:
        all_templates.update(src)
    print(f"\n[4/5] Seeding code_templates ({len(all_templates)} docs)...")
    total += _batch_write("code_templates", all_templates)

    # 5. Best Practices
    all_practices = {}
    for src in [SECURITY_PRACTICES, PRACTICE_BEST_PRACTICES]:
        all_practices.update(src)
    print(f"\n[5/5] Seeding best_practices ({len(all_practices)} docs)...")
    total += _batch_write("best_practices", all_practices)

    print(f"\n{'=' * 60}")
    print(f"  TOTAL SEEDED: {total} documents")
    print(f"{'=' * 60}\n")


if __name__ == "__main__":
    seed_to_firebase()
