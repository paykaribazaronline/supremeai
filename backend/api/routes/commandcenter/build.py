from fastapi import APIRouter

router = APIRouter(prefix="/admin-api/commandcenter", tags=["Command Center"])


@router.get("/build/router")
async def get_router():
    return {"provider_order": [], "cost_quality_preference": 0.5}


@router.get("/build/providers")
async def list_providers():
    return []


@router.get("/build/skills")
async def list_skills():
    return []


@router.get("/build/memory")
async def get_memory():
    return {"banks": [], "semantic_cache_hit_rate": 0, "tokens_saved": 0}


@router.get("/build/knowledge")
async def get_knowledge():
    return {"docs_count": 0, "rag_index_status": "indexed"}
