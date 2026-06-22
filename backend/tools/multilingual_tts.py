import os
import httpx
import hashlib
from typing import Dict, Any, Optional
from loguru import logger

ELEVENLABS_VOICES = {
    "en": "21m00Tcm4TlvDq8ikWAM",
    "bn": "21m00Tcm4TlvDq8ikWAM",
    "ar": "21m00Tcm4TlvDq8ikWAM",
    "zh": "21m00Tcm4TlvDq8ikWAM",
    "es": "21m00Tcm4TlvDq8ikWAM",
    "fr": "21m00Tcm4TlvDq8ikWAM",
    "de": "21m00Tcm4TlvDq8ikWAM",
    "hi": "21m00Tcm4TlvDq8ikWAM",
    "ja": "21m00Tcm4TlvDq8ikWAM",
    "ko": "21m00Tcm4TlvDq8ikWAM",
    "pt": "21m00Tcm4TlvDq8ikWAM",
    "ru": "21m00Tcm4TlvDq8ikWAM",
    "it": "21m00Tcm4TlvDq8ikWAM",
    "tr": "21m00Tcm4TlvDq8ikWAM",
    "pl": "21m00Tcm4TlvDq8ikWAM",
    "nl": "21m00Tcm4TlvDq8ikWAM",
    "sv": "21m00Tcm4TlvDq8ikWAM",
    "da": "21m00Tcm4TlvDq8ikWAM",
    "fi": "21m00Tcm4TlvDq8ikWAM",
    "el": "21m00Tcm4TlvDq8ikWAM",
    "he": "21m00Tcm4TlvDq8ikWAM",
    "cs": "21m00Tcm4TlvDq8ikWAM",
    "sk": "21m00Tcm4TlvDq8ikWAM",
    "ro": "21m00Tcm4TlvDq8ikWAM",
    "hu": "21m00Tcm4TlvDq8ikWAM",
    "uk": "21m00Tcm4TlvDq8ikWAM",
    "id": "21m00Tcm4TlvDq8ikWAM",
    "ms": "21m00Tcm4TlvDq8ikWAM",
    "th": "21m00Tcm4TlvDq8ikWAM",
    "vi": "21m00Tcm4TlvDq8ikWAM",
}

LANG_SCRIPTS = [
    ("bn", (0x0980, 0x09FF)),
    ("ar", (0x0600, 0x06FF)),
    ("he", (0x0590, 0x05FF)),
    ("hi", (0x0900, 0x097F)),
    ("zh", (0x4E00, 0x9FFF)),
    ("ja", (0x3040, 0x309F)),
    ("ko", (0xAC00, 0xD7AF)),
    ("th", (0x0E00, 0x0E7F)),
    ("ru", (0x0400, 0x04FF)),
    ("el", (0x0370, 0x03FF)),
]


