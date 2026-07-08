# 📄 ফাইল: backend/tests/core/test_integration_phase3.py

**প্রকার:** .py  
**সাইজ:** 1,801 বাইট  
**আপডেট:** 2026-07-08T19:34:18.911952

---

## কোড

```py
import pytest
import litellm
from unittest.mock import patch, MagicMock
from core.llm_gateway import llm_gateway
from utils.firestore_helpers import get_firestore_db


@pytest.fixture
def mock_db_integration():
    db = MagicMock()
    doc_ref_budget = MagicMock()
    snapshot_budget = MagicMock()
    snapshot_budget.exists = True
    snapshot_budget.to_dict.return_value = {"monthly_limit": 10.0, "spent_amount": 0.0}
    doc_ref_budget.get.return_value = snapshot_budget

    doc_ref_fixes = MagicMock()

    def collection_side_effect(path):
        col_mock = MagicMock()
        if "budget" in path:
            col_mock.document.return_value = doc_ref_budget
        elif "fixes" in path:
            col_mock.document.return_value = doc_ref_fixes
        return col_mock

    db.collection.side_effect = collection_side_effect
    return db, doc_ref_fixes


@pytest.mark.asyncio
@patch("core.llm_gateway.get_firestore_db")
@patch("litellm.acompletion")
async def test_llm_gateway_self_healer_integration(mock_acompletion, mock_get_firestore_db, mock_db_integration):
    db, doc_ref_fixes = mock_db_integration
    mock_get_firestore_db.return_value = db

    # Force acompletion to fail
    mock_acompletion.side_effect = Exception("LiteLLM RateLimitError")

    with pytest.raises(Exception, match="LiteLLM RateLimitError"):
        await llm_gateway.acompletion(prompt="Hello", model="openai/gpt-3.5-turbo", tenant_id="tenant-integration")

    # Verify SelfHealer was called and pending_review is saved
    doc_ref_fixes.set.assert_called_once()
    payload = doc_ref_fixes.set.call_args[0][0]

    assert payload.get("status") == "pending_review"
    assert "LiteLLM RateLimitError" in payload.get("error_pattern", "")
    assert "core.llm_gateway" in payload.get("dependency_tree", [])

```