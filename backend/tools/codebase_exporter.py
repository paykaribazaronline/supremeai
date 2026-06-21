import os

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

def export_codebase_to_markdown(root_dir: str = ".") -> str:
    """
    Scans the codebase starting from root_dir, filters binaries and lockfiles, 
    and returns a structured Markdown string containing all code.
    """
    markdown_parts = ["# 🔱 SupremeAI 2.0 - Full Codebase\n\n"]
    
    for root, dirs, files in os.walk(root_dir):
        # Filter directories in-place to avoid traversing ignored folders
        dirs[:] = [d for d in dirs if d not in IGNORE_DIRS and not d.startswith('.')]

        for file in files:
            if file in IGNORE_FILES or file.startswith('.'):
                continue

            ext = os.path.splitext(file)[1].lower()
            if ext in IGNORE_EXTS:
                continue

            file_path = os.path.join(root, file)
            rel_path = os.path.relpath(file_path, root_dir)

            if rel_path == "supremeai_full_codebase.md" or file == "code_to_md.py":
                continue

            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()

                if content.strip():
                    lang = get_language(file)
                    markdown_parts.append(f"### File: `{rel_path}`\n\n")
                    markdown_parts.append(f"```{lang}\n")
                    markdown_parts.append(content)
                    if not content.endswith('\n'):
                        markdown_parts.append("\n")
                    markdown_parts.append("```\n\n")
            except Exception:
                # Silently skip read errors to avoid crashing during prompt construction
                continue
                
    return "".join(markdown_parts)
