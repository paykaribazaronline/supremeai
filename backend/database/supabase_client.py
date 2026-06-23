#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# ============================================================================
# file >> supabase_client.py
# project >> SupremeAI 2.0
# purpose >> Supabase integration
# module >> database
# ============================================================================
import os
from typing import Any, Optional
from loguru import logger
from supabase import create_client, Client

class SupabaseDB:
    """
    Supabase client wrapper for SupremeAI 2.0.
    Manages github_repos, system_config, and feature_flags.
    """

    def __init__(self):
        self.url = os.environ.get("SUPABASE_URL")
        self.key = os.environ.get("SUPABASE_KEY")
        self.client: Optional[Client] = None
        
        if self.url and self.key:
            try:
                self.client = create_client(self.url, self.key)
                logger.info("Initialized Supabase Client")
            except Exception as e:
                logger.error(f"Failed to initialize Supabase client: {e}")
        else:
            logger.warning("SUPABASE_URL or SUPABASE_KEY not found. Running in offline/mock mode.")

    # --- System Config ---
    def get_config(self, key: str) -> Optional[Any]:
        if not self.client: return None
        try:
            res = self.client.table("system_config").select("value").eq("key", key).execute()
            if res.data:
                return res.data[0].get("value")
            return None
        except Exception as e:
            logger.error(f"Failed to fetch config '{key}': {e}")
            return None

    def set_config(self, key: str, value: Any, category: str = "general"):
        if not self.client: return
        try:
            self.client.table("system_config").upsert({
                "key": key,
                "value": value,
                "category": category
            }).execute()
        except Exception as e:
            logger.error(f"Failed to set config '{key}': {e}")

    # --- Feature Flags ---
    def is_feature_enabled(self, feature_name: str, user_id: Optional[str] = None) -> bool:
        if not self.client: return False
        try:
            res = self.client.table("feature_flags").select("*").eq("feature_name", feature_name).execute()
            if res.data:
                flag = res.data[0]
                if not flag.get("enabled", False):
                    return False
                if user_id and flag.get("allowed_users"):
                    if user_id in flag["allowed_users"]:
                        return True
                # Real implementation would hash user_id against rollout_percentage here
                return True
            return False
        except Exception as e:
            logger.error(f"Failed to check feature flag '{feature_name}': {e}")
            return False

    # --- GitHub Repos ---
    def add_github_repo(self, repo_name: str, owner: str, description: str = "", language: str = ""):
        if not self.client: return
        try:
            self.client.table("github_repos").upsert({
                "repo_name": repo_name,
                "owner": owner,
                "description": description,
                "language": language
            }).execute()
        except Exception as e:
            logger.error(f"Failed to add GitHub repo '{repo_name}': {e}")

    # --- User Preferences ---
    def get_user_preferences(self, user_id: str) -> Optional[Any]:
        if not self.client: return None
        try:
            res = self.client.table("user_preferences").select("*").eq("user_id", user_id).execute()
            if res.data:
                return res.data[0]
            return None
        except Exception as e:
            logger.error(f"Failed to fetch preferences for '{user_id}': {e}")
            return None

    def upsert_user_preferences(self, data: dict) -> Optional[Any]:
        if not self.client: return None
        try:
            res = self.client.table("user_preferences").upsert(data).execute()
            return res.data[0] if res.data else None
        except Exception as e:
            logger.error(f"Failed to upsert preferences: {e}")
            return None

    # --- Usage Metrics ---
    def upsert_usage_metric(self, data: dict) -> Optional[Any]:
        if not self.client: return None
        try:
            res = self.client.table("usage_metrics").upsert(data).execute()
            return res.data[0] if res.data else None
        except Exception as e:
            logger.error(f"Failed to upsert usage metrics: {e}")
            return None

db = SupabaseDB()
