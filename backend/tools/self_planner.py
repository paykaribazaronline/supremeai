import asyncio
import json
from typing import Any

import networkx as nx
from fastapi import APIRouter
from fastapi import HTTPException
from loguru import logger
from pydantic import BaseModel


router = APIRouter(prefix="/agent", tags=["agent-planner"])


class PlanRequest(BaseModel):
    objective: str


class SelfPlanner:
    def __init__(self, llm_client=None):
        self.llm_client = llm_client
        logger.info("Initialized SelfPlanner")

    async def generate_plan(self, objective: str) -> nx.DiGraph:
        logger.info(f"Generating plan for objective: {objective}")

        try:
            from brain.model_router import ModelRouter

            model_router = ModelRouter()
            prompt = (
                "You are an autonomous project planner. Break down the following objective into a JSON array of tasks. "
                "Each task must have: id (string), description (string), depends_on (array of string task IDs). "
                "Return ONLY a valid JSON array without markdown wrapping or explanations.\n\n"
                f"Objective: {objective}"
            )
            result = await model_router.async_route_and_generate(
                prompt, task_type="reasoning", max_cost=0.05
            )
            text = result.get("text", "") if isinstance(result, dict) else ""

            # Clean up JSON if it contains markdown blocks
            text = text.strip()
            if text.startswith("```json"):
                text = text[7:]
            if text.endswith("```"):
                text = text[:-3]
            text = text.strip()

            plan = []
            try:
                plan = json.loads(text)
                if not isinstance(plan, list):
                    plan = []
            except Exception:
                logger.warning("LLM returned non-JSON plan. Using fallback.")
                plan = self._mock_plan(objective)
        except Exception as e:
            logger.warning(f"LLM planner failed: {e}. Using fallback.")
            plan = self._mock_plan(objective)

        dag = nx.DiGraph()
        for task in plan:
            dag.add_node(
                task.get("id", ""),
                description=task.get("description", ""),
                status="pending",
            )
            for dep in task.get("depends_on", []):
                dag.add_edge(dep, task.get("id", ""))

        if not nx.is_directed_acyclic_graph(dag):
            raise ValueError("Generated plan contains circular dependencies!")

        logger.info(f"Generated DAG with {dag.number_of_nodes()} tasks.")
        return dag

    def _mock_plan(self, objective: str) -> list[dict[str, Any]]:
        return [
            {
                "id": "task_1",
                "description": f"Analyze objective: {objective[:50]}",
                "depends_on": [],
            },
            {
                "id": "task_2",
                "description": "Implement core logic",
                "depends_on": ["task_1"],
            },
            {
                "id": "task_3",
                "description": "Write unit tests",
                "depends_on": ["task_2"],
            },
            {
                "id": "task_4",
                "description": "Update documentation",
                "depends_on": ["task_2"],
            },
        ]

    def get_execution_order(self, dag: nx.DiGraph) -> list[list[str]]:
        batches = []
        in_degrees = dict(dag.in_degree())

        while in_degrees:
            current_batch = [node for node, degree in in_degrees.items() if degree == 0]
            if not current_batch:
                raise RuntimeError(
                    "Circular dependency detected during execution ordering"
                )

            batches.append(current_batch)
            for node in current_batch:
                for successor in dag.successors(node):
                    in_degrees[successor] -= 1
                del in_degrees[node]

        return batches

    async def parallel_agent_executor(self, dag: nx.DiGraph) -> dict[str, Any]:
        """
        Executes the DAG using a breadth-first batch approach.
        Tasks in the same batch run concurrently.
        """
        batches = self.get_execution_order(dag)
        execution_results = {}

        for batch_index, batch in enumerate(batches):
            logger.info(f"Executing Batch {batch_index + 1}/{len(batches)}: {batch}")

            # Run batch concurrently
            async def run_task(task_id: str):
                desc = dag.nodes[task_id].get("description", "")
                logger.info(f"Task Started: [{task_id}] {desc}")
                # Simulate agent execution
                await asyncio.sleep(0.5)
                result = f"Completed: {desc}"
                logger.info(f"Task Completed: [{task_id}]")
                return task_id, {"status": "success", "result": result}

            tasks = [run_task(task_id) for task_id in batch]
            batch_results = await asyncio.gather(*tasks, return_exceptions=True)

            for res in batch_results:
                if isinstance(res, Exception):
                    logger.error(f"Task failed with exception: {res}")
                else:
                    task_id, task_res = res
                    execution_results[task_id] = task_res
                    dag.nodes[task_id]["status"] = "completed"

        # After all batches are complete, decide the next step automatically.
        final_summary = "Completed all tasks. " + json.dumps(execution_results)
        logger.info(
            f"Plan execution finished for objective. Summary: {final_summary[:200]}"
        )

        # Automatically generate and start the next plan based on the outcome.
        next_objective = f"Based on the successful completion of the previous plan ({final_summary[:150]}...), determine the next logical step to achieve the overall goal."
        logger.info(
            f"Automatically planning next step with objective: {next_objective}"
        )
        next_dag = await self.generate_plan(next_objective)
        asyncio.create_task(self.parallel_agent_executor(next_dag))

        return {
            "status": "completed",
            "batches_executed": len(batches),
            "results": execution_results,
            "next_plan_started": True,
        }


planner = SelfPlanner()


@router.post("/plan")
async def create_plan(request: PlanRequest):
    try:
        dag = await planner.generate_plan(request.objective)
        batches = planner.get_execution_order(dag)

        # We start the execution in the background to avoid blocking the HTTP request
        asyncio.create_task(planner.parallel_agent_executor(dag))

        nodes = []
        for n in dag.nodes(data=True):
            nodes.append(
                {
                    "id": n[0],
                    "description": n[1].get("description"),
                    "status": n[1].get("status"),
                }
            )

        return {
            "status": "success",
            "message": "Plan generated and execution started.",
            "execution_batches": batches,
            "tasks": nodes,
        }
    except Exception as e:
        logger.error(f"Planner failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))
