#!/usr/bin/env python3
"""
API Endpoints for Knowledge Base Interaction.
"""

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel

from ask_scribe import answer_question
from core.security import get_current_user_or_guest

router = APIRouter()


class ScribeQuestion(BaseModel):
    """Request model for asking a question to the Scribe."""

    question: str


@router.post("/knowledge/ask-scribe", tags=["Knowledge Base"])
async def ask_the_scribe(
    request: ScribeQuestion,
    user: dict = Depends(get_current_user_or_guest),  # Basic security
):
    """
    Asks a question to the AI Scribe about the codebase.
    The Scribe uses a RAG approach on the indexed documentation.
    """
    answer = await answer_question(request.question)
    return {"answer": answer}
