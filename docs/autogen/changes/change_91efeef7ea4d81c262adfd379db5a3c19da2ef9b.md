# 📋 Commit 91efeef7ea4d81c262adfd379db5a3c19da2ef9b

## Commit Stats
```
commit 91efeef7ea4d81c262adfd379db5a3c19da2ef9b
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sun Jul 5 21:17:31 2026 +0600

    style(backend): auto-fix ruff F823 local variable referenced before assignment

 backend/alembic/env.py                       | 11 ++------
 backend/api/routes/__init__.py               | 38 ++++++++++++++++++++++++++++
 backend/api/routes/execution_policies.py     |  3 ++-
 backend/api/routes/selector_healing.py       |  3 ++-
 backend/api/routes/session_stream.py         |  8 +++---
 backend/api/routes/session_takeover.py       |  9 ++++---
 backend/api/routes/site_actions.py           | 10 ++++----
 backend/core/enum_guard.py                   |  9 ++++---
 backend/core/log_batcher.py                  |  6 +----
 backend/core/secure_credential_store.py      | 12 +++++----
 backend/coverage.json                        |  2 +-
 backend/memory/long_term_memory.py           |  3 ++-
 backend/models/agent_session.py              | 10 +++++---
 backend/models/base.py                       |  7 -----
 backend/models/execution_log.py              | 16 ++++++++----
 backend/models/execution_policy.py           |  8 ++++--
 backend/models/handoff_event.py              | 10 +++++---
 backend/models/selector_healing_event.py     |  7 +++--
 backend/models/target_platform_credential.py | 11 +++++---
 backend/models/wallet.py                     |  2 +-
 backend/tools/api_gateway.py                 |  4 +--
 backend/tools/bangla_voice.py                |  8 +++---
 backend/tools/browser_agent.py               |  4 +--
 backend/tools/browser_stealth.py             |  4 +--
 backend/tools/cloud_sandbox_orchestrator.py  |  8 +++---
 backend/tools/code_smell_detector.py         |  4 +--
 backend/tools/comment_thread_ai.py           | 20 +++++++--------
 backend/tools/docker_sandbox.py              |  4 +--
 backend/tools/domain_adapter.py              |  4 +--
 backend/tools/gcp_cloud_functions.py         |  4 +--
 backend/tools/health_checker.py              |  4 +--
 backend/tools/knowledge_base_indexer.py      | 12 ++++-----
 backend/tools/local_search_rag.py            |  4 +--
 backend/tools/mcp_supabase.py                | 20 +++++++--------
 backend/tools/meta_architect.py              |  8 +++---
 backend/tools/multilingual_tts.py            |  4 +--
 backend/tools/parallel_agent_executor.py     |  8 +++---
 backend/tools/pdf_to_sdk.py                  |  8 +++---
 backend/tools/playwright_browser_agent.py    |  4 +--
 backend/tools/pr_reviewer.py                 |  4 +--
 backend/tools/pre_commit_ai.py               |  4 +--
 backend/tools/presentation_generator.py      |  4 +--
 backend/tools/repo_deep_indexer.py           |  8 +++---
 backend/tools/self_planner.py                |  4 +--
 backend/tools/skill_recommender.py           |  4 +--
 backend/tools/style_learner.py               | 12 ++++-----
 backend/tools/telegram_bot.py                |  8 +++---
 backend/tools/tenant_rate_limiter.py         |  4 +--
 backend/tools/viral_referral_engine.py       |  4 +--
 backend/tools/voice.py                       |  4 +--
 backend/tools/voice_coder.py                 |  4 +--
 backend/tools/vpn_switcher.py                |  4 +--
 52 files changed, 222 insertions(+), 167 deletions(-)

```

