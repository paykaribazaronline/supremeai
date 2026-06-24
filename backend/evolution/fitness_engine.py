import os
import json
import shutil
from typing import Dict, Any, Optional
from loguru import logger

class FitnessEngine:
    """
    Fitness Score Engine to calculate performance of dynamic skills,
    and automatically deprecate / soft prune low-performing ones.
    """
    def __init__(
        self,
        metrics_path: Optional[str] = None,
        registry_path: Optional[str] = None,
        skills_dir: Optional[str] = None,
        deprecated_dir: Optional[str] = None,
        db: Optional[Any] = None
    ):
        base_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        self.metrics_path = metrics_path or os.path.join(base_dir, "backend", "data", "skills_fitness_metrics.json")
        self.skills_dir = skills_dir or os.path.join(base_dir, "skills", "dynamic")
        self.deprecated_dir = deprecated_dir or os.path.join(base_dir, "skills", "deprecated")
        self.db = db

        # Initialize SkillRegistry
        from skills.registry import SkillRegistry
        self.registry = SkillRegistry(registry_path=registry_path)
        
        self.metrics = self._load_metrics()

    def _load_metrics(self) -> Dict[str, Any]:
        if os.path.exists(self.metrics_path):
            try:
                with open(self.metrics_path, "r", encoding="utf-8") as f:
                    return json.load(f)
            except Exception:
                pass
        return {}

    def _save_metrics(self):
        os.makedirs(os.path.dirname(self.metrics_path), exist_ok=True)
        try:
            with open(self.metrics_path, "w", encoding="utf-8") as f:
                json.dump(self.metrics, f, indent=4)
        except Exception as e:
            logger.error(f"Failed to save fitness metrics: {e}")

    def track_execution(self, skill_name: str, success: bool, latency: float, token_cost: float = 0.0):
        """Record telemetry metrics for a skill execution."""
        if skill_name not in self.metrics:
            self.metrics[skill_name] = {
                "success_count": 0,
                "failure_count": 0,
                "total_latency": 0.0,
                "token_cost": 0.0,
                "reuse_count": 0
            }
        
        entry = self.metrics[skill_name]
        if success:
            entry["success_count"] += 1
        else:
            entry["failure_count"] += 1
        
        entry["total_latency"] += latency
        entry["token_cost"] += token_cost
        entry["reuse_count"] += 1
        
        self._save_metrics()

    def calculate_fitness(self, skill_name: str) -> float:
        """
        Calculate a normalized score between 0.0 and 1.0.
        If skill has no executions, default to 1.0.
        
        Formula:
        - Success Rate = success_count / total_runs
        - Latency Penalty = min(1.0, average_latency / 10.0)
        - Fitness = (Success Rate * 0.7) + ((1.0 - Latency Penalty) * 0.3)
        """
        if skill_name not in self.metrics:
            return 1.0
        
        entry = self.metrics[skill_name]
        total_runs = entry["success_count"] + entry["failure_count"]
        if total_runs == 0:
            return 1.0
        
        success_rate = entry["success_count"] / total_runs
        avg_latency = entry["total_latency"] / total_runs
        latency_penalty = min(1.0, avg_latency / 10.0)
        
        score = (success_rate * 0.7) + ((1.0 - latency_penalty) * 0.3)
        return float(score)

    def evaluate_and_prune(self, skill_name: str, threshold: float = 0.5, min_runs: int = 5) -> bool:
        """
        Evaluate the skill and soft prune it if its score is below threshold after min_runs.
        Returns True if pruned/deprecated, False otherwise.
        """
        if skill_name not in self.metrics:
            return False
            
        entry = self.metrics[skill_name]
        total_runs = entry["success_count"] + entry["failure_count"]
        if total_runs < min_runs:
            return False
            
        score = self.calculate_fitness(skill_name)
        if score >= threshold:
            return False
            
        logger.warning(f"⚠️ Skill '{skill_name}' failed fitness evaluation! Score: {score:.2f} (Threshold: {threshold}). Initiating soft pruning...")
        
        # 1. Update Registry status to DEPRECATED
        skill_data = self.registry.get_skill(skill_name)
        if skill_data:
            skill_data["status"] = "DEPRECATED"
            self.registry.skills["skills"][skill_name] = skill_data
            try:
                with open(self.registry.registry_path, "w", encoding="utf-8") as f:
                    json.dump(self.registry.skills, f, indent=4)
            except Exception as e:
                logger.error(f"Failed to update registry status: {e}")
        
        # 2. Update Firestore Status
        if self.db is not None:
            try:
                self.db.collection("supreme_dynamic_skills").document(skill_name).update({"status": "DEPRECATED"})
            except Exception as e:
                logger.error(f"Failed to update Firestore status for skill '{skill_name}': {e}")
        
        # 3. Soft Prune: Move files from skills/dynamic/<skill_name> to skills/deprecated/<skill_name>
        src_dir = os.path.join(self.skills_dir, skill_name)
        dest_dir = os.path.join(self.deprecated_dir, skill_name)
        
        if os.path.exists(src_dir):
            os.makedirs(os.path.dirname(dest_dir), exist_ok=True)
            try:
                if os.path.exists(dest_dir):
                    shutil.rmtree(dest_dir)
                shutil.move(src_dir, dest_dir)
                logger.info(f"📁 Soft pruned skill files moved to deprecated zone: {dest_dir}")
            except Exception as e:
                logger.error(f"Failed to move files to deprecated zone: {e}")
                
        return True
