# Admin Dashboard Implementation Guide

## Overview

Admin dashboards for **Documentation Rules Governance** and **AI Quota Rotation** enable non-developers to manage SupremeAI's core systems without code changes.

## 1. Documentation Rules Dashboard

### Purpose

Control where documentation goes, enforcement levels, and file validation rules.

### REST Endpoints

#### **View Current Rules**

```bash
GET /api/admin/doc-rules/current
```

**Response:**

```json
{
  "status": "✅ OK",
  "ruleId": "default",
  "ruleName": "Default Documentation Rules",
  "enabled": true,
  "allowedInRoot": ["README.md", "LICENSE", "ARCHITECTURE_AND_IMPLEMENTATION.md"],
  "docCategories": {
    "architecture": {
      "categoryName": "architecture",
      "rootPath": "docs/02-ARCHITECTURE/",
      "maxFileSizeKB": 500,
      "requireApproval": false
    }
  },
  "enforcementLevel": "WARNING",
  "autoCorrectPath": true
}
```

#### **Update Rules**

```bash
POST /api/admin/doc-rules/update
Content-Type: application/json

{
  "ruleId": "default",
  "ruleName": "Strict Doc Rules",
  "enforcementLevel": "STRICT",
  "autoCorrectPath": true,
  "docCategories": {...}
}
```

#### **Add New Category**

```bash
POST /api/admin/doc-rules/add-category?categoryName=guides&rootPath=docs/12-GUIDES/&maxFileSizeKB=1000&requireApproval=false
```

#### **Set Enforcement Level**

```bash
POST /api/admin/doc-rules/set-enforcement-level?level=STRICT

Levels:
- STRICT: Block non-compliant docs from generation
- WARNING: Warn but allow non-compliant docs
- INFO: Only log non-compliant docs
```

#### **Validate Document**

```bash
POST /api/admin/doc-rules/validate-document?filepath=docs/architecture.md&category=architecture&fileSizeKB=250

Response:
{
  "status": "✅ Valid",
  "valid": true,
  "errors": [],
  "warnings": [],
  "infos": ["Document requires admin approval before publishing"],
  "requiresApproval": true
}
```

### Dashboard UI Features

**Left Panel - Rules Control:**

- [ ] View Current Rules (read-only)
- [ ] Enforcement Level Selector (STRICT/WARNING/INFO)
- [ ] Auto-Correct Toggle
- [ ] Monthly Reset Status

**Center Panel - Category Management:**

- [ ] List all doc categories
- [ ] Add new category (name, path, max size)
- [ ] Edit category rules
- [ ] Delete category

**Right Panel - File Validation:**

- [ ] Paste document path
- [ ] Select category
- [ ] See validation results
- [ ] View suggested corrections

### Usage Example

**Scenario:** SupremeAI generated a doc in root folder instead of `docs/guides/`

**Admin Actions:**

1. Enforce level → STRICT
2. Validate document → Shows error "File not allowed in root"
3. Auto-suggest correction → "Move to docs/guides/..."
4. System applies correction automatically

---

## 2. AI Quota Rotation Dashboard

### Purpose
Monitor AI provider quotas and optimize AI selection. Supports **1 to unlimited providers** with cost optimization.

### Supported AI Providers (Example: 10 Free-Tier Providers)

| Provider | Free Tier Quota | Monthly Calls |
|----------|-----------------|---------------|
| OpenAI GPT-4 | 3/min | ~900 |
| Anthropic Claude | 5/min | ~1500 |
| Google Gemini | 15/day | ~450 |
| Meta Llama 2 | 100/day | ~3000 |
| Mistral | 10/day | ~300 |
| Cohere | 20/day | ~600 |
| HuggingFace | 50/day | ~1500 |
| xAI Grok | 25/day | ~750 |
| DeepSeek | 30/day | ~900 |
| Perplexity | 40/day | ~1200 |
| **TOTAL (Example)** | - | **~11,000 calls/month** |
| **Configurable** | Admin can add/remove | Any number |

### REST Endpoints

