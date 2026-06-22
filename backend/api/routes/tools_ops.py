from __future__ import annotations

import os
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Any, Dict, List, Optional

from tools.code_smell_detector import CodeSmellDetector

router = APIRouter(prefix="/tools", tags=["tools-ops"])


class SmellCheckRequest(BaseModel):
    path: str
    thresholds: Optional[Dict[str, int]] = None


class SmellCheckResponse(BaseModel):
    path: str
    smells: List[Dict[str, Any]]
    summary: Dict[str, int]


@router.post("/smell-check", response_model=SmellCheckResponse)
async def smell_check(payload: SmellCheckRequest):
    if not os.path.exists(payload.path):
        raise HTTPException(status_code=404, detail="Path not found")

    detector = CodeSmellDetector()
    if os.path.isdir(payload.path):
        result = detector.analyze_directory(payload.path, thresholds=payload.thresholds)
        all_smells = [smell for smells in result.values() for smell in smells]
    else:
        all_smells = detector.analyze_python_file(payload.path, thresholds=payload.thresholds)
        if payload.path.endswith((".js", ".ts", ".jsx", ".tsx")):
            all_smells.extend(detector.analyze_js_ts_file(payload.path, thresholds=payload.thresholds))

    by_severity: Dict[str, int] = {"critical": 0, "warning": 0, "info": 0}
    for s in all_smells:
        sev = s.get("severity", "info")
        by_severity[sev] = by_severity.get(sev, 0) + 1

    return SmellCheckResponse(path=payload.path, smells=all_smells, summary=by_severity)
