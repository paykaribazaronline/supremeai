# Gitingest & GitReverse Implementation Plan

## Overview

This document outlines the complete implementation plan for integrating two powerful tools into the SupremeAI system:

1. **Gitingest** - Converts Git repositories into LLM-friendly text digests
2. **GitReverse** - Generates natural-language prompts from GitHub repos to recreate projects

Both tools enhance SupremeAI's code analysis and project understanding capabilities.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Gitingest Implementation](#gitingest-implementation)
3. [GitReverse Implementation](#gitreverse-implementation)
4. [Integration with SupremeAI](#integration-with-supremeai)
5. [Project Structure](#project-structure)
6. [Implementation Timeline](#implementation-timeline)
7. [Dependencies & Environment](#dependencies--environment)
8. [Testing Strategy](#testing-strategy)

---

## Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    SupremeAI Monorepo                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────┐      ┌─────────────────────────────┐  │
│  │   Gitingest      │      │     GitReverse              │  │
│  │                  │      │                             │  │
│  │  - FastAPI       │      │  - Next.js App Router      │  │
│  │  - CLI (Click)   │      │  - React 19                │  │
│  │  - Python Pkg    │      │  - Tailwind CSS 4          │  │
│  │  - S3/MinIO      │      │  - Supabase (optional)     │  │
│  │  - Prometheus    │      │  - OpenRouter/Google AI    │  │
│  └──────────────────┘      └─────────────────────────────┘  │
│           │                            │                     │
│           └────────────┬───────────────┘                     │
│                        │                                     │
│           ┌────────────▼───────────────┐                    │
│           │   SupremeAI Core Services   │                    │
│           │   (Spring Boot Backend)     │                    │
│           └────────────────────────────┘                    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Technology Stack Summary

| Component | Gitingest | GitReverse |
|-----------|-----------|------------|
| Backend | FastAPI + Uvicorn | Next.js API Routes |
| Frontend | Jinja2 + Tailwind CSS | Next.js App Router + React 19 |
| CLI | Click (Python) | N/A |
| Database | S3/MinIO (cache) | Supabase (optional) |
| LLM Integration | N/A | OpenRouter / Google AI Studio |
| Styling | Tailwind via CDN | Tailwind CSS 4 |
| Validation | Pydantic | Zod (inferred) |
| Logging | Loguru | Next.js built-in |

---

## Gitingest Implementation

### 1. Project Structure

```
gitingest/
├── README.md
├── pyproject.toml              # Package config for pip install
├── requirements.txt
├── .env.example
├── gitingest/
│   ├── __init__.py
│   ├── main.py                 # FastAPI application entry
│   ├── config.py               # Configuration & env vars
│   ├── models.py               # Pydantic request/response schemas
│   ├── cli.py                  # Click CLI entry point
│   ├── core/
│   │   ├── __init__.py
│   │   ├── parser.py           # URL/path parsing, git host detection
│   │   ├── cloner.py           # Async git clone with shallow/sparse
│   │   ├── ignore.py           # .gitignore, .gitingestignore handling
│   │   ├── walker.py           # Directory walking with limits
│   │   ├── reader.py           # File reading with encoding fallback
│   │   ├── processor.py        # Jupyter notebook processing
│   │   ├── formatter.py        # Output formatting (tree + content)
│   │   └── limits.py           # Enforcement of all limits
│   ├── cache/
│   │   ├── __init__.py
│   │   ├── base.py             # Cache interface
│   │   ├── local.py            # Local filesystem cache
│   │   └── s3.py               # S3/MinIO cache
│   ├── api/
│   │   ├── __init__.py
│   │   ├── routes.py           # FastAPI route handlers
│   │   ├── middleware.py       # Rate limiting, CORS, etc.
│   │   └── metrics.py         # Prometheus metrics
│   ├── ui/
│   │   ├── __init__.py
│   │   ├── templates/          # Jinja2 templates
│   │   │   ├── base.html
│   │   │   ├── index.html      # Main page with URL input
│   │   │   └── result.html     # Results display
│   │   └── static/             # CSS/JS (or use CDN)
│   └── utils/
│       ├── __init__.py
│       ├── logging.py          # Loguru setup
│       ├── token_counter.py    # Tiktoken integration
│       ├── git_utils.py        # Git operations helpers
│       └── validators.py       # PAT validation, etc.
├── tests/
│   ├── __init__.py
│   ├── test_parser.py
│   ├── test_cloner.py
│   ├── test_ignore.py
│   ├── test_walker.py
│   ├── test_formatter.py
│   └── test_api.py
└── scripts/
    ├── run_server.sh
    └── setup_dev.sh
```

### 2. Core Implementation Details

#### 2.1 Configuration (`config.py`)

```python
from pydantic_settings import BaseSettings
from functools import lru_cache

class Settings(BaseSettings):
    # Server
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    ALLOWED_HOSTS: list[str] = ["*"]
    
    # GitHub
    GITHUB_TOKEN: str | None = None
    
    # S3/MinIO Cache
    S3_ENABLED: bool = False
    S3_ENDPOINT: str | None = None
    S3_ACCESS_KEY: str | None = None
    S3_SECRET_KEY: str | None = None
    S3_BUCKET_NAME: str = "gitingest-cache"
    S3_USE_SSL: bool = True
    
    # Logging
    LOG_FORMAT: str = "json"  # json or text
    LOG_LEVEL: str = "INFO"
    SENTRY_DSN: str | None = None
    POSTHOG_API_KEY: str | None = None
    
    # Limits
    MAX_FILE_SIZE: int = 10 * 1024 * 1024  # 10 MB
    MAX_DIR_DEPTH: int = 20
    MAX_FILES: int = 10000
    MAX_TOTAL_SIZE: int = 500 * 1024 * 1024  # 500 MB
    CLONE_TIMEOUT: int = 60  # seconds
    
    # Rate limiting
    RATE_LIMIT_PER_MINUTE: int = 10
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"

@lru_cache
def get_settings():
    return Settings()
```

#### 2.2 URL Parser (`parser.py`)

Key responsibilities:
- Parse GitHub URLs (including `github.com/...` and `githubingest.com/...` style)
- Detect git host: GitHub, GitLab, Bitbucket, Gitea, generic HTTPS
- Extract: user, repo, branch/tag/commit, subpath
- Support local paths

```python
from dataclasses import dataclass
from enum import Enum
import re

class GitHost(Enum):
    GITHUB = "github"
    GITLAB = "gitlab"
    BITBUCKET = "bitbucket"
    GITEA = "gitea"
    GENERIC = "generic"

@dataclass
class RepoInfo:
    host: GitHost
    user: str
    repo: str
    branch: str | None = None
    subpath: str | None = None
    original_url: str | None = None
    
    @property
    def full_name(self) -> str:
        return f"{self.user}/{self.repo}"
    
    @property
    def clone_url(self) -> str:
        if self.host == GitHost.GITHUB:
            return f"https://github.com/{self.user}/{self.repo}.git"
        # ... other hosts
```

#### 2.3 Git Cloner (`cloner.py`)

Key features:
- Async clone using `asyncio` + `gitpython` or subprocess
- Shallow clone (`--depth 1`)
- Sparse checkout for subpaths
- 60-second timeout
- Cleanup after processing

```python
import asyncio
import subprocess
from pathlib import Path
from tempfile import mkdtemp

async def clone_repo(
    repo_info: RepoInfo,
    token: str | None = None,
    timeout: int = 60
) -> Path:
    """Clone repo with shallow depth, return path to clone."""
    clone_dir = Path(mkdtemp(prefix="gitingest_"))
    
    # Build clone command with token if provided
    clone_url = repo_info.clone_url
    if token and repo_info.host == GitHost.GITHUB:
        clone_url = clone_url.replace(
            "https://", f"https://{token}@"
        )
    
    cmd = [
        "git", "clone",
        "--depth", "1",
        "--single-branch",
    ]
    
    if repo_info.branch:
        cmd.extend(["--branch", repo_info.branch])
    
    cmd.extend([clone_url, str(clone_dir)])
    
    try:
        process = await asyncio.create_subprocess_exec(
            *cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE
        )
        await asyncio.wait_for(
            process.wait(),
            timeout=timeout
        )
    except asyncio.TimeoutError:
        raise TimeoutError(f"Clone timed out after {timeout}s")
    
    # Sparse checkout for subpath if specified
    if repo_info.subpath:
        await _setup_sparse_checkout(clone_dir, repo_info.subpath)
    
    return clone_dir
```

#### 2.4 Ignore Pattern Handler (`ignore.py`)

Default patterns for common languages + `.gitignore` + `.gitingestignore`:

```python
DEFAULT_IGNORE_PATTERNS = [
    # Python
    "*.pyc", "__pycache__/", "*.egg-info/", "dist/", "build/",
    ".pytest_cache/", ".venv/", "venv/",
    # JavaScript/TypeScript
    "node_modules/", "npm-debug.log", "yarn-error.log",
    ".next/", "dist/", "build/",
    # Java
    "*.class", "*.jar", "target/", ".gradle/", "build/",
    # General
    ".git/", ".svn/", ".hg/",
    "*.log", "*.tmp", "*.cache",
    ".DS_Store", "Thumbs.db",
]

def load_ignore_patterns(repo_path: Path) -> list[str]:
    """Load all ignore patterns from repo."""
    patterns = list(DEFAULT_IGNORE_PATTERNS)
    
    for filename in [".gitignore", ".gitingestignore"]:
        ignore_file = repo_path / filename
        if ignore_file.exists():
            patterns.extend(
                line.strip()
                for line in ignore_file.read_text().splitlines()
                if line.strip() and not line.startswith("#")
            )
    
    return patterns
```

#### 2.5 File Walker (`walker.py`)

With limits enforcement:

```python
from dataclasses import dataclass, field

@dataclass
class WalkResult:
    files: list[Path]
    symlinks: dict[Path, str]  # path -> target
    total_size: int = 0
    skipped_count: int = 0

def walk_directory(
    root: Path,
    ignore_patterns: list[str],
    include_patterns: list[str] | None = None,
    exclude_patterns: list[str] | None = None,
    max_depth: int = 20,
    max_files: int = 10000,
    max_total_size: int = 500 * 1024 * 1024,
) -> WalkResult:
    """Walk directory with limits and pattern matching."""
    result = WalkResult(files=[], symlinks={})
    
    for dirpath, dirnames, filenames in os.walk(root):
        # Check depth
        depth = len(dirpath.relative_to(root).parts)
        if depth > max_depth:
            dirnames.clear()
            continue
        
        # Filter directories in-place
        dirnames[:] = [
            d for d in dirnames
            if not _is_ignored(Path(d), ignore_patterns)
        ]
        
        for filename in filenames:
            filepath = Path(dirpath) / filename
            
            # Skip if ignored
            if _is_ignored(filepath, ignore_patterns):
                continue
            
            # Apply include/exclude patterns
            if include_patterns and not _matches_any(filepath, include_patterns):
                continue
            if exclude_patterns and _matches_any(filepath, exclude_patterns):
                continue
            
            # Handle symlinks
            if filepath.is_symlink():
                result.symlinks[filepath] = os.readlink(filepath)
                continue
            
            # Check file size
            file_size = filepath.stat().st_size
            if file_size > 10 * 1024 * 1024:  # 10 MB per file
                result.skipped_count += 1
                continue
            
            # Check total size
            if result.total_size + file_size > max_total_size:
                result.skipped_count += 1
                continue
            
            # Check file count
            if len(result.files) >= max_files:
                result.skipped_count += 1
                continue
            
            result.files.append(filepath)
            result.total_size += file_size
    
    return result
```

#### 2.6 File Reader (`reader.py`)

Multi-encoding fallback with Jupyter support:

```python
import chardet
from pathlib import Path

def read_file(filepath: Path) -> str:
    """Read file with encoding fallback."""
    # Try UTF-8 first
    try:
        return filepath.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        pass
    
    # Try UTF-16
    try:
        return filepath.read_text(encoding="utf-16")
    except UnicodeDecodeError:
        pass
    
    # Fallback to latin-1 (always works)
    return filepath.read_text(encoding="latin-1")

def read_file_binary(filepath: Path) -> bytes:
    """Read file as bytes for special processing."""
    return filepath.read_bytes()

def process_jupyter_notebook(filepath: Path) -> str:
    """Extract code cells from Jupyter notebook."""
    import json
    content = read_file(filepath)
    notebook = json.loads(content)
    
    code_cells = []
    for cell in notebook.get("cells", []):
        if cell.get("cell_type") == "code":
            source = cell.get("source", [])
            if isinstance(source, list):
                code_cells.append("".join(source))
            else:
                code_cells.append(source)
    
    return "\n\n".join(code_cells)
```

#### 2.7 Output Formatter (`formatter.py`)

```python
from dataclasses import dataclass

@dataclass
class DigestOutput:
    summary: str
    tree: str
    content: str
    file_count: int
    estimated_tokens: int

def format_digest(
    repo_info: RepoInfo,
    files: list[Path],
    repo_path: Path,
    symlinks: dict[Path, str],
) -> DigestOutput:
    """Format the complete digest output."""
    
    # Build summary
    summary = _build_summary(repo_info, files)
    
    # Build tree (README first, then files, hidden files, dirs)
    tree = _build_tree(files, repo_path, symlinks)
    
    # Build content with file separators
    content = _build_content(files, repo_path)
    
    # Estimate tokens
    estimated_tokens = _estimate_tokens(content)
    
    return DigestOutput(
        summary=summary,
        tree=tree,
        content=content,
        file_count=len(files),
        estimated_tokens=estimated_tokens,
    )

def _build_tree(files: list[Path], repo_path: Path, symlinks: dict) -> str:
    """Build indented directory tree."""
    # Sort: README first, then files, hidden files, directories
    sorted_files = _sort_files_for_tree(files)
    
    lines = []
    for filepath in sorted_files:
        rel_path = filepath.relative_to(repo_path)
        indent = "  " * (len(rel_path.parts) - 1)
        lines.append(f"{indent}{rel_path.name}")
    
    return "\n".join(lines)

def _build_content(files: list[Path], repo_path: Path) -> str:
    """Build file contents with separators."""
    parts = []
    for filepath in sorted(files):
        rel_path = filepath.relative_to(repo_path)
        
        # Process Jupyter notebooks specially
        if filepath.suffix == ".ipynb":
            content = process_jupyter_notebook(filepath)
        else:
            content = read_file(filepath)
        
        parts.append(f"{'=' * 50}")
        parts.append(f"File: {rel_path}")
        parts.append(f"{'=' * 50}")
        parts.append(content)
        parts.append("")  # Empty line between files
    
    return "\n".join(parts)
```

#### 2.8 FastAPI Routes (`api/routes.py`)

```python
from fastapi import FastAPI, HTTPException, Depends
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address

limiter = Limiter(key_func=get_remote_address)

app = FastAPI(title="Gitingest", version="1.0.0")
app.state.limiter = limiter
app.add_exception_handler(429, _rate_limit_exceeded_handler)

@app.post("/api/ingest")
@limiter.limit("10/minute")
async def ingest_repo(request: IngestRequest, request_obj: Request):
    """Ingest a repository and return digest."""
    try:
        # Parse URL
        repo_info = parse_url(request.url)
        
        # Check cache
        cache_key = _compute_cache_key(repo_info, request)
        if cached := await get_from_cache(cache_key):
            return cached
        
        # Clone repo
        clone_dir = await clone_repo(repo_info, request.github_pat)
        
        try:
            # Process
            result = process_repo(clone_dir, repo_info, request)
            
            # Cache result
            await save_to_cache(cache_key, result)
            
            return {
                "summary": result.summary,
                "tree": result.tree,
                "content": result.content,
            }
        finally:
            # Cleanup
            shutil.rmtree(clone_dir, ignore_errors=True)
            
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/{user}/{repository}")
@limiter.limit("10/minute")
async def ingest_github_path(
    user: str,
    repository: str,
    branch: str | None = None,
    pattern_type: str = "exclude",
    patterns: str | None = None,
):
    """Handle GitHub-style URL paths."""
    # Implementation...

@app.get("/{full_path:path}")
async def catch_all(full_path: str):
    """Catch-all that renders dynamic page with pre-populated URL."""
    # Convert github.com/user/repo to gitingest.com/user/repo style
    # Render template with URL pre-populated
```

#### 2.9 CLI (`cli.py`)

```python
import click
from pathlib import Path

@click.command()
@click.argument("source", required=False, default=".")
@click.option("-o", "--output", help="Output file (- for stdout)")
@click.option("-i", "--include", multiple=True, help="Include patterns")
@click.option("-e", "--exclude", multiple=True, help="Exclude patterns")
@click.option("-t", "--token", help="GitHub PAT")
@click.option("--include-submodules", is_flag=True)
@click.option("--include-gitignored", is_flag=True)
def main(source, output, include, exclude, token, include_submodules, include_gitignored):
    """Gitingest: Convert Git repos to LLM-friendly text digests."""
    
    # Run ingestion
    result = ingest(
        source=source,
        include_patterns=list(include),
        exclude_patterns=list(exclude),
        github_pat=token,
        # ... other options
    )
    
    # Output
    if output == "-":
        click.echo(result.content)
    else:
        output_path = Path(output or "digest.txt")
        output_path.write_text(result.content)
        click.echo(f"Digest written to {output_path}")

if __name__ == "__main__":
    main()
```

#### 2.10 Programmatic API

```python
# gitingest/__init__.py
from .core import ingest, ingest_async

def ingest(
    source: str,
    include_patterns: list[str] | None = None,
    exclude_patterns: list[str] | None = None,
    max_file_size_kb: int | None = None,
    github_pat: str | None = None,
    **kwargs,
) -> DigestOutput:
    """Synchronous ingestion entry point."""
    # Implementation...

async def ingest_async(
    source: str,
    include_patterns: list[str] | None = None,
    exclude_patterns: list[str] | None = None,
    max_file_size_kb: int | None = None,
    github_pat: str | None = None,
    **kwargs,
) -> DigestOutput:
    """Asynchronous ingestion entry point."""
    # Implementation...
```

### 3. UI Templates

#### 3.1 Home Page (`templates/index.html`)

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gitingest - LLM-Friendly Repo Digests</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <style>
        :root {
            --primary-orange: #FCA847;
            --off-white: #FFFDF8;
            --light-blue: #E8F0FE;
            --cream: #fff4da;
            --dark-text: #333;
            --medium-text: #666;
        }
    </style>
</head>
<body class="bg-[#FFFDF8] text-[#333]">
    <!-- Navbar -->
    <nav class="sticky top-0 bg-white border-b-4 border-zinc-900 z-50">
        <div class="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
            <div class="flex items-center gap-2">
                <svg><!-- Logo SVG --></svg>
                <span class="text-xl font-bold">Gitingest</span>
            </div>
            <div class="flex items-center gap-4">
                <a href="https://github.com/..." class="hover:text-[#FCA847]">GitHub</a>
                <a href="https://discord.com/..." class="hover:text-[#FCA847]">Discord</a>
                <span class="text-sm text-[#666]">v1.0.0</span>
            </div>
        </div>
    </nav>

    <!-- Main Content -->
    <main class="max-w-4xl mx-auto px-4 py-12">
        <!-- Animated Title -->
        <div class="text-center mb-12">
            <h1 class="text-5xl font-bold mb-4 relative inline-block">
                <span class="relative z-10">Gitingest</span>
                <!-- Sparkle SVGs -->
                <svg class="absolute -top-4 -right-8 animate-pulse"><!-- sparkle --></svg>
                <svg class="absolute -bottom-2 -left-6 animate-pulse delay-300"><!-- sparkle --></svg>
            </h1>
            <p class="text-xl text-[#666]">Convert any Git repo into LLM-friendly text</p>
        </div>

        <!-- URL Input Form -->
        <form id="ingest-form" class="mb-8" action="/api/ingest" method="post">
            <div class="relative">
                <!-- Input with shadow offset effect -->
                <div class="absolute inset-0 translate-x-2 translate-y-2 bg-zinc-900 rounded-lg"></div>
                <div class="relative">
                    <input 
                        type="text" 
                        id="repo-url"
                        name="url"
                        placeholder="Paste GitHub URL or replace 'hub' with 'ingest'..."
                        class="w-full px-6 py-4 text-lg border-4 border-zinc-900 rounded-lg bg-[#fff4da] focus:outline-none focus:ring-2 focus:ring-[#FCA847]"
                    >
                    <button 
                        type="submit"
                        class="absolute right-2 top-2 px-6 py-2 bg-[#FCA847] text-zinc-900 font-bold border-4 border-zinc-900 rounded-md hover:translate-x-1 hover:translate-y-1 transition-transform shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]"
                    >
                        Ingest
                    </button>
                </div>
            </div>

            <!-- Options Row -->
            <div class="mt-4 flex flex-wrap gap-4 items-center">
                <!-- File Size Slider -->
                <div class="flex-1 min-w-[200px]">
                    <label class="text-sm font-medium">Max File Size</label>
                    <input type="range" min="1" max="500" value="100" class="w-full" id="file-size-slider">
                    <span id="file-size-label" class="text-sm text-[#666]">100 KB</span>
                </div>

                <!-- Private Repo Checkbox -->
                <div class="flex items-center gap-2">
                    <input type="checkbox" id="private-checkbox" class="w-4 h-4">
                    <label for="private-checkbox" class="text-sm">Private repo</label>
                </div>

                <!-- PAT Input (hidden by default) -->
                <div id="pat-input-container" class="hidden flex-1">
                    <input 
                        type="password" 
                        name="github_pat"
                        placeholder="GitHub PAT (ghp_...)"
                        class="w-full px-4 py-2 border-2 border-zinc-900 rounded bg-[#fff4da]"
                    >
                </div>
            </div>

            <!-- Example Repos -->
            <div class="mt-4 flex flex-wrap gap-2">
                <span class="text-sm text-[#666]">Try:</span>
                <a href="#" class="text-sm text-[#FCA847] hover:underline">supremeai/core</a>
                <a href="#" class="text-sm text-[#FCA847] hover:underline">fastapi/fastapi</a>
                <a href="#" class="text-sm text-[#FCA847] hover:underline">pallets/flask</a>
            </div>
        </form>

        <!-- Results Section (hidden initially) -->
        <div id="results-section" class="hidden">
            <!-- Summary -->
            <div class="mb-6 p-4 bg-white border-4 border-zinc-900 rounded-lg">
                <h2 class="text-xl font-bold mb-2">Summary</h2>
                <div id="summary-content" class="text-[#666]"></div>
            </div>

            <!-- Tree (clickable) -->
            <div class="mb-6 p-4 bg-white border-4 border-zinc-900 rounded-lg">
                <h2 class="text-xl font-bold mb-2">Directory Tree</h2>
                <pre id="tree-content" class="text-sm overflow-x-auto cursor-pointer"></pre>
            </div>

            <!-- File Contents -->
            <div class="p-4 bg-white border-4 border-zinc-900 rounded-lg">
                <h2 class="text-xl font-bold mb-2">File Contents</h2>
                <div id="content-sections"></div>
            </div>
        </div>
    </main>

    <!-- Footer -->
    <footer class="max-w-7xl mx-auto px-4 py-8 border-t-4 border-zinc-900">
        <div class="flex justify-center gap-6">
            <a href="/contributing" class="text-sm text-[#666] hover:text-[#FCA847]">Contributing</a>
            <a href="/security" class="text-sm text-[#666] hover:text-[#FCA847]">Security Policy</a>
        </div>
    </footer>

    <script>
        // JavaScript for form handling, tree interactivity, copy-to-clipboard
        // ... (see full implementation)
    </script>
</body>
</html>
```

### 4. S3/MinIO Cache Implementation

```python
# cache/s3.py
import hashlib
import json
from typing import Any

class S3Cache:
    def __init__(self, settings: Settings):
        self.enabled = settings.S3_ENABLED
        if not self.enabled:
            self.client = None
            return
        
        import boto3
        self.client = boto3.client(
            "s3",
            endpoint_url=settings.S3_ENDPOINT,
            aws_access_key_id=settings.S3_ACCESS_KEY,
            aws_secret_access_key=settings.S3_SECRET_KEY,
            use_ssl=settings.S3_USE_SSL,
        )
        self.bucket = settings.S3_BUCKET_NAME
    
    def _make_key(self, repo_info: RepoInfo, request: IngestRequest) -> str:
        """Create cache key from commit SHA + pattern hash + subpath."""
        parts = [
            repo_info.user,
            repo_info.repo,
            repo_info.branch or "main",
            str(hash(request.patterns)) if request.patterns else "",
            request.subpath or "",
        ]
        return hashlib.sha256("|".join(parts).encode()).hexdigest()
    
    async def get(self, key: str) -> dict[str, Any] | None:
        if not self.enabled:
            return None
        try:
            response = self.client.get_object(Bucket=self.bucket, Key=f"{key}.json")
            return json.loads(response["Body"].read())
        except Exception:
            return None
    
    async def set(self, key: str, data: dict[str, Any]) -> None:
        if not self.enabled:
            return
        self.client.put_object(
            Bucket=self.bucket,
            Key=f"{key}.json",
            Body=json.dumps(data),
        )
```

### 5. Metrics Endpoint (Prometheus)

```python
# api/metrics.py
from prometheus_client import Counter, Histogram, generate_latest
from fastapi.responses import Response

ingest_requests = Counter("gitingest_requests_total", "Total ingest requests")
ingest_errors = Counter("gitingest_errors_total", "Total ingest errors")
ingest_duration = Histogram("gitingest_duration_seconds", "Ingest duration")

@app.get("/metrics")
async def metrics():
    return Response(generate_latest(), media_type="text/plain")
```

---

## GitReverse Implementation

### 1. Project Structure

```
gitreverse/
├── README.md
├── package.json
├── next.config.js
├── tailwind.config.ts
├── tsconfig.json
├── .env.example
├── public/
│   └── favicon.ico
├── src/
│   ├── app/
│   │   ├── layout.tsx              # Root layout with fonts
│   │   ├── page.tsx                # Home page
│   │   ├── library/
│   │   │   └── page.tsx            # Prompt library
│   │   ├── history/
│   │   │   └── page.tsx            # History page (localStorage)
│   │   ├── [owner]/
│   │   │   └── [repo]/
│   │   │       └── page.tsx        # Repo detail page
│   │   ├── api/
│   │   │   ├── reverse/
│   │   │   │   └── route.ts        # Main reverse endpoint
│   │   │   ├── custom/
│   │   │   │   └── route.ts        # Custom/manual control endpoint
│   │   │   └── library/
│   │   │       └── route.ts        # Library API (Supabase)
│   │   └── globals.css
│   ├── components/
│   │   ├── Navbar.tsx
│   │   ├── Footer.tsx
│   │   ├── RepoInput.tsx           # URL input form
│   │   ├── LoadingSpinner.tsx      # Animated spinner + flavor text
│   │   ├── ResultDisplay.tsx       # Pre block with copy button
│   │   ├── LibraryCard.tsx         # Card for library grid
│   │   ├── FileSizeSlider.tsx      # Logarithmic slider
│   │   └── ThemeProvider.tsx       # For styling (if needed)
│   ├── lib/
│   │   ├── github.ts               # GitHub API functions
│   │   ├── llm.ts                  # OpenRouter/Google AI integration
│   │   ├── supabase.ts             # Supabase client (optional)
│   │   ├── cache.ts                # In-flight request map, result caching
│   │   ├── constants.ts            # Colors, defaults
│   │   └── utils.ts                # Utility functions
│   ├── hooks/
│   │   ├── useHistory.ts           # localStorage history management
│   │   ├── useInFlight.ts          # In-flight request tracking
│   │   └── useDebounce.ts          # For library search
│   └── types/
│       └── index.ts                # TypeScript types
└── tests/
    ├── api/
    │   ├── reverse.test.ts
    │   └── custom.test.ts
    └── components/
        ├── RepoInput.test.tsx
        └── LibraryCard.test.tsx
```

### 2. Core Implementation Details

#### 2.1 Next.js Configuration (`next.config.js`)

```javascript
/** @type {import('next').NextConfig} */
const nextConfig = {
  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: 'github.com',
      },
    ],
  },
}

module.exports = nextConfig
```

#### 2.2 Tailwind Configuration (`tailwind.config.ts`)

```typescript
import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        background: "#FFFDF8",
        accent: "#d31611",  // Primary red for "Reverse"
        "input-bg": "#fff4da",
        "button-hover": "#ffc480",
        "brand-orange": "#FCA847",  // For Gitingest compatibility
      },
      fontFamily: {
        sans: ["var(--font-geist-sans)"],
        mono: ["var(--font-geist-mono)"],
      },
      borderWidth: {
        '3': '3px',
      },
    },
  },
  plugins: [],
};

