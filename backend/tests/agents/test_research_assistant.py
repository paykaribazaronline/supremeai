import sys
from unittest.mock import patch


sys.path.append("..")
from agents.research_assistant import ResearchAssistant


class TestResearchAssistant:
    def test_init(self):
        assistant = ResearchAssistant()
        assert assistant is not None

    def test_search_arxiv(self):
        assistant = ResearchAssistant()
        with patch("httpx.get") as mock_get:
            mock_get.return_value.raise_for_status = lambda: None
            mock_get.return_value.text = """<?xml version="1.0"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <id>http://arxiv.org/abs/1234.5678</id>
                <title>Test Paper</title>
                <summary>Test abstract.</summary>
                <published>2024-01-01</published>
                <author><name>Author One</name></author>
                <link rel="alternate" href="http://arxiv.org/abs/1234.5678"/>
              </entry>
            </feed>"""
            results = assistant.search("test query", source="arxiv", max_results=1)
            assert len(results) == 1
            assert results[0]["source"] == "arxiv"
            assert "title" in results[0]

    def test_search_semantic_scholar(self):
        assistant = ResearchAssistant()
        with patch("httpx.get") as mock_get:
            mock_get.return_value.raise_for_status = lambda: None
            mock_get.return_value.json.return_value = {
                "data": [
                    {"title": "SS Paper", "authors": [{"name": "A1"}], "abstract": "abs", "year": 2024, "url": "u", "citationCount": 10}
                ]
            }
            results = assistant.search("test", source="semantic_scholar", max_results=1)
            assert len(results) == 1
            assert results[0]["source"] == "semantic_scholar"

    def test_search_fallback(self):
        assistant = ResearchAssistant()
        with patch("httpx.get", side_effect=Exception("network error")):
            results = assistant.search("test query")
            assert results == []

    def test_summarize_with_abstract(self):
        assistant = ResearchAssistant()
        paper = {"title": "T", "abstract": "This is an abstract about AI.", "source": "arxiv", "url": "http://e.c"}
        with patch("brain.model_router.ModelRouter") as MockRouter:
            mock_router = MockRouter.return_value
            mock_router.route_and_generate.return_value = {"text": '```json\n{"summary": "AI paper", "key_points": ["a", "b"], "limitations": ["l"]}\n```'}
            result = assistant.summarize(paper)
            assert "summary" in result
            assert "key_points" in result

    def test_summarize_no_abstract(self):
        assistant = ResearchAssistant()
        paper = {"title": "T"}
        result = assistant.summarize(paper)
        assert "summary" in result
        assert len(result["key_points"]) == 0

    def test_citations_apa(self):
        assistant = ResearchAssistant()
        paper = {"authors": ["One, A.", "Two, B."], "published": "2024", "title": "My Paper", "url": "http://e.c"}
        result = assistant.citations(paper, style="apa")
        assert "2024" in result
        assert "My Paper" in result

    def test_citations_mla(self):
        assistant = ResearchAssistant()
        paper = {"authors": ["One, A.", "Two, B."], "published": "2024", "title": "My Paper", "url": "http://e.c"}
        result = assistant.citations(paper, style="mla")
        assert "2024" in result

    def test_citations_fallback(self):
        assistant = ResearchAssistant()
        paper = {"authors": ["One, A.", "Two, B.", "Three, C.", "Four, D."], "published": "2024", "title": "My Paper", "url": "http://e.c"}
        result = assistant.citations(paper, style="unknown")
        assert "My Paper" in result
