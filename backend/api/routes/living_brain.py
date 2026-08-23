# backend/api/routes/living_brain.py
"""
Living Brain Dashboard API
===========================

Real-time observability endpoints for SupremeAI's "brain" health.

Provides visibility into:
- ai_memory/pgvector status and contents
- SupremeLearningEngine learning progress
- Self-sufficiency rate over time
- Cost per hour / provider breakdown
- Pattern confidence evolution

This is the "pulse check" for whether AI is truly alive and learning.

Endpoints:
GET /api/living-brain/status - Overall brain health summary
GET /api/living-brain/metrics - Detailed metrics with history
GET /api/living-brain/timeline - Learning timeline events
GET /api/living-brain/costs - Cost breakdown by provider/time
POST /api/living-brain/query - Query learned patterns

Author: System Lead Engineer
Version: 1.0.0
"""

from __future__ import annotations

import time
from datetime import datetime, timedelta
from typing import Any, Dict, List, Optional
from loguru import logger
from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel

# Import brain components
try:
    from backend.brain.supreme_learning_engine import get_learning_engine
    LEARNING_ENGINE_AVAILABLE = True
except ImportError:
    LEARNING_ENGINE_AVAILABLE = False
    logger.warning("⚠️ SupremeLearningEngine not available")

try:
    from backend.memory.supabase_store import SupabaseStore
    MEMORY_STORE_AVAILABLE = True
except ImportError:
    MEMORY_STORE_AVAILABLE = False
    logger.warning("⚠️ Memory store not available")

try:
    from backend.brain.economic_optimizer import EconomicOptimizer
    ECON_OPTIMIZER_AVAILABLE = True
except ImportError:
    ECON_OPTIMIZER_AVAILABLE = False


router = APIRouter(prefix="/api/living-brain", tags=["living-brain"])


# ---------------------------------------------------------------------------
# Response Models
# ---------------------------------------------------------------------------

class BrainStatus(BaseModel):
    """Overall brain health status."""
    is_alive: bool
    overall_health: str  # "healthy", "degraded", "critical"
    uptime_hours: float
    last_learning_activity: Optional[str]
    components: Dict[str, Any]


class LearningMetrics(BaseModel):
    """Learning engine metrics."""
    total_patterns_learned: int
    self_sufficiency_rate: float
    patterns_by_domain: Dict[str, int]
    avg_confidence: float
    recent_learning_velocity: float  # patterns/hour


class MemoryMetrics(BaseModel):
    """Memory system metrics."""
    provider: str  # "supabase", "sqlite", "hybrid"
    total_facts_stored: int
    pgvector_enabled: bool
    embeddings_generated: int
    search_accuracy_estimate: float


class CostBreakdown(BaseModel):
    """Cost metrics."""
    total_cost_today_usd: float
    cost_by_provider: Dict[str, float]
    cost_by_hour: List[Dict[str, Any]]
    tokens_consumed_today: int
    avg_cost_per_1k_tokens: float


# ---------------------------------------------------------------------------
# Main Endpoints
# ---------------------------------------------------------------------------

