# SupremeAI 2.0 Agent Rules

- **Full Path Terminal Commands (Bangla/English):** Whenever suggesting or explaining terminal commands (e.g. `npx`, `pnpm`, `npm`, `python`, `git`), always provide the commands with the exact target directory location or full absolute path (e.g. specifying `F:\supremeai backup\apps\studio-client` or using `cd` guides with full paths) so that the user knows exactly where to run the command.

- **Render Deployment Failure Logging (Bangla/English):** যেকোনো সময় Render ডিপ্লয় ফেইল করলে (Render Deploy Failure), প্রতিটি ফেইল্ড সার্ভিসের সম্পূর্ণ র (raw) লগ সংগ্রহ করে `render_deployment_failure_logs.md` নামে একটি ডেসক্রিপ্টিভ মার্কডাউন ফাইল (Artifact) তৈরি করতে হবে।

- **Timer & User Interaction Control Rule:**
  - If the user explicitly says `"stop"` or expresses frustration, immediately kill all background timers/tasks using `manage_task(Action='kill')` and DO NOT set any new timers unless explicitly requested.
  - Keep responses concise, objective, and focused on empirical log evidence without defensive explanations.

- **Ultra-Concise Responses Rule (CRITICAL):**
  - Always keep answers as short, direct, and minimal as possible.
  - Do NOT write multi-paragraph explanations or background details unless the user explicitly asks "explain" or "why".

- **Single Definite Root-Cause Answer Rule (STRICT & MANDATORY):**
  - **No Multi-Option Speculation:** Never provide 2, 3, or multiple speculative reasons for an error or failure.
  - **Single Verified Truth:** Always perform empirical inspection, trace code, and identify the single exact root cause before answering. Present exactly ONE definitive, verified answer without guessing or hedging.

- **Strict Git Push Rule (NON-NEGOTIABLE):** The AI agent MUST NEVER run `git push` under any circumstances unless the user explicitly sends a prompt that contains the exact word `"push"`. Generic user approvals (e.g. "ok", "do that", "fix it", "yes") DO NOT grant push permission. Without the literal word `"push"` present in the user's message, the AI will NEVER push to GitHub.

- **No Background Timers Rule (STRICT):** The AI agent MUST NEVER schedule background timers or interval scheduler tasks after `git push` or during any execution unless the user explicitly requests a timer.

- **Execution Time-Tracking & Hang Prevention Rule (CRITICAL):**
  - **Estimate Expected Runtime:** Before launching any test, build, or script, estimate its expected duration (unit tests should run in < 15 seconds; builds < 30 seconds).
  - **Strict Time-Tracking:** Keep track of elapsed time vs expected duration. If a test or command takes significantly longer than expected (e.g., > 2x estimated time or > 30 seconds), inspect log output immediately or terminate it to diagnose unmocked network calls.
  - **No Silent Network Blockers:** Ensure all external LLM routes (`core.llm_router.LLMRouter`) and database connections (`core.tenant_db.TenantAwareFirestore`) are fully mocked before executing test suites to prevent network connection timeouts.

- **Commit All Uncommitted Files & Impact Report:** When performing a commit, ALWAYS inspect all uncommitted files (`git status` / `git diff`), stage all uncommitted files, and provide a short, concise summary report explaining how the uncommitted files make the system better and what specific improvements/benefits they bring.

- **Homologous & Scope-Wide Verification Rule (MANDATORY):**
  - Whenever a bug, breaking change, type mismatch, refactoring, or feature update is identified in a specific file or module, the AI agent MUST NOT limit fixes to that single file alone.
  - The AI agent MUST proactively search (`grep_search`) for all related, homologous, duplicate, or caller components across all platforms (Backend, Web Studio, Mobile, Extensions, CI/CD scripts) and fix or update them consistently in the same execution scope.

- **Code Comments (Bangla):** Whenever making changes to the codebase, always try to add explanatory comments in **Bangla** so that the rationale behind the changes is easily understood later by the team.

- **Production-Ready Implementation:** DO NOT use mocks, stubs, or dummy implementations. All code must be production-ready and fully functional. If integrating a feature, integrate it with the real backing services (e.g., Supabase, database).

- **Production-Grade Infrastructure Rule (STRICT):**
  - There are NO local-only environment hacks or local targets. All code, setups, dependencies, models, and workflows MUST be production-grade targeting live cloud infrastructure (GCP Cloud Run, Render, Vercel, Infisical, Supabase, Cloudflare, GitHub Actions).
  - Speculative local-path fallbacks, mock-type checking in production code, or local-only workarounds are strictly forbidden.

