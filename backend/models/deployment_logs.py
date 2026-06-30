# Pydantic schemas for tracking BYOC deployment jobs
# বাংলা মন্তব্য: কন্টেইনার সার্ভিস ডেপ্লয়মেন্টের লাইভ ট্র্যাক করার মডেল স্কিমা।

from typing import Literal, Optional, Dict, Any
from pydantic import BaseModel, Field
from datetime import datetime


class DeploymentJob(BaseModel):
    job_id: str = Field(..., description="Unique Job Identifier")
    user_id: str = Field(..., description="Target User ID")
    skill_name: str = Field(..., description="Target Skill Name")
    provider: Literal["gcp", "aws", "azure"] = "gcp"
    status: Literal["pending", "deploying", "success", "failed"] = "pending"
    started_at: datetime
    finished_at: Optional[datetime] = None
    service_url: Optional[str] = None
    error_message: Optional[str] = None
    logs: list[str] = []
