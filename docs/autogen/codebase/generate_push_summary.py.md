# 📄 ফাইল: generate_push_summary.py

**প্রকার:** .py  
**সাইজ:** 9,991 বাইট  
**আপডেট:** 2026-07-11T17:11:02.585807

---

## কোড

```py
#!/usr/bin/env python
"""
generate_push_summary.py
========================

এই স্ক্রিপ্টটি দুটি Git কমিটের মধ্যেকার পরিবর্তনগুলোর একটি মার্কডাউন সারাংশ তৈরি করে।
এটি প্রতিটি পরিবর্তিত ফাইলের জন্য পুরোনো এবং নতুন লাইনগুলো একটি টেবিল আকারে দেখায়।

ব্যবহার:
    python scripts/generate_push_summary.py <before_sha> <after_sha> <output_file.md> [options]

উদাহরণ:
    python scripts/generate_push_summary.py HEAD~1 HEAD push_summary.md

CI/CD ইন্টিগ্রেশন:
    GitHub Actions-এ এটি ব্যবহার করা যেতে পারে:
    python scripts/generate_push_summary.py ${{ github.event.pull_request.base.sha }} ${{ github.event.pull_request.head.sha }} summary.md \
        --repo ${{ github.repository }} \
        --pr-number ${{ github.event.pull_request.number }} \
        --token ${{ secrets.GITHUB_TOKEN }}
"""

import subprocess
import sys
import re
import os
import argparse
from datetime import datetime
from typing import List, Dict, Any

try:
    import httpx
except ImportError:
    httpx = None

def run_git_command(command: list[str]) -> str:
    """একটি Git কমান্ড রান করে এবং আউটপুট রিটার্ন করে।"""
    try:
        result = subprocess.run(
            ["git"] + command,
            capture_output=True,
            text=True,
            check=True,
            encoding='utf-8'
        )
        return result.stdout
    except subprocess.CalledProcessError as e:
        print(f"Git কমান্ড রান করতে সমস্যা হয়েছে: {e.stderr}", file=sys.stderr)
        sys.exit(1)
    except FileNotFoundError:
        print("Git ইনস্টল করা নেই অথবা PATH-এ নেই।", file=sys.stderr)
        sys.exit(1)

def post_to_pr(repo: str, pr_number: int, token: str, summary: str):
    """GitHub PR-এ একটি কমেন্ট হিসেবে সারাংশ পোস্ট করে।"""
    if not httpx:
        print("`httpx` is not installed. Cannot post comment. Run `pip install httpx`.", file=sys.stderr)
        return
    if not token:
        print("GitHub token is missing. Cannot post comment.", file=sys.stderr)
        return

    url = f"https://api.github.com/repos/{repo}/issues/{pr_number}/comments"
    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github.v3+json",
    }
    payload = {"body": summary}

    try:
        with httpx.Client() as client:
            response = client.post(url, json=payload, headers=headers, timeout=20.0)
            response.raise_for_status()
        print(f"✅ Successfully posted summary to PR #{pr_number}")
    except httpx.HTTPStatusError as e:
        print(f"❌ Failed to post comment to PR #{pr_number}. Status: {e.response.status_code}, Body: {e.response.text}", file=sys.stderr)
    except Exception as e:
        print(f"❌ An unexpected error occurred while posting to PR: {e}", file=sys.stderr)

def get_commit_details(sha: str) -> Dict[str, str]:
    """প্রো-টিপ: কমিটের বিস্তারিত তথ্য (অথর, মেসেজ) যোগ করা।"""
    try:
        # format-এর মাধ্যমে 원하는 তথ্য সহজেই নেওয়া যায়
        details_str = run_git_command(["show", "-s", f"--format=%an%n%ae%n%s", sha])
        author, email, subject = details_str.strip().split('\n', 2)
        return {"author": author, "email": email, "subject": subject}
    except Exception:
        return {"author": "Unknown", "email": "", "subject": "N/A"}

def parse_diff(diff_output: str) -> List[Dict[str, Any]]:
    """git diff আউটপুটকে একটি স্ট্রাকচার্ড লিস্টে পার্স করে।"""
    files = []
    current_file_diff = None

    for line in diff_output.splitlines():
        if line.startswith("diff --git"):
            if current_file_diff:
                files.append(current_file_diff)
            
            # ফাইল পাথ বের করা
            path_match = re.search(r'a/(.+) b/(.+)', line)
            if path_match:
                current_file_diff = {
                    "old_path": path_match.group(1),
                    "new_path": path_match.group(2),
                    "status": "modified", # Default স্ট্যাটাস
                    "changes": []
                }

        elif line.startswith("new file mode"):
            if current_file_diff:
                current_file_diff["status"] = "added"
        elif line.startswith("deleted file mode"):
            if current_file_diff:
                current_file_diff["status"] = "deleted"
        elif line.startswith("rename from"):
            if current_file_diff:
                current_file_diff["status"] = "renamed"
        elif line.startswith("Binary files"):
            if current_file_diff:
                current_file_diff["status"] = "binary"
        elif line.startswith(('--- a/', '+++ b/')):
             continue # diff --git লাইন থেকে পাথ আগেই নেওয়া হয়েছে
        elif current_file_diff:
            current_file_diff["changes"].append(line)

    if current_file_diff:
        files.append(current_file_diff)
        
    return files

def format_summary_markdown(files: List[Dict[str, Any]], before_sha: str, after_sha: str) -> str:
    """পার্স করা ডেটা থেকে ফাইনাল মার্কডাউন তৈরি করে।"""
    commit_details = get_commit_details(after_sha)

    markdown_content = [
        f"# Push Summary: `{after_sha[:7]}`",
        f"**Changes between `{before_sha[:7]}` and `{after_sha[:7]}`**",
        f"> **Commit:** {commit_details['subject']} - by *{commit_details['author']}*",
        f"Generated on: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
        "\n---"
    ]

    if not files:
        markdown_content.append("\n**No changes detected.**")
        return "\n".join(markdown_content)

    for file_diff in files:
        path = file_diff["new_path"]
        status = file_diff["status"]

        if status == "added":
            markdown_content.append(f"\n### ➕ Added: `{path}`")
        elif status == "deleted":
            markdown_content.append(f"\n### ➖ Deleted: `{path}`")
        elif status == "renamed":
            markdown_content.append(f"\n### 🔄 Renamed: `{file_diff['old_path']}` → `{path}`")
        elif status == "binary":
            markdown_content.append(f"\n### 🖼️ Binary file changed: `{path}`")
            continue # বাইনারি ফাইলের জন্য diff দেখানোর দরকার নেই
        else:
            markdown_content.append(f"\n### ✏️ Modified: `{path}`")

        # প্রো-টিপ: পরিবর্তনগুলো diff কোড ব্লকে দেখানো হচ্ছে
        if file_diff["changes"]:
            markdown_content.append("```diff")
            # শুধুমাত্র + এবং - দিয়ে শুরু হওয়া লাইনগুলো দেখানো হচ্ছে
            for change in file_diff["changes"]:
                if change.startswith('+') or change.startswith('-'):
                    markdown_content.append(change)
            markdown_content.append("```")

    return "\n".join(markdown_content)

