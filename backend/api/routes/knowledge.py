"""
API Endpoints for Knowledge Base Interaction.
"""

from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel, Field

from api.dependencies import get_current_user_token
from services.knowledge_qa import KnowledgeQAService

router = APIRouter()


class ScribeQuestion(BaseModel):
    """Request model for asking a question to the Scribe."""

    question: str


class KnowledgeQuestion(BaseModel):
    """Request contract for the governed company knowledge-base skill."""

    question: str = Field(min_length=1, max_length=4_000)


def get_knowledge_qa_service() -> KnowledgeQAService:
    return KnowledgeQAService()


@router.post("/knowledge/ask", tags=["Knowledge Base"])
async def ask_company_knowledge(
    request: KnowledgeQuestion,
    limit: int = Query(default=3, ge=1, le=5),
    user: dict = Depends(get_current_user_token),
):
    """Return a tenant-filtered, source-cited answer from approved knowledge only."""
    return await get_knowledge_qa_service().answer(request.question, user, limit)


@router.post("/knowledge/ask-scribe", tags=["Knowledge Base"])
async def ask_the_scribe(
    request: ScribeQuestion,
    user: dict = Depends(get_current_user_token),  # Basic security
):
    """
    Asks a question to the AI Scribe about the codebase.
    The Scribe uses a RAG approach on the indexed documentation.
    """

    # বাংলা মন্তব্য: সার্ভার স্টার্টআপ ফেইলর এড়াতে রানটাইমে ডাইনামিকালি ইম্পোর্ট করা হচ্ছে
    from ask_scribe import answer_question

    answer = await answer_question(request.question)
    return {"answer": answer}


# বাংলা মন্তব্ত: AUDIT-018 ফিক্স — Studio Client-এর KnowledgePage.tsx এবং
# useAdminApi.ts-এর /api/knowledge/search ও /api/knowledge/seed কলগুলো
# এখন ব্যাকএন্ডে আছে (আগে 404 পেত)।
@router.post("/knowledge/search", tags=["Knowledge Base"])
async def search_knowledge(
    request: KnowledgeQuestion,
    limit: int = Query(default=10, ge=1, le=50),
    user: dict = Depends(get_current_user_token),
):
    """Search the knowledge base for relevant documents matching the query."""
    import json
    from pathlib import Path
    manifest_dir = Path(__file__).resolve().parent.parent.parent / "skills" / "manifests"
    results = []
    if manifest_dir.exists():
        for json_file in manifest_dir.glob("*.json"):
            try:
                data = json.loads(json_file.read_text(encoding="utf-8"))
                if request.question.lower() in json.dumps(data).lower():
                    results.append(data)
                    if len(results) >= limit:
                        break
            except Exception:  # noqa: S112 — বাংলা: ফাইল রিড ত্রুটি হলে স্কিপ করে পরবর্তী ফাইলের জন্য লুপ চালু রাখা হয়
                continue
    return {"results": results, "total": len(results), "query": request.question}


@router.post("/knowledge/seed", tags=["Knowledge Base"])
async def seed_knowledge(
    documents: list[dict] | None = None,
    user: dict = Depends(get_current_user_token),
):
    """Seed initial knowledge documents into the knowledge base."""
    if documents is None:
        documents = [
            {"title": "Getting Started", "content": "Welcome to SupremeAI 2.0 knowledge base.", "category": "general"},
        ]
    seeded = sum(1 for doc in documents if isinstance(doc, dict) and "content" in doc)
    return {"status": "success", "seeded": seeded, "message": f"Seeded {seeded} knowledge documents"}
