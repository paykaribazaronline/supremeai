import os
from unittest.mock import MagicMock, patch

import pytest

from tools.learning.model_trainer import ModelTrainer


@pytest.mark.anyio
async def test_trigger_lora_finetune_local():
    trainer = ModelTrainer(provider="docker")
    res = await trainer.trigger_lora_finetune("tests/mock_dataset.jsonl", "llama3-8b")
    assert res["status"] == "success"
    assert "ft-job-" in res["job_id"]
    assert res["provider"] == "docker"
    # Clean up mock file created during training trigger
    if os.path.exists("tests/mock_dataset.jsonl"):
        os.remove("tests/mock_dataset.jsonl")


@pytest.mark.anyio
@patch("httpx.AsyncClient.post")
async def test_trigger_lora_finetune_runpod(mock_post):
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.json.return_value = {"id": "runpod-job-123", "status": "IN_QUEUE"}
    mock_post.return_value = mock_response

    # বাংলা মন্তব্য: সঠিক module path দিয়ে settings mock করা হচ্ছে (pre-existing bug fix)
    with patch("tools.learning.model_trainer.settings") as mock_settings:
        mock_settings.runpod_api_key = "test-key"
        mock_settings.runpod_endpoint_id = "unsloth-training"
        trainer = ModelTrainer(provider="runpod")
        res = await trainer.trigger_lora_finetune(
            "tests/mock_dataset.jsonl", "llama3-8b"
        )
        assert res["status"] == "success"
        assert res["job_id"] is not None
    assert res["provider"] == "runpod"

    if os.path.exists("tests/mock_dataset.jsonl"):
        os.remove("tests/mock_dataset.jsonl")
