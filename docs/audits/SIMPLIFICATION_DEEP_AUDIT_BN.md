# 🧹 SupremeAI গভীর-অডিট: সরলীকরণ নির্দেশিকা (Simplification Deep Audit)

> **তারিখ:** ২০২৬-০৯-২৭ | **পদ্ধতি:** Multi-agent parallel audit (৯টি স্বাধীন এজেন্ট, প্রতিটি একটি করে module)
> **মূলনীতি (প্রতিষ্ঠাতার দর্শন):**
>
> **"সরলীকরণ মানে হলো ফলাফল আগের মতোই।**
> **2+2 = 4 ... আর 1+8-9+4 = 4।**
> **আমরা 2+2 দিয়ে যদি সবসময় নিখুঁত ফলাফল পাই, তাহলে বাকি জটিলতা লেখাটাই পুরো ফালতু।"**
>
> এই রিপোর্টের **প্রতিটি ফাইন্ডিং-ই এমন**: ফলাফল/আচরণ হুবহু অপরিবর্তিত থাকবে, শুধু অতিরিক্ত কোড/জটিলতা মুছে যাবে।

---

## 📊 এক নজরে (Executive Summary)

| Module | আকার (LOC) | নিশ্চিত-অপসারণযোগ্য (কম ঝুঁকি) | যাচাই-পরবর্তী অপসারণযোগ্য (মাঝারি ঝুঁকি) |
|---|---:|---:|---:|
| backend/tests | ১,২২,৮৫৪ | ~১,৯৪০ | ~২,০০০ + CI-বাইরের ৫,৮৫৫ |
| backend/core | ৯৬,৬৫২ | ~৪,১০০ (+১,০৪৩ টেস্ট) | circuit-breaker unify ~৩০০ |
| backend/api | ৪১,৬৯০ | ~১,০৪০ | ~৫৮০ (shadow-route সিদ্ধান্ত) |
| backend/tools+skills+scout | ৩৩,৮৩৬ | ~২,৪৯০ | ~৪,৫৯০ (orphan scripts) |
| backend/services+agents+adaptive | ৩৪,৬৮৩ | ~৮,৪৪০ | ~৩,৫৯০ |
| frontend/src | ~৬১,৮০০ | ~৯,৮০০ (+৯০৭ টেস্ট) | i18n স্তর ~৩১৬ |
| apps+packages+infra+qa | ~৩৪,৩০০ | ~৫,৮৮০ | ~৪৭০ |
| scripts+tools+workflows | ~১,৩২,৪০০ | ~২,৮৮০ | ~২,৩২০ |
| docs+alembic+root (কোড নয়) | ~২,৯০,০০০ | ~৬১,০০০ | ~১,০৩,০০০ |
| **মোট** | **~৫,৬৪,০০০ কোড + ২,৯০,০০০ ডক** | **≈ ৩৬,৬০০ কোড-LOC (৬.৫%) + ৬১,০০০ ডক** | **≈ ১৩,০০০+ কোড-LOC** |

**অর্থাৎ: একটি লাইনের আচরণও না বদলে প্রায় ৩৭,০০০ লাইন কোড এবং ৬১,০০০ লাইন ডকুমেন্ট এখনই মুছে ফেলা যায়।**

---

## 🔬 পদ্ধতি (কীভাবে অডিট হলো)

৯টি স্বাধীন এজেন্ট সমান্তরালভাবে কাজ করেছে — প্রত্যেকে ঠিক একটি module-এর দায়িত্বে:

| Agent ID | Scope |
|---|---|
| audit-1 | `backend/core/` (৯৬k) |
| audit-2 | `backend/api/` (৪২k) |
| audit-3 | `backend/tools+skills+scout+scripts` (৩৪k) |
| audit-4 | `backend/services+agents+adaptive_engine` (৩৫k) |
| audit-5 | `backend/tests/` (১২৩k) |
| audit-6 | `frontend/` (৬২k) |
| audit-7 | `apps+packages+infrastructure+config` (৩৪k) |
| audit-8 | `scripts+tools+workflows` (১৩২k) |
| audit-9 | `docs+alembic+root` (২৯০k) |

