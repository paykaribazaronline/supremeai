"""
Tests for services/video_to_code_pipeline.py
Focus: constants, format detection, and fallback behaviour.
"""

from __future__ import annotations

import pytest
from services.video_to_code_pipeline import (FRAME_INTERVAL_SECONDS,
                                             MAX_VIDEO_SIZE_MB,
                                             VIDEO_CACHE_TTL, UIComponent,
                                             VideoFormat, VideoFrameExtractor)


def test_constants():
    assert FRAME_INTERVAL_SECONDS == 2
    assert MAX_VIDEO_SIZE_MB == 50
    assert VIDEO_CACHE_TTL == 3600


def test_video_format_enum_values():
    assert VideoFormat.MP4 == "mp4"
    assert VideoFormat.AUTO == "auto"


def test_ui_component_dataclass():
    c = UIComponent(
        id="c1",
        component_type="button",
        framework_hint="react",
        position=(10, 20, 100, 40),
        properties={"label": "Submit"},
        detected_text="Submit",
    )
    assert c.component_type == "button"
    assert c.position == (10, 20, 100, 40)


def test_video_frame_extractor_check_ffmpeg_false(monkeypatch):
    extractor = VideoFrameExtractor()

    def fake_run(*args, **kwargs):
        raise FileNotFoundError

    monkeypatch.setattr("subprocess.run", fake_run)
    assert extractor._check_ffmpeg() is False


@pytest.mark.anyio
async def test_extract_frames_fallback_when_no_ffmpeg(tmp_path, monkeypatch):
    extractor = VideoFrameExtractor()

    def fake_run(*args, **kwargs):
        raise FileNotFoundError

    monkeypatch.setattr("subprocess.run", fake_run)
    video = tmp_path / "vid.mp4"
    video.write_bytes(b"fake")
    frames = await extractor.extract_frames(str(video), max_frames=3)
    assert isinstance(frames, list)
