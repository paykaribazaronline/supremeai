from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from loguru import logger

from backend.tools.multilingual_tts import MultilingualTTS

router = APIRouter()

# Initialize TTS engine (can be reused across requests)
tts_engine = MultilingualTTS()


@router.get("/stream_audio")
async def stream_audio(text: str, voice: str | None = None):
    """Stream TTS audio bytes in real-time for the given text.
    Uses ElevenLabs for primary synthesis (if API key configured) with fallback to edge-tts.
    """
    if not text or not text.strip():
        raise HTTPException(status_code=400, detail="Text cannot be empty")

    async def audio_stream():
        try:
            # Stream audio bytes from TTS engine (ElevenLabs with edge-tts fallback)
            async for chunk in tts_engine.synthesize_stream(
                text=text.strip(),
                voice_id=None,  # Use language-based voice for ElevenLabs; voice param for edge-tts fallback handled internally
            ):
                yield chunk
        except Exception as e:
            logger.error(f"Audio streaming failed: {e}")
            yield b""  # Return empty bytes on error to avoid breaking the stream

    return StreamingResponse(
        audio_stream(),
        media_type="audio/mpeg",
        headers={
            "Cache-Control": "no-cache, no-transform",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
            "Content-Disposition": "inline",
        },
    )
