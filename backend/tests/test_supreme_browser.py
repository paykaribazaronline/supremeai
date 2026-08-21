"""
backend/tests/test_supreme_browser.py
=====================================
Comprehensive verification test suite for SupremeBrowser Engine (L1 — L5+).
"""

import pytest


@pytest.mark.asyncio
async def test_semantic_dom_resolution():
    """Verify SemanticDOM resolves natural language queries by element meaning."""
    from browser.semantic_dom import SemanticDOM

    sdom = SemanticDOM(page=None)
    total_elements = await sdom.build_index()
    assert total_elements > 0

    # Query matching button
    match = await sdom.query("checkout and pay", threshold=0.30)
    assert match is not None
    assert "xpath" in match
    assert match["semantic_confidence"] > 0.30


@pytest.mark.asyncio
async def test_vision_grounding_fallback():
    """Verify VisionGrounding calculates valid visual coordinates."""
    from browser.vision_grounding import VisionGrounding

    vg = VisionGrounding(page=None)
    loc = await vg.locate("Submit button", min_confidence=0.50)
    assert "x" in loc
    assert "y" in loc
    assert loc["confidence"] >= 0.50


@pytest.mark.asyncio
async def test_browsing_memory_and_preflight():
    """Verify cross-session browsing memory and automated preflight action generation."""
    from browser.browsing_memory import BrowsingMemory

    memory = BrowsingMemory()
    site = "https://example-store.com/checkout"

    # Observe cookie banner encounter
    await memory.observe(site, {"has_cookie_banner": True, "load_ms": 350})
    await memory.observe(site, {"has_cookie_banner": True, "load_ms": 400})

    intel = await memory.site_intel(site)
    assert intel["cookie_banner_rate"] > 0.5

    preflight = await memory.preflight_actions(site)
    assert "dismiss_cookie_banner" in preflight


@pytest.mark.asyncio
async def test_autonomous_browser_agent():
    """Verify AutonomousBrowserAgent executes goals with autonomous reasoning and replanning."""
    from browser.autonomous_browser import AutonomousBrowserAgent

    agent = AutonomousBrowserAgent(session=None)
    res = await agent.achieve("Locate the pricing plans and checkout")
    assert res["achieved"] is True
    assert res["total_steps"] > 0
    assert "trace" in res


@pytest.mark.asyncio
async def test_swarm_browser_exploration():
    """Verify SwarmBrowser deploys parallel agents and synthesizes multi-agent exploration."""
    from browser.swarm_browser import SwarmBrowser

    swarm = SwarmBrowser()
    goals = ["Find shoes category", "Find electronics category"]
    res = await swarm.explore("https://ecommerce.dev", goals)
    assert res["status"] == "success"
    assert res["total_agents"] == 2
    assert "synthesis_summary" in res


@pytest.mark.asyncio
async def test_digital_twin_flow_dry_run():
    """Verify flow digital twin simulates actions before execution."""
    from browser.swarm_browser import SwarmBrowser

    swarm = SwarmBrowser()
    flow = [{"action": "navigate", "url": "/login"}, {"action": "click", "target": "#submit"}]
    res = await swarm.dry_run_flow("https://ecommerce.dev", flow)
    assert res["safe"] is True
    assert res["steps_simulated"] == 2
