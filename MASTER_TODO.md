# SupremeAI - Master Todo List

## Current Status
- **Backend**: Spring Boot 3 on Cloud Run (us-central1)
- **Database**: Cloud Firestore
- **Frontend**: React Dashboard on Firebase Hosting
- **CI/CD**: Google Cloud Build with Artifact Registry

---

## 🚀 High Priority (Current)

### Cost Optimization & Maintenance (Permanent)
- [x] **Cloud Run Min-Instances** - Set to 0 for all services (`supremeai-backend`, `reverse-engineering`, `simulator-runtime`) to eliminate idle costs.
- [x] **Artifact Registry Cleanup** - Automated cleanup of untagged images integrated into `deploy.sh`.
- [ ] **Billing Alerts** - Configure GCP billing alerts for $10, $50, and $100 thresholds.
- [ ] **Resource Rightsizing** - Monitor CPU/Memory usage and adjust Cloud Run limits (currently 2Gi/2CPU).

### System Stability & Integration
- [ ] **WebSocket Sync** - Ensure frontend listeners match backend topics (`/topic/pipeline/progress`).
- [ ] **AI Model Registry** - Complete decoupling of model names from code; move all to Firestore metadata.
- [ ] **Self-Learning Loop** - Validate active scraping integration with `SelfImprovementService`.

---

## 🛠 Feature Development

### Admin Dashboard Enhancements
- [ ] **Security Panel** - Implement real-time threat visualization.
- [ ] **Provider Management** - UI to enable/disable AI providers dynamically.
- [ ] **Cost Monitoring** - Integration with GCP billing API to show costs in the dashboard.

### AI Capabilities
- [ ] **Voicebox Integration** - Standalone service for STT/TTS on Cloud Run.
- [ ] **Multimodal Expansion** - Better support for image and document analysis across all providers.

---

## 🧹 Technical Debt & Cleanup
- [x] Removed outdated `TODO_LIST.md` and `project_todo_list.md`.
- [ ] Standardize logging across all services (Structured JSON Logging).
- [ ] Implement global error handling in the React dashboard.

---
*Last Updated: 2026-05-16*