@router.get("/status", response_model=BrainStatus)
async def get_brain_status():
    """
    Get overall brain health status.
    
    This is the main "pulse check" endpoint.
    Returns quickly to indicate if AI systems are operational.
    """
    start_time = time.time()
    
    # Initialize component statuses
    components = {
        "learning_engine": {"status": "unknown", "details": {}},
        "memory": {"status": "unknown", "details": {}},
        "economic_optimizer": {"status": "unknown", "details": {}},
        "digital_twin": {"status": "not_configured"},
    }
    
    # Check Learning Engine
    if LEARNING_ENGINE_AVAILABLE:
        try:
            engine = get_learning_engine()
            stats = engine.get_stats()
            
            components["learning_engine"] = {
                "status": "healthy",
                "details": {
                    "patterns_learned": stats.get("total_patterns_in_db", 0),
                    "self_sufficiency_rate": f"{stats.get('self_sufficiency_rate', 0):.1f}%",
                    "total_interactions": stats.get("total_interactions", 0),
                    "knowledge_graph_nodes": stats.get("knowledge_graph_nodes", 0),
                }
            }
        except Exception as e:
            components["learning_engine"] = {"status": "error", "error": str(e)}
    else:
        components["learning_engine"]["status"] = "unavailable"
    
    # Check Memory Store
    if MEMORY_STORE_AVAILABLE:
        try:
            # Try to get or create a store instance
            store = SupabaseStore()
            mem_stats = store.get_stats()
            
            components["memory"] = {
                "status": "healthy" if mem_stats.get("provider") else "degraded",
                "details": {
                    "provider": mem_stats.get("provider", "unknown"),
                    "pgvector_enabled": mem_stats.get("pgvector_enabled", False),
                    "total_queries": mem_stats.get("total_queries", 0),
                    "pgvector_success_rate": (
                        f"{mem_stats['pgvector_success'] / max(1, mem_stats['total_queries']) * 100:.1f}%"
                        if mem_stats.get("total_queries", 0) > 0 else "N/A"
                    ),
                }
            }
        except Exception as e:
            components["memory"] = {"status": "error", "error": str(e)}
    else:
        components["memory"]["status"] = "unavailable"
    
    # Check Economic Optimizer
    if ECON_OPTIMIZER_AVAILABLE:
        try:
            optimizer = EconomicOptimizer()
            opt_stats = optimizer.get_stats()
            
            components["economic_optimizer"] = {
                "status": "healthy",
                "details": {
                    "total_optimizations": opt_stats.get("total_routes_optimized", 0),
                    "cost_saved_usd": opt_stats.get("total_cost_saved", 0.0),
                    "active_providers": len(opt_stats.get("provider_stats", {})),
                }
            }
        except Exception as e:
            components["economic_optimizer"] = {"status": "error", "error": str(e)}
    
    # Determine overall health
    healthy_count = sum(
        1 for c in components.values() 
        if isinstance(c, dict) and c.get("status") == "healthy"
    )
    total_checked = sum(
        1 for c in components.values() 
        if isinstance(c, dict) and c.get("status") != "not_configured"
    )
    
    if healthy_count == total_checked:
        overall_health = "healthy"
    elif healthy_count >= total_checked * 0.5:
        overall_health = "degraded"
    else:
        overall_health = "critical"
    
    return BrainStatus(
        is_alive=overall_health != "critical",
        overall_health=overall_health,
        uptime_hours=(time.time() - _get_startup_time()) / 3600,
        last_learning_activity=_get_last_learning_time(),
        components=components,
    )


@router.get("/metrics")
async def get_detailed_metrics(
    hours: int = Query(default=24, ge=1, le=168),  # Max 7 days
):
    """Get detailed metrics over time period."""
    metrics = {
        "timestamp": datetime.now().isoformat(),
        "period_hours": hours,
        "learning": {},
        "memory": {},
        "costs": {},
    }
    
    # Learning metrics
    if LEARNING_ENGINE_AVAILABLE:
        try:
            engine = get_learning_engine()
            stats = engine.get_stats()
            
            metrics["learning"] = LearningMetrics(
                total_patterns_learned=stats.get("total_patterns_in_db", 0),
                self_sufficiency_rate=stats.get("self_sufficiency_rate", 0.0),
                patterns_by_domain=_get_patterns_by_domain(engine),
                avg_confidence=_get_avg_confidence(engine),
                recent_learning_velocity=_calculate_learning_velocity(stats, hours),
            ).model_dump()
        except Exception as e:
            metrics["learning"] = {"error": str(e)}
    
    # Memory metrics
    if MEMORY_STORE_AVAILABLE:
        try:
            store = SupabaseStore()
            stats = store.get_stats()
            
            metrics["memory"] = MemoryMetrics(
                provider=stats.get("provider", "unknown"),
                total_facts_stored=stats.get("pgvector_success", 0) + stats.get("sqlite_fallback", 0),
                pgvector_enabled=stats.get("pgvector_enabled", False),
                embeddings_generated=stats.get("embeddings_generated", 0),
                search_accuracy_estimate=_estimate_search_accuracy(stats),
            ).model_dump()
        except Exception as e:
            metrics["memory"] = {"error": str(e)}
    
    return metrics