#### **Get Quota Summary**

```bash
GET /api/quotas/summary
```

**Response:**
```json
{
  "status": "✅ OK",
  "current_month": "2026-04",
  "total_providers": 10,
  "total_quota": 11000,
  "total_used": 2500,
  "total_remaining": 8500,
  "providers_healthy": 8,
  "providers_exhausted": 2,
  "projected_cost": "$0.00/month",
  "next_provider": "Claude",
  "optimal_provider": "Mistral"
}
```

#### **Get Detailed Provider Status**

```bash
GET /api/quotas/status

Response:
{
  "status": "✅ OK",
  "timestamp": "2026-04-02T10:30:00Z",
  "providers": {
    "OpenAI GPT-4 (free tier)": {
      "provider": "OPENAI_GAPI",
      "quota_total": 900,
      "quota_used": 250,
      "quota_remaining": 650,
      "quota_percent": "27.8%",
      "status": "OK",
      "estimated_cost": "$0.00",
      "failures": 0
    },
    "Google Gemini (free tier)": {
      "provider": "GOOGLE_GEMINI",
      "quota_total": 450,
      "quota_used": 450,
      "quota_remaining": 0,
      "quota_percent": "100.0%",
      "status": "EXHAUSTED",
      "estimated_cost": "$0.00",
      "failures": 3
    }
  }
}
```

#### **Get Next Provider (Round-Robin)**
```bash
GET /api/quotas/next-provider

Response:
{
  "status": "✅ Selected",
  "provider": "ANTHROPIC_API",
  "display_name": "Anthropic Claude (free tier)",
  "quota_remaining": 1200,
  "quota_status": "OK"
}
```

#### **Get Optimal Provider (Best Strategy)**
```bash
GET /api/quotas/optimal-provider

Response:
{
  "status": "✅ Recommended",
  "provider": "META_LLAMA",
  "display_name": "Meta Llama 2 (huggingface)",
  "strategy": "Highest remaining quota + lowest failure rate"
}
```

#### **Record API Call Success**
```bash
POST /api/quotas/record-success?provider=OPENAI_GAPI&tokensUsed=150

Response:
{
  "status": "✅ Recorded",
  "provider": "OPENAI_GAPI",
  "tokens_used": 150,
  "action": "Quota consumed"
}
```

#### **Record API Call Failure**
```bash
POST /api/quotas/record-failure?provider=GOOGLE_GEMINI

Response:
  "status": "⚠️ Failure Recorded",
  "provider": "GOOGLE_GEMINI",
  "note": "After 3 failures, provider will be skipped in rotation"
}
```

#### **Manual Monthly Reset**
```bash
POST /api/quotas/reset-monthly

Response:
{
  "status": "✅ Reset",
  "action": "Monthly quotas reset to full capacity",
  "timestamp": "2026-04-01T00:00:00Z"
}
```

#### **List All Providers**
```bash
GET /api/quotas/providers

Response:
{
  "status": "✅ OK",
  "total_providers": 10,
  "providers": [
    {
      "name": "OPENAI_GAPI",
      "display_name": "OpenAI GPT-4 (free tier)",
      "daily_quota": 30,
      "monthly_quota": 900,
      "monthly_cost_free_tier": "$0.00"
    },
    ...
  ]
}
```

#### **Health Check**
```bash
GET /api/quotas/health

Response:
{
  "status": "✅ HEALTHY",
  "quota_remaining": 8500,
  "timestamp": "2026-04-02T10:30:00Z"
}
```

### Dashboard UI Features

**Top Stats Panel:**
- [ ] Total remaining quota (8500)
- [ ] Projected monthly cost ($0.00)
- [ ] Providers healthy (8/10)
- [ ] Current month (2026-04)

**Provider Grid:**
- [ ] Cards for each provider showing:
  - Name and status (OK/NEAR_LIMIT/EXHAUSTED)
  - Quota bar (visual %)
  - API calls remaining
  - Failure count
  - Color coding (green/yellow/red)

