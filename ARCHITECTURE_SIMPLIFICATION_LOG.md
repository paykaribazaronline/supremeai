# Architecture Simplification Log

**Date:** 2026-03-31  
**Goal:** Reduce system complexity, remove bottlenecks, and shift to a Guest-to-Persistent access model.

## 1. Visualization Cleanup (Phase 6)

- **Changes:** Deleted `VisualizationController.java` and `TimelineVisualizationController.java`.
- **Reason:** Real-time 3D rendering (30 FPS) was causing excessive server and client load.
- **Replacement:** Use `Minimalist Admin Dashboard` for essential system health.

## 2. Publishing Pipeline Simplification (Phase 7)

- **Changes:** Deprecated/Deleted `PlayStorePublisherAgent.java` and `AppStorePublisherAgent.java`.
- **Reason:** Direct publishing from application code is fragile and requires constant updates to match platform APIs.
- **Replacement:** Offloaded publishing to GitHub Actions CI/CD pipelines.

## 3. Quota Management Consolidation (Phases 1-5)

- **Changes:** Deleted `QuotaTracker.java`, `GuestQuotaService.java`, and `SimulatorQuotaService.java`.
- **Replacement:** Created `UnifiedQuotaService.java` to handle all quota types (User, Guest, Simulator) in one centralized location.

## 4. Data Collection Consolidation (Phases 1-5)

- **Changes:** Deleted `HybridDataCollector.java`, `DataCollectorService.java`, and `DataRetentionService.java`.
- **Replacement:** Created `UnifiedDataService.java` for centralized data collection and retention.

## 5. Security & Access Model Shift

- **Changes:** Deleted `APIKeyManager.java` (Complex API Key management).
- **Replacement:** Implemented `RateLimiterService.java` based on IP-based rate limiting.
- **Future-Proofing:** Moved to "Guest-to-Persistent" Auth model:
  - **Guest Mode:** No login required for daily usage.
  - **Login Mode:** Merges session data to permanent storage.
- **Reason:** Removed friction for new users/AI learners. Login is now optional, not mandatory.

## 6. Self-Improvement Evolution (Phase 10)

- **Changes:** Shifted from "Autonomous Auto-Evolving Algorithms" to "Rule-based Control".
- **Reason:** Autonomous evolution was creating unpredictable system behaviors and complex bugs.
- **New Feature:** Added `AdminRuleController.java` allowing Admin manual oversight of system rules via the dashboard.

---
**Guideline for future developers/AI:**

- Keep the system **Lean**. If a service handles a simple task, do not create a separate agent.
- Prioritize **Human-in-the-loop** for self-improvement rather than black-box AI evolution.
- Favor **CI/CD pipelines** over hardcoded publishing logic.
- Stick to the **Guest-to-Persistent** auth model to minimize user friction.
