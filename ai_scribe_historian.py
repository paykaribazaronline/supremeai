# SupremeAI - AI Scribe Agent (The Codebase Historian)
# Purpose: To automatically scan the codebase, generate docstrings for Python files,
# and create README.md files for directories.
# Author: Gemini Code Assist
# Date: July 12, 2026

import os
import argparse
import json
import time
from pathlib import Path
import hashlib
import litellm
import logging
import concurrent.futures

import sys
sys.path.insert(0, str(Path(__file__).parent / "backend"))
from core.config import settings
import ast

# --- Configuration ---
logging.basicConfig(level=logging.INFO, format='[%(levelname)s] %(message)s')

# বাংলা মন্তব্য: এখানে আমরা litellm ব্যবহার করছি যাতে ভবিষ্যতে সহজেই মডেল পরিবর্তন করা যায় (Gemini, GPT, Claude ইত্যাদি)।
litellm.set_verbose=False
CACHE_FILE = Path(__file__).parent / ".scribe_cache.json"
litellm.max_retries = 3
litellm.retry_strategy = {
    "wait_time": 16, # wait 16 seconds between retries
    "allowed_exceptions": [Exception] # retry on all exceptions
}


TARGET_DIRECTORIES = ["backend/core", "backend/tools"]
FILE_PATTERN = "*.py"
EXCLUDE_FILES = ["__init__.py"]

# --- AI Prompts ---

DOCSTRING_PROMPT_TEMPLATE = """
You are an expert Python programmer, the "Codebase Historian" for the SupremeAI project.
Your task is to generate a comprehensive and structured PEP 257 compliant module-level docstring for the given Python code.

**Analysis Context:**
- The file path is `{file_path}`.
- The project is a highly scalable AI ecosystem with a FastAPI backend and multiple agentic tools.

**Instructions:**
1.  **Analyze:** Deeply analyze the entire code provided below.
2.  **Summarize:** Write a concise one-paragraph summary of the module's primary purpose and its role within the SupremeAI project.
3.  **Structure:** After the summary, create the following sections:
    - `Key Components`: List the main classes and functions (e.g., `MyClass`, `my_function`) and provide a one-sentence description for each.
    - `Dependencies`: List key internal (e.g., `core.utils`) and external (e.g., `litellm`, `fastapi`) libraries this module depends on.
4.  **Format:** The entire output must be a single, valid Python docstring enclosed in triple quotes (`\"\"\"...\"\"\"`). Do not add any other text, code, or explanations outside the docstring.
5.  **Clarity:** The docstring should be clear and helpful for new developers to quickly grasp the module's functionality and context.

**Example Output Structure:**
```python
\"\"\"
<One-paragraph summary of the module's purpose and role.>

Key Components:
- `ClassName`: Brief description of the class.
- `function_name()`: Brief description of the function.

Dependencies:
- `core.config`: For accessing application settings.
- `external_library`: For providing specific functionality.
\"\"\"
```

**Code to Analyze:**
```python
{file_content}
```
"""

README_PROMPT_TEMPLATE = """
You are the "AI Scribe" and technical writer for the SupremeAI project. Your goal is to create a beautiful, informative, and developer-friendly `README.md` file.

**Analysis Context:**
- You are generating a README.md for the directory: `{dir_path}`.
- This directory contains the following modules, each with a summary of its purpose and key components:

---
{file_summaries}
---

**Instructions:**
1.  **Main Heading:** Start with a main heading (`#`) using the directory name (e.g., `# core/cache`).
2.  **Overview:** Write a high-level overview of the directory's purpose. Explain its role within the SupremeAI architecture and how its modules collaborate.
3.  **Modules Table:** Create a Markdown table with two columns: "Module" and "Description".
    - In the "Module" column, list each file name (e.g., `autocache_proxy.py`).
    - In the "Description" column, provide a concise, one-sentence summary for that module based on the provided summaries.
4.  **Interaction Flow (Mermaid Diagram):** Create a simple Mermaid.js `graph TD` diagram to visualize the interaction between the modules in this directory. If there's only one file, show its main purpose. This is crucial for developer understanding.
    - Example: `A[module_a] --> B[module_b]`
5.  **Detailed Summaries:** Under a "Module Details" section (`##`), list each file again with its full summary (the content from `{file_summaries}`).
6.  **Final Output:** The final output should be clean, well-formatted Markdown. Return ONLY the Markdown content.

**Example Structure:**
# Directory Name

<High-level overview...>

## Modules Overview
| Module | Description |
|---|---|
| `file_one.py` | Brief one-sentence summary. |

## Interaction Flow
```mermaid
graph TD;
    A --> B;
```

## Module Details
### `file_one.py`
<Full summary from file_summaries...>
"""

