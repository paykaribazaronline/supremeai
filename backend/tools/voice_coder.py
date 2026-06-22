import asyncio
from typing import Dict, Any, Optional
from loguru import logger
from tools.voice import VoiceInterface

class VoiceCoder:
    def __init__(self, stt_engine: str = "whisper", tts_engine: str = "auto"):
        self.voice = VoiceInterface()
        self.stt_engine = stt_engine
        self.tts_engine = tts_engine
        logger.info(f"Initialized VoiceCoder with STT: {stt_engine}, TTS: {tts_engine}")

    async def process_voice_command(self, audio_filepath: str) -> Dict[str, Any]:
        logger.info(f"Processing voice command from {audio_filepath}")
        try:
            transcript = await self.voice.speech_to_text_async(audio_filepath)
            if not transcript:
                return {"status": "error", "error": "Transcription failed or empty."}
            logger.info(f"Transcribed: '{transcript}'")
            
            if transcript.lower().startswith("generate "):
                instruction = transcript[len("generate "):].strip()
                code = await self._generate_code_from_instruction(instruction)
                audio_feedback = await self.voice.text_to_speech_async("Done. I have generated the code you requested.")
                return {
                    "status": "success",
                    "transcript": transcript,
                    "action": "generate_code",
                    "code": code,
                    "feedback_audio": audio_feedback,
                }
            
            return {
                "status": "success",
                "transcript": transcript,
                "action": "transcribed",
                "text": transcript,
            }
        except Exception as e:
            logger.error(f"Voice coding failed: {str(e)}")
            return {"status": "error", "error": str(e)}

    async def _generate_code_from_instruction(self, instruction: str) -> str:
        try:
            from brain.model_router import ModelRouter
            router = ModelRouter()
            prompt = f"You are an expert developer. Generate clean, production-ready code for the following request. Return only the code, no explanations.\n\nRequest: {instruction}"
            result = router.async_route_and_generate(prompt, task_type="coding", max_cost=0.02)
            text = result.get("text", "") if isinstance(result, dict) else ""
            if not text:
                return f"# Could not generate code for: {instruction}\n"
            return text
        except Exception as e:
            logger.error(f"Code generation failed: {e}")
            return f"# Error generating code: {e}\n"

    async def stream_voice_session(self, audio_stream):
        raise NotImplementedError("Streaming voice coding is not yet implemented.")
