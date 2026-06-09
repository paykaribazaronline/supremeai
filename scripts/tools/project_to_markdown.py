#!/usr/bin/env python3
"""
Project to Markdown Converter
Consolidates the entire project source into a single .md file for LLM context.
"""

import os
import sys

# Directories to ignore
EXCLUDE_DIRS = {
    '.git', 'node_modules', 'build', 'target', '.gradle', 
    '.idea', '.vscode', 'dist', 'out', 'bin', 'obj'
}

# Files to ignore
EXCLUDE_FILES = {
    'project_context.md', 'package-lock.json', 'gradle-wrapper.jar', 
    'SCRIPTS_INVENTORY.md', 'SCRIPTS_GUIDE_BN.md'
}

# Extensions to include
INCLUDE_EXTENSIONS = {
    '.java', '.py', '.ts', '.tsx', '.js', '.dart', 
    '.md', '.yml', '.yaml', '.json', '.ps1', '.sh', '.xml', '.sql'
}

def convert_to_md(root_dir, output_file):
    print(f"🚀 Converting {root_dir} to {output_file}...")
    count = 0
    
    with open(output_file, 'w', encoding='utf-8') as out:
        out.write(f"# SupremeAI Project Context\nGenerated on: {os.popen('date').read()}\n")
        
        for root, dirs, files in os.walk(root_dir):
            # Prune excluded directories
            dirs[:] = [d for d in dirs if d not in EXCLUDE_DIRS]
            
            for file in files:
                if file in EXCLUDE_FILES:
                    continue
                    
                _, ext = os.path.splitext(file)
                if ext.lower() in INCLUDE_EXTENSIONS:
                    rel_path = os.path.relpath(os.path.join(root, file), root_dir)
                    out.write(f"\n## FILE: {rel_path}\n")
                    lang = ext[1:] if ext else ""
                    out.write(f"```{lang}\n")
                    try:
                        with open(os.path.join(root, file), 'r', encoding='utf-8') as f:
                            out.write(f.read())
                    except Exception as e:
                        out.write(f"// Error reading file: {e}\n")
                    out.write("\n```\n")
                    count += 1
    print(f"✅ Done! Packed {count} files into {output_file}")

if __name__ == "__main__":
    convert_to_md('.', 'project_context.md')