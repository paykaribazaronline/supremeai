from unittest.mock import patch, MagicMock
from enum import Enum

from backend.core.enum_guard import check_enum_integrity, guard_enum, EnumMismatchError


# Define a test enum for use in tests
class TestEnum(Enum):
    VALUE1 = "value1"
    VALUE2 = "value2"


def test_check_enum_integrity_match():
    """Test when the database values match the enum values."""
    with patch("backend.core.enum_guard.get_db_connection") as mock_get_db, \
         patch("backend.core.enum_guard.logger") as mock_logger:
        # Mock the database connection and cursor
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_get_db.return_value = mock_conn
        mock_conn.cursor.return_value = mock_cursor
        # Simulate the database returning the same values as the enum
        mock_cursor.fetchall.return_value = [("value1",), ("value2",)]

        result = check_enum_integrity("test_table", TestEnum)

        assert result is True
        mock_logger.info.assert_called_once_with(
            "Enum integrity check passed for test_table"
        )
        mock_logger.warning.assert_not_called()


def test_check_enum_integrity_mismatch():
    """Test when the database values do not match the enum values."""
    with patch("backend.core.enum_guard.get_db_connection") as mock_get_db, \
         patch("backend.core.enum_guard.logger") as mock_logger:
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_get_db.return_value = mock_conn
        mock_conn.cursor.return_value = mock_cursor
        # Simulate the database returning different values
        mock_cursor.fetchall.return_value = [("value1",), ("value3",)]

        result = check_enum_integrity("test_table", TestEnum)

        assert result is False
        mock_logger.warning.assert_called_once()
        warning_msg = mock_logger.warning.call_args[0][0]
        assert "Enum integrity check failed for test_table" in warning_msg
        assert "Missing in DB: {'value2'}" in warning_msg
        assert "Extra in DB: {'value3'}" in warning_msg


def test_check_enum_integrity_empty_db():
    """Test when the database table is empty."""
    with patch("backend.core.enum_guard.get_db_connection") as mock_get_db, \
         patch("backend.core.enum_guard.logger") as mock_logger:
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_get_db.return_value = mock_conn
        mock_conn.cursor.return_value = mock_cursor
        mock_cursor.fetchall.return_value = []

        result = check_enum_integrity("test_table", TestEnum)

        assert result is False
        mock_logger.warning.assert_called_once()
        warning_msg = mock_logger.warning.call_args[0][0]
        assert "Enum integrity check failed for test_table" in warning_msg
        assert "Missing in DB: {'value1', 'value2'}" in warning_msg
        assert "Extra in DB: set()" in warning_msg


def test_check_enum_integrity_exception():
    """Test when an exception occurs during the database query."""
    with patch("backend.core.enum_guard.get_db_connection") as mock_get_db, \
         patch("backend.core.enum_guard.logger") as mock_logger:
        mock_get_db.side_effect = Exception("Database connection failed")

        result = check_enum_integrity("test_table", TestEnum)

        assert result is False
        mock_logger.error.assert_called_once()
        error_msg = mock_logger.error.call_args[0][0]
        assert "Error checking enum integrity for test_table" in error_msg
        assert "Database connection failed" in error_msg


def test_guard_enum_success():
    """Test guard_enum when the enum integrity check passes."""
    with patch("backend.core.enum_guard.check_enum_integrity") as mock_check, \
         patch("backend.core.enum_guard.logger") as mock_logger:
        mock_check.return_value = True

        # Should not raise an exception
        guard_enum("test_table", TestEnum)

        mock_check.assert_called_once_with("test_table", TestEnum)
        mock_logger.info.assert_not_called()  # guard_enum does not log on success
        mock_logger.warning.assert_not_called()


def test_guard_enum_failure():
    """Test guard_enum when the enum integrity check fails."""
    with patch("backend.core.enum_guard.check_enum_integrity") as mock_check, \
         patch("backend.core.enum_guard.logger") as mock_logger:
        mock_check.return_value = False

        try:
            guard_enum("test_table", TestEnum)
        except RuntimeError as e:
            assert str(e) == "Enum integrity check failed for test_table"
            mock_logger.error.assert_called_once_with(
                "Enum integrity check failed for test_table"
            )
        else:
            raise AssertionError("Expected RuntimeError was not raised")


def test_guard_enum_exception():
    """Test guard_enum when an unexpected exception occurs."""
    with patch("backend.core.enum_guard.check_enum_integrity") as mock_check, \
         patch("backend.core.enum_guard.logger") as mock_logger:
        mock_check.side_effect = Exception("Unexpected error")

        try:
            guard_enum("test_table", TestEnum)
        except RuntimeError as e:
            assert "Enum integrity check failed for test_table" in str(e)
            mock_logger.error.assert_called_once()
            error_msg = mock_logger.error.call_args[0][0]
            assert "Error during enum integrity check for test_table" in error_msg
            assert "Unexpected error" in error_msg
        else:
            raise AssertionError("Expected RuntimeError was not raised")