**প্রতিটি ফাইন্ডিং-এর জন্য প্রমাণ বাধ্যতামূলক ছিল:**
- সমগ্র repo-জুড়ে grep করে caller-সংখ্যা যাচাই (import, string-keyed registry, dynamic import)
- FastAPI route registration (`routers.py` ALL_ROUTERS), docker-compose, CI workflow trigger বিশ্লেষণ
- Test-coverage যাচাই — dead বলার আগে নিশ্চিত যে কোনো test আচরণের উপর নির্ভর করছে না
- এই ধরনের ভুল-পজিটিভ ফাঁদ এড়ানো হয়েছে: `__init__.py` re-export parity, `importlib` দিয়ে lazy import, entry-point স্ক্রিপ্ট

---

# 🏗️ ভাগ-১: Backend কোড (Python)

## 1.1 `backend/core/` — ~৪,১০০ LOC অপসারণযোগ্য

| # | ক্যাটাগরি | ফাইল | সমস্যা | প্রস্তাব | ঝুঁকি | LOC |
|---|---|---|---|---|---|---|
| ১ | DEAD | `core/testing/qa_suite.py` + `core/accessibility/wcag_compliance.py` + `core/deployment/production_deploy.py` | "Complete AI system" ট্রায়ো — একমাত্র কনজিউমার `get_complete_ai_system()` যার পুরো repo-তে **শূন্য caller**, শুধু debug-print | ৩ মডিউল + `core/__init__.py`-এর eager import + `test_qa_suite_honesty.py` মুছুন | মাঝারি | ২,২১৩ |
| ২ | DEAD | `core/competitive_kit.py` | ১,৫৭৩ LOC-এর "competitive intelligence kit" — শুধু নিজের টেস্ট (৯০৯ LOC) import করে; প্রোডাকশনে শূন্য | মডিউল+টেস্ট মুছুন | কম | ২,৪৮২ |
| ৩ | DUP | `core/circuit_breaker.py` (৩৫১) বনাম `core/resilience/circuit_breaker.py` (৪৪২) | **দুটি স্বাধীন CircuitBreaker!** state-casing-ও ভিন্ন (`"open"` বনাম `"OPEN"`) | `resilience`-কে canonical রেখে আগেরটি thin adapter-shim | মাঝারি | ~৩০০ |
| ৪ | CMB | `core/startup/agents.py:49` (৬৩৯ LOC), `app_builder.py:44` (৫৪৫), `lifespan.py:48` (২৮৪) | বিশাল single-function যেগুলো আসলে ধাপে ধাপে service চালু করে | named helper-এ ভাগ করুন — আচরণ হুবহু এক | কম | পঠনযোগ্যতা |

**যা কখনো মুছবেন না (যাচাইকৃত load-bearing):** `llm_gateway` (৮৫ importer), `tier8`, `circles`, `self_evolution`, `config_classification.py`।

**Quick win:** `competitive_kit.py` + টেস্ট মুছুন → এক কমিটে −২,৪৮২ LOC।

---

## 1.2 `backend/api/` — ~১,০৪০ LOC (কম ঝুঁকি) + ~৫৮০ (সিদ্ধান্ত প্রয়োজন)

