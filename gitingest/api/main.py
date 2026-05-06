"""
FastAPI application for Gitingest
"""
import asyncio
from typing import Optional
from fastapi import FastAPI, HTTPException, Request, Depends
from fastapi.responses import JSONResponse, HTMLResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded

from ..config import get_settings
from ..models import IngestRequest, IngestResponse, ErrorResponse
from ..core import ingest_async, compute_cache_key
from ..core.parser import parse_url
from ..core.cloner import clone_repo, cleanup_repo, get_current_commit

# Initialize rate limiter
limiter = Limiter(key_func=get_remote_address)

# Create FastAPI app
app = FastAPI(
    title="Gitingest",
    description="Convert Git repositories into LLM-friendly text digests",
    version="1.0.0",
)

# Add rate limiter
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

# Setup templates
import os
templates = Jinja2Templates(directory=os.path.join(os.path.dirname(__file__), "ui/templates"))

# Setup static files
if os.path.exists(os.path.join(os.path.dirname(__file__), "ui/static")):
    app.mount("/static", StaticFiles(directory=os.path.join(os.path.dirname(__file__), "ui/static")), name="static")


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {"status": "healthy", "version": "1.0.0"}


@app.get("/robots.txt")
async def robots_txt():
    """Robots.txt to prevent indexing"""
    content = """User-agent: *
Disallow: /api/
Disallow: /static/
"""
    return HTMLResponse(content, media_type="text/plain")


@app.get("/llms.txt")
async def llms_txt():
    """LLMs.txt with information about the API"""
    content = """# Gitingest

Gitingest converts Git repositories into LLM-friendly text digests.

## API Endpoints

- POST /api/ingest - Ingest a repository
- GET /api/{user}/{repository} - Ingest via GitHub-style URL
- GET /{full_path:path} - Web UI with pre-populated URL

## Usage

Send a POST request to /api/ingest with JSON body:
{
  "url": "https://github.com/user/repo",
  "pattern_type": "exclude",
  "patterns": "*.md,*.txt",
  "max_file_size_kb": 100,
  "github_pat": "ghp_..."
}
"""
    return HTMLResponse(content, media_type="text/plain")


@app.post("/api/ingest", response_model=IngestResponse)
@limiter.limit("10/minute")
async def api_ingest(request: IngestRequest, req: Request):
    """
    Ingest a repository and return digest.
    
    Rate limited to 10 requests per minute per IP.
    """
    try:
        result = await ingest_async(request)
        return result
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except TimeoutError as e:
        raise HTTPException(status_code=504, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/{user}/{repository}")
@limiter.limit("10/minute")
async def api_ingest_github_path(
    user: str,
    repository: str,
    req: Request,
    branch: Optional[str] = None,
    pattern_type: str = "exclude",
    patterns: Optional[str] = None,
    max_file_size_kb: Optional[int] = None,
    github_pat: Optional[str] = None,
):
    """
    Handle GitHub-style URL paths.
    
    Example: /api/fastapi/fastapi?branch=main
    """
    from ..models import IngestRequest
    
    url = f"https://github.com/{user}/{repository}"
    if branch:
        url += f"/tree/{branch}"
    
    request = IngestRequest(
        url=url,
        pattern_type=pattern_type,
        patterns=patterns,
        max_file_size_kb=max_file_size_kb,
        github_pat=github_pat,
    )
    
    return await api_ingest(request, req)


@app.get("/{full_path:path}")
async def catch_all(request: Request, full_path: str):
    """
    Catch-all that renders dynamic page with pre-populated URL.
    
    Converts github.com/user/repo to page with URL pre-populated.
    """
    # Check if it's a GitHub-style URL
    if full_path.startswith("github.com/"):
        url = "https://" + full_path
    elif "github.com" in full_path:
        url = full_path
    else:
        url = None
    
    return templates.TemplateResponse("index.html", {
        "request": request,
        "prepopulated_url": url,
    })


@app.get("/")
async def index(request: Request):
    """Home page"""
    return templates.TemplateResponse("index.html", {
        "request": request,
        "prepopulated_url": None,
    })


# Metrics endpoint (Prometheus)
@app.get("/metrics")
async def metrics():
    """Prometheus metrics endpoint"""
    try:
        from prometheus_client import generate_latest, CONTENT_TYPE_LATEST
        return HTMLResponse(
            content=generate_latest(),
            media_type=CONTENT_TYPE_LATEST,
        )
    except ImportError:
        raise HTTPException(status_code=501, detail="Prometheus not installed")


def run_server():
    """Run the FastAPI server with Uvicorn"""
    import uvicorn
    settings = get_settings()
    
    uvicorn.run(
        "gitingest.api.main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=True,
    )


if __name__ == "__main__":
    run_server()
