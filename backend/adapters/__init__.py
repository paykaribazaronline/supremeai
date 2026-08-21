# backend/adapters/__init__.py
"""SupremeAI Domain Adapters Module."""

from adapters.base_adapter import AdaptationResult, BaseAdapter
from adapters.business_adapter import BusinessAdapter, BusinessDecision, BusinessMetric
from adapters.dev_adapter import CodeAnalysisResult, DevAdapter, DevelopmentTask
from adapters.ux_adapter import DesignPlatform, DesignSpecification, UIComponent, UXAdapter

__all__ = [
    "AdaptationResult",
    "BaseAdapter",
    "BusinessAdapter",
    "BusinessDecision",
    "BusinessMetric",
    "CodeAnalysisResult",
    "DesignPlatform",
    "DesignSpecification",
    "DevAdapter",
    "DevelopmentTask",
    "UIComponent",
    "UXAdapter",
]
