import json
import os
import tempfile
from unittest.mock import patch

from backend.core.knowledge_base import MEMORY_FILE_PATH, get_from_memory, save_to_memory


def test_get_from_memory_found():
    """Test retrieving an existing prompt from memory."""
    # Arrange: create a temporary memory file with known content
    with tempfile.NamedTemporaryFile(mode="w", delete=False, suffix=".json") as f:
        json.dump({"test prompt": "test solution"}, f)
        temp_path = f.name

    try:
        # Patch the MEMORY_FILE_PATH to point to our temporary file
        with patch("backend.core.knowledge_base.MEMORY_FILE_PATH", temp_path):
            result = get_from_memory("test prompt")
            assert result == "test solution"
    finally:
        os.unlink(temp_path)


def test_get_from_memory_not_found():
    """Test retrieving a non-existent prompt returns None."""
    # Arrange: create a temporary memory file with some content but not the key we're looking for
    with tempfile.NamedTemporaryFile(mode="w", delete=False, suffix=".json") as f:
        json.dump({"other prompt": "other solution"}, f)
        temp_path = f.name

    try:
        with patch("backend.core.knowledge_base.MEMORY_FILE_PATH", temp_path):
            result = get_from_memory("unknown prompt")
            assert result is None
    finally:
        os.unlink(temp_path)


def test_save_to_memory_new_entry():
    """Test saving a new prompt-solution pair to memory."""
    # Arrange: start with an empty memory file
    with tempfile.NamedTemporaryFile(mode="w", delete=False, suffix=".json") as f:
        json.dump({}, f)
        temp_path = f.name

    try:
        with patch("backend.core.knowledge_base.MEMORY_FILE_PATH", temp_path):
            # Act
            save_to_memory("new prompt", "new solution")

            # Assert: read the file and check the content
            with open(temp_path) as f:
                memory = json.load(f)
            assert memory == {"new prompt": "new solution"}
    finally:
        os.unlink(temp_path)


def test_save_to_memory_existing_entry():
    """Test saving a prompt-solution pair when the prompt already exists (should overwrite)."""
    # Arrange: start with a memory file containing an entry
    with tempfile.NamedTemporaryFile(mode="w", delete=False, suffix=".json") as f:
        json.dump({"existing prompt": "old solution"}, f)
        temp_path = f.name

    try:
        with patch("backend.core.knowledge_base.MEMORY_FILE_PATH", temp_path):
            # Act
            save_to_memory("existing prompt", "new solution")

            # Assert: read the file and check the content
            with open(temp_path) as f:
                memory = json.load(f)
            assert memory == {"existing prompt": "new solution"}
    finally:
        os.unlink(temp_path)
