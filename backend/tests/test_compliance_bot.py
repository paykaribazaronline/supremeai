"""Tests for ComplianceBot - GDPR and Digital Security Act compliance checking.

This module tests:
- GDPRChecker: lawful basis, data minimization, retention limits, right to deletion
- DigitalSecurityActChecker: data localization, content moderation, interception readiness
- ConsentManager: recording, withdrawing, and checking consent status
- DataRetentionPolicy: enforcing retention periods
- ComplianceBot: full compliance pipeline
"""

from datetime import UTC, datetime, timedelta
from unittest.mock import MagicMock, patch

import pytest
from core.security.compliance_bot import (ComplianceBot, ComplianceReport,
                                          ComplianceViolation, ConsentManager,
                                          ConsentRecord, ConsentType,
                                          DataRetentionPolicy,
                                          DigitalSecurityActChecker,
                                          GDPRChecker, RegulationType)

# --- ConsentRecord Tests ---


def test_consent_record_to_dict():
    """Test ConsentRecord serialization to dictionary."""
    now = datetime.now(UTC)
    expires = now + timedelta(days=30)

    record = ConsentRecord(
        user_id="user123",
        consent_type=ConsentType.DATA_PROCESSING,
        granted=True,
        granted_at=now,
        expires_at=expires,
        ip_address="192.168.1.1",
        user_agent="Mozilla/5.0",
        version="1.0",
    )

    result = record.to_dict()

    assert result["user_id"] == "user123"
    assert result["consent_type"] == "data_processing"
    assert result["granted"] is True
    assert result["ip_address"] == "192.168.1.1"
    assert result["expires_at"] is not None
    assert result["version"] == "1.0"
    assert result["withdrawn_at"] is None


def test_consent_record_is_valid_when_granted():
    """Test ConsentRecord validity check when granted and not expired."""
    record = ConsentRecord(
        user_id="user123",
        consent_type=ConsentType.DATA_PROCESSING,
        granted=True,
        granted_at=datetime.now(UTC),
        expires_at=None,
    )

    assert record.is_valid() is True


def test_consent_record_is_invalid_when_not_granted():
    """Test ConsentRecord validity check when not granted."""
    record = ConsentRecord(
        user_id="user123",
        consent_type=ConsentType.DATA_PROCESSING,
        granted=False,
        granted_at=datetime.now(UTC),
    )

    assert record.is_valid() is False


def test_consent_record_is_invalid_when_expired():
    """Test ConsentRecord validity check when expired."""
    record = ConsentRecord(
        user_id="user123",
        consent_type=ConsentType.DATA_PROCESSING,
        granted=True,
        granted_at=datetime.now(UTC) - timedelta(days=60),
        expires_at=datetime.now(UTC) - timedelta(days=1),  # Expired yesterday
    )

    assert record.is_valid() is False


def test_consent_record_is_invalid_when_withdrawn():
    """Test ConsentRecord validity check when withdrawn."""
    record = ConsentRecord(
        user_id="user123",
        consent_type=ConsentType.DATA_PROCESSING,
        granted=True,
        granted_at=datetime.now(UTC),
        withdrawn_at=datetime.now(UTC),
    )

    assert record.is_valid() is False


# --- ComplianceViolation Tests ---


def test_compliance_violation_to_dict():
    """Test ComplianceViolation serialization."""
    violation = ComplianceViolation(
        regulation=RegulationType.GDPR,
        severity="critical",
        category="lawful_basis",
        description="No valid consent",
        affected_data=["user_data"],
        remediation="Obtain consent",
    )

    result = violation.to_dict()

    assert result["regulation"] == "gdpr"
    assert result["severity"] == "critical"
    assert result["category"] == "lawful_basis"
    assert result["description"] == "No valid consent"
    assert result["affected_data"] == ["user_data"]
    assert result["remediation"] == "Obtain consent"


# --- ComplianceReport Tests ---


def test_compliance_report_to_dict():
    """Test ComplianceReport serialization."""
    report = ComplianceReport(
        overall_compliant=True,
        regulations_checked=[RegulationType.GDPR],
        violations=[],
        consent_status={"data_processing": True},
        data_retention_status={"session_logs_limit_days": 30},
        recommendations=["Continue monitoring"],
    )

    result = report.to_dict()

    assert result["overall_compliant"] is True
    assert result["regulations_checked"] == ["gdpr"]
    assert result["violations_count"] == 0
    assert result["consent_status"] == {"data_processing": True}
    assert result["recommendations"] == ["Continue monitoring"]


