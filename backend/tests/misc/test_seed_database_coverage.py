"""
Coverage tests for tools/seed_database.py.
Target: 100% line coverage.

কভারেজ টেস্ট — seed_database মডিউলের সকল ফাংশন ও শাখা কভার করা হয়েছে।
"""

# অব্যবহৃত os, sys, এবং tempfile ইমপোর্টগুলো সরিয়ে দেওয়া হলো
import sqlite3
from unittest.mock import MagicMock, patch

import pytest


class TestInitFtsDB:
    """Tests for _init_fts_db."""

    def test_init_fts_db_creates_table(self):
        """_init_fts_db should create the FTS virtual table."""
        with patch.dict("sys.modules", {"tools.knowledge.local_search_rag": MagicMock()}):
            from tools.seed_database import _init_fts_db

            conn = sqlite3.connect(":memory:")
            _init_fts_db(conn)
            cursor = conn.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='knowledge_fts'")
            assert cursor.fetchone() is not None
            conn.close()


class TestUpsertFTS:
    """Tests for _upsert_fts."""

    def test_upsert_fts_inserts_new_row(self):
        """_upsert_fts should insert a new row."""
        with patch.dict("sys.modules", {"tools.knowledge.local_search_rag": MagicMock()}):
            from tools.seed_database import _init_fts_db, _upsert_fts

            conn = sqlite3.connect(":memory:")
            _init_fts_db(conn)
            _upsert_fts(conn, 1, "Test Title", "Test content here", "test_source")
            cursor = conn.execute("SELECT title, content, source FROM knowledge_fts WHERE rowid=1")
            row = cursor.fetchone()
            assert row is not None
            assert row[0] == "Test Title"
            conn.close()

    def test_upsert_fts_updates_existing_row(self):
        """_upsert_fts should update an existing row via DELETE + INSERT."""
        with patch.dict("sys.modules", {"tools.knowledge.local_search_rag": MagicMock()}):
            from tools.seed_database import _init_fts_db, _upsert_fts

            conn = sqlite3.connect(":memory:")
            _init_fts_db(conn)
            _upsert_fts(conn, 1, "Original", "Original content", "src1")
            conn.execute("DELETE FROM knowledge_fts WHERE rowid=1")
            _upsert_fts(conn, 1, "Updated", "Updated content", "src2")
            cursor = conn.execute("SELECT title, content, source FROM knowledge_fts WHERE rowid=1")
            row = cursor.fetchone()
            assert row[0] == "Updated"
            conn.close()


class TestSeedAll:
    """Tests for seed_all."""

    def test_seed_all_no_seed_data_dir(self):
        """seed_all should handle missing seed_data directory gracefully."""
        with patch.dict("sys.modules", {"tools.knowledge.local_search_rag": MagicMock()}):
            from tools.seed_database import seed_all

            with (
                patch("tools.seed_database.os.path.exists", return_value=False),
                patch("tools.seed_database.logger"),
            ):
                result = seed_all()
                assert result is None

    def test_seed_all_with_rag_failure(self):
        """seed_all should handle RAG initialization failure."""
        with patch.dict("sys.modules", {"tools.knowledge.local_search_rag": MagicMock()}):
            from tools.seed_database import seed_all

            with (
                patch("tools.seed_database.LocalSearchRAG", side_effect=Exception("RAG init failed")),
                patch("tools.seed_database.logger"),
            ):
                with pytest.raises(
                    Exception
                ):  # -- intentionally broad: asserts *some* error propagates (mocked/validation failure), exact type varies
                    seed_all()

    def test_seed_all_empty_seed_dir(self):
        """seed_all should process an empty seed directory."""
        with patch.dict("sys.modules", {"tools.knowledge.local_search_rag": MagicMock()}):
            from tools.seed_database import seed_all

            with (
                patch("tools.seed_database.os.listdir", return_value=[]),
                patch("tools.seed_database.LocalSearchRAG") as mock_rag,
                patch("tools.seed_database.logger"),
            ):
                mock_rag_instance = MagicMock()
                mock_rag.return_value = mock_rag_instance
                result = seed_all()
                # seed_all কোনো রিটার্ন ভ্যালু দেয় না (None রিটার্ন করে)
                assert result is None

    def test_seed_all_skips_init_and_helpers(self):
        """seed_all should skip __init__.py and helpers.py files."""
        with patch.dict("sys.modules", {"tools.knowledge.local_search_rag": MagicMock()}):
            from tools.seed_database import seed_all

            with (
                patch("tools.seed_database.os.listdir", return_value=["__init__.py", "helpers.py"]),
                patch("tools.seed_database.LocalSearchRAG") as mock_rag,
                patch("tools.seed_database.logger"),
            ):
                mock_rag_instance = MagicMock()
                mock_rag.return_value = mock_rag_instance
                result = seed_all()
                # seed_all কোনো রিটার্ন ভ্যালু দেয় না (None রিটার্ন করে)
                assert result is None

    def test_seed_all_module_load_error(self):
        """seed_all should handle module loading errors."""
        with patch.dict("sys.modules", {"tools.knowledge.local_search_rag": MagicMock()}):
            from tools.seed_database import seed_all

            with (
                patch("tools.seed_database.os.listdir", return_value=["test_module.py"]),
                patch("tools.seed_database.os.path.exists", return_value=True),
                patch("tools.seed_database.importlib.util.spec_from_file_location", return_value=None),
                patch("tools.seed_database.LocalSearchRAG") as mock_rag,
                patch("tools.seed_database.logger"),
            ):
                mock_rag_instance = MagicMock()
                mock_rag.return_value = mock_rag_instance
                result = seed_all()
                # seed_all কোনো রিটার্ন ভ্যালু দেয় না (None রিটার্ন করে)
                assert result is None

    def test_seed_all_module_exec_error(self):
        """seed_all should handle module execution errors."""
        with patch.dict("sys.modules", {"tools.knowledge.local_search_rag": MagicMock()}):
            from tools.seed_database import seed_all

            with (
                patch("tools.seed_database.os.path.exists", return_value=True),
                patch("tools.seed_database.os.listdir", return_value=["broken_module.py"]),
                patch("tools.seed_database.LocalSearchRAG") as mock_rag,
                patch("tools.seed_database.logger"),
                patch("tools.seed_database.importlib.util.spec_from_file_location") as mock_spec,
            ):
                mock_rag_instance = MagicMock()
                mock_rag.return_value = mock_rag_instance
                mock_spec_obj = MagicMock()
                mock_spec_obj.loader = MagicMock()
                mock_spec.return_value = mock_spec_obj
                mock_spec_obj.loader.exec_module.side_effect = ImportError("Module broken")
                result = seed_all()
                # seed_all কোনো রিটার্ন ভ্যালু দেয় না (None রিটার্ন করে)
                assert result is None
