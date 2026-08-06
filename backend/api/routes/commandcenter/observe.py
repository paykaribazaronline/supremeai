from fastapi import APIRouter

router = APIRouter(prefix="/admin-api/commandcenter", tags=["Command Center"])


@router.get("/observe/metrics")
async def get_metrics():
    return {}


@router.get("/observe/logs")
async def get_logs():
    return []


@router.get("/observe/events")
async def get_events():
    return []


@router.get("/observe/ci")
async def get_ci():
    return []


@router.get("/observe/health")
async def get_health():
    return {
        "gcp": {"status": "unknown"},
        "railway": {"status": "unknown"},
        "render": {"status": "unknown"},
        "overall_health_percent": 0,
    }


@router.get("/observe/traffic")
async def get_traffic():
    return {"current_rps": 0, "window_30min": [], "distribution": {}}
