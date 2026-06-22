from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional
from tools.codebase_exporter import export_codebase_to_markdown

router = APIRouter(prefix="/markdown", tags=["markdown"])

class MarkdownExportRequest(BaseModel):
    root_dir: str = "."
    time_since: Optional[str] = None
    time_until: Optional[str] = None
    git_diff_only: bool = False
    clone_url: Optional[str] = None

@router.post("/export")
async def export_markdown(payload: MarkdownExportRequest):
    try:
        markdown_content = await export_codebase_to_markdown(
            root_dir=payload.root_dir,
            time_since=payload.time_since,
            time_until=payload.time_until,
            git_diff_only=payload.git_diff_only,
            clone_url=payload.clone_url
        )
        return {
            "status": "success",
            "markdown": markdown_content
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
