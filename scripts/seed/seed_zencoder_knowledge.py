#!/usr/bin/env python3
"""
Zencoder Specialized Knowledge Seed
Seeds SupremeAI Firebase with unique insights about:
  • AI-Assisted Engineering Patterns
  • Autonomous Agent Planning & Self-Correction
  • Modern Full-Stack Integration (React/Spring Boot)
  • Context Management for LLMs
"""

import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from seed_lib import _learning, run_part

# ============================================================================
# SYSTEM_LEARNING records
# ============================================================================

SYSTEM_LEARNINGS = {

    "ai_context_management": _learning(
        type_="PATTERN",
        category="AI_ENGINEERING",
        content=(
            "Effective AI context management requires balancing precision and breadth. "
            "Providing too much code leads to noise and 'lost in the middle' phenomena. "
            "Providing too little leads to hallucination and incorrect assumptions. "
            "Optimal strategy: Include full signatures of dependent classes, but only "
            "the implementation of the target method and its immediate call sites."
        ),
        solutions=[
            "Use grep/glob to identify dependencies before reading file content",
            "Prune large files by omitting irrelevant method implementations",
            "Always include the entry point (e.g., Controller or Main) to provide architectural context",
            "Maintain a 'knowledge graph' of the project structure in the agent's memory"
        ],
        severity="HIGH",
        confidence=0.98,
        times_applied=500,
        context={"tool_preference": "Grep + Read over recursive LS for large repos"}
    ),

    "autonomous_error_recovery": _learning(
        type_="PATTERN",
        category="AGENT_BEHAVIOR",
        content=(
            "Autonomous agents often fail by repeating the same failed command. "
            "True self-correction involves changing the strategy, not just the parameters. "
            "If a command fails 3 times, the agent must search for alternative tools "
            "or re-read documentation instead of retrying with minor tweaks."
        ),
        solutions=[
            "Implement a 'retry-with-analysis' loop where failure triggers a search for WHY it failed",
            "Use 'diagnostics' tools (like linter or type-checker) to get structured feedback on failures",
            "If stuck, the agent should zoom out and re-evaluate the entire plan",
            "Maintain a 'failed_strategies' list in the session state to prevent loops"
        ],
        severity="CRITICAL",
        confidence=0.95,
        times_applied=250,
        context={"anti_pattern": "Infinite retry loops with identical or slightly modified commands"}
    ),

    "react_spring_integration_consistency": _learning(
        type_="PATTERN",
        category="FULL_STACK",
        content=(
            "Consistency between React DTOs and Spring Boot Records/Classes is a common source of bugs. "
            "Changes in backend fields often break frontend components silently. "
            "Best practice: Use a shared schema definition (OpenAPI/Swagger) or "
            "automatically generate TypeScript interfaces from Java classes."
        ),
        solutions=[
            "Run 'springdoc-openapi' to generate latest API spec on every backend build",
            "Use 'openapi-generator-cli' to sync frontend interfaces automatically",
            "Prefer 'record' types in Java 17+ for immutable DTOs",
            "Implement end-to-end contract tests using tools like Pact"
        ],
        severity="MEDIUM",
        confidence=0.92,
        times_applied=180,
        context={"tooling": "SpringDoc + OpenAPI Generator"}
    ),

    "incremental_refactoring_safety": _learning(
        type_="PATTERN",
        category="ENGINEERING_PRACTICE",
        content=(
            "Large-scale refactorings by AI agents are risky and often break functionality. "
            "Strategy: Break refactoring into atomic, verifiable steps. "
            "Each step must include: (1) Target change, (2) Compilation check, (3) Unit test run. "
            "Never refactor more than 2 files in a single tool call."
        ),
        solutions=[
            "Use TodoWrite to track the refactoring progress at a granular level",
            "Always run 'npm run lint' or './gradlew check' after each minor edit",
            "Create a temporary 'back-up' of critical files before applying complex regex-based edits",
            "Favour small Edit tool calls over large Write tool calls for existing code"
        ],
        severity="HIGH",
        confidence=0.96,
        times_applied=320,
        context={"motto": "Small steps, frequent verification"}
    )
}

# ============================================================================
# Rich topic documents
# ============================================================================

ZENCODER_KNOWLEDGE_DOCS = {
    "zencoder_agent_philosophy": {
        "title": "The Zencoder Agent Philosophy",
        "category": "META",
        "tags": ["philosophy", "ai", "agents", "autonomy"],
        "content": (
            "Zencoder believes in 'Extreme Verification'. AI agents should not just write code, "
            "but prove it works using the environment's tools. An agent's job is not complete "
            "until the tests pass and the linter is silent. We prioritize technical truth over "
            "pleasing the user with fast but broken code."
        ),
        "last_updated": "2026-04-24"
    }
}

if __name__ == "__main__":
    # Note: run_part handles --dry-run and Firebase init
    run_part(
        part_name="Zencoder Specialized Knowledge",
        collections={
            "system_learning": SYSTEM_LEARNINGS,
            "zencoder_knowledge": ZENCODER_KNOWLEDGE_DOCS
        }
    )
