from fastapi import APIRouter
from pydantic import BaseModel

from tools.image_generator import HFImageGenerator
from tools.video_generator import VideoGenerator


router = APIRouter(prefix="/api/media", tags=["media"])
image_generator = HFImageGenerator()
video_generator = VideoGenerator()


class ImageRequest(BaseModel):
    prompt: str
    model: str | None = None
    output_path: str | None = "data/generated_image.png"


class VideoRequest(BaseModel):
    prompt: str
    duration: int | None = 5
    provider: str | None = "auto"
    output_path: str | None = "data/generated_video.mp4"


class MediaResponse(BaseModel):
    success: bool
    provider: str
    prompt: str
    output_path: str
    mock: bool
    duration: int | None = None
    error: str | None = None


@router.post("/generate/image", response_model=MediaResponse)
async def generate_image(req: ImageRequest):
    out_path = req.output_path or "data/generated_image.png"
    result = image_generator.generate_image(
        req.prompt, model=req.model, output_path=out_path
    )
    return MediaResponse(
        success=result.get("success", False),
        provider=result.get("model", result.get("provider", "")),
        prompt=req.prompt,
        output_path=result.get("output_path", out_path),
        mock=result.get("mock", False),
        error=result.get("error"),
    )


@router.post("/generate/video", response_model=MediaResponse)
async def generate_video(req: VideoRequest):
    out_path = req.output_path or "data/generated_video.mp4"
    duration = req.duration or 5
    provider = req.provider or "auto"
    result = video_generator.generate(
        req.prompt, duration=duration, provider=provider, output_path=out_path
    )
    return MediaResponse(
        success=result.get("success", False),
        provider=result.get("provider", ""),
        prompt=req.prompt,
        output_path=result.get("output_path", out_path),
        mock=result.get("mock", False),
        duration=result.get("duration", req.duration),
        error=result.get("error"),
    )
