#!/usr/bin/env python3
"""
packages/scripts/master_validator.py — Autonomous Readiness Orchestrator.

সিস্টেম রিবুট বা প্রোডাকশন ডেপ্লয়মেন্টের ঠিক আগে এই স্ক্রিপ্টটি চলে। এটি পুরো
SupremeAI ইকোসিস্টেমের "Health & Config" স্ক্যান করে গ্রিন-সিগন্যাল দেয়।

প্রজেক্টের রিয়েল এনভায়রনমেন্ট কনভেনশন অনুযায়ী অ্যালাইন করা হয়েছে (backend/core/config.py):
  - SUPREMEAI_JWT_SECRET   (production-এ বাধ্যতামূলক, >=64 bytes)
  - SUPABASE_DATABASE_URL / SUPABASE_DATABASE_URL_POOLER  (DB)
  - OPENAI_API_KEY          (LLM Gateway)
  - REDIS_URL               (Distributed Cache / CostGuard — fail-open warning)

রান করুন:  python3 packages/scripts/master_validator.py
"""

import asyncio
import os
import sys

import httpx

# Windows console (cp1252) cannot encode Unicode glyphs — force UTF-8 stdout/stderr
# so the readiness report renders correctly everywhere.
if getattr(sys.stdout, "encoding", "").lower() not in ("utf-8", "utf8", ""):
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:  # noqa: BLE001 - best-effort, never block the scan
        pass

# ANSI Colors for Terminal Output
GREEN = "\033[92m"
RED = "\033[91m"
YELLOW = "\033[93m"
RESET = "\033[0m"

WEAK_JWT_SECRETS = {"secret", "password", "123456", "changeme", "admin", "jwt_secret"}


class MasterValidator:
    """
    Final System Readiness Check before Production Reboot.
    Validates Environment, External APIs, and Critical Infrastructure.
    """

    def __init__(self):
        self.errors = []
        self.warnings = []

    def _env(self, key: str) -> str:
        val = os.getenv(key)
        return val.strip() if isinstance(val, str) else ""

    async def check_environment_variables(self):
        print(f"{YELLOW}Checking Environment Integrity...{RESET}")

        # LLM Gateway — backend/core/config.py: openai_api_key (OPENAI_API_KEY)
        if not self._env("OPENAI_API_KEY"):
            self.errors.append("Missing critical environment variable: OPENAI_API_KEY")

        # JWT Secret — backend/core/config.py: SUPREMEAI_JWT_SECRET (fail-closed)
        jwt = self._env("SUPREMEAI_JWT_SECRET")
        if not jwt:
            self.errors.append(
                "Missing critical environment variable: SUPREMEAI_JWT_SECRET"
            )
        elif len(jwt) < 64:
            self.errors.append(
                f"SUPREMEAI_JWT_SECRET must be >= 64 bytes (current: {len(jwt)}). "
                "Config rejects weak secrets in all environments."
            )
        elif jwt.lower() in WEAK_JWT_SECRETS:
            self.errors.append(
                "SUPREMEAI_JWT_SECRET is a known weak secret - change it immediately."
            )

        # Database — SUPABASE_DATABASE_URL or SUPABASE_DATABASE_URL_POOLER
        if not (
            self._env("SUPABASE_DATABASE_URL")
            or self._env("SUPABASE_DATABASE_URL_POOLER")
        ):
            self.errors.append(
                "Missing database URL: set SUPABASE_DATABASE_URL or SUPABASE_DATABASE_URL_POOLER"
            )

        # Redis is optional at boot but several paths are fail-closed (multi_layer_cache, swarm_pubsub)
        if not self._env("REDIS_URL"):
            self.warnings.append(
                "REDIS_URL not set. CostGuard/cache run fail-closed - expect runtime errors "
                "until Redis is provisioned."
            )

        if self.errors:
            print(f"{RED}Environment has critical gaps{RESET}")
        else:
            print(f"{GREEN}Environment Configured{RESET}")

    async def check_llm_gateway(self):
        print(f"{YELLOW}Pinging LLM Gateway (OpenAI)...{RESET}")
        api_key = self._env("OPENAI_API_KEY")
        if not api_key:
            return

        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(
                    "https://api.openai.com/v1/models",
                    headers={"Authorization": f"Bearer {api_key}"},
                )
                if response.status_code == 200:
                    print(f"{GREEN}LLM Gateway Online{RESET}")
                else:
                    self.errors.append(
                        f"LLM Gateway rejected credentials (HTTP {response.status_code})"
                    )
        except httpx.RequestError as e:
            self.errors.append(f"LLM Gateway unreachable: {e}")

    async def check_redis_cache(self):
        print(f"{YELLOW}Verifying Distributed Cache (Redis)...{RESET}")
        redis_url = self._env("REDIS_URL")
        if not redis_url:
            return

        try:
            import redis.asyncio as redis

            client = redis.from_url(redis_url)
            await client.ping()
            await client.aclose()
            print(f"{GREEN}Distributed Cache Online{RESET}")
        except (
            Exception
        ) as e:  # noqa: BLE001 - readiness check must never crash the scan
            self.errors.append(f"Redis connection failed: {e}")

    async def run_all(self):
        print("\n" + "=" * 50)
        print(" SUPREME-AI: MASTER SYSTEM VALIDATION")
        print("=" * 50 + "\n")

        await self.check_environment_variables()
        await self.check_llm_gateway()
        await self.check_redis_cache()

        print("\n" + "=" * 50)
        if self.errors:
            print(f"{RED}SYSTEM BOOT ABORTED! Critical Errors Found:{RESET}")
            for err in self.errors:
                print(f"  - {err}")
            sys.exit(1)
        else:
            if self.warnings:
                print(f"{YELLOW}Warnings:{RESET}")
                for warn in self.warnings:
                    print(f"  - {warn}")
            print(
                f"{GREEN}ALL SYSTEMS GO! The Autonomous Architecture is ready.{RESET}"
            )
            sys.exit(0)


if __name__ == "__main__":
    validator = MasterValidator()
    asyncio.run(validator.run_all())
