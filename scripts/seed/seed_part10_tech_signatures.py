#!/usr/bin/env python3
"""
Part 10 — Technology Signatures
Seeds the 'system_learning' collection with patterns used by ProjectDNAHarvester.
This replaces hardcoded signatures in SignatureRegistry.java to enable 
architecture awareness without redeploying code.
"""

import sys
import os

# Ensure the script can find seed_lib
sys.path.insert(0, os.path.dirname(__file__))

try:
    from seed_lib import _learning, run_part
except ImportError:
    print("Error: seed_lib.py not found in the same directory. Ensure this script is in scripts/seed/")
    sys.exit(1)

# Signature Data: (subCategory, label, regex)
SIGNATURE_DATA = [
    ("JS_DEP", "React", r"\"react\""),
    ("JS_DEP", "Vue", r"\"vue\""),
    ("JS_DEP", "Angular", r"\"@angular/core\""),
    ("DOCKER", "PostgreSQL (Docker)", r"postgres"),
    ("DOCKER", "Redis (Docker)", r"redis"),
    ("DOCKER", "MongoDB (Docker)", r"mongo"),
    ("PYTHON", "Django", r"django"),
    ("PYTHON", "Flask", r"flask"),
    ("PYTHON", "FastAPI", r"fastapi"),
]

SYSTEM_LEARNINGS = {}

for sub_cat, label, regex in SIGNATURE_DATA:
    # Create a normalized document ID
    safe_label = label.lower().replace(" ", "_").replace("(", "").replace(")", "").replace(".", "_")
    doc_id = f"tech_sig_{safe_label}"
    
    SYSTEM_LEARNINGS[doc_id] = _learning(
        type_="PATTERN",
        category="TECH_SIGNATURE",
        content=f"Technology detection signature for {label}.",
        solutions=[],
        severity="INFO",
        confidence=1.0,
        context={
            "label": label,
            "regex": regex,
            "subCategory": sub_cat
        }
    )

if __name__ == "__main__":
    run_part(
        part_name="Part 10 — Technology Signatures",
        collections={"system_learning": SYSTEM_LEARNINGS},
    )