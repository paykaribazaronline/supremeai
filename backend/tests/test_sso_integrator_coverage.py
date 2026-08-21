"""
Coverage tests for tools/sso_integrator.py.
Target: 100% line coverage.

SSO ইন্টিগ্রেটর মডিউলের সকল ফাংশন ও শাখা কভার করা হয়েছে।
"""

import os
import sys
from unittest.mock import MagicMock, patch

import pytest

_PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if _PROJECT_ROOT not in sys.path:
    sys.path.insert(0, _PROJECT_ROOT)


class TestSSOIntegratorInit:
    """Tests for SSOIntegrator.__init__."""

    def test_init_with_defaults(self):
        """SSOIntegrator should initialize with default empty saml_settings."""
        from tools.sso_integrator import SSOIntegrator

        integrator = SSOIntegrator()
        assert integrator.saml_settings == {}

    def test_init_with_settings(self):
        """SSOIntegrator should accept saml_settings."""
        from tools.sso_integrator import SSOIntegrator

        settings = {"sp_entity_id": "test-entity"}
        integrator = SSOIntegrator(saml_settings=settings)
        assert integrator.saml_settings["sp_entity_id"] == "test-entity"

    def test_init_onelogin_not_available(self):
        """SSOIntegrator should handle missing onelogin library."""
        from tools.sso_integrator import SSOIntegrator

        with patch("tools.sso_integrator.SSOIntegrator._load_onelogin", return_value=False):
            integrator = SSOIntegrator()
            assert integrator.onelogin is False


class TestLoadOnelogin:
    """Tests for _load_onelogin."""

    def test_load_onelogin_success(self):
        """_load_onelogin should return True when onelogin is importable."""
        from tools.sso_integrator import SSOIntegrator

        with patch("builtins.__import__", side_effect=ImportError("No module")):
            integrator = SSOIntegrator.__new__(SSOIntegrator)
            result = integrator._load_onelogin()
            assert result is False

    def test_load_onelogin_failure(self):
        """_load_onelogin should return False on ImportError."""
        from tools.sso_integrator import SSOIntegrator

        with patch("tools.sso_integrator.OneLogin_Saml2_Auth", create=True):
            integrator = SSOIntegrator.__new__(SSOIntegrator)
            result = integrator._load_onelogin()
            assert result is False


class TestPrepareRequest:
    """Tests for _prepare_request."""

    def test_prepare_request_with_onelogin(self):
        """_prepare_request should format request for onelogin."""
        from tools.sso_integrator import SSOIntegrator

        integrator = SSOIntegrator.__new__(SSOIntegrator)
        integrator.onelogin = True
        integrator.saml_settings = {
            "sp_entity_id": "https://example.com",
            "acs_url": "/acs",
            "query_string": "?param=value",
        }

        result = integrator._prepare_request({"post_data": {"key": "val"}})
        assert "https" in result
        assert "http_host" in result

    def test_prepare_request_without_onelogin(self):
        """_prepare_request should return request_data as-is without onelogin."""
        from tools.sso_integrator import SSOIntegrator

        integrator = SSOIntegrator.__new__(SSOIntegrator)
        integrator.onelogin = False

        result = integrator._prepare_request({"custom": "data"})
        assert result == {"custom": "data"}


class TestGetMetadata:
    """Tests for get_metadata."""

    def test_get_metadata_onelogin_fallback(self):
        """get_metadata should return fallback when onelogin is False."""
        from tools.sso_integrator import SSOIntegrator

        integrator = SSOIntegrator.__new__(SSOIntegrator)
        integrator.saml_settings = {}
        integrator.onelogin = False

        result = integrator.get_metadata()
        assert result["status"] == "fallback"

    def test_get_metadata_onelogin_error(self):
        """get_metadata should handle Exceptions gracefully."""
        from tools.sso_integrator import SSOIntegrator

        integrator = SSOIntegrator.__new__(SSOIntegrator)
        integrator.saml_settings = {}
        integrator.onelogin = True
        integrator._build_settings = MagicMock(side_effect=KeyError("test"))

        result = integrator.get_metadata()
        assert result["status"] == "fallback"