export default config;
```

#### 2.3 Root Layout (`app/layout.tsx`)

```typescript
import type { Metadata } from "next";
import { GeistSans } from "geist/font/sans";
import { GeistMono } from "geist/font/mono";
import "./globals.css";

export const metadata: Metadata = {
  title: "GitReverse - Recreate Projects with AI",
  description: "Generate natural-language prompts from GitHub repos",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body
        className={`${GeistSans.variable} ${GeistMono.variable} font-sans bg-background`}
      >
        {children}
      </body>
    </html>
  );
}
```

#### 2.4 Home Page (`app/page.tsx`)

```typescript
"use client";

import { useState } from "react";
import Navbar from "@/components/Navbar";
import RepoInput from "@/components/RepoInput";
import LoadingSpinner from "@/components/LoadingSpinner";
import ResultDisplay from "@/components/ResultDisplay";

const FLAVOR_TEXTS = [
  "Gathering metadata...",
  "Shaping prompt...",
  "Consulting the AI oracle...",
  "Crafting the perfect prompt...",
  "Analyzing repository structure...",
];

export default function HomePage() {
  const [loading, setLoading] = useState(false);
  const [flavorIndex, setFlavorIndex] = useState(0);
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Rotate flavor text every 450ms
  useEffect(() => {
    if (!loading) return;
    const interval = setInterval(() => {
      setFlavorIndex((prev) => (prev + 1) % FLAVOR_TEXTS.length);
    }, 450);
    return () => clearInterval(interval);
  }, [loading]);

  const handleSubmit = async (url: string, focus?: string) => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch("/api/reverse", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ url, focus }),
      });
      
      if (!response.ok) {
        if (response.status === 429 || response.status === 503) {
          // Rate limit or service unavailable
          const data = await response.json();
          setError(data.message || "Service temporarily unavailable");
          return;
        }
        throw new Error("Failed to generate prompt");
      }
      
      const data = await response.json();
      setResult(data.prompt);
      
      // Save to history
      saveToHistory(url, data.prompt);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-screen">
      <Navbar />
      
      <div className="max-w-4xl mx-auto px-4 py-12">
        {/* Logo/Title */}
        <div className="text-center mb-12">
          <h1 className="text-5xl font-bold">
            Git<span className="text-accent">Reverse</span>
          </h1>
          <p className="text-xl text-gray-600 mt-2">
            Paste a repo, get a prompt to recreate it from scratch
          </p>
        </div>

        {/* Input Form */}
        <RepoInput onSubmit={handleSubmit} />

        {/* Loading State */}
        {loading && (
          <LoadingSpinner flavorText={FLAVOR_TEXTS[flavorIndex]} />
        )}

        {/* Error State */}
        {error && (
          <div className="mt-8 p-4 bg-red-50 border-4 border-red-500 rounded-lg">
            <p className="text-red-700">{error}</p>
          </div>
        )}

        {/* Result */}
        {result && !loading && (
          <div className="mt-8">
            <ResultDisplay content={result} />
          </div>
        )}
      </div>
    </main>
  );
}
```

#### 2.5 API Route - Reverse (`app/api/reverse/route.ts`)

```typescript
import { NextRequest, NextResponse } from "next/server";
import { GitHubAPI } from "@/lib/github";
import { LLMProvider } from "@/lib/llm";
import { InFlightCache } from "@/lib/cache";

