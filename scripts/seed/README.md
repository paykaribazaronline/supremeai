# 🌱 Seed Data — `/home/nazifarabbu/supremeai/scripts/seed/`

Production seed scripts for SupremeAI Firestore collections.  
All AI endpoints are sourced from `../../src/main/resources/ai-cloud-endpoints.json` — real GCP Cloud Run deployed endpoints.

---

## Files

| File | Purpose |
|------|---------|
| `seed-data.json` | 13-collection seed payload (57 records): users, tiers, providers, agents, learning, domains, entries, recommendations, consensus, performance, workflows, activity logs, reasoning logs |
| `seed-all-data.js` | Multi-collection seeder — reads `seed-data.json`, writes to Firestore in batches |
| `seed-ai-providers.js` | Reads cloud endpoints, pings each for live latency + health status, writes to `api_providers` |
| `package.json` | NPM scripts (run from `scripts/` directory) |

---

## Requirements

- Node.js >= 18
- Credentials: one of
  - `GOOGLE_APPLICATION_CREDENTIALS` env var pointing to service account key
  - `src/main/resources/firebase-service-account.json` (auto-detected)

---

## Commands (run from `scripts/`)

```bash
# Preview only — shows what would be written
node seed/seed-all-data.js

# Write all 13 collections to Firestore
NODE_PATH=./functions/node_modules node seed/seed-all-data.js --execute

# Preview AI providers
NODE_PATH=./functions/node_modules node seed/seed-ai-providers.js

# Health-check all endpoints with live latency measurement
NODE_PATH=./functions/node_modules node seed/seed-ai-providers.js --healthcheck

# Write providers with measured latency
NODE_PATH=./functions/node_modules node seed/seed-ai-providers.js --execute --healthcheck

# Clear a collection, then re-seed
NODE_PATH=./functions/node_modules node seed/seed-all-data.js --clear --execute
```

---

## Verified Live Data (2026-05-16)

All 13 collections confirmed in Firestore project `supremeai-a`.

```
Collection                    Count   Status
────────────────────────────  ─────  ───────
users                            7   ✅
user_tiers                       3   ✅
ai_agents                        5   ✅
system_learning                 10   ✅
knowledge_domains               10   ✅
knowledge_entries                5   ✅
knowledge_recommendations        3   ✅
consensus_results                2   ✅
provider_task_performance        8   ✅
workflow_definitions             4   ✅
activity_logs                   10   ✅
reasoning_logs                   4   ✅
──────────────────────────────────────
Total                         5 AI providers seeded + approved
```

**AI Providers (GCP Cloud Run)**:
| Provider | URL | Latency | Roles |
|---|---|---|---|
| qwen-coder | `…qwen-coder-…run.app` | ≤3420ms | C,E,V |
| llama-3-1 | `…llama-3-1-…run.app` | ≤2890ms | C,V |
| deepseek-pro | `…deepseek-pro-…run.app` | ≤4100ms | C,E,V |
| phi-3 | `…phi-3-…run.app` | ≤1800ms | C,V |
| nomic-embed | `…nomic-embed-…run.app` | ≤900ms | — |

---

## Startup Validation

`SeedDataValidator` (`src/main/java/com/supremeai/config/SeedDataValidator.java`) automatically checks collection counts at app startup when running on non-local, non-test profiles:

```
[SeedDataValidator] 🔍 Checking seed data health...
[OK] Users: 7 records (min 3)
[OK] AI Providers: 5 records (min 5)
[OK] User Tiers: 3 records (FREE/PRO/ADMIN)
[OK] Knowledge Domains: 10 records
[OK] System Learning: 10 records
[OK] Knowledge Entries: 5 records
[OK] Provider Performance: 8 records
[OK] Workflow Definitions: 4 records
[SeedDataValidator] ✅ All seed data checks passed
```

---

> **Note**: Seed scripts use `NODE_PATH=./functions/node_modules` because `firebase-admin` is installed in `functions/`. Root `package.json` can be used to add a shared dev dependency if needed.
