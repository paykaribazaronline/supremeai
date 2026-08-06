"""
Tests for services/knowledge_qa.py — KnowledgeQAService
"""

from __future__ import annotations

from unittest.mock import MagicMock

import pytest
from services.knowledge_qa import Citation, KnowledgeQAService


class TestCitation:
    def test_as_dict_with_all_fields(self):
        citation = Citation(
            document_id="doc-123",
            source="https://example.com/doc",
            chunk_index=0,
            score=0.95,
        )
        result = citation.as_dict()
        assert result["document_id"] == "doc-123"
        assert result["source"] == "https://example.com/doc"
        assert result["chunk_index"] == 0
        assert result["score"] == 0.95

    def test_as_dict_with_none_chunk_index(self):
        citation = Citation(
            document_id="doc-456",
            source="https://example.com/other",
            chunk_index=None,
            score=0.5,
        )
        result = citation.as_dict()
        assert result["chunk_index"] is None

    def test_score_rounding(self):
        citation = Citation(
            document_id="doc-789",
            source="source",
            chunk_index=1,
            score=0.95555,
        )
        result = citation.as_dict()
        assert result["score"] == 0.9556


class TestKnowledgeQAService:
    @pytest.fixture
    def service(self):
        return KnowledgeQAService(vector_store=MagicMock(), gateway=MagicMock())

    def test_init(self, service):
        assert service.vector_store is not None
        assert service.gateway is not None

    def test_init_with_defaults(self):
        service = KnowledgeQAService()
        assert hasattr(service, "gateway")

    def test_min_retrieval_score_constant(self):
        from services.knowledge_qa import MIN_RETRIEVAL_SCORE

        assert MIN_RETRIEVAL_SCORE == 0.05

    def test_max_context_chars_constant(self):
        from services.knowledge_qa import MAX_CONTEXT_CHARS

        assert MAX_CONTEXT_CHARS == 12_000