| # | ক্যাটাগরি | ফাইল | সমস্যা | প্রস্তাব | ঝুঁকি | LOC |
|---|---|---|---|---|---|---|
| ১ | DEAD | `api/server.py` | **দ্বিতীয় FastAPI app যা কোনোদিন serve হয় না!** সব boot-path `core.app:app` ব্যবহার করে; একমাত্র importer একটি টেস্ট | ফাইল+টেস্ট মুছুন | কম | ৩৮৪+১২৬ |
| ২ | SHADOW | `browser_routes.py`-এর ৪ handler | `browser/` package router আগে mount হয়, তাই `/api/browser/ai-action` ইত্যাদির **admin-guarded টুইন কখনোই চলে না** (first-match-wins) | canonical নির্ধারণ করে অন্যটি মুছুন | মাঝারি | ~৪৫০ |
| ৩ | DEAD | `workspace_feature_routes.py` + `tier_s_routes.py` | `register_tier_s_routes`-এর **শূন্য caller** — ১২টি router আলাদাভাবেই registered | দুটি ফাইল মুছুন | কম | ২৩৮ |
| ৪ | CFG | `routers.py:15-481` | ৪৬৭ লাইনে মাত্র ১৬৩টি dict — মাল্টি-লাইন র‍্যাপিং ছাড়া কিছুই না | স্ক্রিপ্টে ১-লাইন/entry করুন | কম | ~২৯০ |
| ৫ | OVE | `browser/__init__.py` | ~৯০ নামের re-export block, কিন্তু প্রকৃত কনজিউমার ৪টি নাম | ৪টি রেখে বাকি ফেলে দিন | মাঝারি | ~১৭০ |
| ৬ | SHADOW | `approval_manager.py` | ৩ endpoint `hitl_admin` দ্বারা shadowed — unreachable | owner সিদ্ধান্ত | মাঝারি | ~৯৫ |
| ৭ | DEAD | ৪টি shim ফাইল | `meta_ai.py`, `codeflow.py`, `healing.py`, `dock_actions.py` — শূন্য importer | মুছুন | কম | ৭৩ |

**⚠️ নিরাপত্তা-সংক্রান্ত বোনাস পাওয়া গেছে:** `evolution.py:52` ও `agent_breeding.py:48`-এ `require_admin_token`-এর **দুর্বল ক্লোন** আছে যা DEEP-007-এ বাদ দেওয়া static-token fallback এখনো মেনে নেয় — এটা মার্জ করলে সরলীকরণই নয়, **নিরাপত্তা-সংশোধনও** হবে।

---

## 1.3 `backend/tools/ + skills + scout + scripts` — ~২,৪৯০ LOC নিশ্চিত, ~৭,০৮০ founder-নিশ্চিতকরণের অপেক্ষায়

| # | ক্যাটাগরি | টার্গেট | প্রমাণ | LOC |
|---|---|---|---|---|
| ১ | LEG | `backend/scripts/`-এ **১৩টি orphan CLI** | দুই-ধাপ স্ক্যানে শূন্য রেফারেন্স (যেমন `superai_free_tier_monitor.py` ১,৩৭৫ LOC; `seed_tools_registry.py` যা ৩৮টি tool-এর **পুরোনো-ভাঙা path** seed করে) | ৩,০০৭ |
| ২ | LEG | **১১টি test-only মডিউল** | প্রোডাকশনে শূন্য import, শুধু নিজের টেস্ট (যেমন `mcp_neon.py` ৩৬৩, `mcp_telegram.py` ২১২) | ১,৫৭৭ |
| ৩ | DEAD | `tools/creative/` পুরো প্যাকেজ | registry-এর শূন্য caller — প্রমাণিত unreachable | ৪৭৫ |
| ৪ | DEAD | `mcp_tools.py` + `web_fallback_agent.py` | শূন্য importer; docstring-ই প্রমাণ | ২৯৯ |
| ৫ | LEG | ৬টি self-confessed shim | docstring-এ লেখাই আছে "Compatibility shim", "Backward compatibility bridge" — importer শূন্য | ৩২৬ |
| ৬ | OVE | `tools/__init__.py` LazyModule | ১০৭ LOC-এর machinery, বাস্তবে ৪টি alias-ই দরকার | ৮৫ |
| ৭ | CMB | `mcp_github_cicd.py` (১,৪৯৪ LOC) | ১৪ বার কপি-হওয়া boilerplate → একটি `_github_request()` helper | ২৫০-৩৫০ |