def get_ai_response(prompt: str) -> str:
    """
    Sends a prompt to the configured LLM and returns the response.
    """
key_index = 0

def get_ai_response(prompt: str) -> str:
    global key_index
    try:
        api_keys_str = settings.gemini_api_key
        if not api_keys_str:
            logging.error("No Gemini API key found in settings.")
            return ""
        
        keys = [k.strip() for k in api_keys_str.split(',') if k.strip()]
        if not keys:
            logging.error("No valid Gemini API keys found.")
            return ""
            
        import time
        max_retries = 3 * len(keys) # Allow enough retries to cycle through keys
        for attempt in range(max_retries):
            current_key = keys[key_index % len(keys)]
            try:
                response = litellm.completion(
                    model="gemini/gemini-2.5-flash",
                    messages=[{"content": prompt, "role": "user"}],
                    temperature=0.1,
                    api_key=current_key
                )
                return response.choices[0].message.content or ""
            except Exception as e:
                error_msg = str(e)
                if "429" in error_msg or "RESOURCE_EXHAUSTED" in error_msg or "RateLimit" in error_msg:
                    logging.warning(f"Rate limit hit with key ending in ...{current_key[-4:]}. Rotating key... (Attempt {attempt+1}/{max_retries})")
                    key_index += 1 # Rotate to next key
                    time.sleep(2) # Short sleep before trying next key
                    continue
                elif "403" in error_msg or "PERMISSION_DENIED" in error_msg or "API_KEY_SERVICE_BLOCKED" in error_msg:
                    logging.error(f"API Key ending in ...{current_key[-4:]} is BLOCKED (403). Rotating to next key...")
                    key_index += 1
                    continue
                else:
                    raise e
        return ""
    except Exception as e:
        logging.error(f"LLM API call failed: {e}")
        return ""

def get_existing_docstring(content: str) -> str | None:
    """Safely extracts the module-level docstring using AST."""
    try:
        tree = ast.parse(content)
        return ast.get_docstring(tree)
    except SyntaxError:
        return None

def load_cache() -> dict:
    """Loads the cache file from disk."""
    if CACHE_FILE.exists():
        return json.loads(CACHE_FILE.read_text(encoding="utf-8"))
    return {}

def save_cache(cache: dict):
    """Saves the cache to disk."""
    CACHE_FILE.write_text(json.dumps(cache, indent=2), encoding="utf-8")

def get_file_hash(content: str) -> str:
    """Generates a SHA-256 hash of the file content."""
    return hashlib.sha256(content.encode('utf-8')).hexdigest()

