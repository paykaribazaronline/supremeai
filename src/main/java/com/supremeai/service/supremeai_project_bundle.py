import requests
import json
import os

# সুপ্রিমএআই ব্যাকএন্ডের MCP এন্ডপয়েন্টের বেস URL
# আপনার সুপ্রিমএআই অ্যাপ্লিকেশন যেখানে চলছে, সেই অনুযায়ী এটি পরিবর্তন করুন।
SUPREMEAI_BASE_URL = "http://localhost:8080/api/mcp"

def get_project_structure():
    """
    McpToolingController থেকে প্রোজেক্টের ফাইল স্ট্রাকচার নিয়ে আসে।
    """
    response = requests.get(f"{SUPREMEAI_BASE_URL}/project/structure")
    response.raise_for_status()
    return response.json()["structure"].split('\n')

def read_file_content(file_path):
    """
    McpToolingController থেকে নির্দিষ্ট ফাইলের কন্টেন্ট পড়ে।
    """
    payload = {"path": file_path}
    response = requests.post(f"{SUPREMEAI_BASE_URL}/project/read-file", json=payload)
    response.raise_for_status()
    return response.json()["content"]

def get_preamble():
    """
    এআই-এর জন্য নির্দেশাবলী তৈরি করে যা ফাইলের শুরুতে থাকবে।
    """
    return """# SYSTEM INSTRUCTIONS FOR SUPREMEAI ANALYSIS
You are SupremeAI Expert Analyst. Below is the complete context of the SupremeAI project.
Your goals:
1. Understand the 'Zero-Cost, Zero-Maintenance, No-Boundary' architecture.
2. Analyze the Multi-AI Voting system and MCP implementation.
3. Provide code fixes, security audits, and feature improvements based on this specific codebase.
4. Reference specific files using the format '--- path/to/file ---'.

PROJECT SNAPSHOT START:
"""

def generate_project_bundle():
    """
    প্রোজেক্টের সমস্ত প্রাসঙ্গিক ফাইলকে একটি সিঙ্গেল মার্কডাউন বান্ডলে রূপান্তর করে।
    """
    print("Fetching project structure from SupremeAI backend...")
    structure_lines = get_project_structure()
    project_bundle = [get_preamble()]
    
    # শুধুমাত্র প্রাসঙ্গিক ফাইল এক্সটেনশনগুলো বিবেচনা করা হচ্ছে
    relevant_extensions = ('.java', '.md', '.yml', '.xml', '.gradle', '.js', '.ts', '.html', '.css', '.json', '.properties', '.txt', '.py')
    
    for line in structure_lines:
        file_path = line.strip()
        # ডিরেক্টরি, খালি লাইন এবং অপ্রাসঙ্গিক এক্সটেনশন বাদ দেওয়া হচ্ছে
        if not file_path or file_path.endswith('/') or not file_path.lower().endswith(relevant_extensions):
            continue

        print(f"Reading file: {file_path}")
        try:
            content = read_file_content(file_path)
            project_bundle.append(f"--- {file_path} ---\n")
            project_bundle.append(f"```\n{content}\n```\n\n")
        except requests.exceptions.RequestException as e:
            print(f"Error reading {file_path}: {e}")
            project_bundle.append(f"--- {file_path} (Error: Could not read content) ---\n\n")
    
    return "\n".join(project_bundle)

if __name__ == "__main__":
    bundle = generate_project_bundle()
    output_filename = "supremeai_project_bundle.md"
    with open(output_filename, "w", encoding="utf-8") as f:
        f.write(bundle)
    print(f"\nProject bundle generated successfully: {output_filename}")
    print("You can now copy the content of this file and paste it into kimi.com or any other LLM.")