**Selection Strategy Panel:**
- [ ] Next Provider (round-robin) → "Claude"
- [ ] Optimal Provider (smart) → "Llama 2"
- [ ] Strategy explanation

**Historical Tracking:**
- [ ] Monthly usage chart
- [ ] Provider performance graph
- [ ] Cost savings vs competitors

### Usage Example

**Scenario:** Google Gemini quota exhausted, need to route to next provider

**System Workflow:**
1. **Check quota** → `GET /api/quotas/status` → Gemini shows 100%
2. **Get next provider** → `GET /api/quotas/next-provider` → Returns "Mistral"
3. **Route request** → Use Mistral for next API call
4. **Record result** → `POST /api/quotas/record-success?provider=MISTRAL`
5. **Update summary** → Dashboard refreshes showing new provider allocation

---

## 3. AI Provider Routing Service

### Purpose
Intelligently route requests to best available provider based on quota, success rate, and category affinity.

### Routing Logic

```
User Request
  ↓
[Check] Category affinity?
  ├─ YES: Use category-optimized provider (learned over time)
  │       └─ If quota available → Route + record performance
  │
  └─ NO: Use optimal provider
         └─ Highest remaining quota + success rate
            └─ Fallback to round-robin
```

### Performance Tracking Per Category

**Available Categories:**
- `architecture` - Design and system decisions
- `coding` - Code generation and refactoring
- `error_handling` - Debugging and fixes
- `documentation` - Writing and docs
- `innovation` - Novel problem solving

### Endpoint: Record Provider Performance
```bash
POST /api/routing/record-performance
Content-Type: application/json

{
  "provider": "ANTHROPIC_API",
  "category": "architecture",
  "success": true,
  "responseTimeMs": 1250,
  "qualityScore": 0.92
}
```

### Endpoint: Get Recommendations
```bash
GET /api/routing/recommendations/architecture

Response:
{
  "category": "architecture",
  "recommendations": [
    {
      "provider": "ANTHROPIC_API",
      "success_rate": "94.5%",
      "attempts": 55,
      "avg_response_ms": 1200,
      "quality_score": "0.89"
    },
    {
      "provider": "OPENAI_GAPI",
      "success_rate": "88.2%",
      "attempts": 34,
      "avg_response_ms": 950,
      "quality_score": "0.85"
    }
  ]
}
```

---

## 4. Admin Dashboard Workflow

### Daily Operations

**Morning:**
1. Check `/api/quotas/summary` → See daily quota usage
2. Review `/api/quotas/status` → Identify exhausted providers
3. Check `/api/routing/recommendations/[category]` → Confirm AI assignments

**When Assigning New AI Task:**
1. Call `GET /api/quotas/optimal-provider` → Get best provider
2. System auto-routes request using that provider
3. After completion: Call `POST /api/quotas/record-success` → Log performance
4. System learns category affinity for future

**When Provider Fails:**
1. Call `POST /api/quotas/record-failure?provider=X` → Track failure
2. After 3 failures → Provider skipped in rotation
3. System moves to next healthy provider

**Monthly Maintenance (April 1st):**
1. Call `POST /api/quotas/reset-monthly` → Reset all quotas
2. Review `/api/routing/recommendations/[category]` → Adjust AI assignments if needed
3. Archive previous month's `/api/quotas/status` for records

### Documentation Governance

**Weekly:**
1. Review `/api/admin/doc-rules/current` → Check active rules
2. Validate new docs: `POST /api/admin/doc-rules/validate-document`
3. If violations found: Adjust enforcement level or auto-correct

**When SupremeAI Generates Docs:**
1. System calls validation endpoint automatically
2. If level=STRICT and violations → Block generation
3. If level=WARNING → Generate but warn
4. If auto-correct=true → Correct path automatically

---

## 5. Integration with SupremeAI Code Generation

### When Creating New Service:

**Step 1:** User submits requirement
```bash
POST /api/extend/requirement
body: "create UserAuditService with methods: audit, log, export"
```

**Step 2:** SupremeAI generates code

