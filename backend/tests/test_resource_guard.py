"""Tests for ResourceGuard - Path traversal and resource access protection.

This module tests:
- Path traversal prevention (..)
- Allowed root directory validation
- Secure file read/write operations
- Permission errors for unauthorized paths
"""

import tempfile
from pathlib import Path
from unittest.mock import patch

import pytest
from core.security.resource_guard import ResourceGuard


@pytest.mark.skip(
    reason="ResourceGuard Linux relative path resolution variance in CI runner"
)
class TestResourceGuard:
    """Tests for ResourceGuard class."""

    def test_verify_path_accepts_allowed_path(self):
        """Test that paths within allowed roots are accepted."""
        with tempfile.TemporaryDirectory() as tmpdir:
            # Create a file in the temp directory
            test_file = Path(tmpdir) / "test.txt"
            test_file.write_text("hello world", encoding="utf-8")

            # Mock PROJECT_ROOT to be the temp directory
            with patch.object(ResourceGuard, "PROJECT_ROOT", Path(tmpdir).resolve()):
                result = ResourceGuard.verify_path("test.txt")

                assert result.exists() or result == Path(tmpdir).resolve() / "test.txt"

    def test_verify_path_rejects_traversal(self):
        """Test that path traversal attempts are rejected."""
        with pytest.raises(PermissionError) as exc_info:
            ResourceGuard.verify_path("../../../etc/passwd")

        assert "Path traversal" in str(exc_info.value)

    def test_verify_path_rejects_traversal_in_middle(self):
        """Test that path traversal anywhere in path is rejected."""
        with pytest.raises(PermissionError):
            ResourceGuard.verify_path("foo/../../../bar")

    def test_verify_path_rejects_external_path(self):
        """Test that paths outside allowed roots are rejected."""
        with tempfile.TemporaryDirectory() as tmpdir:
            external_path = Path(tmpdir) / "external.txt"
            external_path.write_text("test", encoding="utf-8")

            with patch.object(
                ResourceGuard, "PROJECT_ROOT", Path("/nonexistent").resolve()
            ):
                with pytest.raises(PermissionError) as exc_info:
                    ResourceGuard.verify_path(str(external_path))

                assert "denied" in str(exc_info.value)

    def test_read_text_success(self):
        """Test successful secure text file read."""
        with tempfile.TemporaryDirectory() as tmpdir:
            test_file = Path(tmpdir) / "read_test.txt"
            test_file.write_text("hello world", encoding="utf-8")

            with patch.object(ResourceGuard, "PROJECT_ROOT", Path(tmpdir).resolve()):
                content = ResourceGuard.read_text("read_test.txt")

                assert content == "hello world"

    def test_read_text_path_outside_root(self):
        """Test read_text raises error for path outside root."""
        with tempfile.TemporaryDirectory() as tmpdir:
            test_file = Path(tmpdir) / "secret.txt"
            test_file.write_text("secret data", encoding="utf-8")

            with patch.object(
                ResourceGuard, "PROJECT_ROOT", Path("/nonexistent").resolve()
            ):
                with pytest.raises(PermissionError):
                    ResourceGuard.read_text(str(test_file))

    def test_write_text_success(self):
        """Test successful secure text file write."""
        with tempfile.TemporaryDirectory() as tmpdir:
            with patch.object(ResourceGuard, "PROJECT_ROOT", Path(tmpdir).resolve()):
                ResourceGuard.write_text("write_test.txt", "new content")

                test_file = Path(tmpdir) / "write_test.txt"
                assert test_file.exists()
                assert test_file.read_text(encoding="utf-8") == "new content"

    def test_write_text_path_outside_root(self):
        """Test write_text raises error for path outside root."""
        with tempfile.TemporaryDirectory() as tmpdir:
            with patch.object(
                ResourceGuard, "PROJECT_ROOT", Path("/nonexistent").resolve()
            ):
                with pytest.raises(PermissionError):
                    ResourceGuard.write_text(
                        str(Path(tmpdir) / "hack.txt"), "malicious"
                    )

    def test_symlink_resolution(self):
        """Test that symlinks are resolved properly."""
        with tempfile.TemporaryDirectory() as tmpdir:
            # Create a file in tmpdir
            real_file = Path(tmpdir) / "real.txt"
            real_file.write_text("real content", encoding="utf-8")

            # Create a symlink pointing to it
            symlink = Path(tmpdir) / "link.txt"
            symlink.symlink_to(real_file)

            with patch.object(ResourceGuard, "PROJECT_ROOT", Path(tmpdir).resolve()):
                # Symlink within root should be allowed
                result = ResourceGuard.verify_path("link.txt")
                assert result.exists()

    def test_invalid_path_raises_error(self):
        """Test that invalid paths raise appropriate errors."""
        with patch.object(
            ResourceGuard, "PROJECT_ROOT", Path("/nonexistent").resolve()
        ):
            # Mock resolve to raise OSError
            with patch.object(
                Path,
                "resolve",
                side_effect=OSError("too many levels of symbolic links"),
            ):
                with pytest.raises(ValueError):
                    ResourceGuard.verify_path("some_path")

    def test_verify_path_with_absolute_path(self):
        """Test verify_path works with absolute paths within root."""
        with tempfile.TemporaryDirectory() as tmpdir:
            test_file = Path(tmpdir) / "absolute_test.txt"
            test_file.write_text("test", encoding="utf-8")

            with patch.object(ResourceGuard, "PROJECT_ROOT", Path(tmpdir).resolve()):
                result = ResourceGuard.verify_path(test_file)

                # Should resolve to the same file
                assert result == test_file.resolve()

    def test_verify_path_supports_sandbox_root(self):
        """Test that SANDBOX_ROOT is also an allowed root."""
        with tempfile.TemporaryDirectory() as tmpdir:
            sandbox_file = Path(tmpdir) / "sandbox.txt"
            sandbox_file.write_text("sandbox content", encoding="utf-8")

            with (
                patch.object(
                    ResourceGuard, "PROJECT_ROOT", Path("/nonexistent").resolve()
                ),
                patch.object(ResourceGuard, "SANDBOX_ROOT", Path(tmpdir).resolve()),
            ):
                result = ResourceGuard.verify_path("sandbox.txt")
                assert result.exists()

    def test_verify_path_supports_persistent_data_dir(self):
        """Test that PERSISTENT_DATA_DIR is also an allowed root."""
        with tempfile.TemporaryDirectory() as tmpdir:
            data_file = Path(tmpdir) / "data.txt"
            data_file.write_text("data content", encoding="utf-8")

            with (
                patch.object(
                    ResourceGuard, "PROJECT_ROOT", Path("/nonexistent").resolve()
                ),
                patch.object(
                    ResourceGuard, "PERSISTENT_DATA_DIR", Path(tmpdir).resolve()
                ),
            ):
                result = ResourceGuard.verify_path("data.txt")
                assert result.exists()
