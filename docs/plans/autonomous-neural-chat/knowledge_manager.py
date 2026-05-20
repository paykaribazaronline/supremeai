
import json
import os
import hashlib
from typing import Dict, List, Optional, Any
from datetime import datetime
from pathlib import Path


# --------------------------------------------------------------------------- #
# Knowledge Manager — powers the Autonomous Neural Chat
# Implements: continuous-knowledge-improvement.md + supremeai-agent-learning-rules.md
# --------------------------------------------------------------------------- #

class KnowledgeManager:
    """
    Manages local knowledge bases and the continuous-learning loop.
    Operates 100% offline (zero-AI mode by default).
    """

    def __init__(self, kb_dir: str = None):
        if kb_dir is None:
            # Walk up from this file's directory until we find the knowledge files
            # so the class can be instantiated standalone from any package.
            start = Path(__file__).parent
            kb_dir = str(start.parent)     # default: smart_chat_system's parent = project root
            if not (os.path.exists(os.path.join(kb_dir, "core_knowledge.json")) or
                    os.path.exists(os.path.join(kb_dir, "autonomous_seed_knowledge.json"))):
                # Try the immediate package directory as a last resort
                kb_dir = str(start)
        self.kb_dir = kb_dir
        self.core_kb_path   = os.path.join(kb_dir, "core_knowledge.json")
        self.seed_kb_path   = os.path.join(kb_dir, "autonomous_seed_knowledge.json")
        self.learn_log_path = os.path.join(kb_dir, "local_learning_log.json")

        # Loaded data caches
        self.core_entries: List[Dict]  = []
        self.seed_entries:  List[Dict] = []
        self.learning_log:  List[Dict] = []

        # Self-improvement counters
        self.total_interactions  = 0
        self.knowledge_lookups   = 0
        self.corrections_captured = 0
        self.local_fallback_hits = 0

        self._load_all()

    # ── Loading ───────────────────────────────────────────────────────────

    def _load_all(self):
        self._load_core_kb()
        self._load_seed_kb()
        self._load_learning_log()

    def _load_core_kb(self):
        """Load core_knowledge.json — the primary offline knowledge base."""
        try:
            with open(self.core_kb_path, "r", encoding="utf-8") as f:
                raw = json.load(f)

            # Two possible formats: root-level "entries" list or bare list
            if isinstance(raw, dict):
                self.core_entries = raw.get("entries", raw.get("data", []))
            else:
                self.core_entries = raw

            # Normalise each entry — accept both SupremeAI format and
            # simple {task, solution} format.
            self.core_entries = [self._normalise_entry(e) for e in self.core_entries if e]

        except FileNotFoundError:
            self.core_entries = []
        except (json.JSONDecodeError, Exception):
            self.core_entries = []

    def _load_seed_kb(self):
        """Load autonomous_seed_knowledge.json — seed knowledge."""
        try:
            with open(self.seed_kb_path, "r", encoding="utf-8") as f:
                raw = json.load(f)

            if isinstance(raw, dict):
                self.seed_entries = raw.get("seed_knowledge", raw.get("entries", []))
            else:
                self.seed_entries = raw

            self.seed_entries = [self._normalise_seed(e) for e in self.seed_entries if e]

        except FileNotFoundError:
            self.seed_entries = []
        except (json.JSONDecodeError, Exception):
            self.seed_entries = []

    def _load_learning_log(self):
        """Load the local learning log (persists across restarts)."""
        try:
            with open(self.learn_log_path, "r", encoding="utf-8") as f:
                raw = json.load(f)
                if isinstance(raw, list):
                    self.learning_log = raw
                elif isinstance(raw, dict):
                    self.learning_log = raw.get("entries", [])
        except (FileNotFoundError, json.JSONDecodeError):
            self.learning_log = []

    def _save_learning_log(self):
        """Persist learning log to disk."""
        try:
            with open(self.learn_log_path, "w", encoding="utf-8") as f:
                json.dump({"entries": self.learning_log}, f, ensure_ascii=False, indent=2)
        except Exception:
            pass

    # ── Normalisation ─────────────────────────────────────────────────────

    @staticmethod
    def _normalise_entry(entry: Dict) -> Dict:
        if not entry:
            return {}
        return {
            "task":        entry.get("task", ""),
            "solution":    entry.get("solution", entry.get("content", "")),
            "confidence":  entry.get("confidence", 0.0),
            "tags":        entry.get("tags", []),
            "source":      entry.get("source", "core_kb"),
            "added_at":    entry.get("added_at", entry.get("generated_at", "")),
        }

    @staticmethod
    def _normalise_seed(entry: Dict) -> Dict:
        if not entry:
            return {}
        return {
            "id":          entry.get("id", ""),
            "category":    entry.get("category", ""),
            "title":       entry.get("title", ""),
            "description": entry.get("description", ""),
            "confidence":  entry.get("confidence", 0.0),
            "anti_patterns": entry.get("anti_patterns", []),
            "verification_steps": entry.get("verification_steps", []),
            "source":      "autonomous_seed",
        }

    # ── Core Lookup ───────────────────────────────────────────────────────

    def lookup(self, message: str, threshold: float = 0.10) -> Optional[Dict]:
        """
        Search local knowledge bases for a relevant answer to the message.
        Returns the best matching entry or None.
        Zero-AI mode: this works with NO external AI.
        """
        self.knowledge_lookups += 1
        message_lower = message.lower()
        tokens = set(message_lower.split())
        stem_tokens = set(self._stem(w) for w in tokens)

        best_match = None
        best_score = threshold

        # ── 1. core_knowledge.json ──
        for entry in self.core_entries:
            score = self._score_message_vs_task(
                message_lower, stem_tokens, entry.get("task", ""), entry.get("tags", [])
            )
            if score > best_score:
                best_score = score
                best_match = {**entry, "match_score": round(score, 4),
                              "matched_via": "task_field"}

        # ── 2. autonomous_seed_knowledge.json ──
        if not best_match:
            for seed_entry in self.seed_entries:
                # Score each sub-field independently
                seed = seed_entry if isinstance(seed_entry, dict) else {}
                title_s = self._score_message_vs_task(
                    message_lower, stem_tokens, seed.get("title", ""), [])
                desc_s  = self._score_message_vs_task(
                    message_lower, stem_tokens, seed.get("description", ""), [])
                cat_s   = self._score_message_vs_task(
                    message_lower, stem_tokens, seed.get("category", ""), [])
                # anti-patterns: penalise if any query word is an anti-pattern
                ant_s   = self._anti_pattern_penalty(message_lower, seed.get("anti_patterns", []))

                score = max(title_s, desc_s, cat_s * 0.7) - ant_s
                score = max(score, 0.0)

                if score > best_score:
                    best_score = score
                    best_match = {
                        id:               seed.get("id", ""),
                        "title":          seed.get("title", ""),
                        "description":    seed.get("description", ""),
                        "task":           " ".join(filter(None, [
                            seed.get("title", ""),
                            seed.get("description", ""),
                            seed.get("category", "")])),
                        "solution":       seed.get("description", ""),
                        "confidence":     seed.get("confidence", 0.0),
                        "category":       seed.get("category", ""),
                        "source":         "autonomous_seed",
                        "match_score":    round(score, 4),
                        "matched_via":    "seed_keyword",
                        "verification_steps": seed.get("verification_steps", []),
                        "anti_patterns":  seed.get("anti_patterns", []),
                    }

        if best_match:
            self.local_fallback_hits += 1

        return best_match

    @staticmethod
    def _score_message_vs_task(message_lower: str, stem_tokens: set,
                               task_text: str, tags: list) -> float:
        """
        Combined scoring: token Jaccard + tag bonus + word-level substring.
        task_text should already be lowercase.
        """
        if not task_text:
            return 0.0
        task_lower   = task_text.lower()
        task_tokens  = set(task_lower.split())
        task_stems   = set(KnowledgeManager._stem(w) for w in task_tokens)

        # --- stem token overlap (main signal) ---
        overlap = len(stem_tokens & task_stems)
        union   = len(stem_tokens | task_stems)
        token_score = overlap / union if union else 0.0

        # --- direct word overlap (secondary signal) ---
        raw_overlap = len(tokens_fallback := (set(message_lower.split()) & task_tokens))
        raw_union   = len(set(message_lower.split()) | task_tokens)
        raw_score   = raw_overlap / raw_union if raw_union else 0.0

        # --- substring bonus ---
        sub_score = 0.0
        for tok in tokens_fallback if 'tokens_fallback' in dir() else set(message_lower.split()):
            if len(tok) >= 3 and tok in task_lower:
                sub_score = max(sub_score, 0.35)
                break

        # --- tag bonus ---
        tag_score = 0.0
        for tag in tags:
            if tag.lower() in message_lower:
                tag_score = max(tag_score, 0.15)

        return min(token_score * 0.5 + raw_score * 0.4 + sub_score + tag_score, 1.0)

    @staticmethod
    def _anti_pattern_penalty(message_lower: str, anti_patterns: list) -> float:
        """Return a penalty if the user is ASKING about an anti-pattern NOT to do."""
        penalty = 0.0
        for ap in anti_patterns:
            words = str(ap).lower().split()
            match = sum(1 for w in words if w in message_lower and len(w) >= 3)
            if match >= len(words) * 0.5:
                penalty = max(penalty, 0.15)  # small penalty so we don't block entirely
        return penalty

    @staticmethod
    def _stem(word: str) -> str:
        """Very light stemmer: strip common English suffixes."""
        if len(word) <= 3:
            return word
        for suffix in ("ing", "ed", "es", "er", "est", "ly", "tion", "s"):
            if word.endswith(suffix) and len(word) > len(suffix) + 2:
                return word[:-len(suffix)]
        return word

    # ── Learning — capture new knowledge ──────────────────────────────────

    def capture_correction(self,
                           original_message: str,
                           incorrect_response: str,
                           corrected_by: str,
                           corrected_content: str) -> Dict:
        """
        Record a user correction so the system learns from it.
        Per continuous-knowledge-improvement.md §1 trigger #4.
        Returns the created log entry.
        """
        entry = {
            "type":              "user_correction",
            "timestamp":         datetime.utcnow().isoformat() + "Z",
            "original_message":  original_message,
            "incorrect_response": incorrect_response,
            "corrected_content":  corrected_content,
            "corrected_by":       corrected_by,
            "session_id":         self._session_id(),
            "confidence":         0.93,   # high — user directly corrected us
            "correction_hash":    self._hash(
                original_message + corrected_content + corrected_by
            ),
        }
        self.learning_log.append(entry)
        self._save_learning_log()
        self.corrections_captured += 1
        return entry

    def capture_error_pattern(self,
                              error_signature: str,
                              error_message: str,
                              context: str,
                              fix_applied: str,
                              success: bool) -> Dict:
        """
        Record an error-signature + its fix pattern.
        Per continuous-knowledge-improvement.md §1 trigger #1, #2, #5.
        """
        status = "resolved" if success else "unresolved"
        entry = {
            "type":            "error_pattern",
            "timestamp":       datetime.utcnow().isoformat() + "Z",
            "error_signature": error_signature,
            "error_message":   error_message,
            "context":         context,
            "fix_applied":     fix_applied,
            "resolution":      status,
            "confidence":      0.88 if success else 0.50,
            "session_id":      self._session_id(),
            "signature_hash":  self._hash(error_signature + context),
        }
        self.learning_log.append(entry)
        self._save_learning_log()
        return entry

    def capture_recurring_pattern(self, query_keywords: List[str], count: int) -> Dict:
        """
        Record a query that has appeared multiple times (≥ 2),
        indicating a knowledge gap.
        Per continuous-knowledge-improvement.md §1 trigger #6.
        """
        entry = {
            "type":       "recurring_query",
            "timestamp":  datetime.utcnow().isoformat() + "Z",
            "keywords":   query_keywords,
            "occurrence_count": count,
            "confidence": 0.85,
            "session_id": self._session_id(),
        }
        self.learning_log.append(entry)
        self._save_learning_log()
        return entry

    # ── Knowledge growth detection ─────────────────────────────────────────

    def check_and_log_gap(self, message: str) -> Optional[Dict]:
        """
        Detect recurring patterns: if same topic (by keyword hash) appears
        ≥ 3 times with no knowledge base match → flag a knowledge gap.

        Per continuous-knowledge-improvement.md §2 mandatory categories.
        """
        if not self.lookup(message, threshold=0.0):   # no match at all
            sig = self._hash(message[:120])
            count = sum(
                1 for e in self.learning_log
                if e.get("type") == "knowledge_gap" and e.get("query_hash") == sig
            )
            count += 1   # this invocation

            if count >= 3:
                gap_entry = {
                    "type":       "knowledge_gap",
                    "timestamp":  datetime.utcnow().isoformat() + "Z",
                    "message":    message,
                    "query_hash": sig,
                    "occurrence_count": count,
                    "confidence": 0.80,
                    "session_id": self._session_id(),
                    "resolved":   False,
                }
                self.learning_log.append(gap_entry)
                self._save_learning_log()
                return gap_entry
        return None

    # ── Stats / self-monitoring ───────────────────────────────────────────

    def get_stats(self) -> Dict[str, Any]:
        """Return knowledge-engine health metrics.
        Per 'have to keep eyes on supremeai plugin…md' monitoring rule.
        """
        total_entries = len(self.core_entries) + len(self.seed_entries)

        # Category coverage from seed knowledge
        cat_set = set(e.get("category", "") for e in self.seed_entries if e.get("category"))

        # Recurring query counts
        recurring = [e for e in self.learning_log if e.get("type") == "recurring_query"]
        corrections = [e for e in self.learning_log if e.get("type") == "user_correction"]
        gaps = [e for e in self.learning_log if e.get("type") == "knowledge_gap"]

        # Unresolved gaps
        open_gaps = [g for g in gaps if not g.get("resolved", False)]

        fallback_rate = (
            self.local_fallback_hits / self.total_interactions
            if self.total_interactions else 0.0
        )

        return {
            "total_interactions":      self.total_interactions,
            "total_kb_entries":        total_entries,
            "core_entries":            len(self.core_entries),
            "seed_entries":            len(self.seed_entries),
            "seed_categories_covered": sorted(cat_set),
            "knowledge_lookups":       self.knowledge_lookups,
            "local_fallback_hits":     self.local_fallback_hits,
            "local_fallback_rate":     round(fallback_rate, 4),
            "corrections_captured":    self.corrections_captured,
            "recurring_patterns":      len(recurring),
            "open_knowledge_gaps":     len(open_gaps),
            "total_gaps_detected":     len(gaps),
            "session_id":              self._session_id(),
            "healthy":                 total_entries > 0,
        }

    # ── Helpers ───────────────────────────────────────────────────────────

    @staticmethod
    def _hash(text: str) -> str:
        return hashlib.sha256(text.encode("utf-8")).hexdigest()[:16]

    @staticmethod
    def _session_id():
        return datetime.utcnow().strftime("%Y%m%d_%H%M")

    def record_interaction(self):
        """Call this after every user message — tracks session health."""
        self.total_interactions += 1