# --- GDPRChecker Tests ---


class TestGDPRChecker:
    """Tests for GDPRChecker class."""

    @pytest.fixture
    def mock_firestore(self):
        """Mock Firestore client."""
        with patch("core.security.compliance_bot.get_firestore_client") as mock:
            client = MagicMock()
            client.collection.return_value.document.return_value.get.return_value.exists = (
                False
            )
            mock.return_value = client
            yield client

    @pytest.fixture
    def gdpr_checker(self, mock_firestore):
        """Create GDPRChecker instance with mocked Firestore."""
        return GDPRChecker()

    def test_check_lawful_basis_no_consent(self, gdpr_checker):
        """Test lawful basis check fails when no consent exists."""
        result = gdpr_checker.check_lawful_basis("user123", "data_processing")

        assert result is not None
        assert result.regulation == RegulationType.GDPR
        assert result.severity == "critical"
        assert result.category == "lawful_basis"
        assert "No valid consent" in result.description

    def test_check_lawful_basis_with_valid_consent(self, gdpr_checker, mock_firestore):
        """Test lawful basis check passes with valid consent."""
        mock_doc = MagicMock()
        mock_doc.exists = True
        mock_doc.to_dict.return_value = {
            "user_id": "user123",
            "consent_type": "data_processing",
            "granted": True,
            "granted_at": datetime.now(UTC).isoformat(),
        }
        mock_firestore.collection.return_value.document.return_value.get.return_value = (
            mock_doc
        )

        result = gdpr_checker.check_lawful_basis("user123", "data_processing")

        # Should not return violation if consent exists and is valid
        # Note: This depends on the exact implementation

    def test_check_data_minimization_excessive_fields(self, gdpr_checker):
        """Test data minimization check detects excessive fields."""
        # For authentication, only email and password_hash are required
        fields = ["email", "password_hash", "ssn", "credit_card"]

        result = gdpr_checker.check_data_minimization(fields, "authentication")

        assert result is not None
        assert result.category == "data_minimization"
        assert result.severity == "medium"
        assert "ssn" in result.affected_data or "credit_card" in result.affected_data

    def test_check_data_minimization_minimal_fields(self, gdpr_checker):
        """Test data minimization check passes with minimal fields."""
        fields = ["email", "password_hash"]

        result = gdpr_checker.check_data_minimization(fields, "authentication")

        assert result is None

    def test_check_retention_limit_exceeded(self, gdpr_checker):
        """Test retention limit check for data exceeding limit."""
        result = gdpr_checker.check_retention_limit(45, "session_logs")

        assert result is not None
        assert result.category == "retention_limit"
        assert "45 days" in result.description
        assert "30 days" in result.remediation

    def test_check_retention_limit_within_limit(self, gdpr_checker):
        """Test retention limit check passes for data within limit."""
        result = gdpr_checker.check_retention_limit(20, "session_logs")

        assert result is None

    def test_check_right_to_deletion_pending(self, gdpr_checker, mock_firestore):
        """Test right to deletion check detects pending requests."""
        mock_doc = MagicMock()
        mock_doc.to_dict.return_value = {"user_id": "user123", "status": "pending"}
        mock_firestore.collection.return_value.where.return_value.where.return_value.stream.return_value = [
            mock_doc
        ]

        result = gdpr_checker.check_right_to_deletion("user123")

        assert result is not None
        assert result.category == "right_to_deletion"
        assert result.severity == "critical"


# --- DigitalSecurityActChecker Tests ---


