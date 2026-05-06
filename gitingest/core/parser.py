"""
URL parser for detecting git hosts and extracting repo information
"""
import re
from typing import Optional
from .models import RepoInfo


class GitHost:
    """Git host constants"""
    GITHUB = "github"
    GITLAB = "gitlab"
    BITBUCKET = "bitbucket"
    GITEA = "gitea"
    GENERIC = "generic"


def parse_url(url: str) -> RepoInfo:
    """
    Parse a Git repository URL and extract information.
    
    Supports:
    - https://github.com/user/repo
    - https://github.com/user/repo/tree/branch/path
    - https://gitlab.com/user/repo
    - https://bitbucket.org/user/repo
    - githubingest.com/user/repo (converted to github.com)
    - Local paths
    
    Args:
        url: Repository URL or local path
        
    Returns:
        RepoInfo object with extracted information
    """
    # Handle githubingest.com URLs (convert to github.com)
    url = re.sub(r'https?://githubingest\.com', 'https://github.com', url)
    
    # Check if it's a local path (no protocol)
    if not url.startswith(('http://', 'https://', 'git://')):
        return _parse_local_path(url)
    
    # Parse HTTPS URLs
    patterns = [
        # GitHub with tree/branch/path
        r'https?://github\.com/([^/]+)/([^/]+?)(?:\.git)?(?:/tree/([^/]+)(?:/(.+))?)?/?$',
        # GitLab
        r'https?://gitlab\.com/([^/]+)/([^/]+?)(?:\.git)?(?:/tree/([^/]+)(?:/(.+))?)?/?$',
        # Bitbucket
        r'https?://bitbucket\.org/([^/]+)/([^/]+?)(?:\.git)?(?:/src/([^/]+)(?:/(.+))?)?/?$',
        # Generic HTTPS
        r'https?://([^/]+)/([^/]+)/([^/]+?)(?:\.git)?/?$',
    ]
    
    for i, pattern in enumerate(patterns):
        match = re.match(pattern, url)
        if match:
            if i == 0:  # GitHub
                user, repo, branch, subpath = match.groups()
                return RepoInfo(
                    host=GitHost.GITHUB,
                    user=user,
                    repo=repo.rstrip('/'),
                    branch=branch,
                    subpath=subpath,
                    original_url=url,
                )
            elif i == 1:  # GitLab
                user, repo, branch, subpath = match.groups()
                return RepoInfo(
                    host=GitHost.GITLAB,
                    user=user,
                    repo=repo.rstrip('/'),
                    branch=branch,
                    subpath=subpath,
                    original_url=url,
                )
            elif i == 2:  # Bitbucket
                user, repo, branch, subpath = match.groups()
                return RepoInfo(
                    host=GitHost.BITBUCKET,
                    user=user,
                    repo=repo.rstrip('/'),
                    branch=branch,
                    subpath=subpath,
                    original_url=url,
                )
            elif i == 3:  # Generic
                host, user, repo = match.groups()
                return RepoInfo(
                    host=host,
                    user=user,
                    repo=repo.rstrip('/'),
                    original_url=url,
                )
    
    raise ValueError(f"Could not parse URL: {url}")


def _parse_local_path(path: str) -> RepoInfo:
    """
    Parse a local filesystem path.
    
    Args:
        path: Local path to git repository
        
    Returns:
        RepoInfo with local path info
    """
    import os
    from pathlib import Path
    
    abs_path = Path(path).resolve()
    
    if not abs_path.exists():
        raise ValueError(f"Path does not exist: {path}")
    
    # Try to extract repo name from path
    repo_name = abs_path.name
    
    return RepoInfo(
        host="local",
        user="local",
        repo=repo_name,
        original_url=str(abs_path),
    )


def detect_git_host(url: str) -> str:
    """
    Detect the git hosting service from URL.
    
    Args:
        url: Repository URL
        
    Returns:
        Host identifier string
    """
    if 'github.com' in url or 'githubingest.com' in url:
        return GitHost.GITHUB
    elif 'gitlab.com' in url:
        return GitHost.GITLAB
    elif 'bitbucket.org' in url:
        return GitHost.BITBUCKET
    elif 'gitea' in url:
        return GitHost.GITEA
    else:
        # Extract hostname
        match = re.search(r'https?://([^/]+)', url)
        if match:
            return match.group(1)
        return GitHost.GENERIC
