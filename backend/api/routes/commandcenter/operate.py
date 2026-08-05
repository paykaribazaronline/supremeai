from fastapi import APIRouter

router = APIRouter(prefix="/admin-api/commandcenter", tags=["Command Center"])


@router.get("/operate/agents")
async def list_agents():
    return []


@router.get("/operate/swarm")
async def get_swarm():
    return {"nodes": [], "edges": []}


@router.get("/operate/tasks")
async def list_tasks():
    return []


@router.get("/operate/sessions")
async def list_sessions():
    return []


@router.get("/operate/tenants")
async def list_tenants():
    return []