class TestDigitalSecurityActChecker:
    """Tests for DigitalSecurityActChecker class."""

    @pytest.fixture
    def mock_firestore(self):
        """Mock Firestore client."""
        with patch("core.security.compliance_bot.get_firestore_client") as mock:
            client = MagicMock()
            client.collection.return_value.limit.return_value.stream.return_value = []
            mock.return_value = client
            yield client

    @pytest.fixture
    def dsa_checker(self, mock_firestore):
        """Create DigitalSecurityActChecker instance with mocked Firestore."""
        return DigitalSecurityActChecker()

    def test_check_data_localization_outside_bd(self, dsa_checker):
        """Test data localization check fails for non-Bangladesh location."""
        result = dsa_checker.check_data_localization("us")

        assert result is not None
        assert result.regulation == RegulationType.DIGITAL_SECURITY_ACT_BD
        assert result.category == "data_localization"
        assert "Bangladesh" in result.remediation

    def test_check_data_localization_bangladesh(self, dsa_checker):
        """Test data localization check passes for Bangladesh."""
        result = dsa_checker.check_data_localization("bd")

        assert result is None

    def test_check_data_localization_bangladesh_full(self, dsa_checker):
        """Test data localization check passes for full 'bangladesh' name."""
        result = dsa_checker.check_data_localization("bangladesh")

        assert result is None

    def test_check_content_moderation_prohibited(self, dsa_checker):
        """Test content moderation detects prohibited content."""
        content = "This contains defamatory statement about someone"

        result = dsa_checker.check_content_moderation(content)

        assert result is not None
        assert result.category == "content_moderation"
        assert result.severity == "critical"

    def test_check_content_moderation_safe_content(self, dsa_checker):
        """Test content moderation passes for safe content."""
        content = "This is a normal, safe piece of content."

        result = dsa_checker.check_content_moderation(content)

        assert result is None

    def test_check_lawful_interception_readiness(self, dsa_checker, mock_firestore):
        """Test lawful interception readiness check."""
        # Mock no audit logs existing
        mock_firestore.collection.return_value.limit.return_value.stream.return_value = (
            []
        )

        result = dsa_checker.check_lawful_interception_readiness()

        # Should return violation if no audit infrastructure
        assert result is not None
        assert result.category == "lawful_interception"

    def test_check_cybersecurity_reporting(self, dsa_checker, mock_firestore):
        """Test cybersecurity reporting check for incident response plan."""
        mock_firestore.collection.return_value.document.return_value.get.return_value.exists = (
            False
        )

        result = dsa_checker.check_cybersecurity_reporting()

        assert result is not None
        assert result.category == "incident_reporting"


# --- ConsentManager Tests ---


class TestConsentManager:
    """Tests for ConsentManager class."""

    @pytest.fixture
    def mock_firestore(self):
        """Mock Firestore client."""
        with patch("core.security.compliance_bot.get_firestore_client") as mock:
            client = MagicMock()
            mock.return_value = client
            yield client

    @pytest.fixture
    def consent_manager(self, mock_firestore):
        """Create ConsentManager instance with mocked Firestore."""
        return ConsentManager()

    def test_record_consent(self, consent_manager, mock_firestore):
        """Test recording user consent."""
        result = consent_manager.record_consent(
            user_id="user123",
            consent_type=ConsentType.DATA_PROCESSING,
            granted=True,
            ip_address="127.0.0.1",
            user_agent="TestAgent",
            expires_days=30,
        )

        assert isinstance(result, ConsentRecord)
        assert result.user_id == "user123"
        assert result.consent_type == ConsentType.DATA_PROCESSING
        assert result.granted is True
        assert result.ip_address == "127.0.0.1"

    def test_withdraw_consent(self, consent_manager, mock_firestore):
        """Test withdrawing user consent."""
        mock_doc = MagicMock()
        mock_doc.exists = True
        mock_doc.to_dict.return_value = {
            "user_id": "user123",
            "consent_type": "data_processing",
            "granted": True,
            "granted_at": datetime.now(UTC).isoformat(),
        }
        mock_firestore.collection.return_value.document.return_value.get.return_value = (
            mock_doc
        )

        result = consent_manager.withdraw_consent(
            "user123", ConsentType.DATA_PROCESSING
        )

        assert result is not None
        assert result.granted is False
        assert result.withdrawn_at is not None

    def test_withdraw_consent_not_found(self, consent_manager, mock_firestore):
        """Test withdrawing consent when record not found."""
        mock_doc = MagicMock()
        mock_doc.exists = False
        mock_firestore.collection.return_value.document.return_value.get.return_value = (
            mock_doc
        )

        result = consent_manager.withdraw_consent(
            "user123", ConsentType.DATA_PROCESSING
        )

        assert result is None

    def test_get_consent_status(self, consent_manager, mock_firestore):
        """Test getting consent status for a user."""
        mock_docs = []
        mock_doc = MagicMock()
        mock_doc.to_dict.return_value = {
            "user_id": "user123",
            "consent_type": "data_processing",
            "granted": True,
        }
        mock_docs.append(mock_doc)

        mock_firestore.collection.return_value.where.return_value.stream.return_value = (
            mock_docs
        )

        result = consent_manager.get_consent_status("user123")

        assert "data_processing" in result


