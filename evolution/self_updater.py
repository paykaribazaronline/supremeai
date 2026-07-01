import os
from pathlib import Path
from loguru import logger

_ALLOWED_BASE_DIR = Path(__file__).resolve().parent.parent


class SelfUpdater:
    """
    Self-Updater module for SupremeAI 2.0.
    Applies patches, updates code modules, and keeps components up-to-date.
    """
    def __init__(self, authorized: bool = False):
        self.authorized = authorized

    def _validate_path(self, file_path: str) -> Path:
        target = Path(file_path).resolve()
        if not str(target).startswith(str(_ALLOWED_BASE_DIR)):
            raise ValueError(f"Hotfix target '{file_path}' is outside allowed project directory.")
        if not target.exists():
            raise ValueError(f"Target file does not exist: {file_path}")
        return target

    def apply_hotfix(self, file_path: str, new_content: str) -> bool:
        """Applies a hotpatch directly to an active file."""
        logger.info(f"Applying self-evolution hotfix to {file_path}")
        try:
            target = self._validate_path(file_path)
        except Exception as e:
            logger.error(f"Hotfix path validation failed: {e}")
            return False

        if not self.authorized:
            logger.error("Hotfix rejected: updater is not authorized.")
            return False
            
        try:
            # Backup original
            backup_path = target.with_suffix(target.suffix + ".bak")
            original = target.read_text(encoding="utf-8")
            backup_path.write_text(original, encoding="utf-8")
                
            # Write new version
            target.write_text(new_content, encoding="utf-8")
                
            logger.info("Hotfix successfully applied.")
            return True
        except Exception as e:
            logger.error(f"Failed to apply hotpatch: {e}")
            return False
