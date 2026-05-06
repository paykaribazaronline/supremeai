"""
Ignore pattern handling for .gitignore, .gitingestignore, and default patterns
"""
import fnmatch
from pathlib import Path
from typing import List, Set


# Default ignore patterns for common languages and tools
DEFAULT_IGNORE_PATTERNS = [
    # Python
    "*.pyc", "__pycache__/", "*.egg-info/", "dist/", "build/",
    ".pytest_cache/", ".venv/", "venv/", "env/",
    "*.egg", "*.whl", "pip-log.txt", "pip-delete-this-directory.txt",
    ".coverage", ".tox/", ".nox/", "coverage.xml", "*.cover",
    ".python-version", "celerybeat-schedule", "*.sage.py",
    
    # JavaScript/TypeScript
    "node_modules/", "npm-debug.log", "yarn-error.log", "yarn-debug.log",
    ".next/", "out/", ".nuxt/", ".output/", "dist/", "build/",
    ".vercel/", ".turbo/", ".eslintcache", ".stylelintcache",
    "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
    
    # Java
    "*.class", "*.jar", "*.war", "*.ear",
    "target/", ".gradle/", "build/", ".settings/",
    ".classpath", "*.project", ".factorypath",
    
    # General build artifacts
    "bin/", "obj/", "out/", "build/", "dist/",
    "*.o", "*.so", "*.dylib", "*.dll", "*.exe",
    
    # IDE
    ".idea/", ".vscode/", "*.swp", "*.swo", "*~",
    ".project", ".classpath", ".settings",
    "*.iml", ".DS_Store", "Thumbs.db",
    
    # Git
    ".git/", ".svn/", ".hg/", ".gitignore",
    
    # Logs and temp files
    "*.log", "*.tmp", "*.temp", "*.cache", "*.swp",
    ".DS_Store", "Thumbs.db", "desktop.ini",
    
    # Documentation (often large, not useful for code analysis)
    "docs/_build/", "site/", ".docusaurus/",
    
    # Media files
    "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.ico",
    "*.mp3", "*.mp4", "*.avi", "*.mov", "*.wmv",
    "*.pdf", "*.zip", "*.tar", "*.gz", "*.bz2", "*.xz",
    "*.rar", "*.7z",
    
    # Database
    "*.db", "*.sqlite", "*.sqlite3", "*.sql",
    
    # Environment and config
    ".env", ".env.local", ".env.*.local",
    "*.pem", "*.key", "*.cert", "*.p12", "*.pfx",
]

# Patterns that should never be ignored (essential files)
NEVER_IGNORE = [
    "README*",
    "LICENSE*",
    "requirements.txt",
    "package.json",
    "pom.xml",
    "build.gradle*",
    ".gitignore",
    "Dockerfile*",
]


def load_ignore_patterns(repo_path: Path) -> List[str]:
    """
    Load all ignore patterns from repo and defaults.
    
    Args:
        repo_path: Path to repository root
        
    Returns:
        List of ignore patterns
    """
    patterns = list(DEFAULT_IGNORE_PATTERNS)
    
    # Load .gitignore if present
    gitignore_path = repo_path / ".gitignore"
    if gitignore_path.exists():
        patterns.extend(_parse_ignore_file(gitignore_path))
    
    # Load .gitingestignore if present
    gitingestignore_path = repo_path / ".gitingestignore"
    if gitingestignore_path.exists():
        patterns.extend(_parse_ignore_file(gitingestignore_path))
    
    return patterns


def _parse_ignore_file(file_path: Path) -> List[str]:
    """
    Parse an ignore file (.gitignore style).
    
    Args:
        file_path: Path to ignore file
        
    Returns:
        List of patterns from file
    """
    patterns = []
    try:
        content = file_path.read_text(encoding='utf-8', errors='ignore')
        for line in content.splitlines():
            line = line.strip()
            # Skip empty lines and comments
            if not line or line.startswith('#'):
                continue
            patterns.append(line)
    except Exception:
        pass
    return patterns


def is_ignored(
    path: Path,
    patterns: List[str],
    repo_root: Path,
) -> bool:
    """
    Check if a path matches any ignore pattern.
    
    Args:
        path: Path to check
        patterns: List of ignore patterns
        repo_root: Repository root path
        
    Returns:
        True if path should be ignored
    """
    # Get relative path from repo root
    try:
        rel_path = path.relative_to(repo_root)
    except ValueError:
        return True  # Outside repo, ignore
    
    rel_path_str = str(rel_path)
    path_name = path.name
    
    # Check never-ignore patterns first
    for pattern in NEVER_IGNORE:
        if fnmatch.fnmatch(path_name, pattern) or fnmatch.fnmatch(rel_path_str, pattern):
            return False
    
    # Check ignore patterns
    for pattern in patterns:
        # Handle directory patterns (ending with /)
        if pattern.endswith('/'):
            dir_pattern = pattern.rstrip('/')
            if path.is_dir() and (
                fnmatch.fnmatch(path_name, dir_pattern) or 
                fnmatch.fnmatch(rel_path_str, dir_pattern) or
                fnmatch.fnmatch(rel_path_str, pattern)
            ):
                return True
        else:
            # File pattern
            if (fnmatch.fnmatch(path_name, pattern) or 
                fnmatch.fnmatch(rel_path_str, pattern)):
                return True
    
    return False


def filter_paths(
    paths: List[Path],
    patterns: List[str],
    repo_root: Path,
    include: bool = False,
) -> List[Path]:
    """
    Filter paths based on patterns.
    
    Args:
        paths: List of paths to filter
        patterns: List of patterns
        repo_root: Repository root path
        include: If True, keep only matching; if False, exclude matching
        
    Returns:
        Filtered list of paths
    """
    if not patterns:
        return paths
    
    result = []
    for path in paths:
        matches = is_ignored(path, patterns, repo_root)
        if include:
            # Keep only if matches
            if matches:
                result.append(path)
        else:
            # Keep only if doesn't match
            if not matches:
                result.append(path)
    
    return result
