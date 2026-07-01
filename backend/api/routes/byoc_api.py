# Secure Endpoints for Universal BYOC Management
# বাংলা মন্তব্য: সিকিউর প্রক্সি, রোটেশন, ক্রেডেনশিয়াল ম্যানেজমেন্ট ও টেরাফর্ম রানার ট্রিগার এপিআই।

import json
import os
import uuid
from datetime import UTC
from datetime import datetime

from fastapi import APIRouter
from fastapi import BackgroundTasks
from fastapi import HTTPException

from byoc.cloud_connector import GCPCredentialManager
from byoc.container_orchestrator import ContainerOrchestrator
from models.byoc_payloads import BYOCCredentialsPayload
from models.byoc_payloads import BYOCDeployRequest
from models.deployment_logs import DeploymentJob


router = APIRouter(prefix="/api/byoc", tags=["BYOC Management"])
orchestrator = ContainerOrchestrator()

# Memory database for tracking deployment jobs (simulating live backend db)
active_jobs: dict[str, DeploymentJob] = {}
encrypted_vault: dict[str, bytes] = {}


# ==========================================
# 🔐 ROUTE: Upload & Encrypt Credentials
# ==========================================
@router.post("/credentials")
async def save_credentials(payload: BYOCCredentialsPayload):
    """
    Encrypts and saves client-provided cloud service credentials securely.
    """
    # বাংলা মন্তব্য: GCP IAM কী সঠিকতা যাচাইয়ের জন্য Ping টোকেন জেনারেট টেস্ট রান করা হচ্ছে
    sa_dict = payload.gcp_credentials.model_dump()
    is_valid = GCPCredentialManager.validate_service_account(sa_dict)
    if not is_valid:
        raise HTTPException(
            status_code=400,
            detail="GCP Service Account validation failed: Key is invalid or malformed."
        )

    # বাংলা মন্তব্য: প্লেইন-টেক্সট সেভ না করে Fernet কী দিয়ে এনক্রিপ্ট করে সিকিউরড ভোল্ট-এ রাখা হচ্ছে
    try:
        encrypted_data = GCPCredentialManager.encrypt_credentials(sa_dict)
        user_id = "default_user_session"
        encrypted_vault[user_id] = encrypted_data
        
        return {
            "status": "success",
            "message": "GCP Service Account credentials encrypted and securely saved.",
            "provider": payload.provider
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to encrypt credentials: {str(e)}") from e


# ==========================================
# 🚀 ROUTE: Trigger Terraform Container Deploy
# ==========================================
@router.post("/deploy")
async def deploy_container(payload: BYOCDeployRequest, background_tasks: BackgroundTasks):
    """
    Checks user tier quota limits and starts background container deployment.
    """
    user_id = "default_user_session"
    user_tier = "free" # প্রোডাকশনে সেশন ও সাবস্ক্রিপশন টিয়ার থেকে আসবে

    # Load quota limits
    # বাংলা মন্তব্য: রাউট লেভেলেই কোটা চেক করে রিকোয়েস্ট ফিল্টার করা হচ্ছে যাতে ওভারফ্লো না হয়
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    config_path = os.path.join(base_dir, "config", "byoc_limits.json")
    try:
        with open(config_path, encoding="utf-8") as f:
            limits = json.load(f)["limits"]
            user_limits = limits.get(user_tier, limits["free"])
    except Exception:
        user_limits = {"max_containers": 1, "max_memory": "256Mi", "max_cpu": "500m"}

    # Count active containers (simulation check)
    current_active = sum(1 for job in active_jobs.values() if job.user_id == user_id and job.status == "success")
    if current_active >= user_limits["max_containers"]:
        raise HTTPException(
            status_code=403,
            detail=f"Deployment blocked: Account tier limit reached ({user_limits['max_containers']} active containers max)."
        )

    # Check if credentials exist in secure vault
    if user_id not in encrypted_vault:
        raise HTTPException(
            status_code=400,
            detail="GCP Service Account credentials not found. Please upload credentials first."
        )

    # Initiate background job deployment
    job_id = str(uuid.uuid4())
    job = DeploymentJob(
        job_id=job_id,
        user_id=user_id,
        skill_name=payload.skill_name,
        provider=payload.provider,
        status="deploying",
        started_at=datetime.now(UTC),
        logs=["Initializing Terraform build pipeline...", "Spinning up GCP Cloud Run service context..."]
    )
    active_jobs[job_id] = job

    # Async background task definition
    async def run_deployment():
        try:
            res = await orchestrator.deploy(user_id, payload.skill_name)
            if res.get("status") == "deployed":
                job.status = "success"
                job.finished_at = datetime.now(UTC)
                job.service_url = f"https://byoc-skill-{payload.skill_name}-mock-url.a.run.app"
                job.logs.append("✅ Cloud Run deployment finished successfully.")
            else:
                job.status = "failed"
                job.finished_at = datetime.now(UTC)
                job.error_message = res.get("error", "Deployment failed")
                job.logs.append(f"❌ Deployment failed: {job.error_message}")
        except Exception as ex:
            job.status = "failed"
            job.finished_at = datetime.now(UTC)
            job.error_message = str(ex)
            job.logs.append(f"❌ Pipeline crashed: {str(ex)}")

    background_tasks.add_task(run_deployment)

    return {
        "status": "pending",
        "job_id": job_id,
        "message": f"Deployment pipeline initialized for skill '{payload.skill_name}'."
    }


# ==========================================
# 📊 ROUTE: Fetch Deployment Job Status
# ==========================================
@router.get("/status/{job_id}")
async def get_deployment_status(job_id: str):
    """
    Returns live execution state and logs for a container deployment job.
    """
    job = active_jobs.get(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Deployment job not found.")
    return job.model_dump()
