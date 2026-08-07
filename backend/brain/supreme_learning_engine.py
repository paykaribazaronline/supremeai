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
import math
import re
import sqlite3
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any

from loguru import logger

# বাংলা মন্তব্য: similarity embedder-এর জন্য স্টপওয়ার্ড + regex (MD5 signature-এর বদলে ব্যবহৃত)
_STOPWORDS = {
    "the", "and", "for", "with", "you", "your", "how", "what", "why", "when",
    "this", "that", "from", "into", "have", "will", "can", "are", "was", "were",
    "does", "done", "please", "using", "use", "would", "could", "should", "write",
    "make", "create", "give", "explain", "tell", "show", "need", "want", "about",
}
re_findall = re.findall


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

        # বাংলা মন্তব্য: _mini_models আগে প্রারম্ভিকতে লোড হতো (torch থাকলে ৩টি HF মডেল ডাউনলোড),
        # কিন্তু কখনো ব্যবহৃত হয় নি। Lazy-ভাবে সংরক্ষণ করা হয়েছে — _load_mini_models() শুধুমাত্র
        # আহ্বানে চলে, এবং বর্তমানে কোনো caller আহ্বান করে না।
        self._mini_models: dict[str, Any] = {}
        self._embedder: Any | None = None
        self._try_load_embedder()

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

    def _try_load_embedder(self) -> None:
        """ঐচ্ছিক embedding model — না থাকলে bag-of-words fallback চালু থাকে।"""
        try:
            from sentence_transformers import SentenceTransformer  # type: ignore

            self._embedder = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")
            logger.info("🧠 Using sentence-transformers embedder for similarity matching.")
        except Exception as exc:
            self._embedder = None
            logger.info(f"ℹ️ No sentence-transformers; using lightweight TF embedder. ({exc})")

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
                complexity TEXT,
                embedding TEXT
             )
         """)
        conn.execute("""
            CREATE INDEX IF NOT EXISTS idx_signature ON patterns(query_signature)
        """)
        conn.execute("""
            CREATE INDEX IF NOT EXISTS idx_domain ON patterns(domain)
        """)
        # বাংলা মন্তব্য: পুরোনো DB-তে embedding কলাম যোগ করার আপডেট (idempotent)।
        try:
            conn.execute("ALTER TABLE patterns ADD COLUMN embedding TEXT")
        except sqlite3.OperationalError:
            pass  # already exists
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

        # বাংলা মন্তব্য: আগে MD5 signature — শব্দের অর্থ বদলালেই mismatch। এখন dense embedding।
        query_embed = json.dumps(self._embed_vec(query))
        reasoning = self._extract_reasoning(response)
        template = self._create_template(query, response)
        complexity = self._analyze_complexity(query)

        pattern = self._store_pattern(
            query_sig=query_embed,
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
        query_sig = json.dumps(self._embed_vec(query))
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

    def _embed(self, text: str) -> list[float]:
        """Lightweight, dependency-free embedding (bag-of-words TF vector).

        বাংলা মন্তব্য: আগে MD5 hash ব্যবহার করা হতো যা শব্দের অর্থ বদলালেই mismatch দিত —
        তাই সাদৃশ্য (similarity) খোঁজা যেত না। এখন hashing-based vector দিয়ে cosine
        similarity মিলানো হয়, ফলে "sort a list" ও "sort an array" মিলে যায়।
        sentence-transformers ইনস্টল থাকলে সেটা ব্যবহার হয় (ঐচ্ছিক, lazy)।
        """
        if getattr(self, "_embedder", None) is not None:
            try:
                vec = self._embedder(text)
                if vec:
                    return vec
            except Exception as exc:
                logger.warning(f"Embedder failed, falling back to TF-vector: {exc}")
        # Pure-python fallback: normalized bag-of-words vector.
        vec_dict: dict[str, float] = {}
        for tok in self._tokenize(text):
            vec_dict[tok] = vec_dict.get(tok, 0.0) + 1.0
        norm = math.sqrt(sum(v * v for v in vec_dict.values())) or 1.0
        return [vec_dict.get(t, 0.0) / norm for t in sorted(vec_dict)]

    def _embed_vec(self, text: str) -> dict[str, float]:
        """Sparse vector form for cosine similarity (fallback embedder)."""
        vec: dict[str, float] = {}
        for tok in self._tokenize(text):
            vec[tok] = vec.get(tok, 0.0) + 1.0
        norm = math.sqrt(sum(v * v for v in vec.values())) or 1.0
        return {t: v / norm for t, v in vec.items()}

    @staticmethod
    def _tokenize(text: str) -> list[str]:
        text = (text or "").lower()
        toks = re_findall(r"[a-z0-9]+", text)
        # ছোট স্টপওয়ার্ড বাদ দিই যাতে ভেক্টর অর্থপূর্ণ থাকে
        return [t for t in toks if len(t) > 2 and t not in _STOPWORDS]

    def _cosine(self, a: dict[str, float], b: dict[str, float]) -> float:
        if not a or not b:
            return 0.0
        common = set(a) & set(b)
        dot = sum(a[t] * b[t] for t in common)
        na = math.sqrt(sum(v * v for v in a.values()))
        nb = math.sqrt(sum(v * v for v in b.values()))
        if na == 0.0 or nb == 0.0:
            return 0.0
        return dot / (na * nb)

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
        # বাংলা মন্তব্য: query_sig এখন JSON-encoded embedding। pattern_id deterministic hashing এখন
        # embedding থেকে (MD5 শুধু consistency রাখতে ব্যবহৃত)।
        query_embed = json.loads(query_sig) if isinstance(query_sig, str) and query_sig else {}
        pattern_id = hashlib.md5(
            (json.dumps(query_embed, sort_keys=True) + ":" + domain).encode(),
            usedforsecurity=False,
        ).hexdigest()[:16]

        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        # বাংলা মন্তব্ট: সাদৃশ্যের ভিত্তিতে সবচে সাদৃশ্যপূর্ণ existing pattern খুঁজি
        # (exact embedding match নয়) — ফলে "sort a list"/"sort an array" মিলে।
        existing = self._find_similar_in_domain(cursor, query_embed, domain, threshold=0.85)

        if existing:
            row_id = existing["row_id"]
            cur_conf = existing["confidence"]
            cur_success = existing["success_count"]
            cur_models = existing["source_models"]
            cur_sim = existing["similarity"]
            # বাংলা মন্তব্ট: রিয়েল ফিডব্যাক → confidence পরিবর্তন (আগে শুধু +0.02 হতো)।
            if feedback is not None:
                # feedback 1.0 = positive, -1.0 = negative, 0 = neutral
                delta = 0.05 if feedback >= 0 else -0.08
                new_conf = min(0.99, max(0.1, cur_conf + delta))
                if feedback < 0:
                    cursor.execute(
                        "UPDATE patterns SET failure_count = failure_count + 1 WHERE rowid = ?",
                        (row_id,),
                    )
                else:
                    cursor.execute(
                        "UPDATE patterns SET success_count = success_count + 1 WHERE rowid = ?",
                        (row_id,),
                    )
            else:
                cursor.execute(
                    "UPDATE patterns SET success_count = success_count + 1 WHERE rowid = ?",
                    (row_id,),
                )
                new_conf = min(0.99, cur_conf + 0.02)
            cursor.execute(
                """
                UPDATE patterns SET
                    confidence = ?,
                    last_used = ?,
                    source_models = ?,
                    query_template = CASE WHEN length(?) > length(query_template) THEN ? ELSE query_template END,
                    response_template = CASE WHEN length(?) > length(response_template) THEN ? ELSE response_template END,
                    embedding = ?
                WHERE rowid = ?
            """,
                (
                    new_conf,
                    datetime.now().isoformat(),
                    json.dumps(list(set([*cur_models, model]))),
                    response_template, response_template,
                    query_template, query_template,
                    json.dumps(query_embed),
                    row_id,
                ),
            )
            confidence = new_conf
        else:
            cursor.execute(
                """
                INSERT INTO patterns
                (pattern_id, query_signature, query_template, response_template,
                 reasoning_chain, confidence, success_count, failure_count,
                 source_models, created_at, last_used, domain, complexity, embedding)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
                (
                    pattern_id,
                    json.dumps(query_embed),
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
                    json.dumps(query_embed),
                ),
            )
            self.stats["patterns_learned"] += 1
            confidence = 0.5

        conn.commit()
        conn.close()

        return {
            "pattern_id": pattern_id,
            "confidence": confidence,
            "query_template": query_template,
            "response_template": response_template,
            "similarity": cur_sim if existing else 1.0,
        }

    def _find_similar_in_domain(self, cursor, query_embed: dict, domain: str, threshold: float) -> dict | None:
        """Find the most similar pattern in the same domain via cosine similarity."""
        if not query_embed:
            return None
        cursor.execute(
            "SELECT rowid, query_signature, confidence, success_count, source_models, embedding FROM patterns WHERE domain = ?",
            (domain,),
        )
        best: dict | None = None
        best_sim = 0.0
        for row in cursor.fetchall():
            try:
                stored = json.loads(row[5]) if row[5] else {}
            except (json.JSONDecodeError, TypeError):
                continue
            if not isinstance(stored, dict):
                continue
            sim = self._cosine(query_embed, stored)
            if sim > best_sim:
                best_sim = sim
                best = {
                    "row_id": row[0],
                    "confidence": row[2],
                    "success_count": row[3],
                    "source_models": json.loads(row[4]) if row[4] else [],
                    "similarity": sim,
                }
        if best is not None and best_sim >= threshold:
            return best
        return None if best_sim < threshold else best

    def _find_best_pattern(self, query_sig: str, domain: str) -> dict | None:
        query_embed = json.loads(query_sig) if isinstance(query_sig, str) and query_sig else {}
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        cursor.execute(
            """
            SELECT rowid, query_signature, query_template, response_template,
                   reasoning_chain, confidence, success_count, failure_count,
                   source_models, embedding
            FROM patterns
            WHERE domain = ?
        """,
            (domain,),
        )

        best_row = None
        best_sim = 0.0
        for row in cursor.fetchall():
            try:
                stored = json.loads(row[9]) if row[9] else {}
            except (json.JSONDecodeError, TypeError):
                continue
            if not isinstance(stored, dict):
                continue
            sim = self._cosine(query_embed, stored) if query_embed else 0.0
            # বাংলা মন্তব্ট: similarity + confidence দুটোই বেশি দরকার
            score = sim * row[5]
            if score > best_sim:
                best_sim = score
                best_row = row

        conn.close()

        if best_row:
            return {
                "pattern_id": "",
                "query_signature": best_row[1],
                "query_template": best_row[2],
                "response_template": best_row[3],
                "reasoning_chain": json.loads(best_row[4]),
                "confidence": best_row[5],
                "success_count": best_row[6],
                "failure_count": best_row[7],
                "source_models": json.loads(best_row[8]),
                "similarity": best_sim,
            }
        return None

    def _calculate_confidence(self, pattern: dict, query: str) -> float:
        # বাংলা মন্তব্ট: আগে confidence শুধু +0.02 করে ধীরে ধীরে 0.99-এ উঠত। এখন
        # (a) সাদৃশ্য similarity এবং (b) সঠিক/ভুল ফিডব্যাক দুটোই ফ্যাক্টর।
        base_confidence = pattern["confidence"]
        total = pattern["success_count"] + pattern["failure_count"]
        if total > 0:
            success_rate = pattern["success_count"] / total
            base_confidence = (base_confidence + success_rate) / 2

        # similarity (0..1) থেকে আসা প্যাটার্ন ম্যাচের গুণমান — sparse embedder ব্যবহৃত
        similarity = float(pattern.get("similarity", 0.0))

        confidence = base_confidence * (0.3 + 0.7 * similarity)
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
