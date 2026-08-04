import os
from unittest.mock import MagicMock, patch

from memory.supabase_store import SupabaseStore


def test_supabase_store_sqlite_fallback():
    # If no URL, defaults to SQLite
    with patch.dict(os.environ, {}, clear=True):
        store = SupabaseStore(database_url=None, local_path=":memory:")
    assert store.provider == "sqlite"

    # Save/get conversation
    messages = [{"role": "user", "content": "hi"}]
    store.save_conversation("session_123", messages)

    # Retrive
    history = store.get_conversation("session_123")
    assert len(history) == 1
    assert history[0]["content"] == "hi"


def test_supabase_store_cloud_upsert():
    mock_client = MagicMock()
    mock_table = MagicMock()
    mock_upsert = MagicMock()
    mock_client.table.return_value = mock_table
    mock_table.upsert.return_value = mock_upsert

    mock_supabase_mod = MagicMock()
    mock_supabase_mod.create_client.return_value = mock_client

    with patch.dict("sys.modules", {"supabase": mock_supabase_mod}), patch.dict(
        os.environ, {"SUPABASE_KEY": "my-key"}
    ):
        store = SupabaseStore(
            database_url="postgresql://db.supabase.co:5432/postgres",
            local_path=":memory:",
        )
        assert store.provider == "supabase"

        messages = [{"role": "system", "content": "hello"}]
        store.save_conversation("session_abc", messages)

        mock_client.table.assert_called_with("conversations")
        mock_table.upsert.assert_called_once()
        mock_upsert.execute.assert_called_once()


def test_supabase_store_learned_fact_vector_search():
    # বাংলা মন্তব্য: learned_facts টেবিলে pgvector সেম্যান্টিক সার্চ ও এর ilike ফলব্যাক মেকানিজম ভেরিফাই করার জন্য টেস্ট।
    mock_client = MagicMock()
    mock_table = MagicMock()
    mock_upsert = MagicMock()
    mock_rpc = MagicMock()

    mock_client.table.return_value = mock_table
    mock_table.upsert.return_value = mock_upsert
    mock_client.rpc.return_value = mock_rpc

    mock_supabase_mod = MagicMock()
    mock_supabase_mod.create_client.return_value = mock_client

    # Mocking litellm embedding generation
    mock_embedding_response = MagicMock()
    mock_embedding_response.data = [{"embedding": [0.1] * 1536}]

    with (
        patch.dict("sys.modules", {"supabase": mock_supabase_mod}),
        patch.dict(os.environ, {"SUPABASE_KEY": "my-key"}),
        patch("litellm.embedding", return_value=mock_embedding_response),
    ):
        store = SupabaseStore(
            database_url="postgresql://db.supabase.co:5432/postgres",
            local_path=":memory:",
        )

        # Test 1: save_learned_fact sends embedding
        fact = {"id": "fact-1", "content": "SupremeAI is awesome", "tags": ["tech"]}
        store.save_learned_fact(fact)

        mock_client.table.assert_called_with("learned_facts")
        upsert_data = mock_table.upsert.call_args[0][0]
        assert upsert_data["id"] == "fact-1"
        assert upsert_data["embedding"] == [0.1] * 1536

        # Test 2: search_facts triggers RPC
        mock_rpc.execute.return_value = MagicMock(
            data=[{"content": '{"id": "fact-1", "content": "SupremeAI is awesome"}'}]
        )
        results = store.search_facts("awesome")

        mock_client.rpc.assert_called_with(
            "match_learned_facts",
            {"query_embedding": [0.1] * 1536, "match_threshold": 0.3, "match_count": 5},
        )
        assert len(results) == 1
        assert results[0]["id"] == "fact-1"

        # Test 3: search_facts falls back to ilike if RPC fails
        mock_client.rpc.side_effect = Exception("RPC failed")
        mock_ilike = MagicMock()
        mock_execute = MagicMock()

        mock_table.select.return_value = mock_ilike
        mock_ilike.ilike.return_value = mock_execute
        mock_execute.execute.return_value = MagicMock(
            data=[{"content": '{"id": "fact-1", "content": "SupremeAI is awesome"}'}]
        )

        results_fallback = store.search_facts("awesome")
        assert len(results_fallback) == 1
        assert results_fallback[0]["content"] == "SupremeAI is awesome"
