#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# ============================================================================
# file >> test_new_interfaces.py
# project >> SupremeAI 2.0
# purpose >> Unit testing and QC
# module >> tests
# ============================================================================
import os
from unittest.mock import MagicMock, patch
from tools.voice import VoiceInterface
from core.discord_bot import SupremeDiscordBot

def test_voice_interface_stt_missing_file():
    vi = VoiceInterface()
    res = vi.speech_to_text("non_existent_file.wav")
    assert res == ""

@patch("httpx.post")
def test_voice_interface_stt_success(mock_post):
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.json.return_value = {"text": "hello world"}
    mock_post.return_value = mock_response

    # Create dummy file
    dummy_path = "tests/dummy_audio.wav"
    os.makedirs("tests", exist_ok=True)
    with open(dummy_path, "wb") as f:
        f.write(b"dummy audio data")

    try:
        os.environ["HF_API_KEY"] = "mock_key"
        vi = VoiceInterface()
        res = vi.speech_to_text(dummy_path)
        assert res == "hello world"
    finally:
        if os.path.exists(dummy_path):
            os.remove(dummy_path)

def test_discord_bot_initialization():
    bot = SupremeDiscordBot()
    assert bot is not None

def test_local_ocr_extractor():
    from tools.local_ocr_extractor import LocalOCRExtractor
    import tempfile
    import os

    extractor = LocalOCRExtractor()
    assert extractor.languages == ["en", "bn"]

    # Test parse_to_rows
    text_table = "Header1 | Header2\nVal1 | Val2"
    parsed = extractor.parse_to_rows(text_table, columns=["h1", "h2"])
    assert parsed["success"] is True
    assert len(parsed["rows"]) == 2
    assert parsed["rows"][0] == {"h1": "Header1", "h2": "Header2"}

    # Test export_to_excel
    temp_dir = tempfile.gettempdir()
    excel_path = os.path.join(temp_dir, "test_ocr_export.xlsx")
    try:
        export_res = extractor.export_to_excel(parsed["rows"], excel_path)
        assert export_res["success"] is True
        assert os.path.exists(excel_path)
    finally:
        if os.path.exists(excel_path):
            os.remove(excel_path)

