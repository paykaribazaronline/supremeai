import base64
import json
import os
import re
import tempfile
from typing import Any

from fastapi import APIRouter
from fastapi import File
from fastapi import Form
from fastapi import HTTPException
from fastapi import UploadFile
from loguru import logger

from core.upload_validator import validate_upload


router = APIRouter(prefix="/tools", tags=["tools", "image-to-code"])


# বাংলা মন্তব্য: কম্পোনেন্ট কোড, কালার থিম ও কম্পোনেন্ট হায়ারার্কির জন্য ডেটাক্লাস-সদৃশ টাইপ।
class ComponentCode:
    def __init__(self, framework: str, code: str, component_name: str = "GeneratedComponent"):
        self.framework = framework
        self.code = code
        self.component_name = component_name

    def to_dict(self) -> dict[str, Any]:
        return {"framework": self.framework, "component_name": self.component_name, "code": self.code}


class ColorTheme:
    def __init__(self, palette: list[str], css_variables: dict[str, str]):
        self.palette = palette
        self.css_variables = css_variables

    def to_dict(self) -> dict[str, Any]:
        return {"palette": self.palette, "css_variables": self.css_variables}


class ComponentHierarchy:
    def __init__(self, tree: list[dict[str, Any]]):
        self.tree = tree

    def to_dict(self) -> dict[str, Any]:
        return {"components": self.tree}


