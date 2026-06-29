from unittest.mock import MagicMock
from unittest.mock import patch

import pytest

from tools.skill_recommender import SkillRecommender


@pytest.fixture
def recommender():
    return SkillRecommender()


def test_local_history_empty(recommender):
    assert recommender._get_user_history("user-1") == []


def test_record_task_local(recommender):
    recommender._record_task("user-1", {"description": "test task", "type": "query"})
    history = recommender._get_user_history("user-1")
    assert len(history) == 1
    assert history[0]["task"]["description"] == "test task"


def test_embedding_shape(recommender):
    vec = recommender._embedding("hello world")
    assert len(vec) == 64
    assert all(isinstance(x, float) for x in vec)
    assert all(-1 <= x <= 1 for x in vec)


def test_embedding_deterministic(recommender):
    a = recommender._embedding("task text")
    b = recommender._embedding("task text")
    assert a == b


def test_cosine_similarity_identical():
    r = SkillRecommender()
    vec = [1.0, 0.0, 0.0]
    assert abs(r._cosine_similarity(vec, vec) - 1.0) < 1e-9


def test_cosine_similarity_orthogonal():
    r = SkillRecommender()
    a = [1.0, 0.0]
    b = [0.0, 1.0]
    assert abs(r._cosine_similarity(a, b)) < 1e-9


def test_cosine_similarity_zero_vector():
    r = SkillRecommender()
    assert r._cosine_similarity([0.0, 0.0], [1.0, 1.0]) == 0.0


def test_recommend_no_history(recommender):
    recs = recommender.recommend("user-1", "new task")
    assert recs == []


def test_recommend_with_local_history():
    rec = SkillRecommender()
    rec._record_task("user-1", {"description": "draft email", "type": "query", "skill_id": "skill-email"})
    rec._record_task("user-1", {"description": "send report", "type": "query", "skill_id": "skill-report"})
    recs = rec.recommend("user-1", "draft memo")
    assert len(recs) > 0
    scores = [r["match_score"] for r in recs]
    assert scores == sorted(scores, reverse=True)


def test_recommend_enriches_from_db(recommender):
    mock_db = MagicMock()
    mock_db.client = MagicMock()
    with patch("tools.skill_recommender.db", mock_db):
        recommender._get_user_history = MagicMock(return_value=[
            {"task": {"description": "invoice generation", "skill_id": "skill-invoice"}}
        ])
        mock_db.client.table.return_value.select.return_value.eq.return_value.execute.return_value.data = [
            {"id": "skill-invoice", "name": "Invoice Generator", "category": "billing"}
        ]
        recs = recommender.recommend("user-1", "create invoice")
        assert len(recs) == 1
        assert recs[0]["id"] == "skill-invoice"
        assert isinstance(recs[0]["match_score"], float)
        assert recs[0]["category"] == "billing"


def test_recommend_db_failure_falls_back(recommender):
    mock_db = MagicMock()
    mock_db.client = MagicMock()
    with patch("tools.skill_recommender.db", mock_db):
        recommender._get_user_history = MagicMock(return_value=[
            {"task": {"description": "email task", "skill_id": "skill-email"}}
        ])
        mock_db.client.table.return_value.select.return_value.eq.return_value.execute.side_effect = Exception("db error")
        recs = recommender.recommend("user-1", "send email")
        assert len(recs) == 1
        assert recs[0]["id"] == "skill-email"
        assert recs[0]["category"] == "inferred"


def test_record_and_recommend(recommender):
    with patch.object(recommender, "recommend", return_value=[]):
        result = recommender.record_and_recommend("user-1", "new task")
    assert result["user_id"] == "user-1"
    assert result["count"] == 0
    assert len(recommender._get_user_history("user-1")) == 2
