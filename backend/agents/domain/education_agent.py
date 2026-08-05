"""
SupremeAI — Education Agent
============================
Personalized learning and educational content generation.
Provides curriculum planning, quiz generation, and learning path recommendations.
"""

from __future__ import annotations

import hashlib
import logging
from dataclasses import dataclass
from datetime import UTC, datetime
from enum import StrEnum

from core.cache import get_cache
from core.error_bus import with_error_bus
from core.llm_router import LLMRouter

logger = logging.getLogger("supremeai.education")

EDUCATION_CACHE_TTL = 3600  # 1 hour


class DifficultyLevel(StrEnum):
    BEGINNER = "beginner"
    INTERMEDIATE = "intermediate"
    ADVANCED = "advanced"
    EXPERT = "expert"


class LearningStyle(StrEnum):
    VISUAL = "visual"
    AUDITORY = "auditory"
    READING = "reading"
    KINESTHETIC = "kinesthetic"


@dataclass(frozen=True)
class LearningModule:
    """Immutable learning module."""

    title: str
    subject: str
    difficulty: DifficultyLevel
    topics: list[str]
    estimated_minutes: int
    prerequisites: list[str]
    content_summary: str


@dataclass(frozen=True)
class QuizQuestion:
    """Immutable quiz question."""

    question: str
    options: list[str]
    correct_answer: int  # Index
    explanation: str
    difficulty: DifficultyLevel


@dataclass(frozen=True)
class LearningPath:
    """Immutable learning path recommendation."""

    user_id: str
    goal: str
    modules: list[LearningModule]
    total_estimated_hours: float
    difficulty_progression: str


class EducationAgent:
    """
    Personalized learning and educational content generation.
    """

    def __init__(self, llm_router: LLMRouter | None = None) -> None:
        self.llm = llm_router or LLMRouter()
        self.cache = get_cache()

    def _cache_key(self, prefix: str, identifier: str) -> str:
        raw = f"education:{prefix}:{identifier}:{datetime.now(UTC).strftime('%Y%m%d')}"
        return f"education:{hashlib.sha256(raw.encode()).hexdigest()[:16]}"

    async def generate_quiz(
        self,
        subject: str,
        topic: str,
        difficulty: DifficultyLevel = DifficultyLevel.INTERMEDIATE,
        num_questions: int = 5,
    ) -> list[QuizQuestion]:
        """Generate quiz questions on a topic."""
        cache_key = self._cache_key(f"quiz:{subject}:{topic}:{difficulty.value}", str(num_questions))
        cached = await self.cache.get(cache_key)
        if cached:
            return [QuizQuestion(**q) for q in cached]

        prompt = (
            f"Generate {num_questions} multiple-choice quiz questions about {topic} in {subject}.\n"
            f"Difficulty: {difficulty.value}\n\n"
            f"Return as JSON array with fields: question, options (list of 4), correct_answer (0-3 index), explanation, difficulty."
        )

        try:
            result = await self.llm.route(
                prompt=prompt,
                task_type="reasoning",
                max_tokens=1000,
            )
            import json

            content = result.get("content", "[]")
            data = json.loads(content) if isinstance(content, str) else content
            questions = [QuizQuestion(**q) for q in data[:num_questions]]
        except Exception as e:
            logger.error("Failed to generate quiz: %s", e)
            questions = [
                QuizQuestion(
                    question=f"Sample question about {topic}?",
                    options=["Option A", "Option B", "Option C", "Option D"],
                    correct_answer=0,
                    explanation=f"This is a sample explanation about {topic}",
                    difficulty=difficulty,
                )
            ]

        await self.cache.set(
            cache_key,
            [
                {
                    "question": q.question,
                    "options": q.options,
                    "correct_answer": q.correct_answer,
                    "explanation": q.explanation,
                    "difficulty": q.difficulty.value,
                }
                for q in questions
            ],
            ttl=EDUCATION_CACHE_TTL,
        )

        return questions

    @with_error_bus("create_learning_path")
    async def create_learning_path(
        self,
        user_id: str,
        goal: str,
        current_level: DifficultyLevel = DifficultyLevel.BEGINNER,
        available_hours_per_week: float = 5.0,
    ) -> LearningPath:
        """Create a personalized learning path."""
        prompt = (
            f"Create a learning path for a user who wants to: {goal}\n"
            f"Current level: {current_level.value}\n"
            f"Available time: {available_hours_per_week} hours/week\n\n"
            f"Return as JSON with: modules (list of title, subject, difficulty, topics, "
            f"estimated_minutes, prerequisites, content_summary), total_estimated_hours, difficulty_progression."
        )

        try:
            result = await self.llm.route(
                prompt=prompt,
                task_type="reasoning",
                max_tokens=1500,
            )
            import json

            content = result.get("content", "{}")
            data = json.loads(content) if isinstance(content, str) else content
            modules = [LearningModule(**m) for m in data.get("modules", [])]
        except Exception:
            modules = [
                LearningModule(
                    title=f"Introduction to {goal}",
                    subject=goal,
                    difficulty=current_level,
                    topics=["Fundamentals", "Core concepts"],
                    estimated_minutes=60,
                    prerequisites=[],
                    content_summary=f"Getting started with {goal}",
                )
            ]

        return LearningPath(
            user_id=user_id,
            goal=goal,
            modules=modules,
            total_estimated_hours=sum(m.estimated_minutes for m in modules) / 60,
            difficulty_progression=f"{current_level.value} to advanced",
        )

    async def explain_concept(self, concept: str, audience_level: DifficultyLevel = DifficultyLevel.BEGINNER) -> str:
        """Explain a concept at the appropriate level."""
        prompt = (
            f"Explain '{concept}' to a {audience_level.value}-level learner.\n"
            f"Use simple analogies, avoid jargon, and provide a practical example.\n"
            f"Keep it under 200 words."
        )

        try:
            result = await self.llm.route(
                prompt=prompt,
                task_type="reasoning",
                max_tokens=300,
            )
            return result.get("content", f"Explanation of {concept} is not available.")
        except Exception as e:
            logger.error("Failed to explain concept: %s", e)
            return f"Explanation of {concept} is not available."


# Singleton
_education_instance: EducationAgent | None = None


def get_education_agent() -> EducationAgent:
    """Get or create the singleton EducationAgent."""
    global _education_instance
    if _education_instance is None:
        _education_instance = EducationAgent()
    return _education_instance