- **Strict Master Audit Execution Rule (MANDATORY):**
  - Whenever the user requests an audit (e.g., "audit", "run audit", "check code", "phase audit"), the AI agent **MUST strictly execute the Master Audit Blueprint** defined in [`docs/long-term-maintenance/SUPREMEAI_MASTER_AUDIT_PLAN.md`](file:///g:/supremeai%20backup/docs/long-term-maintenance/SUPREMEAI_MASTER_AUDIT_PLAN.md).
  - **Audit Reports Location:** Every Phase report MUST be saved as a separate markdown file inside the `docs/audit_reports/` directory (e.g., `docs/audit_reports/PHASE_01_CORE_BACKEND.md`).
  - **Empirical Evidence First:** Every finding MUST contain concrete evidence (exact file path, line number, grep output, or test log). Never claim an issue is "Fixed" without verifying code changes.
  - **Technical Taxonomy:** Audit findings MUST be categorized using standard technical error terms (`Content Security Policy Violation`, `CORS Blocked`, `Configuration Drift`, `Silent Failure`, `Race Condition`, etc.) and P0-P3 severity matrix.

- **End-to-End Diagnostic, Verification & Push Protocol (MANDATORY & NON-NEGOTIABLE):**
  - **Step 1 — Remote & Environment Discovery:** Always check target repositories (`git remote -v`) to clearly distinguish primary (`origin`) vs target (`target`) repos before running checks.
  - **Step 2 — Direct API Failure Retrieval:** Query GitHub API (`/actions/runs?status=failure`) to extract the exact list of failed runs, workflow names, run numbers, branch names, commit SHAs, and trigger events.
  - **Step 3 — Raw Execution Log Extraction:** Fetch raw job logs and inspect failed steps directly. When encountering HTTP 403 authorization redirects on log artifacts, handle signed URL redirects by stripping auth headers on S3 targets.
  - **Step 4 — Single Empirical Root-Cause Analysis:** Diagnose exact underlying failures (e.g., pytest assertion mismatches, working-directory misconfigurations, syntax errors, detached HEAD git auto-commits) with zero multi-option speculation.
  - **Step 5 — Homologous Fix Implementation:** Implement production-ready fixes across all caller and homologous files (backend, frontend, CI scripts). Add explanatory code comments in **Bangla**.
  - **Step 6 — Full Verification & Uncommitted Files Report:** Run local test/build verification, stage all uncommitted files (`git add`), and generate a short impact summary of all staged changes.
  - **Step 7 — Explicit Push Authorization & Single Remote Rule:** The AI agent MUST ONLY push to `origin` (`SaifulHaqueNiloy/supremeai`). Direct pushes to `target` (`paykaribazaronline`) are STRICTLY FORBIDDEN. Never run `git push` unless the user's prompt literally contains the exact word `"push"`.
  - **Token-Efficient CI Verification Protocol:** To conserve context and token limits, avoid high-frequency polling. Execute batch API background scripts, monitor until `paykaribazaronline` reaches 100% SUCCESS GREEN, and extract the clean final report efficiently.
  - **2-Minute Maximum CI Polling Rule:** When tracking GitHub Actions workflow runs after a push, the AI agent MUST poll status at 15-30 second intervals and deliver an empirical status update or failure log diagnosis within **2 minutes maximum**. Never leave CI status unverified beyond 2 minutes.
  - **Continuous Green Loop Mandate:** This diagnostic, fixing, log extraction, and verification process MUST run continuously in an iterative loop across both target (`target`) and primary (`origin`) repositories until EVERY SINGLE WORKFLOW RUN IS 100% FULL GREEN (zero failures).

---

## 🇧🇩 Bengali Language Excellence (BLE-001~003)

- **BLE-001:** প্রজেক্টের **ডিফল্ট ভাষা বাংলা** (Default Language is Bangla)। ইউজার যেকোনো ভাষায় (বাংলা বা ইংরেজি) প্রশ্ন করলেও উত্তর সবসময় স্পষ্ট ও সাবলীল বাংলায় দিতে হবে — **Banglish সম্পূর্ণ নিষিদ্ধ**।
- **BLE-002:** Customer-কে সর্বদা **'আপনি'** সম্বোধন করো — 'তুমি' নয়।
- **BLE-003:** Code সবসময় English-এ — তবে কোডের মন্তব্য (comments) বাধ্যতামূলক বাংলায় থাকতে হবে।
