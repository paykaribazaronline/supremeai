import json
import os
from typing import Any

from fastapi import APIRouter
from fastapi import HTTPException
from loguru import logger
from pydantic import BaseModel


router = APIRouter(prefix="/style", tags=["style-learner"])


class StyleRequest(BaseModel):
    repo_path: str
    language: str | None = "python"


class StyleLearner:
    def __init__(self):
        self.indexer = None
        self.learned_styles: dict[str, Any] = {}
        logger.info("Initialized StyleLearner")

    def _get_indexer(self):
        if self.indexer is None:
            try:
                from tools.repo_deep_indexer import RepoDeepIndexer

                self.indexer = RepoDeepIndexer()
            except ImportError:
                logger.warning("RepoDeepIndexer not available.")
        return self.indexer

    async def extract_style_guidelines(self, repo_path: str) -> dict[str, Any]:
        logger.info(f"Analyzing {repo_path} for style guidelines...")
        code_samples: list[str] = []

        for root, _, files in os.walk(repo_path):
            # Skip hidden dirs and venv
            if any(x in root for x in [".venv", "node_modules", "__pycache__", ".git"]):
                continue
            for file in files:
                if file.endswith((".py", ".ts", ".tsx", ".js")):
                    path = os.path.join(root, file)
                    try:
                        with open(path, encoding="utf-8") as f:
                            code_samples.append(f.read()[:1500])
                    except Exception:
                        pass
                if len(code_samples) >= 20:
                    break
            if len(code_samples) >= 20:
                break

        if code_samples:
            try:
                from brain.model_router import ModelRouter

                router_llm = ModelRouter()
                combined = "\n\n---FILE---\n\n".join(code_samples[:5])
                prompt = (
                    "Analyze the following code samples from a repository and extract strict style guidelines. "
                    "Return a JSON object with keys: python, typescript, general_patterns. "
                    "Do not include any markdown or explanation.\n\n"
                    f"Code:\n{combined[:5000]}"
                )
                # ✅ FIXED: was missing await
                result = await router_llm.async_route_and_generate(prompt, task_type="coding", max_cost=0.03)
                text = result.get("text", "") if isinstance(result, dict) else ""
                try:
                    cleaned = text.strip()
                    if cleaned.startswith("```"):
                        cleaned = "\n".join(cleaned.splitlines()[1:])
                    if cleaned.endswith("```"):
                        cleaned = "\n".join(cleaned.splitlines()[:-1])
                    parsed = json.loads(cleaned)
                    if isinstance(parsed, dict):
                        self.learned_styles[repo_path] = parsed
                        await self._persist_style(repo_path, parsed)
                        return parsed
                except Exception:
                    logger.warning("Failed to parse style guidelines JSON from LLM.")
            except Exception as e:
                logger.warning(f"LLM style analysis failed: {e}")

        guidelines = self._default_guidelines()
        self.learned_styles[repo_path] = guidelines
        return guidelines

    async def _persist_style(self, repo_path: str, style: dict[str, Any]) -> None:
        """Persist learned style to Supabase or local fallback."""
        try:
            from database.supabase_client import db

            if db.client:
                db.client.table("user_preferences").upsert(
                    {
                        "user_id": f"repo:{repo_path}",
                        "custom_shortcuts": style,
                    }
                ).execute()
                return
        except Exception:
            pass
        # Local fallback
        try:
            os.makedirs("data/styles", exist_ok=True)
            safe_name = repo_path.replace("/", "_").replace("\\", "_")[:50]
            with open(f"data/styles/{safe_name}.json", "w") as f:
                json.dump(style, f, indent=2)
        except Exception as e:
            logger.debug(f"Style persist fallback failed: {e}")

    def _default_guidelines(self) -> dict[str, Any]:
        return {
            "python": {
                "naming_convention": "snake_case",
                "class_naming": "PascalCase",
                "type_hints": "strict",
                "docstrings": "google_style",
            },
            "typescript": {
                "interfaces": "prefix_with_I",
                "quotes": "single",
                "semicolons": "always",
            },
            "general_patterns": [
                "Early returns preferred",
                "Dependency injection used for external services",
                "Loguru used for logging",
            ],
        }

    def generate_style_prompt(self, repo_path: str, language: str) -> str:
        if repo_path not in self.learned_styles:
            return "Follow standard industry best practices for the language."
        styles = self.learned_styles[repo_path]
        lang_style = styles.get(language.lower(), {})
        general = styles.get("general_patterns", [])

        prompt = "CRITICAL STYLE GUIDELINES:\n"
        for key, value in lang_style.items():
            prompt += f"- {key.replace('_', ' ').capitalize()}: {value}\n"
        if general:
            prompt += "\nGeneral Patterns:\n"
            for pattern in general:
                prompt += f"- {pattern}\n"
        return prompt


_learner = StyleLearner()


@router.post("/learn")
async def learn_style(request: StyleRequest):
    """Extract and persist coding style from a repository path."""
    if not os.path.isdir(request.repo_path):
        raise HTTPException(status_code=400, detail=f"Path not found: {request.repo_path}")
    guidelines = await _learner.extract_style_guidelines(request.repo_path)
    return {"status": "success", "guidelines": guidelines}


@router.get("/prompt")
async def get_style_prompt(repo_path: str, language: str = "python"):
    """Get a style-injection prompt for the given repo and language."""
    prompt = _learner.generate_style_prompt(repo_path, language)
    return {"status": "success", "prompt": prompt}
