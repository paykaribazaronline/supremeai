import asyncio
from typing import Dict, Any
from loguru import logger

class MultilingualTTS:
    def __init__(self, provider: str = "auto"):
        self.provider = provider
        logger.info(f"Initialized MultilingualTTS with provider {self.provider}")

    def _detect_language(self, text: str) -> str:
        if any(0x0980 <= ord(c) <= 0x09FF for c in text):
            return "bn"
        if any(0x0600 <= ord(c) <= 0x06FF for c in text):
            return "ar"
        if any(0x4E00 <= ord(c) <= 0x9FFF for c in text):
            return "zh"
        return "en"

    async def synthesize(self, text: str) -> Dict[str, Any]:
        lang = self._detect_language(text)
        logger.info(f"Synthesizing speech in language {lang} using {self.provider}")
        try:
            from tools.voice import VoiceInterface
            voice = VoiceInterface()
            output_path = f"data/tts_{hash(text)}.mp3"
            ok = await voice.text_to_speech_async(text, output_path)
            if ok:
                return {
                    "status": "success",
                    "language": lang,
                    "provider": "voice_interface",
                    "audio_path": output_path,
                    "audio_url": output_path,
                }
        except Exception as exc:
            logger.debug(f"VoiceInterface TTS failed: {exc}")
        return {
            "status": "error",
            "language": lang,
            "error": "All TTS providers failed.",
        }