**DUP-সংকেত:** browser/scraper পরিবারে ৬টি overlapping সারফেস + Telegram URL স্ট্রিং ৫ ফাইলে + GitHub API URL ৫ ফাইলে → `core/config`-এ একটি constant-এ আনুন।

---

## 1.4 `backend/services/ + agents/ + adaptive_engine/` — **সবচেয়ে বড় সাফাই: ~৮,৪৪০ LOC**

| # | ক্যাটাগরি | টার্গেট | প্রমাণ | LOC |
|---|---|---|---|---|
| ১ | DEAD | `agents/governance/*` (৪ এজেন্ট) | module+class নামে সমগ্র repo-তে শূন্য রেফারেন্স | ১,৩৭৯ |
| ২ | DEAD | `agents/monitoring/*` (৪ এজেন্ট) | একই | ১,০১৮ |
| ৩ | DEAD | `agents/devops/{llm_cost_optimizer,multicloud_quota_monitor}` | শূন্য caller | ৯৭৪ |
| ৪ | DEAD | `agents/ux/accessibility_agent.py` | `ux/__init__.py`-ই নেই! | ৬৯৫ |
| ৫ | DUP | `services/ide_trio/` (৪২৮) | `agents/ide/trio_adapters.py`-এর নিদ্রিত টুইন — প্রোডাকশন শুধু `agents` ভার্সন import করে; নিজস্ব `MODULES_LIST.md`-ই লেখে "0 active callers (dormant)" | ৪২৮ |
| ৬ | DEAD | `escrow_service` + `delivery_fleet_tracker` + `rider_tracker` | মালিক-যাচাইকৃত orphan — `MODULE_STATUS_REGISTRY.md`-তে "ORPHANED-VERIFIED" চিহ্নিতই আছে | ৭২৬ |
| ৭ | DEAD | `agents/evolution_agents/*` | ২টি সক্রিয় + ২টি DeprecationWarning-shim, সব শূন্য-কলার | ৪৭২ |
| ৮ | DEAD | ৯টি zero-ref service (`worker/main.py`, `browser/main.py`, `minio_client.py`...) | deployed worker আসলে `backend/worker_service.py` | ৯৯০ |
| ৯ | LEG | `adaptive_engine/self_improving_agent.py` | নিজের docstring: "⚠️ ARCHIVED / SUPERSEDED — DO NOT WIRE" + ভাঙা import | ৪৪৭ |
| ১০ | DEAD | `services/llm/llm_router.py`-এর ভেতরের মৃত অংশ | legacy `LLMGateway` (আসল gateway হলো `core/llm/llm_gateway.py`), `HFSwarmRouter`, `quick_chat` — সব শূন্য-রেফারেন্স | ২২৫ |

**🏗️ কাঠামোগত পর্যবেক্ষণ (আসল "1+8-9+4" খুঁজে পাওয়া গেছে):** LLM কল পথে **৩-স্তর স্তূপ** — `core/llm/llm_gateway` → `services/llm/llm_router.route()` (যা আগেই `dynamic_ai.orchestrator.generate()`-এ delegate করে!) → `services/dynamic_ai/*` (১,৮৭৩ LOC-এর সমান্তরাল provider-registry)। এবং পুরো repo-তে **৪টি পৃথক circuit-breaker implementation!** এগুলো একত্রিত করা হলে হাজার হাজার LOC বাঁচবে — তবে এটা ধাপে ধাপে (hot path) করতে হবে।

---

## 1.5 `backend/tests/` — ৬৪৯ ফাইল, ১,২২,৮৫৪ LOC-এর সাফাই

