from typing import Any
from core.config import settings

from loguru import logger


class GameDevAgent:
    """
    গেম ডেভেলপমেন্ট এজেন্ট — Unity/Unreal/Godot এর জন্য কোড জেনারেট করে।

    Devin/Cursor-এর মতো নির্দিষ্ট ডোমেইনে সেরা হওয়ার জন্য এই এজেন্টটি:
      - Unity C# script generation
      - Game design document (GDD) থেকে কোড
      - Asset description থেকে Blender Python script
      - Performance profiling suggestions
    """

    def __init__(self, model: str = "gpt-4o"):
        self.model = model
        logger.info(f"Initialized GameDevAgent with model {self.model}")

    async def generate_unity_script(self, description: str, script_type: str = "MonoBehaviour") -> dict[str, Any]:
        """Unity C# script জেনারেট করে।"""
        logger.info(f"Generating Unity {script_type} script for: {description}")
        try:
            from brain.model_router import ModelRouter

            router = ModelRouter()
            prompt = (
                f"You are a senior Unity game developer. Write a complete, production-ready C# script of type "
                f"'{script_type}' for the following requirement: {description}. "
                "Include proper Unity lifecycle methods, XML doc comments, and null-safety. "
                "Return ONLY the C# code, no markdown."
            )
            result = await router.async_route_and_generate(prompt, task_type="coding", max_cost=0.03)
            code = result.get("text", "") if isinstance(result, dict) else ""
            return {
                "status": "success",
                "script_type": script_type,
                "engine": "unity",
                "code": code.strip(),
            }
        except Exception as exc:  # noqa: BLE001
            logger.error(f"Unity script generation failed: {exc}")
            return {
                "status": "error",
                "error": str(exc),
                "note": "Real game dev requires Unity SDK integration.",
            }

    async def gdd_to_code(self, gdd_text: str, engine: str = "unity") -> dict[str, Any]:
        """Game design document (GDD) থেকে গেম কোড জেনারেট করে।"""
        logger.info(f"Converting GDD to {engine} code...")
        try:
            from brain.model_router import ModelRouter

            router = ModelRouter()
            prompt = (
                f"You are a game architect. Based on the following Game Design Document, generate the core "
                f"{engine} code scaffolding (classes, systems, and key mechanics). "
                "Return ONLY valid code files concatenated, no markdown.\n\n"
                f"GDD:\n{gdd_text[:4000]}"
            )
            result = await router.async_route_and_generate(prompt, task_type="coding", max_cost=0.05)
            code = result.get("text", "") if isinstance(result, dict) else ""
            return {
                "status": "success",
                "engine": engine,
                "code": code.strip(),
            }
        except Exception as exc:  # noqa: BLE001
            logger.error(f"GDD to code failed: {exc}")
            return {"status": "error", "error": str(exc)}

    async def generate_asset_script(self, asset_description: str) -> dict[str, Any]:
        """Asset description থেকে Blender Python (bpy) script জেনারেট করে।"""
        logger.info(f"Generating Blender script for asset: {asset_description}")
        try:
            from brain.model_router import ModelRouter

            router = ModelRouter()
            prompt = (
                f"You are a Blender Python scripting expert. Write a bpy script that procedurally generates "
                f"the following asset: {asset_description}. Return ONLY valid Python bpy code, no markdown."
            )
            result = await router.async_route_and_generate(prompt, task_type="coding", max_cost=0.03)
            code = result.get("text", "") if isinstance(result, dict) else ""
            return {
                "status": "success",
                "asset": asset_description,
                "blender_script": code.strip(),
            }
        except Exception as exc:  # noqa: BLE001
            logger.error(f"Asset script generation failed: {exc}")
            return {"status": "error", "error": str(exc)}

    async def profile_game_code(self, code: str) -> dict[str, Any]:
        """গেম কোডের জন্য পারফরম্যান্স প্রোফাইলিং সাজেশন দেয়।"""
        logger.info("Profiling game code for performance issues...")
        suggestions: list[str] = []
        # বাংলা মন্তব্য: স্থানীয় হিউরিস্টিক চেক — Update() এ ভারী অপারেশন থাকলে সতর্ক করা হচ্ছে।
        if "Update(" in code and ("Instantiate" in code or "FindObject" in code or "GetComponent" in code):
            suggestions.append("Avoid heavy operations (Instantiate/Find/GetComponent) inside Update(); cache references in Awake/Start.")
        if "foreach" in code and "List<" in code:
            suggestions.append("Consider caching list count and using for-loop to avoid enumerator allocation in hot paths.")
        if "Debug.Log" in code:
            suggestions.append("Remove Debug.Log calls from shipping builds to reduce GC pressure.")
        if not suggestions:
            suggestions.append("No obvious performance anti-patterns detected. Consider object pooling for frequent spawns.")
        return {
            "status": "success",
            "suggestions": suggestions,
            "severity": "info" if len(suggestions) <= 1 else "warning",
        }
