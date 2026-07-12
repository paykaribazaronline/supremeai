# SupremeAI - AI Scribe Agent (The Codebase Historian)
# Purpose: To automatically scan the codebase, generate docstrings for Python files,
# and create README.md files for directories.
# Author: Gemini Code Assist
# Date: July 12, 2026

import os
import argparse
from pathlib import Path
import litellm
import logging

import sys
sys.path.insert(0, str(Path(__file__).parent / "backend"))
from core.config import settings

# --- Configuration ---
logging.basicConfig(level=logging.INFO, format='[%(levelname)s] %(message)s')

# বাংলা মন্তব্য: এখানে আমরা litellm ব্যবহার করছি যাতে ভবিষ্যতে সহজেই মডেল পরিবর্তন করা যায় (Gemini, GPT, Claude ইত্যাদি)।
litellm.set_verbose=False

TARGET_DIRECTORIES = ["backend/core", "backend/tools"]
FILE_PATTERN = "*.py"
EXCLUDE_FILES = ["__init__.py"]

# --- AI Prompts ---

DOCSTRING_PROMPT_TEMPLATE = """
You are an expert Python programmer, a "Codebase Historian" for the SupremeAI project.
Your task is to generate a concise and accurate PEP 257 compliant docstring for the given Python code.

**Analysis Context:**
- The file path is `{file_path}`.
- The project is a highly scalable AI ecosystem with a FastAPI backend and multiple agentic tools.

**Instructions:**
1.  Analyze the entire code provided below.
2.  Identify the primary purpose of the file, its main classes, and functions.
3.  Generate a single, file-level docstring that summarizes its role.
4.  Do NOT generate docstrings for individual functions or classes, only a module-level docstring.
5.  The docstring should be helpful for new developers to quickly understand the file's responsibility.
6.  Return ONLY the Python docstring content, enclosed in triple quotes (`\"\"\"...\"\"\"`). Do not include any other text or explanation.

**Code to Analyze:**
```python
{file_content}
```
"""

README_PROMPT_TEMPLATE = """
You are the "AI Scribe" for the SupremeAI project, tasked with creating clear and helpful documentation.

**Analysis Context:**
- You are generating a README.md for the directory: `{dir_path}`.
- This directory contains several modules with the following responsibilities:

---
{file_summaries}
---

**Instructions:**
1.  Based on the summaries above, write a `README.md` for this directory.
2.  Start with a main heading (`#`) for the directory name.
3.  Provide a high-level overview of the directory's purpose and how its components work together.
4.  Use a section "Core Components" or "Modules" to list each file and its one-sentence summary.
5.  The tone should be professional, clear, and aimed at developers.
6.  Return ONLY the Markdown content. Do not include any other text.
"""

def get_ai_response(prompt: str) -> str:
    """
    Sends a prompt to the configured LLM and returns the response.
    """
    try:
        api_key = settings.gemini_api_key
        if api_key and ',' in api_key:
            api_key = api_key.split(',')[0].strip()
        if not api_key:
            logging.error("No Gemini API key found in settings.")
            return ""
            
        import time
        max_retries = 3
        for attempt in range(max_retries):
            try:
                response = litellm.completion(
                    model="gemini/gemini-2.5-flash",
                    messages=[{"content": prompt, "role": "user"}],
                    temperature=0.1,
                    max_tokens=1024,
                    api_key=api_key
                )
                return response.choices[0].message.content or ""
            except Exception as e:
                if "429" in str(e) or "RESOURCE_EXHAUSTED" in str(e) or "RateLimit" in str(e):
                    logging.warning(f"Rate limit hit. Retrying in 16 seconds... (Attempt {attempt+1}/{max_retries})")
                    time.sleep(16)
                    continue
                else:
                    raise e
        return ""
    except Exception as e:
        logging.error(f"LLM API call failed: {e}")
        return ""

def generate_docstring_for_file(file_path: Path) -> str:
    """
    Generates a module-level docstring for a given Python file.
    """
    logging.info(f"Analyzing file for docstring: {file_path}")
    content = file_path.read_text(encoding="utf-8")

    # যদি ফাইলটিতে আগে থেকেই মডিউল-লেভেল ডকস্ট্রিং থাকে, তবে স্কিপ করা হবে
    if content.strip().startswith('"""') or content.strip().startswith("'''"):
        logging.info(f"Skipping {file_path}, as it already has a docstring.")
        return content.split('"""')[1].split('"""')[0].strip()

    prompt = DOCSTRING_PROMPT_TEMPLATE.format(file_path=file_path, file_content=content)
    docstring = get_ai_response(prompt)

    if docstring:
        # 생성된 ডকস্ট্রিং থেকে ```python এবং ``` মার্কডাউন ব্লক রিমুভ করা হচ্ছে
        docstring = docstring.replace("```python", "").replace("```", "").strip()
        new_content = f"{docstring}\n\n{content}"
        file_path.write_text(new_content, encoding="utf-8")
        logging.info(f"✅ Docstring added to {file_path}")
        return docstring.replace('"""', '').strip()
    else:
        logging.warning(f"Could not generate docstring for {file_path}")
        return f"- `{file_path.name}`: No summary generated."

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

def main(dry_run: bool = False):
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

    for target_dir in TARGET_DIRECTORIES:
        base_path = Path(target_dir)
        if not base_path.exists():
            logging.warning(f"Directory not found: {base_path}. Skipping.")
            continue

        # প্রতিটি সাব-ডিরেক্টরির জন্য আলাদাভাবে কাজ করা হবে
        for dir_path in [p for p in base_path.rglob("*") if p.is_dir()]:
            py_files = list(dir_path.glob(FILE_PATTERN))
            if not py_files:
                continue

            file_summaries = {}
            logging.info(f"\n--- Processing Directory: {dir_path} ---")
            for file_path in py_files:
                if file_path.name in EXCLUDE_FILES:
                    continue
                
                if not dry_run:
                    summary = generate_docstring_for_file(file_path)
                    file_summaries[file_path.name] = summary
                else:
                    logging.info(f"[DRY-RUN] Would analyze {file_path}")
                    file_summaries[file_path.name] = f"This is a placeholder summary for {file_path.name}."

            if file_summaries and not dry_run:
                generate_readme_for_dir(dir_path, file_summaries)
            elif file_summaries and dry_run:
                logging.info(f"[DRY-RUN] Would generate README.md for {dir_path}")

    logging.info("\nHistorian's work complete. The past is now documented. ✨")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="AI Scribe: Codebase Historian - Auto-Doc Generator")
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Run the script without modifying any files."
    )
    args = parser.parse_args()
    main(args.dry_run)

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