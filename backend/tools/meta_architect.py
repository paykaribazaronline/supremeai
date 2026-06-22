import os
from typing import Dict, Any
from loguru import logger

class MetaArchitect:
    """
    Self-improving architecture agent.
    Analyzes the codebase, proposes architectural improvements,
    and can implement them autonomously.
    (Closes Gap #86)
    """

    def __init__(self):
        logger.info("Initialized MetaArchitect")

    async def analyze_codebase(self, root_dir: str = ".") -> Dict[str, Any]:
        logger.info(f"Analyzing codebase architecture at {root_dir}")
        metrics = {"total_files": 0, "total_lines": 0, "languages": {}, "avg_file_size": 0}
        issues = []
        suggestions = []
        try:
            for dirpath, _, filenames in os.walk(root_dir):
                for file in filenames:
                    if file.startswith(".") or file.endswith((".png", ".jpg", ".mp3", ".mp4")):
                        continue
                    file_path = os.path.join(dirpath, file)
                    metrics["total_files"] += 1
                    try:
                        with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
                            lines = f.readlines()
                            metrics["total_lines"] += len(lines)
                            ext = os.path.splitext(file)[1].lower()
                            lang = ext.lstrip(".") or "unknown"
                            metrics["languages"][lang] = metrics["languages"].get(lang, 0) + len(lines)
                    except Exception:
                        pass
            if metrics["total_files"]:
                metrics["avg_file_size"] = metrics["total_lines"] / metrics["total_files"]
            if metrics["total_files"] > 500:
                issues.append("Codebase is very large; consider modularization.")
                suggestions.append("Split into microservices or feature modules.")
            if metrics.get("avg_file_size", 0) > 500:
                issues.append("Some files are very large; consider splitting them.")
                suggestions.append("Break down files larger than 500 lines.")
            py_files = metrics["languages"].get("py", 0)
            if py_files > 200:
                suggestions.append("Consider adding type hints to Python files for better maintainability.")
        except Exception as exc:
            logger.error(f"Codebase analysis failed: {exc}")
            issues.append(f"Analysis error: {exc}")
        return {
            "status": "success",
            "metrics": metrics,
            "issues": issues,
            "suggestions": suggestions,
        }

    async def propose_refactor(self, analysis: Dict[str, Any]) -> Dict[str, Any]:
        try:
            from brain.model_router import ModelRouter
            router = ModelRouter()
            prompt = (
                "You are a senior software architect. Based on the following codebase analysis, "
                "propose a concrete refactoring plan. Return a JSON object with keys: "
                '"priority" (high/medium/low), "steps" (array of strings), "estimated_effort_days" (number). '
                "No markdown, no explanation.\n\n"
                f"Analysis: {analysis}"
            )
            result = router.async_route_and_generate(prompt, task_type="reasoning", max_cost=0.03)
            text = result.get("text", "") if isinstance(result, dict) else ""
            import json
            plan = {}
            try:
                cleaned = text.strip()
                if cleaned.startswith("```"):
                    cleaned = "\n".join(cleaned.splitlines()[1:])
                if cleaned.endswith("```"):
                    cleaned = "\n".join(cleaned.splitlines()[:-1])
                plan = json.loads(cleaned)
            except Exception:
                plan = {
                    "priority": "medium",
                    "steps": [
                        "Review high-issue files identified in analysis.",
                        "Apply incremental refactoring with tests.",
                    ],
                    "estimated_effort_days": 3,
                }
            return {"status": "success", "plan": plan}
        except Exception as exc:
            logger.error(f"Refactor proposal failed: {exc}")
            return {"status": "error", "error": str(exc)}

    async def implement_refactor(self, target_path: str, instruction: str) -> Dict[str, Any]:
        logger.info(f"Implementing refactor for {target_path}: {instruction}")
        try:
            from brain.model_router import ModelRouter
            router = ModelRouter()
            with open(target_path, "r", encoding="utf-8") as f:
                original = f.read()
            prompt = (
                "Refactor the following code according to the instruction. "
                "Return ONLY the complete refactored code. No markdown, no explanations.\n\n"
                f"Instruction: {instruction}\n\nCode:\n{original[:8000]}"
            )
            result = router.async_route_and_generate(prompt, task_type="coding", max_cost=0.05)
            new_code = result.get("text", "") if isinstance(result, dict) else ""
            if not new_code:
                return {"status": "error", "error": "Model returned empty response."}
            backup_path = target_path + ".bak"
            with open(backup_path, "w", encoding="utf-8") as f:
                f.write(original)
            with open(target_path, "w", encoding="utf-8") as f:
                f.write(new_code)
            return {
                "status": "success",
                "target": target_path,
                "backup": backup_path,
                "changes_applied": True,
            }
        except Exception as exc:
            logger.error(f"Refactor implementation failed: {exc}")
            return {"status": "error", "error": str(exc)}