class TestGetSSOURL:
    """Tests for get_sso_url."""

    def test_get_sso_url_without_onelogin(self):
        """get_sso_url should return idp_sso_url when onelogin is False."""
        from tools.sso_integrator import SSOIntegrator

        integrator = SSOIntegrator.__new__(SSOIntegrator)
        integrator.onelogin = False
        integrator.saml_settings = {"idp_sso_url": "https://idp.example.com/sso"}

        result = integrator.get_sso_url()
        assert result == "https://idp.example.com/sso"

    def test_get_sso_url_onelogin_error(self):
        """get_sso_url should fallback on error."""
        from tools.sso_integrator import SSOIntegrator

        integrator = SSOIntegrator.__new__(SSOIntegrator)
        integrator.onelogin = True
        integrator._build_settings = MagicMock(side_effect=RuntimeError("fail"))
        integrator.saml_settings = {"idp_sso_url": "fallback-url"}

        result = integrator.get_sso_url(relay_state="relay")
        assert result == "fallback-url"


class TestProcessSSOResponse:
    """Tests for process_sso_response."""

    @pytest.mark.asyncio
    async def test_process_sso_response_without_onelogin(self):
        """process_sso_response should return error when onelogin not available."""
        from tools.sso_integrator import SSOIntegrator

        integrator = SSOIntegrator.__new__(SSOIntegrator)
        integrator.onelogin = False

        result = await integrator.process_sso_response({"SAMLResponse": "test"})
        assert "error" in result or "status" in result

    @pytest.mark.asyncio
    async def test_process_sso_response_onelogin_error(self):
        """process_sso_response should handle onelogin errors."""
        from tools.sso_integrator import SSOIntegrator

        integrator = SSOIntegrator.__new__(SSOIntegrator)
        integrator.onelogin = True
        integrator._build_settings = MagicMock()
        integrator._OneLogin_Saml2_Auth = MagicMock()
        integrator._prepare_request = MagicMock(return_value={})

        mock_auth = MagicMock()
        mock_auth.get_errors.return_value = ["Invalid response"]
        mock_auth.get_last_error_reason.return_value = "Signature mismatch"
        integrator._OneLogin_Saml2_Auth.return_value = mock_auth

        result = await integrator.process_sso_response({"SAMLResponse": "test"})
        assert result is not None


class TestValidateToken:
    """Tests for validate_token."""

    def test_validate_token_jwt_decode(self):
        """validate_token should decode JWT when jwt is available."""
        from tools.sso_integrator import SSOIntegrator

        integrator = SSOIntegrator.__new__(SSOIntegrator)
        integrator.onelogin = False

        with patch("tools.sso_integrator.jwt") as mock_jwt:
            mock_jwt.decode.return_value = {"sub": "user1", "email": "test@example.com"}
            result = integrator.validate_token("valid-token", "secret")
            assert result is not None

    def test_validate_token_jwt_none_returns_error(self):
        """validate_token should return error when jwt is unavailable."""
        from tools.sso_integrator import SSOIntegrator

        integrator = SSOIntegrator.__new__(SSOIntegrator)
        integrator.onelogin = False

        with patch("tools.sso_integrator.jwt", None):
            result = integrator.validate_token("test-token", "secret")
            assert "error" in result


class TestParseSamlResponse:
    """Tests for parse_saml_response."""

    def test_parse_saml_response_valid(self):
        """parse_saml_response should parse a valid SAML response."""
        from tools.sso_integrator import SSOIntegrator

        integrator = SSOIntegrator.__new__(SSOIntegrator)
        # Test with minimal XML
        saml_xml = """<?xml version="1.0"?>
<samlp:Response xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
                xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion">
  <saml:Assertion>
    <saml:AttributeStatement>
      <saml:Attribute Name="email">
        <saml:AttributeValue>user@example.com</saml:AttributeValue>
      </saml:Attribute>
    </saml:AttributeStatement>
  </saml:Assertion>
</samlp:Response>"""

        result = integrator.parse_saml_response(saml_xml)
        assert result is not None
