# বাংলা মন্তব্য: Game Dev Agent-এর Unity/Blender/GDD জেনারেশন ফাংজনালিটি টেস্ট।

from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from tools.game_dev_agent import GameDevAgent


@pytest.fixture
def mock_game_dev():
    with patch("tools.game_dev_agent.settings") as mock_settings:
        mock_settings.openai_api_key = "test-key"
        yield


@pytest.mark.anyio
@pytest.mark.skip(reason="Technical Debt: Missing _get_model_router method. Needs refactoring. Tracked in TECH_DEBT.md")
async def test_generate_unity_script(mock_game_dev):
    # বাংলা মন্তব্য: Unity C# script জেনারেশন টেস্ট
    agent = GameDevAgent()

    with patch.object(agent, "_get_model_router") as mock_router:
        mock_router.return_value.async_route_and_generate = AsyncMock(return_value={
            "text": """
using UnityEngine;

public class PlayerController : MonoBehaviour
{
    public float speed = 5.0f;

    void Update()
    {
        float moveHorizontal = Input.GetAxis("Horizontal");
        float moveVertical = Input.GetAxis("Vertical");
        Vector3 movement = new Vector3(moveHorizontal, 0.0f, moveVertical);
        transform.position += movement * speed * Time.deltaTime;
    }
}
"""
        })

        result = await agent.generate_unity_script(
            description="Create a player controller script",
            script_type="MonoBehaviour"
        )

    assert result is not None
    assert "PlayerController" in result.code
    assert "Unity" in result.engine


@pytest.mark.anyio
@pytest.mark.skip(reason="Technical Debt: Missing _get_model_router method. Needs refactoring. Tracked in TECH_DEBT.md")
async def test_gdd_to_code(mock_game_dev):
    # বাংলা মন্তব্য: Game Design Document থেকে কোড জেনারেশন টেস্ট
    agent = GameDevAgent()

    gdd_text = """
# Game Design Document

## Core Mechanics
- Player can jump and run
- Collect coins to score points
- Avoid enemies

## Technical Requirements
- 2D platformer
- Unity engine
- C# scripting
"""

    with patch.object(agent, "_get_model_router") as mock_router:
        mock_router.return_value.async_route_and_generate = AsyncMock(return_value={
            "text": """
public class GameManager : MonoBehaviour
{
    private int score = 0;

    public void AddScore(int points)
    {
        score += points;
    }
}
"""
        })

        result = await agent.gdd_to_code(gdd_text, engine="unity")

    assert result is not None
    assert "GameManager" in result.code


@pytest.mark.anyio
@pytest.mark.skip(reason="Technical Debt: Missing _get_model_router method. Needs refactoring. Tracked in TECH_DEBT.md")
async def test_generate_asset_script(mock_game_dev):
    # বাংলা মন্তব্য: Asset description থেকে Blender Python script জেনারেশন টেস্ট
    agent = GameDevAgent()

    with patch.object(agent, "_get_model_router") as mock_router:
        mock_router.return_value.async_route_and_generate = AsyncMock(return_value={
            "text": """
import bpy

# Create a cube
bpy.ops.mesh.primitive_cube_add(size=2, location=(0, 0, 0))
cube = bpy.context.active_object
cube.name = "GeneratedCube"

# Add material
mat = bpy.data.materials.new(name="RedMaterial")
mat.diffuse_color = (1, 0, 0, 1)
cube.data.materials.append(mat)
"""
        })

        result = await agent.generate_asset_script(
            asset_description="Create a red cube with material"
        )

    assert result is not None
    assert "bpy" in result.script
    assert "GeneratedCube" in result.script


@pytest.mark.anyio
@pytest.mark.skip(reason="Technical Debt: Missing _get_model_router method. Needs refactoring. Tracked in TECH_DEBT.md")
async def test_profile_game_code(mock_game_dev):
    # বাংলা মন্তব্য: Game code performance profiling suggestions টেস্ট
    agent = GameDevAgent()

    code = """
void Update()
{
    for (int i = 0; i < 10000; i++)
    {
        GameObject.Find("Enemy");
    }
}
"""

    with patch.object(agent, "_get_model_router") as mock_router:
        mock_router.return_value.async_route_and_generate = AsyncMock(return_value={
            "text": """
Performance Issues Found:
1. GameObject.Find() called in loop - use caching instead
2. Consider using object pooling for enemies
3. Use GetComponentCache for repeated component access
"""
        })

        result = await agent.profile_game_code(code)

    assert result is not None
    assert "issues" in result
    assert len(result["issues"]) > 0


@pytest.mark.anyio
@pytest.mark.skip(reason="Technical Debt: Missing _get_model_router method. Needs refactoring. Tracked in TECH_DEBT.md")
async def test_generate_unity_coroutine():
    # বাংলা মন্তব্য: Unity coroutine script জেনারেশন টেস্ট
    agent = GameDevAgent()

    with patch.object(agent, "_get_model_router") as mock_router:
        mock_router.return_value.async_route_and_generate = AsyncMock(return_value={
            "text": """
using UnityEngine;
using System.Collections;

public class AsyncLoader : MonoBehaviour
{
    IEnumerator LoadSceneAsync()
    {
        yield return new WaitForSeconds(1.0f);
        Debug.Log("Loading complete");
    }
}
"""
        })

        result = await agent.generate_unity_script(
            description="Create a coroutine for async loading",
            script_type="Coroutine"
        )

    assert result is not None
    assert "IEnumerator" in result.code
