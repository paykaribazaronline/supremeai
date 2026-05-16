"""
SupremeAI Reverse Engineering Service
FastAPI microservice for website reverse engineering and API discovery.
Deployed on Cloud Run, triggered via Pub/Sub push subscription.
"""

import os
import json
import uuid
import logging
from datetime import datetime
from typing import Optional, Dict, Any, List

import firebase_admin
from firebase_admin import credentials, firestore
from fastapi import FastAPI, HTTPException, BackgroundTasks, Request
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, HttpUrl
import requests
from bs4 import BeautifulSoup
from google.cloud import pubsub_v1

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize Firebase Admin (if service account key provided)
if os.path.exists('/etc/secrets/firebase-service-account.json'):
    cred = credentials.Certificate('/etc/secrets/firebase-service-account.json')
    firebase_admin.initialize_app(cred)
    db = firestore.client()
else:
    # Fallback to application default credentials (for Cloud Run with default service account)
    try:
        firebase_admin.initialize_app()
        db = firestore.client()
    except Exception as e:
        logger.warning(f"Firebase not initialized: {e}")
        db = None

# Initialize Pub/Sub Publisher
PROJECT_ID = os.getenv("GOOGLE_CLOUD_PROJECT", "supremeai-a")
RESULTS_TOPIC = "reverse-engineering-results"
publisher = pubsub_v1.PublisherClient()
topic_path = publisher.topic_path(PROJECT_ID, RESULTS_TOPIC)

