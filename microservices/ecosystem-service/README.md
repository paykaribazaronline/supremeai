# SupremeAI Ecosystem Service (Microservice)

Standalone ecosystem foundation service — deployable to Render as a separate microservice.

## What this service does

- Capability Registry (ROADMAP §12)
- Task Engine (ROADMAP §22)
- Resource Registry + provider adapters (ROADMAP §36, §37)
- Approval Workflow + decision memory (ROADMAP §9, §27)
- Source Governance + learning loop (ROADMAP §7, §57)
- Unified Health Model (ROADMAP §41)
- Deployment Tracker (ROADMAP §40)
- MCP Skeleton — Observe/Analyze/Act (ROADMAP §45)
- Governance Engine — risk + budgets (ROADMAP §28, §54)

## Deploy to Render

1. Push this folder to a subdirectory of your backend repo (or its own repo)
2. render.com → New + → Blueprint → select repo → Render reads `render.yaml`
3. Set env vars in dashboard (see `.env.example`)
4. Verify: `curl https://your-service.onrender.com/health`

## Local test

```bash
pip install -r requirements.txt
export ADMIN_TOKEN="test-token"
python -m uvicorn main:app --port 8766
python scripts/test_all_endpoints.py --base http://127.0.0.1:8766 --admin-token test-token
```
