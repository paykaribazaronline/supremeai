"""
Directory walker with limits enforcement
"""
import os
from pathlib import Path
from typing import List, Dict, Tuple
from dataclasses import dataclass, field

from ..config import get_settings


@dataclass
class WalkResult:
    """Result of directory walk operation"""
    files: List[Path] = field(default_factory=list)
    symlinks: Dict[Path, str] = field(default_factory=dict)
    total_size: int = 0
    skipped_count: int = 0
    skipped_large_files: int = 0
    skipped_depth: int = 0


def walk_directory(
    root: Path,
    ignore_patterns: List[str],
    repo_root: Path,
    include_patterns: List[str] = None,
    exclude_patterns: List[str] = None,
    max_depth: int = None,
    max_files: int = None,
    max_total_size: int = None,
    max_file_size: int = None,
) -> WalkResult:
    """
    Walk directory recursively with limits and pattern matching.
    
    Args:
        root: Root directory to walk
        ignore_patterns: List of ignore patterns
        repo_root: Repository root (for relative path calculation)
        include_patterns: Optional include patterns (only include matching)
        exclude_patterns: Optional exclude patterns (exclude matching)
        max_depth: Maximum directory depth (default from settings)
        max_files: Maximum number of files (default from settings)
        max_total_size: Maximum total size in bytes (default from settings)
        max_file_size: Maximum single file size in bytes (default from settings)
        
    Returns:
        WalkResult with files, symlinks, and statistics
    """
    settings = get_settings()
    max_depth = max_depth if max_depth is not None else settings.MAX_DIR_DEPTH
    max_files = max_files if max_files is not None else settings.MAX_FILES
    max_total_size = max_total_size if max_total_size is not None else settings.MAX_TOTAL_SIZE
    max_file_size = max_file_size if max_file_size is not None else settings.MAX_FILE_SIZE
    
    result = WalkResult()
    
    # Convert patterns to list if needed
    if include_patterns is None:
        include_patterns = []
    if exclude_patterns is None:
        exclude_patterns = []
    
    # Use os.walk for efficient traversal
    for dirpath, dirnames, filenames in os.walk(root, followlinks=False):
        current_path = Path(dirpath)
        
        # Check depth
        try:
            rel_path = current_path.relative_to(repo_root)
            depth = len(rel_path.parts)
        except ValueError:
            depth = 0
        
        if depth > max_depth:
            result.skipped_depth += len(list(current_path.iterdir()))
            dirnames.clear()  # Don't go deeper
            continue
        
        # Filter directories in-place (modifies dirnames for os.walk)
        dirnames[:] = [
            d for d in dirnames
            if not _is_path_ignored(
                current_path / d,
                ignore_patterns,
                repo_root,
            )
        ]
        
        # Process files
        for filename in filenames:
            filepath = current_path / filename
            
            # Skip if ignored
            if _is_path_ignored(filepath, ignore_patterns, repo_root):
                continue
            
            # Apply include patterns (if specified, only keep matches)
            if include_patterns:
                if not _matches_any_pattern(filepath, include_patterns, repo_root):
                    continue
            
            # Apply exclude patterns
            if exclude_patterns:
                if _matches_any_pattern(filepath, exclude_patterns, repo_root):
                    continue
            
            # Handle symlinks
            if filepath.is_symlink():
                try:
                    target = os.readlink(filepath)
                    result.symlinks[filepath] = target
                except OSError:
                    pass
                continue
            
            # Check if it's a file
            if not filepath.is_file():
                continue
            
            # Check file size
            try:
                file_size = filepath.stat().st_size
            except OSError:
                continue
            
            if file_size > max_file_size:
                result.skipped_large_files += 1
                result.skipped_count += 1
                continue
            
            # Check total size limit
            if result.total_size + file_size > max_total_size:
                result.skipped_count += 1
                continue
            
            # Check file count limit
            if len(result.files) >= max_files:
                result.skipped_count += 1
                continue
            
            result.files.append(filepath)
            result.total_size += file_size
    
    return result


def _is_path_ignored(
    path: Path,
    patterns: List[str],
    repo_root: Path,
) -> bool:
    """Check if a path matches any ignore pattern"""
    from .ignore import is_ignored
    return is_ignored(path, patterns, repo_root)


def _matches_any_pattern(
    path: Path,
    patterns: List[str],
    repo_root: Path,
) -> bool:
    """Check if path matches any of the given patterns"""
    import fnmatch
    
    try:
        rel_path = path.relative_to(repo_root)
    except ValueError:
        rel_path = path
    
    rel_path_str = str(rel_path)
    name = path.name
    
    for pattern in patterns:
        if fnmatch.fnmatch(name, pattern) or fnmatch.fnmatch(rel_path_str, pattern):
            return True
    
    return False


def sort_files_for_tree(files: List[Path], repo_root: Path) -> List[Path]:
    """
    Sort files for tree display: README first, then files, hidden files, directories.
    
    Args:
        files: List of file paths
        repo_root: Repository root path
        
    Returns:
        Sorted list of paths
    """
    def sort_key(path: Path):
        name = path.name.lower()
        rel_path = str(path.relative_to(repo_root)).lower()
        
        # README first
        if 'readme' in name:
            return (0, rel_path)
        
        # Hidden files next
        if name.startswith('.'):
            return (2, rel_path)
        
        # Regular files
        return (1, rel_path)
    
    return sorted(files, key=sort_key)
