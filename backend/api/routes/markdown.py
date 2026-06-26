import time
import uuid
from typing import Any

from fastapi import APIRouter
from fastapi import BackgroundTasks
from fastapi import HTTPException
from pydantic import BaseModel

from database.supabase_client import db as supabase_db
from tools.codebase_exporter import export_codebase_to_markdown


router = APIRouter(prefix="/markdown", tags=["markdown"])

# In-memory store for jobs
jobs_db: dict[str, dict[str, Any]] = {}


class MarkdownExportRequest(BaseModel):
    root_dir: str = "."
    time_since: str | None = None
    time_until: str | None = None
    git_diff_only: bool = False
    clone_url: str | None = None


class CompareRequest(BaseModel):
    clone_url: str | None = None
    root_dir: str = "."
    range_a_since: str | None = None
    range_a_until: str | None = None
    range_b_since: str | None = None
    range_b_until: str | None = None


class ShareRequest(BaseModel):
    markdown: str
    target_ai: str = "claude"


async def run_export_task(job_id: str, payload: MarkdownExportRequest):
    jobs_db[job_id]["status"] = "cloning"
    jobs_db[job_id]["progress"] = 30
    try:
        markdown_content = await export_codebase_to_markdown(
            root_dir=payload.root_dir,
            time_since=payload.time_since,
            time_until=payload.time_until,
            git_diff_only=payload.git_diff_only,
            clone_url=payload.clone_url,
        )
        jobs_db[job_id]["status"] = "completed"
        jobs_db[job_id]["progress"] = 100
        jobs_db[job_id]["markdown"] = markdown_content

        # Try to save to Supabase history
        try:
            if supabase_db.client:
                supabase_db.client.table("markdown_exports").insert(
                    {
                        "job_id": job_id,
                        "repo_url": payload.clone_url or "local",
                        "time_range": f"{payload.time_since or ''} to {payload.time_until or ''}",
                        "status": "completed",
                        "timestamp": time.time(),
                    }
                ).execute()
        except Exception:
            pass  # Silent ignore if table not created yet

    except Exception as e:
        jobs_db[job_id]["status"] = "failed"
        jobs_db[job_id]["error"] = str(e)
        jobs_db[job_id]["progress"] = 100


@router.post("/export")
async def export_markdown(
    payload: MarkdownExportRequest, background_tasks: BackgroundTasks
):
    job_id = str(uuid.uuid4())
    jobs_db[job_id] = {
        "job_id": job_id,
        "status": "queued",
        "progress": 10,
        "repo_url": payload.clone_url or "local",
        "timestamp": time.time(),
    }
    background_tasks.add_task(run_export_task, job_id, payload)
    return {"status": "success", "job_id": job_id}


@router.get("/export/{job_id}/status")
async def get_job_status(job_id: str):
    if job_id not in jobs_db:
        raise HTTPException(status_code=404, detail="Job not found")
    job = jobs_db[job_id]
    return {
        "status": job["status"],
        "progress": job["progress"],
        "error": job.get("error"),
        "repo_url": job["repo_url"],
    }


@router.get("/export/{job_id}/download")
async def download_markdown(job_id: str):
    if job_id not in jobs_db:
        raise HTTPException(status_code=404, detail="Job not found")
    job = jobs_db[job_id]
    if job["status"] != "completed":
        raise HTTPException(status_code=400, detail="Job not completed yet")
    from fastapi.responses import PlainTextResponse

    return PlainTextResponse(job["markdown"], media_type="text/markdown")


@router.post("/compare")
async def compare_ranges(payload: CompareRequest):
    try:
        # Generate markdown for range A
        markdown_a = await export_codebase_to_markdown(
            root_dir=payload.root_dir,
            time_since=payload.range_a_since,
            time_until=payload.range_a_until,
            git_diff_only=True,
            clone_url=payload.clone_url,
        )
        # Generate markdown for range B
        markdown_b = await export_codebase_to_markdown(
            root_dir=payload.root_dir,
            time_since=payload.range_b_since,
            time_until=payload.range_b_until,
            git_diff_only=True,
            clone_url=payload.clone_url,
        )
        return {
            "status": "success",
            "compare_report": f"# 📊 Time Range Comparison\n\n## 🕐 Range A ({payload.range_a_since or 'start'} to {payload.range_a_until or 'end'})\n{markdown_a}\n\n## 🕑 Range B ({payload.range_b_since or 'start'} to {payload.range_b_until or 'end'})\n{markdown_b}",
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/share")
async def share_to_ai(payload: ShareRequest):
    target = payload.target_ai.lower()
    share_url = "https://claude.ai"
    if target == "chatgpt":
        share_url = "https://chatgpt.com"
    elif target == "gemini":
        share_url = "https://gemini.google.com"

    return {
        "status": "success",
        "share_url": share_url,
        "message": "Copy the markdown below to clipboard and paste it in the target AI console.",
    }


@router.get("/export/history")
async def get_history():
    history = []
    try:
        if supabase_db.client:
            res = (
                supabase_db.client.table("markdown_exports")
                .select("*")
                .order("timestamp", desc=True)
                .limit(50)
                .execute()
            )
            if res.data:
                return {"status": "success", "history": res.data}
    except Exception:
        pass

    for job_id, job in sorted(
        jobs_db.items(), key=lambda x: x[1]["timestamp"], reverse=True
    ):
        history.append(
            {
                "job_id": job_id,
                "repo_url": job["repo_url"],
                "status": job["status"],
                "timestamp": job["timestamp"],
            }
        )
    return {"status": "success", "history": history}
