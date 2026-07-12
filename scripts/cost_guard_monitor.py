# scripts/cost_guard_monitor.py
import os
import json
import requests

# বাংলা মন্তব্য: এখানে আমরা একটি ডামি লগ ডেটা ব্যবহার করছি।
# বাস্তব পরিবেশে, এই ডেটা Sentry, Datadog, বা অন্য কোনো লগিং সিস্টেম থেকে আসবে।
DUMMY_LOG_DATA = [
    {
        "task_id": "task-123",
        "task_name": "text_formatting",
        "model": "gemini-2.5-pro",
        "tokens": 500,
        "latency_ms": 1200,
    },
    {
        "task_id": "task-124",
        "task_name": "complex_analysis",
        "model": "gemini-2.5-pro",
        "tokens": 8000,
        "latency_ms": 5000,
    },
    {
        "task_id": "task-125",
        "task_name": "simple_classification",
        "model": "claude-3-opus",
        "tokens": 300,
        "latency_ms": 1500,
    },
    {
        "task_id": "task-126",
        "task_name": "text_formatting",
        "model": "gemini-2.5-flash",
        "tokens": 450,
        "latency_ms": 400,
    },
]

# বাংলা মন্তব্য: কোন কাজের জন্য কোন মডেলগুলো অতিরিক্ত শক্তিশালী (overkill) তা এখানে নির্ধারণ করা হয়েছে।
OPTIMIZATION_RULES = {
    "text_formatting": {
        "overkill_models": ["gemini-2.5-pro", "claude-3-opus"],
        "suggestion": "gemini-2.5-flash or claude-3-haiku",
        "estimated_savings": "70%",
    },
    "simple_classification": {
        "overkill_models": ["gemini-2.5-pro", "claude-3-opus"],
        "suggestion": "gemini-2.5-flash",
        "estimated_savings": "60-80%",
    },
}

def create_github_issue(title, body):
    """GitHub-এ একটি নতুন Issue তৈরি করে।"""
    token = os.getenv("GITHUB_TOKEN")
    repo = os.getenv("GITHUB_REPOSITORY")

    if not token or not repo:
        print("❌ GITHUB_TOKEN বা GITHUB_REPOSITORY এনভায়রনমেন্ট ভেরিয়েবল সেট করা নেই।")
        return

    url = f"https://api.github.com/repos/{repo}/issues"
    headers = {
        "Authorization": f"token {token}",
        "Accept": "application/vnd.github.v3+json",
    }
    data = {"title": title, "body": body, "labels": ["cost-optimization", "ai-agent"]}

    response = requests.post(url, headers=headers, data=json.dumps(data))

    if response.status_code == 201:
        print(f"✅ সফলভাবে GitHub Issue তৈরি হয়েছে: {response.json()['html_url']}")
    else:
        print(f"❌ GitHub Issue তৈরি করতে ব্যর্থ। স্ট্যাটাস কোড: {response.status_code}")
        print(response.text)

def analyze_logs():
    """লগ বিশ্লেষণ করে অপটিমাইজেশনের সুযোগ খুঁজে বের করে।"""
    print("🤖 AI টাস্ক অপটিমাইজার এজেন্ট চলছে...")
    for log in DUMMY_LOG_DATA:
        task_name = log.get("task_name")
        model_used = log.get("model")

        if task_name in OPTIMIZATION_RULES:
            rule = OPTIMIZATION_RULES[task_name]
            if model_used in rule["overkill_models"]:
                print(f"🔍 অপটিমাইজেশনের সুযোগ পাওয়া গেছে: টাস্ক '{task_name}'")

                title = f"Cost Optimization: Use a cheaper model for '{task_name}' task"
                body = (
                    f"### 🤖 AI Cost Guard Alert\n\n"
                    f"**টাস্ক:** `{log['task_id']}` (`{task_name}`)\n"
                    f"**ব্যবহৃত মডেল:** `{model_used}`\n\n"
                    f"**সুপারিশ:** এই টাস্কের জন্য `{model_used}` একটি অতিরিক্ত শক্তিশালী এবং ব্যয়বহুল মডেল।\n\n"
                    f"খরচ প্রায় **{rule['estimated_savings']}** কমাতে এবং পারফরম্যান্স ঠিক রাখতে, অনুগ্রহ করে `{rule['suggestion']}` মডেল ব্যবহার করার কথা বিবেচনা করুন।\n"
                )
                create_github_issue(title, body)

    print("✅ বিশ্লেষণ সম্পন্ন হয়েছে।")

if __name__ == "__main__":
    analyze_logs()