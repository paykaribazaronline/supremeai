"""
FastAPI microservice for website reverse engineering.
Wraps the existing reverse_engineer Python pipeline.
"""

import os
import uuid
import sys
from datetime import datetime
from typing import Optional, List, Dict, Any

from fastapi import FastAPI, BackgroundTasks, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from google.cloud import firestore

# ─────────────────────────────────────────────────────────────────────────────
# Path setup
# ─────────────────────────────────────────────────────────────────────────────
REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
sys.path.insert(0, REPO_ROOT)
sys.path.insert(0, os.path.join(REPO_ROOT, 'reverse_engineer'))

# Import reverse engineering pipeline (uses absolute imports within package)
from reverse_engineer.observer import KimiObserver
from reverse_engineer.auth_analyzer import AuthAnalyzer
from reverse_engineer.endpoint_discovery import EndpointDiscovery
from reverse_engineer.payload_analyzer import PayloadAnalyzer
from reverse_engineer.code_generator import ConnectorGenerator
from reverse_engineer.validator import ConnectorValidator

app = FastAPI(
    title="SupremeAI Reverse Engineering Service",
    description="Analyze websites and generate API connectors automatically",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Firestore client (synchronous)
try:
    db = firestore.Client()
except Exception as e:
    print(f"Warning: Firestore client init failed: {e}")
    db = None

# ─────────────────────────────────────────────────────────────────────────────
# Models
# ─────────────────────────────────────────────────────────────────────────────

class ReverseEngineeringRequest(BaseModel):
    url: str
    credentials: Optional[Dict[str, str]] = None
    target_languages: List[str] = Field(default=["python", "typescript", "java", "swift", "csharp", "go"])
    enable_browser_automation: bool = True
    user_id: Optional[str] = None

class JobStatusResponse(BaseModel):
    job_id: str
    url: str
    status: str
    progress: float
    current_phase: Optional[str]
    submitted_at: datetime
    started_at: Optional[datetime]
    completed_at: Optional[datetime]
    error: Optional[str] = None

# ─────────────────────────────────────────────────────────────────────────────
# Job Helpers (synchronous)
# ─────────────────────────────────────────────────────────────────────────────

def update_job_status(job_id: str, status: str, progress: float = None, phase: str = None, error: str = None):
    """Update job document in Firestore."""
    if db is None:
        return
    try:
        job_ref = db.collection("reverse_engineering_jobs").document(job_id)
        update_data = {"status": status, "updatedAt": datetime.utcnow()}
        if progress is not None:
            update_data["progress"] = progress
        if phase:
            update_data["currentPhase"] = phase
        if error:
            update_data["error"] = error
        job_ref.update(update_data)
    except Exception as e:
        print(f"[ERROR] Failed to update job {job_id}: {e}")

def run_reverse_engineering_pipeline(job_id: str, request: ReverseEngineeringRequest):
    """
    Full pipeline executed in background thread.
    """
    try:
        update_job_status(job_id, "ANALYZING", 0.1, "OBSERVATION")

        # Step 1: Observe
        observer = KimiObserver(request.url)
        obs_result = observer.analyze()
        update_job_status(job_id, "ANALYZING", 0.2, "AUTH_ANALYSIS")

        # Step 2: Auth Analysis
        auth = AuthAnalyzer(obs_result["page_source"], request.url)
        auth_result = auth.analyze()
        update_job_status(job_id, "ANALYZING", 0.3, "ENDPOINT_DISCOVERY")

        # Step 3: Endpoint Discovery
        discovery = EndpointDiscovery(obs_result["js_bundles"], request.url)
        html_endpoints = discovery.discover_from_html(obs_result["page_source"])
        js_endpoints = discovery.discover()
        endpoints = list(set(html_endpoints + js_endpoints))
        update_job_status(job_id, "GENERATING", 0.5, "PAYLOAD_ANALYSIS")

        # Step 4: Payload Analysis
        sample_endpoint = endpoints[0] if endpoints else "/api/generate"
        analyzer = PayloadAnalyzer(sample_endpoint)
        schema = analyzer.analyze_request({
            'prompt': 'test',
            'language': 'en',
            'max_tokens': 1000
        })
        update_job_status(job_id, "GENERATING", 0.65, "CODE_GENERATION")

        # Step 5: Multi-language Code Generation
        domain = request.url.replace("https://", "").replace("http://", "").replace("/", "_")
        connectors = {}
        for language in request.target_languages:
            try:
                generator = ConnectorGenerator(
                    platform_name=domain,
                    base_url=request.url,
                    auth_type=auth_result.get("auth_type", "UNKNOWN"),
                    endpoints=endpoints,
                    language=language
                )
                code = generator.generate()
                connectors[language] = {
                    "code": code,
                    "filename": f"{domain}_connector.{language_extension(language)}",
                    "status": "GENERATED"
                }
            except Exception as e:
                connectors[language] = {
                    "code": "",
                    "filename": "",
                    "status": "FAILED",
                    "error": str(e)
                }
        update_job_status(job_id, "VALIDATING", 0.85, "VALIDATION")

        # Step 6: Validation
        validation_results = {}
        for lang, connector in connectors.items():
            if connector["status"] == "GENERATED":
                validator = ConnectorValidator(connector["code"], language=lang)
                validation = validator.full_validation(request.credentials)
                validation_results[lang] = validation
                connector["validation"] = validation
                if validation.get("syntax", False):
                    connector["status"] = "VALIDATED"
                else:
                    connector["status"] = "FAILED"
            else:
                validation_results[lang] = {"valid": False, "reason": "Generation failed"}

        update_job_status(job_id, "COMPLETE", 1.0, "DONE")

        # Save results to Firestore
        if db:
            job_ref = db.collection("reverse_engineering_jobs").document(job_id)
            job_ref.update({
                "results": {
                    "observation": obs_result,
                    "auth": auth_result,
                    "endpoints": endpoints,
                    "payload_schema": schema,
                    "connectors": connectors,
                    "validation": validation_results
                }
            })

    except Exception as e:
        print(f"[ERROR] Pipeline failed: {e}")
        update_job_status(job_id, "FAILED", 0.0, error=str(e))

def language_extension(lang: str) -> str:
    mapping = {
        "python": "py",
        "typescript": "ts",
        "java": "java",
        "swift": "swift",
        "csharp": "cs",
        "go": "go"
    }
    return mapping.get(lang.lower(), "txt")

# ─────────────────────────────────────────────────────────────────────────────
# API Endpoints
# ─────────────────────────────────────────────────────────────────────────────

@app.post("/api/reverse-engineer/submit")
async def submit_job(request: ReverseEngineeringRequest, background_tasks: BackgroundTasks):
    """Submit a new reverse engineering job."""
    job_id = f"job_{uuid.uuid4().hex[:12]}"

    job_data = {
        "jobId": job_id,
        "url": request.url,
        "userId": request.user_id,
        "status": "PENDING",
        "progress": 0.0,
        "submittedAt": datetime.utcnow(),
        "updatedAt": datetime.utcnow(),
        "targetLanguages": request.target_languages,
        "results": None,
        "error": None
    }

    if db:
        db.collection("reverse_engineering_jobs").document(job_id).set(job_data)

    # Enqueue background task (runs in threadpool)
    background_tasks.add_task(run_reverse_engineering_pipeline, job_id, request)

    return {"jobId": job_id, "status": "PENDING"}

@app.get("/api/reverse-engineer/job/{jobId}")
async def get_job_status(jobId: str):
    """Get status of a reverse engineering job."""
    if not db:
        raise HTTPException(500, "Firestore not available")
    doc = db.collection("reverse_engineering_jobs").document(jobId).get()
    if not doc.exists:
        raise HTTPException(404, "Job not found")
    data = doc.to_dict()
    # Convert datetimes to ISO strings for JSON
    for field in ['submittedAt', 'startedAt', 'completedAt', 'updatedAt']:
        if data.get(field) and isinstance(data[field], datetime):
            data[field] = data[field].isoformat()
    return data

@app.get("/api/reverse-engineer/job/{jobId}/result")
async def get_job_result(jobId: str):
    """Get generated connectors for a completed job."""
    if not db:
        raise HTTPException(500, "Firestore not available")
    doc = db.collection("reverse_engineering_jobs").document(jobId).get()
    if not doc.exists:
        raise HTTPException(404, "Job not found")
    data = doc.to_dict()
    if data.get("status") != "COMPLETE":
        raise HTTPException(400, f"Job not complete: {data.get('status')}")
    return data.get("results", {})

@app.get("/api/reverse-engineer/history")
async def list_jobs(limit: int = 20, user_id: Optional[str] = None):
    """List recent reverse engineering jobs."""
    if not db:
        raise HTTPException(500, "Firestore not available")
    jobs_ref = db.collection("reverse_engineering_jobs")\
        .order_by("submittedAt", direction=firestore.Query.DESCENDING)\
        .limit(limit)
    if user_id:
        jobs_ref = jobs_ref.where("userId", "==", user_id)
    docs = jobs_ref.stream()
    jobs = []
    for doc in docs:
        j = doc.to_dict()
        j["jobId"] = doc.id
        # serialize datetime
        for f in ['submittedAt','startedAt','completedAt','updatedAt']:
            if j.get(f) and isinstance(j[f], datetime):
                j[f] = j[f].isoformat()
        jobs.append(j)
    return {"jobs": jobs}

@app.delete("/api/reverse-engineer/job/{jobId}")
async def cancel_job(jobId: str):
    """Cancel a pending or running job."""
    if not db:
        raise HTTPException(500, "Firestore not available")
    doc_ref = db.collection("reverse_engineering_jobs").document(jobId)
    doc = doc_ref.get()
    if not doc.exists:
        raise HTTPException(404, "Job not found")
    current_status = doc.to_dict().get("status")
    if current_status in ["COMPLETE", "FAILED", "CANCELLED"]:
        raise HTTPException(400, f"Cannot cancel job in status {current_status}")
    doc_ref.update({"status": "CANCELLED", "error": "Cancelled by user"})
    return {"success": True}

@app.get("/health")
def health():
    return {"status": "UP", "service": "reverse-engineer"}

# ─────────────────────────────────────────────────────────────────────────────
# Entry point
# ─────────────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 8080))
    uvicorn.run(app, host="0.0.0.0", port=port)
