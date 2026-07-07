# 📄 ফাইল: scripts/generate_push_summary.py

**প্রকার:** .py  
**সাইজ:** 4,003 বাইট  
**আপডেট:** 2026-07-07T22:11:19.722696

---

## কোড

```py
#!/usr/bin/env python3
"""
SupremeAI - PR / Push Summary Generator
Generates a markdown summary of git differences between two SHAs using LLM
and posts it as a PR comment (if running in a Pull Request context).
"""

import argparse
import subprocess
import os
import sys
import httpx
from litellm import completion

def get_git_diff(base_sha, head_sha):
    try:
        diff = subprocess.check_output(
            ['git', 'diff', f'{base_sha}..{head_sha}'],
            text=True,
            errors='replace'
        )
        return diff
    except subprocess.CalledProcessError as e:
        print(f"Error generating git diff: {e}")
        sys.exit(1)

def generate_summary(diff_content):
    if not diff_content.strip():
        return "No significant changes found in this push."
        
    # Cap the diff size to avoid hitting LLM token limits (e.g. max 50KB string)
    max_diff_size = 50 * 1024
    if len(diff_content) > max_diff_size:
        diff_content = diff_content[:max_diff_size] + "\n...[DIFF TRUNCATED]..."

    prompt = f"""
You are an expert Enterprise Software Architect. 
Review the following git diff and provide a concise, high-level technical summary of the changes.
Highlight any major architectural shifts, security implications, or critical logic updates.
Keep it strictly under 300 words. Format the output in Markdown.

### Git Diff:
```diff
{diff_content}
```
"""

    try:
        # Using litellm. The model name can be set to gemini/gemini-2.5-pro or an internal litellm proxy endpoint
        # As per Architect mandate, we use litellm. We map SUPREMEAI_API_KEY to litellm's expected API keys via env vars
        # Or simply call completion with gemini
        print("Calling LLM via litellm...")
        response = completion(
            model="gemini/gemini-2.5-pro",
            messages=[{"role": "user", "content": prompt}],
            # We assume GEMINI_API_KEY is available in the environment from CI secrets
        )
        return response.choices[0].message.content
    except Exception as e:
        print(f"LLM Generation Error: {e}")
        return f"### PR Summary\nFailed to generate summary via LLM: {str(e)}"

def post_pr_comment(repo, pr_number, token, content):
    print(f"Posting comment to PR #{pr_number} in {repo}...")
    url = f"https://api.github.com/repos/{repo}/issues/{pr_number}/comments"
    headers = {
        "Authorization": f"token {token}",
        "Accept": "application/vnd.github.v3+json"
    }
    data = {"body": content}
    
    with httpx.Client() as client:
        response = client.post(url, headers=headers, json=data, timeout=30.0)
        
    if response.status_code == 201:
        print("Successfully posted PR comment.")
    else:
        print(f"Failed to post PR comment. Status: {response.status_code}, Response: {response.text}")

def main():
    parser = argparse.ArgumentParser(description="Generate PR/Push Summary via LLM")
    parser.add_argument("base_sha", help="Base commit SHA")
    parser.add_argument("head_sha", help="Head commit SHA")
    parser.add_argument("output_file", help="Path to write the markdown summary")
    parser.add_argument("--repo", help="GitHub repository (e.g. owner/repo)")
    parser.add_argument("--pr-number", help="Pull Request number (if applicable)")
    parser.add_argument("--token", help="GitHub Token for posting comments")
    args = parser.parse_args()

    print(f"Generating summary for {args.base_sha}..{args.head_sha}")
    diff = get_git_diff(args.base_sha, args.head_sha)
    summary = generate_summary(diff)
    
    with open(args.output_file, "w", encoding="utf-8") as f:
        f.write(summary)
    print(f"Saved summary to {args.output_file}")
    
    if args.repo and args.pr_number and args.token:
        # Add a prefix to identify it as AI-generated
        comment_body = f"🤖 **SupremeAI Push Summary**\n\n{summary}"
        post_pr_comment(args.repo, args.pr_number, args.token, comment_body)

if __name__ == "__main__":
    main()

```