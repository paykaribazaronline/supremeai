"""
Kaggle Orchestrator - Heavy Compute Offloading System
Distributes ML/AI tasks across 6 Kaggle accounts (180 hrs/week total).
"""
import json
import hashlib
from datetime import datetime
from typing import Optional, Dict, Any, List
from dataclasses import dataclass, field
from enum import Enum
import redis.asyncio as redis
import httpx
from core.config import settings
from loguru import logger

class KaggleTaskType(Enum):
    """Types of tasks that can be offloaded to Kaggle."""
    EMBEDDING_GENERATION = "embedding_generation"      # Generate vector embeddings
    MODEL_FINE_TUNING = "model_fine_tuning"             # Fine-tune LLMs
    BATCH_INFERENCE = "batch_inference"                 # Bulk LLM calls
    DATA_PROCESSING = "data_processing"                 # ETL jobs
    IMAGE_GENERATION = "image_generation"               # AI image creation
    TRAINING_RUN = "training_run"                       # Model training
    EVALUATION = "evaluation"                           # Model evaluation


class KaggleAccountStatus(Enum):
    """Status of each Kaggle account's quota."""
    AVAILABLE = "available"
    IN_USE = "in_use"
    EXHAUSTED = "exhausted"
    COOLING_DOWN = "cooling_down"


@dataclass
class KaggleAccount:
    """Represents one Kaggle account with its quota tracking."""
    account_id: str
    username: str
    api_key: str
    max_hours: float = 30.0
    used_hours: float = 0.0
    status: KaggleAccountStatus = KaggleAccountStatus.AVAILABLE
    current_task: Optional[str] = None
    last_used: Optional[datetime] = None
    
    @property
    def remaining_hours(self) -> float:
        return max(0.0, self.max_hours - self.used_hours)
    
    def can_accept_task(self, estimated_hours: float) -> bool:
        return (
            self.status == KaggleAccountStatus.AVAILABLE and 
            self.remaining_hours >= estimated_hours
        )


@dataclass
class KaggleJob:
    """A job to be executed on Kaggle."""
    job_id: str
    task_type: KaggleTaskType
    payload: Dict[str, Any]
    priority: int = 5  # 1-10, 10 is highest
    estimated_hours: float = 2.0
    status: str = "queued"
    created_at: datetime = field(default_factory=datetime.utcnow)
    assigned_account: Optional[str] = None
    result: Optional[Dict[str, Any]] = None
    error: Optional[str] = None
    retry_count: int = 0
    max_retries: int = 3


