# scripts/ai_query_optimizer.py
import os
import requests
import json

# বাংলা মন্তব্য: এখানে আমরা একটি ডামি স্লো কোয়েরি লগ ব্যবহার করছি।
# বাস্তব পরিবেশে, এটি আপনার ডেটাবেসের slow query log থেকে আসবে (যেমন: PostgreSQL-এর pg_stat_statements)।
DUMMY_SLOW_QUERY_LOG = [
    {
        "query_id": "q-001",
        "query": "SELECT * FROM users WHERE email LIKE '%@example.com';",
        "execution_time_ms": 2500,
        "calls": 500,
    }
]

# বাংলা মন্তব্য: ডামি EXPLAIN ANALYZE আউটপুট। বাস্তব পরিবেশে এটি ডেটাবেস থেকে আসবে।
DUMMY_EXPLAIN_BEFORE = """
Seq Scan on users  (cost=0.00..60.50 rows=10 width=104) (actual time=0.015..0.541 rows=10 loops=1)
  Filter: (email ~~ '%@example.com%'::text)
  Rows Removed by Filter: 990
Planning Time: 0.069 ms
Execution Time: 0.552 ms
"""

DUMMY_EXPLAIN_AFTER = """
Bitmap Heap Scan on users  (cost=4.34..24.85 rows=10 width=104) (actual time=0.024..0.026 rows=10 loops=1)
  Recheck Cond: (email ~~ '%@example.com%'::text)
  Heap Blocks: exact=1
  ->  Bitmap Index Scan on idx_users_email_trgm  (cost=0.00..4.34 rows=10 width=0) (actual time=0.021..0.021 rows=10 loops=1)
        Index Cond: (email ~~ '%@example.com%'::text)
Planning Time: 0.084 ms
Execution Time: 0.035 ms
"""

def get_optimization_suggestion(query):
    """
    একটি AI মডেল ব্যবহার করে কোয়েরি অপটিমাইজেশনের জন্য পরামর্শ তৈরি করে।
    বাস্তব পরিবেশে, এখানে Gemini API কল করা হবে।
    """
    print(f"🧠 AI মডেল দিয়ে কোয়েরি বিশ্লেষণ করা হচ্ছে: {query}")
    # ডেমোর জন্য, আমরা একটি হার্ডকোডেড পরামর্শ দিচ্ছি।
    if "LIKE '%" in query:
        return {
            "optimized_query": "CREATE INDEX idx_users_email_trgm ON users USING gin (email gin_trgm_ops);",
            "explanation": "The original query uses a leading wildcard (`LIKE '%...'`), which prevents the use of a standard B-tree index. This results in a slow `Seq Scan`.\n\n**Recommendation:**\nCreate a `GIN` index with the `pg_trgm` extension. This allows for efficient index-based searches on trigram patterns, significantly speeding up wildcard searches.",
        }
    return None

def create_github_pr(title, body, branch_name):
    """GitHub-এ একটি নতুন Pull Request তৈরি করে।"""
    token = os.getenv("GITHUB_TOKEN")
    repo = os.getenv("GITHUB_REPOSITORY")
    base_branch = "main" # অথবা আপনার ডিফল্ট ব্রাঞ্চ

    if not token or not repo:
        print("❌ GITHUB_TOKEN বা GITHUB_REPOSITORY এনভায়রনমেন্ট ভেরিয়েবল সেট করা নেই।")
        return

    url = f"https://api.github.com/repos/{repo}/pulls"
    headers = {
        "Authorization": f"token {token}",
        "Accept": "application/vnd.github.v3+json",
    }
    data = {
        "title": title,
        "body": body,
        "head": branch_name,
        "base": base_branch,
    }

    # এখানে একটি ডামি ব্রাঞ্চ তৈরি এবং কমিট করার ধাপগুলো বাদ দেওয়া হয়েছে।
    # বাস্তব ওয়ার্কফ্লোতে, `peter-evans/create-pull-request` অ্যাকশন এটি পরিচালনা করবে।
    print("\n--- 🤖 ডেমো মোড: Pull Request তৈরি করা হচ্ছে ---")
    print(f"Title: {title}")
    print(f"Branch: {branch_name}")
    print("\nBody:")
    print(body)
    print("---------------------------------------------\n")
    print("✅ (সিমুলেটেড) Pull Request সফলভাবে তৈরি হয়েছে।")


def analyze_slow_queries():
    """স্লো কোয়েরি লগ বিশ্লেষণ করে অপটিমাইজেশনের জন্য PR তৈরি করে।"""
    print("🤖 AI ডেটাবেস অপটিমাইজার এজেন্ট চলছে...")
    for query_log in DUMMY_SLOW_QUERY_LOG:
        if query_log["execution_time_ms"] > 1000: # থ্রেশহোল্ড
            print(f"🔍 ধীরগতির কোয়েরি পাওয়া গেছে: {query_log['query_id']}")

            suggestion = get_optimization_suggestion(query_log["query"])

            if suggestion:
                branch_name = f"fix/db-optimize-{query_log['query_id']}"
                title = f"perf(db): Optimize slow query {query_log['query_id']}"
                body = (
                    f"### 🤖 AI-Generated Database Optimization\n\n"
                    f"**ধীরগতির কোয়েরি:**\n```sql\n{query_log['query']}\n```\n\n"
                    f"**AI এজেন্টের বিশ্লেষণ:**\n{suggestion['explanation']}\n\n"
                    f"**সুপারিশকৃত পরিবর্তন (DDL):**\n```sql\n{suggestion['optimized_query']}\n```\n\n"
                    f"### তুলনামূলক পারফরম্যান্স (`EXPLAIN ANALYZE`):\n\n"
                    f"<details><summary>📉 **আগের পারফরম্যান্স (Seq Scan)**</summary>\n\n"
                    f"```\n{DUMMY_EXPLAIN_BEFORE}\n```\n\n</details>\n\n"
                    f"<details><summary>📈 **প্রস্তাবিত পারফরম্যান্স (Index Scan)**</summary>\n\n"
                    f"```\n{DUMMY_EXPLAIN_AFTER}\n```\n\n</details>\n\n"
                    f"**প্রভাব:** এই পরিবর্তনের ফলে কোয়েরির এক্সিকিউশন টাইম উল্লেখযোগ্যভাবে কমে আসবে এবং ডেটাবেস লোড হ্রাস পাবে।"
                )

                create_github_pr(title, body, branch_name)

    print("✅ বিশ্লেষণ সম্পন্ন হয়েছে।")

if __name__ == "__main__":
    analyze_slow_queries()