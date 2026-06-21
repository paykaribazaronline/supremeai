from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from tools.github_agent import GitHubAgent
from tools.repo_discovery_agent import RepoDiscoveryAgent

router = APIRouter(prefix="/github", tags=["github"])
github_agent = GitHubAgent()
repo_discovery_agent = RepoDiscoveryAgent()

class ConnectRequest(BaseModel):
    installation_id: Optional[str] = None
    repo_owner: str
    repo_name: str

class ImproveRequest(BaseModel):
    repo: str
    branch: str
    improvement_type: str

class PushRequest(BaseModel):
    branch: str
    commit_message: str
    files_changed: List[str]

class DiscoverRequest(BaseModel):
    requirement: str
    tech_stack: List[str]
    criteria: dict

class ImplementRequest(BaseModel):
    repo_url: str
    integration_method: str
    target_project: str

@router.post("/connect")
async def connect_repo(payload: ConnectRequest):
    try:
        github_agent.connect_repo(payload.repo_owner, payload.repo_name, payload.installation_id)
        return {"status": "success", "message": f"Connected to {payload.repo_owner}/{payload.repo_name}"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/improve")
async def improve_repo(payload: ImproveRequest):
    try:
        analysis = github_agent.analyze_repo(payload.repo)
        return {"status": "success", "analysis": analysis}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/push")
async def push_improvements(payload: PushRequest):
    try:
        # Enforce PR governance via agent
        improvements = {f: "Optimized" for f in payload.files_changed}
        res = github_agent.create_improvement_pr("dummy/repo", improvements, "main")
        return res
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
