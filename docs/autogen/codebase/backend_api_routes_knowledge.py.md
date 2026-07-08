# 📄 ফাইল: backend/api/routes/knowledge.py

**প্রকার:** .py  
**সাইজ:** 4,801 বাইট  
**আপডেট:** 2026-07-08T03:02:32.614868

---

## কোড

```py
import os
import sys
from typing import Any

from fastapi import APIRouter
from fastapi import HTTPException
from loguru import logger
from pydantic import BaseModel


router = APIRouter(prefix="/api/knowledge", tags=["knowledge"])

sys.path.insert(
    0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
)

try:
    from tools.local_search_rag import LocalSearchRAG as LocalSearchRAGClass
except ImportError:
    LocalSearchRAGClass: Any = None

try:
    from tools.knowledge_base_indexer import KnowledgeBaseIndexer as KnowledgeBaseIndexerClass
except ImportError:
    KnowledgeBaseIndexerClass: Any = None

try:
    import sqlite3
except ImportError:
    sqlite3 = None  # type: ignore

DB_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
    "knowledge_store.db",
)


class KnowledgeSearchRequest(BaseModel):
    query: str
    limit: int = 5
    use_fts: bool = True


class KnowledgeSearchResult(BaseModel):
    id: str
    title: str
    content: str
    score: float | None = None
    source: str | None = None


def _fts_search(query: str, limit: int = 5) -> list[dict[str, Any]]:
    if sqlite3 is None:
        raise RuntimeError("sqlite3 module is not available")
    if not os.path.exists(DB_PATH):
        return []
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    try:
        cursor = conn.cursor()
        cursor.execute(
            "SELECT id, title, content, source, bm25(knowledge_fts) AS score FROM knowledge_fts WHERE knowledge_fts MATCH ? ORDER BY score LIMIT ?",
            [query, limit],
        )
        rows = cursor.fetchall()
        return [dict(r) for r in rows]
    except Exception as exc:  # noqa: BLE001
        # বল মনতবয: করপট query ব FTS তরটত 500 এড়ত খল লসট রটরন কর হয়;
        # তব করণট যন হরয় ন যয় সজনয ডবগ লগ যকত কর হল
        logger.debug(f"FTS query execution failed for {query!r}: {exc}")
        return []
    finally:
        conn.close()


@router.post("/seed")
async def index_seed_data():
    if KnowledgeBaseIndexerClass is None:
        raise HTTPException(status_code=500, detail="KnowledgeBaseIndexer unavailable")
    indexer = KnowledgeBaseIndexerClass()
    result = indexer.index_seed_data()
    return result


@router.get("/search", response_model=list[KnowledgeSearchResult])
async def search_knowledge(q: str, limit: int = 5) -> list[KnowledgeSearchResult]:
    results: list[dict[str, Any]] = []
    if sqlite3 is not None:
        try:
            results = _fts_search(q, limit)
        except Exception as exc:  # noqa: BLE001
            # বল মনতবয: SQLite FTS সার্চ বযরথ হল RAG ফলবযাক বযবহত হয়;
            # খল ফলাফল নরব রটরন ন কর warning লগ কর হল
            logger.warning(f"FTS knowledge search failed for query {q!r}: {exc}")
            results = []
    if not results and LocalSearchRAGClass is not None:
        try:
            rag = LocalSearchRAGClass()
            rag_results = rag.semantic_search(q)
            matches = (
                rag_results.get("matches", []) if isinstance(rag_results, dict) else []
            )
            for m in matches:
                results.append(
                    {
                        "id": m.get("doc_id"),
                        "title": m.get("title", ""),
                        "content": m.get("text", ""),
                        "score": m.get("score"),
                        "source": "chromadb",
                    }
                )
        except Exception as exc:  # noqa: BLE001
            # বল মনতবয: RAG সমযান্টক সার্চ বযরথ হল খল রজাল্ট নরব রটরন হত;
            # এখন warning লগ কর হয় যত search বযরথতর কারণ বঝ যায়
            logger.warning(f"RAG semantic knowledge search failed for query {q!r}: {exc}")
    formatted: list[KnowledgeSearchResult] = []
    for row in results[:limit]:
        formatted.append(
            KnowledgeSearchResult(
                id=row.get("id", ""),
                title=row.get("title", ""),
                content=row.get("content", ""),
                score=row.get("score"),
                source=row.get("source"),
            )
        )
    return formatted


@router.post("/search", response_model=list[KnowledgeSearchResult])
async def knowledge_search(
    request: KnowledgeSearchRequest,
) -> list[KnowledgeSearchResult]:
    return await search_knowledge(q=request.query, limit=request.limit)

```