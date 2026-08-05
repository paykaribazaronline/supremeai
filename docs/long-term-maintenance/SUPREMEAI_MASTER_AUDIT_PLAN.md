# SupremeAI 2.0 — Enterprise Master Audit & Prevention Blueprint v2 (Dev & Prod Continuous Plan)

**উদ্দেশ্য:** পুরো SupremeAI 2.0 মনোরিপো (backend + 5টি app + packages + infra) সিস্টেমেটিক্যালি অডিট করা এবং ডেভেলপমেন্ট (Dev) ও প্রোডাকশন (Prod) উভয় ফেজে টেকনিক্যাল বাগ, সিকিউরিটি ভায়োলেশন, সাইলেন্ট এরর, কনফিগারেশন ড্রিফট এবং **AI-agent-specific attack surface** সম্পূর্ণ স্বয়ংক্রিয়ভাবে প্রতিরোধ করা।

> **v2 changelog:** AI/Agent Security phase, P0 Stop-the-Line Protocol, per-phase Exit Criteria, Independent Verification rule, Supply Chain/CI-CD security, LLM Cost/Quota Guard, Cross-App Contract Testing, RBAC/Data Privacy pass, Rollback Strategy, Time & Parallelization matrix।

---

## 0. গ্লোবাল অডিট ও প্রিভেনশন রুলস (NON-NEGOTIABLE)

1. **Empirical Evidence Required:** প্রমাণ (grep count / static tool log / test run) ছাড়া কোনো ইস্যু "Fixed" দাবি করা যাবে না।
2. **Batching & Sub-task Limits:** বড় মডিউলের ফাইলগুলো ব্যাচ আকারে পড়তে হবে (একবারে সর্বাধিক ২০-৩০ ফাইল)।
3. **Strict Issue Reporting Format:**
   ```
   [ID] [Severity: P0/P1/P2/P3] [Technical Error Term] [ফাইল:লাইন]
   সমস্যা: (১ লাইনে)
   Root Cause: (২-৩ লাইনে)
   প্রমাণ: (কোড স্নিপেট / grep আউটপুট)
   Dev Guard / Prod Monitoring Solution: (প্রতিরোধমূলক ব্যবস্থা)
   ```
4. **Severity Matrix & Technical Mapping:**
   - **P0 (Critical):** `Cross-Site Scripting (XSS)`, `Command Injection`, `Prompt Injection → Tool Execution`, `Secret Invalidation/Rotation Drift`, Auth Bypass, `Data Loss / Corruption`, Production Outage Risk.
   - **P1 (High):** `False-Positive Claim (Regressive Bug)`, `Silent Failure / Exception Swallowing`, `Session Cache Poisoning`, `Agent Sandbox Escape`, `Runaway Cost / Quota Breach`.
   - **P2 (Medium):** `Content Security Policy (CSP) Violation`, `Cross-Origin Resource Sharing (CORS) Blocked`, `Configuration Drift`, `Hardcoded Endpoint Binding`, `Memory Leak`, `API Contract Breakage`.
   - **P3 (Low):** `Unused / Dead Code Dependency`, `Health Check Timeout`, Missing Tests, Code Style/TODO.
5. **Docs & Claims vs Code Truth:** `docs/` বা README-এর দাবি বিশ্বাস করা যাবে না; সরাসরি কোড এবং ডিপ্লয়েড স্টেট চেক করতে হবে।
6. **NEW — P0 Stop-the-Line Protocol:** যেকোনো ফেজে P0 পাওয়া গেলে সেই ফেজের বাকি কাজ থামিয়ে **অবিলম্বে** একটা আলাদা `hotfix/P0-<id>` ব্র্যাঞ্চে ফিক্স নেওয়া হবে, prod-এ deploy ও verify করার পর মূল ফেজে ফেরত আসা হবে। P0 কখনো "পরে দেখব" তালিকায় যাবে না।
7. **NEW — Independent Verification Rule:** যে এজেন্ট/ব্যক্তি ইস্যু ফিক্স করেছে, সে নিজে "Empirically Verified" ট্যাগ দিতে পারবে না — একটা দ্বিতীয় স্বাধীন রান (fresh grep/test/tool output, ভিন্ন সেশনে) দিয়ে ক্রস-চেক বাধ্যতামূলক। এই ছাড়া কোনো ইস্যু "Closed" স্ট্যাটাসে যাবে না।
8. **NEW — Per-Phase Exit Criteria (Definition of Done):** কোনো ফেজ "সম্পূর্ণ" ধরা হবে শুধুমাত্র যদি: (a) নির্ধারিত ফাইল কভারেজ ≥ ৯৫%, (b) সব P0/P1 issue হয় Closed নয়তো explicit owner + deadline সহ ট্র্যাকড, (c) সংশ্লিষ্ট automated guard (lint rule/CI check/monitor) লাইভ, (d) Independent Verification সম্পন্ন।

---

## 0.1 ঐতিহাসিক প্রস্তুতকৃত অডিট বেসলাইন (Historical Known Baseline Issues)