class KaggleOrchestrator:
    """
    Main orchestrator for managing Kaggle job distribution.
    Implements round-robin with quota-aware scheduling.
    """
    
    REDIS_KEY_PREFIX = "kaggle:"
    JOB_QUEUE_KEY = f"{REDIS_KEY_PREFIX}jobs:queue"
    JOB_STATUS_KEY = f"{REDIS_KEY_PREFIX}job:{{job_id}}"
    ACCOUNT_STATUS_KEY = f"{REDIS_KEY_PREFIX}account:{{account_id}}"
    CALLBACK_URL = f"{settings.auto_backend_url.rstrip('/')}/api/v1/kaggle/callback"
    
    _instance = None
    
    @classmethod
    def get_instance(cls):
        if cls._instance is None:
            # Initialize with accounts from settings
            accounts = []
            keys = settings.kaggle_api_keys
            for i, key in enumerate(keys):
                # Simple extraction: normally Kaggle tokens are username:key
                username = f"supremeai_worker_{i+1}"
                api_key = key
                if ":" in key:
                    username, api_key = key.split(":", 1)
                
                accounts.append(KaggleAccount(
                    account_id=f"worker_{i+1}",
                    username=username,
                    api_key=api_key
                ))
            cls._instance = cls(settings.redis_url, accounts)
        return cls._instance
    
    def __init__(self, redis_url: str, accounts: List[KaggleAccount]):
        if not redis_url:
            logger.warning("KaggleOrchestrator initialized without Redis URL. Queue will not work.")
            self.redis_client = None
        else:
            self.redis_client = redis.from_url(redis_url, decode_responses=True)
            
        self.accounts = {acc.account_id: acc for acc in accounts}
        self.http_client = httpx.AsyncClient(timeout=300.0)  # 5 min timeout
    
    async def submit_job(
        self,
        task_type: KaggleTaskType,
        payload: Dict[str, Any],
        priority: int = 5,
        estimated_hours: float = 2.0
    ) -> str:
        if not self.redis_client:
            raise RuntimeError("Redis not configured for KaggleOrchestrator")
            
        job_id = f"job_{datetime.utcnow().strftime('%Y%m%d_%H%M%S')}_{hashlib.md5(json.dumps(payload).encode()).hexdigest()[:8]}"
        
        job = KaggleJob(
            job_id=job_id,
            task_type=task_type,
            payload=payload,
            priority=priority,
            estimated_hours=estimated_hours
        )
        
        job_key = self.JOB_STATUS_KEY.format(job_id=job_id)
        await self.redis_client.hset(job_key, mapping={
            "job_id": job.job_id,
            "task_type": job.task_type.value,
            "payload": json.dumps(job.payload),
            "priority": str(job.priority),
            "estimated_hours": str(job.estimated_hours),
            "status": job.status,
            "created_at": job.created_at.isoformat(),
            "retry_count": str(job.retry_count)
        })
        
        await self.redis_client.expire(job_key, 86400)
        await self.redis_client.zadd(self.JOB_QUEUE_KEY, {job_id: -priority})
        
        logger.info(f"📤 Job submitted: {job_id} ({task_type.value}, priority={priority})")
        return job_id
    
    async def get_next_job(self) -> Optional[KaggleJob]:
        if not self.redis_client:
            return None
            
        results = await self.redis_client.zrange(self.JOB_QUEUE_KEY, 0, 0, withscores=True)
        if not results:
            return None
        
        job_id = results[0][0]
        job_key = self.JOB_STATUS_KEY.format(job_id=job_id)
        job_data = await self.redis_client.hgetall(job_key)
        
        if not job_data:
            await self.redis_client.zrem(self.JOB_QUEUE_KEY, job_id)
            return None
        
        return KaggleJob(
            job_id=job_data.get("job_id", job_id),
            task_type=KaggleTaskType(job_data.get("task_type", "data_processing")),
            payload=json.loads(job_data.get("payload", "{}")),
            priority=int(job_data.get("priority", "5")),
            estimated_hours=float(job_data.get("estimated_hours", "2.0")),
            status=job_data.get("status", "unknown"),
            created_at=datetime.fromisoformat(job_data.get("created_at", datetime.utcnow().isoformat())),
            retry_count=int(job_data.get("retry_count", "0"))
        )
    
    async def select_account_for_job(self, job: KaggleJob) -> Optional[KaggleAccount]:
        available_accounts = [
            acc for acc in self.accounts.values()
            if acc.can_accept_task(job.estimated_hours)
        ]
        
        if not available_accounts:
            logger.warning(f"⚠️ No account available for job {job.job_id} (needs {job.estimated_hours}h)")
            return None
        
        available_accounts.sort(key=lambda x: x.remaining_hours, reverse=True)
        return available_accounts[0]
    
    async def dispatch_job_to_kaggle(self, job: KaggleJob, account: KaggleAccount) -> bool:
        if not self.redis_client:
            return False
            
        try:
            kernel_payload = {
                "id": f"supremeai-{job.job_id}",
                "title": f"SupremeAI: {job.task_type.value} [{job.job_id}]",
                "code": self._generate_kernel_code(job),
                "dataset_sources": [],
                "kernel_sources": [],
                "kernel_type": "script",
                "is_private": True,
                "enable_gpu": True,
                "enable_internet": True,
                "category_ids": [],
                "language": "python"
            }
            
            response = await self.http_client.post(
                "https://www.kaggle.com/api/v1/kernels/push",
                json=kernel_payload,
                headers={
                    "Kaggle-Username": account.username,
                    "Kaggle-Key": account.api_key
                }
            )
            
            if response.status_code in (200, 201):
                job_key = self.JOB_STATUS_KEY.format(job_id=job.job_id)
                await self.redis_client.hset(job_key, mapping={
                    "status": "running",
                    "assigned_account": account.account_id
                })
                
                account.status = KaggleAccountStatus.IN_USE
                account.current_task = job.job_id
                account.last_used = datetime.utcnow()
                
                logger.info(f"🚀 Job {job.job_id} dispatched to Kaggle account {account.username}")
                return True
            else:
                logger.error(f"❌ Failed to dispatch job: {response.text}")
                return False
                
        except Exception as e:
            logger.error(f"❌ Error dispatching job: {e}")
            return False
    
    def _generate_kernel_code(self, job: KaggleJob) -> str:
        base_code = f'''#!/usr/bin/env python3
"""
SupremeAI Auto-Generated Kaggle Kernel
Job ID: {job.job_id}
Task Type: {job.task_type.value}
"""
import json
import os
import traceback
import urllib.request
from datetime import datetime

JOB_ID = "{job.job_id}"
CALLBACK_URL = "{self.CALLBACK_URL}"

def callback(status: str, result: dict = None, error: str = None):
    payload = {{
        "job_id": JOB_ID,
        "status": status,
        "result": result or {{}},
        "error": error,
        "completed_at": datetime.utcnow().isoformat(),
        "kaggle_metadata": {{
            "kernel_output": "/kaggle/working/output.json" if status == "success" else None
        }}
    }}
    
    data = json.dumps(payload).encode('utf-8')
    req = urllib.request.Request(
        CALLBACK_URL,
        data=data,
        headers={{'Content-Type': 'application/json'}}
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            print(f"✅ Callback sent: {{response.read().decode()}}")
    except Exception as e:
        print(f"⚠️ Callback failed: {{e}}")

def main():
    try:
        # Task execution logic would go here
        output = {{"status": "completed"}}
        callback("success", result={{"output": output}})
    except Exception as e:
        error_msg = f"{{traceback.format_exc()}}"
        print(f"❌ Error: {{error_msg}}")
        callback("failed", error=error_msg)

if __name__ == "__main__":
    main()
'''
        return base_code
    
    async def get_queue_stats(self) -> Dict[str, Any]:
        queue_length = 0
        if self.redis_client:
            queue_length = await self.redis_client.zcard(self.JOB_QUEUE_KEY)
            
        account_stats = {}
        for acc_id, acc in self.accounts.items():
            account_stats[acc.username] = {
                "remaining_hours": acc.remaining_hours,
                "status": acc.status.value,
                "current_task": acc.current_task
            }
        
        return {
            "queue_length": queue_length,
            "accounts": account_stats,
            "total_weekly_gpu_hours": sum(acc.max_hours for acc in self.accounts.values()),
            "total_remaining_hours": sum(acc.remaining_hours for acc in self.accounts.values()),
            "active_nodes": len(self.accounts)
        }
