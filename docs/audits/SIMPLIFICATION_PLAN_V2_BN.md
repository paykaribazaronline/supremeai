# 🧭 SupremeAI সরলীকরণ পরিকল্পনা V2 — দর্শন-ভিত্তিক পুনর্নির্মাণ

> **সংস্করণ:** ২.০ | **তারিখ:** ২০২৬-০৯-২৭
> **পূর্বসূরি:** `docs/audits/SIMPLIFICATION_DEEP_AUDIT_BN.md` (৯-agent ডেটা-সংগ্রহ; এই ডকুমেন্ট সেই ডেটাকে **দর্শনের চশমায়** পুনর্ব্যাখ্যা করে)
>
> **এই ডকুমেন্ট কেন দ্বিতীয়বার লেখা হলো:**
> প্রতিষ্ঠাতার সংশোধনী — *"সরলীকরণ মানে কোনো কিছু 'ভাদ দিয়ে' বোঝা নয়। তাই হলে প্রতিটি ফাইল মুছে দিলেই সবচেয়ে 'সরল' হতো। প্রথমে আমাদের মূল সংবিধান-দর্শন বুঝো, তারপর পরিকল্পনা নতুন করে বানাও — পুরো প্রজেক্টকে সামনে রেখে।"*
>
> এই সংস্করণ সেই নির্দেশ মেনে তৈরি: প্রথমে সংবিধান পড়া হয়েছে (README সংবিধান §১–১৪, ARCH-01, GOLDEN_RULES, Charter, Master Plan, MODULE_STATUS_REGISTRY, crown-jewel plan series) — **তারপর** সিদ্ধান্ত নেওয়া হয়েছে।

---

# ভাগ ১ — আমরা কী বুঝলাম: SupremeAI-এর দর্শন

## ১.১ মিশন এক বাক্যে

> **"The compounding capability graph — not the number of individual services — is the real product."**

SupremeAI মানে feature-এর সংখ্যা নয় — **এমন একটি সিস্টেম যা জানে সে কী পারে, কী দরকার, অভাব কোথায় পাবে, ফল কীভাবে যাচাই করবে, এবং প্রতিটি যাচাইকৃত সমস্যার পরে আরও সক্ষম হয়ে ওঠে।**

## ১.২ সংবিধানের ১৪ নীতি (README "SupremeAI Constitution")

| # | নীতি | সরলীকরণের জন্য অর্থ |
|---|---|---|
| ১ | Eternal Brain | স্মৃতি/learning সিস্টেম = পরিচয়; এগুলো কখনো "বাড়তি" নয় |
| ২ | Capability Sovereignty | capability সংগঠিত, replaceable — এক কাজের একাধিক কপি এর লঙ্ঘন |
| ৩ | **Reuse Before Creation** | Discover→Reuse→Compose→Adapt→Extend→Create — **ডুপ্লিকেশন এই নীতির সরাসরি লঙ্ঘন** |
| ৪ | Dynamic Discovery | registry/metadata > hardcoded inventory — মৃত hardcoded তালিকা = লঙ্ঘন |
| ৫ | Verification Before Trust | যাচাই ছাড়া কিছু "done" নয় — **fabricated কোড এই নীতির শত্রু** |
| ৬ | Policy Before Power | governance স্তর "বাড়তি স্তর" নয় — মেরুদণ্ড |
| ৭ | Reversible Evolution | পরিবর্তনে reason/evidence/rollback থাকবে — মোছার সিদ্ধান্তেও একই নিয়ম |
| ৮ | Graceful Degradation | fallback-chain/adapter জটিলতা দেখতে "বাড়তি", আসলে নীতির বাস্তবায়ন |
| ৯ | Provider Agnostic | provider-specific জিনিস adapter-এর পেছনে — ভাঙলে নীতি ভাঙে |
| ১০ | One System, Many Surfaces | web/extension/MCP/worker = এক মেশিনের একাধিক মুখ — প্রতিটি মুখ আলাদা সিদ্ধান্তের বিষয় |
| ১১ | Memory Must Compound | Task→Result→Experience→Memory লুপ = ভবিষ্যৎ মূলধন |
| ১২ | Least Privilege, Maximum Capability | Capability ≠ Permission — রাখা সহজ, তবু আলাদা নীতি |
| ১৩ | **No Silent Failure** | Failure→Detect→Explain→Repair→Verify→Report honestly — **মিথ্যা সাফল্য-দেখানো কোড এই নীতির সরাসরি শত্রু** |
| ১৪ | Sustainable Cost | ন্যূনতম টেকসই খরচ — ভঙ্গুর free-quota অনুমান নয় |