- **JWT Secret Instability (P0):** `backend/core/config.py`-এ `SUPREMEAI_JWT_SECRET` ডায়নামিকভাবে প্রতি রিস্টার্টে জেনারেট হচ্ছে কিনা রি-চেক।
- **Docker Image Secret Leak (P0):** `.dockerignore`-এ `.env*` এক্সক্লুড নিশ্চিত করা।
- **Middleware Chain Vulnerability (P0):** `ChaosInjector`/`HoneypotMiddleware` যেন Auth Middleware-এর আগে না চলে।
- **Secret Vault Fallback Vulnerability (P0):** `secret_vault.py` অনুপস্থিত সিক্রেটের জন্য `""` না দিয়ে `SecretNotFoundError` থ্রো করবে।
- **Thread Safety in Event Bus (P1):** `ErrorEventBus` রেস কন্ডিশন মুক্ত।
- **OTLP Exporter Missing (P1):** OpenTelemetry ডিপেন্ডেন্সি মিসিং থাকায় সাইলেন্ট ড্রপ বন্ধ।

---

## Phase 0 — টুলিং, বেসলাইন ও অটোমেটেড প্রিভেনশন সেটআপ (~২-৩ ঘণ্টা)

1. **Static Analysis & Security Scanner Run:**
   - Python: `ruff check .`, `bandit -r backend/core`, `mypy backend/`
   - TS/Web/Extension: `eslint .`
   - Flutter: `flutter analyze`
   - Secrets: `gitleaks detect --verbose`
   - **NEW — Supply chain:** `pip-audit`, `npm audit --production`, dependency lockfile integrity check, license scan (`pip-licenses`, `license-checker`)
2. **Dev Guard Rules:** pre-commit হুকে `gitleaks`, `eslint`, `ruff` + CSP duplicate-config grep রুল।
3. **Prod Guard Setup:** Infisical / GitHub Actions Environment Var Checker চালু।
4. **NEW — CI/CD Pipeline Self-Audit:** GitHub Actions workflow files নিজেই স্ক্যান করা — pull_request_target misuse, unpinned third-party actions (SHA pin না থাকা), secrets exposure to fork PRs।
5. **Master Log:** রুটে `PHASE_LOG.md` তৈরি ও ট্র্যাক।

---

## ১. অডিট ফেজসমূহ (Phase 1 → Phase 18)

| Phase | মডিউল | টেকনিক্যাল ফোকাস | Dev & Prod Guard | Est. Time | Parallel? |
|---|---|---|---|---|---|
| **1** | `backend/core/` (~205 files) | Command Injection, Silent Failure, Cascading Failure | Dev: AST shell-exec audit / Prod: Sentry error bus | 2-3d | — |
| **2** | `backend/api/`+`middleware/`+`database/` (~104) | CORS Blocked, Auth Bypass, Race Condition | Dev: dynamic CORS test / Prod: gateway CORS audit | 1-2d | — |
| **3** | `backend/agents/`+`brain/`+`evolution/` (~86) | False-Positive Claim, Rate Limit (429) | Dev: provider fallback tests / Prod: Redis rate monitor | 2d | with 6.5 |
| **4** | `backend/tools/`+`scripts/`+`utils/` (~154) | Dead Code, Silent Failure | Dev: `vulture` / Prod: log-level enforcement | 1-2d | with 5 |
| **5** | `backend/memory/`+`skills/`+`models/`+`schemas/` (~55) | Data Corruption, Session Cache Poisoning | Dev: Pydantic v2 validation / Prod: Redis TTL audit | 1d | with 4 |
| **6** | `backend/sandbox/`+`ws/`+`p2p/`+`admin/` (~43) | Command Injection, Memory Leak, Event Loop Blocking | Dev: cgroups limit test / Prod: Prometheus WS metric | 1-2d | — |
| **6.5 (NEW)** | AI/Agent Security — prompt injection surface, tool-permission scoping, agent-to-agent trust boundary, sandbox escape from tool execution | `Prompt Injection → Tool Execution`, `Agent Sandbox Escape`, `Unauthorized Tool Invocation` | Dev: adversarial prompt test suite, tool allow-list enforcement test / Prod: anomalous tool-call pattern alerting | 2-3d | with 3 |
| **7** | `backend/tests/` (~367) | Unmocked Network Timeout, Fake Assertion | Dev: `pytest-socket` / Prod: coverage gate (state actual target, not 38% floor) | 2d | — |
| **8** | `apps/studio-client/` (~348) | Hardcoded Endpoint, Session Cache Poisoning, XSS | Dev: `no-hardcoded-url` ESLint / Prod: `VITE_API_BASE` enforcement | 3d | with 9,10,11 |
| **9** | `tools/vscode-extension/` (~50) | CSP Violation, Message Contract Breakage | Dev: Webview CSP test / Prod: extension telemetry | 1d | with 8,10,11 |
| **10** | `apps/mobile/` (Flutter ~92) | Token Leak, Insecure Storage, Deep Link Flaw | Dev: Secure Storage check / Prod: API failover ping | 2d | with 8,9,11 |
| **11** | `apps/desktop-app/`+`java-worker/`+`hf-space/` | Electron `nodeIntegration` Leak, Docker Secret Leak | Dev: contextIsolation test / Prod: container image scan | 1-2d | with 8,9,10 |
| **12** | `infrastructure/`+`render.yaml`+Cloudflare+Firebase | Config Drift, Secret Rotation Drift, Health Probe Timeout | Dev: `sync_all_platforms_env.py --check` / Prod: health probe monitor | 1d | — |
| **13** | `packages/`+`shared/` (~20+) | Regressive Bug, Shared Dependency Mismatch | Dev: monorepo typecheck / Prod: artifact verifier | 1d | — |
| **13.5 (NEW)** | Cross-App API Contract Testing — backend ↔ studio-client/mobile/desktop/vscode/hf-space | `API Contract Breakage` | Dev: schema-diff / consumer-driven contract tests (Pact-style) / Prod: contract violation alert on deploy | 1-2d | — |
| **14** | Dependency / Vulnerability + Supply Chain (All Repos) | CVE, License Risk, Unpinned Action Risk | Dev: `npm audit`/`pip-audit` / Prod: Dependabot/Snyk PR alerts | 1d | — |
| **14.5 (NEW)** | RBAC & Data Privacy Pass | Missing role checks, PII in logs, over-broad data exposure | Dev: permission-matrix test / Prod: PII log scrubber verification | 1-2d | — |
| **14.75 (NEW)** | LLM Cost/Quota Governance | `Runaway Cost / Quota Breach` | Dev: per-agent token-budget unit test / Prod: real-time spend dashboard + hard kill-switch | 1d | — |
| **15** | Docs-vs-Code Consistency | False Documentation Claims | Dev: cross-verify docs vs grep / Prod: automated KB verification | 1d | — |
| **16** | End-to-End Integration & Master Roadmap | System-wide Cascading Failure | Dev: full E2E simulation / Prod: live synthetic transaction ping | 1-2d | — |
| **17 (NEW)** | Rollback & Deployment Safety | Bad deploy without safe revert path | Dev: blue-green/canary dry-run / Prod: automated rollback trigger on health-check failure | 1d | — |

