import os
from typing import Any

import psycopg2
from loguru import logger
from supabase import Client
from supabase import create_client


class SupabaseDB:
    """
    Supabase client wrapper for SupremeAI 2.0.
    Manages github_repos, system_config, and feature_flags.
    """

    def __init__(self):
        self.url = os.environ.get("SUPABASE_URL") or self._derive_supabase_url(
            os.environ.get("SUPABASE_DATABASE_URL")
            or os.environ.get("SUPABASE_DATABASE_URL_POOLER")
        )
        self.key = os.environ.get("SUPABASE_KEY")
        self.client: Client | None = None

        if self.url and self.key:
            try:
                self.client = create_client(self.url, self.key)
                logger.info("Initialized Supabase Client")
            except Exception as e:
                logger.error(f"Failed to initialize Supabase client: {e}")
        else:
            logger.warning(
                "SUPABASE_URL or SUPABASE_KEY not found. Running in offline/mock mode."
            )

    @staticmethod
    def _derive_supabase_url(database_url: str | None) -> str | None:
        if not database_url:
            return None
        try:
            from urllib.parse import urlparse

            parsed = urlparse(database_url)
            hostname = parsed.hostname or ""
            if hostname.endswith(".supabase.co"):
                if hostname.startswith("db."):
                    return f"https://{hostname[3:]}"
                return f"https://{hostname}"
        except Exception:
            pass
        return None

    @classmethod
    def get_bootstrap_statements(cls) -> list[str]:
        return [
            "CREATE TABLE IF NOT EXISTS system_config ("
            "id SERIAL PRIMARY KEY,"
            "key TEXT NOT NULL UNIQUE,"
            "value TEXT,"
            "category TEXT,"
            "created_at TIMESTAMP WITH TIME ZONE NOT NULL,"
            "updated_at TIMESTAMP WITH TIME ZONE"
            ");",
            "CREATE TABLE IF NOT EXISTS skills ("
            "id UUID PRIMARY KEY DEFAULT gen_random_uuid(),"
            "name TEXT NOT NULL UNIQUE,"
            "category TEXT,"
            "prompt_template TEXT,"
            "parameters_schema JSONB,"
            "success_rate FLOAT DEFAULT 0.0,"
            "usage_count INTEGER DEFAULT 0,"
            "version TEXT DEFAULT '1.0.0',"
            "is_active BOOLEAN DEFAULT true,"
            "created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),"
            "updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),"
            "metadata JSONB DEFAULT '{}'"
            ");",
            "CREATE TABLE IF NOT EXISTS guardrails ("
            "id UUID PRIMARY KEY DEFAULT gen_random_uuid(),"
            "layer_name TEXT NOT NULL UNIQUE,"
            "rule_definition JSONB NOT NULL,"
            "priority INTEGER DEFAULT 0,"
            "is_active BOOLEAN DEFAULT true,"
            "created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),"
            "updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()"
            ");",
            "CREATE TABLE IF NOT EXISTS provider_configs ("
            "id UUID PRIMARY KEY DEFAULT gen_random_uuid(),"
            "provider_name TEXT NOT NULL UNIQUE,"
            "rpm INTEGER DEFAULT 999999,"
            "tpm INTEGER DEFAULT 999999,"
            "rpd INTEGER DEFAULT 999999,"
            "priority INTEGER DEFAULT 0,"
            "is_active BOOLEAN DEFAULT true,"
            "created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),"
            "updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()"
            ");",
            "CREATE TABLE IF NOT EXISTS feature_flags ("
            "id SERIAL PRIMARY KEY,"
            "feature_name TEXT NOT NULL UNIQUE,"
            "enabled BOOLEAN DEFAULT FALSE,"
            "allowed_users TEXT[],"
            "rollout_percentage INTEGER DEFAULT 100,"
            "created_at TIMESTAMP WITH TIME ZONE NOT NULL,"
            "updated_at TIMESTAMP WITH TIME ZONE"
            ");",
            "CREATE TABLE IF NOT EXISTS github_repos ("
            "id SERIAL PRIMARY KEY,"
            "repo_name TEXT NOT NULL,"
            "owner TEXT NOT NULL,"
            "description TEXT,"
            "language TEXT,"
            "created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()"
            ");",
            "CREATE TABLE IF NOT EXISTS ai_model_behavior ("
            "id SERIAL PRIMARY KEY,"
            "model_name TEXT NOT NULL UNIQUE,"
            "behavior JSONB,"
            "created_at TIMESTAMP WITH TIME ZONE NOT NULL,"
            "updated_at TIMESTAMP WITH TIME ZONE"
            ");",
            "CREATE TABLE IF NOT EXISTS user_preferences ("
            "id SERIAL PRIMARY KEY,"
            "user_id TEXT NOT NULL UNIQUE,"
            "preferences JSONB,"
            "created_at TIMESTAMP WITH TIME ZONE NOT NULL,"
            "updated_at TIMESTAMP WITH TIME ZONE"
            ");",
            "CREATE TABLE IF NOT EXISTS usage_metrics ("
            "id SERIAL PRIMARY KEY,"
            "tenant_id TEXT,"
            "metric_name TEXT NOT NULL,"
            "metric_value NUMERIC,"
            "collected_at TIMESTAMP WITH TIME ZONE NOT NULL"
            ");",
            "CREATE TABLE IF NOT EXISTS tenant_limits ("
            "id SERIAL PRIMARY KEY,"
            "tenant_id TEXT NOT NULL UNIQUE,"
            "org_name TEXT,"
            "billing_tier TEXT,"
            "requests_per_minute INTEGER,"
            "max_tokens_per_day BIGINT,"
            "max_concurrent_sessions INTEGER,"
            "stripe_customer_id TEXT,"
            "notes TEXT,"
            "created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),"
            "updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()"
            ");",
            "CREATE TABLE IF NOT EXISTS tenant_usage ("
            "id SERIAL PRIMARY KEY,"
            "tenant_id TEXT NOT NULL,"
            "date DATE NOT NULL,"
            "requests_count INTEGER DEFAULT 0,"
            "tokens_used BIGINT DEFAULT 0,"
            "cost_incurred NUMERIC DEFAULT 0.0,"
            "created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()"
            ");",
            "CREATE TABLE IF NOT EXISTS tools_registry ("
            "id TEXT PRIMARY KEY,"
            "name TEXT NOT NULL,"
            "file_path TEXT,"
            "category TEXT,"
            "dependencies TEXT[],"
            "cost_per_call NUMERIC DEFAULT 0.0,"
            "description TEXT,"
            "config_schema JSONB,"
            "status TEXT DEFAULT 'active',"
            "created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),"
            "updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()"
            ");",
            "CREATE TABLE IF NOT EXISTS markdown_exports ("
            "id SERIAL PRIMARY KEY,"
            "job_id TEXT NOT NULL UNIQUE,"
            "repo_url TEXT,"
            "time_range TEXT,"
            "status TEXT,"
            "timestamp NUMERIC,"
            "created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()"
            ");",
            "CREATE TABLE IF NOT EXISTS referral_codes ("
            "id SERIAL PRIMARY KEY,"
            "code TEXT NOT NULL UNIQUE,"
            "referrer_id TEXT NOT NULL,"
            "status TEXT DEFAULT 'active',"
            "created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),"
            "expires_at NUMERIC,"
            "redeemed_count INTEGER DEFAULT 0,"
            "fraud_score NUMERIC DEFAULT 0.0"
            ");",
            "CREATE TABLE IF NOT EXISTS referral_redemptions ("
            "id SERIAL PRIMARY KEY,"
            "code TEXT NOT NULL,"
            "new_user_id TEXT,"
            "referrer_id TEXT,"
            "reward_amount NUMERIC,"
            "credits_awarded INTEGER,"
            "metadata JSONB,"
            "created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()"
            ");",
            "CREATE TABLE IF NOT EXISTS credit_ledger ("
            "id SERIAL PRIMARY KEY,"
            "tx_id TEXT NOT NULL UNIQUE,"
            "user_id TEXT NOT NULL,"
            "amount NUMERIC NOT NULL,"
            "reason TEXT,"
            "timestamp NUMERIC,"
            "balance_after NUMERIC,"
            "created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()"
            ");",
            "CREATE TABLE IF NOT EXISTS credit_wallets ("
            "id SERIAL PRIMARY KEY,"
            "user_id TEXT NOT NULL UNIQUE,"
            "balance NUMERIC DEFAULT 0.0,"
            "updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()"
            ");",
            "CREATE TABLE IF NOT EXISTS domain_profiles ("
            "id SERIAL PRIMARY KEY,"
            "domain_name TEXT NOT NULL,"
            "profile JSONB,"
            "created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()"
            ");",
            "CREATE TABLE IF NOT EXISTS provider_benchmarks ("
            "id SERIAL PRIMARY KEY,"
            "provider_name TEXT NOT NULL,"
            "latency_ms INTEGER,"
            "cost NUMERIC,"
            "created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()"
            ");",
            "CREATE TABLE IF NOT EXISTS trading_portfolio (id SERIAL PRIMARY KEY,portfolio JSONB,updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW());",
            "CREATE TABLE IF NOT EXISTS conversations ("
            "id SERIAL PRIMARY KEY,"
            "session_id TEXT NOT NULL UNIQUE,"
            "messages JSONB,"
            "updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()"
            ");",
            "CREATE TABLE IF NOT EXISTS learned_facts ("
            "id TEXT PRIMARY KEY,"
            "content JSONB,"
            "tags JSONB,"
            "created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()"
            ");",
            "CREATE TABLE IF NOT EXISTS task_history ("
            "id SERIAL PRIMARY KEY,"
            "task TEXT NOT NULL,"
            "approach TEXT NOT NULL,"
            "result TEXT NOT NULL,"
            "success BOOLEAN NOT NULL,"
            "created_at TIMESTAMP WITH TIME ZONE NOT NULL"
            ");",
            "CREATE TABLE IF NOT EXISTS skill_proposals ("
            "id SERIAL PRIMARY KEY,"
            "skill_name TEXT NOT NULL,"
            "source_pattern TEXT,"
            "generated_code TEXT,"
            "status TEXT DEFAULT 'proposed',"
            "created_at TIMESTAMP WITH TIME ZONE NOT NULL,"
            "registered_at TIMESTAMP WITH TIME ZONE"
            ");",
            "CREATE TABLE IF NOT EXISTS feedback_loop ("
            "id SERIAL PRIMARY KEY,"
            "session_id TEXT NOT NULL,"
            "query TEXT,"
            "retrieved_chunks TEXT,"
            "user_rating REAL,"
            "adjusted BOOLEAN DEFAULT FALSE,"
            "created_at TIMESTAMP WITH TIME ZONE NOT NULL"
            ");",
            "CREATE TABLE IF NOT EXISTS evolution_logs (id SERIAL PRIMARY KEY,event JSONB NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL);",
        ]

    def bootstrap_schema(self):
        db_url = os.environ.get("SUPABASE_DATABASE_URL")
        pooler_url = os.environ.get("SUPABASE_DATABASE_URL_POOLER")
        if not db_url and not pooler_url:
            logger.error(
                "SUPABASE_DATABASE_URL or SUPABASE_DATABASE_URL_POOLER is required for schema bootstrap."
            )
            return

        statements = self.get_bootstrap_statements()

        tried_urls = []
        for candidate_url in (pooler_url, db_url):
            if not candidate_url:
                continue
            tried_urls.append(candidate_url)
            try:
                conn = psycopg2.connect(candidate_url)
                try:
                    cur = conn.cursor()
                    for statement in statements:
                        cur.execute(statement)
                    conn.commit()
                finally:
                    cur.close()
                    conn.close()
                logger.info(
                    "Supabase schema bootstrap completed using %s.",
                    (
                        "SUPABASE_DATABASE_URL_POOLER"
                        if candidate_url == pooler_url
                        else "SUPABASE_DATABASE_URL"
                    ),
                )
                return
            except Exception as e:
                logger.warning(
                    "Supabase schema bootstrap failed for %s: %s",
                    (
                        "SUPABASE_DATABASE_URL_POOLER"
                        if candidate_url == pooler_url
                        else "SUPABASE_DATABASE_URL"
                    ),
                    e,
                )

        logger.error(
            "Supabase schema bootstrap failed for all candidates: %s",
            ", ".join([u for u in tried_urls if u]),
        )

    def _is_schema_cache_error(self, error: Exception) -> bool:
        message = str(error) if error is not None else ""
        return (
            "Could not find the table" in message
            or "PGRST205" in message
            or "schema cache" in message.lower()
        )

    def _execute_response_with_retry(self, operation, fallback=None):
        try:
            response = operation()
            return getattr(response, "data", response)
        except Exception as e:
            if self._is_schema_cache_error(e):
                logger.warning(
                    "Supabase operation failed due missing table schema cache; bootstrapping schema and retrying: %s",
                    e,
                )
                self.bootstrap_schema()
                try:
                    response = operation()
                    return getattr(response, "data", response)
                except Exception as retry_error:
                    logger.error(
                        "Supabase retry after schema bootstrap failed: %s",
                        retry_error,
                    )
                    return fallback
            logger.debug(f"Supabase operation failed: {e}")
            return fallback

    # --- System Config ---
    def get_config(self, key: str) -> Any | None:
        if not self.client:
            return None
        try:
            res = (
                self.client.table("system_config")
                .select("value")
                .eq("key", key)
                .execute()
            )
            if res.data:
                return res.data[0].get("value")
            return None
        except Exception as e:
            logger.error(f"Failed to fetch config '{key}': {e}")
            return None

    def set_config(self, key: str, value: Any, category: str = "general"):
        if not self.client:
            return
        try:
            self.client.table("system_config").upsert(
                {"key": key, "value": value, "category": category}
            ).execute()
        except Exception as e:
            logger.error(f"Failed to set config '{key}': {e}")

    # --- Feature Flags ---
    def is_feature_enabled(self, feature_name: str, user_id: str | None = None) -> bool:
        if not self.client:
            return False
        try:
            res = (
                self.client.table("feature_flags")
                .select("*")
                .eq("feature_name", feature_name)
                .execute()
            )
            if res.data:
                flag = res.data[0]
                if not flag.get("enabled", False):
                    return False
                if (
                    user_id
                    and flag.get("allowed_users")
                    and user_id in flag["allowed_users"]
                ):
                    return True
                # Real implementation would hash user_id against rollout_percentage here
                return True
            return False
        except Exception as e:
            logger.error(f"Failed to check feature flag '{feature_name}': {e}")
            return False

    # --- GitHub Repos ---
    def add_github_repo(
        self, repo_name: str, owner: str, description: str = "", language: str = ""
    ):
        if not self.client:
            return
        try:
            self.client.table("github_repos").upsert(
                {
                    "repo_name": repo_name,
                    "owner": owner,
                    "description": description,
                    "language": language,
                }
            ).execute()
        except Exception as e:
            logger.error(f"Failed to add GitHub repo '{repo_name}': {e}")

    # --- AI Model Behavior ---
    def get_model_behavior(self, model_name: str) -> Any | None:
        if not self.client:
            return None
        try:
            res = (
                self.client.table("ai_model_behavior")
                .select("*")
                .eq("model_name", model_name)
                .single()
                .execute()
            )
            if res.data:
                return res.data
            return None
        except Exception as e:
            # It's okay if a model is not found, so we can log this at a debug level.
            logger.debug(f"Could not fetch AI model behavior for '{model_name}': {e}")
            return None

    def upsert_model_behavior(self, data: dict) -> Any | None:
        if not self.client:
            return None
        try:
            # Use upsert with on_conflict on 'model_name' if the table is set up for it.
            res = self.client.table("ai_model_behavior").upsert(data).execute()
            return res.data[0] if res.data else None
        except Exception as e:
            logger.error(f"Failed to upsert AI model behavior: {e}")
            return None

    # --- User Preferences ---
    def get_user_preferences(self, user_id: str) -> Any | None:
        if not self.client:
            return None
        try:
            res = (
                self.client.table("user_preferences")
                .select("*")
                .eq("user_id", user_id)
                .execute()
            )
            if res.data:
                return res.data[0]
            return None
        except Exception as e:
            logger.error(f"Failed to fetch preferences for '{user_id}': {e}")
            return None

    def upsert_user_preferences(self, data: dict) -> Any | None:
        if not self.client:
            return None
        try:
            res = self.client.table("user_preferences").upsert(data).execute()
            return res.data[0] if res.data else None
        except Exception as e:
            logger.error(f"Failed to upsert preferences: {e}")
            return None

    def get_configs_by_category(self, category: str) -> list[dict]:
        if not self.client:
            return []
        try:
            res = (
                self.client.table("system_config")
                .select("*")
                .eq("category", category)
                .execute()
            )
            return res.data or []
        except Exception as e:
            logger.error(f"Failed to fetch configs by category '{category}': {e}")
            return []

    # --- Evolution / Self-Evolution Persistence ---
    def insert_task_history(
        self,
        task: str,
        approach: str,
        result: str,
        success: bool,
        created_at: str,
    ) -> Any | None:
        if not self.client:
            return None
        entry = {
            "task": task,
            "approach": approach,
            "result": result,
            "success": success,
            "created_at": created_at,
        }
        res_data = self._execute_response_with_retry(
            lambda: self.client.table("task_history").insert(entry).execute(),
            fallback=None,
        )
        return res_data[0] if isinstance(res_data, list) and res_data else None

    def get_repeated_failures(self, min_occurrences: int = 3) -> list[dict[str, Any]]:
        if not self.client:
            return []
        rows = self._execute_response_with_retry(
            lambda: self.client.table("task_history")
            .select("*")
            .eq("success", False)
            .execute(),
            fallback=[],
        )
        rows = rows or []
        groups: dict[tuple[str, str], dict[str, Any]] = {}
        for row in rows:
            key = (row.get("task"), row.get("approach"))
            if key not in groups:
                groups[key] = {
                    "task": row.get("task"),
                    "approach": row.get("approach"),
                    "failures": 0,
                    "last_failed": row.get("created_at"),
                }
            groups[key]["failures"] += 1
            groups[key]["last_failed"] = max(
                groups[key]["last_failed"], row.get("created_at")
            )
        return [
            value for value in groups.values() if value["failures"] >= min_occurrences
        ]

    def insert_skill_proposal(
        self,
        skill_name: str,
        source_pattern: str,
        generated_code: str,
        status: str,
        created_at: str,
    ) -> Any | None:
        if not self.client:
            return None
        try:
            entry = {
                "skill_name": skill_name,
                "source_pattern": source_pattern,
                "generated_code": generated_code,
                "status": status,
                "created_at": created_at,
            }
            res = self.client.table("skill_proposals").insert(entry).execute()
            return res.data[0] if res.data else None
        except Exception as e:
            logger.debug(f"Supabase skill_proposals insert failed: {e}")
            return None

    def insert_feedback(
        self,
        session_id: str,
        query: str,
        retrieved_chunks: str,
        user_rating: float,
        created_at: str,
    ) -> Any | None:
        if not self.client:
            return None
        try:
            entry = {
                "session_id": session_id,
                "query": query,
                "retrieved_chunks": retrieved_chunks,
                "user_rating": user_rating,
                "created_at": created_at,
            }
            res = self.client.table("feedback_loop").insert(entry).execute()
            return res.data[0] if res.data else None
        except Exception as e:
            logger.debug(f"Supabase feedback_loop insert failed: {e}")
            return None

    def append_evolution_log(self, entry: dict[str, Any]) -> Any | None:
        if not self.client:
            return None
        try:
            res = self.client.table("evolution_logs").insert(entry).execute()
            return res.data[0] if res.data else None
        except Exception as e:
            logger.debug(f"Supabase evolution_logs insert failed: {e}")
            return None

    def get_evolution_logs(self, limit: int = 200) -> list[dict[str, Any]]:
        if not self.client:
            return []
        try:
            res = (
                self.client.table("evolution_logs")
                .select("*")
                .order("created_at", desc=True)
                .limit(limit)
                .execute()
            )
            return res.data or []
        except Exception as e:
            logger.debug(f"Supabase get_evolution_logs failed: {e}")
            return []

    # --- Usage Metrics ---
    def upsert_usage_metric(self, data: dict) -> Any | None:
        if not self.client:
            return None
        try:
            res = self.client.table("usage_metrics").upsert(data).execute()
            return res.data[0] if res.data else None
        except Exception as e:
            logger.error(f"Failed to upsert usage metrics: {e}")
            return None

    # --- Skills Registry DB integration ---
    def upsert_db_skill(self, data: dict) -> Any | None:
        if not self.client:
            return None
        try:
            res = self.client.table("skills").upsert(data).execute()
            return res.data[0] if res.data else None
        except Exception as e:
            logger.error(f"Failed to upsert skill into DB: {e}")
            return None

    def get_db_skill(self, name: str) -> Any | None:
        if not self.client:
            return None
        try:
            res = self.client.table("skills").select("*").eq("name", name).execute()
            return res.data[0] if res.data else None
        except Exception as e:
            logger.error(f"Failed to fetch skill '{name}' from DB: {e}")
            return None

    def get_all_db_skills(self) -> list[dict]:
        if not self.client:
            return []
        try:
            res = self.client.table("skills").select("*").execute()
            return res.data or []
        except Exception as e:
            logger.error(f"Failed to fetch all skills from DB: {e}")
            return []

    # --- Guardrails DB integration ---
    def upsert_db_guardrail(self, data: dict) -> Any | None:
        if not self.client:
            return None
        try:
            res = self.client.table("guardrails").upsert(data).execute()
            return res.data[0] if res.data else None
        except Exception as e:
            logger.error(f"Failed to upsert guardrail: {e}")
            return None

    def get_db_guardrails(self) -> list[dict]:
        if not self.client:
            return []
        try:
            res = (
                self.client.table("guardrails")
                .select("*")
                .eq("is_active", True)
                .order("priority", desc=False)
                .execute()
            )
            return res.data or []
        except Exception as e:
            logger.error(f"Failed to fetch active guardrails: {e}")
            return []

    # --- Provider Configs DB integration ---
    def upsert_db_provider_config(self, data: dict) -> Any | None:
        if not self.client:
            return None
        try:
            res = self.client.table("provider_configs").upsert(data).execute()
            return res.data[0] if res.data else None
        except Exception as e:
            logger.error(f"Failed to upsert provider config: {e}")
            return None

    def get_db_provider_configs(self) -> list[dict]:
        if not self.client:
            return []
        try:
            res = (
                self.client.table("provider_configs")
                .select("*")
                .eq("is_active", True)
                .order("priority", desc=False)
                .execute()
            )
            return res.data or []
        except Exception as e:
            logger.error(f"Failed to fetch active provider configs: {e}")
            return []


db = SupabaseDB()
