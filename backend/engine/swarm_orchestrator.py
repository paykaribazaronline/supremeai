import uuid
from datetime import datetime

from agents.crew_departments import ArchitectureAgent
from agents.crew_departments import CodeGeneratorAgent
from agents.crew_departments import QAAgent
from core.log_batcher import batcher
from models.shared_workspace import SharedWorkspace


class SwarmOrchestrator:
    """
    Coordinates specialized agents (Architecture -> Code -> QA) to autonomously resolve complex tasks.
    """

    def __init__(self, user_id: str, session_id: str, task_prompt: str):
        self.user_id = user_id
        self.session_id = session_id
        self.task_prompt = task_prompt
        self.workspace = SharedWorkspace(original_prompt=task_prompt)

        # Override workspace logger to push to real-time batcher
        self._setup_realtime_logging()

    def _setup_realtime_logging(self):
        original_log = self.workspace.log

        def real_time_log(message: str):
            # Call original
            original_log(message)
            # Push to real-time pub-sub
            log_id = str(uuid.uuid4())
            log_entry = {
                "id": log_id,
                "session_id": self.session_id,
                "log_type": "info",
                "message": message,
                "created_at": datetime.utcnow().isoformat(),
            }
            batcher.emit(log_entry)

            import asyncio
            from datetime import datetime

            import psutil

            from core.swarm_pubsub import swarm_streamer

            agent_name = "Orchestrator"
            if "Architecture" in message or "Phase 1:" in message:
                agent_name = "Architect"
            elif "Code" in message or "Phase 2:" in message:
                agent_name = "Coder"
            elif "QA" in message or "Phase 3:" in message:
                agent_name = "QA"

            level = "info"
            if "rejected" in message.lower() or "failed" in message.lower() or "❌" in message:
                level = "error"
            elif "⚠️" in message:
                level = "warn"
            elif "✅" in message:
                level = "success"

            async def push_stream():
                await swarm_streamer.broadcast(
                    event_type="LOG",
                    payload={
                        "id": log_id,
                        "timestamp": int(datetime.utcnow().timestamp() * 1000),
                        "agentName": agent_name,
                        "message": message,
                        "level": level,
                    },
                )
                await swarm_streamer.broadcast(
                    event_type="METRICS",
                    payload={
                        "cpuUsage": psutil.cpu_percent(),
                        "memoryUsage": psutil.virtual_memory().used / (1024 * 1024),
                        "activeAgents": 3,
                        "errorRate": 0.0,
                    },
                )

            try:
                loop = asyncio.get_running_loop()
                loop.create_task(push_stream())
            except RuntimeError:
                pass

        self.workspace.log = real_time_log

    async def execute(self, max_retries: int = 2) -> SharedWorkspace:
        self.workspace.log("🚀 SwarmOrchestrator: Initiating Swarm execution loop...")

        arch_agent = ArchitectureAgent()
        code_agent = CodeGeneratorAgent()
        qa_agent = QAAgent()

        # Phase 1: Architecture
        self.workspace.log("Phase 1: Architecture Design")
        await arch_agent.design(self.workspace, self.user_id, model_name="gemini/gemini-1.5-pro")

        # Phase 2 & 3 Loop: Code -> QA
        attempt = 0
        while attempt <= max_retries:
            attempt += 1
            self.workspace.log(f"Phase 2: Code Generation (Attempt {attempt}/{max_retries + 1})")
            await code_agent.generate_code(self.workspace, self.user_id, model_name="gemini/gemini-1.5-pro")

            self.workspace.log(f"Phase 3: Quality Assurance (Attempt {attempt}/{max_retries + 1})")
            await qa_agent.verify(self.workspace, self.user_id, model_name="anthropic/claude-3-5-sonnet")

            # Check QA feedback
            feedback = self.workspace.test_results.get("feedback", "")
            if "APPROVED" in feedback.upper() and self.workspace.test_results.get("passed", False):
                self.workspace.log("✅ SwarmOrchestrator: Task successfully completed and approved by QA.")
                break
            else:
                self.workspace.log("⚠️ SwarmOrchestrator: QA rejected the code. Self-healing loop triggered.")
                # Append feedback to original prompt to self-heal
                self.workspace.original_prompt += f"\n\nPrevious QA Feedback to fix:\n{feedback}"

        if attempt > max_retries and not self.workspace.test_results.get("passed", False):
            self.workspace.log("❌ SwarmOrchestrator: Max retries reached. Task failed to pass QA.")

        return self.workspace
