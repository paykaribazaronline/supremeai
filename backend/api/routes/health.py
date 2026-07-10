from fastapi import APIRouter, Depends, Query
from typing import List, Dict, Any
from core.services import registry
from loguru import logger

router = APIRouter(prefix="/api/health", tags=["health-monitor"])

@router.get("/agents")
async def get_agents_health(agent_ids: List[str] = Query(default=[])) -> Dict[str, Any]:
    """
    Fetch real-time health status for a list of agents using Upstash MGET.
    Expects a query parameter like: ?agent_ids=agent-1&agent_ids=agent-2
    """
    if not agent_ids:
        return {}
        
    redis_manager = registry.get_service("redis_manager")
    if not redis_manager:
        logger.warning("Redis manager not available. Returning empty health status.")
        return {agent_id: {"status": "dead", "latency": 0} for agent_id in agent_ids}
        
    try:
        health_data = await redis_manager.get_agents_health(agent_ids)
        return health_data
    except Exception as e:
        logger.error(f"Error fetching agent health: {e}")
        return {agent_id: {"status": "dead", "latency": 0} for agent_id in agent_ids}