def update_file_with_docstring(file_path: Path, content: str, new_docstring: str) -> bool:
    """
    Updates a Python file with a new module-level docstring using AST.
    This is safer than string replacement.
    """
    try:
        tree = ast.parse(content)
        
        # Create a new docstring node
        new_docstring_node = ast.Expr(value=ast.Constant(value=new_docstring.strip()))

        # Check if a docstring already exists
        existing_docstring = ast.get_docstring(tree)

        if existing_docstring:
            # Replace the existing docstring node
            tree.body.pop(0)
            tree.body.insert(0, new_docstring_node)
        else:
            # Insert the new docstring at the top
            tree.body.insert(0, new_docstring_node)
        
        # Add a newline after the docstring if it's not the only thing in the file
        if len(tree.body) > 1:
            tree.body[1].lineno = new_docstring_node.lineno + new_docstring.strip().count('\n') + 3

        # Unparse the AST back to code (requires Python 3.9+)
        new_content = ast.unparse(tree)
        file_path.write_text(new_content, encoding="utf-8")
        return True
    except (SyntaxError, AttributeError, TypeError) as e:
        logging.error(f"Failed to update file {file_path} using AST: {e}. Falling back to simple prepend.")
        # Fallback for safety, though less ideal
        final_content = f'"""{new_docstring.strip()}"""\n\n{content}'
        file_path.write_text(final_content, encoding="utf-8")
        return True
    except Exception as e:
        logging.error(f"An unexpected error occurred during file update for {file_path}: {e}")
        return False

def generate_docstring_for_file(file_path: Path, cache: dict, force: bool = False) -> str | None:
    """
    Generates a module-level docstring for a given Python file.
    Returns the docstring content if generated/updated, otherwise None.
    """
    logging.info(f"Analyzing file for docstring: {file_path}")
    content = file_path.read_text(encoding="utf-8")
    content_hash = get_file_hash(content)
    existing_docstring = get_existing_docstring(content)

    # Cache check: যদি হ্যাশ একই থাকে এবং force=False না হয়, তবে ক্যাশ থেকে ডকস্ট্রিং ব্যবহার করা হবে
    if not force and file_path.name in cache and cache[file_path.name]["hash"] == content_hash:
        logging.info(f"Skipping {file_path}, content unchanged (from cache).")
        return cache[file_path.name]["docstring"]

    prompt = DOCSTRING_PROMPT_TEMPLATE.format(file_path=file_path, file_content=content)
    docstring = get_ai_response(prompt)

    if docstring:
        clean_docstring = docstring.replace("```python", "").replace("```", "").replace('"""', '').strip()
        if update_file_with_docstring(file_path, content, clean_docstring):
            logging.info(f"✅ Docstring {'updated' if existing_docstring else 'added'} for {file_path}")
            # Update cache
            cache[file_path.name] = {"hash": content_hash, "docstring": clean_docstring}
            return clean_docstring
    logging.warning(f"Could not generate docstring for {file_path}")
    return None

def generate_readme_for_dir(dir_path: Path, summaries: dict):
    """
    Generates a README.md file for a directory using summaries of its files.
    """
    logging.info(f"Generating README for directory: {dir_path}")
    
    summary_text = "\n".join([f"### {fname}\n{summary}\n" for fname, summary in summaries.items()])

    prompt = README_PROMPT_TEMPLATE.format(dir_path=dir_path, file_summaries=summary_text)
    readme_content = get_ai_response(prompt)

    if readme_content:
        readme_path = dir_path / "README.md"
        readme_path.write_text(readme_content, encoding="utf-8")
        logging.info(f"✅ README.md generated for {dir_path}")
    else:
        logging.warning(f"Could not generate README for {dir_path}")

def process_file(file_path: Path, cache: dict, force: bool, dry_run: bool) -> tuple[str, str | None]:
    """Helper function to process a single file, for concurrency."""
    if dry_run:
        logging.info(f"[DRY-RUN] Would analyze {file_path}")
        return file_path.name, f"This is a placeholder summary for {file_path.name}."
    
    summary = generate_docstring_for_file(file_path, cache, force)
    return file_path.name, summary

