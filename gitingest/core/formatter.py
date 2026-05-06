"""
Output formatter for creating LLM-friendly text digests
"""
from pathlib import Path
from typing import List, Dict
from dataclasses import dataclass

from ..models import RepoInfo, IngestResponse
from .walker import sort_files_for_tree


@dataclass
class DigestOutput:
    """Complete digest output"""
    summary: str
    tree: str
    content: str
    file_count: int
    estimated_tokens: int
    repo_name: str
    branch: str = "main"


def format_digest(
    repo_info: RepoInfo,
    files: List[Path],
    repo_path: Path,
    symlinks: Dict[Path, str],
    total_size: int,
) -> DigestOutput:
    """
    Format the complete digest output.
    
    Args:
        repo_info: Repository information
        files: List of file paths
        repo_path: Path to cloned repository
        symlinks: Dictionary of symlinks (path -> target)
        total_size: Total size of all files
        
    Returns:
        DigestOutput with formatted content
    """
    # Build summary
    summary = _build_summary(repo_info, files, total_size)
    
    # Build tree
    tree = _build_tree(files, repo_path, symlinks)
    
    # Build content
    content = _build_content(files, repo_path)
    
    # Estimate tokens
    estimated_tokens = _estimate_tokens(content)
    
    return DigestOutput(
        summary=summary,
        tree=tree,
        content=content,
        file_count=len(files),
        estimated_tokens=estimated_tokens,
        repo_name=repo_info.full_name,
        branch=repo_info.branch or "main",
    )


def _build_summary(
    repo_info: RepoInfo,
    files: List[Path],
    total_size: int,
) -> str:
    """Build summary section"""
    lines = [
        f"Repository: {repo_info.full_name}",
        f"Branch/Commit: {repo_info.branch or 'main'}",
        f"Files analyzed: {len(files)}",
        f"Total size: {_format_size(total_size)}",
    ]
    
    if repo_info.subpath:
        lines.append(f"Subpath: {repo_info.subpath}")
    
    # Count file types
    extensions = {}
    for f in files:
        ext = f.suffix or '(no extension)'
        extensions[ext] = extensions.get(ext, 0) + 1
    
    if extensions:
        lines.append("\nFile types:")
        for ext, count in sorted(extensions.items(), key=lambda x: -x[1])[:10]:
            lines.append(f"  {ext}: {count}")
    
    return '\n'.join(lines)


def _build_tree(
    files: List[Path],
    repo_path: Path,
    symlinks: Dict[Path, str],
) -> str:
    """
    Build indented directory tree.
    
    README first, then files, then hidden files, then dirs.
    """
    # Sort files for display
    sorted_files = sort_files_for_tree(files, repo_path)
    
    # Build tree structure
    tree_lines = []
    tree_lines.append(f"{repo_info.full_name}/")
    
    # Track directories we've added
    added_dirs = set()
    
    for filepath in sorted_files:
        rel_path = filepath.relative_to(repo_path)
        parts = rel_path.parts
        
        # Add directories leading to this file
        for i in range(len(parts) - 1):
            dir_path = '/'.join(parts[:i+1])
            if dir_path not in added_dirs:
                added_dirs.add(dir_path)
                indent = '  ' * i
                tree_lines.append(f"{indent}{parts[i]}/")
        
        # Add the file
        indent = '  ' * (len(parts) - 1)
        tree_lines.append(f"{indent}{parts[-1]}")
    
    # Add symlinks
    if symlinks:
        tree_lines.append("\nSymlinks:")
        for sym_path, target in symlinks.items():
            rel_path = sym_path.relative_to(repo_path)
            tree_lines.append(f"  {rel_path} -> {target}")
    
    return '\n'.join(tree_lines)


def _build_content(
    files: List[Path],
    repo_path: Path,
) -> str:
    """
    Build file contents with separators.
    """
    parts = []
    
    # Add header
    parts.append("=" * 80)
    parts.append("REPOSITORY DIGEST")
    parts.append("=" * 80)
    parts.append("")
    
    for filepath in sorted(files, key=lambda f: f.relative_to(repo_path)):
        rel_path = filepath.relative_to(repo_path)
        
        # Add file separator
        parts.append("")
        parts.append("=" * 80)
        parts.append(f"File: {rel_path}")
        parts.append("=" * 80)
        parts.append("")
        
        # Process Jupyter notebooks specially
        if filepath.suffix == '.ipynb':
            from .reader import process_jupyter_notebook
            content = process_jupyter_notebook(filepath)
        else:
            from .reader import read_file_safe
            content, encoding = read_file_safe(filepath)
            if encoding == 'decode-error':
                content = "[Binary file or encoding error]"
        
        parts.append(content)
    
    return '\n'.join(parts)


def _estimate_tokens(text: str) -> int:
    """
    Estimate token count using tiktoken.
    
    Args:
        text: Text to count tokens for
        
    Returns:
        Estimated token count (or character count / 4 as fallback)
    """
    try:
        import tiktoken
        encoding = tiktoken.get_encoding("cl100k_base")
        return len(encoding.encode(text))
    except ImportError:
        # Fallback: approximate 1 token per 4 characters
        return len(text) // 4


def _format_size(size_bytes: int) -> str:
    """Format byte size to human readable string"""
    for unit in ['B', 'KB', 'MB', 'GB']:
        if size_bytes < 1024:
            return f"{size_bytes:.2f} {unit}"
        size_bytes /= 1024
    return f"{size_bytes:.2f} TB"


def to_response(digest: DigestOutput) -> IngestResponse:
    """
    Convert DigestOutput to IngestResponse model.
    
    Args:
        digest: DigestOutput instance
        
    Returns:
        IngestResponse model
    """
    from ..models import IngestResponse
    return IngestResponse(
        summary=digest.summary,
        tree=digest.tree,
        content=digest.content,
        file_count=digest.file_count,
        estimated_tokens=digest.estimated_tokens,
        repo_name=digest.repo_name,
        branch=digest.branch,
    )
