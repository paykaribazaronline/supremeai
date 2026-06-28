import json

from fastapi import APIRouter
from fastapi import HTTPException
from fastapi import Request
from pydantic import BaseModel

from database.supabase_client import db
from tools.marketplace_agent import MarketplaceAgent
from tools.resource_catalog import ResourceCatalog


router = APIRouter(prefix="/marketplace", tags=["marketplace"])
marketplace_agent = MarketplaceAgent()

ALLOWED_CATALOG_SOURCES = {
    "awesome-selfhosted",
    "awesome-python",
    "ossinsight",
    "libraries.io",
}
DEFAULT_CATALOG_SOURCES = ["awesome-selfhosted", "awesome-python"]


def get_enabled_catalog_sources() -> list[str]:
    if not db.client:
        return DEFAULT_CATALOG_SOURCES

    try:
        enabled = db.get_config("marketplace.resource_sources")
    except Exception:
        enabled = None

    if isinstance(enabled, str):
        try:
            enabled = json.loads(enabled)
        except Exception:
            enabled = [item.strip() for item in enabled.split(",") if item.strip()]

    if not isinstance(enabled, list):
        return DEFAULT_CATALOG_SOURCES

    enabled_sources = [src for src in enabled if src in ALLOWED_CATALOG_SOURCES]
    return enabled_sources or DEFAULT_CATALOG_SOURCES


def filter_requested_catalog_sources(
    categories: list[str], enabled_sources: list[str]
) -> list[str]:
    return [c for c in categories if c in enabled_sources]


class SearchRequest(BaseModel):
    query: str
    categories: list[str] | None = None
    filters: dict | None = None


class InstallRequest(BaseModel):
    tool_id: str
    target_environment: str
    sandbox: bool = True


@router.post("/search")
async def search_marketplaces(payload: SearchRequest, request: Request):
    try:
        categories = payload.categories if payload.categories is not None else []
        filters = payload.filters if payload.filters is not None else {}

        results = marketplace_agent.search_marketplaces(
            payload.query, categories, filters
        )

        enabled_sources = get_enabled_catalog_sources()
        catalog_sources = filter_requested_catalog_sources(categories, enabled_sources)
        if not catalog_sources and not categories:
            catalog_sources = enabled_sources

        http_client = getattr(request.app.state, "http_client", None)
        async with ResourceCatalog(http_client=http_client) as catalog:
            resource_results = await catalog.search(
                payload.query, sources=catalog_sources, limit=5
            )

        if resource_results:
            results.extend(resource_results)

        return {"status": "success", "tools": results}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e)) from e


@router.post("/install")
async def install_tool(payload: InstallRequest):
    try:
        res = marketplace_agent.install_tool(
            payload.tool_id, payload.target_environment, payload.sandbox
        )
        return res
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e)) from e
