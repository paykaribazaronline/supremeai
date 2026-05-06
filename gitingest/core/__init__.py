"""
Core ingestion module - ties all components together
"""
from pathlib import Path
from typing import Optional, List
import tempfile
import shutil

from ..models import RepoInfo, IngestRequest, IngestResponse
from ..config import get_settings
from .parser import parse_url
from .cloner import clone_repo, cleanup_repo, get_current_commit
from .ignore import load_ignore_patterns
from .walker import walk_directory
from .formatter import format_digest, to_response


def ingest(request: IngestRequest) -> IngestResponse:
    """
    Synchronous ingestion entry point.
    
    Args:
        request: IngestRequest with URL and options
        
    Returns:
        IngestResponse with summary, tree, and content
    """
    import asyncio
    return asyncio.run(ingest_async(request))


async def ingest_async(request: IngestRequest) -> IngestResponse:
    """
    Asynchronous ingestion entry point.
    
    Args:
        request: IngestRequest with URL and options
        
    Returns:
        IngestResponse with summary, tree, and content
        
    Raises:
        ValueError: If URL cannot be parsed
        RuntimeError: If ingestion fails
        TimeoutError: If clone times out
    """
    settings = get_settings()
    
    # Parse URL
    repo_info = parse_url(request.url)
    
    # Determine token
    token = request.github_pat or settings.GITHUB_TOKEN
    
    # Clone repository
    clone_dir = None
    try:
        clone_dir = await clone_repo(repo_info, token)
        
        # Load ignore patterns
        ignore_patterns = load_ignore_patterns(clone_dir)
        
        # Parse user patterns
        user_patterns = None
        if request.patterns:
            user_patterns = [p.strip() for p in request.patterns.split(',')]
        
        # Walk directory
        walk_result = walk_directory(
            root=clone_dir,
            ignore_patterns=ignore_patterns,
            repo_root=clone_dir,
            include_patterns=user_patterns if request.pattern_type == 'include' else None,
            exclude_patterns=user_patterns if request.pattern_type == 'exclude' else None,
            max_file_size=request.max_file_size_kb * 1024 if request.max_file_size_kb else None,
        )
        
        # Format output
        digest = format_digest(
            repo_info=repo_info,
            files=walk_result.files,
            repo_path=clone_dir,
            symlinks=walk_result.symlinks,
            total_size=walk_result.total_size,
        )
        
        return to_response(digest)
        
    finally:
        # Cleanup
        if clone_dir:
            cleanup_repo(clone_dir)


def ingest_local(
    path: str,
    include_patterns: Optional[List[str]] = None,
    exclude_patterns: Optional[List[str]] = None,
    max_file_size_kb: Optional[int] = None,
) -> IngestResponse:
    """
    Ingest a local directory (no cloning needed).
    
    Args:
        path: Path to local directory
        include_patterns: Optional include patterns
        exclude_patterns: Optional exclude patterns
        max_file_size_kb: Maximum file size in KB
        
    Returns:
        IngestResponse
    """
    from pathlib import Path
    
    local_path = Path(path).resolve()
    if not local_path.exists():
        raise ValueError(f"Path does not exist: {path}")
    
    # Create RepoInfo for local path
    from ..models import RepoInfo
    repo_info = RepoInfo(
        host="local",
        user="local",
        repo=local_path.name,
        original_url=str(local_path),
    )
    
    # Load ignore patterns
    ignore_patterns = load_ignore_patterns(local_path)
    
    # Walk directory
    from .walker import walk_directory
    walk_result = walk_directory(
        root=local_path,
        ignore_patterns=ignore_patterns,
        repo_root=local_path,
        include_patterns=include_patterns,
        exclude_patterns=exclude_patterns,
        max_file_size=max_file_size_kb * 1024 if max_file_size_kb else None,
    )
    
    # Format output
    from .formatter import format_digest
    digest = format_digest(
        repo_info=repo_info,
        files=walk_result.files,
        repo_path=local_path,
        symlinks=walk_result.symlinks,
        total_size=walk_result.total_size,
    )
    
    return to_response(digest)


def compute_cache_key(
    repo_info: RepoInfo,
    request: IngestRequest,
    commit_sha: Optional[str] = None,
) -> str:
    """
    Compute cache key from repo info and request.
    
    Args:
        repo_info: Repository information
        request: Ingest request
        commit_sha: Optional commit SHA (if available)
        
    Returns:
        Cache key string
    """
    import hashlib
    import json
    
    parts = [
        repo_info.full_name,
        commit_sha or repo_info.branch or "main",
        request.pattern_type,
        request.patterns or "",
        str(request.max_file_size_kb or ""),
    ]
    
    key_data = "|".join(parts)
    return hashlib.sha256(key_data.encode()).hexdigest()
