"""
Coverage tests for tools/knowledge/local_search_rag.py.
Target: 100% line coverage.

লোকাল সার্চ RAG মডিউলের সকল ফাংশন ও শাখা কভার করা হয়েছে।
"""

import json
import os
import sys
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

_PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if _PROJECT_ROOT not in sys.path:
    sys.path.insert(0, _PROJECT_ROOT)


class TestSearchResult:
    """Tests for SearchResult class."""

    def test_search_result_creation(self):
        """SearchResult should store title, url, snippet, content."""
        from tools.knowledge.local_search_rag import SearchResult

        sr = SearchResult(
            title="Test Title",
            url="https://example.com",
            snippet="A snippet",
            content="Full content here",
        )
        assert sr.title == "Test Title"
        assert sr.url == "https://example.com"

    def test_search_result_to_dict(self):
        """to_dict should return a dictionary representation."""
        from tools.knowledge.local_search_rag import SearchResult

        sr = SearchResult("Title", "https://example.com", "Snippet", "Content")
        d = sr.to_dict()
        assert d["title"] == "Title"
        assert d["url"] == "https://example.com"
        assert d["snippet"] == "Snippet"
        assert d["content"] == "Content"


class TestLocalSearchRAGInit:
    """Tests for LocalSearchRAG.__init__."""

    def test_init_defaults(self):
        """LocalSearchRAG should initialize with default values."""
        from tools.knowledge.local_search_rag import LocalSearchRAG

        with (
            patch("tools.knowledge.local_search_rag.BrowserAgent"),
            patch("pathlib.Path.mkdir"),
            patch.object(Path, "exists", return_value=False),
        ):
            rag = LocalSearchRAG()
            assert rag.max_pages == 5
            assert rag.max_chars == 4000
            assert rag._index == {}

    def test_init_with_chromadb(self):
        """LocalSearchRAG should try to initialize ChromaDB."""
        from tools.knowledge.local_search_rag import LocalSearchRAG

        with (
            patch("tools.knowledge.local_search_rag.BrowserAgent"),
            patch("pathlib.Path.mkdir"),
            patch.object(Path, "exists", return_value=False),
            patch("tools.knowledge.local_search_rag.chromadb") as mock_chromadb,
        ):
            mock_client = MagicMock()
            mock_chromadb.PersistentClient.return_value = mock_client
            rag = LocalSearchRAG()
            assert rag.chroma_client is not None

    def test_init_without_chromadb(self):
        """LocalSearchRAG should handle missing chromadb gracefully."""
        from tools.knowledge.local_search_rag import LocalSearchRAG

        with (
            patch("tools.knowledge.local_search_rag.BrowserAgent"),
            patch("pathlib.Path.mkdir"),
            patch.object(Path, "exists", return_value=False),
            patch.dict("sys.modules", {"chromadb": None}),
        ):
            rag = LocalSearchRAG()
            assert rag.chroma_client is None

    def test_init_loads_existing_index(self):
        """LocalSearchRAG should load existing index from disk."""
        from tools.knowledge.local_search_rag import LocalSearchRAG

        test_index = {"key1": ["result1", "result2"]}
        with (
            patch("tools.knowledge.local_search_rag.BrowserAgent"),
            patch("pathlib.Path.mkdir"),
            patch.object(Path, "exists", return_value=True),
            patch.object(Path, "read_text", return_value=json.dumps(test_index)),
        ):
            rag = LocalSearchRAG()
            assert rag._index == test_index

    def test_init_loads_corrupted_index(self):
        """LocalSearchRAG should handle corrupted index gracefully."""
        from tools.knowledge.local_search_rag import LocalSearchRAG

        with (
            patch("tools.knowledge.local_search_rag.BrowserAgent"),
            patch("pathlib.Path.mkdir"),
            patch.object(Path, "exists", return_value=True),
            patch.object(Path, "read_text", return_value="not valid json"),
        ):
            rag = LocalSearchRAG()
            assert rag._index == {}


class TestLocalSearchRAGSearch:
    """Tests for LocalSearchRAG.search method."""

    def test_search_with_browser(self):
        """search should use BrowserAgent for web search."""
        from tools.knowledge.local_search_rag import LocalSearchRAG

        mock_browser = MagicMock()
        mock_browser_instance = MagicMock()
        mock_browser_instance.fetch_page.return_value = {
            "success": True,
            "content": "Result 1\nhttps://example.com/1\nSnippet 1\nResult 2\nhttps://example.com/2\nSnippet 2",
        }
        mock_browser.return_value = mock_browser_instance

        with (
            patch("tools.knowledge.local_search_rag.BrowserAgent", return_value=mock_browser_instance),
            patch("pathlib.Path.mkdir"),
            patch.object(Path, "exists", return_value=False),
        ):
            rag = LocalSearchRAG()
            result = rag.search("test query")
            assert result["status"] == "ok"
            assert len(result.get("results", [])) > 0

    def test_search_with_local_index(self):
        """search should return results from local index."""
        from tools.knowledge.local_search_rag import LocalSearchRAG

        with (
            patch("tools.knowledge.local_search_rag.BrowserAgent"),
            patch("pathlib.Path.mkdir"),
            patch.object(Path, "exists", return_value=False),
        ):
            rag = LocalSearchRAG()
            rag._index = {"test": ["cached_result"]}
            result = rag.search("test")
            assert isinstance(result, dict)
            assert result.get("status") == "ok"


class TestLocalSearchRAGStore:
    """Tests for LocalSearchRAG.store method."""

    def test_store_and_retrieve(self):
        """store should save content and make it retrievable."""
        from tools.knowledge.local_search_rag import LocalSearchRAG

        with (
            patch("tools.knowledge.local_search_rag.BrowserAgent"),
            patch("pathlib.Path.mkdir"),
            patch.object(Path, "exists", return_value=False),
            patch.object(Path, "write_text"),
        ):
            rag = LocalSearchRAG()
            rag._store_search("test_key", {"test_key": ["test_key", "test_content"]})
            assert "test_key" in rag._index
            assert "test_content" in rag._index["test_key"]


class TestLocalSearchRAGSummarize:
    """Tests for LocalSearchRAG.summarize method."""

    @pytest.mark.asyncio
    async def test_summarize(self):
        """summarize should return a summary of content."""
        from tools.knowledge.local_search_rag import LocalSearchRAG

        with (
            patch("tools.knowledge.local_search_rag.BrowserAgent"),
            patch("pathlib.Path.mkdir"),
            patch.object(Path, "exists", return_value=False),
        ):
            rag = LocalSearchRAG()
            result = rag.summarize("https://example.com", "Full content here")
            assert isinstance(result, dict)
            assert result.get("status") == "ok"
