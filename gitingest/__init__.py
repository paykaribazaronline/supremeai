"""
Gitingest - Convert Git repositories into LLM-friendly text digests
"""

__version__ = "1.0.0"
__author__ = "SupremeAI"

from .core import ingest, ingest_async
from .models import IngestRequest, IngestResponse, RepoInfo

__all__ = [
    "ingest",
    "ingest_async",
    "IngestRequest",
    "IngestResponse",
    "RepoInfo",
]
