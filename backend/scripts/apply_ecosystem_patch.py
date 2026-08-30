"""Apply the ecosystem foundation patch to the existing SupremeAI backend.

বাংলা: এই script-টি production-safe। এটি নিচের কাজ করে:
  1. প্যাচের ফাইলগুলো existing backend tree-তে copy করে (overwrite করে না)।
  2. api/routers.py-তে ecosystem routers-দুটো রেজিস্টার করে (idempotent)।
  3. seed script চালায়।
  4. একটি সারাংশ প্রিন্ট করে।

যদি কোনো ফাইল already exist করে, এটি overwrite করে না — শুধু warn করে।
`--force` দিলে overwrite করবে।

চালানোর নিয়ম (backend ফোল্ডার থেকে):
    python scripts/apply_ecosystem_patch.py           # apply (no overwrite)
    python scripts/apply_ecosystem_patch.py --force   # apply + overwrite
    python scripts/apply_ecosystem_patch.py --dry-run  # শুধু কী করবে দেখায়
    python scripts/apply_ecosystem_patch.py --register-only  # শুধু routers.py patch
"""

from __future__ import annotations

import argparse
import shutil
import sys
from pathlib import Path

_THIS = Path(__file__).resolve()
_PATCH_ROOT = _THIS.parent.parent  # supremeai-ecosystem-foundation-patch/
_BACKEND_SRC = _PATCH_ROOT / "backend"
_SCRIPTS_SRC = _PATCH_ROOT / "scripts"

# বাংলা: apply script সাধারণত backend ফোল্ডারে বসে চালানো হয় — তাই CWD থেকে detect।
# কিন্তু --target দিয়ে explicit করা যায়।


def detect_backend_root() -> Path | None:
    """Detect the supremeai backend root (where main.py / api/ / core/ live)."""
    cwd = Path.cwd()
    candidates = [cwd, cwd.parent, cwd / "backend", cwd.parent / "backend"]
    for c in candidates:
        if (c / "main.py").exists() and (c / "api").exists() and (c / "core").exists():
            return c.resolve()
    return None


def copy_tree(src: Path, dst: Path, *, force: bool, dry_run: bool) -> tuple[int, int]:
    """Copy src into dst; skip existing unless force. Returns (copied, skipped)."""
    copied = 0
    skipped = 0
    for s in src.rglob("*"):
        if s.is_dir():
            continue
        rel = s.relative_to(src)
        d = dst / rel
        d.parent.mkdir(parents=True, exist_ok=True)
        if d.exists() and not force:
            print(f"  SKIP (exists): {rel}")
            skipped += 1
            continue
        if dry_run:
            print(f"  WOULD COPY: {rel}")
        else:
            shutil.copy2(s, d)
            print(f"  COPIED: {rel}")
        copied += 1
    return copied, skipped


ECOSYSTEM_USER_ENTRY = '    {"path": "api.routes.ecosystem", "prefix": "", "is_admin": False, "is_critical": False},'
ECOSYSTEM_ADMIN_ENTRY = '    {"path": "api.routes.ecosystem_admin", "prefix": "", "is_admin": True, "is_critical": False},'
USER_MARKER = "api.routes.global_memory"
ADMIN_MARKER = "api.routes.approval_manager"


def patch_routers(backend_root: Path, *, dry_run: bool) -> bool:
    """ROADMAP §31 — register the two ecosystem routers in api/routers.py."""
    routers = backend_root / "api" / "routers.py"
    if not routers.exists():
        print(f"  ERROR: {routers} not found")
        return False
    text = routers.read_text(encoding="utf-8")
    changed = False

    # বাংলা: idempotent — ইতিমধ্যে registered থাকলে skip।
    if "api.routes.ecosystem_admin" not in text:
        if ADMIN_MARKER in text and not dry_run:
            text = text.replace(
                ADMIN_MARKER,
                ECOSYSTEM_ADMIN_ENTRY + "\n    {\"path\": \"api.routes.approval_manager\",",
            )
            changed = True
        elif dry_run:
            print("  WOULD PATCH routers.py: insert ecosystem_admin entry")
    else:
        print("  routers.py already contains ecosystem_admin — skipping")

    if "api.routes.ecosystem\"" not in text and '    {"path": "api.routes.ecosystem",' not in text:
        # insert before USER_MARKER if present, else append at end of ALL_ROUTERS list
        if USER_MARKER in text and not dry_run:
            text = text.replace(
                USER_MARKER,
                ECOSYSTEM_USER_ENTRY + "\n    {\"path\": \"api.routes.global_memory\",",
            )
            changed = True
        elif dry_run:
            print("  WOULD PATCH routers.py: insert ecosystem (user) entry")
    else:
        print("  routers.py already contains ecosystem (user) — skipping")

    if changed and not dry_run:
        routers.write_text(text, encoding="utf-8")
        print("  PATCHED api/routers.py")
    return True


def run_seed(backend_root: Path) -> None:
    seed = _SCRIPTS_SRC / "seed_ecosystem.py"
    # বাংলা: backend-এ copy হওয়া seed script চালানো হয় যাতে sys.path সঠিক থাকে।
    installed_seed = backend_root / "scripts" / "seed_ecosystem.py"
    if not installed_seed.exists():
        print("  SKIP seed: seed script not installed")
        return
    import subprocess

    print("  running seed_ecosystem.py ...")
    try:
        subprocess.run(
            [sys.executable, str(installed_seed)],
            cwd=str(backend_root),
            check=False,
        )
    except Exception as exc:  # noqa: BLE001
        print(f"  WARN: seed run failed: {exc}")


def main() -> int:
    ap = argparse.ArgumentParser(description="Apply SupremeAI ecosystem foundation patch.")
    ap.add_argument("--target", help="explicit backend root (else auto-detect)")
    ap.add_argument("--force", action="store_true", help="overwrite existing files")
    ap.add_argument("--dry-run", action="store_true", help="show actions without writing")
    ap.add_argument("--register-only", action="store_true", help="only patch routers.py")
    args = ap.parse_args()

    backend_root = Path(args.target).resolve() if args.target else detect_backend_root()
    if backend_root is None:
        print(
            "ERROR: could not auto-detect SupremeAI backend root.\n"
            "Run from inside the backend directory or pass --target <path-to-backend>.",
            file=sys.stderr,
        )
        return 2

    print(f">>> Backend root: {backend_root}")
    print(f">>> Patch source:  {_PATCH_ROOT}")
    print(f">>> Force: {args.force}  Dry-run: {args.dry_run}  Register-only: {args.register_only}")

    if not args.register_only:
        print("\n--- Copying backend/ecosystem + backend/api/routes ---")
        copy_tree(_BACKEND_SRC, backend_root, force=args.force, dry_run=args.dry_run)
        print("\n--- Copying scripts/ ---")
        scripts_dst = backend_root / "scripts"
        scripts_dst.mkdir(parents=True, exist_ok=True)
        copy_tree(_SCRIPTS_SRC, scripts_dst, force=args.force, dry_run=args.dry_run)

    print("\n--- Patching api/routers.py (idempotent) ---")
    patch_routers(backend_root, dry_run=args.dry_run)

    if not args.dry_run and not args.register_only:
        print("\n--- Seeding ecosystem defaults ---")
        run_seed(backend_root)

    print("\n>>> Patch applied. Start/restart the FastAPI server and verify:")
    print("    GET  /api/v1/ecosystem/health")
    print("    GET  /api/v1/ecosystem/capabilities")
    print("    GET  /api/v1/ecosystem/admin/overview")
    print("    GET  /api/v1/ecosystem/mcp/manifest")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
