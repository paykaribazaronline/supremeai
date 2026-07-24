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
        raise HTTPException(
            status_code=500, detail="Skill catalog repository is unavailable."
        )

    catalog = []

    # ডিরেক্টরির সমস্ত .json ম্যানিফেস্ট ফাইল রিড করা হচ্ছে
    for json_file in MANIFEST_DIR.glob("*.json"):
        try:
            # ডিফেন্সিভ চেক: পাথটি সত্যিই আমাদের ডিরেক্টরির ভেতরে কিনা
            if not json_file.resolve().is_relative_to(MANIFEST_DIR):
                logger.warning(
                    f"Path traversal attempt blocked during catalog scan: {json_file}"
                )
                continue

            manifest_data = json.loads(json_file.read_text(encoding="utf-8"))
            catalog.append(manifest_data)

        except json.JSONDecodeError as jde:
            logger.error(
                f"Malformed JSON schema detected in manifest {json_file.name}: {str(jde)}"
            )
            continue
        except Exception as e:
            logger.error(f"Failed to read manifest file {json_file.name}: {str(e)}")
            continue

    logger.info(
        f"Successfully broadcasted {len(catalog)} active skills to the frontend dashboard."
    )
    return catalog
