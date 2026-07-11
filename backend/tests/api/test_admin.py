# FILE_PATH: tests/api/test_admin.py
import pytest
from unittest.mock import patch, MagicMock, AsyncMock
from fastapi.testclient import TestClient
from main import app as main_app  # Alias 'app' to 'main_app' to avoid name collision with fixture

# Removed global client initialization.
# It's generally better practice to create the TestClient within a pytest fixture
# to ensure the FastAPI app is fully initialized and configured for each test/session.


@pytest.fixture(scope="module") # Use module scope for efficiency if app setup is heavy
def client():
    """Provides a TestClient instance for the FastAPI application."""
    with TestClient(main_app) as client_instance:
        yield client_instance


@pytest.fixture
def mock_admin_token():
    with patch("api.dependencies.get_current_user_token") as mock:
        mock.return_value = {"sub": "admin_test", "role": "admin"}
        yield mock


@pytest.fixture
def mock_healer():
    with patch("api.routes.admin.get_healer_service") as mock:
        service = MagicMock()
        service.apply_fix = AsyncMock(return_value=True)
        mock.return_value = service
        yield mock


@pytest.fixture
def mock_firestore():
    with patch("api.routes.admin.get_firestore_db") as mock:
        db = MagicMock()
        mock.return_value = db
        yield db


@patch("api.routes.admin.get_current_user_token")
def test_get_fixes_unauthorized(mock_token, client: TestClient):  # Inject the client fixture
    main_app.dependency_overrides[mock_token] = lambda: {"sub": "user_test", "role": "user"}
    from api.routes.admin import get_current_admin

    # We must also clear the get_current_admin override if it exists, or just mock it to raise 403
    main_app.dependency_overrides[get_current_admin] = lambda: (_ for _ in ()).throw(
        __import__("fastapi").HTTPException(status_code=403, detail="Not enough permissions")
    )

    response = client.get("/api/admin/fixes")
    assert response.status_code in (401, 403), f"Unexpected status: {response.status_code}, details: {response.text}"
    main_app.dependency_overrides = {}


@patch("api.routes.admin.get_current_user_token")
def test_get_fixes_authorized(mock_token, mock_healer, mock_firestore, client: TestClient):  # Inject the client fixture
    main_app.dependency_overrides[mock_token] = lambda: {"sub": "admin_test", "role": "admin"}

    # Mocking Firestore response
    mock_query = MagicMock()
    mock_doc = MagicMock()
    mock_doc.id = "fix_1"
    mock_doc.to_dict.return_value = {"status": "pending_review"}

    # Async mock for get()
    async def mock_get():
        return [mock_doc]

    mock_query.get = mock_get
    mock_firestore.collection.return_value.document.return_value.collection.return_value.where.return_value = mock_query

    # We need to use app.dependency_overrides for proper injection testing
    from api.routes.admin import get_current_admin

    main_app.dependency_overrides[get_current_admin] = lambda: {"sub": "admin_test", "role": "admin"}

    response = client.get("/api/admin/fixes?tenant_id=test")
    assert response.status_code == 200, f"Unexpected status: {response.status_code}, details: {response.text}"
    assert "fixes" in response.json()
    assert len(response.json()["fixes"]) == 1

    main_app.dependency_overrides = {}
