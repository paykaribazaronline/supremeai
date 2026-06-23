#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# ============================================================================
# file >> codeflow.py
# project >> SupremeAI 2.0
# purpose >> Code analysis
# module >> api
# ============================================================================
from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional

router = APIRouter(prefix="/api/codeflow", tags=["codeflow"])


class CodeFlowRequest(BaseModel):
    path: str
    depth: Optional[int] = 1


class CodeFlowEdge(BaseModel):
    source: str
    target: str
    type: str


class CodeFlowNode(BaseModel):
    id: str
    label: str
    kind: str


class CodeFlowResponse(BaseModel):
    path: str
    nodes: list[CodeFlowNode]
    edges: list[CodeFlowEdge]


@router.post("/analyze", response_model=CodeFlowResponse)
async def analyze(req: CodeFlowRequest) -> CodeFlowResponse:
    return CodeFlowResponse(
        path=req.path,
        nodes=[CodeFlowNode(id="main", label=req.path, kind="file")],
        edges=[CodeFlowEdge(source="main", target="stdlib", type="imports")],
    )
