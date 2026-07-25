from typing import Any

from fastapi import APIRouter, HTTPException
from src.agents.syncguard.syncguard_agent import SyncGuardAgent

router = APIRouter(
    prefix="/syncguard",
    tags=["SyncGuard"],
    responses={404: {"description": "Not found"}},
)


@router.post("/audit", response_model=dict[str, Any])
async def trigger_audit() -> dict[str, Any]:
    """
    Manually trigger a full infrastructure and configuration audit.
    Can be called via the 'Recheck Safeguard' button on Web, Mobile, or VS Code.
    """
    try:
        agent = SyncGuardAgent()
        report = await agent.run_full_audit()

        # If the audit failed, we still return the report (maybe 200 OK or 400 depending on design)
        # Returning 200 OK so the client can parse the issues.
        return report
    except Exception as e:  # noqa: BLE001
        raise HTTPException(
            status_code=500, detail=f"Audit execution failed: {str(e)}"
        ) from e
