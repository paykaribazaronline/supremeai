# 📄 ফাইল: backend/engine/debate_engine.py

**প্রকার:** .py  
**সাইজ:** 5,027 বাইট  
**আপডেট:** 2026-07-11T11:14:17.545849

---

## কোড

```py
import logging
from enum import StrEnum
from typing import Any

from pydantic import BaseModel


logger = logging.getLogger(__name__)


class DebateState(StrEnum):
    PROPOSING = "PROPOSING"
    JUDGING = "JUDGING"
    RETHINKING = "RETHINKING"
    CONSENSUS = "CONSENSUS"


class Proposal(BaseModel):
    agent_id: str
    content: str
    score: float = 0.0
    feedback: str | None = None


class JudgeAgent:
    """
    JudgeAgent uses a heavier, more capable model (e.g., GPT-4o or Gemini-1.5-Pro)
    to evaluate proposals from smaller, faster models.
    """

    def __init__(self, model_name: str = "gpt-4o"):
        self.model_name = model_name

    async def evaluate_proposals(self, task_prompt: str, proposals: list[Proposal]) -> dict[str, Any]:
        """
        Evaluates the given proposals against the original task prompt.
        Returns the evaluation result including decision state and the winning proposal (if any).
        """
        logger.info(f"Judge Agent [{self.model_name}] evaluating {len(proposals)} proposals...")

        if not proposals:
            return {"decision": DebateState.RETHINKING, "best_proposal": None, "feedback": "No proposals to evaluate."}

        # 🧠 TODO: Actual LiteLLM / OpenAI call goes here.
        # Prompt: "You are the Judge. Evaluate these proposals for the following task..."

        # 🔧 Mocking the Judge's logic for now
        best_proposal = proposals[0]
        best_proposal.score = 0.98
        best_proposal.feedback = "Excellent architecture and handling of edge cases."

        return {"decision": DebateState.CONSENSUS, "best_proposal": best_proposal, "feedback": "Consensus reached successfully."}


class ConsensusOrchestrator:
    """
    Manages the lifecycle of a multi-agent debate session.
    """

    def __init__(self, session_id: str):
        self.session_id = session_id
        self.current_state = DebateState.PROPOSING
        self.judge = JudgeAgent()
        self.proposals: list[Proposal] = []
        self.iteration = 0
        self.max_iterations = 3

    async def run_debate_cycle(self, task_prompt: str) -> Proposal | None:
        """
        Executes the debate loop: Proposing -> Judging -> (Rethinking) -> Consensus.
        """
        import asyncio

        from core.swarm_pubsub import swarm_streamer

        await swarm_streamer.broadcast("DEBATE_UPDATE", {"session_id": self.session_id, "state": self.current_state})

        while self.iteration < self.max_iterations:
            self.iteration += 1
            logger.info(f"Debate Cycle {self.iteration}/{self.max_iterations} for session {self.session_id}")

            # 1. Proposing Phase
            self.current_state = DebateState.PROPOSING
            await swarm_streamer.broadcast("DEBATE_UPDATE", {"session_id": self.session_id, "state": self.current_state, "iteration": self.iteration})

            # Mocking proposers generation in parallel
            async def generate_proposal(agent_id: str, prompt: str) -> Proposal:
                await asyncio.sleep(1)  # Simulate LLM thinking
                return Proposal(agent_id=agent_id, content=f"Proposal from {agent_id} based on {prompt[:10]}...")

            proposer_tasks = [
                generate_proposal("Architect_1", task_prompt),
                generate_proposal("Coder_1", task_prompt),
                generate_proposal("QA_1", task_prompt),
            ]
            self.proposals = await asyncio.gather(*proposer_tasks)

            # 2. Judging Phase
            self.current_state = DebateState.JUDGING
            await swarm_streamer.broadcast(
                "DEBATE_UPDATE", {"session_id": self.session_id, "state": self.current_state, "proposals_count": len(self.proposals)}
            )

            judge_result = await self.judge.evaluate_proposals(task_prompt, self.proposals)

            if judge_result["decision"] == DebateState.CONSENSUS:
                self.current_state = DebateState.CONSENSUS
                await swarm_streamer.broadcast(
                    "DEBATE_UPDATE",
                    {"session_id": self.session_id, "state": self.current_state, "winning_agent": judge_result["best_proposal"].agent_id},
                )
                return judge_result["best_proposal"]
            else:
                self.current_state = DebateState.RETHINKING
                await swarm_streamer.broadcast(
                    "DEBATE_UPDATE", {"session_id": self.session_id, "state": self.current_state, "feedback": judge_result.get("feedback")}
                )

                # Append feedback to the prompt for the next cycle
                task_prompt = task_prompt + f"\n\nFeedback from Judge: {judge_result.get('feedback', 'Improve the proposal.')}"

        # 3. Fallback Solution if no consensus
        logger.warning(f"Max iterations reached for session {self.session_id}. Returning FALLBACK_SOLUTION.")
        return Proposal(agent_id="Fallback_Agent", content="System fallback executed due to lack of consensus.")

```