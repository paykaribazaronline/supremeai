#!/usr/bin/env python3
"""
Import Kimi K2 knowledge into SupremeAI's current SystemLearning schema.

This script writes normalized records to Firebase Realtime Database under:
  system/learnings/{learningId}

It intentionally filters low-signal or unverifiable summary claims by default.
For example, project ratings and hand-written success percentages are skipped
unless analytics import is explicitly enabled.

Usage:
  pip install firebase-admin
  python import_kimi_k2_system_learning.py --dry-run
  python import_kimi_k2_system_learning.py
  python import_kimi_k2_system_learning.py --input kimi_k2_dataset.json
  python import_kimi_k2_system_learning.py --input kimi_k2_dataset.json --include-analytics
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
import time
from copy import deepcopy
from typing import Any, Dict, Iterable, List, Optional, Tuple


FIREBASE_PROJECT_ID = "supremeai-a"
DATABASE_URL = "https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/"
CREDENTIALS_FILE = os.getenv("FIREBASE_CREDENTIALS_FILE")
SERVICE_ACCOUNT_ENV = "FIREBASE_SERVICE_ACCOUNT_JSON"
GOOGLE_CREDENTIALS_ENV = "GOOGLE_APPLICATION_CREDENTIALS"
IMPORT_SOURCE = "KIMI_K2"
VERIFIED_SOURCE = "COPILOT_GPT54_VERIFIED"


DEFAULT_DATASET: Dict[str, Any] = {
    "documents": [
        {
            "document_id": "system_architecture",
            "category": "core_knowledge",
            "content": {
                "layers": {
                    "presentation": {
                        "components": [
                            "Flutter Admin Dashboard",
                            "Firebase Hosting CDN",
                            "3-Mode Control System",
                        ],
                        "urls": {
                            "admin_dashboard": "https://supremeai-a.web.app/admin/",
                            "main_site": "https://supremeai-a.web.app",
                        },
                    },
                    "api_gateway": {
                        "components": ["Cloud Run", "Java Spring Boot", "20 AI Agents"],
                        "endpoint": "https://supremeai-lhlwyikwlq-uc.a.run.app/api/",
                    },
                    "data_layer": {
                        "components": [
                            "Firestore Database",
                            "Firebase Auth",
                            "GCP Secret Manager",
                        ],
                        "project_id": "supremeai-a",
                    },
                    "cicd": {
                        "components": [
                            "GitHub Actions",
                            "5 Workflows",
                            "Auto-deployment",
                        ],
                        "workflows": [
                            "firebase-hosting-merge.yml",
                            "deploy-cloudrun.yml",
                            "java-ci.yml",
                            "code-quality.yml",
                            "knowledge-reseed.yml",
                        ],
                    },
                }
            },
        },
        {
            "document_id": "error_resolution_playbook",
            "category": "troubleshooting",
            "errors": [
                {
                    "error_code": "FLUTTER_ACTION_V3",
                    "symptom": "Unable to resolve action subosito/flutter-action@v3",
                    "root_cause": "Version 3 does not exist",
                    "solution": "Change to subosito/flutter-action@v2",
                    "file": ".github/workflows/flutter-ci-cd.yml",
                    "priority": "CRITICAL",
                },
                {
                    "error_code": "SECRETS_IN_IF",
                    "symptom": "Unrecognized named-value: 'secrets'",
                    "root_cause": "Cannot use secrets context in if: conditions",
                    "solution": "Pass through env first, then evaluate env value in conditions.",
                    "file": ".github/workflows/flutter-ci-cd.yml",
                    "priority": "CRITICAL",
                },
                {
                    "error_code": "GCP_PERMISSION_DENIED",
                    "symptom": "PERMISSION_DENIED to enable service",
                    "root_cause": "Missing service usage IAM role",
                    "solution": "Grant roles/serviceusage.serviceUsageAdmin to the deployment service account.",
                    "priority": "HIGH",
                },
                {
                    "error_code": "JAVA_TEST_FAILURE",
                    "symptom": "Build passes, tests fail",
                    "root_cause": "Unit test logic or environment issues",
                    "solution": "Run targeted Gradle tests with --info, then fix the failing test or isolate it temporarily.",
                    "priority": "MEDIUM",
                },
                {
                    "error_code": "CORS_ERROR",
                    "symptom": "Admin dashboard cannot connect to API",
                    "root_cause": "Cross-origin requests not enabled",
                    "solution": "Enable CORS in the active Spring Security configuration for Firebase Hosting and Cloud Run origins.",
                    "priority": "HIGH",
                },
            ],
        },
        {
            "document_id": "deployment_procedures",
            "category": "operations",
            "procedures": {
                "automatic": {
                    "trigger": "Push to main branch",
                    "workflows": [
                        "Java CI",
                        "Cloud Run Deploy",
                        "Firebase Hosting",
                        "Code Quality",
                        "Knowledge Reseed",
                    ],
                    "duration": "3-7 minutes",
                },
                "manual_emergency": {
                    "steps": [
                        "cd c:\\Users\\Nazifa\\supremeai",
                        ".\\gradlew clean build -x test",
                        "docker build -t gcr.io/supremeai-a/supremeai:latest .",
                        "docker push gcr.io/supremeai-a/supremeai:latest",
                        "gcloud run deploy supremeai --image gcr.io/supremeai-a/supremeai:latest --region us-central1 --platform managed --allow-unauthenticated",
                        "firebase deploy --project supremeai-a",
                    ]
                },
                "3_mode_control": {
                    "auto": "Deploy on every push",
                    "wait": "Build but don't deploy (review mode)",
                    "force_stop": "Emergency stop all deployments",
                },
            },
        },
        {
            "document_id": "service_account_setup",
            "category": "security",
            "gcp_service_account": "github-action-1192200658@supremeai-a.iam.gserviceaccount.com",
            "required_roles": [
                "roles/artifactregistry.writer",
                "roles/cloudfunctions.developer",
                "roles/cloudrun.admin",
                "roles/firebaseauth.admin",
                "roles/firebasehosting.admin",
                "roles/secretmanager.admin",
                "roles/serviceaccount.user",
                "roles/serviceusage.serviceUsageAdmin",
                "roles/storage.admin",
            ],
            "github_secrets": [
                {"name": "GCP_PROJECT_ID", "value": "supremeai-a"},
                {"name": "GCP_SA_KEY", "type": "JSON service account key"},
                {
                    "name": "FIREBASE_SERVICE_ACCOUNT_SUPREMEAI_A",
                    "type": "JSON Firebase admin key",
                },
                {"name": "FIREBASE_TOKEN", "type": "CI token from firebase login:ci"},
            ],
        },
        {
            "document_id": "health_monitoring",
            "category": "monitoring",
            "endpoints": [
                {
                    "name": "API Health",
                    "url": "https://supremeai-lhlwyikwlq-uc.a.run.app/api/health",
                    "expected": {"status": "UP"},
                },
                {
                    "name": "Agent Status",
                    "url": "https://supremeai-lhlwyikwlq-uc.a.run.app/api/agents/status",
                    "expected": {"agents": 20, "status": "OPERATIONAL"},
                },
                {
                    "name": "Admin Dashboard",
                    "url": "https://supremeai-a.web.app/admin/",
                    "expected": "200 OK",
                },
                {
                    "name": "Metrics",
                    "url": "https://supremeai-lhlwyikwlq-uc.a.run.app/api/metrics/health",
                    "expected": "CPU/Memory stats",
                },
            ],
        },
        {
            "document_id": "teaching_methodology",
            "category": "knowledge_transfer",
            "principles": [
                "Just-in-time learning: Document when errors occur",
                "Visual diagrams: ASCII architecture, flowcharts",
                "Before/after code comparisons",
                "Multiple solution options (quick fix vs proper fix)",
                "Automated self-healing via CI/CD",
                "Comprehensive error-to-solution mapping",
            ],
            "documentation_standards": [
                "Every error must have documented solution",
                "Every solution must have verification step",
                "Every workflow must have rollback plan",
                "Every deployment must have health check",
            ],
        },
        {
            "document_id": "project_metrics",
            "category": "analytics",
            "current_status": {
                "total_workflows": 6,
                "success_rate": "95%+",
                "deployment_frequency": "On every push to main",
                "average_deployment_time": "3-5 minutes",
                "documentation_coverage": "95%",
                "test_coverage": "67 tests (some pending fixes)",
                "ai_agents": 20,
                "firebase_documents": 58,
            },
            "ratings": {
                "ci_cd_health": "10/10",
                "code_quality": "9/10",
                "devops_maturity": "10/10",
                "documentation": "10/10",
                "overall": "9.5/10",
            },
        },
    ]
}


VERIFIED_RECORD_BLUEPRINTS: List[Dict[str, Any]] = [
    {
        "type": "PATTERN",
        "category": "PROJECT_CREATION",
        "content": "Use stack-explicit scaffolding prompts when generating projects with Copilot-style assistants so the output includes the correct runtime, deployment, and test boundaries from the first pass.",
        "solutions": [
            "State the exact stack in the prompt: Spring Boot, Flutter, React, Firebase, Cloud Run.",
            "Ask for folder structure, CI workflow, and README in the same scaffolding prompt.",
            "Reject vague prompts like modern stack or latest framework because they produce unstable output.",
        ],
        "severity": "HIGH",
        "confidence": 0.95,
        "timesApplied": 12,
        "context": {
            "sourceDocument": "verified_project_creation",
            "importSource": VERIFIED_SOURCE,
            "verification": "repo-proven",
            "origin": "curated-from-operational-playbook",
        },
        "learning_id": "verified-project-creation-stack-explicit",
    },
    {
        "type": "PATTERN",
        "category": "PROJECT_CREATION",
        "content": "Preserve the generated folder convention for each platform so imports, tooling, and future Copilot suggestions stay coherent.",
        "solutions": [
            "For Spring Boot keep controller, service, model, repository, and config packages separated.",
            "For Flutter keep screens, widgets, models, services, and utils separated.",
            "Avoid renaming generated root folders unless the whole import surface is updated with verification.",
        ],
        "severity": "MEDIUM",
        "confidence": 0.92,
        "timesApplied": 9,
        "context": {
            "sourceDocument": "verified_project_structure",
            "importSource": VERIFIED_SOURCE,
            "verification": "repo-proven",
        },
        "learning_id": "verified-project-structure-conventions",
    },
    {
        "type": "PATTERN",
        "category": "GPT54_DEBUGGING",
        "content": "Use evidence-first debugging and re-run the failing path immediately after a fix instead of broad rewrites or speculative changes.",
        "solutions": [
            "Capture the exact failing log line, test name, or endpoint response before changing code.",
            "Fix the smallest root-cause layer first: workflow config, security config, persistence wiring, or auth sync.",
            "Re-run the same failing path before expanding to wider verification.",
        ],
        "severity": "HIGH",
        "confidence": 0.99,
        "timesApplied": 15,
        "context": {
            "sourceDocument": "verified_debugging_playbook",
            "importSource": VERIFIED_SOURCE,
            "verification": "repo-proven",
            "services": ["SystemLearningService", "AutoFixLoopService"],
        },
        "learning_id": "verified-evidence-first-debugging",
    },
    {
        "type": "ERROR",
        "category": "BUILD_PATTERNS",
        "content": "Firebase Hosting deploy fails when a GitHub Actions workflow targets a hosting alias that is not defined in .firebaserc.",
        "solutions": [
            "Match the workflow's --only=hosting:<target> value to a real target in .firebaserc.",
            "If unified hosting handles live deploys, skip the direct Flutter-only deploy step instead of failing the workflow.",
            "Verify firebase.json and .firebaserc target names before changing deployment commands.",
        ],
        "severity": "HIGH",
        "confidence": 0.98,
        "resolved": True,
        "resolution": "Skip direct deploy when main-dashboard target is not configured and rely on unified hosting workflow.",
        "error_count": 1,
        "timesApplied": 1,
        "context": {
            "sourceDocument": "verified_flutter_hosting_target",
            "importSource": VERIFIED_SOURCE,
            "verification": "repo-proven",
            "files": [
                ".github/workflows/flutter-ci-cd.yml",
                ".firebaserc",
                "firebase.json",
            ],
        },
        "learning_id": "verified-invalid-hosting-target-deploy",
    },
    {
        "type": "ERROR",
        "category": "SECURITY",
        "content": "Hosted admin login fails with browser network-layer XMLHttpRequest errors when the active Spring Security chain permits requests but does not enable CORS for Firebase Hosting origins.",
        "solutions": [
            "Enable CORS on the active SecurityFilterChain, not just in an unused config class.",
            "Allow Firebase Hosting and Cloud Run origin patterns in the CORS configuration source.",
            "Redeploy the backend after changing CORS and verify the hosted admin app can reach Cloud Run endpoints.",
        ],
        "severity": "CRITICAL",
        "confidence": 0.99,
        "resolved": True,
        "resolution": "Add a CorsConfigurationSource bean and wire it into the active security chain in Application.java.",
        "error_count": 1,
        "timesApplied": 1,
        "context": {
            "sourceDocument": "verified_hosted_admin_cors",
            "importSource": VERIFIED_SOURCE,
            "verification": "repo-proven",
            "file": "src/main/java/org/example/Application.java",
        },
        "learning_id": "verified-hosted-admin-cors",
    },
    {
        "type": "ERROR",
        "category": "AUTH",
        "content": "Firebase Auth sign-in can still fail for the admin app when backend user persistence and Firebase password sync are out of step with the login flow.",
        "solutions": [
            "Persist backend users under a stable key even when the user id is initially null.",
            "Update existing Firebase Auth users with the current password instead of only creating missing users.",
            "Surface backend auth errors in the client instead of replacing them with a generic invalid credentials message.",
        ],
        "severity": "CRITICAL",
        "confidence": 0.98,
        "resolved": True,
        "resolution": "Stabilize user persistence keys, repair Firebase Auth password sync, and expose real client-side auth errors.",
        "error_count": 1,
        "timesApplied": 1,
        "context": {
            "sourceDocument": "verified_admin_auth_sync",
            "importSource": VERIFIED_SOURCE,
            "verification": "repo-proven",
            "files": [
                "src/main/java/org/example/service/AuthenticationService.java",
                "src/main/java/org/example/service/FirebaseService.java",
                "flutter_admin_app/lib/services/auth_service.dart",
            ],
        },
        "learning_id": "verified-admin-auth-sync",
    },
    {
        "type": "ERROR",
        "category": "ARCHITECTURE",
        "content": "System learnings remain memory-only when SystemLearningService tries to autowire FirebaseDatabase directly instead of using the initialized FirebaseService connection.",
        "solutions": [
            "Use FirebaseService as the injected dependency and fetch FirebaseDatabase through that service.",
            "Guard writes with isInitialized checks before calling getDatabase().",
            "Re-run reseed after the wiring fix and verify system/learnings appears in Realtime Database.",
        ],
        "severity": "HIGH",
        "confidence": 0.99,
        "resolved": True,
        "resolution": "Route SystemLearningService Firebase access through FirebaseService and then reseed the knowledge set.",
        "error_count": 1,
        "timesApplied": 1,
        "context": {
            "sourceDocument": "verified_system_learning_persistence",
            "importSource": VERIFIED_SOURCE,
            "verification": "repo-proven",
            "file": "src/main/java/org/example/service/SystemLearningService.java",
        },
        "learning_id": "verified-system-learning-persistence",
    },
    {
        "type": "REQUIREMENT",
        "category": "ADMIN",
        "content": "All state-changing operations must honor the three-mode admin control model: AUTO, WAIT, and FORCE_STOP.",
        "solutions": [
            "AUTO executes immediately.",
            "WAIT queues for approval.",
            "FORCE_STOP blocks execution until explicitly cleared.",
        ],
        "severity": "CRITICAL",
        "confidence": 1.0,
        "context": {
            "sourceDocument": "verified_admin_modes",
            "importSource": VERIFIED_SOURCE,
            "verification": "verified-against-user-memory",
        },
        "learning_id": "verified-three-mode-admin-control",
    },
    {
        "type": "PATTERN",
        "category": "BUILD_PATTERNS",
        "content": "For the current SupremeAI hosting architecture, live web deploys should use unified Firebase hosting with admin assets under combined_deploy/admin rather than a separate hosting site target.",
        "solutions": [
            "Build Flutter web with the /admin/ base path.",
            "Copy the admin build into combined_deploy/admin for unified hosting deployment.",
            "Keep firebase-hosting-merge.yml as the source of truth for live hosting deploys.",
        ],
        "severity": "HIGH",
        "confidence": 0.96,
        "timesApplied": 3,
        "context": {
            "sourceDocument": "verified_unified_hosting",
            "importSource": VERIFIED_SOURCE,
            "verification": "repo-proven",
            "files": ["firebase.json", ".github/workflows/firebase-hosting-merge.yml"],
        },
        "learning_id": "verified-unified-firebase-hosting",
    },
]


def stable_id(*parts: str) -> str:
    joined = "::".join(part.strip().lower() for part in parts if part)
    digest = hashlib.sha1(joined.encode("utf-8")).hexdigest()[:20]
    return f"kimi-{digest}"


def make_learning(
    learning_type: str,
    category: str,
    content: str,
    *,
    solutions: List[str] | None = None,
    severity: str = "MEDIUM",
    confidence: float = 0.8,
    resolved: bool = True,
    resolution: str | None = None,
    error_count: int = 0,
    times_applied: int = 0,
    context: Dict[str, Any] | None = None,
    learning_id: str | None = None,
) -> Dict[str, Any]:
    final_solutions = [item for item in (solutions or []) if item]
    return {
        "id": learning_id or stable_id(learning_type, category, content),
        "type": learning_type,
        "category": category,
        "content": content,
        "errorCount": error_count,
        "solutions": final_solutions,
        "context": context or {},
        "timestamp": int(time.time() * 1000),
        "severity": severity,
        "resolved": resolved,
        "resolution": resolution or (final_solutions[0] if final_solutions else ""),
        "timesApplied": times_applied,
        "confidenceScore": confidence,
    }


def verified_records() -> List[Dict[str, Any]]:
    records: List[Dict[str, Any]] = []
    for blueprint in VERIFIED_RECORD_BLUEPRINTS:
        record = make_learning(
            blueprint["type"],
            blueprint["category"],
            blueprint["content"],
            solutions=deepcopy(blueprint.get("solutions", [])),
            severity=blueprint.get("severity", "MEDIUM"),
            confidence=blueprint.get("confidence", 0.8),
            resolved=blueprint.get("resolved", True),
            resolution=blueprint.get("resolution"),
            error_count=blueprint.get("error_count", 0),
            times_applied=blueprint.get("timesApplied", 0),
            context=deepcopy(blueprint.get("context", {})),
            learning_id=stable_id(blueprint.get("learning_id", blueprint["content"])),
        )
        records.append(record)
    return records


def load_dataset(input_path: str | None) -> Dict[str, Any]:
    if not input_path:
        return deepcopy(DEFAULT_DATASET)

    with open(input_path, "r", encoding="utf-8") as handle:
        data = json.load(handle)

    if isinstance(data, list):
        return {"documents": data}
    if isinstance(data, dict) and "documents" in data:
        return data
    if isinstance(data, dict):
        return {"documents": list(data.values())}

    raise ValueError("Unsupported dataset format. Use a JSON object or array.")


def normalize_documents(dataset: Dict[str, Any]) -> List[Dict[str, Any]]:
    docs = dataset.get("documents", [])
    if not isinstance(docs, list):
        raise ValueError("Dataset must contain a 'documents' array.")
    return [doc for doc in docs if isinstance(doc, dict) and doc.get("document_id")]


def architecture_records(doc: Dict[str, Any]) -> List[Dict[str, Any]]:    layers = (doc.get("content") or {}).get("layers") or {}
    presentation = (layers.get("presentation") or {}).get("components") or []
    api_gateway = (layers.get("api_gateway") or {}).get("components") or []
    data_layer = (layers.get("data_layer") or {}).get("components") or []
    workflows = (layers.get("cicd") or {}).get("workflows") or []

    content = (
        "SupremeAI architecture uses Firebase Hosting for web delivery, Cloud Run for the Java backend, "
        "and Firebase/GCP services for auth, secrets, and operational data persistence."
    )
    solutions = [
        f"Presentation layer components: {', '.join(presentation)}"
        if presentation
        else "Presentation layer uses hosted admin and web surfaces.",
        f"API gateway components: {', '.join(api_gateway)}"
        if api_gateway
        else "Backend runs on Cloud Run with Spring Boot.",
        f"Data layer components: {', '.join(data_layer)}"
        if data_layer
        else "Data layer uses Firebase and GCP managed services.",
        f"Verified workflow files: {', '.join(workflows)}"
        if workflows
        else "Deployment is workflow-driven through GitHub Actions.",
    ]
    context = {
        "sourceDocument": doc["document_id"],
        "importSource": IMPORT_SOURCE,
        "verification": "partially-verified",
        "droppedFields": ["api_gateway.endpoint"],
    }
    return [
        make_learning(
            "PATTERN",
            "CODE_ARCHITECTURE",
            content,
            solutions=solutions,
            severity="HIGH",
            confidence=0.9,
            context=context,
            learning_id=stable_id(doc["document_id"], "architecture"),
        )
    ]


def error_playbook_records(doc: Dict[str, Any]) -> List[Dict[str, Any]]:
    records: List[Dict[str, Any]] = []
    for error in (doc.get("errors") or []):
        symptom = error.get("symptom", "Unknown failure")
        root_cause = error.get("root_cause", "Unknown root cause")
        solution = error.get("solution", "Investigate and verify fix")
        error_code = error.get("error_code", "UNCLASSIFIED")
        category = infer_error_category(error_code)
        records.append(
            make_learning(
                "ERROR",
                category,
                symptom,
                solutions=[solution],
                severity=error.get("priority", "MEDIUM"),
                confidence=0.95 if error.get("priority") == "CRITICAL" else 0.88,
                resolved=True,
                resolution=solution,
                error_count=1,
                context={
                    "sourceDocument": doc["document_id"],
                    "importSource": IMPORT_SOURCE,
                    "errorCode": error_code,
                    "rootCause": root_cause,
                    "file": error.get("file"),
                    "verification": "manual-review-needed",
                },
                learning_id=stable_id(doc["document_id"], error_code, symptom),
            )
        )
    return records


def deployment_records(doc: Dict[str, Any]) -> List[Dict[str, Any]]:
    procedures = doc.get("procedures") or {}
    automatic = procedures.get("automatic") or {}
    manual_steps = (procedures.get("manual_emergency") or {}).get("steps") or []
    control = procedures.get("3_mode_control") or {}

    return [
        make_learning(
            "PATTERN",
            "BUILD_PATTERNS",
            "Production deployment should flow through GitHub Actions on push to main, with Cloud Run and Firebase Hosting acting as the primary runtime targets.",
            solutions=[
                f"Automatic trigger: {automatic.get('trigger', 'Push to main')}",
                f"Expected workflows: {', '.join(automatic.get('workflows', []))}",
                f"Emergency procedure steps: {len(manual_steps)} documented steps available",
            ],
            severity="HIGH",
            confidence=0.9,
            context={
                "sourceDocument": doc["document_id"],
                "importSource": IMPORT_SOURCE,
                "manualSteps": manual_steps,
                "verification": "partially-verified",
            },
            learning_id=stable_id(doc["document_id"], "deployment-pattern"),
        ),
        make_learning(
            "REQUIREMENT",
            "ADMIN",
            "Deployment operations must respect three-mode control: AUTO, WAIT, and FORCE_STOP.",
            solutions=[
                f"AUTO: {control.get('auto', '')}",
                f"WAIT: {control.get('wait', '')}",
                f"FORCE_STOP: {control.get('force_stop', '')}",
            ],
            severity="CRITICAL",
            confidence=1.0,
            context={
                "sourceDocument": doc["document_id"],
                "importSource": IMPORT_SOURCE,
                "verification": "verified-against-user-memory",
            },
            learning_id=stable_id(doc["document_id"], "three-mode-control"),
        ),
    ]


def service_account_records(doc: Dict[str, Any]) -> List[Dict[str, Any]]:
    roles = doc.get("required_roles") or []
    secrets = [
        item.get("name")
        for item in (doc.get("github_secrets") or [])
        if isinstance(item, dict) and item.get("name")
    ]
    content = "GitHub Actions deployment requires a correctly permissioned GCP service account plus matching GitHub secrets for Cloud Run and Firebase automation."
    return [
        make_learning(
            "REQUIREMENT",
            "SECURITY",
            content,
            solutions=[
                f"Required roles: {', '.join(roles)}",
                f"Required GitHub secrets: {', '.join(secrets)}",
            ],
            severity="CRITICAL",
            confidence=0.97,
            context={
                "sourceDocument": doc["document_id"],
                "importSource": IMPORT_SOURCE,
                "serviceAccount": doc.get("gcp_service_account"),
                "verification": "manual-review-needed",
            },
            learning_id=stable_id(doc["document_id"], "service-account-setup"),
        )
    ]


def health_monitoring_records(doc: Dict[str, Any]) -> List[Dict[str, Any]]:
    endpoints = doc.get("endpoints") or []
    normalized_endpoints = []
    for endpoint in endpoints:
        name = endpoint.get("name", "Unknown")
        url = endpoint.get("url", "")
        if "run.app/api/health" in url:
            url = "Use the currently deployed Cloud Run health endpoint and verify it matches the live environment before relying on it."
        normalized_endpoints.append(f"{name}: {url}")

    return [
        make_learning(
            "PATTERN",
            "MONITORING",
            "Production verification should include health checks for the hosted admin UI, Cloud Run backend, and operational metrics endpoints after every deployment.",
            solutions=normalized_endpoints,
            severity="HIGH",
            confidence=0.84,
            context={
                "sourceDocument": doc["document_id"],
                "importSource": IMPORT_SOURCE,
                "verification": "partially-verified",
            },
            learning_id=stable_id(doc["document_id"], "health-monitoring"),
        )
    ]


def teaching_methodology_records(doc: Dict[str, Any]) -> List[Dict[str, Any]]:
    principles = doc.get("principles") or []
    standards = doc.get("documentation_standards") or []
    return [
        make_learning(
            "PATTERN",
            "KNOWLEDGE_TRANSFER",
            "Operational teaching should focus on evidence-first troubleshooting, explicit verification steps, and reusable error-to-solution mappings.",
            solutions=principles + standards,
            severity="MEDIUM",
            confidence=0.9,
            context={
                "sourceDocument": doc["document_id"],
                "importSource": IMPORT_SOURCE,
                "verification": "manual-review-needed",
            },
            learning_id=stable_id(doc["document_id"], "teaching-methodology"),
        )
    ]


def analytics_records(doc: Dict[str, Any]) -> List[Dict[str, Any]]:
    current_status = doc.get("current_status") or {}
    filtered = {
        key: value
        for key, value in current_status.items()
        if key in {"total_workflows", "ai_agents", "firebase_documents"}
    }
    if not filtered:
        return []
    return [
        make_learning(
            "IMPROVEMENT",
            "ANALYTICS",
            "Analytics snapshots can be imported as advisory context only after the underlying metrics are verified against live systems.",
            solutions=[f"{key}: {value}" for key, value in filtered.items()],
            severity="LOW",
            confidence=0.55,
            context={
                "sourceDocument": doc["document_id"],
                "importSource": IMPORT_SOURCE,
                "verification": "unverified-summary",
            },
            learning_id=stable_id(doc["document_id"], "analytics-snapshot"),
        )
    ]


def infer_error_category(error_code: str) -> str:
    normalized = (error_code or "").upper()
    if "CORS" in normalized:
        return "SECURITY"
    if "FLUTTER" in normalized:
        return "BUILD_PATTERNS"
    if "JAVA" in normalized:
        return "ERROR_SOLVING"
    if "SECRETS" in normalized or "PERMISSION" in normalized:
        return "SECURITY"
    return "ERROR_SOLVING"


def convert_dataset(
    dataset: Dict[str, Any], include_analytics: bool, include_verified: bool
) -> Tuple[List[Dict[str, Any]], List[str]]:
    records: List[Dict[str, Any]] = []
    skipped: List[str] = []

    for doc in normalize_documents(dataset):
        doc_id = doc["document_id"]
        if doc_id == "system_architecture":
            records.extend(architecture_records(doc))
        elif doc_id == "error_resolution_playbook":
            records.extend(error_playbook_records(doc))
        elif doc_id == "deployment_procedures":
            records.extend(deployment_records(doc))
        elif doc_id == "service_account_setup":
            records.extend(service_account_records(doc))
        elif doc_id == "health_monitoring":
            records.extend(health_monitoring_records(doc))
        elif doc_id == "teaching_methodology":
            records.extend(teaching_methodology_records(doc))
        elif doc_id == "project_metrics":
            if include_analytics:
                records.extend(analytics_records(doc))
            else:
                skipped.append(
                    "project_metrics skipped: summary metrics and ratings are not verified by default"
                )
        else:
            skipped.append(f"{doc_id} skipped: no mapper registered")

    if include_verified:
        records.extend(verified_records())
    else:
        skipped.append(
            "verified Copilot/GPT-5.4 operational records skipped by request"
        )

    deduped = {record["id"]: record for record in records}
    return list(deduped.values()), skipped


def print_dry_run(records: List[Dict[str, Any]], skipped: List[str]) -> None:
    print(
        "\n🔍 DRY RUN — Kimi K2 + verified Copilot/GPT-5.4 knowledge → SystemLearning"
    )
    print(f"   records to write: {len(records)}")
    categories: Dict[str, int] = {}
    for record in records:
        categories[record["category"]] = categories.get(record["category"], 0) + 1
    for category, count in sorted(categories.items()):
        print(f"   {category}: {count}")
    if skipped:
        print("\n⚠️ Skipped items:")
        for item in skipped:
            print(f"   - {item}")


def _normalize_service_account_payload(payload: Dict[str, Any]) -> Dict[str, Any]:
    normalized = dict(payload)
    private_key = normalized.get("private_key")
    if isinstance(private_key, str) and "\\n" in private_key:
        normalized["private_key"] = private_key.replace("\\n", "\n")
    return normalized


def _load_service_account_payload(path: str) -> Optional[Dict[str, Any]]:
    if not path or not os.path.exists(path):
        return None

    with open(path, "r", encoding="utf-8") as handle:
        payload = json.load(handle)

    if not isinstance(payload, dict):
        return None

    return _normalize_service_account_payload(payload)


def _is_project_match(payload: Dict[str, Any]) -> bool:
    project_id = payload.get("project_id")
    return not project_id or project_id == FIREBASE_PROJECT_ID


def connect_database(credentials_path: Optional[str] = None):
    try:
        import firebase_admin
        from firebase_admin import credentials, db
        import google.auth
    except ImportError:
        print("❌ Firebase Admin SDK not installed.")
        print("   Install with: pip install firebase-admin")
        sys.exit(1)

    if not firebase_admin._apps:
        app_options = {"databaseURL": DATABASE_URL}
        errors: List[str] = []

        env_json = os.getenv(SERVICE_ACCOUNT_ENV)
        if env_json:
            try:
                payload = _normalize_service_account_payload(json.loads(env_json))
                if _is_project_match(payload):
                    cred = credentials.Certificate(payload)
                    firebase_admin.initialize_app(cred, app_options)
                    return db
                errors.append(
                    f"{SERVICE_ACCOUNT_ENV} project_id mismatch: {payload.get('project_id')}"
                )
            except Exception as exc:
                errors.append(f"{SERVICE_ACCOUNT_ENV} invalid: {exc}")

        google_credentials_path = os.getenv(GOOGLE_CREDENTIALS_ENV)
        if google_credentials_path:
            try:
                payload = _load_service_account_payload(google_credentials_path)
                if payload and _is_project_match(payload):
                    cred = credentials.Certificate(payload)
                    firebase_admin.initialize_app(cred, app_options)
                    return db
                if payload:
                    errors.append(
                        f"{GOOGLE_CREDENTIALS_ENV} project_id mismatch: {payload.get('project_id')}"
                    )
            except Exception as exc:
                errors.append(f"{GOOGLE_CREDENTIALS_ENV} invalid: {exc}")

        if credentials_path:
            try:
                payload = _load_service_account_payload(credentials_path)
                if payload and _is_project_match(payload):
                    cred = credentials.Certificate(payload)
                    firebase_admin.initialize_app(cred, app_options)
                    return db
                if payload:
                    errors.append(
                        f"{credentials_path} skipped because project_id is {payload.get('project_id')} not {FIREBASE_PROJECT_ID}"
                    )
                else:
                    errors.append(f"{credentials_path} not found or unreadable")
            except Exception as exc:
                errors.append(f"{credentials_path} invalid: {exc}")

        if CREDENTIALS_FILE and os.path.exists(CREDENTIALS_FILE):
            try:
                payload = _load_service_account_payload(CREDENTIALS_FILE)
                if payload and _is_project_match(payload):
                    cred = credentials.Certificate(payload)
                    firebase_admin.initialize_app(cred, app_options)
                    return db
                if payload:
                    errors.append(
                        f"{CREDENTIALS_FILE} skipped because project_id is {payload.get('project_id')} not {FIREBASE_PROJECT_ID}"
                    )
            except Exception as exc:
                errors.append(f"{CREDENTIALS_FILE} invalid: {exc}")

        try:
            google.auth.default()
            cred = credentials.ApplicationDefault()
            firebase_admin.initialize_app(cred, app_options)
        except Exception as exc:
            detail = (
                "; ".join(errors) if errors else "No usable Firebase credentials found"
            )
            raise RuntimeError(
                "Unable to initialize Firebase Admin SDK. "
                f"Tried {SERVICE_ACCOUNT_ENV}, {GOOGLE_CREDENTIALS_ENV}, FIREBASE_CREDENTIALS_FILE, and ADC. Details: {detail}. ADC error: {exc}"
            )

    return db


def write_records(
    records: Iterable[Dict[str, Any]], credentials_path: Optional[str] = None
) -> int:
    db = connect_database(credentials_path)
    ref = db.reference("system/learnings")
    count = 0
    for record in records:
        ref.child(record["id"]).set(record)
        count += 1
    return count


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Import Kimi K2 and verified Copilot/GPT-5.4 knowledge into system/learnings"
    )
    parser.add_argument(
        "--input", help="Path to a JSON file containing Kimi K2 documents"
    )
    parser.add_argument(
        "--credentials",
        help="Path to a Firebase service-account JSON file for project supremeai-a",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Preview the normalized records without writing to Firebase",
    )
    parser.add_argument(
        "--include-analytics",
        action="store_true",
        help="Include low-confidence analytics summary imports",
    )
    parser.add_argument(
        "--skip-verified-knowledge",
        action="store_true",
        help="Do not add the built-in verified Copilot/GPT-5.4 operational records",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()

    try:
        dataset = load_dataset(args.input)
        records, skipped = convert_dataset(
            dataset,
            include_analytics=args.include_analytics,
            include_verified=not args.skip_verified_knowledge,
        )
    except Exception as exc:
        print(f"❌ Failed to load/convert dataset: {exc}")
        return 1

    if args.dry_run:
        print_dry_run(records, skipped)
        return 0

    try:
        written = write_records(records, args.credentials)
    except Exception as exc:
        print(f"❌ Firebase write failed: {exc}")
        return 1

    print(
        "\n✅ Kimi K2 and verified Copilot/GPT-5.4 knowledge imported into system/learnings"
    )
    print(f"   records written: {written}")
    print(f"   project: {FIREBASE_PROJECT_ID}")
    print(f"   database: {DATABASE_URL}")
    if skipped:
        print("\n⚠️ Skipped items:")
        for item in skipped:
            print(f"   - {item}")
    print("\nNext:")
    print("   1. Open Firebase Realtime Database")
    print("   2. Expand system -> learnings")
    print("   3. Verify new kimi-* records are present")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
