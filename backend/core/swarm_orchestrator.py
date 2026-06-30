# Multi-Agent Swarm Orchestrator Engine
# বাংলা মন্তব্য: মাল্টি-এজেন্ট সিকোয়েন্সিয়াল সোয়ার্ম কোঅর্ডিনেটর ও টাস্ক রানার।

import uuid
from loguru import logger
from models.shared_workspace import SharedWorkspace
from agents.crew_departments import ArchitectureAgent, CodeGeneratorAgent, QAAgent


class SwarmOrchestrator:
    """
    Coordinates execution of specialized agents sharing state inside a workspace context.
    """
    def __init__(self):
        self.architect = ArchitectureAgent()
        self.coder = CodeGeneratorAgent()
        self.qa = QAAgent()

    async def execute_task(self, prompt: str, user_id: str = "default_user_session") -> SharedWorkspace:
        task_id = str(uuid.uuid4())
        workspace = SharedWorkspace(task_id=task_id, original_prompt=prompt)
        
        workspace.log(f"SwarmOrchestrator: Initialized swarm department for task {task_id}")
        
        # 1. Architecture Design Phase
        await self.architect.design(workspace, user_id)
        
        # 2. Code Generation Phase
        await self.coder.generate_code(workspace, user_id)
        
        # 3. QA and Security Analysis Phase
        await self.qa.verify(workspace, user_id)
        
        workspace.log("SwarmOrchestrator: Multi-Agent execution graph completed successfully.")
        return workspace
