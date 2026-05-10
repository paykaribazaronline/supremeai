"""
Git repository cloner with shallow clone and sparse checkout support
"""
import asyncio
import os
import shutil
from pathlib import Path
from tempfile import mkdtemp
from typing import Optional
import subprocess

from ..models import RepoInfo
from ..config import get_settings


async def clone_repo(
    repo_info: RepoInfo,
    token: Optional[str] = None,
    timeout: Optional[int] = None,
) -> Path:
    """
    Clone a git repository with shallow depth.
    
    Args:
        repo_info: Repository information
        token: Optional GitHub PAT for private repos
        timeout: Clone timeout in seconds (default from settings)
        
    Returns:
        Path to the cloned repository
        
    Raises:
        TimeoutError: If clone exceeds timeout
        RuntimeError: If clone fails
    """
    settings = get_settings()
    timeout = timeout or settings.CLONE_TIMEOUT
    
    clone_dir = Path(mkdtemp(prefix="gitingest_"))
    
    try:
        # Build clone URL with authentication if needed
        clone_url = _build_clone_url(repo_info, token)
        
        # Build git clone command
        cmd = [
            "git", "clone",
            "--depth", "1",
            "--single-branch",
            "--no-checkout",  # Don't checkout immediately (for sparse checkout)
        ]
        
        # Add branch if specified
        if repo_info.branch:
            cmd.extend(["--branch", repo_info.branch])
        
        cmd.extend([clone_url, str(clone_dir)])
        
        # Execute clone
        process = await asyncio.create_subprocess_exec(
            *cmd,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
        )
        
        try:
            stdout, stderr = await asyncio.wait_for(
                process.communicate(),
                timeout=timeout,
            )
        except asyncio.TimeoutError:
            process.kill()
            await process.wait()
            shutil.rmtree(clone_dir, ignore_errors=True)
            raise TimeoutError(f"Clone timed out after {timeout} seconds")
        
        if process.returncode != 0:
            error_msg = stderr.decode('utf-8', errors='ignore') if stderr else "Unknown error"
            shutil.rmtree(clone_dir, ignore_errors=True)
            raise RuntimeError(f"Git clone failed: {error_msg}")
        
        # If subpath specified, set up sparse checkout
        if repo_info.subpath:
            await _setup_sparse_checkout(clone_dir, repo_info.subpath)
        else:
            # Regular checkout
            await _checkout(clone_dir)
        
        return clone_dir
        
    except Exception as e:
        shutil.rmtree(clone_dir, ignore_errors=True)
        raise


async def _setup_sparse_checkout(clone_dir: Path, subpath: str) -> None:
    """
    Set up sparse checkout for a specific subpath.
    
    Args:
        clone_dir: Path to cloned repository
        subpath: Subpath to checkout
    """
    # Initialize sparse checkout
    proc = await asyncio.create_subprocess_exec(
        "git", "sparse-checkout", "init",
        cwd=str(clone_dir),
        stdout=asyncio.subprocess.PIPE,
        stderr=asyncio.subprocess.PIPE,
    )
    await proc.wait()
    
    # Set sparse checkout pattern
    proc = await asyncio.create_subprocess_exec(
        "git", "sparse-checkout", "set", subpath,
        cwd=str(clone_dir),
        stdout=asyncio.subprocess.PIPE,
        stderr=asyncio.subprocess.PIPE,
    )
    await proc.wait()
    
    # Checkout
    await _checkout(clone_dir)


async def _checkout(clone_dir: Path) -> None:
    """Perform git checkout"""
    proc = await asyncio.create_subprocess_exec(
        "git", "checkout",
        cwd=str(clone_dir),
        stdout=asyncio.subprocess.PIPE,
        stderr=asyncio.subprocess.PIPE,
    )
    await proc.wait()


def _build_clone_url(repo_info: RepoInfo, token: Optional[str] = None) -> str:
    """
    Build clone URL, optionally with authentication.
    
    Args:
        repo_info: Repository information
        token: Optional authentication token
        
    Returns:
        Clone URL string
    """
    clone_url = repo_info.clone_url
    
    if token and repo_info.host in ("github", "gitlab", "bitbucket"):
        # Insert token into URL for authentication
        clone_url = clone_url.replace(
            "https://",
            f"https://{token}@",
        )
    
    return clone_url


def get_current_commit(clone_dir: Path) -> str:
    """
    Get the current commit SHA of the cloned repo.
    
    Args:
        clone_dir: Path to cloned repository
        
    Returns:
        Commit SHA (full)
    """
    result = subprocess.run(
        ["git", "rev-parse", "HEAD"],
        cwd=str(clone_dir),
        capture_output=True,
        text=True,
    )
    return result.stdout.strip()


def cleanup_repo(clone_dir: Path) -> None:
    """
    Clean up a cloned repository directory.
    
    Args:
        clone_dir: Path to cloned repository
    """
    if clone_dir.exists():
        shutil.rmtree(clone_dir, ignore_errors=True)
