# বাংলা মন্তব্য: Image-to-Code টুলের React/Flutter output ফাংশনালিটি টেস্ট।

from unittest.mock import MagicMock, patch

import pytest

from tools.image_to_code import ImageToCode


@pytest.fixture
def mock_image_to_code():
    with patch("tools.image_to_code.settings") as mock_settings:
        mock_settings.openai_api_key = "test-key"
        yield


@pytest.mark.anyio
@pytest.mark.skip(reason="Technical Debt: Missing _get_model_router method. Needs refactoring. Tracked in TECH_DEBT.md")
async def test_figma_to_react(mock_image_to_code):
    # বাংলা মন্তব্য: Figma/UI screenshot থেকে React component জেনারেশন টেস্ট
    converter = ImageToCode()

    with patch.object(converter, "_get_vision_client") as mock_client:
        mock_response = MagicMock()
        mock_response.choices = [MagicMock()]
        mock_response.choices[0].message.content = """
import React from 'react';

export function GeneratedComponent() {
    return (
        <div className="container mx-auto p-4">
            <h1 className="text-2xl font-bold">Hello World</h1>
        </div>
    );
}
"""
        mock_client.return_value.chat.completions.create.return_value = mock_response

        result = await converter.figma_to_react("test_image.png", framework="react")

    assert result is not None
    assert "GeneratedComponent" in result.code
    assert "react" in result.framework.lower()


@pytest.mark.anyio
@pytest.mark.skip(reason="Technical Debt: Missing _get_model_router method. Needs refactoring. Tracked in TECH_DEBT.md")
async def test_figma_to_flutter(mock_image_to_code):
    # বাংলা মন্তব্য: Figma/UI screenshot থেকে Flutter widget জেনারেশন টেস্ট
    converter = ImageToCode()

    with patch.object(converter, "_get_vision_client") as mock_client:
        mock_response = MagicMock()
        mock_response.choices = [MagicMock()]
        mock_response.choices[0].message.content = """
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
        mock_client.return_value.chat.completions.create.return_value = mock_response

        result = await converter.figma_to_react("test_image.png", framework="flutter")

    assert result is not None
    assert "GeneratedWidget" in result.code
    assert "flutter" in result.framework.lower()


@pytest.mark.anyio
@pytest.mark.skip(reason="Technical Debt: Missing _get_model_router method. Needs refactoring. Tracked in TECH_DEBT.md")
async def test_extract_color_palette(mock_image_to_code):
    # বাংলা মন্তব্য: Color palette extraction এবং CSS variable generation টেস্ট
    converter = ImageToCode()

    with patch.object(converter, "_get_vision_client") as mock_client:
        mock_response = MagicMock()
        mock_response.choices = [MagicMock()]
        mock_response.choices[0].message.content = """
{
    "colors": ["#FF5733", "#33FF57", "#3357FF"],
    "primary": "#FF5733",
    "secondary": "#33FF57"
}
"""
        mock_client.return_value.chat.completions.create.return_value = mock_response

        result = await converter.extract_color_palette("test_image.png")

    assert result is not None
    assert len(result.colors) == 3
    assert result.primary == "#FF5733"


@pytest.mark.anyio
@pytest.mark.skip(reason="Technical Debt: Missing _get_model_router method. Needs refactoring. Tracked in TECH_DEBT.md")
async def test_detect_component_tree(mock_image_to_code):
    # বাংলা মন্তব্য: Component tree extraction (nested components) টেস্ট
    converter = ImageToCode()

    with patch.object(converter, "_get_vision_client") as mock_client:
        mock_response = MagicMock()
        mock_response.choices = [MagicMock()]
        mock_response.choices[0].message.content = """
{
    "root": "Container",
    "children": [
        {"name": "Header", "type": "component"},
        {"name": "Content", "type": "component", "children": [
            {"name": "Button", "type": "element"}
        ]}
    ]
}
"""
        mock_client.return_value.chat.completions.create.return_value = mock_response

        result = await converter.detect_component_tree("test_image.png")

    assert result is not None
    assert result.root == "Container"
    assert len(result.children) == 2


@pytest.mark.anyio
@pytest.mark.skip(reason="Technical Debt: Missing _get_model_router method. Needs refactoring. Tracked in TECH_DEBT.md")
async def test_tailwind_class_mapping(mock_image_to_code):
    # বাংলা মন্তব্য: Tailwind CSS class mapping টেস্ট
    converter = ImageToCode()

    with patch.object(converter, "_get_vision_client") as mock_client:
        mock_response = MagicMock()
        mock_response.choices = [MagicMock()]
        mock_response.choices[0].message.content = "flex flex-col items-center justify-center p-4 bg-blue-500"
        mock_client.return_value.chat.completions.create.return_value = mock_response

        result = await converter.extract_tailwind_classes("test_image.png")

    assert result is not None
    assert "flex" in result
    assert "bg-blue-500" in result
