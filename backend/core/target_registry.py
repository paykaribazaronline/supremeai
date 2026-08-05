"""SupremeAI 2.0 - Target Platform Registry & Scope Engine.

Manages dynamic repository bindings and cloud platform targets across 100+ connected endpoints,
enforcing strict permission scopes (READ_ONLY vs FULL_CONTROL).

Key Features:
- PermissionScope: READ_ONLY for primary/main codebases, FULL_CONTROL for agent workspaces.
- TargetPlatformRegistry: Central thread-safe registry for dynamically bound targets.
- Dynamic Scope Guard: Prevents unauthorized write/mutation operations on READ_ONLY targets.
"""

from __future__ import annotations

import logging
import threading
from dataclasses import dataclass, field
from enum import Enum
from typing import Any

logger = logging.getLogger(__name__)


class PermissionScope(str, Enum):
    """পারমিশন স্কোপ এনাম - রিড-অনলি বনাম ফুল-কন্ট্রোল।"""

    READ_ONLY = "READ_ONLY"
    FULL_CONTROL = "FULL_CONTROL"


class TargetPlatformType(str, Enum):
    """টার্গেট প্ল্যাটফর্ম টাইপ এনাম।"""

    GIT_REPOSITORY = "GIT_REPOSITORY"
    CLOUD_SERVICE = "CLOUD_SERVICE"
    API_ENDPOINT = "API_ENDPOINT"


@dataclass
class TargetEntity:
    """একটি রেজিস্টার্ড টার্গেট রেপো বা প্ল্যাটফর্ম অবজেক্ট।"""

    id: str
    name: str
    target_type: TargetPlatformType
    url: str
    branch: str = "main"
    scope: PermissionScope = PermissionScope.FULL_CONTROL
    credentials_token: str = ""
    metadata: dict[str, Any] = field(default_factory=dict)

    def is_read_only(self) -> bool:
        """টার্গেটটি রিড-অনলি কি না পরীক্ষা করে।"""
        return self.scope == PermissionScope.READ_ONLY

    def can_write(self) -> bool:
        """টার্গেটে রাইট বা অন-স্পট মডিফিকেশনের অনুমতি আছে কি না।"""
        return self.scope == PermissionScope.FULL_CONTROL


class TargetPlatformRegistry:
    """১০০+ ডাইনামিক রেপো ও প্ল্যাটফর্ম টার্গেট রেজিস্ট্রি ম্যানেজার।"""

    def __init__(self) -> None:
        self._targets: dict[str, TargetEntity] = {}
        self._lock = threading.RLock()
        self._register_default_main_repo()

    def _register_default_main_repo(self) -> None:
        """বাংলা মন্তব্য: ডিফল্ট মেইন রেপোকে READ_ONLY স্কোপে রেজিস্টার করে আইসোলেশন নিশ্চিত করা।"""
        main_target = TargetEntity(
            id="main-repository",
            name="SupremeAI Main Codebase",
            target_type=TargetPlatformType.GIT_REPOSITORY,
            url="origin/main",
            branch="main",
            scope=PermissionScope.READ_ONLY,
            metadata={"description": "Protected primary codebase - read-only analysis only"},
        )
        self._targets[main_target.id] = main_target

    def register_target(self, target: TargetEntity) -> TargetEntity:
        """নতুন একটি টার্গেট রেপো বা প্ল্যাটফর্ম রেজিস্টার করে।"""
        with self._lock:
            self._targets[target.id] = target
            logger.info(f"Registered target '{target.id}' ({target.name}) with scope {target.scope}")
            return target

    def unregister_target(self, target_id: str) -> bool:
        """রেজিস্টার্ড টার্গেট সরিয়ে ফেলে (মেইন রেপো সরানো যাবে না)।"""
        with self._lock:
            if target_id == "main-repository":
                raise ValueError("Protected main-repository target cannot be unregistered")
            if target_id in self._targets:
                del self._targets[target_id]
                logger.info(f"Unregistered target '{target_id}'")
                return True
            return False

    def get_target(self, target_id: str) -> TargetEntity | None:
        """টার্গেট আইডি দিয়ে অবজেক্ট রিটার্ন করে।"""
        with self._lock:
            return self._targets.get(target_id)

    def list_targets(self) -> list[TargetEntity]:
        """সমস্ত রেজিস্টার্ড টার্গেটের তালিকা রিটার্ন করে।"""
        with self._lock:
            return list(self._targets.values())

    def validate_write_permission(self, target_id: str) -> bool:
        """রাইট অ্যাকশনের আগে পারমিশন স্কোপ ভ্যালিডেট করে।"""
        target = self.get_target(target_id)
        if not target:
            raise KeyError(f"Target '{target_id}' not found in registry")
        if target.is_read_only():
            logger.warning(f"Write operation blocked for READ_ONLY target '{target_id}'")
            return False
        return True


# Singleton instance
target_registry = TargetPlatformRegistry()
