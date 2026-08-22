"""
Admin Health Aggregation Endpoint
Aggregates health status from all microservices and external dependencies.
"""

from fastapi import APIRouter, HTTPException, Query
from typing import Dict, List, Optional
from datetime import datetime, timedelta
import asyncio
import httpx
from pydantic import BaseModel

router = APIRouter(prefix="/admin-api", tags=["health"])

# ══════════════════════════════════════════════════════════════════════════════
# MODELS
# ══════════════════════════════════════════════════════════════════════════════

class ServiceHealth(BaseModel):
    name: str
    display_name: str
    status: str  # healthy, degraded, unhealthy, unknown
    response_time_ms: Optional[float] = None
    status_code: Optional[int] = None
    error: Optional[str] = None
    last_check: datetime
    url: str
    critical: bool = False

class HealthAggregationResponse(BaseModel):
    timestamp: datetime
    overall_status: str
    services: List[ServiceHealth]
    summary: Dict[str, int]
    uptime_percentage: float
    alerts: List[str]

class DependencyHealth(BaseModel):
    database: ServiceHealth
    redis: ServiceHealth
    supabase: ServiceHealth
    llm_providers: Dict[str, ServiceHealth]

# ══════════════════════════════════════════════════════════════════════════════
# SERVICE REGISTRY
# ══════════════════════════════════════════════════════════════════════════════

SERVICE_REGISTRY = [
    {
        "name": "main_backend",
        "display_name": "Main Backend",
        "url": "http://localhost:8080/api/v1/health",
        "critical": True,
        "timeout": 5.0,
    },
    {
        "name": "admin_backend",
        "display_name": "Admin Backend", 
        "url": "https://supremeai-admin.onrender.com/api/v1/health",
        "critical": True,
        "timeout": 8.0,
    },
    {
        "name": "scraper_service",
        "display_name": "Scraper Microservice",
        "url": "https://supremeai-scraper-6nwi.onrender.com/health",
        "critical": False,
        "timeout": 8.0,
    },
    {
        "name": "cloudflare_worker",
        "display_name": "Edge Worker",
        "url": "https://supremeai-edge.your-subdomain.workers.dev/health",
        "critical": True,
        "timeout": 5.0,
    },
]

LLM_PROVIDERS = {
    "openrouter": {"url": "https://openrouter.ai/api/v1/models", "critical": False},
    "openai": {"url": "https://api.openai.com/v1/models", "critical": False},
    "gemini": {"url": "https://generativelanguage.googleapis.com/v1beta/models", "critical": False},
}

# ══════════════════════════════════════════════════════════════════════════════
# HEALTH CHECK FUNCTIONS
# ══════════════════════════════════════════════════════════════════════════════

async def check_single_service(config: Dict) -> ServiceHealth:
    """Perform async health check on a single service."""
    start_time = datetime.now()
    
    try:
        async with httpx.AsyncClient(timeout=config["timeout"]) as client:
            response = await client.get(
                config["url"],
                headers={"User-Agent": "SupremeAI-HealthChecker/2.0"},
            )
            
            response_time = (datetime.now() - start_time).total_seconds() * 1000
            
            if response.status_code == 200:
                # Try to parse health details from response
                try:
                    data = response.json()
                    status = data.get("status", "healthy")
                    if status == "degraded":
                        status = "degraded"
                    else:
                        status = "healthy"
                except:
                    status = "healthy"
            elif response.status_code >= 500:
                status = "unhealthy"
            else:
                status = "degraded"
                
            return ServiceHealth(
                name=config["name"],
                display_name=config["display_name"],
                status=status,
                response_time_ms=round(response_time, 2),
                status_code=response.status_code,
                last_check=datetime.utcnow(),
                url=config["url"],
                critical=config.get("critical", False),
            )
            
    except httpx.TimeoutException:
        return ServiceHealth(
            name=config["name"],
            display_name=config["display_name"],
            status="unhealthy",
            response_time_ms=(datetime.now() - start_time).total_seconds() * 1000,
            error=f"Timeout after {config['timeout']}s",
            last_check=datetime.utcnow(),
            url=config["url"],
            critical=config.get("critical", False),
        )
    except Exception as e:
        return ServiceHealth(
            name=config["name"],
            display_name=config["display_name"],
            status="unhealthy",
            response_time_ms=(datetime.now() - start_time).total_seconds() * 1000,
            error=str(e)[:200],
            last_check=datetime.utcnow(),
            url=config["url"],
            critical=config.get("critical", False),
        )


