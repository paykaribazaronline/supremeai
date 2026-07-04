# 📄 ফাইল: admin/god.py

**প্রকার:** .py  
**সাইজ:** 6,702 বাইট  
**আপডেট:** 2026-07-04T04:31:35.614268

---

## কোড

```py
import sqlite3
import time
import threading
from pathlib import Path
from typing import Optional

from loguru import logger


class AdminGodLayer:
    """
    Constitutional enforcement layer.
    Every write action requires admin approval unless explicitly whitelisted.
    Reads from an encrypted (best-effort) SQLite DB.
    """

    def __init__(self, db_path: str):
        self.db_path = Path(db_path)
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        self.thread_local = threading.local()
        import os
        self.use_firestore = os.getenv("USE_FIRESTORE", "true").lower() == "true"
        if self.use_firestore:
            try:
                from google.cloud import firestore
                self.db = firestore.Client()
                logger.info("AdminGodLayer connected to Firestore")
            except Exception as e:
                logger.error(f"Firestore initialization failed: {e}. Falling back to SQLite.")
                self.use_firestore = False
        self._init_db()

    def _get_sqlite_conn(self):
        """
        বাংলা মন্তব্য: প্রতিটি থ্রেডের জন্য একটি স্বতন্ত্র SQLite কানেকশন তৈরি ও পরিচালনা করে।
        এটি 'database is locked' ত্রুটি এড়াতে সাহায্য করে।
        """
        if not hasattr(self.thread_local, "conn"):
            # busy_timeout যোগ করা হয়েছে যাতে ডেটাবেস লক থাকলে কোয়েরি কিছু সময় অপেক্ষা করে।
            self.thread_local.conn = sqlite3.connect(self.db_path, timeout=10)
        return self.thread_local.conn

    def _init_db(self):
        # ইনিশিয়ালাইজেশনের জন্য একটি অস্থায়ী কানেকশন ব্যবহার করা হচ্ছে।
        with sqlite3.connect(self.db_path) as conn:
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
            # বাংলা মন্তব্য: নিরাপত্তার জন্য প্রথমবার চালানোর সময় সকল অ্যাডমিন অথরাইজেশন ডিফল্টভাবে 'false' রাখা হচ্ছে এবং সতর্কতা লগ করা হচ্ছে।
            if not self.get_rule("admin_authorized"):
                self.set_rule("admin_authorized", "false")
                logger.warning("Defaulting 'admin_authorized' to 'false' for security. Please configure explicitly.")
            if not self.get_rule("autofix_authorized"):
                self.set_rule("autofix_authorized", "false")
                logger.warning("Defaulting 'autofix_authorized' to 'false' for security.")
            if not self.get_rule("autofix_reporting_authorized"):
                self.set_rule("autofix_reporting_authorized", "false")
                logger.warning("Defaulting 'autofix_reporting_authorized' to 'false' for security.")

    def get_rule(self, key: str, default: Optional[str] = None) -> Optional[str]:
        if self.use_firestore:
            try:
                doc = self.db.collection("admin_rules").document(key).get()
                if doc.exists:
                    return doc.to_dict().get("value", default)
            except Exception as e:
                logger.error(f"Firestore get_rule failed: {e}")
        
        # বাংলা মন্তব্য: থ্রেড-লোকাল কানেকশন ব্যবহার করে ডেটাবেস থেকে পড়া হচ্ছে।
        conn = self._get_sqlite_conn()
        cur = conn.execute("SELECT value FROM rules WHERE key = ?", (key,))
        row = cur.fetchone()
        return row[0] if row else default

    def set_rule(self, key: str, value: str) -> None:
        if self.use_firestore:
            try:
                self.db.collection("admin_rules").document(key).set({
                    "value": value,
                    "updated_at": time.time()
                })
                logger.info(f"Constitutional rule updated in Firestore: {key} = {value}")
                return
            except Exception as e:
                logger.error(f"Firestore set_rule failed: {e}. Falling back to SQLite.")

        # বাংলা মন্তব্য: থ্রেড-লোকাল কানেকশন ব্যবহার করে ডেটাবেসে লেখা হচ্ছে।
        conn = self._get_sqlite_conn()
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
        """
        Returns True if the action is allowed under current rules.
        Blocked actions require explicit admin_authorized flip,
        except whitelisted bootstrap keys.
        """
        whitelist = {"health", "read", "learn", "ping"}
        if action in whitelist:
            return True

        # বাংলা মন্তব্য: ডেস্ট্রাকটিভ অ্যাকশনের জন্য অতিরিক্ত যাচাই
        destructive_actions = {"delete", "drop", "truncate", "destroy", "remove"}
        if action in destructive_actions:
            flag = self.get_rule("admin_authorized")
            return flag == "true"

        flag = self.get_rule("admin_authorized")
        return flag == "true"

    def is_autofix_allowed(self) -> bool:
        """স্বয়ংক্রিয় ফিক্সিং অনুমোদিত কিনা চেক করে।"""
        flag = self.get_rule("autofix_authorized")
        return flag == "true"

    def enforce(self, action: str) -> None:
        if not self.is_admin_action_allowed(action):
            raise PermissionError(
                "Action blocked by constitutional rules. "
                "Admin authorization required."
            )

```