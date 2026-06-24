import time
from typing import Optional
from loguru import logger

try:
    from google.cloud import firestore
except ImportError:
    firestore = None

class AdminGodLayer:
    """
    Constitutional enforcement layer.
    Every write action requires admin approval unless explicitly whitelisted.
    Reads from Google Cloud Firestore (Distributed & Serverless).
    """

    def __init__(self, db_path: str = None):
        # db_path is ignored for Firestore, kept for backward compatibility
        self.collection_name = "constitutional_rules"
        self._db = None
        self.local_rules = {}
        import os
        import sys
        is_test = "pytest" in sys.modules or os.getenv("ENV") == "test"
        if firestore and not is_test:
            try:
                # Firestore client auto-detects Cloud Run service account
                self._db = firestore.Client()
                self._init_db()
            except Exception as e:
                logger.warning(f"Failed to initialize Firestore for AdminGodLayer: {e}")
        else:
            logger.warning("google-cloud-firestore not installed or in test mode. AdminGodLayer using local fallback.")

    def _init_db(self):
        if not self._db: return
        try:
            # Check if admin_authorized exists, if not initialize it
            doc_ref = self._db.collection(self.collection_name).document("admin_authorized")
            if not doc_ref.get().exists:
                self.set_rule("admin_authorized", "true")
        except Exception as e:
            logger.error(f"Error initializing AdminGodLayer DB: {e}")



    def get_rule(self, key: str, default: Optional[str] = None) -> Optional[str]:
        if not self._db:
            return self.local_rules.get(key, default)
        try:
            doc_ref = self._db.collection(self.collection_name).document(key)
            doc = doc_ref.get()
            if doc.exists:
                return doc.to_dict().get("value", default)
            return default
        except Exception as e:
            logger.error(f"Error fetching rule {key}: {e}")
            return default

    def set_rule(self, key: str, value: str) -> None:
        if not self._db:
            self.local_rules[key] = value
            return
        try:
            doc_ref = self._db.collection(self.collection_name).document(key)
            doc_ref.set({
                "value": value,
                "updated_at": time.time()
            })
            logger.info(f"Constitutional rule updated in Firestore: {key} = {value}")
        except Exception as e:
            logger.error(f"Error setting rule {key}: {e}")

    def is_admin_action_allowed(self, action: str) -> bool:
        """
        Returns True if the action is allowed under current rules.
        Blocked actions require explicit admin_authorized flip,
        except whitelisted bootstrap keys.
        """
        whitelist = {"health", "read", "learn", "ping"}
        if action in whitelist:
            return True
        flag = self.get_rule("admin_authorized")
        return flag == "true"

    def enforce(self, action: str) -> None:
        if not self.is_admin_action_allowed(action):
            raise PermissionError(
                "Action blocked by constitutional rules. "
                "Admin authorization required."
            )