| # | ক্যাটাগরি | টার্গেট | প্রমাণ | LOC |
|---|---|---|---|---|
| ১ | DEAD | **৮টি চির-স্কিপড টেস্ট ফাইল** | ৬৫টি টেস্ট যা **কখনোই চলতে পারে না** (unconditional skip; টার্গেট মডিউল মুছে ফেলা হয়েছে) — প্রতি PR-এ collect হয়, execute হয় শূন্যবার | ১,২৭৬ |
| ২ | DUP | `core/test_auth_jit_otp_flow.py` | `services/test_otp_router.py`-এর **AST-hash-যাচাইকৃত byte-identical ডুপ্লিকেট** (১৭/১৭ টেস্ট এক) | ২১৫ |
| ৩ | CHECK | `tests/conftest.py`-এ **১১টি শূন্য-রেফারেন্স fixture** | AST স্ক্যানে ৬৪৮ ফাইলে কোনো ব্যবহার নেই | ১২৬ |
| ৪ | OVE | `tests/factories/__init__.py` | dead fixture-chain | ৬৬ |
| ৫ | LINT | ২৭৮ ফাইলে **৩৮৮টি unused import** | `ruff --select F401 --fix` এক কমান্ড | ~২০০ |
| ৬ | DUP | MCP FakeCursor/FakeConn | ২ ফাইলে হুবহু কপি → `conftest.py`-এ হোস্ট | ৪৫ |
| ৭ | FLAG | repo-root `tests/` (৫,৮৫৫ LOC, ২৯০ টেস্ট) | **কোনো CI/workflow/Makefile এগুলো চালায় না!** মানে ২৯০টি বাস্তব টেস্ট সবসময় অদৃশ্য | CI-তে wire করুন |

**ভালো সংবাদ:** বড় টেস্ট-ফাইলগুলো AST-বিশ্লেষণে সত্যিই বৈচিত্র্যময় — যান্ত্রিক parametrize করে লাভ নেই। অর্থাৎ মোটা অঙ্কের টেস্ট কোড আসলে কাজ করছে; মৃত অংশটাই নির্দিষ্ট করে মুছতে হবে।

---

# 🎨 ভাগ-২: Frontend (React/TypeScript)

## `frontend/src/` — **~৯,৮০০ LOC (১৭%) অপসারণযোগ্য** — সবচেয়ে চমকপ্রদ ফলাফল

Python import-graph স্ক্যানে প্রমাণিত: `main.tsx` থেকে **৪০৯টি prod ফাইলের মধ্যে মাত্র ৩২৫টি পৌঁছানো যায়** → **৮৪ ফাইল (৯,১৫৫ LOC) একদমই unreachable!**

| # | টার্গেট | প্রমাণ | LOC |
|---|---|---|---|
| ১ | `components/dashboard/` "Living/HITL" স্ট্যাক (১৯ ফাইল) | অ্যাপের আসল root `shell/UnifiedAppShell` — এই স্ট্যাকের কোনো import-edge নেই | ~২,১৫০ |
| ২ | `AdminDashboardHome` + ৪টি প্রাইভেট widget | শূন্য importer; admin UI আসলে `AdminShell`→`AdminConsole` চালায় | ১,১৩৭ |
| ৩ | `infra/ServiceHealthMonitor.tsx` | শূন্য importer; সক্রিয় UI `ServiceHealthMetrics` | ৬৫৭ |
| ৪ | admin leftover ঝাঁক (AISurfaceAssignment, RulesEnginePanel, ScreencastViewer...) | সব শূন্য-কলার | ১,১৬৯ |
| ৫ | `costOptimizer.service.ts` + `cache.manager.ts` | **ব্রাউজার-বান্ডেলে `@upstash/redis` (সার্ভার SDK!) শিপ হচ্ছিল** — dependency-ও ঝরবে | ৬১৩ |
| ৬ | customer prototype subtree | সক্রিয় route এগুলো import করে না | ৫৭২ |
| ৭ | পুরোনো layout/shell (Header, NavRail, MainLayout, Shell...) | superseded by `shell/` | ৮৬২ |
| ৮ | services/store orphan ঝাঁক | শূন্য-কলার | ৬১৯ |
| ৯ | commandcenter/realtime স্তর | কোথাও mounted নয় | ৩৪৫ |
| ১০ | **i18n স্তর — mounted কিন্তু কেউ `t()` ডাকে না!** | একমাত্র কনজিউমার (dashboard/Header) নিজেই dead | ৩১৬ |