## ১.৩ সবচেয়ে গুরুত্বপূর্ণ আবিষ্কার: Capability-র ৩টি অবস্থা

README-র capability-composition মডেল বলে — যেকোনো ক্ষমতা ৩টি অবস্থায় থাকতে পারে:

| অবস্থা | অর্থ | সঠিক কাজ |
|---|---|---|
| **Available** | বাস্তবায়িত ও ব্যবহৃত | Reuse / Compose |
| **Near-ready** | ডিজাইন/আংশিক বাস্তবায়িত — শুধু wiring বাকি | **Finish it** — মোছা যাবে না! |
| **Missing** | কিছুই নেই | সর্বনিম্ন নতুন তৈরি |

**এবং ইতিহাস প্রমাণ করেছে এটা খালি কথা নয়:** issue #449-এ `diagram_parser_service` ও `video_to_code_pipeline` "orphaned" ধরা হয়েছিল — পরে সেই একই registry **wire-next** করে মূলধারায় জুড়ে দেওয়া হয়েছে (PR #581)। আজ যে ফাইলের caller শূন্য, কাল সেটা মিশনের অংশ হতে পারে।

**আরও পড়ুন:** README-র "Planning Is Part of the Capability Surface" — planning corpus মানে **"architectural intent"**, পুরোনো নোট নয়। তাই মোছার সিদ্ধান্তের আগে planning corpus অনুসন্ধান বাধ্যতামূলক।

## ১.৪ সত্যের ক্রম (Current-State Caveats)

```text
Runtime evidence > current source code > current tests/CI > telemetry > planning documents > assumptions
```

মোছার সিদ্ধান্তও এই ক্রমেই প্রমাণ চাইবে — assumption-ভিত্তিক "dead" ঘোষণা অগ্রহণযোগ্য।

## ১.৫ Zero-Waste দর্শনের হৃদয়-বাক্য

> **"The most important cost optimization is not another quota trick. It is not doing work that an existing capability can already do."**

অর্থাৎ সবচেয়ে বড় অপচয় হলো **একই কাজ করার দ্বিতীয় পথ তৈরি/রাখা** — ডুপ্লিকেশন। ফাইল-সংখ্যা নয়, **পথ-সংখ্যা** কমানোই আসল সরলীকরণ।

---

# ভাগ ২ — সংশোধিত সংজ্ঞা: এই প্রজেক্টে সরলীকরণ মানে কী

**ভুল সংজ্ঞা (আগের ডকুমেন্ট):** সরলীকরণ = caller-শূন্য ফাইল মোছা → LOC কমা।
*(এই সংজ্ঞা প্রতিষ্ঠাতার সূত্রে ভেঙে পড়ে: "প্রতিটি ফাইল মুছলেই তো সবচেয়ে সরল" — absurdum)*

**সংশোধিত সংজ্ঞা:**

> **সরলীকরণ = ফলাফল (মিশন-ক্ষমতা) হুবহু অপরিবর্তিত বা উন্নত রেখে এমন সব জটিলতা দূর করা, যা সংবিধান-নীতির সাথে সাংঘর্ষিক — এবং প্রতিটি সিদ্ধান্ত সত্যের ক্রম মেনে প্রমাণ-ভিত্তিক।**

ব্যবহারিকভাবে মানে চার ধরনের কাজ — এবং **শুধুমাত্র এই চার**:

| কাজের ধরন | সংবিধান-ভিত্তি | উদাহরণ |
|---|---|---|
| **A. একত্রীকরণ** — এক কাজের একাধিক পথ → এক পথ | নীতি ৩ (Reuse), নীতি ১৪ (Sustainable Cost) | ৪টি circuit-breaker → ১টি |
| **B. মৃত-ওজন অপসারণ** — যার না caller, না plan-intent, না near-ready মর্যাদা | নীতি ৫/১৩ (Honesty), নীতি ১৪ | fabricated `competitive_kit.py`, দ্বিতীয় FastAPI app |
| **C. Near-ready সম্পূর্ণকরণ** — wiring বাকি এমন ক্ষমতা মূলধারায় জোড়া | Phase 1 Gate: "Near-ready → available" | i18n wire-up, orphan adapters-এর সিদ্ধান্ত |
| **D. স্পষ্ট-চিহ্নিতকরণ** — সিদ্ধান্তহীন অবস্থায় ঝুলে থাকা জিনিসকে স্পষ্ট label | নীতি ৭ (Reversible), নীতি ১৩ (No Silent *anything*) | MODULE_STATUS_REGISTRY-প্যাটার্নে প্রতিটি ঝুলন্ত মডিউল চিহ্নিত |

**যা সরলীকরণ নয়:** ভবিষ্যৎ-ক্ষমতার জন্য রাখা কোড মোছা, সংবিধানের নিরাপত্তা-স্তর পাতলা করা, টেস্ট-মান কমানো (Golden Rule #9: NO TEST MANIPULATION), ব্যাখ্যা ছাড়া যেকোনো মুছা।

---

# ভাগ ৩ — আগের অডিটের সব ফাইন্ডিং-এর পুনর্শ্রেণীবদ্ধকরণ

আগের ডকুমেন্টের ৯-module ডেটা এখন দর্শনের চশমায় **৪টি বালতিতে** ভাগ হলো। যেখানে আগের রায় বদলেছে, সেখানে পুরোনো রায় কাটা দাগে দেখানো হলো।

## বালতি A — সংবিধান-লঙ্ঘন ডুপ্লিকেশন → **একত্রীকরণ** (সবচেয়ে দর্শন-সম্মত কাজ)

> "একই রক্ত দুই হৃৎপিণ্ডে" — crown-jewel plan (MODULE_03) নিজেই এটা চিনেছে।

| একই কাজের কতটি পথ | ফাইল | প্রস্তাব |
|---|---|---|
| **LLM কল ৩-স্তর** | `core/llm/llm_gateway` → `services/llm/llm_router.route()` (যা আগেই `dynamic_ai.orchestrator`-এ delegate করে!) → `services/dynamic_ai/*` (১,৮৭৩ LOC সমান্তরাল provider-registry) | `llm_gateway`-কে একমাত্র হৃৎপিণ্ড; বাকি দুই স্তরের অনন্য অংশ (circuit, cache) gateway-তে একত্র, বাকি adapter-shim |
| **৪টি CircuitBreaker** | `core/circuit_breaker.py` (৩৫১), `core/resilience/` (৪৪২), `dynamic_ai/` (১৮০), `auto_healer` (~৯৫) | `resilience` সংস্করণ canonical; বাকিগুলো thin re-export (state-casing `"open"` vs `"OPEN"` adapter-এ সংরক্ষণ) |
| **২টি Render deploy-trigger** | `.github/scripts/trigger_render_deploy.py` বনাম `scripts/ci/render_trigger_deploy.py` | shared `render_client.py`-এ একত্র |
| **২টি claim-লক** | `claim_issue.sh` (৪৯) বনাম canonical `atomic_claim.sh` (২৩৩) | প্রথমটি ৩-লাইন delegator |
| **২টি heartbeat client** | `scripts/agents/heartbeat_ping.*` বনাম `tools/agent_heartbeat/` | transport-adapter মডেলে একত্র (caller inventory আগে) |
| **২টি browser/scraper মুখ** | `core/agents/live/browser_agent` + `PlaywrightBrowserAgent` + মৃত fallback পরিবার | মৃতগুলো বাদ (বালতি B), জীবিতদের facade-একত্র |
| **২৫টি copy-paste Infisical ধাপ** | `ci-deploy-production.yml` | ১টি composite action |
| **docker-compose দ্বৈত** | `docker-compose.yml` ≈ `docker-compose.production.yml` কোর | base + prod-override |
| **সংবিধান-টেক্সট ৩ কপি** | AGENTS.md §2 ≡ charter §2 ≡ GOLDEN_RULES (যেটা নিজেই বিভ্রান্ত: "৮ নিয়ম" শিরোনামে ১০ সারি!) | AGENTS.md single-source; বাকিগুলো pointer |
| **`MODULES_LIST.md` ×২** | root ও docs/reference — md5-identical | generator-নির্ভর এক জায়গা |
| **hardcoded inventory ঝাঁক** | Telegram URL ×৫ ফাইল, GitHub URL ×৫, LazyModule-এর ১৩ proxy-তে ৪টি ব্যবহৃত | `core/config` constants; Dynamic Discovery নীতির সাথে সামঞ্জস্য |

**কেন এটাই সবচেয়ে দর্শন-সম্মত:** একই কাজের একাধিক পথ মানে — bug এক পথে ঠিক হলে অন্য পথে থেকে যায় (এমনিতেই এই repo-র ইতিহাসে ঘটেছে), খরচ দ্বিগুণ, বিশ্বাস অর্ধেক। নীতি ৩-এর সরাসরি প্রতিষ্ঠা।

## বালতি B — আসল মৃত-ওজন → **অপসারণ** (প্রমাণ: না caller, না plan-intent, না near-ready)

প্রতিটি আইটেম ৩-যাচাই পেরিয়েছে: (১) repo-wide grep caller=০, (২) `docs/plans/ + specs/`-এ intent=০, (৩) কোনো near-ready চুক্তি (docstring/registry) নেই।

| টার্গেট | ৩-যাচাই ফল | LOC |
|---|---|---|
| `core/competitive_kit.py` | **plan নিজেই রায় দিয়েছে**: "_call_llm ফেব্রিকেটেড '[Response from ...]' — ডিলিট-অথবা-ওয়্যার দুই সমাপ্তি" — এটা Constitution #5/#13-এর সরাসরি লঙ্ঘন (মিথ্যা সাফল্য!) | ১,৫৭৩ (+৯০৯ টেস্ট) |
| `api/server.py` — দ্বিতীয় FastAPI app | শূন্য boot-path; একই সারফেস মূল app-এ আছে; plan-mention শূন্য | ৩৮৪ |
| `workspace_feature_routes` orphan-registry | register ফাংশনের caller=০; ১২ router আলাদাভাবেই mounted | ২৩৮ |
| `services/ide_trio/` (dormant টুইন) | `MODULES_LIST.md` নিজেই লেখে "0 active callers (dormant)"; প্রোডাকশন `agents/ide` ব্যবহার করে | ৪২৮ |
| `adaptive_engine/self_improving_agent.py` | docstring: "ARCHIVED/SUPERSEDED — DO NOT WIRE" + ভাঙা import | ৪৪৭ |
| `llm_router.py`-এর মৃত exports | legacy `LLMGateway` (আসল gateway অন্যত্র), `HFSwarmRouter`, `quick_chat` — সব শূন্য-রেফারেন্স | ২২৫ |
| **৮টি চির-স্কিপড টেস্ট ফাইল** | ৬৫ টেস্ট যা গাণিতিকভাবে অসম্ভব চলা (টার্গেট মডিউল মুছে ফেলা/hallucinated) — ⚠️ তবে Master Plan Phase 2-এর "skip 125→<30" লক্ষ্য মেনে এগুলো মুছলে SKIPPED_TESTS.md-তে waiver-entry থাকবে — মুছা নয়, **সংখ্যা-হ্রাসের বৈধ রূপ** | ১,২৭৬ |
| **byte-identical ডুপ্লিকেট টেস্ট** | `test_auth_jit_otp_flow.py` ≡ `test_otp_router.py` (AST-hash প্রমাণিত ১৭/১৭) — মুছলে যাচাই-মান একই থাকে | ২১৫ |
| Frontend: ৮৪ unreachable ফাইলের যাচাই-প্রাপ্ত উপসেট | import-closure প্রমাণিত unreachable **এবং** কোনো plan-corpus intent নেই এমন cluster (যেমন `AdminDashboardHome`+widgets — সক্রিয় admin UI ভিন্ন শেল ব্যবহার করে) | ~৫,০০০ (পুরো ৯,১৫৫ নয় — বাকিগুলো বালতি C/D-তে) |
| mission-control-এর ৩০ unused ui component | bundle-পথে নেই; shadcn-এর নিজস্ব সংগ্রহ-প্যাটার্ন বলে এগুলো "টেমপ্লেট" নয়, অব্যবহৃত কপি | ৪,০০৪ |
| `.github/scripts/`-এ ৮ dead ফাইল | একটির docstring: "Replaces all legacy scripts" — কখনোই wire হয়নি; v1 superseded v2 দ্বারা | ১,৫৪২ |
| tools/ শূন্য-রেফারেন্স ৯ ফাইল | ৩-যাচাই শূন্য | ৬৮৬ |
| root playwright configs ×২ | testDir অস্তিত্বহীন; CI অন্য config চালায় | ১১৭ |
| `docs/plans/`-এর ১১৬ মৃত plan | নিজস্ব `plan_registry.json` "historical/superseded" — ⚠️ মুছা নয়, **`docs/archive/`-এ relocation** (intent-সংরক্ষণ সহ) | ৪৪,৬৫৩ (স্থানান্তর) |
| `backend/API-swagger.yaml` | generator-আউটপুট, ৪৩% বাস্তব-বিচ্যুত, কনজিউমাররা `openapi.json` পড়ে | ১১,০০৩ (regenerate বা মুছা) |
| `backend/_archive/` ৫৩ ফাইল | নিজের MANIFEST: "delete after 1 sprint" — মেয়াদ উত্তীর্ণ | ৩,৬৩৬ |

**কেন এগুলো দর্শন-সম্মত মোছা:** মিথ্যা সাফল্য-দেখানো কোড নীতি ৫/১৩-কে ভাঙে; মৃত plan-copy AI-agent-দের context বিষক্রিয়া করে (Dynamic Discovery-তে agent-রা ভুল intent পায়); দ্বিতীয় app/trigger নীতি ১৪-এর লঙ্ঘন।

## বালতি C — Near-ready Capability → **Finish / Formal-keep** (আগের অডিট যেখানে ভুল ছিল ⚠️)

**এই বালতিতে যেসব জিনিস আছে, সেগুলো মোছা হবে না — এটাই আগের ডকুমেন্টের সবচেয়ে বড় সংশোধনী:**

| টার্গেট | আগের রায় | ~~মুছুন~~ → **নতুন রায়** | কেন |
|---|---|---|---|
| `frontend/src/i18n/` | ~~মুছুন (কেউ t() ডাকে না)~~ | **রাখো + wire করো** | সংবিধান-ঘোষিত দ্বিভাষিকতা; plan-corpus-এ **সম্প্রসারণ-পরিকল্পনাই আছে** (`backend/core/i18n/locale_policy.py` নতুন!); CI-তে বাংলা i18n checker চলে। "Near-ready" — Phase 1 Gate-এর হুবহু কেস |
| `tools/vscode-extension/`-এর ১৮ ফাইল | ~~মুছুন (entry import করে না)~~ | **Owner-সিদ্ধান্ত: wire বা formal-freeze** | সংবিধান-ঘোষিত product surface (v6.0.0, ৩১ commands); ৫টি plan-mention; extension-এর নিজস্ব bundling থাকতে পারে — ভুল-পজিটিভ ঝুঁকি উঁচু |
| ৫টি vendor adapter (`e2b, openhands, browser_use, mem0, graphiti`) | ~~মুছুন (শূন্য consumer)~~ | **Registry-র নিজস্ব রায় মানো**: "archive or extract — keeping costs nothing at runtime" | এগুলো double-gated near-ready vendor-পথ; MODULE_STATUS_REGISTRY ইতিমধ্যে DECISION REQUIRED চিহ্নিত করেছে |
| `escrow_service` + `delivery_fleet_tracker` + `rider_tracker` | ~~মুছুন~~ | **Archive-after-owner-confirmation** (মুছা নয়) | সম্পূর্ণ, টেস্টেড state-machine = প্রকৃত near-ready; registry-র সুপারিশই এটা |
| ১১টি test-only tools মডিউল | ~~মুছুন~~ | **Per-module intent check → registry-চিহ্ন** | কিছু হয়তো Phase 1-এর "planned capability" (যেমন telegram MCP); অন্যগুলো বালতি B-তে যাবে |
| `apps/docs/` (Docusaurus) | ~~মুছুন (কেউ build করে না)~~ | **Owner-সিদ্ধান্ত: CI-তে wire বা formal-retire** | সংবিধান-ঘোষিত surface ("Docs site — Everyone", বাংলা-গাইডসহ); reality: শূন্য build-reference — এই বৈপরীত্যই No Silent Failure-লঙ্ঘন, তাই *সিদ্ধান্ত নিতে হবে*, ডিফল্টে মুছা নয় |
| frontend-এর বাকি unreachable cluster (Living/HITL স্ট্যাক ইত্যাদি) | ~~মুছুন~~ | **Cluster-প্রতি plan-intent check → B বা C** | কোনো কোনোটা ভবিষ্যৎ মিশনের UI-শেল হতে পারে; intent নেই প্রমাণ হলে বালতি B |
| surf-501 স্টাব + তাদের টেস্ট | ~~সংকুচিত করুন~~ | **হাত দেবেন না** | "Real work or loud failure" নীতির জীবন্ত বাস্তবায়ন — এটা মৃত-ওজন নয়, সততার চুক্তি |
| fallback chain, keep-alive ×৪, degradation পথ | ~~সরল করুন~~ | **হাত দেবেন না** | নীতি ৮ (Graceful Degradation)-এর মেরুদণ্ড — দেখতে জটিল, আসলে সংবিধান |

## বালতি D — অপরিবর্তিত মেরুদণ্ড → **হাত দেওয়া নিষিদ্ধ**

- `core/llm/llm_gateway` প্যাকেজ (registry, resilience, Tier-0, semantic cache, CostGuard) — plan-এর ভাষায় "প্রতিযোগী-মানের হৃৎপিণ্ড"
- memory/learning পরিবার (Eternal Brain, Memory Must Compound)
- `core/kernel` + `circles` (single-door facade — North-Star)
- HITL/approval/policy স্তর (Policy Before Power)
- টেস্টের যে অংশ বাস্তব আচরণ যাচাই করে (AST-বিশ্লেষণে প্রমাণিত বৈচিত্র্যময়)
- bilingual comment-সংস্কৃতি (আমরা যে ডকুমেন্ট পড়েছি সেগুলোর অর্ধেকই বাংলায় — এটা বৈশিষ্ট্য, বাগ নয়)

---

# ভাগ ৪ — নতুন ফেজ-পরিকল্পনা (Master Plan-এর Phase 1–2-এর সাথে সমকালিন)

## Phase S1 — "এক রক্ত, এক হৃৎপিণ্ড" (বালতি A শুরু) — সবচেয়ে দর্শন-সম্মত, সবচেয়ে মূল্যবান

```text
S1.1  দ্বিতীয় Render-trigger ও claim-লক delegator-এ পরিণত      (ঝুঁকি: কম)
S1.2  Infisical composite action — ২৫ copy-paste ধাপ → ১        (ঝুঁকি: কম)
S1.3  circuit-breaker ×৪ → resilience-canonical + adapter       (ঝুঁকি: মাঝারি)
S1.4  heartbeat ×২ → transport-adapter একত্র                    (ঝুঁকি: মাঝারি)
S1.5  llm_router-এর মৃত exports মুছা + gateway-দিকের দীর্ঘ পথের মানচিত্র (একত্রীকরণ আলাদা ইস্যু-ধারায়)
যাচাই প্রতিটিতে: full pytest green + pass^k + rollback-commit
```

## Phase S2 — সততা-সাফাই (বালতি B) — "Honesty over polish"-এর প্রতিষ্ঠা

```text
S2.1  fabricated কোড ঝাড়: competitive_kit (+ টেস্ট), llm_router মৃত exports
S2.2  দ্বিতীয় app/orphan-registry/dormant টুইন মুছা
S2.3  টেস্ট-সংখ্যা হ্রাস Master Plan-সম্মতভাবে: অসম্ভব-স্কিপ ৮ ফাইল মুছা
      (SKIPPED_TESTS.md waiver-entry সহ) + AST-identical ডুপ্লিকেট মুছা
S2.4  frontend unreachable clusters-এর per-cluster intent check → B/C ভাগ
S2.5  docs/plans relocation (archive, intent-সংরক্ষণ) + swagger regenerate
S2.6  root-file মিলন (MODULES_LIST, coverage-plan merge, dead config ×৭)
```

## Phase S3 — Near-ready সমাপ্তি (বালতি C) — Phase 1 Gate-এর সরাসরি বাস্তবায়ন

```text
S3.1  MODULE_STATUS_REGISTRY-প্যাটার্নে প্রতিটি ঝুলন্ত মডিউলের ৩-অবস্থা
      রায় লেখা (Available / Near-ready / Missing) — সিদ্ধান্তহীনতা নিষিদ্ধ
S3.2  i18n: Phase-1 wire-up (সংবিধান-প্রতিশ্রুত দ্বিভাষিকতা)
S3.3  owner-সিদ্ধান্ত সাপেক্ষে: vscode-extension, apps/docs, vendor-adapters,
      escrow-pair — প্রতিটির রায় registry-তে স্থায়ী হবে
```

## স্থায়ী নিয়ম (প্রতিটি ভবিষ্যৎ মোছার আগে)

```text
১. ৩-যাচাই: caller=০ (গতিশীল import সহ) ∧ plan-corpus intent=০ ∧ near-ready চুক্তি নেই
২. সত্যের ক্রম মেনে প্রমাণ: runtime evidence > source > tests > plans
৩. Near-ready সন্দেহ হলে → মোছা নয়, registry-তে রায় + finish-ইস্যু
৪. এক ইস্যু = এক ব্রাঞ্চ = এক PR; SKIPPED_TESTS/LESSONS_LEARNED হালনাগাদ
৫. মোছার পরেও evidence থাকবে — git history + registry entry (Reversible Evolution)
```

---

# ভাগ ৫ — সংখ্যা (দর্শন-ফিল্টারের পরে)

| বালতি | LOC | চরিত্র |
|---|---:|---|
| A — একত্রীকরণ | ~২,৫০০ সরাসরি + হাজারো ডুপ্লিকেট-ঝুঁকি দূর | দর্শন-সম্মত কাজের মেরুদণ্ড |
| B — মৃত-ওজন | ~১৫,০০০ কোড + ৫৫,০০০+ ডক (স্থানান্তর-সহ) | প্রমাণিত ৩-যাচাই-উত্তীর্ণ |
| C — Near-ready | মোছা হবে না; wire/decision হবে | আগের অডিটের সবচেয়ে বড় সংশোধনী |
| D — মেরুদণ্ড | অস্পৃশ্য | সংবিধান |

---

# শেষ কথা — প্রতিষ্ঠাতার সূত্রের সংশোধিত রূপ

> **2+2 = 4।** সত্যিই সবসময় 4।
> কিন্তু এই ঘরে কিছু `1+8-9+4` লেখা আছে যা মোছা যায় (প্রমাণসহ কারণ: সেটা কখনো 4-ই করেনি, বা 4 করার অন্য আরও ৩টি লেখা আছে একই কাগজে)।
> আবার কিছু লেখা আছে যেমন `x/0 যাচাই → 4` — এগুলো ভবিষ্যতের অঙ্কের প্রস্তুতি (near-ready) — এগুলো **শেষ করতে হবে, মুছতে নয়**।
> আর যেগুলো নিয়ে সন্দেহ আছে, সেগুলোর নাম কাগজের গায়ে লিখে রাখতে হবে — **নীরব ঝুলন্ত অবস্থা এই সংবিধানে নিষিদ্ধ** (No Silent Failure — কোডের মতো প্রক্রিয়াতেও)।

*এই পরিকল্পনা ৯-agent audit-এর ডেটা + সংবিধানের ১৪ নীতি + Master Plan-এর phase-gate + owner-registry-র বিদ্যমান রায় — চারটি স্তরের সংশ্লেষ। প্রতিটি ধাপ আলাদা claimed issue + PR হিসেবে সম্পাদিত হবে (GOLDEN_RULES #2, #3)।*
