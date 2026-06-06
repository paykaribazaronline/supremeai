#!/usr/bin/env python3
"""
Part 6 — API-Driven Initialization
Seeds knowledge about the shift from script-based seeding to API-driven foundation seeding.
"""

import sys
import os

sys.path.insert(0, os.path.dirname(__file__))

from seed_lib import _learning, run_part

SYSTEM_LEARNINGS = {
    "api_initialization_architecture": _learning(
        type_="PATTERN",
        category="SYSTEM_INITIALIZATION",
        content=(
            "Transitioning from Python-based knowledge seeding to Java API-driven 'Foundation Seeding' "
            "enables the system to self-initialize on any cloud environment without shell access. "
            "This centralizes the 'Ground Truth' inside the SupremeAI binary."
        ),
        solutions=[
            "Expose POST /api/knowledge/init for one-click environment setup",
            "Use the seedBulk service method to populate collections during CI/CD bootstrap",
            "Ensure the foundation seed is idempotent (uses fixed document IDs)",
        ],
        severity="MEDIUM",
        confidence=0.99,
        times_applied=1,
        context={"service": "KnowledgeController"},
    )
}

if __name__ == "__main__":
    run_part(
        part_name="Part 6 — API Initialization",
        collections={"system_learning": SYSTEM_LEARNINGS},
    )
