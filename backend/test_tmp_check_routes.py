import io
from unittest.mock import MagicMock
from unittest.mock import patch

import pytest


# Since the source code is a script that executes on import, 
# we need to mock the dependencies before importing it.
# We use a function to reload the module to test different scenarios.

def run_target_module():
    """
    Helper to execute the code in tmp_check_routes.py.
    Because the code is at the module level, we use importlib to reload it.
    """
    import importlib

    import tmp_check_routes
    importlib.reload(tmp_check_routes)

class TestTmpCheckRoutes:
    """
    Test suite for tmp_check_routes.py.
    The target code iterates through app.routes and prints paths containing 'voice' or 'tts'.
    """

    @pytest.fixture
    def mock_app(self, monkeypatch):
        """
        Creates a mock FastAPI-like app object with a routes list.
        """
        mock_app = MagicMock()
        # Default mock routes
        mock_app.routes = [
            MagicMock(path="/api/voice/synthesize"),
            MagicMock(path="/api/tts/stream"),
            MagicMock(path="/api/user/profile"),
            MagicMock(path="/api/health"),
        ]
        return mock_app

    def test_execution_success_with_matching_routes(self, mock_app, monkeypatch):
        """
        Test that the script correctly identifies and prints routes containing 'voice' or 'tts'.
        """
        # Patch the 'core.app.app' import
        monkeypatch.setattr("core.app.app", mock_app)
        
        # Capture stdout to verify printed output
        with patch('sys.stdout', new=io.StringIO()) as fake_out:
            run_target_module()
            output = fake_out.getvalue()
            
            assert "Total routes: 4" in output
            assert "Relevant route: /api/voice/synthesize" in output
            assert "Relevant route: /api/tts/stream" in output
            assert "/api/user/profile" not in output

    def test_execution_with_no_matching_routes(self, monkeypatch):
        """
        Test scenario where no routes contain the keywords 'voice' or 'tts'.
        """
        mock_app = MagicMock()
        mock_app.routes = [
            MagicMock(path="/api/login"),
            MagicMock(path="/api/logout"),
        ]
        monkeypatch.setattr("core.app.app", mock_app)

        with patch('sys.stdout', new=io.StringIO()) as fake_out:
            run_target_module()
            output = fake_out.getvalue()
            
            assert "Total routes: 2" in output
            assert "Relevant route:" not in output

    def test_execution_with_empty_routes(self, monkeypatch):
        """
        Test scenario where the routes list is empty.
        """
        mock_app = MagicMock()
        mock_app.routes = []
        monkeypatch.setattr("core.app.app", mock_app)

        with patch('sys.stdout', new=io.StringIO()) as fake_out:
            run_target_module()
            output = fake_out.getvalue()
            
            assert "Total routes: 0" in output

    def test_execution_with_routes_missing_path_attribute(self, monkeypatch):
        """
        Test scenario where some route objects do not have a 'path' attribute.
        Ensures the `hasattr(r, 'path')` check works.
        """
        mock_app = MagicMock()
        # One object with path, one without
        mock_app.routes = [
            MagicMock(path="/api/voice/test"),
            MagicMock(spec=[]) # This object has no 'path' attribute
        ]
        monkeypatch.setattr("core.app.app", mock_app)

        with patch('sys.stdout', new=io.StringIO()) as fake_out:
            run_target_module()
            output = fake_out.getvalue()
            
            # Only the one with 'path' should be counted
            assert "Total routes: 1" in output
            assert "Relevant route: /api/voice/test" in output

    def test_execution_exception_handling(self, monkeypatch):
        """
        Test that the try-except block catches exceptions and calls traceback.print_exc().
        """
        # Force an exception during the routes access
        mock_app = MagicMock()
        type(mock_app).routes = property(lambda x: exec('raise Exception("Database Error")'))
        monkeypatch.setattr("core.app.app", mock_app)

        with patch('traceback.print_exc') as mock_traceback:
            run_target_module()
            # Verify that traceback.print_exc() was called due to the exception
            mock_traceback.assert_called_once()

    def test_execution_with_none_routes(self, monkeypatch):
        """
        Test scenario where app.routes is None, which should trigger the exception block.
        """
        mock_app = MagicMock()
        mock_app.routes = None 
        monkeypatch.setattr("core.app.app", mock_app)

        with patch('traceback.print_exc') as mock_traceback:
            run_target_module()
            mock_traceback.assert_called_once()

    def test_execution_with_large_number_of_routes(self, monkeypatch):
        """
        Test performance/correctness with a large input of routes.
        """
        mock_app = MagicMock()
        # 1000 routes, half containing 'voice'
        mock_app.routes = [MagicMock(path=f"/route_{i}_voice" if i % 2 == 0 else f"/route_{i}") for i in range(1000)]
        monkeypatch.setattr("core.app.app", mock_app)

        with patch('sys.stdout', new=io.StringIO()) as fake_out:
            run_target_module()
            output = fake_out.getvalue()
            
            assert "Total routes: 1000" in output
            # Count occurrences of "Relevant route"
            assert output.count("Relevant route:") == 500

    @pytest.mark.asyncio
    async def test_async_compatibility(self, mock_app, monkeypatch):
        """
        Ensure that the module execution doesn't block or crash in an async environment.
        """
        monkeypatch.setattr("core.app.app", mock_app)
        # The target code is synchronous, but we ensure it runs within an async test without side effects
        run_target_module()
        assert True # If it reaches here without exception, it's compatible