// In-flight request map to prevent duplicates
const inFlightCache = new InFlightCache();

export async function POST(request: NextRequest) {
  const body = await request.json();
  const { url, focus } = body;

  // Parse GitHub URL
  const repoInfo = parseGitHubURL(url);
  if (!repoInfo) {
    return NextResponse.json(
      { error: "Invalid GitHub URL" },
      { status: 400 }
    );
  }

  // Check in-flight
  const cacheKey = `${repoInfo.owner}/${repoInfo.repo}:${focus || ""}`;
  const inFlight = inFlightCache.get(cacheKey);
  if (inFlight) {
    return inFlight; // Return the same promise
  }

  // Create new request
  const promise = (async () => {
    try {
      // 1. Fetch repo metadata
      const github = new GitHubAPI(process.env.GITHUB_TOKEN);
      const [metadata, readme, tree] = await Promise.all([
        github.getRepo(repoInfo.owner, repoInfo.repo),
        github.getReadme(repoInfo.owner, repoInfo.repo).catch(() => null),
        github.getTree(repoInfo.owner, repoInfo.repo, "main").catch(async (err) => {
          // Retry with master if main fails
          if (err.status === 404) {
            return github.getTree(repoInfo.owner, repoInfo.repo, "master");
          }
          throw err;
        }),
      ]);

      // 2. Build context for LLM
      const context = buildContext({
        metadata,
        readme: readme?.slice(0, 8000), // Max 8000 chars
        tree: tree.filter((item: any) => item.type === "blob").slice(0, 100), // Depth-1 only
      });

      // 3. Call LLM
      const llm = new LLMProvider();
      const prompt = await llm.generatePrompt(context, focus);

      // 4. Cache in Supabase if available
      if (process.env.SUPABASE_URL) {
        await supabase.from("quick_reverse_cache").upsert({
          repo_full_name: `${repoInfo.owner}/${repoInfo.repo}`,
          prompt,
          // ... metadata
        });
      }

      return NextResponse.json({ prompt });
    } catch (error: any) {
      // Check for rate limit errors
      if (error.message?.includes("rate limit") || error.message?.includes("credits")) {
        return NextResponse.json(
          { 
            error: "LLM rate limit exceeded",
            message: "Please check our library for existing prompts instead.",
          },
          { status: 429 }
        );
      }
      throw error;
    } finally {
      // Clean up in-flight
      inFlightCache.delete(cacheKey);
    }
  })();

  inFlightCache.set(cacheKey, promise);
  return promise;
}

