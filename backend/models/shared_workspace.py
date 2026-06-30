# Shared state workspace memory for Multi-Agent Swarm Orchestrations
# বাংলা মন্তব্য: সোয়ার্ম এজেন্টদের মধ্যে কাজের ফলাফল ও ফাইল স্টেট শেয়ার করার শেয়ার্ড মেমরি ক্লাস।

from pydantic import BaseModel, Field
from typing import Dict, Any, List


class SharedWorkspace(BaseModel):
    task_id: str = Field(..., description="Unique Master Task ID")
    original_prompt: str = Field(..., description="User query context")
    architecture_design: str = ""
    generated_code: Dict[str, str] = Field(default_factory=dict, description="File path to content map")
    test_results: Dict[str, Any] = Field(default_factory=dict)
    execution_logs: List[str] = Field(default_factory=list)

    def log(self, message: str):
        self.execution_logs.append(message)
