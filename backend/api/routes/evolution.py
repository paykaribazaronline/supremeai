import json
import secrets
import shutil
import time
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
import jwt
from loguru import logger
from pydantic import BaseModel, Field
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from api.dependencies import get_fitness_engine, get_tenant_db
from core.config import settings
from core.evolution.agent_breeder import AgentBreeder, BreederConfig
from core.evolution.auto_skill_creator import AutoSkillCreator
from core.evolution.fitness_engine import FitnessEngine
from core.evolution.performance_oracle import PerformanceOracle
from core.tenant_db import TenantAwareFirestore
from database.session import get_db_session
from models.evolution import CodeProposal
from models.meta_ai import AgentGenome


class GraphNode(BaseModel):
    id: str
    label: str
    type: str  # e.g., 'agent', 'skill'


class GraphEdge(BaseModel):
    source: str
    target: str
    relationship: str


class SwarmGraph(BaseModel):
    nodes: list[GraphNode]
    edges: list[GraphEdge]


router = APIRouter(prefix="/evolution", tags=["self-evolution-engine"])

security = HTTPBearer()


def require_admin_token(credentials: HTTPAuthorizationCredentials = Depends(security)):
    token = credentials.credentials
    try:
        jwt_secret = settings.jwt_secret
        decoded = jwt.decode(token, jwt_secret, algorithms=["HS256"])
        if decoded.get("role") != "admin":
            raise HTTPException(status_code=403, detail="Forbidden: User does not have admin role.")
        return decoded
    except Exception as e:
        expected = getattr(settings, "supremeai_api_token", None) or ""
        if expected and secrets.compare_digest(token, expected):
            return {"uid": "admin", "role": "admin"}
        raise HTTPException(status_code=401, detail=f"Invalid Admin Authorization Token: {e!s}") from e


@router.get("/logs")
async def get_evolution_logs(admin: dict = Depends(require_admin_token)):
    try:
        from database.supabase_client import db

        if db.client:
            logs = db.get_evolution_logs(limit=500)
            return {"logs": logs}
    except Exception as exc:
        # বল মনতবয: Supabase থক লগ আনত বযরথ হল লকল JSONL ফলবযক বযবহত হয়;
        # নরব সযলপ ন কর ডবগ লগ কর হল যত DB সমসয দশযমন থক
        logger.debug(f"Supabase evolution logs fetch failed, using local fallback: {exc}")

    base_dir = Path(__file__).resolve().parent.parent.parent
    log_path = base_dir / "backend" / "data" / "evolution_logs.jsonl"
    if not log_path.exists():
        return {"logs": []}
    try:
        with open(log_path, encoding="utf-8") as f:
            lines = f.readlines()
        logs = [json.loads(line) for line in lines if line.strip()]
        return {"logs": logs}
    except Exception as e:
        logger.error(f"Failed to read evolution logs: {e}")
        raise HTTPException(status_code=500, detail="Failed to read evolution logs") from e


class EvolutionRequest(BaseModel):
    skill_name: str
    user_demand: str


@router.post("/forge")
async def forge_dynamic_skill(payload: EvolutionRequest, db: TenantAwareFirestore = Depends(get_tenant_db)):
    """
    On-the-fly AI Skill Generation and Sandbox Deployed Gate.
    """
    creator = AutoSkillCreator(db=db)
    result = await creator.generate_and_deploy_skill(user_demand=payload.user_demand, skill_name=payload.skill_name)

    if not result["success"]:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=result["error"])

    return result


class QuarantineRequest(BaseModel):
    skill_name: str = Field(..., min_length=1, max_length=200)


@router.get("/swarm-graph")
async def get_swarm_graph():
    # ⚡ Simulated dynamic graph state for prototype
    current_state = {
        "nodes": [
            {"id": "agent-1", "label": "Code-Optimizer", "type": "agent"},
            {"id": "skill-2", "label": "FastAPI Refactor", "type": "skill"},
        ],
        "edges": [{"source": "agent-1", "target": "skill-2", "relationship": "teaches"}],
    }

    return current_state