function buildContext(data: any) {
  return `
Repository: ${data.metadata.full_name}
Stars: ${data.metadata.stargazers_count}
Language: ${data.metadata.language}
Description: ${data.metadata.description}
Topics: ${data.metadata.topics?.join(", ")}

README:
${data.readme || "No README found"}

File Tree (depth 1):
${data.tree.map((item: any) => `- ${item.path}`).join("\n")}
  `.trim();
}
```

#### 2.6 LLM Integration (`lib/llm.ts`)

```typescript
import OpenAI from "openai";

export class LLMProvider {
  private client: OpenAI | null = null;
  private provider: "openrouter" | "google" = "openrouter";

  constructor() {
    if (process.env.OPENROUTER_API_KEY) {
      this.provider = "openrouter";
      this.client = new OpenAI({
        apiKey: process.env.OPENROUTER_API_KEY,
        baseURL: "https://openrouter.ai/api/v1",
      });
    } else if (process.env.GOOGLE_GENERATIVE_AI_API_KEY) {
      this.provider = "google";
      // Google AI Studio integration
    }
  }

  async generatePrompt(context: string, focus?: string): Promise<string> {
    const systemPrompt = `You are an expert software architect. Given a GitHub repository's metadata, README, and file tree, generate a concise (120-200 words) natural-language prompt that someone could paste into an AI coding assistant to recreate the project from scratch.

Focus on capturing the INTENT and ARCHITECTURE, not implementation details.
${focus ? `Special focus: ${focus}` : ""}`;

    const userPrompt = `Here is the repository context:\n\n${context}\n\nGenerate the prompt:`;

    if (this.provider === "openrouter") {
      const response = await this.client!.chat.completions.create({
        model: process.env.OPENROUTER_MODEL || "google/gemini-2.5-pro",
        messages: [
          { role: "system", content: systemPrompt },
          { role: "user", content: userPrompt },
        ],
        max_tokens: 500,
      });
      return response.choices[0].message.content || "";
    } else {
      // Google AI Studio implementation
      throw new Error("Google AI Studio not yet implemented");
    }
  }
}
```

#### 2.7 Custom Reverse Proxy (`app/api/custom/route.ts`)

```typescript
export async function POST(request: NextRequest) {
  const body = await request.json();
  const { url, focus } = body;

  // Check MD5 cache
  const focusHash = md5(focus);
  // ... cache check

  const serviceUrl = process.env.CUSTOM_REVERSE_SERVICE_URL || "http://localhost:3001";
  
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 15 * 60 * 1000); // 15 min

  try {
    const response = await fetch(`${serviceUrl}/api/custom-reverse`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ url, focus }),
      signal: controller.signal,
    });

    if (!response.ok) {
      if (response.status === 503) {
        return NextResponse.json(
          { 
            error: "Custom reverse service unavailable",
            message: "The custom reverse service is not running. Please start it and try again."
          },
          { status: 503 }
        );
      }
      throw new Error("Custom service error");
    }

    // Handle SSE streaming if supported
    if (response.headers.get("content-type")?.includes("text/event-stream")) {
      return new Response(response.body, {
        headers: { "Content-Type": "text/event-stream" },
      });
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error: any) {
    if (error.name === "AbortError") {
      return NextResponse.json(
        { error: "Request timed out after 15 minutes" },
        { status: 504 }
      );
    }
    throw error;
  } finally {
    clearTimeout(timeout);
  }
}
```

#### 2.8 Supabase Integration (`lib/supabase.ts`)

```typescript
import { createClient, SupabaseClient } from "@supabase/supabase-js";

