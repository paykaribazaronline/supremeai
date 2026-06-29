import sys
from unittest.mock import MagicMock, patch

import pytest

sys.path.append("..")
from agents.trading_agent import TradingAgent


@pytest.fixture
def agent(tmp_path):
    portfolio_file = tmp_path / "paper_trading_portfolio.json"
    with patch.object(TradingAgent, "_local_path", return_value=str(portfolio_file)):
        agent = TradingAgent()
        agent._portfolio = {"cash": 10000.0, "positions": {}, "history": []}
        return agent


class TestTradingAgent:
    def test_init(self, agent):
        assert agent._portfolio["cash"] == 10000.0

    def test_get_market_data_success(self, agent):
        with patch("httpx.get") as mock_get:
            mock_resp = MagicMock()
            mock_resp.status_code = 200
            mock_resp.json.return_value = {
                "chart": {
                    "result": [
                        {
                            "meta": {
                                "regularMarketPrice": 150.0,
                                "chartPreviousClose": 148.0,
                                "currency": "USD",
                            }
                        }
                    ]
                }
            }
            mock_get.return_value = mock_resp
            result = agent.get_market_data("AAPL")
            assert result["symbol"] == "AAPL"
            assert result["price"] == 150.0

    def test_get_market_data_failure(self, agent):
        with patch("httpx.get", side_effect=Exception("network error")):
            result = agent.get_market_data("AAPL")
            assert result["symbol"] == "AAPL"
            assert result["price"] is None

    def test_analyze_trend_bullish(self, agent):
        with patch.object(agent, "get_market_data", return_value={"price": 155.0, "previous_close": 150.0}):
            result = agent.analyze_trend("AAPL")
            assert result["sentiment"] == "bullish"

    def test_analyze_trend_bearish(self, agent):
        with patch.object(agent, "get_market_data", return_value={"price": 145.0, "previous_close": 150.0}):
            result = agent.analyze_trend("AAPL")
            assert result["sentiment"] == "bearish"

    def test_analyze_trend_neutral(self, agent):
        with patch.object(agent, "get_market_data", return_value={"price": 150.0, "previous_close": 149.5}):
            result = agent.analyze_trend("AAPL")
            assert result["sentiment"] == "neutral"

    def test_buy_success(self, agent):
        with patch.object(agent, "_save_portfolio"):
            result = agent.buy("AAPL", 10, price=100.0)
            assert result["status"] == "success"
            assert agent._portfolio["positions"]["AAPL"]["qty"] == 10

    def test_buy_insufficient_funds(self, agent):
        result = agent.buy("AAPL", 1000, price=100.0)
        assert result["status"] == "error"
        assert result["reason"] == "insufficient_funds"

    def test_sell_success(self, agent):
        with patch.object(agent, "_save_portfolio"):
            agent._portfolio["positions"]["AAPL"] = {"qty": 10.0, "avg_price": 100.0}
            result = agent.sell("AAPL", 5, price=110.0)
            assert result["status"] == "success"
            assert agent._portfolio["positions"]["AAPL"]["qty"] == 5.0

    def test_sell_insufficient_position(self, agent):
        result = agent.sell("AAPL", 5)
        assert result["status"] == "error"
        assert result["reason"] == "insufficient_position"

    def test_portfolio(self, agent):
        agent._portfolio = {
            "cash": 5000.0,
            "positions": {"AAPL": {"qty": 10, "avg_price": 100, "current_price": 120, "value": 1200}},
        }
        with patch.object(agent, "get_market_data", return_value={"price": 120.0}):
            result = agent.portfolio()
            assert "total_value" in result
