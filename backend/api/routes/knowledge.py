#!/usr/bin/env python3
"""
API Endpoints for Knowledge Base Interaction.
"""

from api.dependencies import get_current_user_token
from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel, Field
from services.knowledge_qa import KnowledgeQAService

router = APIRouter()


class ScribeQuestion(BaseModel):
    """Request model for asking a question to the Scribe."""

    question: str


class KnowledgeQuestion(BaseModel):
    """Request contract for the governed company knowledge-base skill."""

    question: str = Field(min_length=1, max_length=4_000)


knowledge_qa_service = KnowledgeQAService()


@router.post("/knowledge/ask", tags=["Knowledge Base"])
async def ask_company_knowledge(
    request: KnowledgeQuestion,
    limit: int = Query(default=3, ge=1, le=5),
    user: dict = Depends(get_current_user_token),
):
    """Return a tenant-filtered, source-cited answer from approved knowledge only."""
    return await knowledge_qa_service.answer(request.question, user, limit)


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
