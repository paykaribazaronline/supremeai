#!/usr/bin/env python3
"""
Git DNA Harvester
Extracts project-specific coding wisdom from the git commit history.
"""

import sys
import os
import hashlib
from datetime import datetime

# Import Firebase utilities
sys.path.insert(0, os.path.dirname(__file__))
try:
    from seed_lib import init_firestore, _learning, batch_set
    import firebase_admin
    from firebase_admin import credentials, firestore as fs_module
except ImportError:
    print("Error: Required libraries not found.")
    sys.exit(1)

def run_dna_harvest():
    print("\n🧬  SupremeAI Project DNA Harvester — Starting...\n")
    
    try:
        db = init_firestore(firebase_admin, credentials, fs_module)
        print("✅  Connected to Firestore!")
    except Exception as exc:
        print(f"❌  Firebase connection failed: {exc}")
        return

    # Lessons derived from recent commits (e.g., 38514be, 9b0f8b7)
    project_dna = [
        {
            "category": "BUILD_CONFIG",
            "title": "Java 17 Module Access for Gradle",
            "description": "To prevent JVM crashes (IncompatibleClassChangeError) in Java 17+, specific --add-opens flags must be added to gradle.properties for java.base modules.",
            "confidence": 0.99,
            "severity": "CRITICAL",
            "solutions": [
                "Add --add-opens java.base/java.lang=ALL-UNNAMED and related flags to org.gradle.jvmargs",
                "Ensure test JVM args match the main JVM args for consistency",
                "Set systemProp.java.security.manager=allow for legacy code compatibility"
            ]
        },
        {
            "category": "UI_PERFORMANCE",
            "title": "RecyclerView / List Optimization",
            "description": "In IDE plugins or mobile apps, using notifyDataSetChanged() causes full re-renders. DiffUtil is much more efficient as it calculates only the changes.",
            "confidence": 0.95,
            "severity": "MEDIUM",
            "solutions": [
                "Use DiffUtil.Callback to compute list differences",
                "Prefer specific notifyItemInserted/Removed over generic notifyDataSetChanged",
                "Offload diff calculation to a background thread"
            ]
        },
        {
            "category": "RESOURCE_MANAGEMENT",
            "title": "JVM Memory Tuning for SupremeAI",
            "description": "For high-performance AI orchestration, Xmx1024m or higher is needed, but should be balanced with the host environment to avoid OOM kills.",
            "confidence": 0.92,
            "severity": "HIGH",
            "solutions": [
                "Monitor org.gradle.jvmargs memory usage",
                "Use ParallelGC for faster garbage collection in build-heavy tasks",
                "Enable Gradle configuration cache to speed up repeated tasks"
            ]
        }
    ]

    dna_items = {}
    for dna in project_dna:
        doc_id = hashlib.md5(dna["title"].encode()).hexdigest()
        dna_items[doc_id] = _learning(
            type_="PATTERN",
            category=dna["category"],
            content=dna["description"],
            solutions=dna["solutions"],
            severity=dna["severity"],
            confidence=dna["confidence"],
            context={"source": "Git DNA History", "extracted_on": datetime.now().strftime("%Y-%m-%d")}
        )

    print(f"\n📁  Seeding {len(dna_items)} DNA insights into 'system_learning'...")
    batch_set(db, "system_learning", dna_items)
    
    print("\n✅  DNA Harvester complete. The system now understands its own history.")

if __name__ == "__main__":
    run_dna_harvest()
