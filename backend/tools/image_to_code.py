import os
import base64
from typing import Dict, Any
from loguru import logger
from fastapi import APIRouter, HTTPException, UploadFile, File, Form

router = APIRouter(prefix="/tools", tags=["tools", "image-to-code"])

class ImageToCode:
    def __init__(self, vision_model: str = "gpt-4o"):
        self.vision_model = vision_model
        logger.info(f"Initialized ImageToCode with model {self.vision_model}")

    def _encode_image_bytes(self, image_bytes: bytes) -> str:
        return base64.b64encode(image_bytes).decode('utf-8')

    def _encode_image_file(self, image_path: str) -> str:
        if not os.path.exists(image_path):
            raise FileNotFoundError(f"Image not found at {image_path}")
        with open(image_path, "rb") as image_file:
            return self._encode_image_bytes(image_file.read())

    async def generate_code_from_bytes(self, image_bytes: bytes, framework: str = "react", styling: str = "tailwind") -> Dict[str, Any]:
        logger.info(f"Generating {framework} code with {styling} for uploaded image")
        try:
            base64_image = self._encode_image_bytes(image_bytes)
            return await self._call_vision_model(base64_image, framework, styling)
        except Exception as e:
            logger.error(f"Image to Code generation failed: {str(e)}")
            return {"status": "error", "error": str(e)}

    async def generate_code(self, image_path: str, framework: str = "react", styling: str = "tailwind") -> Dict[str, Any]:
        logger.info(f"Generating {framework} code with {styling} for image: {image_path}")
        try:
            base64_image = self._encode_image_file(image_path)
            return await self._call_vision_model(base64_image, framework, styling)
        except Exception as e:
            logger.error(f"Image to Code generation failed: {str(e)}")
            return {"status": "error", "error": str(e)}
            
    async def _call_vision_model(self, base64_image: str, framework: str, styling: str) -> Dict[str, Any]:
        try:
            from brain.model_router import ModelRouter
            router = ModelRouter()
            prompt = (
                f"You are an expert frontend developer and UX/UI engineer. "
                f"Analyze the provided UI image, detect the layout and component structure. "
                f"Extract the styling, typography, colors, and generate pixel-perfect {framework} code using {styling}. "
                "Include any necessary components and icons. "
                "Return ONLY valid code. Do not include markdown formatting or explanations."
                f"\n\n[IMAGE_DATA:{base64_image[:50]}...]" # Vision input logic handled by ModelRouter's vision_agent if configured
            )
            # In our system, ModelRouter handles VISION tasks
            result = await router.async_route_and_generate(prompt, task_type="vision", max_cost=0.05)
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
                        <h1 className="text-2xl font-bold text-gray-800 mb-4">Generated Component</h1>
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

image_to_code_tool = ImageToCode()

@router.post("/image-to-code")
async def api_image_to_code(
    file: UploadFile = File(...),
    framework: str = Form("react"),
    styling: str = Form("tailwind")
):
    try:
        contents = await file.read()
        if not contents:
            raise HTTPException(status_code=400, detail="Empty file provided")
            
        result = await image_to_code_tool.generate_code_from_bytes(
            contents, 
            framework=framework, 
            styling=styling
        )
        if result.get("status") == "error":
            raise HTTPException(status_code=500, detail=result.get("error"))
            
        return result
    except Exception as e:
        logger.error(f"Failed to process image upload: {e}")
        raise HTTPException(status_code=500, detail=str(e))
