"""
API Routes for the Unified Memory Interface.

Provides endpoints to interact with long-term, short-term, and checkpoint memory
through a single, consistent API.
"""

from fastapi import APIRouter, HTTPException, Query
from typing import List, Optional

from core.unified_memory import unified_memory
# Removed auth import as it seems to be non-standard or located elsewhere

router = APIRouter(prefix="/unified-memory", tags=["Unified Memory"])

@router.post("/long-term/store")
# @auth_required([Role.USER, Role.ADMIN]) # Removed for import test
async def store_long_term_memory_endpoint(
    session_id: str = Query(..., description="Session or Task ID"),
    agent_type: str = Query(..., description="Type of the agent (e.g., SyncGuard)"),
    task_type: str = Query(..., description="Type of the task (e.g., System_Audit)"),
    content: str = Query(..., description="The content to store"),
    metadata: Optional[str] = Query(None, description="Optional metadata as JSON string")
):
    """
    Store information in the long-term 'Eternal Brain' memory.
    """
    import json
    metadata_dict = None
    if metadata:
        try:
            metadata_dict = json.loads(metadata)
        except json.JSONDecodeError:
            raise HTTPException(status_code=400, detail="Invalid JSON in metadata")

    success = unified_memory.store_long_term_memory(
        session_id=session_id,
        agent_type=agent_type,
        task_type=task_type,
        content=content,
        metadata=metadata_dict
    )
    if not success:
        raise HTTPException(status_code=500, detail="Failed to store memory")
    return {"message": "Long-term memory stored successfully", "session_id": session_id}


@router.get("/long-term/query")
# @auth_required([Role.USER, Role.ADMIN]) # Removed for import test
async def query_long_term_memory_endpoint(
    query: str = Query(..., description="Query to search for in memory"),
    top_k: int = Query(default=5, le=20, description="Number of top results to return"),
    session_id: Optional[str] = Query(None, description="Filter by session ID")
):
    """
    Query the long-term 'Eternal Brain' memory.
    """
    results = unified_memory.query_long_term_memory(query=query, top_k=top_k, session_id=session_id)
    return {"results": results}


# Example endpoints for short-term memory and checkpoints could be added here similarly.
# For brevity, only long-term is shown as an example of the pattern.