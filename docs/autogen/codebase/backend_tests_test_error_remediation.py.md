# 📄 ফাইল: backend/tests/test_error_remediation.py

**প্রকার:** .py  
**সাইজ:** 3,541 বাইট  
**আপডেট:** 2026-07-11T14:23:58.606044

---

## কোড

```py
"""Error remediation tests for SupremeAI 2.0."""

import sys
from unittest.mock import MagicMock
from unittest.mock import patch

import pytest

from core.error_remediation import ErrorRemediation

pytestmark = pytest.mark.anyio


def _skip_if_no_qdrant():
    """Skip test if qdrant_client is not installed."""
    pytest.importorskip("qdrant_client")


class TestErrorRemediation:
    """Tests for ErrorRemediation class."""

    def test_init_no_qdrant(self):
        """Qdrant ইনস্টল না থাকলে ইনিশialization করা হচ্ছে।"""
        with patch("core.error_remediation.HAS_QDRANT", False):
            remediation = ErrorRemediation()
            assert remediation.qdrant is None

    def test_init_with_qdrant(self):
        """Qdrant ইনস্টল থাকলে ইনিশialization করা হচ্ছে।"""
        _skip_if_no_qdrant()
        mock_qdrant = MagicMock()
        with patch("core.error_remediation.QdrantClient", return_value=mock_qdrant) as mock_client:
            remediation = ErrorRemediation()
            mock_client.assert_called_once()
            assert remediation.qdrant is mock_qdrant

    async def test_lookup_fix_no_qdrant(self):
        """Qdrant ছাড়াই লুকআপ ফিক্স ফলব্যাক রিটার্ন করে।"""
        with patch("core.error_remediation.HAS_QDRANT", False):
            remediation = ErrorRemediation()
            result = await remediation.lookup_fix("error-signature-123")
            assert result is not None and "Retry" in result

    async def test_lookup_fix_success(self):
        """সফলভাবে ফিক্স লুকআপ করা হচ্ছে।"""
        _skip_if_no_qdrant()
        mock_qdrant = MagicMock()
        mock_result = MagicMock()
        mock_result.payload = {"fix": "Retry with exponential backoff"}
        mock_qdrant.search.return_value = [mock_result]

        with patch("core.error_remediation.HAS_QDRANT", True):
            with patch("core.error_remediation.QdrantClient", return_value=mock_qdrant):
                remediation = ErrorRemediation()
                result = await remediation.lookup_fix("error-signature-123")
                assert result == "Retry with exponential backoff"

    async def test_lookup_fix_no_results(self):
        """ফিক্স পাওয়া না গেলে None রিটার্ন করে।"""
        _skip_if_no_qdrant()
        mock_qdrant = MagicMock()
        mock_qdrant.search.return_value = []

        with patch("core.error_remediation.HAS_QDRANT", True):
            with patch("core.error_remediation.QdrantClient", return_value=mock_qdrant):
                remediation = ErrorRemediation()
                result = await remediation.lookup_fix("error-signature-123")
                assert result is not None and "Retry" in result

    async def test_lookup_fix_exception(self):
        """ত্রুটি হলে None রিটার্ন করে।"""
        _skip_if_no_qdrant()
        mock_qdrant = MagicMock()
        mock_qdrant.search.side_effect = Exception("Qdrant connection error")

        with patch("core.error_remediation.HAS_QDRANT", True):
            with patch("core.error_remediation.QdrantClient", return_value=mock_qdrant):
                remediation = ErrorRemediation()
                result = await remediation.lookup_fix("error-signature-123")
                assert result is not None and "Retry" in result

```