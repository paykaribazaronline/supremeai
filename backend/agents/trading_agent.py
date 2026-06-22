import os
import json
import time
from typing import Any, Dict, Optional
from loguru import logger

from database.supabase_client import db


class TradingAgent:
    """
    Market data retrieval, trend analysis, and paper trading.
    Closes Gap #47
    """

    def __init__(self):
        self._portfolio: Dict[str, Any] = {"cash": 10000.0, "positions": {}, "history": []}
        self._load_portfolio()
        logger.info("Initialized TradingAgent")

    def _local_path(self) -> str:
        base = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        return os.path.join(base, "data", "paper_trading_portfolio.json")

    def _load_portfolio(self) -> None:
        if db.client:
            try:
                res = db.client.table("trading_portfolio").select("*").limit(1).execute()
                if res.data:
                    self._portfolio = res.data[0]
                    return
            except Exception:
                pass
        try:
            with open(self._local_path(), "r", encoding="utf-8") as f:
                self._portfolio = json.load(f)
        except Exception:
            pass

    def _save_portfolio(self) -> None:
        if db.client:
            try:
                db.client.table("trading_portfolio").upsert(self._portfolio).execute()
                return
            except Exception:
                pass
        os.makedirs(os.path.dirname(self._local_path()), exist_ok=True)
        with open(self._local_path(), "w", encoding="utf-8") as f:
            json.dump(self._portfolio, f, indent=2)

    def get_market_data(self, symbol: str) -> Dict[str, Any]:
        try:
            import httpx
            url = f"https://query1.finance.yahoo.com/v8/finance/chart/{symbol}"
            resp = httpx.get(url, timeout=5.0, follow_redirects=True)
            if resp.status_code == 200:
                data = resp.json()
                result = data.get("chart", {}).get("result", [{}])[0]
                meta = result.get("meta", {})
                return {
                    "symbol": symbol,
                    "price": meta.get("regularMarketPrice"),
                    "previous_close": meta.get("chartPreviousClose"),
                    "currency": meta.get("currency", "USD"),
                    "source": "yahoo_finance",
                }
        except Exception as exc:
            logger.debug(f"Market data fetch failed for {symbol}: {exc}")
        return {
            "symbol": symbol,
            "price": None,
            "source": "fallback",
            "note": "Live market data unavailable; using mock snapshot",
        }

    def analyze_trend(self, symbol: str) -> Dict[str, Any]:
        data = self.get_market_data(symbol)
        price = data.get("price") or 0.0
        prev = data.get("previous_close") or price
        change_pct = ((price - prev) / prev * 100) if prev else 0.0
        sentiment = "bullish" if change_pct > 1 else ("bearish" if change_pct < -1 else "neutral")
        return {
            "symbol": symbol,
            "price": price,
            "change_pct": round(change_pct, 2),
            "sentiment": sentiment,
            "recommendation": "HOLD",
            "confidence": 0.6,
            "note": "Trend analysis uses simple price momentum. For richer signals, connect technical-indicators library.",
        }

    def buy(self, symbol: str, quantity: float, price: Optional[float] = None) -> Dict[str, Any]:
        price = price or self.get_market_data(symbol).get("price") or 0.0
        cost = quantity * price
        if cost > self._portfolio.get("cash", 0.0):
            return {"status": "error", "reason": "insufficient_funds"}
        self._portfolio["cash"] = self._portfolio.get("cash", 0.0) - cost
        pos = self._portfolio.setdefault("positions", {}).get(symbol, {"qty": 0.0, "avg_price": 0.0})
        total_qty = pos["qty"] + quantity
        pos["avg_price"] = (pos["qty"] * pos["avg_price"] + quantity * price) / total_qty if total_qty > 0 else 0.0
        pos["qty"] = total_qty
        self._portfolio.setdefault("positions", {})[symbol] = pos
        self._portfolio.setdefault("history", []).append({
            "action": "buy", "symbol": symbol, "qty": quantity, "price": price, "ts": time.time()
        })
        self._save_portfolio()
        return {"status": "success", "action": "buy", "symbol": symbol, "qty": quantity, "price": price}

    def sell(self, symbol: str, quantity: float, price: Optional[float] = None) -> Dict[str, Any]:
        pos = self._portfolio.setdefault("positions", {}).get(symbol, {"qty": 0.0, "avg_price": 0.0})
        if pos.get("qty", 0.0) < quantity:
            return {"status": "error", "reason": "insufficient_position"}
        price = price or self.get_market_data(symbol).get("price") or 0.0
        revenue = quantity * price
        self._portfolio["cash"] = self._portfolio.get("cash", 0.0) + revenue
        pos["qty"] -= quantity
        if pos["qty"] == 0:
            pos["avg_price"] = 0.0
        self._portfolio.setdefault("history", []).append({
            "action": "sell", "symbol": symbol, "qty": quantity, "price": price, "ts": time.time()
        })
        self._save_portfolio()
        return {"status": "success", "action": "sell", "symbol": symbol, "qty": quantity, "price": price}

    def portfolio(self) -> Dict[str, Any]:
        positions = self._portfolio.get("positions", {})
        for sym, pos in positions.items():
            current = self.get_market_data(sym).get("price") or pos.get("avg_price", 0.0)
            pos["current_price"] = current
            pos["value"] = round(pos.get("qty", 0.0) * current, 2)
        return {
            "cash": self._portfolio.get("cash", 0.0),
            "positions": positions,
            "total_value": round(self._portfolio.get("cash", 0.0) + sum(p.get("value", 0.0) for p in positions.values()), 2),
            "history_count": len(self._portfolio.get("history", [])),
        }
