# backend/brain/supreme_learning_engine.py
"""
SupremeAI Learning Engine v2.0
================================
NOT a traditional local LLM. This is a LEARNING SYSTEM that:
1. OBSERVES how external AIs respond
2. EXTRACTS patterns, reasoning chains, and knowledge
3. BUILDS an internal "brain" (lightweight models + knowledge graph)
4. ANSWERS independently when confident
5. FALLS BACK to external AI only when uncertain

Goal: Start at 30% self-sufficiency, grow to 80%+ over time.
Memory: Starts at ~500MB, grows to ~2-5GB as it learns.
"""

import hashlib
import json
import sqlite3
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any

from loguru import logger


@dataclass
class LearnedPattern:
    """A pattern learned from observing AI interactions."""

    pattern_id: str
    query_signature: str  # Hash of query type
    query_template: str  # Generic template (e.g., "explain {topic} to {audience}")
    response_template: str  # Response structure learned
    reasoning_chain: list[str]  # Step-by-step reasoning observed
    confidence: float  # 0.0 - 1.0
    success_count: int  # How many times this pattern worked
    failure_count: int  # How many times it failed
    source_models: list[str]  # Which AIs taught this
    created_at: datetime
    last_used: datetime
    domain: str  # coding, general, math, bangla, etc.
    complexity: str  # simple, medium, complex


@dataclass
class KnowledgeNode:
    """A node in the knowledge graph."""

    node_id: str
    concept: str
    definition: str
    relationships: list[dict]
    examples: list[str]
    source: str
    confidence: float
    usage_count: int


