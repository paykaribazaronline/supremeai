"""
Utilities for Gitingest
"""
import sys
import logging
from pathlib import Path
from typing import Optional


# ============================================================================
# Logging Setup with Loguru
# ============================================================================

def setup_logging(
    log_format: str = "json",
    log_level: str = "INFO",
    sentry_dsn: Optional[str] = None,
    posthog_key: Optional[str] = None,
):
    """
    Setup logging with Loguru.
    
    Args:
        log_format: 'json' for production, 'text' for development
        log_level: Logging level (DEBUG, INFO, WARNING, ERROR)
        sentry_dsn: Optional Sentry DSN
        posthog_key: Optional PostHog API key
    """
    try:
        from loguru import logger
        import sys
        
        # Remove default handler
        logger.remove()
        
        if log_format == "json":
            # JSON format for production
            logger.add(
                sys.stdout,
                format="{time:YYYY-MM-DD HH:mm:ss} {level} {message}",
                level=log_level,
                serialize=True,  # Output as JSON
            )
        else:
            # Human-readable for development
            logger.add(
                sys.stdout,
                format="<green>{time:YYYY-MM-DD HH:mm:ss}</green> | <level>{level: <8}</level> | <cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - <level>{message}</level>",
                level=log_level,
                colorize=True,
            )
        
        # Intercept standard logging
        _intercept_standard_logging()
        
        # Initialize Sentry (stub if not configured)
        if sentry_dsn:
            _init_sentry(sentry_dsn)
        
        # Initialize PostHog (stub if not configured)
        if posthog_key:
            _init_posthog(posthog_key)
            
        return logger
        
    except ImportError:
        # Loguru not installed, fall back to standard logging
        logging.basicConfig(
            level=getattr(logging, log_level),
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        )
        return logging.getLogger(__name__)


def _intercept_standard_logging():
    """Intercept standard logging and redirect to Loguru"""
    try:
        from loguru import logger
        
        class InterceptHandler(logging.Handler):
            def emit(self, record):
                # Get corresponding Loguru level
                try:
                    level = logger.level(record.levelname).name
                except ValueError:
                    level = record.levelno
                
                # Find caller
                frame, depth = logging.currentframe(), 2
                while frame and frame.f_code.co_filename == logging.__file__:
                    frame = frame.f_back
                    depth += 1
                
                logger.opt(depth=depth, exception=record.exc_info).log(
                    level, record.getMessage()
                )
        
        logging.basicConfig(handlers=[InterceptHandler()], level=0)
        
    except ImportError:
        pass


def _init_sentry(dsn: str):
    """Initialize Sentry (stub implementation)"""
    try:
        import sentry_sdk
        sentry_sdk.init(
            dsn=dsn,
            environment="production",
        )
    except ImportError:
        print("Warning: sentry-sdk not installed. Sentry disabled.")
    except Exception as e:
        print(f"Warning: Sentry initialization failed: {e}")


def _init_posthog(api_key: str):
    """Initialize PostHog (stub implementation)"""
    try:
        from posthog import Posthog
        posthog = Posthog(
            api_key,
            host='https://app.posthog.com',
        )
    except ImportError:
        print("Warning: posthog-python not installed. PostHog disabled.")
    except Exception as e:
        print(f"Warning: PostHog initialization failed: {e}")


# ============================================================================
# Token Counter (Tiktoken)
# ============================================================================

def estimate_tokens(text: str) -> int:
    """
    Estimate token count using tiktoken.
    
    Args:
        text: Text to count tokens for
        
    Returns:
        Estimated token count
    """
    try:
        import tiktoken
        encoding = tiktoken.get_encoding("cl100k_base")
        return len(encoding.encode(text))
    except ImportError:
        # Fallback: approximate 1 token per 4 characters
        return len(text) // 4


# ============================================================================
# Git Utilities
# ============================================================================

def get_git_info(repo_path: Path) -> dict:
    """
    Get git repository information.
    
    Args:
        repo_path: Path to git repository
        
    Returns:
        Dictionary with git info (branch, commit, etc.)
    """
    import subprocess
    
    info = {
        'branch': None,
        'commit': None,
        'remote_url': None,
    }
    
    try:
        # Get current branch
        result = subprocess.run(
            ["git", "branch", "--show-current"],
            cwd=str(repo_path),
            capture_output=True,
            text=True,
        )
        if result.returncode == 0:
            info['branch'] = result.stdout.strip()
        
        # Get current commit
        result = subprocess.run(
            ["git", "rev-parse", "HEAD"],
            cwd=str(repo_path),
            capture_output=True,
            text=True,
        )
        if result.returncode == 0:
            info['commit'] = result.stdout.strip()
        
        # Get remote URL
        result = subprocess.run(
            ["git", "remote", "get-url", "origin"],
            cwd=str(repo_path),
            capture_output=True,
            text=True,
        )
        if result.returncode == 0:
            info['remote_url'] = result.stdout.strip()
            
    except Exception:
        pass
    
    return info


# ============================================================================
# Validators
# ============================================================================

def validate_github_pat(token: str) -> bool:
    """
    Validate GitHub Personal Access Token format.
    
    Args:
        token: PAT to validate
        
    Returns:
        True if valid format
    """
    import re
    
    pat_patterns = [
        r'^ghp_[a-zA-Z0-9]{36,}$',        # Fine-grained PAT
        r'^gho_[a-zA-Z0-9]{36,}$',        # OAuth access token
        r'^ghu_[a-zA-Z0-9]{36,}$',        # GitHub App user-to-server
        r'^ghs_[a-zA-Z0-9]{36,}$',        # GitHub App server-to-server
        r'^ghr_[a-zA-Z0-9]{36,}$',        # Refresh token
        r'^github_pat_[a-zA-Z0-9_]{82,}$', # New format PAT
    ]
    
    return any(re.match(pattern, token) for pattern in pat_patterns)


# ============================================================================
# File Utilities
# ============================================================================

def format_file_size(size_bytes: int) -> str:
    """
    Format byte size to human readable string.
    
    Args:
        size_bytes: Size in bytes
        
    Returns:
        Formatted string (e.g., "1.5 MB")
    """
    for unit in ['B', 'KB', 'MB', 'GB']:
        if size_bytes < 1024:
            return f"{size_bytes:.2f} {unit}"
        size_bytes /= 1024
    return f"{size_bytes:.2f} TB"


def get_file_extension_stats(files: list) -> dict:
    """
    Get statistics on file extensions.
    
    Args:
        files: List of file paths
        
    Returns:
        Dictionary with extension counts
    """
    stats = {}
    for f in files:
        ext = f.suffix or '(no extension)'
        stats[ext] = stats.get(ext, 0) + 1
    return stats