def main(dry_run: bool = False, force: bool = False, workers: int = 4):
    """
    Main function to orchestrate the documentation generation process.
    """
    if not settings.gemini_api_key:
        logging.error("FATAL: GEMINI_API_KEY is not set in backend settings.")
        return

    logging.info("Starting AI Scribe: The Codebase Historian...")
    logging.info(f"Target directories: {TARGET_DIRECTORIES}")
    if dry_run:
        logging.warning("Running in DRY-RUN mode. No files will be modified.")
    if force:
        logging.warning("Running in FORCE mode. All docstrings will be regenerated.")
    logging.info(f"Using {workers} workers for concurrent processing.")

    cache = load_cache()

    for target_dir in TARGET_DIRECTORIES:
        base_path = Path(target_dir)
        if not base_path.exists():
            logging.warning(f"Directory not found: {base_path}. Skipping.")
            continue

        # প্রতিটি সাব-ডিরেক্টরির জন্য আলাদাভাবে কাজ করা হবে
        # We walk the directory tree to process parent directories before children.
        for dir_path, _, _ in os.walk(base_path):
            dir_path = Path(dir_path)
            py_files = list(dir_path.glob(FILE_PATTERN))
            # Filter out files in subdirectories, only process files in the current dir_path
            py_files = [f for f in py_files if f.parent == dir_path and f.name not in EXCLUDE_FILES]

            if not py_files: # যদি কোনো পাইথন ফাইল না থাকে
                continue

            file_summaries = {}
            logging.info(f"\n--- Processing Directory: {dir_path} ---")
            
            with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as executor:
                # Submit all file processing tasks to the executor
                future_to_file = {executor.submit(process_file, fp, cache, force, dry_run): fp for fp in py_files}
                for future in concurrent.futures.as_completed(future_to_file):
                    fname, summary = future.result()
                    if summary:
                        file_summaries[fname] = summary

            if file_summaries and not dry_run:
                generate_readme_for_dir(dir_path, file_summaries)
            elif file_summaries and dry_run:
                logging.info(f"[DRY-RUN] Would generate README.md for {dir_path}")

    if not dry_run:
        save_cache(cache)

    logging.info("\nHistorian's work complete. The past is now documented. ✨")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="AI Scribe: Codebase Historian - Auto-Doc Generator")
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Run the script without modifying any files."
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Force regeneration of all docstrings, ignoring the cache."
    )
    parser.add_argument(
        "-w", "--workers",
        type=int,
        default=4,
        help="Number of concurrent workers to use for processing files."
    )
    args = parser.parse_args()
    main(args.dry_run, args.force, args.workers)

# --- কিভাবে ব্যবহার করবেন? ---
#
# ১. API কী সেট করুন:
#    এই স্ক্রিপ্টটি চালানোর জন্য আপনার GEMINI_API_KEY এনভায়রনমেন্ট ভেরিয়েবল সেট করা থাকতে হবে।
#    ```bash
#    export GEMINI_API_KEY="your_gemini_api_key_here"
#    ```
#
# ২. স্ক্রিপ্ট চালান:
#    টার্মিনাল থেকে নিচের কমান্ডটি রান করুন।
#    ```bash
#    python scripts/ai_scribe_historian.py
#    ```
#
# ৩. ড্রাই রান (Dry Run):
#    আপনি যদি ফাইল পরিবর্তন না করে শুধু দেখতে চান কোন কোন ফাইল এবং ফোল্ডার প্রভাবিত হবে, তাহলে --dry-run ফ্ল্যাগ ব্যবহার করুন।
#    ```bash
#    python scripts/ai_scribe_historian.py --dry-run
#    ```
#
# ৪. ফোর্স রি-জেনারেশন (Force Regeneration):
#    ক্যাশ (cache) উপেক্ষা করে সমস্ত ডকস্ট্রিং পুনরায় তৈরি করতে --force ফ্ল্যাগ ব্যবহার করুন।
#    ```bash
#    python scripts/ai_scribe_historian.py --force
#    ```
#
# ৫. কনকারেন্সি লেভেল সেট করা (Set Concurrency Level):
#    একসাথে কতগুলো ফাইল প্রসেস হবে তা নির্ধারণ করতে --workers ফ্ল্যাগ ব্যবহার করুন।
#    ```bash
#    python scripts/ai_scribe_historian.py --workers 8
#    ```