class SupremeLearningEngine:
    """
    The brain of SupremeAI that learns from every interaction.
    """

    def __init__(self, data_dir: str = "./learning_data"):
        self.data_dir = Path(data_dir)
        self.data_dir.mkdir(parents=True, exist_ok=True)

        self.db_path = self.data_dir / "patterns.db"
        self._init_db()

        self.kg_path = self.data_dir / "knowledge_graph.json"
        self.knowledge_graph = self._load_kg()

        self._mini_models: dict[str, Any] = {}
        self._load_mini_models()

        self.stats = {
            "total_interactions": 0,
            "patterns_learned": 0,
            "self_answers": 0,
            "fallback_answers": 0,
            "self_sufficiency_rate": 0.0,
        }

        logger.info("🧠 SupremeLearningEngine initialized")
        logger.info(f"   📊 Patterns DB: {self.db_path}")
        logger.info(f"   🕸️  Knowledge Graph: {len(self.knowledge_graph.get('nodes', {}))} nodes")

    def _init_db(self):
        """Initialize SQLite database for patterns."""
        conn = sqlite3.connect(self.db_path)
        conn.execute("""
            CREATE TABLE IF NOT EXISTS patterns (
                pattern_id TEXT PRIMARY KEY,
                query_signature TEXT,
                query_template TEXT,
                response_template TEXT,
                reasoning_chain TEXT,
                confidence REAL,
                success_count INTEGER,
                failure_count INTEGER,
                source_models TEXT,
                created_at TEXT,
                last_used TEXT,
                domain TEXT,
                complexity TEXT
            )
        """)
        conn.execute("""
            CREATE INDEX IF NOT EXISTS idx_signature ON patterns(query_signature)
        """)
        conn.execute("""
            CREATE INDEX IF NOT EXISTS idx_domain ON patterns(domain)
        """)
        conn.commit()
        conn.close()

    def _load_kg(self) -> dict:
        """Load knowledge graph."""
        if self.kg_path.exists():
            try:
                with open(self.kg_path, encoding="utf-8") as f:
                    return json.load(f)
            except Exception as e:
                logger.warning(f"Failed to load KG, resetting: {e}")
        return {"nodes": {}, "relationships": []}

    def _save_kg(self):
        """Save knowledge graph."""
        try:
            with open(self.kg_path, "w", encoding="utf-8") as f:
                json.dump(self.knowledge_graph, f, indent=2, default=str)
        except Exception as e:
            logger.error(f"Error saving KG: {e}")

    def _load_mini_models(self):
        """Load tiny specialized models lazily if transformers/torch installed."""
        try:
            import torch
            from transformers import pipeline

            model_configs = {
                "intent_classifier": {
                    "model": "distilbert-base-uncased-finetuned-sst-2-english",
                    "task": "text-classification",
                    "description": "Classifies user intent",
                },
                "confidence_scorer": {
                    "model": "cross-encoder/nli-deberta-v3-base",
                    "task": "text-classification",
                    "description": "Scores confidence",
                },
                "query_embedder": {
                    "model": "sentence-transformers/all-MiniLM-L6-v2",
                    "task": "feature-extraction",
                    "description": "Creates embeddings",
                },
            }

            for name, config in model_configs.items():
                try:
                    logger.info(f"📥 Loading mini-model: {name} ({config['description']})")
                    self._mini_models[name] = pipeline(
                        config["task"],
                        model=config["model"],
                        device=-1,
                        torch_dtype=torch.float32,
                    )
                except Exception as e:
                    logger.warning(f"⚠️ Could not load {name}: {e}")
        except ImportError:
            logger.info("ℹ️ Transformers/Torch not loaded; running in lightweight heuristics mode.")

    def learn_from_interaction(
        self,
        query: str,
        response: str,
        model_used: str,
        task_type: str = "general",
        user_feedback: float | None = None,
    ) -> dict:
        """Learn from EVERY interaction with external AI."""
        self.stats["total_interactions"] += 1

        query_sig = self._extract_signature(query)
        reasoning = self._extract_reasoning(response)
        template = self._create_template(query, response)
        complexity = self._analyze_complexity(query)

        pattern = self._store_pattern(
            query_sig=query_sig,
            query_template=template["query_template"],
            response_template=template["response_template"],
            reasoning=reasoning,
            model=model_used,
            domain=task_type,
            complexity=complexity,
            feedback=user_feedback,
        )

        self._extract_knowledge(query, response, model_used)

        logger.info(
            f"🎓 Learned pattern: {pattern['pattern_id']} | "
            f"Domain: {task_type} | Confidence: {pattern['confidence']:.2f}"
        )

        return pattern

    def can_answer_independently(
        self,
        query: str,
        task_type: str = "general",
        min_confidence: float = 0.75,
    ) -> tuple[bool, float, dict | None]:
        """Decide: Can SupremeAI answer this WITHOUT calling external AI?"""
        query_sig = self._extract_signature(query)
        pattern = self._find_best_pattern(query_sig, task_type)

        if pattern is None:
            logger.info("🤷 No matching pattern found - needs external AI")
            return False, 0.0, None

        confidence = self._calculate_confidence(pattern, query)

        if confidence >= min_confidence:
            logger.info(f"🎯 Can answer independently! Confidence: {confidence:.2f}")
            self.stats["self_answers"] += 1
            return True, confidence, pattern
        else:
            logger.info(f"🤔 Confidence too low ({confidence:.2f} < {min_confidence}) - fallback to external AI")
            self.stats["fallback_answers"] += 1
            return False, confidence, pattern

    def generate_independent_response(
        self,
        query: str,
        pattern: dict,
        context: dict | None = None,
    ) -> str:
        """Generate response using learned pattern."""
        response = self._fill_template(pattern["response_template"], query, context)

        if pattern.get("reasoning_chain"):
            reasoning = "\n".join([f"{i+1}. {step}" for i, step in enumerate(pattern["reasoning_chain"])])
            response = f"{response}\n\n💭 Reasoning:\n{reasoning}"

        self._update_pattern_usage(pattern["pattern_id"], success=True)
        logger.info("✅ Generated independent response using learned pattern")
        return response

    def _extract_signature(self, query: str) -> str:
        words = query.lower().split()
        signature_words = []
        for word in words:
            if len(word) > 6 and word.isalpha():
                signature_words.append("{entity}")
            else:
                signature_words.append(word)
        return hashlib.md5(" ".join(signature_words).encode(), usedforsecurity=False).hexdigest()[:16]

    def _extract_reasoning(self, response: str) -> list[str]:
        reasoning = []
        lines = response.split("\n")
        for line in lines:
            line = line.strip()
            if line.startswith(("1.", "2.", "3.", "Step", "First", "Then", "Finally")):
                reasoning.append(line)
        if not reasoning:
            sentences = response.split(". ")
            reasoning = [s.strip() for s in sentences[:3] if len(s) > 20]
        return reasoning

    def _create_template(self, query: str, response: str) -> dict:
        query_template = query
        response_template = response
        words = query.split()
        for word in words:
            if len(word) > 5 and word[0].isupper():
                query_template = query_template.replace(word, "{topic}")
                response_template = response_template.replace(word, "{topic}")
        return {
            "query_template": query_template,
            "response_template": response_template,
        }

    def _analyze_complexity(self, query: str) -> str:
        words = len(query.split())
        if words < 10:
            return "simple"
        elif words < 30:
            return "medium"
        else:
            return "complex"

    def _store_pattern(
        self,
        query_sig: str,
        query_template: str,
        response_template: str,
        reasoning: list[str],
        model: str,
        domain: str,
        complexity: str,
        feedback: float | None,
    ) -> dict:
        pattern_id = hashlib.md5(f"{query_sig}:{domain}".encode(), usedforsecurity=False).hexdigest()[:16]

        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        cursor.execute("SELECT * FROM patterns WHERE pattern_id = ?", (pattern_id,))
        existing = cursor.fetchone()

        if existing:
            cursor.execute(
                """
                UPDATE patterns SET
                    success_count = success_count + 1,
                    confidence = MIN(0.99, confidence + 0.02),
                    last_used = ?,
                    source_models = ?
                WHERE pattern_id = ?
            """,
                (
                    datetime.now().isoformat(),
                    json.dumps(list(set([*json.loads(existing[8]), model]))),
                    pattern_id,
                ),
            )
        else:
            cursor.execute(
                """
                INSERT INTO patterns VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
                (
                    pattern_id,
                    query_sig,
                    query_template,
                    response_template,
                    json.dumps(reasoning),
                    0.5,
                    1,
                    0,
                    json.dumps([model]),
                    datetime.now().isoformat(),
                    datetime.now().isoformat(),
                    domain,
                    complexity,
                ),
            )
            self.stats["patterns_learned"] += 1

        conn.commit()
        conn.close()

        return {
            "pattern_id": pattern_id,
            "confidence": 0.5 if not existing else min(0.99, existing[5] + 0.02),
            "query_template": query_template,
            "response_template": response_template,
        }

    def _find_best_pattern(self, query_sig: str, domain: str) -> dict | None:
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        cursor.execute(
            """
            SELECT * FROM patterns
            WHERE query_signature = ? AND domain = ?
            ORDER BY confidence DESC, success_count DESC
            LIMIT 1
        """,
            (query_sig, domain),
        )

        row = cursor.fetchone()
        conn.close()

        if row:
            return {
                "pattern_id": row[0],
                "query_signature": row[1],
                "query_template": row[2],
                "response_template": row[3],
                "reasoning_chain": json.loads(row[4]),
                "confidence": row[5],
                "success_count": row[6],
                "failure_count": row[7],
                "source_models": json.loads(row[8]),
            }
        return None

    def _calculate_confidence(self, pattern: dict, query: str) -> float:
        base_confidence = pattern["confidence"]
        total = pattern["success_count"] + pattern["failure_count"]
        if total > 0:
            success_rate = pattern["success_count"] / total
            base_confidence = (base_confidence + success_rate) / 2

        query_words = set(query.lower().split())
        template_words = set(pattern["query_template"].lower().split())
        overlap = len(query_words & template_words) / len(query_words | template_words)

        confidence = base_confidence * (0.5 + 0.5 * overlap)
        return min(1.0, max(0.0, confidence))

    def _fill_template(self, template: str, query: str, context: dict | None) -> str:
        words = query.split()
        entities = [w for w in words if len(w) > 5 and w[0].isupper()]

        response = template
        if entities and "{topic}" in response:
            response = response.replace("{topic}", entities[0])

        return response

    def _update_pattern_usage(self, pattern_id: str, success: bool):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        if success:
            cursor.execute(
                """
                UPDATE patterns SET success_count = success_count + 1, last_used = ?
                WHERE pattern_id = ?
            """,
                (datetime.now().isoformat(), pattern_id),
            )
        else:
            cursor.execute(
                """
                UPDATE patterns SET failure_count = failure_count + 1,
                    confidence = MAX(0.1, confidence - 0.05)
                WHERE pattern_id = ?
            """,
                (pattern_id,),
            )

        conn.commit()
        conn.close()

    def _extract_knowledge(self, query: str, response: str, source: str):
        import re

        concepts = re.findall(r"\b[A-Z][a-z]+(?:\s+[A-Z][a-z]+)*\b", response)

        for concept in concepts[:5]:
            node_id = hashlib.md5(concept.encode(), usedforsecurity=False).hexdigest()[:16]

            if node_id not in self.knowledge_graph.get("nodes", {}):
                self.knowledge_graph.setdefault("nodes", {})[node_id] = {
                    "concept": concept,
                    "definition": "",
                    "examples": [],
                    "source": source,
                    "confidence": 0.5,
                    "usage_count": 1,
                }
            else:
                self.knowledge_graph["nodes"][node_id]["usage_count"] += 1

        self._save_kg()

    def get_stats(self) -> dict:
        total = self.stats["self_answers"] + self.stats["fallback_answers"]
        if total > 0:
            self.stats["self_sufficiency_rate"] = self.stats["self_answers"] / total * 100

        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM patterns")
        total_patterns = cursor.fetchone()[0]
        conn.close()

        return {
            **self.stats,
            "total_patterns_in_db": total_patterns,
            "knowledge_graph_nodes": len(self.knowledge_graph.get("nodes", {})),
            "data_dir_size_mb": sum(f.stat().st_size for f in self.data_dir.rglob("*") if f.is_file()) / (1024 * 1024),
        }