let _client: SupabaseClient | null = null;

export function getSupabase() {
  if (!process.env.SUPABASE_URL || !process.env.SUPABASE_PUBLISHABLE_KEY) {
    return null; // Supabase not configured
  }

  if (!_client) {
    _client = createClient(
      process.env.SUPABASE_URL,
      process.env.SUPABASE_PUBLISHABLE_KEY
    );
  }

  return _client;
}

// Cache tables schema (to be created in Supabase):
/*
Table: quick_reverse_cache
- id: uuid (primary)
- repo_full_name: text
- prompt: text
- metadata: jsonb
- created_at: timestamp

Table: custom_reverse_cache
- id: uuid (primary)
- focus_hash: text (md5 of focus string)
- result: text
- created_at: timestamp

Table: view_counter
- ip_hash: text (sha256 of IP + salt)
- count: integer
- updated_at: timestamp
*/
```

#### 2.9 Components

**Navbar.tsx:**
```typescript
"use client";

export default function Navbar() {
  return (
    <nav className="sticky top-0 z-50 bg-background border-b-4 border-zinc-900">
      <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="text-2xl font-bold">
            Git<span className="text-accent">Reverse</span>
          </span>
        </div>
        <div className="flex items-center gap-6">
          <a href="/library" className="hover:text-accent">Library</a>
          <a href="/history" className="hover:text-accent">History</a>
          <a href="https://github.com/..." target="_blank" className="hover:text-accent">
            GitHub
          </a>
        </div>
      </div>
    </nav>
  );
}
```

**LoadingSpinner.tsx:**
```typescript
"use client";