**অব্যবহৃত npm deps:** `@upstash/redis`, `dexie-react-hooks`, `@supremeai/design-tokens` — বান্ডল ছোট হবে, install দ্রুত হবে।

**DUP-নোট (এখনই নয়):** admin console ও commandcenter-এ একই নামের ভিন্ন বাস্তবায়ন (`LiveLogs`, `HealthMap`, `ModelRouter`, ২টি ⌘K palette) — একত্র করলে UI বদলাবে, তাই "ফলাফল-অপরিবর্তিত" ঘরে নয়।

---

# 📦 ভাগ-৩: Monorepo + Infrastructure

| # | টার্গেট | প্রমাণ | LOC |
|---|---|---|---|
| ১ | `apps/mission-control/src/components/ui/`-এর ৩০টি shadcn component | import-closure-এ ৪৮টির মধ্যে মাত্র ১৮টি পৌঁছায়; bundle-থেকে বাইরে | ৪,০০৪ |
| ২ | `apps/docs/` (Docusaurus) | কোনো workflow/compose/script build করে না; আসল docs পাইপলাইন `mkdocs.yml` | ৮৬৬ |
| ৩ | `packages/shared-services/` dead modules | `apiBridge.ts`-এর শূন্য ব্যবহার + corrupted `vscode/index.ts` (`export * from '../n'`!) | ২৮০ |
| ৪ | mcp-control-plane-এর ৬টি অ-ওয়্যারড টেস্ট | কোনো test-runner ডাকে না | ১৭০ |
| ৫ | **root `playwright.config.ts` + `playwright-ct.config.ts`** | testDir অস্তিত্বহীন; CI অন্য config ব্যবহার করে; `pnpm test:e2e` শুধুই "no tests found" দেবে | ১১৭ |
| ৬ | `config/`-এ ৭টি মৃত ফাইল | কোনো কোড পড়ে না (একটি নিজেই "DEPRECATED" লেখা) | ১৩১ |

**Bonus:** `packages/ui-components` = একটিমাত্র export-এর পুরো প্যাকেজ (৪৭ LOC) → `frontend/src/providers`-এ নিয়ে প্যাকেজটি মুছুন।

---

# ⚙️ ভাগ-৪: Scripts + CI Workflows

| # | টার্গেট | প্রমাণ | LOC |
|---|---|---|---|
| ১ | `.github/scripts/`-এ **৮টি dead ফাইল** | `ci_error_report.py` (৩৪৬), `supreme_ci.py` (২৮২ — docstring: "Replaces all legacy scripts", কখনো wire হয়নি!), `ci_smart_summary.py` (v2-এর superseded v1), `check-render-quota.py` (প্রতিস্থাপিত)... | ১,৫৪২ |
| ২ | `tools/`-এ ৯টি শূন্য-রেফারেন্স ফাইল | `multi_model_knowledge_distiller.py` (৪৩৩) সহ | ৬৮৬ |
| ৩ | `tools/vscode-extension/src/`-এর ১৮ ফাইল | entry বা কেউ import করে না; কোনো CI/release পাইপলাইন নেই | ~২,২৭৬ (MED) |
| ৪ | **Render-deploy trigger ×২** | `.github/scripts/trigger_render_deploy.py` (২০৮) বনাম `scripts/ci/render_trigger_deploy.py` (১২৩) — একই API কল, একটিতে shared client আছে | ~১৭০ |
| ৫ | **ci-deploy-production.yml-এ ২৫টি copy-paste Infisical ধাপ** | প্রতিটি ১১-লাইন, শুধু secret-name ভিন্ন → ১টি composite action | ~২০০ |
| ৬ | setup-python ×৩৬ + pip ×৩ | composite action-এ শোষণ | ~১২০ |
| ৭ | **দুটি claim-লক স্ক্রিপ্ট** | `claim_issue.sh` (৪৯) বনাম canonical `atomic_claim.sh` (২৩৩) — একই GAP-01 প্যাটার্ন | ~৪৫ |

