"""
Cloud-native PostgreSQL store using Supabase/Cloud SQL.
Replaces local SQLite for production.
"""
import os
from typing import Dict, Any, Optional, List
from loguru import logger
import psycopg2
from psycopg2.extras import RealDictCursor

class CloudPostgresStore:
    """
    Production-grade PostgreSQL store.
    Uses Supabase or Cloud SQL connection string.
    """

    def __init__(self):
        self.conn_string = os.getenv(
            "SUPABASE_DATABASE_URL_POOLER",
            os.getenv(
                "DATABASE_URL",
                os.getenv("SUPABASE_DATABASE_URL", "")
            )
        )
        self._init_tables()

    def _execute(self, query: str, params: tuple = (), fetch: bool = False, fetchone: bool = False) -> Any:
        """Executes a query and guarantees connection closure to prevent connection leakage in serverless environments."""
        conn = None
        try:
            conn = psycopg2.connect(self.conn_string, cursor_factory=RealDictCursor)
            with conn:
                with conn.cursor() as cur:
                    cur.execute(query, params)
                    if fetchone:
                        return cur.fetchone()
                    if fetch:
                        return cur.fetchall()
                    return None
        finally:
            if conn:
                try:
                    conn.close()
                except Exception:
                    pass

    def _init_tables(self):
        """Initialize tables if not exist."""
        self._execute("""
            CREATE TABLE IF NOT EXISTS task_history (
                id SERIAL PRIMARY KEY,
                task_type VARCHAR(50),
                prompt TEXT,
                result TEXT,
                provider VARCHAR(100),
                cost DECIMAL(10,6),
                latency_ms INTEGER,
                success BOOLEAN DEFAULT true,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)
        self._execute("""
            CREATE TABLE IF NOT EXISTS conversation_context (
                id SERIAL PRIMARY KEY,
                session_id VARCHAR(100) UNIQUE,
                user_id VARCHAR(100),
                messages JSONB,
                summary TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)
        self._execute("""
            CREATE TABLE IF NOT EXISTS verification_queue (
                id SERIAL PRIMARY KEY,
                email_target VARCHAR(255),
                otp_code VARCHAR(20),
                verification_link TEXT,
                processed BOOLEAN DEFAULT false,
                received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)
        logger.info("PostgreSQL tables initialized and connections closed cleanly.")

    def save_task(self, task_data: Dict[str, Any]) -> int:
        """Save task execution record."""
        query = """
            INSERT INTO task_history 
            (task_type, prompt, result, provider, cost, latency_ms, success)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
            RETURNING id
        """
        params = (
            task_data.get("task_type"),
            task_data.get("prompt"),
            task_data.get("result"),
            task_data.get("provider"),
            task_data.get("cost", 0.0),
            task_data.get("latency_ms", 0),
            task_data.get("success", True)
        )
        row = self._execute(query, params, fetchone=True)
        return row["id"] if row else 0

    def get_conversation(self, session_id: str) -> Optional[Dict[str, Any]]:
        """Get conversation context by session."""
        query = """
            SELECT * FROM conversation_context 
            WHERE session_id = %s 
            ORDER BY updated_at DESC 
            LIMIT 1
        """
        row = self._execute(query, (session_id,), fetchone=True)
        return dict(row) if row else None

    def update_conversation(self, session_id: str, messages: List[Dict], summary: str = ""):
        """Update or create conversation context."""
        from psycopg2.extras import Json
        query = """
            INSERT INTO conversation_context (session_id, messages, summary)
            VALUES (%s, %s, %s)
            ON CONFLICT (session_id) DO UPDATE SET
                messages = EXCLUDED.messages,
                summary = EXCLUDED.summary,
                updated_at = CURRENT_TIMESTAMP
        """
        self._execute(query, (session_id, Json(messages), summary))

    def get_stats(self) -> Dict[str, Any]:
        """Get system statistics."""
        query = """
            SELECT 
                COUNT(*) as total_tasks,
                AVG(cost) as avg_cost,
                SUM(cost) as total_cost,
                AVG(latency_ms) as avg_latency,
                COUNT(CASE WHEN success THEN 1 END)::FLOAT / COUNT(*) * 100 as success_rate
            FROM task_history
        """
        row = self._execute(query, fetchone=True)
        return dict(row) if row else {}

# Keep SQLite fallback for local dev
class SQLiteStore:
    """Local SQLite store for development only."""
    def __init__(self, db_path: str = "data/supremeai.db"):
        self.db_path = db_path
        os.makedirs(os.path.dirname(db_path), exist_ok=True)
        # ... existing SQLite implementation ...