**Step 3:** System validates documentation:
```bash
POST /api/admin/doc-rules/validate-document
filepath: "docs/services/UserAuditService.md"
category: "implementation"
```

**Step 4:** If enforcement level = STRICT and violations:
- Block service creation
- Return validation errors to admin
- Admin adjusts rules or approves override

**Step 5:** If passes validation:
- Complete service creation
- Auto-commit with audit trail
- Record provider performance metrics

---

## 6. Monitoring & Alerts

### Critical Thresholds

| Metric | Warning | Critical |
|--------|---------|----------|
| Quota remaining | 15% | 5% |
| Provider failures | 2/3 | 3/3 (skip) |
| API latency | >2s | >5s |
| Doc violations | 1 | 3+ |

### Alert Types
- ⚠️ **WARNING**: Quota near limit → Consider next provider
- 🔴 **CRITICAL**: All providers exhausted → Wait for monthly reset
- 🚫 **BLOCKED**: Doc violates STRICT rules → Admin approval needed
- 📊 **INFO**: Monthly reset completed → New quotas available

---

## 7. Troubleshooting

| Issue | Diagnosis | Solution |
|-------|-----------|----------|
| High latency >5s | Check provider response times | Switch to optimal provider |
| All quotas exhausted | End of month quota depletion | Wait for monthly reset or upgrade to paid |
| Docs in wrong folder | Doc rule violation | Enable auto-correct or adjust category paths |
| Provider keeps failing | API key expiration or rate limiting | Record failure, skip provider after 3× |
| AI assignment unclear | No learning data yet | Route by optimal (quota+success) until learning kicks in |

---

## 8. Best Practices

✅ **DO:**
- Check health endpoint daily: `GET /api/quotas/health`
- Monitor provider performance weekly
- Use optimal provider for new/unknown tasks
- Set doc enforcement to WARNING initially, STRICT after validation
- Document all rule changes with timestamps

❌ **DON'T:**
- Manually override quota counts (let system track automatically)
- Use exhausted provider (system auto-skips after learning)
- Set enforcement to STRICT without 2 weeks of WARNING data first
- Change doc rules mid-generation (wait for completion)

---

## 9. Cost Tracking

### Monthly Cost Formula
```
Monthly Cost = Σ(provider_calls × provider_cost_per_call)

Using only free tiers:
Monthly Cost = Σ(provider_calls × $0) = $0
```

### Projected Annual Savings
- Traditional multi-subscription: ~$12,000/year
- SupremeAI (free tiers): ~$0/year
- Savings: **100%** ✅

### When to Consider Premium Tiers
- Monthly quota consistently exceeds 11,000 calls
- Latency requirements <500ms (free tiers average 1-2s)
- Reliability SLA > 99% (free tiers may throttle)

---

## 10. API Reference Quick Table

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/admin/doc-rules/current` | GET | View active rules |
| `/api/admin/doc-rules/update` | POST | Update rules |
| `/api/admin/doc-rules/add-category` | POST | Create new category |
| `/api/admin/doc-rules/remove-category` | DELETE | Remove category |
| `/api/admin/doc-rules/set-enforcement-level` | POST | Change strictness |
| `/api/admin/doc-rules/validate-document` | POST | Check doc compliance |
| `/api/quotas/summary` | GET | Overall quota status |
| `/api/quotas/status` | GET | Detailed provider stats |
| `/api/quotas/next-provider` | GET | Round-robin selection |
| `/api/quotas/optimal-provider` | GET | Smart selection |
| `/api/quotas/record-success` | POST | Log successful call |
| `/api/quotas/record-failure` | POST | Log failed call |
| `/api/quotas/remaining` | GET | Total remaining quota |
| `/api/quotas/reset-monthly` | POST | Manual reset |
| `/api/quotas/providers` | GET | List all providers |
| `/api/quotas/health` | GET | System health check |

---

**Document Updated:** April 2, 2026  
**Next Review:** April 9, 2026  
**Version:** 1.0 (Quota Rotation + Doc Governance)
