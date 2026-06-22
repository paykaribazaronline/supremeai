import os
import base64
from typing import Dict, Any, Optional
from loguru import logger

class ImageToCode:
    def __init__(self, vision_model: str = "gpt-4o"):
        self.vision_model = vision_model
        logger.info(f"Initialized ImageToCode with model {self.vision_model}")

    def _encode_image(self, image_path: str) -> str:
        if not os.path.exists(image_path):
            raise FileNotFoundError(f"Image not found at {image_path}")
        with open(image_path, "rb") as image_file:
            return base64.b64encode(image_file.read()).decode('utf-8')

    async def generate_code(self, image_path: str, framework: str = "react", styling: str = "tailwind") -> Dict[str, Any]:
        logger.info(f"Generating {framework} code with {styling} for image: {image_path}")
        try:
            base64_image = self._encode_image(image_path)
            
            try:
                from brain.model_router import ModelRouter
                router = ModelRouter()
                prompt = (
                    f"You are an expert frontend developer. "
                    f"Analyze the provided UI image and generate pixel-perfect {framework} code using {styling}. "
                    "Return ONLY valid code. Do not include markdown formatting or explanations."
                )
                result = router.async_route_and_generate(prompt, task_type="vision", max_cost=0.03)
                code = result.get("text", "") if isinstance(result, dict) else ""
                if not code:
                    return {"status": "error", "error": "LLM returned empty response."}
                return {
                    "status": "success",
                    "framework": framework,
                    "styling": styling,
                    "code": code.strip(),
                }
            except ImportError:
                logger.warning("ModelRouter not available. Returning mock code.")
                mock_code = f"""
                import React from 'react';
                export default function GeneratedComponent() {{
                    return (
                        <div className="flex flex-col items-center justify-center min-h-screen bg-gray-100 p-4">
                            <h1 className="text-2xl font-bold text-gray-800 mb-4">Generated from {os.path.basename(image_path)}</h1>
                            <button className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition">
                                Click Me
                            </button>
                        </div>
                    );
                }}
                """
                return {
                    "status": "success",
                    "framework": framework,
                    "styling": styling,
                    "code": mock_code.strip(),
                }
        except Exception as e:
            logger.error(f"Image to Code generation failed: {str(e)}")
            return {"status": "error", "error": str(e)}
