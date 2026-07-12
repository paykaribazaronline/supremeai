# বাংলা মন্তব্য: Image-to-Code টুলের React/Flutter output ফাংশনালিটি টেস্ট।

from unittest.mock import MagicMock, patch, AsyncMock

import pytest

from tools.image_to_code import ImageToCode
from core.llm.llm_gateway import LLMGateway


@pytest.fixture
def mock_image_to_code():
    with (
        patch("tools.image_to_code.settings") as mock_settings,
        patch("tools.image_to_code.ImageToCode._encode_image_file", return_value="dummy_base64"),
    ):
        mock_settings.openai_api_key = "test-key"
        yield


@pytest.mark.anyio
@pytest.mark.anyio
async def test_figma_to_react(mock_image_to_code):
    # বাংলা মন্তব্য: Figma/UI screenshot থেকে React component জেনারেশন টেস্ট
    converter = ImageToCode()

    with patch("core.llm.llm_gateway.LLMGateway.acompletion", new_callable=AsyncMock) as mock_acompletion:
        mock_acompletion.return_value = {
            "text": """
import React from 'react';

export function GeneratedComponent() {
    return (
        <div className="container mx-auto p-4">
            <h1 className="text-2xl font-bold">Hello World</h1>
        </div>
    );
}
"""
        }

        result = await converter.figma_to_react("test_image.png", framework="react")

    assert result is not None
    assert "GeneratedComponent" in result.code
    assert "react" in result.framework.lower()


@pytest.mark.anyio
@pytest.mark.anyio
async def test_figma_to_flutter(mock_image_to_code):
    # বাংলা মন্তব্য: Figma/UI screenshot থেকে Flutter widget জেনারেশন টেস্ট
    converter = ImageToCode()

    with patch("core.llm.llm_gateway.LLMGateway.acompletion", new_callable=AsyncMock) as mock_acompletion:
        mock_acompletion.return_value = {
            "text": """
import 'package:flutter/material.dart';

class GeneratedWidget extends StatelessWidget {
    @override
    Widget build(BuildContext context) {
        return Container(
            padding: EdgeInsets.all(16),
            child: Text('Hello World'),
        );
    }
}
"""
        }

        result = await converter.figma_to_react("test_image.png", framework="flutter")

    assert result is not None
    assert "GeneratedWidget" in result.code
    assert "flutter" in result.framework.lower()


@pytest.mark.anyio
@pytest.mark.anyio
async def test_extract_color_palette(mock_image_to_code):
    # বাংলা মন্তব্য: Color palette extraction এবং CSS variable generation টেস্ট
    converter = ImageToCode()

    with patch("core.llm.llm_gateway.LLMGateway.acompletion", new_callable=AsyncMock) as mock_acompletion:
        mock_acompletion.return_value = {
            "text": """
{
    "palette": ["#FF5733", "#33FF57", "#3357FF"],
    "css_variables": {
        "primary": "#FF5733",
        "secondary": "#33FF57"
    }
}
"""
        }

        result = await converter.extract_color_palette("test_image.png")

    assert result is not None
    assert len(result.palette) == 3
    assert result.css_variables["primary"] == "#FF5733"


@pytest.mark.anyio
@pytest.mark.anyio
async def test_detect_component_tree(mock_image_to_code):
    # বাংলা মন্তব্য: Component tree extraction (nested components) টেস্ট
    converter = ImageToCode()

    with patch("core.llm.llm_gateway.LLMGateway.acompletion", new_callable=AsyncMock) as mock_acompletion:
        mock_acompletion.return_value = {
            "text": """
[
    {
        "name": "Container",
        "type": "component",
        "children": [
            {"name": "Header", "type": "component"},
            {"name": "Content", "type": "component"}
        ]
    }
]
"""
        }

        result = await converter.detect_component_tree("test_image.png")

    assert result is not None
    assert len(result.tree) == 1
    assert result.tree[0]["name"] == "Container"
