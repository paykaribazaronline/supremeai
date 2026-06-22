import json
from typing import List, Dict, Any, Optional
from loguru import logger
import networkx as nx

class SelfPlanner:
    def __init__(self, llm_client=None):
        self.llm_client = llm_client
        logger.info("Initialized SelfPlanner")
        
    async def generate_plan(self, objective: str) -> nx.DiGraph:
        logger.info(f"Generating plan for objective: {objective}")
        
        try:
            from brain.model_router import ModelRouter
            router = ModelRouter()
            prompt = (
                "You are an autonomous project planner. Break down the following objective into a JSON array of tasks. "
                "Each task must have: id, description, depends_on (list of task IDs). "
                "Return ONLY a valid JSON array. No markdown, no explanations.\n\n"
                f"Objective: {objective}"
            )
            result = router.async_route_and_generate(prompt, task_type="reasoning", max_cost=0.02)
            text = result.get("text", "") if isinstance(result, dict) else ""
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
            dag.add_node(task.get("id", ""), description=task.get("description", ""), status="pending")
            for dep in task.get("depends_on", []):
                dag.add_edge(dep, task.get("id", ""))
                
        if not nx.is_directed_acyclic_graph(dag):
            raise ValueError("Generated plan contains circular dependencies!")
            
        logger.info(f"Generated DAG with {dag.number_of_nodes()} tasks.")
        return dag
    
    def _mock_plan(self, objective: str) -> List[Dict[str, Any]]:
        return [
            {"id": "task_1", "description": f"Analyze objective: {objective[:50]}", "depends_on": []},
            {"id": "task_2", "description": "Implement core logic", "depends_on": ["task_1"]},
            {"id": "task_3", "description": "Write unit tests", "depends_on": ["task_2"]},
            {"id": "task_4", "description": "Update documentation", "depends_on": ["task_2"]}
        ]
        
    def get_execution_order(self, dag: nx.DiGraph) -> List[List[str]]:
        batches = []
        in_degrees = dict(dag.in_degree())
        
        while in_degrees:
            current_batch = [node for node, degree in in_degrees.items() if degree == 0]
            if not current_batch:
                raise RuntimeError("Circular dependency detected during execution ordering")
                
            batches.append(current_batch)
            for node in current_batch:
                for successor in dag.successors(node):
                    in_degrees[successor] -= 1
                del in_degrees[node]
                
        return batches
