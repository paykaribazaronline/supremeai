#!/usr/bin/env python3
"""
Part 7 — Knowledge Verification
Seeds knowledge about the importance and implementation of verifying foundational knowledge.
"""

import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from seed_lib import _learning, run_part

SYSTEM_LEARNINGS = {
    "knowledge_verification_importance": _learning(
        type_="IMPROVEMENT",
        category="KNOWLEDGE_MANAGEMENT",
        content=(
            "Implementing a KnowledgeVerificationService is critical to ensure the integrity "
            "of the system's foundational knowledge. It validates that core patterns and "
            "resilience rules are correctly seeded and meet required confidence thresholds. "
            "This prevents silent failures due to missing or low-quality foundational data."
        ),
        solutions=[
            "Expose a /api/knowledge/verify-foundation endpoint to check core knowledge status",
            "Run verification during system startup and after any major knowledge update",
            "Alert administrators if critical foundation entries are missing or have low confidence"
        ],
        severity="CRITICAL",
        confidence=0.99,
        times_applied=1,
        context={"service": "KnowledgeVerificationService"}
    )
}

if __name__ == "__main__":
    run_part(
        part_name="Part 7 — Knowledge Verification",
        collections={"system_learning": SYSTEM_LEARNINGS}
    )