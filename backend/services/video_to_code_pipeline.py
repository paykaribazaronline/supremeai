"""
SupremeAI — Video-to-Code Pipeline
==================================

Video frame analysis pipeline that:
- Extracts key frames from video at configurable intervals
- Analyzes frames for UI components using vision model
- Generates code (React components, Tailwind CSS) from analyzed frames
- Supports MP4, WebM, MOV formats
- Uses ffmpeg for frame extraction (with graceful fallback)
- Caches frame analysis results
- Returns structured component tree + generated code
"""

from __future__ import annotations

import hashlib
import json
import os
import re
import subprocess
from dataclasses import dataclass
from enum import StrEnum
from pathlib import Path
from typing import Any

from fastapi import APIRouter, File, HTTPException, UploadFile
from loguru import logger

from core.cache import get_cache
from core.upload_validator import validate_upload
from services.llm.llm_router import LLMRouter

router = APIRouter(prefix="/video-to-code", tags=["video-to-code"])


class VideoFormat(StrEnum):
    MP4 = "mp4"
    WEBM = "webm"
    MOV = "mov"
    AVI = "avi"
    AUTO = "auto"


@dataclass(frozen=True)
class UIComponent:
    """A UI component extracted from video frame analysis."""

    id: str
    component_type: str  # button, card, form, navbar, etc.
    framework_hint: str  # react, vue, vanilla
    position: tuple[int, int, int, int]  # x, y, width, height
    properties: dict[str, Any]
    detected_text: str | None


@dataclass(frozen=True)
class CodeGenerationResult:
    """Result of video-to-code generation."""

    component_tree: list[UIComponent]
    generated_code: str
    framework: str
    styling: str  # tailwind, css-modules, styled-components
    confidence: float


# ── Constants ────────────────────────────────────────────────────────────────
FRAME_INTERVAL_SECONDS = 2  # Extract frame every N seconds
MAX_VIDEO_SIZE_MB = 50
VIDEO_CACHE_TTL = 3600  # 1 hour


class VideoFrameExtractor:
    """
    Extracts key frames from video using ffmpeg.
    Has graceful fallback when ffmpeg is unavailable.
    """

    @staticmethod
    def _check_ffmpeg() -> bool:
        """Check if ffmpeg is available."""
        try:
            result = subprocess.run(
                ["ffmpeg", "-version"],
                capture_output=True,
                text=True,
                timeout=5,
            )
            return result.returncode == 0
        except (subprocess.SubprocessError, FileNotFoundError):
            return False

    async def extract_frames(
        self,
        video_path: str,
        interval: int = FRAME_INTERVAL_SECONDS,
        max_frames: int = 10,
    ) -> list[str]:
        """
        Extract frames from video at specified intervals.

        Args:
            video_path: Path to video file.
            interval: Seconds between frame extraction.
            max_frames: Maximum frames to extract.

        Returns:
            List of paths to extracted frame images.
        """
        if not self._check_ffmpeg():
            logger.warning("ffmpeg not available, using fallback")
            return await self._fallback_extract(video_path, max_frames)

        output_dir = Path(video_path).parent / f"frames_{Path(video_path).stem}"
        output_dir.mkdir(exist_ok=True)

        output_pattern = str(output_dir / "frame_%03d.jpg")

        cmd = [
            "ffmpeg",
            "-i",
            video_path,
            "-vf",
            f"fps=1/{interval}",
            "-vframes",
            str(max_frames),
            "-q:v",
            "2",
            output_pattern,
        ]

        try:
            subprocess.run(cmd, capture_output=True, check=True, timeout=120)
            return [str(p) for p in output_dir.glob("*.jpg")][:max_frames]
        except (subprocess.SubprocessError, TimeoutError) as e:
            logger.error(f"Frame extraction failed: {e}")
            return []

    async def _fallback_extract(self, video_path: str, max_frames: int) -> list[str]:
        """
        Fallback: extract only first frame or a single representative image.
        Used when ffmpeg is unavailable.
        """
        # For fallback, we just use the video thumbnail or first frame
        # This is a simplified approach
        return [video_path]  # Pass through to vision analysis