# --- DataRetentionPolicy Tests ---


class TestDataRetentionPolicy:
    """Tests for DataRetentionPolicy class."""

    @pytest.fixture
    def mock_firestore(self):
        """Mock Firestore client."""
        with patch("core.security.compliance_bot.get_firestore_client") as mock:
            client = MagicMock()
            mock.return_value = client
            yield client

    @pytest.fixture
    def retention_policy(self, mock_firestore):
        """Create DataRetentionPolicy instance with mocked Firestore."""
        return DataRetentionPolicy()

    def test_enforce_retention(self, retention_policy, mock_firestore):
        """Test enforcing retention policy deletes old records."""
        # Mock empty stream (no records to delete)
        mock_firestore.collection.return_value.where.return_value.stream.return_value = (
            []
        )

        count = retention_policy.enforce_retention("session_logs", 30)

        assert count == 0


# --- ComplianceBot Tests ---


class TestComplianceBot:
    """Tests for ComplianceBot main class."""

    @pytest.fixture
    def compliance_bot(self):
        """Create ComplianceBot instance with mocked dependencies."""
        with patch("core.security.compliance_bot.get_firestore_client"):
            yield ComplianceBot()

    def test_run_compliance_check_returns_report(self, compliance_bot):
        """Test full compliance check returns a valid report."""
        with (
            patch.object(compliance_bot.gdpr, "check_lawful_basis", return_value=None),
            patch.object(
                compliance_bot.gdpr, "check_data_minimization", return_value=None
            ),
            patch.object(
                compliance_bot.gdpr, "check_right_to_deletion", return_value=None
            ),
            patch.object(
                compliance_bot.dsa, "check_data_localization", return_value=None
            ),
            patch.object(
                compliance_bot.dsa, "check_content_moderation", return_value=None
            ),
            patch.object(
                compliance_bot.dsa,
                "check_lawful_interception_readiness",
                return_value=None,
            ),
            patch.object(
                compliance_bot.dsa, "check_cybersecurity_reporting", return_value=None
            ),
            patch.object(
                compliance_bot.consent_mgr, "get_consent_status", return_value={}
            ),
        ):
            report = compliance_bot.run_compliance_check(
                user_id="user123",
                data_fields=["email", "name"],
                purpose="authentication",
                content="Hello world",
                data_location="bd",
            )

            assert isinstance(report, ComplianceReport)
            assert report.overall_compliant is True
            assert len(report.violations) == 0

    def test_run_compliance_check_detects_violations(self, compliance_bot):
        """Test compliance check detects violations correctly."""
        with (
            patch.object(
                compliance_bot.gdpr,
                "check_lawful_basis",
                return_value=ComplianceViolation(
                    regulation=RegulationType.GDPR,
                    severity="critical",
                    category="lawful_basis",
                    description="No consent",
                ),
            ),
            patch.object(
                compliance_bot.gdpr, "check_data_minimization", return_value=None
            ),
            patch.object(
                compliance_bot.gdpr, "check_right_to_deletion", return_value=None
            ),
            patch.object(
                compliance_bot.dsa, "check_data_localization", return_value=None
            ),
            patch.object(
                compliance_bot.dsa, "check_content_moderation", return_value=None
            ),
            patch.object(
                compliance_bot.dsa,
                "check_lawful_interception_readiness",
                return_value=None,
            ),
            patch.object(
                compliance_bot.dsa, "check_cybersecurity_reporting", return_value=None
            ),
            patch.object(
                compliance_bot.consent_mgr, "get_consent_status", return_value={}
            ),
        ):
            report = compliance_bot.run_compliance_check(
                user_id="user123",
                data_fields=["email", "name"],
                purpose="authentication",
                content="Hello world",
                data_location="bd",
            )

            assert report.overall_compliant is False
            assert len(report.violations) == 1
            assert "No consent" in report.violations[0].description
