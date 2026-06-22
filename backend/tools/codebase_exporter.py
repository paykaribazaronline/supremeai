import os
import asyncio
from typing import List

IGNORE_DIRS = {
    '.git', 'node_modules', 'venv', 'env', '__pycache__',
    'dist', 'build', '.next', '.vscode', '.idea', 'coverage', '.github', '.venv'
}

IGNORE_EXTS = {
    '.png', '.jpg', '.jpeg', '.gif', '.ico', '.svg', '.webp',
    '.pdf', '.zip', '.tar', '.gz', '.mp4', '.mp3',
    '.pyc', '.pyo', '.pyd', '.so', '.dll', '.exe',
    '.woff', '.woff2', '.ttf', '.eot', '.log'
}

IGNORE_FILES = {
    'package-lock.json', 'yarn.lock', 'pnpm-lock.yaml', 'poetry.lock'
}

def get_language(file_name: str) -> str:
    ext = os.path.splitext(file_name)[1].lower()
    mapping = {
        '.py': 'python', '.js': 'javascript', '.jsx': 'jsx',
        '.ts': 'typescript', '.tsx': 'tsx', '.json': 'json',
        '.html': 'html', '.css': 'css', '.scss': 'scss',
        '.md': 'markdown', '.yml': 'yaml', '.yaml': 'yaml',
        '.sh': 'bash', 'Dockerfile': 'dockerfile'
    }
    return mapping.get(ext, 'text')

def _collect_files(root_dir: str, follow_symlinks: bool = False) -> List[str]:
    files = []
    for dirpath, dirnames, filenames in os.walk(root_dir, followlinks=follow_symlinks):
        dirnames[:] = [d for d in dirnames if d not in IGNORE_DIRS and not d.startswith('.')]
        for file in filenames:
            if file in IGNORE_FILES or file.startswith('.'):
                continue
            ext = os.path.splitext(file)[1].lower()
            if ext in IGNORE_EXTS:
                continue
            rel = os.path.relpath(os.path.join(dirpath, file), root_dir)
            if rel == "supremeai_full_codebase.md" or file == "code_to_md.py":
                continue
            files.append(os.path.join(dirpath, file))
    return files

def _chunk_lines(content: str, chunk_size: int = 200000) -> List[str]:
    if len(content) <= chunk_size:
        return [content]
    chunks = []
    start = 0
    while start < len(content):
        end = start + chunk_size
        chunk = content[start:end]
        if end < len(content):
            nl = chunk.rfind('\n')
            if nl != -1:
                end = start + nl + 1
                chunk = content[start:end]
        chunks.append(chunk)
        start = end
    return [c for c in chunks if c.strip()]

async def export_file_async(file_path: str, root_dir: str) -> str:
    rel = os.path.relpath(file_path, root_dir)
    language = get_language(os.path.basename(file_path))
    try:
        loop = asyncio.get_running_loop()
        content = await loop.run_in_executor(None, _read_file, file_path)
    except Exception as exc:
        return f"### File: `{rel}` (read error: {exc})\n\n"
    parts = [f"### File: `{rel}`\n\n```{language}\n"]
    for idx, chunk in enumerate(_chunk_lines(content)):
        if idx > 0:
            parts.append(f"\n...[truncated chunk {idx+1}]\n")
        parts.append(chunk)
        if not chunk.endswith('\n'):
            parts.append('\n')
    parts.append("```\n\n")
    return "".join(parts)

def _read_file(path: str) -> str:
    with open(path, 'r', encoding='utf-8', errors='ignore') as f:
        return f.read()

async def export_codebase_to_markdown(root_dir: str = ".", max_concurrency: int = 8) -> str:
    root_dir = os.path.abspath(root_dir)
    files = _collect_files(root_dir)
    semaphore = asyncio.Semaphore(max_concurrency)
    async def bounded(path):
        async with semaphore:
            return await export_file_async(path, root_dir)
    chunks = await asyncio.gather(*(bounded(p) for p in files))
    return "# 🔱 SupremeAI 2.0 - Full Codebase\n\n" + "".join(chunks)