export default function LoadingSpinner({ flavorText }: { flavorText: string }) {
  return (
    <div className="mt-12 text-center">
      {/* Animated spinner */}
      <div className="inline-block w-12 h-12 border-4 border-accent border-t-transparent rounded-full animate-spin"></div>
      {/* Rotating flavor text */}
      <p className="mt-4 text-lg text-gray-600 animate-pulse">{flavorText}</p>
    </div>
  );
}
```

**Library Page (`app/library/page.tsx`):**
```typescript
"use client";

import { useState, useEffect } from "react";
import { getSupabase } from "@/lib/supabase";
import LibraryCard from "@/components/LibraryCard";
import { useDebounce } from "@/hooks/useDebounce";

export default function LibraryPage() {
  const [items, setItems] = useState<any[]>([]);
  const [search, setSearch] = useState("");
  const [sort, setSort] = useState<"newest" | "trending" | "oldest">("newest");
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  const debouncedSearch = useDebounce(search, 300);

  useEffect(() => {
    loadItems();
  }, [debouncedSearch, sort]);

  async function loadItems() {
    const supabase = getSupabase();
    if (!supabase) {
      setItems([]); // No Supabase = empty library
      return;
    }

    let query = supabase
      .from("quick_reverse_cache")
      .select("*")
      .range(page * 24, (page + 1) * 24 - 1);

    if (debouncedSearch) {
      query = query.ilike("repo_full_name", `%${debouncedSearch}%`);
    }

    switch (sort) {
      case "newest":
        query = query.order("created_at", { ascending: false });
        break;
      case "oldest":
        query = query.order("created_at", { ascending: true });
        break;
      case "trending":
        // Would need view counts
        query = query.order("created_at", { ascending: false });
        break;
    }

    const { data } = await query;
    setItems(data || []);
    setHasMore((data || []).length === 24);
  }

  return (
    <main className="max-w-7xl mx-auto px-4 py-12">
      <h1 className="text-4xl font-bold mb-8">Prompt Library</h1>

      {/* Search and Sort */}
      <div className="flex gap-4 mb-8">
        <input
          type="text"
          placeholder="Search repositories..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="flex-1 px-4 py-2 border-4 border-zinc-900 rounded-lg bg-input-bg"
        />
        <select
          value={sort}
          onChange={(e) => setSort(e.target.value as any)}
          className="px-4 py-2 border-4 border-zinc-900 rounded-lg bg-input-bg"
        >
          <option value="newest">Newest</option>
          <option value="oldest">Oldest</option>
          <option value="trending">Trending</option>
        </select>
      </div>

      {/* Grid */}
      {items.length === 0 ? (
        <p className="text-center text-gray-500 py-12">
          {getSupabase() ? "No prompts yet. Be the first to create one!" : "Library unavailable (Supabase not configured)"}
        </p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {items.map((item) => (
            <LibraryCard key={item.id} item={item} />
          ))}
        </div>
      )}

      {/* Load More */}
      {hasMore && (
        <div className="text-center mt-8">
          <button
            onClick={() => setPage(p => p + 1)}
            className="px-6 py-2 bg-accent text-white font-bold border-4 border-zinc-900 rounded-lg hover:translate-x-1 hover:translate-y-1 transition-transform"
          >
            Load More
          </button>
        </div>
      )}
    </main>
  );
}
```

### 3. Environment Variables

Create `.env.example`:

```bash
# GitReverse

