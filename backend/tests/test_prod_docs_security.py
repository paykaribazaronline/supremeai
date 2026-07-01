import os
import subprocess
import sys
import textwrap


def _run(code: str) -> subprocess.CompletedProcess:
    project_root = os.path.dirname(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    )
    backend_root = os.path.join(project_root, "backend")
    env = os.environ.copy()
    env["PYTHONPATH"] = os.pathsep.join([project_root, backend_root])

    gcp_mock_code = textwrap.dedent(
        """
        import sys
        from unittest.mock import MagicMock
        import google.auth
        google.auth.default = lambda *args, **kwargs: (MagicMock(), "dummy-project")
        
        # Patch clients to prevent network calls
        try:
            import google.cloud.firestore
            google.cloud.firestore.Client = MagicMock
        except ImportError:
            sys.modules['google.cloud.firestore'] = MagicMock()
        
        try:
            import google.cloud.secretmanager
            google.cloud.secretmanager.SecretManagerServiceClient = MagicMock
        except ImportError:
            sys.modules['google.cloud.secretmanager'] = MagicMock()
        """
    )
    full_code = gcp_mock_code + "\n" + code

    return subprocess.run(
        [sys.executable, "-c", full_code],
        cwd=project_root,
        env=env,
        capture_output=True,
        text=True,
        check=False,
    )


def test_docs_visible_in_local():
    code = textwrap.dedent(
        """
        import os
        os.environ["env"] = "local"
        os.environ["openrouter_api_key"] = "sk"
        os.environ["gemini_api_key"] = "sk"
        os.environ["sentry_dsn"] = "https://sentry.io/123"
        os.environ["SUPREMEAI_JWT_SECRET"] = "secure_jwt_secret_value_at_least_32_chars_long_test"
        import core.services as app_mod
        import core.services as services

        from fastapi.testclient import TestClient
        client = TestClient(app_mod.app)
        assert client.get("/docs").status_code == 200
        assert client.get("/redoc").status_code == 200
        assert client.get("/openapi.json").status_code == 200
        """
    )
    result = _run(code)
    assert result.returncode == 0, result.stdout + result.stderr


def test_docs_disabled_in_production():
    code = textwrap.dedent(
        """
        import os
        os.environ["env"] = "production"
        os.environ["debug"] = "false"
        os.environ["openrouter_api_key"] = "sk"
        os.environ["gemini_api_key"] = "sk"
        os.environ["sentry_dsn"] = "https://sentry.io/123"
        os.environ["SUPREMEAI_JWT_SECRET"] = "secure_jwt_secret_value_at_least_32_chars_long_test"
        import core.services as app_mod
        import core.services as services

        from fastapi.testclient import TestClient
        client = TestClient(app_mod.app)
        assert client.get("/docs").status_code == 404
        assert client.get("/redoc").status_code == 404
        assert client.get("/openapi.json").status_code == 404
        """
    )
    result = _run(code)
    assert result.returncode == 0, result.stdout + result.stderr