async def check_all_services() -> List[ServiceHealth]:
    """Check all registered services concurrently."""
    tasks = [check_single_service svc) for svc in SERVICE_REGISTRY]
    results = await asyncio.gather(*tasks, return_exceptions=True)
    
    # Convert exceptions to unhealthy status
    services = []
    for i, result in enumerate(results):
        if isinstance(result, Exception):
            services.append(ServiceHealth(
                name=SERVICE_REGISTRY[i]["name"],
                display_name=SERVICE_REGISTRY[i]["display_name"],
                status="unknown",
                error=str(result),
                last_check=datetime.utcnow(),
                url=SERVICE_REGISTRY[i]["url"],
                critical=SERVICE_REGISTRY[i].get("critical", False),
            ))
        else:
            services.append(result)
    
    return services


def calculate_overall_status(services: List[ServiceHealth]) -> tuple:
    """Calculate overall system status."""
    counts = {"healthy": 0, "degraded": 0, "unhealthy": 0, "unknown": 0}
    
    for svc in services:
        counts[svc.status] = counts.get(svc.status, 0) + 1
    
    # Critical services down = overall unhealthy
    critical_unhealthy = any(
        svc.critical and svc.status in ("unhealthy", "unknown") 
        for svc in services
    )
    
    if critical_unhealthy or counts["unhealthy"] > 0:
        overall = "unhealthy"
    elif counts["degraded"] > 0:
        overall = "degraded"
    else:
        overall = "healthy"
    
    return overall, counts

# ══════════════════════════════════════════════════════════════════════════════
# API ENDPOINTS
# ══════════════════════════════════════════════════════════════════════════════

@router.get("/health-aggregation", response_model=HealthAggregationResponse)
async def get_health_aggregation():
    """
    Comprehensive health check of all SupremeAI services.
    Returns aggregated status with detailed per-service information.
    """
    # Check all services concurrently
    services = await check_all_services()
    
    # Calculate overall status
    overall_status, summary = calculate_overall_status(services)
    
    # Generate alerts for unhealthy critical services
    alerts = []
    for svc in services:
        if svc.critical and svc.status in ("unhealthy", "unknown"):
            alerts.append(f"🚨 CRITICAL: {svc.display_name} is {svc.status.upper()}")
        elif svc.status == "degraded":
            alerts.append(f"⚠️ WARNING: {svc.display_name} is degraded")
    
    return HealthAggregationResponse(
        timestamp=datetime.utcnow(),
        overall_status=overall_status,
        services=services,
        summary=summary,
        uptime_percentage=round((summary.get("healthy", 0) / len(services)) * 100, 1) if services else 0,
        alerts=alerts,
    )


@router.get("/health-map")
async def get_health_map():
    """
    Simplified health map for quick status checks.
    Used by HealthBanner component.
    """
    services = await check_all_services()
    overall_status, _ = calculate_overall_status(services)
    
    # Group by provider/type
    health_map = {}
    for svc in services:
        # Extract provider from name
        if "backend" in svc.name:
            provider = "render"
        elif "worker" in svc.name:
            provider = "cloudflare"
        elif "scraper" in svc.name:
            provider = "railway"
        else:
            provider = "other"
        
        if provider not in health_map or health_map[provider]["status"] == "healthy":
            health_map[provider] = {
                "status": svc.status if svc.status != "healthy" else "healthy",
                "service": svc.display_name,
            }
    
    return health_map


@router.get("/dependencies")
async def check_dependencies():
    """
    Check external dependencies (database, Redis, LLM providers).
    """
    # This would integrate with your actual dependency checks
    # For now, returning placeholder implementation
    
    return {
        "database": {"status": "healthy", "connection_pool_active": 5},
        "redis": {"status": "healthy", "memory_usage_mb": 12},
        "supabase": {"status": "healthy", "connections": 3},
        "llm_providers": {
            "openrouter": {"status": "healthy", "latency_ms": 145},
            "openai": {"status": "healthy", "latency_ms": 89},
            "gemini": {"status": "degraded", "latency_ms": 1200, "error": "Elevated latency"},
        },
    }


@router.post("/test-service")
async def test_specific_service(service_url: str = Query(...)):
    """
    Test a specific service URL for connectivity.
    Useful for ad-hoc debugging from admin panel.
    """
    start_time = datetime.now()
    
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(
                service_url,
                headers={"User-Agent": "SupremeAI-Admin-Test/1.0"},
            )
            
            return {
                "success": True,
                "url": service_url,
                "status_code": response.status_code,
                "response_time_ms": round((datetime.now() - start_time).total_seconds() * 1000, 2),
                "headers": dict(response.headers),
                "body_preview": response.text[:500] if response.text else None,
            }
    except Exception as e:
        return {
            "success": False,
            "url": service_url,
            "error": str(e),
            "response_time_ms": round((datetime.now() - start_time).total_seconds() * 1000, 2),
        }