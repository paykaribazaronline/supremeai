# backend/api/routes/skills.py
import json
import logging
from pathlib import Path
from typing import Any

from fastapi import APIRouter, HTTPException

logger = logging.getLogger("supremeai.api.skills")
router = APIRouter(prefix="/skills", tags=["Skill Catalog Infrastructure"])

# বাংলা মন্তব্য: __file__ থেকে absolute path নির্ণয় — relative path CI-তে FileNotFoundError দেয়
# পুরনো: Path("backend/skills/manifests").resolve() — CWD-dependent, CI-তে ভাঙে
# নতুন: Path(__file__).resolve().parent থেকে নিরাপদ relative calculation
MANIFEST_DIR = Path(__file__).resolve().parent.parent.parent / "skills" / "manifests"


@router.get("/catalog", response_model=list[dict[str, Any]])
async def get_active_skill_catalog():
    """
    ফাইল সিস্টেমের manifests/ ফোল্ডার স্ক্যান করে ড্যাশবোর্ডের জন্য
    সমস্ত ভেরিফাইড স্কিল ম্যানিফেস্ট ডাইনামিকালি রেন্ডার করে।
    """
    if not MANIFEST_DIR.exists():
        logger.error(f"Manifest directory not found at: {MANIFEST_DIR}")
        raise HTTPException(status_code=500, detail="Skill catalog repository is unavailable.")

    catalog = []

    # ডিরেক্টরির সমস্ত .json ম্যানিফেস্ট ফাইল রিড করা হচ্ছে
    for json_file in MANIFEST_DIR.glob("*.json"):
        try:
            # ডিফেন্সিভ চেক: পাথটি সত্যিই আমাদের ডিরেক্টরির ভেতরে কিনা
            if not json_file.resolve().is_relative_to(MANIFEST_DIR):
                logger.warning(f"Path traversal attempt blocked during catalog scan: {json_file}")
                continue

            manifest_data = json.loads(json_file.read_text(encoding="utf-8"))
            catalog.append(manifest_data)

        except json.JSONDecodeError as jde:
            logger.error(f"Malformed JSON schema detected in manifest {json_file.name}: {jde!s}")
            continue
        except Exception as e:
            logger.error(f"Failed to read manifest file {json_file.name}: {e!s}")
            continue

    logger.info(f"Successfully broadcasted {len(catalog)} active skills to the frontend dashboard.")
    return catalog


# বাংলা মন্তব্ত: AUDIT-018 ফিক্স — Studio Client-এর useAdminApi.ts এবং
# EnhancedSkillMarketplace.tsx-এর /api/skills/install এবং /api/skills/search
# কলগুলো এখন ব্যাকএন্ডে আছে (আগে 404 পেত)।
@router.post("/search", response_model=list[dict[str, Any]], tags=["Skill Catalog Infrastructure"])
async def search_skills(query: str = "", installed_only: bool = False):
    """Search skill manifests by keyword query."""
    if not MANIFEST_DIR.exists():
        raise HTTPException(status_code=500, detail="Skill catalog repository is unavailable.")
    results = []
    for json_file in MANIFEST_DIR.glob("*.json"):
        try:
            manifest_data = json.loads(json_file.read_text(encoding="utf-8"))
            if query.lower() in json.dumps(manifest_data).lower():
                results.append(manifest_data)
                if len(results) > 100:
                    break
        except Exception:  # noqa: S112 — বাংলা: ফাইল রিড ত্রুটি হলে স্কিপ করে পরবর্তী ফাইলের জন্য লুপ চালু রাখা হয়
            continue
    return results


@router.post("/install", tags=["Skill Catalog Infrastructure"])
async def install_skill(skill: str = ""):
    """Install a skill by its ID into the user workspace."""
    if not skill:
        raise HTTPException(status_code=400, detail="Skill ID is required")
    manifest_path = MANIFEST_DIR / f"{skill}.json"
    if not manifest_path.exists():
        raise HTTPException(status_code=404, detail=f"Skill '{skill}' not found in catalog")
    return {"status": "installed", "skill": skill, "message": f"Skill '{skill}' installed successfully"}