# LLM Provider (at least one required)
OPENROUTER_API_KEY=sk-or-v1-...
OPENROUTER_MODEL=google/gemini-2.5-pro
GOOGLE_GENERATIVE_AI_API_KEY=...
GOOGLE_AI_STUDIO_MODEL=gemini-2.5-pro

# GitHub
GITHUB_TOKEN=ghp_...  # Optional, for higher rate limits

# Supabase (all optional)
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_PUBLISHABLE_KEY=eyJ...
SUPABASE_SERVICE_ROLE_KEY=eyJ...  # Server-side only

# Custom Reverse Service
CUSTOM_REVERSE_SERVICE_URL=http://localhost:3001

# View Counter
VIEWS_IP_SALT=random-salt-string

# Next.js
NEXT_PUBLIC_APP_URL=http://localhost:3000
```

---

## Integration with SupremeAI

### 1. Adding to Monorepo Structure

```
supremeai/
├── src/main/java/com/supremeai/    # Existing Spring Boot backend
├── dashboard/                       # Existing React dashboard
├── gitingest/                       # NEW: Gitingest tool
├── gitreverse/                      # NEW: GitReverse tool
├── supremeai/                       # Existing Flutter app
└── ...
```

### 2. Integration Points

#### 2.1 Backend Integration (Spring Boot)

Add new controllers that proxy to the Gitingest/GitReverse services:

```java
// GitingestProxyController.java
@RestController
@RequestMapping("/api/v1/tools/gitingest")
public class GitingestProxyController {
    
    @Value("${gitingest.url:http://localhost:8000}")
    private String gitingestUrl;
    
