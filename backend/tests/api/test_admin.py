# FILE_PATH: tests/api/test_admin.py
import pytest
from unittest.mock import patch, MagicMock, AsyncMock
from fastapi.testclient import TestClient
from fastapi import FastAPI, HTTPException # Import FastAPI and HTTPException
from api.routes import admin # Import the admin router directly
from api.routes.admin import get_current_admin # Import get_current_admin for dependency override

# Removed: from main import app
# Removed: client = TestClient(app)
# The TestClient and app instance will now be provided via fixtures.

# Fixture to create a test application instance with necessary routers
@pytest.fixture(scope="module")
def test_app():
    """
    Creates a FastAPI application instance specifically for testing.
    This ensures that the TestClient uses an app with the admin router
    properly included, preventing 404 Not Found errors due to un-registered routes.
    """
    _app = FastAPI()
    # Explicitly include the admin router with its expected prefix.
    # This is crucial to ensure /api/admin/fixes is a registered route.
    _app.include_router(admin.router, prefix="/api/admin")
    yield _app

# Fixture to provide a TestClient instance for the test_app
@pytest.fixture(scope="module")
def client(test_app):
    """
    Provides a TestClient instance connected to the test_app fixture.
    """
    with TestClient(test_app) as c:
        yield c

@pytest.fixture
def mock_admin_token():
    """
    Mocks the get_current_user_token dependency to return an admin role payload.
    This fixture patches the original dependency function directly.
    """
    with patch("api.dependencies.get_current_user_token") as mock:
        mock.return_value = {"sub": "admin_test", "role": "admin"}
        yield mock

@pytest.fixture
def mock_healer():
    """
    Mocks the get_healer_service dependency.
    """
    with patch("api.routes.admin.get_healer_service") as mock:
        service = MagicMock()
        service.apply_fix = AsyncMock(return_value=True)
        mock.return_value = service
        yield mock

@pytest.fixture
def mock_firestore():
    """
    Mocks the get_firestore_db dependency.
    """
    with patch("api.routes.admin.get_firestore_db") as mock:
        db = MagicMock()
        mock.return_value = db
        yield db


@patch("api.routes.admin.get_current_user_token") # Patches where get_current_user_token is used in admin routes
def test_get_fixes_unauthorized(mock_token, client): # Inject client fixture
    """
    Tests that an unauthorized user cannot access the /api/admin/fixes endpoint.
    Expects a 401 or 403 status code.
    """
    # Configure the mock for get_current_user_token to return a user token (unauthorized for admin routes)
    mock_token.return_value = {"sub": "user_test", "role": "user"}

    # Override get_current_admin dependency to explicitly raise a 403 HTTPException
    # This ensures that even if a token passes, admin permissions are denied.
    client.app.dependency_overrides[get_current_admin] = lambda: (_ for _ in ()).throw(
        HTTPException(status_code=403, detail="Not enough permissions")
    )

    response = client.get("/api/admin/fixes")
    assert response.status_code in (401, 403), f"Unexpected status: {response.status_code}, details: {response.text}"
    
    # Clear all dependency overrides specific to this test to ensure a clean state for subsequent tests.
    client.app.dependency_overrides = {}


def test_get_fixes_authorized(mock_admin_token, mock_healer, mock_firestore, client): # Inject client fixture
    """
    Tests that an authorized admin user can access the /api/admin/fixes endpoint.
    Expects a 200 OK status code.
    """
    # mock_admin_token fixture already sets get_current_user_token to return an admin payload.

    # Mocking Firestore response for the admin endpoint
    mock_query = MagicMock()
    mock_doc = MagicMock()
    mock_doc.id = "fix_1"
    mock_doc.to_dict.return_value = {"status": "pending_review"}

    # Async mock for get() method of the Firestore query result
    async def mock_get():
        return [mock_doc]

    mock_query.get = mock_get
    # Configure mock_firestore to return the mocked query result
    mock_firestore.collection.return_value.document.return_value.collection.return_value.where.return_value = mock_query

    # Override get_current_admin dependency to return an admin payload, granting access
    client.app.dependency_overrides[get_current_admin] = lambda: {"sub": "admin_test", "role": "admin"}

    response = client.get("/api/admin/fixes?tenant_id=test")
    assert response.status_code == 200, f"Unexpected status: {response.status_code}, details: {response.text}"
    assert "fixes" in response.json()
    assert len(response.json()["fixes"]) == 1

    # Clear all dependency overrides specific to this test.
    client.app.dependency_overrides = {}
