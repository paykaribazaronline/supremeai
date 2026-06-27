import os
import tempfile
from typing import Any

from fastapi import APIRouter
from fastapi import File
from fastapi import UploadFile
from fastapi import WebSocket
from fastapi import WebSocketDisconnect
from loguru import logger


router = APIRouter(prefix="/voice", tags=["voice-coder"])


class VoiceCoder:
    def __init__(self, stt_engine: str = "whisper", tts_engine: str = "auto"):
        from tools.voice import VoiceInterface

        self.voice = VoiceInterface()
        self.stt_engine = stt_engine
        self.tts_engine = tts_engine
        logger.info(f"Initialized VoiceCoder with STT: {stt_engine}, TTS: {tts_engine}")

    async def process_voice_command(self, audio_filepath: str) -> dict[str, Any]:
        logger.info(f"Processing voice command from {audio_filepath}")
        try:
            transcript = await self.voice.speech_to_text_async(audio_filepath)
            if not transcript:
                return {"status": "error", "error": "Transcription failed or empty."}
            logger.info(f"Transcribed: '{transcript}'")

            action, code = await self._classify_and_execute(transcript)

            feedback_text = f"Done. {action}."
            try:
                audio_feedback = await self.voice.text_to_speech_async(feedback_text)
            except Exception:
                audio_feedback = None

            return {
                "status": "success",
                "transcript": transcript,
                "action": action,
                "code": code,
                "feedback_audio": audio_feedback,
            }
        except Exception as e:
            logger.error(f"Voice coding failed: {str(e)}")
            return {"status": "error", "error": str(e)}

    async def _classify_and_execute(self, transcript: str):
        """Classify intent and generate code or explain."""
        lower = transcript.lower()

        if any(w in lower for w in ["generate", "write", "create", "build", "make"]):
            code = await self._generate_code_from_instruction(transcript)
            return "generate_code", code
        elif any(w in lower for w in ["explain", "what is", "how does"]):
            explanation = await self._explain(transcript)
            return "explanation", explanation
        elif any(w in lower for w in ["fix", "debug", "error"]):
            code = await self._generate_code_from_instruction(f"Fix the following: {transcript}")
            return "fix_code", code
        else:
            code = await self._generate_code_from_instruction(transcript)
            return "generate_code", code

    async def _generate_code_from_instruction(self, instruction: str) -> str:
        try:
            from brain.model_router import ModelRouter

            router = ModelRouter()
            prompt = (
                "You are an expert developer. Generate clean, production-ready code for the "
                "following request. Return only the code, no explanations.\n\n"
                f"Request: {instruction}"
            )
            result = await router.async_route_and_generate(prompt, task_type="coding", max_cost=0.03)
            text = result.get("text", "") if isinstance(result, dict) else ""
            if not text:
                return f"# Could not generate code for: {instruction}\n"
            return text
        except Exception as e:
            logger.error(f"Code generation failed: {e}")
            return f"# Error generating code: {e}\n"

    async def _explain(self, question: str) -> str:
        try:
            from brain.model_router import ModelRouter

            router = ModelRouter()
            result = await router.async_route_and_generate(question, task_type="general", max_cost=0.01)
            return result.get("text", "") if isinstance(result, dict) else ""
        except Exception as e:
            return f"Could not explain: {e}"


voice_coder = VoiceCoder()


@router.post("/process-audio")
async def process_audio(file: UploadFile = File(...)):
    """Upload an audio file and get code generated from it."""
    try:
        suffix = os.path.splitext(file.filename or "audio.wav")[1] or ".wav"
        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
            tmp.write(await file.read())
            tmp_path = tmp.name

        result = await voice_coder.process_voice_command(tmp_path)
        os.unlink(tmp_path)
        return result
    except Exception as e:
        logger.error(f"Audio processing failed: {e}")
        return {"status": "error", "error": str(e)}


@router.websocket("/ws")
async def voice_ws(websocket: WebSocket):
    """WebSocket for real-time voice coding sessions."""
    await websocket.accept()
    logger.info("Voice coder WebSocket connected")
    try:
        while True:
            data = await websocket.receive_bytes()
            # Save incoming audio chunk to temp file and process
            with tempfile.NamedTemporaryFile(delete=False, suffix=".wav") as tmp:
                tmp.write(data)
                tmp_path = tmp.name
            result = await voice_coder.process_voice_command(tmp_path)
            os.unlink(tmp_path)
            await websocket.send_json(result)
    except WebSocketDisconnect:
        logger.info("Voice coder WebSocket disconnected")
    except Exception as e:
        logger.error(f"Voice WebSocket error: {e}")
        await websocket.close()
