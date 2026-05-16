# 🚀 SupremeAI Ultimate AI Controller Plan
**Version:** 3.0 (Zero-Hardcode Evolution)
**Status:** Approved for Implementation
**Focus:** Absolute control over AI Intelligence, Roles, and Real-time Telemetry.

---

## 1. Dashboard Structure (The 8 Pillars)

| # | Tab Name | Purpose | Key Features |
|---| :--- | :--- | :--- |
| 1 | **Intelligence Registry** | AI Discovery | Add/Delete local (Ollama) & Cloud (Gemini, GPT, Claude, etc.) models via API or Endpoint. |
| 2 | **Scenario Orchestration** | Role Delegation | Assign any number of roles (Chat, Execution, Voting, Reasoning, etc.) to any model. One model can handle 100% of tasks if assigned. |
| 3 | **Live Telemetry** | System Pulse | Real-time RAM/GPU usage, Tokens/sec, and Latency matrix (3D Visualizer integrated). |
| 4 | **Consensus Map** | Decision Logic | Visual display of Multi-agent voting, disagreement resolution, and final decision path. |
| 5 | **Quota & Traffic** | Resource Control | API usage tracking with auto-failover to alternative models when limits are reached. |
| 6 | **Knowledge (RAG)** | Brain Injection | Interface to upload documents or data streams to specific models or global knowledge store. |
| 7 | **Self-Healing Logs** | System Repair | Live diagnostic stream showing errors and the system's autonomous self-correction attempts. |
| 8 | **Learning Hub** | Cognitive Control | Manage subjects and topics the AI agents are actively learning. Add/Remove learning targets. |

---

## 2. Technical Implementation Requirements

### A. Backend (Spring Boot 3 + Firestore)
- **Unified Provider Model**: `APIProvider` must support a dynamic array of capabilities.
- **WebSocket Metrics**: Finalize `MetricsBroadcasterService` to stream hardware and performance data.
- **Production Security**: Fix CORS and CSRF configurations for `supremeai-a.web.app` and `supremeai-a.firebaseapp.com`.
- **Dynamic Role Patching**: Ensure PATCH `/api/admin/providers/{id}/capability` supports adding/removing multiple roles.

### B. Frontend (React + Vite + Ant Design)
- **Tabbed Interface Cleanup**: Remove all irrelevant legacy tabs (Overview, Projects, etc.).
- **Dynamic Role UI**: Replace simple switches with a multi-select capability list in `APIManagement`.
- **Telemetry Integration**: Connect `ThreeDashboard` (3D Graph) to live metrics from the backend.
- **Production URL Sync**: Ensure `VITE_API_URL` correctly points to the GCloud Cloud Run instance.

---

## 3. Implementation Workflow
1. **Core UI Cleanup**: Strip `AdminDashboardUnified.tsx` of all non-essential tabs.
2. **Backend Hardening**: Fix the "Invalid CORS request" on production by updating `SecurityConfig`.
3. **Role Logic wiring**: Connect the Scenario Orchestration checkboxes to the `capability` patch endpoint.
4. **Telemetry Stream**: Wire the 3D visualizer to actual backend state via WebSockets.
5. **Final Production Push**: Deploy to Cloud Run and Firebase to verify zero-error connectivity.