**মোট আনুমানিক সময়:** সিকোয়েনশিয়াল হলে ~২৮-৩২ দিন; উপরের parallel gruoping ব্যবহার করলে ~১৮-২০ দিনে নামানো সম্ভব (ধরে নিয়ে একাধিক reviewer/agent সমান্তরালে কাজ করছে)।

---

## ২. ডেভেলপমেন্ট ও প্রোডাকশন কন্টিনিউয়াস মনিটরিং ফ্রেমওয়ার্ক

### A. Development Phase Guard
1. Pre-commit: `gitleaks`, `ruff`, `eslint`, CSP duplicate-check grep।
2. `pytest-socket` দিয়ে unmocked network কল ব্লক।
3. `.env` পরিবর্তনে `sync_all_platforms_env.py` বাধ্যতামূলক।
4. **NEW:** Adversarial prompt-injection regression suite — নতুন agent/tool যোগ হলেই রান হবে।
5. **NEW:** CI-তে unpinned GitHub Action ব্যবহার ব্লক করা (SHA-pin বাধ্যতামূলক)।

### B. Production Phase Guard
1. Health probe tracking (`/health`), backup backend sync cron (৬ ঘণ্টায়)।
2. Sentry/Datadog দিয়ে Silent Failure রিয়েল-টাইম ক্যাচ।
3. CORS/session watchdog — ব্রাউজার কনসোল এরর ট্র্যাকিং।
4. **NEW:** LLM spend dashboard + per-tenant/per-agent hard budget kill-switch।
5. **NEW:** Anomalous tool-invocation pattern alert (agent একটা tool normal-এর চেয়ে অস্বাভাবিক বেশি/অপ্রত্যাশিতভাবে কল করলে flag)।
6. **NEW:** Automated rollback — health check ফেল করলে পূর্ববর্তী stable deploy-এ auto-revert।

---

## ৩. মাস্টার লগ ফরম্যাট (`PHASE_LOG.md`)

```markdown
## Phase [N] — [মডিউল নাম] — [তারিখ]
- ফাইল কভারেজ: X/Y সোর্স ফাইল স্ক্যান করা হয়েছে
- ইস্যু সংখ্যা: P0=?, P1=?, P2=?, P3=?
- টেকনিক্যাল এরর ক্যাটাগরি ফোকাস: [...]
- Top 3 Critical Findings:
  1. [ID] [Severity] [Technical Error Term] [ফাইল:লাইন] — বিবরণ
  2. ...
  3. ...
- Dev Guard Action: [...]
- Prod Guard Action: [...]
- Exit Criteria Met: [ ] File coverage ≥95%  [ ] P0/P1 closed or tracked  [ ] Guard live  [ ] Independent verification done
- Self-Verification: ✅ Empirically Verified by [name/agent, different from fixer] (Grep/Logs/Test Output attached)
- P0 Stop-the-Line Triggered: Yes/No — if yes, hotfix branch: [link]
```

---

_SupremeAI 2.0 — Comprehensive Master Audit & Continuous Quality Blueprint
