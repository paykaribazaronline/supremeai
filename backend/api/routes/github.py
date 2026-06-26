from fastapi import APIRouter
from fastapi import Depends
from fastapi import HTTPException
from firebase_admin import firestore
from pydantic import BaseModel

from api.dependencies import get_tenant_db
from tools.github_agent import GitHubAgent
from tools.repo_discovery_agent import RepoDiscoveryAgent


router = APIRouter(prefix="/github", tags=["github"])
github_agent = GitHubAgent()
repo_discovery_agent = RepoDiscoveryAgent()


def _resolve_repo(payload_repo: str | None, db: firestore.Client) -> str:
    repo = payload_repo
    if not repo or not repo.strip():
        profile = db.get_tenant_profile() or {}
        repo = profile.get("github_repo")
    if not repo or not repo.strip():
        raise HTTPException(
            status_code=400,
            detail="Repository not connected. Please connect your GitHub repository or provide one in the request.",
        )
    return repo.strip()


class ConnectRequest(BaseModel):
    installation_id: str | None = None
    repo_owner: str
    repo_name: str


class ImproveRequest(BaseModel):
    repo: str | None = None
    branch: str
    improvement_type: str


class PushRequest(BaseModel):
    repo: str | None = None
    branch: str = "main"
    commit_message: str = "AI: Automated improvements"
    files_changed: list[str]


class DiscoverRequest(BaseModel):
    requirement: str
    tech_stack: list[str]
    criteria: dict


class ImplementRequest(BaseModel):
    repo_url: str
    integration_method: str
    target_project: str


@router.post("/connect")
async def connect_repo(payload: ConnectRequest, db=Depends(get_tenant_db)):
    try:
        inst_id = payload.installation_id if payload.installation_id is not None else ""
        github_agent.connect_repo(payload.repo_owner, payload.repo_name, inst_id)
        # ট্যানান্টের প্রোফাইলে গিটহাব রেপো কানেকশন সেভ করা হচ্ছে
        tenant_ref = db.tenant_root
        tenant_ref.set(
            {"github_repo": f"{payload.repo_owner}/{payload.repo_name}"}, merge=True
        )
        return {
            "status": "success",
            "message": f"Connected to {payload.repo_owner}/{payload.repo_name}",
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/improve")
async def improve_repo(payload: ImproveRequest, db=Depends(get_tenant_db)):
    try:
        repo = _resolve_repo(payload.repo, db)
        analysis = github_agent.analyze_repo(repo)
        return {"status": "success", "analysis": analysis}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/push")
async def push_improvements(payload: PushRequest, db=Depends(get_tenant_db)):
    try:
        repo = _resolve_repo(payload.repo, db)
        improvements = dict.fromkeys(payload.files_changed, "Optimized")
        res = github_agent.create_improvement_pr(repo, improvements, payload.branch)
        return res
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/discover")
async def discover_repos(payload: DiscoverRequest):
    try:
        repos = repo_discovery_agent.discover_repos(
            payload.requirement, payload.tech_stack, payload.criteria
        )
        return {"status": "success", "repos": repos}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/implement")
async def implement_repo(payload: ImplementRequest):
    try:
        res = repo_discovery_agent.implement_repo(
            payload.repo_url, payload.integration_method, payload.target_project
        )
        return res
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
