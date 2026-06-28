from fastapi.testclient import TestClient
from core.app import app

# বাংলা মন্তব্য: এপিআই এন্ডপয়েন্টটি ড্রাই-রান মোডে সঠিক নোড ও এজ ফরম্যাট দিচ্ছে কিনা তা যাচাই করা।


def test_get_skill_graph_dry_run():
    # বাংলা মন্তব্য: ড্রাই-রান মোডে গ্রাফ নোড এবং এজ ফরম্যাট ভ্যালিডেশন
    with TestClient(app) as client:
        response = client.get("/api/v1/graph/skills")
        assert response.status_code == 200

        data = response.json()
        assert "nodes" in data
        assert "edges" in data
        assert len(data["nodes"]) > 0
        assert data["nodes"][0]["id"] == "python"


def test_get_learning_path_dry_run():
    # বাংলা মন্তব্য: দুটি স্কিলের মধ্যে ড্রাই-রান মোডে পাথ ফাইন্ডিং চেক
    with TestClient(app) as client:
        response = client.get("/api/v1/graph/path?start_skill=Python&end_skill=FastAPI")
        assert response.status_code == 200

        data = response.json()
        assert "path" in data
        assert "Dry-run Path Node 1" in data["path"]