**ভালো সংবাদ:** ১৯টি workflow-ই জীবিত (শূন্য dead workflow); ci.yml-এর ৩৪ job সত্যিই parallel — বাদ দেওয়ার কিছু নেই।

---

# 📚 ভাগ-৫: Docs + Migrations + Root (সবচেয়ে বড় সংখ্যা!)

| # | টার্গেট | প্রমাণ | প্রভাব |
|---|---|---|---|
| ১ | `docs/plans/`-এ **১১৬/১৮৩ প্ল্যান মৃত** (historical/superseded/complete) | নিজস্ব `plan_registry.json` মেশিন-ট্র্যাকড | **৪৪,৬৫৩ LOC** `docs/archive/`-এ সরান |
| ২ | `backend/API-swagger.yaml` | generator-এর জমা হয়ে যাওয়া আউটপুট — ২৯৯ path vs প্রকৃত ৬৭৭ (**৪৩% coverage**), ≥২০ path কোডে নেই-ই; কনজিউমাররা `openapi.json` পড়ে | **১১,০০৩ LOC** মুছুন/regenerate |
| ৩ | `docs/generated/`-এ ৯১k LOC regenerable JSON | CI diff-gate শুধু `STATUS_PROOF.md` পড়ে | CI artifact-এ সরান |
| ৪ | Master-docs মৃত-কোড বর্ণনা করে | `AIBRAIN-01`-এর **৭০% রেফারেন্স deleted মডিউল**; `DOCUMENTATION_MASTER_INDEX`-এর ৭১% | স্টাব-এ রূপান্তর |
| ৫ | `backend/_archive/` (৫৩ ফাইল) | নিজের MANIFEST: "archive first, delete after 1 sprint" — তারিখ পেরিয়ে গেছে | ৩,৬৩৬ LOC |
| ৬ | সংবিধান-নিয়ম ৩ ফাইলে হুবহু কপি | `AGENTS.md` §2 ≡ charter §2 ≡ GOLDEN_RULES (যেটা নিজেই বিভ্রান্ত: "8 rules" শিরোনাম, সারি ১০!) | এক জায়গায় |
| ৭ | `MODULES_LIST.md` ×২ | **md5-verified byte-identical দুই জায়গায়** | ২০৪ |
| ৮ | `001_initial_schema.sql` | নিজের হেডার: "HISTORICAL / SUPERSEDED — DO NOT USE" — alembic তবুতে `.sql` চেনেই না | ৭৬৮ |

---

# 🗺️ অগ্রাধিকার-ভিত্তিক অ্যাকশন প্ল্যান

## ধাপ ১ — "এক সপ্তাহের দ্রুত জয়" (কম ঝুঁকি, সরাসরি মুছে ফেলা)

```
কমান্ড-স্তরের কাজ, প্রতিটির পরে full test suite সবুজ থাকবে:

১. ৮টি চির-স্কিপড টেস্ট ফাইল মুছুন           → −১,২৭৬
২. byte-identical OTP টেস্ট ডুপ্লিকেট মুছুন      →   −২১৫
৩. competitive_kit.py + টেস্ট মুছুন              → −২,৪৮২
৪. api/server.py (দ্বিতীয় app) মুছুন             →   −৫১০
৫. tier_s_routes অর্ফান মুছুন                    →   −২৩৮
৬. ৬টি self-confessed shim মুছুন                 →   −৩২৬
৭. tools/creative/ মুছুন                         →   −৪৭৫
৮. dead exports (llm_router) মুছুন               →   −২২৫
৯. ruff F401 --fix backend/tests                →   −২০০
১০. root playwright configs মুছুন               →   −১১৭
১১. .github/scripts/ ৮ dead ফাইল মুছুন           → −১,৫৪২
১২. ৯টি শূন্য-রেফারেন্স tools ফাইল মুছুন         →   −৬৮৬
─────────────────────────────────────────────────────
উপযোগ: ≈ ৮,৩০০ LOC, একটি ফাংশনের আচরণও বদলাবে না
```