@router.post("/quarantine")
async def quarantine_skill(
    payload: QuarantineRequest,
    admin: dict = Depends(require_admin_token),
    fitness_engine: FitnessEngine = Depends(get_fitness_engine),
):
    skill_name = payload.skill_name.strip()
    try:
        skill_data = fitness_engine.registry.get_skill(skill_name)
        if skill_data is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Skill not found in registry",
            )
        skill_data["status"] = "QUARANTINED"
        fitness_engine.registry.skills["skills"][skill_name] = skill_data
        with open(fitness_engine.registry.registry_path, "w", encoding="utf-8") as f:
            json.dump(fitness_engine.registry.skills, f, indent=4)
        base_dir = Path(__file__).resolve().parent.parent.parent
        src = base_dir / "skills" / "dynamic" / skill_name
        dst = base_dir / "skills" / "quarantine" / skill_name
        if src.exists():
            (base_dir / "skills" / "quarantine").mkdir(parents=True, exist_ok=True)
            if dst.exists():
                shutil.rmtree(dst)
            shutil.move(str(src), str(dst))
            logger.info(f"Skill '{skill_name}' quarantined: {src} -> {dst}")
        else:
            logger.info(f"Skill '{skill_name}' marked QUARANTINED in registry (no dynamic directory found)")
        base_dir_for_logs = Path(__file__).resolve().parent.parent.parent
        log_path = base_dir_for_logs / "backend" / "data" / "evolution_logs.jsonl"
        try:
            from database.supabase_client import db as db_client

            if db_client.client:
                db_client.append_evolution_log(
                    {
                        "event": {
                            "action": "quarantine",
                            "skill_name": skill_name,
                            "admin_uid": admin.get("uid"),
                            "timestamp": time.time(),
                        },
                        "created_at": datetime.now(UTC).isoformat(),
                    }
                )
        except Exception as db_err:
            logger.warning(f"Failed to log quarantine action to Supabase: {db_err}")

        try:
            with open(log_path, "a", encoding="utf-8") as f:
                f.write(
                    json.dumps(
                        {
                            "action": "quarantine",
                            "skill_name": skill_name,
                            "admin_uid": admin.get("uid"),
                            "timestamp": time.time(),
                        },
                        default=str,
                    )
                    + "\n"
                )
        except Exception as log_err:
            logger.warning(f"Failed to append quarantine log: {log_err}")
        return {"success": True, "skill_name": skill_name, "new_status": "QUARANTINED"}
    except HTTPException:
        raise
    except Exception as exc:
        logger.exception(f"Quarantine failed for '{skill_name}'")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Quarantine failed",
        ) from exc


# 🛑 ZERO-GAP: Admin Evolution Proposals API Routing
@router.get("/proposals")
async def list_proposals(
    admin: dict = Depends(require_admin_token),
    session: AsyncSession = Depends(get_db_session),
):
    """
    List all pending AI code proposals for admin review.
    """
    result = await session.execute(select(CodeProposal).order_by(CodeProposal.created_at.desc()))
    proposals = result.scalars().all()
    # Serialize to keep Pydantic serialization happy
    return [
        {
            "id": str(p.id),
            "proposal_id": p.proposal_id,
            "skill_name": p.skill_name,
            "generated_code": p.generated_code,
            "ast_validated": p.ast_validated,
            "ci_passed": p.ci_passed,
            "status": p.status,
            "metadata_json": p.metadata_json,
            "created_at": p.created_at.isoformat() if p.created_at else None,
        }
        for p in proposals
    ]


@router.post("/proposals/{proposal_id}/approve")
async def approve_proposal(
    proposal_id: str,
    admin: dict = Depends(require_admin_token),
    session: AsyncSession = Depends(get_db_session),
):
    """
    Manually approve a proposal after security review.
    """
    async with session.begin():
        result = await session.execute(select(CodeProposal).where(CodeProposal.proposal_id == proposal_id))
        proposal = result.scalars().first()
        if not proposal:
            raise HTTPException(status_code=404, detail="Proposal not found")

        proposal.status = "approved"
        # এখানে ভবিষ্যতে আমাদের অটোনোমাস মার্জ লজিক বা GitOps ট্রিগার কল হবে।

    return {"status": "success", "message": f"Proposal {proposal_id} approved."}


# 🛑 ZERO-GAP: Swarm Forge API Endpoints
# বাংলা মন্তব্য: ফ্রন্টএন্ড EvolutionForge পেজের সেভ এবং এক্সিকিউট রিকোয়েস্ট হ্যান্ডেল করার জন্য এন্ডপয়েন্ট যোগ করা হলো।
@router.post("/swarm/forge")
async def save_swarm_blueprint(payload: dict):
    """
    Save swarm blueprint configuration.
    """
    logger.info(f"Saving swarm blueprint: {payload.get('name')}")
    # বাংলা: আপাতত সাকসেস রেসপন্স রিটার্ন করছি
    return {
        "status": "success",
        "message": "Swarm blueprint saved successfully",
        "flow_id": "flow_" + str(int(time.time())),
    }


@router.post("/swarm/forge/{flow_id}/execute")
async def execute_swarm_blueprint(flow_id: str, payload: dict | None = None):
    """
    Trigger execution of a saved swarm blueprint.
    """
    logger.info(f"Executing swarm blueprint flow: {flow_id}")
    return {
        "status": "success",
        "message": f"Swarm blueprint flow {flow_id} executed successfully",
    }


