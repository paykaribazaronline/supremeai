from __future__ import annotations

from typing import Any

from fastapi import APIRouter
from fastapi import HTTPException
from pydantic import BaseModel


router = APIRouter(prefix="/agents", tags=["specialized-agents"])


class SymptomRequest(BaseModel):
    symptoms: str
    age: int | None = None
    medical_history: str | None = None


class DrugInteractionRequest(BaseModel):
    medications: list[str]


class LegalAnalysisRequest(BaseModel):
    document_text: str
    doc_type: str = "contract"


class TradeRequest(BaseModel):
    symbol: str
    quantity: float
    price: float | None = None


class ResearchRequest(BaseModel):
    query: str
    source: str = "arxiv"
    max_results: int = 5


class SummarizeRequest(BaseModel):
    paper: dict[str, Any]
    style: str = "apa"


@router.post("/legal/analyze")
async def legal_analyze(payload: LegalAnalysisRequest):
    try:
        from agents.legal_agent import LegalAgent

        agent = LegalAgent()
        result = agent.analyze(payload.document_text, doc_type=payload.doc_type)
        return result
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@router.post("/medical/symptoms")
async def medical_symptoms(payload: SymptomRequest):
    try:
        from agents.medical_agent import MedicalAgent

        agent = MedicalAgent()
        result = agent.symptom_analysis(
            payload.symptoms, age=payload.age, medical_history=payload.medical_history
        )
        return result
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@router.post("/medical/drug-interactions")
async def medical_drug_interactions(payload: DrugInteractionRequest):
    try:
        from agents.medical_agent import MedicalAgent

        agent = MedicalAgent()
        result = agent.drug_interaction(payload.medications)
        return result
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@router.post("/trading/analyze")
async def trading_analyze(symbol: str):
    try:
        from agents.trading_agent import TradingAgent

        agent = TradingAgent()
        return agent.analyze_trend(symbol)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@router.post("/trading/buy")
async def trading_buy(payload: TradeRequest):
    try:
        from agents.trading_agent import TradingAgent

        agent = TradingAgent()
        return agent.buy(payload.symbol, payload.quantity, price=payload.price)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@router.post("/trading/sell")
async def trading_sell(payload: TradeRequest):
    try:
        from agents.trading_agent import TradingAgent

        agent = TradingAgent()
        return agent.sell(payload.symbol, payload.quantity, price=payload.price)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@router.get("/trading/portfolio")
async def trading_portfolio():
    try:
        from agents.trading_agent import TradingAgent

        agent = TradingAgent()
        return agent.portfolio()
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@router.post("/research/search")
async def research_search(payload: ResearchRequest):
    try:
        from agents.research_assistant import ResearchAssistant

        assistant = ResearchAssistant()
        results = assistant.search(
            payload.query, source=payload.source, max_results=payload.max_results
        )
        return {
            "query": payload.query,
            "source": payload.source,
            "papers": results,
            "count": len(results),
        }
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@router.post("/research/summarize")
async def research_summarize(payload: SummarizeRequest):
    try:
        from agents.research_assistant import ResearchAssistant

        assistant = ResearchAssistant()
        return assistant.summarize(payload.paper)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@router.post("/research/cite")
async def research_cite(payload: SummarizeRequest):
    try:
        from agents.research_assistant import ResearchAssistant

        assistant = ResearchAssistant()
        return {"citation": assistant.citations(payload.paper, style=payload.style)}
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc
