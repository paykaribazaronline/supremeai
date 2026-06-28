import logging
from typing import Any

from tenacity import retry
from tenacity import retry_if_exception_type
from tenacity import stop_after_attempt
from tenacity import wait_exponential


# Custom Exception for Circuit Breaking
class PrimaryDatabaseDownException(Exception):
    pass


class SmartDataRepository:
    def __init__(self, firebase_client: Any, supabase_client: Any):
        self.firebase = firebase_client
        self.supabase = supabase_client

    # Tier 1: Try Firebase 3 times with exponential backoff
    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=5),
        retry=retry_if_exception_type(PrimaryDatabaseDownException),
        reraise=True,
    )
    async def _fetch_from_primary(
        self, collection: str, doc_id: str
    ) -> dict[str, Any] | None:
        try:
            # Firebase Client check and fetch
            if hasattr(self.firebase, "collection"):
                doc_ref = self.firebase.collection(collection).document(doc_id)
                # Check if it has async get or normal get
                import inspect

                if inspect.iscoroutinefunction(doc_ref.get):
                    doc = await doc_ref.get()
                else:
                    doc = doc_ref.get()

                if not doc.exists:
                    return None
                return doc.to_dict()
            else:
                raise PrimaryDatabaseDownException(
                    "Firebase client not initialized or missing collection method"
                )
        except Exception as e:
            logging.warning(f"⚠️ Firebase unreachable ({str(e)}). Retrying...")
            raise PrimaryDatabaseDownException(str(e)) from e

    # Tier 2: Fallback to Supabase if primary database fails
    async def get_document_with_fallback(
        self, table_name: str, doc_id: str
    ) -> dict[str, Any] | None:
        try:
            # Try to fetch from Firebase
            return await self._fetch_from_primary(table_name, doc_id)
        except PrimaryDatabaseDownException:
            logging.critical(
                "🚨 FIREBASE IS DOWN! Circuit Breaker Tripped. Falling back to Supabase."
            )
            try:
                # If Supabase client has the execute API (standard Supabase-py)
                if hasattr(self.supabase, "table"):
                    response = (
                        self.supabase.table(table_name)
                        .select("*")
                        .eq("id", doc_id)
                        .execute()
                    )
                    return response.data[0] if response.data else None
                # If it's CloudPostgresStore helper
                elif hasattr(self.supabase, "_execute"):
                    query = f"SELECT * FROM {table_name} WHERE id = %s LIMIT 1"  # nosec B608
                    row = self.supabase._execute(query, (doc_id,), fetchone=True)
                    return dict(row) if row else None
                else:
                    logging.critical(
                        "Supabase client is not compatible or not initialized."
                    )
                    return None
            except Exception as backup_error:
                logging.critical(
                    f"💀 FATAL: Both databases are down! {str(backup_error)}"
                )
                return {"error": "Service degraded, please try again later."}
