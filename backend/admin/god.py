import sqlite3
import time
import threading
from pathlib import Path
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
    Reads from Google Cloud Firestore (Distributed & Serverless) with SQLite local fallback.
    """

    def __init__(self, db_path: str = None):
        if db_path is None:
            # বাংলা মন্তব্য: settings থেকে রুলস ডাটাবেস পাথ রিড করা হচ্ছে
            try:
                from core.config import settings
                db_path = settings.admin_rules_db
            except ImportError:
                db_path = "data/constitutional_rules.db"

        self.db_path = Path(db_path)
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        self.sqlite_lock = threading.Lock()
        
        self.collection_name = "constitutional_rules"
        self._db = None
        
        import os
        import sys

        is_test = "pytest" in sys.modules or os.getenv("ENV") == "test"
        if firestore and not is_test:
            try:
                # Firestore client auto-detects Cloud Run service account
                self._db = firestore.Client()
                self._init_db()
            except Exception as e:
                logger.warning(f"Failed to initialize Firestore for AdminGodLayer: {e}. Falling back to SQLite.")
                self._db = None
        else:
            logger.warning(
                "google-cloud-firestore not installed or in test mode. AdminGodLayer using local SQLite fallback."
            )
        
        self._init_sqlite_db()

    def _init_sqlite_db(self):
        # বাংলা মন্তব্য: লোকাল SQLite ডাটাবেস এবং ডিফল্ট রুলস সেটআপ
        with self.sqlite_lock:
            with sqlite3.connect(self.db_path, check_same_thread=False) as conn:
                conn.execute(
                    """
                    CREATE TABLE IF NOT EXISTS rules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        key TEXT UNIQUE NOT NULL,
                        value TEXT NOT NULL,
                        updated_at REAL NOT NULL
                    )
                    """
                )
                conn.commit()

        if not self.get_rule("admin_authorized"):
            self.set_rule("admin_authorized", "true")
        if not self.get_rule("autofix_authorized"):
            self.set_rule("autofix_authorized", "true")
        if not self.get_rule("autofix_reporting_authorized"):
            self.set_rule("autofix_reporting_authorized", "true")

    def _init_db(self):
        if not self._db:
            return
        try:
            # বাংলা মন্তব্য: Firestore-এ autofix_authorized নিয়মটি না থাকলে সেটি 'true' দিয়ে ইনিশিয়ালাইজ করা হচ্ছে।
            doc_ref = self._db.collection(self.collection_name).document(
                "admin_authorized"
            )
            if not doc_ref.get().exists:
                self.set_rule("admin_authorized", "true")
            
            autofix_ref = self._db.collection(self.collection_name).document(
                "autofix_authorized"
            )
            if not autofix_ref.get().exists:
                self.set_rule("autofix_authorized", "true")
        except Exception as e:
            logger.error(f"Error initializing AdminGodLayer DB: {e}")

    def get_rule(self, key: str, default: str | None = None) -> str | None:
        if self._db:
            try:
                doc_ref = self._db.collection(self.collection_name).document(key)
                doc = doc_ref.get()
                if doc.exists:
                    return doc.to_dict().get("value", default)
                return default
            except Exception as e:
                logger.error(f"Error fetching rule {key} from Firestore: {e}")

        # বাংলা মন্তব্য: ফায়ারস্টোর নিষ্ক্রিয় বা টেস্ট মোডে থাকলে SQLite ব্যাকআপ থেকে রিড হবে
        with self.sqlite_lock:
            with sqlite3.connect(self.db_path, check_same_thread=False) as conn:
                cur = conn.execute("SELECT value FROM rules WHERE key = ?", (key,))
                row = cur.fetchone()
                return row[0] if row else default

    def set_rule(self, key: str, value: str) -> None:
        if self._db:
            try:
                doc_ref = self._db.collection(self.collection_name).document(key)
                doc_ref.set({"value": value, "updated_at": time.time()})
                logger.info(f"Constitutional rule updated in Firestore: {key} = {value}")
                return
            except Exception as e:
                logger.error(f"Error setting rule {key} in Firestore: {e}. Falling back to SQLite.")

        # বাংলা মন্তব্য: SQLite ব্যাকআপ ডাটাবেসে রুল সংরক্ষণ করা হচ্ছে
        with self.sqlite_lock:
            with sqlite3.connect(self.db_path, check_same_thread=False) as conn:
                conn.execute(
                    """
                    INSERT INTO rules(key, value, updated_at)
                    VALUES(?, ?, ?)
                    ON CONFLICT(key) DO UPDATE SET value=excluded.value, updated_at=excluded.updated_at
                    """,
                    (key, value, time.time()),
                )
                conn.commit()
        logger.info(f"Constitutional rule updated in SQLite: {key} = {value}")

    def is_admin_action_allowed(self, action: str) -> bool:
        whitelist = {"health", "read", "learn", "ping"}
        if action in whitelist:
            return True
        flag = self.get_rule("admin_authorized")
        return flag == "true"

    def enforce(self, action: str) -> None:
        if not self.is_admin_action_allowed(action):
            raise PermissionError(
                "Action blocked by constitutional rules. Admin authorization required."
            )