class FrameAnalyzer:
    """
    Analyzes video frames using vision model to extract UI components.
    """

    def __init__(self, llm_router: LLMRouter | None = None) -> None:
        self.llm_router = llm_router or LLMRouter()
        self.cache = get_cache()

    def _cache_key(self, frame_path: str, offset: int) -> str:
        stat = os.stat(frame_path) if os.path.exists(frame_path) else None
        stat_str = f"{stat.st_mtime}" if stat else ""
        return f"frame_analysis:{hashlib.sha256((frame_path + stat_str).encode()).hexdigest()[:16]}"

    async def analyze(
        self,
        frame_path: str,
        timestamp_seconds: int = 0,
        framework_hint: str = "react",
    ) -> list[UIComponent]:
        """
        Analyze a frame image for UI components.

        Args:
            frame_path: Path to frame image.
            timestamp_seconds: Timestamp for context.
            framework_hint: Preferred framework for code gen.

        Returns:
            List of detected UI components.
        """
        cache_key = self._cache_key(frame_path, timestamp_seconds)
        cached = await self.cache.get(cache_key)
        if cached:
            return [UIComponent(**c) if isinstance(c, dict) else c for c in cached]

        import base64

        with open(frame_path, "rb") as f:
            base64_image = base64.b64encode(f.read()).decode("utf-8")

        prompt = (
            f"Analyze this UI design screenshot and extract all components. "
            f"Focus on {framework_hint} component patterns. "
            "Return JSON array: "
            '{"id", "component_type", "position": [x, y, w, h], "properties": {}, "detected_text"}. '
            "Include buttons, forms, cards, navigation, text blocks, images."
        )

        try:
            result = await self.llm_router.route(
                prompt=prompt,
                task_type="vision",
                max_tokens=1500,
                images=[{"base64": base64_image, "mime": "image/png"}],
            )

            text = result.get("content", "") if isinstance(result, dict) else ""
            text = re.sub(r"^```(?:json)?\s*", "", text.strip())
            text = re.sub(r"\s*```$", "", text)

            data = json.loads(text)
            components = [
                UIComponent(
                    id=item.get("id", f"c_{i}"),
                    component_type=item.get("component_type", "unknown"),
                    framework_hint=framework_hint,
                    position=tuple(item.get("position", [0, 0, 100, 50]))[:4],
                    properties=item.get("properties", {}),
                    detected_text=item.get("detected_text"),
                )
                for i, item in enumerate(data)
            ]

            await self.cache.set(cache_key, [c.__dict__ for c in components], ttl=VIDEO_CACHE_TTL)
            return components

        except Exception as e:
            logger.error(f"Frame analysis failed: {e}")
            return []


class CodeGenerator:
    """
    Generates code from UI component tree.
    """

    def __init__(self, llm_router: LLMRouter | None = None) -> None:
        self.llm_router = llm_router or LLMRouter()
        self.cache = get_cache()

    async def generate(
        self,
        components: list[UIComponent],
        framework: str = "react",
        styling: str = "tailwind",
    ) -> str:
        """
        Generate component code from UI component tree.

        Args:
            components: List of UI components.
            framework: Target framework (react, vue, svelte).
            styling: Styling approach (tailwind, css-modules, styled-components).

        Returns:
            Generated component code.
        """
        cache_key = f"code_gen:{framework}:{styling}:{hashlib.sha256(str([c.id for c in components]).encode()).hexdigest()[:16]}"

        cached = await self.cache.get(cache_key)
        if cached:
            return cached  # type: ignore

        component_descriptions = []
        for c in components:
            desc = f"- {c.id}: {c.component_type}"
            if c.detected_text:
                desc += f' with text "{c.detected_text}"'
            component_descriptions.append(desc)

        prompt = (
            f"You are a UI component generator. Generate a complete {framework} component "
            f"using {styling} for styling. Component tree:\n"
            + "\n".join(component_descriptions)
            + "\n\nReturn only valid code, no markdown."
        )

        try:
            result = await self.llm_router.route(
                prompt=prompt,
                task_type="generation",
                max_tokens=2000,
            )

            code = result.get("content", "") if isinstance(result, dict) else ""

            # Clean up code formatting
            code = re.sub(r"^```(?:jsx?|tsx?|css)?\s*", "", code.strip())
            code = re.sub(r"\s*```$", "", code)

            await self.cache.set(cache_key, code, ttl=VIDEO_CACHE_TTL)
            return code

        except Exception as e:
            logger.error(f"Code generation failed: {e}")
            return f"// Error generating code: {e}"


