import pytest
import os
import shutil
from tools.rlhf_pipeline import RLHFPipeline

@pytest.fixture
def rlhf_pipeline():
    test_dir = "data/test_rlhf"
    pipeline = RLHFPipeline(storage_dir=test_dir)
    yield pipeline
    if os.path.exists(test_dir):
        shutil.rmtree(test_dir)

@pytest.mark.anyio
async def test_record_and_export(rlhf_pipeline):
    res = rlhf_pipeline.record_preference("Question", "Good Answer", "Bad Answer")
    assert res["status"] == "success"
    assert res["recorded"] == 1

    export_res = await rlhf_pipeline.export_dpo_dataset()
    assert export_res["status"] == "success"
    assert export_res["exported"] == 1
    assert os.path.exists(export_res["output_path"])

@pytest.mark.anyio
async def test_dpo_training_trigger(rlhf_pipeline):
    # Verify fallback to ModelTrainer
    res = await rlhf_pipeline.trigger_dpo_training("gpt2")
    assert res["status"] == "success"
    assert res["method"] in ("local_trl", "model_trainer_delegation")