## ধাপ ২ — গাছপালা কাটা (frontend + agents dead clusters)

- frontend-এর ৮৪-fail ঝাঁক (~৯,৮০০ LOC) + npm dep cleanup
- `agents/governance|monitoring|devops|ux|evolution_agents` (~৪,৫০০ LOC)
- mission-control-এর ৩০ unused component (~৪,০০০ LOC)
- `docs/plans/` relocation (~৪৪,৬৫৩) + swagger মুছা (১১,০০৩)
- প্রতিটি ধাপের যাচাই: `npm run knip` (frontend, ইতিমধ্যে wired), `pytest` full green, build green

## ধাপ ৩ — "সত্যিকারের 2+2" রূপান্তর (কাঠামোগত, সাবধানে)

এইগুলোই founder-এর দর্শনের আসল স্বাদ — একই ফল, এক রাস্তা:

1. **LLM পথ ৩-স্তর → ১-স্তর**: `llm_gateway` → `llm_router` → `dynamic_ai` স্তূপ ভেঙে এক canonical পথ
2. **৪টি circuit-breaker → ১টি** (`resilience` সংস্করণ canonical)
3. **২টি Render-trigger, ২টি claim-লক, ২টি heartbeat, ২টি browser-router → প্রতিটিতে ১টি**
4. **docker-compose base+override** মডেল

## ধাপ ৪ — যাচাই-প্রয়োজন সিদ্ধান্ত (মালিকের নোটিশ)

- shadow-route পরিবার: browser_routes টুইন, approval_manager, stream_chat_sse — কোনটি canonical?
- orphan CLI ১৩টি (ম্যানুয়াল ops টুল কিছু হলে রাখুন)
- root `tests/` (২৯০ টেস্ট) CI-তে wire করা হবে কি?

---

# 🛡️ যাচাই-নিয়ম (প্রতিটি মুছে ফেলার আগে)

```
১. Grep প্রমাণ:  caller-সংখ্যা = 0 (import + string-registry + dynamic)
২. Route-প্রমাণ: routers.py/ALL_ROUTERS-এ নেই
৩. Test-প্রমাণ:  যে test রাখবেন, তার target জীবিত
৪. ধাপে ধাপে:  এক PR = এক কারণ; full suite green; knip/build green
৫. বিশ্বাস নয়, প্রমাণ: docstring-এর "dead" লেখাও নিজে grep করে যাচাই করুন
```

**সতর্কতা-তালিকা (ভুল-পজিটিভ ফাঁদ):** `__init__.py` re-export, `importlib.import_module()`, FastAPI first-match-wins, alembic applied history, string-keyed registry (`"tools.browser_agent.*"` জাতীয় patch-string)।

---

# 🏁 শেষ কথা

**2+2=4 সূত্রে ফিরে গেলে:**

এই কোডবেসে এখন লেখা আছে হাজারো `1+8-9+4` — যেখানে ফলাফলটা আসলে সবসময় সেই চেনা `4`।
- **৩৭,০০০+ কোড-লাইন** এমন, যার কোনো পাঠক নেই (zero caller)
- **৬১,০০০+ ডক-লাইন** এমন, যার বর্ণনা করা জিনিসটাই আর নেই
- **২৯০টি টেস্ট** এমন, যা কোনোদিন চলেই না
- **৪টি circuit-breaker, ২টি claim-লক, ২টি app, ২টি deploy-trigger** — একই দায়িত্বের নকল দাবার

সরলীকরণের পরে ফলাফল হুবহু একই থাকবে — **কিন্তু কোড পড়বে কম লোক, বুঝবে দ্রুত, বাগ লুকাবে কম জায়গায়।**

*রিপোর্ট প্রস্তুতি: ৯-agent parallel audit | সব ফাইন্ডিং grep-প্রমাণিত | কোনো repo ফাইল পরিবর্তন করা হয়নি*