class VideoToCodePipeline:
    """
    Main pipeline that orchestrates video-to-code conversion.
    """

    def __init__(
        self,
        extractor: VideoFrameExtractor | None = None,
        analyzer: FrameAnalyzer | None = None,
        generator: CodeGenerator | None = None,
    ) -> None:
        self.extractor = extractor or VideoFrameExtractor()
        self.analyzer = analyzer or FrameAnalyzer()
        self.generator = generator or CodeGenerator()
        self.cache = get_cache()
        logger.info("VideoToCodePipeline initialized")

    async def process(
        self,
        video_path: str,
        framework: str = "react",
        styling: str = "tailwind",
        interval: int = FRAME_INTERVAL_SECONDS,
    ) -> CodeGenerationResult:
        """
        Full pipeline: extract → analyze → generate.

        Args:
            video_path: Path to video file.
            framework: Target UI framework.
            styling: CSS styling approach.
            interval: Frame extraction interval in seconds.

        Returns:
            CodeGenerationResult with all components and generated code.
        """
        # Step 1: Extract frames
        frames = await self.extractor.extract_frames(video_path, interval)

        # Step 2: Analyze all frames
        all_components: list[UIComponent] = []
        for i, frame in enumerate(frames):
            components = await self.analyzer.analyze(
                str(frame), timestamp_seconds=i * interval, framework_hint=framework
            )
            all_components.extend(components)

        # Deduplicate by ID
        seen = set()
        deduped = []
        for c in all_components:
            if c.id not in seen:
                seen.add(c.id)
                deduped.append(c)

        # Step 3: Generate code
        code = await self.generator.generate(deduped, framework, styling)

        return CodeGenerationResult(
            component_tree=deduped,
            generated_code=code,
            framework=framework,
            styling=styling,
            confidence=0.8 if all_components else 0.4,
        )


# Singleton
_pipeline_instance: VideoToCodePipeline | None = None


def get_video_pipeline() -> VideoToCodePipeline:
    """Get or create the singleton VideoToCodePipeline instance."""
    global _pipeline_instance
    if _pipeline_instance is None:
        _pipeline_instance = VideoToCodePipeline()
    return _pipeline_instance


@router.post("/process")
async def process_video(
    file: UploadFile = File(...),
    framework: str = "react",
    styling: str = "tailwind",
):
    """Upload a video and generate UI component code."""
    await validate_upload(file)

    if not file.content_type or not file.content_type.startswith("video/"):
        raise HTTPException(status_code=400, detail="File must be a video")

    # Save temporarily
    import tempfile

    suffix = Path(file.filename or "video.mp4").suffix or ".mp4"
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        tmp.write(await file.read())
        tmp_path = tmp.name

    try:
        result = await get_video_pipeline().process(tmp_path, framework, styling)
        return {
            "status": "success",
            "framework": result.framework,
            "styling": result.styling,
            "component_count": len(result.component_tree),
            "code": result.generated_code,
            "confidence": result.confidence,
        }
    finally:
        os.unlink(tmp_path)
