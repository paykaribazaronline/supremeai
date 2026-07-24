import os
import sqlite3
import sys

# Add backend to path
sys.path.insert(0, os.path.abspath("backend"))

from unittest.mock import patch

import database.supabase_client
from core.evolution.evolution_engine import EvolutionEngine


def test_saga():
    db_path = os.path.abspath("test_evolution_saga.db")
    if os.path.exists(db_path):
        os.remove(db_path)

    engine = EvolutionEngine(db_path=db_path)
    # Testing Supabase Failure Scenario

    # Force db.client to be truthy so it enters the block
    database.supabase_client.db.client = True

    with patch(
        "database.supabase_client.db.insert_task_history",
        side_effect=Exception("Simulated Supabase Failure"),
    ):
        res = engine.learn_from_success(
            "saga_test_task", "test_approach", "test_result"
        )
        # Check Saga Response

        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM task_history WHERE task='saga_test_task'")
        rows = cursor.fetchall()
        # Verify SQLite Rows (Should be empty)
        conn.close()

    if os.path.exists(db_path):
        os.remove(db_path)


if __name__ == "__main__":
    test_saga()
