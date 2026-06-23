#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# ============================================================================
# file >> marketplace_endpoints.py
# project >> SupremeAI 2.0
# purpose >> Skill marketplace
# module >> api
# ============================================================================
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from tools.marketplace_agent import MarketplaceAgent

router = APIRouter(prefix="/marketplace", tags=["marketplace"])
marketplace_agent = MarketplaceAgent()

class SearchRequest(BaseModel):
    query: str
    categories: Optional[List[str]] = None
    filters: Optional[dict] = None

class InstallRequest(BaseModel):
    tool_id: str
    target_environment: str
    sandbox: bool = True

@router.post("/search")
async def search_marketplaces(payload: SearchRequest):
    try:
        categories = payload.categories if payload.categories is not None else []
        filters = payload.filters if payload.filters is not None else {}
        results = marketplace_agent.search_marketplaces(
            payload.query, categories, filters
        )
        return {"status": "success", "tools": results}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/install")
async def install_tool(payload: InstallRequest):
    try:
        res = marketplace_agent.install_tool(
            payload.tool_id, payload.target_environment, payload.sandbox
        )
        return res
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
