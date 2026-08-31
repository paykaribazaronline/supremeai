"""Standalone ecosystem API server — lightweight, no full supremeai deps."""
import os
import sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import FastAPI
from contextlib import asynccontextmanager

@asynccontextmanager
async def lifespan(app):
    from ecosystem import (
        get_capability_registry, get_task_engine, get_resource_registry,
        get_approval_workflow, get_governance_engine, get_health_aggregator,
        get_deployment_tracker, get_learning_loop,
    )
    get_task_engine(); get_approval_workflow(); get_deployment_tracker()
    get_health_aggregator(); get_governance_engine(); get_learning_loop()
    # seed
    try:
        sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'scripts'))
        from seed_ecosystem import seed_capabilities, seed_policies
        seed_capabilities(); seed_policies()
    except: pass
    yield

app = FastAPI(title="Ecosystem Test", lifespan=lifespan)

@app.get("/health")
def health(): return {"status": "ok", "service": "ecosystem-test"}

@app.get("/api/v1/ecosystem/capabilities")
def list_caps():
    from ecosystem import get_capability_registry
    return [c.model_dump() for c in get_capability_registry().list()]

@app.get("/api/v1/ecosystem/mcp/manifest")
def mcp_manifest():
    from ecosystem import get_mcp_skeleton
    return get_mcp_skeleton().manifest()

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=int(os.getenv("PORT", "8000")))
