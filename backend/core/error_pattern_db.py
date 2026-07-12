"""This module defines the `ErrorPatternDB` class, a specialized component for managing a persistent SQLite database dedicated to logging, retrieving, and analyzing AI model errors and specific AI mistakes. Within the SupremeAI project, it serves as a crucial feedback mechanism, enabling the system to learn from past failures, identify recurring patterns (e.g., hallucinations), and derive actionable prevention strategies to continuously improve the reliability, accuracy, and robustness of AI agent outputs.

Key Components:
- `ErrorPatternDB`: Manages an SQLite database to store and retrieve historical data on general AI errors and detailed AI model mistakes.
- `ErrorPatternDB.log_error()`: Records a general error pattern, its type, and a correction into the `errors` table.
- `ErrorPatternDB.log_ai_mistake()`: Logs comprehensive details about a specific AI model mistake, including the model, task, original/correct output, root cause, and a prevention strategy, into the `ai_mistakes` table.
- `ErrorPatternDB.get_prevention_strategy()`: Queries the database to retrieve the most frequently recorded prevention strategy for a given AI model and task type.
- `ErrorPatternDB.check_pattern()`: Analyzes a given AI output string against known error patterns in the database to identify potential matches and suggest prevention.

Dependencies:
- `sqlite3`: For interacting with the SQLite database to store and retrieve error patterns and AI mistakes.
- `datetime`: For generating precise timestamps for all database entries."""

import sqlite3
from datetime import UTC
from datetime import datetime


class ErrorPatternDB:
    def __init__(self, db_path: str = "hallucination_patterns.db"):
        self.db_path = db_path
        self._init_db()

    def _init_db(self):
        conn = sqlite3.connect(self.db_path, check_same_thread=False)
        cursor = conn.cursor()
        cursor.execute(
            """
            CREATE TABLE IF NOT EXISTS errors (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                output TEXT,
                error_type TEXT,
                correction TEXT,
                timestamp TEXT
            )
        """
        )
        cursor.execute(
            """
            CREATE TABLE IF NOT EXISTS ai_mistakes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                model_name TEXT,
                mistake_type TEXT,
                task_description TEXT,
                original_output TEXT,
                correct_output TEXT,
                root_cause TEXT,
                prevention_strategy TEXT,
                timestamp TEXT
            )
        """
        )
        conn.commit()
        conn.close()

    def log_error(self, output: str, error_type: str, correction: str):
        conn = sqlite3.connect(self.db_path, check_same_thread=False)
        cursor = conn.cursor()
        cursor.execute(
            "INSERT INTO errors (output, error_type, correction, timestamp) VALUES (?, ?, ?, ?)",
            (output, error_type, correction, datetime.now(UTC).isoformat()),
        )
        conn.commit()
        conn.close()

    def log_ai_mistake(self, mistake: dict):
        conn = sqlite3.connect(self.db_path, check_same_thread=False)
        cursor = conn.cursor()
        cursor.execute(
            "INSERT INTO ai_mistakes (model_name, mistake_type, task_description, "
            "original_output, correct_output, root_cause, prevention_strategy, "
            "timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            (
                mistake.get("model", "unknown"),
                mistake.get("type", "unknown"),
                mistake.get("task", "unknown"),
                mistake.get("original", ""),
                mistake.get("correct", ""),
                mistake.get("root_cause", ""),
                mistake.get("prevention", ""),
                datetime.now(UTC).isoformat(),
            ),
        )
        conn.commit()
        conn.close()

    def get_prevention_strategy(self, model: str, task_type: str) -> str:
        conn = sqlite3.connect(self.db_path, check_same_thread=False)
        cursor = conn.cursor()
        cursor.execute(
            "SELECT prevention_strategy FROM ai_mistakes WHERE model_name = ? AND "
            "task_description LIKE ? GROUP BY prevention_strategy ORDER BY COUNT(*) DESC LIMIT 1",
            (model, f"%{task_type}%"),
        )
        result = cursor.fetchone()
        conn.close()
        return result[0] if result else "No historical data - use default validation"

    def check_pattern(self, output: str) -> dict:
        conn = sqlite3.connect(self.db_path, check_same_thread=False)
        cursor = conn.cursor()
        cursor.execute(
            "SELECT error_type, correction, COUNT(*) FROM errors WHERE ? LIKE '%' || output || '%' GROUP BY error_type",
            (output,),
        )
        patterns = cursor.fetchall()
        conn.close()
        return {"known_patterns": patterns, "should_prevent": len(patterns) > 0}