    @PostMapping("/ingest")
    public ResponseEntity<?> proxyIngest(@RequestBody IngestRequest request) {
        // Proxy to Gitingest FastAPI service
    }
}
```

#### 2.2 Dashboard Integration

Add new navigation items in the React dashboard:

```typescript
// In dashboard navigation
const tools = [
  { name: "Gitingest", href: "/tools/gitingest", icon: FileTextIcon },
  { name: "GitReverse", href: "/tools/gitreverse", icon: GitBranchIcon },
];
```

#### 2.3 Shared Configuration

Add to SupremeAI's `application.yml`:

```yaml
tools:
  gitingest:
    url: ${GITINGEST_URL:http://localhost:8000}
    enabled: true
  gitreverse:
    url: ${GITREVERSE_URL:http://localhost:3000}
    enabled: true
```

---

## Implementation Timeline

### Phase 1: Foundation (Week 1)
- [ ] Set up Gitingest project structure
- [ ] Implement core parser and cloner modules
- [ ] Basic FastAPI server with `/health` endpoint
- [ ] Set up GitReverse Next.js project
- [ ] Configure Tailwind CSS and base layout

### Phase 2: Core Features (Week 2)
- [ ] Complete Gitingest ingestion pipeline
- [ ] Implement ignore pattern handling
- [ ] Add file reading with encoding fallback
- [ ] Build output formatter
- [ ] GitReverse GitHub API integration
- [ ] LLM provider integration (OpenRouter)

### Phase 3: API & CLI (Week 3)
- [ ] Gitingest FastAPI routes (`/api/ingest`, `/api/{user}/{repo}`)
- [ ] Rate limiting with slowapi
- [ ] Click CLI with all options
- [ ] Python package setup (`pyproject.toml`)
- [ ] GitReverse custom reverse endpoint
- [ ] In-flight request deduplication

### Phase 4: UI & UX (Week 4)
- [ ] Gitingest Jinja2 templates with Tailwind
- [ ] Interactive tree with click-to-exclude
- [ ] Copy-to-clipboard functionality
- [ ] GitReverse home page with loading states
- [ ] GitReverse library page with search/pagination
- [ ] History page (localStorage)

### Phase 5: Caching & Production (Week 5)
- [ ] S3/MinIO cache for Gitingest
- [ ] Local filesystem cache fallback
- [ ] Supabase integration for GitReverse
- [ ] Prompt library browsable UI
- [ ] View counter with IP hashing

### Phase 6: Testing & Deployment (Week 6)
- [ ] Unit tests for both tools
- [ ] Integration tests
- [ ] Load testing (Gitingest rate limits)
- [ ] Docker configurations
- [ ] Documentation (README, CONTRIBUTING)
- [ ] Deploy to staging/production

---

## Dependencies & Environment

### Gitingest Requirements

```
# Core
fastapi>=0.110.0
uvicorn[standard]>=0.27.0
pydantic>=2.0.0
pydantic-settings>=2.0.0

# Git operations
gitpython>=3.1.0
# OR use subprocess (no additional dep)

# LLM token counting
tiktoken>=0.5.0

# Caching
boto3>=1.34.0  # S3/MinIO
python-dotenv>=1.0.0

# CLI
click>=8.0.0

# Logging
loguru>=0.7.0

# Rate limiting
slowapi>=0.1.9

# Metrics
prometheus-client>=0.19.0

# Validation
# PAT validation via regex

# Templates
jinja2>=3.1.0

# Utilities
aiofiles>=23.0.0
httpx>=0.26.0
```

### GitReverse Requirements

```json
{
  "dependencies": {
    "next": "^14.0.0",
    "react": "^19.0.0",
    "react-dom": "^19.0.0",
    "@supabase/supabase-js": "^2.39.0",
    "openai": "^4.0.0",
    "geist": "^1.0.0"
  },
  "devDependencies": {
    "typescript": "^5.0.0",
    "tailwindcss": "^4.0.0",
    "postcss": "^8.0.0",
    "autoprefixer": "^10.0.0"
  }
}
```

### Environment Variables Summary

| Variable | Gitingest | GitReverse | Required |
|----------|-----------|------------|----------|
| `GITHUB_TOKEN` | ✓ | ✓ | No |
| `S3_ENABLED` | ✓ | - | No |
| `S3_ENDPOINT` | ✓ | - | No* |
| `S3_ACCESS_KEY` | ✓ | - | No* |
| `S3_SECRET_KEY` | ✓ | - | No* |
| `S3_BUCKET_NAME` | ✓ | - | No* |
| `OPENROUTER_API_KEY` | - | ✓ | No** |
| `GOOGLE_GENERATIVE_AI_API_KEY` | - | ✓ | No** |
| `SUPABASE_URL` | - | ✓ | No |
| `SUPABASE_PUBLISHABLE_KEY` | - | ✓ | No |
| `CUSTOM_REVERSE_SERVICE_URL` | - | ✓ | No |
| `VIEWS_IP_SALT` | - | ✓ | No |
| `LOG_FORMAT` | ✓ | - | No |
| `LOG_LEVEL` | ✓ | - | No |
| `SENTRY_DSN` | ✓ | - | No |
| `ALLOWED_HOSTS` | ✓ | - | No |

*Required if `S3_ENABLED=true`
**At least one LLM provider required

---

## Testing Strategy

### Gitingest Tests

```python
# tests/test_parser.py
def test_parse_github_url():
    url = "https://github.com/fastapi/fastapi"
    result = parse_url(url)
    assert result.host == GitHost.GITHUB
    assert result.user == "fastapi"
    assert result.repo == "fastapi"

def test_parse_githubingest_url():
    url = "https://githubingest.com/fastapi/fastapi"
    result = parse_url(url)
    assert result.user == "fastapi"
    assert result.repo == "fastapi"

# tests/test_cloner.py
@pytest.mark.asyncio
async def test_clone_public_repo():
    repo_info = RepoInfo(
        host=GitHost.GITHUB,
        user="fastapi",
        repo="fastapi",
    )
    clone_dir = await clone_repo(repo_info, timeout=30)
    assert (clone_dir / "fastapi").exists() or (clone_dir / "README.md").exists()
    shutil.rmtree(clone_dir)

# tests/test_ignore.py
def test_default_ignore_patterns():
    patterns = DEFAULT_IGNORE_PATTERNS
    assert "node_modules/" in patterns
    assert "*.pyc" in patterns

# tests/test_formatter.py
def test_format_digest():
    # Create temp files
    # Run formatter
    # Assert output structure
    pass
```

### GitReverse Tests

```typescript
// tests/api/reverse.test.ts
import { POST } from "@/app/api/reverse/route";

describe("/api/reverse", () => {
  it("should generate prompt for valid repo", async () => {
    const request = new NextRequest("http://localhost/api/reverse", {
      method: "POST",
      body: JSON.stringify({ url: "https://github.com/fastapi/fastapi" }),
    });
    
    const response = await POST(request);
    const data = await response.json();
    
    expect(response.status).toBe(200);
    expect(data.prompt).toBeDefined();
    expect(data.prompt.length).toBeGreaterThan(100);
  });

  it("should return 429 on rate limit", async () => {
    // Mock LLM rate limit error
  });
});
```

---

## Deployment Considerations

### Docker Compose (for local development)

```yaml
version: '3.8'

services:
  gitingest:
    build: ./gitingest
    ports:
      - "8000:8000"
      - "9090:9090"  # Prometheus metrics
    environment:
      - GITHUB_TOKEN=${GITHUB_TOKEN}
      - S3_ENABLED=false
      - LOG_FORMAT=text
    volumes:
      - ./gitingest:/app

  gitreverse:
    build: ./gitreverse
    ports:
      - "3000:3000"
    environment:
      - OPENROUTER_API_KEY=${OPENROUTER_API_KEY}
      - GITHUB_TOKEN=${GITHUB_TOKEN}
      - NEXT_PUBLIC_APP_URL=http://localhost:3000
    volumes:
      - ./gitreverse:/app

  # Optional: MinIO for S3 testing
  minio:
    image: minio/minio
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      - MINIO_ROOT_USER=minioadmin
      - MINIO_ROOT_PASSWORD=minioadmin
    command: server /data --console-address ":9001"

  # Optional: Supabase (use Supabase CLI or cloud)
```

### Kubernetes Deployment (production)

```yaml
# gitingest-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gitingest
spec:
  replicas: 2
  selector:
    matchLabels:
      app: gitingest
  template:
    metadata:
      labels:
        app: gitingest
    spec:
      containers:
      - name: gitingest
        image: supremeai/gitingest:latest
        ports:
        - containerPort: 8000
        - containerPort: 9090
        env:
        - name: S3_ENABLED
          value: "true"
        - name: S3_ENDPOINT
          valueFrom:
            secretKeyRef:
              name: s3-secrets
              key: endpoint
        # ... other env vars
```

---

## Success Criteria

### Gitingest
- [ ] `pip install gitingest` works and provides CLI
- [ ] `gitingest https://github.com/fastapi/fastapi` produces valid digest
- [ ] POST `/api/ingest` returns valid JSON with summary, tree, content
- [ ] Rate limiting works (10 req/min)
- [ ] S3 caching works when enabled
- [ ] UI renders correctly with all interactive features
- [ ] Prometheus metrics available on `/metrics`

### GitReverse
- [ ] Home page renders with correct styling
- [ ] Submitting repo URL generates prompt (120-200 words)
- [ ] Library page works (with or without Supabase)
- [ ] History page shows last 20 entries from localStorage
- [ ] Repo detail page auto-submits if no cache
- [ ] Custom reverse proxies to backend service
- [ ] Loading spinner shows rotating flavor text
- [ ] In-flight request deduplication works

---

## Next Steps

1. **Review this plan** with the team
2. **Set up project scaffolding** for both tools
3. **Start with Gitingest core** (parser, cloner, walker)
4. **Parallel work**: One developer on GitReverse while another finishes Gitingest
5. **Integration testing** with SupremeAI backend
6. **Deploy to staging** for user acceptance testing

---

**Document Version**: 1.0  
**Created**: 2026-05-06  
**Last Updated**: 2026-05-06
