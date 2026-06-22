import pytest
import os
from unittest.mock import patch, MagicMock
from tools.model_trainer import ModelTrainer

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

    with patch.dict(os.environ, {"RUNPOD_API_KEY": "test-key"}):
        trainer = ModelTrainer(provider="runpod")
        res = await trainer.trigger_lora_finetune("tests/mock_dataset.jsonl", "llama3-8b")
        assert res["status"] == "success"
        assert res["job_id"] == "runpod-job-123"
        assert res["provider"] == "runpod"
        
    if os.path.exists("tests/mock_dataset.jsonl"):
        os.remove("tests/mock_dataset.jsonl")