## Diff Detail
```diff
commit 91efeef7ea4d81c262adfd379db5a3c19da2ef9b
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sun Jul 5 21:17:31 2026 +0600

    style(backend): auto-fix ruff F823 local variable referenced before assignment

diff --git a/backend/alembic/env.py b/backend/alembic/env.py
index 1133f96a5..e68c0f954 100644
--- a/backend/alembic/env.py
+++ b/backend/alembic/env.py
@@ -11,17 +11,10 @@ from sqlalchemy import pool
 
 from alembic import context
 from core.config import settings
-
 from models.base import Base
+
+
 # Import all models to ensure they are registered with Base.metadata before autogenerate
-from models.wallet import UserWallet, TransactionLedgerEntry
-from models.evolution import SkillFitness, CodeProposal
-from models.agent_session import AgentSession
-from models.execution_log import ExecutionLog
-from models.execution_policy import ExecutionPolicy
-from models.target_platform_credential import TargetPlatformCredential
-from models.selector_healing_event import SelectorHealingEvent
-from models.handoff_event import HandoffEvent
 
 
 # this is the Alembic Config object, which provides
diff --git a/backend/api/routes/__init__.py b/backend/api/routes/__init__.py
index 39c54c018..5d93d064d 100644
--- a/backend/api/routes/__init__.py
+++ b/backend/api/routes/__init__.py
@@ -6,6 +6,7 @@ try:
     _safe_imports["approval_manager_router"] = approval_manager_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for approval_manager_router: {traceback.format_exc()}")
     approval_manager_router = None
@@ -16,6 +17,7 @@ try:
     _safe_imports["admin_dashboard_router"] = admin_dashboard_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for admin_dashboard_router: {traceback.format_exc()}")
     admin_dashboard_router = None
@@ -26,6 +28,7 @@ try:
     _safe_imports["agent_router"] = agent_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for agent_router: {traceback.format_exc()}")
     agent_router = None
@@ -36,6 +39,7 @@ try:
     _safe_imports["auth_router"] = auth_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for auth_router: {traceback.format_exc()}")
     auth_router = None
@@ -46,6 +50,7 @@ try:
     _safe_imports["async_task_router"] = async_task_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for async_task_router: {traceback.format_exc()}")
     async_task_router = None
@@ -56,6 +61,7 @@ try:
     _safe_imports["cdc_router"] = cdc_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for cdc_router: {traceback.format_exc()}")
     cdc_router = None
@@ -66,6 +72,7 @@ try:
     _safe_imports["browser_router"] = browser_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for browser_router: {traceback.format_exc()}")
     browser_router = None
@@ -76,6 +83,7 @@ try:
     _safe_imports["codeflow_router"] = codeflow_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for codeflow_router: {traceback.format_exc()}")
     codeflow_router = None
@@ -86,6 +94,7 @@ try:
     _safe_imports["feedback_router"] = feedback_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for feedback_router: {traceback.format_exc()}")
     feedback_router = None
@@ -96,6 +105,7 @@ try:
     _safe_imports["knowledge_router"] = knowledge_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for knowledge_router: {traceback.format_exc()}")
     knowledge_router = None
@@ -106,6 +116,7 @@ try:
     _safe_imports["marketplace_router"] = marketplace_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for marketplace_router: {traceback.format_exc()}")
     marketplace_router = None
@@ -116,6 +127,7 @@ try:
     _safe_imports["media_router"] = media_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for media_router: {traceback.format_exc()}")
     media_router = None
@@ -126,6 +138,7 @@ try:
     _safe_imports["memory_router"] = memory_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for memory_router: {traceback.format_exc()}")
     memory_router = None
@@ -136,6 +149,7 @@ try:
     _safe_imports["metrics_router"] = metrics_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for metrics_router: {traceback.format_exc()}")
     metrics_router = None
@@ -147,6 +161,7 @@ try:
     _safe_imports["site_actions_router"] = site_actions_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for site_actions_router: {traceback.format_exc()}")
     site_actions_router = None
@@ -158,6 +173,7 @@ try:
     _safe_imports["llm_gateway_router"] = llm_gateway_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for llm_gateway_router: {traceback.format_exc()}")
     llm_gateway_router = None
@@ -168,6 +184,7 @@ try:
     _safe_imports["simulator_router"] = simulator_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for simulator_router: {traceback.format_exc()}")
     simulator_router = None
@@ -178,6 +195,7 @@ try:
     _safe_imports["stream_router"] = stream_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for stream_router: {traceback.format_exc()}")
     stream_router = None
@@ -188,6 +206,7 @@ try:
     _safe_imports["task_router"] = task_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for task_router: {traceback.format_exc()}")
     task_router = None
@@ -198,6 +217,7 @@ try:
     _safe_imports["email_router"] = email_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for email_router: {traceback.format_exc()}")
     email_router = None
@@ -208,6 +228,7 @@ try:
     _safe_imports["github_router"] = github_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for github_router: {traceback.format_exc()}")
     github_router = None
@@ -218,6 +239,7 @@ try:
     _safe_imports["internal_router"] = internal_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for internal_router: {traceback.format_exc()}")
     internal_router = None
@@ -228,6 +250,7 @@ try:
     _safe_imports["config_router"] = config_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for config_router: {traceback.format_exc()}")
     config_router = None
@@ -238,6 +261,7 @@ try:
     _safe_imports["sso_router"] = sso_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for sso_router: {traceback.format_exc()}")
     sso_router = None
@@ -248,6 +272,7 @@ try:
     _safe_imports["repos_router"] = repos_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for repos_router: {traceback.format_exc()}")
     repos_router = None
@@ -258,6 +283,7 @@ try:
     _safe_imports["tools_ops_router"] = tools_ops_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for tools_ops_router: {traceback.format_exc()}")
     tools_ops_router = None
@@ -268,6 +294,7 @@ try:
     _safe_imports["voice_router"] = voice_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for voice_router: {traceback.format_exc()}")
     voice_router = None
@@ -278,6 +305,7 @@ try:
     _safe_imports["onboarding_router"] = onboarding_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for onboarding_router: {traceback.format_exc()}")
     onboarding_router = None
@@ -288,6 +316,7 @@ try:
     _safe_imports["tools_registry_router"] = tools_registry_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for tools_registry_router: {traceback.format_exc()}")
     tools_registry_router = None
@@ -298,6 +327,7 @@ try:
     _safe_imports["preferences_router"] = preferences_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for preferences_router: {traceback.format_exc()}")
     preferences_router = None
@@ -308,6 +338,7 @@ try:
     _safe_imports["usage_metrics_router"] = usage_metrics_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for usage_metrics_router: {traceback.format_exc()}")
     usage_metrics_router = None
@@ -318,6 +349,7 @@ try:
     _safe_imports["agents_router"] = agents_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for agents_router: {traceback.format_exc()}")
     agents_router = None
@@ -328,6 +360,7 @@ try:
     _safe_imports["payments_router"] = payments_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for payments_router: {traceback.format_exc()}")
     payments_router = None
@@ -338,6 +371,7 @@ try:
     _safe_imports["markdown_router"] = markdown_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for markdown_router: {traceback.format_exc()}")
     markdown_router = None
@@ -348,6 +382,7 @@ try:
     _safe_imports["api_keys_router"] = api_keys_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for api_keys_router: {traceback.format_exc()}")
     api_keys_router = None
@@ -358,6 +393,7 @@ try:
     _safe_imports["graph_router"] = graph_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for graph_router: {traceback.format_exc()}")
     graph_router = None
@@ -368,6 +404,7 @@ try:
     _safe_imports["ci_webhooks_router"] = ci_webhooks_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for ci_webhooks_router: {traceback.format_exc()}")
     ci_webhooks_router = None
@@ -377,6 +414,7 @@ try:
     _safe_imports["websocket_voice_router"] = websocket_voice_router
 except Exception:
     import traceback
+
     from loguru import logger
     logger.warning(f"Router import failed for websocket_voice_router: {traceback.format_exc()}")
     websocket_voice_router = None
diff --git a/backend/api/routes/execution_policies.py b/backend/api/routes/execution_policies.py
index e53dfc301..6b352d091 100644
--- a/backend/api/routes/execution_policies.py
+++ b/backend/api/routes/execution_policies.py
@@ -1,6 +1,7 @@
+
 from fastapi import APIRouter
 from pydantic import BaseModel
-from typing import List
+
 
 router = APIRouter(prefix="/api/admin/execution-policies", tags=["Guardrails"])
 
diff --git a/backend/api/routes/selector_healing.py b/backend/api/routes/selector_healing.py
index 69cd70b7f..3aa45c523 100644
--- a/backend/api/routes/selector_healing.py
+++ b/backend/api/routes/selector_healing.py
@@ -1,8 +1,9 @@
 import time
-from typing import List
+
 from fastapi import APIRouter
 from pydantic import BaseModel
 
+
 router = APIRouter(prefix="/api/admin/selector-healing", tags=["Self-Healing Logs"])
 
 class HealingEventOut(BaseModel):
diff --git a/backend/api/routes/session_stream.py b/backend/api/routes/session_stream.py
index aa2354451..421910c53 100644
--- a/backend/api/routes/session_stream.py
+++ b/backend/api/routes/session_stream.py
@@ -1,11 +1,13 @@
 import asyncio
 import json
 
-from fastapi import APIRouter, Depends, Path, Request
+from fastapi import APIRouter
+from fastapi import Path
+from fastapi import Request
 from sse_starlette.sse import EventSourceResponse
 
 from core.log_batcher import batcher
-from database.session import get_db_session
+
 
 router = APIRouter()
 
@@ -46,7 +48,7 @@ async def stream_session(
                         "event": "message",
                         "data": json.dumps({"channel": channel, "data": item})
                     }
-                except asyncio.TimeoutError:
+                except TimeoutError:
                     # Heartbeat
                     yield {
                         "event": "ping",
diff --git a/backend/api/routes/session_takeover.py b/backend/api/routes/session_takeover.py
index a638b7504..f5d963b7a 100644
--- a/backend/api/routes/session_takeover.py
+++ b/backend/api/routes/session_takeover.py
@@ -1,10 +1,11 @@
 import asyncio
-import base64
 
-from fastapi import APIRouter, Query, WebSocket, WebSocketDisconnect
+from fastapi import APIRouter
+from fastapi import Query
+from fastapi import WebSocket
+from fastapi import WebSocketDisconnect
 from loguru import logger
 
-from database.session import get_db_session
 
 router = APIRouter()
 
@@ -82,5 +83,5 @@ async def takeover_session_websocket(
         logger.error(f"WebSocket takeover error: {e}")
     finally:
         emitter_task.cancel()
-        if not websocket.client_state.name == "DISCONNECTED":
+        if websocket.client_state.name != "DISCONNECTED":
             await websocket.close()
diff --git a/backend/api/routes/site_actions.py b/backend/api/routes/site_actions.py
index a7c10ce8f..53c809017 100644
--- a/backend/api/routes/site_actions.py
+++ b/backend/api/routes/site_actions.py
@@ -1,14 +1,14 @@
+import json
 import os
 import sqlite3
 import threading
 import time
-import json
-import base64
-from typing import List
 
-from fastapi import APIRouter, HTTPException
+from fastapi import APIRouter
+from fastapi import HTTPException
 from pydantic import BaseModel
 
+
 router = APIRouter(prefix="/api/admin/site-actions", tags=["Site Actions Registry"])
 
 DB_PATH = os.getenv("SITE_ACTIONS_DB", "data/site_actions.db")
@@ -57,7 +57,7 @@ class SiteActionIn(BaseModel):
     action_type: str = "click"
     notes: str = ""
     enabled: bool = True
-    fallback_selectors: List[str] = []
+    fallback_selectors: list[str] = []
     selector_strategy: str = "exact"
     health_score: int = 100
 
diff --git a/backend/core/enum_guard.py b/backend/core/enum_guard.py
index 20992e4c7..17fe73631 100644
--- a/backend/core/enum_guard.py
+++ b/backend/core/enum_guard.py
@@ -1,5 +1,4 @@
 import enum
-from typing import Type
 
 from loguru import logger
 from sqlalchemy import text
@@ -10,7 +9,7 @@ from database.session import engine
 class EnumMismatchError(Exception):
     pass
 
-async def guard_enum(db_enum_name: str, py_enum: Type[enum.Enum]):
+async def guard_enum(db_enum_name: str, py_enum: type[enum.Enum]):
     """
     Validates that the Python Enum matches the Postgres Enum at startup.
     Prevents runtime crashes due to database mismatches.
@@ -51,10 +50,12 @@ async def guard_enum(db_enum_name: str, py_enum: Type[enum.Enum]):
 
 
 async def run_enum_guards():
-    from models.agent_session import AgentSessionState, ControlMode
+    from models.agent_session import AgentSessionState
+    from models.agent_session import ControlMode
     from models.execution_log import LogType
     from models.execution_policy import PolicyScope
-    from models.target_platform_credential import AuthType, CredentialStatus
+    from models.target_platform_credential import AuthType
+    from models.target_platform_credential import CredentialStatus
     
     logger.info("Running Startup Enum Guards...")
     
diff --git a/backend/core/log_batcher.py b/backend/core/log_batcher.py
index c00423e25..9c1566afe 100644
--- a/backend/core/log_batcher.py
+++ b/backend/core/log_batcher.py
@@ -1,9 +1,5 @@
 import asyncio
-import os
-import signal
 from collections import deque
-from datetime import datetime
-from typing import Any, Dict
 
 from loguru import logger
 from sqlalchemy import insert
@@ -84,7 +80,7 @@ class LogBatcherService:
                         
                 if len(self.buffer) >= self.batch_size:
                     await self._flush()
-            except asyncio.TimeoutError:
+            except TimeoutError:
                 if self.buffer:
                     await self._flush()
             except Exception as e:
diff --git a/backend/core/secure_credential_store.py b/backend/core/secure_credential_store.py
index b069ab5de..a9f1108b3 100644
--- a/backend/core/secure_credential_store.py
+++ b/backend/core/secure_credential_store.py
@@ -2,13 +2,15 @@ from __future__ import annotations
 
 import base64
 import os
-from abc import ABC, abstractmethod
-from typing import Any, Tuple
+from abc import ABC
+from abc import abstractmethod
+from typing import Any
 
 from loguru import logger
 
 from core.config import settings
 
+
 try:
     from cryptography.fernet import Fernet
     CRYPTO_AVAILABLE = True
@@ -24,7 +26,7 @@ def generate_key() -> str:
 
 class EncryptionProvider(ABC):
     @abstractmethod
-    def encrypt(self, plaintext: str) -> Tuple[str, str | None]:
+    def encrypt(self, plaintext: str) -> tuple[str, str | None]:
         """Returns (ciphertext, key_ref)"""
         pass
 
@@ -49,7 +51,7 @@ class LocalFernetProvider(EncryptionProvider):
         if not self.enabled:
             logger.warning("Credential encryption is disabled. Credentials will be stored as plaintext.")
 
-    def encrypt(self, plaintext: str) -> Tuple[str, str | None]:
+    def encrypt(self, plaintext: str) -> tuple[str, str | None]:
         if not self.enabled or self.fernet is None:
             return plaintext, "local:plaintext"
         try:
@@ -74,7 +76,7 @@ class CloudKMSProvider(EncryptionProvider):
         # In a real scenario, initialize GCP KMS Client or Supabase Vault Client here
         logger.info("Initializing CloudKMSProvider for envelope encryption.")
 
-    def encrypt(self, plaintext: str) -> Tuple[str, str | None]:
+    def encrypt(self, plaintext: str) -> tuple[str, str | None]:
         # STUB for Production Cloud KMS
         # Actually call the KMS API
         logger.debug("CloudKMSProvider: encrypting payload...")
diff --git a/backend/coverage.json b/backend/coverage.json
index c024c12c2..cbd705638 100644
--- a/backend/coverage.json
+++ b/backend/coverage.json
@@ -1 +1 @@
-{"meta": {"format": 3, "version": "7.14.1", "timestamp": "2026-07-05T02:23:25.901265", "branch_coverage": true, "show_contexts": false}, "files": {"core\\__init__.py": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 0, "percent_covered": 100.0, "percent_covered_display": "100", "missing_lines": 0, "excluded_lines": 0, "percent_statements_covered": 100.0, "percent_statements_covered_display": "100", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [], "excluded_lines": [], "executed_branches": [], "missing_branches": [], "functions": {"": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 0, "percent_covered": 100.0, "percent_covered_display": "100", "missing_lines": 0, "excluded_lines": 0, "percent_statements_covered": 100.0, "percent_statements_covered_display": "100", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}, "classes": {"": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 0, "percent_covered": 100.0, "percent_covered_display": "100", "missing_lines": 0, "excluded_lines": 0, "percent_statements_covered": 100.0, "percent_statements_covered_display": "100", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}}, "core\\config.py": {"executed_lines": [1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 13, 16, 21, 22, 27, 28, 30, 31, 32, 33, 34, 35, 37, 38, 39, 41, 51, 52, 53, 54, 56, 64, 66, 67, 68, 70, 73, 78, 83, 89, 93, 95, 96, 97, 98, 99, 100, 101, 102, 104, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 120, 121, 122, 123, 124, 125, 126, 128, 129, 131, 132, 133, 134, 135, 139, 140, 141, 142, 143, 145, 147, 148, 149, 151, 156, 158, 159, 160, 162, 167, 169, 170, 171, 172, 173, 174, 178, 181, 182, 183, 184, 185, 187, 189, 190, 191, 192, 194, 204, 205, 207, 209, 228, 230], "summary": {"covered_lines": 117, "num_statements": 169, "percent_covered": 59.36073059360731, "percent_covered_display": "59", "missing_lines": 52, "excluded_lines": 0, "percent_statements_covered": 69.23076923076923, "percent_statements_covered_display": "69", "num_branches": 50, "num_partial_branches": 13, "covered_branches": 13, "missing_branches": 37, "percent_branches_covered": 26.0, "percent_branches_covered_display": "26"}, "missing_lines": [17, 18, 57, 58, 59, 60, 61, 62, 63, 65, 69, 144, 152, 153, 154, 155, 163, 164, 165, 166, 175, 179, 186, 195, 196, 197, 199, 200, 201, 202, 206, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 231, 232, 234, 235, 236, 237, 238], "excluded_lines": [], "executed_branches": [[16, 21], [56, 64], [64, 66], [68, 70], [143, 145], [151, 156], [162, 167], [173, 174], [174, 178], [185, 187], [194, 204], [205, 207], [230, -1]], "missing_branches": [[16, 17], [56, 57], [58, 59], [58, 60], [64, 65], [68, 69], [143, 144], [151, 152], [153, 154], [153, 155], [162, 163], [164, 165], [164, 166], [173, 179], [174, 175], [185, 186], [194, 195], [196, 197], [196, 199], [205, 206], [210, -209], [210, 211], [212, 213], [212, 214], [214, 215], [214, 216], [216, 217], [216, 218], [218, 219], [218, 220], [220, 221], [220, 222], [222, -209], [222, 223], [230, 231], [234, -1], [234, 235]], "functions": {"Settings.sanitize_cors_origins": {"executed_lines": [54, 56, 64, 66, 67, 68, 70], "summary": {"covered_lines": 7, "num_statements": 16, "percent_covered": 41.666666666666664, "percent_covered_display": "42", "missing_lines": 9, "excluded_lines": 0, "percent_statements_covered": 43.75, "percent_statements_covered_display": "44", "num_branches": 8, "num_partial_branches": 3, "covered_branches": 3, "missing_branches": 5, "percent_branches_covered": 37.5, "percent_branches_covered_display": "38"}, "missing_lines": [57, 58, 59, 60, 61, 62, 63, 65, 69], "excluded_lines": [], "start_line": 53, "executed_branches": [[56, 64], [64, 66], [68, 70]], "missing_branches": [[56, 57], [58, 59], [58, 60], [64, 65], [68, 69]]}, "Settings.validate_env": {"executed_lines": [142, 143, 145], "summary": {"covered_lines": 3, "num_statements": 4, "percent_covered": 66.66666666666667, "percent_covered_display": "67", "missing_lines": 1, "excluded_lines": 0, "percent_statements_covered": 75.0, "percent_statements_covered_display": "75", "num_branches": 2, "num_partial_branches": 1, "covered_branches": 1, "missing_branches": 1, "percent_branches_covered": 50.0, "percent_branches_covered_display": "50"}, "missing_lines": [144], "excluded_lines": [], "start_line": 141, "executed_branches": [[143, 145]], "missing_branches": [[143, 144]]}, "Settings.parse_admin_emails": {"executed_lines": [151, 156], "summary": {"covered_lines": 2, "num_statements": 6, "percent_covered": 30.0, "percent_covered_display": "30", "missing_lines": 4, "excluded_lines": 0, "percent_statements_covered": 33.333333333333336, "percent_statements_covered_display": "33", "num_branches": 4, "num_partial_branches": 1, "covered_branches": 1, "missing_branches": 3, "percent_branches_covered": 25.0, "percent_branches_covered_display": "25"}, "missing_lines": [152, 153, 154, 155], "excluded_lines": [], "start_line": 149, "executed_branches": [[151, 156]], "missing_branches": [[151, 152], [153, 154], [153, 155]]}, "Settings.parse_allowed_hosts": {"executed_lines": [162, 167], "summary": {"covered_lines": 2, "num_statements": 6, "percent_covered": 30.0, "percent_covered_display": "30", "missing_lines": 4, "excluded_lines": 0, "percent_statements_covered": 33.333333333333336, "percent_statements_covered_display": "33", "num_branches": 4, "num_partial_branches": 1, "covered_branches": 1, "missing_branches": 3, "percent_branches_covered": 25.0, "percent_branches_covered_display": "25"}, "missing_lines": [163, 164, 165, 166], "excluded_lines": [], "start_line": 160, "executed_branches": [[162, 167]], "missing_branches": [[162, 163], [164, 165], [164, 166]]}, "Settings.set_test_secret": {"executed_lines": [172, 173, 174, 178], "summary": {"covered_lines": 4, "num_statements": 6, "percent_covered": 60.0, "percent_covered_display": "60", "missing_lines": 2, "excluded_lines": 0, "percent_statements_covered": 66.66666666666667, "percent_statements_covered_display": "67", "num_branches": 4, "num_partial_branches": 2, "covered_branches": 2, "missing_branches": 2, "percent_branches_covered": 50.0, "percent_branches_covered_display": "50"}, "missing_lines": [175, 179], "excluded_lines": [], "start_line": 171, "executed_branches": [[173, 174], [174, 178]], "missing_branches": [[173, 179], [174, 175]]}, "Settings.debug_must_be_false_in_production": {"executed_lines": [184, 185, 187], "summary": {"covered_lines": 3, "num_statements": 4, "percent_covered": 66.66666666666667, "percent_covered_display": "67", "missing_lines": 1, "excluded_lines": 0, "percent_statements_covered": 75.0, "percent_statements_covered_display": "75", "num_branches": 2, "num_partial_branches": 1, "covered_branches": 1, "missing_branches": 1, "percent_branches_covered": 50.0, "percent_branches_covered_display": "50"}, "missing_lines": [186], "excluded_lines": [], "start_line": 183, "executed_branches": [[185, 187]], "missing_branches": [[185, 186]]}, "Settings.parse_cors_origins": {"executed_lines": [192, 194, 204, 205, 207], "summary": {"covered_lines": 5, "num_statements": 13, "percent_covered": 36.8421052631579, "percent_covered_display": "37", "missing_lines": 8, "excluded_lines": 0, "percent_statements_covered": 38.46153846153846, "percent_statements_covered_display": "38", "num_branches": 6, "num_partial_branches": 2, "covered_branches": 2, "missing_branches": 4, "percent_branches_covered": 33.333333333333336, "percent_branches_covered_display": "33"}, "missing_lines": [195, 196, 197, 199, 200, 201, 202, 206], "excluded_lines": [], "start_line": 191, "executed_branches": [[194, 204], [205, 207]], "missing_branches": [[194, 195], [196, 197], [196, 199], [205, 206]]}, "Settings.validate_config": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 14, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 14, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 14, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 14, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223], "excluded_lines": [], "start_line": 209, "executed_branches": [], "missing_branches": [[210, -209], [210, 211], [212, 213], [212, 214], [214, 215], [214, 216], [216, 217], [216, 218], [218, 219], [218, 220], [220, 221], [220, 222], [222, -209], [222, 223]]}, "": {"executed_lines": [1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 13, 16, 21, 22, 27, 28, 30, 31, 32, 33, 34, 35, 37, 38, 39, 41, 51, 52, 53, 73, 78, 83, 89, 93, 95, 96, 97, 98, 99, 100, 101, 102, 104, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 120, 121, 122, 123, 124, 125, 126, 128, 129, 131, 132, 133, 134, 135, 139, 140, 141, 147, 148, 149, 158, 159, 160, 169, 170, 171, 181, 182, 183, 189, 190, 191, 209, 228, 230], "summary": {"covered_lines": 91, "num_statements": 100, "percent_covered": 87.73584905660377, "percent_covered_display": "88", "missing_lines": 9, "excluded_lines": 0, "percent_statements_covered": 91.0, "percent_statements_covered_display": "91", "num_branches": 6, "num_partial_branches": 2, "covered_branches": 2, "missing_branches": 4, "percent_branches_covered": 33.333333333333336, "percent_branches_covered_display": "33"}, "missing_lines": [17, 18, 231, 232, 234, 235, 236, 237, 238], "excluded_lines": [], "start_line": 1, "executed_branches": [[16, 21], [230, -1]], "missing_branches": [[16, 17], [230, 231], [234, -1], [234, 235]]}}, "classes": {"Settings": {"executed_lines": [54, 56, 64, 66, 67, 68, 70, 142, 143, 145, 151, 156, 162, 167, 172, 173, 174, 178, 184, 185, 187, 192, 194, 204, 205, 207], "summary": {"covered_lines": 26, "num_statements": 69, "percent_covered": 32.743362831858406, "percent_covered_display": "33", "missing_lines": 43, "excluded_lines": 0, "percent_statements_covered": 37.68115942028985, "percent_statements_covered_display": "38", "num_branches": 44, "num_partial_branches": 11, "covered_branches": 11, "missing_branches": 33, "percent_branches_covered": 25.0, "percent_branches_covered_display": "25"}, "missing_lines": [57, 58, 59, 60, 61, 62, 63, 65, 69, 144, 152, 153, 154, 155, 163, 164, 165, 166, 175, 179, 186, 195, 196, 197, 199, 200, 201, 202, 206, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223], "excluded_lines": [], "start_line": 21, "executed_branches": [[56, 64], [64, 66], [68, 70], [143, 145], [151, 156], [162, 167], [173, 174], [174, 178], [185, 187], [194, 204], [205, 207]], "missing_branches": [[56, 57], [58, 59], [58, 60], [64, 65], [68, 69], [143, 144], [151, 152], [153, 154], [153, 155], [162, 163], [164, 165], [164, 166], [173, 179], [174, 175], [185, 186], [194, 195], [196, 197], [196, 199], [205, 206], [210, -209], [210, 211], [212, 213], [212, 214], [214, 215], [214, 216], [216, 217], [216, 218], [218, 219], [218, 220], [220, 221], [220, 222], [222, -209], [222, 223]]}, "": {"executed_lines": [1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 13, 16, 21, 22, 27, 28, 30, 31, 32, 33, 34, 35, 37, 38, 39, 41, 51, 52, 53, 73, 78, 83, 89, 93, 95, 96, 97, 98, 99, 100, 101, 102, 104, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 120, 121, 122, 123, 124, 125, 126, 128, 129, 131, 132, 133, 134, 135, 139, 140, 141, 147, 148, 149, 158, 159, 160, 169, 170, 171, 181, 182, 183, 189, 190, 191, 209, 228, 230], "summary": {"covered_lines": 91, "num_statements": 100, "percent_covered": 87.73584905660377, "percent_covered_display": "88", "missing_lines": 9, "excluded_lines": 0, "percent_statements_covered": 91.0, "percent_statements_covered_display": "91", "num_branches": 6, "num_partial_branches": 2, "covered_branches": 2, "missing_branches": 4, "percent_branches_covered": 33.333333333333336, "percent_branches_covered_display": "33"}, "missing_lines": [17, 18, 231, 232, 234, 235, 236, 237, 238], "excluded_lines": [], "start_line": 1, "executed_branches": [[16, 21], [230, -1]], "missing_branches": [[16, 17], [230, 231], [234, -1], [234, 235]]}}}, "core\\enum_guard.py": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 43, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 43, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 10, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 10, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [1, 2, 4, 5, 7, 10, 11, 13, 18, 19, 20, 28, 30, 31, 32, 34, 36, 37, 38, 39, 40, 41, 42, 43, 44, 46, 47, 48, 49, 50, 53, 54, 55, 56, 57, 59, 61, 62, 63, 64, 65, 66, 68], "excluded_lines": [], "executed_branches": [], "missing_branches": [[30, 31], [30, 34], [36, 37], [36, 46], [40, 41], [40, 42], [42, 43], [42, 44], [48, 49], [48, 50]], "functions": {"guard_enum": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 22, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 22, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 10, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 10, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [18, 19, 20, 28, 30, 31, 32, 34, 36, 37, 38, 39, 40, 41, 42, 43, 44, 46, 47, 48, 49, 50], "excluded_lines": [], "start_line": 13, "executed_branches": [], "missing_branches": [[30, 31], [30, 34], [36, 37], [36, 46], [40, 41], [40, 42], [42, 43], [42, 44], [48, 49], [48, 50]]}, "run_enum_guards": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 12, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 12, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [54, 55, 56, 57, 59, 61, 62, 63, 64, 65, 66, 68], "excluded_lines": [], "start_line": 53, "executed_branches": [], "missing_branches": []}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 9, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 9, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [1, 2, 4, 5, 7, 10, 11, 13, 53], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}, "classes": {"EnumMismatchError": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 0, "percent_covered": 100.0, "percent_covered_display": "100", "missing_lines": 0, "excluded_lines": 0, "percent_statements_covered": 100.0, "percent_statements_covered_display": "100", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [], "excluded_lines": [], "start_line": 10, "executed_branches": [], "missing_branches": []}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 43, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 43, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 10, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 10, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [1, 2, 4, 5, 7, 10, 11, 13, 18, 19, 20, 28, 30, 31, 32, 34, 36, 37, 38, 39, 40, 41, 42, 43, 44, 46, 47, 48, 49, 50, 53, 54, 55, 56, 57, 59, 61, 62, 63, 64, 65, 66, 68], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": [[30, 31], [30, 34], [36, 37], [36, 46], [40, 41], [40, 42], [42, 43], [42, 44], [48, 49], [48, 50]]}}}, "core\\llm_gateway.py": {"executed_lines": [4, 5, 6, 7, 9, 10, 12, 16, 18, 19, 20, 21, 22, 25, 26, 28, 31, 32, 34, 35, 36, 37, 38, 45, 48, 56, 57, 58, 59, 61, 63, 80, 88, 89, 91, 180, 206], "summary": {"covered_lines": 37, "num_statements": 109, "percent_covered": 28.571428571428573, "percent_covered_display": "29", "missing_lines": 72, "excluded_lines": 0, "percent_statements_covered": 33.944954128440365, "percent_statements_covered_display": "34", "num_branches": 38, "num_partial_branches": 1, "covered_branches": 5, "missing_branches": 33, "percent_branches_covered": 13.157894736842104, "percent_branches_covered_display": "13"}, "missing_lines": [39, 40, 41, 43, 64, 65, 66, 67, 68, 70, 72, 73, 77, 78, 81, 82, 83, 105, 108, 109, 112, 113, 114, 115, 116, 118, 119, 120, 121, 125, 126, 127, 128, 136, 137, 140, 141, 142, 143, 144, 145, 148, 149, 151, 153, 154, 157, 158, 159, 160, 161, 167, 173, 174, 175, 176, 178, 183, 184, 185, 186, 187, 193, 194, 195, 196, 197, 198, 199, 200, 201, 203], "excluded_lines": [], "executed_branches": [[36, 37], [56, -45], [56, 57], [57, 56], [57, 58]], "missing_branches": [[36, 39], [108, 109], [108, 112], [113, 114], [113, 115], [115, 116], [115, 118], [118, 119], [118, 120], [120, 121], [120, 125], [125, 126], [125, 136], [127, 128], [127, 136], [141, 142], [141, 143], [143, 144], [143, 148], [144, 143], [144, 145], [148, 149], [148, 151], [153, 154], [153, 157], [158, 159], [158, 178], [184, 185], [184, 203], [193, 194], [193, 197], [195, 193], [195, 196]], "functions": {"LLMGateway.__init__": {"executed_lines": [20, 21, 22, 25, 26, 28, 31, 32], "summary": {"covered_lines": 8, "num_statements": 8, "percent_covered": 100.0, "percent_covered_display": "100", "missing_lines": 0, "excluded_lines": 0, "percent_statements_covered": 100.0, "percent_statements_covered_display": "100", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [], "excluded_lines": [], "start_line": 19, "executed_branches": [], "missing_branches": []}, "LLMGateway._load_routing_policy": {"executed_lines": [35, 36, 37, 38], "summary": {"covered_lines": 4, "num_statements": 8, "percent_covered": 50.0, "percent_covered_display": "50", "missing_lines": 4, "excluded_lines": 0, "percent_statements_covered": 50.0, "percent_statements_covered_display": "50", "num_branches": 2, "num_partial_branches": 1, "covered_branches": 1, "missing_branches": 1, "percent_branches_covered": 50.0, "percent_branches_covered_display": "50"}, "missing_lines": [39, 40, 41, 43], "excluded_lines": [], "start_line": 34, "executed_branches": [[36, 37]], "missing_branches": [[36, 39]]}, "LLMGateway._inject_secrets": {"executed_lines": [48, 56, 57, 58, 59], "summary": {"covered_lines": 5, "num_statements": 5, "percent_covered": 100.0, "percent_covered_display": "100", "missing_lines": 0, "excluded_lines": 0, "percent_statements_covered": 100.0, "percent_statements_covered_display": "100", "num_branches": 4, "num_partial_branches": 0, "covered_branches": 4, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [], "excluded_lines": [], "start_line": 45, "executed_branches": [[56, -45], [56, 57], [57, 56], [57, 58]], "missing_branches": []}, "LLMGateway._setup_callbacks": {"executed_lines": [63, 80, 88, 89], "summary": {"covered_lines": 4, "num_statements": 4, "percent_covered": 100.0, "percent_covered_display": "100", "missing_lines": 0, "excluded_lines": 0, "percent_statements_covered": 100.0, "percent_statements_covered_display": "100", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [], "excluded_lines": [], "start_line": 61, "executed_branches": [], "missing_branches": []}, "LLMGateway._setup_callbacks.success_callback": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 10, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 10, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [64, 65, 66, 67, 68, 70, 72, 73, 77, 78], "excluded_lines": [], "start_line": 63, "executed_branches": [], "missing_branches": []}, "LLMGateway._setup_callbacks.failure_callback": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 3, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 3, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [81, 82, 83], "excluded_lines": [], "start_line": 80, "executed_branches": [], "missing_branches": []}, "LLMGateway.acompletion": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 40, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 40, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 26, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 26, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [105, 108, 109, 112, 113, 114, 115, 116, 118, 119, 120, 121, 125, 126, 127, 128, 136, 137, 140, 141, 142, 143, 144, 145, 148, 149, 151, 153, 154, 157, 158, 159, 160, 161, 167, 173, 174, 175, 176, 178], "excluded_lines": [], "start_line": 91, "executed_branches": [], "missing_branches": [[108, 109], [108, 112], [113, 114], [113, 115], [115, 116], [115, 118], [118, 119], [118, 120], [120, 121], [120, 125], [125, 126], [125, 136], [127, 128], [127, 136], [141, 142], [141, 143], [143, 144], [143, 148], [144, 143], [144, 145], [148, 149], [148, 151], [153, 154], [153, 157], [158, 159], [158, 178]]}, "LLMGateway._stream_completion": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 15, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 15, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 6, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 6, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [183, 184, 185, 186, 187, 193, 194, 195, 196, 197, 198, 199, 200, 201, 203], "excluded_lines": [], "start_line": 180, "executed_branches": [], "missing_branches": [[184, 185], [184, 203], [193, 194], [193, 197], [195, 193], [195, 196]]}, "": {"executed_lines": [4, 5, 6, 7, 9, 10, 12, 16, 18, 19, 34, 45, 61, 91, 180, 206], "summary": {"covered_lines": 16, "num_statements": 16, "percent_covered": 100.0, "percent_covered_display": "100", "missing_lines": 0, "excluded_lines": 0, "percent_statements_covered": 100.0, "percent_statements_covered_display": "100", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}, "classes": {"LLMGateway": {"executed_lines": [20, 21, 22, 25, 26, 28, 31, 32, 35, 36, 37, 38, 48, 56, 57, 58, 59, 63, 80, 88, 89], "summary": {"covered_lines": 21, "num_statements": 93, "percent_covered": 19.84732824427481, "percent_covered_display": "20", "missing_lines": 72, "excluded_lines": 0, "percent_statements_covered": 22.580645161290324, "percent_statements_covered_display": "23", "num_branches": 38, "num_partial_branches": 1, "covered_branches": 5, "missing_branches": 33, "percent_branches_covered": 13.157894736842104, "percent_branches_covered_display": "13"}, "missing_lines": [39, 40, 41, 43, 64, 65, 66, 67, 68, 70, 72, 73, 77, 78, 81, 82, 83, 105, 108, 109, 112, 113, 114, 115, 116, 118, 119, 120, 121, 125, 126, 127, 128, 136, 137, 140, 141, 142, 143, 144, 145, 148, 149, 151, 153, 154, 157, 158, 159, 160, 161, 167, 173, 174, 175, 176, 178, 183, 184, 185, 186, 187, 193, 194, 195, 196, 197, 198, 199, 200, 201, 203], "excluded_lines": [], "start_line": 18, "executed_branches": [[36, 37], [56, -45], [56, 57], [57, 56], [57, 58]], "missing_branches": [[36, 39], [108, 109], [108, 112], [113, 114], [113, 115], [115, 116], [115, 118], [118, 119], [118, 120], [120, 121], [120, 125], [125, 126], [125, 136], [127, 128], [127, 136], [141, 142], [141, 143], [143, 144], [143, 148], [144, 143], [144, 145], [148, 149], [148, 151], [153, 154], [153, 157], [158, 159], [158, 178], [184, 185], [184, 203], [193, 194], [193, 197], [195, 193], [195, 196]]}, "": {"executed_lines": [4, 5, 6, 7, 9, 10, 12, 16, 18, 19, 34, 45, 61, 91, 180, 206], "summary": {"covered_lines": 16, "num_statements": 16, "percent_covered": 100.0, "percent_covered_display": "100", "missing_lines": 0, "excluded_lines": 0, "percent_statements_covered": 100.0, "percent_statements_covered_display": "100", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}}, "core\\log_batcher.py": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 89, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 89, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 28, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 28, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [1, 2, 3, 4, 5, 6, 8, 9, 11, 12, 15, 16, 17, 18, 19, 20, 21, 22, 23, 25, 26, 27, 28, 29, 30, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 43, 48, 51, 52, 53, 54, 56, 57, 58, 59, 60, 61, 63, 64, 65, 66, 67, 68, 69, 70, 72, 73, 74, 76, 77, 80, 81, 82, 83, 84, 85, 87, 88, 89, 90, 91, 92, 93, 95, 96, 97, 99, 100, 102, 104, 105, 109, 110, 111, 112, 113, 115, 116, 119], "excluded_lines": [], "executed_branches": [], "missing_branches": [[26, 27], [26, 28], [34, 35], [34, 40], [52, -43], [52, 53], [53, -43], [53, 54], [57, 58], [57, 59], [64, -63], [64, 65], [69, -63], [69, 70], [73, -72], [73, 74], [80, 81], [80, 87], [87, 73], [87, 88], [90, 73], [90, 91], [96, 97], [96, 99], [104, 105], [104, 111], [115, -95], [115, 116]], "functions": {"LogBatcherService.__init__": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 7, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 7, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [17, 18, 19, 20, 21, 22, 23], "excluded_lines": [], "start_line": 16, "executed_branches": [], "missing_branches": []}, "LogBatcherService.start": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 5, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 5, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 2, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 2, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [26, 27, 28, 29, 30], "excluded_lines": [], "start_line": 25, "executed_branches": [], "missing_branches": [[26, 27], [26, 28]]}, "LogBatcherService.stop": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 9, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 9, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 2, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 2, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [33, 34, 35, 36, 37, 38, 39, 40, 41], "excluded_lines": [], "start_line": 32, "executed_branches": [], "missing_branches": [[34, 35], [34, 40]]}, "LogBatcherService.emit": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 5, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 5, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 4, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 4, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [48, 51, 52, 53, 54], "excluded_lines": [], "start_line": 43, "executed_branches": [], "missing_branches": [[52, -43], [52, 53], [53, -43], [53, 54]]}, "LogBatcherService.subscribe": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 5, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 5, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 2, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 2, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [57, 58, 59, 60, 61], "excluded_lines": [], "start_line": 56, "executed_branches": [], "missing_branches": [[57, 58], [57, 59]]}, "LogBatcherService.unsubscribe": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 7, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 7, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 4, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 4, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [64, 65, 66, 67, 68, 69, 70], "excluded_lines": [], "start_line": 63, "executed_branches": [], "missing_branches": [[64, -63], [64, 65], [69, -63], [69, 70]]}, "LogBatcherService._run": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 17, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 17, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 8, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 8, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [73, 74, 76, 77, 80, 81, 82, 83, 84, 85, 87, 88, 89, 90, 91, 92, 93], "excluded_lines": [], "start_line": 72, "executed_branches": [], "missing_branches": [[73, -72], [73, 74], [80, 81], [80, 87], [87, 73], [87, 88], [90, 73], [90, 91]]}, "LogBatcherService._flush": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 14, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 14, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 6, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 6, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [96, 97, 99, 100, 102, 104, 105, 109, 110, 111, 112, 113, 115, 116], "excluded_lines": [], "start_line": 95, "executed_branches": [], "missing_branches": [[96, 97], [96, 99], [104, 105], [104, 111], [115, -95], [115, 116]]}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 20, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 20, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [1, 2, 3, 4, 5, 6, 8, 9, 11, 12, 15, 16, 25, 32, 43, 56, 63, 72, 95, 119], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}, "classes": {"LogBatcherService": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 69, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 69, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 28, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 28, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [17, 18, 19, 20, 21, 22, 23, 26, 27, 28, 29, 30, 33, 34, 35, 36, 37, 38, 39, 40, 41, 48, 51, 52, 53, 54, 57, 58, 59, 60, 61, 64, 65, 66, 67, 68, 69, 70, 73, 74, 76, 77, 80, 81, 82, 83, 84, 85, 87, 88, 89, 90, 91, 92, 93, 96, 97, 99, 100, 102, 104, 105, 109, 110, 111, 112, 113, 115, 116], "excluded_lines": [], "start_line": 15, "executed_branches": [], "missing_branches": [[26, 27], [26, 28], [34, 35], [34, 40], [52, -43], [52, 53], [53, -43], [53, 54], [57, 58], [57, 59], [64, -63], [64, 65], [69, -63], [69, 70], [73, -72], [73, 74], [80, 81], [80, 87], [87, 73], [87, 88], [90, 73], [90, 91], [96, 97], [96, 99], [104, 105], [104, 111], [115, -95], [115, 116]]}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 20, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 20, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [1, 2, 3, 4, 5, 6, 8, 9, 11, 12, 15, 16, 25, 32, 43, 56, 63, 72, 95, 119], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}}, "core\\swarm_orchestrator.py": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 19, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 19, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [4, 6, 7, 8, 9, 12, 16, 17, 18, 19, 21, 22, 23, 25, 28, 31, 34, 36, 37], "excluded_lines": [], "executed_branches": [], "missing_branches": [], "functions": {"SwarmOrchestrator.__init__": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 3, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 3, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [17, 18, 19], "excluded_lines": [], "start_line": 16, "executed_branches": [], "missing_branches": []}, "SwarmOrchestrator.execute_task": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 8, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 8, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [22, 23, 25, 28, 31, 34, 36, 37], "excluded_lines": [], "start_line": 21, "executed_branches": [], "missing_branches": []}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 8, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 8, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [4, 6, 7, 8, 9, 12, 16, 21], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}, "classes": {"SwarmOrchestrator": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 11, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 11, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [17, 18, 19, 22, 23, 25, 28, 31, 34, 36, 37], "excluded_lines": [], "start_line": 12, "executed_branches": [], "missing_branches": []}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 8, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 8, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [4, 6, 7, 8, 9, 12, 16, 21], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}}}, "totals": {"covered_lines": 154, "num_statements": 429, "percent_covered": 30.99099099099099, "percent_covered_display": "31", "missing_lines": 275, "excluded_lines": 0, "percent_statements_covered": 35.8974358974359, "percent_statements_covered_display": "36", "num_branches": 126, "num_partial_branches": 14, "covered_branches": 18, "missing_branches": 108, "percent_branches_covered": 14.285714285714286, "percent_branches_covered_display": "14"}}
\ No newline at end of file
+{"meta": {"format": 3, "version": "7.14.1", "timestamp": "2026-07-05T21:06:50.152829", "branch_coverage": true, "show_contexts": false}, "files": {"core\\__init__.py": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 0, "percent_covered": 100.0, "percent_covered_display": "100", "missing_lines": 0, "excluded_lines": 0, "percent_statements_covered": 100.0, "percent_statements_covered_display": "100", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [], "excluded_lines": [], "executed_branches": [], "missing_branches": [], "functions": {"": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 0, "percent_covered": 100.0, "percent_covered_display": "100", "missing_lines": 0, "excluded_lines": 0, "percent_statements_covered": 100.0, "percent_statements_covered_display": "100", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}, "classes": {"": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 0, "percent_covered": 100.0, "percent_covered_display": "100", "missing_lines": 0, "excluded_lines": 0, "percent_statements_covered": 100.0, "percent_statements_covered_display": "100", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}}, "core\\config.py": {"executed_lines": [1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 13, 16, 21, 22, 27, 28, 30, 31, 32, 33, 34, 35, 37, 38, 39, 41, 53, 58, 63, 69, 73, 75, 76, 77, 78, 79, 80, 81, 82, 83, 85, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 101, 102, 103, 104, 105, 106, 107, 109, 110, 112, 113, 114, 115, 116, 120, 121, 122, 123, 124, 125, 126, 128, 129, 130, 132, 133, 134, 135, 137, 139, 140, 141, 143, 144, 145, 146, 148, 150, 151, 152, 153, 154, 155, 159, 160, 162, 163, 164, 165, 166, 167, 168, 170, 171, 172, 173, 175, 185, 186, 187, 188, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 201, 202, 203, 204, 209, 211], "summary": {"covered_lines": 131, "num_statements": 151, "percent_covered": 80.82901554404145, "percent_covered_display": "81", "missing_lines": 20, "excluded_lines": 0, "percent_statements_covered": 86.75496688741723, "percent_statements_covered_display": "87", "num_branches": 42, "num_partial_branches": 13, "covered_branches": 25, "missing_branches": 17, "percent_branches_covered": 59.523809523809526, "percent_branches_covered_display": "60"}, "missing_lines": [17, 18, 136, 147, 156, 176, 177, 178, 180, 181, 182, 183, 200, 212, 213, 215, 216, 217, 218, 219], "excluded_lines": [], "executed_branches": [[16, 21], [124, 125], [124, 126], [132, 133], [132, 137], [134, 135], [143, 144], [143, 148], [145, 146], [154, 155], [154, 160], [155, 159], [166, 167], [166, 168], [175, 185], [186, 187], [186, 188], [191, 192], [193, 194], [195, 196], [197, 198], [199, 201], [201, 202], [203, 204], [211, -1]], "missing_branches": [[16, 17], [134, 136], [145, 147], [155, 156], [175, 176], [177, 178], [177, 180], [191, -190], [193, 195], [195, 197], [197, 199], [199, 200], [201, 203], [203, -190], [211, 212], [215, -1], [215, 216]], "functions": {"Settings.validate_env": {"executed_lines": [123, 124, 125, 126], "summary": {"covered_lines": 4, "num_statements": 4, "percent_covered": 100.0, "percent_covered_display": "100", "missing_lines": 0, "excluded_lines": 0, "percent_statements_covered": 100.0, "percent_statements_covered_display": "100", "num_branches": 2, "num_partial_branches": 0, "covered_branches": 2, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [], "excluded_lines": [], "start_line": 122, "executed_branches": [[124, 125], [124, 126]], "missing_branches": []}, "Settings.parse_admin_emails": {"executed_lines": [132, 133, 134, 135, 137], "summary": {"covered_lines": 5, "num_statements": 6, "percent_covered": 80.0, "percent_covered_display": "80", "missing_lines": 1, "excluded_lines": 0, "percent_statements_covered": 83.33333333333333, "percent_statements_covered_display": "83", "num_branches": 4, "num_partial_branches": 1, "covered_branches": 3, "missing_branches": 1, "percent_branches_covered": 75.0, "percent_branches_covered_display": "75"}, "missing_lines": [136], "excluded_lines": [], "start_line": 130, "executed_branches": [[132, 133], [132, 137], [134, 135]], "missing_branches": [[134, 136]]}, "Settings.parse_allowed_hosts": {"executed_lines": [143, 144, 145, 146, 148], "summary": {"covered_lines": 5, "num_statements": 6, "percent_covered": 80.0, "percent_covered_display": "80", "missing_lines": 1, "excluded_lines": 0, "percent_statements_covered": 83.33333333333333, "percent_statements_covered_display": "83", "num_branches": 4, "num_partial_branches": 1, "covered_branches": 3, "missing_branches": 1, "percent_branches_covered": 75.0, "percent_branches_covered_display": "75"}, "missing_lines": [147], "excluded_lines": [], "start_line": 141, "executed_branches": [[143, 144], [143, 148], [145, 146]], "missing_branches": [[145, 147]]}, "Settings.set_test_secret": {"executed_lines": [153, 154, 155, 159, 160], "summary": {"covered_lines": 5, "num_statements": 6, "percent_covered": 80.0, "percent_covered_display": "80", "missing_lines": 1, "excluded_lines": 0, "percent_statements_covered": 83.33333333333333, "percent_statements_covered_display": "83", "num_branches": 4, "num_partial_branches": 1, "covered_branches": 3, "missing_branches": 1, "percent_branches_covered": 75.0, "percent_branches_covered_display": "75"}, "missing_lines": [156], "excluded_lines": [], "start_line": 152, "executed_branches": [[154, 155], [154, 160], [155, 159]], "missing_branches": [[155, 156]]}, "Settings.debug_must_be_false_in_production": {"executed_lines": [165, 166, 167, 168], "summary": {"covered_lines": 4, "num_statements": 4, "percent_covered": 100.0, "percent_covered_display": "100", "missing_lines": 0, "excluded_lines": 0, "percent_statements_covered": 100.0, "percent_statements_covered_display": "100", "num_branches": 2, "num_partial_branches": 0, "covered_branches": 2, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [], "excluded_lines": [], "start_line": 164, "executed_branches": [[166, 167], [166, 168]], "missing_branches": []}, "Settings.parse_cors_origins": {"executed_lines": [173, 175, 185, 186, 187, 188], "summary": {"covered_lines": 6, "num_statements": 13, "percent_covered": 47.36842105263158, "percent_covered_display": "47", "missing_lines": 7, "excluded_lines": 0, "percent_statements_covered": 46.15384615384615, "percent_statements_covered_display": "46", "num_branches": 6, "num_partial_branches": 1, "covered_branches": 3, "missing_branches": 3, "percent_branches_covered": 50.0, "percent_branches_covered_display": "50"}, "missing_lines": [176, 177, 178, 180, 181, 182, 183], "excluded_lines": [], "start_line": 172, "executed_branches": [[175, 185], [186, 187], [186, 188]], "missing_branches": [[175, 176], [177, 178], [177, 180]]}, "Settings.validate_config": {"executed_lines": [191, 192, 193, 194, 195, 196, 197, 198, 199, 201, 202, 203, 204], "summary": {"covered_lines": 13, "num_statements": 14, "percent_covered": 71.42857142857143, "percent_covered_display": "71", "missing_lines": 1, "excluded_lines": 0, "percent_statements_covered": 92.85714285714286, "percent_statements_covered_display": "93", "num_branches": 14, "num_partial_branches": 7, "covered_branches": 7, "missing_branches": 7, "percent_branches_covered": 50.0, "percent_branches_covered_display": "50"}, "missing_lines": [200], "excluded_lines": [], "start_line": 190, "executed_branches": [[191, 192], [193, 194], [195, 196], [197, 198], [199, 201], [201, 202], [203, 204]], "missing_branches": [[191, -190], [193, 195], [195, 197], [197, 199], [199, 200], [201, 203], [203, -190]]}, "": {"executed_lines": [1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 13, 16, 21, 22, 27, 28, 30, 31, 32, 33, 34, 35, 37, 38, 39, 41, 53, 58, 63, 69, 73, 75, 76, 77, 78, 79, 80, 81, 82, 83, 85, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 101, 102, 103, 104, 105, 106, 107, 109, 110, 112, 113, 114, 115, 116, 120, 121, 122, 128, 129, 130, 139, 140, 141, 150, 151, 152, 162, 163, 164, 170, 171, 172, 190, 209, 211], "summary": {"covered_lines": 89, "num_statements": 98, "percent_covered": 87.5, "percent_covered_display": "88", "missing_lines": 9, "excluded_lines": 0, "percent_statements_covered": 90.81632653061224, "percent_statements_covered_display": "91", "num_branches": 6, "num_partial_branches": 2, "covered_branches": 2, "missing_branches": 4, "percent_branches_covered": 33.333333333333336, "percent_branches_covered_display": "33"}, "missing_lines": [17, 18, 212, 213, 215, 216, 217, 218, 219], "excluded_lines": [], "start_line": 1, "executed_branches": [[16, 21], [211, -1]], "missing_branches": [[16, 17], [211, 212], [215, -1], [215, 216]]}}, "classes": {"Settings": {"executed_lines": [123, 124, 125, 126, 132, 133, 134, 135, 137, 143, 144, 145, 146, 148, 153, 154, 155, 159, 160, 165, 166, 167, 168, 173, 175, 185, 186, 187, 188, 191, 192, 193, 194, 195, 196, 197, 198, 199, 201, 202, 203, 204], "summary": {"covered_lines": 42, "num_statements": 53, "percent_covered": 73.03370786516854, "percent_covered_display": "73", "missing_lines": 11, "excluded_lines": 0, "percent_statements_covered": 79.24528301886792, "percent_statements_covered_display": "79", "num_branches": 36, "num_partial_branches": 11, "covered_branches": 23, "missing_branches": 13, "percent_branches_covered": 63.888888888888886, "percent_branches_covered_display": "64"}, "missing_lines": [136, 147, 156, 176, 177, 178, 180, 181, 182, 183, 200], "excluded_lines": [], "start_line": 21, "executed_branches": [[124, 125], [124, 126], [132, 133], [132, 137], [134, 135], [143, 144], [143, 148], [145, 146], [154, 155], [154, 160], [155, 159], [166, 167], [166, 168], [175, 185], [186, 187], [186, 188], [191, 192], [193, 194], [195, 196], [197, 198], [199, 201], [201, 202], [203, 204]], "missing_branches": [[134, 136], [145, 147], [155, 156], [175, 176], [177, 178], [177, 180], [191, -190], [193, 195], [195, 197], [197, 199], [199, 200], [201, 203], [203, -190]]}, "": {"executed_lines": [1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 13, 16, 21, 22, 27, 28, 30, 31, 32, 33, 34, 35, 37, 38, 39, 41, 53, 58, 63, 69, 73, 75, 76, 77, 78, 79, 80, 81, 82, 83, 85, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 101, 102, 103, 104, 105, 106, 107, 109, 110, 112, 113, 114, 115, 116, 120, 121, 122, 128, 129, 130, 139, 140, 141, 150, 151, 152, 162, 163, 164, 170, 171, 172, 190, 209, 211], "summary": {"covered_lines": 89, "num_statements": 98, "percent_covered": 87.5, "percent_covered_display": "88", "missing_lines": 9, "excluded_lines": 0, "percent_statements_covered": 90.81632653061224, "percent_statements_covered_display": "91", "num_branches": 6, "num_partial_branches": 2, "covered_branches": 2, "missing_branches": 4, "percent_branches_covered": 33.333333333333336, "percent_branches_covered_display": "33"}, "missing_lines": [17, 18, 212, 213, 215, 216, 217, 218, 219], "excluded_lines": [], "start_line": 1, "executed_branches": [[16, 21], [211, -1]], "missing_branches": [[16, 17], [211, 212], [215, -1], [215, 216]]}}}, "core\\enum_guard.py": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 43, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 43, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 10, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 10, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [1, 2, 4, 5, 7, 10, 11, 13, 18, 19, 20, 28, 30, 31, 32, 34, 36, 37, 38, 39, 40, 41, 42, 43, 44, 46, 47, 48, 49, 50, 53, 54, 55, 56, 57, 59, 61, 62, 63, 64, 65, 66, 68], "excluded_lines": [], "executed_branches": [], "missing_branches": [[30, 31], [30, 34], [36, 37], [36, 46], [40, 41], [40, 42], [42, 43], [42, 44], [48, 49], [48, 50]], "functions": {"guard_enum": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 22, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 22, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 10, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 10, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [18, 19, 20, 28, 30, 31, 32, 34, 36, 37, 38, 39, 40, 41, 42, 43, 44, 46, 47, 48, 49, 50], "excluded_lines": [], "start_line": 13, "executed_branches": [], "missing_branches": [[30, 31], [30, 34], [36, 37], [36, 46], [40, 41], [40, 42], [42, 43], [42, 44], [48, 49], [48, 50]]}, "run_enum_guards": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 12, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 12, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [54, 55, 56, 57, 59, 61, 62, 63, 64, 65, 66, 68], "excluded_lines": [], "start_line": 53, "executed_branches": [], "missing_branches": []}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 9, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 9, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [1, 2, 4, 5, 7, 10, 11, 13, 53], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}, "classes": {"EnumMismatchError": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 0, "percent_covered": 100.0, "percent_covered_display": "100", "missing_lines": 0, "excluded_lines": 0, "percent_statements_covered": 100.0, "percent_statements_covered_display": "100", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [], "excluded_lines": [], "start_line": 10, "executed_branches": [], "missing_branches": []}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 43, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 43, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 10, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 10, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [1, 2, 4, 5, 7, 10, 11, 13, 18, 19, 20, 28, 30, 31, 32, 34, 36, 37, 38, 39, 40, 41, 42, 43, 44, 46, 47, 48, 49, 50, 53, 54, 55, 56, 57, 59, 61, 62, 63, 64, 65, 66, 68], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": [[30, 31], [30, 34], [36, 37], [36, 46], [40, 41], [40, 42], [42, 43], [42, 44], [48, 49], [48, 50]]}}}, "core\\llm_gateway.py": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 114, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 114, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 40, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 40, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [4, 5, 6, 7, 9, 10, 12, 16, 18, 19, 20, 21, 22, 25, 26, 28, 31, 32, 34, 35, 36, 37, 38, 39, 40, 41, 43, 45, 48, 56, 57, 58, 59, 61, 63, 64, 65, 66, 67, 68, 70, 72, 73, 77, 78, 80, 81, 82, 83, 88, 89, 91, 106, 109, 110, 113, 114, 115, 116, 117, 119, 120, 121, 122, 126, 127, 128, 129, 137, 138, 141, 142, 143, 145, 146, 147, 148, 149, 151, 152, 153, 156, 157, 159, 161, 162, 165, 166, 167, 168, 169, 175, 181, 182, 183, 184, 186, 188, 191, 192, 193, 194, 195, 201, 202, 203, 204, 205, 206, 207, 208, 209, 211, 214], "excluded_lines": [], "executed_branches": [], "missing_branches": [[36, 37], [36, 39], [56, -45], [56, 57], [57, 56], [57, 58], [109, 110], [109, 113], [114, 115], [114, 116], [116, 117], [116, 119], [119, 120], [119, 121], [121, 122], [121, 126], [126, 127], [126, 137], [128, 129], [128, 137], [142, 143], [142, 145], [146, 147], [146, 151], [151, 152], [151, 156], [152, 151], [152, 153], [156, 157], [156, 159], [161, 162], [161, 165], [166, 167], [166, 186], [192, 193], [192, 211], [201, 202], [201, 205], [203, 201], [203, 204]], "functions": {"LLMGateway.__init__": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 8, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 8, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [20, 21, 22, 25, 26, 28, 31, 32], "excluded_lines": [], "start_line": 19, "executed_branches": [], "missing_branches": []}, "LLMGateway._load_routing_policy": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 8, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 8, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 2, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 2, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [35, 36, 37, 38, 39, 40, 41, 43], "excluded_lines": [], "start_line": 34, "executed_branches": [], "missing_branches": [[36, 37], [36, 39]]}, "LLMGateway._inject_secrets": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 5, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 5, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 4, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 4, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [48, 56, 57, 58, 59], "excluded_lines": [], "start_line": 45, "executed_branches": [], "missing_branches": [[56, -45], [56, 57], [57, 56], [57, 58]]}, "LLMGateway._setup_callbacks": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 4, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 4, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [63, 80, 88, 89], "excluded_lines": [], "start_line": 61, "executed_branches": [], "missing_branches": []}, "LLMGateway._setup_callbacks.success_callback": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 10, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 10, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [64, 65, 66, 67, 68, 70, 72, 73, 77, 78], "excluded_lines": [], "start_line": 63, "executed_branches": [], "missing_branches": []}, "LLMGateway._setup_callbacks.failure_callback": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 3, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 3, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [81, 82, 83], "excluded_lines": [], "start_line": 80, "executed_branches": [], "missing_branches": []}, "LLMGateway.acompletion": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 45, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 45, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 28, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 28, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [106, 109, 110, 113, 114, 115, 116, 117, 119, 120, 121, 122, 126, 127, 128, 129, 137, 138, 141, 142, 143, 145, 146, 147, 148, 149, 151, 152, 153, 156, 157, 159, 161, 162, 165, 166, 167, 168, 169, 175, 181, 182, 183, 184, 186], "excluded_lines": [], "start_line": 91, "executed_branches": [], "missing_branches": [[109, 110], [109, 113], [114, 115], [114, 116], [116, 117], [116, 119], [119, 120], [119, 121], [121, 122], [121, 126], [126, 127], [126, 137], [128, 129], [128, 137], [142, 143], [142, 145], [146, 147], [146, 151], [151, 152], [151, 156], [152, 151], [152, 153], [156, 157], [156, 159], [161, 162], [161, 165], [166, 167], [166, 186]]}, "LLMGateway._stream_completion": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 15, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 15, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 6, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 6, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [191, 192, 193, 194, 195, 201, 202, 203, 204, 205, 206, 207, 208, 209, 211], "excluded_lines": [], "start_line": 188, "executed_branches": [], "missing_branches": [[192, 193], [192, 211], [201, 202], [201, 205], [203, 201], [203, 204]]}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 16, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 16, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [4, 5, 6, 7, 9, 10, 12, 16, 18, 19, 34, 45, 61, 91, 188, 214], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}, "classes": {"LLMGateway": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 98, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 98, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 40, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 40, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [20, 21, 22, 25, 26, 28, 31, 32, 35, 36, 37, 38, 39, 40, 41, 43, 48, 56, 57, 58, 59, 63, 64, 65, 66, 67, 68, 70, 72, 73, 77, 78, 80, 81, 82, 83, 88, 89, 106, 109, 110, 113, 114, 115, 116, 117, 119, 120, 121, 122, 126, 127, 128, 129, 137, 138, 141, 142, 143, 145, 146, 147, 148, 149, 151, 152, 153, 156, 157, 159, 161, 162, 165, 166, 167, 168, 169, 175, 181, 182, 183, 184, 186, 191, 192, 193, 194, 195, 201, 202, 203, 204, 205, 206, 207, 208, 209, 211], "excluded_lines": [], "start_line": 18, "executed_branches": [], "missing_branches": [[36, 37], [36, 39], [56, -45], [56, 57], [57, 56], [57, 58], [109, 110], [109, 113], [114, 115], [114, 116], [116, 117], [116, 119], [119, 120], [119, 121], [121, 122], [121, 126], [126, 127], [126, 137], [128, 129], [128, 137], [142, 143], [142, 145], [146, 147], [146, 151], [151, 152], [151, 156], [152, 151], [152, 153], [156, 157], [156, 159], [161, 162], [161, 165], [166, 167], [166, 186], [192, 193], [192, 211], [201, 202], [201, 205], [203, 201], [203, 204]]}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 16, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 16, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [4, 5, 6, 7, 9, 10, 12, 16, 18, 19, 34, 45, 61, 91, 188, 214], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}}, "core\\log_batcher.py": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 87, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 87, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 28, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 28, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [1, 2, 3, 4, 5, 6, 8, 9, 11, 12, 15, 16, 17, 18, 19, 20, 21, 22, 23, 25, 26, 27, 28, 29, 30, 32, 33, 34, 35, 36, 37, 38, 39, 40, 42, 47, 50, 51, 52, 53, 55, 56, 57, 58, 59, 60, 62, 63, 64, 65, 66, 67, 68, 70, 71, 72, 74, 75, 78, 79, 80, 81, 82, 83, 85, 86, 87, 88, 89, 90, 91, 93, 94, 95, 97, 98, 100, 102, 103, 107, 108, 109, 110, 111, 113, 114, 117], "excluded_lines": [], "executed_branches": [], "missing_branches": [[26, 27], [26, 28], [34, 35], [34, 39], [51, -42], [51, 52], [52, -42], [52, 53], [56, 57], [56, 58], [63, -62], [63, 64], [67, -62], [67, 68], [71, -70], [71, 72], [78, 79], [78, 85], [85, 71], [85, 86], [88, 71], [88, 89], [94, 95], [94, 97], [102, 103], [102, 109], [113, -93], [113, 114]], "functions": {"LogBatcherService.__init__": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 7, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 7, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [17, 18, 19, 20, 21, 22, 23], "excluded_lines": [], "start_line": 16, "executed_branches": [], "missing_branches": []}, "LogBatcherService.start": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 5, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 5, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 2, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 2, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [26, 27, 28, 29, 30], "excluded_lines": [], "start_line": 25, "executed_branches": [], "missing_branches": [[26, 27], [26, 28]]}, "LogBatcherService.stop": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 8, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 8, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 2, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 2, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [33, 34, 35, 36, 37, 38, 39, 40], "excluded_lines": [], "start_line": 32, "executed_branches": [], "missing_branches": [[34, 35], [34, 39]]}, "LogBatcherService.emit": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 5, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 5, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 4, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 4, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [47, 50, 51, 52, 53], "excluded_lines": [], "start_line": 42, "executed_branches": [], "missing_branches": [[51, -42], [51, 52], [52, -42], [52, 53]]}, "LogBatcherService.subscribe": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 5, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 5, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 2, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 2, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [56, 57, 58, 59, 60], "excluded_lines": [], "start_line": 55, "executed_branches": [], "missing_branches": [[56, 57], [56, 58]]}, "LogBatcherService.unsubscribe": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 6, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 6, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 4, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 4, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [63, 64, 65, 66, 67, 68], "excluded_lines": [], "start_line": 62, "executed_branches": [], "missing_branches": [[63, -62], [63, 64], [67, -62], [67, 68]]}, "LogBatcherService._run": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 17, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 17, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 8, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 8, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [71, 72, 74, 75, 78, 79, 80, 81, 82, 83, 85, 86, 87, 88, 89, 90, 91], "excluded_lines": [], "start_line": 70, "executed_branches": [], "missing_branches": [[71, -70], [71, 72], [78, 79], [78, 85], [85, 71], [85, 86], [88, 71], [88, 89]]}, "LogBatcherService._flush": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 14, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 14, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 6, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 6, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [94, 95, 97, 98, 100, 102, 103, 107, 108, 109, 110, 111, 113, 114], "excluded_lines": [], "start_line": 93, "executed_branches": [], "missing_branches": [[94, 95], [94, 97], [102, 103], [102, 109], [113, -93], [113, 114]]}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 20, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 20, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [1, 2, 3, 4, 5, 6, 8, 9, 11, 12, 15, 16, 25, 32, 42, 55, 62, 70, 93, 117], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}, "classes": {"LogBatcherService": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 67, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 67, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 28, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 28, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [17, 18, 19, 20, 21, 22, 23, 26, 27, 28, 29, 30, 33, 34, 35, 36, 37, 38, 39, 40, 47, 50, 51, 52, 53, 56, 57, 58, 59, 60, 63, 64, 65, 66, 67, 68, 71, 72, 74, 75, 78, 79, 80, 81, 82, 83, 85, 86, 87, 88, 89, 90, 91, 94, 95, 97, 98, 100, 102, 103, 107, 108, 109, 110, 111, 113, 114], "excluded_lines": [], "start_line": 15, "executed_branches": [], "missing_branches": [[26, 27], [26, 28], [34, 35], [34, 39], [51, -42], [51, 52], [52, -42], [52, 53], [56, 57], [56, 58], [63, -62], [63, 64], [67, -62], [67, 68], [71, -70], [71, 72], [78, 79], [78, 85], [85, 71], [85, 86], [88, 71], [88, 89], [94, 95], [94, 97], [102, 103], [102, 109], [113, -93], [113, 114]]}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 20, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 20, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [1, 2, 3, 4, 5, 6, 8, 9, 11, 12, 15, 16, 25, 32, 42, 55, 62, 70, 93, 117], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}}, "core\\swarm_orchestrator.py": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 19, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 19, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [4, 6, 7, 8, 9, 12, 16, 17, 18, 19, 21, 22, 23, 25, 28, 31, 34, 36, 37], "excluded_lines": [], "executed_branches": [], "missing_branches": [], "functions": {"SwarmOrchestrator.__init__": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 3, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 3, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [17, 18, 19], "excluded_lines": [], "start_line": 16, "executed_branches": [], "missing_branches": []}, "SwarmOrchestrator.execute_task": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 8, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 8, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [22, 23, 25, 28, 31, 34, 36, 37], "excluded_lines": [], "start_line": 21, "executed_branches": [], "missing_branches": []}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 8, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 8, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [4, 6, 7, 8, 9, 12, 16, 21], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}, "classes": {"SwarmOrchestrator": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 11, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 11, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [17, 18, 19, 22, 23, 25, 28, 31, 34, 36, 37], "excluded_lines": [], "start_line": 12, "executed_branches": [], "missing_branches": []}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 8, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 8, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [4, 6, 7, 8, 9, 12, 16, 21], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}}}, "totals": {"covered_lines": 131, "num_statements": 414, "percent_covered": 29.213483146067414, "percent_covered_display": "29", "missing_lines": 283, "excluded_lines": 0, "percent_statements_covered": 31.642512077294686, "percent_statements_covered_display": "32", "num_branches": 120, "num_partial_branches": 13, "covered_branches": 25, "missing_branches": 95, "percent_branches_covered": 20.833333333333332, "percent_branches_covered_display": "21"}}
\ No newline at end of file
diff --git a/backend/memory/long_term_memory.py b/backend/memory/long_term_memory.py
index f78ab2b38..a85baea72 100644
--- a/backend/memory/long_term_memory.py
+++ b/backend/memory/long_term_memory.py
@@ -4,6 +4,7 @@ from typing import Any
 
 from loguru import logger
 
+
 try:
     from brain.model_router import ModelRouter
     from database.supabase_client import db
@@ -103,4 +104,4 @@ class LongTermMemory:
             parts.append("Summary: " + "; ".join(item["content"] for item in self._summaries))
         if self._facts:
             parts.append("Facts: " + "; ".join(item["content"] for item in self._facts))
-        return "\n".join(parts) if parts else "No memory available."
\ No newline at end of file
+        return "\n".join(parts) if parts else "No memory available."
diff --git a/backend/models/agent_session.py b/backend/models/agent_session.py
index 1393f7144..b071527f1 100644
--- a/backend/models/agent_session.py
+++ b/backend/models/agent_session.py
@@ -1,10 +1,14 @@
 import enum
 import uuid
-from datetime import UTC, datetime
+from datetime import UTC
+from datetime import datetime
 
-from sqlalchemy import DateTime, Enum, ForeignKey, String
+from sqlalchemy import DateTime
+from sqlalchemy import Enum
+from sqlalchemy import String
 from sqlalchemy.dialects.postgresql import UUID
-from sqlalchemy.orm import Mapped, mapped_column, relationship
+from sqlalchemy.orm import Mapped
+from sqlalchemy.orm import mapped_column
 
 from models.base import Base
 
diff --git a/backend/models/base.py b/backend/models/base.py
index e9d9d09b9..2e1e951d7 100644
--- a/backend/models/base.py
+++ b/backend/models/base.py
@@ -1,12 +1,5 @@
-import uuid
-from datetime import UTC
-from datetime import datetime
-from typing import Any
 
-from sqlalchemy import DateTime
 from sqlalchemy.orm import DeclarativeBase
-from sqlalchemy.orm import Mapped
-from sqlalchemy.orm import mapped_column
 
 
 class Base(DeclarativeBase):
diff --git a/backend/models/execution_log.py b/backend/models/execution_log.py
index dd0d9a748..642da2f7e 100644
--- a/backend/models/execution_log.py
+++ b/backend/models/execution_log.py
@@ -1,10 +1,16 @@
 import enum
 import uuid
-from datetime import UTC, datetime
-
-from sqlalchemy import DateTime, Enum, ForeignKey, Integer, String
-from sqlalchemy.dialects.postgresql import JSONB, UUID
-from sqlalchemy.orm import Mapped, mapped_column
+from datetime import UTC
+from datetime import datetime
+
+from sqlalchemy import DateTime
+from sqlalchemy import Enum
+from sqlalchemy import ForeignKey
+from sqlalchemy import Integer
+from sqlalchemy.dialects.postgresql import JSONB
+from sqlalchemy.dialects.postgresql import UUID
+from sqlalchemy.orm import Mapped
+from sqlalchemy.orm import mapped_column
 
 from models.base import Base
 
diff --git a/backend/models/execution_policy.py b/backend/models/execution_policy.py
index 7512f84a0..ad4f25208 100644
--- a/backend/models/execution_policy.py
+++ b/backend/models/execution_policy.py
@@ -2,9 +2,13 @@ import enum
 import uuid
 from decimal import Decimal
 
-from sqlalchemy import Enum, Integer, Numeric, String
+from sqlalchemy import Enum
+from sqlalchemy import Integer
+from sqlalchemy import Numeric
+from sqlalchemy import String
 from sqlalchemy.dialects.postgresql import UUID
-from sqlalchemy.orm import Mapped, mapped_column
+from sqlalchemy.orm import Mapped
+from sqlalchemy.orm import mapped_column
 
 from models.base import Base
 
diff --git a/backend/models/handoff_event.py b/backend/models/handoff_event.py
index 534c0a3b1..d36f21807 100644
--- a/backend/models/handoff_event.py
+++ b/backend/models/handoff_event.py
@@ -1,9 +1,13 @@
 import uuid
-from datetime import UTC, datetime
+from datetime import UTC
+from datetime import datetime
 
-from sqlalchemy import DateTime, ForeignKey, Integer
+from sqlalchemy import DateTime
+from sqlalchemy import ForeignKey
+from sqlalchemy import Integer
 from sqlalchemy.dialects.postgresql import UUID
-from sqlalchemy.orm import Mapped, mapped_column
+from sqlalchemy.orm import Mapped
+from sqlalchemy.orm import mapped_column
 
 from models.base import Base
 
diff --git a/backend/models/selector_healing_event.py b/backend/models/selector_healing_event.py
index bb152b050..fe43c9167 100644
--- a/backend/models/selector_healing_event.py
+++ b/backend/models/selector_healing_event.py
@@ -1,8 +1,11 @@
 import uuid
 
-from sqlalchemy import Boolean, Numeric, String
+from sqlalchemy import Boolean
+from sqlalchemy import Numeric
+from sqlalchemy import String
 from sqlalchemy.dialects.postgresql import UUID
-from sqlalchemy.orm import Mapped, mapped_column
+from sqlalchemy.orm import Mapped
+from sqlalchemy.orm import mapped_column
 
 from models.base import Base
 
diff --git a/backend/models/target_platform_credential.py b/backend/models/target_platform_credential.py
index af3e3ae9c..23e11fab0 100644
--- a/backend/models/target_platform_credential.py
+++ b/backend/models/target_platform_credential.py
@@ -1,10 +1,15 @@
 import enum
 import uuid
-from datetime import UTC, datetime
+from datetime import UTC
+from datetime import datetime
 
-from sqlalchemy import DateTime, Enum, LargeBinary, String
+from sqlalchemy import DateTime
+from sqlalchemy import Enum
+from sqlalchemy import LargeBinary
+from sqlalchemy import String
 from sqlalchemy.dialects.postgresql import UUID
-from sqlalchemy.orm import Mapped, mapped_column
+from sqlalchemy.orm import Mapped
+from sqlalchemy.orm import mapped_column
 
 from models.base import Base
 
diff --git a/backend/models/wallet.py b/backend/models/wallet.py
index c3c8f1015..e2733a6f5 100644
--- a/backend/models/wallet.py
+++ b/backend/models/wallet.py
@@ -9,12 +9,12 @@ from sqlalchemy import Integer
 from sqlalchemy import Numeric
 from sqlalchemy import String
 from sqlalchemy.dialects.postgresql import UUID
-from sqlalchemy.orm import DeclarativeBase
 from sqlalchemy.orm import Mapped
 from sqlalchemy.orm import mapped_column
 
 from models.base import Base
 
+
 class UserWallet(Base):
     __tablename__ = "user_wallets"
 
diff --git a/backend/tools/api_gateway.py b/backend/tools/api_gateway.py
index 5311690ce..052b8e692 100644
--- a/backend/tools/api_gateway.py
+++ b/backend/tools/api_gateway.py
@@ -154,8 +154,8 @@ async def gateway_forward(request: GatewayRequest, http_request: Request) -> Res
                     )
                 except Exception as e:
                     try:
-                        from loguru import logger
-                        logger.error(f"Tool execution error: {e}")
+                        import loguru
+                        loguru.logger.error(f"Tool execution error: {e}")
                     except Exception:
                         pass
                     pass
diff --git a/backend/tools/bangla_voice.py b/backend/tools/bangla_voice.py
index a91bee589..941b48cd3 100644
--- a/backend/tools/bangla_voice.py
+++ b/backend/tools/bangla_voice.py
@@ -25,8 +25,8 @@ class BanglaVoice:
             return whisper is not None
         except Exception as e:
             try:
-                from loguru import logger
-                logger.error(f"Tool execution error: {e}")
+                import loguru
+                loguru.logger.error(f"Tool execution error: {e}")
             except Exception:
                 pass
             return False
@@ -38,8 +38,8 @@ class BanglaVoice:
             return TTS is not None
         except Exception as e:
             try:
-                from loguru import logger
-                logger.error(f"Tool execution error: {e}")
+                import loguru
+                loguru.logger.error(f"Tool execution error: {e}")
             except Exception:
                 pass
             return False
diff --git a/backend/tools/browser_agent.py b/backend/tools/browser_agent.py
index 71092fd18..76d2ac3b5 100644
--- a/backend/tools/browser_agent.py
+++ b/backend/tools/browser_agent.py
@@ -30,8 +30,8 @@ def is_safe_url(url: str) -> bool:
         return not (ip_obj.is_private or ip_obj.is_loopback or ip_obj.is_link_local)
     except Exception as e:
         try:
-            from loguru import logger
-            logger.error(f"Tool execution error: {e}")
+            import loguru
+            loguru.logger.error(f"Tool execution error: {e}")
         except Exception:
             pass
         return False
diff --git a/backend/tools/browser_stealth.py b/backend/tools/browser_stealth.py
index df0568e99..97861551a 100644
--- a/backend/tools/browser_stealth.py
+++ b/backend/tools/browser_stealth.py
@@ -153,8 +153,8 @@ class BrowserStealth:
                 await self.playwright.stop()
         except Exception as e:
             try:
-                from loguru import logger
-                logger.error(f"Tool execution error: {e}")
+                import loguru
+                loguru.logger.error(f"Tool execution error: {e}")
             except Exception:
                 pass
             pass
diff --git a/backend/tools/cloud_sandbox_orchestrator.py b/backend/tools/cloud_sandbox_orchestrator.py
index cd4bf3c2b..cdac2bed3 100644
--- a/backend/tools/cloud_sandbox_orchestrator.py
+++ b/backend/tools/cloud_sandbox_orchestrator.py
@@ -152,8 +152,8 @@ class CloudSandboxOrchestrator:
                     container.kill()
                 except Exception as e:
                     try:
-                        from loguru import logger
-                        logger.error(f"Tool execution error: {e}")
+                        import loguru
+                        loguru.logger.error(f"Tool execution error: {e}")
                     except Exception:
                         pass
                     pass
@@ -189,8 +189,8 @@ class CloudSandboxOrchestrator:
                     container.remove(force=True)
                 except Exception as e:
                     try:
-                        from loguru import logger
-                        logger.error(f"Tool execution error: {e}")
+                        import loguru
+                        loguru.logger.error(f"Tool execution error: {e}")
                     except Exception:
                         pass
                     pass
diff --git a/backend/tools/code_smell_detector.py b/backend/tools/code_smell_detector.py
index c13cf12ae..5e8d0b69e 100644
--- a/backend/tools/code_smell_detector.py
+++ b/backend/tools/code_smell_detector.py
@@ -318,8 +318,8 @@ class CodeSmellDetector:
                     )
             except Exception as e:
                 try:
-                    from loguru import logger
-                    logger.error(f"Tool execution error: {e}")
+                    import loguru
+                    loguru.logger.error(f"Tool execution error: {e}")
                 except Exception:
                     pass
                 pass
diff --git a/backend/tools/comment_thread_ai.py b/backend/tools/comment_thread_ai.py
index 7b5a11b80..8d3a7d19d 100644
--- a/backend/tools/comment_thread_ai.py
+++ b/backend/tools/comment_thread_ai.py
@@ -98,8 +98,8 @@ class CommentThreadAI:
             comments.extend(review if isinstance(review, list) else [])
         except Exception as e:
             try:
-                from loguru import logger
-                logger.error(f"Tool execution error: {e}")
+                import loguru
+                loguru.logger.error(f"Tool execution error: {e}")
             except Exception:
                 pass
             pass
@@ -109,8 +109,8 @@ class CommentThreadAI:
             comments.extend(issue if isinstance(issue, list) else [])
         except Exception as e:
             try:
-                from loguru import logger
-                logger.error(f"Tool execution error: {e}")
+                import loguru
+                loguru.logger.error(f"Tool execution error: {e}")
             except Exception:
                 pass
             pass
@@ -121,8 +121,8 @@ class CommentThreadAI:
             return await self._gh_get(f"/repos/{repo}/pulls/{pr_number}/files")
         except Exception as e:
             try:
-                from loguru import logger
-                logger.error(f"Tool execution error: {e}")
+                import loguru
+                loguru.logger.error(f"Tool execution error: {e}")
             except Exception:
                 pass
             return []
@@ -318,8 +318,8 @@ class CommentThreadAI:
                         )
                 except Exception as e:
                     try:
-                        from loguru import logger
-                        logger.error(f"Tool execution error: {e}")
+                        import loguru
+                        loguru.logger.error(f"Tool execution error: {e}")
                     except Exception:
                         pass
                     pass
@@ -430,8 +430,8 @@ async def github_webhook(
         payload = await request.json()
     except Exception as e:
         try:
-            from loguru import logger
-            logger.error(f"Tool execution error: {e}")
+            import loguru
+            loguru.logger.error(f"Tool execution error: {e}")
         except Exception:
             pass
         raise HTTPException(status_code=400, detail="Invalid JSON payload")
diff --git a/backend/tools/docker_sandbox.py b/backend/tools/docker_sandbox.py
index 056ec5136..3f86268cd 100644
--- a/backend/tools/docker_sandbox.py
+++ b/backend/tools/docker_sandbox.py
@@ -23,8 +23,8 @@ class DockerSandbox:
             return res.returncode == 0
         except Exception as e:
             try:
-                from loguru import logger
-                logger.error(f"Tool execution error: {e}")
+                import loguru
+                loguru.logger.error(f"Tool execution error: {e}")
             except Exception:
                 pass
             return False
diff --git a/backend/tools/domain_adapter.py b/backend/tools/domain_adapter.py
index a89739771..aaee225ee 100644
--- a/backend/tools/domain_adapter.py
+++ b/backend/tools/domain_adapter.py
@@ -80,8 +80,8 @@ class DomainAdapter:
             self._profiles.update(data)
         except Exception as e:
             try:
-                from loguru import logger
-                logger.error(f"Tool execution error: {e}")
+                import loguru
+                loguru.logger.error(f"Tool execution error: {e}")
             except Exception:
                 pass
             pass
diff --git a/backend/tools/gcp_cloud_functions.py b/backend/tools/gcp_cloud_functions.py
index 66a5caed3..2587a5533 100644
--- a/backend/tools/gcp_cloud_functions.py
+++ b/backend/tools/gcp_cloud_functions.py
@@ -119,8 +119,8 @@ class GCPCloudFunctionClient:
             return response.json()
         except Exception as e:
             try:
-                from loguru import logger
-                logger.error(f"Tool execution error: {e}")
+                import loguru
+                loguru.logger.error(f"Tool execution error: {e}")
             except Exception:
                 pass
             return {"text": response.text}
diff --git a/backend/tools/health_checker.py b/backend/tools/health_checker.py
index 0ef3dad84..efec38cdd 100644
--- a/backend/tools/health_checker.py
+++ b/backend/tools/health_checker.py
@@ -90,8 +90,8 @@ class HealthChecker:
                             recent_errors.append(record)
                     except Exception as e:
                         try:
-                            from loguru import logger
-                            logger.error(f"Tool execution error: {e}")
+                            import loguru
+                            loguru.logger.error(f"Tool execution error: {e}")
                         except Exception:
                             pass
                         continue
diff --git a/backend/tools/knowledge_base_indexer.py b/backend/tools/knowledge_base_indexer.py
index ca86cd0de..a181b7e63 100644
--- a/backend/tools/knowledge_base_indexer.py
+++ b/backend/tools/knowledge_base_indexer.py
@@ -36,8 +36,8 @@ class KnowledgeBaseIndexer:
                 source = f.read()
         except Exception as e:
             try:
-                from loguru import logger
-                logger.error(f"Tool execution error: {e}")
+                import loguru
+                loguru.logger.error(f"Tool execution error: {e}")
             except Exception:
                 pass
             return docs
@@ -218,8 +218,8 @@ class KnowledgeBaseIndexer:
                     confidence = float(args[6].value)
         except Exception as e:
             try:
-                from loguru import logger
-                logger.error(f"Tool execution error: {e}")
+                import loguru
+                loguru.logger.error(f"Tool execution error: {e}")
             except Exception:
                 pass
             return None, None
@@ -326,8 +326,8 @@ class KnowledgeBaseIndexer:
             ]
         except Exception as e:
             try:
-                from loguru import logger
-                logger.error(f"Tool execution error: {e}")
+                import loguru
+                loguru.logger.error(f"Tool execution error: {e}")
             except Exception:
                 pass
             return []
diff --git a/backend/tools/local_search_rag.py b/backend/tools/local_search_rag.py
index bc8728203..0e61d16fb 100644
--- a/backend/tools/local_search_rag.py
+++ b/backend/tools/local_search_rag.py
@@ -64,8 +64,8 @@ class LocalSearchRAG:
                 )
             except Exception as e:
                 try:
-                    from loguru import logger
-                    logger.error(f"Tool execution error: {e}")
+                    import loguru
+                    loguru.logger.error(f"Tool execution error: {e}")
                 except Exception:
                     pass
                 self._index = {}
diff --git a/backend/tools/mcp_supabase.py b/backend/tools/mcp_supabase.py
index 2bbf45b12..88ebabf4b 100644
--- a/backend/tools/mcp_supabase.py
+++ b/backend/tools/mcp_supabase.py
@@ -68,8 +68,8 @@ def _get_connection():
         return conn
     except Exception as e:
         try:
-            from loguru import logger
-            logger.error(f"Tool execution error: {e}")
+            import loguru
+            loguru.logger.error(f"Tool execution error: {e}")
         except Exception:
             pass
         return None
@@ -178,8 +178,8 @@ async def supabase_execute_sql(params: ExecuteQueryInput) -> str:
                 conn.close()
             except Exception as e:
                 try:
-                    from loguru import logger
-                    logger.error(f"Tool execution error: {e}")
+                    import loguru
+                    loguru.logger.error(f"Tool execution error: {e}")
                 except Exception:
                     pass
                 pass
@@ -245,8 +245,8 @@ async def supabase_create_table(params: CreateTableInput) -> str:
                 conn.close()
             except Exception as e:
                 try:
-                    from loguru import logger
-                    logger.error(f"Tool execution error: {e}")
+                    import loguru
+                    loguru.logger.error(f"Tool execution error: {e}")
                 except Exception:
                     pass
                 pass
@@ -334,8 +334,8 @@ async def supabase_run_migration(params: MigrationInput) -> str:
                 conn.close()
             except Exception as e:
                 try:
-                    from loguru import logger
-                    logger.error(f"Tool execution error: {e}")
+                    import loguru
+                    loguru.logger.error(f"Tool execution error: {e}")
                 except Exception:
                     pass
                 pass
@@ -390,8 +390,8 @@ async def supabase_list_tables() -> str:
                 conn.close()
             except Exception as e:
                 try:
-                    from loguru import logger
-                    logger.error(f"Tool execution error: {e}")
+                    import loguru
+                    loguru.logger.error(f"Tool execution error: {e}")
                 except Exception:
                     pass
                 pass
diff --git a/backend/tools/meta_architect.py b/backend/tools/meta_architect.py
index d9183ee88..6f632b657 100644
--- a/backend/tools/meta_architect.py
+++ b/backend/tools/meta_architect.py
@@ -45,8 +45,8 @@ class MetaArchitect:
                             ) + len(lines)
                     except Exception as e:
                         try:
-                            from loguru import logger
-                            logger.error(f"Tool execution error: {e}")
+                            import loguru
+                            loguru.logger.error(f"Tool execution error: {e}")
                         except Exception:
                             pass
                         pass
@@ -103,8 +103,8 @@ class MetaArchitect:
                 plan = json.loads(cleaned)
             except Exception as e:
                 try:
-                    from loguru import logger
-                    logger.error(f"Tool execution error: {e}")
+                    import loguru
+                    loguru.logger.error(f"Tool execution error: {e}")
                 except Exception:
                     pass
                 plan = {
diff --git a/backend/tools/multilingual_tts.py b/backend/tools/multilingual_tts.py
index aebcaf9a9..12cef1417 100644
--- a/backend/tools/multilingual_tts.py
+++ b/backend/tools/multilingual_tts.py
@@ -496,8 +496,8 @@ async def clear_cache():
                     removed += 1
                 except Exception as e:
                     try:
-                        from loguru import logger
-                        logger.error(f"Tool execution error: {e}")
+                        import loguru
+                        loguru.logger.error(f"Tool execution error: {e}")
                     except Exception:
                         pass
                     pass
diff --git a/backend/tools/parallel_agent_executor.py b/backend/tools/parallel_agent_executor.py
index bb1704242..cf66362d7 100644
--- a/backend/tools/parallel_agent_executor.py
+++ b/backend/tools/parallel_agent_executor.py
@@ -76,8 +76,8 @@ class ParallelAgentExecutor:
                     redis = app_mod.redis_queue
                 except Exception as e:
                     try:
-                        from loguru import logger
-                        logger.error(f"Tool execution error: {e}")
+                        import loguru
+                        loguru.logger.error(f"Tool execution error: {e}")
                     except Exception:
                         pass
                     redis = None
@@ -117,8 +117,8 @@ class ParallelAgentExecutor:
                     await self._publish_state(redis, agent_name, "failed", error=str(e))
             except Exception as e:
                 try:
-                    from loguru import logger
-                    logger.error(f"Tool execution error: {e}")
+                    import loguru
+                    loguru.logger.error(f"Tool execution error: {e}")
                 except Exception:
                     pass
                 pass
diff --git a/backend/tools/pdf_to_sdk.py b/backend/tools/pdf_to_sdk.py
index 3fb687139..f794e9e11 100644
--- a/backend/tools/pdf_to_sdk.py
+++ b/backend/tools/pdf_to_sdk.py
@@ -22,8 +22,8 @@ class PDFToSDKConverter:
             text = "\n".join(page.get_text() for page in doc)
         except Exception as e:
             try:
-                from loguru import logger
-                logger.error(f"Tool execution error: {e}")
+                import loguru
+                loguru.logger.error(f"Tool execution error: {e}")
             except Exception:
                 pass
             pass
@@ -36,8 +36,8 @@ class PDFToSDKConverter:
                     text = "\n".join(page.extract_text() or "" for page in pdf.pages)
             except Exception as e:
                 try:
-                    from loguru import logger
-                    logger.error(f"Tool execution error: {e}")
+                    import loguru
+                    loguru.logger.error(f"Tool execution error: {e}")
                 except Exception:
                     pass
                 pass
diff --git a/backend/tools/playwright_browser_agent.py b/backend/tools/playwright_browser_agent.py
index 799b67f07..3eb070292 100644
--- a/backend/tools/playwright_browser_agent.py
+++ b/backend/tools/playwright_browser_agent.py
@@ -197,8 +197,8 @@ class PlaywrightBrowserAgent:
                     is_authenticated = page.is_visible(login_check_selector)
                 except Exception as e:
                     try:
-                        from loguru import logger
-                        logger.error(f"Tool execution error: {e}")
+                        import loguru
+                        loguru.logger.error(f"Tool execution error: {e}")
                     except Exception:
                         pass
                     is_authenticated = False
diff --git a/backend/tools/pr_reviewer.py b/backend/tools/pr_reviewer.py
index 35e85bc16..8cd5b2ff6 100644
--- a/backend/tools/pr_reviewer.py
+++ b/backend/tools/pr_reviewer.py
@@ -134,8 +134,8 @@ class PRReviewer:
                             )
             except Exception as e:
                 try:
-                    from loguru import logger
-                    logger.error(f"Tool execution error: {e}")
+                    import loguru
+                    loguru.logger.error(f"Tool execution error: {e}")
                 except Exception:
                     pass
                 logger.warning("Failed to parse LLM response in PRReviewer.")
diff --git a/backend/tools/pre_commit_ai.py b/backend/tools/pre_commit_ai.py
index aa9655d51..ba44625c7 100644
--- a/backend/tools/pre_commit_ai.py
+++ b/backend/tools/pre_commit_ai.py
@@ -101,8 +101,8 @@ class PreCommitAI:
                     original_content = f.read()
             except Exception as e:
                 try:
-                    from loguru import logger
-                    logger.error(f"Tool execution error: {e}")
+                    import loguru
+                    loguru.logger.error(f"Tool execution error: {e}")
                 except Exception:
                     pass
                 continue  # Skip binary files that can't be read
diff --git a/backend/tools/presentation_generator.py b/backend/tools/presentation_generator.py
index 2ea8c0e11..cfdb2a1b3 100644
--- a/backend/tools/presentation_generator.py
+++ b/backend/tools/presentation_generator.py
@@ -33,8 +33,8 @@ class PresentationGenerator:
                     slides = []
             except Exception as e:
                 try:
-                    from loguru import logger
-                    logger.error(f"Tool execution error: {e}")
+                    import loguru
+                    loguru.logger.error(f"Tool execution error: {e}")
                 except Exception:
                     pass
                 for i in range(1, num_slides + 1):
diff --git a/backend/tools/repo_deep_indexer.py b/backend/tools/repo_deep_indexer.py
index 9e9a0b321..b779f1eb1 100644
--- a/backend/tools/repo_deep_indexer.py
+++ b/backend/tools/repo_deep_indexer.py
@@ -56,8 +56,8 @@ class RepoDeepIndexer:
                             snippet = f.read()[:200]
                     except Exception as e:
                         try:
-                            from loguru import logger
-                            logger.error(f"Tool execution error: {e}")
+                            import loguru
+                            loguru.logger.error(f"Tool execution error: {e}")
                         except Exception:
                             pass
                         snippet = ""
@@ -89,8 +89,8 @@ class RepoDeepIndexer:
                 return await self.vector_db_client.query(query, limit)
         except Exception as e:
             try:
-                from loguru import logger
-                logger.error(f"Tool execution error: {e}")
+                import loguru
+                loguru.logger.error(f"Tool execution error: {e}")
             except Exception:
                 pass
             pass
diff --git a/backend/tools/self_planner.py b/backend/tools/self_planner.py
index cb888c6bb..961a5febe 100644
--- a/backend/tools/self_planner.py
+++ b/backend/tools/self_planner.py
@@ -56,8 +56,8 @@ class SelfPlanner:
                     plan = []
             except Exception as e:
                 try:
-                    from loguru import logger
-                    logger.error(f"Tool execution error: {e}")
+                    import loguru
+                    loguru.logger.error(f"Tool execution error: {e}")
                 except Exception:
                     pass
                 logger.warning("LLM returned non-JSON plan. Using fallback.")
diff --git a/backend/tools/skill_recommender.py b/backend/tools/skill_recommender.py
index b36f36229..735860202 100644
--- a/backend/tools/skill_recommender.py
+++ b/backend/tools/skill_recommender.py
@@ -99,8 +99,8 @@ class SkillRecommender:
                         )
                 except Exception as e:
                     try:
-                        from loguru import logger
-                        logger.error(f"Tool execution error: {e}")
+                        import loguru
+                        loguru.logger.error(f"Tool execution error: {e}")
                     except Exception:
                         pass
                     pass
diff --git a/backend/tools/style_learner.py b/backend/tools/style_learner.py
index 7d48202dd..2898600fa 100644
--- a/backend/tools/style_learner.py
+++ b/backend/tools/style_learner.py
@@ -48,8 +48,8 @@ class StyleLearner:
                             code_samples.append(f.read()[:1500])
                     except Exception as e:
                         try:
-                            from loguru import logger
-                            logger.error(f"Tool execution error: {e}")
+                            import loguru
+                            loguru.logger.error(f"Tool execution error: {e}")
                         except Exception:
                             pass
                         pass
@@ -88,8 +88,8 @@ class StyleLearner:
                         return parsed
                 except Exception as e:
                     try:
-                        from loguru import logger
-                        logger.error(f"Tool execution error: {e}")
+                        import loguru
+                        loguru.logger.error(f"Tool execution error: {e}")
                     except Exception:
                         pass
                     logger.warning("Failed to parse style guidelines JSON from LLM.")
@@ -115,8 +115,8 @@ class StyleLearner:
                 return
         except Exception as e:
             try:
-                from loguru import logger
-                logger.error(f"Tool execution error: {e}")
+                import loguru
+                loguru.logger.error(f"Tool execution error: {e}")
             except Exception:
                 pass
             pass
diff --git a/backend/tools/telegram_bot.py b/backend/tools/telegram_bot.py
index 720923766..ceb96c859 100644
--- a/backend/tools/telegram_bot.py
+++ b/backend/tools/telegram_bot.py
@@ -88,8 +88,8 @@ class TelegramBotHandler:
                 )
         except Exception as e:
             try:
-                from loguru import logger
-                logger.error(f"Tool execution error: {e}")
+                import loguru
+                loguru.logger.error(f"Tool execution error: {e}")
             except Exception:
                 pass
             pass
@@ -198,8 +198,8 @@ class TelegramBotHandler:
                     status_lines.append(f"{icon} {name}: `{r.status_code}`")
             except Exception as e:
                 try:
-                    from loguru import logger
-                    logger.error(f"Tool execution error: {e}")
+                    import loguru
+                    loguru.logger.error(f"Tool execution error: {e}")
                 except Exception:
                     pass
                 status_lines.append(f"❌ {name}: unreachable")
diff --git a/backend/tools/tenant_rate_limiter.py b/backend/tools/tenant_rate_limiter.py
index 34e814aac..e0e461c90 100644
--- a/backend/tools/tenant_rate_limiter.py
+++ b/backend/tools/tenant_rate_limiter.py
@@ -28,8 +28,8 @@ class TenantRateLimiter:
             return getattr(app_mod, "redis_queue", None)
         except Exception as e:
             try:
-                from loguru import logger
-                logger.error(f"Tool execution error: {e}")
+                import loguru
+                loguru.logger.error(f"Tool execution error: {e}")
             except Exception:
                 pass
             return None
diff --git a/backend/tools/viral_referral_engine.py b/backend/tools/viral_referral_engine.py
index 540f3f147..94fb1ba86 100644
--- a/backend/tools/viral_referral_engine.py
+++ b/backend/tools/viral_referral_engine.py
@@ -39,8 +39,8 @@ class ViralReferralEngine:
                 return json.load(f)
         except Exception as e:
             try:
-                from loguru import logger
-                logger.error(f"Tool execution error: {e}")
+                import loguru
+                loguru.logger.error(f"Tool execution error: {e}")
             except Exception:
                 pass
             return {"codes": {}, "wallets": {}}
diff --git a/backend/tools/voice.py b/backend/tools/voice.py
index fc3b02cc0..b99b9d101 100644
--- a/backend/tools/voice.py
+++ b/backend/tools/voice.py
@@ -88,8 +88,8 @@ class VoiceInterface:
                     device = "cuda"
             except Exception as e:
                 try:
-                    from loguru import logger
-                    logger.error(f"Tool execution error: {e}")
+                    import loguru
+                    loguru.logger.error(f"Tool execution error: {e}")
                 except Exception:
                     pass
                 pass
diff --git a/backend/tools/voice_coder.py b/backend/tools/voice_coder.py
index 63be2f592..e62f6e83c 100644
--- a/backend/tools/voice_coder.py
+++ b/backend/tools/voice_coder.py
@@ -39,8 +39,8 @@ class VoiceCoder:
                 audio_feedback = await self.voice.text_to_speech_async(feedback_text)
             except Exception as e:
                 try:
-                    from loguru import logger
-                    logger.error(f"Tool execution error: {e}")
+                    import loguru
+                    loguru.logger.error(f"Tool execution error: {e}")
                 except Exception:
                     pass
                 audio_feedback = None
diff --git a/backend/tools/vpn_switcher.py b/backend/tools/vpn_switcher.py
index e2077e6ad..3d8ecfbfa 100644
--- a/backend/tools/vpn_switcher.py
+++ b/backend/tools/vpn_switcher.py
@@ -140,8 +140,8 @@ class VPNRotator:
             return {"proxy": proxy, "source": "premium", "use_case": use_case}
         except Exception as e:
             try:
-                from loguru import logger
-                logger.error(f"Tool execution error: {e}")
+                import loguru
+                loguru.logger.error(f"Tool execution error: {e}")
             except Exception:
                 pass
             return {"proxy": None, "source": "premium", "reason": "not configured"}

```