class MultilingualTTS:
    def __init__(self, provider: str = "auto", api_key: Optional[str] = None):
        self.provider = provider
        self.api_key = api_key or os.getenv("ELEVENLABS_API_KEY", "")
        self.model_id = "eleven_multilingual_v2"
        logger.info(f"Initialized MultilingualTTS with provider={self.provider}, elevenlabs_available={bool(self.api_key)}")

    def _detect_language(self, text: str) -> str:
        for char in text:
            cp = ord(char)
            for lang, (lo, hi) in LANG_SCRIPTS:
                if lo <= cp <= hi:
                    return lang
        return "en"

    def _output_path(self, text: str, lang: str, fmt: str = "mp3") -> str:
        base_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "data", "tts")
        os.makedirs(base_dir, exist_ok=True)
        h = hashlib.sha256(text.encode("utf-8")).hexdigest()[:16]
        return os.path.join(base_dir, f"tts_{lang}_{h}.{fmt}")

    def _cache_ttl(self) -> int:
        try:
            return int(os.getenv("TTS_CACHE_TTL", "86400"))
        except ValueError:
            return 86400

    def _cache_path(self, text: str, lang: str) -> Optional[str]:
        out = self._output_path(text, lang)
        if os.path.exists(out):
            import time
            if (time.time() - os.path.getmtime(out)) < self._cache_ttl():
                return out
        return None

    async def synthesize(self, text: str, voice_id: Optional[str] = None, language: Optional[str] = None, output_path: Optional[str] = None) -> Dict[str, Any]:
        lang = language or self._detect_language(text)
        cached = self._cache_path(text, lang)
        if cached:
            logger.info(f"TTS cache hit for lang={lang}")
            return {
                "status": "success",
                "language": lang,
                "provider": "cache",
                "audio_path": cached,
                "audio_url": cached,
                "text_length": len(text),
            }

        out_path = output_path or self._output_path(text, lang)
        result = await self._elevenlabs(text, out_path, lang, voice_id=voice_id)
        if result["status"] == "success":
            return result
        result = await self._voice_fallback(text, out_path, lang)
        return result

    async def _elevenlabs(self, text: str, out_path: str, lang: str, voice_id: Optional[str] = None, stability: float = 0.5, similarity_boost: float = 0.75) -> Dict[str, Any]:
        if not self.api_key:
            return {"status": "skipped", "error": "ElevenLabs API key not configured"}
        if self.provider not in ("auto", "elevenlabs"):
            return {"status": "skipped", "error": f"Provider mismatch: {self.provider}"}

        v = voice_id or ELEVENLABS_VOICES.get(lang) or ELEVENLABS_VOICES["en"]
        url = f"https://api.elevenlabs.io/v1/text-to-speech/{v}"
        headers = {
            "xi-api-key": self.api_key,
            "Content-Type": "application/json",
        }
        payload = {
            "text": text,
            "model_id": self.model_id,
            "voice_settings": {"stability": stability, "similarity_boost": similarity_boost},
        }

        try:
            async with httpx.AsyncClient(timeout=60.0) as client:
                res = await client.post(url, headers=headers, json=payload)
                if res.status_code == 200:
                    os.makedirs(os.path.dirname(os.path.abspath(out_path)), exist_ok=True)
                    with open(out_path, "wb") as f:
                        f.write(res.content)
                    logger.info(f"ElevenLabs synthesis succeeded: {out_path}")
                    return {
                        "status": "success",
                        "language": lang,
                        "provider": "elevenlabs",
                        "audio_path": out_path,
                        "audio_url": out_path,
                        "text_length": len(text),
                        "voice_id": v,
                        "model_id": self.model_id,
                    }
                error_body = res.text
                logger.warning(f"ElevenLabs failed ({res.status_code}): {error_body[:300]}")
                return {"status": "error", "language": lang, "error": f"ElevenLabs error {res.status_code}: {error_body[:200]}"}
        except Exception as exc:
            logger.warning(f"ElevenLabs exception: {exc}")
            return {"status": "error", "language": lang, "error": str(exc)}

    async def _voice_fallback(self, text: str, out_path: str, lang: str) -> Dict[str, Any]:
        try:
            from tools.voice import VoiceInterface
            voice = VoiceInterface()
            ok = await voice.text_to_speech_async(text, out_path)
            if ok:
                logger.info(f"Fallback VoiceInterface TTS succeeded: {out_path}")
                return {
                    "status": "success",
                    "language": lang,
                    "provider": "voice_interface",
                    "audio_path": out_path,
                    "audio_url": out_path,
                    "text_length": len(text),
                }
        except Exception as exc:
            logger.debug(f"VoiceInterface fallback failed: {exc}")

        try:
            from gtts import gTTS
            tts = gTTS(text=text, lang=lang)
            tts.save(out_path)
            logger.info(f"gTTS fallback succeeded: {out_path}")
            return {"status": "success", "language": lang, "provider": "gtts", "audio_path": out_path, "audio_url": out_path}
        except Exception as exc:
            logger.debug(f"gTTS fallback failed: {exc}")

        return {"status": "error", "language": lang, "error": "All TTS providers failed."}

    def synthesize_stream(self, text: str, chunk_size: int = 200):
        chunks = [text[i:i + chunk_size] for i in range(0, len(text), chunk_size)]
        for chunk in chunks:
            yield chunk
