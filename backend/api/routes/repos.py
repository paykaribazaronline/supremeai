#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# ============================================================================
# file >> repos.py
# project >> SupremeAI 2.0
# purpose >> General utility
# module >> api
# ============================================================================
from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel
from typing import Optional
from database.supabase_client import db

router = APIRouter(prefix="/repos", tags=["repos"])


class RepoCreate(BaseModel):
    id: str
    name: str
    url: str
    description: str = ""
    language: str = ""
    category: Optional[str] = None
    priority: Optional[str] = "medium"
    purpose: Optional[str] = None
    install_command: Optional[str] = None
    status: Optional[str] = "active"
    metadata: Optional[dict] = None


class RepoUpdate(BaseModel):
    name: Optional[str] = None
    url: Optional[str] = None
    description: Optional[str] = None
    language: Optional[str] = None
    category: Optional[str] = None
    priority: Optional[str] = None
    purpose: Optional[str] = None
    install_command: Optional[str] = None
    status: Optional[str] = None
    metadata: Optional[dict] = None


@router.get("/")
async def list_repos(
    category: Optional[str] = None,
    priority: Optional[str] = None,
    status: str = "active",
    limit: int = Query(default=50, le=200),
    offset: int = 0,
):
    if not db.client:
        raise HTTPException(status_code=503, detail="Database not configured")
    query = db.client.table("github_repos").select("*").eq("status", status)
    if category:
        query = query.eq("category", category)
    if priority:
        query = query.eq("priority", priority)
    res = query.range(offset, offset + limit - 1).execute()
    return {"items": res.data or [], "total": len(res.data or [])}


@router.post("/")
async def create_repo(payload: RepoCreate):
    if not db.client:
        raise HTTPException(status_code=503, detail="Database not configured")
    data = payload.dict(exclude_none=True)
    res = db.client.table("github_repos").insert(data).execute()
    return {"status": "success", "repo": res.data[0] if res.data else data}


@router.patch("/{repo_id}")
async def update_repo(repo_id: str, payload: RepoUpdate):
    if not db.client:
        raise HTTPException(status_code=503, detail="Database not configured")
    data = payload.dict(exclude_none=True)
    if not data:
        raise HTTPException(status_code=400, detail="No fields to update")
    res = db.client.table("github_repos").update(data).eq("id", repo_id).execute()
    return {"status": "success", "repo": res.data[0] if res.data else None}


@router.delete("/{repo_id}")
async def delete_repo(repo_id: str):
    if not db.client:
        raise HTTPException(status_code=503, detail="Database not configured")
    db.client.table("github_repos").update({"status": "archived"}).eq("id", repo_id).execute()
    return {"status": "success", "message": "Repo archived"}