app = FastAPI(
    title="SupremeAI Reverse Engineering Service",
    description="Discovers APIs and data structures from websites",
    version="1.0.0"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# --- Pydantic Models ---

class ReverseEngineeringJobRequest(BaseModel):
    jobId: str
    userId: str
    websiteUrl: HttpUrl
    scrapeDepth: Optional[int] = 1
    discoverApis: Optional[bool] = True
    screenshotEnabled: Optional[bool] = False


class ApiEndpoint(BaseModel):
    url: str
    method: str
    contentType: Optional[str]
    parameters: Optional[Dict[str, Any]]
    responseSample: Optional[Dict[str, Any]]


class ReverseEngineeringResult(BaseModel):
    jobId: str
    userId: str
    websiteUrl: str
    status: str  # COMPLETED, FAILED
    discoveredApis: List[ApiEndpoint]
    scrapedData: Dict[str, Any]
    errorMessage: Optional[str]
    completedAt: datetime


# --- Endpoints ---

@app.get("/health")
async def health():
    return {"status": "UP", "service": "reverse-engineering"}


@app.post("/reveng")
async def start_reverse_engineering(request: ReverseEngineeringJobRequest, background_tasks: BackgroundTasks):
    """
    Start a reverse engineering job.
    Job will be processed asynchronously; returns immediately with job ID.
    Result will be stored in Firestore 'reverse_engineering_jobs' collection.
    """
    logger.info(f"Received reverse engineering job: {request.jobId} for {request.websiteUrl}")

    # Store job initial state in Firestore
    if db:
        job_ref = db.collection('reverse_engineering_jobs').document(request.jobId)
        job_ref.set({
            'jobId': request.jobId,
            'userId': request.userId,
            'websiteUrl': str(request.websiteUrl),
            'status': 'PENDING',
            'scrapeDepth': request.scrapeDepth,
            'discoverApis': request.discoverApis,
            'createdAt': firestore.SERVER_TIMESTAMP,
            'updatedAt': firestore.SERVER_TIMESTAMP
        })

    # Enqueue background processing (direct call for simplicity in Cloud Run)
    background_tasks.add_task(process_reverse_engineering_job, request)
    
    return {"jobId": request.jobId, "status": "PENDING"}


@app.get("/reveng/{jobId}")
async def get_job_status(jobId: str):
    """Retrieve job status and results from Firestore."""
    if not db:
        raise HTTPException(status_code=503, detail="Firestore not available")
    
    doc = db.collection('reverse_engineering_jobs').document(jobId).get()
    if not doc.exists:
        raise HTTPException(status_code=404, detail="Job not found")
    
    return doc.to_dict()


@app.post("/reveng/{jobId}/complete")
async def manual_complete(jobId: str, result: ReverseEngineeringResult):
    """Manually mark job as completed (used by worker or admin)."""
    if not db:
        raise HTTPException(status_code=503, detail="Firestore not available")
    
    job_ref = db.collection('reverse_engineering_jobs').document(jobId)
    job_ref.update({
        'status': result.status,
        'discoveredApis': [api.dict() for api in result.discoveredApis],
        'scrapedData': result.scrapedData,
        'errorMessage': result.errorMessage,
        'completedAt': result.completedAt,
        'updatedAt': firestore.SERVER_TIMESTAMP
    })
    
    return {"jobId": jobId, "status": result.status}


# --- Background Job Processing ---

def process_reverse_engineering_job(request: ReverseEngineeringJobRequest):
    """
    Process reverse engineering job: scrape website, discover APIs, extract data schemas.
    Stores results to Firestore and can trigger downstream code generation.
    """
    logger.info(f"Processing reverse engineering job: {request.jobId}")
    
    try:
        # 1. Basic HTTP GET and parse HTML
        headers = {
            'User-Agent': 'Mozilla/5.0 (compatible; SupremeAI-RevEng/1.0)'
        }
        resp = requests.get(str(request.websiteUrl), headers=headers, timeout=30)
        resp.raise_for_status()
        
        soup = BeautifulSoup(resp.text, 'html.parser')
        
        # 2. Extract forms (potential API endpoints)
        forms = soup.find_all('form')
        discovered_apis: List[Dict[str, Any]] = []
        
        for form in forms:
            action = form.get('action', '')
            method = form.get('method', 'GET').upper()
            inputs = form.find_all('input')
            params = {inp.get('name'): inp.get('type', 'text') for inp in inputs if inp.get('name')}
            
            discovered_apis.append({
                'url': action if action.startswith('http') else str(request.websiteUrl).rstrip('/') + '/' + action.lstrip('/'),
                'method': method,
                'parameters': params,
                'type': 'form_submission'
            })
        
        # 3. Extract scripts that might be API calls
        scripts = soup.find_all('script')
        api_candidates: List[Dict[str, Any]] = []
        
        for script in scripts:
            script_content = script.string
            if script_content:
                # Simple pattern matching for API endpoints in JS code
                import re
                # Look for fetch/axios calls
                fetch_patterns = re.findall(r'fetch\s*\(\s*[\'"](.+?)[\'"]', script_content, re.IGNORECASE)
                axios_patterns = re.findall(r'axios\.(?:get|post|put|delete)\s*\(\s*[\'"](.+?)[\'"]', script_content, re.IGNORECASE)
                
                for url in fetch_patterns + axios_patterns:
                    if 'api' in url.lower() or 'graphql' in url.lower():
                        api_candidates.append({'url': url, 'type': 'js_api_call'})
        
        discovered_apis.extend(api_candidates)
        
        # 4. Extract structured data (JSON-LD, microdata)
        json_ld_scripts = soup.find_all('script', type='application/ld+json')
        structured_data = []
        for jld in json_ld_scripts:
            try:
                data = json.loads(jld.string)
                structured_data.append(data)
            except Exception:
                pass
        
        # 5. Collect scraped data
        scraped_data = {
            'title': soup.title.string if soup.title else None,
            'metaDescription': (soup.find('meta', attrs={'name': 'description'}) or {}).get('content'),
            'formsCount': len(forms),
            'scriptsCount': len(scripts),
            'structuredDataCount': len(json_ld_scripts),
            'urls': [a.get('href') for a in soup.find_all('a', href=True)][:50],  # top 50 links
        }
        
        # 6. Build result
        result = ReverseEngineeringResult(
            jobId=request.jobId,
            userId=request.userId,
            websiteUrl=str(request.websiteUrl),
            status="COMPLETED",
            discoveredApis=[
                ApiEndpoint(
                    url=api.get('url', ''),
                    method=api.get('method', 'GET'),
                    type=api.get('type', 'unknown'),
                    parameters=api.get('parameters'),
                    responseSample=None
                )
                for api in discovered_apis
            ],
            scrapedData=scraped_data,
            errorMessage=None,
            completedAt=datetime.utcnow()
        )
        
        # 7. Persist to Firestore
        if db:
            job_ref = db.collection('reverse_engineering_jobs').document(request.jobId)
            job_ref.update({
                'status': 'COMPLETED',
                'discoveredApis': [api.dict() for api in result.discoveredApis],
                'scrapedData': result.scrapedData,
                'completedAt': result.completedAt,
                'updatedAt': firestore.SERVER_TIMESTAMP
            })
            logger.info(f"Job {request.jobId} completed and saved to Firestore")
            
            # 8. Notify Backend via Pub/Sub
            try:
                message_data = {
                    "jobId": request.jobId,
                    "userId": request.userId,
                    "status": "COMPLETED",
                    "timestamp": datetime.utcnow().isoformat()
                }
                data = json.dumps(message_data).encode("utf-8")
                future = publisher.publish(topic_path, data)
                logger.info(f"Published result for job {request.jobId} to {RESULTS_TOPIC}: {future.result()}")
            except Exception as pe:
                logger.error(f"Failed to publish result to Pub/Sub: {pe}")
        else:
            logger.warning("Firestore not available, skipping persistence")
            
    except Exception as e:
        logger.error(f"Reverse engineering job failed: {e}", exc_info=True)
        if db:
            job_ref = db.collection('reverse_engineering_jobs').document(request.jobId)
            job_ref.update({
                'status': 'FAILED',
                'errorMessage': str(e),
                'updatedAt': firestore.SERVER_TIMESTAMP
            })


# --- Pub/Sub push subscription endpoint (optional) ---

@app.post("/pubsub/push")
async def pubsub_push(request: Request):
    """
    Pub/Sub push subscription endpoint.
    Expects JSON payload with job details.
    """
    try:
        # Pub/Sub push format
        envelope = await request.json()
        message = envelope.get('message', {})
        data = message.get('data', '')
        
        if data:
            import base64
            decoded = base64.b64decode(data).decode('utf-8')
            payload = json.loads(decoded)
            
            job_id = payload.get('jobId') or str(uuid.uuid4())
            user_id = payload.get('userId', 'anonymous')
            website_url = payload.get('websiteUrl')
            
            if website_url:
                # Start reverse engineering
                job_req = ReverseEngineeringJobRequest(
                    jobId=job_id,
                    userId=user_id,
                    websiteUrl=website_url,
                    scrapeDepth=payload.get('scrapeDepth', 1),
                    discoverApis=payload.get('discoverApis', True)
                )
                # Process inline (Cloud Run instance handles it)
                process_reverse_engineering_job(job_req)
                return {"status": "processing", "jobId": job_id}
        
        return {"status": "ignored"}
    except Exception as e:
        logger.error(f"Pub/Sub push error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 8080))
    uvicorn.run(app, host="0.0.0.0", port=port)