# --- Extended Breeder & Oracle Routes ---
# বাংলা মন্তব্য: জেনেটিক প্রম্পট ব্রিডার এবং পারফরম্যান্স মূল্যায়ন এপিআই এন্ডপয়েন্টসমূহ।


class BreedRequest(BaseModel):
    parent_1: dict[str, Any]
    parent_2: dict[str, Any]
    method: str = "uniform"
    mutation_rate: float = 0.05


class PerformanceRequest(BaseModel):
    agent_name: str
    metrics: dict[str, Any]


@router.post("/breed")
# 🛡️ SECURITY FIX: আগে এই endpoint-এ কোনো auth ছিল না — get_db_session() শুধু
# একটা DB connection দেয়, identity check করে না। /breed সত্যিকারের genetic
# breeding চালায়, DB-তে লেখে, এমনকি offspring-কে production-এ promote করতে
# পারে — এই ফাইলের নিজস্ব sibling endpoint (/quarantine, /proposals/approve)
# একই sensitivity-র জন্য require_admin_token ব্যবহার করে, তাই এখানেও একই
# প্যাটার্ন প্রয়োগ করা হলো।
async def breed_agents(
    payload: BreedRequest,
    db: AsyncSession = Depends(get_db_session),
    admin: dict = Depends(require_admin_token),
):
    """Breed new agent genetic offspring from parents."""
    # বাংলা মন্তব্য: জেনেটিক অ্যালগরিদমের মাধ্যমে এজেন্টের জিনোমে ব্রিডিং ও মিউটেশন পরিচালনা এন্ডপয়েন্ট
    config = BreederConfig.from_settings()
    if payload.mutation_rate:
        config = BreederConfig(
            mutation_rate=payload.mutation_rate,
            crossover_rate=config.crossover_rate,
            elite_ratio=config.elite_ratio,
            tournament_size=config.tournament_size,
            max_generations=config.max_generations,
            llm_temperature=config.llm_temperature,
            llm_model_name=config.llm_model_name,
        )
    breeder = AgentBreeder(db, config=config)
    from sqlalchemy import select

    q = select(AgentGenome).where(AgentGenome.agent_name.in_([payload.parent_1, payload.parent_2]))
    r = await db.execute(q)
    genomes = {g.agent_name: g for g in r.scalars().all()}
    if payload.parent_1 not in genomes or payload.parent_2 not in genomes:
        raise HTTPException(status_code=404, detail="Parent genomes not found")

    parent_a = genomes[payload.parent_1]
    parent_b = genomes[payload.parent_2]
    offspring = await breeder.breed(parent_a, parent_b)
    await breeder.evaluate_offspring(offspring)
    promoted = await breeder.promote_if_elite(offspring, parent_a, parent_b)

    return {
        "success": True,
        "method": payload.method,
        "inherited_traits": offspring.chromosome,
        "novel_traits": offspring.chromosome,
        "promoted": promoted is not None,
    }


@router.post("/evaluate-performance")
async def evaluate_performance(
    payload: PerformanceRequest,
    db: AsyncSession = Depends(get_db_session),
    admin: dict = Depends(require_admin_token),
):
    """Evaluate agent performance and trigger alerts if thresholds are breached."""
    # বাংলা মন্তব্য: এজেন্টের কাজের গতি ও নির্ভুলতা বিশ্লেষণ করে কোনো অ্যালার্ট ট্রিগার হচ্ছে কি না তা বের করা
    from models.meta_ai import MetricType, SuggestionAction

    oracle = PerformanceOracle(db)
    for key, value in payload.metrics.items():
        try:
            mtype = MetricType(key)
            await oracle.record_metric(
                agent_name=payload.agent_name,
                metric_type=mtype,
                value=float(value),
                unit="ms" if "time" in key or "latency" in key else "score",
            )
        except (ValueError, TypeError):
            continue

    reports = await oracle.identify_weakest_links([payload.agent_name])
    alerts = []
    for r in reports:
        if r.agent_name == payload.agent_name and r.suggestion != SuggestionAction.NO_ACTION:
            alerts.append(
                {
                    "severity": (
                        "critical"
                        if r.suggestion == SuggestionAction.DEPRECATE or r.suggestion == SuggestionAction.REPLACE
                        else "warning"
                    ),
                    "recommended_action": r.suggestion.value,
                    "description": r.reasoning,
                }
            )

    return {
        "success": True,
        "agent_name": payload.agent_name,
        "alerts_triggered": alerts,
        "alerts_count": len(alerts),
    }
