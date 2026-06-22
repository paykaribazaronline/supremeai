import os
import json
from typing import Dict, Any, List
from loguru import logger

class StyleLearner:
    def __init__(self):
        self.indexer = None
        self.learned_styles: Dict[str, Any] = {}
        logger.info("Initialized StyleLearner")

    def _get_indexer(self):
        if self.indexer is None:
            try:
                from tools.repo_deep_indexer import RepoDeepIndexer
                self.indexer = RepoDeepIndexer()
            except ImportError:
                logger.warning("RepoDeepIndexer not available.")
        return self.indexer

    async def extract_style_guidelines(self, repo_path: str) -> Dict[str, Any]:
        logger.info(f"Analyzing {repo_path} for style guidelines...")
        indexer = self._get_indexer()
        if not indexer:
            return self._default_guidelines()
        
        try:
            index_result = await indexer.index_repository(repo_path)
            code_samples = []
            for root, _, files in os.walk(repo_path):
                for file in files:
                    if file.endswith(('.py', '.ts', '.tsx', '.js')):
                        path = os.path.join(root, file)
                        try:
                            with open(path, "r", encoding="utf-8") as f:
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
                    router = ModelRouter()
                    combined = "\n\n---FILE---\n\n".join(code_samples[:5])
                    prompt = (
                        "Analyze the following code samples from a repository and extract strict style guidelines. "
                        "Return a JSON object with keys: python, typescript, general_patterns. "
                        "Do not include any markdown or explanation.\n\n"
                        f"Code:\n{combined[:5000]}"
                    )
                    result = router.async_route_and_generate(prompt, task_type="coding", max_cost=0.03)
                    text = result.get("text", "") if isinstance(result, dict) else ""
                    parsed = {}
                    try:
                        cleaned = text.strip()
                        if cleaned.startswith("```"):
                            cleaned = "\n".join(cleaned.splitlines()[1:])
                        if cleaned.endswith("```"):
                            cleaned = "\n".join(cleaned.splitlines()[:-1])
                        parsed = json.loads(cleaned)
                        if isinstance(parsed, dict):
                            self.learned_styles[repo_path] = parsed
                            return parsed
                    except Exception:
                        logger.warning("Failed to parse style guidelines JSON from LLM.")
                except Exception as e:
                    logger.warning(f"LLM style analysis failed: {e}")
        except Exception as e:
            logger.error(f"Style extraction failed: {e}")
        
        guidelines = self._default_guidelines()
        self.learned_styles[repo_path] = guidelines
        return guidelines

    def _default_guidelines(self) -> Dict[str, Any]:
        return {
            "python": {
                "naming_convention": "snake_case",
                "class_naming": "PascalCase",
                "type_hints": "strict",
                "docstrings": "google_style"
            },
            "typescript": {
                "interfaces": "prefix_with_I",
                "quotes": "single",
                "semicolons": "always"
            },
            "general_patterns": [
                "Early returns preferred",
                "Dependency injection used for external services",
                "Loguru used for logging"
            ]
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