def main():
    """দুটি কমিটের মধ্যে পরিবর্তন নিয়ে একটি মার্কডাউন ফাইল তৈরি করে।"""
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawTextHelpFormatter)
    parser.add_argument("before_sha", help="The base commit SHA.")
    parser.add_argument("after_sha", help="The head commit SHA.")
    parser.add_argument("output_file", help="Path to the output markdown file.")
    parser.add_argument("--repo", help="GitHub repository (e.g., 'owner/repo').")
    parser.add_argument("--pr-number", type=int, help="Pull Request number to comment on.")
    parser.add_argument("--token", help="GitHub token for posting comments.", default=os.getenv("GITHUB_TOKEN"))
    args = parser.parse_args()

    # --unified=0 এর বদলে স্ট্যান্ডার্ড diff আউটপুট ব্যবহার করা হচ্ছে
    # --find-renames দিয়ে ফাইলের নাম পরিবর্তন সনাক্ত করা হচ্ছে
    diff_output = run_git_command(["diff", "--find-renames", args.before_sha, args.after_sha])
    
    # ১. ডিফের আউটপুট পার্স করে একটি স্ট্রাকচার্ড লিস্ট তৈরি করা
    parsed_files = parse_diff(diff_output)
    
    # ২. পার্স করা ডেটা থেকে ফাইনাল মার্কডাউন তৈরি করা
    markdown_summary = format_summary_markdown(parsed_files, args.before_sha, args.after_sha)

    with open(args.output_file, 'w', encoding='utf-8') as f:
        f.write(markdown_summary)
    
    print(f"✅ Summary successfully generated at: {args.output_file}")

    # যদি PR নম্বর এবং রিপোজিটরি দেওয়া থাকে, তাহলে কমেন্ট পোস্ট করা হবে
    if args.repo and args.pr_number and args.token:
        post_to_pr(args.repo, args.pr_number, args.token, markdown_summary)

if __name__ == "__main__":
    main()
```