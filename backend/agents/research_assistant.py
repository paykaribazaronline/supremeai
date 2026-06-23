#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# ============================================================================
# file >> research_assistant.py
# project >> SupremeAI 2.0
# purpose >> Research agent
# module >> agents
# ============================================================================
import re
from typing import Any, Dict, List
from loguru import logger
import httpx


class ResearchAssistant:
    """
    ArXiv / Semantic Scholar paper search, summarization, and citation extraction.
    Closes Gap #44
    """

    ARXIV_API = "https://export.arxiv.org/api/query"
    SEMANTIC_SCHOLAR_API = "https://api.semanticscholar.org/graph/v1/paper/search"

    def __init__(self):
        logger.info("Initialized ResearchAssistant")

    def search(self, query: str, source: str = "arxiv", max_results: int = 5) -> List[Dict[str, Any]]:
        if source == "semantic_scholar":
            return self._search_semantic_scholar(query, max_results)
        return self._search_arxiv(query, max_results)

    def _search_arxiv(self, query: str, max_results: int = 5) -> List[Dict[str, Any]]:
        try:
            params = {"search_query": f"all:{query}", "start": 0, "max_results": max_results}
            resp = httpx.get(self.ARXIV_API, params=params, timeout=15.0)
            resp.raise_for_status()
            return self._parse_arxiv_xml(resp.text)
        except Exception as exc:
            logger.debug(f"ArXiv search failed: {exc}")
            return []

    def _parse_arxiv_xml(self, xml: str) -> List[Dict[str, Any]]:
        import xml.etree.ElementTree as ET
        ns = {"atom": "http://www.w3.org/2005/Atom", "arxiv": "http://arxiv.org/schemas/atom"}
        root = ET.fromstring(xml)
        papers: List[Dict[str, Any]] = []
        for entry in root.findall("atom:entry", ns):
            arxiv_id = ""
            id_el = entry.find("atom:id", ns)
            if id_el is not None and id_el.text:
                m = re.search(r"abs/([^/\s]+)", id_el.text)
                arxiv_id = m.group(1) if m else (id_el.text or "")
            title = (entry.findtext("atom:title", default="", namespaces=ns) or "").strip()
            title = re.sub(r"\s+", " ", title)
            summary = (entry.findtext("atom:summary", default="", namespaces=ns) or "").strip()
            published = entry.findtext("atom:published", default="", namespaces=ns) or ""
            authors = [a.findtext("atom:name", default="", namespaces=ns) or "" for a in entry.findall("atom:author", ns)]
            link = ""
            for link_el in entry.findall("atom:link", ns):
                if link_el.get("rel") == "alternate":
                    link = link_el.get("href", "")
                    break
            papers.append({
                "source": "arxiv",
                "arxiv_id": arxiv_id,
                "title": title,
                "authors": authors[:5],
                "abstract": summary[:1000],
                "published": published[:10],
                "url": link or f"https://arxiv.org/abs/{arxiv_id}",
                "citations": None,
            })
        return papers

    def _search_semantic_scholar(self, query: str, max_results: int = 5) -> List[Dict[str, Any]]:
        try:
            params = {"query": query, "limit": max_results, "fields": "title,authors,abstract,year,url,citationCount"}
            resp = httpx.get(self.SEMANTIC_SCHOLAR_API, params=params, timeout=15.0)
            resp.raise_for_status()
            data = resp.json()
            papers: List[Dict[str, Any]] = []
            for item in (data.get("data") or [])[:max_results]:
                papers.append({
                    "source": "semantic_scholar",
                    "arxiv_id": "",
                    "title": item.get("title", ""),
                    "authors": [a.get("name", "") for a in (item.get("authors") or [])[:5]],
                    "abstract": (item.get("abstract") or "")[:1000],
                    "published": str(item.get("year", ""))[:4],
                    "url": item.get("url", ""),
                    "citations": item.get("citationCount"),
                })
            return papers
        except Exception as exc:
            logger.debug(f"Semantic Scholar search failed: {exc}")
            return []

    def summarize(self, paper: Dict[str, Any]) -> Dict[str, Any]:
        abstract = paper.get("abstract", "")
        if not abstract:
            return {"summary": "No abstract available.", "key_points": [], "limitations": []}
        try:
            from brain.model_router import ModelRouter
            router = ModelRouter()
            prompt = (
                "Summarize the following research paper abstract into exactly 3 concise bullet points. "
                "Return JSON: {\"summary\": \"...\", \"key_points\": [...], \"limitations\": [\"...\"]}.\n\n"
                f"Title: {paper.get('title', 'N/A')}\nAbstract: {abstract}"
            )
            result = router.route_and_generate(prompt, task_type="reasoning", max_cost=0.02)
            text = result.get("text", "") if isinstance(result, dict) else ""
            text = text.strip()
            if "```json" in text:
                text = text.split("```json")[1].split("```")[0].strip()
            elif "```" in text:
                text = text.split("```")[1].split("```")[0].strip()
            import json
            parsed = json.loads(text)
            if isinstance(parsed, dict):
                return {
                    "summary": parsed.get("summary", ""),
                    "key_points": parsed.get("key_points", [])[:3],
                    "limitations": parsed.get("limitations", [])[:2],
                    "source": paper.get("source"),
                    "url": paper.get("url"),
                }
        except Exception as exc:
            logger.debug(f"Paper summarization failed: {exc}")
        return {
            "summary": abstract[:300] + "...",
            "key_points": [abstract[0:150]],
            "limitations": ["LLM summarization unavailable"],
            "source": paper.get("source"),
            "url": paper.get("url"),
        }

    def citations(self, paper: Dict[str, Any], style: str = "apa") -> str:
        authors = paper.get("authors", [])
        year = paper.get("published", "n.d.")[:4]
        title = paper.get("title", "Untitled")
        url = paper.get("url", "")
        if style == "apa":
            auth_str = ", ".join(authors[:3]) + (" et al." if len(authors) > 3 else "")
            return f"{auth_str} ({year}). {title}. Retrieved from {url}"
        if style == "mla":
            auth_str = ", ".join(authors[:2])
            return f'{auth_str}. "{title}." {year}. Web. {url}.'
        return f"{title} — {', '.join(authors)} ({year}). {url}"
