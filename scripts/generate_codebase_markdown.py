import os
from pathlib import Path

def generate_markdown():
    root = Path(".")
    output_file = Path("docs/codebase_dump.md")
    output_file.parent.mkdir(parents=True, exist_ok=True)
    
    # Exclude directories
    exclude_dirs = {
        ".git", "node_modules", ".venv", "dist", "build", 
        "__pycache__", ".next", ".turbo", ".idea", ".vscode", 
        "artifacts", "brain", ".agents"
    }
    
    # Allowed file extensions
    allowed_extensions = {
        ".py", ".ts", ".tsx", ".css", ".html", ".js", ".jsx", ".json", ".sh", ".ps1", ".yml", ".yaml"
    }
    
    # Excluded specific files (lock files or very large configuration assets)
    exclude_files = {
        "pnpm-lock.yaml", "package-lock.json", "poetry.lock", "codebase_dump.md"
    }
    
    markdown_lines = [
        "# 🧠 SupremeAI 2.0 Codebase Analysis\n",
        "# বাংলা মন্তব্য: এটি একটি স্বয়ংক্রিয়ভাবে জেনারেট করা কোডবেস ডাম্প ফাইল যা প্রজেক্টের সামগ্রিক বিশ্লেষণের জন্য ব্যবহৃত হয়।\n\n"
    ]
    markdown_lines.append(f"Generated at: {__import__('datetime').datetime.utcnow().isoformat()} UTC\n\n")
    
    for path in sorted(root.rglob("*")):
        if path.is_file():
            # Check exclusions
            parts = path.parts
            if any(exclude in parts for exclude in exclude_dirs):
                continue
            
            if path.name in exclude_files:
                continue
            
            # Check extension
            if path.suffix not in allowed_extensions:
                continue
                
            try:
                content = path.read_text(encoding="utf-8")
                # Normalize file path with forward slashes
                rel_path = path.as_posix()
                
                markdown_lines.append(f"## File: `{rel_path}`\n")
                
                # Determine language for markdown syntax highlighting
                lang = path.suffix.lstrip(".")
                if lang in ("tsx", "ts", "jsx", "js"):
                    lang = "typescript" if lang.startswith("t") else "javascript"
                elif lang in ("yml", "yaml"):
                    lang = "yaml"
                elif lang == "py":
                    lang = "python"
                elif lang == "sh":
                    lang = "bash"
                elif lang == "ps1":
                    lang = "powershell"
                
                markdown_lines.append(f"```{lang}\n{content}\n```\n\n")
            except Exception as e:
                # Skip binary or unreadable files silently
                continue
                
    output_file.write_text("".join(markdown_lines), encoding="utf-8")
    print(f"Generated codebase markdown at {output_file}")

if __name__ == "__main__":
    generate_markdown()
