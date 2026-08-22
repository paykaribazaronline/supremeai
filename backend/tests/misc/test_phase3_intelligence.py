"""
Unit tests for Phase 3 Intelligence Features:
- SyntheticDataPipeline instruction-tuning dataset export
- LearningLoop EWC loss penalty calculation
- VoiceService STT and TTS synthesis
- VisionService image and diagram analysis
"""

import pytest

from adaptive_engine.learning_loop import LearningLoop
from pipelines.synthetic_data_pipeline import SyntheticDataPipeline
from services.vision_service import VisionService
from services.voice_service import VoiceService


@pytest.mark.asyncio
async def test_synthetic_data_pipeline(tmp_path):
    pipeline = SyntheticDataPipeline()
    out_path = tmp_path / "test_ft.jsonl"
    result = await pipeline.generate_dataset(output_path=str(out_path))
    assert result["status"] == "success"
    assert out_path.exists()


def test_ewc_loss_penalty():
    loop = LearningLoop()
    cur_weights = {"w1": 0.5, "w2": 0.9}
    old_weights = {"w1": 0.4, "w2": 0.9}
    fisher = {"w1": 1.0, "w2": 1.0}
    penalty = loop.compute_ewc_loss_penalty(cur_weights, old_weights, fisher, ewc_lambda=0.5)
    assert penalty > 0.0


@pytest.mark.asyncio
async def test_voice_service():
    voice = VoiceService()
    stt_res = await voice.speech_to_text(b"fake_wav_data")
    assert stt_res["status"] == "success"
    assert len(stt_res["transcript"]) > 0

    tts_res = await voice.text_to_speech("সুপ্রিম এআই সিস্টেমে আপনাকে স্বাগতম।")
    assert tts_res["status"] == "success"
    assert tts_res["audio_bytes_length"] > 0


@pytest.mark.asyncio
async def test_vision_service():
    vision = VisionService()
    res = await vision.analyze_image(b"fake_image_bytes", query="Analyze architecture")
    assert res["status"] == "success"
    assert "architecture" in res["analysis"].lower()
