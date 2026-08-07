# SupremeAI 2.0 — AGENTS.md
_Status: ACTIVE_
_Last Updated: 2026-08-07_

---

## Direct Agent Instructions & Core Rules

- **Empirical Truth & Direct Factuality Rule (STRICT & NON-NEGOTIABLE):**
  - **No Speculation or Unverified Assumptions:** Never state conclusions, code behavior, error root causes, or architectural facts based on memory, guesses, or assumed context.
  - **Empirical Grounding:** Every statement or answer must be directly backed by verified file contents (`view_file`), search results (`grep_search`), or real execution logs (`run_command`).
  - **Direct Truth Presentation:** Present facts, raw numbers, and status clearly, directly, and transparently without exaggeration, smoothing over failures, or soft promises.

- **Strict Execution Log & Status Verification Rule (NON-NEGOTIABLE):**
  - **No Unverified Success Claims:** NEVER claim a script, deployment, API call, or build has "completed", "triggered", or "succeeded" without explicitly reading the raw execution output log (`view_file` on log URI or command stdout) first.
  - **Strict Status Inspection:** Always verify exact HTTP status codes, error tracebacks, and service IDs from actual logs BEFORE reporting status to the user.
  - **No Speculative Confirmation:** If a deployment script outputs 404, error, or skipping messages, report the exact error directly. Do NOT assume or promise that background services are updating.

- **Universal Anti-Loop & Root-Cause First Rule (CRITICAL):**
  - **No Command/Execution Loop:** If ANY task, command, code fix, or Git operation fails twice consecutively, STOP immediately. Do NOT try a 3rd time with small variations.
  - **Root Cause Diagnosis:** Step back, inspect exact error logs/diffs, identify the core underlying issue, and present a clear single-line diagnostic summary to the user before proceeding.
  - **Zero-Assumption & Empirical Verification Rule (STRICT):** Never modify code, schemas, or config files, nor answer architecture/workflow questions based on memory or assumptions. Always perform empirical inspection (`view_file`, `grep_search`, or log reads) of authoritative source files, CI workflows, and deployment manifests BEFORE formulating answers or executing actions.

- **Zero Exaggeration & Strict Truthfulness Rule (NON-NEGOTIABLE):**
  - **No Fake Promises:** Never claim or promise that a script, file, or patch will fix "all errors" or "100% pass" unless empirical evidence (actual test runs/logs) proves it.
  - **Strict Objectivity:** Always report exact facts, raw test counts, and true limitations. Over-promising or hallucinating capabilities is strictly forbidden.

- **Strict PR & Merge Anti-Loop Rule:**
  - **DO NOT Create Multiple PRs:** Never create multiple pull requests for the same issue or task. If a PR has conflicts or fails, resolve the conflict on the EXISTING branch and push to the existing PR.
  - **No PR Spamming:** If a PR creation or merge fails twice consecutively, STOP immediately, analyze the root cause (e.g., diverged main, protected branch, local file locks), and explain the exact issue to the user instead of trying alternative branch creation loops.
  - **Direct Root-Cause Sync:** Always inspect `git diff` against `origin/main` FIRST before making changes or pushing, ensuring local code is aligned with the remote base.

- **Conflict Resolution & Admin Permission Rule (CRITICAL):**
  - Whenever any code, configuration, or environment key conflict is detected, the AI agent MUST NOT autonomously choose or duplicate items.
  - The AI agent MUST list the conflicting options clearly for the admin and keep ONLY the single approved item after obtaining explicit admin permission.

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
  - **Continuous Green Loop Mandate:** This diagnostic, fixing, log extraction, and verification process MUST run continuously in an iterative loop across both target (`target`) and primary (`origin`) repositories until EVERY SINGLE WORKFLOW RUN IS 100% FULL GREEN (zero failures).

---

## 🇧🇩 Bengali Language Excellence (BLE-001~003)

- **BLE-001:** প্রজেক্টের **ডিফল্ট ভাষা বাংলা** (Default Language is Bangla)। ইউজার যেকোনো ভাষায় (বাংলা বা ইংরেজি) প্রশ্ন করলেও উত্তর সবসময় স্পষ্ট ও সাবলীল বাংলায় দিতে হবে — **Banglish সম্পূর্ণ নিষিদ্ধ**।
- **BLE-002:** Customer-কে সর্বদা **'আপনি'** সম্বোধন করো — 'তুমি' নয়।
- **BLE-003:** Code সবসময় English-এ — তবে কোডের মন্তব্য (comments) বাধ্যতামূলক বাংলায় থাকতে হবে।
