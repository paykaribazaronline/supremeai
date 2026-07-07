# SupremeAI 2.0 - Architecture Overview

## Introduction
SupremeAI 2.0 is a multi-cloud AI orchestration platform built on FastAPI with a React/Vite frontend. It targets zero-cost operation through aggressive free-tier utilization across multiple AI providers.

## Core Philosophy
- **Database-Driven Logic:** Hardcoded configurations are deprecated. Settings and rules are managed dynamically through Firestore.
- **Zero Operating Cost:** Through dynamic API routing, CostGuard, and Sandbox Auto-Destroy.
- **Self-Learning and Self-Healing Ecosystem:** Errors trigger self-correcting mechanisms under human oversight.

## Phase 1 & 2: Security & Configuration Management
- **Security Lockdown:** Implemented strict credential loading to prevent hardcoded secrets.
- **Dynamic Config Proxy:** Replaced hardcoded variables with a Firestore-backed `DynamicConfigProxy`.

## Phase 3: Cost Guard, Self Healer, and Control Tower
- **CostGuard:** Ensures zero-cost operations by acting as a pre-flight checker. It blocks transactions for tenants exceeding their `monthly_limit`. 
- **SelfHealerService:** Catches backend failures (like 429 Rate Limits or internal errors) and automatically generates `pending_review` fixes.
- **Cloud Sandbox Orchestrator:** Implements an `auto_destroy_worker` using a TTL (Time-To-Live) mechanism to terminate idle sandboxes, guaranteeing maximum resource utilization.
- **Architectural Control Tower:** A React-based HITL (Human-in-the-loop) dashboard in `apps/studio-client/` at `/architect-tower`. It allows administrators to review, approve, or reject SelfHealer's generated fixes securely.
- **Audit Trails:** Administrative approvals are logged with `reviewed_by` and `applied_at` timestamps for strict traceability.

## API Architecture
- Backend operations are channeled through `llm_gateway.py`.
- Security constraints enforce JWT-based authentication for administrative and sensitive routes (`admin.py`).