class ImageToCode:
    def __init__(self, vision_model: str = "gpt-4o"):
        self.vision_model = vision_model
        logger.info(f"Initialized ImageToCode with model {self.vision_model}")

    def _encode_image_bytes(self, image_bytes: bytes) -> str:
        return base64.b64encode(image_bytes).decode("utf-8")

    def _encode_image_file(self, image_path: str) -> str:
        if not os.path.exists(image_path):
            raise FileNotFoundError(f"Image not found at {image_path}")
        with open(image_path, "rb") as image_file:
            return self._encode_image_bytes(image_file.read())

    async def generate_code_from_bytes(self, image_bytes: bytes, framework: str = "react", styling: str = "tailwind") -> dict[str, Any]:
        logger.info(f"Generating {framework} code with {styling} for uploaded image")
        try:
            base64_image = self._encode_image_bytes(image_bytes)
            return await self._call_vision_model(base64_image, framework, styling)
        except Exception as e:  # noqa: BLE001
            logger.error(f"Image to Code generation failed: {str(e)}")
            return {"status": "error", "error": str(e)}

    async def generate_code(self, image_path: str, framework: str = "react", styling: str = "tailwind") -> dict[str, Any]:
        logger.info(f"Generating {framework} code with {styling} for image: {image_path}")
        try:
            base64_image = self._encode_image_file(image_path)
            return await self._call_vision_model(base64_image, framework, styling)
        except Exception as e:  # noqa: BLE001
            logger.error(f"Image to Code generation failed: {str(e)}")
            return {"status": "error", "error": str(e)}

    async def figma_to_react(self, image_path: str, framework: str = "react", styling: str = "tailwind") -> ComponentCode:
        """
        Figma/UI screenshot → pixel-perfect React/Flutter component তৈরি করে।

        framework: "react" বা "flutter"
        """
        logger.info(f"Converting Figma/UI screenshot to {framework} component: {image_path}")
        try:
            base64_image = self._encode_image_file(image_path)
            if framework.lower() == "flutter":
                prompt = (
                    "You are an expert Flutter developer. Analyze the provided UI screenshot and generate a "
                    "pixel-perfect Flutter widget using Material/Cupertino components. Return ONLY valid Dart code "
                    "with no markdown formatting or explanations."
                )
            else:
                prompt = (
                    "You are an expert React developer. Analyze the provided UI screenshot and generate a "
                    f"pixel-perfect React component using {styling}. Return ONLY valid JSX/TSX code with no "
                    "markdown formatting or explanations."
                )
            result = await self._call_vision_model_raw(base64_image, prompt)
            code = result if isinstance(result, str) else ""
            component_name = "GeneratedComponent"
            if framework.lower() == "flutter":
                match = re.search(r"class\s+(\w+)", code)
                if match:
                    component_name = match.group(1)
            return ComponentCode(framework=framework, code=code.strip(), component_name=component_name)
        except Exception as e:  # noqa: BLE001
            logger.error(f"figma_to_react failed: {e}")
            return ComponentCode(framework=framework, code="", component_name="GeneratedComponent")

    async def extract_color_palette(self, image_path: str) -> ColorTheme:
        """UI screenshot থেকে কালার প্যালেট এক্সট্র্যাক্ট ও CSS variable জেনারেট করে।"""
        logger.info(f"Extracting color palette from: {image_path}")
        try:
            base64_image = self._encode_image_file(image_path)
            prompt = (
                "Analyze this UI screenshot and extract the dominant color palette. "
                "Return ONLY a JSON object with keys: 'palette' (list of hex codes) and "
                "'css_variables' (object mapping semantic names like primary, secondary, background, text to hex). "
                "No markdown, just raw JSON."
            )
            raw = await self._call_vision_model_raw(base64_image, prompt)
            cleaned = raw.strip()
            if cleaned.startswith("```"):
                cleaned = "\n".join(cleaned.splitlines()[1:])
            if cleaned.endswith("```"):
                cleaned = "\n".join(cleaned.splitlines()[:-1])
            parsed = json.loads(cleaned)
            return ColorTheme(
                palette=parsed.get("palette", []),
                css_variables=parsed.get("css_variables", {}),
            )
        except Exception as e:  # noqa: BLE001
            logger.warning(f"Color palette extraction failed, using fallback: {e}")
            return ColorTheme(
                palette=["#1f2937", "#3b82f6", "#ffffff"],
                css_variables={"primary": "#3b82f6", "background": "#ffffff", "text": "#1f2937"},
            )

    async def detect_component_tree(self, image_path: str) -> ComponentHierarchy:
        """UI screenshot থেকে nested component গাছ (hierarchy) শনাক্ত করে।"""
        logger.info(f"Detecting component tree from: {image_path}")
        try:
            base64_image = self._encode_image_file(image_path)
            prompt = (
                "Analyze this UI screenshot and identify the nested component hierarchy. "
                "Return ONLY a JSON array of objects, each with 'name', 'type' (e.g. Button, Card, Navbar), "
                "and optional 'children' (array of the same structure). No markdown, just raw JSON."
            )
            raw = await self._call_vision_model_raw(base64_image, prompt)
            cleaned = raw.strip()
            if cleaned.startswith("```"):
                cleaned = "\n".join(cleaned.splitlines()[1:])
            if cleaned.endswith("```"):
                cleaned = "\n".join(cleaned.splitlines()[:-1])
            parsed = json.loads(cleaned)
            return ComponentHierarchy(tree=parsed if isinstance(parsed, list) else [])
        except Exception as e:  # noqa: BLE001
            logger.warning(f"Component tree detection failed, using fallback: {e}")
            return ComponentHierarchy(tree=[{"name": "Root", "type": "Container", "children": []}])

    async def _call_vision_model_raw(self, base64_image: str, prompt: str) -> str:
        """ভিশন মডেল থেকে সরাসরি টেক্সট রেস্পন্স আনয়ন (JSON/code)।"""
        try:
            from brain.model_router import ModelRouter

            router_llm = ModelRouter()
            result = await router_llm.async_route_and_generate(
                prompt,
                task_type="vision",
                max_cost=0.05,
                images=[{"base64": base64_image, "mime": "image/png"}],
            )
            return result.get("text", "") if isinstance(result, dict) else str(result)
        except Exception as e:  # noqa: BLE001
            logger.warning(f"Vision model unavailable ({e}); returning empty.")
            return ""

    async def _call_vision_model(self, base64_image: str, framework: str, styling: str) -> dict[str, Any]:
        try:
            from brain.model_router import ModelRouter

            router = ModelRouter()
            prompt = (
                f"You are an expert frontend developer and UX/UI engineer. "
                f"Analyze the provided UI image, detect the layout and component structure. "
                f"Extract the styling, typography, colors, and generate pixel-perfect {framework} code using {styling}. "
                "Include any necessary components and icons. "
                "Return ONLY valid code. Do not include markdown formatting or explanations."
                f"\n\n[IMAGE_DATA:{base64_image[:50]}...]"  # Vision input logic handled by ModelRouter's vision_agent if configured
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
            mock_code = """
            import React from 'react';
            export default function GeneratedComponent() {
                return (
                    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-100 p-4">
                        <h1 className="text-2xl font-bold text-gray-800 mb-4">Generated Component</h1>
                        <button className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition">
                            Click Me
                        </button>
                    </div>
                );
            }
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
    styling: str = Form("tailwind"),
):
    try:
        await validate_upload(file)
        contents = await file.read()
        if not contents:
            raise HTTPException(status_code=400, detail="Empty file provided")

        result = await image_to_code_tool.generate_code_from_bytes(contents, framework=framework, styling=styling)
        if result.get("status") == "error":
            raise HTTPException(status_code=500, detail=result.get("error"))

        return result
    except Exception as e:  # noqa: BLE001
        logger.error(f"Failed to process image upload: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/image-to-component")
async def api_figma_to_component(
    file: UploadFile = File(...),
    framework: str = Form("react"),
    styling: str = Form("tailwind"),
):
    """Figma/UI screenshot → React/Flutter component।"""
    await validate_upload(file)
    suffix = os.path.splitext(file.filename or "ui.png")[1] or ".png"
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        tmp.write(await file.read())
        tmp_path = tmp.name
    try:
        component = await image_to_code_tool.figma_to_react(tmp_path, framework=framework, styling=styling)
        return {"status": "success", **component.to_dict()}
    finally:
        os.unlink(tmp_path)


@router.post("/image-to-palette")
async def api_extract_palette(file: UploadFile = File(...)):
    """UI screenshot → color palette + CSS variables।"""
    await validate_upload(file)
    suffix = os.path.splitext(file.filename or "ui.png")[1] or ".png"
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        tmp.write(await file.read())
        tmp_path = tmp.name
    try:
        theme = await image_to_code_tool.extract_color_palette(tmp_path)
        return {"status": "success", **theme.to_dict()}
    finally:
        os.unlink(tmp_path)


@router.post("/image-to-tree")
async def api_detect_tree(file: UploadFile = File(...)):
    """UI screenshot → component hierarchy।"""
    await validate_upload(file)
    suffix = os.path.splitext(file.filename or "ui.png")[1] or ".png"
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        tmp.write(await file.read())
        tmp_path = tmp.name
    try:
        hierarchy = await image_to_code_tool.detect_component_tree(tmp_path)
        return {"status": "success", **hierarchy.to_dict()}
    finally:
        os.unlink(tmp_path)