@router.get("/timeline")
async def get_learning_timeline(
    limit: int = Query(default=50, ge=1, le=200),
):
    """
    Get recent learning events timeline.
    
    Shows what the AI has been learning recently.
    """
    events = []
    
    if LEARNING_ENGINE_AVAILABLE:
        try:
            engine = get_learning_engine()
            # Get recent patterns from database
            conn = engine.db_path  # Access the SQLite DB
            import sqlite3
            
            db_conn = sqlite3.connect(conn)
            cursor = db_conn.cursor()
            
            cursor.execute("""
                SELECT pattern_id, domain, complexity, confidence, 
                       success_count, created_at, last_used
                FROM patterns 
                ORDER BY created_at DESC 
                LIMIT ?
            """, (limit,))
            
            for row in cursor.fetchall():
                events.append({
                    "pattern_id": row[0],
                    "domain": row[1],
                    "complexity": row[2],
                    "confidence": row[3],
                    "success_count": row[4],
                    "learned_at": row[5],
                    "last_used": row[6],
                    "event_type": "pattern_learned",
                })
            
            db_conn.close()
        except Exception as e:
            logger.error(f"Failed to get timeline: {e}")
    
    return {
        "total_events": len(events),
        "events": events,
        "query_time": datetime.now().isoformat(),
    }


@router.post("/query")
async def query_learned_patterns(query: str, limit: int = 5):
    """
    Query the learned knowledge base.
    
    This shows what the AI already knows about a topic.
    """
    results = []
    
    if MEMORY_STORE_AVAILABLE:
        try:
            store = SupabaseStore()
            facts = store.search_facts(query)
            
            for fact in facts[:limit]:
                results.append({
                    "content": fact.get("content", fact.get("text", ""))[:500],
                    "confidence": fact.get("confidence", 0.0),
                    "source": fact.get("source", "unknown"),
                    "learned_at": fact.get("created_at", ""),
                })
        except Exception as e:
            logger.error(f"Query failed: {e}")
    
    return {
        "query": query,
        "results_found": len(results),
        "results": results,
    }


# ---------------------------------------------------------------------------
# Helper Functions
# ---------------------------------------------------------------------------

_startup_time: float = time.time()


def _get_startup_time() -> float:
    """Get application startup time."""
    global _startup_time
    return _startup_time


def _get_last_learning_time() -> Optional[str]:
    """Get timestamp of most recent learning activity."""
    if LEARNING_ENGINE_AVAILABLE:
        try:
            engine = get_learning_engine()
            conn = engine.db_path
            import sqlite3
            
            db_conn = sqlite3.connect(conn)
            cursor = db_conn.cursor()
            cursor.execute("SELECT MAX(created_at) FROM patterns")
            result = cursor.fetchone()[0]
            db_conn.close()
            
            return result
        except Exception:
            import logging
            logging.getLogger(__name__).warning('Ignored exception')
    return None


def _get_patterns_by_domain(engine) -> Dict[str, int]:
    """Get pattern count grouped by domain."""
    try:
        import sqlite3
        conn = sqlite3.connect(engine.db_path)
        cursor = conn.cursor()
        cursor.execute("SELECT domain, COUNT(*) FROM patterns GROUP BY domain")
        result = dict(cursor.fetchall())
        conn.close()
        return result
    except Exception:
        return {}


def _get_avg_confidence(engine) -> float:
    """Get average pattern confidence."""
    try:
        import sqlite3
        conn = sqlite3.connect(engine.db_path)
        cursor = conn.cursor()
        cursor.execute("SELECT AVG(confidence) FROM patterns")
        result = cursor.fetchone()[0] or 0.0
        conn.close()
        return round(result, 3)
    except Exception:
        return 0.0


def _calculate_learning_velocity(stats: dict, hours: int) -> float:
    """Calculate patterns learned per hour."""
    total = stats.get("patterns_learned", 0)
    return round(total / max(1, hours), 2)


def _estimate_search_accuracy(mem_stats: dict) -> float:
    """Estimate search accuracy from pgvector success rate."""
    total = mem_stats.get("total_queries", 0)
    success = mem_stats.get("pgvector_success", 0)
    
    if total == 0:
        return 0.0
    
    return round(success / total, 3)
